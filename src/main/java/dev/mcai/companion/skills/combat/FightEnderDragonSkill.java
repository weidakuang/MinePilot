package dev.mcai.companion.skills.combat;

import dev.mcai.companion.action.ActionHand;
import dev.mcai.companion.action.ActionMath;
import dev.mcai.companion.action.ActionOutcome;
import dev.mcai.companion.action.BlockFace;
import dev.mcai.companion.action.LookIntent;
import dev.mcai.companion.action.MovementIntent;
import dev.mcai.companion.navigation.GridPos;
import dev.mcai.companion.perception.InventoryItemSummary;
import dev.mcai.companion.perception.DangerKind;
import dev.mcai.companion.perception.PerceptionVec3;
import dev.mcai.companion.perception.PerceptionProvenance;
import dev.mcai.companion.perception.VisibleBlockFace;
import dev.mcai.companion.perception.VisibleEntity;
import dev.mcai.companion.skill.Skill;
import dev.mcai.companion.skill.SkillCheckpoint;
import dev.mcai.companion.skill.SkillContext;
import dev.mcai.companion.skill.SkillFailure;
import dev.mcai.companion.skill.SkillParameterParser;
import dev.mcai.companion.skill.SkillResult;
import dev.mcai.companion.skill.SkillTickResult;
import dev.mcai.companion.skills.bridging.BridgeMaterialActuator;
import dev.mcai.companion.skills.bridging.BridgeMaterialResult;
import dev.mcai.companion.skills.bridging.TowerUpParameters;
import dev.mcai.companion.skills.bridging.TowerUpSkill;
import dev.mcai.companion.skills.bridging.WaterClutchDescendParameters;
import dev.mcai.companion.skills.bridging.WaterClutchDescendSkill;
import dev.mcai.companion.skills.core.CoreSkillActuator;
import dev.mcai.companion.skills.core.CoreSkillFrame;
import dev.mcai.companion.skills.core.CoreSkillFrameSource;
import dev.mcai.companion.skills.core.MoveToParameters;
import dev.mcai.companion.skills.core.MoveToSkill;
import dev.mcai.companion.skills.core.TravelToParameters;
import dev.mcai.companion.skills.core.TravelToSkill;
import dev.mcai.companion.skills.interaction.BreakBlockParameters;
import dev.mcai.companion.skills.interaction.BreakBlockSkill;
import dev.mcai.companion.skills.interaction.InteractionSkillActuator;
import dev.mcai.companion.skills.interaction.InteractionSkillFrame;
import dev.mcai.companion.skills.interaction.InteractionSkillFrameSource;
import dev.mcai.companion.skills.interaction.InteractionSkillPolicy;
import dev.mcai.companion.skills.interaction.ObservedBlockTarget;
import dev.mcai.companion.skills.inventory.EquipItemParameters;
import dev.mcai.companion.skills.inventory.EquipmentTarget;
import dev.mcai.companion.skills.inventory.InventoryOperationResult;
import dev.mcai.companion.skills.inventory.InventorySkillActuator;
import dev.mcai.companion.waypoint.DimensionRef;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Bounded End-fight coordination driven only by the player's current semantic
 * view. Every projectile is delegated to the normal ranged combat skill and
 * victory is accepted only from the companion-attributed dragon-death event.
 */
public final class FightEnderDragonSkill
        implements Skill<FightEnderDragonParameters> {
    public static final String NAME = "fight_ender_dragon";

    private static final String BOW = "minecraft:bow";
    private static final String ARROW = "minecraft:arrow";
    private static final String END_CRYSTAL =
            "minecraft:end_crystal";
    private static final String ENDER_DRAGON =
            "minecraft:ender_dragon";
    private static final String IRON_BARS =
            "minecraft:iron_bars";
    private static final int SCAN_INTERVAL_TICKS = 3;
    private static final int SCANS_BEFORE_RALLY = 48;
    private static final int MAXIMUM_RALLY_ATTEMPTS = 4;
    /* A full bow flight at the maximum observed dragon distance is shorter
     * than this; leave enough time for the vanilla projectile to advance
     * before reacquiring a target without idling a whole second. */
    private static final int PROJECTILE_SETTLE_TICKS = 8;
    private static final int CAGE_SCANS_BEFORE_RALLY = 12;
    private static final int CAGE_DESCENT_SCAN_LIMIT = 12;
    private static final int MAXIMUM_CAGE_APPROACH_ATTEMPTS = 2;
    private static final int MAXIMUM_CAGE_BARS_MINED = 12;
    private static final int CRYSTAL_CAGE_INSPECTION_SCANS = 12;
    private static final int MAXIMUM_CRYSTAL_STANDOFF_SCANS = 12;
    private static final int MAXIMUM_CRYSTAL_STANDOFF_ATTEMPTS = 8;
    private static final int MAXIMUM_CRYSTAL_LANE_ATTEMPTS = 3;
    private static final int MAXIMUM_CRYSTAL_LANE_SCANS = 8;
    private static final double CAGE_LINE_RADIUS = 1.15;
    private static final double CAGE_BREAK_REACH = 4.5;
    private static final double CAGE_MINING_ALIGNMENT_DEGREES = 1.5;
    private static final double MELEE_DRAGON_DISTANCE = 5.75;
    private static final double MELEE_ALIGNMENT_DEGREES = 5.0;
    private static final double MELEE_ATTACK_STRENGTH = 0.90;
    /*
     * A vanilla player can land many swings on a dragon part while the
     * attack cooldown and the dragon's damage immunity are out of phase.
     * Staying pressed against one tail/wing indefinitely is nevertheless a
     * poor recovery strategy: it leaves no room for the ordinary bow path
     * and makes a multipart target look like a frozen conversation.  After
     * this bounded local melee burst, keep using the currently visible
     * dragon and normal arrows until it is dead.  This is a combat policy,
     * not a health/world shortcut; every shot still goes through the
     * first-person crosshair and vanilla projectile interaction checks.
     */
    private static final int MELEE_ATTACKS_BEFORE_RANGED = 24;
    private static final int DEFENSIVE_DODGE_COOLDOWN_TICKS = 6;
    /*
     * Keep the first-person retreat inside the observed dragon arena.  The
     * movement actuator advances roughly 1.2 blocks per tick; twenty-four
     * ticks therefore sent the body almost thirty blocks away, where the
     * dragon could occlude every crystal lane and heal indefinitely.  Eight
     * ticks gives a normal melee-to-bow separation of about ten blocks while
     * retaining vanilla collision and hazard handling.
     */
    private static final int DRAGON_RANGED_RETREAT_TICKS = 8;
    private static final double IMMEDIATE_DRAGON_DISTANCE = 8.75;
    private static final int MELEE_REACH_MISSES_BEFORE_RANGED = 3;
    private static final int RANGED_SHOTS_BEFORE_MELEE_RETRY = 4;
    private static final List<String> MELEE_WEAPONS = List.of(
            "minecraft:netherite_sword",
            "minecraft:diamond_sword",
            "minecraft:iron_sword",
            "minecraft:stone_sword",
            "minecraft:netherite_axe",
            "minecraft:diamond_axe",
            "minecraft:iron_axe",
            "minecraft:stone_axe"
    );
    private static final List<String> CAGE_MINING_TOOLS = List.of(
            "minecraft:netherite_pickaxe",
            "minecraft:diamond_pickaxe",
            "minecraft:iron_pickaxe",
            "minecraft:stone_pickaxe",
            "minecraft:golden_pickaxe",
            "minecraft:wooden_pickaxe"
    );
    private static final float[] SCAN_PITCHES = {
        -15.0F,
        -35.0F,
        -60.0F,
        8.0F
    };

    private final UUID expectedPlayerId;
    private final CoreSkillActuator core;
    private final CoreSkillFrameSource coreFrames;
    private final InteractionSkillActuator interactions;
    private final InteractionSkillFrameSource interactionFrames;
    private final InventorySkillActuator inventory;
    private final BridgeMaterialActuator bridgeMaterials;
    private final DragonVictorySource victory;
    private final LongSupplier sessionGeneration;

    private Phase phase = Phase.IDLE;
    private SkillFailure failure;
    private long startedAtTick = -1;
    private long nextActionTick = -1;
    private long lastDefensiveDodgeTick = -1;
    private long lastObservationRevision = -1;
    private long boundSessionGeneration = -1;
    private int shotsDispatched;
    private int meleeAttacks;
    private int meleeReachMisses;
    private int rangedDragonShotsSinceMeleeRetry;
    private int dragonRetreatTicksRemaining;
    private int scanTurns;
    private int rallyAttempts;
    private int cageScanTurns;
    private int cageBarsMined;
    private int cageApproachAttempts;
    private int cageDescentScans;
    private int cageLandingScans;
    private float scanBaseYaw;
    /**
     * Last fair direction from a recent damage/contact cue.  A dodge turns
     * the camera away from the attacker; preserving this direction lets the
     * next bounded scan reacquire that same visible threat instead of
     * repeatedly sweeping the opposite hemisphere.
     */
    private PerceptionVec3 lastThreatDirection;
    private PerceptionVec3 localRallyPoint;
    private boolean recoveringSafetyReserve;
    private boolean dragonRangedMode;
    private PerceptionVec3 dragonRetreatDirection;
    private boolean sawCageBarBeyondReach;
    private boolean cageTowered;
    private boolean cageLandingVerified;
    private long cageLandingVerifiedRevision = -1;
    private int crystalStandOffScans;
    private int crystalStandOffAttempts;
    private int crystalCageInspectionTurns;
    private int crystalLaneAttempts;
    private int crystalLaneScanTurns;
    private CageStatus cageStatus = CageStatus.NONE;
    private TravelPurpose travelPurpose = TravelPurpose.NONE;
    private UUID cageCrystalId;
    private PerceptionVec3 cageLastSeenPosition;
    private PerceptionVec3 crystalStandOffPosition;
    private CagedCrystalTraversalPlanner.Plan cagePlan;
    private ShootObservedEntitySkill shot;
    private ShootObservedEntityParameters shotParameters;
    private boolean shotTargetDragon;
    private TravelToSkill travel;
    private TravelToParameters travelParameters;
    private MoveToSkill crystalStandOff;
    private MoveToParameters crystalStandOffParameters;
    private MoveToSkill crystalLane;
    private MoveToParameters crystalLaneParameters;
    private BreakBlockSkill cageBreak;
    private BreakBlockParameters cageBreakParameters;
    private TowerUpSkill cageTower;
    private TowerUpParameters cageTowerParameters;
    private WaterClutchDescendSkill cageDescent;
    private WaterClutchDescendParameters cageDescentParameters;

    public FightEnderDragonSkill(
            final UUID expectedPlayerId,
            final CoreSkillActuator core,
            final CoreSkillFrameSource coreFrames,
            final InteractionSkillActuator interactions,
            final InteractionSkillFrameSource interactionFrames,
            final InventorySkillActuator inventory,
            final BridgeMaterialActuator bridgeMaterials,
            final DragonVictorySource victory,
            final LongSupplier sessionGeneration
    ) {
        this.expectedPlayerId = Objects.requireNonNull(
                expectedPlayerId,
                "expectedPlayerId"
        );
        this.core = Objects.requireNonNull(core, "core");
        this.coreFrames = Objects.requireNonNull(
                coreFrames,
                "coreFrames"
        );
        this.interactions = Objects.requireNonNull(
                interactions,
                "interactions"
        );
        this.interactionFrames = Objects.requireNonNull(
                interactionFrames,
                "interactionFrames"
        );
        this.inventory = Objects.requireNonNull(
                inventory,
                "inventory"
        );
        this.bridgeMaterials = Objects.requireNonNull(
                bridgeMaterials,
                "bridgeMaterials"
        );
        this.victory = Objects.requireNonNull(victory, "victory");
        this.sessionGeneration = Objects.requireNonNull(
                sessionGeneration,
                "sessionGeneration"
        );
    }

    @Override
    public SkillParameterParser<FightEnderDragonParameters> parameters() {
        return DragonCombatSkillParameters::parse;
    }

    @Override
    public boolean allowsWorldRevisionTransition() {
        /*
         * Dragon death is a server-verified completion boundary.  The skill
         * must receive one more ordinary tick to observe victory and finish;
         * invalidating its bound epoch first would turn a real kill into a
         * stale-world failure.
         */
        return true;
    }

    @Override
    public boolean managesVisibleHostileProximity() {
        return phase != Phase.IDLE
                && phase != Phase.COMPLETED
                && phase != Phase.CANCELLED
                && phase != Phase.FAILED
                && !recoveringSafetyReserve;
    }

    @Override
    public boolean managesVisibleProjectileThreats() {
        return phase != Phase.IDLE
                && phase != Phase.COMPLETED
                && phase != Phase.CANCELLED
                && phase != Phase.FAILED
                && !recoveringSafetyReserve;
    }

    @Override
    public boolean managesPhysicalContactThreats() {
        return phase != Phase.IDLE
                && phase != Phase.COMPLETED
                && phase != Phase.CANCELLED
                && phase != Phase.FAILED
                && !recoveringSafetyReserve;
    }

    @Override
    public OptionalDouble hardcoreRiskThresholdOverride(
            final SkillContext context,
            final FightEnderDragonParameters parameters
    ) {
        final Optional<CoreSkillFrame> frame = coreFrames.current()
                .filter(current ->
                        expectedPlayerId.equals(current.playerId())
                );
        return frame.isEmpty()
                ? OptionalDouble.empty()
                : CombatHardcoreRisk.threshold(
                        context,
                        frame.orElseThrow(),
                        1.0
                );
    }

    @Override
    public Optional<SkillFailure> preconditions(
            final SkillContext context,
            final FightEnderDragonParameters parameters
    ) {
        final SnapshotValidation validation =
                validateSnapshot(parameters);
        if (validation.failure().isPresent()) {
            return validation.failure();
        }
        final CoreSkillFrame frame = validation.snapshot()
                .orElseThrow()
                .core();
        if (!DimensionRef.END.equals(parameters.dimension())) {
            return Optional.of(SkillFailure.of(
                    NAME + ".end_dimension_required"
            ));
        }
        if (healthTooLow(context, frame)) {
            return Optional.of(SkillFailure.of(
                    NAME + ".health_reserve_required"
            ));
        }
        if (frame.foodLevel() < (context.hardcore() ? 10 : 4)) {
            return Optional.of(SkillFailure.of(
                    NAME + ".food_reserve_required"
            ));
        }
        if (inventoryCount(frame, BOW) < 1) {
            return Optional.of(SkillFailure.of(
                    NAME + ".bow_required"
            ));
        }
        if (inventoryCount(frame, ARROW) < 1) {
            return Optional.of(SkillFailure.of(
                    NAME + ".arrows_required"
            ));
        }
        if (!frame.onGround() || frame.inWater()) {
            return Optional.of(SkillFailure.of(
                    NAME + ".stable_local_rally_required"
            ));
        }
        return Optional.empty();
    }

    @Override
    public void start(
            final SkillContext context,
            final FightEnderDragonParameters parameters
    ) {
        final Snapshot snapshot = validateSnapshot(parameters)
                .snapshot()
                .orElseThrow(() -> new IllegalStateException(
                        "Dragon-fight body changed before start"
                ));
        phase = Phase.SEARCHING;
        failure = null;
        startedAtTick = context.gameTick();
        nextActionTick = context.gameTick();
        lastDefensiveDodgeTick = -1;
        lastObservationRevision = -1;
        boundSessionGeneration =
                snapshot.interaction().sessionGeneration();
        shotsDispatched = 0;
        meleeAttacks = 0;
        meleeReachMisses = 0;
        rangedDragonShotsSinceMeleeRetry = 0;
        dragonRetreatTicksRemaining = 0;
        scanTurns = 0;
        rallyAttempts = 0;
        cageScanTurns = 0;
        cageBarsMined = 0;
        cageApproachAttempts = 0;
        cageDescentScans = 0;
        cageLandingScans = 0;
        scanBaseYaw = lookYaw(snapshot.core());
        lastThreatDirection = null;
        /*
         * This exact pose belongs to the live authoritative body and proves a
         * reachable standing point. It replaces the old model-supplied rally
         * coordinates, which could be invented or stale.
         */
        localRallyPoint = snapshot.core().position();
        recoveringSafetyReserve = false;
        dragonRangedMode = false;
        dragonRetreatDirection = null;
        sawCageBarBeyondReach = false;
        cageTowered = false;
        cageLandingVerified = false;
        cageLandingVerifiedRevision = -1;
        crystalStandOffScans = 0;
        crystalStandOffAttempts = 0;
        crystalCageInspectionTurns = 0;
        crystalLaneAttempts = 0;
        crystalLaneScanTurns = 0;
        cageStatus = CageStatus.NONE;
        travelPurpose = TravelPurpose.NONE;
        cageCrystalId = null;
        cageLastSeenPosition = null;
        crystalStandOffPosition = null;
        cagePlan = null;
        shot = null;
        shotParameters = null;
        shotTargetDragon = false;
        travel = null;
        travelParameters = null;
        crystalStandOff = null;
        crystalStandOffParameters = null;
        crystalLane = null;
        crystalLaneParameters = null;
        cageBreak = null;
        cageBreakParameters = null;
        cageTower = null;
        cageTowerParameters = null;
        cageDescent = null;
        cageDescentParameters = null;
    }

    @Override
    public SkillTickResult tick(
            final SkillContext context,
            final FightEnderDragonParameters parameters
    ) {
        if (!phase.active()) {
            return SkillTickResult.failed(NAME + ".invalid_state");
        }
        try {
            return tickSafely(context, parameters);
        } catch (RuntimeException exception) {
            return fail(context, NAME + ".internal_failure");
        }
    }

    @Override
    public SkillCheckpoint checkpoint(
            final SkillContext context,
            final FightEnderDragonParameters parameters
    ) {
        return new SkillCheckpoint(
                1,
                String.format(
                        Locale.ROOT,
                        "{\"phase\":\"%s\",\"shots\":%d,\"melee\":%d,"
                            + "\"meleeReachMisses\":%d,"
                            + "\"rangedDragonShotsSinceMeleeRetry\":%d,"
                            + "\"dragonRangedMode\":%s,"
                            + "\"dragonRetreatTicksRemaining\":%d,"
                            + "\"scanTurns\":%d,\"rallyAttempts\":%d,"
                            + "\"cageBarsMined\":%d,"
                            + "\"cageApproaches\":%d,"
                            + "\"cageRecoveryTurns\":%d,"
                            + "\"cageTowerBlocks\":%d,"
                            + "\"cageLandingVerified\":%s,"
                            + "\"crystalStandOffScans\":%d,"
                            + "\"crystalStandOffAttempts\":%d,"
                            + "\"crystalCageInspectionTurns\":%d,"
                            + "\"crystalLaneAttempts\":%d,"
                            + "\"crystalLaneScanTurns\":%d,"
                            + "\"recoveringSafetyReserve\":%s,"
                            + "\"cageStatus\":\"%s\","
                            + "\"visibleEntityTypes\":\"%s\","
                            + "\"bodyPosition\":\"%s\"}",
                        phase.name(),
                        shotsDispatched,
                        meleeAttacks,
                        meleeReachMisses,
                        rangedDragonShotsSinceMeleeRetry,
                        dragonRangedMode,
                        dragonRetreatTicksRemaining,
                        scanTurns,
                        rallyAttempts,
                        cageBarsMined,
                        cageApproachAttempts,
                        cageScanTurns,
                        cagePlan == null
                            ? 0
                            : cagePlan.towerBlocks(),
                        cageLandingVerified,
                        crystalStandOffScans,
                        crystalStandOffAttempts,
                        crystalCageInspectionTurns,
                        crystalLaneAttempts,
                        crystalLaneScanTurns,
                        recoveringSafetyReserve,
                        cageStatus.name(),
                        visibleEntityTypes(),
                        currentBodyPosition()
                )
        );
    }

    private String visibleEntityTypes() {
        return coreFrames.current()
                .map(frame -> frame.visibleEntities().stream()
                        .limit(12)
                        .map(entity -> entity.entityTypeId())
                        .distinct()
                        .sorted()
                        .reduce((left, right) -> left + "|" + right)
                        .orElse(""))
                .orElse("");
    }

    private String currentBodyPosition() {
        return coreFrames.current()
                .map(frame -> String.format(
                        Locale.ROOT,
                        "%.1f,%.1f,%.1f",
                        frame.position().x(),
                        frame.position().y(),
                        frame.position().z()
                ))
                .orElse("");
    }

    @Override
    public void cancel(
            final SkillContext context,
            final FightEnderDragonParameters parameters
    ) {
        cancelChildren(context);
        quiesce();
        phase = Phase.CANCELLED;
    }

    @Override
    public SkillResult result(
            final SkillContext context,
            final FightEnderDragonParameters parameters
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
            final FightEnderDragonParameters parameters
    ) {
        if (victory.dragonKilled(context.goalRevision())) {
            cancelChildren(context);
            quiesce();
            phase = Phase.COMPLETED;
            return SkillTickResult.completed();
        }
        if (context.gameTick() - startedAtTick
                >= parameters.timeoutTicks()) {
            return fail(context, NAME + ".timed_out");
        }
        final SnapshotValidation validation =
                validateSnapshot(parameters);
        if (validation.failure().isPresent()) {
            return fail(
                    context,
                    validation.failure().orElseThrow()
            );
        }
        final Snapshot snapshot =
                validation.snapshot().orElseThrow();
        final CoreSkillFrame frame = snapshot.core();
        if (frame.observationRevision() < lastObservationRevision) {
            return fail(context, NAME + ".stale_observation");
        }
        final boolean fresh = frame.observationRevision()
                > lastObservationRevision;
        lastObservationRevision = Math.max(
                lastObservationRevision,
                frame.observationRevision()
        );
        final Optional<SkillTickResult> projectileResponse =
                evadeVisibleProjectile(context, frame, fresh);
        if (projectileResponse.isPresent()) {
            return projectileResponse.orElseThrow();
        }
        final Optional<SkillTickResult> damageResponse =
                evadeRecentDamage(context, frame);
        if (damageResponse.isPresent()) {
            return damageResponse.orElseThrow();
        }
        if (phase == Phase.OPENING_CAGE
                && immediateDragonIndex(frame).isPresent()) {
            abortCageBreak(context);
            phase = Phase.SEARCHING;
            cageStatus = CageStatus.SEEKING_VISIBLE_BAR;
            nextActionTick = context.gameTick();
        }
        if (healthTooLow(context, frame)
                || frame.foodLevel()
                    < (context.hardcore() ? 6 : 2)) {
            recoveringSafetyReserve = true;
            if (!core.stop().accepted()) {
                return fail(
                        context,
                        NAME + ".recovery_stop_rejected"
                );
            }
            /*
             * Survival control executes after the skill lease. Relinquishing
             * hostile-proximity ownership here lets it eat, guard, or evade
             * at 20 TPS; the dragon coordinator keeps its bounded deadline
             * and resumes only when both reserves are safe again.
             */
            return SkillTickResult.running(fresh, true);
        }
        recoveringSafetyReserve = false;
        return switch (phase) {
            case SEARCHING -> search(
                    context,
                    parameters,
                    snapshot,
                    fresh
            );
            case SHOOTING -> tickShot(context, fresh);
            case RETREATING_DRAGON -> tickDragonRangedRetreat(
                    context,
                    frame,
                    fresh
            );
            case REPOSITIONING_CRYSTAL ->
                    tickCrystalStandOff(
                            context,
                            parameters,
                            fresh
                    );
            case REPOSITIONING_CRYSTAL_LANE ->
                    tickCrystalLane(context, fresh);
            case TRAVELLING -> tickTravel(context, fresh);
            case OPENING_CAGE -> tickCageBreak(
                    context,
                    fresh
            );
            case TOWERING_CAGE -> tickCageTower(
                    context,
                    parameters,
                    fresh
            );
            case PREPARING_CAGE_TOWER ->
                    prepareCageTower(
                            context,
                            parameters,
                            frame,
                            fresh
                    );
            case PREPARING_CAGE_DESCENT ->
                    prepareCageDescent(
                            context,
                            frame,
                            fresh
                    );
            case DESCENDING_CAGE -> tickCageDescent(
                    context,
                    fresh
            );
            default -> SkillTickResult.failed(
                    NAME + ".invalid_state"
            );
        };
    }

    /**
     * Dragon fireballs are fair first-person projectile observations, not a
     * reason for the emergency lane to freeze the entire fight.  On a fresh
     * visible projectile signal, turn away and issue one bounded sprint/strafe
     * input.  If the projectile is not in the current semantic list, the
     * directionless danger signal still triggers a short alternating strafe;
     * no hidden projectile position or world scan is used.
     */
    private Optional<SkillTickResult> evadeVisibleProjectile(
            final SkillContext context,
            final CoreSkillFrame frame,
            final boolean fresh
    ) {
        final Optional<VisibleEntity> projectile = frame.visibleEntities()
                .stream()
                .filter(VisibleEntity::projectile)
                .filter(FightEnderDragonSkill::projectileThreatensBody)
                .min(Comparator.comparingDouble(VisibleEntity::distance));
        final boolean danger = frame.dangerSignals().stream().anyMatch(
                signal -> signal.kind()
                        == dev.mcai.companion.perception.DangerKind
                            .PROJECTILE_PROXIMITY
                    && signal.severity() >= 0.35
        );
        if (projectile.isEmpty() && !danger) {
            return Optional.empty();
        }
        if (lastDefensiveDodgeTick >= 0
                && context.gameTick() - lastDefensiveDodgeTick
                    < DEFENSIVE_DODGE_COOLDOWN_TICKS) {
            return Optional.empty();
        }
        final PerceptionVec3 away = projectile
                .map(entity -> frame.position().subtract(entity.position()))
                .filter(vector -> vector.lengthSquared() > 1.0E-12)
                .orElseGet(() -> new PerceptionVec3(
                        ((frame.gameTime() / 6L) & 1L) == 0L ? 1.0 : -1.0,
                        0.0,
                        0.35
                ));
        final LookIntent look = lookAt(
                frame.eyePosition(),
                frame.eyePosition().add(away)
        );
        if (!core.look(look).accepted()) {
            return Optional.of(fail(
                    context,
                    NAME + ".projectile_dodge_look_rejected"
            ));
        }
        final PerceptionVec3 horizontal = new PerceptionVec3(
                away.x(),
                0.0,
                away.z()
        );
        final PerceptionVec3 desired = horizontal.lengthSquared() > 1.0E-12
                ? horizontal.normalized()
                : new PerceptionVec3(1.0, 0.0, 0.0);
        final PerceptionVec3 horizontalForward = new PerceptionVec3(
                frame.lookDirection().x(),
                0.0,
                frame.lookDirection().z()
        );
        final PerceptionVec3 forward = horizontalForward.lengthSquared()
                > 1.0E-12
                ? horizontalForward.normalized()
                : new PerceptionVec3(0.0, 0.0, 1.0);
        final PerceptionVec3 left = new PerceptionVec3(
                forward.z(),
                0.0,
                -forward.x()
        );
        final ActionOutcome moved = core.move(new MovementIntent(
                desired.dot(forward),
                desired.dot(left),
                true,
                false
        ));
        if (!moved.accepted()) {
            return Optional.of(fail(
                    context,
                    NAME + ".projectile_dodge_move_rejected"
            ));
        }
        lastDefensiveDodgeTick = context.gameTick();
        nextActionTick = context.gameTick() + 1;
        return Optional.of(SkillTickResult.running(true, true));
    }

    /**
     * Dragon breath and multipart contact can arrive as a fair recent-damage
     * cue without a projectile entity in the current semantic sample.  Keep
     * the fight alive with one bounded first-person side/back step, then let
     * the ordinary combat phase resume; this never selects an unseen target
     * or edits the world.
     */
    private Optional<SkillTickResult> evadeRecentDamage(
            final SkillContext context,
            final CoreSkillFrame frame
    ) {
        final Optional<dev.mcai.companion.perception.DangerSignal> damage =
                frame.dangerSignals().stream()
                        .filter(signal ->
                                signal.kind() == DangerKind.THREAT_CONTACT)
                        .filter(signal ->
                                signal.provenance()
                                        == PerceptionProvenance
                                                .RECENT_DAMAGE_EVENT
                                || signal.provenance()
                                        == PerceptionProvenance
                                                .PHYSICAL_CONTACT)
                        .filter(signal -> signal.severity() >= 0.35)
                        .max(Comparator.comparingDouble(
                                dev.mcai.companion.perception.DangerSignal
                                        ::severity
                        ));
        if (damage.isEmpty()) {
            return Optional.empty();
        }
        if (lastDefensiveDodgeTick >= 0
                && context.gameTick() - lastDefensiveDodgeTick
                    < DEFENSIVE_DODGE_COOLDOWN_TICKS) {
            return Optional.empty();
        }
        final PerceptionVec3 toward = damage.orElseThrow()
                .contactDirection()
                .filter(vector -> vector.lengthSquared() > 1.0E-12)
                .orElseGet(() -> new PerceptionVec3(
                        ((context.gameTick() / DEFENSIVE_DODGE_COOLDOWN_TICKS)
                                & 1L) == 0L ? 1.0 : -1.0,
                        0.0,
                        0.35
                ));
        final PerceptionVec3 horizontalToward = new PerceptionVec3(
                toward.x(),
                0.0,
                toward.z()
        );
        if (horizontalToward.lengthSquared() > 1.0E-12) {
            lastThreatDirection = horizontalToward.normalized();
            scanBaseYaw = yawFromDirection(lastThreatDirection);
        }
        final PerceptionVec3 horizontalAway = new PerceptionVec3(
                -toward.x(),
                0.0,
                -toward.z()
        );
        final PerceptionVec3 away = horizontalAway.lengthSquared() > 1.0E-12
                ? horizontalAway.normalized()
                : new PerceptionVec3(
                        ((context.gameTick() / DEFENSIVE_DODGE_COOLDOWN_TICKS)
                                & 1L) == 0L ? 1.0 : -1.0,
                        0.0,
                        0.0
                );
        final PerceptionVec3 horizontalForward = new PerceptionVec3(
                frame.lookDirection().x(),
                0.0,
                frame.lookDirection().z()
        );
        final PerceptionVec3 forward = horizontalForward.lengthSquared()
                > 1.0E-12
                ? horizontalForward.normalized()
                : new PerceptionVec3(0.0, 0.0, 1.0);
        final PerceptionVec3 left = new PerceptionVec3(
                forward.z(),
                0.0,
                -forward.x()
        );
        if (!core.look(lookAt(
                frame.eyePosition(),
                frame.eyePosition().add(away)
        )).accepted()) {
            return Optional.of(fail(
                    context,
                    NAME + ".damage_dodge_look_rejected"
            ));
        }
        final ActionOutcome moved = core.move(new MovementIntent(
                away.dot(forward),
                away.dot(left),
                true,
                false
        ));
        if (!moved.accepted()) {
            return Optional.of(fail(
                    context,
                    NAME + ".damage_dodge_move_rejected"
            ));
        }
        lastDefensiveDodgeTick = context.gameTick();
        nextActionTick = context.gameTick() + 1;
        return Optional.of(SkillTickResult.running(true, true));
    }

    private void abortCageBreak(final SkillContext context) {
        if (cageBreak != null && cageBreakParameters != null) {
            try {
                cageBreak.cancel(context, cageBreakParameters);
            } catch (RuntimeException ignored) {
                interactions.abortMining();
            }
        }
        cageBreak = null;
        cageBreakParameters = null;
    }

    private SkillTickResult search(
            final SkillContext context,
            final FightEnderDragonParameters parameters,
            final Snapshot snapshot,
            final boolean fresh
    ) {
        final CoreSkillFrame frame = snapshot.core();
        if (context.gameTick() < nextActionTick) {
            core.stop();
            return SkillTickResult.running(fresh, true);
        }
        if (shouldDescendFromCage(frame)) {
            return startOrPrepareCageDescent(
                    context,
                    parameters,
                    frame,
                    fresh
            );
        }
        final Optional<Integer> immediateDragon =
                immediateDragonIndex(frame);
        if (immediateDragon.isEmpty()
                && (closestCrystalWithAlignedCageBar(frame).isPresent()
                || boundBlockedCrystal(frame).isPresent()
                || clearCrystalIndex(frame).isEmpty()
                    && hasBlockedCrystal(frame))) {
            return handleBlockedCrystal(
                    context,
                    parameters,
                    snapshot,
                    fresh
            );
        }
        rememberUnsafeClearCrystal(frame);
        if (crystalStandOffPosition != null) {
            return prepareCrystalStandOff(
                    context,
                    parameters,
                    frame,
                    fresh
            );
        }
        if (!cageTowered && cagePlan != null) {
            clearCageTraversalPlan();
        }
        cageScanTurns = 0;
        cageStatus = CageStatus.NONE;
        sawCageBarBeyondReach = false;
        final Optional<Integer> clearCrystal = clearCrystalIndex(frame);
        final boolean activeDragonThreat = frame.dangerSignals().stream()
                .anyMatch(signal ->
                        (signal.kind() == DangerKind.THREAT_CONTACT
                                || signal.kind()
                                        == DangerKind.PROJECTILE_PROXIMITY)
                                && signal.severity() >= 0.35
                );
        boolean crystalLaneBlocked = false;
        if (!activeDragonThreat && clearCrystal.isPresent()) {
            final VisibleEntity crystal = frame.visibleEntities()
                    .get(clearCrystal.orElseThrow());
            crystalLaneBlocked = EndCrystalStandOffPlanner.dragonBlocksFiringLane(
                    frame,
                    crystal
            );
            if (!crystalLaneBlocked) {
                crystalLaneAttempts = 0;
                crystalLaneScanTurns = 0;
            } else if (crystalLaneAttempts
                    < MAXIMUM_CRYSTAL_LANE_ATTEMPTS) {
                final Optional<GridPos> lane =
                        EndCrystalStandOffPlanner.selectFiringLane(
                                frame,
                                crystal.position(),
                                context.hardcore()
                        ).filter(candidate ->
                                !candidate.equals(frame.feet())
                        );
                if (lane.isPresent()) {
                    return startCrystalLane(
                            context,
                            parameters,
                            lane.orElseThrow(),
                            fresh
                    );
                }
                if (crystalLaneScanTurns
                        < MAXIMUM_CRYSTAL_LANE_SCANS) {
                    final PerceptionVec3 toCrystal = crystal.position()
                            .subtract(frame.position());
                    final PerceptionVec3 horizontal = new PerceptionVec3(
                            toCrystal.x(),
                            0.0,
                            toCrystal.z()
                    );
                    final PerceptionVec3 forward = horizontal.lengthSquared()
                            > 1.0E-12
                            ? horizontal.normalized()
                            : new PerceptionVec3(0.0, 0.0, 1.0);
                    final PerceptionVec3 side = new PerceptionVec3(
                            forward.z(),
                            0.0,
                            -forward.x()
                    );
                    final double sign =
                            (crystalLaneScanTurns & 1) == 0 ? 0.85 : -0.85;
                    if (!core.move(MovementIntent.STOPPED).accepted()
                            || !core.look(lookAt(
                                    frame.eyePosition(),
                                    frame.eyePosition().add(
                                            forward.add(side.scale(sign))
                                    )
                            )).accepted()) {
                        return fail(
                                context,
                                NAME + ".crystal_lane_scan_rejected"
                        );
                    }
                    if (fresh) {
                        crystalLaneScanTurns++;
                    }
                    nextActionTick = context.gameTick()
                            + SCAN_INTERVAL_TICKS;
                    return SkillTickResult.running(true, true);
                }
            }
        }
        /*
         * A nearby dragon is not sufficient reason to ignore an exposed
         * crystal. Vanilla crystals continuously heal the dragon, so a
         * clear crystal must be removed first whenever the body is not
         * currently under contact or projectile pressure. During an active
         * threat, keep the dragon as the immediate target so emergency
         * melee/ranged handling can take over without pausing for a crystal.
         */
        /*
         * A dragon can hover directly between the body and an exposed
         * crystal. After the bounded lane observations above, there may be no
         * fair standing cell from which the crystal ray is clear at all. Do
         * not stare at that crystal forever: choose the nearest observed
         * dragon part and use the ordinary ranged path until the dragon moves
         * or its health changes. This is still a normal first-person target;
         * it never reads hidden entity state or fabricates a projectile hit.
         */
        final boolean useDragonFallback = crystalLaneBlocked
                && crystalLaneScanTurns >= MAXIMUM_CRYSTAL_LANE_SCANS;
        final Optional<Integer> targetIndex =
                !activeDragonThreat
                        && clearCrystal.isPresent()
                        && !useDragonFallback
                        ? clearCrystal
                        : immediateDragon.or(() ->
                                useDragonFallback
                                        ? nearestDragonIndex(frame)
                                        : selectTargetIndex(frame)
                        );
        if (targetIndex.isEmpty()) {
            return scan(
                    context,
                    parameters,
                    frame,
                    fresh
            );
        }
        final int index = targetIndex.orElseThrow();
        final VisibleEntity target =
                frame.visibleEntities().get(index);
        final PerceptionVec3 meleeAim =
                meleeAimPoint(target);
        if (ENDER_DRAGON.equals(target.entityTypeId())
                && meleeAttacks >= MELEE_ATTACKS_BEFORE_RANGED
                && !dragonRangedMode) {
            dragonRangedMode = true;
            dragonRetreatDirection = horizontalAwayDirection(
                    meleeAim,
                    frame.position()
            );
            dragonRetreatTicksRemaining =
                    DRAGON_RANGED_RETREAT_TICKS;
            phase = Phase.RETREATING_DRAGON;
            nextActionTick = context.gameTick();
            return SkillTickResult.running(true, true);
        }
        if (ENDER_DRAGON.equals(target.entityTypeId())
                && meleeReachMisses
                    < MELEE_REACH_MISSES_BEFORE_RANGED
                && !dragonRangedMode
                && meleeAttacks < MELEE_ATTACKS_BEFORE_RANGED
                && meleeAim.subtract(frame.eyePosition()).length()
                    <= MELEE_DRAGON_DISTANCE
                && interactionLineClear(target)) {
            final Optional<String> meleeWeapon =
                    preferredMeleeWeapon(frame);
            if (meleeWeapon.isPresent()) {
                return meleeDragon(
                        context,
                        frame,
                        target,
                        meleeWeapon.orElseThrow(),
                        fresh
                );
            }
        }
        if (shotsDispatched >= parameters.maximumShots()) {
            return fail(
                    context,
                    NAME + ".shot_budget_exhausted"
            );
        }
        if (!BOW.equals(frame.mainHand().itemId())) {
            final InventoryOperationResult equipped =
                    inventory.equip(new EquipItemParameters(
                            BOW,
                            EquipmentTarget.MAINHAND
                    ));
            if (!equipped.succeeded()) {
                return fail(
                        context,
                        equipped.failure().orElseThrow()
                );
            }
            return SkillTickResult.running(true, true);
        }
        if (inventoryCount(frame, ARROW) < 1) {
            return fail(context, NAME + ".arrows_exhausted");
        }
        shotParameters = new ShootObservedEntityParameters(
                frame.observationRevision(),
                "visible-" + index,
                ActionHand.MAIN_HAND,
                1
        );
        shotTargetDragon = ENDER_DRAGON.equals(
                target.entityTypeId()
        );
        shot = new ShootObservedEntitySkill(
                expectedPlayerId,
                core,
                coreFrames,
                interactions,
                interactionFrames,
                RangedCombatSkillPolicy.defaults()
        );
        final Optional<SkillFailure> precondition =
                shot.preconditions(context, shotParameters);
        if (precondition.isPresent()) {
            shot = null;
            shotParameters = null;
            shotTargetDragon = false;
            nextActionTick = context.gameTick()
                    + SCAN_INTERVAL_TICKS;
            return scan(
                    context,
                    parameters,
                    frame,
                    fresh
            );
        }
        shot.start(context, shotParameters);
        phase = Phase.SHOOTING;
        scanTurns = 0;
        return SkillTickResult.running(true, true);
    }

    private SkillTickResult handleBlockedCrystal(
            final SkillContext context,
            final FightEnderDragonParameters parameters,
            final Snapshot snapshot,
            final boolean fresh
    ) {
        final CoreSkillFrame frame = snapshot.core();
        final VisibleEntity crystal =
                closestCrystalWithAlignedCageBar(frame)
                .or(() -> boundBlockedCrystal(frame))
                .or(() -> closestBlockedCrystal(frame))
                .orElseThrow();
        final Optional<VisibleBlockFace> maybeBar =
                alignedCageBar(frame, crystal);
        if (maybeBar.isPresent()) {
            final VisibleBlockFace bar = maybeBar.orElseThrow();
            if (bar.distance() <= CAGE_BREAK_REACH) {
                return startCageBreak(
                        context,
                        parameters,
                        frame,
                        bar,
                        fresh
                );
            }
            sawCageBarBeyondReach = true;
            cageStatus =
                    CageStatus.APPROACH_OR_ELEVATION_REQUIRED;
            if (cageTowered) {
                return startOrPrepareCageDescent(
                        context,
                        parameters,
                        frame,
                        fresh
                );
            }
            final Optional<CagedCrystalTraversalPlanner.Plan>
                    planned =
                    CagedCrystalTraversalPlanner.plan(
                            context,
                            frame,
                            bar
                    );
            if (planned.isPresent()) {
                return startCageTraversal(
                        context,
                        parameters,
                        frame,
                        crystal,
                        planned.orElseThrow(),
                        fresh
                );
            }
            cageStatus =
                    CageStatus.SAFE_TRAVERSAL_UNAVAILABLE;
        } else {
            cageStatus = CageStatus.SEEKING_VISIBLE_BAR;
            if (cageTowered) {
                return startOrPrepareCageDescent(
                        context,
                        parameters,
                        frame,
                        fresh
                );
            }
        }
        return recoverCageView(
                context,
                parameters,
                frame,
                crystal,
                fresh
        );
    }

    private SkillTickResult startCageTraversal(
            final SkillContext context,
            final FightEnderDragonParameters parameters,
            final CoreSkillFrame frame,
            final VisibleEntity crystal,
            final CagedCrystalTraversalPlanner.Plan plan,
            final boolean fresh
    ) {
        if (!cageTraversalSafety(context, frame)) {
            return fail(
                    context,
                    NAME + ".cage_descent_safety_reserve_required"
            );
        }
        if (inventoryCount(
                frame,
                "minecraft:water_bucket"
        ) < 1) {
            cageStatus = CageStatus.WATER_BUCKET_REQUIRED;
            return fail(
                    context,
                    NAME + ".cage_water_bucket_required"
            );
        }
        if (preferredCageMiningTool(frame).isEmpty()) {
            cageStatus = CageStatus.PICKAXE_REQUIRED;
            return fail(
                    context,
                    NAME + ".cage_pickaxe_required"
            );
        }
        if (cageCrystalId != null
                && !cageCrystalId.equals(crystal.entityId())) {
            clearCageTraversalPlan();
        }
        cageCrystalId = crystal.entityId();
        cageLastSeenPosition = crystal.position();
        cagePlan = plan;
        if (!frame.feet().equals(plan.approach())) {
            if (cageApproachAttempts
                    >= MAXIMUM_CAGE_APPROACH_ATTEMPTS) {
                return fail(
                        context,
                        NAME + ".cage_approach_failed"
                );
            }
            return startCageApproach(
                    context,
                    parameters,
                    plan,
                    fresh
            );
        }
        rememberVisibleLanding(frame, plan.landing());
        if (!recentLandingVerification(frame)) {
            phase = Phase.PREPARING_CAGE_TOWER;
            cageLandingScans = 0;
            return prepareCageTower(
                    context,
                    parameters,
                    frame,
                    fresh
            );
        }
        cageLandingScans = 0;
        final BridgeMaterialResult material =
                bridgeMaterials.ensureEquipped();
        if (!material.ready()) {
            return fail(
                    context,
                    NAME + ".cage_tower_material_unavailable"
            );
        }
        if (material.availableCount() < plan.towerBlocks()) {
            return fail(
                    context,
                    NAME + ".cage_tower_material_insufficient"
            );
        }
        cageTowerParameters = new TowerUpParameters(
                parameters.dimension(),
                plan.targetY(),
                0.2,
                plan.towerBlocks()
        );
        cageTower = new TowerUpSkill(
                expectedPlayerId,
                core,
                coreFrames,
                bridgeMaterials
        );
        final Optional<SkillFailure> precondition =
                cageTower.preconditions(
                        context,
                        cageTowerParameters
                );
        if (precondition.isPresent()) {
            cageTower = null;
            cageTowerParameters = null;
            return fail(
                    context,
                    NAME + ".cage_tower_precondition_failed"
            );
        }
        cageTower.start(context, cageTowerParameters);
        cageStatus = CageStatus.TOWERING;
        phase = Phase.TOWERING_CAGE;
        return SkillTickResult.running(true, false);
    }

    private SkillTickResult startCageApproach(
            final SkillContext context,
            final FightEnderDragonParameters parameters,
            final CagedCrystalTraversalPlanner.Plan plan,
            final boolean fresh
    ) {
        travelParameters = new TravelToParameters(
                parameters.dimension(),
                plan.approach().x() + 0.5,
                plan.approach().y(),
                plan.approach().z() + 0.5,
                0.45
        );
        travel = new TravelToSkill(
                expectedPlayerId,
                core,
                coreFrames,
                sessionGeneration
        );
        final Optional<SkillFailure> precondition =
                travel.preconditions(context, travelParameters);
        if (precondition.isPresent()) {
            travel = null;
            travelParameters = null;
            cageApproachAttempts++;
            cagePlan = null;
            cageStatus = CageStatus.APPROACH_CELL_UNAVAILABLE;
            nextActionTick = context.gameTick()
                    + SCAN_INTERVAL_TICKS;
            return SkillTickResult.running(fresh, true);
        }
        travel.start(context, travelParameters);
        cageApproachAttempts++;
        travelPurpose = TravelPurpose.CAGE_APPROACH;
        cageStatus = CageStatus.APPROACHING;
        phase = Phase.TRAVELLING;
        return SkillTickResult.running(true, true);
    }

    private SkillTickResult startCageBreak(
            final SkillContext context,
            final FightEnderDragonParameters parameters,
            final CoreSkillFrame frame,
            final VisibleBlockFace bar,
            final boolean fresh
    ) {
        if (cageBarsMined >= MAXIMUM_CAGE_BARS_MINED) {
            return fail(
                    context,
                    NAME + ".cage_block_budget_exhausted"
            );
        }
        final Optional<String> tool =
                preferredCageMiningTool(frame);
        if (tool.isEmpty()) {
            cageStatus = CageStatus.PICKAXE_REQUIRED;
            return fail(
                    context,
                    NAME + ".cage_pickaxe_required"
            );
        }
        if (!tool.orElseThrow().equals(
                frame.mainHand().itemId()
        )) {
            final InventoryOperationResult equipped =
                    inventory.equip(new EquipItemParameters(
                            tool.orElseThrow(),
                            EquipmentTarget.MAINHAND
                    ));
            if (!equipped.succeeded()) {
                return fail(
                        context,
                        equipped.failure().orElseThrow()
                );
            }
            cageStatus = CageStatus.EQUIPPING_PICKAXE;
            return SkillTickResult.running(true, true);
        }
        final PerceptionVec3 barDirection =
                bar.hitPosition().subtract(frame.eyePosition());
        if (angularError(
                frame.lookDirection(),
                barDirection
        ) > CAGE_MINING_ALIGNMENT_DEGREES) {
            final ActionOutcome stopped = core.stop();
            final ActionOutcome looking = core.look(lookAt(
                    frame.eyePosition(),
                    bar.hitPosition()
            ));
            if (!stopped.accepted() || !looking.accepted()) {
                return fail(
                        context,
                        NAME + ".cage_mining_alignment_rejected"
                );
            }
            cageStatus = CageStatus.ALIGNING_VISIBLE_BAR;
            nextActionTick = context.gameTick() + 1;
            return SkillTickResult.running(true, true);
        }
        final Optional<BlockFace> face = blockFace(bar.face());
        if (face.isEmpty()) {
            cageStatus = CageStatus.SEEKING_VISIBLE_BAR;
            return recoverCageView(
                    context,
                    parameters,
                    frame,
                    closestBlockedCrystal(frame).orElseThrow(),
                    fresh
            );
        }
        cageBreakParameters = new BreakBlockParameters(
                parameters.dimension(),
                new ObservedBlockTarget(
                        frame.observationRevision(),
                        bar.block().x(),
                        bar.block().y(),
                        bar.block().z(),
                        face.orElseThrow()
                )
        );
        cageBreak = new BreakBlockSkill(
                expectedPlayerId,
                interactions,
                interactionFrames,
                InteractionSkillPolicy.defaults()
        );
        final Optional<SkillFailure> precondition =
                cageBreak.preconditions(
                        context,
                        cageBreakParameters
                );
        if (precondition.isPresent()) {
            final String code =
                    precondition.orElseThrow().code();
            cageBreak = null;
            cageBreakParameters = null;
            if (transientCageBreakFailure(code)) {
                cageStatus =
                        CageStatus.SEEKING_VISIBLE_BAR;
                nextActionTick = context.gameTick()
                        + SCAN_INTERVAL_TICKS;
                return SkillTickResult.running(
                        fresh,
                        true
                );
            }
            return fail(
                    context,
                    NAME + ".cage_mining_unavailable"
            );
        }
        cageBreak.start(context, cageBreakParameters);
        cageStatus = CageStatus.MINING_VISIBLE_BAR;
        phase = Phase.OPENING_CAGE;
        cageScanTurns = 0;
        return SkillTickResult.running(true, true);
    }

    private SkillTickResult recoverCageView(
            final SkillContext context,
            final FightEnderDragonParameters parameters,
            final CoreSkillFrame frame,
            final VisibleEntity crystal,
            final boolean fresh
    ) {
        if (!core.move(MovementIntent.STOPPED).accepted()) {
            return fail(context, NAME + ".stop_rejected");
        }
        if (cageScanTurns >= CAGE_SCANS_BEFORE_RALLY) {
            cageScanTurns = 0;
            if (rallyAttempts >= MAXIMUM_RALLY_ATTEMPTS) {
                return fail(
                        context,
                        cageStatus
                            == CageStatus
                                .SAFE_TRAVERSAL_UNAVAILABLE
                            ? NAME
                                + ".cage_safe_traversal_unavailable"
                            : sawCageBarBeyondReach
                            ? NAME
                                + ".cage_requires_approach_or_tower"
                            : NAME
                                + ".cage_obstruction_unresolved"
                );
            }
            return startRallyTravel(
                    context,
                    parameters,
                    fresh
            );
        }
        final PerceptionVec3 offset = cageLookOffset(
                cageScanTurns
        );
        if (!core.look(lookAt(
                frame.eyePosition(),
                crystal.position().add(offset)
        )).accepted()) {
            return fail(context, NAME + ".look_rejected");
        }
        cageScanTurns++;
        nextActionTick = context.gameTick()
                + SCAN_INTERVAL_TICKS;
        return SkillTickResult.running(true, true);
    }

    private SkillTickResult tickCageBreak(
            final SkillContext context,
            final boolean fresh
    ) {
        final SkillTickResult result = cageBreak.tick(
                context,
                cageBreakParameters
        );
        if (result.status()
                == SkillTickResult.Status.COMPLETED) {
            cageBarsMined++;
            cageBreak = null;
            cageBreakParameters = null;
            cageStatus = CageStatus.VERIFYING_OPENING;
            phase = Phase.SEARCHING;
            nextActionTick = context.gameTick()
                    + SCAN_INTERVAL_TICKS;
            return SkillTickResult.running(true, true);
        }
        if (result.status()
                == SkillTickResult.Status.FAILED) {
            final String code = result.failure()
                    .orElseThrow()
                    .code();
            cageBreak = null;
            cageBreakParameters = null;
            phase = Phase.SEARCHING;
            nextActionTick = context.gameTick()
                    + SCAN_INTERVAL_TICKS;
            if (transientCageBreakFailure(code)) {
                cageStatus =
                        CageStatus.SEEKING_VISIBLE_BAR;
                return SkillTickResult.running(true, true);
            }
            return fail(
                    context,
                    NAME + ".cage_mining_failed"
            );
        }
        return SkillTickResult.running(
                result.madeProgress() || fresh,
                result.safeCheckpoint()
        );
    }

    private SkillTickResult tickCageTower(
            final SkillContext context,
            final FightEnderDragonParameters parameters,
            final boolean fresh
    ) {
        final SkillTickResult result = cageTower.tick(
                context,
                cageTowerParameters
        );
        if (result.status()
                == SkillTickResult.Status.COMPLETED) {
            cageTower = null;
            cageTowerParameters = null;
            cageTowered = true;
            cageStatus = CageStatus.ELEVATED;
            phase = Phase.SEARCHING;
            nextActionTick = context.gameTick()
                    + SCAN_INTERVAL_TICKS;
            return SkillTickResult.running(true, false);
        }
        if (result.status()
                == SkillTickResult.Status.FAILED) {
            cageTower = null;
            cageTowerParameters = null;
            final Optional<CoreSkillFrame> current =
                    coreFrames.current();
            if (current.isPresent()
                    && cageDrop(current.orElseThrow())
                        >= 3.5) {
                cageTowered = true;
                return startOrPrepareCageDescent(
                        context,
                        parameters,
                        current.orElseThrow(),
                        fresh
                );
            }
            return fail(
                    context,
                    NAME + ".cage_tower_failed"
            );
        }
        return SkillTickResult.running(
                result.madeProgress() || fresh,
                result.safeCheckpoint()
        );
    }

    private SkillTickResult prepareCageTower(
            final SkillContext context,
            final FightEnderDragonParameters parameters,
            final CoreSkillFrame frame,
            final boolean fresh
    ) {
        if (cagePlan == null
                || cageCrystalId == null
                || cageLastSeenPosition == null) {
            return fail(
                    context,
                    NAME + ".cage_tower_plan_unavailable"
            );
        }
        rememberVisibleLanding(
                frame,
                cagePlan.landing()
        );
        final Optional<VisibleEntity> currentCrystal =
                frame.visibleEntities().stream()
                        .filter(entity ->
                                cageCrystalId.equals(
                                        entity.entityId()
                                )
                        )
                        .filter(entity ->
                                END_CRYSTAL.equals(
                                        entity.entityTypeId()
                                )
                        )
                        .findFirst();
        if (currentCrystal.isPresent()) {
            final VisibleEntity crystal =
                    currentCrystal.orElseThrow();
            cageLastSeenPosition = crystal.position();
            if (interactionLineClear(crystal)) {
                cageTowered = false;
                clearCageTraversalPlan();
                phase = Phase.SEARCHING;
                nextActionTick = context.gameTick();
                return SkillTickResult.running(true, true);
            }
            final Optional<VisibleBlockFace> bar =
                    alignedCageBar(frame, crystal);
            if (recentLandingVerification(frame)
                    && bar.isPresent()) {
                if (bar.orElseThrow().distance()
                        <= CAGE_BREAK_REACH) {
                    phase = Phase.SEARCHING;
                    return startCageBreak(
                            context,
                            parameters,
                            frame,
                            bar.orElseThrow(),
                            fresh
                    );
                }
                return startCageTraversal(
                        context,
                        parameters,
                        frame,
                        crystal,
                        cagePlan,
                        fresh
                );
            }
        }
        if (cageLandingScans
                >= CAGE_DESCENT_SCAN_LIMIT) {
            return fail(
                    context,
                    NAME + ".cage_landing_not_visible"
            );
        }
        final PerceptionVec3 target =
                recentLandingVerification(frame)
                    ? cageLastSeenPosition
                    : landingTop(cagePlan.landing());
        if (!core.move(MovementIntent.STOPPED).accepted()
                || !core.look(
                        lookAt(frame.eyePosition(), target)
                ).accepted()) {
            return fail(context, NAME + ".look_rejected");
        }
        if (fresh) {
            cageLandingScans++;
        }
        cageStatus = recentLandingVerification(frame)
                ? CageStatus.REACQUIRING_CAGE
                : CageStatus.VERIFYING_LANDING;
        return SkillTickResult.running(fresh, true);
    }

    private SkillTickResult startOrPrepareCageDescent(
            final SkillContext context,
            final FightEnderDragonParameters parameters,
            final CoreSkillFrame frame,
            final boolean fresh
    ) {
        if (cagePlan == null) {
            return fail(
                    context,
                    NAME + ".cage_descent_plan_unavailable"
            );
        }
        final double drop = cageDrop(frame);
        if (drop < 3.5) {
            cageTowered = false;
            clearCageTraversalPlan();
            phase = Phase.SEARCHING;
            nextActionTick = context.gameTick();
            return SkillTickResult.running(true, true);
        }
        final int maximumDrop = Math.max(
                4,
                Math.min(32, (int) Math.ceil(drop) + 1)
        );
        cageDescentParameters =
                new WaterClutchDescendParameters(
                        parameters.dimension(),
                        cagePlan.landing().x() + 0.5,
                        cagePlan.landing().y(),
                        cagePlan.landing().z() + 0.5,
                        0.6,
                        maximumDrop
                );
        cageDescent = new WaterClutchDescendSkill(
                expectedPlayerId,
                core,
                coreFrames
        );
        cageDescentScans = 0;
        cageLandingScans = 0;
        phase = Phase.PREPARING_CAGE_DESCENT;
        return prepareCageDescent(
                context,
                frame,
                fresh
        );
    }

    private SkillTickResult prepareCageDescent(
            final SkillContext context,
            final CoreSkillFrame frame,
            final boolean fresh
    ) {
        if (cageDescent == null
                || cageDescentParameters == null
                || cagePlan == null) {
            return fail(
                    context,
                    NAME + ".cage_descent_plan_unavailable"
            );
        }
        final Optional<SkillFailure> precondition =
                cageDescent.preconditions(
                        context,
                        cageDescentParameters
                );
        if (precondition.isEmpty()) {
            cageDescent.start(
                    context,
                    cageDescentParameters
            );
            cageStatus = CageStatus.DESCENDING;
            phase = Phase.DESCENDING_CAGE;
            return SkillTickResult.running(true, false);
        }
        final String code =
                precondition.orElseThrow().code();
        if (!code.endsWith(
                ".visible_safe_landing_required"
        )) {
            return fail(
                    context,
                    NAME + ".cage_safe_descent_unavailable"
            );
        }
        if (fresh) {
            cageDescentScans++;
        }
        if (cageDescentScans
                >= CAGE_DESCENT_SCAN_LIMIT) {
            return fail(
                    context,
                    NAME + ".cage_landing_not_visible"
            );
        }
        final PerceptionVec3 landing =
                new PerceptionVec3(
                        cagePlan.landing().x() + 0.5,
                        cagePlan.landing().y(),
                        cagePlan.landing().z() + 0.5
                );
        if (!core.move(MovementIntent.STOPPED).accepted()
                || !core.look(
                        lookAt(frame.eyePosition(), landing)
                ).accepted()) {
            return fail(context, NAME + ".look_rejected");
        }
        cageStatus = CageStatus.SCANNING_LANDING;
        return SkillTickResult.running(fresh, true);
    }

    private SkillTickResult tickCageDescent(
            final SkillContext context,
            final boolean fresh
    ) {
        final SkillTickResult result = cageDescent.tick(
                context,
                cageDescentParameters
        );
        if (result.status()
                == SkillTickResult.Status.COMPLETED) {
            cageDescent = null;
            cageDescentParameters = null;
            cageTowered = false;
            clearCageTraversalPlan();
            cageStatus = CageStatus.DESCENT_COMPLETED;
            phase = Phase.SEARCHING;
            nextActionTick = context.gameTick()
                    + SCAN_INTERVAL_TICKS;
            return SkillTickResult.running(true, true);
        }
        if (result.status()
                == SkillTickResult.Status.FAILED) {
            cageDescent = null;
            cageDescentParameters = null;
            return fail(
                    context,
                    NAME + ".cage_descent_failed"
            );
        }
        return SkillTickResult.running(
                result.madeProgress() || fresh,
                result.safeCheckpoint()
        );
    }

    private SkillTickResult meleeDragon(
            final SkillContext context,
            final CoreSkillFrame frame,
            final VisibleEntity target,
            final String meleeWeapon,
            final boolean fresh
    ) {
        if (!meleeWeapon.equals(frame.mainHand().itemId())) {
            final InventoryOperationResult equipped =
                    inventory.equip(new EquipItemParameters(
                            meleeWeapon,
                            EquipmentTarget.MAINHAND
                    ));
            if (!equipped.succeeded()) {
                return fail(
                        context,
                        equipped.failure().orElseThrow()
                );
            }
            return SkillTickResult.running(true, true);
        }
        final PerceptionVec3 interactionAim =
                meleeAimPoint(target);
        if (!core.move(MovementIntent.STOPPED).accepted()
                || !core.look(
                        lookAt(frame.eyePosition(), interactionAim)
                ).accepted()) {
            return fail(context, NAME + ".melee_aim_rejected");
        }
        if (angularError(
                frame.lookDirection(),
                interactionAim.subtract(frame.eyePosition())
        ) > MELEE_ALIGNMENT_DEGREES) {
            return SkillTickResult.running(true, false);
        }
        final OptionalDouble strength =
                interactions.attackStrengthScale();
        if (strength.isEmpty()) {
            return fail(
                    context,
                    NAME + ".attack_strength_unavailable"
            );
        }
        if (strength.orElseThrow() < MELEE_ATTACK_STRENGTH) {
            return SkillTickResult.running(fresh, false);
        }
        final ActionOutcome attacked =
                interactions.attack(target.entityId());
        if (!attacked.accepted()) {
            if (transientMeleeTargetFailure(attacked)) {
                if (attacked
                        == ActionOutcome.TARGET_OUT_OF_REACH) {
                    meleeReachMisses++;
                    if (frame.onGround()
                            && interactionAim.y()
                                - frame.eyePosition().y()
                                >= 0.75
                            && !core.jump().accepted()) {
                        return fail(
                                context,
                                NAME + ".melee_jump_rejected"
                        );
                    }
                }
                nextActionTick = context.gameTick() + 1;
                scanTurns = 0;
                return SkillTickResult.running(true, true);
            }
            return fail(context, NAME + ".melee_attack_rejected");
        }
        meleeReachMisses = 0;
        rangedDragonShotsSinceMeleeRetry = 0;
        meleeAttacks++;
        nextActionTick = context.gameTick() + 2;
        scanTurns = 0;
        return SkillTickResult.running(true, false);
    }

    private static PerceptionVec3 meleeAimPoint(
            final VisibleEntity target
    ) {
        final Map<String, String> properties =
                target.visibleProperties();
        if ("true".equals(properties.get("multipartParent"))) {
            try {
                final double x = Double.parseDouble(
                        properties.get("interactionAimX")
                );
                final double y = Double.parseDouble(
                        properties.get("interactionAimY")
                );
                final double z = Double.parseDouble(
                        properties.get("interactionAimZ")
                );
                if (Double.isFinite(x)
                        && Double.isFinite(y)
                        && Double.isFinite(z)) {
                    return new PerceptionVec3(x, y, z);
                }
            } catch (NullPointerException
                    | NumberFormatException ignored) {
                // Fail closed to the ordinary semantic entity position.
            }
        }
        return target.position().add(
                new PerceptionVec3(0.0, 2.0, 0.0)
        );
    }

    private static boolean transientMeleeTargetFailure(
            final ActionOutcome outcome
    ) {
        return outcome == ActionOutcome.TARGET_NOT_FOUND
                || outcome == ActionOutcome.TARGET_UNLOADED
                || outcome == ActionOutcome.TARGET_OUT_OF_REACH
                || outcome == ActionOutcome.TARGET_OCCLUDED
                || outcome == ActionOutcome.TARGET_CHANGED;
    }

    private SkillTickResult scan(
            final SkillContext context,
            final FightEnderDragonParameters parameters,
            final CoreSkillFrame frame,
            final boolean fresh
    ) {
        /*
         * A real first-person player does not plant their feet while
         * searching the sky.  The old STOPPED input left the body exposed to
         * dragon breath during every 48-view sweep, and also produced the
         * exact "looks around but does nothing" failure seen in live runs.
         * Use a small alternating strafe only while no legal target is
         * visible.  It remains a normal player-relative input; collision,
         * fall, and emergency-survival checks still own the final movement.
         */
        final double searchStrafe =
                ((scanTurns / SCAN_PITCHES.length) & 1) == 0
                        ? 0.35
                        : -0.35;
        if (!core.move(new MovementIntent(
                0.0,
                searchStrafe,
                false,
                false
        )).accepted()) {
            return fail(context, NAME + ".search_move_rejected");
        }
        if (scanTurns >= SCANS_BEFORE_RALLY) {
            if (rallyAttempts >= MAXIMUM_RALLY_ATTEMPTS) {
                return fail(
                        context,
                        NAME + ".no_visible_combat_target"
                );
            }
            return startRallyTravel(
                    context,
                    parameters,
                    fresh
            );
        }
        final int pitchIndex =
                scanTurns % SCAN_PITCHES.length;
        final int yawStep =
                scanTurns / SCAN_PITCHES.length;
        final LookIntent look = new LookIntent(
                ActionMath.wrapDegrees(
                        scanBaseYaw + yawStep * 30.0F
                ),
                SCAN_PITCHES[pitchIndex]
        );
        if (!core.look(look).accepted()) {
            return fail(context, NAME + ".look_rejected");
        }
        scanTurns++;
        nextActionTick = context.gameTick()
                + SCAN_INTERVAL_TICKS;
        return SkillTickResult.running(true, true);
    }

    /**
     * Move out of a close multipart hitbox before switching to the bow.  A
     * normal player backs away from a dragon tail instead of continuing to
     * swing at the same small part from inside its collider.  The destination
     * is deliberately not teleported or path-injected: this is only a
     * bounded forward input after a first-person look, so vanilla collision,
     * void and emergency-survival rules still decide the result.
     */
    private SkillTickResult tickDragonRangedRetreat(
            final SkillContext context,
            final CoreSkillFrame frame,
            final boolean fresh
    ) {
        if (dragonRetreatTicksRemaining <= 0
                || dragonRetreatDirection == null) {
            dragonRetreatDirection = null;
            phase = Phase.SEARCHING;
            nextActionTick = context.gameTick();
            return SkillTickResult.running(true, true);
        }
        final PerceptionVec3 away = dragonRetreatDirection.lengthSquared()
                > 1.0E-12
                ? dragonRetreatDirection.normalized()
                : new PerceptionVec3(
                        ((context.gameTick() / 6L) & 1L) == 0L
                                ? 1.0
                                : -1.0,
                        0.0,
                        0.0
                );
        final PerceptionVec3 horizontalForward = new PerceptionVec3(
                frame.lookDirection().x(),
                0.0,
                frame.lookDirection().z()
        );
        final PerceptionVec3 forward = horizontalForward.lengthSquared()
                > 1.0E-12
                ? horizontalForward.normalized()
                : new PerceptionVec3(0.0, 0.0, 1.0);
        final PerceptionVec3 left = new PerceptionVec3(
                forward.z(),
                0.0,
                -forward.x()
        );
        if (!core.look(lookAt(
                frame.eyePosition(),
                frame.eyePosition().add(away)
        )).accepted()) {
            return fail(context, NAME + ".ranged_retreat_look_rejected");
        }
        if (!core.move(new MovementIntent(
                away.dot(forward),
                away.dot(left),
                true,
                false
        )).accepted()) {
            return fail(context, NAME + ".ranged_retreat_move_rejected");
        }
        if (fresh) {
            dragonRetreatTicksRemaining--;
        }
        nextActionTick = context.gameTick() + 1;
        return SkillTickResult.running(true, true);
    }

    private void rememberUnsafeClearCrystal(
            final CoreSkillFrame frame
    ) {
        closestClearCrystal(frame)
                .filter(crystal ->
                        EndCrystalStandOffPlanner
                            .horizontalDistance(
                                frame.position(),
                                crystal.position()
                            )
                            < EndCrystalStandOffPlanner
                                .MINIMUM_FIRE_DISTANCE
                )
                .ifPresent(crystal -> {
                    crystalStandOffPosition =
                            crystal.position();
                });
    }

    private SkillTickResult prepareCrystalStandOff(
            final SkillContext context,
            final FightEnderDragonParameters parameters,
            final CoreSkillFrame frame,
            final boolean fresh
    ) {
        final PerceptionVec3 crystal =
                Objects.requireNonNull(
                        crystalStandOffPosition
                );
        if (EndCrystalStandOffPlanner.horizontalDistance(
                frame.position(),
                crystal
        ) >= EndCrystalStandOffPlanner.MINIMUM_FIRE_DISTANCE) {
            clearCrystalStandOffMemory();
            scanTurns = 0;
            scanBaseYaw = lookYaw(frame);
            nextActionTick = context.gameTick();
            return SkillTickResult.running(true, true);
        }
        if (crystalCageInspectionTurns
                < CRYSTAL_CAGE_INSPECTION_SCANS) {
            if (!core.move(MovementIntent.STOPPED).accepted()) {
                return fail(
                        context,
                        NAME + ".crystal_cage_inspection_stop_rejected"
                );
            }
            final PerceptionVec3 inspectionTarget =
                    crystal.add(
                            cageLookOffset(
                                    crystalCageInspectionTurns
                            )
                    );
            if (!core.look(
                    lookAt(
                            frame.eyePosition(),
                            inspectionTarget
                    )
            ).accepted()) {
                return fail(
                        context,
                        NAME + ".crystal_cage_inspection_look_rejected"
                );
            }
            if (fresh) {
                crystalCageInspectionTurns++;
            }
            nextActionTick = context.gameTick()
                    + SCAN_INTERVAL_TICKS;
            return SkillTickResult.running(true, true);
        }
        final Optional<GridPos> retreat =
                EndCrystalStandOffPlanner.select(
                        frame,
                        crystal,
                        context.hardcore()
                );
        if (retreat.isEmpty()) {
            if (crystalStandOffScans
                    >= MAXIMUM_CRYSTAL_STANDOFF_SCANS) {
                return fail(
                        context,
                        NAME + ".crystal_standoff_unobserved"
                );
            }
            if (!core.stop().accepted()) {
                return fail(
                        context,
                        NAME + ".crystal_standoff_stop_rejected"
                );
            }
            final PerceptionVec3 away =
                    horizontalAwayDirection(
                            crystal,
                            frame.position()
                    );
            final PerceptionVec3 inspectionTarget =
                    frame.eyePosition().add(
                            away.lengthSquared() <= 1.0E-12
                                ? new PerceptionVec3(
                                        0.0,
                                        -2.0,
                                        -8.0
                                )
                                : new PerceptionVec3(
                                        away.x() * 8.0,
                                        -2.0,
                                        away.z() * 8.0
                                )
                    );
            if (!core.look(
                    lookAt(
                            frame.eyePosition(),
                            inspectionTarget
                    )
            ).accepted()) {
                return fail(
                        context,
                        NAME + ".crystal_standoff_look_rejected"
                );
            }
            crystalStandOffScans++;
            nextActionTick = context.gameTick()
                    + SCAN_INTERVAL_TICKS;
            return SkillTickResult.running(true, true);
        }
        final GridPos destination = retreat.orElseThrow();
        crystalStandOffParameters = new MoveToParameters(
                parameters.dimension(),
                destination.x() + 0.5,
                destination.y(),
                destination.z() + 0.5,
                0.30
        );
        crystalStandOff = new MoveToSkill(
                expectedPlayerId,
                core,
                coreFrames,
                (authorizedContext, authorizedFrame, target) ->
                    EndCrystalStandOffPlanner
                        .authorizesAggregateRisk(
                            authorizedFrame,
                            target,
                            crystal
                        )
        );
        final Optional<SkillFailure> precondition =
                crystalStandOff.preconditions(
                        context,
                        crystalStandOffParameters
                );
        if (precondition.isPresent()) {
            crystalStandOff = null;
            crystalStandOffParameters = null;
            return fail(
                    context,
                    NAME + ".crystal_standoff_precondition"
            );
        }
        crystalStandOff.start(
                context,
                crystalStandOffParameters
        );
        crystalStandOffAttempts++;
        phase = Phase.REPOSITIONING_CRYSTAL;
        return SkillTickResult.running(fresh, true);
    }

    private SkillTickResult tickCrystalStandOff(
            final SkillContext context,
            final FightEnderDragonParameters parameters,
            final boolean fresh
    ) {
        final SkillTickResult result =
                crystalStandOff.tick(
                        context,
                        crystalStandOffParameters
                );
        if (result.status()
                == SkillTickResult.Status.COMPLETED) {
            clearCrystalStandOffChild();
            phase = Phase.SEARCHING;
            scanTurns = 0;
            nextActionTick = context.gameTick();
            return SkillTickResult.running(true, true);
        }
        if (result.status()
                == SkillTickResult.Status.FAILED) {
            clearCrystalStandOffChild();
            if (crystalStandOffAttempts
                    >= MAXIMUM_CRYSTAL_STANDOFF_ATTEMPTS) {
                return fail(
                        context,
                        NAME + ".crystal_standoff_failed"
                );
            }
            phase = Phase.SEARCHING;
            crystalStandOffScans = 0;
            nextActionTick = context.gameTick()
                    + SCAN_INTERVAL_TICKS;
            return SkillTickResult.running(true, true);
        }
        return SkillTickResult.running(
                result.madeProgress() || fresh,
                result.safeCheckpoint()
        );
    }

    private SkillTickResult startCrystalLane(
            final SkillContext context,
            final FightEnderDragonParameters parameters,
            final GridPos destination,
            final boolean fresh
    ) {
        crystalLaneParameters = new MoveToParameters(
                parameters.dimension(),
                destination.x() + 0.5,
                destination.y(),
                destination.z() + 0.5,
                0.35
        );
        crystalLane = new MoveToSkill(
                expectedPlayerId,
                core,
                coreFrames
        );
        final Optional<SkillFailure> precondition =
                crystalLane.preconditions(
                        context,
                        crystalLaneParameters
                );
        if (precondition.isPresent()) {
            crystalLane = null;
            crystalLaneParameters = null;
            crystalLaneAttempts++;
            return SkillTickResult.running(fresh, true);
        }
        crystalLane.start(context, crystalLaneParameters);
        crystalLaneAttempts++;
        crystalLaneScanTurns = 0;
        phase = Phase.REPOSITIONING_CRYSTAL_LANE;
        return SkillTickResult.running(true, true);
    }

    private SkillTickResult tickCrystalLane(
            final SkillContext context,
            final boolean fresh
    ) {
        if (crystalLane == null || crystalLaneParameters == null) {
            phase = Phase.SEARCHING;
            return SkillTickResult.running(true, true);
        }
        final SkillTickResult result = crystalLane.tick(
                context,
                crystalLaneParameters
        );
        if (result.status() == SkillTickResult.Status.COMPLETED) {
            crystalLane = null;
            crystalLaneParameters = null;
            phase = Phase.SEARCHING;
            scanTurns = 0;
            crystalLaneScanTurns = 0;
            nextActionTick = context.gameTick();
            return SkillTickResult.running(true, true);
        }
        if (result.status() == SkillTickResult.Status.FAILED) {
            crystalLane = null;
            crystalLaneParameters = null;
            phase = Phase.SEARCHING;
            nextActionTick = context.gameTick()
                    + SCAN_INTERVAL_TICKS;
            return SkillTickResult.running(true, true);
        }
        return SkillTickResult.running(
                result.madeProgress() || fresh,
                result.safeCheckpoint()
        );
    }

    private static Optional<VisibleEntity> closestClearCrystal(
            final CoreSkillFrame frame
    ) {
        return frame.visibleEntities()
                .stream()
                .filter(entity ->
                        END_CRYSTAL.equals(
                                entity.entityTypeId()
                        )
                )
                .filter(
                        FightEnderDragonSkill
                            ::interactionLineClear
                )
                .min(Comparator.comparingDouble(
                        VisibleEntity::distance
                ));
    }

    private static PerceptionVec3 horizontalAwayDirection(
            final PerceptionVec3 crystal,
            final PerceptionVec3 body
    ) {
        final PerceptionVec3 away = new PerceptionVec3(
                body.x() - crystal.x(),
                0.0,
                body.z() - crystal.z()
        );
        return away.lengthSquared() <= 1.0E-12
                ? away
                : away.normalized();
    }

    private void clearCrystalStandOffChild() {
        crystalStandOff = null;
        crystalStandOffParameters = null;
    }

    private void clearCrystalStandOffMemory() {
        clearCrystalStandOffChild();
        crystalStandOffPosition = null;
        crystalStandOffScans = 0;
        crystalStandOffAttempts = 0;
        crystalCageInspectionTurns = 0;
    }

    private SkillTickResult tickShot(
            final SkillContext context,
            final boolean fresh
    ) {
        final SkillTickResult result =
                shot.tick(context, shotParameters);
        if (result.status() == SkillTickResult.Status.COMPLETED) {
            shotsDispatched++;
            if (shotTargetDragon) {
                rangedDragonShotsSinceMeleeRetry++;
                if (rangedDragonShotsSinceMeleeRetry
                        >= RANGED_SHOTS_BEFORE_MELEE_RETRY) {
                    meleeReachMisses = 0;
                    rangedDragonShotsSinceMeleeRetry = 0;
                }
            }
            shot = null;
            shotParameters = null;
            shotTargetDragon = false;
            phase = Phase.SEARCHING;
            nextActionTick = context.gameTick()
                    + PROJECTILE_SETTLE_TICKS;
            return SkillTickResult.running(true, true);
        }
        if (result.status() == SkillTickResult.Status.FAILED) {
            final String code = result.failure()
                    .orElseThrow()
                    .code();
            shot = null;
            shotParameters = null;
            shotTargetDragon = false;
            phase = Phase.SEARCHING;
            nextActionTick = context.gameTick()
                    + SCAN_INTERVAL_TICKS;
            if (transientShotFailure(code)) {
                return SkillTickResult.running(true, true);
            }
            return fail(context, NAME + ".shot_failed");
        }
        return SkillTickResult.running(
                result.madeProgress() || fresh,
                result.safeCheckpoint()
        );
    }

    private SkillTickResult startRallyTravel(
            final SkillContext context,
            final FightEnderDragonParameters parameters,
            final boolean fresh
    ) {
        travelParameters = new TravelToParameters(
                parameters.dimension(),
                Objects.requireNonNull(localRallyPoint).x(),
                localRallyPoint.y(),
                localRallyPoint.z(),
                3.0
        );
        travel = new TravelToSkill(
                expectedPlayerId,
                core,
                coreFrames,
                sessionGeneration
        );
        final Optional<SkillFailure> precondition =
                travel.preconditions(context, travelParameters);
        if (precondition.isPresent()) {
            travel = null;
            travelParameters = null;
            rallyAttempts++;
            scanTurns = 0;
            scanBaseYaw = ActionMath.wrapDegrees(
                    scanBaseYaw + 15.0F
            );
            return SkillTickResult.running(true, true);
        }
        travel.start(context, travelParameters);
        rallyAttempts++;
        travelPurpose = TravelPurpose.RALLY;
        phase = Phase.TRAVELLING;
        return SkillTickResult.running(fresh, true);
    }

    private SkillTickResult tickTravel(
            final SkillContext context,
            final boolean fresh
    ) {
        final SkillTickResult result =
                travel.tick(context, travelParameters);
        if (result.status() != SkillTickResult.Status.RUNNING) {
            final TravelPurpose completedPurpose =
                    travelPurpose;
            travel = null;
            travelParameters = null;
            travelPurpose = TravelPurpose.NONE;
            phase = Phase.SEARCHING;
            scanTurns = 0;
            nextActionTick = context.gameTick();
            if (completedPurpose
                    == TravelPurpose.CAGE_APPROACH) {
                if (result.status()
                        == SkillTickResult.Status.COMPLETED) {
                    cageStatus =
                            CageStatus.APPROACH_REACHED;
                } else {
                    cagePlan = null;
                    cageStatus =
                            CageStatus.APPROACH_CELL_UNAVAILABLE;
                }
            }
            coreFrames.current().ifPresent(frame ->
                    scanBaseYaw = lookYaw(frame)
            );
            return SkillTickResult.running(true, true);
        }
        return SkillTickResult.running(
                result.madeProgress() || fresh,
                result.safeCheckpoint()
        );
    }

    private static Optional<Integer> selectTargetIndex(
            final CoreSkillFrame frame
    ) {
        final Optional<Integer> crystal =
                clearCrystalIndex(frame);
        if (crystal.isPresent()) {
            return crystal;
        }
        return java.util.stream.IntStream.range(
                0,
                frame.visibleEntities().size()
        )
                .boxed()
                .filter(index ->
                        ENDER_DRAGON.equals(
                            frame.visibleEntities()
                                .get(index)
                                .entityTypeId()
                        )
                )
                .min(Comparator.comparingDouble(index ->
                        frame.visibleEntities()
                            .get(index)
                            .distance()
                ));
    }

    private static Optional<Integer> immediateDragonIndex(
            final CoreSkillFrame frame
    ) {
        final boolean damageNearby = frame.dangerSignals().stream()
                .anyMatch(signal ->
                        (signal.kind() == DangerKind.THREAT_CONTACT
                                || signal.kind()
                                        == DangerKind.PROJECTILE_PROXIMITY)
                                && signal.severity() >= 0.35);
        return java.util.stream.IntStream.range(
                        0,
                        frame.visibleEntities().size()
                )
                .boxed()
                .filter(index -> ENDER_DRAGON.equals(
                        frame.visibleEntities().get(index).entityTypeId()
                ))
                .filter(index ->
                        damageNearby
                                || frame.visibleEntities().get(index)
                                        .distance()
                                    <= IMMEDIATE_DRAGON_DISTANCE
                )
                .min(Comparator.comparingDouble(index ->
                        frame.visibleEntities().get(index).distance()
                ));
    }

    private static Optional<Integer> nearestDragonIndex(
            final CoreSkillFrame frame
    ) {
        return java.util.stream.IntStream.range(
                        0,
                        frame.visibleEntities().size()
                )
                .boxed()
                .filter(index -> ENDER_DRAGON.equals(
                        frame.visibleEntities().get(index).entityTypeId()
                ))
                .min(Comparator.comparingDouble(index ->
                        frame.visibleEntities().get(index).distance()
                ));
    }

    private static Optional<Integer> clearCrystalIndex(
            final CoreSkillFrame frame
    ) {
        return java.util.stream.IntStream.range(
                        0,
                        frame.visibleEntities().size()
                )
                    .boxed()
                    .filter(index ->
                            END_CRYSTAL.equals(
                                frame.visibleEntities()
                                    .get(index)
                                    .entityTypeId()
                            )
                    )
                    .filter(index ->
                            frame.visibleEntities()
                                .get(index)
                                .distance()
                                    >= EndCrystalStandOffPlanner
                                        .MINIMUM_FIRE_DISTANCE
                    )
                    .filter(index ->
                            interactionLineClear(
                                frame.visibleEntities().get(index)
                            )
                    )
                    .min(Comparator.comparingDouble(index ->
                            frame.visibleEntities()
                                .get(index)
                                .distance()
                    ));
    }

    private static boolean hasBlockedCrystal(
            final CoreSkillFrame frame
    ) {
        return frame.visibleEntities().stream().anyMatch(entity ->
                END_CRYSTAL.equals(entity.entityTypeId())
                    && !interactionLineClear(entity)
        );
    }

    private boolean shouldDescendFromCage(
            final CoreSkillFrame frame
    ) {
        if (!cageTowered || cagePlan == null) {
            return false;
        }
        if (cageCrystalId == null) {
            return true;
        }
        return frame.visibleEntities().stream()
                .filter(entity ->
                        cageCrystalId.equals(entity.entityId())
                )
                .findFirst()
                .map(FightEnderDragonSkill::interactionLineClear)
                .orElse(true);
    }

    private Optional<VisibleEntity> boundBlockedCrystal(
            final CoreSkillFrame frame
    ) {
        if (cageCrystalId == null) {
            return Optional.empty();
        }
        return frame.visibleEntities().stream()
                .filter(entity ->
                        cageCrystalId.equals(entity.entityId())
                )
                .filter(entity ->
                        END_CRYSTAL.equals(
                                entity.entityTypeId()
                        )
                )
                .filter(entity ->
                        !interactionLineClear(entity)
                )
                .findFirst();
    }

    private static Optional<VisibleEntity> closestBlockedCrystal(
            final CoreSkillFrame frame
    ) {
        return frame.visibleEntities().stream()
                .filter(entity ->
                        END_CRYSTAL.equals(
                                entity.entityTypeId()
                        )
                )
                .filter(entity ->
                        !interactionLineClear(entity)
                )
                .min(Comparator.comparingDouble(
                        VisibleEntity::distance
                ));
    }

    private static Optional<VisibleEntity>
            closestCrystalWithAlignedCageBar(
                    final CoreSkillFrame frame
            ) {
        return frame.visibleEntities().stream()
                .filter(entity ->
                        END_CRYSTAL.equals(
                                entity.entityTypeId()
                        )
                )
                .filter(entity ->
                        alignedCageBar(
                                frame,
                                entity
                        ).isPresent()
                )
                .min(Comparator.comparingDouble(
                        VisibleEntity::distance
                ));
    }

    private double cageDrop(final CoreSkillFrame frame) {
        return cagePlan == null
                ? 0.0
                : frame.position().y()
                    - cagePlan.landing().y();
    }

    private static boolean visibleLandingSupport(
            final CoreSkillFrame frame,
            final GridPos landing
    ) {
        final GridPos support = landing.below();
        return frame.visibleBlockFaces().stream().anyMatch(face ->
                face.block().x() == support.x()
                    && face.block().y() == support.y()
                    && face.block().z() == support.z()
                    && blockFace(face.face())
                        .filter(value -> value == BlockFace.UP)
                        .isPresent()
        );
    }

    private void rememberVisibleLanding(
            final CoreSkillFrame frame,
            final GridPos landing
    ) {
        if (visibleLandingSupport(frame, landing)) {
            cageLandingVerified = true;
            cageLandingVerifiedRevision =
                    frame.observationRevision();
        }
    }

    private boolean recentLandingVerification(
            final CoreSkillFrame frame
    ) {
        return cageLandingVerified
                && cageLandingVerifiedRevision >= 0
                && frame.observationRevision()
                    >= cageLandingVerifiedRevision
                && frame.observationRevision()
                    - cageLandingVerifiedRevision <= 4;
    }

    private static PerceptionVec3 landingTop(
            final GridPos landing
    ) {
        return new PerceptionVec3(
                landing.x() + 0.5,
                landing.y(),
                landing.z() + 0.5
        );
    }

    private void clearCageTraversalPlan() {
        cageCrystalId = null;
        cageLastSeenPosition = null;
        cagePlan = null;
        cageApproachAttempts = 0;
        cageDescentScans = 0;
        cageLandingScans = 0;
        cageLandingVerified = false;
        cageLandingVerifiedRevision = -1;
        sawCageBarBeyondReach = false;
    }

    private static Optional<VisibleBlockFace> alignedCageBar(
            final CoreSkillFrame frame,
            final VisibleEntity crystal
    ) {
        final PerceptionVec3 origin = frame.eyePosition();
        final PerceptionVec3 ray =
                crystal.position().subtract(origin);
        final double rayLengthSquared = ray.lengthSquared();
        if (rayLengthSquared <= 1.0E-12) {
            return Optional.empty();
        }
        return frame.visibleBlockFaces().stream()
                .filter(face ->
                        IRON_BARS.equals(face.blockTypeId())
                )
                .filter(face -> {
                    final PerceptionVec3 fromOrigin =
                            face.hitPosition().subtract(origin);
                    final double fraction = fromOrigin.dot(ray)
                            / rayLengthSquared;
                    if (fraction <= 0.0 || fraction >= 1.05) {
                        return false;
                    }
                    final PerceptionVec3 closest =
                            origin.add(ray.scale(fraction));
                    return face.hitPosition()
                            .subtract(closest)
                            .length()
                            <= CAGE_LINE_RADIUS;
                })
                .min(Comparator.comparingDouble(
                        VisibleBlockFace::distance
                ));
    }

    private static boolean interactionLineClear(
            final VisibleEntity entity
    ) {
        return !"false".equals(
                entity.visibleProperties().get(
                    "interactionLineClear"
                )
        );
    }

    private static boolean projectileThreatensBody(
            final VisibleEntity projectile
    ) {
        /*
         * The fair sampler marks the companion's own arrows as visible but
         * non-threatening. Older fixtures without this property remain
         * conservative and are treated as threats.
         */
        return !"false".equals(
                projectile.visibleProperties().get("projectileThreat")
        );
    }

    private static Optional<String> preferredMeleeWeapon(
            final CoreSkillFrame frame
    ) {
        return MELEE_WEAPONS.stream().filter(itemId ->
                inventoryCount(frame, itemId) > 0
        ).findFirst();
    }

    private static Optional<String> preferredCageMiningTool(
            final CoreSkillFrame frame
    ) {
        return CAGE_MINING_TOOLS.stream().filter(itemId ->
                inventoryCount(frame, itemId) > 0
        ).findFirst();
    }

    private static Optional<BlockFace> blockFace(
            final String serialized
    ) {
        final int separator = serialized.lastIndexOf(':');
        final String token = separator >= 0
                ? serialized.substring(separator + 1)
                : serialized;
        try {
            return Optional.of(BlockFace.valueOf(
                    token.toUpperCase(Locale.ROOT)
            ));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static PerceptionVec3 cageLookOffset(
            final int scanTurn
    ) {
        return switch (scanTurn % 6) {
            case 0 -> new PerceptionVec3(0.0, 0.0, 0.0);
            case 1 -> new PerceptionVec3(0.0, 1.25, 0.0);
            case 2 -> new PerceptionVec3(0.0, -0.75, 0.0);
            case 3 -> new PerceptionVec3(0.75, 0.25, 0.0);
            case 4 -> new PerceptionVec3(-0.75, 0.25, 0.0);
            default -> new PerceptionVec3(0.0, 0.25, 0.75);
        };
    }

    private static LookIntent lookAt(
            final PerceptionVec3 eye,
            final PerceptionVec3 target
    ) {
        final PerceptionVec3 delta = target.subtract(eye);
        return new LookIntent(
                (float) Math.toDegrees(
                        Math.atan2(-delta.x(), delta.z())
                ),
                (float) Math.toDegrees(Math.atan2(
                        -delta.y(),
                        Math.hypot(delta.x(), delta.z())
                ))
        );
    }

    private static double angularError(
            final PerceptionVec3 current,
            final PerceptionVec3 desired
    ) {
        if (desired.lengthSquared() <= 1.0E-12) {
            return 180.0;
        }
        final double dot = current.normalized()
                .dot(desired.normalized());
        return Math.toDegrees(Math.acos(
                Math.max(-1.0, Math.min(1.0, dot))
        ));
    }

    private SnapshotValidation validateSnapshot(
            final FightEnderDragonParameters parameters
    ) {
        final Optional<CoreSkillFrame> maybeCore =
                coreFrames.current();
        final Optional<InteractionSkillFrame> maybeInteraction =
                interactionFrames.current();
        if (maybeCore.isEmpty() || maybeInteraction.isEmpty()) {
            return SnapshotValidation.failed(
                    NAME + ".observation_unavailable"
            );
        }
        final CoreSkillFrame coreFrame = maybeCore.orElseThrow();
        final InteractionSkillFrame interaction =
                maybeInteraction.orElseThrow();
        if (!expectedPlayerId.equals(coreFrame.playerId())
                || !expectedPlayerId.equals(
                    interaction.playerId()
                )) {
            return SnapshotValidation.failed(
                    NAME + ".body_mismatch"
            );
        }
        if (!parameters.dimension().equals(coreFrame.dimension())
                || !coreFrame.dimension().equals(
                    interaction.dimension()
                )) {
            return SnapshotValidation.failed(
                    NAME + ".wrong_dimension"
            );
        }
        if (coreFrame.observationRevision()
                != interaction.observationRevision()) {
            return SnapshotValidation.failed(
                    NAME + ".observation_desynchronized"
            );
        }
        final OptionalLong session =
                interactions.sessionGeneration();
        if (session.isEmpty()
                || session.orElseThrow()
                    != interaction.sessionGeneration()
                || boundSessionGeneration >= 0
                    && session.orElseThrow()
                        != boundSessionGeneration) {
            return SnapshotValidation.failed(
                    NAME + ".session_mismatch"
            );
        }
        return SnapshotValidation.available(
                new Snapshot(coreFrame, interaction)
        );
    }

    private static boolean healthTooLow(
            final SkillContext context,
            final CoreSkillFrame frame
    ) {
        final double ratio = frame.health() / frame.maxHealth();
        return ratio < (context.hardcore() ? 0.50 : 0.25);
    }

    private static boolean cageTraversalSafety(
            final SkillContext context,
            final CoreSkillFrame frame
    ) {
        final double maximumDanger =
                context.hardcore() ? 0.04 : 0.12;
        final double minimumHealth =
                context.hardcore() ? 0.95 : 0.80;
        return context.riskScore() <= maximumDanger
                && frame.danger() <= maximumDanger
                && frame.health() / frame.maxHealth()
                    >= minimumHealth
                && frame.foodLevel() >= 8
                && frame.onGround()
                && !frame.inWater();
    }

    private static int inventoryCount(
            final CoreSkillFrame frame,
            final String itemId
    ) {
        return frame.inventory().stream()
                .filter(item -> item.itemId().equals(itemId))
                .mapToInt(InventoryItemSummary::count)
                .sum();
    }

    private static float lookYaw(final CoreSkillFrame frame) {
        return (float) Math.toDegrees(Math.atan2(
                -frame.lookDirection().x(),
                frame.lookDirection().z()
        ));
    }

    private static float yawFromDirection(
            final PerceptionVec3 direction
    ) {
        return (float) Math.toDegrees(Math.atan2(
                -direction.x(),
                direction.z()
        ));
    }

    private static boolean transientShotFailure(
            final String code
    ) {
        return code.endsWith(".target_lost")
                || code.endsWith(".stale_observation")
                || code.endsWith(".stale_observation_id")
                /*
                 * The 20 TPS survival supervisor may fairly pre-empt the
                 * bow with food after a crystal explosion. The child has
                 * already released its use action on failure; reobserve and
                 * equip again after the safety action instead of converting
                 * that expected pre-emption into a terminal dragon failure.
                 */
                || code.endsWith(".weapon_changed")
                || code.endsWith(".crystal_too_close")
                || code.endsWith(".target_out_of_reach")
                /*
                 * A dragon part can move between the sampled observation
                 * and the vanilla use packet.  Alignment, line-of-sight,
                 * and use-start failures are therefore ordinary retryable
                 * first-person races, not reasons to hand the whole fight
                 * back to the language model.  Releasing the child below
                 * always returns the skill to SEARCHING, where a fresh
                 * observation must prove the next shot.
                 */
                || code.endsWith(".interaction_line_blocked")
                || code.endsWith(".look_rejected")
                || code.endsWith(".stop_rejected")
                || code.endsWith(".use_start_rejected")
                || code.endsWith(".use_interrupted")
                || code.endsWith(".release_rejected")
                /*
                 * A dragon wing/contact pulse can raise the sampled danger
                 * after the child has already begun aiming.  The emergency
                 * supervisor owns the immediate retreat/guard response; the
                 * dragon skill must reobserve and retry instead of ending
                 * the entire victory goal on that one fair safety rejection.
                 */
                || code.endsWith(".danger_too_high");
    }

    private static boolean transientCageBreakFailure(
            final String code
    ) {
        return code.endsWith(".observation_expired")
                || code.endsWith(".target_not_visible")
                || code.endsWith(".target_out_of_range")
                || code.endsWith(".stale_observation")
                || code.endsWith(".target_changed")
                || code.endsWith(".action_target_occluded")
                || code.endsWith(".action_target_out_of_reach");
    }

    private void cancelChildren(final SkillContext context) {
        if (shot != null && shotParameters != null) {
            try {
                shot.cancel(context, shotParameters);
            } catch (RuntimeException ignored) {
                interactions.releaseUse();
            }
        }
        shot = null;
        shotParameters = null;
        shotTargetDragon = false;
        if (travel != null && travelParameters != null) {
            try {
                travel.cancel(context, travelParameters);
            } catch (RuntimeException ignored) {
                core.stop();
            }
        }
        travel = null;
        travelParameters = null;
        travelPurpose = TravelPurpose.NONE;
        if (crystalStandOff != null
                && crystalStandOffParameters != null) {
            try {
                crystalStandOff.cancel(
                        context,
                        crystalStandOffParameters
                );
            } catch (RuntimeException ignored) {
                core.stop();
            }
        }
        clearCrystalStandOffChild();
        if (crystalLane != null && crystalLaneParameters != null) {
            try {
                crystalLane.cancel(context, crystalLaneParameters);
            } catch (RuntimeException ignored) {
                core.stop();
            }
        }
        crystalLane = null;
        crystalLaneParameters = null;
        if (cageBreak != null
                && cageBreakParameters != null) {
            try {
                cageBreak.cancel(
                        context,
                        cageBreakParameters
                );
            } catch (RuntimeException ignored) {
                interactions.abortMining();
            }
        }
        cageBreak = null;
        cageBreakParameters = null;
        if (cageTower != null
                && cageTowerParameters != null) {
            try {
                cageTower.cancel(
                        context,
                        cageTowerParameters
                );
            } catch (RuntimeException ignored) {
                core.stop();
            }
        }
        cageTower = null;
        cageTowerParameters = null;
        if (cageDescent != null
                && cageDescentParameters != null) {
            try {
                cageDescent.cancel(
                        context,
                        cageDescentParameters
                );
            } catch (RuntimeException ignored) {
                core.stop();
            }
        }
        cageDescent = null;
        cageDescentParameters = null;
    }

    private void quiesce() {
        interactions.releaseUse();
        core.releaseUse();
        core.stop();
    }

    private SkillTickResult fail(
            final SkillContext context,
            final String code
    ) {
        return fail(context, SkillFailure.of(code));
    }

    private SkillTickResult fail(
            final SkillContext context,
            final SkillFailure reason
    ) {
        cancelChildren(context);
        quiesce();
        failure = reason;
        phase = Phase.FAILED;
        return SkillTickResult.failed(reason);
    }

    private enum Phase {
        IDLE,
        SEARCHING,
        SHOOTING,
        RETREATING_DRAGON,
        REPOSITIONING_CRYSTAL,
        REPOSITIONING_CRYSTAL_LANE,
        TRAVELLING,
        OPENING_CAGE,
        PREPARING_CAGE_TOWER,
        TOWERING_CAGE,
        PREPARING_CAGE_DESCENT,
        DESCENDING_CAGE,
        COMPLETED,
        FAILED,
        CANCELLED;

        private boolean active() {
            return this == SEARCHING
                    || this == SHOOTING
                    || this == RETREATING_DRAGON
                    || this == REPOSITIONING_CRYSTAL
                    || this == REPOSITIONING_CRYSTAL_LANE
                    || this == TRAVELLING
                    || this == OPENING_CAGE
                    || this == PREPARING_CAGE_TOWER
                    || this == TOWERING_CAGE
                    || this == PREPARING_CAGE_DESCENT
                    || this == DESCENDING_CAGE;
        }
    }

    private enum CageStatus {
        NONE,
        SEEKING_VISIBLE_BAR,
        APPROACH_OR_ELEVATION_REQUIRED,
        SAFE_TRAVERSAL_UNAVAILABLE,
        PICKAXE_REQUIRED,
        WATER_BUCKET_REQUIRED,
        EQUIPPING_PICKAXE,
        APPROACH_CELL_UNAVAILABLE,
        APPROACHING,
        APPROACH_REACHED,
        VERIFYING_LANDING,
        REACQUIRING_CAGE,
        TOWERING,
        ELEVATED,
        ALIGNING_VISIBLE_BAR,
        MINING_VISIBLE_BAR,
        VERIFYING_OPENING,
        SCANNING_LANDING,
        DESCENDING,
        DESCENT_COMPLETED
    }

    private enum TravelPurpose {
        NONE,
        RALLY,
        CAGE_APPROACH
    }

    private record Snapshot(
            CoreSkillFrame core,
            InteractionSkillFrame interaction
    ) {
    }

    private record SnapshotValidation(
            Optional<Snapshot> snapshot,
            Optional<SkillFailure> failure
    ) {
        private static SnapshotValidation available(
                final Snapshot snapshot
        ) {
            return new SnapshotValidation(
                    Optional.of(snapshot),
                    Optional.empty()
            );
        }

        private static SnapshotValidation failed(
                final String code
        ) {
            return new SnapshotValidation(
                    Optional.empty(),
                    Optional.of(SkillFailure.of(code))
            );
        }
    }
}
