package dev.mcai.companion.gametest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.OptionalDouble;

import com.mojang.authlib.GameProfile;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.gametest.GameTest;
import net.minecraftforge.gametest.GameTestDontPrefix;
import net.minecraftforge.gametest.GameTestNamespace;

import dev.mcai.companion.MinecraftAiCompanion;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.AgentRuntime.ExternalToolCall;
import dev.mcai.companion.agent.body.AgentControlFrame;
import dev.mcai.companion.agent.body.MinePilotServerPlayer;
import dev.mcai.companion.agent.navigation.NavigationIntent;
import dev.mcai.companion.agent.navigation.NavigationTarget;
import dev.mcai.companion.agent.navigation.NavigationToolCoordinator;
import dev.mcai.companion.agent.navigation.NavigationToolCoordinator.Phase;
import dev.mcai.companion.agent.navigation.RouteOption;
import dev.mcai.companion.agent.navigation.TravelPace;

/**
 * Backend-only live-model gate. Assertions use only player chat plus the
 * MinePilot body's coordinates, health, inventory and hotbar.
 */
@GameTestNamespace(MinecraftAiCompanion.MOD_ID)
@GameTestDontPrefix
public final class NavigationBackendGameTests {
    private static final String STRUCTURE = "forge:empty48x32x48";
    private static final long WALL_CLOCK_TIMEOUT_NANOS = Duration.ofSeconds(150).toNanos();

    private NavigationBackendGameTests() {
    }

    /** Real save/logout/login regression, with no inventory reconstruction on rejoin. */
    @GameTest(name = "headless_saved_player_restoration", structure = STRUCTURE,
            maxTicks = 200, padding = 8)
    public static void headlessSavedPlayerRestoration(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        BlockPos feet = helper.absolutePos(new BlockPos(10, 2, 10));
        helper.getLevel().setBlockAndUpdate(feet.below(), Blocks.STONE.defaultBlockState());
        Vec3 savedPosition = Vec3.atBottomCenterOf(feet);
        String identity = "persistence-regression-" + UUID.randomUUID();
        var first = dev.mcai.companion.agent.body.HeadlessPlayerSession.join(
                server, helper.getLevel(), identity, "SaveProbe", savedPosition.x, savedPosition.y, savedPosition.z);
        try {
            var body = first.player();
            body.getInventory().clearContent();
            var pick = new ItemStack(net.minecraft.world.item.Items.WOODEN_PICKAXE);
            pick.setDamageValue(7);
            body.getInventory().setItem(4, pick);
            body.getInventory().setItem(0, new ItemStack(net.minecraft.world.item.Items.SPRUCE_LOG, 6));
            body.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,
                    new ItemStack(net.minecraft.world.item.Items.TORCH, 3));
            body.snapTo(savedPosition, 82.0F, -12.0F);
            body.setDeltaMovement(Vec3.ZERO);
            body.setGameMode(GameType.ADVENTURE);
            body.setHealth(17.0F);
            body.getFoodData().setFoodLevel(13);
        } finally {
            first.close();
        }
        var fallback = server.getLevel(net.minecraft.world.level.Level.NETHER);
        var restored = dev.mcai.companion.agent.body.HeadlessPlayerSession.join(
                server, fallback == null ? helper.getLevel() : fallback, identity, "SaveProbe", 0.5, 100, 0.5);
        helper.addCleanup(ignored -> restored.close());
        var body = restored.player();
        helper.assertTrue(body.position().distanceTo(savedPosition) < 0.001,
                "Rejoin discarded the saved position: " + body.position());
        helper.assertTrue(body.level() == helper.getLevel(), "Rejoin discarded the saved dimension");
        helper.assertTrue(body.gameMode.getGameModeForPlayer() == GameType.ADVENTURE,
                "Rejoin reset the saved game mode");
        helper.assertTrue(body.getYRot() == 82 && body.getXRot() == -12,
                "Rejoin discarded the saved look direction");
        helper.assertTrue(body.getHealth() == 17 && body.getFoodData().getFoodLevel() == 13,
                "Rejoin discarded saved health/food");
        helper.assertTrue(body.getInventory().getItem(0).is(net.minecraft.world.item.Items.SPRUCE_LOG)
                        && body.getInventory().getItem(0).getCount() == 6
                        && body.getInventory().getItem(4).is(net.minecraft.world.item.Items.WOODEN_PICKAXE)
                        && body.getInventory().getItem(4).getDamageValue() == 7
                        && body.getOffhandItem().is(net.minecraft.world.item.Items.TORCH)
                        && body.getOffhandItem().getCount() == 3,
                "Rejoin lost native inventory, offhand or tool durability");
        List<ItemStack> inventory = copyInventory(body);
        helper.onEachTick(() -> {
            restored.tick();
            helper.assertTrue(inventoryEquals(inventory, body), "Loaded inventory changed after login physics");
            helper.assertTrue(body.position().distanceTo(savedPosition) < 0.1,
                    "Loaded body moved away from its saved position");
            if (helper.getTick() >= 10) {
                MinecraftAiCompanion.LOGGER.info("Persistence physical gate: position={}, inventoryRestored=true, "
                        + "woodenPickaxeDamage=7, offhandTorches=3, gameMode=adventure, dimension={}",
                        body.position(), body.level().dimension().identifier());
                helper.succeed();
            }
        });
    }

    /** Controlled physics regressions; deliberately separate from the public Skill gate. */
    @GameTest(name = "navigation_repair_regressions", structure = STRUCTURE,
            maxTicks = 40000, padding = 8)
    public static void navigationRepairRegressions(GameTestHelper helper) {
        if (!Boolean.getBoolean("minepilot.navigationRepairTest")) {
            helper.fail("Repair gate selected without minepilot.navigationRepairTest=true");
            return;
        }
        RepairScenario scenario = new RepairScenario(helper);
        helper.addCleanup(ignored -> scenario.close());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    private static final class RepairScenario implements AutoCloseable {
        private final GameTestHelper helper;
        private final AgentRuntime runtime;
        private final MinePilotServerPlayer body;
        private final NavigationToolCoordinator navigation;
        private final BlockPos origin;
        private BackendHuman human;
        private int scenario;
        private long startedTick;
        private long planningBeganNanos;
        private UUID requestId;
        private Vec3 start;
        private Vec3 previous;
        private int stableTicks;
        private boolean disturbanceApplied;
        private boolean sawAirborne;
        private boolean targetMoved;
        private boolean replanned;
        private long changedRevision;
        private double travelled;
        private double highestY;

        private RepairScenario(GameTestHelper helper) {
            this.helper = helper;
            runtime = AgentRuntime.active(helper.getLevel().getServer());
            helper.assertTrue(runtime != null && runtime.externalControlAvailable(),
                    "Repair gate requires an externally controlled body");
            body = runtime.player();
            navigation = runtime.navigation();
            origin = helper.absolutePos(new BlockPos(10, 2, 10));
        }

        private void start() {
            // Fixture-only mutations. Once a request starts, the Agent moves exclusively
            // through its production coordinator, follower and server-authoritative physics.
            for (int x = -3; x <= (scenario==13?32:18); x++) {
                for (int z = (scenario==13?-7:-3); z <= (scenario==13?7:3); z++) {
                    helper.getLevel().setBlockAndUpdate(origin.offset(x, -1, z),
                            Blocks.STONE.defaultBlockState());
                    for (int y = 0; y <= 4; y++) {
                        helper.getLevel().setBlockAndUpdate(origin.offset(x, y, z),
                                Blocks.AIR.defaultBlockState());
                    }
                }
            }
            double offset = scenario == 0 ? 0.1 : 0.5;
            if (scenario == 5) {
                for (int x = 1; x <= 3; x++) {
                    for (int z = (scenario==13?-7:-3); z <= (scenario==13?7:3); z++) {
                        helper.getLevel().setBlockAndUpdate(origin.offset(x, -1, z), Blocks.AIR.defaultBlockState());
                    }
                }
            } else if (scenario == 6) {
                for (int y = 0; y < 4; y++) {
                    helper.getLevel().setBlockAndUpdate(origin.offset(1, y, 0), Blocks.STONE.defaultBlockState());
                    helper.getLevel().setBlockAndUpdate(origin.offset(0, y, 0), Blocks.LADDER.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.LadderBlock.FACING, net.minecraft.core.Direction.WEST));
                }
            } else if (scenario == 9) {
                for (int x = 1; x <= 3; x++) for (int y = 0; y < x; y++)
                    helper.getLevel().setBlockAndUpdate(origin.offset(x, y, 0), Blocks.STONE.defaultBlockState());
            }
            if(scenario==12)for(int x=1;x<=7;x++)
                helper.getLevel().setBlockAndUpdate(origin.offset(x,0,0),Blocks.STONE_SLAB.defaultBlockState());
            if(scenario==13) {
                for(int x=3;x<=27;x+=3) {
                    int z=(x/3%3-1)*2;
                    for(int y=0;y<4;y++)helper.getLevel().setBlockAndUpdate(origin.offset(x,y,z),Blocks.SPRUCE_LOG.defaultBlockState());
                    for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)helper.getLevel().setBlockAndUpdate(origin.offset(x+dx,3,z+dz),Blocks.SPRUCE_LEAVES.defaultBlockState());
                }
            }
            body.setGameMode(GameType.ADVENTURE);
            body.getInventory().clearContent();
            body.setPos(origin.getX() + offset, origin.getY(), origin.getZ() + 0.5);
            body.setDeltaMovement(Vec3.ZERO);
            body.resetFallDistance();
            body.level().getChunkSource().move(body);
            start = body.position();
            previous = start;
            stableTicks = 0;
            disturbanceApplied = false;
            sawAirborne = false;
            targetMoved = false;
            replanned = false;
            startedTick = helper.getTick();
            travelled = 0;
            highestY = body.getY();
            if (scenario == 7 || scenario == 11) {
                runtime.jumpOnce();
                return;
            }
            NavigationTarget target;
            if (scenario == 4) {
                human = BackendHuman.create(helper,
                        Vec3.atBottomCenterOf(origin.offset(8, 0, 0)));
                target = new NavigationTarget.Named(NavigationTarget.Kind.PLAYER,
                        human.player().getUUID().toString(), 2.0, OptionalDouble.empty());
            } else {
                double targetX = origin.getX() + (scenario == 0 ? 2.4 : scenario == 1 ? 0.5
                        : scenario == 13 ? 30.5 : scenario == 5 ? 4.5 : scenario == 6 ? 1.5 : scenario == 8 ? 10.5 : scenario == 9 ? 3.5 : scenario == 10 ? 8.5 : 6.5);
                target = new NavigationTarget.Coordinates(
                        body.level().dimension().identifier().toString(), targetX,
                        origin.getY() + (scenario == 6 ? 4 : scenario == 9 ? 3 : scenario == 10 ? 6 : scenario == 12 ? .5 : 0), origin.getZ() + (scenario == 8 ? 2.5 : .5),
                        scenario <= 1 ? 2.0 : 0.5, OptionalDouble.empty());
            }
            requestId = UUID.randomUUID();
            navigation.requestNavigation(new NavigationIntent(requestId,
                    navigation.status().worldRevision(), target, scenario == 5 || scenario == 8 || scenario == 13 ? TravelPace.SPRINT : TravelPace.WALK,
                    "Controlled navigation repair regression " + scenario, "GameTest", scenario == 10));
            runtime.say("正在进行移动修复测试。");
            navigation.acknowledgementSent(requestId);
            helper.assertTrue(navigation.status().validActions().contains("plan_navigation"),
                    "Acknowledged request must advertise plan_navigation");
            navigation.planNavigation(requestId);
        }

        private void tick() {
            if (human != null) {
                human.tick();
            }
            // GameTest advances ticks faster than wall time. A bounded worker
            // deadline must use real time; execution still has its original tick bound.
            if(navigation.status().phase()==Phase.PLANNING) {
                if(planningBeganNanos==0)planningBeganNanos=System.nanoTime();
                helper.assertTrue(System.nanoTime()-planningBeganNanos<2_000_000_000L,"Async planning exceeded two real seconds");
                startedTick++;
            } else planningBeganNanos=0;
            helper.assertTrue(helper.getTick() - startedTick < 700,
                    "Repair scenario timed out: " + scenario + " " + navigation.status());
            Vec3 current = body.position();
            travelled += current.distanceTo(previous);
            highestY = Math.max(highestY, current.y);
            helper.assertTrue(current.distanceTo(previous) < 1.0, "Discontinuous body movement");
            helper.assertTrue(body.getInventory().isEmpty(), "Traversal unexpectedly acquired or invented an item");
            boolean stable = body.onGround()
                    && body.getDeltaMovement().horizontalDistanceSqr() < 0.0004
                    && current.subtract(previous).horizontalDistanceSqr() < 0.0004
                    && Math.abs(current.y - previous.y) < 0.01;
            stableTicks = stable ? stableTicks + 1 : 0;
            previous = current;
            helper.assertTrue(body.isAlive() && body.getHealth() == 20.0F,
                    "Navigation regression lost health");
            if (scenario == 7) {
                if (runtime.jumpPhase().equals("COMPLETED")) {
                    helper.assertTrue(highestY - start.y > .8 && current.distanceTo(start) < .05,
                            "Single jump did not physically rise and return: rise=" + (highestY - start.y));
                    advance();
                } else helper.assertTrue(!runtime.jumpPhase().equals("FAILED"), "Single jump failed");
                return;
            }
            if (scenario == 11) {
                if (!disturbanceApplied) {
                    runtime.onChat("TestHuman", "停下");
                    disturbanceApplied = true;
                    helper.assertTrue(runtime.jumpPhase().equals("CANCELLED"), "Stop must cancel the pending jump");
                    helper.assertTrue(runtime.playerChatSince(runtime.latestChatSequence() - 1, 1).getFirst().handledLocally(),
                            "Stop must suppress a pending model action even before navigation starts");
                }
                if (helper.getTick() - startedTick >= 60) {
                    helper.assertTrue(stableTicks >= 30 && current.distanceTo(start) < .05,
                            "Cancelled jump retained an input frame or repeated after landing");
                    advance();
                }
                return;
            }
            if (scenario == 8) {
                helper.assertTrue(highestY <= start.y + .05, "Flat sprint must not bunny-hop");
                helper.assertTrue(travelled <= 13, "Flat movement circled or oscillated");
            }
            NavigationToolCoordinator.Status status = navigation.status();
            if (scenario == 1 && disturbanceApplied && !body.onGround()) {
                sawAirborne = true;
                helper.assertTrue(status.phase() != Phase.COMPLETED,
                        "Navigation completed while the body was airborne");
            }
            if (status.phase() == Phase.PLAN_READY) {
                RouteOption route = status.routeOptions().getFirst();
                navigation.chooseNavigation(requestId, route.optionId(), scenario == 5 || scenario == 8 || scenario == 13 ? TravelPace.SPRINT : TravelPace.WALK);
                if (!disturbanceApplied && scenario == 1) {
                    disturbanceApplied = true;
                    // External impulse fixture: completion must wait for actual landing.
                    body.setDeltaMovement(new Vec3(0, .42, 0));
                    body.setOnGround(false);
                }
                return;
            }
            if ((scenario == 2 || scenario == 3) && status.phase() == Phase.EXECUTING
                    && !disturbanceApplied && current.distanceTo(start) >= 0.03) {
                // Change the imminent segment only after a real forward frame moved the body.
                disturbanceApplied = true;
                RouteOption.PathStep next = status.routeOptions().getFirst().steps().getFirst();
                BlockPos feet = BlockPos.containing(next.x(), next.y(), next.z());
                helper.getLevel().setBlockAndUpdate(scenario == 2 ? feet.below() : feet,
                        scenario == 2 ? Blocks.AIR.defaultBlockState() : Blocks.LAVA.defaultBlockState());
                changedRevision = status.worldRevision();
            }
            if (scenario == 4 && status.phase() == Phase.EXECUTING && !targetMoved
                    && current.distanceTo(start) >= 1.0) {
                human.player().connection.teleport(origin.getX() + 11.5, origin.getY(),
                        origin.getZ() + .5, 0, 0);
                targetMoved = true;
            }
            if (status.phase() == Phase.REPLAN_REQUIRED) {
                if (scenario == 2 || scenario == 3) {
                    helper.assertTrue(disturbanceApplied, "Terrain fixture was not applied");
                    helper.assertTrue(status.worldRevision() > changedRevision,
                            "Observed corridor change did not invalidate its revision");
                    helper.assertTrue(current.distanceTo(start) < 0.5,
                            "Body did not stop before the unsafe next segment: start="+start+", current="+current+", velocity="+body.getDeltaMovement());
                    if (stableTicks < 20) {
                        return;
                    }
                    navigation.cancel(requestId, "Unsafe corridor correctly rejected");
                    advance();
                    return;
                }
                helper.assertTrue(scenario == 4 && targetMoved,
                        "Unexpected replan in scenario " + scenario + ": " + status.lastEventMessage());
                replanned = true;
                navigation.planNavigation(requestId);
                return;
            }
            helper.assertTrue(status.phase() != Phase.FAILED && status.phase() != Phase.CANCELLED,
                    "Navigation failed in scenario " + scenario + ": " + status.lastEventMessage());
            if (status.phase() == Phase.APPROACHED) {
                helper.assertTrue(scenario == 10 && stableTicks >= 20, "Unexpected partial arrival");
                var partial = navigation.partialDestination().orElseThrow();
                helper.assertTrue(current.distanceTo(new Vec3(partial.x(), partial.y(), partial.z())) <= .5,
                        "Partial terminal event lacks physical arrival");
                helper.assertTrue(current.distanceTo(new Vec3(status.destination().x(), status.destination().y(), status.destination().z())) > 5,
                        "Partial arrival must retain the unmet original destination");
                helper.assertTrue(current.distanceTo(start) > 5, "Partial approach made no useful progress");
                advance();
            } else if (status.phase() == Phase.COMPLETED) {
                helper.assertTrue(scenario != 10, "An unreachable elevated target was falsely completed");
                helper.assertTrue(stableTicks >= 20, "Completion preceded 20 physically stable ticks");
                var destination = status.destination();
                helper.assertTrue(current.distanceTo(new Vec3(destination.x(), destination.y(),
                        destination.z())) <= destination.acceptanceRadius(),
                        "Final body coordinates are outside the requested radius");
                if(scenario==13)helper.assertTrue(current.distanceTo(start)>29 && travelled<45,"Forest route stalled or took a disproportionate detour: "+travelled);
                if (scenario == 0) {
                    helper.assertTrue(current.distanceTo(start) >= 0.3,
                            "Exact-start repair did not physically approach the target");
                } else if (scenario == 1) {
                    helper.assertTrue(sawAirborne, "Airborne fixture was not observed");
                } else if (scenario == 4) {
                    helper.assertTrue(targetMoved && current.distanceTo(start) > 5.0,
                            "Moving target was not physically pursued to its updated position");
                }
                advance();
            }
        }

        private void advance() {
            MinecraftAiCompanion.LOGGER.info(
                    "Repair scenario {} passed: start={}, final={}, phase={}, travelled={}, executionTicks={}",
                    scenario, start, body.position(), navigation.status().phase(), travelled, helper.getTick()-startedTick);
            if (++scenario == 14) {
                helper.succeed();
            } else {
                start();
            }
        }

        @Override
        public void close() {
            if (navigation.status().requestId() != null && !navigation.status().phase().terminal()) {
                navigation.cancel(navigation.status().requestId(), "Repair regression stopped");
            }
            if (human != null) {
                human.close();
            }
        }
    }

    @GameTest(
            name = "live_model_chat_navigation_blackbox",
            structure = STRUCTURE,
            maxTicks = 600_000,
            padding = 8
    )
    public static void liveModelChatNavigationBlackbox(GameTestHelper helper) {
        if (!Boolean.getBoolean("minepilot.liveNavigationTest")) {
            helper.fail("Live-model gate was selected without minepilot.liveNavigationTest=true");
            return;
        }
        Scenario scenario = new Scenario(helper);
        helper.addCleanup(ignored -> scenario.close());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Keeps a backend world open for an independently created Codex task.
     * Codex must use the installed skill and loopback MCP endpoint; this test
     * never calls navigation internals on its behalf.
     */
    @GameTest(
            name = "codex_skill_chat_navigation_blackbox",
            structure = STRUCTURE,
            maxTicks = 600_000,
            padding = 8
    )
    public static void codexSkillChatNavigationBlackbox(GameTestHelper helper) {
        if (!Boolean.getBoolean("minepilot.codexSkillTest")) {
            helper.fail("Codex gate was selected without minepilot.codexSkillTest=true");
            return;
        }
        CodexSkillScenario scenario = new CodexSkillScenario(helper);
        helper.addCleanup(ignored -> scenario.close());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Narrow physics regression for the headless Player authority boundary.
     * This is not a substitute for the live chat/model gate above.
     */
    @GameTest(
            name = "headless_control_frame_displacement",
            structure = STRUCTURE,
            maxTicks = 200,
            padding = 8
    )
    public static void headlessControlFrameDisplacement(GameTestHelper helper) {
        ServerPlayer found = helper.getLevel().getServer().getPlayerList()
                .getPlayerByName("MinePilot");
        helper.assertTrue(found instanceof MinePilotServerPlayer,
                "MinePilot headless ServerPlayer is not online");
        MinePilotServerPlayer body = (MinePilotServerPlayer) found;
        BlockPos feet = helper.absolutePos(new BlockPos(10, 2, 10));
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 14; dz++) {
                helper.getLevel().setBlockAndUpdate(
                        feet.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
                helper.getLevel().setBlockAndUpdate(
                        feet.offset(dx, 0, dz), Blocks.AIR.defaultBlockState());
                helper.getLevel().setBlockAndUpdate(
                        feet.offset(dx, 1, dz), Blocks.AIR.defaultBlockState());
            }
        }
        body.setPos(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
        body.setDeltaMovement(Vec3.ZERO);
        body.resetFallDistance();
        body.level().getChunkSource().move(body);
        Vec3 start = body.position();
        float health = body.getHealth();
        List<ItemStack> inventory = copyInventory(body);
        AgentControlFrame forward = new AgentControlFrame(
                0.0F, 0.0F, 1.0F, 0.0F, false, false, false);
        body.applyControlFrame(forward);

        final Vec3[] previous = {start};
        final double[] maximumTickStep = {0.0};
        helper.addCleanup(ignored -> body.stopControlling());
        helper.onEachTick(() -> {
            // NavigationFollower owns this frame in production. Reapply it in
            // the isolated body test because an idle follower clears control
            // at the end of every server tick.
            body.applyControlFrame(forward);
            Vec3 current = body.position();
            maximumTickStep[0] = Math.max(
                    maximumTickStep[0], current.distanceTo(previous[0]));
            previous[0] = current;
            helper.assertTrue(body.isAlive() && body.getHealth() >= health,
                    "MinePilot lost health during flat movement physics regression");
            helper.assertTrue(inventoryEquals(inventory, body),
                    "MinePilot inventory changed during flat movement physics regression");
            helper.assertTrue(maximumTickStep[0] <= 0.9,
                    "MinePilot movement was not continuous vanilla physics: maxStep="
                            + maximumTickStep[0]);
            if (current.distanceTo(start) >= 3.0) {
                body.stopControlling();
                helper.succeed();
            }
        });
    }

    private static final class Scenario implements AutoCloseable {
        private static final double MINIMUM_MOVEMENT = 1.0;
        private static final double ARRIVAL_RADIUS = 2.5;

        private final GameTestHelper helper;
        private BackendHuman human;
        private ServerPlayer body;
        private Vec3 start;
        private Vec3 destination;
        private float startingHealth;
        private List<ItemStack> startingInventory;
        private long startedNanos;
        private boolean moved;
        private boolean acknowledgementSeen;
        private long arrivalObservedTick = -1L;
        private Vec3 arrivalPosition;

        private Scenario(GameTestHelper helper) {
            this.helper = helper;
        }

        private void start() {
            body = helper.getLevel().getServer().getPlayerList()
                    .getPlayerByName("MinePilot");
            helper.assertTrue(body != null, "MinePilot body is not online");

            BlockPos agentFeet = helper.absolutePos(new BlockPos(10, 2, 10));
            BlockPos humanFeet = helper.absolutePos(new BlockPos(20, 2, 10));
            prepareFlatArena(agentFeet, humanFeet);
            body.setPos(agentFeet.getX() + 0.5, agentFeet.getY(), agentFeet.getZ() + 0.5);
            body.setDeltaMovement(Vec3.ZERO);
            body.resetFallDistance();
            body.level().getChunkSource().move(body);

            human = BackendHuman.create(helper, Vec3.atBottomCenterOf(humanFeet));
            start = body.position();
            destination = human.player().position();
            startingHealth = body.getHealth();
            startingInventory = copyInventory(body);
            startedNanos = System.nanoTime();

            Component submitted = ForgeHooks.onServerChatSubmittedEvent(
                    human.player(), Component.literal("到我这里来，到了以后停下。"));
            helper.assertTrue(submitted != null, "Normal player chat was cancelled");
        }

        private void tick() {
            human.tick();
            Vec3 current = body.position();
            List<String> chat = human.drainSystemChat();
            helper.assertFalse(chat.stream().anyMatch(text ->
                            text.contains("could not reach the configured model")),
                    "The configured model request failed before navigation began; start="
                            + start + ", current=" + current + ", target=" + destination
                            + ", health=" + body.getHealth()
                            + ", inventoryUnchanged="
                            + inventoryEquals(startingInventory, body));
            acknowledgementSeen |= chat.stream()
                    .anyMatch(text -> text.startsWith("[AI] MinePilot:"));

            boolean nowMoved = current.distanceTo(start) >= MINIMUM_MOVEMENT;
            if (nowMoved && !moved) {
                helper.assertTrue(acknowledgementSeen,
                        "MinePilot changed position before visible acknowledgement chat");
            }
            moved |= nowMoved;
            helper.assertTrue(body.isAlive(), "MinePilot died during the navigation gate");
            helper.assertTrue(body.getHealth() >= startingHealth,
                    "MinePilot lost health during the flat navigation gate");
            helper.assertTrue(inventoryEquals(startingInventory, body),
                    "MinePilot inventory changed during navigation");

            if (current.distanceTo(destination) <= ARRIVAL_RADIUS) {
                if (arrivalObservedTick < 0L) {
                    arrivalObservedTick = helper.getTick();
                    arrivalPosition = current;
                    return;
                }
                if (helper.getTick() - arrivalObservedTick < 20L) {
                    return;
                }
                helper.assertTrue(moved,
                        "MinePilot reached the assertion without physical displacement");
                helper.assertTrue(acknowledgementSeen,
                        "MinePilot moved without first sending visible acknowledgement chat");
                helper.assertTrue(current.distanceTo(arrivalPosition) <= 1.5,
                        "MinePilot crossed the target but failed to stop; firstArrival="
                                + arrivalPosition + ", current=" + current);
                helper.succeed();
                return;
            }
            if (arrivalObservedTick >= 0L) {
                helper.fail("MinePilot entered the arrival radius and then left it; firstArrival="
                        + arrivalPosition + ", current=" + current);
                return;
            }
            helper.assertTrue(System.nanoTime() - startedNanos <= WALL_CLOCK_TIMEOUT_NANOS,
                    "MinePilot did not physically reach the player within 150 seconds; start="
                            + start + ", current=" + current + ", target=" + destination);
        }

        private void prepareFlatArena(BlockPos start, BlockPos end) {
            int minX = Math.min(start.getX(), end.getX()) - 5;
            int maxX = Math.max(start.getX(), end.getX()) + 5;
            int minZ = Math.min(start.getZ(), end.getZ()) - 5;
            int maxZ = Math.max(start.getZ(), end.getZ()) + 5;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    helper.getLevel().setBlockAndUpdate(
                            new BlockPos(x, start.getY() - 1, z),
                            Blocks.STONE.defaultBlockState());
                    for (int y = start.getY(); y <= start.getY() + 2; y++) {
                        helper.getLevel().setBlockAndUpdate(
                                new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }

        @Override
        public void close() {
            if (human != null) {
                human.close();
                human = null;
            }
        }
    }

    private static final class CodexSkillScenario implements AutoCloseable {
        private static final double MINIMUM_MOVEMENT = 1.0;
        private static final double ARRIVAL_RADIUS = 2.5;
        private static final double STOP_SPEED = 0.03;
        private static final double STOP_DRIFT = 0.35;
        private static final int STOP_STABILITY_TICKS = 20;
        // A separate Codex/Luna reasoning turn can take longer than one minute.
        // Keep the stopped body observable until it reads one target-bound
        // terminal status, while retaining a wall-clock upper bound.
        private static final long RESULT_OBSERVATION_NANOS =
                Duration.ofMinutes(5).toNanos();
        private static final long TIMEOUT_NANOS = Duration.ofMinutes(8).toNanos();

        private final GameTestHelper helper;
        private BackendHuman human;
        private ServerPlayer body;
        private AgentRuntime runtime;
        private Vec3 start;
        private Vec3 destination;
        private float startingHealth;
        private List<ItemStack> startingInventory;
        private long startedNanos;
        private long externalTraceStart;
        private boolean visibleAiChatSeen;
        private boolean moved;
        private long stopCandidateTick = -1L;
        private Vec3 stopCandidatePosition;
        private long verifiedStopTick = -1L;
        private long verifiedStopNanos = -1L;
        private boolean codexTraceVerified;

        private CodexSkillScenario(GameTestHelper helper) {
            this.helper = helper;
        }

        private void start() {
            body = helper.getLevel().getServer().getPlayerList()
                    .getPlayerByName("MinePilot");
            helper.assertTrue(body != null, "MinePilot body is not online");
            runtime = AgentRuntime.active(helper.getLevel().getServer());
            helper.assertTrue(runtime != null, "MinePilot runtime is not available");
            externalTraceStart = runtime.latestExternalToolSequence();

            BlockPos agentFeet = helper.absolutePos(new BlockPos(10, 2, 10));
            BlockPos humanFeet = helper.absolutePos(new BlockPos(20, 2, 10));
            prepareFlatArena(agentFeet, humanFeet);
            body.setPos(agentFeet.getX() + 0.5, agentFeet.getY(), agentFeet.getZ() + 0.5);
            body.setDeltaMovement(Vec3.ZERO);
            body.resetFallDistance();
            body.level().getChunkSource().move(body);

            human = BackendHuman.create(helper, Vec3.atBottomCenterOf(humanFeet));
            start = body.position();
            destination = human.player().position();
            startingHealth = body.getHealth();
            startingInventory = copyInventory(body);
            startedNanos = System.nanoTime();
        }

        private void tick() {
            human.tick();
            List<String> chat = human.drainSystemChat();
            visibleAiChatSeen |= chat.stream()
                    .anyMatch(text -> text.startsWith("[AI] MinePilot:"));

            Vec3 current = body.position();
            boolean nowMoved = current.distanceTo(start) >= MINIMUM_MOVEMENT;
            if (nowMoved && !moved) {
                helper.assertTrue(visibleAiChatSeen,
                        "Codex moved MinePilot before sending visible in-game chat");
            }
            moved |= nowMoved;
            helper.assertTrue(body.isAlive(),
                    "MinePilot died during the Codex skill test");
            helper.assertTrue(body.getHealth() >= startingHealth,
                    "MinePilot lost health during the Codex skill test");
            helper.assertTrue(inventoryEquals(startingInventory, body),
                    "MinePilot inventory changed during the Codex skill test");
            captureCodexMcpTrace();

            double horizontalSpeed = Math.hypot(
                    body.getDeltaMovement().x, body.getDeltaMovement().z);
            boolean stoppedAtDestination = current.distanceTo(destination) <= ARRIVAL_RADIUS
                    && body.onGround()
                    && horizontalSpeed <= STOP_SPEED;
            if (verifiedStopTick >= 0L) {
                helper.assertTrue(current.distanceTo(stopCandidatePosition) <= STOP_DRIFT,
                        "MinePilot moved after a verified stop; stoppedAt="
                                + stopCandidatePosition + ", current=" + current);
                if (codexTraceVerified) {
                    helper.succeed();
                    return;
                }
                helper.assertTrue(System.nanoTime() - verifiedStopNanos
                                <= RESULT_OBSERVATION_NANOS,
                        "Codex never returned a target-bound COMPLETED status within five minutes");
                return;
            }
            if (stoppedAtDestination) {
                if (stopCandidateTick < 0L
                        || current.distanceTo(stopCandidatePosition) > STOP_DRIFT) {
                    stopCandidateTick = helper.getTick();
                    stopCandidatePosition = current;
                    return;
                }
                if (helper.getTick() - stopCandidateTick >= STOP_STABILITY_TICKS) {
                    helper.assertTrue(moved,
                            "Codex did not produce physical MinePilot displacement");
                    helper.assertTrue(visibleAiChatSeen,
                            "Codex never sent visible MinePilot chat");
                    verifiedStopTick = helper.getTick();
                    verifiedStopNanos = System.nanoTime();
                }
            } else {
                stopCandidateTick = -1L;
                stopCandidatePosition = null;
            }
            helper.assertTrue(System.nanoTime() - startedNanos <= TIMEOUT_NANOS,
                    "Codex did not move MinePilot to TestHuman within eight minutes; start="
                            + start + ", current=" + current + ", target=" + destination
                            + ", health=" + body.getHealth()
                            + ", inventoryUnchanged="
                            + inventoryEquals(startingInventory, body));
        }

        private void captureCodexMcpTrace() {
            if (codexTraceVerified) {
                return;
            }
            List<ExternalToolCall> calls = runtime.externalToolCallsSince(externalTraceStart);
            int observe = indexOf(calls, "observe", null, null, 0);
            if (observe < 0) {
                return;
            }
            int request = indexOf(calls, "request_navigation", null, null, observe + 1);
            if (request < 0) {
                return;
            }
            String requestId = calls.get(request).navigationRequestId();
            helper.assertTrue(requestId != null,
                    "Codex request_navigation did not produce a request id");
            int say = indexOf(calls, "say", requestId, null, request + 1);
            if (say < 0) {
                return;
            }
            int plan = indexOf(calls, "plan_navigation", requestId, null, say + 1);
            if (plan < 0) {
                return;
            }
            int choose = indexOf(calls, "choose_navigation", requestId, null, plan + 1);
            if (choose < 0) {
                return;
            }
            int completedStatus = indexOf(
                    calls, "navigation_status", requestId, "COMPLETED", choose + 1);
            if (completedStatus < 0) {
                return;
            }

            ExternalToolCall terminal = calls.get(completedStatus);
            helper.assertTrue(human.player().getUUID().toString().equals(
                            terminal.targetIdentity()),
                    "COMPLETED status is not bound to TestHuman: " + terminal);
            helper.assertTrue(terminal.destinationX() != null
                            && terminal.destinationY() != null
                            && terminal.destinationZ() != null,
                    "COMPLETED status lacks a resolved destination: " + terminal);
            Vec3 tracedDestination = new Vec3(
                    terminal.destinationX(), terminal.destinationY(), terminal.destinationZ());
            Vec3 tracedBody = new Vec3(
                    terminal.bodyX(), terminal.bodyY(), terminal.bodyZ());
            helper.assertTrue(tracedDestination.distanceTo(destination) <= 0.01,
                    "COMPLETED status names the wrong destination: expected="
                            + destination + ", traced=" + tracedDestination);
            helper.assertTrue(tracedBody.distanceTo(tracedDestination) <= ARRIVAL_RADIUS,
                    "COMPLETED status was emitted while the body was outside the arrival radius: "
                            + terminal);
            codexTraceVerified = true;
        }

        private static int indexOf(
                List<ExternalToolCall> calls,
                String tool,
                String requestId,
                String outcome,
                int fromIndex
        ) {
            for (int index = Math.max(0, fromIndex); index < calls.size(); index++) {
                ExternalToolCall call = calls.get(index);
                if (tool.equals(call.tool())
                        && (requestId == null
                        || requestId.equals(call.navigationRequestId()))
                        && (outcome == null || outcome.equals(call.outcome()))) {
                    return index;
                }
            }
            return -1;
        }

        private void prepareFlatArena(BlockPos start, BlockPos end) {
            int minX = Math.min(start.getX(), end.getX()) - 5;
            int maxX = Math.max(start.getX(), end.getX()) + 5;
            int minZ = Math.min(start.getZ(), end.getZ()) - 5;
            int maxZ = Math.max(start.getZ(), end.getZ()) + 5;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    helper.getLevel().setBlockAndUpdate(
                            new BlockPos(x, start.getY() - 1, z),
                            Blocks.STONE.defaultBlockState());
                    for (int y = start.getY(); y <= start.getY() + 2; y++) {
                        helper.getLevel().setBlockAndUpdate(
                                new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }

        @Override
        public void close() {
            if (human != null) {
                human.close();
                human = null;
            }
        }
    }

    private static List<ItemStack> copyInventory(ServerPlayer player) {
        List<ItemStack> result = new ArrayList<>(player.getInventory().getContainerSize());
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            result.add(player.getInventory().getItem(slot).copy());
        }
        return List.copyOf(result);
    }

    private static boolean inventoryEquals(List<ItemStack> expected, ServerPlayer player) {
        if (expected.size() != player.getInventory().getContainerSize()) {
            return false;
        }
        for (int slot = 0; slot < expected.size(); slot++) {
            if (!ItemStack.matches(expected.get(slot), player.getInventory().getItem(slot))) {
                return false;
            }
        }
        return true;
    }

    /** A PlayerList-backed chat actor with an in-memory network connection. */
    private static final class BackendHuman implements AutoCloseable {
        private final Connection connection;
        private final EmbeddedChannel channel;
        private final ServerGamePacketListenerImpl listener;
        private final ServerPlayer player;
        private final List<String> systemChat = new ArrayList<>();
        private boolean closed;

        private BackendHuman(
                Connection connection,
                EmbeddedChannel channel,
                ServerGamePacketListenerImpl listener,
                ServerPlayer player
        ) {
            this.connection = connection;
            this.channel = channel;
            this.listener = listener;
            this.player = player;
        }

        private static BackendHuman create(GameTestHelper helper, Vec3 position) {
            GameProfile profile = new GameProfile(UUID.randomUUID(), "TestHuman");
            CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
            ServerPlayer player = new ServerPlayer(
                    helper.getLevel().getServer(), helper.getLevel(), profile,
                    cookie.clientInformation());
            player.setPos(position.x(), position.y(), position.z());
            Connection connection = new Connection(PacketFlow.SERVERBOUND);
            EmbeddedChannel channel = new EmbeddedChannel(connection);
            helper.getLevel().getServer().getPlayerList()
                    .placeNewPlayer(connection, player, cookie);
            player.setGameMode(GameType.SURVIVAL);
            ServerGamePacketListenerImpl listener = player.connection;
            if (listener == null) {
                throw new IllegalStateException("Player login did not install a game listener");
            }
            listener.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            BackendHuman result = new BackendHuman(connection, channel, listener, player);
            result.pumpPackets();
            return result;
        }

        private ServerPlayer player() {
            return player;
        }

        private void tick() {
            if (closed || !connection.isConnected()) {
                return;
            }
            connection.tick();
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            channel.flushOutbound();
            pumpPackets();
        }

        private List<String> drainSystemChat() {
            List<String> result = List.copyOf(systemChat);
            systemChat.clear();
            return result;
        }

        private void pumpPackets() {
            Object packet;
            while ((packet = channel.readOutbound()) != null) {
                try {
                    if (packet instanceof ClientboundKeepAlivePacket keepAlive) {
                        listener.handleKeepAlive(new ServerboundKeepAlivePacket(keepAlive.getId()));
                    } else if (packet instanceof ClientboundPlayerPositionPacket position) {
                        listener.handleAcceptTeleportPacket(
                                new ServerboundAcceptTeleportationPacket(position.id()));
                    } else if (packet instanceof ClientboundChunkBatchFinishedPacket) {
                        listener.handleChunkBatchReceived(
                                new ServerboundChunkBatchReceivedPacket(3.5F));
                    } else if (packet instanceof ClientboundSystemChatPacket chat) {
                        systemChat.add(chat.content().getString());
                    }
                } finally {
                    ReferenceCountUtil.release(packet);
                }
            }
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (connection.isConnected()) {
                connection.disconnect(Component.literal("Backend navigation test complete"));
            }
            connection.handleDisconnection();
            channel.finishAndReleaseAll();
        }
    }
}
