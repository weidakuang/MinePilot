package dev.mcai.companion.agent.navigation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

import dev.mcai.companion.agent.navigation.NavigationWorldSnapshot.Cell;
import dev.mcai.companion.agent.navigation.NavigationWorldSnapshot.GridPosition;

/**
 * A bounded multi-policy weighted A* planner. Support-block use and survivable
 * damage are part of the search state, so every returned option is executable
 * with the resources captured in the immutable snapshot.
 */
public final class AnytimeNavigationPlanner {
    private static final int[][] CARDINAL_AND_DIAGONAL = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };
    private static final double SQRT_TWO = Math.sqrt(2.0);
    private static final double MINIMUM_HEALTH_RESERVE = 0.5;

    private final NavigationPlannerConfig config;
    private static final ThreadLocal<Map<GridPosition,Boolean>> OCCUPANCY = new ThreadLocal<>();

    public AnytimeNavigationPlanner(NavigationPlannerConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public NavigationPlan plan(UUID requestId, NavigationWorldSnapshot snapshot) {
        return plan(requestId, snapshot, false);
    }

    public NavigationPlan plan(UUID requestId, NavigationWorldSnapshot snapshot, boolean allowPartial) {
        OCCUPANCY.set(new HashMap<>());
        try { return planSnapshot(requestId,snapshot,allowPartial); }
        finally { OCCUPANCY.remove(); }
    }

    private NavigationPlan planSnapshot(UUID requestId, NavigationWorldSnapshot snapshot, boolean allowPartial) {
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(snapshot, "snapshot");
        long budgetNanos = Math.multiplyExact(config.planningBudgetMillis(), 1_000_000L);
        long startedNanos = System.nanoTime();
        long deadline = startedNanos > Long.MAX_VALUE - budgetNanos
                ? Long.MAX_VALUE
                : startedNanos + budgetNanos;
        Goal goal = effectiveGoal(snapshot);
        SearchResult direct = directWalk(snapshot, goal);
        if (direct != null) return new NavigationPlan(requestId, snapshot.worldRevision(), Instant.now(),
                snapshot.destination(), List.of(toOption(0, direct, snapshot, Policy.FASTEST)));
        LinkedHashMap<String, RouteOption> distinct = new LinkedHashMap<>();
        SearchDiagnostics diagnostics = new SearchDiagnostics(snapshot.start());

        for (Policy policy : Policy.values()) {
            if (distinct.size() >= config.maximumRouteOptions()
                    || expired(deadline)) {
                break;
            }
            long walkDeadline=Math.min(deadline, System.nanoTime()+budgetNanos/3);
            SearchResult result = search(snapshot, goal, policy, walkDeadline, diagnostics, false);
            if(result==null)result=search(snapshot,goal,policy,deadline,diagnostics,true);
            if (result == null) {
                continue;
            }
            RouteOption option = toOption(distinct.size(), result, snapshot, policy);
            distinct.putIfAbsent(signature(option), option);
        }

        if (distinct.isEmpty()) {
            double startDistance = Math.sqrt(Math.pow(snapshot.exactStart().x() - goal.x(), 2)
                    + Math.pow(snapshot.exactStart().y() - goal.y(), 2) + Math.pow(snapshot.exactStart().z() - goal.z(), 2));
            if (allowPartial && diagnostics.nearestNode != null && diagnostics.nearestNode.previous() != null
                    && diagnostics.nearestDistance < startDistance - 1.0) {
                var point = diagnostics.nearestNode.state().position();
                var partial = new NavigationPlan.ResolvedDestination(snapshot.dimension(), point.x() + .5,
                        point.y(), point.z() + .5, .5, false, snapshot.destination().targetIdentity(),
                        java.util.OptionalDouble.empty(), java.util.Optional.empty());
                return new NavigationPlan(requestId, snapshot.worldRevision(), Instant.now(), snapshot.destination(),
                        List.of(toOption(0, reconstruct(diagnostics.nearestNode), snapshot, Policy.SAFEST)),
                        java.util.Optional.of(partial));
            }
            throw new NoRouteException(diagnostics.message(goal));
        }
        return new NavigationPlan(
                requestId,
                snapshot.worldRevision(),
                Instant.now(),
                snapshot.destination(),
                List.copyOf(distinct.values())
        );
    }

    /** Avoid expanding gap candidates when a direct level walk is already safe. */
    private SearchResult directWalk(NavigationWorldSnapshot snapshot, Goal goal) {
        GridPosition from = snapshot.start();
        if (from.y() != goal.position().y() || !canOccupy(snapshot, from, true)) return null;
        List<Transition> steps = new ArrayList<>();
        if (reached(snapshot.exactStart().x(), snapshot.exactStart().y(), snapshot.exactStart().z(), goal))
            return new SearchResult(steps, 0, 0);
        if (reached(snapshot, from, goal) || from.equals(goal.position())) return null;
        for (int i = 0; i < 256; i++) {
            int dx = Integer.signum(goal.position().x() - from.x()), dz = Integer.signum(goal.position().z() - from.z());
            GridPosition next = from.offset(dx, 0, dz);
            if (!canOccupy(snapshot, next, true) || Math.abs(snapshot.feetY(next)-snapshot.feetY(from))>.6 || actionFor(snapshot, next, RouteOption.Action.WALK) != RouteOption.Action.WALK
                    || snapshot.cell(next).hostileRisk() > 0 || snapshot.cell(next.offset(0, -1, 0)).unstable()
                    || dx != 0 && dz != 0 && (!canOccupy(snapshot, from.offset(dx, 0, 0), true)
                    || !canOccupy(snapshot, from.offset(0, 0, dz), true))) return null;
            addTransition(snapshot, steps, from, next, RouteOption.Action.WALK, dx != 0 && dz != 0);
            if (steps.getLast().expectedDamage() > 0 || steps.getLast().risk() > 0) return null;
            if (reached(snapshot, next, goal) || next.equals(goal.position())) return new SearchResult(steps, 0, 0);
            from = next;
        }
        return null;
    }

    private SearchResult search(
            NavigationWorldSnapshot snapshot,
            Goal goal,
            Policy policy,
            long deadline,
            SearchDiagnostics diagnostics,
            boolean includeGaps
    ) {
        PriorityQueue<SearchNode> open = new PriorityQueue<>(Comparator.comparingDouble(SearchNode::f));
        Map<SearchState, Double> best = new HashMap<>();
        SearchState start = new SearchState(snapshot.start(), 0, 0);
        open.add(new SearchNode(start, null, null, 0.0,
                heuristic(start.position(), goal.position(), policy), 0.0));
        best.put(start, 0.0);
        int expanded = 0;

        while (!open.isEmpty() && expanded++ < config.maximumExpandedNodes()
                && !expired(deadline)) {
            SearchNode current = open.poll();
            if (current.g() > best.getOrDefault(current.state(), Double.POSITIVE_INFINITY) + 1.0e-9) {
                continue;
            }
            diagnostics.visit(current, goal);
            if (current.previous() == null && reached(
                    snapshot.exactStart().x(), snapshot.exactStart().y(),
                    snapshot.exactStart().z(), goal)) {
                return reconstruct(current);
            }
            if (reached(snapshot, current.state().position(), goal)
                    || current.state().position().equals(goal.position())) {
                if (current.previous() == null) {
                    // A cell center can be inside the goal while the actual body is outside.
                    // Physically approach that center instead of returning an empty route.
                    if (!canOccupy(snapshot, snapshot.start(), true)) {
                        continue;
                    }
                    GridPosition center = snapshot.start();
                    double distance = Math.sqrt(
                            Math.pow(center.x() + 0.5 - snapshot.exactStart().x(), 2)
                            + Math.pow(center.y() - snapshot.exactStart().y(), 2)
                            + Math.pow(center.z() + 0.5 - snapshot.exactStart().z(), 2));
                    Transition approach = new Transition(center,
                            actionFor(snapshot, center, RouteOption.Action.WALK),
                            distance, 0.0, 0.0, 0, snapshot.cell(center).openableDoor(),
                            snapshot.cell(center).water());
                    return new SearchResult(List.of(approach), 0, 0.0);
                }
                return reconstruct(current);
            }

            for (Transition transition : transitions(
                    snapshot, current.state().position(), current.state().supportBlocksUsed(), includeGaps)) {
                if (transition.action() == RouteOption.Action.GAP_JUMP && !snapshot.resources().canSprint()) continue;
                int nextSupport = current.state().supportBlocksUsed() + transition.supportBlocks();
                double nextDamage = current.predictedDamage() + transition.expectedDamage();
                if (nextSupport > snapshot.resources().supportBlocks()
                        || nextDamage >= survivableHealth(snapshot.resources())) {
                    continue;
                }
                int damageQuarterPoints = (int) Math.ceil(nextDamage * 4.0 - 1.0e-9);
                SearchState nextState = new SearchState(
                        transition.to(), nextSupport, damageQuarterPoints);
                double nextCost = current.g() + policy.cost(
                        transition, policy.pace(snapshot.resources()));
                if (nextCost + 1.0e-9 >= best.getOrDefault(
                        nextState, Double.POSITIVE_INFINITY)) {
                    continue;
                }
                best.put(nextState, nextCost);
                double estimate = heuristic(transition.to(), goal.position(), policy);
                open.add(new SearchNode(
                        nextState,
                        current,
                        transition,
                        nextCost,
                        nextCost + estimate,
                        nextDamage
                ));
            }
        }
        diagnostics.limited |= expired(deadline) || expanded >= config.maximumExpandedNodes();
        return null;
    }

    private static final class SearchDiagnostics {
        final GridPosition start;
        GridPosition nearest;
        GridPosition highest;
        GridPosition farthest;
        double nearestDistance = Double.POSITIVE_INFINITY;
        int visited;
        boolean limited;
        SearchNode nearestNode;
        SearchDiagnostics(GridPosition start) {
            this.start = start;
            nearest = highest = farthest = start;
        }
        void visit(SearchNode node, Goal goal) {
            GridPosition point = node.state().position();
            visited++;
            double distance = Math.sqrt(Math.pow(point.x() + .5 - goal.x(), 2)
                    + Math.pow(point.y() - goal.y(), 2) + Math.pow(point.z() + .5 - goal.z(), 2));
            if (distance < nearestDistance && node.predictedDamage() == 0 && node.state().supportBlocksUsed() == 0) {
                nearestDistance = distance; nearest = point; nearestNode = node;
            }
            if (point.y() > highest.y()) highest = point;
            if (point.distanceTo(start) > farthest.distanceTo(start)) farthest = point;
        }
        String message(Goal goal) {
            return (limited ? "SEARCH_LIMIT_REACHED" : "DISCONNECTED_LOCAL_GRAPH")
                    + ": No complete route; explored " + visited + " nodes. Predicted reachable planning waypoints: "
                    + "nearest to goal " + coordinates(nearest) + ", highest " + coordinates(highest)
                    + ", farthest from start " + coordinates(farthest)
                    + ". These are candidate intermediate targets, not arrival evidence. "
                    + "Do not repeat an unchanged target without new information.";
        }
        String coordinates(GridPosition p) { return "(" + (p.x() + .5) + "," + p.y() + "," + (p.z() + .5) + ")"; }
    }

    private List<Transition> transitions(
            NavigationWorldSnapshot snapshot,
            GridPosition from,
            int supportBlocksUsed,
            boolean includeGaps
    ) {
        List<Transition> result = new ArrayList<>(24);
        Cell fromCell = snapshot.cell(from);
        if (fromCell.climbable() || snapshot.cell(from.offset(0, -1, 0)).climbable()) {
            for (int dy : new int[]{1, -1}) {
                GridPosition next = from.offset(0, dy, 0);
                if (bodySpacePassable(snapshot, next)
                        && (snapshot.cell(next).climbable() || dy > 0 && fromCell.climbable())) {
                    addTransition(snapshot, result, from, next, RouteOption.Action.CLIMB, false);
                }
            }
        }
        if (fromCell.water() && snapshot.resources().canSwim()) {
            for(int dy:new int[]{1,-1}) {
                var next=from.offset(0,dy,0);
                if(snapshot.cell(next).water() && canOccupy(snapshot,next,true))
                    addTransition(snapshot,result,from,next,RouteOption.Action.SWIM,false);
            }
        }
        if (includeGaps && !fromCell.climbable() && !fromCell.water() && snapshot.resources().canSprint()
                && canOccupy(snapshot, from, true) && bodySpacePassable(snapshot, from.offset(0, 1, 0))) {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    double length = Math.hypot(dx, dz);
                    if (length < 1.4 || length > 4.01) continue;
                    for (int dy : new int[]{0, 1, -1}) {
                        if (dy > 0 && length > 3.01) continue;
                        GridPosition landing = from.offset(dx, dy, dz);
                        if (canOccupy(snapshot, landing, true) && jumpCorridorClear(snapshot, from, landing)) {
                            addTransition(snapshot, result, from, landing, RouteOption.Action.GAP_JUMP, false);
                        }
                    }
                }
            }
        }
        for (int[] direction : CARDINAL_AND_DIAGONAL) {
            int dx = direction[0];
            int dz = direction[1];
            boolean diagonal = dx != 0 && dz != 0;
            if (diagonal && (!canOccupy(snapshot, from.offset(dx, 0, 0), true)
                    || !canOccupy(snapshot, from.offset(0, 0, dz), true))) {
                continue;
            }

            GridPosition same = from.offset(dx, 0, dz);
            if (canOccupy(snapshot, same, true) && Math.abs(snapshot.feetY(same)-snapshot.feetY(from))<=.6) {
                addTransition(snapshot, result, from, same,
                        actionFor(snapshot, same, RouteOption.Action.WALK), diagonal);
                continue;
            }

            GridPosition up = from.offset(dx, 1, dz);
            if (canOccupy(snapshot, up, true) && snapshot.feetY(up)-snapshot.feetY(from)<=1.25
                    && snapshot.contains(from.offset(0, 2, 0))
                    && snapshot.cell(from.offset(0, 2, 0)).passable()) {
                addTransition(snapshot, result, from, up, RouteOption.Action.JUMP, diagonal);
                continue;
            }

            for (int drop = 1; drop <= config.maximumDropBlocks(); drop++) {
                GridPosition down = from.offset(dx, -drop, dz);
                if (!bodySpacePassable(snapshot, down)) {
                    break;
                }
                if (canOccupy(snapshot, down, true)) {
                    RouteOption.Action action = drop == 1
                            ? RouteOption.Action.STEP_DOWN
                            : RouteOption.Action.DROP;
                    addTransition(snapshot, result, from, down, action, diagonal);
                    break;
                }
            }

            if (!diagonal && canBridge(snapshot, from, same, supportBlocksUsed)) {
                addTransition(snapshot, result, from, same,
                        RouteOption.Action.PLACE_SUPPORT, diagonal);
            }
        }
        return result;
    }

    private static boolean jumpCorridorClear(NavigationWorldSnapshot snapshot, GridPosition from, GridPosition to) {
        double length = Math.hypot(to.x() - from.x(), to.z() - from.z());
        int samples = (int) Math.ceil(length * 4);
        boolean gap = false;
        for (int sample = 1; sample < samples; sample++) {
            double t = (double) sample / samples;
            double x = from.x() + .5 + (to.x() - from.x()) * t;
            double z = from.z() + .5 + (to.z() - from.z()) * t;
            int y = Math.max(from.y(), to.y());
            gap |= !canOccupy(snapshot, new GridPosition((int) Math.floor(x), from.y(), (int) Math.floor(z)), true);
            for (int bx = (int) Math.floor(x - .29); bx <= (int) Math.floor(x + .29); bx++) {
                for (int bz = (int) Math.floor(z - .29); bz <= (int) Math.floor(z + .29); bz++) {
                    GridPosition feet = new GridPosition(bx, y, bz);
                    if (!bodySpacePassable(snapshot, feet) || !bodySpacePassable(snapshot, feet.offset(0, 1, 0))) return false;
                }
            }
        }
        return gap;
    }

    private static boolean bodySpacePassable(
            NavigationWorldSnapshot snapshot,
            GridPosition feet
    ) {
        return snapshot.bodyClear(feet.x()+.5,snapshot.feetY(feet),feet.z()+.5);
    }

    private static boolean canOccupy(
            NavigationWorldSnapshot snapshot,
            GridPosition feet,
            boolean requireSupport
    ) {
        var cache=OCCUPANCY.get();
        if(requireSupport && cache!=null) {
            Boolean cached=cache.get(feet);if(cached!=null)return cached;
            boolean result=occupiable(snapshot,feet,true);cache.put(feet,result);return result;
        }
        return occupiable(snapshot,feet,requireSupport);
    }

    private static boolean occupiable(NavigationWorldSnapshot snapshot, GridPosition feet, boolean requireSupport) {
        if (!bodySpacePassable(snapshot, feet)) return false;
        Cell feetCell = snapshot.cell(feet);
        if (feetCell.climbable()) return true;
        if (feetCell.water()) {
            return snapshot.resources().canSwim();
        }
        return !requireSupport || !Double.isNaN(snapshot.standingY(feet));
    }

    private static boolean canBridge(
            NavigationWorldSnapshot snapshot,
            GridPosition from,
            GridPosition feet,
            int supportBlocksUsed
    ) {
        GridPosition head = feet.offset(0, 1, 0);
        GridPosition floor = feet.offset(0, -1, 0);
        GridPosition anchor = from.offset(0, -1, 0);
        if (supportBlocksUsed >= snapshot.resources().supportBlocks()
                || !snapshot.contains(feet) || !snapshot.contains(head)
                || !snapshot.contains(floor) || !snapshot.contains(anchor)) {
            return false;
        }
        Cell feetCell = snapshot.cell(feet);
        Cell headCell = snapshot.cell(head);
        Cell floorCell = snapshot.cell(floor);
        Cell anchorCell = snapshot.cell(anchor);
        return feetCell.passable() && headCell.passable()
                && !feetCell.water() && !feetCell.lava() && !headCell.lava()
                && !feetCell.damaging() && !headCell.damaging()
                && !floorCell.collision() && !floorCell.water() && !floorCell.lava()
                && anchorCell.collision() && !anchorCell.unstable();
    }

    private static void addTransition(
            NavigationWorldSnapshot snapshot,
            List<Transition> output,
            GridPosition from,
            GridPosition to,
            RouteOption.Action action,
            boolean diagonal
    ) {
        Cell feet = snapshot.cell(to);
        Cell floor = snapshot.cell(to.offset(0, -1, 0));
        int descent = Math.max(0, from.y() - to.y());
        double fallDamage = descent == 0 ? 0.0 : Math.max(0.0, Math.floor(
                (descent + 1.0e-6 - snapshot.resources().safeFallDistance())
                        * snapshot.resources().fallDamageMultiplier()));
        double hostileRisk = feet.hostileRisk() + floor.hostileRisk();
        double terrainRisk = (feet.damaging() ? 8.0 : 0.0)
                + (floor.damaging() ? 4.0 : 0.0)
                + (feet.unstable() || floor.unstable() ? 2.0 : 0.0)
                + (feet.water() ? 0.4 : 0.0);
        // Falls and several terrain damage sources bypass ordinary armor. Ignore
        // beneficial enchantments/effects here until their estimates are verified.
        double expectedDamage = fallDamage
                + (feet.damaging() ? 2.0 : 0.0)
                + (floor.damaging() ? 1.0 : 0.0)
                + Math.min(6.0, hostileRisk * 0.25)
                * snapshot.resources().expectedDamageMultiplier();
        double horizontal = Math.hypot(to.x() - from.x(), to.z() - from.z());
        double distance = Math.hypot(horizontal, to.y() - from.y());
        // Entry/down/up transitions must agree with live fluid validation too.
        if(feet.water())action=RouteOption.Action.SWIM;
        output.add(new Transition(
                to,
                action,
                distance,
                expectedDamage,
                hostileRisk + terrainRisk,
                action == RouteOption.Action.PLACE_SUPPORT ? 1 : 0,
                feet.openableDoor(),
                feet.water()
        ));
    }

    private static RouteOption.Action actionFor(
            NavigationWorldSnapshot snapshot,
            GridPosition position,
            RouteOption.Action fallback
    ) {
        Cell cell = snapshot.cell(position);
        if (cell.openableDoor()) {
            return RouteOption.Action.OPEN_DOOR;
        }
        if (cell.climbable()) return RouteOption.Action.CLIMB;
        if (cell.water()) {
            return RouteOption.Action.SWIM;
        }
        return fallback;
    }

    private RouteOption toOption(
            int index,
            SearchResult result,
            NavigationWorldSnapshot snapshot,
            Policy policy
    ) {
        TravelPace suggestedPace = policy.pace(snapshot.resources());
        Set<TravelPace> supportedPaces = supportedPaces(result, snapshot.resources());
        if (!supportedPaces.contains(suggestedPace)) {
            suggestedPace = supportedPaces.contains(TravelPace.WALK)
                    ? TravelPace.WALK
                    : supportedPaces.iterator().next();
        }

        List<RouteOption.PathStep> steps = new ArrayList<>(result.transitions().size());
        double distance = 0.0;
        double damage = 0.0;
        double risk = 0.0;
        double seconds = 0.0;
        double exhaustion = 0.0;
        int blocks = 0;
        Set<String> hazards = new HashSet<>();
        Set<String> actions = new HashSet<>();

        for (Transition transition : result.transitions()) {
            TravelPace stepPace = paceForTransition(suggestedPace, transition);
            GridPosition to = transition.to();
            steps.add(new RouteOption.PathStep(
                    to.x() + 0.5,
                    snapshot.feetY(to),
                    to.z() + 0.5,
                    transition.action(),
                    stepPace,
                    yawTolerance(transition.action())
            ));
            distance += transition.distance();
            damage += transition.expectedDamage();
            risk += transition.risk();
            blocks += transition.supportBlocks();
            seconds += secondsFor(transition, stepPace);
            exhaustion += exhaustionFor(transition, stepPace);

            if (transition.water()) {
                hazards.add("water traversal");
            }
            if (transition.expectedDamage() > 0.0) {
                hazards.add(String.format(Locale.ROOT,
                        "predicted health loss up to %.2f", transition.expectedDamage()));
            }
            if (transition.risk() > 0.0) {
                hazards.add("server-predicted terrain or nearby-entity exposure");
            }
            if (transition.action() == RouteOption.Action.GAP_JUMP) actions.add("running jump across a gap");
            if (transition.action() == RouteOption.Action.CLIMB) actions.add("climb an existing ladder or scaffolding");
            if (transition.openDoor()) {
                actions.add("open a door through normal item use");
            }
            if (transition.supportBlocks() > 0) {
                actions.add("sneak and place a support block against a verified anchor");
            }
        }

        // End at the requested point when its cell is reached. Grid centers are
        // planning nodes, not substitutes for arbitrary coordinate targets.
        if (!steps.isEmpty()) {
            int last = steps.size() - 1;
            RouteOption.PathStep end = steps.get(last);
            var destination = snapshot.destination();
            if ((int) Math.floor(end.x()) == (int) Math.floor(destination.x())
                    && (int) Math.floor(end.y()) == (int) Math.floor(destination.y())
                    && (int) Math.floor(end.z()) == (int) Math.floor(destination.z())
                    && Math.abs(end.y()-destination.y())<.05) {
                steps.set(last, new RouteOption.PathStep(destination.x(), destination.y(),
                        destination.z(), end.action(), end.recommendedPace(), end.yawTolerance()));
            }
        }
        double foodLost = estimatedFoodLoss(snapshot.resources(), exhaustion);
        return new RouteOption(
                String.valueOf((char) ('A' + index)),
                policy.label,
                suggestedPace,
                supportedPaces,
                seconds,
                distance,
                exhaustion,
                foodLost,
                damage,
                blocks,
                risk,
                true,
                hazards.stream().sorted().toList(),
                actions.stream().sorted().toList(),
                steps,
                supportManifest(snapshot.resources().supportStock(), blocks)
        );
    }

    public static List<RouteOption.SupportMaterial> supportManifest(List<RouteOption.SupportMaterial> stock, int required) {
        var selected = new java.util.LinkedHashMap<String,RouteOption.SupportMaterial>();
        int remaining = required;
        for (var material:stock.stream().sorted(java.util.Comparator.comparingInt(RouteOption.SupportMaterial::importance).reversed()
                .thenComparing(RouteOption.SupportMaterial::entryId)).toList()) {
            if(remaining==0)break;
            int count=Math.min(remaining,material.count());remaining-=count;
            var old=selected.get(material.entryId());
            selected.put(material.entryId(),new RouteOption.SupportMaterial(material.entryId(),material.item(),count+(old==null?0:old.count()),material.importance()));
        }
        if(remaining!=0)throw new IllegalStateException("The route has no complete expendable material manifest");
        return List.copyOf(selected.values());
    }

    private static Set<TravelPace> supportedPaces(
            SearchResult result,
            NavigationWorldSnapshot.BodyResources resources
    ) {
        EnumSet<TravelPace> paces = EnumSet.of(TravelPace.WALK, TravelPace.SNEAK);
        if (resources.canSprint()) {
            paces.add(TravelPace.SPRINT);
            paces.add(TravelPace.SPRINT_JUMP);
        }
        for (Transition transition : result.transitions()) {
            if (transition.action() == RouteOption.Action.GAP_JUMP) {
                paces.retainAll(EnumSet.of(TravelPace.SPRINT, TravelPace.SPRINT_JUMP));
            }
            if ((transition.action() == RouteOption.Action.PLACE_SUPPORT
                    || transition.action() == RouteOption.Action.SNEAK_EDGE)
                    && result.transitions().stream().noneMatch(t -> t.action() == RouteOption.Action.GAP_JUMP)) {
                paces.retainAll(EnumSet.of(TravelPace.WALK, TravelPace.SNEAK));
            }
            if (transition.water()) {
                paces.remove(TravelPace.SPRINT_JUMP);
            }
        }
        return Set.copyOf(paces);
    }

    private static TravelPace paceForTransition(
            TravelPace suggested,
            Transition transition
    ) {
        return switch (transition.action()) {
            case GAP_JUMP -> TravelPace.SPRINT_JUMP;
            case CLIMB -> TravelPace.WALK;
            case PLACE_SUPPORT, SNEAK_EDGE -> TravelPace.SNEAK;
            case OPEN_DOOR, STEP_DOWN, DROP -> TravelPace.WALK;
            case SWIM -> suggested == TravelPace.SNEAK ? TravelPace.WALK : suggested;
            default -> suggested;
        };
    }

    private static float yawTolerance(RouteOption.Action action) {
        return switch (action) {
            case PLACE_SUPPORT, SNEAK_EDGE -> 8.0F;
            case JUMP, GAP_JUMP, DROP, STEP_DOWN -> 18.0F;
            case OPEN_DOOR -> 15.0F;
            default -> 42.0F;
        };
    }

    private static double secondsFor(Transition transition, TravelPace pace) {
        double speed = switch (pace) {
            case SNEAK -> 1.30;
            case WALK, AUTO -> 4.317;
            case SPRINT -> 5.612;
            case SPRINT_JUMP -> 7.127;
        };
        double actionSeconds = switch (transition.action()) {
            case PLACE_SUPPORT -> 0.55;
            case OPEN_DOOR -> 0.25;
            case JUMP, GAP_JUMP, DROP -> 0.12;
            case CLIMB -> 0.3;
            default -> 0.0;
        };
        return transition.distance() / speed + actionSeconds;
    }

    private static double exhaustionFor(Transition transition, TravelPace pace) {
        if (transition.water()) {
            return transition.distance() * 0.01;
        }
        double exhaustion = switch (pace) {
            case SPRINT, SPRINT_JUMP -> transition.distance() * 0.1;
            case SNEAK, WALK, AUTO -> 0.0;
        };
        if ((transition.action() == RouteOption.Action.JUMP || transition.action() == RouteOption.Action.GAP_JUMP)) {
            exhaustion += pace == TravelPace.SPRINT || pace == TravelPace.SPRINT_JUMP
                    ? 0.2
                    : 0.05;
        } else if (pace == TravelPace.SPRINT_JUMP
                && transition.action() == RouteOption.Action.WALK) {
            exhaustion += transition.distance() * 0.05;
        }
        return exhaustion;
    }

    private static double estimatedFoodLoss(
            NavigationWorldSnapshot.BodyResources resources,
            double addedExhaustion
    ) {
        double total = resources.exhaustion() + addedExhaustion;
        int depletionEvents = (int) Math.floor(total / 4.0);
        return Math.max(0.0, depletionEvents - resources.saturation());
    }

    private static SearchResult reconstruct(SearchNode end) {
        List<Transition> reverse = new ArrayList<>();
        SearchNode cursor = end;
        while (cursor.previous() != null && cursor.transition() != null) {
            reverse.add(cursor.transition());
            cursor = cursor.previous();
        }
        List<Transition> ordered = new ArrayList<>(reverse.size());
        for (int i = reverse.size() - 1; i >= 0; i--) {
            ordered.add(reverse.get(i));
        }
        return new SearchResult(ordered, end.state().supportBlocksUsed(), end.predictedDamage());
    }

    private static Goal effectiveGoal(NavigationWorldSnapshot snapshot) {
        NavigationPlan.ResolvedDestination destination = snapshot.destination();
        double x = destination.x();
        double y = destination.y();
        double z = destination.z();
        // Velocity is not a collision-aware forecast: resting entities retain gravity
        // or bounce velocity, which can extrapolate a ground target underground.
        // Plan to observed coordinates; the coordinator re-resolves moving targets.
        return new Goal(
                new GridPosition((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)),
                x,
                y,
                z,
                destination.acceptanceRadius()
        );
    }

    private static boolean reached(NavigationWorldSnapshot snapshot, GridPosition position, Goal goal) {
        // A cell exactly on the radius leaves no room for physical braking error.
        // Choose an interior endpoint; actual-body verification keeps the full
        // requested radius and never reports an outside position as arrived.
        double dx=position.x()+.5-goal.x(), dy=snapshot.feetY(position)-goal.y(), dz=position.z()+.5-goal.z();
        double radius=Math.max(0,goal.acceptanceRadius()-.05);
        return dx*dx+dy*dy+dz*dz<=radius*radius;
    }

    private static boolean reached(double x, double y, double z, Goal goal) {
        double dx = x - goal.x();
        double dy = y - goal.y();
        double dz = z - goal.z();
        return dx * dx + dy * dy + dz * dz <= goal.acceptanceRadius() * goal.acceptanceRadius();
    }

    private static double heuristic(GridPosition from, GridPosition goal, Policy policy) {
        double dx = Math.abs(from.x() - goal.x());
        double dy = Math.abs(from.y() - goal.y());
        double dz = Math.abs(from.z() - goal.z());
        return (Math.max(dx, dz) + 0.35 * Math.min(dx, dz) + dy)
                * policy.heuristicWeight;
    }

    private static double survivableHealth(NavigationWorldSnapshot.BodyResources resources) {
        return Math.max(0.0, resources.health() + resources.absorption() - MINIMUM_HEALTH_RESERVE);
    }

    private static boolean expired(long deadline) {
        return Thread.currentThread().isInterrupted() || System.nanoTime() >= deadline;
    }

    private static String signature(RouteOption option) {
        StringBuilder value = new StringBuilder();
        for (RouteOption.PathStep step : option.steps()) {
            value.append((int) Math.floor(step.x())).append(',')
                    .append((int) Math.floor(step.y())).append(',')
                    .append((int) Math.floor(step.z())).append(',')
                    .append(step.action()).append(';');
        }
        return value.toString();
    }

    private record SearchState(
            GridPosition position,
            int supportBlocksUsed,
            int damageQuarterPoints
    ) {
    }

    private record SearchNode(
            SearchState state,
            SearchNode previous,
            Transition transition,
            double g,
            double f,
            double predictedDamage
    ) {
    }

    private record Transition(
            GridPosition to,
            RouteOption.Action action,
            double distance,
            double expectedDamage,
            double risk,
            int supportBlocks,
            boolean openDoor,
            boolean water
    ) {
    }

    private record SearchResult(
            List<Transition> transitions,
            int supportBlocks,
            double predictedDamage
    ) {
    }

    private record Goal(
            GridPosition position,
            double x,
            double y,
            double z,
            double acceptanceRadius
    ) {
    }

    private enum Policy {
        FASTEST("fastest by estimated traversal time", 1.25, 0.4, 4.0, 2.0, TravelPace.SPRINT),
        SHORTEST("shortest by traversed blocks", 1.00, 0.8, 5.0, 2.5, TravelPace.SPRINT),
        SAFEST("lowest combined terrain and entity risk", 1.08, 14.0, 18.0, 3.0, TravelPace.SPRINT),
        LOW_RESOURCE("fewest placed support blocks", 1.04, 2.0, 5.0, 24.0, TravelPace.WALK),
        LOW_DAMAGE("lowest predicted health loss", 1.06, 3.0, 40.0, 5.0, TravelPace.WALK),
        CONSERVATIVE("low exhaustion and low exposure", 1.12, 8.0, 12.0, 6.0, TravelPace.WALK),
        STEALTH("edge-safe sneaking route", 1.15, 10.0, 14.0, 8.0, TravelPace.SNEAK),
        DIRECT("direct feasible route with light risk penalty", 1.35, 1.0, 7.0, 1.0, TravelPace.SPRINT);

        private final String label;
        private final double heuristicWeight;
        private final double riskWeight;
        private final double damageWeight;
        private final double blockWeight;
        private final TravelPace desiredPace;

        Policy(
                String label,
                double heuristicWeight,
                double riskWeight,
                double damageWeight,
                double blockWeight,
                TravelPace desiredPace
        ) {
            this.label = label;
            this.heuristicWeight = heuristicWeight;
            this.riskWeight = riskWeight;
            this.damageWeight = damageWeight;
            this.blockWeight = blockWeight;
            this.desiredPace = desiredPace;
        }

        private double cost(Transition transition, TravelPace pace) {
            double exhaustionWeight = this == CONSERVATIVE ? 5.0 : 0.15;
            return secondsFor(transition, pace)
                    + transition.risk() * riskWeight
                    + transition.expectedDamage() * damageWeight
                    + transition.supportBlocks() * blockWeight
                    + exhaustionFor(transition, pace) * exhaustionWeight;
        }

        private TravelPace pace(NavigationWorldSnapshot.BodyResources resources) {
            if (!resources.canSprint()
                    && EnumSet.of(TravelPace.SPRINT, TravelPace.SPRINT_JUMP).contains(desiredPace)) {
                return TravelPace.WALK;
            }
            return desiredPace;
        }
    }

    public static final class NoRouteException extends RuntimeException {
        public NoRouteException(String message) {
            super(message);
        }
    }
}
