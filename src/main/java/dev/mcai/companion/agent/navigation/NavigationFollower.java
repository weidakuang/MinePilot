package dev.mcai.companion.agent.navigation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import dev.mcai.companion.agent.body.AgentControlFrame;
import dev.mcai.companion.agent.body.MinePilotServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;

/** Converts a selected route into continuous vanilla player input. */
public final class NavigationFollower {
    private static final double STEP_REACHED_DISTANCE_SQUARED = 0.14;
    private static final int STUCK_SAMPLE_TICKS = 10;
    private static final int STUCK_LIMIT_SAMPLES = 3;
    private static final int ACTION_RETRY_TICKS = 4;
    private static final int MAX_ACTION_ATTEMPTS = 3;
    private static final int REQUIRED_STABLE_TICKS = 20;
    private static final int MAX_SETTLING_TICKS = 100;

    private final MinePilotServerPlayer player;
    private final Consumer<NavigationEvent> events;
    private final Function<UUID, String> completionProblem;
    private final Consumer<String> corridorInvalidated;
    private ActiveRoute active;

    public NavigationFollower(
            MinePilotServerPlayer player,
            Consumer<NavigationEvent> events,
            Function<UUID, String> completionProblem,
            Consumer<String> corridorInvalidated
    ) {
        this.player = Objects.requireNonNull(player, "player");
        this.events = Objects.requireNonNull(events, "events");
        this.completionProblem = Objects.requireNonNull(
                completionProblem, "completionProblem");
        this.corridorInvalidated = Objects.requireNonNull(corridorInvalidated, "corridorInvalidated");
    }

    public boolean isActive() {
        return active != null;
    }

    public void start(
            NavigationPlan plan,
            RouteOption option,
            TravelPace pace,
            OptionalDouble arrivalHeading
    ) {
        if (!option.feasibleNow()) {
            throw new IllegalArgumentException("Cannot execute an infeasible route");
        }
        TravelPace effectivePace = pace == TravelPace.AUTO ? option.suggestedPace() : pace;
        if (!option.supportedPaces().contains(effectivePace)) {
            throw new IllegalArgumentException(
                    "Selected pace is not supported by route " + option.optionId());
        }
        active = new ActiveRoute(
                plan.requestId(), plan, option, effectivePace, arrivalHeading,
                0, player.tickCount, player.getX(), player.getY(), player.getZ(),
                0, player.getX(), player.getY(), player.getZ(), 0.0);
        events.accept(event(NavigationEvent.Type.NAVIGATION_STARTED,
                "Selected route " + option.optionId() + " started"));
    }

    /** Repair only a verified level, dry corridor. Never spend new materials or add a jump. */
    public boolean retargetLevel(NavigationPlan.ResolvedDestination destination) {
        if (active == null || !player.onGround() || active.option.supportBlocksRequired() != 0
                || active.option.estimatedHealthLost() != 0 || !active.option.hazards().isEmpty()
                || active.stepIndex < active.steps.size()
                    && active.steps.get(active.stepIndex).action() != RouteOption.Action.WALK) return false;
        double dx=destination.x()-player.getX(), dz=destination.z()-player.getZ();
        double length=Math.hypot(dx,dz);
        if(length>32 || Math.abs(destination.y()-player.getY())>.05) return false;
        var corridor=new AABB(player.position(),new Vec3(destination.x(),destination.y(),destination.z())).inflate(3);
        if(!player.level().getEntities(player,corridor,e->e instanceof net.minecraft.world.entity.monster.Enemy && e.isAlive()).isEmpty())return false;
        var points=new java.util.ArrayList<RouteOption.PathStep>();
        double travel=Math.max(0, length-Math.min(1.0,destination.acceptanceRadius()*.5));
        int samples=Math.max(1,(int)Math.ceil(travel*4));
        for(int i=1;i<=samples;i++) {
            double t=length<.001?0:travel/length*i/samples;
            var point=new RouteOption.PathStep(player.getX()+dx*t,player.getY(),player.getZ()+dz*t,
                    RouteOption.Action.WALK,active.pace,30);
            if(liveStepProblem(point)!=null) return false;
            var box=new AABB(point.x()-.3,point.y()+.001,point.z()-.3,point.x()+.3,point.y()+1.8,point.z()+.3);
            if(player.level().getBlockCollisions(player,box).iterator().hasNext())return false;
            if(i%4==0 || i==samples)points.add(point);
        }
        active.steps=java.util.List.copyOf(points); active.stepIndex=0;
        active.progressStep=-1; active.stableTicks=0; active.settlingTicks=0;
        // Keep input, momentum, request identity, distance and elapsed time.
        return true;
    }

    public double distanceToRouteEnd() {
        if(active==null || active.steps.isEmpty())return 0;
        var end=active.steps.getLast();
        return player.position().distanceTo(new Vec3(end.x(),end.y(),end.z()));
    }

    public void cancel(String reason) {
        if (active == null) {
            return;
        }
        UUID requestId = active.requestId;
        player.stopControlling();
        brakeForUnsafeCorridor();
        active = null;
        events.accept(new NavigationEvent(
                NavigationEvent.Type.NAVIGATION_CANCELLED,
                requestId,
                reason,
                observedState(),
                java.util.List.of()
        ));
    }

    public void requestReplan(String reason) {
        if (active == null) {
            return;
        }
        requireDecision(active, reason);
    }

    /** Recheck before the body consumes the previous tick's control frame. */
    public void beforePhysicsTick() {
        if(active!=null && !materialsAvailable(active)) {
            requireDecision(active,"SUPPORT_MATERIAL_CHANGED: The declared material quantities are missing or protected; no substitution is allowed");
            return;
        }
        if (active != null && active.stepIndex < active.steps.size()) {
            String problem = liveStepProblem(active.steps.get(active.stepIndex));
            if (problem != null) {
                corridorInvalidated.accept(problem);
                brakeForUnsafeCorridor();
            }
        }
    }

    /** One counter-input frame reduces momentum through ordinary player acceleration. */
    private void brakeForUnsafeCorridor() {
        Vec3 velocity = player.getDeltaMovement();
        double speed = Math.hypot(velocity.x, velocity.z);
        if (!player.onGround() || speed < 0.005) {
            return;
        }
        double yaw = Math.toRadians(player.getYRot());
        float backward = (float) -((-velocity.x * Math.sin(yaw)
                + velocity.z * Math.cos(yaw)) / speed);
        float strafe = (float) -((velocity.x * Math.cos(yaw)
                + velocity.z * Math.sin(yaw)) / speed);
        player.applyControlFrame(new AgentControlFrame(
                player.getYRot(), player.getXRot(), backward, strafe,
                false, false, false));
    }

    public void tick() {
        ActiveRoute route = active;
        if (route == null || !player.isAlive()) {
            player.stopControlling();
            if (route != null) {
                fail("Agent body is no longer alive");
            }
            return;
        }

        double movedX = player.getX() - route.lastX;
        double movedY = player.getY() - route.lastY;
        double movedZ = player.getZ() - route.lastZ;
        route.horizontalMovementSquared = movedX * movedX + movedZ * movedZ;
        route.verticalMovement = Math.abs(movedY);
        route.distanceTravelled += Math.sqrt(route.horizontalMovementSquared + movedY * movedY);
        route.lastX = player.getX();
        route.lastY = player.getY();
        route.lastZ = player.getZ();

        if (route.stepIndex >= route.steps.size()) {
            finish(route);
            return;
        }

        RouteOption.PathStep step = route.steps.get(route.stepIndex);
        double dx = step.x() - player.getX();
        double dz = step.z() - player.getZ();
        double horizontalSquared = dx * dx + dz * dz;
        double dy = step.y() - player.getY();
        boolean lastStep = route.stepIndex == route.steps.size() - 1;
        double waypointTolerance = step.action() == RouteOption.Action.OPEN_DOOR ? 0.0144 : STEP_REACHED_DISTANCE_SQUARED;
        boolean waypointReached=(horizontalSquared <= waypointTolerance || passedWalkWaypoint(route,step,dx,dz))
                && Math.abs(dy)<.22 && (player.onGround() || player.onClimbable() || player.isInWater());
        String finalProblem=waypointReached && lastStep ? completionProblem.apply(route.requestId) : null;
        if(finalProblem!=null && finalProblem.startsWith("The moving destination left")) {
            requireDecision(route,finalProblem);return;
        }
        if(waypointReached && finalProblem==null) {
            route.stepIndex++;
            route.actionAttempts = 0;
            if (route.stepIndex >= route.steps.size()) {
                finish(route);
                return;
            }
            step = route.steps.get(route.stepIndex);
            dx = step.x() - player.getX();
            dz = step.z() - player.getZ();
            dy = step.y() - player.getY();
            horizontalSquared = dx * dx + dz * dz;
        }

        double remaining = Math.sqrt(horizontalSquared + dy * dy);
        if (route.progressStep != route.stepIndex) {
            route.progressStep = route.stepIndex;
            route.bestRemaining = remaining;
            route.lastApproachTick = player.tickCount;
        } else if (remaining < route.bestRemaining - .05) {
            route.bestRemaining = remaining;
            route.lastApproachTick = player.tickCount;
        } else if (player.tickCount - route.lastApproachTick > 100) {
            requireDecision(route, "NO_WAYPOINT_PROGRESS: The body cannot approach the next waypoint; jumping or turning in place is not progress");
            return;
        }

        String stepProblem = liveStepProblem(step);
        if (stepProblem != null) {
            corridorInvalidated.accept(stepProblem);
            brakeForUnsafeCorridor();
            return;
        }
        if (!prepareStep(route, step)) {
            return;
        }

        if (step.action() == RouteOption.Action.GAP_JUMP && prepareTakeoff(route, step)) return;

        float targetYaw = horizontalSquared < 0.0025 ? player.getYRot()
                : (float) Math.toDegrees(Math.atan2(-dx, dz));
        if (step.action() == RouteOption.Action.GAP_JUMP) {
            targetYaw = (float) Math.toDegrees(Math.atan2(-route.gapDirectionX, route.gapDirectionZ));
        }
        float yawError = Math.abs(Mth.wrapDegrees(targetYaw - player.getYRot()));
        boolean edgeAction = step.action() == RouteOption.Action.SNEAK_EDGE
                || step.action() == RouteOption.Action.PLACE_SUPPORT;
        TravelPace stepPace = effectivePace(route.pace, step);
        boolean climbing = step.action() == RouteOption.Action.CLIMB;
        boolean gapJump = step.action() == RouteOption.Action.GAP_JUMP;
        boolean finalApproach = !gapJump && !climbing && route.stepIndex == route.steps.size() - 1
                && horizontalSquared < 2.25;
        if (finalApproach && stepPace != TravelPace.SNEAK) {
            stepPace = TravelPace.WALK;
        }
        boolean jump = (climbing && dy > 0.1)
                || (step.action() == RouteOption.Action.JUMP && route.jumpedStep != route.stepIndex);
        boolean sprint = (stepPace == TravelPace.SPRINT
                || stepPace == TravelPace.SPRINT_JUMP)
                && !edgeAction && yawError < 45.0F;
        float forward = yawError > step.yawTolerance() || horizontalSquared < 0.0025 ? 0.0F : 1.0F;
        if (climbing) forward *= (float) Math.min(1.0, Math.sqrt(horizontalSquared) * 3.0);
        if (horizontalSquared < 0.36 && !climbing && (finalApproach || step.action() != RouteOption.Action.WALK)) {
            double toward = horizontalSquared < 0.0001 ? 0.0
                    : (player.getDeltaMovement().x * dx + player.getDeltaMovement().z * dz)
                    / Math.sqrt(horizontalSquared);
            if (toward > Math.max(0.035, Math.sqrt(horizontalSquared) * 0.3)) forward = -0.65F;
        }
        float strafe = 0;
        if (gapJump) {
            // Keep facing the jump while using ordinary backward/side input to
            // brake over the landing platform. Do not turn around or jump again.
            double distance = Math.sqrt(horizontalSquared);
            double desiredSpeed = Math.min(.34, distance * .35);
            double desiredX = distance < .001 ? 0 : dx / distance * desiredSpeed;
            double desiredZ = distance < .001 ? 0 : dz / distance * desiredSpeed;
            double acceleration = player.onGround() ? .1 : .02;
            double inputX = (desiredX - player.getDeltaMovement().x) / acceleration;
            double inputZ = (desiredZ - player.getDeltaMovement().z) / acceleration;
            double scale = Math.max(1, Math.hypot(inputX, inputZ));
            inputX /= scale; inputZ /= scale;
            double angle = Math.toRadians(player.getYRot());
            forward = (float) (-Math.sin(angle) * inputX + Math.cos(angle) * inputZ);
            strafe = (float) (Math.cos(angle) * inputX + Math.sin(angle) * inputZ);
            jump = false;
            sprint = forward >= .8F;
        }
        if (finalApproach) {
            forward *= (float) Math.min(1.0, Math.sqrt(horizontalSquared) * 2.0);
        }
        if (stepPace == TravelPace.SNEAK) {
            sprint = false;
        }
        if (jump && step.action() == RouteOption.Action.JUMP && yawError < 20 && player.onGround()) {
            route.jumpedStep = route.stepIndex;
        }

        player.applyControlFrame(new AgentControlFrame(
                targetYaw,
                pitchToward(step),
                forward,
                strafe,
                jump && yawError < 20.0F && (player.onGround() || player.isInWater() || player.onClimbable()),
                sprint,
                edgeAction || stepPace == TravelPace.SNEAK
        ));

        if (player.tickCount - route.lastProgressSampleTick >= STUCK_SAMPLE_TICKS) {
            double progress = Math.sqrt(
                    square(player.getX() - route.sampleX)
                            + square(player.getY() - route.sampleY)
                            + square(player.getZ() - route.sampleZ));
            route.stuckSamples = progress < 0.12 ? route.stuckSamples + 1 : 0;
            route.lastProgressSampleTick = player.tickCount;
            route.sampleX = player.getX();
            route.sampleY = player.getY();
            route.sampleZ = player.getZ();
            if (route.stuckSamples >= STUCK_LIMIT_SAMPLES) {
                player.stopControlling();
                events.accept(new NavigationEvent(
                        NavigationEvent.Type.NAVIGATION_DECISION_REQUIRED,
                        route.requestId,
                        "The active route is blocked and requires repair",
                        observedState(),
                        java.util.List.of(
                                "plan_navigation", "cancel_navigation", "say")
                ));
                active = null;
            }
        }
    }

    /** A small overshoot along a straight walk must not make the body turn back. */
    private boolean passedWalkWaypoint(ActiveRoute route, RouteOption.PathStep step, double dx, double dz) {
        if (step.action() != RouteOption.Action.WALK || route.stepIndex + 1 >= route.steps.size()) return false;
        var next = route.steps.get(route.stepIndex + 1);
        if (next.action() != RouteOption.Action.WALK || next.y() != step.y()) return false;
        double nx = next.x() - step.x(), nz = next.z() - step.z();
        double length = Math.hypot(nx, nz);
        return length > .01 && dx * nx + dz * nz < 0
                && Math.abs(dx * nz - dz * nx) / length < .25 && dx * dx + dz * dz < .64;
    }

    /** Accelerate on real support before a long jump; never assign position or velocity. */
    private boolean prepareTakeoff(ActiveRoute route, RouteOption.PathStep step) {
        if (route.gapStepIndex != route.stepIndex) {
            route.gapStepIndex = route.stepIndex;
            route.gapLaunched = false;
            route.gapPreparationTicks = 0;
            double sourceX = route.stepIndex == 0 ? player.getX() : route.steps.get(route.stepIndex - 1).x();
            double sourceZ = route.stepIndex == 0 ? player.getZ() : route.steps.get(route.stepIndex - 1).z();
            route.gapCenterX = Math.floor(sourceX) + .5;
            route.gapCenterZ = Math.floor(sourceZ) + .5;
            double dx = step.x() - route.gapCenterX;
            double dz = step.z() - route.gapCenterZ;
            double length = Math.hypot(dx, dz);
            route.gapDirectionX = dx / length;
            route.gapDirectionZ = dz / length;
            route.gapBackUp = true;
        }
        if (route.gapLaunched) return false;
        if (++route.gapPreparationTicks > 80 || !player.onGround()) {
            requireDecision(route, "The running takeoff could not be prepared on stable support");
            return true;
        }
        double ux = route.gapDirectionX, uz = route.gapDirectionZ;
        double along = (player.getX() - route.gapCenterX) * ux + (player.getZ() - route.gapCenterZ) * uz;
        float yaw = (float) Math.toDegrees(Math.atan2(-ux, uz));
        double yawError = Math.abs(Mth.wrapDegrees(yaw - player.getYRot()));
        if (route.gapBackUp) {
            double dx = route.gapCenterX - .12 * ux - player.getX();
            double dz = route.gapCenterZ - .12 * uz - player.getZ();
            double distance = Math.hypot(dx, dz);
            if (distance < .08) route.gapBackUp = false;
            double yawRadians = Math.toRadians(player.getYRot());
            float forward = distance < .01 ? 0 : (float) ((-Math.sin(yawRadians) * dx + Math.cos(yawRadians) * dz) / distance * .6);
            float strafe = distance < .01 ? 0 : (float) ((Math.cos(yawRadians) * dx + Math.sin(yawRadians) * dz) / distance * .6);
            player.applyControlFrame(new AgentControlFrame(yaw, 0, yawError < 10 ? forward : 0,
                    yawError < 10 ? strafe : 0, false, false, false));
            return true;
        }
        double edgeDistance = .5 / Math.max(Math.abs(ux), Math.abs(uz));
        double toward = player.getDeltaMovement().x * ux + player.getDeltaMovement().z * uz;
        boolean jump = yawError < 10 && along + toward >= edgeDistance + .03;
        if (jump) route.gapLaunched = true;
        player.applyControlFrame(new AgentControlFrame(yaw, 0, yawError < 10 ? 1 : 0, 0, jump, true, false));
        return true;
    }

    private String liveStepProblem(RouteOption.PathStep step) {
        BlockPos feet = BlockPos.containing(step.x(), step.y(), step.z());
        BlockPos head = feet.above();
        BlockPos floor = feet.below();
        if (!player.level().isLoaded(feet) || !player.level().isLoaded(head)
                || !player.level().isLoaded(floor)
                || !player.level().getWorldBorder().isWithinBounds(feet)) {
            return "The next path segment is no longer loaded or inside the world border";
        }
        BlockState feetState = player.level().getBlockState(feet);
        BlockState headState = player.level().getBlockState(head);
        BlockState floorState = player.level().getBlockState(floor);
        if (NavigationTerrain.damaging(feetState) || NavigationTerrain.damaging(headState)
                || NavigationTerrain.damaging(floorState)) {
            return "A hazardous block or fluid occupies the next path segment";
        }
        boolean swimming = step.action() == RouteOption.Action.SWIM;
        if ((!feetState.getFluidState().isEmpty() || !headState.getFluidState().isEmpty())
                && !swimming) {
            return "An unplanned fluid occupies the next path segment";
        }
        if (swimming && !feetState.getFluidState().is(FluidTags.WATER)) {
            return "The planned swimming segment no longer contains water";
        }
        boolean climbing = step.action() == RouteOption.Action.CLIMB;
        boolean feetClimbable = feetState.is(BlockTags.CLIMBABLE);
        boolean headClimbable = headState.is(BlockTags.CLIMBABLE);
        boolean door = step.action() == RouteOption.Action.OPEN_DOOR
                || step.action() == RouteOption.Action.GAP_JUMP;
        boolean feetDoor = door && feetState.hasProperty(BlockStateProperties.OPEN);
        boolean headDoor = door && headState.hasProperty(BlockStateProperties.OPEN);
        var bodyBox=new AABB(step.x()-.299,step.y()+.001,step.z()-.299,step.x()+.299,step.y()+1.799,step.z()+.299);
        var contact=new AABB(step.x()-.299,step.y()-.035,step.z()-.299,step.x()+.299,step.y()+.001,step.z()+.299);
        boolean supported=false;
        for(int y=(int)Math.floor(step.y())-2;y<=(int)Math.floor(step.y()+1.8);y++) {
            var pos=new BlockPos(feet.getX(),y,feet.getZ());
            if(!player.level().isLoaded(pos))return "The next body volume is no longer loaded";
            var state=player.level().getBlockState(pos);
            boolean canOpen=door && state.hasProperty(BlockStateProperties.OPEN);
            if(state.is(BlockTags.CLIMBABLE))continue;
            for(var local:state.getCollisionShape(player.level(),pos).toAabbs()) {
                var box=local.move(pos);
                if(box.intersects(contact) && box.maxY<=step.y()+.002)supported=true;
                if(!canOpen && box.intersects(bodyBox))return "A block obstructed the next body volume";
            }
        }
        if (!swimming && step.action() != RouteOption.Action.PLACE_SUPPORT
                && !feetClimbable && !(climbing && floorState.is(BlockTags.CLIMBABLE))
                && !floorState.is(Blocks.SCAFFOLDING) && !supported) {
            return "The next path segment lost its supporting surface";
        }
        return null;
    }

    private boolean prepareStep(ActiveRoute route, RouteOption.PathStep step) {
        if (step.action() == RouteOption.Action.PLACE_SUPPORT) {
            BlockPos floor = BlockPos.containing(step.x(), step.y() - 1.0, step.z());
            if (!player.level().getBlockState(floor).getCollisionShape(
                    player.level(), floor).isEmpty()) {
                route.actionAttempts = 0;
                return true;
            }
            if (player.tickCount - route.lastActionTick < ACTION_RETRY_TICKS) {
                return false;
            }
            route.lastActionTick = player.tickCount;
            if (tryPlaceSupport(floor)) {
                return false;
            }
            route.actionAttempts++;
            if (route.actionAttempts >= MAX_ACTION_ATTEMPTS) {
                requireDecision(route, "Unable to place the planned support block");
            }
            return false;
        }

        if (step.action() == RouteOption.Action.OPEN_DOOR || step.action() == RouteOption.Action.GAP_JUMP) {
            BlockPos door = BlockPos.containing(step.x(), step.y(), step.z());
            BlockState state = player.level().getBlockState(door);
            if (!state.hasProperty(BlockStateProperties.OPEN)
                    || state.getValue(BlockStateProperties.OPEN)) {
                route.actionAttempts = 0;
                return true;
            }
            double reachSquared = player.distanceToSqr(Vec3.atCenterOf(door));
            if (reachSquared > 16.0) {
                return true;
            }
            Vec3 hitPoint = Vec3.atCenterOf(door);
            BlockHitResult visible = player.level().clip(new ClipContext(player.getEyePosition(), hitPoint,
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            boolean sameDoor = visible.getBlockPos().equals(door)
                    || (state.getBlock() instanceof net.minecraft.world.level.block.DoorBlock
                    && visible.getBlockPos().equals(door.above())
                    && player.level().getBlockState(door.above()).getBlock() == state.getBlock());
            if (visible.getType() != HitResult.Type.BLOCK || !sameDoor) {
                if (step.action() == RouteOption.Action.GAP_JUMP) {
                    requireDecision(route, "The landing door cannot be opened from the takeoff platform");
                    return false;
                }
                return true;
            }
            Vec3 look = visible.getLocation().subtract(player.getEyePosition());
            float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
            float pitch = (float) -Math.toDegrees(Math.atan2(look.y, Math.hypot(look.x, look.z)));
            player.applyControlFrame(new AgentControlFrame(yaw, pitch, 0, 0, false, false, false));
            if (Math.abs(Mth.wrapDegrees(yaw - player.getYRot())) > 10
                    || Math.abs(pitch - player.getXRot()) > 10) return false;
            if (player.tickCount - route.lastActionTick < ACTION_RETRY_TICKS) {
                return false;
            }
            route.lastActionTick = player.tickCount;
            route.actionAttempts++;
            InteractionResult result = player.gameMode.useItemOn(
                    player,
                    player.level(),
                    player.getMainHandItem(),
                    InteractionHand.MAIN_HAND,
                    visible
            );
            if (!result.consumesAction() && route.actionAttempts >= MAX_ACTION_ATTEMPTS) {
                requireDecision(route, "Unable to open the planned door");
            }
            return false;
        }
        return true;
    }

    /** True means aiming or a verified use; route progress still requires the actual floor next tick. */
    private boolean tryPlaceSupport(BlockPos target) {
        if (!materialsAvailable(active)) return false;
        int previousSlot=player.getInventory().getSelectedSlot(),supportSlot=findSupportBlock();
        if(supportSlot<0 || player.gameMode.getGameModeForPlayer()!=net.minecraft.world.level.GameType.SURVIVAL)return false;
        boolean equipped=false;
        try {
            dev.mcai.companion.agent.placement.HandController.equipSlot(player,supportSlot,InteractionHand.MAIN_HAND);equipped=true;
            var candidates=dev.mcai.companion.agent.placement.PlacementGeometry.aims(player,target,player.getMainHandItem(),InteractionHand.MAIN_HAND,java.util.Map.of(),player.position());
            if(candidates.isEmpty())return false;
            var aim=candidates.getFirst();
            player.applyControlFrame(new AgentControlFrame(aim.yaw(),aim.pitch(),0,0,false,false,true));
            if(!player.isShiftKeyDown() || !dev.mcai.companion.agent.placement.PlacementGeometry.clearActualRay(player,aim))return true;
            if(!player.level().mayInteract(player,target) || player.blockActionRestricted(player.level(),target,player.gameMode.getGameModeForPlayer()) || player.level().getServer().isUnderSpawnProtection(player.level(),target,player))return false;
            var stack=player.getMainHandItem();int before=stack.getCount();
            player.swing(InteractionHand.MAIN_HAND);
            player.gameMode.useItemOn(player,player.level(),stack,InteractionHand.MAIN_HAND,aim.hit());
            if(before-player.getMainHandItem().getCount()!=1 || !player.level().getBlockState(target).equals(aim.state()) || !aim.state().isCollisionShapeFullBlock(player.level(),target))return false;
            active.usedMaterials.merge(active.materialBeingPlaced,1,Integer::sum);
            return true;
        } catch(RuntimeException rejected) {
            return false;
        } finally {
            // Restore the prior real hand. Storage swaps conserve all remaining items.
            if(equipped && supportSlot>=9)dev.mcai.companion.agent.placement.HandController.equipSlot(player,supportSlot,InteractionHand.MAIN_HAND);
            else if(equipped)player.connection.handleSetCarriedItem(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(previousSlot));
        }
    }

    private int findSupportBlock() {
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && player.inventoryLedger != null && player.inventoryLedger.expendable(stack)
                    && active.option.supportMaterials().stream().anyMatch(m -> m.entryId().equals(player.inventoryLedger.key(stack))
                    && active.usedMaterials.getOrDefault(m.entryId(),0)<m.count()) && stack.getItem() instanceof BlockItem blockItem
                    && blockItem.getBlock().defaultBlockState().isCollisionShapeFullBlock(
                            player.level(), player.blockPosition())) {
                active.materialBeingPlaced=player.inventoryLedger.key(stack); return slot;
            }
        }
        return -1;
    }

    private boolean materialsAvailable(ActiveRoute route) {
        if(route==null)return false;
        for(var material:route.option.supportMaterials()) {
            int available=0;
            for(int slot=0;slot<36;slot++) {
                var s=player.getInventory().getItem(slot);
                if(!s.isEmpty() && player.inventoryLedger!=null && player.inventoryLedger.expendable(s)
                        && player.inventoryLedger.key(s).equals(material.entryId()))available+=s.getCount();
            }
            if(available<material.count()-route.usedMaterials.getOrDefault(material.entryId(),0))return false;
        }
        return true;
    }

    private void requireDecision(ActiveRoute route, String reason) {
        player.stopControlling();
        events.accept(new NavigationEvent(
                NavigationEvent.Type.NAVIGATION_DECISION_REQUIRED,
                route.requestId,
                reason,
                observedStateWithRoute(route),
                java.util.List.of("plan_navigation", "cancel_navigation", "say")
        ));
        active = null;
    }

    private static TravelPace effectivePace(
            TravelPace selected,
            RouteOption.PathStep step
    ) {
        return switch (step.action()) {
            case GAP_JUMP -> TravelPace.SPRINT_JUMP;
            case CLIMB -> TravelPace.WALK;
            case PLACE_SUPPORT, SNEAK_EDGE -> TravelPace.SNEAK;
            case OPEN_DOOR, STEP_DOWN, DROP -> TravelPace.WALK;
            default -> selected;
        };
    }

    private float pitchToward(RouteOption.PathStep step) {
        // Locomotion looks along the route at eye height. Interaction aiming
        // still uses the exact face elsewhere; nearby feet must not drag the gaze down.
        double horizontal = Math.hypot(step.x() - player.getX(), step.z() - player.getZ());
        return Mth.clamp((float) -Math.toDegrees(Math.atan2(step.y() - player.getY(),
                Math.max(3.0, horizontal))), -25.0F, 15.0F);
    }

    private void finish(ActiveRoute route) {
        player.stopControlling();
        if (++route.settlingTicks > MAX_SETTLING_TICKS) {
            requireDecision(route, "The body could not settle safely at the destination");
            return;
        }
        String problem = completionProblem.apply(route.requestId);
        if (problem != null) {
            requireDecision(route, problem);
            return;
        }
        if (route.arrivalHeading.isPresent()) {
            float finalYaw = headingToMinecraftYaw(route.arrivalHeading.getAsDouble());
            float yawError = Math.abs(Mth.wrapDegrees(finalYaw - player.getYRot()));
            if (yawError > 2.0F) {
                route.stableTicks = 0;
                player.applyControlFrame(new AgentControlFrame(
                        finalYaw, player.getXRot(), 0.0F, 0.0F,
                        false, false, false));
                return;
            }
        }
        boolean stableMedium = player.onGround()
                || (player.isInWater() && !player.isUnderWater()
                && Math.abs(player.getDeltaMovement().y) < 0.02);
        boolean stable = stableMedium
                && player.getDeltaMovement().horizontalDistanceSqr() < 0.0004
                && route.horizontalMovementSquared < 0.0004 && route.verticalMovement < 0.01;
        route.stableTicks = stable ? route.stableTicks + 1 : 0;
        if (route.stableTicks < REQUIRED_STABLE_TICKS) {
            return;
        }
        active = null;
        events.accept(new NavigationEvent(
                NavigationEvent.Type.NAVIGATION_COMPLETED,
                route.requestId,
                "Destination reached",
                observedStateWithRoute(route),
                java.util.List.of("say", "request_navigation")
        ));
    }

    private void fail(String reason) {
        ActiveRoute route = active;
        player.stopControlling();
        active = null;
        events.accept(new NavigationEvent(
                NavigationEvent.Type.NAVIGATION_FAILED,
                route.requestId,
                reason,
                observedStateWithRoute(route),
                java.util.List.of("say", "request_navigation")
        ));
    }

    private NavigationEvent event(NavigationEvent.Type type, String message) {
        return new NavigationEvent(
                type,
                active.requestId,
                message,
                observedState(),
                java.util.List.of()
        );
    }

    private Map<String, Object> observedState() {
        Map<String, Object> values = new HashMap<>();
        values.put("x", player.getX());
        values.put("y", player.getY());
        values.put("z", player.getZ());
        values.put("heading", minecraftYawToHeading(player.getYRot()));
        values.put("health", player.getHealth());
        values.put("absorption", player.getAbsorptionAmount());
        values.put("food", player.getFoodData().getFoodLevel());
        values.put("saturation", player.getFoodData().getSaturationLevel());
        values.put("dimension", player.level().dimension().identifier().toString());
        return values;
    }

    private Map<String, Object> observedStateWithRoute(ActiveRoute route) {
        Map<String, Object> values = new HashMap<>(observedState());
        values.put("elapsedSeconds", (player.tickCount - route.startedTick) / 20.0);
        values.put("distanceTravelled", route.distanceTravelled);
        values.put("selectedOption", route.option.optionId());
        values.put("selectedPace", route.pace.name().toLowerCase(java.util.Locale.ROOT));
        return values;
    }

    public static float headingToMinecraftYaw(double heading) {
        return Mth.wrapDegrees((float) (heading + 180.0));
    }

    public static double minecraftYawToHeading(float yaw) {
        return Mth.positiveModulo(yaw - 180.0, 360.0);
    }

    private static double square(double value) {
        return value * value;
    }

    private static final class ActiveRoute {
        private final java.util.Map<String,Integer> usedMaterials = new java.util.HashMap<>();
        private String materialBeingPlaced;
        private final UUID requestId;
        private final NavigationPlan plan;
        private final RouteOption option;
        private java.util.List<RouteOption.PathStep> steps;
        private final TravelPace pace;
        private final OptionalDouble arrivalHeading;
        private int stepIndex;
        private final int startedTick;
        private double lastX;
        private double lastY;
        private double lastZ;
        private int lastProgressSampleTick;
        private double sampleX;
        private double sampleY;
        private double sampleZ;
        private int stuckSamples;
        private double distanceTravelled;
        private int lastActionTick = Integer.MIN_VALUE / 2;
        private int actionAttempts;
        private int stableTicks;
        private int settlingTicks;
        private double horizontalMovementSquared;
        private double verticalMovement;
        private int gapStepIndex = -1;
        private int jumpedStep = -1;
        private int progressStep = -1;
        private int lastApproachTick;
        private double bestRemaining;
        private boolean gapLaunched;
        private boolean gapBackUp;
        private int gapPreparationTicks;
        private double gapCenterX, gapCenterZ, gapDirectionX, gapDirectionZ;

        private ActiveRoute(
                UUID requestId,
                NavigationPlan plan,
                RouteOption option,
                TravelPace pace,
                OptionalDouble arrivalHeading,
                int stepIndex,
                int startedTick,
                double lastX,
                double lastY,
                double lastZ,
                int lastProgressSampleTick,
                double sampleX,
                double sampleY,
                double sampleZ,
                double distanceTravelled
        ) {
            this.requestId = requestId;
            this.plan = plan;
            this.option = option;
            this.steps = option.steps();
            this.pace = pace;
            this.arrivalHeading = arrivalHeading;
            this.stepIndex = stepIndex;
            this.startedTick = startedTick;
            this.lastX = lastX;
            this.lastY = lastY;
            this.lastZ = lastZ;
            this.lastProgressSampleTick = startedTick;
            this.sampleX = sampleX;
            this.sampleY = sampleY;
            this.sampleZ = sampleZ;
            this.distanceTravelled = distanceTravelled;
        }
    }
}
