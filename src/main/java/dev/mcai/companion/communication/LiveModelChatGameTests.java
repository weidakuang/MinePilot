package dev.mcai.companion.communication;

import com.mojang.authlib.GameProfile;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mcai.companion.control.BehaviorArbiter;
import dev.mcai.companion.control.GoalSnapshot;
import dev.mcai.companion.control.GoalSource;
import dev.mcai.companion.control.GoalStatus;
import dev.mcai.companion.embodiment.AiPlayerManager;
import dev.mcai.companion.embodiment.GameTestCompanionSpawn;
import dev.mcai.companion.embodiment.SessionState;
import dev.mcai.companion.memory.MemoryEvent;
import dev.mcai.companion.memory.transport.VerifiedPortalEdge;
import dev.mcai.companion.mcp.MinecraftMcpBackend;
import dev.mcai.companion.mixin.WorldGenSettingsAccessor;
import dev.mcai.companion.model.CapabilityProbeOutcome;
import dev.mcai.companion.model.GatewayStatus;
import dev.mcai.companion.model.ModelGateway;
import dev.mcai.companion.model.ModelOutcome;
import dev.mcai.companion.model.ObservationKind;
import dev.mcai.companion.model.PlannerInput;
import dev.mcai.companion.model.RequestedObservation;
import dev.mcai.companion.navigation.GridPos;
import dev.mcai.companion.navigation.LocalNavSnapshot;
import dev.mcai.companion.perception.BlockCoordinate;
import dev.mcai.companion.perception.PerceptionVec3;
import dev.mcai.companion.progression.ServerFoundationEvidenceVerifier;
import dev.mcai.companion.progression.ServerShelterEvidenceVerifier;
import dev.mcai.companion.progression.SurvivalMilestone;
import dev.mcai.companion.runtime.CompanionRuntime;
import dev.mcai.companion.runtime.ServerRuntime;
import dev.mcai.companion.security.CompanionCommandAccess;
import dev.mcai.companion.skill.SkillSupervisor;
import dev.mcai.companion.skills.core.EmergencySurvivalController;
import dev.mcai.companion.skills.building.DynamicShelterPlanner;
import dev.mcai.companion.skills.loot.SecureEnderPearlReserveSkill;
import dev.mcai.companion.skills.portal.ObservedEndPortalGeometry;
import dev.mcai.companion.skills.portal.PortalKind;
import dev.mcai.companion.skills.portal.PortalSkills;
import dev.mcai.companion.skills.portal.PortalTraversalResult;
import dev.mcai.companion.skills.stronghold.EyeTraceHistorySnapshot;
import dev.mcai.companion.skills.stronghold.EyeTraceSnapshot;
import dev.mcai.companion.skills.stronghold.StrongholdSkills;
import dev.mcai.companion.skills.stronghold
        .TriangulateStrongholdSearchAreaSkill;
import dev.mcai.companion.waypoint.DimensionRef;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.stats.Stats;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.tags.StructureTags;
import net.minecraftforge.common.ForgeHooks;

/**
 * Opt-in, development-only test that spends real model tokens. It enters
 * through Forge's official post-packet chat hook and runs without a client
 * renderer or launcher.
 */
public final class LiveModelChatGameTests {
    private static final int BODY_TIMEOUT_TICKS = 3_000;
    private static final long MODEL_TIMEOUT_NANOS =
            java.time.Duration.ofSeconds(120).toNanos();
    private static final long FOUNDATION_TOOLKIT_TIMEOUT_NANOS =
            java.time.Duration.ofMinutes(6).toNanos();

    private LiveModelChatGameTests() {
    }

    /**
     * One GameTest server owns one verified model runtime. The first live
     * scenario performs a real capability handshake; later scenarios reuse
     * that exact installed capability profile instead of repeatedly probing
     * the same provider and manufacturing a rate-limit burst.
     */
    private static CompletableFuture<CapabilityProbeOutcome>
            probeOrReuseVerifiedModel(final ServerRuntime runtime) {
        final var setup = runtime.model().snapshot();
        if (setup.gatewayReady()
                && setup.capabilities().isPresent()) {
            return CompletableFuture.completedFuture(
                    new CapabilityProbeOutcome.Supported(
                            setup.capabilities().orElseThrow(),
                            1
                    )
            );
        }
        return runtime.model()
                .prepareConfiguredProfile()
                .toCompletableFuture();
    }

    /**
     * A physical GameTest can reach its asserted outcome one server tick
     * before the model emits COMPLETE_GOAL. End only that test's active goal
     * during cleanup so the next exclusive live scenario cannot inherit a
     * planner skill or suppress its idle equipment controller.
     */
    private static void finishScenarioGoal(
            final ServerRuntime runtime
    ) {
        final GoalStatus status = runtime.goals()
                .snapshot()
                .status();
        if (status == GoalStatus.RUNNING
                || status == GoalStatus.CANCEL_PENDING) {
            runtime.goals().markTerminal(
                    GoalStatus.SAFE_IDLE,
                    "live_test_cleanup"
            );
        }
    }

    /**
     * Test-only boundary for the shared GameTest server. A failed live
     * scenario may leave an active headless body, a leased action, or an
     * emergency lane behind even after its normal cleanup callback runs.
     * Clear that state before the next scenario asks for a body; production
     * startup and respawn never call this helper.
     */
    private static void resetIsolatedScenario(
            final ServerRuntime runtime
    ) {
        GameTestCompanionSpawn.resetForIsolatedFixture(runtime.server());
    }

    /**
     * Distinguishes a gameplay-planner response from the separate
     * conversation request. Both deliberately share the same single model
     * and usage event type, but only {@code brain-*} requests prove that the
     * high-level gameplay loop actually observed the installed task.
     */
    private static boolean isGameplayPlannerUsage(
            final MemoryEvent event,
            final Instant commandAt,
            final long goalRevision
    ) {
        if (event.occurredAt().isBefore(commandAt)
                || event.goalRevision() != goalRevision) {
            return false;
        }
        try {
            final JsonObject payload = JsonParser.parseString(
                    event.payloadJson()
            ).getAsJsonObject();
            final String requestId =
                    payload.get("requestId").getAsString();
            return requestId.startsWith(
                    "brain-" + goalRevision + "-"
            );
        } catch (RuntimeException malformedAuditEvent) {
            return false;
        }
    }

    /**
     * Verifies the production login lifecycle without a renderer or model:
     * a real PlayerList login must create the companion beside that player,
     * publish an explicit AI TAB name, and leave the body motionless while
     * no verified gateway exists.
     */
    public static void autoPresenceOnHumanLogin(
            final GameTestHelper helper
    ) {
        final ServerRuntime runtime = CompanionRuntime.active()
                .filter(candidate ->
                        candidate.server()
                            == helper.getLevel().getServer())
                .orElseThrow(() -> new IllegalStateException(
                        "Companion runtime is unavailable"
                ));
        final AutoPresenceScenario scenario =
                new AutoPresenceScenario(helper, runtime);
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Covers the production-only ordering that ordinary auto-presence tests
     * miss: the dedicated server has no human for longer than the short
     * unanchored admission grace, the AI becomes ACTIVE at the saved/world
     * spawn, and only then does the first human log in.  The companion must
     * reconcile that initial placement through a normal remove/relogin (not a
     * gameplay teleport) while preserving its UUID, idle goal and inventory.
     */
    public static void delayedHumanLoginAfterZeroHumanActive(
            final GameTestHelper helper
    ) {
        final ServerRuntime runtime = CompanionRuntime.active()
                .filter(candidate -> candidate.server()
                        == helper.getLevel().getServer())
                .orElseThrow(() -> new IllegalStateException(
                        "Companion runtime is unavailable"
                ));
        final DelayedHumanLoginScenario scenario =
                new DelayedHumanLoginScenario(helper, runtime, false);
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Regression for the dangerous ordering where the first human joins while
     * the unanchored body is already in its local emergency lane.  The login
     * must record a deferred anchor and leave that exact ServerPlayer alive;
     * the normal remove/relogin is allowed only after the emergency clears.
     */
    public static void delayedHumanLoginWhileEmergencyActive(
            final GameTestHelper helper
    ) {
        final ServerRuntime runtime = CompanionRuntime.active()
                .filter(candidate -> candidate.server()
                        == helper.getLevel().getServer())
                .orElseThrow(() -> new IllegalStateException(
                        "Companion runtime is unavailable"
                ));
        final DelayedHumanLoginScenario scenario =
                new DelayedHumanLoginScenario(helper, runtime, true);
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    public static void realPlayerChatToLiveModel(
            final GameTestHelper helper
    ) {
        if (!Boolean.getBoolean("mcai.liveModelTest")) {
            helper.succeed();
            return;
        }
        final ServerRuntime runtime = CompanionRuntime.active()
                .filter(candidate ->
                        candidate.server()
                            == helper.getLevel().getServer())
                .orElseThrow(() -> new IllegalStateException(
                        "Companion runtime is unavailable"
                ));
        final LiveScenario scenario =
                new LiveScenario(helper, runtime);
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Exercises the complete task path: a Forge chat submission from an
     * authorized mock player, live-model task classification, a second
     * live-model gameplay decision, and ordinary ServerPlayer movement.
     */
    public static void realPlayerTaskToLiveModelMovement(
            final GameTestHelper helper
    ) {
        if (!Boolean.getBoolean("mcai.liveModelTest")) {
            helper.succeed();
            return;
        }
        final ServerRuntime runtime = CompanionRuntime.active()
                .filter(candidate ->
                        candidate.server()
                            == helper.getLevel().getServer())
                .orElseThrow(() -> new IllegalStateException(
                        "Companion runtime is unavailable"
                ));
        final LiveMovementScenario scenario =
                new LiveMovementScenario(
                        helper,
                        runtime
                );
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Exercises the field-facing follow contract rather than a fixed
     * coordinate walk. One ordinary, unaddressed chat message must make the
     * live model bind {@code follow_entity}; the human then walks farther
     * along a collision-checked course while the companion continuously
     * follows through ordinary player movement.
     */
    public static void realPlayerTaskToLiveModelFollow(
            final GameTestHelper helper
    ) {
        if (!Boolean.getBoolean("mcai.liveModelTest")) {
            helper.succeed();
            return;
        }
        final ServerRuntime runtime = CompanionRuntime.active()
                .filter(candidate ->
                        candidate.server()
                            == helper.getLevel().getServer())
                .orElseThrow(() -> new IllegalStateException(
                        "Companion runtime is unavailable"
                ));
        final LiveFollowScenario scenario =
                new LiveFollowScenario(helper, runtime, true);
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Inner-loop physical integration for the trusted immediate-follow lane.
     * A real PlayerList-backed test player submits ordinary Forge chat and
     * the production body must acquire {@code follow_entity} and walk the
     * moving course without a model round trip.  This is deliberately not a
     * formal real-client/model gate; the external Actor/Observer gate remains
     * authoritative for that claim.
     */
    public static void realPlayerChatToImmediateBoundFollow(
            final GameTestHelper helper
    ) {
        final ServerRuntime runtime = CompanionRuntime.active()
                .filter(candidate ->
                        candidate.server()
                            == helper.getLevel().getServer())
                .orElseThrow(() -> new IllegalStateException(
                        "Companion runtime is unavailable"
                ));
        final LiveFollowScenario scenario =
                new LiveFollowScenario(helper, runtime, false);
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Starts from one fairly visible ordinary dropped stack. A logged-in
     * player asks through normal chat, the configured live model must bind
     * {@code collect_observed_item}, and the body must walk to the exact
     * observed entity and acquire it through vanilla pickup.
     */
    public static void realPlayerTaskToLiveModelItemCollection(
            final GameTestHelper helper
    ) {
        if (!Boolean.getBoolean("mcai.liveModelTest")) {
            helper.succeed();
            return;
        }
        final ServerRuntime runtime = CompanionRuntime.active()
                .filter(candidate ->
                        candidate.server()
                            == helper.getLevel().getServer())
                .orElseThrow(() -> new IllegalStateException(
                        "Companion runtime is unavailable"
                ));
        final LiveItemCollectionScenario scenario =
                new LiveItemCollectionScenario(helper, runtime);
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Starts with an unopened visible chest containing ordinary fixture
     * materials. A logged-in player asks through normal chat, then leaves.
     * The configured model must first open that chest through
     * {@code use_block}, bind the resulting semantic menu, and transfer the
     * requested exact count through vanilla menu clicks.
     */
    public static void realPlayerTaskToLiveModelContainerWithdrawal(
            final GameTestHelper helper
    ) {
        if (!Boolean.getBoolean("mcai.liveModelTest")) {
            helper.succeed();
            return;
        }
        final ServerRuntime runtime = CompanionRuntime.active()
                .filter(candidate ->
                        candidate.server()
                            == helper.getLevel().getServer())
                .orElseThrow(() -> new IllegalStateException(
                        "Companion runtime is unavailable"
                ));
        final LiveContainerWithdrawalScenario scenario =
                new LiveContainerWithdrawalScenario(helper, runtime);
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Exercises inventory upkeep and combat through the same real-chat/live
     * model entry used by a player. Test setup only supplies owned equipment
     * and a vanilla Zombie; all equipping, task selection and attacks remain
     * ordinary companion actions.
     */
    public static void realPlayerTaskToLiveModelZombieDefense(
            final GameTestHelper helper
    ) {
        if (!Boolean.getBoolean("mcai.liveModelTest")) {
            helper.succeed();
            return;
        }
        final ServerRuntime runtime = CompanionRuntime.active()
                .filter(candidate ->
                        candidate.server()
                            == helper.getLevel().getServer())
                .orElseThrow(() -> new IllegalStateException(
                        "Companion runtime is unavailable"
                ));
        final LiveCombatScenario scenario =
                new LiveCombatScenario(helper, runtime);
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Reproduces the field failure where a hostile approaches from outside
     * the current view and the companion merely stares until death.  The
     * command still enters through ordinary player chat and the live model,
     * but first contact must trigger a physical reaction before a provider
     * round trip can finish.
     */
    public static void realPlayerChatToSurpriseZombieDefense(
            final GameTestHelper helper
    ) {
        if (!Boolean.getBoolean("mcai.liveModelTest")) {
            helper.succeed();
            return;
        }
        final ServerRuntime runtime = CompanionRuntime.active()
                .filter(candidate ->
                        candidate.server()
                            == helper.getLevel().getServer())
                .orElseThrow(() -> new IllegalStateException(
                        "Companion runtime is unavailable"
                ));
        final SurpriseZombieScenario scenario =
                new SurpriseZombieScenario(helper, runtime);
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Reproduces the real low-health gift conversation. A logged-in player
     * drops a normal golden apple, the companion acquires that item through
     * vanilla pickup, and the player says only "给你了，快吃吧". Success
     * requires an actual item-use transaction and a live-model response for
     * the installed goal; a spoken promise is not an outcome.
     */
    public static void realPlayerChatToCriticalGoldenApple(
            final GameTestHelper helper
    ) {
        if (!Boolean.getBoolean("mcai.liveModelTest")) {
            helper.succeed();
            return;
        }
        final ServerRuntime runtime = CompanionRuntime.active()
                .filter(candidate ->
                        candidate.server()
                            == helper.getLevel().getServer())
                .orElseThrow(() -> new IllegalStateException(
                        "Companion runtime is unavailable"
                ));
        final CriticalGoldenAppleScenario scenario =
                new CriticalGoldenAppleScenario(helper, runtime);
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Sends a natural player request to the live model, then requires the
     * selected production parkour skill to clear three real one-block gaps
     * using ordinary sprint-jumps.
     */
    public static void realPlayerTaskToLiveModelParkour(
            final GameTestHelper helper
    ) {
        if (!Boolean.getBoolean("mcai.liveModelTest")) {
            helper.succeed();
            return;
        }
        final ServerRuntime runtime = CompanionRuntime.active()
                .filter(candidate ->
                        candidate.server()
                            == helper.getLevel().getServer())
                .orElseThrow(() -> new IllegalStateException(
                        "Companion runtime is unavailable"
                ));
        final LiveParkourScenario scenario =
                new LiveParkourScenario(helper, runtime);
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Enters through ordinary player chat and a live model, then creates a
     * real twelve-block fall. The online model owns the high-level task while
     * the production 20 TPS emergency controller must equip the owned bucket
     * and perform the time-critical vanilla water placement without waiting
     * for another network response.
     */
    public static void realPlayerTaskToLiveModelWaterClutch(
            final GameTestHelper helper
    ) {
        if (!Boolean.getBoolean("mcai.liveModelTest")) {
            helper.succeed();
            return;
        }
        final ServerRuntime runtime = CompanionRuntime.active()
                .filter(candidate ->
                        candidate.server()
                            == helper.getLevel().getServer())
                .orElseThrow(() -> new IllegalStateException(
                        "Companion runtime is unavailable"
                ));
        final LiveWaterClutchScenario scenario =
                new LiveWaterClutchScenario(helper, runtime);
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Sends the player's ordinary multi-step farm request through the live
     * model and requires three independent production farming skills to
     * harvest mature wheat and restore every plot through vanilla actions.
     */
    public static void realPlayerTaskToLiveModelFarmWork(
            final GameTestHelper helper
    ) {
        if (!Boolean.getBoolean("mcai.liveModelTest")) {
            helper.succeed();
            return;
        }
        final ServerRuntime runtime = CompanionRuntime.active()
                .filter(candidate ->
                        candidate.server()
                            == helper.getLevel().getServer())
                .orElseThrow(() -> new IllegalStateException(
                        "Companion runtime is unavailable"
                ));
        final LiveFarmScenario scenario =
                new LiveFarmScenario(helper, runtime);
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Starts the actual M1 route from an empty inventory through ordinary
     * player chat. The fixture supplies only visible vanilla terrain and an
     * exposed connected oak-log cluster; the model must select the production
     * gatherer, while the body performs normal mining and pickup.
     */
    public static void realPlayerTaskToLiveModelFoundationBootstrap(
            final GameTestHelper helper
    ) {
        if (!Boolean.getBoolean("mcai.liveModelTest")) {
            helper.succeed();
            return;
        }
        final ServerRuntime runtime = CompanionRuntime.active()
                .filter(candidate ->
                        candidate.server()
                            == helper.getLevel().getServer())
                .orElseThrow(() -> new IllegalStateException(
                        "Companion runtime is unavailable"
                ));
        final LiveFoundationBootstrapScenario scenario =
                new LiveFoundationBootstrapScenario(helper, runtime);
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Covers the first unverified M2 handoff after the iron toolkit. A real
     * player submits one completion-route chat request and leaves. The live
     * model must bind the ordinary portal builder, that builder must consume
     * fourteen owned obsidian and flint-and-steel durability, and the same
     * survival body must then enter the resulting vanilla portal without a
     * second command or teleport.
     */
    public static void
            realPlayerTaskToLiveModelNetherPortalBuildAndEntry(
                    final GameTestHelper helper
            ) {
        if (!Boolean.getBoolean("mcai.liveModelTest")) {
            helper.succeed();
            return;
        }
        final ServerRuntime runtime = CompanionRuntime.active()
                .filter(candidate ->
                        candidate.server()
                            == helper.getLevel().getServer())
                .orElseThrow(() -> new IllegalStateException(
                        "Companion runtime is unavailable"
                ));
        final LiveNetherPortalScenario scenario =
                new LiveNetherPortalScenario(helper, runtime);
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Closes the next completion-route handoff after verified Nether entry.
     * A real player submits one ordinary completion message and leaves. The
     * configured model must select the parameterless durable Blaze-reserve
     * controller, which then performs repeated vanilla combat and pickups
     * until the server-authoritative fourteen-unit route threshold is met.
     */
    public static void
            realPlayerTaskToLiveModelNetherBlazeMaterial(
                    final GameTestHelper helper
            ) {
        if (!Boolean.getBoolean("mcai.liveModelTest")) {
            helper.succeed();
            return;
        }
        final ServerRuntime runtime = CompanionRuntime.active()
                .filter(candidate ->
                        candidate.server()
                            == helper.getLevel().getServer())
                .orElseThrow(() -> new IllegalStateException(
                        "Companion runtime is unavailable"
                ));
        final LiveNetherBlazeScenario scenario =
                new LiveNetherBlazeScenario(helper, runtime);
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Closes the completion-route handoff after Blaze material. A real
     * player submits one ordinary completion message and leaves. The live
     * model must select the durable Ender-pearl reserve controller, which
     * physically builds its safety roof before repeated vanilla combat and
     * pickup cycles reach the fourteen-unit route threshold.
     */
    public static void
            realPlayerTaskToLiveModelEnderPearlReserve(
                    final GameTestHelper helper
            ) {
        if (!Boolean.getBoolean("mcai.liveModelTest")) {
            helper.succeed();
            return;
        }
        final ServerRuntime runtime = CompanionRuntime.active()
                .filter(candidate ->
                        candidate.server()
                            == helper.getLevel().getServer())
                .orElseThrow(() -> new IllegalStateException(
                        "Companion runtime is unavailable"
                ));
        final LiveEnderPearlScenario scenario =
                new LiveEnderPearlScenario(helper, runtime);
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Proves the live high-level handoff for the End-portal activation
     * compound. A real player submits one natural chat request, then leaves.
     * The configured model must select the parameterless production skill;
     * the local controller may resolve the ring center only from the
     * headless player's current first-person frame evidence.
     */
    public static void realPlayerTaskToLiveModelEndPortalActivation(
            final GameTestHelper helper
    ) {
        if (!Boolean.getBoolean("mcai.liveModelTest")) {
            helper.succeed();
            return;
        }
        final ServerRuntime runtime = CompanionRuntime.active()
                .filter(candidate ->
                        candidate.server()
                            == helper.getLevel().getServer())
                .orElseThrow(() -> new IllegalStateException(
                        "Companion runtime is unavailable"
                ));
        final LiveEndPortalActivationScenario scenario =
                new LiveEndPortalActivationScenario(
                        helper,
                        runtime,
                        false,
                        false
                );
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Proves the missing handoff after activation. One ordinary player chat
     * request asks for both operations and the player leaves. The configured
     * model must first select the parameterless activation compound, then
     * make a second gameplay decision that starts the parameterless local
     * portal finder. The same survival body must consume the Eyes and cross
     * the resulting portal without a teleport or a second human command.
     */
    public static void
            realPlayerTaskToLiveModelEndPortalActivationAndEntry(
                    final GameTestHelper helper
            ) {
        if (!Boolean.getBoolean("mcai.liveModelTest")) {
            helper.succeed();
            return;
        }
        final ServerRuntime runtime = CompanionRuntime.active()
                .filter(candidate ->
                        candidate.server()
                            == helper.getLevel().getServer())
                .orElseThrow(() -> new IllegalStateE