Warning: truncated output (original token count: 135598)
Total output lines: 13052

/Users/weida/.zprofile:7: no such file or directory: /opt/homebrew/bin/brew
package dev.mcai.companion.embodiment;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mcai.companion.MinecraftAiCompanion;
import dev.mcai.companion.action.ActionOutcome;
import dev.mcai.companion.action.ActionVec3;
import dev.mcai.companion.action.BlockFace;
import dev.mcai.companion.action.BlockInteractionTarget;
import dev.mcai.companion.action.FairPlayerActuator;
import dev.mcai.companion.action.MovementIntent;
import dev.mcai.companion.brain.BrainObservation;
import dev.mcai.companion.control.GoalSource;
import dev.mcai.companion.control.GoalStatus;
import dev.mcai.companion.model.DecisionEnvelope;
import dev.mcai.companion.model.DecisionKind;
import dev.mcai.companion.model.GatewayStatus;
import dev.mcai.companion.model.ModelGateway;
import dev.mcai.companion.model.ModelOutcome;
import dev.mcai.companion.model.ObservationKind;
import dev.mcai.companion.model.PlannerInput;
import dev.mcai.companion.model.RequestedObservation;
import dev.mcai.companion.model.SkillArgument;
import dev.mcai.companion.navigation.GridPos;
import dev.mcai.companion.perception.PerceptionProvenance;
import dev.mcai.companion.perception.PerceptionVec3;
import dev.mcai.companion.mixin.WorldGenSettingsAccessor;
import dev.mcai.companion.runtime.CompanionRuntime;
import dev.mcai.companion.runtime.RuntimeTickMetrics;
import dev.mcai.companion.runtime.ServerRuntime;
import dev.mcai.companion.skill.SkillContext;
import dev.mcai.companion.skill.SkillSupervisor;
import dev.mcai.companion.skills.building.DynamicShelterSkills;
import dev.mcai.companion.skills.building.BuildingGameTests;
import dev.mcai.companion.skills.bridging.BridgeSkills;
import dev.mcai.companion.skills.combat.CombatSkills;
import dev.mcai.companion.skills.core.CoreSkills;
import dev.mcai.companion.skills.core.EmergencySurvivalController;
import dev.mcai.companion.skills.core.TravelSkills;
import dev.mcai.companion.skills.exploration.ExplorationSkills;
import dev.mcai.companion.skills.farming.FarmingGameTests;
import dev.mcai.companion.skills.farming.FarmingSkills;
import dev.mcai.companion.skills.foundation.FoundationGameTests;
import dev.mcai.companion.skills.foundation
        .ShelterMaterialExplorationGameTests;
import dev.mcai.companion.skills.foundation
        .WorkstationPrerequisiteGameTests;
import dev.mcai.companion.skills.gathering.ResourceGatheringSkills;
import dev.mcai.companion.skills.inventory.InventoryGameTests;
import dev.mcai.companion.skills.inventory.InventorySkills;
import dev.mcai.companion.skills.interaction.FairInteractionSkills;
import dev.mcai.companion.skills.loot.LootSkills;
import dev.mcai.companion.skills.loot.LootGameTests;
import dev.mcai.companion.skills.loot.SecureEnderPearlReserveSkill;
import dev.mcai.companion.communication.LiveModelChatGameTests;
import dev.mcai.companion.skills.menu.MenuGameTests;
import dev.mcai.companion.skills.redstone.RedstoneGameTests;
import dev.mcai.companion.skills.menu.MenuSkills;
import dev.mcai.companion.skills.memory.MemorySkills;
import dev.mcai.companion.skills.parkour.ParkourSkills;
import dev.mcai.companion.skills.portal.PortalBuildSkills;
import dev.mcai.companion.skills.portal.PortalSkills;
import dev.mcai.companion.skills.progress.ProgressSkills;
import dev.mcai.companion.skills.sleeping.SleepSkills;
import dev.mcai.companion.skills.survey.SurveySkills;
import dev.mcai.companion.skills.stronghold.EyeTraceSnapshot;
import dev.mcai.companion.skills.stronghold
        .SearchObservedStrongholdPortalRoomSkill;
import dev.mcai.companion.skills.stronghold.StrongholdSkills;
import dev.mcai.companion.waypoint.DimensionRef;
import dev.mcai.companion.skills.transport.BoatTransportSkills;
import dev.mcai.companion.skills.transport.MinecartTransportSkills;
import dev.mcai.companion.skin.AiProfileMarker;
import dev.mcai.companion.progression.SurvivalMilestone;
import dev.mcai.companion.progression.ServerShelterEvidenceVerifier;
import dev.mcai.companion.progression.ServerFoundationEvidenceVerifier;
import dev.mcai.companion.progression.VerifiedFixtureLocation;
import dev.mcai.companion.progression.VerifiedFoundationEvidence;
import dev.mcai.companion.progression.VerifiedShelterEvidence;
import dev.mcai.companion.world.CompanionWorldData;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.stats.Stats;
import net.minecraftforge.gametest.GameTest;
import net.minecraftforge.gametest.GameTestDontPrefix;
import net.minecraftforge.gametest.GameTestNamespace;

/**
 * Runs inside the real dedicated GameTest server, not a mocked JVM.
 */
@GameTestNamespace(MinecraftAiCompanion.MOD_ID)
@GameTestDontPrefix
public final class EmbodimentGameTests {
    /**
     * Keeps every overworld fixture, including the separated stronghold-eye
     * triangulation baseline, inside the GameTest barrier.  The former
     * 3x3x3 default put the companion against the south wall, so a valid
     * vanilla movement input could advance only 0.2 blocks.
     */
    private static final String TEST_STRUCTURE = "forge:empty160x24x192";
    private static final BlockPos TEST_ORIGIN = new BlockPos(80, 8, 80);
    private static final long TEST_MAX_TICKS = 12_000L;
    private static final String FOCUSED_TEST_STRUCTURE =
        "forge:empty48x32x48";
    private static final BlockPos FOCUSED_TEST_ORIGIN =
        new BlockPos(16, 8, 16);
    private static final int FOCUSED_BODY_START_TIMEOUT_TICKS = 3_000;
    private static final int FOCUSED_SIMULATION_TICKET_TIMEOUT_TICKS =
        3_000;
    private static final int CROSS_DIMENSION_TICKET_TEST_MAX_TICKS =
        20_000;
    private static final long FOCUSED_SIMULATION_TICKET_TIMEOUT_NANOS =
        java.time.Duration.ofSeconds(15).toNanos();
    private static final long FOCUSED_ASYNC_CHUNK_YIELD_NANOS =
        java.time.Duration.ofMillis(1).toNanos();
    private static final int WATER_TEST_MAX_TICKS = 4_000;
    private static final int PARKOUR_TEST_MAX_TICKS = 5_000;
    private static final int TRAVEL_DETOUR_TEST_MAX_TICKS = 5_000;
    private static final int PORTAL_RETURN_TEST_MAX_TICKS = 6_000;
    private static final int BLAZE_TEST_MAX_TICKS = 20_000;
    private static final int BLAZE_RESERVE_TEST_MAX_TICKS = 20_000;
    private static final int ENDER_SINGLE_TEST_MAX_TICKS = 4_000;
    private static final int ENDER_RESERVE_TEST_MAX_TICKS = 30_000;
    private static final int STRONGHOLD_TEST_MAX_TICKS = 8_000;
    private static final int STRONGHOLD_REACH_TEST_MAX_TICKS = 10_000;
    private static final int END_PORTAL_TEST_MAX_TICKS = 5_000;
    private static final int END_VICTORY_TEST_MAX_TICKS = 8_000;
    private static final int OFFLINE_DRAGON_FIGHT_WINDOW_TICKS = 4_000;
    /* The production cue is valid through tick 40 inclusive; allow a few
     * semantic publication ticks for the fixture to observe its expiry. */
    private static final int RECENT_DAMAGE_SETTLE_MAX_TICKS = 100;
    private static final double PHYSICAL_CAGE_BREAK_REACH = 4.5;
    private static final double PHYSICAL_END_CRYSTAL_STANDOFF = 12.25;
    private static final int EMERGENCY_ENDERMAN_TEST_MAX_TICKS = 1_200;
    private static final int EMERGENCY_SLIME_TEST_MAX_TICKS = 1_600;
    private static final int EMERGENCY_GOLEM_TEST_MAX_TICKS = 3_000;
    private static final int EMERGENCY_HORDE_TEST_MAX_TICKS = 2_000;
    private static final double EMERGENCY_SAFE_STANDOFF_DISTANCE = 8.0D;

    private EmbodimentGameTests() {
    }

    @GameTest(
        name = "realtime_clock_contract",
        structure = FOCUSED_TEST_STRUCTURE,
        maxTicks = 320,
        padding = 8
    )
    public static void realtimeClockContract(
            final GameTestHelper helper
    ) {
        if (!Boolean.getBoolean("mcai.realtimeGameTest")) {
            helper.succeed();
            return;
        }
        final long started = System.nanoTime();
        final AtomicLong maximumScheduleLagNanos = new AtomicLong();
        /*
         * Real servers spend time doing work inside each tick. The throttle
         * must count that work toward the 50 ms budget instead of adding a
         * fresh 50 ms sleep after it. A small deterministic load exposes the
         * old cumulative-drift bug without depending on model/network speed.
         */
        for (long tick = 1L; tick <= 260L; tick++) {
            final long scheduledTick = tick;
            helper.runAtTickTime(scheduledTick, () -> {
                LockSupport.parkNanos(
                        java.time.Duration.ofMillis(8).toNanos()
                );
                if (scheduledTick >= 20L) {
                    final long scheduleLag = System.nanoTime()
                        - helper.getLevel().getServer()
                            .getNextTickTime();
                    maximumScheduleLagNanos.accumulateAndGet(
                            Math.max(0L, scheduleLag),
                            Math::max
                    );
                }
            });
        }
        helper.runAtTickTime(261, () -> {
            final long elapsed = System.nanoTime() - started;
            helper.assertTrue(
                    elapsed >= java.time.Duration.ofSeconds(11)
                        .toNanos(),
                    "Real-time GameTest clock advanced 261 ticks in only "
                        + java.time.Duration.ofNanos(elapsed).toMillis()
                        + " ms"
            );
            helper.assertTrue(
                    elapsed <= java.time.Duration.ofMillis(13_750)
                        .toNanos(),
                    "Tick work was added after the 50 ms budget; 261 ticks "
                        + "took "
                        + java.time.Duration.ofNanos(elapsed).toMillis()
                        + " ms"
            );
            helper.assertTrue(
                    maximumScheduleLagNanos.get()
                        <= java.time.Duration.ofMillis(750).toNanos(),
                    "Real-time GameTest schedule accumulated "
                        + java.time.Duration.ofNanos(
                                maximumScheduleLagNanos.get()
                            ).toMillis()
                        + " ms of lag"
            );
            helper.succeed();
        });
    }

    /**
     * Physical regression for the field failure where a shielded companion
     * guarded forever and let one Enderman kill it. The production arbiter,
     * emergency controller, menu equipment transaction and melee actuator all
     * run on the real headless survival player; this test never damages the
     * target or edits either combatant after the encounter starts. Endermen
     * may naturally teleport, so a clean, damage-free separation is a valid
     * defensive outcome alongside a defeated target.
     */
    @GameTest(
        name = "real_emergency_enderman_defense",
        environment = "exclusive_real_emergency_enderman_defense",
        structure = FOCUSED_TEST_STRUCTURE,
        maxTicks = EMERGENCY_ENDERMAN_TEST_MAX_TICKS,
        skyAccess = true,
        padding = 8
    )
    public static void realEmergencyEndermanDefense(
            final GameTestHelper helper
    ) {
        final var server = helper.getLevel().getServer();
        final AtomicInteger phase = new AtomicInteger();
        final AtomicLong phaseStarted = new AtomicLong(helper.getTick());
        final AtomicReference<Mob> enderman = new AtomicReference<>();
        final AtomicReference<Vec3> bodyStart = new AtomicReference<>();
        final AtomicInteger evidence = new AtomicInteger();
        final AtomicLong lastEmergencyTick =
                new AtomicLong(helper.getTick());
        final AtomicReference<Float> lastObservedHealth =
                new AtomicReference<>();
        final AtomicLong lastHealthChangeTick =
                new AtomicLong(helper.getTick());
        final AtomicInteger guardingTicks = new AtomicInteger();
        final AtomicInteger maximumShieldTicks = new AtomicInteger();
        final AtomicInteger consecutiveShieldTicks = new AtomicInteger();
        final AtomicReference<Double> maximumDistanceFromStart =
                new AtomicReference<>(0.0D);

        helper.addCleanup(ignored -> {
            final Mob target = enderman.get();
            if (target != null && !target.isRemoved()) {
                target.discard();
            }
            CompanionRuntime.active()
                    .filter(runtime -> runtime.server() == server)
                    .ifPresent(runtime -> {
                        final GoalStatus goalStatus =
                                runtime.goals().snapshot().status();
                        if (goalStatus == GoalStatus.RUNNING
                                || goalStatus
                                    == GoalStatus.CANCEL_PENDING) {
                            runtime.goals().markTerminal(
                                    GoalStatus.SAFE_IDLE,
                                    "enderman_gate_cleanup"
                            );
                        }
                        runtime.survival().reset();
                        runtime.coreActions().quiesceNow();
                        runtime.skillSupervisor()
                                .abandonForSessionEnd();
                    });
            if (AiPlayerManager.status(server).state()
                    != SessionState.ABSENT) {
                AiPlayerManager.requestRemove(server);
            }
        });

        final var spawn = GameTestCompanionSpawn.request(
                helper,
                FOCUSED_TEST_ORIGIN
        );
        helper.assertTrue(
                spawn.accepted(),
                "Enderman-defense body spawn was rejected: "
                    + spawn.code()
        );
        scheduleEveryTick(
                helper,
                EMERGENCY_ENDERMAN_TEST_MAX_TICKS,
                () -> {
                    final long now = helper.getTick();
                    if (phase.get() == 0) {
                        final var status =
                                AiPlayerManager.status(server);
                        helper.assertTrue(
                                status.state() != SessionState.FAILED,
                                "Enderman-defense body failed: "
                                    + status
                        );
                        if (status.state() != SessionState.ACTIVE
                                || !status.online()) {
                            helper.assertTrue(
                                    now - phaseStarted.get()
                                        <= FOCUSED_BODY_START_TIMEOUT_TICKS,
                                    "Enderman-defense body timed out"
                            );
                            return;
                        }
                        final var runtime = CompanionRuntime.active()
                                .filter(candidate ->
                                        candidate.server() == server)
                                .orElseThrow(() ->
                                        helper.assertionException(
                                            "Enderman-defense runtime "
                                                + "is unavailable"
                                        ));
                        runtime.survival().reset();
                        runtime.coreActions().quiesceNow();
                        runtime.skillSupervisor()
                                .abandonForSessionEnd();
                        /*
                         * Deliberately do not install a test gateway here.
                         * This is the regression for the real field failure
                         * where an invalid/missing model credential used to
                         * skip even local survival. The runtime must keep
                         * the fair emergency lane alive while the high-level
                         * model lane remains unavailable.
                         */
                        runtime.brain().close();
                        final var goal = runtime.goals().setGoal(
                                "Survive the physical Enderman encounter",
                                GoalSource.RECOVERY
                        );
                        helper.assertTrue(
                                goal.accepted(),
                                "Enderman-defense goal was rejected: "
                                    + goal.code()
                        );

                        final var body = AiPlayerManager
                                .onlinePlayer(server)
                                .orElseThrow();
                        final BlockPos center =
                                helper.absolutePos(
                                    FOCUSED_TEST_ORIGIN
                                );
                        for (int x = -8; x <= 8; x++) {
                            for (int z = -8; z <= 8; z++) {
                                helper.getLevel()
                                        .setBlockAndUpdate(
                                            center.offset(x, -1, z),
                                            Blocks.SMOOTH_STONE
                                                .defaultBlockState()
                                        );
                                for (int y = 0; y <= 5; y++) {
                                    helper.getLevel()
                                            .setBlockAndUpdate(
                                                center.offset(x, y, z),
                                                Blocks.AIR
                                                    .defaultBlockState()
                                            );
                                }
                            }
                        }
                        body.teleportTo(
                                center.getX() + 0.5D,
                                center.getY(),
                                center.getZ() + 0.5D
                        );
                        body.setGameMode(GameType.SURVIVAL);
                        body.getInventory().clearContent();
                        body.getInventory().setItem(
                                0,
                                new ItemStack(
                                        Items.COOKED_BEEF,
                                        3
                                )
                        );
                        body.getInventory().setItem(
                                3,
                                new ItemStack(Items.DIAMOND_SWORD)
                        );
                        body.getInventory().setSelectedSlot(0);
                        body.setItemSlot(
                                EquipmentSlot.OFFHAND,
                                new ItemStack(Items.SHIELD)
                        );
                        body.setItemSlot(
                                EquipmentSlot.HEAD,
                                new ItemStack(Items.DIAMOND_HELMET)
                        );
                        body.setItemSlot(
                                EquipmentSlot.CHEST,
                                new ItemStack(
                                        Items.DIAMOND_CHESTPLATE
                                )
                        );
                        body.setItemSlot(
                                EquipmentSlot.LEGS,
                                new ItemStack(
                                        Items.DIAMOND_LEGGINGS
                                )
                        );
                        body.setItemSlot(
                                EquipmentSlot.FEET,
                                new ItemStack(Items.DIAMOND_BOOTS)
                        );
                        body.setHealth(12.0F);
                        body.getFoodData().setFoodLevel(20);
                        body.getFoodData().setSaturation(5.0F);
                        body.setDeltaMovement(Vec3.ZERO);
                        body.fallDistance = 0.0F;
                        body.inventoryMenu.broadcastChanges();
                        lastObservedHealth.set(body.getHealth());
                        lastHealthChangeTick.set(now);

                        final net.minecraft.world.entity.monster.EnderMan
                                target = EntityTypes.ENDERMAN.create(
                                    helper.getLevel(),
                                    EntitySpawnReason.COMMAND
                                );
                        helper.assertTrue(
                                target != null,
                                "Could not create physical Enderman"
                        );
                        target.setPos(
                                center.getX() + 2.25D,
                                center.getY(),
                                center.getZ() + 0.5D
                        );
                        target.setPersistenceRequired();
                        helper.assertTrue(
                                helper.getLevel()
                                    .addFreshEntity(target),
                                "Could not add physical Enderman"
                        );
                        target.setPersistentAngerTarget(
                                net.minecraft.world.entity
                                    .EntityReference.of(body)
                        );
                        target.setTimeToRemainAngry(1_200L);
                        target.setTarget(body);
                        enderman.set(target);
                        body.lookAt(
                                EntityAnchorArgument.Anchor.EYES,
                                target.position().add(
                                        0.0D,
                                        1.0D,
                                        0.0D
                                )
                        );
                        body.setYHeadRot(body.getYRot());
                        bodyStart.set(body.position());
                        phase.set(1);
                        phaseStarted.set(now);
                        lastEmergencyTick.set(now);
                        return;
                    }

                    final var runtime = CompanionRuntime.active()
                            .orElseThrow();
                    final var body = AiPlayerManager
                            .onlinePlayer(server)
                            .orElseThrow();
                    final Mob target = enderman.get();
                    if (runtime.survival().state()
                            == EmergencySurvivalController.State.GUARDING) {
                        guardingTicks.incrementAndGet();
                    }
                    final float health = body.getHealth();
                    final Float previousHealth =
                            lastObservedHealth.getAndSet(health);
                    if (previousHealth != null
                            && Math.abs(previousHealth - health) > 1.0E-4F) {
                        lastHealthChangeTick.set(now);
                    }
                    helper.assertTrue(
                            body.isAlive()
                                && !body.isDeadOrDying(),
                            "Shielded companion was killed by one "
                                + "Enderman: health="
                                + body.getHealth()
                                + ", position=" + body.position()
                                + ", velocity=" + body.getDeltaMovement()
                                + ", onGround=" + body.onGround()
                                + ", targetPosition="
                                + (target == null ? "null" : target.position())
                                + ", survival="
                                + runtime.survival().state()
                    );
                    if (body.getMainHandItem()
                            .is(Items.DIAMOND_SWORD)) {
                        evidence.getAndUpdate(bits -> bits | 1);
                        if (body.getMainHandItem()
                                .getDamageValue() > 0) {
                            evidence.getAndUpdate(bits -> bits | 2);
                        }
                    }
                    if (body.isUsingItem()
                            && body.getUseItem().is(Items.SHIELD)) {
                        evidence.getAndUpdate(bits -> bits | 4);
                        final int continuous =
                                consecutiveShieldTicks.incrementAndGet();
                        maximumShieldTicks.accumulateAndGet(
                                continuous,
                                Math::max
                        );
                    } else {
                        consecutiveShieldTicks.set(0);
                    }
                    runtime.behaviorArbiter().latest()
                            .filter(resolution ->
                                    resolution.claimedBy(
                                        dev.mcai.companion.control
                                            .BehaviorArbiter.Lane
                                            .EMERGENCY_SURVIVAL
                                    ))
                            .ifPresent(resolution ->
                                    {
                                        evidence.getAndUpdate(
                                            bits -> bits | 8
                                        );
                                        lastEmergencyTick.set(now);
                                    });
                    final double distanceFromStart = body.position()
                            .distanceTo(bodyStart.get());
                    maximumDistanceFromStart.accumulateAndGet(
                            distanceFromStart,
                            Math::max
                    );
                    /*
                     * Net displacement is not a reliable footwork oracle:
                     * vanilla knockback and an Enderman teleport can return
                     * the body to its starting cell after a legal backstep.
                     * Require observed excursion from the original pose while
                     * retaining the ordinary movement/physics path.
                     */
                    if (maximumDistanceFromStart.get() >= 0.25D) {
                        evidence.getAndUpdate(bits -> bits | 16);
                    }
                    final boolean defeated = target == null
                            || target.isRemoved()
                            || !target.isAlive();
                    final boolean repelled = !defeated
                            && target.getHealth()
                                <= target.getMaxHealth() * 0.50F
                            && now - lastEmergencyTick.get() >= 40L;
                    final boolean safelySeparated = !defeated
                            && target.distanceTo(body)
                                >= EMERGENCY_SAFE_STANDOFF_DISTANCE
                            && now - lastHealthChangeTick.get() >= 40L
                            && now - lastEmergencyTick.get() >= 40L;
                    if (defeated || repelled || safelySeparated) {
                        helper.assertTrue(
                                (evidence.get() & 31) == 31,
                                "Enderman defense lacked normal "
                                    + "weapon equip/durability, emergency "
                                    + "ownership and footwork; evidence="
                                    + evidence.get()
                                    + ", maximumShieldTicks="
                                    + maximumShieldTicks.get()
                                    + ", guardingTicks=" + guardingTicks.get()
                                    + ", maximumDistanceFromStart="
                                    + maximumDistanceFromStart.get()
                                    + ", position=" + body.position()
                                    + ", survival=" + runtime.survival().state()
                                    + ", using=" + body.isUsingItem()
                                    + ", useItem=" + body.getUseItem().getHoverName().getString()
                                    + ", clientLoaded=" + body.connection.hasClientLoaded()
                        );
                        helper.succeed();
                        return;
                    }
                    helper.assertTrue(
                            now - phaseStarted.get() <= 500L,
                            "Physical Enderman defense timed out: "
                                + "targetHealth=" + target.getHealth()
                                + ", bodyHealth=" + body.getHealth()
                                + ", targetDistance="
                                + target.distanceTo(body)
                                + ", stableHealthTicks="
                                + (now - lastHealthChangeTick.get())
                                + ", evidence=" + evidence.get()
                                + ", survival="
                                + runtime.survival().state()
                                + ", arbiter="
                                + runtime.behaviorArbiter().latest()
                    );
                }
        );
    }

    /**
     * Physical regression for a hostile that begins behind the companion.
     * This catches a deceptive shield state where telemetry said
     * {@code GUARDING}, but the controller released and restarted use every
     * tick so vanilla never completed the shield warmup.
     */
    @GameTest(
        name = "real_emergency_slime_defense",
        environment = "exclusive_real_emergency_slime_defense",
        structure = FOCUSED_TEST_STRUCTURE,
        maxTicks = EMERGENCY_SLIME_TEST_MAX_TICKS,
        skyAccess = true,
        padding = 8
    )
    public static void realEmergencySlimeDefense(
            final GameTestHelper helper
    ) {
        final var server = helper.getLevel().getServer();
        final AtomicInteger phase = new AtomicInteger();
        final AtomicLong phaseStarted = new AtomicLong(helper.getTick());
        final AtomicReference<Mob> slime = new AtomicReference<>();
        final AtomicReference<Vec3> bodyStart = new AtomicReference<>();
        final AtomicInteger evidence = new AtomicInteger();
        final AtomicInteger consecutiveShieldTicks = new AtomicInteger();
        final AtomicInteger maximumShieldTicks = new AtomicInteger();
        final AtomicInteger guardingTicks = new AtomicInteger();

        helper.addCleanup(ignored -> {
            final Mob target = slime.get();
            if (target != null && !target.isRemoved()) {
                target.discard();
            }
            CompanionRuntime.active()
                    .filter(runtime -> runtime.server() == server)
                    .ifPresent(runtime -> {
                        final GoalStatus goalStatus =
                                runtime.goals().snapshot().status();
                        if (goalStatus == GoalStatus.RUNNING
                                || goalStatus
                                    == GoalStatus.CANCEL_PENDING) {
                            runtime.goals().markTerminal(
                                    GoalStatus.SAFE_IDLE,
                                    "slime_gate_cleanup"
                            );
                        }
                        runtime.survival().reset();
                        runtime.coreActions().quiesceNow();
                        runtime.skillSupervisor()
                                .abandonForSessionEnd();
                    });
            if (AiPlayerManager.status(server).state()
                    != SessionState.ABSENT) {
                AiPlayerManager.requestRemove(server);
            }
        });

        final var spawn = GameTestCompanionSpawn.request(
                helper,
                FOCUSED_TEST_ORIGIN
        );
        helper.assertTrue(
                spawn.accepted(),
                "Slime-defense body spawn was rejected: "
                    + spawn.code()
        );
        scheduleEveryTick(
                helper,
                EMERGENCY_SLIME_TEST_MAX_TICKS,
                () -> {
                    final long now = helper.getTick();
                    if (phase.get() == 0) {
                        final var status =
                                AiPlayerManager.status(server);
                        helper.assertTrue(
                                status.state() != SessionState.FAILED,
                                "Slime-defense body failed: " + status
                        );
                        if (status.state() != SessionState.ACTIVE
                                || !status.online()) {
                            helper.assertTrue(
                                    now - phaseStarted.get()
                                        <= FOCUSED_BODY_START_TIMEOUT_TICKS,
                                    "Slime-defense body timed out"
                            );
                            return;
                        }
                        final var runtime = CompanionRuntime.active()
                                .filter(candidate ->
                                        candidate.server() == server)
                                .orElseThrow(() ->
                                        helper.assertionException(
                                            "Slime-defense runtime "
                                                + "is unavailable"
                                        ));
                        runtime.survival().reset();
                        runtime.coreActions().quiesceNow();
                        runtime.skillSupervisor()
                                .abandonForSessionEnd();
                        /*
                         * Deliberately do not install a test gateway here.
                         * This regression must exercise the production
                         * no-credential path: high-level model control is
                         * unavailable, but the fair local survival lane
                         * must still respond to the real hostile.
                         */
                        runtime.brain().close();
                        final var goal = runtime.goals().setGoal(
                                "Survive the physical Slime encounter",
                                GoalSource.RECOVERY
                        );
                        helper.assertTrue(
                                goal.accepted(),
                                "Slime-defense goal was rejected: "
                                    + goal.code()
                        );

                        final var body = AiPlayerManager
                                .onlinePlayer(server)
                                .orElseThrow();
                        final BlockPos center = helper.absolutePos(
                                FOCUSED_TEST_ORIGIN
                        );
                        for (int x = -8; x <= 8; x++) {
                            for (int z = -8; z <= 8; z++) {
                                helper.getLevel().setBlockAndUpdate(
                                        center.offset(x, -1, z),
                                        Blocks.SMOOTH_STONE
                                            .defaultBlockState()
                                );
                                for (int y = 0; y <= 5; y++) {
                                    helper.getLevel().setBlockAndUpdate(
                                            center.offset(x, y, z),
                                            Blocks.AIR
                                                .defaultBlockState()
                                    );
                                }
                            }
                        }
                        body.teleportTo(
                                center.getX() + 0.5D,
                                center.getY(),
                                center.getZ() + 0.5D
                        );
                        body.setGameMode(GameType.SURVIVAL);
                        body.getInventory().clearContent();
                        body.getInventory().setItem(
                                0,
                                new ItemStack(Items.COOKED_BEEF, 3)
                        );
                        body.getInventory().setItem(
                                3,
                                new ItemStack(Items.DIAMOND_SWORD)
                        );
                        body.getInventory().setSelectedSlot(0);
                        body.setItemSlot(
                                EquipmentSlot.OFFHAND,
                                new ItemStack(Items.SHIELD)
                        );
                        body.setItemSlot(
                                EquipmentSlot.HEAD,
                                new ItemStack(Items.DIAMOND_HELMET)
                        );
                        body.setItemSlot(
                                EquipmentSlot.CHEST,
                                new ItemStack(Items.DIAMOND_CHESTPLATE)
                        );
                        body.setItemSlot(
                                EquipmentSlot.LEGS,
                                new ItemStack(Items.DIAMOND_LEGGINGS)
                        );
                        body.setItemSlot(
                                EquipmentSlot.FEET,
                                new ItemStack(Items.DIAMOND_BOOTS)
                        );
                        body.setHealth(body.getMaxHealth());
                        body.getFoodData().setFoodLevel(20);
                        body.getFoodData().setSaturation(5.0F);
                        body.setDeltaMovement(Vec3.ZERO);
                        body.fallDistance = 0.0F;
                        body.inventoryMenu.broadcastChanges();

                        final var target = EntityTypes.SLIME.create(
                                helper.getLevel(),
                                EntitySpawnReason.COMMAND
                        );
                        helper.assertTrue(
                                target != null,
                                "Could not create physical Slime"
                        );
                        target.setSize(3, true);
                        target.setPos(
                                center.getX() + 0.5D,
                                center.getY(),
                                center.getZ() + 2.25D
                        );
                        target.setPersistenceRequired();
                        helper.assertTrue(
                                helper.getLevel().addFreshEntity(target),
                                "Could not add physical Slime"
                        );
                        target.setTarget(body);
                        slime.set(target);
                        body.lookAt(
                                EntityAnchorArgument.Anchor.EYES,
                                body.position().add(0.0D, 0.0D, -5.0D)
                        );
                        body.setYHeadRot(body.getYRot());
                        bodyStart.set(body.position());
                        phase.set(1);
                        phaseStarted.set(now);
                        return;
                    }

                    final var runtime = CompanionRuntime.active()
                            .orElseThrow();
                    final var body = AiPlayerManager
                            .onlinePlayer(server)
                            .orElseThrow();
                    final Mob target = slime.get();
                    if (runtime.survival().state()
                            == EmergencySurvivalController.State.GUARDING) {
                        guardingTicks.incrementAndGet();
                    }
                    helper.assertTrue(
                            body.isAlive() && !body.isDeadOrDying(),
                            "Shielded companion was killed by one Slime: "
                                + "health=" + body.getHealth()
                                + ", survival="
                                + runtime.survival().state()
                    );
                    if (body.isUsingItem()
                            && body.getUseItem().is(Items.SHIELD)) {
                        final int continuous =
                                consecutiveShieldTicks.incrementAndGet();
                        maximumShieldTicks.accumulateAndGet(
                                continuous,
                                Math::max
                        );
                        evidence.getAndUpdate(bits -> bits | 4);
                    } else {
                        consecutiveShieldTicks.set(0);
                    }
                    if (body.getMainHandItem()
                            .is(Items.DIAMOND_SWORD)) {
                        evidence.getAndUpdate(bits -> bits | 1);
                        if (body.getMainHandItem()
                                .getDamageValue() > 0) {
                            evidence.getAndUpdate(bits -> bits | 2);
                        }
                    }
                    runtime.behaviorArbiter().latest()
                            .filter(resolution ->
                                    resolution.claimedBy(
                                        dev.mcai.companion.control
                                            .BehaviorArbiter.Lane
                                            .EMERGENCY_SURVIVAL
                                    ))
                            .ifPresent(resolution ->
                                    evidence.getAndUpdate(
                                        bits -> bits | 8
                                    ));
                    if (body.position().distanceTo(
                            bodyStart.get()
                    ) >= 0.25D) {
                        evidence.getAndUpdate(bits -> bits | 16);
                    }

                    final boolean defeated = target == null
                            || target.isRemoved()
                            || !target.isAlive();
                    if (defeated) {
                        helper.assertTrue(
                                (evidence.get() & 31) == 31,
                                "Slime defense lacked normal shield, "
                                    + "weapon durability, emergency "
                                    + "ownership or footwork; evidence="
                                    + evidence.get()
                                    + ", maximumShieldTicks="
                                    + maximumShieldTicks.get()
                                    + ", guardingTicks=" + guardingTicks.get()
                                    + ", survival=" + runtime.survival().state()
                                    + ", using=" + body.isUsingItem()
                                    + ", useItem=" + body.getUseItem().getHoverName().getString()
                                    + ", clientLoaded=" + body.connection.hasClientLoaded()
                        );
                        helper.assertTrue(
                                maximumShieldTicks.get() >= 5,
                                "Slime defense never held the shield "
                                    + "through vanilla warmup; max="
                                    + maximumShieldTicks.get()
                        );
                        helper.succeed();
                        return;
                    }
                    helper.assertTrue(
                            now - phaseStarted.get() <= 700L,
                            "Physical Slime defense timed out: "
                                + "targetHealth=" + target.getHealth()
                                + ", bodyHealth=" + body.getHealth()
                                + ", evidence=" + evidence.get()
                                + ", maximumShieldTicks="
                                + maximumShieldTicks.get()
                                + ", guardingTicks=" + guardingTicks.get()
                                + ", survival="
                                + runtime.survival().state()
                    );
                }
        );
    }

    /**
     * Regression for a particularly confusing field symptom: a hostile is
     * already touching the body while the body is looking away, so a
     * first-person view-cone sample contains no entity.  The emergency lane
     * must use the real collision cue to reacquire the target, turn through
     * the ordinary input path, and dispatch a normal attack instead of only
     * speaking/guarding until death.  No model gateway is installed here.
     */
    @GameTest(
        name = "real_emergency_contact_reacquisition",
        environment = "exclusive_real_emergency_contact_reacquisition",
        structure = FOCUSED_TEST_STRUCTURE,
        maxTicks = 1_200,
        skyAccess = true,
        padding = 8
    )
    public static void realEmergencyContactReacquisition(
            final GameTestHelper helper
    ) {
        final var server = helper.getLevel().getServer();
        final AtomicInteger phase = new AtomicInteger();
        final AtomicLong phaseStarted = new AtomicLong(helper.getTick());
        final AtomicReference<Mob> target = new AtomicReference<>();
        final AtomicReference<Vec3> bodyStart = new AtomicReference<>();
        final AtomicInteger evidence = new AtomicInteger();

        helper.addCleanup(ignored -> {
            final Mob current = target.get();
            if (current != null && !current.isRemoved()) {
                current.discard();
            }
            CompanionRuntime.active()
                    .filter(runtime -> runtime.server() == server)
                    .ifPresent(runtime -> {
                        runtime.survival().reset();
                        runtime.coreActions().quiesceNow();
                        runtime.skillSupervisor().abandonForSessionEnd();
                    });
            if (AiPlayerManager.status(server).state()
                    != SessionState.ABSENT) {
                AiPlayerManager.requestRemove(server);
            }
        });

        final var spawn = GameTestCompanionSpawn.request(
                helper,
                FOCUSED_TEST_ORIGIN
        );
        helper.assertTrue(
                spawn.accepted(),
                "Contact-reacquisition body spawn was rejected: "
                    + spawn.code()
        );
        scheduleEveryTick(
                helper,
                1_200,
                () -> {
                    final long now = helper.getTick();
                    if (phase.get() == 0) {
                        final var status = AiPlayerManager.status(server);
                        helper.assertTrue(
                                status.state() != SessionState.FAILED,
                                "Contact-reacquisition body failed: "
                                    + status
                        );
                        if (status.state() != SessionState.ACTIVE
                                || !status.online()) {
                            helper.assertTrue(
                                    now - phaseStarted.get()
                                        <= FOCUSED_BODY_START_TIMEOUT_TICKS,
                                    "Contact-reacquisition body timed out"
                            );
                            return;
                        }
                        final var runtime = CompanionRuntime.active()
                                .filter(candidate ->
                                        candidate.server() == server)
                                .orElseThrow(() -> helper.assertionException(
                                        "Contact-reacquisition runtime unavailable"
                                ));
                        runtime.survival().reset();
                        runtime.coreActions().quiesceNow();
                        runtime.skillSupervisor().abandonForSessionEnd();
                        runtime.brain().close();
                        final var goal = runtime.goals().setGoal(
                                "Survive a hostile contact",
                                GoalSource.RECOVERY
                        );
                        helper.assertTrue(
                                goal.accepted(),
                                "Contact-reacquisition goal was rejected: "
                                    + goal.code()
                        );

                        final BlockPos center = helper.absolutePos(
                                FOCUSED_TEST_ORIGIN
                        );
                        final var level = helper.getLevel();
                        for (int x = -6; x <= 6; x++) {
                            for (int z = -6; z <= 6; z++) {
                                level.setBlockAndUpdate(
                                        center.offset(x, -1, z),
                                        Blocks.SMOOTH_STONE.defaultBlockState()
                                );
                                for (int y = 0; y <= 4; y++) {
                                    level.setBlockAndUpdate(
                                            center.offset(x, y, z),
                                            Blocks.AIR.defaultBlockState()
                                    );
                                }
                            }
                        }
                        final var body = AiPlayerManager.onlinePlayer(server)
                                .orElseThrow();
                        body.teleportTo(
                                center.getX() + 0.5D,
                                center.getY(),
                                center.getZ() + 0.5D
                        );
                        body.setGameMode(GameType.SURVIVAL);
                        body.getInventory().clearContent();
                        body.getInventory().setItem(
                                0,
                                new ItemStack(Items.DIAMOND_SWORD)
                        );
                        body.getInventory().setSelectedSlot(0);
                        body.setItemSlot(
                                EquipmentSlot.OFFHAND,
                                new ItemStack(Items.SHIELD)
                        );
                        body.setItemSlot(
                                EquipmentSlot.HEAD,
                                new ItemStack(Items.DIAMOND_HELMET)
                        );
                        body.setItemSlot(
                                EquipmentSlot.CHEST,
                                new ItemStack(Items.DIAMOND_CHESTPLATE)
                        );
                        body.setItemSlot(
                                EquipmentSlot.LEGS,
                                new ItemStack(Items.DIAMOND_LEGGINGS)
                        );
                        body.setItemSlot(
                                EquipmentSlot.FEET,
                                new ItemStack(Items.DIAMOND_BOOTS)
                        );
                        body.setHealth(body.getMaxHealth());
                        body.getFoodData().setFoodLevel(20);
                        body.setDeltaMovement(Vec3.ZERO);
                        body.fallDistance = 0.0F;
                        body.inventoryMenu.broadcastChanges();

                        final Mob zombie = EntityTypes.ZOMBIE.create(
                                level,
                                EntitySpawnReason.COMMAND
                        );
                        helper.assertTrue(
                                zombie != null,
                                "Could not create contact zombie"
                        );
                        zombie.setPos(
                                center.getX() + 0.5D,
                                center.getY(),
                                center.getZ() + 0.45D
                        );
                        zombie.setPersistenceRequired();
                        helper.assertTrue(
                                level.addFreshEntity(zombie),
                                "Could not add contact zombie"
                        );
                        zombie.setTarget(body);
                        target.set(zombie);
                        /* Face the empty side of the arena, not the mob. */
                        body.lookAt(
                                EntityAnchorArgument.Anchor.EYES,
                                body.position().add(0.0D, 0.0D, -6.0D)
                        );
                        body.setYHeadRot(body.getYRot());
                        bodyStart.set(body.position());
                        phase.set(1);
                        phaseStarted.set(now);
                        return;
                    }

                    final var runtime = CompanionRuntime.active()
                            .orElseThrow();
                    final var body = AiPlayerManager.onlinePlayer(server)
                            .orElseThrow();
                    final Mob zombie = target.get();
                    helper.assertTrue(
                            body.isAlive() && !body.isDeadOrDying(),
                            "Contact hostile killed the companion: health="
                                + body.getHealth()
                                + ", survival=" + runtime.survival().state()
                    );
                    if (zombie != null
                            && zombie.getHealth() < zombie.getMaxHealth()) {
                        evidence.getAndUpdate(bits -> bits | 1);
                    }
                    if (body.getMainHandItem().is(Items.DIAMOND_SWORD)
                            && body.getMainHandItem().getDamageValue() > 0) {
                        evidence.getAndUpdate(bits -> bits | 2);
                    }
                    if (body.position().distanceTo(bodyStart.get()) >= 0.10D) {
                        evidence.getAndUpdate(bits -> bits | 4);
                    }
                    runtime.behaviorArbiter().latest()
                            .filter(resolution -> resolution.claimedBy(
                                    dev.mcai.companion.control.BehaviorArbiter
                                            .Lane.EMERGENCY_SURVIVAL
                            ))
                            .ifPresent(ignored ->
                                    evidence.getAndUpdate(bits -> bits | 8)
                            );
                    if ((evidence.get() & 15) == 15
                            && now - phaseStarted.get() >= 60L) {
                        helper.succeed();
                        return;
                    }
                    helper.assertTrue(
                            now - phaseStarted.get() <= 900L,
                            "Contact hostile produced no complete response: "
                                + "evidence=" + evidence.get()
                                + ", health=" + body.getHealth()
                                + ", zombieHealth="
                                + (zombie == null ? "null" : zombie.getHealth())
                                + ", position=" + body.position()
                                + ", survival=" + runtime.survival().state()
                                + ", frame=" + runtime.coreFrames().current()
                    );
                }
        );
    }

    /**
     * Physical regression for a neutral mob that has already chosen the
     * companion as its target.  Iron golems are intentionally not marked
     * hostile by the fair sampler, so the only legal automatic retaliation
     * trigger is a real vanilla damage/contact cue.  The body must use its
     * owned sword through the ordinary attack actuator and must not remain a
     * stationary shield spectator.
     */
    @GameTest(
        name = "real_emergency_iron_golem_duel",
        environment = "exclusive_real_emergency_iron_golem_duel",
        structure = FOCUSED_TEST_STRUCTURE,
        maxTicks = EMERGENCY_GOLEM_TEST_MAX_TICKS,
        skyAccess = true,
        padding = 8
    )
    public static void realEmergencyIronGolemDuel(
            final GameTestHelper helper
    ) {
        final var server = helper.getLevel().getServer();
        final AtomicInteger phase = new AtomicInteger();
        final AtomicLong phaseStarted = new AtomicLong(helper.getTick());
        final AtomicReference<Mob> target = new AtomicReference<>();
        final AtomicReference<Vec3> bodyStart = new AtomicReference<>();
        final AtomicReference<Float> targetInitialHealth =
                new AtomicReference<>();
        final AtomicInteger evidence = new AtomicInteger();
        final AtomicBoolean targetActivated = new AtomicBoolean();

        helper.addCleanup(ignored -> {
            final Mob current = target.get();
            if (current != null && !current.isRemoved()) {
                current.discard();
            }
            CompanionRuntime.active()
                    .filter(runtime -> runtime.server() == server)
                    .ifPresent(runtime -> {
                        runtime.survival().reset();
                        runtime.coreActions().quiesceNow();
                        runtime.skillSupervisor().abandonForSessionEnd();
                    });
            if (AiPlayerManager.status(server).state()
                    != SessionState.ABSENT) {
                AiPlayerManager.requestRemove(server);
            }
        });

        final var spawn = GameTestCompanionSpawn.request(
                helper,
                FOCUSED_TEST_ORIGIN
        );
        helper.assertTrue(
                spawn.accepted(),
                "Iron-golem duel body spawn was rejected: " + spawn.code()
        );
        scheduleEveryTick(
                helper,
                EMERGENCY_GOLEM_TEST_MAX_TICKS,
                () -> {
                    final long now = helper.getTick();
                    if (phase.get() == 0) {
                        final var status = AiPlayerManager.status(server);
                        helper.assertTrue(
                                status.state() != SessionState.FAILED,
                                "Iron-golem duel body failed: " + status
                        );
                        if (status.state() != SessionState.ACTIVE
                                || !status.online()) {
                            helper.assertTrue(
                                    now - phaseStarted.get()
                                        <= FOCUSED_BODY_START_TIMEOUT_TICKS,
                                    "Iron-golem duel body timed out"
                            );
                            return;
                        }
                        final var runtime = CompanionRuntime.active()
                                .filter(candidate ->
                                        candidate.server() == server)
                                .orElseThrow(() -> helper.assertionException(
                                        "Iron-golem duel runtime unavailable"
                                ));
                        runtime.survival().reset();
                        runtime.coreActions().quiesceNow();
                        runtime.skillSupervisor().abandonForSessionEnd();
                        runtime.brain().close();
                        final var goal = runtime.goals().setGoal(
                                "Survive the iron golem duel",
                                GoalSource.RECOVERY
                        );
                        helper.assertTrue(
                                goal.accepted(),
                                "Iron-golem duel goal was rejected: "
                                    + goal.code()
                        );

                        final var level = helper.getLevel();
                        final BlockPos center = helper.absolutePos(
                                FOCUSED_TEST_ORIGIN
                        );
                        for (int x = -8; x <= 8; x++) {
                            for (int z = -8; z <= 8; z++) {
                                level.setBlockAndUpdate(
                                        center.offset(x, -1, z),
                                        Blocks.SMOOTH_STONE.defaultBlockState()
                                );
                                for (int y = 0; y <= 5; y++) {
                                    level.setBlockAndUpdate(
                                            center.offset(x, y, z),
                                            Blocks.AIR.defaultBlockState()
                                    );
                                }
                            }
                        }
                        final var body = AiPlayerManager
                                .onlinePlayer(server)
                                .orElseThrow();
                        body.teleportTo(
                                center.getX() + 0.5D,
                                center.getY(),
                                center.getZ() + 0.5D
                        );
                        body.setGameMode(GameType.SURVIVAL);
                        body.getInventory().clearContent();
                        body.getInventory().setItem(
                                0,
                                new ItemStack(Items.DIAMOND_SWORD)
                        );
                        body.getInventory().setItem(
                                8,
                                new ItemStack(Items.COOKED_BEEF, 8)
                        );
                        body.getInventory().setSelectedSlot(0);
                        body.setItemSlot(
                                EquipmentSlot.OFFHAND,
                                new ItemStack(Items.SHIELD)
                        );
                        body.setItemSlot(
                                EquipmentSlot.HEAD,
                                new ItemStack(Items.DIAMOND_HELMET)
                        );
                        body.setItemSlot(
                                EquipmentSlot.CHEST,
                                new ItemStack(Items.DIAMOND_CHESTPLATE)
                        );
                        body.setItemSlot(
                                EquipmentSlot.LEGS,
                                new ItemStack(Items.DIAMOND_LEGGINGS)
                        );
                        body.setItemSlot(
                                EquipmentSlot.FEET,
                                new ItemStack(Items.DIAMOND_BOOTS)
                        );
                        body.setHealth(body.getMaxHealth());
                        body.getFoodData().setFoodLevel(20);
                        body.getFoodData().setSaturation(5.0F);
                        body.setDeltaMovement(Vec3.ZERO);
                        body.fallDistance = 0.0F;
                        body.inventoryMenu.broadcastChanges();

                        final Mob golem = EntityTypes.IRON_GOLEM.create(
                                level,
                                EntitySpawnReason.COMMAND
                        );
                        helper.assertTrue(
                                golem != null,
                                "Could not create physical iron golem"
                        );
                        golem.setPos(
                                center.getX() + 2.5D,
                                center.getY(),
                                center.getZ() + 0.5D
                        );
                        golem.setPersistenceRequired();
                        // Match the live-model fixture lifecycle: the mob is
                        // staged without AI, then receives an ordinary
                        // vanilla target on the following server tick.
                        golem.setNoAi(true);
                        helper.assertTrue(
                                level.addFreshEntity(golem),
                                "Could not add physical iron golem"
                        );
                        golem.setTarget(null);
                        target.set(golem);
                        targetInitialHealth.set(golem.getHealth());
                        body.lookAt(
                                EntityAnchorArgument.Anchor.EYES,
                                golem.position().add(0.0D, 1.0D, 0.0D)
                        );
                        body.setYHeadRot(body.getYRot());
                        bodyStart.set(body.position());
                        phase.set(1);
                        phaseStarted.set(now);
                        return;
                    }

                    final var runtime = CompanionRuntime.active()
                            .orElseThrow();
                    final var body = AiPlayerManager
                            .onlinePlayer(server)
                            .orElseThrow();
                    final Mob golem = target.get();
                    if (!targetActivated.get()) {
                        golem.setNoAi(false);
                        golem.setTarget(body);
                        golem.setLastHurtByMob(body);
                        golem.setAggressive(true);
                        if (golem instanceof net.minecraft.world.entity.NeutralMob neutral) {
                            neutral.setPersistentAngerTarget(
                                    net.minecraft.world.entity.EntityReference.of(body)
                            );
                            neutral.startPersistentAngerTimer();
                        }
                        targetActivated.set(true);
                    }
                    helper.assertTrue(
                            body.isAlive() && !body.isDeadOrDying(),
                            "Iron-golem duel killed the companion: health="
                                + body.getHealth()
                                + ", survival=" + runtime.survival().state()
                    );
                    if (golem != null
                            && targetInitialHealth.get() != null
                            && golem.getHealth()
                                < targetInitialHealth.get() - 0.1F) {
                        evidence.getAndUpdate(bits -> bits | 1);
                    }
                    if (body.getMainHandItem().is(Items.DIAMOND_SWORD)
                            && body.getMainHandItem().getDamageValue() > 0) {
                        evidence.getAndUpdate(bits -> bits | 2);
                    }
                    if (body.position().distanceTo(bodyStart.get()) >= 0.25D) {
                        evidence.getAndUpdate(bits -> bits | 4);
                    }
                    if (runtime.survival().state()
                            == EmergencySurvivalController.State.COUNTERATTACKING
                            || runtime.survival().state()
                                == EmergencySurvivalController.State.GUARDING) {
                        evidence.getAndUpdate(bits -> bits | 8);
                    }
                    /*
                     * This is a pressure/behavior gate, not a synthetic
                     * instant-kill assertion. A real duel has already been
                     * demonstrated once the golem lost meaningful health,
                     * the sword took vanilla durability, the body moved and
                     * the emergency arbiter owned the encounter. Requiring
                     * the full 100-health golem to die here would make the
                     * gate depend on the golem's pathing after a legitimate
                     * separation, rather than catching the reported
                     * stand-still failure.
                     */
                    if (golem != null
                            && targetInitialHealth.get() != null
                            && targetInitialHealth.get()
                                - golem.getHealth() >= 5.0F
                            && (evidence.get() & 15) == 15
                            && now - phaseStarted.get() >= 120L) {
                        helper.succeed();
                        return;
                    }
                    if (golem == null || golem.isRemoved() || !golem.isAlive()) {
                        helper.assertTrue(
                                (evidence.get() & 15) == 15,
                                "Iron-golem duel ended without real combat "
                                    + "evidence=" + evidence.get()
                                    + ", bodyHealth=" + body.getHealth()
                                    + ", survival="
                                    + runtime.survival().state()
                        );
                        helper.succeed();
                        return;
                    }
                    helper.assertTrue(
                            now - phaseStarted.get()
                                <= EMERGENCY_GOLEM_TEST_MAX_TICKS - 120L,
                            "Iron-golem duel timed out: targetHealth="
                                + golem.getHealth()
                                + ", bodyHealth=" + body.getHealth()
                                + ", evidence=" + evidence.get()
                                + ", survival=" + runtime.survival().state()
                    );
                }
        );
    }

    /**
     * Bounded horde-pressure gate requested for combat review: ten zombies
     * and ten skeletons are allowed to use their ordinary AI and attacks
     * against one survival body.  The assertion is intentionally about the
     * reported failure mode (standing still and never counterattacking), not
     * an artificial claim that one unenchanted player must clear twenty mobs
     * in every world.
     */
    @GameTest(
        name = "real_emergency_zombie_skeleton_horde",
        environment = "exclusive_real_emergency_zombie_skeleton_horde",
        structure = FOCUSED_TEST_STRUCTURE,
        maxTicks = EMERGENCY_HORDE_TEST_MAX_TICKS,
        skyAccess = true,
        padding = 8
    )
    public static void realEmergencyZombieSkeletonHorde(
            final GameTestHelper helper
    ) {
        final var server = helper.getLevel().getServer();
        final AtomicInteger phase = new AtomicInteger();
        final AtomicLong phaseStarted = new AtomicLong(helper.getTick());
        final AtomicReference<List<Mob>> targets = new AtomicReference<>(
                List.of()
        );
        final AtomicReference<Vec3> bodyStart = new AtomicReference<>();

        helper.addCleanup(ignored -> {
            targets.get().forEach(target -> {
                if (target != null && !target.isRemoved()) {
                    target.discard();
                }
            });
            CompanionRuntime.active()
                    .filter(runtime -> runtime.server() == server)
                    .ifPresent(runtime -> {
                        runtime.survival().reset();
                        runtime.coreActions().quiesceNow();
                        runtime.skillSupervisor().abandonForSessionEnd();
                    });
            if (AiPlayerManager.status(server).state()
                    != SessionState.ABSENT) {
                AiPlayerManager.requestRemove(server);
            }
        });

        final var spawn = GameTestCompanionSpawn.request(
                helper,
                FOCUSED_TEST_ORIGIN
        );
        helper.assertTrue(
                spawn.accepted(),
                "Horde-defense body spawn was rejected: " + spawn.code()
        );
        scheduleEveryTick(
                helper,
                EMERGENCY_HORDE_TEST_MAX_TICKS,
                () -> {
                    final long now = helper.getTick();
                    if (phase.get() == 0) {
                        final var status = AiPlayerManager.status(server);
                        helper.assertTrue(
                                status.state() != SessionState.FAILED,
                                "Horde-defense body failed: " + status
                        );
                        if (status.state() != SessionState.ACTIVE
                                || !status.online()) {
                            helper.assertTrue(
                                    now - phaseStarted.get()
                                        <= FOCUSED_BODY_START_TIMEOUT_TICKS,
                                    "Horde-defense body timed out"
                            );
                            return;
                        }
                        final var runtime = CompanionRuntime.active()
                                .filter(candidate ->
                                        candidate.server() == server)
                                .orElseThrow(() -> helper.assertionException(
                                        "Horde-defense runtime unavailable"
                                ));
                        runtime.survival().reset();
                        runtime.coreActions().quiesceNow();
                        runtime.skillSupervisor().abandonForSessionEnd();
                        runtime.brain().close();
                        final var goal = runtime.goals().setGoal(
                                "Survive ten zombies and ten skeletons",
                                GoalSource.RECOVERY
                        );
                        helper.assertTrue(
                                goal.accepted(),
                                "Horde-defense goal was rejected: "
                                    + goal.code()
                        );

                        final var level = helper.getLevel();
                        final BlockPos center = helper.absolutePos(
                                FOCUSED_TEST_ORIGIN
                        );
                        for (int x = -10; x <= 10; x++) {
                            for (int z = -10; z <= 10; z++) {
                                level.setBlockAndUpdate(
                                        center.offset(x, -1, z),
                                        Blocks.SMOOTH_STONE.defaultBlockState()
                                );
                                for (int y = 0; y <= 5; y++) {
                                    level.setBlockAndUpdate(
                                            center.offset(x, y, z),
                                            Blocks.AIR.defaultBlockState()
                                    );
                                }
                            }
                        }
                        final var body = AiPlayerManager
                                .onlinePlayer(server)
                                .orElseThrow();
                        body.teleportTo(
                                center.getX() + 0.5D,
                                center.getY(),
                                center.getZ() + 0.5D
                        );
                        body.setGameMode(GameType.SURVIVAL);
                        body.getInventory().clearContent();
                        body.getInventory().setItem(
                                0,
                                new ItemStack(Items.DIAMOND_SWORD)
                        );
                        body.getInventory().setItem(
                                8,
                                new ItemStack(Items.COOKED_BEEF, 16)
                        );
                        body.getInventory().setItem(
                                7,
                                new ItemStack(Items.GOLDEN_APPLE, 2)
                        );
                        body.getInventory().setSelectedSlot(0);
                        body.setItemSlot(
                                EquipmentSlot.OFFHAND,
                                new ItemStack(Items.SHIELD)
                        );
                        body.setItemSlot(
                                EquipmentSlot.HEAD,
                                new ItemStack(Items.DIAMOND_HELMET)
                        );
                        body.setItemSlot(
                                EquipmentSlot.CHEST,
                                new ItemStack(Items.DIAMOND_CHESTPLATE)
                        );
                        body.setItemSlot(
                                EquipmentSlot.LEGS,
                                new ItemStack(Items.DIAMOND_LEGGINGS)
                        );
                        body.setItemSlot(
                                EquipmentSlot.FEET,
                                new ItemStack(Items.DIAMOND_BOOTS)
                        );
                        body.setHealth(body.getMaxHealth());
                        body.getFoodData().setFoodLevel(20);
                        body.getFoodData().setSaturation(5.0F);
                        body.setDeltaMovement(Vec3.ZERO);
                        body.fallDistance = 0.0F;
                        body.inventoryMenu.broadcastChanges();

                        final List<Mob> spawned =
                                new java.util.ArrayList<>();
                        for (int index = 0; index < 20; index++) {
                            final boolean skeleton = index >= 10;
                            final Mob hostile = (skeleton
                                    ? EntityTypes.SKELETON
                                    : EntityTypes.ZOMBIE).create(
                                        level,
                                        EntitySpawnReason.COMMAND
                                    );
                            helper.assertTrue(
                                    hostile != null,
                                    "Could not create horde member " + index
                            );
                            final double angle =
                                    (Math.PI * 2.0 * index) / 20.0;
                            hostile.setPos(
                                    center.getX() + 5.5D
                                        * Math.cos(angle),
                                    center.getY(),
                                    center.getZ() + 5.5D
                                        * Math.sin(angle)
                            );
                            hostile.setPersistenceRequired();
                            helper.assertTrue(
                                    level.addFreshEntity(hostile),
                                    "Could not add horde member " + index
                            );
                            hostile.setTarget(body);
                            spawned.add(hostile);
                        }
                        helper.assertTrue(
                                spawned.size() == 20,
                                "Horde fixture did not create 20 mobs"
                        );
                        targets.set(List.copyOf(spawned));
                        body.lookAt(
                                EntityAnchorArgument.Anchor.EYES,
                                spawned.get(0).position()
                                    .add(0.0D, 1.0D, 0.0D)
                        );
                        body.setYHeadRot(body.getYRot());
                        bodyStart.set(body.position());
                        phase.set(1);
                        phaseStarted.set(now);
                        return;
                    }

                    final var runtime = CompanionRuntime.active()
                            .orElseThrow();
                    final var body = AiPlayerManager
                            .onlinePlayer(server)
                            .orElseThrow();
                    helper.assertTrue(
                            body.isAlive() && !body.isDeadOrDying(),
                            "Twenty-mob pressure killed the companion: "
                                + "health=" + body.getHealth()
                                + ", survival=" + runtime.survival().state()
                    );
                    final long damagedTargets = targets.get().stream()
                            .filter(target -> target.isRemoved()
                                    || !target.isAlive()
                                    || target.getHealth()
                                        < target.getMaxHealth() - 0.1F)
                            .count();
                    final boolean moved = body.position().distanceTo(
                            bodyStart.get()
                    ) >= 0.25D;
                    final boolean swordUsed = body.getMainHandItem()
                            .is(Items.DIAMOND_SWORD)
                            && body.getMainHandItem().getDamageValue() > 0;
                    if (damagedTargets >= 3
                            && moved
                            && swordUsed
                            && now - phaseStarted.get() >= 120L) {
                        helper.succeed();
                        return;
                    }
                    helper.assertTrue(
                            now - phaseStarted.get()
                                <= EMERGENCY_HORDE_TEST_MAX_TICKS - 120L,
                            "Twenty-mob pressure produced insufficient combat "
                                + "evidence: damagedTargets=" + damagedTargets
                                + ", moved=" + moved
                                + ", swordUsed=" + swordUsed
                                + ", bodyHealth=" + body.getHealth()
                                + ", survival=" + runtime.survival().state()
                    );
                }
        );
    }

    /**
     * Offline-provider gate for the smallest useful "someone handed me
     * equipment" interaction.  The fixture deliberately does not configure
     * a model gateway.  The body must still use the normal inventory-menu
     * transaction to put an owned helmet on its head and a shield in its
     * off-hand; this is local upkeep, not a fabricated item/world write.
     */
    @GameTest(
        name = "offline_idle_equipment",
        environment = "exclusive_offline_idle_equipment",
        structure = FOCUSED_TEST_STRUCTURE,
        maxTicks = 1_000,
        skyAccess = true,
        padding = 8
    )
    public static void offlineIdleEquipment(
            final GameTestHelper helper
    ) {
        final var server = helper.getLevel().getServer();
        final AtomicInteger phase = new AtomicInteger();
        final AtomicLong phaseStarted = new AtomicLong(helper.getTick());

        helper.addCleanup(ignored -> {
            CompanionRuntime.active()
                    .filter(runtime -> runtime.server() == server)
                    .ifPresent(runtime -> {
                        runtime.survival().reset();
                        runtime.coreActions().quiesceNow();
                        runtime.skillSupervisor().abandonForSessionEnd();
                    });
            if (AiPlayerManager.status(server).state()
                    != SessionState.ABSENT) {
                AiPlayerManager.requestRemove(server);
            }
        });

        final var spawn = GameTestCompanionSpawn.request(
                helper,
                FOCUSED_TEST_ORIGIN
        );
        helper.assertTrue(
                spawn.accepted(),
                "Offline equipment body spawn was rejected: " + spawn.code()
        );
        scheduleEveryTick(
                helper,
                1_000,
                () -> {
                    final long now = helper.getTick();
                    if (phase.get() == 0) {
                        final var status = AiPlayerManager.status(server);
                        helper.assertTrue(
                                status.state() != SessionState.FAILED,
                                "Offline equipment body failed: " + status
                        );
                        if (status.state() != SessionState.ACTIVE
                                || !status.online()) {
                            helper.assertTrue(
                                    now - phaseStarted.get()
                                        <= FOCUSED_BODY_START_TIMEOUT_TICKS,
                                    "Offline equipment body timed out"
                            );
                            return;
                        }
                        final var runtime = CompanionRuntime.active()
                                .filter(candidate ->
                                        candidate.server() == server)
                                .orElseThrow(() -> helper.assertionException(
                                        "Offline equipment runtime unavailable"
                                ));
                        /*
                         * GameTestServer may reuse the production runtime
                         * between exclusive fixtures. A previous focused
                         * movement fixture can have installed its inert
                         * HoldingGameTestGateway; remove only that verified
                         * delegate before asserting this test's deliberate
                         * offline boundary. This is fixture isolation, not a
                         * production fallback or a model result.
                         */
                        runtime.model().gateway().clearVerifiedDelegate();
                        helper.assertTrue(
                                !runtime.model().snapshot().gatewayReady(),
                                "Offline equipment gate unexpectedly has a "
                                    + "live model gateway"
                        );
                        runtime.survival().reset();
                        runtime.coreActions().quiesceNow();
                        runtime.skillSupervisor().abandonForSessionEnd();
                        runtime.brain().close();

                        final var body = AiPlayerManager
                                .onlinePlayer(server)
                                .orElseThrow();
                        body.setGameMode(GameType.SURVIVAL);
                        body.getInventory().clearContent();
                        body.setItemSlot(
                                EquipmentSlot.HEAD,
                                ItemStack.EMPTY
                        );
                        body.setItemSlot(
                                EquipmentSlot.OFFHAND,
                                ItemStack.EMPTY
                        );
                        body.getInventory().setItem(
                                0,
                                new ItemStack(Items.IRON_HELMET)
                        );
                        body.getInventory().setItem(
                                1,
                                new ItemStack(Items.SHIELD)
                        );
                        body.getInventory().setSelectedSlot(0);
                        body.inventoryMenu.broadcastChanges();
                        phase.set(1);
                        phaseStarted.set(now);
                        return;
                    }

                    final var body = AiPlayerManager
                            .onlinePlayer(server)
                            .orElseThrow();
                    helper.assertTrue(
                            body.isAlive() && !body.isDeadOrDying(),
                            "Offline equipment body died while idle"
                    );
                    final boolean helmet = body.getItemBySlot(
                            EquipmentSlot.HEAD
                    ).is(Items.IRON_HELMET);
                    final boolean shield = body.getItemBySlot(
                            EquipmentSlot.OFFHAND
                    ).is(Items.SHIELD);
                    if (helmet && shield) {
                        helper.succeed();
                        return;
                    }
                    helper.assertTrue(
                            now - phaseStarted.get() < 800L,
                            "Offline idle equipment did not commit through "
                                + "vanilla menus: helmet=" + helmet
                                + ", shield=" + shield
                                + ", inventoryHelmet="
                                + body.getInventory().countItem(
                                    Items.IRON_HELMET
                                )
                                + ", inventoryShield="
                                + body.getInventory().countItem(
                                    Items.SHIELD
                                )
                );
            }
        );
    }

    /**
     * Release-excluded, no-provider safety gate for the failure mode where a
     * companion is handed an always-edible golden apple at critical health.
     * The fixture gives the body the item through setup only; production code
     * must move it through the ordinary inventory transaction and then issue
     * a normal held-item use.  No health, inventory or effect is written by
     * the assertion itself after setup, and no model decision is installed.
     */
    @GameTest(
        name = "real_offline_critical_golden_apple",
        environment = "exclusive_real_offline_critical_golden_apple",
        structure = FOCUSED_TEST_STRUCTURE,
        maxTicks = 2_000,
        skyAccess = true,
        padding = 8
    )
    public static void realOfflineCriticalGoldenApple(
            final GameTestHelper helper
    ) {
        final var server = helper.getLevel().getServer();
        final AtomicInteger phase = new AtomicInteger();
        final AtomicLong phaseStarted = new AtomicLong(helper.getTick());
        final AtomicReference<Integer> initialCount =
                new AtomicReference<>();

        helper.addCleanup(ignored -> {
            CompanionRuntime.active()
                    .filter(runtime -> runtime.server() == server)
                    .ifPresent(runtime -> {
                        runtime.survival().reset();
                        runtime.coreActions().quiesceNow();
                        runtime.skillSupervisor().abandonForSessionEnd();
                    });
            if (AiPlayerManager.status(server).state()
                    != SessionState.ABSENT) {
                AiPlayerManager.requestRemove(server);
            }
        });

        final var spawn = GameTestCompanionSpawn.request(
                helper,
                FOCUSED_TEST_ORIGIN
        );
        helper.assertTrue(
                spawn.accepted(),
                "Critical-food body spawn was rejected: " + spawn.code()
        );
        scheduleEveryTick(
                helper,
                2_000,
                () -> {
                    final long now = helper.getTick();
                    final var status = AiPlayerManager.status(server);
                    helper.assertTrue(
                            status.state() != SessionState.FAILED,
                            "Critical-food body failed: " + status
                    );
                    if (phase.get() == 0) {
                        if (status.state() != SessionState.ACTIVE
                                || !status.online()) {
                            helper.assertTrue(
                                    now <= FOCUSED_BODY_START_TIMEOUT_TICKS,
                                    "Critical-food body did not become active"
                            );
                            return;
                        }
                        final var runtime = CompanionRuntime.active()
                                .filter(candidate ->
                                        candidate.server() == server)
                                .orElseThrow(() -> helper.assertionException(
                                        "Critical-food runtime unavailable"
                                ));
                        /* This gate is intentionally offline/no-model. */
                        runtime.model().gateway().clearVerifiedDelegate();
                        runtime.brain().close();
                        runtime.survival().reset();
                        runtime.coreActions().quiesceNow();
                        runtime.skillSupervisor().abandonForSessionEnd();

                        final var body = AiPlayerManager
                                .onlinePlayer(server)
                                .orElseThrow();
                        body.setGameMode(GameType.SURVIVAL);
                        body.getInventory().clearContent();
                        body.setItemSlot(
                                EquipmentSlot.MAINHAND,
                                ItemStack.EMPTY
                        );
                        body.setItemSlot(
                                EquipmentSlot.OFFHAND,
                                ItemStack.EMPTY
                        );
                        body.removeAllEffects();
                        body.clearFire();
                        body.setHealth(4.0F);
                        body.getFoodData().setFoodLevel(20);
                        body.getFoodData().setSaturation(5.0F);
                        body.getInventory().setItem(
                                8,
                                new ItemStack(Items.GOLDEN_APPLE)
                        );
                        body.getInventory().setSelectedSlot(0);
                        body.inventoryMenu.broadcastChanges();
                        initialCount.set(
                                body.getInventory().countItem(
                                        Items.GOLDEN_APPLE
                                )
                        );
                        helper.assertTrue(
                                initialCount.get() == 1,
                                "Critical-food fixture did not install one "
                                    + "owned golden apple"
                        );
                        phase.set(1);
                        phaseStarted.set(now);
                        return;
                    }

                    final var body = AiPlayerManager.onlinePlayer(server)
                            .orElseThrow(() -> helper.assertionException(
                                    "Critical-food body disappeared"
                            ));
                    helper.assertTrue(
                            body.isAlive() && !body.isDeadOrDying(),
                            "Critical-food body died before eating: health="
                                + body.getHealth()
                    );
                    final int remaining = body.getInventory().countItem(
                            Items.GOLDEN_APPLE
                    );
                    final boolean held = body.getMainHandItem().is(
                            Items.GOLDEN_APPLE
                    );
                    final boolean consumed = remaining
                            < initialCount.get();
                    final boolean protectedByAbsorption =
                            body.getAbsorptionAmount() > 0.0F;
                    if (consumed && protectedByAbsorption) {
                        helper.succeed();
                        return;
                    }
                    helper.assertTrue(
                            now - phaseStarted.get() < 1_700L,
                            "Critical golden apple was not consumed through "
                                + "vanilla use: held=" + held
                                + ", remaining=" + remaining
                                + ", absorption="
                                + body.getAbsorptionAmount()
                                + ", health=" + body.getHealth()
                                + ", survival="
                                + CompanionRuntime.active()
                                    .map(runtime -> runtime.survival().state())
                                    .orElse(null)
                    );
                }
        );
    }

    /**
     * Dedicated-server gate for the case the production companion actually
     * needs most: every human is offline, yet the headless AI remains the only
     * vanilla player ticket and its remote work area keeps simulating.
     *
     * <p>The probe is deliberately 640 blocks from the GameTest structure.
     * Both a non-player entity and a scheduled water update must advance
     * there. The test also rejects force-loaded chunks, then kills the body
     * and verifies that ordinary respawn changes the body generation without
     * producing a runtime tick failure.</p>
     */
    @GameTest(
        name = "zero_human_dedicated_server_chunk_and_respawn",
        environment = "exclusive_zero_human_dedicated_server",
        structure = FOCUSED_TEST_STRUCTURE,
        maxTicks = 4_000,
        skyAccess = true,
        padding = 8
    )
    public static void zeroHumanDedicatedServerChunkAndRespawn(
            final GameTestHelper helper
    ) {
        final var level = helper.getLevel();
        final var server = level.getServer();
        final BlockPos remoteAnchor = helper.absolutePos(
                FOCUSED_TEST_ORIGIN
        ).offset(640, 0, 0);
        final ChunkPos remoteChunk = new ChunkPos(
                SectionPos.blockToSectionCoord(remoteAnchor.getX()),
                SectionPos.blockToSectionCoord(remoteAnchor.getZ())
        );
        final BlockPos remoteStand = new BlockPos(
                SectionPos.sectionToBlockCoord(remoteChunk.x(), 8),
                remoteAnchor.getY(),
                SectionPos.sectionToBlockCoord(remoteChunk.z(), 8)
        );
        /*
         * Keep the simulation probes outside the body's current chunk. A
         * same-chunk probe cannot distinguish a real player simulation
         * window from GameTestServer's otherwise-zero simulation radius.
         */
        final ChunkPos simulationProbeChunk = new ChunkPos(
                remoteChunk.x() + 2,
                remoteChunk.z() + 1
        );
        final BlockPos simulationProbeStand = new BlockPos(
                SectionPos.sectionToBlockCoord(
                        simulationProbeChunk.x(),
                        8
                ),
                remoteStand.getY(),
                SectionPos.sectionToBlockCoord(
                        simulationProbeChunk.z(),
                        8
                )
        );
        final BlockPos blockTickProbe =
                simulationProbeStand.offset(3, 0, 0);
        final AtomicInteger phase = new AtomicInteger();
        final AtomicLong phaseStartedAt = new AtomicLong(helper.getTick());
        final AtomicLong probeStartAge = new AtomicLong(-1L);
        final AtomicLong bodyGenerationBeforeDeath =
                new AtomicLong(-1L);
        final AtomicLong failureCountBeforeDeath =
                new AtomicLong(-1L);
        final AtomicReference<ItemEntity> probe = new AtomicReference<>();
        final AtomicReference<Mob> damageAttacker =
                new AtomicReference<>();
        final AtomicReference<
                net.minecraft.server.level.ServerPlayer
        > killedBody = new AtomicReference<>();

        helper.addCleanup(ignored -> {
            final ItemEntity currentProbe = probe.get();
            if (currentProbe != null && !currentProbe.isRemoved()) {
                currentProbe.discard();
            }
            final Mob currentAttacker = damageAttacker.get();
            if (currentAttacker != null
                    && !currentAttacker.isRemoved()) {
                currentAttacker.discard();
            }
            if (AiPlayerManager.status(server).state()
                    != SessionState.ABSENT) {
                AiPlayerManager.requestRemove(server);
            }
        });

        /*
         * The ordinary selector runs beside other GameTests on one server.
         * Clear a previous fixture's runtime before requesting this test's
         * body, while preserving the dedicated zero-human startup assertion
         * (that path must observe the production-created body untouched).
         */
        if (!Boolean.getBoolean("mcai.zeroHumanAutoSpawnTest")) {
            GameTestCompanionSpawn.resetForIsolatedFixture(server);
        }
        assertNoHumanPlayers(helper);
        if (Boolean.getBoolean("mcai.zeroHumanAutoSpawnTest")) {
            final var startupStatus = AiPlayerManager.status(server);
            helper.assertTrue(
                    startupStatus.state() != SessionState.ABSENT,
                    "Production ServerStartedEvent did not start the "
                        + "companion with zero humans online"
            );
            helper.assertTrue(
                    startupStatus.state() != SessionState.FAILED,
                    "Production zero-human startup failed: "
                        + startupStatus
            );
        } else {
            final var spawn = AiPlayerManager.requestSpawn(server);
            helper.assertTrue(
                    spawn.accepted(),
                    "Zero-human companion spawn was rejected: "
                        + spawn.code()
            );
        }

        scheduleEveryTick(helper, 4_000L, () -> {
            assertNoHumanPlayers(helper);
            final int currentPhase = phase.get();
            final long now = helper.getTick();

            if (currentPhase == 0) {
                final var status = AiPlayerManager.status(server);
                helper.assertTrue(
                        status.state() != SessionState.FAILED,
                        "Zero-human body failed to become active: "
                            + status
                );
                if (status.state() != SessionState.ACTIVE
                        || !status.online()) {
                    helper.assertTrue(
                            now - phaseStartedAt.get()
                                <= FOCUSED_BODY_START_TIMEOUT_TICKS,
                            "Zero-human body did not become active"
                    );
                    return;
                }

                final var player = AiPlayerManager.onlinePlayer(server)
                        .orElseThrow(() -> helper.assertionException(
                                "Active zero-human body was missing"
                        ));
                helper.assertTrue(
                        server.getPlayerList().getPlayers().size() == 1,
                        "Expected exactly one AI and zero humans, found "
                            + server.getPlayerList()
                                .getPlayers().size()
                            + " player-list entries"
                );
                player.setInvulnerable(true);
                player.setGameMode(GameType.SURVIVAL);
                player.setDeltaMovement(Vec3.ZERO);

                for (int x = -5; x <= 5; x++) {
                    for (int z = -5; z <= 5; z++) {
                        level.setBlockAndUpdate(
                                remoteStand.offset(x, -1, z),
                                Blocks.SMOOTH_STONE
                                        .defaultBlockState()
                        );
                        level.setBlockAndUpdate(
                                remoteStand.offset(x, 0, z),
                                Blocks.AIR.defaultBlockState()
                        );
                        level.setBlockAndUpdate(
                                remoteStand.offset(x, 1, z),
                                Blocks.AIR.defaultBlockState()
                        );
                    }
                }
                player.teleportTo(
                        remoteStand.getX() + 0.5,
                        remoteStand.getY(),
                        remoteStand.getZ() + 0.5
                );

                phase.set(1);
                phaseStartedAt.set(now);
                return;
            }

            if (currentPhase == 1) {
                final var player = AiPlayerManager.onlinePlayer(server)
                        .orElseThrow(() -> helper.assertionException(
                                "Zero-human body disconnected at its "
                                    + "remote work area"
                        ));
                helper.assertTrue(
                        player.chunkPosition().equals(remoteChunk),
                        "AI did not remain in the remote work chunk"
                );
                if (!level.shouldTickBlocksAt(remoteChunk.pack())
                        || !level.areEntitiesActuallyLoadedAndTicking(
                            remoteChunk
                        )
                        || !level.shouldTickBlocksAt(
                            simulationProbeChunk.pack()
                        )
                        || !level.areEntitiesActuallyLoadedAndTicking(
                            simulationProbeChunk
                        )) {
                    helper.assertTrue(
                            now - phaseStartedAt.get() <= 600L,
                            "The only AI player never established a "
                                + "multi-chunk vanilla entity/block "
                                + "simulation window"
                    );
                    return;
                }

                /*
                 * Do not insert either probe until vanilla has processed the
                 * teleported ServerPlayer and established its own ticket.
                 * Adding an entity to an unloaded far chunk would measure a
                 * fixture race, not player-ticket behavior.
                 */
                level.setBlockAndUpdate(
                        blockTickProbe,
                        Blocks.REDSTONE_LAMP
                                .defaultBlockState()
                                .setValue(
                                    RedstoneLampBlock.LIT,
                                    true
                                )
                );
                level.scheduleTick(
                        blockTickProbe,
                        Blocks.REDSTONE_LAMP,
                        4
                );
                final ItemEntity tickingProbe = new ItemEntity(
                        level,
                        simulationProbeStand.getX() + 0.5,
                        simulationProbeStand.getY() + 0.25,
                        simulationProbeStand.getZ() + 0.5,
                        new ItemStack(Items.COBBLESTONE)
                );
                tickingProbe.setNoGravity(true);
                tickingProbe.setPickUpDelay(32_767);
                helper.assertTrue(
                        level.addFreshEntity(tickingProbe),
                        "Could not add the remote ticking probe"
                );
                probe.set(tickingProbe);
                phase.set(2);
                phaseStartedAt.set(now);
                return;
            }

            if (currentPhase == 2) {
                final ItemEntity currentProbe = probe.get();
                helper.assertTrue(
                        currentProbe != null
                            && !currentProbe.isRemoved(),
                        "Remote entity probe vanished after the AI ticket "
                            + "was established"
                );
                if (!level.isPositionEntityTicking(
                            currentProbe.blockPosition()
                        )) {
                    helper.assertTrue(
                            now - phaseStartedAt.get() <= 100L,
                            "Remote probe did not enter entity-ticking "
                                + "state under the AI player ticket"
                    );
                    return;
                }
                probeStartAge.set(currentProbe.tickCount);
                phase.set(3);
                phaseStartedAt.set(now);
                return;
            }

            if (currentPhase == 3) {
                if (now - phaseStartedAt.get() < 120L) {
                    return;
                }
                final ItemEntity currentProbe = probe.get();
                helper.assertTrue(
                        currentProbe != null
                            && !currentProbe.isRemoved(),
                        "Remote entity probe disappeared during "
                            + "zero-human simulation"
                );
                helper.assertTrue(
                        currentProbe.tickCount
                            - probeStartAge.get() >= 100L,
                        "Remote non-player entity received only "
                            + (currentProbe.tickCount
                                - probeStartAge.get())
                            + " ticks while the AI was the sole player"
                );
                helper.assertTrue(
                        !level.getBlockState(blockTickProbe)
                                .getValue(RedstoneLampBlock.LIT),
                        "Scheduled block simulation did not run in the "
                            + "AI-only remote chunk"
                );
                helper.assertTrue(
                        !level.getChunkSource()
                                .getForceLoadedChunks()
                                .contains(remoteChunk.pack()),
                        "The test remote chunk was force-loaded instead "
                            + "of maintained by the AI's vanilla player "
                            + "ticket"
                );
                helper.assertTrue(
                        !level.getChunkSource()
                                .getForceLoadedChunks()
                                .contains(
                                    simulationProbeChunk.pack()
                                ),
                        "The outlying simulation probe was force-loaded "
                            + "instead of maintained by the AI's vanilla "
                            + "player ticket"
                );

                final var player = AiPlayerManager.onlinePlayer(server)
                        .orElseThrow();
                final var runtime = CompanionRuntime.active()
                        .filter(candidate ->
                                candidate.server() == server
                        )
                        .orElseThrow(() -> helper.assertionException(
                                "Zero-human test lost its production "
                                    + "runtime"
                        ));
                if (runtime.coreFrames().current().isEmpty()) {
                    helper.assertTrue(
                            now - phaseStartedAt.get() <= 320L,
                            "No fair core frame became available for the "
                                + "damage-reaction gate"
                    );
                    return;
                }

                final Mob attacker = EntityTypes.ZOMBIE.create(
                        level,
                        EntitySpawnReason.COMMAND
                );
                helper.assertTrue(
                        attacker != null,
                        "Could not create the damage-cue attacker"
                );
                attacker.setPos(
                        remoteStand.getX() + 2.5,
                        remoteStand.getY(),
                        remoteStand.getZ() + 0.5
                );
                attacker.setNoAi(true);
                helper.assertTrue(
                        level.addFreshEntity(attacker),
                        "Could not add the damage-cue attacker"
                );
                damageAttacker.set(attacker);
                player.setInvulnerable(false);
                player.invulnerableTime = 0;
                helper.assertTrue(
                        player.hurtServer(
                                level,
                                player.damageSources()
                                        .mobAttack(attacker),
                                2.0F
                        ),
                        "Controlled hostile damage did not reach the "
                            + "headless AI"
                );
                player.setInvulnerable(true);
                final var damageFrame = runtime.coreFrames()
                        .current()
                        .orElseThrow(() -> helper.assertionException(
                                "Damage invalidated the current core frame"
                        ));
                helper.assertTrue(
                        damageFrame.dangerSignals().stream()
                                .anyMatch(signal ->
                                        signal.provenance()
                                            == PerceptionProvenance
                                                .RECENT_DAMAGE_EVENT
                                            && signal.contactDirection()
                                                .isPresent()
                                ),
                        "A hostile hit outside the current view did not "
                            + "produce a directional recent-damage cue"
                );
                final var reaction = runtime.survival().tick(
                        false,
                        false
                );
                helper.assertTrue(
                        reaction.intervened()
                            && reaction.state()
                                != dev.mcai.companion.skills.core
                                    .EmergencySurvivalController
                                    .State.CLEAR,
                        "Local survival controller ignored a confirmed "
                            + "hostile hit: " + reaction
                );
                runtime.survival().reset();
                attacker.discard();
                player.setHealth(player.getMaxHealth());
                killedBody.set(player);
                bodyGenerationBeforeDeath.set(
                        AiPlayerManager.status(server)
                                .sessionGeneration()
                );
                failureCountBeforeDeath.set(
                        CompanionRuntime.runtimeFailureAudit(server)
                                .failureCount()
                );
                player.setInvulnerable(false);
                player.kill(level);
                phase.set(4);
                phaseStartedAt.set(now);
                return;
            }

            helper.assertTrue(
                    CompanionRuntime.runtimeFailureAudit(server)
                            .failureCount()
                        == failureCountBeforeDeath.get(),
                    "Companion runtime threw while the headless player "
                        + "was dead or respawning"
            );
            if (currentPhase == 4) {
                final var replacement =
                        AiPlayerManager.onlinePlayer(server);
                if (replacement.isEmpty()
                        || replacement.orElseThrow()
                            == killedBody.get()
                        || !replacement.orElseThrow().isAlive()
                        || replacement.orElseThrow().getHealth()
                            <= 0.0F) {
                    helper.assertTrue(
                            now - phaseStartedAt.get() <= 200L,
                            "Ordinary zero-human respawn did not produce "
                                + "a fresh living ServerPlayer"
                    );
                    return;
                }
                helper.assertTrue(
                        AiPlayerManager.status(server)
                                .sessionGeneration()
                            > bodyGenerationBeforeDeath.get(),
                        "Respawn replaced the body without advancing the "
                            + "body-session generation"
                );
                replacement.orElseThrow().setInvulnerable(true);
                phase.set(5);
                phaseStartedAt.set(now);
                return;
            }

            if (now - phaseStartedAt.get() < 60L) {
                return;
            }
            final var stableBod…75598 tokens truncated…y; the production
             * skill receives neither these blocks nor their coordinates.
             */
            for (int x = -14; x <= 14; x++) {
                for (int z = -14; z <= 14; z++) {
                    for (int y = -18; y <= -1; y++) {
                        level.setBlockAndUpdate(
                            strongholdReachSearchFeet.offset(x, y, z),
                            Blocks.STONE.defaultBlockState()
                        );
                    }
                }
            }
            for (int x = -6; x <= 102; x++) {
                for (int z = -6; z <= 6; z++) {
                    final BlockPos floor = origin.offset(x, -1, z);
                    level.setBlockAndUpdate(
                        floor,
                        Blocks.SMOOTH_STONE.defaultBlockState()
                    );
                    for (int y = 0; y <= 6; y++) {
                        level.setBlockAndUpdate(
                            origin.offset(x, y, z),
                            Blocks.AIR.defaultBlockState()
                        );
                    }
                }
            }

            /*
             * Model a buried fragment of an actual stronghold wall rather
             * than one isolated marker block. The Eye intersection has an
             * intentional uncertainty radius, so the production skill stops
             * several blocks short of its centre before searching. A wall
             * through that search volume is both representative and
             * independent of the exact legal stopping coordinate. It remains
             * fully occluded by ordinary stone from the surface and the
             * production skill receives none of these fixture coordinates.
             */
            final int strongholdRingRadius = 8;
            for (int offset = -strongholdRingRadius;
                    offset <= strongholdRingRadius; offset++) {
                for (int y = -10; y <= -3; y++) {
                    level.setBlockAndUpdate(
                        strongholdReachSearchFeet.offset(
                            strongholdRingRadius,
                            y,
                            offset
                        ),
                        Blocks.STONE_BRICKS.defaultBlockState()
                    );
                    level.setBlockAndUpdate(
                        strongholdReachSearchFeet.offset(
                            -strongholdRingRadius,
                            y,
                            offset
                        ),
                        Blocks.STONE_BRICKS.defaultBlockState()
                    );
                    level.setBlockAndUpdate(
                        strongholdReachSearchFeet.offset(
                            offset,
                            y,
                            strongholdRingRadius
                        ),
                        Blocks.STONE_BRICKS.defaultBlockState()
                    );
                    level.setBlockAndUpdate(
                        strongholdReachSearchFeet.offset(
                            offset,
                            y,
                            -strongholdRingRadius
                        ),
                        Blocks.STONE_BRICKS.defaultBlockState()
                    );
                }
            }
            /*
             * The approach terminates inside this buried ring. Its first
             * descending leg passes two blocks beneath the side wall, so the
             * next leg is rejected by the global safe-Y bound. The room is
             * at the same vertical offsets as the continuous portal maze:
             * the companion must stop its outward ascending probe on the
             * preserved wall, trial an entry, retreat, climb along the wall,
             * and retry until it discovers the supported room layer.
             */
            for (int x = strongholdRingRadius + 1;
                    x <= strongholdRingRadius + 6; x++) {
                for (int z = -7; z <= 7; z++) {
                    level.setBlockAndUpdate(
                        strongholdReachSearchFeet.offset(x, -5, z),
                        Blocks.STONE_BRICKS.defaultBlockState()
                    );
                    level.setBlockAndUpdate(
                        strongholdReachSearchFeet.offset(x, -4, z),
                        Blocks.AIR.defaultBlockState()
                    );
                    level.setBlockAndUpdate(
                        strongholdReachSearchFeet.offset(x, -3, z),
                        Blocks.AIR.defaultBlockState()
                    );
                }
            }
            strongholdReachEvidence =
                strongholdReachSearchFeet.offset(
                    strongholdRingRadius,
                    -4,
                    3
                );

            final long goalRevision =
                runtime.goals().snapshot().revision();
            final double targetX =
                strongholdReachSearchFeet.getX() + 0.5;
            final double targetZ =
                strongholdReachSearchFeet.getZ() + 0.5;
            final double traceY = strongholdReachSearchFeet.getY() + 1.5;
            runtime.eyeTraceResults().clear();
            runtime.eyeTraceResults().publish(strongholdFixtureTrace(
                goalRevision,
                new PerceptionVec3(targetX - 80.0, traceY, targetZ),
                1.0,
                0.0,
                10L
            ));
            runtime.eyeTraceResults().publish(strongholdFixtureTrace(
                goalRevision,
                new PerceptionVec3(targetX, traceY, targetZ - 80.0),
                0.0,
                1.0,
                20L
            ));

            final var player = player();
            player.getInventory().clearContent();
            player.getInventory().setItem(
                0,
                new ItemStack(Items.IRON_PICKAXE)
            );
            player.getInventory().setItem(
                1,
                new ItemStack(Items.TORCH, 32)
            );
            player.getInventory().setSelectedSlot(0);
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(5.0F);
            player.teleportTo(
                origin.getX() + 0.5,
                origin.getY(),
                origin.getZ() + 0.5
            );
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0.0F;
            face(
                player,
                new Vec3(
                    targetX,
                    player.getEyeY(),
                    targetZ
                )
            );
            enter(Stage.WAITING_FOR_STRONGHOLD_REACH);
        }

        private EyeTraceSnapshot strongholdFixtureTrace(
                final long goalRevision,
                final PerceptionVec3 throwOrigin,
                final double directionX,
                final double directionZ,
                final long observationRevision
        ) {
            return new EyeTraceSnapshot(
                goalRevision,
                DimensionRef.OVERWORLD,
                throwOrigin,
                Integer.toUnsignedLong(runtime.server().getTickCount()),
                observationRevision,
                observationRevision + 1L,
                List.of(
                    new EyeTraceSnapshot.Sample(
                        observationRevision,
                        throwOrigin.add(new PerceptionVec3(
                            directionX,
                            1.0,
                            directionZ
                        ))
                    ),
                    new EyeTraceSnapshot.Sample(
                        observationRevision + 1L,
                        throwOrigin.add(new PerceptionVec3(
                            directionX * 6.0,
                            2.0,
                            directionZ * 6.0
                        ))
                    )
                ),
                directionX,
                directionZ,
                Math.toDegrees(Math.atan2(-directionX, directionZ)),
                5.0
            );
        }

        private void tryStartStrongholdReach() {
            final var body = player();
            if (!body.onGround()) {
                helper.assertTrue(
                    helper.getTick() - stageStartedAt <= 40,
                    "Stronghold reach body did not settle on its course"
                );
                return;
            }
            final BrainObservation observation = freshObservation();
            helper.assertTrue(
                findBlock(observation, "minecraft:stone_bricks") == null,
                "Buried stronghold evidence was visible before excavation"
            );
            final JsonObject trusted = JsonParser.parseString(
                observation.trustedRuntimeJson()
            ).getAsJsonObject();
            final JsonObject traceData = trusted.getAsJsonObject(
                "recentFairEyeTraceData"
            );
            helper.assertTrue(
                traceData != null
                    && traceData.getAsJsonArray("traces").size() == 2
                    && traceData.has("estimatedIntersection"),
                "Stronghold reach did not receive a goal-scoped measured "
                    + "Eye intersection"
            );
            strongholdReachStart = body.position();
            strongholdReachPickaxeDamage =
                inventoryDamage(Items.IRON_PICKAXE);
            strongholdReachTorchCount =
                body.getInventory().countItem(Items.TORCH);
            startSkill(
                StrongholdSkills.REACH_OBSERVED_STRONGHOLD,
                List.of(),
                observation
            );
            enter(Stage.REACHING_STRONGHOLD);
        }

        private void tickStrongholdReach() {
            final SkillSupervisor.Snapshot snapshot = tickSkill(true);
            if (!completed(snapshot)) {
                if ((helper.getTick() - stageStartedAt) % 100 == 0) {
                    MinecraftAiCompanion.LOGGER.info(
                        "Stronghold reach diagnostic tick={} position={} "
                            + "search={} evidence={} checkpoint={} "
                            + "pickaxeDamage={} torches={} supervisor={}",
                        helper.getTick(),
                        player().position(),
                        strongholdReachSearchFeet,
                        strongholdReachEvidence,
                        snapshot.checkpointSequence(),
                        inventoryDamage(Items.IRON_PICKAXE),
                        player().getInventory().countItem(Items.TORCH),
                        snapshot
                    );
                }
                helper.assertTrue(
                    helper.getTick() - stageStartedAt <= 9_000,
                    "Stronghold reach exceeded its physical window"
                );
                return;
            }

            final BrainObservation observation = freshObservation();
            helper.assertTrue(
                horizontalDistance(
                    player().position(),
                    strongholdReachStart
                ) >= 80.0,
                "Stronghold reach did not physically traverse the "
                    + "approach corridor"
            );
            helper.assertTrue(
                player().getY() <= strongholdReachStart.y() - 2.0,
                "Stronghold reach completed without descending underground"
            );
            helper.assertTrue(
                player().getX()
                    > strongholdReachSearchFeet.getX() + 0.5,
                "Stronghold reach stopped at masonry instead of opening "
                    + "and crossing a legal wall entry"
            );
            helper.assertTrue(
                Math.abs(
                    player().getY()
                        - (strongholdReachSearchFeet.getY() - 4.0)
                ) <= 0.25,
                "Stronghold reach crossed into the room's floor layer "
                    + "instead of its supported feet layer: body="
                    + player().position()
            );
            helper.assertTrue(
                helper.getLevel().getBlockState(
                    strongholdReachEvidence
                ).is(Blocks.STONE_BRICKS),
                "Stronghold evidence was mined instead of preserved on sight"
            );
            helper.assertTrue(
                inventoryDamage(Items.IRON_PICKAXE)
                    > strongholdReachPickaxeDamage,
                "Stronghold reach did not consume ordinary pickaxe durability"
            );
            helper.assertTrue(
                player().getInventory().countItem(Items.TORCH)
                    < strongholdReachTorchCount,
                "Stronghold reach did not place an owned torch"
            );
            helper.assertTrue(
                findBlock(observation, "minecraft:stone_bricks") != null,
                "Stronghold reach completed without current first-person "
                    + "stone-brick evidence"
            );
            helper.assertTrue(
                SearchObservedStrongholdPortalRoomSkill
                    .hasObservedAdjacentFrontier(
                        runtime.coreFrames().current().orElseThrow(),
                        true,
                        1,
                        0
                    ),
                "Stronghold reach completed without an immediately "
                    + "traversable first-person portal-search frontier"
            );
            helper.assertTrue(
                runtime.eyeTraceResults()
                    .snapshot(runtime.goals().snapshot().revision())
                    .flatMap(history -> history.estimatedIntersection())
                    .isPresent(),
                "Measured Eye intersection disappeared during the handoff"
            );
            completeFocused();
        }

        private int inventoryDamage(
                final net.minecraft.world.item.Item expected
        ) {
            for (int slot = 0;
                    slot < player().getInventory().getContainerSize();
                    slot++) {
                final ItemStack stack =
                    player().getInventory().getItem(slot);
                if (stack.is(expected)) {
                    return stack.getDamageValue();
                }
            }
            return -1;
        }

        private void tryStartStrongholdTriangulation() {
            final var player = player();
            if (!player.onGround()) {
                helper.assertTrue(
                    helper.getTick() - stageStartedAt <= 40,
                    "Stronghold compound body did not settle on its course"
                );
                return;
            }
            helper.assertTrue(
                player.requestedViewDistance()
                    == runtime.server().getPlayerList()
                        .getViewDistance(),
                "Headless player retained bootstrap view distance "
                    + player.requestedViewDistance()
                    + " instead of server player distance "
                    + runtime.server().getPlayerList()
                        .getViewDistance()
            );
            face(
                player,
                new Vec3(
                    strongholdTraceTarget.getX() + 0.5,
                    player.getEyeY() + 8.0,
                    strongholdTraceTarget.getZ() + 0.5
                )
            );
            final BrainObservation observation = freshObservation();
            startSkill(
                StrongholdSkills
                    .TRIANGULATE_STRONGHOLD_SEARCH_AREA,
                List.of(),
                observation
            );
            enter(Stage.TRIANGULATING_STRONGHOLD);
        }

        private void tickStrongholdTriangulation() {
            final SkillSupervisor.Snapshot snapshot = tickSkill(true);
            if (!completed(snapshot)) {
                if ((helper.getTick() - stageStartedAt) % 100 == 0) {
                    final var body = player();
                    final var navigation = runtime.coreFrames().current()
                            .map(frame -> frame.navigation())
                            .orElse(null);
                    final int observedVoxels = navigation == null
                            ? 0
                            : navigation.observedVoxels().size();
                    final int targetChunkX =
                            SectionPos.blockToSectionCoord(
                                (int) Math.floor(
                                    secondEyeThrowTarget.x()
                                )
                            );
                    final int targetChunkZ =
                            SectionPos.blockToSectionCoord(
                                (int) Math.floor(
                                    secondEyeThrowTarget.z()
                                )
                            );
                    MinecraftAiCompanion.LOGGER.info(
                        "Stronghold compound diagnostic tick={} "
                            + "position={} target={} checkpoint={} "
                            + "requestedView={} bodyChunk={} "
                            + "lastSection={} trackingView={} "
                            + "targetChunkLoaded={} observedVoxels={} "
                            + "supervisor={}",
                        helper.getTick(),
                        body.position(),
                        secondEyeThrowTarget,
                        snapshot.checkpointSequence(),
                        body.requestedViewDistance(),
                        body.chunkPosition(),
                        body.getLastSectionPos(),
                        body.getChunkTrackingView(),
                        body.level().getChunkSource().hasChunk(
                            targetChunkX,
                            targetChunkZ
                        ),
                        observedVoxels,
                        snapshot
                    );
                }
                helper.assertTrue(
                    helper.getTick() - stageStartedAt <= 7_000,
                    "Stronghold compound exceeded its physical window"
                );
                return;
            }
            final JsonObject trusted = JsonParser.parseString(
                freshObservation().trustedRuntimeJson()
            ).getAsJsonObject();
            final JsonObject traceData = trusted.getAsJsonObject(
                "recentFairEyeTraceData"
            );
            helper.assertTrue(
                traceData != null
                    && traceData.getAsJsonArray("traces").size() >= 2
                    && traceData.has("estimatedIntersection"),
                "Compound completed without two fair traces and an "
                    + "estimated search area"
            );
            final JsonObject first = traceData
                .getAsJsonArray("traces")
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("throwOrigin");
            final JsonObject second = traceData
                .getAsJsonArray("traces")
                .get(1)
                .getAsJsonObject()
                .getAsJsonObject("throwOrigin");
            helper.assertTrue(
                Math.hypot(
                    second.get("x").getAsDouble()
                        - first.get("x").getAsDouble(),
                    second.get("z").getAsDouble()
                        - first.get("z").getAsDouble()
                ) >= 250.0,
                "Compound did not create its baseline through physical travel"
            );
            helper.assertTrue(
                player().getInventory().countItem(Items.ENDER_EYE) == 12,
                "Two normal Eye throws did not leave the twelve-Eye "
                    + "portal reserve"
            );
            helper.assertTrue(
                horizontalDistance(
                    player().position(),
                    firstEyeThrowPosition
                ) >= 250.0,
                "Body did not physically travel the measured baseline"
            );
            completeFocused();
        }

        /**
         * The End crystal's ordinary explosion can leave the player's
         * two-second recent-damage cue active even after the fixture moves the
         * body to the eye-throw site. The production stronghold skill is
         * correct to refuse a nonessential throw while that sensory danger is
         * active. Wait for the same fair cue to expire instead of clearing it
         * through a test backdoor or weakening the safety precondition.
         */
        private void tryStartStrongholdEyeTrace() {
            final BrainObservation observation = freshObservation();
            final var liveDangers = runtime.coreFrames().current()
                    .map(frame -> frame.dangerSignals())
                    .orElse(List.of());
            if (!liveDangers.isEmpty()
                    || observation.skillContext().riskScore() > 0.10) {
                helper.assertTrue(
                    helper.getTick() - stageStartedAt
                        <= RECENT_DAMAGE_SETTLE_MAX_TICKS,
                    "Stronghold trace remained unsafe after the End crystal "
                        + "encounter: " + liveDangers
                );
                return;
            }
            firstEyeThrowPosition = player().position();
            startSkill(
                StrongholdSkills.TRACE_STRONGHOLD_EYE,
                List.of(
                    argument("dimension", OVERWORLD),
                    argument(
                        "sampleSequence",
                        Long.toString(sampleSequence(observation))
                    ),
                    argument("hand", "main_hand")
                ),
                observation
            );
            enter(Stage.TRACING_STRONGHOLD_EYE);
        }

        private void tickStrongholdEyeTrace() {
            final SkillSupervisor.Snapshot snapshot = tickSkill(true);
            if (!completed(snapshot)) {
                helper.assertTrue(
                    helper.getTick() - stageStartedAt <= 140,
                    "trace_stronghold_eye exceeded its window"
                );
                return;
            }
            helper.assertTrue(
                player().getInventory().countItem(Items.ENDER_EYE)
                    == 1,
                "stronghold trace did not consume exactly one normally "
                    + "owned Eye of Ender"
            );
            final JsonObject trusted = JsonParser.parseString(
                freshObservation().trustedRuntimeJson()
            ).getAsJsonObject();
            helper.assertTrue(
                trusted.has("recentFairEyeTraceData")
                    && trusted
                        .getAsJsonObject("recentFairEyeTraceData")
                        .getAsJsonArray("traces")
                        .size() >= 1
                    && trusted
                        .getAsJsonObject("recentFairEyeTraceData")
                        .getAsJsonArray("traces")
                        .get(0)
                        .getAsJsonObject()
                        .getAsJsonArray("observedSamples")
                        .size() >= 2,
                "stronghold trace did not publish a visible trajectory"
            );
            prepareSecondEyeTravel(
                    trusted.getAsJsonObject(
                            "recentFairEyeTraceData"
                    )
            );
        }

        private void prepareSecondEyeTravel(
                final JsonObject traceData
        ) {
            final JsonObject first = traceData
                    .getAsJsonArray("traces")
                    .get(0)
                    .getAsJsonObject();
            final double directionX =
                    first.get("directionX").getAsDouble();
            final double directionZ =
                    first.get("directionZ").getAsDouble();
            final double perpendicularX = -directionZ;
            final double perpendicularZ = directionX;
            /*
             * Keep the required baseline above 60 blocks while avoiding an
             * endpoint within a few hundredths of a negative-coordinate
             * chunk boundary. A projectile crossing that synthetic far-world
             * GameTest boundary on its first tick can be parked by the test
             * server's asynchronous entity-section transition, obscuring the
             * first-person trace behavior this scenario is meant to verify.
             */
            secondEyeThrowTarget = firstEyeThrowPosition.add(
                    perpendicularX * 68.0,
                    0.0,
                    perpendicularZ * 68.0
            );
            final int minimumX = (int) Math.floor(Math.min(
                    firstEyeThrowPosition.x(),
                    secondEyeThrowTarget.x()
            )) - 3;
            final int maximumX = (int) Math.ceil(Math.max(
                    firstEyeThrowPosition.x(),
                    secondEyeThrowTarget.x()
            )) + 3;
            final int minimumZ = (int) Math.floor(Math.min(
                    firstEyeThrowPosition.z(),
                    secondEyeThrowTarget.z()
            )) - 3;
            final int maximumZ = (int) Math.ceil(Math.max(
                    firstEyeThrowPosition.z(),
                    secondEyeThrowTarget.z()
            )) + 3;
            final var level = helper.getLevel();
            for (int x = minimumX; x <= maximumX; x++) {
                for (int z = minimumZ; z <= maximumZ; z++) {
                    level.setBlockAndUpdate(
                            new BlockPos(x, origin.getY() - 1, z),
                            Blocks.SMOOTH_STONE.defaultBlockState()
                    );
                    for (int y = 0; y <= 4; y++) {
                        level.setBlockAndUpdate(
                                new BlockPos(
                                        x,
                                        origin.getY() + y,
                                        z
                                ),
                                Blocks.AIR.defaultBlockState()
                        );
                    }
                }
            }
            final BrainObservation observation = freshObservation();
            startSkill(
                    TravelSkills.TRAVEL_TO,
                    List.of(
                            argument("dimension", OVERWORLD),
                            argument(
                                    "x",
                                    decimal(secondEyeThrowTarget.x())
                            ),
                            argument(
                                    "y",
                                    decimal(secondEyeThrowTarget.y())
                            ),
                            argument(
                                    "z",
                                    decimal(secondEyeThrowTarget.z())
                            ),
                            argument("arrivalRadius", "0.8")
                    ),
                    observation
            );
            enter(Stage.TRAVELLING_FOR_SECOND_EYE);
        }

        private void tickTravelForSecondEye() {
            freshObservation();
            final SkillSupervisor.Snapshot snapshot = tickSkill(true);
            if (!completed(snapshot)) {
                helper.assertTrue(
                        helper.getTick() - stageStartedAt <= 1_200,
                        "AI did not physically reposition for a second Eye"
                );
                return;
            }
            helper.assertTrue(
                    horizontalDistance(
                            player().position(),
                            firstEyeThrowPosition
                    ) >= 60.0,
                    "Second Eye baseline was not created by real travel"
            );
            helper.assertTrue(
                    player().getInventory().countItem(
                            Items.ENDER_EYE
                    ) == 1,
                    "Physical Eye reposition changed the remaining Eye count"
            );
            player().setDeltaMovement(Vec3.ZERO);
            /*
             * Travel follows the perpendicular baseline, so its final yaw
             * intentionally points away from the stronghold. A player doing
             * triangulation turns back toward the expected Eye flight before
             * throwing; make that ordinary camera action explicit so the
             * first-person entity sampler has a fair chance to bind the fast
             * vanilla projectile.
             */
            face(
                player(),
                new Vec3(
                    strongholdTraceTarget.getX() + 0.5,
                    player().getEyeY() + 8.0,
                    strongholdTraceTarget.getZ() + 0.5
                )
            );
            final BrainObservation observation = freshObservation();
            startSkill(
                    StrongholdSkills.TRACE_STRONGHOLD_EYE,
                    List.of(
                            argument("dimension", OVERWORLD),
                            argument(
                                    "sampleSequence",
                                    Long.toString(
                                            sampleSequence(observation)
                                    )
                            ),
                            argument("hand", "main_hand")
                    ),
                    observation
            );
            enter(Stage.TRACING_SECOND_STRONGHOLD_EYE);
        }

        private void tickSecondStrongholdEyeTrace() {
            final SkillSupervisor.Snapshot snapshot = tickSkill(true);
            if (!completed(snapshot)) {
                helper.assertTrue(
                        helper.getTick() - stageStartedAt <= 140,
                        "second trace_stronghold_eye exceeded its window"
                );
                return;
            }
            helper.assertTrue(
                    player().getInventory().countItem(
                            Items.ENDER_EYE
                    ) == 0,
                    "Second trace did not consume exactly one owned Eye"
            );
            final JsonObject trusted = JsonParser.parseString(
                    freshObservation().trustedRuntimeJson()
            ).getAsJsonObject();
            final JsonObject traceData = trusted.getAsJsonObject(
                    "recentFairEyeTraceData"
            );
            helper.assertTrue(
                    traceData != null
                        && traceData.getAsJsonArray("traces").size()
                            >= 2,
                    "Two physically separated Eye traces were not retained"
            );
            final JsonObject first = traceData
                    .getAsJsonArray("traces")
                    .get(0)
                    .getAsJsonObject()
                    .getAsJsonObject("throwOrigin");
            final JsonObject second = traceData
                    .getAsJsonArray("traces")
                    .get(1)
                    .getAsJsonObject()
                    .getAsJsonObject("throwOrigin");
            helper.assertTrue(
                    Math.hypot(
                            second.get("x").getAsDouble()
                                - first.get("x").getAsDouble(),
                            second.get("z").getAsDouble()
                                - first.get("z").getAsDouble()
                    ) >= 60.0,
                    "Retained Eye traces did not preserve the real baseline"
            );
            prepareEndPortalActivation();
        }

        private void prepareEndPortalActivation() {
            final var level = helper.getLevel();
            final BlockPos searchStart = player().blockPosition();
            endPortalCenter = searchStart.offset(9, 0, 25);
            endPortalMazeDeadEnd = searchStart.offset(0, 0, 12);
            endPortalMazeSecondTurn = searchStart.offset(9, 0, 8);
            endPortalMazeDeadEndVisited = false;
            endPortalMazeSecondTurnVisited = false;

            /*
             * Start with an opaque stone-brick volume and carve only the
             * interior cells a player could actually traverse. The route
             * first continues into a dead branch, then returns to a junction,
             * turns east, and turns south into the portal room. A ceiling and
             * full-height walls prevent the first-person ray fan from seeing
             * the frame across the untravelled world-space diagonal.
             */
            for (int x = -4; x <= 14; x++) {
                for (int z = -3; z <= 31; z++) {
                    level.setBlockAndUpdate(
                        searchStart.offset(x, -1, z),
                        Blocks.STONE_BRICKS.defaultBlockState()
                    );
                    for (int y = 0; y <= 4; y++) {
                        level.setBlockAndUpdate(
                            searchStart.offset(x, y, z),
                            Blocks.STONE_BRICKS.defaultBlockState()
                        );
                    }
                }
            }
            for (int z = 0; z <= 12; z++) {
                carveStrongholdInterior(searchStart, 0, z);
            }
            for (int x = 0; x <= 9; x++) {
                carveStrongholdInterior(searchStart, x, 8);
            }
            for (int z = 8; z <= 22; z++) {
                carveStrongholdInterior(searchStart, 9, z);
            }
            for (int x = 6; x <= 12; x++) {
                for (int z = 21; z <= 29; z++) {
                    carveStrongholdInterior(searchStart, x, z);
                }
            }
            for (int offset = -1; offset <= 1; offset++) {
                setEmptyEndPortalFrame(
                    endPortalCenter.offset(offset, 0, -2),
                    Direction.SOUTH
                );
                setEmptyEndPortalFrame(
                    endPortalCenter.offset(offset, 0, 2),
                    Direction.NORTH
                );
                setEmptyEndPortalFrame(
                    endPortalCenter.offset(-2, 0, offset),
                    Direction.EAST
                );
                setEmptyEndPortalFrame(
                    endPortalCenter.offset(2, 0, offset),
                    Direction.WEST
                );
            }
            final var player = player();
            player.getInventory().clearContent();
            player.getInventory().setItem(
                0,
                new ItemStack(Items.ENDER_EYE, 12)
            );
            player.getInventory().setSelectedSlot(0);
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.setDeltaMovement(Vec3.ZERO);
            face(
                player,
                Vec3.atCenterOf(endPortalCenter)
                    .add(0.0, 0.2, 0.0)
            );
            final BrainObservation observation = freshObservation();
            helper.assertTrue(
                findBlock(
                    observation,
                    "minecraft:end_portal_frame"
                ) == null,
                "End portal room was visible before physical exploration"
            );
            endPortalSearchStart = player.position();
            enter(Stage.SETTLING_FOR_END_PORTAL_SEARCH);
        }

        private void carveStrongholdInterior(
                final BlockPos origin,
                final int offsetX,
                final int offsetZ
        ) {
            for (int y = 0; y <= 3; y++) {
                helper.getLevel().setBlockAndUpdate(
                        origin.offset(offsetX, y, offsetZ),
                        Blocks.AIR.defaultBlockState()
                );
            }
        }

        private void tickEndPortalSearchSettlement() {
            final var body = player();
            if (!body.onGround()) {
                helper.assertTrue(
                        helper.getTick() - stageStartedAt <= 80,
                        "End portal search body did not settle: position="
                            + body.position()
                            + " velocity="
                            + body.getDeltaMovement()
                            + " onGround="
                            + body.onGround()
                            + " noGravity="
                            + body.isNoGravity()
                            + " feet="
                            + body.level().getBlockState(
                                body.blockPosition()
                            )
                            + " below="
                            + body.level().getBlockState(
                                body.blockPosition().below()
                            )
                );
                return;
            }
            face(
                body,
                Vec3.atCenterOf(endPortalCenter)
                    .add(0.0, 0.2, 0.0)
            );
            final BrainObservation observation = freshObservation();
            helper.assertTrue(
                    findBlock(
                            observation,
                            "minecraft:end_portal_frame"
                    ) == null,
                    "End portal room became visible before search start"
            );
            /*
             * The preceding controlled End-crystal encounter leaves the
             * same short-lived recent-damage signal that production uses to
             * pause nonessential exploration. Let that fair first-person
             * cue expire naturally instead of weakening the stronghold
             * skill's danger precondition or clearing it through a fixture
             * backdoor.
             */
            final var liveDangers = runtime.coreFrames().current()
                    .map(frame -> frame.dangerSignals())
                    .orElse(List.of());
            if (!liveDangers.isEmpty()
                    || observation.skillContext().riskScore() > 0.10) {
                final long dangerWaitTicks =
                        helper.getTick() - stageStartedAt;
                if (dangerWaitTicks == 0L || dangerWaitTicks % 10L == 0L) {
                    final var currentFrame = runtime.coreFrames().current();
                    MinecraftAiCompanion.LOGGER.warn(
                            "End portal danger diagnostic tick={} wait={} "
                                + "worldGameTime={} frameGameTime={} "
                                + "health={} hurtTime={} invulnerableTime={} "
                                + "invulnerable={} position={} "
                                + "dangers={} risk={}",
                            helper.getTick(),
                            dangerWaitTicks,
                            body.level().getGameTime(),
                            currentFrame.map(frame -> frame.gameTime())
                                    .orElse(-1L),
                            body.getHealth(),
                            body.hurtTime,
                            body.invulnerableTime,
                            body.isInvulnerable(),
                            body.position(),
                            liveDangers,
                            observation.skillContext().riskScore()
                    );
                }
                helper.assertTrue(
                        dangerWaitTicks
                                <= RECENT_DAMAGE_SETTLE_MAX_TICKS,
                        "End portal search remained unsafe after the prior "
                                + "crystal encounter: " + liveDangers
                );
                return;
            }
            startSkill(
                StrongholdSkills
                        .SEARCH_OBSERVED_STRONGHOLD_PORTAL_ROOM,
                    List.of(),
                    observation
            );
            endPortalSearchStart = body.position();
            enter(Stage.EXPLORING_FOR_END_PORTAL);
        }

        private void tickExploreForEndPortal() {
            final BrainObservation observation = freshObservation();
            final SkillSupervisor.Snapshot snapshot = tickSkill(true);
            if (horizontalDistance(
                    player().position(),
                    Vec3.atCenterOf(endPortalMazeDeadEnd)
            ) <= 1.35) {
                endPortalMazeDeadEndVisited = true;
            }
            if (endPortalMazeDeadEndVisited
                    && horizontalDistance(
                        player().position(),
                        Vec3.atCenterOf(endPortalMazeSecondTurn)
                    ) <= 1.35) {
                endPortalMazeSecondTurnVisited = true;
            }
            if (!completed(snapshot)) {
                helper.assertTrue(
                        helper.getTick() - stageStartedAt <= 700,
                        "AI did not physically explore for the portal room"
                );
                return;
            }
            helper.assertTrue(
                    horizontalDistance(
                            player().position(),
                            endPortalSearchStart
                    ) >= 8.0,
                    "Portal-room search completed without physical movement"
            );
            helper.assertTrue(
                    endPortalMazeDeadEndVisited,
                    "Portal-room search never entered the occluded dead branch"
            );
            helper.assertTrue(
                    endPortalMazeSecondTurnVisited,
                    "Portal-room search did not backtrack and take the "
                        + "second corridor"
            );
            /*
             * The observation sampled at the start of this tick is the exact
             * fair frame that allowed the production search skill to finish.
             * Re-sampling after the supervisor stops its child movement can
             * legitimately place a peripheral frame ray outside the next
             * finite fan. Hand the causal frame directly to the next atomic
             * skill instead of pretending the completion evidence vanished.
             */
            final BrainObservation activationObservation = observation;
            helper.assertTrue(
                    findBlock(
                            activationObservation,
                            "minecraft:end_portal_frame"
                    ) != null,
                    "Portal-room search completed before a frame was visible"
            );
            startSkill(
                    PortalBuildSkills.ACTIVATE_OBSERVED_END_PORTAL,
                    List.of(),
                    activationObservation
            );
            enter(Stage.ACTIVATING_END_PORTAL);
        }

        private void tickEndPortalActivation() {
            final SkillSupervisor.Snapshot snapshot = tickSkill(true);
            if (!completed(snapshot)) {
                helper.assertTrue(
                    helper.getTick() - stageStartedAt <= 900,
                    "activate_observed_end_portal exceeded its window"
                );
                return;
            }
            int eyedFrames = 0;
            for (int offset = -1; offset <= 1; offset++) {
                for (BlockPos position : List.of(
                        endPortalCenter.offset(offset, 0, -2),
                        endPortalCenter.offset(offset, 0, 2),
                        endPortalCenter.offset(-2, 0, offset),
                        endPortalCenter.offset(2, 0, offset)
                )) {
                    final var state =
                            helper.getLevel().getBlockState(position);
                    helper.assertTrue(
                        state.is(Blocks.END_PORTAL_FRAME)
                            && state.getValue(
                                net.minecraft.world.level.block
                                    .EndPortalFrameBlock.HAS_EYE
                            ),
                        "End portal skill left an empty frame at "
                            + position
                    );
                    eyedFrames++;
                }
            }
            int portalBlocks = 0;
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    helper.assertTrue(
                        helper.getLevel().getBlockState(
                            endPortalCenter.offset(x, 0, z)
                        ).is(Blocks.END_PORTAL),
                        "End portal did not activate its 3x3 interior"
                    );
                    portalBlocks++;
                }
            }
            helper.assertTrue(
                eyedFrames == 12
                    && portalBlocks == 9
                    && player().getInventory().countItem(
                        Items.ENDER_EYE
                    ) == 0,
                "End portal activation did not consume exactly twelve eyes"
            );
            if (scope == ScenarioScope.END_PORTAL_ONLY) {
                completeFocused();
                return;
            }
            prepareBed();
        }

        private void setEmptyEndPortalFrame(
                final BlockPos position,
                final Direction facing
        ) {
            setEndPortalFrame(position, facing, false);
        }

        private void setEndPortalFrame(
                final BlockPos position,
                final Direction facing,
                final boolean hasEye
        ) {
            helper.getLevel().setBlockAndUpdate(
                position,
                Blocks.END_PORTAL_FRAME.defaultBlockState()
                    .setValue(
                        net.minecraft.world.level.block
                            .EndPortalFrameBlock.FACING,
                        facing
                    )
                    .setValue(
                        net.minecraft.world.level.block
                            .EndPortalFrameBlock.HAS_EYE,
                        hasEye
                    )
            );
        }

        private void prepareFocusedEndVictory() {
            final var level = helper.getLevel();
            endPortalCenter = origin.offset(0, 0, 8);
            for (int x = -6; x <= 6; x++) {
                for (int z = -5; z <= 13; z++) {
                    level.setBlockAndUpdate(
                            origin.offset(x, -1, z),
                            Blocks.STONE_BRICKS.defaultBlockState()
                    );
                    for (int y = 0; y <= 4; y++) {
                        level.setBlockAndUpdate(
                                origin.offset(x, y, z),
                                Blocks.AIR.defaultBlockState()
                        );
                    }
                }
            }
            for (int offset = -1; offset <= 1; offset++) {
                setEndPortalFrame(
                        endPortalCenter.offset(offset, 0, -2),
                        Direction.SOUTH,
                        true
                );
                setEndPortalFrame(
                        endPortalCenter.offset(offset, 0, 2),
                        Direction.NORTH,
                        true
                );
                setEndPortalFrame(
                        endPortalCenter.offset(-2, 0, offset),
                        Direction.EAST,
                        true
                );
                setEndPortalFrame(
                        endPortalCenter.offset(2, 0, offset),
                        Direction.WEST,
                        true
                );
            }
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    level.setBlockAndUpdate(
                            endPortalCenter.offset(x, 0, z),
                            Blocks.END_PORTAL.defaultBlockState()
                    );
                }
            }
            final var player = player();
            player.teleportTo(
                    endPortalCenter.getX() + 0.5,
                    endPortalCenter.getY(),
                    endPortalCenter.getZ() - 3.0
            );
            player.setDeltaMovement(Vec3.ZERO);
            face(
                    player,
                    Vec3.atCenterOf(endPortalCenter)
                        .add(0.0, -0.15, 0.0)
            );
            enter(Stage.FINDING_PORTAL);
        }

        private void prepareBed() {
            final var level = helper.getLevel();
            player().stopRiding();
            minecart.discard();
            for (int x = -1; x <= 1; x++) {
                for (int z = 2; z <= 12; z++) {
                    level.setBlockAndUpdate(
                        origin.offset(x, 0, z),
                        Blocks.AIR.defaultBlockState()
                    );
                }
            }

            final BlockPos foot = origin.offset(0, 0, 14);
            bedHead = foot.relative(Direction.SOUTH);
            final var base = Blocks.BED.red().defaultBlockState()
                .setValue(
                    HorizontalDirectionalBlock.FACING,
                    Direction.SOUTH
                );
            level.setBlockAndUpdate(
                bedHead,
                base.setValue(BedBlock.PART, BedPart.HEAD)
            );
            level.setBlockAndUpdate(
                foot,
                base.setValue(BedBlock.PART, BedPart.FOOT)
            );
            level.setBlockAndUpdate(
                foot.below(),
                Blocks.SMOOTH_STONE.defaultBlockState()
            );
            level.setBlockAndUpdate(
                bedHead.below(),
                Blocks.SMOOTH_STONE.defaultBlockState()
            );
            final var player = player();
            player.teleportTo(
                origin.getX() + 0.5,
                origin.getY(),
                origin.getZ() + 12.5
            );
            player.setDeltaMovement(Vec3.ZERO);
            face(player, Vec3.atCenterOf(foot).add(0.0, -0.2, 0.0));
            helper.setTime(13_000L);
            setSleepPercentage(100);
            heldSleepOpen = false;
            observedRealSleep = false;
            enter(Stage.FINDING_BED);
        }

        private void tryStartSleeping() {
            face(
                player(),
                Vec3.atCenterOf(bedHead).add(0.0, -0.2, 0.0)
            );
            final BrainObservation observation = freshObservation();
            final JsonObject target = findBlock(
                observation,
                "minecraft:red_bed"
            );
            if (target == null) {
                awaitTarget("red bed");
                return;
            }
            startSkill(
                SleepSkills.SLEEP_IN_OBSERVED_BED,
                blockArguments(observation, target, false),
                observation
            );
            enter(Stage.SLEEPING);
        }

        private void tickSleeping() {
            final SkillSupervisor.Snapshot snapshot = tickSkill(false);
            if (snapshot.state() == SkillSupervisor.State.RUNNING) {
                /*
                 * Keep the first real sleeping tick observable before
                 * restoring the normal threshold. Time is still advanced by
                 * vanilla's ordinary all-players-sleeping path.
                 */
                if (snapshot.executedTicks() == 1 && !heldSleepOpen) {
                    setSleepPercentage(101);
                    heldSleepOpen = true;
                } else if (snapshot.executedTicks() >= 2
                        && player().isSleeping()) {
                    observedRealSleep = true;
                    setSleepPercentage(100);
                }
            }
            if (!completed(snapshot)) {
                return;
            }
            helper.assertTrue(
                observedRealSleep,
                "sleep_in_observed_bed never entered real ServerPlayer sleep"
            );
            helper.assertTrue(
                !player().isSleeping()
                    && !helper.getLevel().isDarkOutside(),
                "sleep_in_observed_bed completed before a natural dawn wake"
            );
            preparePortal();
        }

        private void preparePortal() {
            setSleepPercentage(originalSleepPercentage);
            final var player = player();
            player.teleportTo(
                endPortalCenter.getX() + 0.5,
                endPortalCenter.getY(),
                endPortalCenter.getZ() - 3.0
            );
            player.setDeltaMovement(Vec3.ZERO);
            face(
                player,
                Vec3.atCenterOf(endPortalCenter)
                    .add(0.0, -0.15, 0.0)
            );
            enter(Stage.FINDING_PORTAL);
        }

        private void tryStartPortalEntry() {
            face(
                player(),
                Vec3.atCenterOf(endPortalCenter)
                    .add(0.0, -0.15, 0.0)
            );
            final BrainObservation observation = freshObservation();
            final JsonObject target = findBlock(
                observation,
                "minecraft:end_portal"
            );
            if (target == null) {
                awaitTarget("end portal");
                return;
            }
            startSkill(
                PortalSkills.ENTER_OBSERVED_PORTAL,
                blockArguments(observation, target, true),
                observation
            );
            enter(Stage.ENTERING_PORTAL);
        }

        private void tickPortalEntry() {
            final SkillSupervisor.Snapshot snapshot = tickSkill(true);
            if (!completed(snapshot)) {
                return;
            }
            final var player = player();
            helper.assertTrue(
                player.level().dimension().equals(Level.END),
                "enter_observed_portal completed without real End traversal"
            );
            prepareEndApproach();
        }

        private void prepareEndApproach() {
            final var end = runtime.server().getLevel(Level.END);
            helper.assertTrue(end != null, "End level is unavailable");
            final var player = player();
            endRouteStart = player.blockPosition();
            endRouteTravelStart = player.position();
            endArena = endRouteStart.offset(0, 0, 30);
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 38; z++) {
                    final boolean gap = z >= 11 && z <= 13;
                    end.setBlockAndUpdate(
                            endRouteStart.offset(x, -1, z),
                            gap
                                ? Blocks.AIR.defaultBlockState()
                                : Blocks.OBSIDIAN.defaultBlockState()
                    );
                    for (int y = 0; y <= 5; y++) {
                        end.setBlockAndUpdate(
                                endRouteStart.offset(x, y, z),
                                Blocks.AIR.defaultBlockState()
                        );
                    }
                    if (gap) {
                        for (int y = -2; y >= -20; y--) {
                            end.setBlockAndUpdate(
                                    endRouteStart.offset(x, y, z),
                                    Blocks.AIR.defaultBlockState()
                            );
                        }
                    }
                }
            }
            player.getInventory().clearContent();
            player.getInventory().setItem(
                    0,
                    new ItemStack(Items.COBBLESTONE, 16)
            );
            player.getInventory().setSelectedSlot(0);
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.setDeltaMovement(Vec3.ZERO);
            face(
                    player,
                    Vec3.atCenterOf(
                            endRouteStart.offset(0, -1, 14)
                    )
            );
            final BrainObservation observation = freshObservation();
            startSkill(
                    TravelSkills.TRAVEL_TO,
                    List.of(
                            argument(
                                    "dimension",
                                    "minecraft:the_end"
                            ),
                            argument(
                                    "x",
                                    decimal(
                                            endRouteStart.getX() + 0.5
                                    )
                            ),
                            argument(
                                    "y",
                                    decimal(endRouteStart.getY())
                            ),
                            argument(
                                    "z",
                                    decimal(
                                            endRouteStart.getZ() + 10.5
                                    )
                            ),
                            argument("arrivalRadius", "0.65")
                    ),
                    observation
            );
            enter(Stage.TRAVELLING_TO_END_GAP);
        }

        private void tickTravelToEndGap() {
            freshObservation();
            final SkillSupervisor.Snapshot snapshot = tickSkill(true);
            if (!completed(snapshot)) {
                helper.assertTrue(
                        helper.getTick() - stageStartedAt <= 360,
                        "AI did not walk from the End spawn to the gap"
                );
                return;
            }
            final BlockPos farSupport =
                    endRouteStart.offset(0, -1, 14);
            face(player(), Vec3.atCenterOf(farSupport));
            final BrainObservation observation = freshObservation();
            startSkill(
                    BridgeSkills.BRIDGE_TO,
                    List.of(
                            argument(
                                    "dimension",
                                    "minecraft:the_end"
                            ),
                            argument(
                                    "x",
                                    decimal(
                                            endRouteStart.getX() + 0.5
                                    )
                            ),
                            argument(
                                    "y",
                                    decimal(endRouteStart.getY())
                            ),
                            argument(
                                    "z",
                                    decimal(
                                            endRouteStart.getZ() + 14.5
                                    )
                            ),
                            argument("arrivalRadius", "0.65"),
                            argument("maxBlocks", "3")
                    ),
                    observation
            );
            enter(Stage.BRIDGING_END_GAP);
        }

        private void tickBridgeEndGap() {
            freshObservation();
            final SkillSupervisor.Snapshot snapshot = tickSkill(true);
            if (!completed(snapshot)) {
                helper.assertTrue(
                        helper.getTick() - stageStartedAt <= 520,
                        "AI did not bridge the three-block End gap"
                );
                return;
            }
            for (int z = 11; z <= 13; z++) {
                helper.assertTrue(
                        player().level()
                            .getBlockState(
                                endRouteStart.offset(0, -1, z)
                            )
                            .is(Blocks.COBBLESTONE),
                        "End bridge omitted an owned cobblestone"
                );
            }
            helper.assertTrue(
                    player().getInventory().countItem(
                            Items.COBBLESTONE
                    ) == 13,
                    "End bridge did not consume exactly three blocks"
            );
            final BrainObservation observation = freshObservation();
            startSkill(
                    TravelSkills.TRAVEL_TO,
                    List.of(
                            argument(
                                    "dimension",
                                    "minecraft:the_end"
                            ),
                            argument(
                                    "x",
                                    decimal(endArena.getX() + 0.5)
                            ),
                            argument(
                                    "y",
                                    decimal(endArena.getY())
                            ),
                            argument(
                                    "z",
                                    decimal(endArena.getZ() + 0.5)
                            ),
                            argument("arrivalRadius", "0.75")
                    ),
                    observation
            );
            enter(Stage.TRAVELLING_TO_END_ARENA);
        }

        private void tickTravelToEndArena() {
            freshObservation();
            final SkillSupervisor.Snapshot snapshot = tickSkill(true);
            if (!completed(snapshot)) {
                helper.assertTrue(
                        helper.getTick() - stageStartedAt <= 600,
                        "AI did not walk from its bridge to the End arena"
                );
                return;
            }
            helper.assertTrue(
                    horizontalDistance(
                            player().position(),
                            endRouteTravelStart
                    ) >= 25.0,
                    "End approach completed without material travel"
            );
            prepareDragonFight();
        }

        private void prepareDragonFight() {
            final var end = runtime.server().getLevel(Level.END);
            helper.assertTrue(end != null, "End level is unavailable");
            for (int x = -20; x <= 20; x++) {
                for (int z = -20; z <= 28; z++) {
                    end.setBlockAndUpdate(
                        endArena.offset(x, -1, z),
                        Blocks.OBSIDIAN.defaultBlockState()
                    );
                    for (int y = 0; y <= 10; y++) {
                        end.setBlockAndUpdate(
                            endArena.offset(x, y, z),
                            Blocks.AIR.defaultBlockState()
                        );
                    }
                }
            }
            final var player = player();
            dragonFightPlayerId = player.getUUID();
            helper.assertTrue(
                    horizontalDistance(
                            player.position(),
                            Vec3.atBottomCenterOf(endArena)
                    ) <= 1.25,
                    "AI reached no valid End arena approach position"
            );
            player.setDeltaMovement(Vec3.ZERO);
            player.getInventory().clearContent();
            player.getInventory().setItem(
                0,
                new ItemStack(Items.BOW)
            );
            player.getInventory().setItem(
                1,
                new ItemStack(Items.ARROW, 99)
            );
            player.getInventory().setItem(
                2,
                new ItemStack(Items.ARROW, 29)
            );
            player.getInventory().setItem(
                3,
                new ItemStack(Items.COOKED_BEEF, 16)
            );
            player.getInventory().setItem(
                4,
                new ItemStack(Items.DIAMOND_SWORD)
            );
            player.getInventory().setItem(
                5,
                new ItemStack(Items.DIAMOND_PICKAXE)
            );
            player.getInventory().setSelectedSlot(0);
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.setItemSlot(
                EquipmentSlot.HEAD,
                new ItemStack(Items.IRON_HELMET)
            );
            player.setItemSlot(
                EquipmentSlot.CHEST,
                new ItemStack(Items.IRON_CHESTPLATE)
            );
            player.setItemSlot(
                EquipmentSlot.LEGS,
                new ItemStack(Items.IRON_LEGGINGS)
            );
            player.setItemSlot(
                EquipmentSlot.FEET,
                new ItemStack(Items.IRON_BOOTS)
            );

            fightCrystal = EntityTypes.END_CRYSTAL.create(
                end,
                EntitySpawnReason.COMMAND
            );
            helper.assertTrue(
                fightCrystal != null,
                "GameTest could not create an End crystal"
            );
            fightCrystal.setPos(
                endArena.getX() + 0.5,
                endArena.getY(),
                endArena.getZ() + 7.5
            );
            helper.assertTrue(
                end.addFreshEntity(fightCrystal),
                "GameTest could not add an End crystal"
            );
            maximumFightCrystalDistance =
                    player.position().distanceTo(
                            fightCrystal.position()
                    );
            /*
             * Use a real two-block collider between the body and the crystal.
             * The former one-block rail sat above the ray to the crystal
             * base, so the crystal could truthfully be clear and its
             * explosion could remove the rail without any mining. That
             * geometry did not prove the cage-opening contract.
             */
            fightCageBar = endArena.offset(0, 0, 3);
            end.setBlockAndUpdate(
                fightCageBar,
                Blocks.IRON_BARS.defaultBlockState()
            );
            end.setBlockAndUpdate(
                fightCageBar.above(),
                Blocks.IRON_BARS.defaultBlockState()
            );
            helper.assertTrue(
                    player.getEyePosition().distanceTo(
                        Vec3.atCenterOf(fightCageBar)
                    ) <= PHYSICAL_CAGE_BREAK_REACH,
                    "Controlled crystal cage is outside ordinary player "
                        + "block reach"
            );

            /*
             * Reuse the dragon already owned by the real End fight manager.
             * Replacing it with an unrelated entity leaves the manager free
             * to respawn or credit its saved UUID, producing two independent
             * dragons. Only create one when this fresh End genuinely has none.
             */
            final List<? extends EnderDragon> managedDragons =
                end.getEntities(
                    EntityTypes.ENDER_DRAGON,
                    existing -> true
                );
            if (managedDragons.isEmpty()) {
                fightDragon = EntityTypes.ENDER_DRAGON.create(
                    end,
                    EntitySpawnReason.COMMAND
                );
                helper.assertTrue(
                    fightDragon != null,
                    "GameTest could not create an Ender Dragon"
                );
                helper.assertTrue(
                    end.addFreshEntity(fightDragon),
                    "GameTest could not add an Ender Dragon"
                );
            } else {
                fightDragon = managedDragons.getFirst();
                managedDragons.stream()
                    .skip(1)
                    .forEach(Entity::discard);
            }
            fightDragon.setPos(
                endArena.getX() + 0.5,
                endArena.getY() + 2.0,
                /* Keep the first cage interaction observable before the
                 * semantic dragon root becomes an immediate melee threat. */
                endArena.getZ() + 12.5
            );
            fightDragon.setYRot(180.0F);
            fightDragon.setNoAi(true);
            fightDragon.setHealth(fightDragon.getMaxHealth());
            positionStaticDragonParts(fightDragon);
            helper.assertTrue(
                    end.getEntities(
                            EntityTypes.ENDER_DRAGON,
                            dragon -> dragon.isAlive()
                        ).size() == 1,
                    "Controlled dragon fixture contains multiple live dragons"
            );
            face(player, fightCrystal.getEyePosition());
            final BrainObservation observation = freshObservation();
            final JsonObject observedCrystal = findEntity(
                    observation,
                    "minecraft:end_crystal"
            );
            helper.assertTrue(
                    observedCrystal != null,
                    "Controlled End crystal was not fairly visible behind "
                        + "the physical cage; visible entities="
                        + semantic(observation)
                            .getAsJsonArray("visibleEntities")
            );
            final JsonObject observedDragon = findEntity(
                    observation,
                    "minecraft:ender_dragon"
            );
            helper.assertTrue(
                    observedDragon != null,
                    "Controlled Ender Dragon was not fairly visible from "
                        + "the initial first-person pose; visible entities="
                        + semantic(observation)
                            .getAsJsonArray("visibleEntities")
            );
            startSkill(
                    CombatSkills.FIGHT_ENDER_DRAGON,
                List.of(),
                observation
            );
            enter(Stage.FIGHTING_DRAGON);
        }

        /**
         * {@code EnderDragon#setNoAi(true)} intentionally skips vanilla's
         * multipart-position update, leaving newly constructed parts at the
         * world origin. This controlled full-health target is stationary so
         * the coordinator can deterministically prove sustained melee, but
         * its selectable parts must still occupy the same relative geometry
         * a yaw-180 dragon would produce.
         */
        private void positionStaticDragonParts(
                final EnderDragon dragon
        ) {
            final EnderDragonPart[] parts =
                    dragon.getSubEntities();
            helper.assertTrue(
                    parts.length == 8,
                    "Unexpected Ender Dragon part count "
                        + parts.length
            );
            final double x = dragon.getX();
            final double y = dragon.getY();
            final double z = dragon.getZ();
            parts[0].setPos(x, y - 1.0, z + 6.5);
            parts[1].setPos(x, y - 1.0, z + 5.5);
            parts[2].setPos(x, y, z + 0.5);
            parts[3].setPos(x, y + 1.5, z - 3.5);
            parts[4].setPos(x, y + 1.5, z - 5.5);
            parts[5].setPos(x, y + 1.5, z - 7.5);
            parts[6].setPos(x - 4.5, y + 2.0, z);
            parts[7].setPos(x + 4.5, y + 2.0, z);
        }

        private void tickDragonFight() {
            if (fightCrystal.isAlive()
                    && !fightCrystal.isRemoved()) {
                maximumFightCrystalDistance = Math.max(
                        maximumFightCrystalDistance,
                        player().position().distanceTo(
                                fightCrystal.position()
                        )
                );
            }
            freshObservation();
            final SkillSupervisor.Snapshot snapshot = tickSkill(true);
            if (!completed(snapshot)) {
                helper.assertTrue(
                    helper.getTick() - stageStartedAt
                        <= OFFLINE_DRAGON_FIGHT_WINDOW_TICKS,
                    "fight_ender_dragon exceeded its window; "
                        + dragonFightDiagnostics()
                );
                return;
            }
            helper.assertTrue(
                !fightCrystal.isAlive() || fightCrystal.isRemoved(),
                "Dragon coordinator did not clear the visible crystal"
            );
            helper.assertTrue(
                    maximumFightCrystalDistance
                        >= PHYSICAL_END_CRYSTAL_STANDOFF,
                    "Dragon coordinator fired inside the End-crystal "
                        + "explosion radius: maximum stand-off was "
                        + maximumFightCrystalDistance
            );
            helper.assertTrue(
                    player().isAlive()
                        && !player().isDeadOrDying()
                        && player().getHealth() > 0.0F,
                    "Dragon coordinator did not survive an ordinary "
                        + "iron-armour "
                        + "End-crystal explosion"
            );
            helper.assertTrue(
                player().level().getBlockState(fightCageBar).isAir(),
                "Dragon coordinator did not normally mine the reachable "
                    + "iron-bar crystal cage"
            );
            helper.assertTrue(
                fightDragon.isDeadOrDying()
                    || fightDragon.getPhaseManager()
                        .getCurrentPhase()
                        .getPhase()
                        == EnderDragonPhase.DYING
                    || fightDragon.isRemoved(),
                "Dragon coordinator completed before killing the dragon"
            );
            helper.assertTrue(
                runtime.worldData()
                    .verifiedRouteProgress(
                        runtime.goals().snapshot().revision()
                    )
                    .milestones()
                    .contains(SurvivalMilestone.DRAGON_KILLED),
                "Server death event did not credit the companion"
            );
            helper.assertTrue(
                player().getInventory().countItem(Items.ARROW) < 128,
                "Dragon coordinator did not use normal ammunition"
            );
            int swordDurabilityConsumed = 0;
            int pickaxeDurabilityConsumed = 0;
            final int arrowsConsumed = 128
                    - player().getInventory().countItem(
                            Items.ARROW
                    );
            for (int slot = 0;
                    slot < player().getInventory()
                        .getContainerSize();
                    slot++) {
                final ItemStack stack =
                        player().getInventory().getItem(slot);
                if (stack.is(Items.DIAMOND_SWORD)) {
                    swordDurabilityConsumed = Math.max(
                            swordDurabilityConsumed,
                            stack.getDamageValue()
                    );
                }
                if (stack.is(Items.DIAMOND_PICKAXE)) {
                    pickaxeDurabilityConsumed = Math.max(
                            pickaxeDurabilityConsumed,
                            stack.getDamageValue()
                    );
                }
            }
            helper.assertTrue(
                    swordDurabilityConsumed >= 20
                        || arrowsConsumed >= 4,
                    "Full-health dragon fight used neither sustained "
                        + "reachable melee nor sustained ordinary bow "
                        + "damage: sword durability="
                        + swordDurabilityConsumed
                        + ", arrows="
                        + arrowsConsumed
            );
            helper.assertTrue(
                    pickaxeDurabilityConsumed >= 1,
                    "Crystal-cage opening did not consume normal pickaxe "
                        + "durability"
            );
            /*
             * The controlled target is frozen while the production skill
             * proves a sustained full-health kill. Vanilla's DYING phase is
             * itself AI-driven: leaving no-AI enabled strands the dragon at
             * one health forever, and the emergency controller correctly
             * treats that still-live Enemy as a contact threat. Resume the
             * ordinary death phase and wait for its real 200-tick removal
             * before testing return-portal entry.
             */
            if (!fightDragon.isRemoved()) {
                fightDragon.setNoAi(false);
                runtime.coreActions().quiesceNow();
                enter(Stage.WAITING_FOR_DRAGON_DEATH);
                return;
            }
            prepareReturnPortal();
        }

        /**
         * Keep the offline dragon gate honest when it times out.  The
         * diagnostic is deliberately made from the same semantic observation
         * supplied to the skill; it is not a fallback entity scan or a world
         * mutation.  Multipart positions are included only to distinguish a
         * missing fair target from a failed actuator.
         */
        private String dragonFightDiagnostics() {
            final var body = player();
            final BrainObservation observation =
                runtime.observations().observe(runtime.goals().snapshot());
            final List<String> parts = fightDragon == null
                ? List.of()
                : java.util.Arrays.stream(fightDragon.getSubEntities())
                    .map(part -> part.name + "@" + part.position())
                    .toList();
            return "body=" + body.position()
                + ", yaw=" + body.getYRot()
                + ", pitch=" + body.getXRot()
                + ", dragonHealth="
                + (fightDragon == null
                    ? "unknown"
                    : fightDragon.getHealth())
                + ", root=" + (fightDragon == null
                    ? "none" : fightDragon.position())
                + ", parts=" + parts
                + ", visibleEntities="
                + semantic(observation).getAsJsonArray("visibleEntities")
                + ", dangerSignals="
                + semantic(observation).getAsJsonArray("dangers")
                + ", supervisor=" + runtime.skillSupervisor().snapshot()
                + ", checkpoint=" + runtime.skillSupervisor()
                    .lastCheckpointPayload().orElse("none");
        }

        private void tickDragonDeathAnimation() {
            freshObservation();
            runtime.survival().tick(false);
                runtime.coreActions().postServerTick();
            helper.assertTrue(
                    helper.getTick() - stageStartedAt <= 420,
                    "Vanilla Ender Dragon death animation did not finish"
            );
            if (!fightDragon.isRemoved()) {
                return;
            }
            helper.assertTrue(
                    !fightDragon.isAlive(),
                    "Removed Ender Dragon remained alive"
            );
            runtime.survival().reset();
            runtime.coreActions().quiesceNow();
            prepareReturnPortal();
        }

        private void prepareReturnPortal() {
            final var end = runtime.server().getLevel(Level.END);
            final var player = player();
            returnPortal = player.blockPosition().offset(0, 0, 5);
            end.setBlockAndUpdate(
                    returnPortal.below(),
                    Blocks.OBSIDIAN.defaultBlockState()
            );
            end.setBlockAndUpdate(
                returnPortal,
                Blocks.END_PORTAL.defaultBlockState()
            );
            player.setDeltaMovement(Vec3.ZERO);
            face(
                player,
                player.position().add(0.0, 0.0, -5.0)
            );
            enter(Stage.FINDING_RETURN_PORTAL);
        }

        private void tryStartReturnPortalEntry() {
            final BrainObservation observation = freshObservation();
            startSkill(
                PortalSkills.FIND_AND_ENTER_OBSERVED_PORTAL,
                List.of(),
                observation
            );
            enter(Stage.ENTERING_RETURN_PORTAL);
        }

        private void tickReturnPortalEntry() {
            final SkillSupervisor.Snapshot snapshot = tickSkill(true);
            if (!completed(snapshot)) {
                return;
            }
            final var returnedPlayer = player();
            helper.assertTrue(
                returnedPlayer.level().dimension().equals(Level.OVERWORLD),
                "Return portal completed without reaching the overworld"
            );
            helper.assertTrue(
                dragonFightPlayerId != null
                    && dragonFightPlayerId.equals(
                        returnedPlayer.getUUID()
                    )
                    && dragonFightPlayerId.equals(
                        runtime.worldData().companionUuid()
                    ),
                "End return did not preserve the credited AI identity"
            );
            helper.assertTrue(
                runtime.worldData()
                    .verifiedRouteProgress(
                        runtime.goals().snapshot().revision()
                    )
                    .milestones()
                    .contains(SurvivalMilestone.RETURNED_FROM_END),
                "Server did not verify the companion's End return"
            );
            final var connectionAudit =
                AiPlayerManager.connectionAudit(runtime.server())
                    .orElseThrow(() -> helper.assertionException(
                        "Headless connection audit disappeared before "
                            + "End return verification"
                    ));
            helper.assertTrue(
                connectionAudit.endCreditsRespawnRequests() == 1L,
                "End return required exactly one WIN_GAME response, got "
                    + connectionAudit.endCreditsRespawnRequests()
            );
            if (scope == ScenarioScope.END_VICTORY_ONLY) {
                completeFocused();
                return;
            }
            final var tickMetrics =
                runtime.tickMetrics().snapshotSince(performanceStart);
            /*
             * Measure this scenario rather than the runtime lifetime. Forge
             * may register GameTests in a different order between builds,
             * and a lifetime average made this gate depend on unrelated
             * fixtures that happened to run first. The production thresholds
             * and the audited skill chain are unchanged.
             */
            MinecraftAiCompanion.LOGGER.info(
                    "Integrated GameTest scenario runtime metrics: "
                        + "samples={}, "
                        + "averageNanos={}, rollingP95Nanos={}, "
                        + "windowMaximumNanos={}, overP95={}",
                    tickMetrics.samples(),
                    Math.round(tickMetrics.averageNanos()),
                    tickMetrics.windowP95Nanos(),
                    tickMetrics.windowMaximumNanos(),
                    tickMetrics.overP95Target()
            );
            helper.assertTrue(
                    tickMetrics.samples() >= 1_000,
                    "Integrated scenario did not collect enough runtime "
                        + "tick samples: "
                        + tickMetrics.samples()
            );
            helper.assertTrue(
                    tickMetrics.averageTargetMet(),
                    "Companion runtime average tick cost exceeded 1 ms: "
                        + tickMetrics.averageNanos()
                        + " ns"
            );
            helper.assertTrue(
                    tickMetrics.p95TargetMet(),
                    "Companion runtime rolling p95 exceeded 2 ms: "
                        + tickMetrics.windowP95Nanos()
                        + " ns"
            );
            helper.assertTrue(
                AiPlayerManager.requestRemove(runtime.server()).accepted(),
                "Companion cleanup was rejected"
            );
            stage = Stage.FINISHED;
            MinecraftAiCompanion.LOGGER.info(
                "Completed real headless_player_lifecycle_state_and_fair_action GameTest"
            );
            helper.succeed();
        }

        private void completeFocused() {
            helper.assertTrue(
                scope != ScenarioScope.FULL,
                "Full scenario attempted focused completion"
            );
            runtime.survival().reset();
            runtime.coreActions().quiesceNow();
            runtime.interactionActions().quiesceNow();
            runtime.boatActions().quiesceNow();
            runtime.minecartActions().quiesceNow();
            final var status = AiPlayerManager.status(runtime.server());
            if (status.state() != SessionState.ABSENT) {
                helper.assertTrue(
                    AiPlayerManager.requestRemove(runtime.server())
                        .accepted(),
                    "Focused movement companion cleanup was rejected"
                );
            }
            stage = Stage.FINISHED;
            MinecraftAiCompanion.LOGGER.info(
                "Completed focused real {} GameTest",
                scope.logName()
            );
            helper.succeed();
        }

        private SkillSupervisor.Snapshot tickSkill(
                final boolean executeCoreLease
        ) {
            final BrainObservation observation =
                runtime.observations().observe(runtime.goals().snapshot());
            final SkillSupervisor.Snapshot snapshot;
            if (executeCoreLease) {
                /*
                 * Match the production arbiter exactly: the 20 TPS
                 * emergency controller runs before the active skill.  A
                 * claimed emergency owns the complete body for this tick;
                 * advancing the lower skill first would let it rotate or
                 * move, after which the emergency overwrites the view and
                 * creates a false alignment timeout.
                */
                if (runtime.skillSupervisor()
                        .consumeActiveSkillEndedHandoff()) {
                    runtime.survival().onActiveSkillEnded();
                }
                final var survival = runtime.survival().tick(
                    runtime.skillSupervisor()
                        .activeSkillManagesVisibleHostileProximity(),
                    runtime.skillSupervisor()
                        .activeSkillManagesPhysicalContactThreats()
                );
                snapshot = survival.intervened()
                    ? runtime.skillSupervisor().snapshot()
                    : runtime.skillSupervisor().tick(
                        connectedContext(observation)
                    );
                final var leaseBefore = runtime.coreActions().snapshot();
                if (scope == ScenarioScope.WATER_ONLY
                        && survival.intervened()) {
                    final var body = player();
                    MinecraftAiCompanion.LOGGER.debug(
                        "Focused water survival state={} reason={} "
                            + "position={} velocity={} fallDistance={} "
                            + "mainHand={}",
                        survival.state(),
                        survival.reason(),
                        body.position(),
                        body.getDeltaMovement(),
                        body.fallDistance,
                        body.getMainHandItem()
                    );
                }
                final var postTick =
                    runtime.coreActions().postServerTick();
                if (stage == Stage.ACQUIRING_NETHER_BLAZE_ROD
                        && (helper.getTick() - stageStartedAt) % 50
                            == 0) {
                    MinecraftAiCompanion.LOGGER.warn(
                        "Nether Blaze lease diagnostic tick={} "
                            + "survival=({},{},{}) before={} "
                            + "post=({},{},{},{}) after={}",
                        helper.getTick(),
                        survival.state(),
                        survival.intervened(),
                        survival.reason(),
                        leaseBefore,
                        postTick.status(),
                        postTick.serverTick(),
                        postTick.tickOutcome(),
                        postTick.failsafeQuiesced(),
                        runtime.coreActions().snapshot()
                    );
                }
            } else {
                snapshot = runtime.skillSupervisor().tick(
                    connectedContext(observation)
                );
            }
            if (snapshot.state() != SkillSupervisor.State.RUNNING
                    && snapshot.state()
                        != SkillSupervisor.State.CANCEL_PENDING
                    && snapshot.state()
                        != SkillSupervisor.State.COMPLETED
                    && stage == Stage.ACQUIRING_ENDER_PEARL) {
                final var fairFrame =
                    runtime.coreFrames().current();
                final List<String> fairVoxels =
                    new java.util.ArrayList<>();
                fairFrame.ifPresent(frame -> {
                    final var feet = frame.feet();
                    frame.navigation().observedVoxels()
                        .values()
                        .stream()
                        .filter(voxel ->
                            Math.abs(
                                voxel.position().x() - feet.x()
                            ) <= 1
                                && voxel.position().y()
                                    >= feet.y() - 1
                                && voxel.position().y()
                                    <= feet.y() + 2
                                && Math.abs(
                                    voxel.position().z() - feet.z()
                                ) <= 1
                        )
                        .sorted(java.util.Comparator.comparing(
                            voxel -> voxel.position().toString()
                        ))
                        .forEach(voxel -> fairVoxels.add(
                            voxel.position() + "="
                                + voxel.kind() + "/"
                                + voxel.occupancyEvidence()
                                + "/support:"
                                + voxel.topSupportAffordance()
                                + "/danger:"
                                + voxel.effectiveDanger()
                                + "/rev:"
                                + voxel.observationRevision()
                        ));
                });
                MinecraftAiCompanion.LOGGER.warn(
                    "Ender reserve failure evidence position={} "
                        + "velocity={} onGround={} fallDistance={} "
                        + "risk={} dangers={} roof={} pillarRemoved={} "
                        + "fairVoxels={} visibleFaces={} terminal={}",
                    player().position(),
                    player().getDeltaMovement(),
                    player().onGround(),
                    player().fallDistance,
                    observation.skillContext().riskScore(),
                    runtime.coreFrames().current()
                        .map(frame -> frame.dangerSignals())
                        .orElse(List.of()),
                    focusedEndermanRoofComplete(),
                    focusedEndermanTemporaryPillarRemoved(),
                    fairVoxels,
                    fairFrame.map(
                        frame -> frame.visibleBlockFaces()
                    ).orElse(List.of()),
                    snapshot.terminalResult()
                );
            }
            if (snapshot.state() == SkillSupervisor.State.FAILED
                    && stage == Stage.BUILDING_NETHER_PORTAL
                    && builtPortalAnchor != null) {
                final List<String> interior = new java.util.ArrayList<>();
                for (int u = 1; u <= 2; u++) {
                    for (int v = 1; v <= 3; v++) {
                        final BlockPos position =
                            builtPortalAnchor.offset(u, v, 0);
                        interior.add(
                            position + "="
                                + helper.getLevel()
                                    .getBlockState(position)
                        );
                    }
                }
                MinecraftAiCompanion.LOGGER.warn(
                    "Portal activation failure world state: "
                        + "interior={} mainHand={}",
                    interior,
                    player().getMainHandItem()
                );
            }
            if (snapshot.state() == SkillSupervisor.State.FAILED
                    && stage == Stage.RETURNING_TO_NETHER_PORTAL) {
                final var body = player();
                final BlockPos feet = body.blockPosition();
                final BlockPos target = BlockPos.containing(
                    netherPortalReturnTarget
                );
                final List<String> corridorVoxels =
                    new java.util.ArrayList<>();
                runtime.observations().navigationSnapshot()
                    .ifPresent(navigation -> {
                        final int minimumX = Math.max(
                            Math.min(feet.getX(), target.getX()) - 1,
                            feet.getX() - 10
                        );
                        final int maximumX = Math.min(
                            Math.max(feet.getX(), target.getX()) + 1,
                            feet.getX() + 10
                        );
                        final int minimumY = Math.max(
                            Math.min(feet.getY(), target.getY()) - 1,
                            feet.getY() - 4
                        );
                        final int maximumY = Math.min(
                            Math.max(feet.getY(), target.getY()) + 2,
                            feet.getY() + 4
                        );
                        final int minimumZ = Math.max(
                            Math.min(feet.getZ(), target.getZ()) - 1,
                            feet.getZ() - 10
                        );
                        final int maximumZ = Math.min(
                            Math.max(feet.getZ(), target.getZ()) + 1,
                            feet.getZ() + 10
                        );
                        for (int x = minimumX; x <= maximumX; x++) {
                            for (int y = minimumY;
                                    y <= maximumY; y++) {
                                for (int z = minimumZ;
                                        z <= maximumZ; z++) {
                                    final var position =
                                        new dev.mcai.companion.navigation.GridPos(
                                            x,
                                            y,
                                            z
                                        );
                                    navigation.voxelAt(position)
                                        .ifPresent(voxel ->
                                            corridorVoxels.add(
                                                position + "="
                                                    + voxel.kind() + "/"
                                                    + voxel.occupancyEvidence()
                                                    + "/support:"
                                                    + voxel.topSupportAffordance()
                                                    + "/danger:"
                                                    + voxel.effectiveDanger()
                                                    + "/rev:"
                                                    + voxel.observationRevision()
                                            )
                                        );
                                }
                            }
                        }
                    });
                MinecraftAiCompanion.LOGGER.warn(
                    "Nether return rejection evidence position={} "
                        + "target={} semanticDangers={} corridorVoxels={}",
                    body.position(),
                    netherPortalReturnTarget,
                    semantic(observation).getAsJsonArray("dangers"),
                    corridorVoxels
                );
            }
            if (snapshot.state() == SkillSupervisor.State.FAILED
                    && stage == Stage.FIGHTING_DRAGON
                    && fightCageBar != null) {
                final var body = player();
                final Vec3 eye = body.getEyePosition();
                final BlockHitResult crosshair = body.level().clip(
                    new ClipContext(
                        eye,
                        eye.add(
                            body.getLookAngle().scale(
                                body.blockInteractionRange()
                            )
                        ),
                        ClipContext.Block.OUTLINE,
                        ClipContext.Fluid.NONE,
                        body
                    )
                );
                final Vec3 entityRayEnd = eye.add(
                        body.getLookAngle().scale(16.0)
                );
                final EntityHitResult entityCrosshair =
                        ProjectileUtil.getEntityHitResult(
                                body,
                                eye,
                                entityRayEnd,
                                body.getBoundingBox()
                                    .expandTowards(
                                        entityRayEnd.subtract(eye)
                                    )
                                    .inflate(1.0),
                                EntitySelector.CAN_BE_PICKED.and(
                                    entity -> entity != body
                                ),
                                16.0 * 16.0
                        );
                final List<String> dragonParts =
                        fightDragon == null
                            ? List.of()
                            : java.util.Arrays.stream(
                                    fightDragon.getSubEntities()
                                )
                                .map(part ->
                                    part.name
                                        + "@"
                                        + part.position()
                                        + " bounds="
                                        + part.getBoundingBox()
                                        + " inRange="
                                        + body.isWithinAttackRange(
                                            body.getMainHandItem(),
                                            part.getBoundingBox(),
                                            0.0
                                        )
                                )
                                .toList();
                MinecraftAiCompanion.LOGGER.warn(
                    "Dragon cage mining failure evidence player={} "
                        + "yaw={} pitch={} mainHand={} bar={} "
                        + "barState={} crosshair={} coreActions={} "
                        + "interactionSession={} entityCrosshair={} "
                        + "dragonRoot={} dragonParts={} visibleEntities={}",
                    body.position(),
                    body.getYRot(),
                    body.getXRot(),
                    body.getMainHandItem(),
                    fightCageBar,
                    body.level().getBlockState(fightCageBar),
                    crosshair,
                    runtime.coreActions().snapshot(),
                    runtime.interactionActions()
                        .sessionGeneration(),
                    entityCrosshair == null
                        ? null
                        : entityCrosshair.getEntity()
                            .getClass().getSimpleName()
                            + "@"
                            + entityCrosshair.getLocation(),
                    fightDragon == null
                        ? null
                        : fightDragon.position(),
                    dragonParts,
                    semantic(observation)
                        .getAsJsonArray("visibleEntities")
                );
            }
            assertNotFailed(snapshot);
            return snapshot;
        }

        private void startSkill(
                final String name,
                final List<SkillArgument> arguments,
                final BrainObservation observation
        ) {
            final SkillContext context = connectedContext(observation);
            final DecisionEnvelope decision = new DecisionEnvelope(
                "gametest-" + name + "-" + helper.getTick(),
                context.worldRevision(),
                context.goalRevision(),
                DecisionKind.START_SKILL,
                name,
                arguments,
                RequestedObservation.none(),
                "",
                1.0
            );
            final SkillSupervisor.StartOutcome outcome =
                runtime.skillSupervisor().start(decision, context);
            if (!outcome.accepted()) {
                MinecraftAiCompanion.LOGGER.warn(
                    "GameTest skill start rejected at tick {}: "
                        + "skill={} failure={} supervisor={}",
                    helper.getTick(),
                    name,
                    outcome.failure()
                        .map(failure -> failure.code())
                        .orElse("unknown"),
                    outcome.snapshot()
                );
            }
            helper.assertTrue(
                outcome.accepted(),
                "Production skill " + name + " rejected GameTest start: "
                    + outcome.failure()
                        .map(failure -> failure.code())
                        .orElse("unknown")
            );
        }

        private BrainObservation freshObservation() {
            runtime.observations().requestObservation(
                new RequestedObservation(
                    ObservationKind.SEMANTIC_REFRESH,
                    "GameTest fixture changed"
                )
            );
            return runtime.observations().observe(
                runtime.goals().snapshot()
            );
        }

        /**
         * The full integration chain forces a refresh after every fixture
         * mutation. Focused movement gates force once during setup, then use
         * the production sampler's ordinary cadence so water and parkour are
         * not accidentally validated with a 20 Hz semantic oracle.
         */
        private BrainObservation movementObservation() {
            if (scope == ScenarioScope.FULL) {
                return freshObservation();
            }
            return runtime.observations().observe(
                runtime.goals().snapshot()
            );
        }

        private SkillContext connectedContext(
                final BrainObservation observation
        ) {
            final SkillContext sampled = observation.skillContext();
            return new SkillContext(
                sampled.goalRevision(),
                sampled.worldRevision(),
                Integer.toUnsignedLong(runtime.server().getTickCount()),
                sampled.hardcore() || scope.forceHardcorePolicy(),
                true,
                sampled.riskScore()
            );
        }

        private List<SkillArgument> blockArguments(
                final BrainObservation observation,
                final JsonObject target,
                final boolean expectedEnd
        ) {
            final JsonObject block = target.getAsJsonObject("block");
            final var arguments = new java.util.ArrayList<SkillArgument>(
                expectedEnd ? 7 : 6
            );
            arguments.add(argument("dimension", OVERWORLD));
            arguments.add(argument(
                "sampleSequence",
                Long.toString(sampleSequence(observation))
            ));
            arguments.add(argument("x", block.get("x").getAsString()));
            arguments.add(argument("y", block.get("y").getAsString()));
            arguments.add(argument("z", block.get("z").getAsString()));
            arguments.add(argument(
                "face",
                target.get("face").getAsString()
            ));
            if (expectedEnd) {
                arguments.add(argument(
                    "expectedDestination",
                    "minecraft:the_end"
                ));
            }
            return List.copyOf(arguments);
        }

        private JsonObject findEntity(
                final BrainObservation observation,
                final String type
        ) {
            final var entities = semantic(observation)
                .getAsJsonArray("visibleEntities");
            for (final var element : entities) {
                final JsonObject entity = element.getAsJsonObject();
                if (type.equals(entity.get("type").getAsString())) {
                    return entity;
                }
            }
            return null;
        }

        private BlockPos nearestPortalBlock(
                final Level level,
                final BlockPos center
        ) {
            BlockPos best = null;
            double bestDistance = Double.POSITIVE_INFINITY;
            for (int x = -4; x <= 4; x++) {
                for (int y = -4; y <= 4; y++) {
                    for (int z = -4; z <= 4; z++) {
                        final BlockPos candidate =
                            center.offset(x, y, z);
                        if (!level.getBlockState(candidate)
                                .is(Blocks.NETHER_PORTAL)) {
                            continue;
                        }
                        final double distance =
                            candidate.distSqr(center);
                        if (distance < bestDistance) {
                            best = candidate.immutable();
                            bestDistance = distance;
                        }
                    }
                }
            }
            if (best == null) {
                throw helper.assertionException(
                    "Verified Nether traversal had no nearby portal"
                );
            }
            return best;
        }

        private JsonObject findBlock(
                final BrainObservation observation,
                final String type
        ) {
            final var blocks = semantic(observation)
                .getAsJsonArray("visibleBlockFaces");
            for (final var element : blocks) {
                final JsonObject block = element.getAsJsonObject();
                if (type.equals(block.get("type").getAsString())) {
                    return block;
                }
            }
            return null;
        }

        private JsonObject findBlockAt(
                final BrainObservation observation,
                final String type,
                final BlockPos position
        ) {
            final var blocks = semantic(observation)
                .getAsJsonArray("visibleBlockFaces");
            for (final var element : blocks) {
                final JsonObject face = element.getAsJsonObject();
                final JsonObject block =
                    face.getAsJsonObject("block");
                if (type.equals(face.get("type").getAsString())
                        && block.get("x").getAsInt()
                            == position.getX()
                        && block.get("y").getAsInt()
                            == position.getY()
                        && block.get("z").getAsInt()
                            == position.getZ()) {
                    return face;
                }
            }
            return null;
        }

        private JsonObject semantic(
                final BrainObservation observation
        ) {
            return JsonParser.parseString(observation.semanticJson())
                .getAsJsonObject();
        }

        private long sampleSequence(
                final BrainObservation observation
        ) {
            return semantic(observation)
                .get("sampleSequence")
                .getAsLong();
        }

        private void awaitTarget(final String description) {
            helper.assertTrue(
                helper.getTick() - stageStartedAt
                    <= TARGET_DISCOVERY_TIMEOUT_TICKS,
                "Fair first-person sampler did not observe " + description
            );
        }

        private boolean completed(
                final SkillSupervisor.Snapshot snapshot
        ) {
            assertNotFailed(snapshot);
            return snapshot.state() == SkillSupervisor.State.COMPLETED;
        }

        private void assertNotFailed(
                final SkillSupervisor.Snapshot snapshot
        ) {
            if (snapshot.state() == SkillSupervisor.State.RUNNING
                    || snapshot.state()
                        == SkillSupervisor.State.CANCEL_PENDING
                    || snapshot.state()
                        == SkillSupervisor.State.COMPLETED) {
                return;
            }
            final String failure = snapshot.terminalResult()
                .flatMap(result -> result.failure())
                .map(value -> value.code())
                .orElse(snapshot.state().name());
            final String bodyState =
                " position=" + player().position()
                    + " velocity=" + player().getDeltaMovement()
                    + " onGround=" + player().onGround();
            throw helper.assertionException(
                "Production skill " + snapshot.skillName()
                    + " terminated unexpectedly: " + failure
                    + " stage=" + stage
                    + bodyState
            );
        }

        private void enter(final Stage next) {
            stage = next;
            stageStartedAt = helper.getTick();
            stageStartedNanos = System.nanoTime();
        }

        private void setSleepPercentage(final int percentage) {
            helper.getLevel().getGameRules().set(
                GameRules.PLAYERS_SLEEPING_PERCENTAGE,
                percentage,
                runtime.server()
            );
        }

        private void setNaturalSpawning(
                final boolean mobs,
                final boolean monsters
        ) {
            helper.getLevel().getGameRules().set(
                GameRules.SPAWN_MOBS,
                mobs,
                runtime.server()
            );
            helper.getLevel().getGameRules().set(
                GameRules.SPAWN_MONSTERS,
                monsters,
                runtime.server()
            );
        }

        private void setMobDrops(final boolean enabled) {
            helper.getLevel().getGameRules().set(
                GameRules.MOB_DROPS,
                enabled,
                runtime.server()
            );
        }

        private void setGenerateStructures(final boolean enabled) {
            final var settings = runtime.server()
                .getWorldGenSettings();
            ((WorldGenSettingsAccessor) (Object) settings)
                .mcai$setOptions(
                    settings.options().withStructures(enabled)
                );
        }

        private net.minecraft.server.level.ServerPlayer player() {
            return AiPlayerManager.onlinePlayer(runtime.server())
                .orElseThrow(() -> helper.assertionException(
                    "Companion body disappeared during integrated skill test"
                ));
        }

        private void cleanup() {
            if (cleaned) {
                return;
            }
            cleaned = true;
            setSleepPercentage(originalSleepPercentage);
            setNaturalSpawning(
                originalSpawnMobs,
                originalSpawnMonsters
            );
            setMobDrops(originalMobDrops);
            setGenerateStructures(originalGenerateStructures);
            if (boat != null && !boat.isRemoved()) {
                boat.discard();
            }
            if (minecart != null && !minecart.isRemoved()) {
                minecart.discard();
            }
            if (rangedTarget != null && !rangedTarget.isRemoved()) {
                rangedTarget.discard();
            }
            if (rangedMinecart != null
                    && !rangedMinecart.isRemoved()) {
                rangedMinecart.discard();
            }
            if (endCrystal != null && !endCrystal.isRemoved()) {
                endCrystal.discard();
            }
            if (fightCrystal != null && !fightCrystal.isRemoved()) {
                fightCrystal.discard();
            }
            if (fightDragon != null && !fightDragon.isRemoved()) {
                fightDragon.discard();
            }
            if (lootDrop != null && !lootDrop.isRemoved()) {
                lootDrop.discard();
            }
            if (occludedThreat != null
                    && !occludedThreat.isRemoved()) {
                occludedThreat.discard();
            }
            if (shelteredEnderman != null
                    && !shelteredEnderman.isRemoved()) {
                shelteredEnderman.discard();
            }
            if (resourceTarget != null
                    && !resourceTarget.isRemoved()) {
                resourceTarget.discard();
            }
            AiPlayerManager.onlinePlayer(runtime.server()).ifPresent(
                player -> {
                    if (player.isSleeping()) {
                        player.stopSleepInBed(true, false);
                    }
                    if (player.level() != runtime.server().overworld()) {
                        player.teleportTo(
                            runtime.server().overworld(),
                            origin.getX() + 0.5,
                            origin.getY(),
                            origin.getZ() + 0.5,
                            Set.of(),
                            0.0F,
                            0.0F,
                            false
                        );
                    }
                }
            );
        }

        private static SkillArgument argument(
                final String name,
                final String value
        ) {
            return new SkillArgument(name, value);
        }

        private static String decimal(final double value) {
            return Double.toString(value == 0.0 ? 0.0 : value);
        }

        /**
         * Entity-spawn helpers consume GameTest-relative coordinates, while
         * block fixtures use the absolute {@link #origin}.  Keep both rooted
         * at the active scenario origin so a focused fixture cannot silently
         * spawn its entity at the full-chain origin.
         */
        private Vec3 fixtureRelative(
                final double x,
                final double y,
                final double z
        ) {
            final BlockPos relativeOrigin =
                    scope == ScenarioScope.FULL
                        ? TEST_ORIGIN
                        : FOCUSED_TEST_ORIGIN;
            return new Vec3(
                relativeOrigin.getX() + x,
                relativeOrigin.getY() + y,
                relativeOrigin.getZ() + z
            );
        }

        private static double horizontalDistance(
                final Vec3 left,
                final Vec3 right
        ) {
            return Math.hypot(left.x() - right.x(), left.z() - right.z());
        }

        private static void face(
                final net.minecraft.server.level.ServerPlayer player,
                final Vec3 target
        ) {
            player.lookAt(EntityAnchorArgument.Anchor.EYES, target);
            player.setYHeadRot(player.getYRot());
        }

        private enum Stage {
            FINDING_BOAT,
            ENTERING_BOAT,
            TRAVELLING_BY_BOAT,
            FINDING_MINECART,
            ENTERING_MINECART,
            TRAVELLING_BY_MINECART,
            SETTLING_FOR_BRIDGE,
            BRIDGING_GAP,
            TOWERING_UP,
            SETTLING_FOR_FOCUSED_WATER,
            WATER_CLUTCH_DESCENDING,
            WATER_CLUTCHING,
            SETTLING_FOR_PARKOUR,
            PARKOUR_RUNNING,
            SETTLING_FOR_PARKOUR_LONG_GAP,
            PARKOUR_LONG_GAP,
            SETTLING_FOR_PARKOUR_TURNING_UP,
            PARKOUR_TURNING_UP,
            TRAVELLING_DIAGONAL_DETOUR,
            SCANNING_NETHER_PORTAL_SITE,
            SETTLING_FOR_NETHER_PORTAL_BUILD,
            BUILDING_NETHER_PORTAL,
            FINDING_BUILT_NETHER_PORTAL,
            ENTERING_BUILT_NETHER_PORTAL,
            EXPLORING_NETHER_FOR_TARGET,
            NETHER_FALL_CLUTCHING,
            WAITING_FOR_FOCUSED_NETHER_SIMULATION,
            FINDING_NETHER_BLAZE,
            ACQUIRING_NETHER_BLAZE_ROD,
            RETURNING_TO_NETHER_PORTAL,
            FINDING_NETHER_RETURN_PORTAL,
            ENTERING_NETHER_RETURN_PORTAL,
            EXPLORING_FOR_TARGET,
            COLLECTING_DROP,
            VERIFYING_OCCLUDED_THREAT,
            SETTLING_FOR_ENDER_RESERVE,
            FINDING_SHELTERED_ENDERMAN,
            ACQUIRING_ENDER_PEARL,
            FINDING_RESOURCE_TARGET,
            ENGAGING_AND_COLLECTING,
            FINDING_RANGED_TARGET,
            SHOOTING_RANGED_TARGET,
            VERIFYING_RANGED_HIT,
            TOWERING_TO_CRYSTAL_CAGE,
            EQUIPPING_CRYSTAL_PICKAXE,
            FINDING_CRYSTAL_CAGE_BAR,
            BREAKING_CRYSTAL_CAGE_BAR,
            DESCENDING_FROM_CRYSTAL_CAGE,
            MOVING_TO_CRYSTAL_SHOT,
            EQUIPPING_CRYSTAL_BOW,
            FINDING_END_CRYSTAL,
            SHOOTING_END_CRYSTAL,
            VERIFYING_END_CRYSTAL,
            WAITING_FOR_STRONGHOLD_TRACE_SAFETY,
            TRACING_STRONGHOLD_EYE,
            TRAVELLING_FOR_SECOND_EYE,
            TRACING_SECOND_STRONGHOLD_EYE,
            WAITING_FOR_STRONGHOLD_COMPOUND,
            TRIANGULATING_STRONGHOLD,
            WAITING_FOR_STRONGHOLD_REACH,
            REACHING_STRONGHOLD,
            SETTLING_FOR_END_PORTAL_SEARCH,
            EXPLORING_FOR_END_PORTAL,
            ACTIVATING_END_PORTAL,
            FINDING_BED,
            SLEEPING,
            FINDING_PORTAL,
            ENTERING_PORTAL,
            TRAVELLING_TO_END_GAP,
            BRIDGING_END_GAP,
            TRAVELLING_TO_END_ARENA,
            FIGHTING_DRAGON,
            WAITING_FOR_DRAGON_DEATH,
            FINDING_RETURN_PORTAL,
            ENTERING_RETURN_PORTAL,
            FINISHED
        }
    }

}
