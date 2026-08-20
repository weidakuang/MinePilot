package dev.mcai.companion.skills.end;

import dev.mcai.companion.MinecraftAiCompanion;
import dev.mcai.companion.brain.BrainObservation;
import dev.mcai.companion.control.GoalSource;
import dev.mcai.companion.control.GoalStatus;
import dev.mcai.companion.embodiment.AiPlayerManager;
import dev.mcai.companion.embodiment.GameTestCompanionSpawn;
import dev.mcai.companion.embodiment.SessionState;
import dev.mcai.companion.model.DecisionEnvelope;
import dev.mcai.companion.model.DecisionKind;
import dev.mcai.companion.model.GatewayStatus;
import dev.mcai.companion.model.ModelGateway;
import dev.mcai.companion.model.ModelOutcome;
import dev.mcai.companion.model.PlannerInput;
import dev.mcai.companion.model.RequestedObservation;
import dev.mcai.companion.runtime.CompanionRuntime;
import dev.mcai.companion.runtime.ServerRuntime;
import dev.mcai.companion.skill.SkillContext;
import dev.mcai.companion.skill.SkillSupervisor;
import dev.mcai.companion.waypoint.DimensionRef;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTest;
import net.minecraftforge.gametest.GameTestDontPrefix;
import net.minecraftforge.gametest.GameTestNamespace;

/**
 * Release-excluded physical gate for the natural End-island ingress skill.
 *
 * <p>The controller and structure remain in the Overworld. The same
 * headless survival player crosses a vanilla End-portal block, after which
 * this fixture performs no teleport, block write, entity write, chunk
 * preload, heightmap lookup, or hidden-terrain query. The production runtime
 * owns every movement and placement until a fresh fair frame proves that the
 * body is standing on natural End stone inside the arena-ready radius.</p>
 */
@GameTestNamespace(MinecraftAiCompanion.MOD_ID)
@GameTestDontPrefix
public final class EndIslandIngressGameTests {
    private static final String TEST_STRUCTURE = "forge:empty48x32x48";
    private static final BlockPos TEST_ORIGIN = new BlockPos(16, 8, 16);
    private static final int MAX_TICKS = 12_000;
    private static final int BODY_TIMEOUT_TICKS = 3_000;
    private static final int END_ENTRY_TIMEOUT_TICKS = 400;
    private static final int END_SETTLE_TIMEOUT_TICKS = 400;
    private static final int BRIDGE_BLOCKS = 64;

    private EndIslandIngressGameTests() {
    }

    @GameTest(
        name = "natural_end_island_ingress",
        environment = "exclusive_natural_end_island_ingress",
        structure = TEST_STRUCTURE,
        maxTicks = MAX_TICKS,
        skyAccess = true,
        padding = 8
    )
    public static void naturalEndIslandIngress(
            final GameTestHelper helper
    ) {
        final var server = helper.getLevel().getServer();
        final AtomicReference<Scenario> scenario = new AtomicReference<>();
        helper.addCleanup(ignored -> {
            final Scenario current = scenario.get();
            if (current != null) {
                current.cleanup();
            } else {
                cleanupRuntime(server);
            }
        });

        final var spawn = GameTestCompanionSpawn.request(
                helper,
                TEST_ORIGIN
        );
        helper.assertTrue(
                spawn.accepted(),
                "Natural End ingress body spawn was rejected: "
                    + spawn.code()
        );
        scheduleEveryTick(helper, MAX_TICKS, () -> {
            final Scenario current = scenario.get();
            if (current != null) {
                current.tick();
                return;
            }
            final var status = AiPlayerManager.status(server);
            helper.assertTrue(
                    status.state() != SessionState.FAILED,
                    "Natural End ingress body failed: " + status
            );
            if (status.state() != SessionState.ACTIVE
                    || !status.online()) {
                helper.assertTrue(
                        helper.getTick() <= BODY_TIMEOUT_TICKS,
                        "Natural End ingress body did not become active"
                );
                return;
            }
            final ServerRuntime runtime = CompanionRuntime.active()
                    .filter(candidate -> candidate.server() == server)
                    .orElseThrow(() -> helper.assertionException(
                            "Natural End ingress has no production runtime"
                    ));
            scenario.set(new Scenario(helper, runtime));
        });
    }

    private static void scheduleEveryTick(
            final GameTestHelper helper,
            final long finalTickInclusive,
            final Runnable action
    ) {
        for (long tick = helper.getTick();
                tick <= finalTickInclusive;
                tick++) {
            final long scheduledTick = tick;
            helper.runAtTickTime(scheduledTick, () -> {
                if (helper.getTick() < scheduledTick) {
                    throw helper.assertionException(
                            "GameTest callback ran before its scheduled tick"
                    );
                }
                action.run();
            });
        }
    }

    private static void cleanupRuntime(
            final net.minecraft.server.MinecraftServer server
    ) {
        CompanionRuntime.active()
                .filter(runtime -> runtime.server() == server)
                .ifPresent(runtime -> {
                    final GoalStatus status = runtime.goals()
                            .snapshot()
                            .status();
                    if (status == GoalStatus.RUNNING
                            || status == GoalStatus.CANCEL_PENDING) {
                        runtime.goals().markTerminal(
                                GoalStatus.SAFE_IDLE,
                                "natural_end_ingress_test_cleanup"
                        );
                    }
                    runtime.survival().reset();
                    runtime.coreActions().quiesceNow();
                    runtime.interactionActions().quiesceNow();
                    runtime.boatActions().quiesceNow();
                    runtime.minecartActions().quiesceNow();
                    runtime.skillSupervisor().abandonForSessionEnd();
                });
        if (AiPlayerManager.status(server).state()
                != SessionState.ABSENT) {
            AiPlayerManager.requestRemove(server);
        }
    }

    private static final class Scenario {
        private final GameTestHelper helper;
        private final ServerRuntime runtime;
        private final UUID bodyId;
        private Stage stage = Stage.ENTERING_END;
        private long stageStartedAt;
        private long ingressStartedAt = -1L;
        private long ingressStartObservationRevision = -1L;
        private int initialBlocks;
        private int initialBlockUseStat;
        private boolean cleaned;

        private Scenario(
                final GameTestHelper helper,
                final ServerRuntime runtime
        ) {
            this.helper = helper;
            this.runtime = runtime;
            helper.assertTrue(
                    helper.getLevel().dimension().equals(Level.OVERWORLD),
                    "GameTest controller did not remain in the Overworld"
            );
            resetRuntime();
            final var goal = runtime.goals().setGoal(
                    "Reach the natural central End island",
                    GoalSource.RECOVERY
            );
            helper.assertTrue(
                    goal.accepted(),
                    "Natural End ingress goal was rejected: " + goal.code()
            );
            final ServerPlayer body = body();
            bodyId = body.getUUID();
            preparePreEntryBody(body);
            stageStartedAt = helper.getTick();
        }

        private void resetRuntime() {
            runtime.survival().reset();
            runtime.coreActions().quiesceNow();
            runtime.interactionActions().quiesceNow();
            runtime.boatActions().quiesceNow();
            runtime.minecartActions().quiesceNow();
            runtime.skillSupervisor().abandonForSessionEnd();
            /*
             * GameTest callbacks run before CompanionRuntime's server-tick
             * listener. Close only the high-level planner so a direct start
             * is not advanced twice on its admission tick; the fixture calls
             * the same supervisor, survival arbiter and player actuator from
             * the next tick onward. This is the established release-excluded
             * physical-gate boundary used by the embodiment suite.
             */
            runtime.brain().close();
            runtime.model().gateway().install(
                    new HoldingGameTestGateway()
            );
        }

        private void preparePreEntryBody(final ServerPlayer body) {
            body.stopRiding();
            if (body.isSleeping()) {
                body.stopSleepInBed(true, false);
            }
            body.setGameMode(GameType.SURVIVAL);
            body.getInventory().clearContent();
            body.getEnderChestInventory().clearContent();
            body.removeAllEffects();
            body.clearFire();
            body.setHealth(body.getMaxHealth());
            body.getFoodData().setFoodLevel(20);
            body.getFoodData().setSaturation(5.0F);
            body.setDeltaMovement(Vec3.ZERO);
            body.fallDistance = 0.0F;
            body.getInventory().setItem(
                    0,
                    new ItemStack(Items.IRON_PICKAXE)
            );
            body.getInventory().setItem(
                    1,
                    new ItemStack(Items.IRON_SWORD)
            );
            body.getInventory().setItem(
                    2,
                    new ItemStack(Items.COOKED_BEEF, 16)
            );
            body.getInventory().setItem(
                    3,
                    new ItemStack(Items.WATER_BUCKET)
            );
            body.setItemSlot(
                    EquipmentSlot.HEAD,
                    new ItemStack(Items.IRON_HELMET)
            );
            body.setItemSlot(
                    EquipmentSlot.CHEST,
                    new ItemStack(Items.IRON_CHESTPLATE)
            );
            body.setItemSlot(
                    EquipmentSlot.LEGS,
                    new ItemStack(Items.IRON_LEGGINGS)
            );
            body.setItemSlot(
                    EquipmentSlot.FEET,
                    new ItemStack(Items.IRON_BOOTS)
            );
            body.setItemSlot(
                    EquipmentSlot.OFFHAND,
                    new ItemStack(Items.SHIELD)
            );
            body.getInventory().setSelectedSlot(0);
            helper.assertTrue(
                    body.getInventory().add(
                            new ItemStack(Items.COBBLESTONE, BRIDGE_BLOCKS)
                    ),
                    "Natural End ingress could not provision owned blocks"
            );
            initialBlocks = body.getInventory().countItem(
                    Items.COBBLESTONE
            );
            initialBlockUseStat = body.getStats().getValue(
                    Stats.ITEM_USED.get(Items.COBBLESTONE)
            );

            /*
             * Fixture setup ends at this point. Placing a vanilla portal
             * under the body and resynchronizing that same pre-entry pose is
             * the established GameTest entry boundary. Once Level.END is
             * observed below, this class never writes either world or moves
             * the player.
             */
            final BlockPos entry = body.blockPosition();
            helper.getLevel().setBlockAndUpdate(
                    entry,
                    Blocks.END_PORTAL.defaultBlockState()
            );
            body.teleportTo(
                    entry.getX() + 0.5D,
                    entry.getY(),
                    entry.getZ() + 0.5D
            );
            body.setDeltaMovement(Vec3.ZERO);
        }

        private void tick() {
            helper.assertTrue(
                    helper.getLevel().dimension().equals(Level.OVERWORLD),
                    "GameTest controller left the Overworld"
            );
            assertNoHumanPlayers();
            final ServerPlayer body = body();
            helper.assertTrue(
                    bodyId.equals(body.getUUID()),
                    "Natural End ingress replaced the companion body"
            );
            helper.assertTrue(
                    body.isAlive()
                        && !body.isDeadOrDying()
                        && body.getHealth() > 0.0F,
                    "Natural End ingress body died"
            );
            switch (stage) {
                case ENTERING_END -> waitForEndEntry(body);
                case SETTLING_END -> waitForStableEndFrame(body);
                case RUNNING -> waitForIngress(body);
                case DONE -> {
                    // GameTest is already terminal.
                }
            }
        }

        private void waitForEndEntry(final ServerPlayer body) {
            if (body.level().dimension().equals(Level.END)) {
                stage = Stage.SETTLING_END;
                stageStartedAt = helper.getTick();
                return;
            }
            helper.assertTrue(
                    helper.getTick() - stageStartedAt
                        <= END_ENTRY_TIMEOUT_TICKS,
                    "Companion did not cross the vanilla End portal"
            );
        }

        private void waitForStableEndFrame(final ServerPlayer body) {
            helper.assertTrue(
                    body.level().dimension().equals(Level.END),
                    "Companion left the End before ingress started"
            );
            final var frame = runtime.coreFrames().current();
            if (!body.onGround()
                    || frame.isEmpty()
                    || !DimensionRef.END.equals(
                            frame.orElseThrow().dimension()
                    )
                    || !frame.orElseThrow().onGround()) {
                helper.assertTrue(
                        helper.getTick() - stageStartedAt
                            <= END_SETTLE_TIMEOUT_TICKS,
                        "Natural End spawn never produced a grounded fair "
                            + "frame: " + diagnostics()
                );
                return;
            }
            final BrainObservation observation = runtime.observations()
                    .observe(runtime.goals().snapshot());
            final SkillContext sampled = observation.skillContext();
            final SkillContext connected = new SkillContext(
                    sampled.goalRevision(),
                    sampled.worldRevision(),
                    Integer.toUnsignedLong(
                            runtime.server().getTickCount()
                    ),
                    sampled.hardcore(),
                    true,
                    sampled.riskScore()
            );
            final DecisionEnvelope decision = new DecisionEnvelope(
                    "gametest-reach-end-island-" + helper.getTick(),
                    connected.worldRevision(),
                    connected.goalRevision(),
                    DecisionKind.START_SKILL,
                    EndSkills.REACH_END_ISLAND,
                    List.of(),
                    RequestedObservation.none(),
                    "",
                    1.0
            );
            final SkillSupervisor.StartOutcome outcome =
                    runtime.skillSupervisor().start(
                            decision,
                            connected
                    );
            helper.assertTrue(
                    outcome.accepted(),
                    "Production reach_end_island rejected natural spawn: "
                        + outcome.failure()
                            .map(failure -> failure.code())
                            .orElse("unknown")
                        + "; " + diagnostics()
            );
            ingressStartedAt = helper.getTick();
            ingressStartObservationRevision = frame.orElseThrow()
                    .observationRevision();
            stage = Stage.RUNNING;
        }

        private void waitForIngress(final ServerPlayer body) {
            helper.assertTrue(
                    body.level().dimension().equals(Level.END),
                    "Companion left the End during island ingress"
            );
            final SkillSupervisor.Snapshot snapshot = advanceSkill();
            if (snapshot.state() == SkillSupervisor.State.RUNNING
                    || snapshot.state()
                        == SkillSupervisor.State.CANCEL_PENDING) {
                return;
            }
            helper.assertTrue(
                    snapshot.state() == SkillSupervisor.State.COMPLETED,
                    "Production reach_end_island did not complete: "
                        + diagnostics()
            );
            final var frame = runtime.coreFrames().current()
                    .orElseThrow(() -> helper.assertionException(
                            "Completed ingress has no fair body frame"
                    ));
            final BlockPos support = body.blockPosition().below();
            final boolean fairCurrentEndStone = frame.visibleBlockFaces()
                    .stream()
                    .anyMatch(face ->
                            face.block().x() == support.getX()
                                && face.block().y() == support.getY()
                                && face.block().z() == support.getZ()
                                && face.blockTypeId().equals(
                                    "minecraft:end_stone"
                                )
                                && face.face().equals("up")
                    );
            final int remainingBlocks = body.getInventory().countItem(
                    Items.COBBLESTONE
            );
            final int usedBlocks = body.getStats().getValue(
                    Stats.ITEM_USED.get(Items.COBBLESTONE)
            ) - initialBlockUseStat;
            helper.assertTrue(
                    frame.observationRevision()
                        > ingressStartObservationRevision,
                    "Ingress completed without a post-start fair frame"
            );
            helper.assertTrue(
                    EndArenaTopology.insideArenaReadyRadius(
                            frame.position()
                    ),
                    "Ingress completed outside the arena-ready radius: "
                        + frame.position()
            );
            helper.assertTrue(
                    fairCurrentEndStone
                        && body.level().getBlockState(support)
                            .is(Blocks.END_STONE),
                    "Ingress completion lacked exact natural End-stone "
                        + "support: " + diagnostics()
            );
            /* A natural seed may expose a continuous End-stone route from
             * the entry platform, so no bridge placement is required. If
             * the route encounters a gap, the production bridge child must
             * still consume an ordinary owned block through the vanilla
             * item-use path; never accept a placement-less gap crossing. */
            helper.assertTrue(
                    remainingBlocks <= initialBlocks
                        && (remainingBlocks == initialBlocks
                            || usedBlocks >= 1),
                    "Ingress changed owned-block accounting without a "
                        + "vanilla placement: initial=" + initialBlocks
                        + ", remaining=" + remainingBlocks
                        + ", itemUseStat=" + usedBlocks
            );
            helper.assertTrue(
                    ingressStartedAt >= 0L
                        && helper.getTick() > ingressStartedAt,
                    "Ingress completed without a physical runtime interval"
            );
            stage = Stage.DONE;
            helper.succeed();
        }

        private SkillSupervisor.Snapshot advanceSkill() {
            final BrainObservation observation = runtime.observations()
                    .observe(runtime.goals().snapshot());
            if (runtime.skillSupervisor()
                    .consumeActiveSkillEndedHandoff()) {
                runtime.survival().onActiveSkillEnded();
            }
            final var survival = runtime.survival().tick(
                    runtime.skillSupervisor()
                        .activeSkillManagesVisibleHostileProximity(),
                    runtime.skillSupervisor()
                        .activeSkillManagesPhysicalContactThreats(),
                    runtime.skillSupervisor()
                        .activeSkillManagesVisibleProjectileThreats()
            );
            final SkillSupervisor.Snapshot snapshot;
            if (survival.intervened()) {
                snapshot = runtime.skillSupervisor().snapshot();
            } else {
                final SkillContext sampled = observation.skillContext();
                snapshot = runtime.skillSupervisor().tick(
                        new SkillContext(
                                sampled.goalRevision(),
                                sampled.worldRevision(),
                                Integer.toUnsignedLong(
                                    runtime.server().getTickCount()
                                ),
                                sampled.hardcore(),
                                true,
                                sampled.riskScore()
                        )
                );
            }
            runtime.coreActions().postServerTick();
            return snapshot;
        }

        private ServerPlayer body() {
            return AiPlayerManager.onlinePlayer(runtime.server())
                    .orElseThrow(() -> helper.assertionException(
                            "Natural End ingress body disappeared"
                    ));
        }

        private void assertNoHumanPlayers() {
            final long humans = runtime.server().getPlayerList()
                    .getPlayers()
                    .stream()
                    .filter(player -> !player.getUUID().equals(bodyId))
                    .count();
            helper.assertTrue(
                    humans == 0L,
                    "Natural End ingress retained " + humans
                        + " human player(s)"
            );
        }

        private String diagnostics() {
            final var snapshot = runtime.skillSupervisor().snapshot();
            final var body = AiPlayerManager.onlinePlayer(runtime.server());
            return "stage=" + stage
                    + ", supervisor=" + snapshot
                    + ", checkpoint=" + runtime.skillSupervisor()
                        .lastCheckpointPayload().orElse("none")
                    + ", dimension=" + body.map(player ->
                            player.level().dimension().identifier()
                                .toString()
                        ).orElse("offline")
                    + ", body=" + body.map(ServerPlayer::position)
                        .orElse(null)
                    + ", onGround=" + body.map(ServerPlayer::onGround)
                        .orElse(false)
                    + ", blocks=" + body.map(player ->
                            player.getInventory().countItem(
                                Items.COBBLESTONE
                            )
                        ).orElse(-1)
                    + ", frame=" + runtime.coreFrames().current()
                        .map(frame -> "revision="
                            + frame.observationRevision()
                            + ", position=" + frame.position()
                            + ", faces=" + frame.visibleBlockFaces())
                        .orElse("unavailable")
                    + ", terrain=" + endTerrainProfile(body);
        }

        private String endTerrainProfile(
                final Optional<ServerPlayer> body
        ) {
            if (body.isEmpty()
                    || !body.orElseThrow().level().dimension()
                        .equals(Level.END)) {
                return "unavailable";
            }
            final ServerLevel end = (ServerLevel) body.orElseThrow().level();
            final StringBuilder profile = new StringBuilder();
            for (int x = 112; x >= 0; x--) {
                final BlockPos position = new BlockPos(x, 49, 0);
                if (!end.getBlockState(position).isAir()) {
                    if (!profile.isEmpty()) {
                        profile.append(';');
                    }
                    profile.append(x).append('=')
                            .append(end.getBlockState(position)
                                    .getBlock().toString());
                }
            }
            return profile.toString();
        }

        private void cleanup() {
            if (cleaned) {
                return;
            }
            cleaned = true;
            cleanupRuntime(runtime.server());
        }
    }

    private enum Stage {
        ENTERING_END,
        SETTLING_END,
        RUNNING,
        DONE
    }

    private static final class HoldingGameTestGateway
            implements ModelGateway {
        private final CompletableFuture<ModelOutcome> pending =
                new CompletableFuture<>();
        private boolean requesting;
        private boolean closed;

        @Override
        public CompletionStage<ModelOutcome> decide(
                final PlannerInput input
        ) {
            if (closed) {
                throw new IllegalStateException(
                        "GameTest gateway is closed"
                );
            }
            requesting = true;
            return pending;
        }

        @Override
        public void cancelForGoalRevision(
                final long currentGoalRevision
        ) {
            // A directly authorized production skill owns this fixture.
        }

        @Override
        public GatewayStatus status() {
            if (closed) {
                return GatewayStatus.CLOSED;
            }
            return requesting
                    ? GatewayStatus.REQUESTING
                    : GatewayStatus.IDLE;
        }

        @Override
        public void close() {
            closed = true;
            pending.cancel(false);
        }
    }
}
