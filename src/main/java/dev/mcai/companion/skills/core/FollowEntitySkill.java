package dev.mcai.companion.skills.core;

import dev.mcai.companion.MinecraftAiCompanion;
import dev.mcai.companion.action.ActionOutcome;
import dev.mcai.companion.navigation.LocalAStarPlanner;
import dev.mcai.companion.perception.PerceptionVec3;
import dev.mcai.companion.perception.VisibleEntity;
import dev.mcai.companion.skill.Skill;
import dev.mcai.companion.skill.SkillCheckpoint;
import dev.mcai.companion.skill.SkillContext;
import dev.mcai.companion.skill.SkillFailure;
import dev.mcai.companion.skill.SkillParameterParser;
import dev.mcai.companion.skill.SkillResult;
import dev.mcai.companion.skill.SkillTickResult;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Follows one currently observable, non-hostile entity without privileged
 * entity tracking. The target must repeatedly pass the ordinary semantic
 * distance/FOV/occlusion filter. Brief loss of sight causes a bounded visual
 * search, never hidden-position pursuit.
 */
public final class FollowEntitySkill implements Skill<FollowEntityParameters> {
    private static final double RETARGET_DISTANCE = 0.75;
    private static final int SCAN_INTERVAL_TICKS = 4;
    private static final long MAX_BINDING_SAMPLE_LAG = 512L;
    /**
     * Turning to scan is perception work, not physical follow progress. A
     * blocked local route must therefore hand control back to the planner
     * instead of rotating indefinitely in place.
     */
    private static final int MAX_PHYSICAL_STALL_TICKS = 80;
    private static final double MINIMUM_PHYSICAL_PROGRESS = 0.08;

    private final UUID expectedPlayerId;
    private final CoreSkillActuator actuator;
    private final CoreSkillFrameSource frames;
    private final LocalAStarPlanner planner;
    private final CoreSkillPolicy policy;

    private Phase phase = Phase.IDLE;
    private SkillFailure failure;
    private UUID boundEntityId;
    private MoveToSkill movement;
    private MoveToParameters movementTarget;
    private long lastObservationRevision = -1;
    private long lostSinceTick = -1;
    private long nextScanTick;
    private String lastMovementFailure = "";
    private int movementFailureCount;
    private PerceptionVec3 lastPhysicalPosition;
    private long lastPhysicalProgressTick = -1;

    public FollowEntitySkill(
            UUID expectedPlayerId,
            CoreSkillActuator actuator,
            CoreSkillFrameSource frames,
            LocalAStarPlanner planner,
            CoreSkillPolicy policy
    ) {
        this.expectedPlayerId = Objects.requireNonNull(
                expectedPlayerId,
                "expectedPlayerId"
        );
        this.actuator = Objects.requireNonNull(actuator, "actuator");
        this.frames = Objects.requireNonNull(frames, "frames");
        this.planner = Objects.requireNonNull(planner, "planner");
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    @Override
    public SkillParameterParser<FollowEntityParameters> parameters() {
        return CoreSkillParameters::parseFollowEntity;
    }

    @Override
    public Optional<SkillFailure> preconditions(
            SkillContext context,
            FollowEntityParameters parameters
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(parameters, "parameters");
        Optional<CoreSkillFrame> current = frames.current();
        if (current.isEmpty()) {
            return Optional.of(SkillFailure.of(
                    "follow_entity.observation_unavailable"
            ));
        }
        CoreSkillFrame frame = current.orElseThrow();
        if (!expectedPlayerId.equals(frame.playerId())) {
            return Optional.of(SkillFailure.of(
                    "follow_entity.player_mismatch"
            ));
        }
        Optional<VisibleEntity> target =
                resolveAuthoredTarget(frame, parameters);
        if (target.isEmpty()) {
            return Optional.of(SkillFailure.of(
                    parameters.sampleSequence()
                                    == frame.observationRevision()
                            ? "follow_entity.invalid_observation_id"
                            : "follow_entity.stale_observation_id"
            ));
        }
        if (!safeFollowTarget(target.orElseThrow())) {
            return Optional.of(SkillFailure.of(
                    "follow_entity.unsafe_target"
            ));
        }
        /*
         * Do not require the target to remain in the current camera frame
         * while a model response is in flight.  The authored sample is still
         * fair evidence: it is bounded by MAX_BINDING_SAMPLE_LAG, retained
         * by the first-person frame source, and restricted to the same
         * dimension.  If the target has left the current view, start in the
         * bounded SEARCHING phase and let the ordinary lostGraceTicks window
         * reacquire it.  This closes the real field race where a player turns
         * a corner during model latency and a valid follow command is
         * rejected before it can issue one movement input.
         */
        return Optional.empty();
    }

    @Override
    public void start(
            SkillContext context,
            FollowEntityParameters parameters
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(parameters, "parameters");
        phase = Phase.FOLLOWING;
        failure = null;
        CoreSkillFrame frame = frames.current().orElseThrow(
                () -> new IllegalStateException(
                    "Follow observation disappeared before start"
                )
        );
        final VisibleEntity authoredTarget = resolveAuthoredTarget(
                frame,
                parameters
        )
                .filter(FollowEntitySkill::safeFollowTarget)
                .orElseThrow(() -> new IllegalStateException(
                        "Follow observation changed before start"
                ));
        boundEntityId = authoredTarget.entityId();
        phase = currentlyVisibleBoundTarget(frame, authoredTarget).isPresent()
                ? Phase.FOLLOWING
                : Phase.SEARCHING;
        movement = null;
        movementTarget = null;
        lastObservationRevision = -1;
        lostSinceTick = -1;
        nextScanTick = context.gameTick();
        lastMovementFailure = "";
        movementFailureCount = 0;
        lastPhysicalPosition = frame.position();
        lastPhysicalProgressTick = context.gameTick();
    }

    @Override
    public SkillTickResult tick(
            SkillContext context,
            FollowEntityParameters parameters
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(parameters, "parameters");
        if (phase != Phase.FOLLOWING && phase != Phase.SEARCHING) {
            return SkillTickResult.failed("follow_entity.invalid_state");
        }
        try {
            return tickSafely(context, parameters);
        } catch (RuntimeException exception) {
            return fail("follow_entity.internal_failure");
        }
    }

    @Override
    public SkillCheckpoint checkpoint(
            SkillContext context,
            FollowEntityParameters parameters
    ) {
        return new SkillCheckpoint(
                1,
                String.format(
                        Locale.ROOT,
                        "{\"phase\":\"%s\",\"observationId\":\"%s\","
                                + "\"sampleSequence\":%d,"
                                + "\"followDistance\":%.3f,"
                        + "\"lostSinceTick\":%d,"
                        + "\"movementFailures\":%d,"
                                + "\"lastMovementFailure\":\"%s\","
                                + "\"physicalStallAge\":%d}",
                        phase.name(),
                        parameters.observationId(),
                        parameters.sampleSequence(),
                        parameters.followDistance(),
                        lostSinceTick,
                        movementFailureCount,
                        lastMovementFailure,
                        lastPhysicalProgressTick < 0
                                ? -1
                                : Math.max(
                                    0L,
                                    context.gameTick()
                                        - lastPhysicalProgressTick
                                )
                )
        );
    }

    @Override
    public void cancel(
            SkillContext context,
            FollowEntityParameters parameters
    ) {
        cancelMovement(context);
        frames.current().ifPresent(
                frame -> CoreSkillSafety.quiesce(actuator, frame)
        );
        phase = Phase.CANCELLED;
        boundEntityId = null;
        lastPhysicalPosition = null;
        lastPhysicalProgressTick = -1;
    }

    @Override
    public SkillResult result(
            SkillContext context,
            FollowEntityParameters parameters
    ) {
        return switch (phase) {
            case CANCELLED -> SkillResult.cancelled();
            case FAILED -> SkillResult.failed(Objects.requireNonNull(failure));
            default -> SkillResult.failed(
                    SkillFailure.of("follow_entity.invalid_state")
            );
        };
    }

    private SkillTickResult tickSafely(
            SkillContext context,
            FollowEntityParameters parameters
    ) {
        Optional<CoreSkillFrame> current = frames.current();
        if (current.isEmpty()) {
            return fail("follow_entity.observation_unavailable");
        }
        CoreSkillFrame frame = current.orElseThrow();
        if (!expectedPlayerId.equals(frame.playerId())) {
            return fail("follow_entity.player_mismatch");
        }
        if (frame.observationRevision() < lastObservationRevision) {
            return fail("follow_entity.stale_observation");
        }

        Optional<VisibleEntity> visible = visibleTarget(frame);
        if (visible.isEmpty()) {
            return search(context, parameters, frame);
        }
        final boolean reacquired = phase == Phase.SEARCHING;
        if (reacquired) {
            /* Do not carry pre-loss stall age into a newly reacquired route. */
            lastPhysicalPosition = frame.position();
            lastPhysicalProgressTick = context.gameTick();
        }
        VisibleEntity target = visible.orElseThrow();
        if (!safeFollowTarget(target)) {
            return fail("follow_entity.unsafe_target");
        }

        phase = Phase.FOLLOWING;
        lostSinceTick = -1;
        if (lastPhysicalPosition == null) {
            lastPhysicalPosition = frame.position();
            lastPhysicalProgressTick = context.gameTick();
        } else if (frame.position().subtract(lastPhysicalPosition).length()
                >= MINIMUM_PHYSICAL_PROGRESS) {
            lastPhysicalPosition = frame.position();
            lastPhysicalProgressTick = context.gameTick();
        }
        PerceptionVec3 targetPosition = target.position();
        boolean freshTarget = frame.observationRevision()
                > lastObservationRevision;
        lastObservationRevision = frame.observationRevision();

        double distance = frame.position()
                .subtract(targetPosition)
                .length();
        if (distance <= parameters.followDistance()) {
            cancelMovement(context);
            if (!actuator.stop().accepted()) {
                return fail("follow_entity.actuator_rejected");
            }
            if (!aimAt(frame, targetPosition)) {
                return fail("follow_entity.actuator_rejected");
            }
            // Maintaining the requested moving-distance invariant is useful
            // progress for a deliberately long-running follow skill.
            return SkillTickResult.running(true, true);
        }

        if (context.gameTick() - lastPhysicalProgressTick
                >= MAX_PHYSICAL_STALL_TICKS) {
            cancelMovement(context);
            actuator.stop();
            return fail("follow_entity.no_physical_progress");
        }

        if (context.hardcore()
                && frame.danger() > policy.hardcoreMaximumDanger()) {
            cancelMovement(context);
            actuator.stop();
            return SkillTickResult.running(true, true);
        }

        boolean targetMoved = movementTarget == null
                || movementTarget.target()
                .subtract(targetPosition)
                .length() >= RETARGET_DISTANCE;
        if (movement == null || (freshTarget && targetMoved)) {
            cancelMovement(context);
            movementTarget = new MoveToParameters(
                    frame.dimension(),
                    targetPosition.x(),
                    targetPosition.y(),
                    targetPosition.z(),
                    parameters.followDistance()
            );
            movement = new MoveToSkill(
                    expectedPlayerId,
                    actuator,
                    frames,
                    planner,
                    policy
            );
            Optional<SkillFailure> rejected = movement.preconditions(
                    context,
                    movementTarget
            );
            if (rejected.isPresent()) {
                movement = null;
                movementTarget = null;
                actuator.stop();
                return SkillTickResult.running(freshTarget, true);
            }
            movement.start(context, movementTarget);
        }

        SkillTickResult result = movement.tick(context, movementTarget);
        if (result.status() == SkillTickResult.Status.COMPLETED) {
            movement = null;
            movementTarget = null;
            lastMovementFailure = "";
            movementFailureCount = 0;
            return SkillTickResult.running(true, true);
        }
        if (result.status() == SkillTickResult.Status.FAILED) {
            lastMovementFailure = result.failure()
                    .map(SkillFailure::code)
                    .orElse("move_to.invalid_tick_result");
            movementFailureCount++;
            if (Boolean.getBoolean("mcai.liveModelTest")
                    && (movementFailureCount == 1
                        || movementFailureCount % 20 == 0)) {
                MinecraftAiCompanion.LOGGER.info(
                        "Live follow local movement attempt failed: "
                            + "code={}, attempts={}, observation={}, "
                            + "navigation={}, feet={}",
                        lastMovementFailure,
                        movementFailureCount,
                        frame.observationRevision(),
                        frame.navigation().revision(),
                        frame.feet()
                );
            }
            movement = null;
            movementTarget = null;
            actuator.stop();
            // A target can remain visible beyond the currently mapped local
            // corridor. Let subsequent head scans grow fair perception rather
            // than converting that into privileged direct pursuit.
            return SkillTickResult.running(false, true);
        }
        return result;
    }

    private SkillTickResult search(
            SkillContext context,
            FollowEntityParameters parameters,
            CoreSkillFrame frame
    ) {
        cancelMovement(context);
        if (!actuator.stop().accepted()) {
            return fail("follow_entity.actuator_rejected");
        }
        if (lostSinceTick < 0) {
            lostSinceTick = context.gameTick();
        }
        if (context.gameTick() - lostSinceTick
                >= parameters.lostGraceTicks()) {
            return fail("follow_entity.target_lost");
        }
        phase = Phase.SEARCHING;
        if (context.gameTick() < nextScanTick) {
            return SkillTickResult.running(false, true);
        }
        ActionOutcome scanned = actuator.look(
                CoreSkillGeometry.scanTarget(frame, policy.scanYawDegrees())
        );
        if (!scanned.accepted()) {
            return fail("follow_entity.actuator_rejected");
        }
        nextScanTick = context.gameTick() + SCAN_INTERVAL_TICKS;
        return SkillTickResult.running(true, true);
    }

    private void cancelMovement(SkillContext context) {
        if (movement != null && movementTarget != null) {
            movement.cancel(context, movementTarget);
        }
        movement = null;
        movementTarget = null;
    }

    private boolean aimAt(
            CoreSkillFrame frame,
            PerceptionVec3 targetPosition
    ) {
        PerceptionVec3 targetCenter = new PerceptionVec3(
                targetPosition.x(),
                targetPosition.y() + 1.0,
                targetPosition.z()
        );
        if (targetCenter.subtract(frame.eyePosition()).lengthSquared()
                > 1.0E-12) {
            return actuator.look(CoreSkillGeometry.lookAt(
                    frame.eyePosition(),
                    targetCenter
            )).accepted();
        }
        return true;
    }

    private SkillTickResult fail(String code) {
        frames.current().ifPresent(
                frame -> CoreSkillSafety.quiesce(actuator, frame)
        );
        failure = SkillFailure.of(code);
        phase = Phase.FAILED;
        boundEntityId = null;
        movement = null;
        movementTarget = null;
        lastPhysicalPosition = null;
        lastPhysicalProgressTick = -1;
        return SkillTickResult.failed(failure);
    }

    private Optional<VisibleEntity> visibleTarget(
            CoreSkillFrame frame
    ) {
        if (boundEntityId == null) {
            return Optional.empty();
        }
        return frame.visibleEntities().stream()
                .filter(entity -> entity.entityId().equals(boundEntityId))
                .findFirst();
    }

    /**
     * Resolves the exact fair sample the model named while allowing the
     * perception loop to continue publishing during network latency. The
     * model still never receives an entity UUID: the server privately maps
     * the old public observation index to a UUID, then requires that same
     * entity to be visible in the current frame before granting movement.
     */
    private Optional<VisibleEntity> resolveAuthoredTarget(
            final CoreSkillFrame current,
            final FollowEntityParameters parameters
    ) {
        if (current.observationRevision()
                    < parameters.sampleSequence()
                || current.observationRevision()
                    - parameters.sampleSequence()
                    > MAX_BINDING_SAMPLE_LAG) {
            return Optional.empty();
        }
        return frames.visibleEntityAtObservation(
                        parameters.sampleSequence(),
                        parameters.observationIndex()
                )
                .filter(binding -> binding.dimension().equals(
                        current.dimension()
                ))
                .map(CoreSkillFrameSource.VisibleEntityBinding::entity);
    }

    private static Optional<VisibleEntity>
            currentlyVisibleBoundTarget(
                    final CoreSkillFrame current,
                    final VisibleEntity authoredTarget
            ) {
        return current.visibleEntities().stream()
                .filter(entity -> entity.entityId().equals(
                        authoredTarget.entityId()
                ))
                .filter(FollowEntitySkill::safeFollowTarget)
                .findFirst();
    }

    private static boolean safeFollowTarget(VisibleEntity target) {
        return !target.hostile() && !target.projectile();
    }

    private enum Phase {
        IDLE,
        FOLLOWING,
        SEARCHING,
        CANCELLED,
        FAILED
    }
}
