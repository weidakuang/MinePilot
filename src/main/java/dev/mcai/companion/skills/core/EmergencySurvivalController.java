package dev.mcai.companion.skills.core;

import dev.mcai.companion.action.ActionHand;
import dev.mcai.companion.action.ActionMath;
import dev.mcai.companion.action.ActionOutcome;
import dev.mcai.companion.action.ActionVec3;
import dev.mcai.companion.action.BlockFace;
import dev.mcai.companion.action.BlockInteractionTarget;
import dev.mcai.companion.action.LookIntent;
import dev.mcai.companion.action.MovementIntent;
import dev.mcai.companion.navigation.GridPos;
import dev.mcai.companion.navigation.LocalNavSnapshot;
import dev.mcai.companion.navigation.NavigationEvidence;
import dev.mcai.companion.navigation.ObservedVoxel;
import dev.mcai.companion.navigation.VoxelKind;
import dev.mcai.companion.perception.DangerKind;
import dev.mcai.companion.perception.DangerSignal;
import dev.mcai.companion.perception.HeldItemSummary;
import dev.mcai.companion.perception.InventoryItemSummary;
import dev.mcai.companion.perception.PerceptionVec3;
import dev.mcai.companion.perception.PerceptionProvenance;
import dev.mcai.companion.perception.VisibleBlockFace;
import dev.mcai.companion.perception.VisibleEntity;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * Small fail-safe state machine that runs after the selected skill and before
 * the one-tick actuator lease is executed. It may override movement only from
 * player-owned state and the last fair semantic observation.
 *
 * <p>The controller never edits inventory, health/food, or the world
 * directly, queries a level, or follows an occluded threat. It may request an
 * atomic vanilla menu swap through {@link EmergencyEquipmentActuator}, then
 * eat, guard, deploy a water bucket or owned vanilla fall-arrest block, swim
 * upward, or retreat into an adjacent fully observed standable cell.</p>
 */
public final class EmergencySurvivalController {
    private static final double IMMEDIATE_DANGER = 0.50;
    private static final double SAFE_VOXEL_DANGER = 0.10;
    private static final int MAX_EATING_TICKS = 60;
    private static final int MAX_RETREAT_TICKS = 60;
    private static final double MAXIMUM_WATER_REACH = 4.75;
    private static final double MAXIMUM_CLUTCH_COLUMN_OFFSET = 1.45;
    private static final double WATER_ALIGNMENT_DEGREES = 5.0;
    private static final double MINIMUM_CLUTCH_DROP = 2.0;
    private static final double IMMINENT_FALL_SEVERITY = 0.15;
    private static final double EMERGENCY_MELEE_REACH = 3.25;
    private static final double HOSTILE_REACQUIRE_OBSERVATION_RADIUS = 12.0;
    private static final double EMERGENCY_ATTACK_COOLDOWN = 0.85;
    private static final double EMERGENCY_ATTACK_ALIGNMENT_DEGREES = 7.5;
    private static final int MAX_WARNING_REACTION_TICKS = 12;
    /**
     * A directionless physical hit is not enough evidence to choose a
     * particular adjacent voxel, but standing still forever is also not a
     * fair survival policy.  After one complete first-person cardinal scan,
     * issue a short sneaking backstep using only the current look direction.
     * Vanilla collision, gravity and support remain authoritative; this is a
     * bounded separation probe, not a teleport or an unseen-world query.
     */
    private static final int DIRECTIONLESS_DAMAGE_SCAN_TICKS = 4;
    private static final double DIRECTIONLESS_DAMAGE_PROBE_SPEED = 0.45;
    private static final int MAX_HOSTILE_REACQUIRE_TICKS = 120;
    private static final int MAX_ATTACK_FOOTWORK_TICKS = 40;
    private static final double WARNING_SCAN_ALIGNMENT_DEGREES = 20.0;
    /**
     * A recent vanilla damage event carries only the attacker's direction,
     * never its identity or hidden position.  Start the first-person scan at
     * that direction, then use a small bounded fan so a moving hostile can be
     * reacquired without turning the reflex lane into an omniscient radar.
     */
    private static final float[] DAMAGE_DIRECTION_SCAN_OFFSETS = {
        0.0F, -35.0F, 35.0F, -70.0F, 70.0F, 180.0F
    };
    /**
     * If vanilla did not expose a source direction (for example a generic
     * environmental contact), cover the four cardinal sectors relative to
     * the current first-person heading instead of oscillating in one 80°
     * window forever.
     */
    private static final float[] DAMAGE_UNKNOWN_SCAN_OFFSETS = {
        0.0F, 90.0F, 180.0F, -90.0F
    };
    private static final List<String> FALL_CLUTCH_ITEMS = List.of(
            "minecraft:slime_block",
            "minecraft:cobweb",
            "minecraft:hay_block"
    );
    private static final List<String> EMERGENCY_MELEE_WEAPONS =
            List.of(
                    "minecraft:netherite_sword",
                    "minecraft:diamond_sword",
                    "minecraft:iron_sword",
                    "minecraft:stone_sword",
                    "minecraft:golden_sword",
                    "minecraft:wooden_sword",
                    "minecraft:netherite_axe",
                    "minecraft:diamond_axe",
                    "minecraft:iron_axe",
                    "minecraft:stone_axe",
                    "minecraft:golden_axe",
                    "minecraft:wooden_axe"
            );
    private static final int[][] CARDINALS = {
            {-1, 0},
            {0, -1},
            {0, 1},
            {1, 0}
    };

    private final UUID expectedPlayerId;
    private final CoreSkillActuator actuator;
    private final CoreSkillFrameSource frames;
    private final EmergencyEquipmentActuator equipment;
    private final EmergencyMeleeActuator melee;
    private final Runnable preemptTaskControls;

    private State state = State.CLEAR;
    private long stateStartedTick;
    private int eatingBaseline;
    private String eatingItemId;
    private int eatingItemCount;
    private ActionHand activeUseHand;
    private PerceptionVec3 retreatTarget;
    private long retreatRevision = -1;
    private long lastVisibleHostileTick = -1;
    /**
     * Local attack timestamp used as a conservative vanilla cooldown guard.
     * The server-side attack-strength sample is authoritative for dispatch,
     * but retaining this timestamp prevents a clientless packet loop from
     * issuing another attack before the previous attack has had a chance to
     * recharge.  This is also what gives a real shield a continuous warm-up
     * window instead of repeatedly staring at a hostile.
     */
    private long lastEmergencyAttackTick = -1;
    /**
     * Bounded direction of the last fair close hostile observation. It is not
     * an entity id or position; it only permits one short separation step
     * after an Enderman teleports outside the current semantic sample.
     */
    private PerceptionVec3 lastEmergencyAway;

    public EmergencySurvivalController(
            UUID expectedPlayerId,
            CoreSkillActuator actuator,
            CoreSkillFrameSource frames
    ) {
        this(
                expectedPlayerId,
                actuator,
                frames,
                EmergencyEquipmentActuator.unavailable(),
                EmergencyMeleeActuator.unavailable(),
                () -> {
                }
        );
    }

    public EmergencySurvivalController(
            UUID expectedPlayerId,
            CoreSkillActuator actuator,
            CoreSkillFrameSource frames,
            EmergencyEquipmentActuator equipment
    ) {
        this(
                expectedPlayerId,
                actuator,
                frames,
                equipment,
                EmergencyMeleeActuator.unavailable(),
                () -> {
                }
        );
    }

    public EmergencySurvivalController(
            UUID expectedPlayerId,
            CoreSkillActuator actuator,
            CoreSkillFrameSource frames,
            EmergencyEquipmentActuator equipment,
            EmergencyMeleeActuator melee
    ) {
        this(
                expectedPlayerId,
                actuator,
                frames,
                equipment,
                melee,
                () -> {
                }
        );
    }

    /**
     * Creates the production controller with a bounded callback that releases
     * task-owned mining, item-use and vehicle inputs before a new emergency
     * state writes its first body action.
     */
    public EmergencySurvivalController(
            UUID expectedPlayerId,
            CoreSkillActuator actuator,
            CoreSkillFrameSource frames,
            EmergencyEquipmentActuator equipment,
            EmergencyMeleeActuator melee,
            Runnable preemptTaskControls
    ) {
        this.expectedPlayerId = Objects.requireNonNull(
                expectedPlayerId,
                "expectedPlayerId"
        );
        this.actuator = Objects.requireNonNull(actuator, "actuator");
        this.frames = Objects.requireNonNull(frames, "frames");
        this.equipment = Objects.requireNonNull(
                equipment,
                "equipment"
        );
        this.melee = Objects.requireNonNull(melee, "melee");
        this.preemptTaskControls = Objects.requireNonNull(
                preemptTaskControls,
                "preemptTaskControls"
        );
    }

    /**
     * Executes at most one bounded intervention for the current server tick.
     */
    public TickReport tick() {
        return tick(false);
    }

    /**
     * Executes one bounded intervention while respecting an active combat
     * skill's ownership of ordinary visible-hostile proximity. Physical
     * contact and projectile threats are never delegated.
     */
    public TickReport tick(
            final boolean visibleHostileProximityManaged
    ) {
        return tick(visibleHostileProximityManaged, false);
    }

    /**
     * Executes one bounded intervention while allowing an active, locally
     * supervised melee skill to retain contact control for its target.
     * Projectile, fire, fall, air, food and health emergencies remain local.
     */
    public TickReport tick(
            final boolean visibleHostileProximityManaged,
            final boolean physicalContactManaged
    ) {
        return tick(
                visibleHostileProximityManaged,
                physicalContactManaged,
                false
        );
    }

    /**
     * Executes one bounded intervention while allowing a specialized active
     * skill to own visible projectile proximity.  Contact, fire, fall, air,
     * food, health and any projectile that is not covered by that explicit
     * declaration remain emergency-owned.
     */
    public TickReport tick(
            final boolean visibleHostileProximityManaged,
            final boolean physicalContactManaged,
            final boolean visibleProjectileThreatManaged
    ) {
        Optional<CoreSkillFrame> current = frames.current();
        if (current.isEmpty()
                || !expectedPlayerId.equals(
                        current.orElseThrow().playerId()
                )) {
            return TickReport.none(state);
        }
        CoreSkillFrame frame = current.orElseThrow();

        /*
         * Retain only the time of a recently visible nearby hostile. This is
         * not an entity handle or a hidden position; it permits a bounded
         * first-person sweep after an Enderman/hostile teleports or slips
         * outside one semantic sample. The twelve-block observation radius
         * keeps the timestamp fresh while the hostile remains in the fair
         * view, even though ordinary melee still requires 3.25 blocks.
         */
        if (frame.visibleEntities().stream().anyMatch(entity ->
                (entity.hostile()
                    || "minecraft:iron_golem".equals(entity.entityTypeId())
                        && neutralCombatLeaseActive(frame))
                    && !entity.projectile()
                    && entity.distance()
                        <= HOSTILE_REACQUIRE_OBSERVATION_RADIUS
        )) {
            lastVisibleHostileTick = frame.gameTime();
        }

        if (danger(frame, DangerKind.LOW_AIR).isPresent()
                && frame.inWater()) {
            return surface(frame);
        }
        if (danger(frame, DangerKind.FALLING).isPresent()) {
            return respondToFall(frame);
        }

        Optional<DangerSignal> threat = mostSevereThreat(
                frame,
                visibleHostileProximityManaged,
                physicalContactManaged,
                visibleProjectileThreatManaged
        );
        boolean burning = danger(frame, DangerKind.ON_FIRE).isPresent();
        final double healthRatio = frame.health() / frame.maxHealth();
        final boolean criticalHealth = healthRatio <= 0.40;
        final boolean physicalContact =
                danger(frame, DangerKind.THREAT_CONTACT).isPresent();
        final boolean localAttackCooldown = lastEmergencyAttackTick >= 0L
                && frame.gameTime() - lastEmergencyAttackTick
                    < emergencyAttackIntervalTicks(frame);
        if (!burning
                && localAttackCooldown
                && !visibleHostileProximityManaged
                && !physicalContactManaged) {
            final Optional<ActionHand> cooldownShield = shieldHand(frame);
            if (cooldownShield.isPresent()) {
                /*
                 * Keep the shield up even when an Enderman teleports out of
                 * the current entity sample immediately after a legal hit.
                 * The bounded local attack timestamp and last fair direction
                 * are the only memory used here; neither carries entity
                 * identity or a hidden position.
                 */
                final TickReport guarded = guard(
                        frame,
                        cooldownShield.orElseThrow(),
                        threatDirection(frame, threat),
                        shouldScanForHostile(
                                frame,
                                visibleHostileProximityManaged
                        ),
                        true
                );
                nearestEmergencyMeleeTarget(frame).ifPresentOrElse(
                        target -> emergencyFootwork(frame, target),
                        () -> {
                            if (lastEmergencyAway != null) {
                                emergencyFootwork(frame, lastEmergencyAway);
                            }
                        }
                );
                return guarded;
            }
        }
        /*
         * A golden apple is an emergency health item, not ordinary hunger
         * food. At critical health a merely nearby hostile must not starve
         * this action forever: eat once physical contact has been broken.
         * Fire, falling, drowning and actual contact still take precedence.
         */
        if (criticalHealth && !burning && !physicalContact) {
            final Optional<ActionHand> heldGolden =
                    emergencyGoldenAppleHand(frame);
            if (state == State.EATING && heldGolden.isPresent()) {
                return continueEating(frame);
            }
            if (heldGolden.isPresent()) {
                return startEating(
                        frame,
                        heldGolden.orElseThrow()
                );
            }
            final Optional<String> ownedGolden =
                    preferredOwnedEmergencyGoldenApple(frame);
            if (ownedGolden.isPresent()) {
                return equipEmergencyItem(
                        frame,
                        ActionHand.MAIN_HAND,
                        ownedGolden.orElseThrow(),
                        State.EQUIPPING_FOOD,
                        "equipping_critical_golden_apple"
                );
            }
        }
        if (burning
                || threat.isPresent()
                || shouldScanForHostile(
                    frame,
                    visibleHostileProximityManaged
                )) {
            return avoidDanger(
                    frame,
                    threat,
                    burning,
                    visibleHostileProximityManaged
            );
        }

        if (state == State.EATING) {
            return continueEating(frame);
        }
