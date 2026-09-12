package dev.mcai.companion.agent.navigation;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import dev.mcai.companion.agent.body.MinePilotServerPlayer;
import net.minecraft.server.MinecraftServer;

/**
 * Enforces the navigation tool protocol. In particular, planning cannot start
 * until the model's acknowledgement has actually been emitted to chat.
 */
public final class NavigationToolCoordinator implements AutoCloseable {
    private final MinecraftServer server;
    private final MinePilotServerPlayer player;
    private final NavigationTargetResolver targetResolver;
    private final NavigationSnapshotBuilder snapshotBuilder;
    private final PlanningExecutor planningExecutor;
    private final NavigationFollower follower;
    private final Consumer<NavigationEvent> events;
    private final ConcurrentLinkedQueue<Runnable> serverCompletions = new ConcurrentLinkedQueue<>();

    private ActiveNavigation active;
    private NavigationEvent lastEvent;
    private long worldRevision;

    public NavigationToolCoordinator(
            MinecraftServer server,
            MinePilotServerPlayer player,
            NavigationPlannerConfig plannerConfig,
            NavigationSnapshotBuilder.CaptureConfig captureConfig,
            Consumer<NavigationEvent> events
    ) {
        this.server = Objects.requireNonNull(server, "server");
        this.player = Objects.requireNonNull(player, "player");
        this.targetResolver = new NavigationTargetResolver();
        this.snapshotBuilder = new NavigationSnapshotBuilder(captureConfig);
        this.planningExecutor = new PlanningExecutor(plannerConfig);
        this.events = Objects.requireNonNull(events, "events");
        this.follower = new NavigationFollower(
                player, this::onFollowerEvent, this::arrivalProblem, this::markWorldChanged);
    }

    public NavigationEvent requestNavigation(NavigationIntent intent) {
        return requestNavigation(intent, null);
    }

    /** Validate a replacement before stopping the exact request observed by the controller. */
    public NavigationEvent requestNavigation(NavigationIntent intent, UUID replacesRequest) {
        requireServerThread();
        var runtime=dev.mcai.companion.agent.AgentRuntime.active(server);
        if(runtime!=null && (runtime.excavation()!=null && runtime.excavation().ownsBody() || runtime.placement().ownsBody() || runtime.mining().ownsBody() || runtime.collection().ownsBody()))throw new ProtocolException("Cancel mining/collection before requesting navigation");
        Objects.requireNonNull(intent, "intent");
        if (!player.isAlive()) throw new ProtocolException("The Agent body is dead; navigation cannot start");
        if (intent.worldRevision() != worldRevision) {
            throw new ProtocolException("request_navigation used a stale world revision");
        }
        if (replacesRequest != null) {
            if (active == null || !active.intent.requestId().equals(replacesRequest))
                throw new ProtocolException("Replacement refers to a stale navigation request");
            targetResolver.resolve(server, player, intent.target());
            if (!active.phase.terminal()) cancel(replacesRequest, "Player changed destination");
        } else if (active != null && !active.phase.terminal()) {
            throw new ProtocolException("Another navigation request is active; supply replace_request_id to change the destination");
        }
        active = new ActiveNavigation(intent, Phase.ACKNOWLEDGEMENT_REQUIRED);
        NavigationEvent event = new NavigationEvent(
                NavigationEvent.Type.NAVIGATION_ACCEPTED,
                intent.requestId(),
                "Navigation intent accepted; emit one acknowledgement before planning",
                observedState(),
                List.of("say")
        );
        emit(event);
        return event;
    }

    /** Called only after the acknowledgement message is visible in server chat. */
    public void acknowledgementSent(UUID requestId) {
        requireServerThread();
        var runtime=dev.mcai.companion.agent.AgentRuntime.active(server);
        if(runtime!=null && (runtime.excavation()!=null && runtime.excavation().ownsBody() || runtime.placement().ownsBody() || runtime.mining().ownsBody() || runtime.collection().ownsBody()))throw new ProtocolException("Cancel mining/collection before starting navigation");
        ActiveNavigation current = requireActive(requestId);
        if (current.phase != Phase.ACKNOWLEDGEMENT_REQUIRED) {
            throw new ProtocolException("SAY_SENT is not valid in phase " + current.phase);
        }
        current.phase = Phase.ACKNOWLEDGED;
    }

    public void planNavigation(UUID requestId) {
        requireServerThread();
        var runtime=dev.mcai.companion.agent.AgentRuntime.active(server);
        if(runtime!=null && (runtime.excavation()!=null && runtime.excavation().ownsBody() || runtime.placement().ownsBody() || runtime.mining().ownsBody() || runtime.collection().ownsBody()))throw new ProtocolException("Cancel mining/collection before starting navigation");
        ActiveNavigation current = requireActive(requestId);
        if (current.phase == Phase.COMPLETED && current.intent.target().kind() == NavigationTarget.Kind.PLAYER) return;
        if (current.phase != Phase.ACKNOWLEDGED
                && current.phase != Phase.REPLAN_REQUIRED) {
            throw new ProtocolException(
                    "plan_navigation requires an emitted acknowledgement or replan event");
        }
        current.phase = Phase.PLANNING;
        NavigationPlan.ResolvedDestination destination;
        NavigationSnapshotBuilder.Capture capture;
        try {
            destination = targetResolver.resolve(server, player, current.intent.target());
            current.destination = destination;
            capture = snapshotBuilder.begin(player, destination, worldRevision);
        } catch (NavigationTargetResolver.UnresolvedTargetException
                | NavigationSnapshotBuilder.SnapshotUnavailableException failure) {
            completePlanning(requestId, null, failure);
            return;
        }
        current.destination = destination;
        current.planningRevision = worldRevision;
        current.capture=capture;
    }

    private void advanceCapture() {
        if(active==null || active.phase!=Phase.PLANNING || active.capture==null)return;
        var current=active;
        try {
            if(!current.capture.advance(2_000_000L))return;
            var snapshot=current.capture;current.capture=null;
            if(player.position().distanceToSqr(new net.minecraft.world.phys.Vec3(snapshot.startPosition().x(),snapshot.startPosition().y(),snapshot.startPosition().z()))>.25) {
                current.phase=Phase.REPLAN_REQUIRED;planNavigation(current.intent.requestId());return;
            }
            UUID requestId=current.intent.requestId();
            CompletableFuture<NavigationPlan> future=planningExecutor.submit(requestId,snapshot,current.intent.allowPartial());
            current.planningFuture=future;
            future.whenComplete((plan,failure)->serverCompletions.add(()->completePlanning(requestId,plan,failure)));
        } catch(RuntimeException failure) {current.capture=null;completePlanning(current.intent.requestId(),null,failure);}
    }

    public boolean chooseNavigation(
            UUID requestId,
            String optionId,
            TravelPace pace
    ) {
        requireServerThread();
        ActiveNavigation current = requireActive(requestId);
        if (current.phase == Phase.COMPLETED && current.intent.target().kind() == NavigationTarget.Kind.PLAYER) return true;
        if (current.phase != Phase.PLAN_READY || current.plan == null) {
            throw new ProtocolException("choose_navigation requires NAVIGATION_PLAN_READY");
        }
        if (current.plan.plannedWorldRevision() != worldRevision) {
            current.phase = Phase.REPLAN_REQUIRED;
            emit(new NavigationEvent(
                    NavigationEvent.Type.NAVIGATION_DECISION_REQUIRED,
                    requestId,
                    "The world changed after planning; the route must be repaired",
                    observedState(),
                    List.of("plan_navigation", "cancel_navigation", "say")
            ));
            return false;
        }
        RouteOption option = current.plan.requireOption(optionId);
        follower.start(
                current.plan,
                option,
                Objects.requireNonNull(pace, "pace"),
                current.destination == null
                        ? OptionalDouble.empty()
                        : current.destination.arrivalHeading(),
                current.intent.continuousFollow()
        );
        current.phase = Phase.EXECUTING;
        current.selectedOption = option.optionId();
        current.selectedPace = pace == TravelPace.AUTO ? option.suggestedPace() : pace;
        return true;
    }

    public void cancel(UUID requestId, String reason) {
        requireServerThread();
        ActiveNavigation current = requireActive(requestId);
        if (current.planningFuture != null) {
            current.planningFuture.cancel(true);
        }
        if (follower.isActive()) {
            follower.cancel(reason);
        } else {
            current.phase = Phase.CANCELLED;
            emit(new NavigationEvent(
                    NavigationEvent.Type.NAVIGATION_CANCELLED,
                    requestId,
                    reason,
                    observedState(),
                    List.of()
            ));
        }
    }

    /** Called when live corridor validation observes a material change. */
    private void markWorldChanged(String reason) {
        requireServerThread();
        worldRevision++;
        if (active != null && active.phase == Phase.EXECUTING) {
            follower.requestReplan(reason);
        }
    }

    public void beforePhysicsTick() {
        requireServerThread();
        if (!finishPlayerProximity()) follower.beforePhysicsTick();
    }

    public void tick() {
        requireServerThread();
        if (finishPlayerProximity()) return;
        advanceCapture();
        Runnable completion;
        while ((completion = serverCompletions.poll()) != null) {
            completion.run();
        }
        updateDynamicTarget();
        follower.tick();
    }

    /** Numen's live closeEnough check belongs before waypoint/retarget work. */
    private boolean finishPlayerProximity() {
        var current = active;
        if (current == null || current.phase.terminal()
                || current.phase == Phase.ACKNOWLEDGEMENT_REQUIRED || current.phase == Phase.ACKNOWLEDGED
                || current.intent.target().kind() != NavigationTarget.Kind.PLAYER) return false;
        try {
            var target = targetResolver.resolve(server, player, current.intent.target());
            if (!target.dimension().equals(player.level().dimension().identifier().toString())) return false;
            var position = new net.minecraft.world.phys.Vec3(target.x(), target.y(), target.z());
            if (!current.intent.continuousFollow()) {
                if (player.position().distanceToSqr(position) > 9) return false;
                finishAtPlayer(target, "Arrived within three blocks of the player; movement finished.");
                return true;
            }
            // Position, not gaze changes, renews the stationary timer. Only
            // finish after we have caught up, never while stranded far away.
            if (current.stationaryAnchor == null || position.distanceToSqr(current.stationaryAnchor) > .0625) {
                current.stationaryAnchor = position;
                current.stationarySince = player.tickCount;
            }
            if (player.tickCount - current.stationarySince >= 600
                    && player.position().distanceToSqr(position) <= 9) {
                finishAtPlayer(target, "Follow finished: player stayed here for 30 seconds.");
                return true;
            }
        } catch (NavigationTargetResolver.UnresolvedTargetException ignored) {
            // The normal target-loss handler reports the authoritative failure.
        }
        return false;
    }

    /** Explicit arrival chat ends only this speaker's active follow. */
    public boolean finishFollow(String speaker) {
        requireServerThread();
        if (active == null || active.phase.terminal() || !active.intent.continuousFollow()
                || active.intent.target().kind() != NavigationTarget.Kind.PLAYER) return false;
        try {
            var target = targetResolver.resolve(server, player, active.intent.target());
            var human = server.getPlayerList().getPlayer(UUID.fromString(target.targetIdentity()));
            if (human == null || !human.getGameProfile().name().equalsIgnoreCase(speaker)) return false;
            finishAtPlayer(target, "Follow finished: followed player said we have arrived.");
            return true;
        } catch (NavigationTargetResolver.UnresolvedTargetException ignored) { return false; }
    }

    private void finishAtPlayer(NavigationPlan.ResolvedDestination target, String reason) {
        var current = active;
        current.suppressFollowerEvents = true;
        try {
            if (current.planningFuture != null) current.planningFuture.cancel(true);
            current.capture = null;
            follower.cancel(reason);
            player.stopControlling();
        } finally { current.suppressFollowerEvents = false; }
        current.destination = target;
        current.phase = Phase.COMPLETED;
        var runtime = dev.mcai.companion.agent.AgentRuntime.active(server);
        if (runtime != null) runtime.attendToPlayer(UUID.fromString(target.targetIdentity()));
        emit(new NavigationEvent(NavigationEvent.Type.NAVIGATION_COMPLETED, current.intent.requestId(),
                reason, observedState(), List.of("say", "request_navigation")));
    }

    public Status status() {
        ActiveNavigation current = active;
        return current == null
                ? new Status(null, Phase.IDLE, worldRevision, null, null, null,
                        lastEvent == null ? null : lastEvent.message(),
                        Phase.IDLE.validActions(),
                        List.of())
                : new Status(
                current.intent.requestId(),
                current.phase,
                worldRevision,
                current.selectedOption,
                current.selectedPace,
                current.destination,
                lastEvent == null ? null : lastEvent.message(),
                current.phase.validActions(),
                current.plan == null ? List.of() : current.plan.options()
        );
    }

    public boolean continuousFollow() {return active!=null && active.intent.continuousFollow();}

    public Optional<NavigationPlan.ResolvedDestination> partialDestination() {
        return active == null || active.plan == null ? Optional.empty() : active.plan.partialDestination();
    }

    private void updateDynamicTarget() {
        ActiveNavigation current = active;
        if (current == null || (current.phase != Phase.EXECUTING && current.phase != Phase.FOLLOWING)
                || current.destination == null || !current.destination.dynamic() || partialDestination().isPresent()
                || player.tickCount - current.lastDynamicCheckTick < (current.intent.continuousFollow() ? 1 : 5)) {
            return;
        }
        current.lastDynamicCheckTick = player.tickCount;
        try {
            NavigationPlan.ResolvedDestination latest = targetResolver.resolve(
                    server, player, current.intent.target());
            if (current.intent.continuousFollow()) {
                if (!latest.dimension().equals(current.destination.dimension())) {
                    stopLostFollow("The followed target changed dimension; a verified portal route is required");return;
                }
                // Numen's resident/live-goal policy, using our own checked corridor
                // and body. A nearby moving player need not create another path job.
                if (follower.trackFollowTarget(latest)) {
                    current.destination = latest;
                    return;
                }
            }
            if (current.phase == Phase.FOLLOWING) {
                double resumeRadius=latest.acceptanceRadius()+.75;
                if (!latest.dimension().equals(current.destination.dimension())) {
                    stopLostFollow("The followed target changed dimension; a verified portal route is required");return;
                }
                if (square(player.getX()-latest.x())+square(player.getY()-latest.y())+square(player.getZ()-latest.z()) > square(resumeRadius)) {
                    current.destination=latest;
                    repairFollow();
                }
                return;
            }
            double driftSquared = square(latest.x() - current.destination.x())
                    + square(latest.y() - current.destination.y())
                    + square(latest.z() - current.destination.z());
            if (!latest.dimension().equals(current.destination.dimension())) {
                if(current.intent.continuousFollow())stopLostFollow("The followed target changed dimension; a verified portal route is required");
                else follower.requestReplan("The destination changed dimension");
                return;
            }
            if(driftSquared>.04 && follower.retargetLevel(latest)) {
                current.destination=latest;
            } else if(driftSquared>square(Math.max(2,latest.acceptanceRadius()))
                    && follower.distanceToRouteEnd()<6) {
                // Keep the valid prefix while the destination moves. Repair at
                // the end of that prefix instead of stopping on every footstep.
                current.destination=latest;
                follower.requestReplan("The moving destination left the active route corridor");
            }
        } catch (NavigationTargetResolver.UnresolvedTargetException failure) {
            if(current.intent.continuousFollow())stopLostFollow(failure.getMessage());
            else follower.requestReplan(failure.getMessage());
        }
    }

    private void stopLostFollow(String reason) {
        active.suppressFollowerEvents=true;
        try {if(follower.isActive())follower.cancel(reason);}
        finally {active.suppressFollowerEvents=false;}
        active.phase=Phase.FAILED;
        emit(new NavigationEvent(NavigationEvent.Type.NAVIGATION_FAILED,active.intent.requestId(),reason,observedState(),List.of("say","request_navigation")));
    }
    private void repairFollow() {
        // A resting follow can retain its executor; release it before an actual
        // terrain repair so it cannot publish arrival over the new planning phase.
        if (active.intent.continuousFollow() && follower.isActive()) {
            active.suppressFollowerEvents = true;
            try { follower.cancel("Repairing the followed target's route"); }
            finally { active.suppressFollowerEvents = false; }
        }
        active.phase=Phase.REPLAN_REQUIRED;
        active.followRepair=true;
        planNavigation(active.intent.requestId());
    }

    private boolean repairWithoutModel(NavigationEvent event) {
        if (active == null || active.selectedOption == null || active.plan == null
                || event.type() != NavigationEvent.Type.NAVIGATION_DECISION_REQUIRED
                || event.message().startsWith("SUPPORT_MATERIAL_CHANGED")
                || event.message().equals("A hazardous block or fluid occupies the next path segment")) return false;
        var prior = active.plan.options().stream().filter(option -> option.optionId().equals(active.selectedOption)).findFirst().orElse(null);
        if (prior == null || prior.supportBlocksRequired() > 16 || prior.estimatedHealthLost() != 0) return false;
        // Numen's progress-bounded recovery principle: movement, not jumping or
        // rotating in place, renews the repair allowance. A hard limit also
        // bounds loops that move without ever reaching the destination.
        var here = player.position();
        if (active.repairAnchor == null || here.distanceToSqr(active.repairAnchor) >= 4) {
            active.repairAnchor = here;
            active.repairsWithoutProgress = 0;
        }
        if (active.repairsWithoutProgress >= 3 || active.totalRepairs >= 48) return false;
        active.repairsWithoutProgress++;
        active.totalRepairs++;
        repairFollow();
        return true;
    }

    private void completePlanning(
            UUID requestId,
            NavigationPlan plan,
            Throwable failure
    ) {
        requireServerThread();
        if (active == null || !active.intent.requestId().equals(requestId)
                || active.phase != Phase.PLANNING) {
            return;
        }
        active.planningFuture = null;
        if (failure != null) {
            Throwable cause = unwrap(failure);
            boolean retryFollow=active.intent.continuousFollow()
                    && cause instanceof AnytimeNavigationPlanner.NoRouteException
                    && ++active.noRouteFailures <= 3;
            active.phase = retryFollow ? Phase.FOLLOWING : Phase.FAILED;
            if(retryFollow) {
                active.lastDynamicCheckTick=player.tickCount+80;
                boolean alreadyWaiting=active.waitingForPath;active.waitingForPath=true;
                if(alreadyWaiting)return;
            }
            emit(new NavigationEvent(
                    retryFollow ? NavigationEvent.Type.NAVIGATION_FOLLOWING : NavigationEvent.Type.NAVIGATION_FAILED,
                    requestId,
                    (retryFollow ? "Follow remains active, but no route is currently available; waiting and retrying: " : "")
                            +(cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage()),
                    observedState(),
                    retryFollow ? List.of("say","cancel_navigation") : List.of("say", "request_navigation")
            ));
            return;
        }
        if (plan.plannedWorldRevision() != worldRevision) {
            active.phase = Phase.REPLAN_REQUIRED;
            emit(new NavigationEvent(
                    NavigationEvent.Type.NAVIGATION_DECISION_REQUIRED,
                    requestId,
                    "Planning completed against a stale world revision",
                    observedState(),
                    List.of("plan_navigation", "cancel_navigation", "say")
            ));
            return;
        }
        active.waitingForPath=false;
        active.noRouteFailures=0;
        active.plan = plan;
        active.phase = Phase.PLAN_READY;
        if(active.followRepair && active.selectedOption!=null && plan.partialDestination().isEmpty()) {
            var pace=active.selectedPace;
            var option=plan.options().stream().filter(o->o.feasibleNow() && o.estimatedHealthLost()==0 && o.supportBlocksRequired()<=16
                    && o.hazards().isEmpty() && o.supportedPaces().contains(pace))
                    .min(java.util.Comparator.comparingInt((RouteOption o) -> o.supportBlocksRequired()>0 ? 1 : 0)
                            .thenComparingDouble(RouteOption::estimatedSeconds)).orElse(plan.options().getFirst());
            if(option.feasibleNow() && option.estimatedHealthLost()==0 && option.supportBlocksRequired()<=16
                    && option.hazards().isEmpty() && option.supportedPaces().contains(pace)) {
                active.followRepair=false;
                chooseNavigation(requestId,option.optionId(),pace);
                return;
            }
        }
        active.followRepair=false;
        emit(new NavigationEvent(
                NavigationEvent.Type.NAVIGATION_PLAN_READY,
                requestId,
                "Choose one of the terrain- and resource-evaluated route options",
                Map.of(
                        "worldRevision", plan.plannedWorldRevision(),
                        "routeCount", plan.options().size(),
                        "destinationX", plan.destination().x(),
                        "destinationY", plan.destination().y(),
                        "destinationZ", plan.destination().z()
                ),
                List.of("choose_navigation", "cancel_navigation")
        ));
    }

    private void onFollowerEvent(NavigationEvent event) {
        if(active!=null && active.suppressFollowerEvents)return;
        if(active!=null && active.intent.requestId().equals(event.requestId())
                && event.type()==NavigationEvent.Type.NAVIGATION_COMPLETED && partialDestination().isEmpty()
                && active.destination!=null && active.destination.dynamic()) {
            try {active.destination=targetResolver.resolve(server,player,active.intent.target());}
            catch(NavigationTargetResolver.UnresolvedTargetException failure) {
                event=new NavigationEvent(NavigationEvent.Type.NAVIGATION_FAILED,event.requestId(),failure.getMessage(),event.observedState(),List.of("say","request_navigation"));
            }
        }
        if(active!=null && active.intent.requestId().equals(event.requestId()) && repairWithoutModel(event)) return;
        if(active!=null && active.intent.requestId().equals(event.requestId()) && active.intent.continuousFollow()) {
            if(event.type()==NavigationEvent.Type.NAVIGATION_COMPLETED && partialDestination().isEmpty()) {
                event=new NavigationEvent(NavigationEvent.Type.NAVIGATION_FOLLOWING,event.requestId(),
                        "Inside follow radius; waiting for target movement. Follow remains active until cancelled.",
                        event.observedState(),List.of("say","cancel_navigation"));
            }
        }
        if (active != null && active.intent.requestId().equals(event.requestId())
                && event.type() == NavigationEvent.Type.NAVIGATION_COMPLETED && partialDestination().isPresent()) {
            event = new NavigationEvent(NavigationEvent.Type.NAVIGATION_APPROACHED, event.requestId(),
                    "Reached only the nearest evaluated reachable point. The original destination was NOT reached.",
                    event.observedState(), List.of("say", "request_navigation"));
        }
        if (active != null && active.intent.requestId().equals(event.requestId())) {
            active.phase = switch (event.type()) {
                case NAVIGATION_STARTED, NAVIGATION_PROGRESS, NAVIGATION_REPLANNED -> Phase.EXECUTING;
                case NAVIGATION_DECISION_REQUIRED -> Phase.REPLAN_REQUIRED;
                case NAVIGATION_COMPLETED -> Phase.COMPLETED;
                case NAVIGATION_FOLLOWING -> Phase.FOLLOWING;
                case NAVIGATION_APPROACHED -> Phase.APPROACHED;
                case NAVIGATION_FAILED -> Phase.FAILED;
                case NAVIGATION_CANCELLED -> Phase.CANCELLED;
                default -> active.phase;
            };
        }
        emit(event);
    }

    private String arrivalProblem(UUID requestId) {
        ActiveNavigation current = active;
        if (current == null || !current.intent.requestId().equals(requestId)
                || current.destination == null) {
            return "The active destination is no longer available";
        }
        NavigationPlan.ResolvedDestination destination = partialDestination().orElse(current.destination);
        if (destination.dynamic()) {
            try {
                destination = targetResolver.resolve(server, player, current.intent.target());
            } catch (NavigationTargetResolver.UnresolvedTargetException failure) {
                return failure.getMessage();
            }
        }
        String currentDimension = player.level().dimension().identifier().toString();
        if (!currentDimension.equals(destination.dimension())) {
            return "The destination is in a different dimension";
        }
        double distanceSquared = square(player.getX() - destination.x())
                + square(player.getY() - destination.y())
                + square(player.getZ() - destination.z());
        if (distanceSquared > square(destination.acceptanceRadius())) {
            if(destination.dynamic() && (square(destination.x()-current.destination.x())
                    +square(destination.y()-current.destination.y())+square(destination.z()-current.destination.z()))>.0001)
                return "The moving destination left the active route corridor";
            return "The body is outside the destination's requested arrival radius";
        }
        return null;
    }

    private void emit(NavigationEvent event) {
        lastEvent = event;
        events.accept(event);
    }

    private ActiveNavigation requireActive(UUID requestId) {
        if (active == null || !active.intent.requestId().equals(requestId)) {
            throw new ProtocolException("Unknown or inactive navigation request");
        }
        return active;
    }

    private void requireServerThread() {
        if (!server.isSameThread()) {
            throw new IllegalStateException("Navigation coordination must run on the server thread");
        }
    }

    private Map<String, Object> observedState() {
        return Map.of(
                "x", player.getX(),
                "y", player.getY(),
                "z", player.getZ(),
                "heading", NavigationFollower.minecraftYawToHeading(player.getYRot()),
                "health", player.getHealth(),
                "food", player.getFoodData().getFoodLevel(),
                "worldRevision", worldRevision
        );
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable cursor = failure;
        while (cursor.getCause() != null
                && (cursor instanceof java.util.concurrent.CompletionException
                || cursor instanceof java.util.concurrent.ExecutionException)) {
            cursor = cursor.getCause();
        }
        return cursor;
    }

    private static double square(double value) {
        return value * value;
    }

    @Override
    public void close() {
        requireServerThread();
        if (active != null && active.planningFuture != null) {
            active.planningFuture.cancel(true);
        }
        follower.cancel("Navigation runtime stopped");
        planningExecutor.close();
    }

    public enum Phase {
        IDLE,
        ACKNOWLEDGEMENT_REQUIRED,
        ACKNOWLEDGED,
        PLANNING,
        PLAN_READY,
        EXECUTING,
        FOLLOWING,
        REPLAN_REQUIRED,
        COMPLETED,
        APPROACHED,
        FAILED,
        CANCELLED;

        public boolean terminal() {
            return this == COMPLETED || this == APPROACHED || this == FAILED || this == CANCELLED;
        }

        public List<String> validActions() {
            return switch (this) {
                case IDLE, COMPLETED, APPROACHED, FAILED, CANCELLED -> List.of("request_navigation", "say");
                case ACKNOWLEDGEMENT_REQUIRED -> List.of("say", "cancel_navigation");
                case ACKNOWLEDGED, REPLAN_REQUIRED ->
                        List.of("plan_navigation", "cancel_navigation", "say");
                case PLANNING, EXECUTING, FOLLOWING -> List.of("cancel_navigation", "say");
                case PLAN_READY -> List.of("choose_navigation", "cancel_navigation", "say");
            };
        }
    }

    public record Status(
            UUID requestId,
            Phase phase,
            long worldRevision,
            String selectedOption,
            TravelPace selectedPace,
            NavigationPlan.ResolvedDestination destination,
            String lastEventMessage,
            List<String> validActions,
            List<RouteOption> routeOptions
    ) {
        public Status {
            validActions = List.copyOf(validActions);
            routeOptions = List.copyOf(routeOptions);
        }
    }

    private static final class ActiveNavigation {
        private NavigationSnapshotBuilder.Capture capture;
        private boolean waitingForPath;
        private final NavigationIntent intent;
        private Phase phase;
        private NavigationPlan.ResolvedDestination destination;
        private NavigationPlan plan;
        private CompletableFuture<NavigationPlan> planningFuture;
        private long planningRevision;
        private String selectedOption;
        private TravelPace selectedPace;
        private int lastDynamicCheckTick;
        private net.minecraft.world.phys.Vec3 stationaryAnchor;
        private int stationarySince;
        private int noRouteFailures;
        private boolean followRepair;
        private boolean suppressFollowerEvents;
        private net.minecraft.world.phys.Vec3 repairAnchor;
        private int repairsWithoutProgress, totalRepairs;

        private ActiveNavigation(NavigationIntent intent, Phase phase) {
            this.intent = intent;
            this.phase = phase;
        }
    }

    public static final class ProtocolException extends RuntimeException {
        public ProtocolException(String message) {
            super(message);
        }
    }
}
