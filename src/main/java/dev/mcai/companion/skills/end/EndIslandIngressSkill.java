package dev.mcai.companion.skills.end;

import dev.mcai.companion.action.ActionOutcome;
import dev.mcai.companion.action.BlockFace;
import dev.mcai.companion.action.LookIntent;
import dev.mcai.companion.action.MovementIntent;
import dev.mcai.companion.navigation.GridPos;
import dev.mcai.companion.navigation.LocalAStarPlanner;
import dev.mcai.companion.navigation.LocalPlannerOptions;
import dev.mcai.companion.navigation.LocalPlanningBudget;
import dev.mcai.companion.navigation.LocalRoute;
import dev.mcai.companion.navigation.NavigationEvidence;
import dev.mcai.companion.navigation.NavigationRiskProfile;
import dev.mcai.companion.navigation.ObservedVoxel;
import dev.mcai.companion.perception.BlockCoordinate;
import dev.mcai.companion.perception.PerceptionVec3;
import dev.mcai.companion.perception.VisibleBlockFace;
import dev.mcai.companion.skill.Skill;
import dev.mcai.companion.skill.SkillCheckpoint;
import dev.mcai.companion.skill.SkillContext;
import dev.mcai.companion.skill.SkillFailure;
import dev.mcai.companion.skill.SkillParameterParser;
import dev.mcai.companion.skill.SkillParameterResult;
import dev.mcai.companion.skill.SkillResult;
import dev.mcai.companion.skill.SkillTickResult;
import dev.mcai.companion.skills.bridging.BridgeMaterialActuator;
import dev.mcai.companion.skills.bridging.BridgeToParameters;
import dev.mcai.companion.skills.bridging.BridgeToSkill;
import dev.mcai.companion.skills.bridging.TowerUpParameters;
import dev.mcai.companion.skills.bridging.TowerUpSkill;
import dev.mcai.companion.skills.core.CoreSkillActuator;
import dev.mcai.companion.skills.core.CoreSkillFrame;
import dev.mcai.companion.skills.core.CoreSkillFrameSource;
import dev.mcai.companion.skills.core.TravelToParameters;
import dev.mcai.companion.skills.core.TravelToSkill;
import dev.mcai.companion.skills.interaction.BreakBlockParameters;
import dev.mcai.companion.skills.interaction.BreakBlockSkill;
import dev.mcai.companion.skills.interaction.InteractionSkillActuator;
import dev.mcai.companion.skills.interaction.InteractionSkillFrameSource;
import dev.mcai.companion.skills.interaction.InteractionSkillPolicy;
import dev.mcai.companion.skills.interaction.ObservedBlockTarget;
import dev.mcai.companion.waypoint.DimensionRef;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

/**
 * Reaches the natural central End island using only fair observations.
 *
 * <p>The only global fact used here is vanilla's seed-independent horizontal
 * End origin. It is a heading, never a landing coordinate. Every future
 * standing cell must be supplied by the current first-person semantic frame.
 * Unknown space is inspected, one bridge block is authorized at a time, and
 * completion requires a new observation of natural End stone directly under
 * the authoritative body.</p>
 */
public final class EndIslandIngressSkill
        implements Skill<EndIslandIngressParameters> {
    public static final String NAME = "reach_end_island";

    private static final String END_STONE = "minecraft:end_stone";
    private static final double BRIDGE_ARRIVAL_RADIUS = 0.65;
    private static final double LANDFALL_ARRIVAL_RADIUS = 0.75;
    private static final double MINIMUM_CENTER_PROGRESS = 0.20;
    private static final double CURRENT_SUPPORT_CENTER_TOLERANCE = 0.72;
    private static final int MINIMUM_SCAN_INTERVAL_TICKS = 2;
    private static final int BLOCK_ALIGNMENT_TIMEOUT_TICKS = 60;
    private static final int STABLE_GROUND_RECOVERY_TIMEOUT_TICKS = 80;
    private static final List<String> PICKAXE_PRIORITY = List.of(
            "minecraft:netherite_pickaxe",
            "minecraft:diamond_pickaxe",
            "minecraft:iron_pickaxe",
            "minecraft:stone_pickaxe",
            "minecraft:golden_pickaxe",
            "minecraft:wooden_pickaxe"
    );
    private static final LocalPlanningBudget LANDFALL_PLANNING_BUDGET =
            new LocalPlanningBudget(2_048, Duration.ofMillis(2));

    private final UUID expectedPlayerId;
    private final CoreSkillActuator actuator;
    private final CoreSkillFrameSource frames;
    private final BridgeMaterialActuator materials;
    private final LongSupplier sessionGeneration;
    private final InteractionSkillActuator interactionActuator;
    private final InteractionSkillFrameSource interactionFrames;
    private final LongConsumer completionSink;
    private final LocalAStarPlanner landfallPlanner =
            new LocalAStarPlanner();

    private Phase phase = Phase.IDLE;
    private SkillFailure failure;
    private long boundSessionGeneration = -1;
    private long startedAtTick = -1;
    private long lastObservationRevision = -1;
    private long requiredFreshRevision = -1;
    private long nextScanTick = -1;
    private double initialRadius = Double.NaN;
    private double bestRadius = Double.NaN;
    private int bridgeSteps;
    private int towerSteps;
    private int minedBlocks;
    private int scanTurns;
    private int childFailures;
    private int landfallAttempts;
    private String lastChildFailureCode = "";
    private GridPos candidateSupport;
    private BridgeToSkill bridge;
    private BridgeToParameters bridgeParameters;
    private TowerUpSkill tower;
    private TowerUpParameters towerParameters;
    private BreakBlockSkill blockBreak;
    private BreakBlockParameters blockBreakParameters;
    private BlockCoordinate pendingBreakBlock;
    private long blockAlignmentStartedTick = -1;
    private long stableGroundRecoveryStartedTick = -1;
    private TravelToSkill travel;
    private TravelToParameters travelParameters;
    private boolean completionPublished;

    public EndIslandIngressSkill(
            final UUID expectedPlayerId,
            final CoreSkillActuator actuator,
            final CoreSkillFrameSource frames,
            final BridgeMaterialActuator materials,
            final LongSupplier sessionGeneration
    ) {
        this(
                expectedPlayerId,
                actuator,
                frames,
                materials,
                sessionGeneration,
                null,
                null,
                ignored -> {
                }
        );
    }

    public EndIslandIngressSkill(
            final UUID expectedPlayerId,
            final CoreSkillActuator actuator,
            final CoreSkillFrameSource frames,
            final BridgeMaterialActuator materials,
            final LongSupplier sessionGeneration,
            final InteractionSkillActuator interactionActuator,
            final InteractionSkillFrameSource interactionFrames
    ) {
        this(
                expectedPlayerId,
                actuator,
                frames,
                materials,
                sessionGeneration,
                interactionActuator,
                interactionFrames,
                ignored -> {
                }
        );
    }

    public EndIslandIngressSkill(
            final UUID expectedPlayerId,
            final CoreSkillActuator actuator,
            final CoreSkillFrameSource frames,
            final BridgeMaterialActuator materials,
            final LongSupplier sessionGeneration,
            final InteractionSkillActuator interactionActuator,
            final InteractionSkillFrameSource interactionFrames,
            final LongConsumer completionSink
    ) {
        this.expectedPlayerId = Objects.requireNonNull(
                expectedPlayerId,
                "expectedPlayerId"
        );
        this.actuator = Objects.requireNonNull(actuator, "actuator");
        this.frames = Objects.requireNonNull(frames, "frames");
        this.materials = Objects.requireNonNull(materials, "materials");
        this.sessionGeneration = Objects.requireNonNull(
                sessionGeneration,
                "sessionGeneration"
        );
        if ((interactionActuator == null) != (interactionFrames == null)) {
            throw new IllegalArgumentException(
                    "Interaction dependencies must be supplied together"
            );
        }
        this.interactionActuator = interactionActuator;
        this.interactionFrames = interactionFrames;
        this.completionSink = Objects.requireNonNull(
                completionSink,
                "completionSink"
        );
    }

    @Override
    public SkillParameterParser<EndIslandIngressParameters> parameters() {
        return arguments -> arguments != null && arguments.isEmpty()
                ? SkillParameterResult.valid(
                        EndIslandIngressParameters
                                .localControllerDefaults()
                )
                : SkillParameterResult.invalid(
                        NAME + ".invalid_arguments"
                );
    }

    @Override
    public Optional<SkillFailure> preconditions(
            final SkillContext context,
            final EndIslandIngressParameters parameters
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(parameters, "parameters");
        final Optional<CoreSkillFrame> current = frames.current();
        if (current.isEmpty()) {
            return failure(NAME + ".observation_unavailable");
        }
        final CoreSkillFrame frame = current.orElseThrow();
        final Optional<SkillFailure> bodyFailure = validateBody(frame);
        if (bodyFailure.isPresent()) {
            return bodyFailure;
        }
        if (!DimensionRef.END.equals(frame.dimension())) {
            return failure(NAME + ".end_dimension_required");
        }
        if (!frame.onGround() || frame.inWater()) {
            return failure(NAME + ".stable_ground_required");
        }
        if (EndArenaTopology.horizontalRadius(frame.position())
                > parameters.maximumStartRadius()) {
            return failure(NAME + ".outside_end_spawn_envelope");
        }
        if (currentSessionGeneration() < 0) {
            return failure(NAME + ".session_unavailable");
        }
        return Optional.empty();
    }

    @Override
    public void start(
            final SkillContext context,
            final EndIslandIngressParameters parameters
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(parameters, "parameters");
        final CoreSkillFrame frame = frames.current().orElseThrow(
                () -> new IllegalStateException(
                        "End ingress body changed before start"
                )
        );
        boundSessionGeneration = currentSessionGeneration();
        if (boundSessionGeneration < 0) {
            throw new IllegalStateException(
                    "End ingress session is unavailable"
            );
        }
        phase = Phase.SCANNING;
        failure = null;
        startedAtTick = context.gameTick();
        lastObservationRevision = frame.observationRevision();
        requiredFreshRevision = frame.observationRevision();
        nextScanTick = context.gameTick();
        initialRadius = EndArenaTopology.horizontalRadius(
                frame.position()
        );
        bestRadius = initialRadius;
        bridgeSteps = 0;
        towerSteps = 0;
        minedBlocks = 0;
        scanTurns = 0;
        childFailures = 0;
        landfallAttempts = 0;
        lastChildFailureCode = "";
        candidateSupport = null;
        bridge = null;
        bridgeParameters = null;
        tower = null;
        towerParameters = null;
        blockBreak = null;
        blockBreakParameters = null;
        pendingBreakBlock = null;
        blockAlignmentStartedTick = -1;
        stableGroundRecoveryStartedTick = -1;
        travel = null;
        travelParameters = null;
        completionPublished = false;
    }

    @Override
    public SkillTickResult tick(
            final SkillContext context,
            final EndIslandIngressParameters parameters
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(parameters, "parameters");
        if (!phase.active()) {
            return SkillTickResult.failed(NAME + ".invalid_state");
        }
        try {
            return tickSafely(context, parameters);
        } catch (RuntimeException exception) {
            return fail(NAME + ".internal_failure");
        }
    }

    @Override
    public SkillCheckpoint checkpoint(
            final SkillContext context,
            final EndIslandIngressParameters parameters
    ) {
        final Optional<CoreSkillFrame> current = frames.current();
        final PerceptionVec3 position = current
                .map(CoreSkillFrame::position)
                .orElse(new PerceptionVec3(0.0, 0.0, 0.0));
        final double radius = current
                .map(CoreSkillFrame::position)
                .map(EndArenaTopology::horizontalRadius)
                .orElse(Double.NaN);
        return new SkillCheckpoint(
                1,
                String.format(
                        Locale.ROOT,
                        "{\"phase\":\"%s\",\"x\":%.3f,"
                                + "\"y\":%.3f,\"z\":%.3f,"
                                + "\"radius\":%.3f,"
                                + "\"initialRadius\":%.3f,"
                                + "\"bridgeSteps\":%d,"
                                + "\"towerSteps\":%d,"
                                + "\"minedBlocks\":%d,"
                                + "\"scanTurns\":%d,"
                                + "\"childFailures\":%d,"
                                + "\"landfallAttempts\":%d,"
                                + "\"lastChildFailure\":\"%s\","
                                + "\"candidateSupport\":\"%s\"}",
                        phase.name(),
                        position.x(),
                        position.y(),
                        position.z(),
                        radius,
                        initialRadius,
                        bridgeSteps,
                        towerSteps,
                        minedBlocks,
                        scanTurns,
                        childFailures,
                        landfallAttempts,
                        lastChildFailureCode,
                        candidateSupport == null
                                ? ""
                                : candidateSupport.toString()
                )
        );
    }

    @Override
    public void cancel(
            final SkillContext context,
            final EndIslandIngressParameters parameters
    ) {
        cancelChildren(context);
        quiesce();
        phase = Phase.CANCELLED;
        candidateSupport = null;
    }

    @Override
    public SkillResult result(
            final SkillContext context,
            final EndIslandIngressParameters parameters
    ) {
        return switch (phase) {
            case COMPLETED -> SkillResult.completed();
            case CANCELLED -> SkillResult.cancelled();
            case FAILED -> SkillResult.failed(
                    Objects.requireNonNull(failure)
            );
            default -> SkillResult.failed(
                    SkillFailure.of(NAME + ".invalid_state")
            );
        };
    }

    private SkillTickResult tickSafely(
            final SkillContext context,
            final EndIslandIngressParameters parameters
    ) {
        if (context.gameTick() - startedAtTick >= parameters.timeoutTicks()) {
            return fail(NAME + ".timed_out");
        }
        if (currentSessionGeneration() != boundSessionGeneration) {
            return fail(NAME + ".body_session_changed");
        }
        final Optional<CoreSkillFrame> current = frames.current();
        if (current.isEmpty()) {
            return fail(NAME + ".observation_unavailable");
        }
        final CoreSkillFrame frame = current.orElseThrow();
        final Optional<SkillFailure> bodyFailure = validateBody(frame);
        if (bodyFailure.isPresent()) {
            return fail(bodyFailure.orElseThrow());
        }
        if (!DimensionRef.END.equals(frame.dimension())) {
            return fail(NAME + ".end_dimension_lost");
        }
        if (frame.observationRevision() < lastObservationRevision) {
            return fail(NAME + ".stale_observation");
        }
        final double currentRadius = EndArenaTopology.horizontalRadius(
                frame.position()
        );
        if (Double.isNaN(bestRadius)
                || currentRadius + MINIMUM_CENTER_PROGRESS < bestRadius) {
            bestRadius = currentRadius;
        }
        final boolean fresh = frame.observationRevision()
                > lastObservationRevision;
        if (fresh) {
            lastObservationRevision = frame.observationRevision();
        }

        if (phase == Phase.BRIDGING_ONE_STEP) {
            return tickBridge(context, parameters, frame, fresh);
        }
        if (phase == Phase.TOWERING_FOR_LANDFALL) {
            return tickTower(context, parameters, frame, fresh);
        }
        if (phase == Phase.TRAVELLING_TO_OBSERVED_END_STONE) {
            return tickTravel(context, parameters, frame, fresh);
        }
        if (phase == Phase.RECOVERING_STABLE_GROUND) {
            return tickStableGroundRecovery(context, frame);
        }

        /* Active child skills own their transient jumping/falling pose and
         * enforce their own safety envelope. Stable ground is required only
         * before this parent authorizes a new child or accepts completion. */
        if (!frame.onGround() || frame.inWater()) {
            cancelChildren(context);
            pendingBreakBlock = null;
            blockAlignmentStartedTick = -1;
            stableGroundRecoveryStartedTick = context.gameTick();
            phase = Phase.RECOVERING_STABLE_GROUND;
            quiesce();
            return SkillTickResult.running(true, false);
        }
        if (phase == Phase.MINING_VISIBLE_END_STONE) {
            return tickBlockBreak(context, parameters, frame, fresh);
        }
        if (phase == Phase.ALIGNING_VISIBLE_BLOCK_BREAK) {
            return tickBlockAlignment(context, parameters, frame, fresh);
        }

        if (fresh
                && frame.observationRevision() > requiredFreshRevision
                && verifiedCurrentEndStoneSupport(frame, parameters)) {
            quiesce();
            if (!completionPublished) {
                completionSink.accept(context.goalRevision());
                completionPublished = true;
            }
            phase = Phase.COMPLETED;
            candidateSupport = frame.feet().below();
            return SkillTickResult.completed();
        }

        if (phase == Phase.VERIFYING_CURRENT_SUPPORT) {
            if (fresh
                    && frame.observationRevision()
                        > requiredFreshRevision) {
                candidateSupport = null;
                return recoverChildFailure(
                        context,
                        parameters,
                        frame,
                        "landfall_verification"
                );
            }
            return orientForFreshObservation(context, parameters, frame);
        }
        if (context.gameTick() < nextScanTick
                || frame.observationRevision() <= requiredFreshRevision) {
            return orientForFreshObservation(context, parameters, frame);
        }

        scanTurns++;
        if (scanTurns > parameters.maximumScanTurns()) {
            return fail(NAME + ".scan_budget_exhausted");
        }
        final Optional<GridPos> landfall = selectLandfall(
                frame,
                parameters,
                context.hardcore()
        );
        if (landfall.isPresent()) {
            return startLandfallTravel(
                    context,
                    parameters,
                    frame,
                    landfall.orElseThrow()
            );
        }
        final Optional<VisibleBlockFace> obstruction =
                visibleIngressObstruction(frame);
        if (obstruction.filter(face ->
                isCurrentColumnOverhead(frame, face)
        ).isPresent()) {
            return startVisibleBlockBreak(
                    context,
                    parameters,
                    frame,
                obstruction.orElseThrow()
            );
        }
        /* A visible wall at the body/head columns is a legal, bounded
         * excavation opportunity.  Prefer it when the body has been pushed
         * outward, or when real interaction wiring is available.  The
         * old tower-first branch could repeatedly pillar on the outside of
         * the natural End spawn wall and spend every retry without gaining
         * centerward distance. */
        final Optional<VisibleBlockFace> forwardWall =
                visibleCenterwardBodyWall(frame);
        final boolean centerwardRecoveryNeeded = currentRadius
                > bestRadius + MINIMUM_CENTER_PROGRESS;
        if (interactionActuator != null && forwardWall.isPresent()) {
            return startVisibleBlockBreak(
                    context,
                    parameters,
                    frame,
                forwardWall.orElseThrow()
            );
        }
        if (centerwardRecoveryNeeded) {
            return startOneBlockBridge(context, parameters, frame);
        }
        if (visibleCenterwardEndStoneWall(frame)) {
            return startOneBlockTower(context, parameters, frame);
        }
        if (obstruction.isPresent()) {
            return startVisibleBlockBreak(
                    context,
                    parameters,
                    frame,
                    obstruction.orElseThrow()
            );
        }
        return startOneBlockBridge(context, parameters, frame);
    }

    private SkillTickResult tickBridge(
            final SkillContext context,
            final EndIslandIngressParameters parameters,
            final CoreSkillFrame frame,
            final boolean fresh
    ) {
        if (bridge == null || bridgeParameters == null) {
            return fail(NAME + ".bridge_binding_missing");
        }
        final SkillTickResult child = bridge.tick(
                context,
                bridgeParameters
        );
        if (child.status() == SkillTickResult.Status.COMPLETED) {
            bridge = null;
            bridgeParameters = null;
            bridgeSteps++;
            childFailures = 0;
            phase = Phase.SCANNING;
            requiredFreshRevision = frame.observationRevision();
            nextScanTick = context.gameTick()
                    + MINIMUM_SCAN_INTERVAL_TICKS;
            return SkillTickResult.running(true, true);
        }
        if (child.status() == SkillTickResult.Status.FAILED) {
            bridge = null;
            bridgeParameters = null;
            final String code = child.failure()
                    .map(SkillFailure::code)
                    .orElse("bridge");
            return recoverChildFailure(
                    context,
                    parameters,
                    frame,
                    code
            );
        }
        return SkillTickResult.running(
                child.madeProgress() || fresh,
                child.safeCheckpoint()
        );
    }

    private SkillTickResult tickTravel(
            final SkillContext context,
            final EndIslandIngressParameters parameters,
            final CoreSkillFrame frame,
            final boolean fresh
    ) {
        if (travel == null || travelParameters == null) {
            return fail(NAME + ".travel_binding_missing");
        }
        final SkillTickResult child = travel.tick(
                context,
                travelParameters
        );
        if (child.status() == SkillTickResult.Status.COMPLETED) {
            travel = null;
            travelParameters = null;
            childFailures = 0;
            phase = Phase.VERIFYING_CURRENT_SUPPORT;
            requiredFreshRevision = frame.observationRevision();
            nextScanTick = context.gameTick()
                    + MINIMUM_SCAN_INTERVAL_TICKS;
            return SkillTickResult.running(true, true);
        }
        if (child.status() == SkillTickResult.Status.FAILED) {
            travel = null;
            travelParameters = null;
            candidateSupport = null;
            final String code = child.failure()
                    .map(SkillFailure::code)
                    .orElse("travel");
            return recoverChildFailure(
                    context,
                    parameters,
                    frame,
                    code
            );
        }
        return SkillTickResult.running(
                child.madeProgress() || fresh,
                child.safeCheckpoint()
        );
    }

    private SkillTickResult tickTower(
            final SkillContext context,
            final EndIslandIngressParameters parameters,
            final CoreSkillFrame frame,
            final boolean fresh
    ) {
        if (tower == null || towerParameters == null) {
            return fail(NAME + ".tower_binding_missing");
        }
        final SkillTickResult child = tower.tick(
                context,
                towerParameters
        );
        if (child.status() == SkillTickResult.Status.COMPLETED) {
            tower = null;
            towerParameters = null;
            towerSteps++;
            childFailures = 0;
            phase = Phase.SCANNING;
            requiredFreshRevision = frame.observationRevision();
            nextScanTick = context.gameTick()
                    + MINIMUM_SCAN_INTERVAL_TICKS;
            return SkillTickResult.running(true, true);
        }
        if (child.status() == SkillTickResult.Status.FAILED) {
            tower = null;
            towerParameters = null;
            final String code = child.failure()
                    .map(SkillFailure::code)
                    .orElse("tower");
            return recoverChildFailure(
                    context,
                    parameters,
                    frame,
                    code
            );
        }
        return SkillTickResult.running(
                child.madeProgress() || fresh,
                child.safeCheckpoint()
        );
    }

    private SkillTickResult tickBlockBreak(
            final SkillContext context,
            final EndIslandIngressParameters parameters,
            final CoreSkillFrame frame,
            final boolean fresh
    ) {
        if (blockBreak == null || blockBreakParameters == null) {
            return fail(NAME + ".block_break_binding_missing");
        }
        final SkillTickResult child = blockBreak.tick(
                context,
                blockBreakParameters
        );
        if (child.status() == SkillTickResult.Status.COMPLETED) {
            blockBreak = null;
            blockBreakParameters = null;
            minedBlocks++;
            childFailures = 0;
            phase = Phase.SCANNING;
            requiredFreshRevision = frame.observationRevision();
            nextScanTick = context.gameTick()
                    + MINIMUM_SCAN_INTERVAL_TICKS;
            return SkillTickResult.running(true, true);
        }
        if (child.status() == SkillTickResult.Status.FAILED) {
            blockBreak = null;
            blockBreakParameters = null;
            final String code = child.failure()
                    .map(SkillFailure::code)
                    .orElse("block_break");
            return recoverChildFailure(
                    context,
                    parameters,
                    frame,
                    code
            );
        }
        return SkillTickResult.running(
                child.madeProgress() || fresh,
                child.safeCheckpoint()
        );
    }

    private SkillTickResult tickBlockAlignment(
            final SkillContext context,
            final EndIslandIngressParameters parameters,
            final CoreSkillFrame frame,
            final boolean fresh
    ) {
        if (interactionActuator == null
                || interactionFrames == null
                || pendingBreakBlock == null) {
            return fail(NAME + ".block_alignment_binding_missing");
        }
        if (context.gameTick() - blockAlignmentStartedTick
                >= BLOCK_ALIGNMENT_TIMEOUT_TICKS) {
            pendingBreakBlock = null;
            blockAlignmentStartedTick = -1;
            return recoverChildFailure(
                    context,
                    parameters,
                    frame,
                    "block_alignment"
            );
        }
        final Optional<VisibleBlockFace> visibleTarget =
                frame.visibleBlockFaces().stream()
                        .filter(face -> END_STONE.equals(
                                face.blockTypeId()
                        ))
                        .filter(face -> face.block().equals(
                                pendingBreakBlock
                        ))
                        .min(Comparator.comparingDouble(
                                VisibleBlockFace::distance
                        ));
        if (visibleTarget.isEmpty()) {
            if (fresh
                    && frame.observationRevision()
                        > requiredFreshRevision) {
                pendingBreakBlock = null;
                blockAlignmentStartedTick = -1;
                return recoverChildFailure(
                        context,
                        parameters,
                        frame,
                        "block_alignment_target_lost"
                );
            }
            return SkillTickResult.running(false, true);
        }
        final VisibleBlockFace aim = visibleTarget.orElseThrow();
        if (!fresh
                || frame.observationRevision()
                    <= requiredFreshRevision) {
            if (!actuator.move(MovementIntent.STOPPED).accepted()
                    || !actuator.look(lookAt(
                            frame.eyePosition(),
                            aim.hitPosition()
                    )).accepted()) {
                return fail(NAME + ".block_alignment_rejected");
            }
            return SkillTickResult.running(true, true);
        }
        final Optional<dev.mcai.companion.skills.interaction
                .InteractionSkillFrame> interactionFrame =
                interactionFrames.current().filter(candidate ->
                        candidate.observationRevision()
                                == frame.observationRevision()
                );
        if (interactionFrame.isEmpty()) {
            if (!actuator.move(MovementIntent.STOPPED).accepted()
                    || !actuator.look(lookAt(
                            frame.eyePosition(),
                            aim.hitPosition()
                    )).accepted()) {
                return fail(NAME + ".block_alignment_rejected");
            }
            return SkillTickResult.running(true, true);
        }
        final Optional<VisibleBlockFace> crosshair =
                interactionFrames.currentCrosshairBlock()
                        .filter(face -> END_STONE.equals(
                                face.blockTypeId()
                        ))
                        .filter(face -> face.block().equals(
                                pendingBreakBlock
                        ));
        if (crosshair.isEmpty()) {
            if (!actuator.move(MovementIntent.STOPPED).accepted()
                    || !actuator.look(lookAt(
                            frame.eyePosition(),
                            aim.hitPosition()
                    )).accepted()) {
                return fail(NAME + ".block_alignment_rejected");
            }
            return SkillTickResult.running(true, true);
        }
        final VisibleBlockFace crosshairFace = crosshair.orElseThrow();
        final Optional<VisibleBlockFace> executable =
                interactionFrame.orElseThrow()
                        .visibleBlockFaces().stream()
                        .filter(face -> END_STONE.equals(
                                face.blockTypeId()
                        ))
                        .filter(face -> face.block().equals(
                                pendingBreakBlock
                        ))
                        .filter(face -> face.face().equals(
                                crosshairFace.face()
                        ))
                        .findFirst();
        if (executable.isEmpty()) {
            if (!actuator.move(MovementIntent.STOPPED).accepted()
                    || !actuator.look(lookAt(
                            frame.eyePosition(),
                            aim.hitPosition()
                    )).accepted()) {
                return fail(NAME + ".block_alignment_rejected");
            }
            return SkillTickResult.running(true, true);
        }
        pendingBreakBlock = null;
        blockAlignmentStartedTick = -1;
        return beginAlignedBlockBreak(
                context,
                parameters,
                frame,
                executable.orElseThrow()
        );
    }

    private SkillTickResult tickStableGroundRecovery(
            final SkillContext context,
            final CoreSkillFrame frame
    ) {
        if (frame.onGround() && !frame.inWater()) {
            stableGroundRecoveryStartedTick = -1;
            phase = Phase.SCANNING;
            requiredFreshRevision = frame.observationRevision();
            nextScanTick = context.gameTick()
                    + MINIMUM_SCAN_INTERVAL_TICKS;
            childFailures = 0;
            quiesce();
            return SkillTickResult.running(true, true);
        }
        if (context.gameTick() - stableGroundRecoveryStartedTick
                >= STABLE_GROUND_RECOVERY_TIMEOUT_TICKS) {
            return fail(NAME + ".stable_ground_recovery_exhausted");
        }
        quiesce();
        return SkillTickResult.running(false, false);
    }

    private SkillTickResult recoverChildFailure(
            final SkillContext context,
            final EndIslandIngressParameters parameters,
            final CoreSkillFrame frame,
            final String child
    ) {
        childFailures++;
        lastChildFailureCode = child;
        if (childFailures > parameters.maximumChildFailures()) {
            return fail(NAME + "." + child + "_retry_exhausted");
        }
        phase = Phase.SCANNING;
        requiredFreshRevision = frame.observationRevision();
        nextScanTick = context.gameTick() + MINIMUM_SCAN_INTERVAL_TICKS;
        return SkillTickResult.running(true, true);
    }

    private SkillTickResult startOneBlockBridge(
            final SkillContext context,
            final EndIslandIngressParameters parameters,
            final CoreSkillFrame frame
    ) {
        if (bridgeSteps >= parameters.maximumBridgeBlocks()) {
            return fail(NAME + ".bridge_budget_exhausted");
        }
        final PerceptionVec3 target = bridgeTargetTowardCenter(
                frame.position()
        );
        if (target.equals(frame.position())) {
            return fail(NAME + ".center_heading_unavailable");
        }
        final BridgeToParameters childParameters =
                new BridgeToParameters(
                        DimensionRef.END,
                        target.x(),
                        frame.position().y(),
                        target.z(),
                        BRIDGE_ARRIVAL_RADIUS,
                        1
                );
        final BridgeToSkill child = new BridgeToSkill(
                expectedPlayerId,
                actuator,
                frames,
                materials
        );
        final Optional<SkillFailure> rejected = child.preconditions(
                context,
                childParameters
        );
        if (rejected.isPresent()) {
            childFailures++;
            if (childFailures > parameters.maximumChildFailures()) {
                return fail(NAME + ".bridge_retry_exhausted");
            }
            requiredFreshRevision = frame.observationRevision();
            nextScanTick = context.gameTick()
                    + MINIMUM_SCAN_INTERVAL_TICKS;
            return SkillTickResult.running(false, true);
        }
        child.start(context, childParameters);
        bridge = child;
        bridgeParameters = childParameters;
        phase = Phase.BRIDGING_ONE_STEP;
        return tickBridge(context, parameters, frame, true);
    }

    private SkillTickResult startLandfallTravel(
            final SkillContext context,
            final EndIslandIngressParameters parameters,
            final CoreSkillFrame frame,
            final GridPos support
    ) {
        final GridPos feet = support.above();
        final TravelToParameters childParameters =
                new TravelToParameters(
                        DimensionRef.END,
                        feet.x() + 0.5,
                        feet.y(),
                        feet.z() + 0.5,
                        LANDFALL_ARRIVAL_RADIUS
                );
        final TravelToSkill child = new TravelToSkill(
                expectedPlayerId,
                actuator,
                frames,
                sessionGeneration
        );
        final Optional<SkillFailure> rejected = child.preconditions(
                context,
                childParameters
        );
        if (rejected.isPresent()) {
            childFailures++;
            if (childFailures > parameters.maximumChildFailures()) {
                return fail(NAME + ".travel_retry_exhausted");
            }
            requiredFreshRevision = frame.observationRevision();
            nextScanTick = context.gameTick()
                    + MINIMUM_SCAN_INTERVAL_TICKS;
            return SkillTickResult.running(false, true);
        }
        child.start(context, childParameters);
        travel = child;
        travelParameters = childParameters;
        candidateSupport = support;
        landfallAttempts++;
        phase = Phase.TRAVELLING_TO_OBSERVED_END_STONE;
        return SkillTickResult.running(true, true);
    }

    private SkillTickResult startOneBlockTower(
            final SkillContext context,
            final EndIslandIngressParameters parameters,
            final CoreSkillFrame frame
    ) {
        if (towerSteps >= parameters.maximumTowerBlocks()) {
            return fail(NAME + ".tower_budget_exhausted");
        }
        final TowerUpParameters childParameters =
                new TowerUpParameters(
                        DimensionRef.END,
                        frame.position().y() + 1.0,
                        0.35,
                        1
                );
        final TowerUpSkill child = new TowerUpSkill(
                expectedPlayerId,
                actuator,
                frames,
                materials
        );
        final Optional<SkillFailure> rejected = child.preconditions(
                context,
                childParameters
        );
        if (rejected.isPresent()) {
            childFailures++;
            if (childFailures > parameters.maximumChildFailures()) {
                return fail(NAME + ".tower_retry_exhausted");
            }
            requiredFreshRevision = frame.observationRevision();
            nextScanTick = context.gameTick()
                    + MINIMUM_SCAN_INTERVAL_TICKS;
            return SkillTickResult.running(false, true);
        }
        child.start(context, childParameters);
        tower = child;
        towerParameters = childParameters;
        phase = Phase.TOWERING_FOR_LANDFALL;
        return tickTower(context, parameters, frame, true);
    }

    private SkillTickResult startVisibleBlockBreak(
            final SkillContext context,
            final EndIslandIngressParameters parameters,
            final CoreSkillFrame frame,
            final VisibleBlockFace face
    ) {
        if (interactionActuator == null || interactionFrames == null) {
            return fail(NAME + ".mining_unavailable");
        }
        if (minedBlocks >= parameters.maximumMinedBlocks()) {
            return fail(NAME + ".mining_budget_exhausted");
        }
        final Optional<String> pickaxe = ownedPickaxe(frame);
        if (pickaxe.isEmpty()) {
            return fail(NAME + ".pickaxe_required");
        }
        pendingBreakBlock = face.block();
        blockAlignmentStartedTick = context.gameTick();
        requiredFreshRevision = frame.observationRevision();
        phase = Phase.ALIGNING_VISIBLE_BLOCK_BREAK;
        if (!actuator.move(MovementIntent.STOPPED).accepted()
                || !actuator.look(lookAt(
                        frame.eyePosition(),
                        face.hitPosition()
                )).accepted()) {
            return fail(NAME + ".block_alignment_rejected");
        }
        return SkillTickResult.running(true, true);
    }

    private SkillTickResult beginAlignedBlockBreak(
            final SkillContext context,
            final EndIslandIngressParameters parameters,
            final CoreSkillFrame frame,
            final VisibleBlockFace face
    ) {
        final Optional<String> pickaxe = ownedPickaxe(frame);
        if (pickaxe.isEmpty()) {
            return fail(NAME + ".pickaxe_required");
        }
        final ActionOutcome equipped = interactionActuator.equipMainHand(
                pickaxe.orElseThrow()
        );
        if (!equipped.accepted()) {
            return fail(NAME + ".pickaxe_equip_rejected");
        }
        final BlockCoordinate block = face.block();
        final BreakBlockParameters childParameters =
                new BreakBlockParameters(
                        DimensionRef.END,
                        new ObservedBlockTarget(
                                frame.observationRevision(),
                                block.x(),
                                block.y(),
                                block.z(),
                                blockFace(face.face())
                        )
                );
        final BreakBlockSkill child = new BreakBlockSkill(
                expectedPlayerId,
                interactionActuator,
                interactionFrames,
                InteractionSkillPolicy.defaults()
        );
        final Optional<SkillFailure> rejected = child.preconditions(
                context,
                childParameters
        );
        if (rejected.isPresent()) {
            return recoverChildFailure(
                    context,
                    parameters,
                    frame,
                    "block_break_precondition"
            );
        }
        child.start(context, childParameters);
        blockBreak = child;
        blockBreakParameters = childParameters;
        phase = Phase.MINING_VISIBLE_END_STONE;
        return tickBlockBreak(context, parameters, frame, true);
    }

    private SkillTickResult orientForFreshObservation(
            final SkillContext context,
            final EndIslandIngressParameters parameters,
            final CoreSkillFrame frame
    ) {
        if (scanTurns >= parameters.maximumScanTurns()) {
            return fail(NAME + ".scan_budget_exhausted");
        }
        if (!actuator.move(MovementIntent.STOPPED).accepted()) {
            return fail(NAME + ".actuator_rejected");
        }
        if (!actuator.look(towardCenterFloor(frame)).accepted()) {
            return fail(NAME + ".actuator_rejected");
        }
        /* Set one observation deadline, then let ticks advance toward it.
         * Moving the deadline forward on every early tick would keep the
         * controller orienting forever and never admit the fresh frame. */
        if (nextScanTick <= context.gameTick()) {
            nextScanTick = context.gameTick()
                    + MINIMUM_SCAN_INTERVAL_TICKS;
        }
        return SkillTickResult.running(true, true);
    }

    private Optional<GridPos> selectLandfall(
            final CoreSkillFrame frame,
            final EndIslandIngressParameters parameters,
            final boolean hardcore
    ) {
        if (frame.navigation().revision()
                != frame.observationRevision()) {
            return Optional.empty();
        }
        final double currentRadius = EndArenaTopology.horizontalRadius(
                frame.position()
        );
        final java.util.List<GridPos> candidates =
                frame.visibleBlockFaces().stream()
                .filter(face -> END_STONE.equals(face.blockTypeId()))
                .filter(face -> "up".equals(face.face()))
                .filter(face -> face.topSupportAffordance()
                        .safelySupportsStanding())
                .filter(face -> face.distance()
                        <= parameters.maximumVisibleLandfallDistance())
                .map(VisibleBlockFace::block)
                .map(EndIslandIngressSkill::grid)
                .filter(support -> horizontalDistance(
                        frame.position(),
                        support
                ) <= parameters.maximumVisibleLandfallDistance())
                .filter(support -> !support.equals(frame.feet().below()))
                .filter(support -> radiusOf(support)
                        <= currentRadius - Math.max(
                                MINIMUM_CENTER_PROGRESS,
                                LANDFALL_ARRIVAL_RADIUS
                        ))
                .filter(support -> safeFreshDestination(frame, support))
                .distinct()
                .sorted(Comparator.comparingDouble(
                        EndIslandIngressSkill::radiusOf
                ))
                .toList();
        final LocalPlannerOptions options = new LocalPlannerOptions(
                hardcore
                        ? NavigationRiskProfile.HARDCORE
                        : NavigationRiskProfile.NORMAL,
                LANDFALL_PLANNING_BUDGET,
                hardcore ? 1 : 3,
                true,
                false
        );
        final Set<GridPos> candidateFeet = candidates.stream()
                .map(GridPos::above)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (candidateFeet.isEmpty()) {
            return Optional.empty();
        }
        final LocalRoute route = landfallPlanner.planToAny(
                frame.navigation(),
                frame.feet(),
                candidateFeet,
                options,
                true
        );
        return route.found()
                ? Optional.of(route.reached().below())
                : Optional.empty();
    }

    private static boolean visibleCenterwardEndStoneWall(
            final CoreSkillFrame frame
    ) {
        final PerceptionVec3 target =
                EndArenaTopology.oneCardinalStepTowardCenter(
                        frame.position()
                );
        if (target.equals(frame.position())) {
            return false;
        }
        final GridPos targetFeet = new GridPos(
                (int) Math.floor(target.x()),
                frame.feet().y(),
                (int) Math.floor(target.z())
        );
        return frame.visibleBlockFaces().stream().anyMatch(face -> {
            final GridPos block = grid(face.block());
            return END_STONE.equals(face.blockTypeId())
                    && (block.equals(targetFeet)
                        || block.equals(targetFeet.above()));
        });
    }

    /**
     * BridgeToSkill accepts a bounded arrival radius.  Aim half a block
     * beyond the next cell center so that its legal radius cannot complete
     * while the body is still standing in the previous cell.  The target
     * remains the adjacent observed column; this only fixes the local
     * stopping geometry and does not reveal an unobserved block.
     */
    private static PerceptionVec3 bridgeTargetTowardCenter(
            final PerceptionVec3 position
    ) {
        final PerceptionVec3 step =
                EndArenaTopology.oneCardinalStepTowardCenter(position);
        if (step.equals(position)) {
            return step;
        }
        final double deltaX = EndArenaTopology.CENTER_X - position.x();
        final double deltaZ = EndArenaTopology.CENTER_Z - position.z();
        if (Math.abs(deltaX) >= Math.abs(deltaZ)) {
            return new PerceptionVec3(
                    step.x() + Math.copySign(0.5, deltaX),
                    step.y(),
                    step.z()
            );
        }
        return new PerceptionVec3(
                step.x(),
                step.y(),
                step.z() + Math.copySign(0.5, deltaZ)
        );
    }

    private static Optional<VisibleBlockFace> visibleCenterwardBodyWall(
            final CoreSkillFrame frame
    ) {
        final PerceptionVec3 target =
                EndArenaTopology.oneCardinalStepTowardCenter(
                        frame.position()
                );
        if (target.equals(frame.position())) {
            return Optional.empty();
        }
        final GridPos targetFeet = new GridPos(
                (int) Math.floor(target.x()),
                frame.feet().y(),
                (int) Math.floor(target.z())
        );
        return frame.visibleBlockFaces().stream()
                .filter(face -> END_STONE.equals(face.blockTypeId()))
                .filter(face -> {
                    final GridPos block = grid(face.block());
                    return block.equals(targetFeet)
                            || block.equals(targetFeet.above());
                })
                .min(Comparator.comparingInt(face ->
                        grid(face.block()).y()
                ));
    }

    private static Optional<VisibleBlockFace> visibleIngressObstruction(
            final CoreSkillFrame frame
    ) {
        final GridPos feet = frame.feet();
        final PerceptionVec3 target =
                EndArenaTopology.oneCardinalStepTowardCenter(
                        frame.position()
                );
        final GridPos targetFeet = new GridPos(
                (int) Math.floor(target.x()),
                feet.y(),
                (int) Math.floor(target.z())
        );
        return frame.visibleBlockFaces().stream()
                .filter(face -> END_STONE.equals(face.blockTypeId()))
                .filter(face -> {
                    final GridPos block = grid(face.block());
                    final boolean overhead = block.x() == feet.x()
                            && block.z() == feet.z()
                            && block.y() >= feet.y() + 2
                            && block.y() <= feet.y() + 3;
                    final boolean forward = block.equals(targetFeet)
                            || block.equals(targetFeet.above());
                    return overhead || forward;
                })
                .min(Comparator.comparingInt(face -> {
                    final GridPos block = grid(face.block());
                    return block.x() == feet.x()
                            && block.z() == feet.z() ? 0 : 1;
                }));
    }

    private static boolean isCurrentColumnOverhead(
            final CoreSkillFrame frame,
            final VisibleBlockFace face
    ) {
        final GridPos feet = frame.feet();
        final GridPos block = grid(face.block());
        return block.x() == feet.x()
                && block.z() == feet.z()
                && block.y() >= feet.y() + 2
                && block.y() <= feet.y() + 3;
    }

    private static Optional<String> ownedPickaxe(
            final CoreSkillFrame frame
    ) {
        for (String item : PICKAXE_PRIORITY) {
            if ((item.equals(frame.mainHand().itemId())
                    && frame.mainHand().count() > 0)
                    || frame.inventory().stream().anyMatch(entry ->
                            item.equals(entry.itemId())
                                && entry.count() > 0
                    )) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    private static BlockFace blockFace(final String face) {
        return switch (face) {
            case "down" -> BlockFace.DOWN;
            case "up" -> BlockFace.UP;
            case "north" -> BlockFace.NORTH;
            case "south" -> BlockFace.SOUTH;
            case "west" -> BlockFace.WEST;
            case "east" -> BlockFace.EAST;
            default -> throw new IllegalArgumentException(
                    "Unknown observed block face"
            );
        };
    }

    private static boolean safeFreshDestination(
            final CoreSkillFrame frame,
            final GridPos support
    ) {
        final long revision = frame.navigation().revision();
        final Optional<ObservedVoxel> supportVoxel =
                frame.navigation().voxelAt(support);
        final Optional<ObservedVoxel> feet =
                frame.navigation().voxelAt(support.above());
        final Optional<ObservedVoxel> head =
                frame.navigation().voxelAt(support.above(2));
        return supportVoxel.filter(voxel ->
                        NavigationEvidence.isFreshStandingSupport(
                                voxel,
                                revision
                        )).isPresent()
                && feet.filter(voxel ->
                        NavigationEvidence.hasFreshTraversalClearance(
                                voxel,
                                revision
                        )).isPresent()
                && head.filter(voxel ->
                        NavigationEvidence.hasFreshTraversalClearance(
                                voxel,
                                revision
                        )).isPresent();
    }

    private static boolean verifiedCurrentEndStoneSupport(
            final CoreSkillFrame frame,
            final EndIslandIngressParameters parameters
    ) {
        return EndIslandRallyEvidence.supportsCurrentStandingCell(
                frame,
                parameters.arenaReadyRadius()
        );
    }

    private Optional<SkillFailure> validateBody(
            final CoreSkillFrame frame
    ) {
        if (!expectedPlayerId.equals(frame.playerId())) {
            return failure(NAME + ".player_mismatch");
        }
        return Optional.empty();
    }

    private long currentSessionGeneration() {
        try {
            return sessionGeneration.getAsLong();
        } catch (RuntimeException exception) {
            return -1;
        }
    }

    private void cancelChildren(final SkillContext context) {
        if (bridge != null && bridgeParameters != null) {
            bridge.cancel(context, bridgeParameters);
        }
        if (travel != null && travelParameters != null) {
            travel.cancel(context, travelParameters);
        }
        if (tower != null && towerParameters != null) {
            tower.cancel(context, towerParameters);
        }
        if (blockBreak != null && blockBreakParameters != null) {
            blockBreak.cancel(context, blockBreakParameters);
        }
        bridge = null;
        bridgeParameters = null;
        travel = null;
        travelParameters = null;
        tower = null;
        towerParameters = null;
        blockBreak = null;
        blockBreakParameters = null;
        pendingBreakBlock = null;
        blockAlignmentStartedTick = -1;
        stableGroundRecoveryStartedTick = -1;
    }

    private SkillTickResult fail(final String code) {
        return fail(SkillFailure.of(code));
    }

    private SkillTickResult fail(final SkillFailure reason) {
        quiesce();
        failure = Objects.requireNonNull(reason, "reason");
        phase = Phase.FAILED;
        bridge = null;
        bridgeParameters = null;
        travel = null;
        travelParameters = null;
        tower = null;
        towerParameters = null;
        blockBreak = null;
        blockBreakParameters = null;
        return SkillTickResult.failed(failure);
    }

    private void quiesce() {
        actuator.releaseUse();
        actuator.stop();
        if (interactionActuator != null) {
            interactionActuator.abortMining();
        }
    }

    private static Optional<SkillFailure> failure(final String code) {
        return Optional.of(SkillFailure.of(code));
    }

    private static LookIntent towardCenterFloor(
            final CoreSkillFrame frame
    ) {
        final double deltaX = EndArenaTopology.CENTER_X
                - frame.position().x();
        final double deltaZ = EndArenaTopology.CENTER_Z
                - frame.position().z();
        final double horizontal = Math.hypot(deltaX, deltaZ);
        final double scale = horizontal <= 1.0E-9
                ? 0.0
                : Math.min(4.0, horizontal) / horizontal;
        final PerceptionVec3 target = new PerceptionVec3(
                frame.position().x() + deltaX * scale,
                frame.position().y() - 0.20,
                frame.position().z() + deltaZ * scale
        );
        final PerceptionVec3 look = target.subtract(frame.eyePosition());
        final float yaw = (float) Math.toDegrees(Math.atan2(
                -look.x(),
                look.z()
        ));
        final float pitch = (float) Math.toDegrees(Math.atan2(
                -look.y(),
                Math.hypot(look.x(), look.z())
        ));
        return new LookIntent(yaw, pitch);
    }

    private static LookIntent lookAt(
            final PerceptionVec3 eye,
            final PerceptionVec3 target
    ) {
        final PerceptionVec3 delta = target.subtract(eye);
        final float yaw = (float) Math.toDegrees(Math.atan2(
                -delta.x(),
                delta.z()
        ));
        final float pitch = (float) Math.toDegrees(Math.atan2(
                -delta.y(),
                Math.hypot(delta.x(), delta.z())
        ));
        return new LookIntent(yaw, pitch);
    }

    private static double horizontalDistance(
            final PerceptionVec3 position,
            final GridPos support
    ) {
        return Math.hypot(
                position.x() - (support.x() + 0.5),
                position.z() - (support.z() + 0.5)
        );
    }

    private static double radiusOf(final GridPos position) {
        return Math.hypot(position.x() + 0.5, position.z() + 0.5);
    }

    private static GridPos grid(final BlockCoordinate block) {
        return new GridPos(block.x(), block.y(), block.z());
    }

    enum Phase {
        IDLE,
        SCANNING,
        BRIDGING_ONE_STEP,
        TOWERING_FOR_LANDFALL,
        ALIGNING_VISIBLE_BLOCK_BREAK,
        MINING_VISIBLE_END_STONE,
        TRAVELLING_TO_OBSERVED_END_STONE,
        RECOVERING_STABLE_GROUND,
        VERIFYING_CURRENT_SUPPORT,
        COMPLETED,
        FAILED,
        CANCELLED;

        private boolean active() {
            return this == SCANNING
                    || this == BRIDGING_ONE_STEP
                    || this == TOWERING_FOR_LANDFALL
                    || this == ALIGNING_VISIBLE_BLOCK_BREAK
                    || this == MINING_VISIBLE_END_STONE
                    || this == TRAVELLING_TO_OBSERVED_END_STONE
                    || this == RECOVERING_STABLE_GROUND
                    || this == VERIFYING_CURRENT_SUPPORT;
        }
    }
}
