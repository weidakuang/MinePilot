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
                .orElseThrow(() -> new IllegalStateException(
                        "Companion runtime is unavailable"
                ));
        final LiveEndPortalActivationScenario scenario =
                new LiveEndPortalActivationScenario(
                        helper,
                        runtime,
                        true,
                        false
                );
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Proves the previously missing continuous completion handoff under one
     * ordinary chat goal. Before that command, the fixture establishes an
     * already-completed Nether resource stage by physically traversing a
     * valid portal and naturally picking up the owned crafting ingredients.
     * After the human leaves, no fixture mutation is permitted: the
     * configured model must select ordinary recipe crafting, the
     * parameterless verified-portal return, stronghold triangulation, and
     * fair stronghold approach/excavation compounds with the same survival
     * body.
     */
    public static void
            realPlayerTaskToLiveModelEyeCraftReturnAndStronghold(
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
        final LiveEyeCraftReturnStrongholdScenario scenario =
                new LiveEyeCraftReturnStrongholdScenario(
                        helper,
                        runtime,
                        false
                );
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Extends the controlled Nether-material route through every remaining
     * irreversible completion handoff under one ordinary chat goal. The
     * configured model must compose Eyes, return through its verified portal,
     * triangulate and reach a stronghold, search an occluded portal room,
     * activate and enter the End portal, defeat the dragon, and physically
     * return with the same survival body.
     */
    public static void
            realPlayerTaskToLiveModelNetherMaterialsToVictory(
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
        final LiveEyeCraftReturnStrongholdScenario scenario =
                new LiveEyeCraftReturnStrongholdScenario(
                        helper,
                        runtime,
                        true
                );
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Closes the next completion-route handoff after fair stronghold
     * discovery. Before the command, the fixture creates an opaque
     * stronghold corridor with one dead branch and a hidden portal room.
     * After one ordinary player chat and disconnect, the configured model
     * must select portal-room search, activation, and entry in order. The
     * same survival body must physically explore and backtrack, consume its
     * Eyes through vanilla interactions, activate all nine portal cells, and
     * enter the End. No fixture mutation occurs after the route checkpoint
     * is installed.
     */
    public static void
            realPlayerTaskToLiveModelStrongholdPortalRoomAndEntry(
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
        final LiveStrongholdPortalRoomScenario scenario =
                new LiveStrongholdPortalRoomScenario(
                        helper,
                        runtime,
                        false
                );
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Extends the stronghold-interior gate across the two remaining
     * irreversible completion phases under the same ordinary chat goal. The
     * companion must preserve its UUID and goal revision through portal-room
     * search, activation, End entry, credited dragon combat, and physical
     * return to the Overworld.
     */
    public static void
            realPlayerTaskToLiveModelStrongholdPortalRoomToVictory(
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
        final LiveStrongholdPortalRoomScenario scenario =
                new LiveStrongholdPortalRoomScenario(
                        helper,
                        runtime,
                        true
                );
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Starts from a previously verified late-game route checkpoint and then
     * proves the four irreversible handoffs under one ordinary player chat
     * goal: activate the observed portal, enter the End, defeat the dragon,
     * and physically return. The checkpoint represents work completed before
     * this test; no model skill is selected or world outcome manufactured
     * after the command.
     */
    public static void
            realPlayerTaskToLiveModelLateEndCompletionChain(
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
                        true,
                        true
                );
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Proves the two late irreversible completion phases with the configured
     * model. Test setup first moves the body through a real End portal and
     * prepares a deterministic full-health dragon arena. Only then does a
     * real player submit one chat goal and leave. The model must select the
     * parameterless dragon compound, then bind and enter the currently
     * visible return portal with the same survival body.
     */
    public static void realPlayerTaskToLiveModelEndVictoryAndReturn(
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
        final LiveEndVictoryScenario scenario =
                new LiveEndVictoryScenario(helper, runtime);
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Isolates the field failure where previously placed workstations occupy
     * every shelter footprint around the companion. A real player supplies
     * one natural chat request to the configured live model. Test setup owns
     * only the flat terrain, ordinary owned materials, and the already
     * existing workstation cluster; success still requires normal walking,
     * first-person surveying, vanilla block placement, and server-verified
     * shelter evidence.
     *
     * <p>This is a seeded fault-reproduction gate, not proof of the complete
     * empty-inventory M1 route.</p>
     */
    public static void realPlayerTaskToLiveModelShelterRelocation(
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
        final LiveShelterRelocationScenario scenario =
                new LiveShelterRelocationScenario(helper, runtime);
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Runs the same live-model foundation transaction on a dedicated server
     * that has no human player at any point in the test. The single initial
     * goal enters through the production MCP backend, which is the supported
     * unattended-server control path. All autonomous work then happens 640
     * blocks from the GameTest origin under the headless player's ordinary
     * PLAYER_SIMULATION ticket.
     */
    public static void zeroHumanDedicatedServerToLiveModelFoundation(
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
                new LiveFoundationBootstrapScenario(
                        helper,
                        runtime,
                        true
                );
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    private static final class LiveScenario {
        private final GameTestHelper helper;
        private final ServerRuntime runtime;
        private final long createdAt;
        private final Instant auditNotBefore;

        private PlacedHuman humanSession;
        private Stage stage = Stage.BODY;
        private CompletableFuture<Optional<MemoryEvent>> speechRead;
        private GoalSnapshot goalBefore;
        private long stageStartedNanos;

        private LiveScenario(
                final GameTestHelper helper,
                final ServerRuntime runtime
        ) {
            this.helper = helper;
            this.runtime = runtime;
            createdAt = helper.getTick();
            stageStartedNanos = System.nanoTime();
            auditNotBefore = Instant.now();
        }

        private void start() {
            finishScenarioGoal(runtime);
            resetIsolatedScenario(runtime);
            final var status = AiPlayerManager.status(runtime.server());
            if (status.state() != SessionState.ABSENT) {
                AiPlayerManager.requestRemove(runtime.server());
            }
            helper.assertTrue(
                    AiPlayerManager.status(runtime.server()).state()
                            == SessionState.ABSENT,
                    "Live-chat fixture could not begin absent"
            );
            final BlockPos loginFeet =
                    helper.absolutePos(new BlockPos(12, 2, 12));
            for (int x = -9; x <= 9; x++) {
                for (int z = -9; z <= 9; z++) {
                    helper.getLevel().setBlockAndUpdate(
                            loginFeet.offset(x, -1, z),
                            Blocks.STONE.defaultBlockState()
                    );
                }
            }
            humanSession = PlacedHuman.create(
                    helper,
                    runtime,
                    Vec3.atBottomCenterOf(loginFeet)
            );
            goalBefore = runtime.goals().snapshot();
            /*
             * Submit through Forge chat immediately after the real login.
             * The startup Keychain probe is intentionally not awaited here:
             * production must retain this utterance until the saved model is
             * restored instead of dropping it or demanding the key again.
             */
            final Component submitted =
                    ForgeHooks.onServerChatSubmittedEvent(
                            humanSession.player(),
                            Component.literal(
                                "Could you speak Chinese?"
                            )
                    );
            helper.assertTrue(
                    submitted != null,
                    "Companion cancelled ordinary player chat"
            );
        }

        private void tick() {
            humanSession.tick();
            switch (stage) {
                case BODY -> waitForBody();
                case PROBE -> waitForStartupRestore();
                case SPEECH -> waitForSpeech();
                case DONE -> {
                    // GameTest is already terminal.
                }
            }
        }

        private void waitForBody() {
            final var status =
                    AiPlayerManager.status(runtime.server());
            helper.assertTrue(
                    status.state() != SessionState.FAILED,
                    "Live-chat companion body failed"
            );
            if (status.state() != SessionState.ACTIVE
                    || !status.online()) {
                helper.assertTrue(
                        helper.getTick() - createdAt
                            <= BODY_TIMEOUT_TICKS,
                        "Live-chat companion body timed out"
                );
                return;
            }
            final ServerPlayer body = AiPlayerManager
                    .onlinePlayer(runtime.server())
                    .orElseThrow();
            helper.assertTrue(
                    body.level() == humanSession.player().level()
                        && body.distanceToSqr(humanSession.player())
                            <= 12.0D * 12.0D,
                    "Startup-restored companion did not auto-spawn "
                        + "beside the logged-in player"
            );
            helper.assertTrue(
                    body.getTabListDisplayName() != null
                        && body.getTabListDisplayName()
                            .getString()
                            .startsWith("[AI] "),
                    "Startup-restored companion is absent from TAB"
            );
            stage = Stage.PROBE;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForStartupRestore() {
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= MODEL_TIMEOUT_NANOS,
                    "Saved model did not restore automatically: "
                        + startupModelDiagnostic()
            );
            final var setup = runtime.model().snapshot();
            if (!setup.gatewayReady()) {
                return;
            }
            stage = Stage.SPEECH;
            stageStartedNanos = System.nanoTime();
        }

        private String startupModelDiagnostic() {
            final var setup = runtime.model().snapshot();
            return "endpointConfigured=" + setup.endpointConfigured()
                    + ", credentialAvailable="
                    + setup.credentialAvailable()
                    + ", probeInFlight=" + setup.probeInFlight()
                    + ", gatewayReady=" + setup.gatewayReady()
                    + ", configurationErrorCode="
                    + setup.configurationErrorCode();
        }

        private void waitForSpeech() {
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= MODEL_TIMEOUT_NANOS,
                    "Live model conversation timed out"
            );
            if (speechRead == null) {
                speechRead = runtime.memory().latestEvent(
                        "brain_speech"
                );
                return;
            }
            if (!speechRead.isDone()) {
                return;
            }
            final Optional<MemoryEvent> found = speechRead.join();
            speechRead = null;
            if (found.isEmpty()
                    || found.orElseThrow().occurredAt()
                        .isBefore(auditNotBefore)) {
                return;
            }
            final String reply = JsonParser.parseString(
                    found.orElseThrow().payloadJson()
                ).getAsJsonObject()
                .get("message")
                .getAsString();
            helper.assertTrue(
                    containsHanCharacter(reply),
                    "Model did not answer the language request in "
                        + "Chinese: " + reply
            );
            final GoalSnapshot after = runtime.goals().snapshot();
            helper.assertTrue(
                    after.revision() == goalBefore.revision()
                        && after.goal().equals(goalBefore.goal()),
                    "Casual conversation was incorrectly promoted "
                        + "to a gameplay goal"
            );
            stage = Stage.DONE;
            helper.succeed();
        }

        private void cleanup() {
            finishScenarioGoal(runtime);
            if (humanSession != null) {
                humanSession.close();
            }
            if (AiPlayerManager.status(runtime.server()).state()
                    != SessionState.ABSENT) {
                AiPlayerManager.requestRemove(runtime.server());
            }
        }

        private static boolean containsHanCharacter(
                final String text
        ) {
            return text.codePoints().anyMatch(codePoint ->
                    Character.UnicodeScript.of(codePoint)
                        == Character.UnicodeScript.HAN
            );
        }
    }

    private enum Stage {
        BODY,
        PROBE,
        SPEECH,
        DONE
    }

    private static final class AutoPresenceScenario {
        private static final double MAXIMUM_LOGIN_DISTANCE = 12.0D;
        private static final double MAXIMUM_IDLE_DRIFT_SQUARED =
                0.01D;
        private static final int STABLE_TICKS_REQUIRED = 10;
        private static final int IDLE_AUDIT_TICKS = 40;

        private final GameTestHelper helper;
        private final ServerRuntime runtime;
        private final boolean offlineAudit;
        private PlacedHuman humanSession;
        private PresenceStage stage = PresenceStage.LOGIN;
        private Vec3 settlingPosition;
        private Vec3 stablePosition;
        private int stableTicks;
        private int auditTicks;

        private AutoPresenceScenario(
                final GameTestHelper helper,
                final ServerRuntime runtime
        ) {
            this.helper = helper;
            this.runtime = runtime;
            this.offlineAudit =
                    !Boolean.getBoolean("mcai.liveModelTest");
        }

        private void start() {
            finishScenarioGoal(runtime);
            resetIsolatedScenario(runtime);
            if (offlineAudit) {
                /*
                 * Other release-excluded integrated fixtures may install a
                 * holding gateway into the shared GameTest runtime. Remove
                 * that test delegate so this scenario represents the real
                 * no-credential/no-verified-model state. The live-model
                 * suite deliberately keeps its startup-restored gateway.
                 */
                runtime.model().gateway().clearVerifiedDelegate();
            }
            final var status =
                    AiPlayerManager.status(runtime.server());
            if (status.state() != SessionState.ABSENT) {
                AiPlayerManager.requestRemove(runtime.server());
            }
            helper.assertTrue(
                    AiPlayerManager.status(runtime.server()).state()
                            == SessionState.ABSENT,
                    "Auto-presence fixture could not begin absent"
            );
            final BlockPos loginFeet =
                    helper.absolutePos(new BlockPos(12, 2, 12));
            for (int x = -9; x <= 9; x++) {
                for (int z = -9; z <= 9; z++) {
                    helper.getLevel().setBlockAndUpdate(
                            loginFeet.offset(x, -1, z),
                            Blocks.STONE.defaultBlockState()
                    );
                }
            }
            humanSession = PlacedHuman.create(
                    helper,
                    runtime,
                    Vec3.atBottomCenterOf(loginFeet)
            );
        }

        private void tick() {
            humanSession.tick();
            switch (stage) {
                case LOGIN -> waitForAutomaticBody();
                case SETTLE -> waitForStableBody();
                case AUDIT -> auditIdleBody();
                case DONE -> {
                    // GameTest is already terminal.
                }
            }
        }

        private void waitForAutomaticBody() {
            final var status =
                    AiPlayerManager.status(runtime.server());
            helper.assertTrue(
                    status.state() != SessionState.FAILED,
                    "Automatic companion login failed: " + status
            );
            if (status.state() != SessionState.ACTIVE
                    || !status.online()) {
                return;
            }
            final ServerPlayer body = AiPlayerManager
                    .onlinePlayer(runtime.server())
                    .orElseThrow();
            helper.assertTrue(
                    runtime.server().getPlayerList().getPlayer(
                            runtime.worldData().companionUuid()
                    ) == body,
                    "Automatic body is absent from the authoritative "
                        + "PlayerList"
            );
            helper.assertTrue(
                    body.getTabListDisplayName() != null
                        && body.getTabListDisplayName()
                            .getString()
                            .startsWith("[AI] "),
                    "TAB entry does not disclose the online AI identity"
            );
            helper.assertTrue(
                    body.level() == humanSession.player().level(),
                    "Automatic body spawned in another dimension"
            );
            helper.assertTrue(
                    body.distanceToSqr(humanSession.player())
                        <= MAXIMUM_LOGIN_DISTANCE
                            * MAXIMUM_LOGIN_DISTANCE,
                    "Automatic body did not spawn beside the player: "
                        + "distance="
                        + Math.sqrt(body.distanceToSqr(
                                humanSession.player()
                        ))
            );
            if (offlineAudit) {
                helper.assertTrue(
                        !runtime.model().snapshot().gatewayReady(),
                        "Offline presence test unexpectedly has a model "
                            + "gateway"
                );
            } else {
                stage = PresenceStage.DONE;
                helper.succeed();
                return;
            }
            stage = PresenceStage.SETTLE;
        }

        private void waitForStableBody() {
            final ServerPlayer body = AiPlayerManager
                    .onlinePlayer(runtime.server())
                    .orElseThrow();
            final Vec3 current = body.position();
            if (settlingPosition == null
                    || current.distanceToSqr(settlingPosition)
                        > MAXIMUM_IDLE_DRIFT_SQUARED) {
                settlingPosition = current;
                stableTicks = 0;
                return;
            }
            stableTicks++;
            if (stableTicks < STABLE_TICKS_REQUIRED) {
                return;
            }
            stablePosition = current;
            auditTicks = 0;
            stage = PresenceStage.AUDIT;
        }

        private void auditIdleBody() {
            final ServerPlayer body = AiPlayerManager
                    .onlinePlayer(runtime.server())
                    .orElseThrow();
            auditTicks++;
            helper.assertTrue(
                    body.position().distanceToSqr(stablePosition)
                        <= MAXIMUM_IDLE_DRIFT_SQUARED,
                    "Companion authored movement without a verified "
                        + "model gateway"
            );
            if (auditTicks < IDLE_AUDIT_TICKS) {
                return;
            }
            stage = PresenceStage.DONE;
            helper.succeed();
        }

        private void cleanup() {
            finishScenarioGoal(runtime);
            if (humanSession != null) {
                humanSession.close();
            }
            if (AiPlayerManager.status(runtime.server()).state()
                    != SessionState.ABSENT) {
                AiPlayerManager.requestRemove(runtime.server());
            }
        }
    }

    private enum PresenceStage {
        LOGIN,
        SETTLE,
        AUDIT,
        DONE
    }

    private enum DelayedHumanLoginStage {
        WAITING_FOR_UNANCHORED_BODY,
        WAITING_FOR_EMERGENCY,
        WAITING_FOR_REANCHOR,
        DONE
    }

    private static final class DelayedHumanLoginScenario {
        private static final int MAX_WAIT_TICKS = 1_200;
        private static final double MAXIMUM_LOGIN_DISTANCE = 12.0D;

        private final GameTestHelper helper;
        private final ServerRuntime runtime;
        private final boolean emergencyAtLogin;
        private DelayedHumanLoginStage stage =
                DelayedHumanLoginStage.WAITING_FOR_UNANCHORED_BODY;
        private PlacedHuman humanSession;
        private UUID originalBodyUuid;
        private ServerPlayer originalBody;
        private Mob emergencyAttacker;
        private Vec3 loginPosition;
        private boolean loginDeferredObserved;
        private long originalGoalRevision;
        private int age;

        private DelayedHumanLoginScenario(
                final GameTestHelper helper,
                final ServerRuntime runtime,
                final boolean emergencyAtLogin
        ) {
            this.helper = helper;
            this.runtime = runtime;
            this.emergencyAtLogin = emergencyAtLogin;
        }

        private void start() {
            helper.assertTrue(
                    Boolean.getBoolean("mcai.zeroHumanAutoSpawnTest"),
                    "Delayed first-login gate requires the production "
                        + "zero-human auto-spawn property"
            );
            assertOnlyAiIsOnline();
        }

        private void tick() {
            age++;
            if (humanSession != null) {
                humanSession.tick();
            }
            helper.assertTrue(
                    age <= MAX_WAIT_TICKS,
                    "Delayed first-login anchor scenario timed out at stage "
                        + stage
            );
            switch (stage) {
                case WAITING_FOR_UNANCHORED_BODY -> waitForUnanchoredBody();
                case WAITING_FOR_EMERGENCY -> waitForEmergency();
                case WAITING_FOR_REANCHOR -> waitForReanchoredBody();
                case DONE -> {
                    // GameTest is already terminal.
                }
            }
        }

        private void waitForUnanchoredBody() {
            assertOnlyAiIsOnline();
            final var status = AiPlayerManager.status(runtime.server());
            helper.assertTrue(
                    status.state() != SessionState.FAILED,
                    "Zero-human body failed before first login: " + status
            );
            if (status.state() != SessionState.ACTIVE || !status.online()) {
                return;
            }
            final ServerPlayer body = AiPlayerManager.onlinePlayer(
                    runtime.server()
            ).orElseThrow();
            helper.assertTrue(
                    runtime.worldData().bodyNeedsInitialAnchor(),
                    "Zero-human body did not retain its unanchored startup "
                        + "provenance"
            );
            helper.assertTrue(
                    runtime.server().getPlayerList().getPlayers().size() == 1,
                    "Active zero-human stage did not contain exactly one AI"
            );
            originalBody = body;
            originalBodyUuid = body.getUUID();
            originalGoalRevision = runtime.goals().snapshot().revision();
            final BlockPos loginFeet = helper.absolutePos(
                    new BlockPos(32, 2, 32)
            );
            for (int x = -4; x <= 4; x++) {
                for (int z = -4; z <= 4; z++) {
                    helper.getLevel().setBlockAndUpdate(
                            loginFeet.offset(x, -1, z),
                            Blocks.SMOOTH_STONE.defaultBlockState()
                    );
                    for (int y = 0; y <= 2; y++) {
                        helper.getLevel().setBlockAndUpdate(
                                loginFeet.offset(x, y, z),
                                Blocks.AIR.defaultBlockState()
                        );
                    }
                }
            }
            loginPosition = Vec3.atBottomCenterOf(loginFeet);
            if (!emergencyAtLogin) {
                createHumanAtLoginPosition();
                stage = DelayedHumanLoginStage.WAITING_FOR_REANCHOR;
                return;
            }

            emergencyAttacker = EntityTypes.ZOMBIE.create(
                    helper.getLevel(),
                    EntitySpawnReason.COMMAND
            );
            helper.assertTrue(
                    emergencyAttacker != null,
                    "Could not create the hostile for the deferred-anchor gate"
            );
            emergencyAttacker.setPos(
                    body.getX() + 2.0D,
                    body.getY(),
                    body.getZ() + 0.5D
            );
            emergencyAttacker.setNoAi(true);
            emergencyAttacker.setPersistenceRequired();
            emergencyAttacker.setTarget(body);
            helper.assertTrue(
                    helper.getLevel().addFreshEntity(emergencyAttacker),
                    "Could not add the hostile for the deferred-anchor gate"
            );
            body.setGameMode(GameType.SURVIVAL);
            body.getAbilities().invulnerable = false;
            body.setInvulnerable(false);
            body.invulnerableTime = 0;
            body.setHealth(body.getMaxHealth());
            helper.assertTrue(
                    body.hurtServer(
                            helper.getLevel(),
                            body.damageSources().mobAttack(
                                    emergencyAttacker
                            ),
                            2.0F
                    ),
                    "Controlled hostile damage did not enter the emergency "
                        + "anchor gate"
            );
            body.setInvulnerable(true);
            final var reaction = runtime.survival().tick(false, false);
            helper.assertTrue(
                    reaction.intervened()
                        && reaction.state()
                            != EmergencySurvivalController.State.CLEAR,
                    "Emergency lane did not claim the hostile before login: "
                        + reaction
            );
            stage = DelayedHumanLoginStage.WAITING_FOR_EMERGENCY;
        }

        private void waitForEmergency() {
            final ServerPlayer body = AiPlayerManager.onlinePlayer(
                    runtime.server()
            ).orElseThrow();
            helper.assertTrue(
                    originalBody == body,
                    "Emergency anchor gate replaced the body before human "
                        + "login"
            );
            if (runtime.survival().state()
                    == EmergencySurvivalController.State.CLEAR) {
                helper.assertTrue(
                        age <= MAX_WAIT_TICKS - 120,
                        "Emergency lane cleared before the deferred login "
                            + "could be exercised"
                );
                final var reaction = runtime.survival().tick(false, false);
                if (reaction.state()
                        == EmergencySurvivalController.State.CLEAR) {
                    return;
                }
            }
            createHumanAtLoginPosition();
            helper.assertTrue(
                    AiPlayerManager.onlinePlayer(runtime.server())
                            .orElseThrow() == originalBody,
                    "First human login removed an emergency body instead of "
                        + "deferring the initial anchor"
            );
            loginDeferredObserved = true;
            /*
             * The danger has now been observed at the exact login boundary.
             * Resolve this controlled threat so the production retry can be
             * observed without making the test depend on combat eventually
             * defeating an invulnerable fixture mob.
             */
            if (emergencyAttacker != null
                    && !emergencyAttacker.isRemoved()) {
                emergencyAttacker.discard();
            }
            stage = DelayedHumanLoginStage.WAITING_FOR_REANCHOR;
        }

        private void createHumanAtLoginPosition() {
            humanSession = PlacedHuman.create(
                    helper,
                    runtime,
                    loginPosition
            );
        }

        private void waitForReanchoredBody() {
            final var status = AiPlayerManager.status(runtime.server());
            helper.assertTrue(
                    status.state() != SessionState.FAILED,
                    "First-login body re-anchor failed: " + status
            );
            if (status.state() != SessionState.ACTIVE || !status.online()) {
                return;
            }
            final ServerPlayer body = AiPlayerManager.onlinePlayer(
                    runtime.server()
            ).orElseThrow();
            if (emergencyAtLogin) {
                helper.assertTrue(
                        loginDeferredObserved,
                        "Emergency login never observed a deferred initial "
                            + "anchor"
                );
                if (!runtime.worldData().bodySpawnAnchored()) {
                    helper.assertTrue(
                            body == originalBody,
                            "Emergency retry removed the body before the "
                                + "danger cleared"
                    );
                    return;
                }
            }
            helper.assertTrue(
                    runtime.worldData().bodySpawnAnchored(),
                    "First human login did not claim the initial anchor"
            );
            helper.assertTrue(
                    originalBodyUuid.equals(body.getUUID()),
                    "Re-login changed the stable companion UUID"
            );
            helper.assertTrue(
                    originalGoalRevision
                            == runtime.goals().snapshot().revision(),
                    "Initial anchor reconciliation changed the idle goal"
            );
            helper.assertTrue(
                    runtime.goals().snapshot().status() == GoalStatus.IDLE,
                    "Initial anchor reconciliation created a gameplay goal"
            );
            helper.assertTrue(
                    body.getInventory().isEmpty(),
                    "Initial anchor reconciliation changed the body inventory"
            );
            helper.assertTrue(
                    body.distanceToSqr(humanSession.player())
                            <= MAXIMUM_LOGIN_DISTANCE
                                * MAXIMUM_LOGIN_DISTANCE,
                    "Re-anchored body is not beside the first human: "
                        + Math.sqrt(body.distanceToSqr(
                                humanSession.player()
                        ))
            );
            helper.assertTrue(
                    body.getTabListDisplayName() != null
                        && body.getTabListDisplayName().getString()
                            .startsWith("[AI] "),
                    "Re-anchored body lost its disclosed AI TAB identity"
            );
            helper.assertTrue(
                    runtime.server().getPlayerList().getPlayers().size() == 2,
                    "First-login gate did not retain exactly one AI and one human"
            );
            stage = DelayedHumanLoginStage.DONE;
            helper.succeed();
        }

        private void assertOnlyAiIsOnline() {
            helper.assertTrue(
                    runtime.server().getPlayerList().getPlayers().stream()
                            .allMatch(player ->
                                    dev.mcai.companion.skin.AiProfileMarker
                                            .isMarked(player.getGameProfile())),
                    "Zero-human stage contains a non-AI player"
            );
        }

        private void cleanup() {
            finishScenarioGoal(runtime);
            if (emergencyAttacker != null
                    && !emergencyAttacker.isRemoved()) {
                emergencyAttacker.discard();
            }
            runtime.survival().reset();
            if (humanSession != null) {
                humanSession.close();
            }
            if (AiPlayerManager.status(runtime.server()).state()
                    != SessionState.ABSENT) {
                AiPlayerManager.requestRemove(runtime.server());
            }
        }
    }

    private static final class LiveMovementScenario {
        private static final double MINIMUM_REAL_MOVEMENT = 2.0D;
        private static final double ARRIVAL_RADIUS = 2.25D;

        private final GameTestHelper helper;
        private final ServerRuntime runtime;
        private final long createdAt;

        private PlacedHuman humanSession;
        private ServerPlayer human;
        private MovementStage stage = MovementStage.BODY;
        private CompletableFuture<CapabilityProbeOutcome> probe;
        private long stageStartedNanos;
        private long goalRevisionBefore;
        private double startX;
        private double startZ;
        private double targetX;
        private double targetY;
        private double targetZ;

        private LiveMovementScenario(
                final GameTestHelper helper,
                final ServerRuntime runtime
        ) {
            this.helper = helper;
            this.runtime = runtime;
            this.createdAt = helper.getTick();
            this.stageStartedNanos = System.nanoTime();
        }

        private void start() {
            final var status = AiPlayerManager.status(runtime.server());
            if (status.state() == SessionState.ABSENT) {
                helper.assertTrue(
                        AiPlayerManager.requestSpawn(
                                runtime.server()
                        ).accepted(),
                        "Live-task companion spawn was rejected"
                );
            }
        }

        private void tick() {
            if (humanSession != null) {
                humanSession.tick();
            }
            switch (stage) {
                case BODY -> waitForBody();
                case PROBE -> waitForProbe();
                case GOAL -> waitForGoal();
                case MOVEMENT -> waitForMovement();
                case DONE -> {
                    // GameTest is already terminal.
                }
            }
        }

        private void waitForBody() {
            final var status =
                    AiPlayerManager.status(runtime.server());
            helper.assertTrue(
                    status.state() != SessionState.FAILED,
                    "Live-task companion body failed"
            );
            if (status.state() != SessionState.ACTIVE
                    || !status.online()) {
                helper.assertTrue(
                        helper.getTick() - createdAt
                            <= BODY_TIMEOUT_TICKS,
                        "Live-task companion body timed out"
                );
                return;
            }
            final ServerPlayer body = AiPlayerManager
                    .onlinePlayer(runtime.server())
                    .orElseThrow();
            prepareStraightSafeCourse(body);
            probe = probeOrReuseVerifiedModel(runtime);
            stage = MovementStage.PROBE;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForProbe() {
            assertWithinModelDeadline(
                    "Live task model capability probe timed out"
            );
            if (!probe.isDone()) {
                return;
            }
            final CapabilityProbeOutcome outcome = probe.join();
            helper.assertTrue(
                    outcome
                        instanceof CapabilityProbeOutcome.Supported,
                    "Configured live model probe failed: " + outcome
            );
            humanSession = PlacedHuman.create(helper, runtime);
            human = humanSession.player();
            helper.assertTrue(
                    CompanionCommandAccess.mayAdmin(
                            human.createCommandSourceStack()
                    ),
                    "Logged-in test player did not gain task-write "
                        + "permission: isOp="
                        + runtime.server().getPlayerList().isOp(
                                new NameAndId(human.getGameProfile())
                        )
                        + ", playerPermission="
                        + human.permissions().hasPermission(
                                Permissions.COMMANDS_GAMEMASTER
                        )
                        + ", sourcePermission="
                        + human.createCommandSourceStack()
                            .permissions()
                            .hasPermission(
                                Permissions.COMMANDS_GAMEMASTER
                            )
            );
            goalRevisionBefore = runtime.goals()
                    .snapshot()
                    .revision();
            final String command = """
                    %s，请走到坐标 %.1f %.1f %.1f，正常步行，不要传送。
                    """.formatted(
                            runtime.worldData().displayName(),
                            targetX,
                            targetY,
                            targetZ
                    ).strip();
            final Component submitted =
                    ForgeHooks.onServerChatSubmittedEvent(
                            human,
                            Component.literal(command)
                    );
            helper.assertTrue(
                    submitted != null,
                    "Companion cancelled the movement chat command"
            );
            humanSession.close();
            humanSession = null;
            stage = MovementStage.GOAL;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForGoal() {
            assertNoHumanPlayersDuringAutonomy();
            assertWithinModelDeadline(
                    "Live model did not classify the movement task"
            );
            final GoalSnapshot goal = runtime.goals().snapshot();
            if (goal.revision() == goalRevisionBefore) {
                return;
            }
            helper.assertTrue(
                    goal.revision() > goalRevisionBefore,
                    "Movement task did not advance goal revision"
            );
            helper.assertTrue(
                    goal.goal().contains("坐标"),
                    "Authorized chat task was not preserved as the goal: "
                        + goal.goal()
            );
            stage = MovementStage.MOVEMENT;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForMovement() {
            assertNoHumanPlayersDuringAutonomy();
            /*
             * The first human login may still be completing the ordinary
             * initial-anchor remove/relogin transaction.  During that
             * bounded server-thread window the authoritative PlayerList has
             * no AI entry; treating it as a movement failure both hides the
             * real model result and makes the live gate flaky.  Wait for the
             * same UUID to be online again instead of teleporting or creating
             * a substitute entity.
             */
            final Optional<ServerPlayer> bodyCandidate = AiPlayerManager
                    .onlinePlayer(runtime.server());
            if (bodyCandidate.isEmpty()) {
                assertWithinModelDeadline(
                        "Companion body did not return after initial-anchor "
                            + "relogin"
                );
                return;
            }
            final ServerPlayer body = bodyCandidate.orElseThrow();
            final double moved = Math.hypot(
                    body.getX() - startX,
                    body.getZ() - startZ
            );
            final double remaining = Math.sqrt(
                    Math.pow(body.getX() - targetX, 2.0D)
                        + Math.pow(body.getY() - targetY, 2.0D)
                        + Math.pow(body.getZ() - targetZ, 2.0D)
            );
            if (remaining <= ARRIVAL_RADIUS) {
                helper.assertTrue(
                        moved >= MINIMUM_REAL_MOVEMENT,
                        "Body reached the target without material vanilla "
                            + "movement: " + moved
                );
                stage = MovementStage.DONE;
                helper.succeed();
                return;
            }
            if (System.nanoTime() - stageStartedNanos
                    <= MODEL_TIMEOUT_NANOS) {
                return;
            }
            helper.assertTrue(
                    false,
                    moved < MINIMUM_REAL_MOVEMENT
                        ? "Live model task produced no material player "
                            + "movement: " + moved
                        : "Body moved but did not reach the commanded point "
                            + "before the wall-clock deadline: remaining="
                            + remaining + ", moved=" + moved
            );
        }

        private void assertNoHumanPlayersDuringAutonomy() {
            final long humanPlayers = runtime.server()
                    .getPlayerList()
                    .getPlayers()
                    .stream()
                    .filter(player -> !player.getUUID().equals(
                            runtime.worldData().companionUuid()
                    ))
                    .count();
            helper.assertTrue(
                    humanPlayers == 0L,
                    "Live autonomous movement observed "
                        + humanPlayers + " human player(s) after the "
                        + "initial chat command"
            );
        }

        private void prepareStraightSafeCourse(
                final ServerPlayer body
        ) {
            startX = body.getX();
            startZ = body.getZ();
            final BlockPos start = body.blockPosition();
            final int floorY = start.getY() - 1;
            for (int dx = -2; dx <= 10; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    final BlockPos floor = new BlockPos(
                            start.getX() + dx,
                            floorY,
                            start.getZ() + dz
                    );
                    helper.getLevel().setBlockAndUpdate(
                            floor,
                            Blocks.SMOOTH_STONE.defaultBlockState()
                    );
                    for (int dy = 1; dy <= 3; dy++) {
                        helper.getLevel().setBlockAndUpdate(
                                floor.above(dy),
                                Blocks.AIR.defaultBlockState()
                        );
                    }
                }
            }
            targetX = start.getX() + 7.5D;
            targetY = start.getY();
            targetZ = start.getZ() + 0.5D;
        }

        private void assertWithinModelDeadline(
                final String message
        ) {
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= MODEL_TIMEOUT_NANOS,
                    message
            );
        }

        private void cleanup() {
            finishScenarioGoal(runtime);
            if (humanSession != null) {
                humanSession.close();
            }
            if (AiPlayerManager.status(runtime.server()).state()
                    != SessionState.ABSENT) {
                AiPlayerManager.requestRemove(runtime.server());
            }
        }
    }

    private enum MovementStage {
        BODY,
        PROBE,
        GOAL,
        MOVEMENT,
        DONE
    }

    private static final class LiveFollowScenario {
        private static final double INITIAL_LEAD = 6.0D;
        private static final double SECOND_LEG = 6.0D;
        private static final double HUMAN_STEP = 0.075D;
        private static final double FIRST_ARRIVAL = 3.35D;
        private static final double FINAL_ARRIVAL = 3.75D;
        private static final double MINIMUM_BODY_PATH = 7.0D;
        private static final double MAXIMUM_BODY_TICK_STEP = 0.9D;
        private static final long PHYSICAL_FOLLOW_TIMEOUT_NANOS =
                java.time.Duration.ofSeconds(20).toNanos();

        private final GameTestHelper helper;
        private final ServerRuntime runtime;
        private final long createdAt;
        private final boolean requireModelProbe;

        private FollowStage stage = FollowStage.BODY;
        private CompletableFuture<CapabilityProbeOutcome> probe;
        private PlacedHuman humanSession;
        private ServerPlayer human;
        private long stageStartedNanos;
        private long goalRevisionBefore;
        private long followGoalRevision;
        private Vec3 bodyStart;
        private Vec3 previousBodyPosition;
        private double bodyPath;
        private double maximumBodyTickStep;
        private double humanSecondLegStartX;
        private boolean secondLegStarted;
        private boolean sawActiveSkillArbiter;
        private boolean continuationNudgeSent;
        private HoldingModelGateway holdingGateway;
        private boolean followCourseRepositioned;

        private LiveFollowScenario(
                final GameTestHelper helper,
                final ServerRuntime runtime,
                final boolean requireModelProbe
        ) {
            this.helper = helper;
            this.runtime = runtime;
            this.requireModelProbe = requireModelProbe;
            this.createdAt = helper.getTick();
            this.stageStartedNanos = System.nanoTime();
        }

        private void start() {
            final var status = AiPlayerManager.status(runtime.server());
            if (status.state() == SessionState.ABSENT) {
                helper.assertTrue(
                        AiPlayerManager.requestSpawn(
                                runtime.server()
                        ).accepted(),
                        "Live-follow companion spawn was rejected"
                );
            }
        }

        private void tick() {
            if (holdingGateway != null) {
                helper.assertTrue(
                        holdingGateway.requestCount() == 0,
                        "Immediate follow unexpectedly used the holding "
        …43007 tokens truncated…           return;
            }
            humanSession = PlacedHuman.create(
                    helper,
                    runtime,
                    new Vec3(
                            autonomousWorkCenter.getX() + 0.5,
                            autonomousWorkCenter.getY() + 1.0,
                            autonomousWorkCenter.getZ() - 3.5
                    )
            );
            final ServerPlayer human = humanSession.player();
            helper.assertTrue(
                    CompanionCommandAccess.mayAdmin(
                            human.createCommandSourceStack()
                    ),
                    "Logged-in foundation test player lacked task-write "
                        + "permission"
            );
            final Component submitted =
                    ForgeHooks.onServerChatSubmittedEvent(
                            human,
                            Component.literal(
                                runtime.worldData().displayName()
                                    + "，从空背包开始建立安全据点并生存"
                                    + "到第二天。先把你眼前这组相连的"
                                    + "橡木原木全部砍下并捡进背包，"
                                    + "然后继续基础生存；不要使用命令。"
                            )
                    );
            helper.assertTrue(
                    submitted != null,
                    "Companion cancelled the foundation chat command"
            );
            stage = FoundationBootstrapStage.GOAL;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForGoal() {
            assertWithinModelDeadline(
                    "Live model did not classify the foundation task"
            );
            final GoalSnapshot goal = runtime.goals().snapshot();
            if (goal.revision() == goalRevisionBefore) {
                return;
            }
            helper.assertTrue(
                    goal.revision() > goalRevisionBefore
                        && goal.goal().contains("安全据点")
                        && goal.goal().contains("第二天"),
                    (zeroHumanFromStart
                            ? "Unattended MCP goal was not preserved "
                            : "Authorized foundation chat was not preserved ")
                        + "as the M1 goal"
            );
            if (zeroHumanFromStart) {
                helper.assertTrue(
                        goal.source() == GoalSource.MCP,
                        "Unattended goal bypassed the production MCP source"
                );
            }
            if (humanSession != null) {
                humanSession.close();
                humanSession = null;
            }
            foundationGoalRevision = goal.revision();
            stage = FoundationBootstrapStage.GATHER;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForGathering() {
            assertNoHumanPlayersDuringAutonomy();
            final Optional<ServerPlayer> bodyCandidate = AiPlayerManager
                    .onlinePlayer(runtime.server());
            if (bodyCandidate.isEmpty()) {
                return;
            }
            final ServerPlayer body = bodyCandidate.get();
            helper.assertTrue(
                    body.isAlive(),
                    "Companion died during foundation wood gathering"
            );
            final var skill = runtime.skillSupervisor().snapshot();
            if ("gather_visible_block_cluster".equals(
                    skill.skillName()
            )) {
                sawClusterGatherer = true;
            }
            final int ownedLogs = body.getInventory()
                    .countItem(Items.OAK_LOG);
            final int pickedUpLogs = body.getStats().getValue(
                    Stats.ITEM_PICKED_UP.get(Items.OAK_LOG)
            ) - initialPickedUpLogs;
            final int minedLogs = body.getStats().getValue(
                    Stats.BLOCK_MINED.get(Blocks.OAK_LOG)
            ) - initialMinedLogs;
            final boolean milestone = runtime.worldData()
                    .verifiedRouteProgress(foundationGoalRevision)
                    .milestones()
                    .contains(SurvivalMilestone.WOOD_OBTAINED);
            if (pickedUpLogs >= REQUIRED_LOGS
                    && minedLogs >= REQUIRED_LOGS
                    && milestone) {
                helper.assertTrue(
                        sawClusterGatherer,
                        "M1 bootstrap used repeated model micro-actions "
                            + "instead of the bounded production gatherer"
                );
                helper.assertTrue(
                        minedLogs >= REQUIRED_LOGS,
                        "M1 bootstrap did not record five vanilla log "
                            + "mining actions"
                );
                stage = FoundationBootstrapStage.BASIC_CRAFTING;
                stageStartedNanos = System.nanoTime();
                return;
            }
            final GoalSnapshot goal = runtime.goals().snapshot();
            if (goal.status() != GoalStatus.RUNNING
                    && goal.status() != GoalStatus.CANCEL_PENDING) {
                helper.assertTrue(
                        false,
                        "Foundation goal became terminal before the "
                            + "wood milestone: " + goal + ", logs="
                            + ownedLogs + ", pickedUp=" + pickedUpLogs
                            + ", blocks=" + logSummary()
                            + ", skill=" + skill.skillName()
                            + ", rejection="
                            + skill.lastStartRejection()
                            + ", "
                            + basicCraftingDiagnostic(body)
                );
            }
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= MODEL_TIMEOUT_NANOS,
                    "Live foundation bootstrap did not gather the "
                        + "visible logs: owned=" + ownedLogs
                        + ", pickedUp=" + pickedUpLogs
                        + ", mined=" + minedLogs
                        + ", blocks=" + logSummary()
                        + ", milestone=" + milestone
                        + ", skill=" + skill.skillName()
                        + ", rejection="
                        + skill.lastStartRejection()
            );
        }

        private void waitForBasicCrafting() {
            assertNoHumanPlayersDuringAutonomy();
            final Optional<ServerPlayer> bodyCandidate = AiPlayerManager
                    .onlinePlayer(runtime.server());
            if (bodyCandidate.isEmpty()) {
                return;
            }
            final ServerPlayer body = bodyCandidate.get();
            helper.assertTrue(
                    body.isAlive(),
                    "Companion died during foundation basic crafting"
            );
            final var skill = runtime.skillSupervisor().snapshot();
            if ("craft_recipe".equals(skill.skillName())) {
                sawCraftRecipe = true;
            }
            if ("prepare_basic_crafting".equals(
                    skill.skillName()
            )) {
                sawCraftRecipe = true;
            }
            final boolean tableCrafted = body.getStats().getValue(
                    Stats.ITEM_CRAFTED.get(Items.CRAFTING_TABLE)
            ) - initialCraftedTables >= 1;
            final boolean pickaxeCrafted = body.getStats().getValue(
                    Stats.ITEM_CRAFTED.get(Items.WOODEN_PICKAXE)
            ) - initialCraftedWoodenPickaxes >= 1;
            final boolean ownsBasicPickaxe =
                    body.getInventory().countItem(
                            Items.WOODEN_PICKAXE
                    ) > 0
                    || body.getInventory().countItem(
                            Items.STONE_PICKAXE
                    ) > 0
                    || body.getInventory().countItem(
                            Items.IRON_PICKAXE
                    ) > 0
                    || body.getInventory().countItem(
                            Items.GOLDEN_PICKAXE
                    ) > 0
                    || body.getInventory().countItem(
                            Items.DIAMOND_PICKAXE
                    ) > 0
                    || body.getInventory().countItem(
                            Items.NETHERITE_PICKAXE
                    ) > 0;
            final boolean tableAvailable =
                    body.getInventory().countItem(
                            Items.CRAFTING_TABLE
                    ) > 0
                    || runtime.worldData()
                        .verifiedFoundationEvidence(
                                foundationGoalRevision
                        )
                        .flatMap(evidence ->
                                evidence.craftingTable()
                        )
                        .isPresent();
            final boolean milestone = runtime.worldData()
                    .verifiedRouteProgress(foundationGoalRevision)
                    .milestones()
                    .contains(
                            SurvivalMilestone.BASIC_CRAFTING_READY
                    );
            if (tableCrafted
                    && pickaxeCrafted
                    && ownsBasicPickaxe
                    && tableAvailable
                    && milestone) {
                helper.assertTrue(
                        sawCraftRecipe,
                        "M1 basic crafting did not use the production "
                            + "recipe skill"
                );
                stage = FoundationBootstrapStage.STONE_GATHERING;
                stageStartedNanos = System.nanoTime();
                return;
            }
            final GoalSnapshot goal = runtime.goals().snapshot();
            if (goal.status() != GoalStatus.RUNNING
                    && goal.status() != GoalStatus.CANCEL_PENDING) {
                helper.assertTrue(
                        false,
                        "Foundation goal became terminal before basic "
                            + "crafting: " + goal
                            + ", tableCrafted=" + tableCrafted
                            + ", pickaxeCrafted=" + pickaxeCrafted
                            + ", ownsPickaxe=" + ownsBasicPickaxe
                            + ", tableAvailable=" + tableAvailable
                            + ", milestone=" + milestone
                            + ", inventory=" + body.getInventory()
                            + ", skill=" + skill.skillName()
                            + ", rejection="
                            + skill.lastStartRejection()
                );
            }
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= MODEL_TIMEOUT_NANOS,
                    "Live foundation bootstrap did not prepare basic "
                        + "crafting: tableCrafted=" + tableCrafted
                        + ", pickaxeCrafted=" + pickaxeCrafted
                        + ", ownsPickaxe=" + ownsBasicPickaxe
                        + ", tableAvailable=" + tableAvailable
                        + ", milestone=" + milestone
                        + ", inventory=" + body.getInventory()
                        + ", skill=" + skill.skillName()
                        + ", rejection="
                        + skill.lastStartRejection()
                        + ", "
                        + basicCraftingDiagnostic(body)
            );
        }

        private String basicCraftingDiagnostic(
                final ServerPlayer body
        ) {
            final BlockPos center = body.blockPosition();
            final List<BlockPos> nearbyTables =
                    BlockPos.betweenClosedStream(
                            center.offset(-10, -3, -10),
                            center.offset(10, 3, 10)
                    ).filter(pos ->
                            helper.getLevel().getBlockState(pos)
                                    .is(Blocks.CRAFTING_TABLE)
                    ).map(BlockPos::immutable).toList();
            return "bodyPosition=" + body.position()
                    + ", bodyBlock=" + center
                    + ", onGround=" + body.onGround()
                    + ", rotation=[" + body.getYRot()
                    + "," + body.getXRot() + "]"
                    + ", mainHand=" + body.getMainHandItem()
                    + ", nearbyTables=" + nearbyTables;
        }

        private void waitForStoneGathering() {
            assertNoHumanPlayersDuringAutonomy();
            final Optional<ServerPlayer> bodyCandidate = AiPlayerManager
                    .onlinePlayer(runtime.server());
            if (bodyCandidate.isEmpty()) {
                return;
            }
            final ServerPlayer body = bodyCandidate.get();
            helper.assertTrue(
                    body.isAlive(),
                    "Companion died during foundation stone gathering"
            );
            final var skill = runtime.skillSupervisor().snapshot();
            if ("gather_visible_block_cluster".equals(
                    skill.skillName()
            ) || "prepare_stone_tools".equals(
                    skill.skillName()
            )) {
                sawStoneGatherer = true;
            }
            final int ownedCobblestone = body.getInventory()
                    .countItem(Items.COBBLESTONE);
            final int pickedUpCobblestone = body.getStats().getValue(
                    Stats.ITEM_PICKED_UP.get(Items.COBBLESTONE)
            ) - initialPickedUpCobblestone;
            final int minedStone = body.getStats().getValue(
                    Stats.BLOCK_MINED.get(Blocks.STONE)
            ) - initialMinedStone;
            if (ownedCobblestone >= REQUIRED_COBBLESTONE
                    && pickedUpCobblestone >= REQUIRED_COBBLESTONE
                    && minedStone >= REQUIRED_COBBLESTONE) {
                helper.assertTrue(
                        sawStoneGatherer,
                        "M1 stone acquisition bypassed the bounded "
                            + "production gatherer"
                );
                stage = FoundationBootstrapStage.STONE_CRAFTING;
                stageStartedNanos = System.nanoTime();
                return;
            }
            final GoalSnapshot goal = runtime.goals().snapshot();
            helper.assertTrue(
                    goal.status() == GoalStatus.RUNNING
                        || goal.status() == GoalStatus.CANCEL_PENDING,
                    "Foundation goal became terminal before stone "
                        + "gathering: " + goal
                        + ", ownedCobblestone=" + ownedCobblestone
                        + ", pickedUp=" + pickedUpCobblestone
                        + ", minedStone=" + minedStone
                        + ", blocks=" + stoneSummary()
                        + ", skill=" + skill.skillName()
                        + ", rejection=" + skill.lastStartRejection()
            );
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= MODEL_TIMEOUT_NANOS,
                    "Live foundation route did not gather stone: "
                        + "ownedCobblestone=" + ownedCobblestone
                        + ", pickedUp=" + pickedUpCobblestone
                        + ", minedStone=" + minedStone
                        + ", blocks=" + stoneSummary()
                        + ", skill=" + skill.skillName()
                        + ", rejection=" + skill.lastStartRejection()
            );
        }

        private void waitForStoneCrafting() {
            assertNoHumanPlayersDuringAutonomy();
            final Optional<ServerPlayer> bodyCandidate = AiPlayerManager
                    .onlinePlayer(runtime.server());
            if (bodyCandidate.isEmpty()) {
                return;
            }
            final ServerPlayer body = bodyCandidate.get();
            helper.assertTrue(
                    body.isAlive(),
                    "Companion died during foundation stone crafting"
            );
            final var skill = runtime.skillSupervisor().snapshot();
            if ("craft_recipe".equals(skill.skillName())) {
                sawStoneCraftRecipe = true;
            }
            if ("prepare_stone_tools".equals(skill.skillName())) {
                sawStoneCraftRecipe = true;
            }
            final boolean crafted = body.getStats().getValue(
                    Stats.ITEM_CRAFTED.get(Items.STONE_PICKAXE)
            ) - initialCraftedStonePickaxes >= 1;
            final boolean ownsStonePickaxe =
                    body.getInventory().countItem(
                            Items.STONE_PICKAXE
                    ) > 0;
            final boolean milestone = runtime.worldData()
                    .verifiedRouteProgress(foundationGoalRevision)
                    .milestones()
                    .contains(
                            SurvivalMilestone.STONE_TOOL_OBTAINED
                    );
            if (crafted && ownsStonePickaxe && milestone) {
                helper.assertTrue(
                        sawStoneCraftRecipe,
                        "M1 stone pickaxe did not use the production "
                            + "recipe transaction"
                );
                stage = FoundationBootstrapStage.FOOD;
                stageStartedNanos = System.nanoTime();
                return;
            }
            final GoalSnapshot goal = runtime.goals().snapshot();
            helper.assertTrue(
                    goal.status() == GoalStatus.RUNNING
                        || goal.status() == GoalStatus.CANCEL_PENDING,
                    "Foundation goal became terminal before stone "
                        + "crafting: " + goal
                        + ", crafted=" + crafted
                        + ", ownsStonePickaxe=" + ownsStonePickaxe
                        + ", milestone=" + milestone
                        + ", skill=" + skill.skillName()
                        + ", rejection=" + skill.lastStartRejection()
            );
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= MODEL_TIMEOUT_NANOS,
                    "Live foundation route did not craft a stone pickaxe: "
                        + "crafted=" + crafted
                        + ", ownsStonePickaxe=" + ownsStonePickaxe
                        + ", milestone=" + milestone
                        + ", skill=" + skill.skillName()
                        + ", rejection=" + skill.lastStartRejection()
            );
        }

        private void waitForFood() {
            assertNoHumanPlayersDuringAutonomy();
            final Optional<ServerPlayer> bodyCandidate = AiPlayerManager
                    .onlinePlayer(runtime.server());
            if (bodyCandidate.isEmpty()) {
                return;
            }
            final ServerPlayer body = bodyCandidate.get();
            helper.assertTrue(
                    body.isAlive(),
                    "Companion died while securing M1 food"
            );
            final var skill = runtime.skillSupervisor().snapshot();
            if ("hunt_observed_food_animal".equals(
                    skill.skillName()
            ) || "secure_visible_food_reserve".equals(
                    skill.skillName()
            )) {
                sawFoodHunt = true;
            }
            final int safeFood = body.getInventory()
                    .countItem(Items.BEEF);
            final boolean milestone = runtime.worldData()
                    .verifiedRouteProgress(foundationGoalRevision)
                    .milestones()
                    .contains(SurvivalMilestone.FOOD_SECURED);
            if (safeFood >= 8 && milestone) {
                helper.assertTrue(
                        sawFoodHunt,
                        "M1 food reserve bypassed the production "
                            + "observed-animal hunt"
                );
                helper.assertTrue(
                        foodAnimals.stream()
                            .filter(Cow::isAlive)
                            .count() <= foodAnimals.size() - 3L,
                        "M1 food stage did not physically hunt enough "
                            + "of the observed animals"
                );
                stage = FoundationBootstrapStage.IRON_TOOLKIT;
                stageStartedNanos = System.nanoTime();
                return;
            }
            final GoalSnapshot goal = runtime.goals().snapshot();
            helper.assertTrue(
                    goal.status() == GoalStatus.RUNNING
                        || goal.status() == GoalStatus.CANCEL_PENDING,
                    "Foundation goal became terminal before food "
                        + "readiness: " + goal + ", beef="
                        + safeFood + ", milestone=" + milestone
                        + ", skill=" + skill.skillName()
                        + ", rejection="
                        + skill.lastStartRejection()
            );
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= FOUNDATION_TOOLKIT_TIMEOUT_NANOS,
                    "Live foundation route did not secure eight food: "
                        + "beef=" + safeFood
                        + ", livingAnimals="
                        + foodAnimals.stream()
                            .filter(Cow::isAlive)
                            .count()
                        + ", milestone=" + milestone
                        + ", skill=" + skill.skillName()
                        + ", rejection="
                        + skill.lastStartRejection()
            );
        }

        private void waitForIronToolkit() {
            assertNoHumanPlayersDuringAutonomy();
            final Optional<ServerPlayer> bodyCandidate = AiPlayerManager
                    .onlinePlayer(runtime.server());
            if (bodyCandidate.isEmpty()) {
                return;
            }
            final ServerPlayer body = bodyCandidate.get();
            helper.assertTrue(
                    body.isAlive(),
                    "Companion died while preparing the M1 iron toolkit"
            );
            final var skill = runtime.skillSupervisor().snapshot();
            if ("prepare_iron_toolkit".equals(
                    skill.skillName()
            )) {
                sawIronToolkit = true;
            }
            final boolean ownsIronPickaxe =
                    body.getInventory().countItem(
                            Items.IRON_PICKAXE
                    ) > 0;
            final boolean ownsBucket =
                    body.getInventory().countItem(Items.BUCKET) > 0
                    || body.getInventory().countItem(
                            Items.WATER_BUCKET
                    ) > 0
                    || body.getInventory().countItem(
                            Items.LAVA_BUCKET
                    ) > 0;
            final boolean ownsShield =
                    body.getInventory().countItem(Items.SHIELD) > 0;
            final boolean milestone = runtime.worldData()
                    .verifiedRouteProgress(foundationGoalRevision)
                    .milestones()
                    .contains(
                            SurvivalMilestone.IRON_TOOLKIT_OBTAINED
                    );
            final boolean furnaceVerified = runtime.worldData()
                    .verifiedFoundationEvidence(
                            foundationGoalRevision
                    )
                    .flatMap(evidence -> evidence.furnace())
                    .isPresent();
            if (ownsIronPickaxe
                    && ownsBucket
                    && ownsShield
                    && milestone
                    && furnaceVerified) {
                helper.assertTrue(
                        sawIronToolkit,
                        "M1 iron readiness bypassed the bounded "
                            + "production iron-toolkit skill"
                );
                final boolean minedCoalFuel =
                        body.getStats().getValue(
                                Stats.BLOCK_MINED.get(
                                        Blocks.COAL_ORE
                                )
                        ) - initialMinedCoal >= 1
                        && body.getStats().getValue(
                                Stats.ITEM_PICKED_UP.get(Items.COAL)
                        ) - initialPickedUpCoal >= 1;
                final boolean smeltedCharcoalFuel =
                        body.getStats().getValue(
                                Stats.ITEM_CRAFTED.get(
                                        Items.CHARCOAL
                                )
                        ) - initialCraftedCharcoal >= 1;
                helper.assertTrue(
                        minedCoalFuel || smeltedCharcoalFuel,
                        "M1 iron toolkit produced no physically audited "
                            + "coal or charcoal fuel"
                );
                helper.assertTrue(
                        body.getStats().getValue(
                            Stats.BLOCK_MINED.get(Blocks.IRON_ORE)
                        ) - initialMinedIron >= 7,
                        "M1 iron toolkit did not mine seven iron ore"
                );
                helper.assertTrue(
                        body.getStats().getValue(
                            Stats.ITEM_PICKED_UP.get(Items.RAW_IRON)
                        ) - initialPickedUpRawIron >= 7,
                        "M1 iron toolkit did not physically collect "
                            + "seven raw iron"
                );
                helper.assertTrue(
                        body.getStats().getValue(
                            Stats.ITEM_CRAFTED.get(Items.FURNACE)
                        ) - initialCraftedFurnaces >= 1,
                        "M1 iron toolkit did not craft its furnace"
                );
                helper.assertTrue(
                        body.getStats().getValue(
                            Stats.ITEM_CRAFTED.get(Items.IRON_PICKAXE)
                        ) - initialCraftedIronPickaxes >= 1
                        && body.getStats().getValue(
                            Stats.ITEM_CRAFTED.get(Items.BUCKET)
                        ) - initialCraftedBuckets >= 1
                        && body.getStats().getValue(
                            Stats.ITEM_CRAFTED.get(Items.SHIELD)
                        ) - initialCraftedShields >= 1,
                        "M1 iron toolkit did not craft all three "
                            + "retained iron items through recipes"
                );
                if (zeroHumanFromStart) {
                    assertAutonomousChunkSimulation(body);
                }
                stage = FoundationBootstrapStage.WORKSTATIONS;
                stageStartedNanos = System.nanoTime();
                return;
            }
            final GoalSnapshot goal = runtime.goals().snapshot();
            helper.assertTrue(
                    goal.status() == GoalStatus.RUNNING
                        || goal.status() == GoalStatus.CANCEL_PENDING,
                    "Foundation goal became terminal before iron "
                        + "toolkit readiness: " + goal
                        + ", pickaxe=" + ownsIronPickaxe
                        + ", bucket=" + ownsBucket
                        + ", shield=" + ownsShield
                        + ", furnace=" + furnaceVerified
                        + ", milestone=" + milestone
                        + ", skill=" + skill.skillName()
                        + ", rejection="
                        + skill.lastStartRejection()
            );
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= FOUNDATION_TOOLKIT_TIMEOUT_NANOS,
                    "Live foundation route did not complete its iron "
                        + "toolkit: pickaxe=" + ownsIronPickaxe
                        + ", bucket=" + ownsBucket
                        + ", shield=" + ownsShield
                        + ", furnace=" + furnaceVerified
                        + ", milestone=" + milestone
                        + ", coal=" + coalSummary()
                        + ", iron=" + ironSummary()
                        + ", skill=" + skill.skillName()
                        + ", rejection="
                        + skill.lastStartRejection()
            );
        }

        private void waitForWorkstations() {
            assertNoHumanPlayersDuringAutonomy();
            final Optional<ServerPlayer> bodyCandidate = AiPlayerManager
                    .onlinePlayer(runtime.server());
            if (bodyCandidate.isEmpty()) {
                return;
            }
            final ServerPlayer body = bodyCandidate.get();
            helper.assertTrue(
                    body.isAlive(),
                    "Companion died while establishing M1 workstations"
            );
            final var evidence = runtime.worldData()
                    .verifiedFoundationEvidence(
                            foundationGoalRevision
                    );
            final var verification = evidence
                    .map(found ->
                            ServerFoundationEvidenceVerifier.verify(
                                    runtime.server(),
                                    found
                            )
                    )
                    .orElseGet(() ->
                            new ServerFoundationEvidenceVerifier.Result(
                                    false,
                                    false
                            )
                    );
            final boolean milestone = runtime.worldData()
                    .verifiedRouteProgress(foundationGoalRevision)
                    .milestones()
                    .contains(
                            SurvivalMilestone.WORKSTATIONS_ESTABLISHED
                    );
            final boolean chestCrafted = body.getStats().getValue(
                    Stats.ITEM_CRAFTED.get(Items.CHEST)
            ) - initialCraftedChests >= 1;
            if (verification.workstationsEstablished()
                    && milestone
                    && chestCrafted) {
                helper.assertTrue(
                        evidence.orElseThrow().craftingTable().isPresent()
                            && evidence.orElseThrow().furnace().isPresent()
                            && evidence.orElseThrow().storage().isPresent(),
                        "M1 workstation evidence omitted an opened fixture"
                );
                stage = FoundationBootstrapStage.STORAGE;
                stageStartedNanos = System.nanoTime();
                return;
            }
            final var skill = runtime.skillSupervisor().snapshot();
            final GoalSnapshot goal = runtime.goals().snapshot();
            helper.assertTrue(
                    goal.status() == GoalStatus.RUNNING
                        || goal.status() == GoalStatus.CANCEL_PENDING,
                    "Foundation goal became terminal before workstation "
                        + "verification: " + goal
                        + ", verification=" + verification
                        + ", chestCrafted=" + chestCrafted
                        + ", evidence=" + evidence
                        + ", inventory=" + body.getInventory()
                        + ", skill=" + skill.skillName()
                        + ", rejection=" + skill.lastStartRejection()
            );
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= FOUNDATION_TOOLKIT_TIMEOUT_NANOS,
                    "Live foundation route did not establish and open its "
                        + "crafting table, furnace, and chest: verification="
                        + verification + ", chestCrafted=" + chestCrafted
                        + ", evidence=" + evidence
                        + ", inventory=" + body.getInventory()
                        + ", skill=" + skill.skillName()
                        + ", rejection=" + skill.lastStartRejection()
            );
        }

        private void waitForStorage() {
            assertNoHumanPlayersDuringAutonomy();
            final Optional<ServerPlayer> bodyCandidate = AiPlayerManager
                    .onlinePlayer(runtime.server());
            if (bodyCandidate.isEmpty()) {
                return;
            }
            final ServerPlayer body = bodyCandidate.get();
            helper.assertTrue(
                    body.isAlive(),
                    "Companion died while storing M1 supplies"
            );
            final var evidence = runtime.worldData()
                    .verifiedFoundationEvidence(
                            foundationGoalRevision
                    );
            final var verification = evidence
                    .map(found ->
                            ServerFoundationEvidenceVerifier.verify(
                                    runtime.server(),
                                    found
                            )
                    )
                    .orElseGet(() ->
                            new ServerFoundationEvidenceVerifier.Result(
                                    false,
                                    false
                            )
                    );
            final boolean milestone = runtime.worldData()
                    .verifiedRouteProgress(foundationGoalRevision)
                    .milestones()
                    .contains(SurvivalMilestone.SUPPLIES_STORED);
            if (verification.suppliesStored() && milestone) {
                final var recorded = evidence.orElseThrow();
                helper.assertTrue(
                        recorded.suppliesDeposited()
                            && recorded.depositedItemCount() > 0
                            && !recorded.depositedItemId().isBlank(),
                        "M1 storage milestone lacked a genuine recorded "
                            + "menu deposit"
                );
                if (zeroHumanFromStart) {
                    assertAutonomousChunkSimulation(body);
                }
                stage = FoundationBootstrapStage.SHELTER_MATERIALS;
                stageStartedNanos = System.nanoTime();
                return;
            }
            final var skill = runtime.skillSupervisor().snapshot();
            final GoalSnapshot goal = runtime.goals().snapshot();
            helper.assertTrue(
                    goal.status() == GoalStatus.RUNNING
                        || goal.status() == GoalStatus.CANCEL_PENDING,
                    "Foundation goal became terminal before storage "
                        + "verification: " + goal
                        + ", verification=" + verification
                        + ", evidence=" + evidence
                        + ", inventory=" + body.getInventory()
                        + ", skill=" + skill.skillName()
                        + ", rejection=" + skill.lastStartRejection()
            );
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= FOUNDATION_TOOLKIT_TIMEOUT_NANOS,
                    "Live foundation route did not deposit surplus through "
                        + "the opened chest menu: verification="
                        + verification + ", evidence=" + evidence
                        + ", inventory=" + body.getInventory()
                        + ", skill=" + skill.skillName()
                        + ", rejection=" + skill.lastStartRejection()
            );
        }

        private void waitForShelterMaterials() {
            assertNoHumanPlayersDuringAutonomy();
            final Optional<ServerPlayer> bodyCandidate = AiPlayerManager
                    .onlinePlayer(runtime.server());
            if (bodyCandidate.isEmpty()) {
                return;
            }
            final ServerPlayer body = bodyCandidate.get();
            helper.assertTrue(
                    body.isAlive(),
                    "Companion died while preparing shelter materials"
            );
            final int structural = body.getInventory()
                    .countItem(Items.OAK_PLANKS);
            final int doors = body.getInventory()
                    .countItem(Items.OAK_DOOR);
            final int lights = body.getInventory()
                    .countItem(Items.TORCH);
            final var skill = runtime.skillSupervisor().snapshot();
            if (structural >= DynamicShelterPlanner
                        .structuralBlockCount(3, 3)
                    && doors >= 1
                    && lights >= 1) {
                final long minedReserveLogs = reserveLogs.stream()
                        .filter(pos -> helper.getLevel()
                                .getBlockState(pos)
                                .isAir())
                        .count();
                helper.assertTrue(
                        body.getStats().getValue(
                            Stats.BLOCK_MINED.get(Blocks.OAK_LOG)
                        ) - initialMinedLogs
                            >= REQUIRED_LOGS + minedReserveLogs,
                        "Shelter material preparation did not physically "
                            + "mine the reserve wood"
                );
                helper.assertTrue(
                        minedReserveLogs > 0,
                        "Shelter material preparation did not consume any "
                            + "physical reserve wood"
                );
                helper.assertTrue(
                        body.getStats().getValue(
                            Stats.ITEM_CRAFTED.get(Items.OAK_DOOR)
                        ) - initialCraftedDoors >= 3,
                        "Shelter door did not come from its vanilla recipe"
                );
                helper.assertTrue(
                        body.getStats().getValue(
                            Stats.ITEM_CRAFTED.get(Items.TORCH)
                        ) - initialCraftedTorches >= 4,
                        "Shelter light did not come from its vanilla recipe"
                );
                stage = FoundationBootstrapStage.SHELTER;
                stageStartedNanos = System.nanoTime();
                return;
            }
            final GoalSnapshot goal = runtime.goals().snapshot();
            helper.assertTrue(
                    goal.status() == GoalStatus.RUNNING
                        || goal.status() == GoalStatus.CANCEL_PENDING,
                    "Foundation goal became terminal before shelter "
                        + "materials: " + goal
                        + ", planks=" + structural
                        + ", doors=" + doors
                        + ", lights=" + lights
                        + ", reserve=" + reserveLogSummary()
                        + ", skill=" + skill.skillName()
                        + ", rejection=" + skill.lastStartRejection()
            );
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= FOUNDATION_TOOLKIT_TIMEOUT_NANOS,
                    "Live foundation route did not prepare shelter "
                        + "materials: planks=" + structural
                        + ", doors=" + doors
                        + ", lights=" + lights
                        + ", reserve=" + reserveLogSummary()
                        + ", skill=" + skill.skillName()
                        + ", rejection=" + skill.lastStartRejection()
            );
        }

        private void waitForShelter() {
            assertNoHumanPlayersDuringAutonomy();
            final Optional<ServerPlayer> bodyCandidate = AiPlayerManager
                    .onlinePlayer(runtime.server());
            if (bodyCandidate.isEmpty()) {
                return;
            }
            final ServerPlayer body = bodyCandidate.get();
            helper.assertTrue(
                    body.isAlive(),
                    "Companion died while constructing the M1 shelter"
            );
            final var evidence = runtime.worldData()
                    .verifiedShelterEvidence(foundationGoalRevision);
            final boolean verified = evidence
                    .filter(found ->
                            ServerShelterEvidenceVerifier.verify(
                                    runtime.server(),
                                    found
                            )
                    )
                    .isPresent();
            final boolean milestone = runtime.worldData()
                    .verifiedRouteProgress(foundationGoalRevision)
                    .milestones()
                    .contains(SurvivalMilestone.SHELTER_COMPLETED);
            if (verified && milestone) {
                final var shelter = evidence.orElseThrow();
                helper.assertTrue(
                        shelter.interiorWidth() >= 3
                            && shelter.interiorDepth() >= 3
                            && shelter.interiorHeight() >= 2,
                        "Verified M1 shelter was below the required "
                            + "3x3x2 interior"
                );
                if (zeroHumanFromStart) {
                    assertAutonomousChunkSimulation(body);
                }
                stage = FoundationBootstrapStage.FIRST_NIGHT;
                stageStartedNanos = System.nanoTime();
                return;
            }
            final var skill = runtime.skillSupervisor().snapshot();
            final GoalSnapshot goal = runtime.goals().snapshot();
            helper.assertTrue(
                    goal.status() == GoalStatus.RUNNING
                        || goal.status() == GoalStatus.CANCEL_PENDING,
                    "Foundation goal became terminal before shelter "
                        + "verification: " + goal
                        + ", evidence=" + evidence
                        + ", verified=" + verified
                        + ", milestone=" + milestone
                        + ", skill=" + skill.skillName()
                        + ", rejection=" + skill.lastStartRejection()
            );
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= FOUNDATION_TOOLKIT_TIMEOUT_NANOS,
                    "Live foundation route did not complete a verified "
                        + "dynamic shelter: evidence=" + evidence
                        + ", verified=" + verified
                        + ", milestone=" + milestone
                        + ", skill=" + skill.skillName()
                        + ", rejection=" + skill.lastStartRejection()
            );
        }

        private void waitForFirstNight() {
            assertNoHumanPlayersDuringAutonomy();
            final Optional<ServerPlayer> bodyCandidate = AiPlayerManager
                    .onlinePlayer(runtime.server());
            if (bodyCandidate.isEmpty()) {
                return;
            }
            final ServerPlayer body = bodyCandidate.get();
            helper.assertTrue(
                    body.isAlive(),
                    "Companion died before reaching the second day"
            );
            final var milestones = runtime.worldData()
                    .verifiedRouteProgress(foundationGoalRevision)
                    .milestones();
            final boolean survived = milestones.contains(
                    SurvivalMilestone.FIRST_NIGHT_SURVIVED
            );
            final boolean shelterStillValid = runtime.worldData()
                    .verifiedShelterEvidence(foundationGoalRevision)
                    .filter(evidence ->
                            ServerShelterEvidenceVerifier.verify(
                                    runtime.server(),
                                    evidence
                            )
                    )
                    .isPresent();
            final GoalSnapshot goal = runtime.goals().snapshot();
            if (survived
                    && shelterStillValid
                    && goal.status() == GoalStatus.COMPLETED) {
                helper.assertTrue(
                        milestones.contains(
                            SurvivalMilestone.SHELTER_COMPLETED
                        ),
                        "First-night completion lost shelter evidence"
                );
                stage = FoundationBootstrapStage.DONE;
                helper.succeed();
                return;
            }
            helper.assertTrue(
                    goal.status() == GoalStatus.RUNNING
                        || goal.status() == GoalStatus.CANCEL_PENDING
                        || survived
                            && goal.status() == GoalStatus.COMPLETED,
                    "Foundation goal failed before verified second day: "
                        + goal + ", survived=" + survived
                        + ", shelterValid=" + shelterStillValid
            );
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= FOUNDATION_TOOLKIT_TIMEOUT_NANOS,
                    "Live foundation route did not survive the actual "
                        + "night and complete its goal: clock="
                        + runtime.server().overworld()
                            .getOverworldClockTime()
                        + ", milestones=" + milestones
                        + ", goal=" + goal
                        + ", shelterValid=" + shelterStillValid
            );
        }

        private void prepareFixture(final ServerPlayer body) {
            final var level = helper.getLevel();
            helper.setTime(18_000L);
            level.getEntitiesOfClass(
                    Mob.class,
                    body.getBoundingBox().inflate(48.0)
            ).forEach(Mob::discard);
            final BlockPos center;
            if (zeroHumanFromStart) {
                center = helper.absolutePos(
                        new BlockPos(8, 1, 8)
                ).offset(640, 0, 0);
            } else {
                center = body.blockPosition()
                        .below()
                        .offset(0, 0, 4);
            }
            for (int x = -10; x <= 10; x++) {
                for (int z = -10; z <= 10; z++) {
                    final BlockPos floor = center.offset(x, 0, z);
                    level.setBlockAndUpdate(
                            floor,
                            Blocks.DIRT.defaultBlockState()
                    );
                    for (int y = 1; y <= 6; y++) {
                        level.setBlockAndUpdate(
                                floor.above(y),
                                Blocks.AIR.defaultBlockState()
                        );
                    }
                }
            }
            final BlockPos base = center.above();
            logs = List.of(
                    base,
                    base.above(),
                    base.above(2),
                    base.above(3),
                    base.above(2).east(),
                    base.above(2).west(),
                    base.above(3).north(),
                    base.above(3).south()
            );
            logs.forEach(pos -> level.setBlockAndUpdate(
                    pos,
                    Blocks.OAK_LOG.defaultBlockState()
            ));
            final List<BlockPos> preparedReserveLogs =
                    new ArrayList<>();
            /*
             * The empty-inventory route can consume the original eight logs
             * on its table, three pickaxes, shield and chest before shelter
             * preparation starts. The shelter then needs 55 planks, a door,
             * sticks and, when the iron furnace used the last coal, one
             * retained log plus a plank for legal charcoal smelting. Four
             * rows left the controlled underground fixture two logs short
             * after every staged log had been physically mined. Five rows
             * provide that bounded recipe budget plus a small pickup margin;
             * they remain ordinary world blocks and the oracle still
             * requires the production body to discover, mine and collect
             * them after the test starts.
             */
            for (int x = -9; x <= -5; x++) {
                for (int z = -2; z <= 1; z++) {
                    preparedReserveLogs.add(
                            base.offset(x, 0, z)
                    );
                }
            }
            reserveLogs = List.copyOf(preparedReserveLogs);
            reserveLogs.forEach(pos -> level.setBlockAndUpdate(
                    pos,
                    Blocks.OAK_LOG.defaultBlockState()
            ));
            final List<BlockPos> preparedStone =
                    new ArrayList<>();
            for (int x = -3; x <= 0; x++) {
                for (int y = 0; y <= 2; y++) {
                    preparedStone.add(base.offset(x, y, 4));
                }
            }
            stoneBlocks = List.copyOf(preparedStone);
            stoneBlocks.forEach(pos -> level.setBlockAndUpdate(
                    pos,
                    Blocks.STONE.defaultBlockState()
            ));
            coalBlocks = List.of(
                    base.offset(1, 0, 4),
                    base.offset(1, 1, 4)
            );
            coalBlocks.forEach(pos -> level.setBlockAndUpdate(
                    pos,
                    Blocks.COAL_ORE.defaultBlockState()
            ));
            ironBlocks = List.of(
                    base.offset(2, 0, 4),
                    base.offset(2, 1, 4),
                    base.offset(2, 2, 4),
                    base.offset(3, 0, 4),
                    base.offset(3, 1, 4),
                    base.offset(3, 2, 4),
                    base.offset(4, 0, 4)
            );
            ironBlocks.forEach(pos -> level.setBlockAndUpdate(
                    pos,
                    Blocks.IRON_ORE.defaultBlockState()
            ));
            final List<Cow> preparedAnimals = new ArrayList<>();
            for (int index = 0; index < 8; index++) {
                final Cow cow = EntityTypes.COW.create(
                        level,
                        EntitySpawnReason.COMMAND
                );
                helper.assertTrue(
                        cow != null,
                        "Live foundation fixture could not create cow"
                );
                cow.setBaby(false);
                cow.setNoAi(true);
                cow.setHealth(1.0F);
                final int column = index % 4;
                final int row = index / 4;
                cow.setPos(
                        center.getX() - 3.0 + column * 1.75,
                        center.getY() + 1.0,
                        center.getZ() + 1.0 + row * 1.75
                );
                helper.assertTrue(
                        level.addFreshEntity(cow),
                        "Live foundation fixture could not add cow"
                );
                preparedAnimals.add(cow);
            }
            foodAnimals = List.copyOf(preparedAnimals);
            body.setGameMode(GameType.SURVIVAL);
            body.getInventory().clearContent();
            body.getInventory().setSelectedSlot(0);
            body.setHealth(body.getMaxHealth());
            body.getFoodData().setFoodLevel(20);
            body.teleportTo(
                    center.getX() + 0.5,
                    center.getY() + 1.0,
                    center.getZ() - 3.5
            );
            autonomousWorkCenter = center.immutable();
            autonomousAnchorChunk = body.chunkPosition();
            body.setDeltaMovement(Vec3.ZERO);
            body.fallDistance = 0.0F;
        }

        private void assertAutonomousChunkSimulation(
                final ServerPlayer body
        ) {
            helper.assertTrue(
                    autonomousWorkCenter != null
                        && autonomousAnchorChunk != null,
                    "Unattended work-area ticket evidence was not "
                        + "initialized"
            );
            final double deltaX = body.getX()
                    - (autonomousWorkCenter.getX() + 0.5D);
            final double deltaZ = body.getZ()
                    - (autonomousWorkCenter.getZ() + 0.5D);
            final double horizontalDistanceSquared =
                    deltaX * deltaX + deltaZ * deltaZ;
            helper.assertTrue(
                    horizontalDistanceSquared
                        <= AUTONOMOUS_WORK_RADIUS
                            * AUTONOMOUS_WORK_RADIUS,
                    "Unattended companion left its bounded work area: "
                        + "center=" + autonomousWorkCenter
                        + ", body=" + body.position()
                        + ", radius=" + AUTONOMOUS_WORK_RADIUS
            );

            /*
             * The 21x21 fixture necessarily straddles chunk boundaries for
             * many random GameTest origins. A real player gathering ore is
             * expected to cross those boundaries, so equality with the
             * initial chunk is not a valid ticket invariant. Assert both the
             * original work anchor and the body's current chunk instead: the
             * former proves the nearby work area remains simulated and the
             * latter proves the vanilla player ticket followed the moving
             * headless body.
             */
            assertVanillaPlayerTicketSimulation(
                    autonomousAnchorChunk,
                    "initial work anchor"
            );
            assertVanillaPlayerTicketSimulation(
                    body.chunkPosition(),
                    "current companion chunk"
            );
        }

        private void assertVanillaPlayerTicketSimulation(
                final ChunkPos chunk,
                final String role
        ) {
            helper.assertTrue(
                    helper.getLevel().shouldTickBlocksAt(
                            chunk.pack()
                    ),
                    "Unattended companion did not maintain block "
                        + "simulation in " + role
                        + " through its player ticket: " + chunk
            );
            helper.assertTrue(
                    helper.getLevel()
                        .areEntitiesActuallyLoadedAndTicking(
                                chunk
                        ),
                    "Unattended companion did not maintain entity "
                        + "simulation in " + role
                        + " through its player ticket: " + chunk
            );
            helper.assertTrue(
                    !helper.getLevel()
                        .getChunkSource()
                        .getForceLoadedChunks()
                        .contains(chunk.pack()),
                    "Unattended test force-loaded " + role
                        + " instead of relying on the companion's vanilla "
                        + "player ticket: " + chunk
            );
        }

        private boolean latestObservationSeesOakLog() {
            final Optional<String> semantic =
                    runtime.observations().latestSemanticJson();
            if (semantic.isEmpty()) {
                return false;
            }
            try {
                final var root = JsonParser
                        .parseString(semantic.orElseThrow())
                        .getAsJsonObject();
                for (var element
                        : root.getAsJsonArray("visibleBlockFaces")) {
                    if ("minecraft:oak_log".equals(
                            element.getAsJsonObject()
                                .get("type").getAsString()
                    )) {
                        return true;
                    }
                }
                return false;
            } catch (RuntimeException malformedObservation) {
                return false;
            }
        }

        private String logSummary() {
            return logs.stream()
                    .map(pos -> pos + "="
                            + helper.getLevel().getBlockState(pos))
                    .toList()
                    .toString();
        }

        private String stoneSummary() {
            return stoneBlocks.stream()
                    .map(pos -> pos + "="
                            + helper.getLevel().getBlockState(pos))
                    .toList()
                    .toString();
        }

        private String reserveLogSummary() {
            return reserveLogs.stream()
                    .map(pos -> pos + "="
                            + helper.getLevel().getBlockState(pos))
                    .toList()
                    .toString();
        }

        private String coalSummary() {
            return coalBlocks.stream()
                    .map(pos -> pos + "="
                            + helper.getLevel().getBlockState(pos))
                    .toList()
                    .toString();
        }

        private String ironSummary() {
            return ironBlocks.stream()
                    .map(pos -> pos + "="
                            + helper.getLevel().getBlockState(pos))
                    .toList()
                    .toString();
        }

        private void assertWithinModelDeadline(
                final String message
        ) {
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= MODEL_TIMEOUT_NANOS,
                    message
            );
        }

        private void assertNoHumanPlayersDuringAutonomy() {
            assertNoHumanPlayers(
                    "after the initial chat command"
            );
        }

        private void assertNoHumanPlayers(final String phaseDescription) {
            final long humanPlayers = runtime.server()
                    .getPlayerList()
                    .getPlayers()
                    .stream()
                    .filter(player -> !player.getUUID().equals(
                            runtime.worldData().companionUuid()
                    ))
                    .count();
            helper.assertTrue(
                    humanPlayers == 0L,
                    "Live autonomous foundation bootstrap observed "
                        + humanPlayers + " human player(s) "
                        + phaseDescription
            );
        }

        private void cleanup() {
            finishScenarioGoal(runtime);
            foodAnimals.stream()
                    .filter(Cow::isAlive)
                    .forEach(Cow::discard);
            if (humanSession != null) {
                humanSession.close();
            }
            if (AiPlayerManager.status(runtime.server()).state()
                    != SessionState.ABSENT) {
                AiPlayerManager.requestRemove(runtime.server());
            }
        }
    }

    private enum FoundationBootstrapStage {
        BODY,
        PROBE,
        SETTLE,
        GOAL,
        GATHER,
        BASIC_CRAFTING,
        STONE_GATHERING,
        STONE_CRAFTING,
        FOOD,
        IRON_TOOLKIT,
        WORKSTATIONS,
        STORAGE,
        SHELTER_MATERIALS,
        SHELTER,
        FIRST_NIGHT,
        DONE
    }

    private static final class LiveShelterRelocationScenario {
        private static final long BUILD_TIMEOUT_NANOS =
                java.time.Duration.ofMinutes(6).toNanos();
        private static final double MINIMUM_RELOCATION_DISTANCE = 1.5D;

        private final GameTestHelper helper;
        private final ServerRuntime runtime;
        private final long createdAt;

        private ShelterRelocationStage stage =
                ShelterRelocationStage.BODY;
        private CompletableFuture<CapabilityProbeOutcome> probe;
        private PlacedHuman humanSession;
        private BlockPos initialFeet;
        private BlockPos craftingTable;
        private BlockPos furnace;
        private BlockPos chest;
        private long goalRevisionBefore;
        private long goalRevision;
        private long stageStartedNanos;
        private int stableTicks;
        private int initialPlankCount;
        private boolean sawBuildSkill;

        private LiveShelterRelocationScenario(
                final GameTestHelper helper,
                final ServerRuntime runtime
        ) {
            this.helper = helper;
            this.runtime = runtime;
            this.createdAt = helper.getTick();
            this.stageStartedNanos = System.nanoTime();
        }

        private void start() {
            finishScenarioGoal(runtime);
            final var status = AiPlayerManager.status(runtime.server());
            if (status.state() == SessionState.ABSENT) {
                helper.assertTrue(
                        AiPlayerManager.requestSpawn(runtime.server())
                                .accepted(),
                        "Live-shelter companion spawn was rejected"
                );
            }
        }

        private void tick() {
            if (humanSession != null) {
                humanSession.tick();
            }
            switch (stage) {
                case BODY -> waitForBody();
                case PROBE -> waitForProbe();
                case SETTLE -> waitForSettlement();
                case GOAL -> waitForGoal();
                case BUILD -> waitForShelter();
                case DONE -> {
                    // GameTest is already terminal.
                }
            }
        }

        private void waitForBody() {
            final var status = AiPlayerManager.status(runtime.server());
            helper.assertTrue(
                    status.state() != SessionState.FAILED,
                    "Live-shelter companion body failed"
            );
            if (status.state() != SessionState.ACTIVE
                    || !status.online()) {
                helper.assertTrue(
                        helper.getTick() - createdAt
                            <= BODY_TIMEOUT_TICKS,
                        "Live-shelter companion body timed out"
                );
                return;
            }
            final ServerPlayer body = AiPlayerManager
                    .onlinePlayer(runtime.server())
                    .orElseThrow();
            prepareFixture(body);
            probe = probeOrReuseVerifiedModel(runtime);
            stage = ShelterRelocationStage.PROBE;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForProbe() {
            assertWithinModelDeadline(
                    "Live shelter model capability probe timed out"
            );
            if (!probe.isDone()) {
                return;
            }
            final CapabilityProbeOutcome outcome = probe.join();
            helper.assertTrue(
                    outcome
                        instanceof CapabilityProbeOutcome.Supported,
                    "Configured live model probe failed: " + outcome
            );
            stage = ShelterRelocationStage.SETTLE;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForSettlement() {
            final ServerPlayer body = AiPlayerManager
                    .onlinePlayer(runtime.server())
                    .orElseThrow();
            body.lookAt(
                    EntityAnchorArgument.Anchor.EYES,
                    Vec3.atCenterOf(furnace)
            );
            body.setYHeadRot(body.getYRot());
            if (!body.onGround()) {
                stableTicks = 0;
                return;
            }
            if (++stableTicks < 12
                    || !latestObservationMatchesFixture(body)) {
                helper.assertTrue(
                        System.nanoTime() - stageStartedNanos
                            <= java.time.Duration.ofSeconds(20)
                                .toNanos(),
                        "Live-shelter semantic view did not settle on "
                            + "the workstation fixture"
                );
                return;
            }
            initialPlankCount = body.getInventory()
                    .countItem(Items.OAK_PLANKS);
            goalRevisionBefore = runtime.goals()
                    .snapshot()
                    .revision();
            humanSession = PlacedHuman.create(helper, runtime);
            final Component submitted =
                    ForgeHooks.onServerChatSubmittedEvent(
                            humanSession.player(),
                            Component.literal(
                                runtime.worldData().displayName()
                                    + "，去旁边安全的空地给我们建一个"
                                    + "紧凑的、有门和照明的房子。材料"
                                    + "已经在你背包里，别只说，直接动手。"
                            )
                    );
            helper.assertTrue(
                    submitted != null,
                    "Companion cancelled the shelter chat command"
            );
            humanSession.close();
            humanSession = null;
            stage = ShelterRelocationStage.GOAL;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForGoal() {
            assertNoHumanPlayersDuringAutonomy();
            assertWithinModelDeadline(
                    "Live model did not classify the shelter task"
            );
            final GoalSnapshot goal = runtime.goals().snapshot();
            if (goal.revision() == goalRevisionBefore) {
                return;
            }
            helper.assertTrue(
                    goal.revision() > goalRevisionBefore
                        && goal.goal().contains("房子"),
                    "Authorized shelter chat was not preserved as a task: "
                        + goal
            );
            goalRevision = goal.revision();
            stage = ShelterRelocationStage.BUILD;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForShelter() {
            assertNoHumanPlayersDuringAutonomy();
            final ServerPlayer body = AiPlayerManager
                    .onlinePlayer(runtime.server())
                    .orElseThrow();
            helper.assertTrue(
                    body.isAlive(),
                    "Companion died during isolated shelter construction"
            );
            final var skill = runtime.skillSupervisor().snapshot();
            if ("build_shelter_step".equals(skill.skillName())) {
                sawBuildSkill = true;
            }
            final var evidence = runtime.worldData()
                    .verifiedShelterEvidence(goalRevision);
            final boolean verified = evidence
                    .filter(found ->
                            ServerShelterEvidenceVerifier.verify(
                                    runtime.server(),
                                    found
                            )
                    )
                    .isPresent();
            if (verified) {
                final double moved = Math.sqrt(
                        body.distanceToSqr(
                                Vec3.atBottomCenterOf(initialFeet)
                        )
                );
                helper.assertTrue(
                        sawBuildSkill,
                        "Shelter appeared without the production "
                            + "build_shelter_step skill"
                );
                helper.assertTrue(
                        moved >= MINIMUM_RELOCATION_DISTANCE,
                        "Crowded shelter fixture completed without ordinary "
                            + "relocation: moved=" + moved
                );
                helper.assertTrue(
                        body.getInventory().countItem(Items.OAK_PLANKS)
                            < initialPlankCount,
                        "Verified shelter did not consume owned planks"
                );
                assertWorkstationsRemain();
                stage = ShelterRelocationStage.DONE;
                helper.succeed();
                return;
            }
            final GoalSnapshot currentGoal =
                    runtime.goals().snapshot();
            helper.assertTrue(
                    currentGoal.status() != GoalStatus.SAFE_IDLE
                            && currentGoal.status()
                                    != GoalStatus.FAILED,
                    "Live shelter entered a terminal failure before "
                            + "physical completion: position="
                            + body.position()
                            + ", initialFeet=" + initialFeet
                            + ", skill=" + skill.skillName()
                            + ", terminalResult="
                            + skill.terminalResult()
                            + ", rejection="
                            + skill.lastStartRejection()
                            + ", goal=" + currentGoal
                            + ", evidence=" + evidence
            );
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= BUILD_TIMEOUT_NANOS,
                    "Live model did not physically relocate and complete "
                        + "the seeded shelter: position="
                        + body.position()
                        + ", initialFeet=" + initialFeet
                        + ", inventory=" + body.getInventory()
                        + ", skill=" + skill.skillName()
                        + ", rejection=" + skill.lastStartRejection()
                        + ", goal=" + runtime.goals().snapshot()
                        + ", evidence=" + evidence
            );
        }

        private void prepareFixture(final ServerPlayer body) {
            final var level = helper.getLevel();
            level.getEntitiesOfClass(
                    Mob.class,
                    body.getBoundingBox().inflate(48.0)
            ).forEach(Mob::discard);
            final BlockPos floorCenter =
                    helper.absolutePos(new BlockPos(12, 1, 12));
            for (int x = -12; x <= 12; x++) {
                for (int z = -12; z <= 12; z++) {
                    final BlockPos floor = floorCenter.offset(x, 0, z);
                    level.setBlockAndUpdate(
                            floor,
                            Blocks.SMOOTH_STONE.defaultBlockState()
                    );
                    for (int y = 1; y <= 4; y++) {
                        level.setBlockAndUpdate(
                                floor.above(y),
                                Blocks.AIR.defaultBlockState()
                        );
                    }
                }
            }
            initialFeet = floorCenter.above();
            craftingTable = initialFeet.offset(-2, 0, -2);
            furnace = initialFeet.offset(-1, 0, -1);
            chest = initialFeet.offset(0, 0, -2);
            level.setBlockAndUpdate(
                    craftingTable,
                    Blocks.CRAFTING_TABLE.defaultBlockState()
            );
            level.setBlockAndUpdate(
                    furnace,
                    Blocks.FURNACE.defaultBlockState()
            );
            level.setBlockAndUpdate(
                    chest,
                    Blocks.CHEST.defaultBlockState()
            );
            body.setGameMode(GameType.SURVIVAL);
            body.getInventory().clearContent();
            body.getInventory().setSelectedSlot(0);
            body.getInventory().setItem(
                    0,
                    new ItemStack(Items.OAK_PLANKS, 64)
            );
            body.getInventory().setItem(
                    1,
                    new ItemStack(Items.OAK_DOOR, 3)
            );
            body.getInventory().setItem(
                    2,
                    new ItemStack(Items.TORCH, 4)
            );
            body.setHealth(body.getMaxHealth());
            body.getFoodData().setFoodLevel(20);
            body.getFoodData().setSaturation(5.0F);
            body.teleportTo(
                    initialFeet.getX() + 0.5,
                    initialFeet.getY(),
                    initialFeet.getZ() + 0.5
            );
            body.setDeltaMovement(Vec3.ZERO);
            body.fallDistance = 0.0F;
            body.inventoryMenu.broadcastChanges();
        }

        private boolean latestObservationMatchesFixture(
                final ServerPlayer body
        ) {
            final Optional<String> semantic =
                    runtime.observations().latestSemanticJson();
            if (semantic.isEmpty()) {
                return false;
            }
            try {
                final var root = JsonParser
                        .parseString(semantic.orElseThrow())
                        .getAsJsonObject();
                final var self = root.getAsJsonObject("self");
                if (self == null
                        || !self.get("onGround").getAsBoolean()) {
                    return false;
                }
                final var position =
                        self.getAsJsonObject("position");
                final double dx = position.get("x").getAsDouble()
                        - body.getX();
                final double dy = position.get("y").getAsDouble()
                        - body.getY();
                final double dz = position.get("z").getAsDouble()
                        - body.getZ();
                return dx * dx + dy * dy + dz * dz <= 0.25D;
            } catch (RuntimeException malformedObservation) {
                return false;
            }
        }

        private void assertWorkstationsRemain() {
            helper.assertTrue(
                    helper.getLevel().getBlockState(craftingTable)
                            .is(Blocks.CRAFTING_TABLE)
                        && helper.getLevel().getBlockState(furnace)
                            .is(Blocks.FURNACE)
                        && helper.getLevel().getBlockState(chest)
                            .is(Blocks.CHEST),
                    "Shelter relocation overwrote the existing "
                        + "workstation cluster"
            );
        }

        private void assertNoHumanPlayersDuringAutonomy() {
            final long humanPlayers = runtime.server()
                    .getPlayerList()
                    .getPlayers()
                    .stream()
                    .filter(player -> !player.getUUID().equals(
                            runtime.worldData().companionUuid()
                    ))
                    .count();
            helper.assertTrue(
                    humanPlayers == 0L,
                    "Live shelter autonomy retained "
                        + humanPlayers + " human player(s)"
            );
        }

        private void assertWithinModelDeadline(
                final String message
        ) {
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= MODEL_TIMEOUT_NANOS,
                    message
            );
        }

        private void cleanup() {
            finishScenarioGoal(runtime);
            if (humanSession != null) {
                humanSession.close();
            }
            if (AiPlayerManager.status(runtime.server()).state()
                    != SessionState.ABSENT) {
                AiPlayerManager.requestRemove(runtime.server());
            }
        }
    }

    private enum ShelterRelocationStage {
        BODY,
        PROBE,
        SETTLE,
        GOAL,
        BUILD,
        DONE
    }

    private static final class LiveFarmScenario {
        private static final int REQUIRED_PLOTS = 3;

        private final GameTestHelper helper;
        private final ServerRuntime runtime;
        private final long createdAt;

        private FarmStage stage = FarmStage.BODY;
        private CompletableFuture<CapabilityProbeOutcome> probe;
        private PlacedHuman humanSession;
        private List<BlockPos> crops = List.of();
        private long stageStartedNanos;
        private long goalRevisionBefore;
        private int stableTicks;
        private int farmingSkillStarts;
        private boolean farmingSkillActive;
        private int initialMinedWheat;

        private LiveFarmScenario(
                final GameTestHelper helper,
                final ServerRuntime runtime
        ) {
            this.helper = helper;
            this.runtime = runtime;
            createdAt = helper.getTick();
            stageStartedNanos = System.nanoTime();
        }

        private void start() {
            final var status = AiPlayerManager.status(runtime.server());
            if (status.state() == SessionState.ABSENT) {
                helper.assertTrue(
                        AiPlayerManager.requestSpawn(
                                runtime.server()
                        ).accepted(),
                        "Live-farm companion spawn was rejected"
                );
            }
        }

        private void tick() {
            if (humanSession != null) {
                humanSession.tick();
            }
            switch (stage) {
                case BODY -> waitForBody();
                case PROBE -> waitForProbe();
                case SETTLE -> waitForSettlement();
                case GOAL -> waitForGoal();
                case FARM -> waitForFarmWork();
                case DONE -> {
                    // GameTest is already terminal.
                }
            }
        }

        private void waitForBody() {
            final var status =
                    AiPlayerManager.status(runtime.server());
            helper.assertTrue(
                    status.state() != SessionState.FAILED,
                    "Live-farm companion body failed"
            );
            if (status.state() != SessionState.ACTIVE
                    || !status.online()) {
                helper.assertTrue(
                        helper.getTick() - createdAt
                            <= BODY_TIMEOUT_TICKS,
                        "Live-farm companion body timed out"
                );
                return;
            }
            final Optional<ServerPlayer> bodyCandidate = AiPlayerManager
                    .onlinePlayer(runtime.server());
            if (bodyCandidate.isEmpty()) {
                return;
            }
            final ServerPlayer body = bodyCandidate.get();
            prepareFixture(body);
            probe = probeOrReuseVerifiedModel(runtime);
            stage = FarmStage.PROBE;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForProbe() {
            assertWithinModelDeadline(
                    "Live farm model capability probe timed out"
            );
            if (!probe.isDone()) {
                return;
            }
            final CapabilityProbeOutcome outcome = probe.join();
            helper.assertTrue(
                    outcome
                        instanceof CapabilityProbeOutcome.Supported,
                    "Configured live model probe failed: " + outcome
            );
            stage = FarmStage.SETTLE;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForSettlement() {
            final Optional<ServerPlayer> bodyCandidate = AiPlayerManager
                    .onlinePlayer(runtime.server());
            if (bodyCandidate.isEmpty()) {
                return;
            }
            final ServerPlayer body = bodyCandidate.get();
            body.lookAt(
                    EntityAnchorArgument.Anchor.EYES,
                    Vec3.atCenterOf(crops.get(1))
            );
            body.setYHeadRot(body.getYRot());
            if (!body.onGround()) {
                stableTicks = 0;
                return;
            }
            if (++stableTicks < 8
                    || !latestObservationSeesMatureWheat(body)) {
                helper.assertTrue(
                        System.nanoTime() - stageStartedNanos
                            <= java.time.Duration.ofSeconds(15)
                                .toNanos(),
                        "Live-farm semantic view never exposed the "
                            + "mature field"
                );
                return;
            }
            initialMinedWheat = body.getStats().getValue(
                    Stats.BLOCK_MINED.get(Blocks.WHEAT)
            );
            humanSession = PlacedHuman.create(
                    helper,
                    runtime,
                    new Vec3(
                            crops.get(1).getX() + 0.5,
                            crops.get(1).getY() + 1.0,
                            crops.get(1).getZ() + 0.5
                    )
            );
            final ServerPlayer human = humanSession.player();
            helper.assertTrue(
                    CompanionCommandAccess.mayAdmin(
                            human.createCommandSourceStack()
                    ),
                    "Logged-in farm test player lacked task-write "
                        + "permission"
            );
            goalRevisionBefore = runtime.goals()
                    .snapshot()
                    .revision();
            final Component submitted =
                    ForgeHooks.onServerChatSubmittedEvent(
                            human,
                            Component.literal(
                                runtime.worldData().displayName()
                                    + "，把你面前成熟的三格小麦全部"
                                    + "收割，收完每一格都要重新种上；"
                                    + "把收获物捡进背包，不要漏掉，"
                                    + "也不要用命令。"
                            )
                    );
            helper.assertTrue(
                    submitted != null,
                    "Companion cancelled the farm chat command"
            );
            stage = FarmStage.GOAL;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForGoal() {
            assertWithinModelDeadline(
                    "Live model did not classify the farm task"
            );
            final GoalSnapshot goal = runtime.goals().snapshot();
            if (goal.revision() == goalRevisionBefore) {
                return;
            }
            helper.assertTrue(
                    goal.revision() > goalRevisionBefore
                        && goal.goal().contains("小麦"),
                    "Authorized farm chat was not preserved as the goal"
            );
            if (humanSession != null) {
                humanSession.close();
                humanSession = null;
            }
            stage = FarmStage.FARM;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForFarmWork() {
            final Optional<ServerPlayer> bodyCandidate = AiPlayerManager
                    .onlinePlayer(runtime.server());
            if (bodyCandidate.isEmpty()) {
                return;
            }
            final ServerPlayer body = bodyCandidate.get();
            helper.assertTrue(
                    body.isAlive(),
                    "Companion died during controlled farm work"
            );
            final var skill = runtime.skillSupervisor().snapshot();
            final boolean activeNow =
                    ("harvest_and_replant_step".equals(skill.skillName())
                            || "maintain_observed_crop_field".equals(
                                    skill.skillName()
                            ))
                    && (skill.state()
                        == dev.mcai.companion.skill.SkillSupervisor
                            .State.RUNNING
                        || skill.state()
                        == dev.mcai.companion.skill.SkillSupervisor
                            .State.CANCEL_PENDING);
            if (activeNow && !farmingSkillActive) {
                farmingSkillStarts++;
            }
            farmingSkillActive = activeNow;

            final boolean replanted = crops.stream().allMatch(pos -> {
                final var state = helper.getLevel()
                        .getBlockState(pos);
                return state.is(Blocks.WHEAT)
                        /*
                         * A successfully replanted age-zero crop may grow
                         * while the external model is answering. Requiring
                         * age zero would turn normal random ticks into a
                         * false failure, especially on Mojang's deliberately
                         * accelerated GameTest server. The isolated fixture
                         * began with age-seven crops, so wheat below maturity
                         * proves that every exact plot was replanted.
                         */
                        && state.getValue(CropBlock.AGE)
                            < CropBlock.MAX_AGE;
            });
            final int collectedWheat =
                    body.getInventory().countItem(Items.WHEAT);
            if (replanted && collectedWheat >= REQUIRED_PLOTS) {
                helper.assertTrue(
                        farmingSkillStarts >= 1,
                        "All plots changed without a model-selected "
                            + "production farming skill: "
                            + farmingSkillStarts
                );
                helper.assertTrue(
                        body.getStats().getValue(
                            Stats.BLOCK_MINED.get(Blocks.WHEAT)
                        ) - initialMinedWheat >= REQUIRED_PLOTS,
                        "Farm work did not record three vanilla wheat "
                            + "harvests"
                );
                stage = FarmStage.DONE;
                helper.succeed();
                return;
            }
            final GoalSnapshot goal = runtime.goals().snapshot();
            if (goal.status() != GoalStatus.RUNNING
                    && goal.status() != GoalStatus.CANCEL_PENDING) {
                helper.assertTrue(
                        false,
                        "Live farm goal became terminal before every "
                            + "farm requirement was satisfied: " + goal
                            + ", crops=" + cropSummary()
                            + ", collectedWheat=" + collectedWheat
                            + ", nearbyDrops="
                            + nearbyDropSummary(body)
                            + ", body=" + body.position()
                            + ", starts=" + farmingSkillStarts
                            + ", skill=" + skill.skillName()
                            + ", rejection="
                            + skill.lastStartRejection()
                );
            }
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= MODEL_TIMEOUT_NANOS,
                    "Live model did not harvest and replant all plots: "
                        + cropSummary() + ", starts="
                        + farmingSkillStarts + ", collectedWheat="
                        + collectedWheat + ", nearbyDrops="
                        + nearbyDropSummary(body) + ", skill="
                        + skill.skillName() + ", body="
                        + body.position() + ", rejection="
                        + skill.lastStartRejection()
            );
        }

        private void prepareFixture(final ServerPlayer body) {
            final var level = helper.getLevel();
            level.getEntitiesOfClass(
                    Mob.class,
                    body.getBoundingBox().inflate(48.0)
            ).forEach(Mob::discard);
            final BlockPos center = body.blockPosition()
                    .below()
                    .offset(0, 0, 3);
            crops = List.of(
                    center.above().west(),
                    center.above(),
                    center.above().east()
            );
            for (int x = -3; x <= 3; x++) {
                for (int z = -2; z <= 5; z++) {
                    final BlockPos floor = center.offset(x, 0, z - 3);
                    level.setBlockAndUpdate(
                            floor,
                            Blocks.SMOOTH_STONE
                                .defaultBlockState()
                    );
                    for (int y = 1; y <= 4; y++) {
                        level.setBlockAndUpdate(
                                floor.above(y),
                                Blocks.AIR.defaultBlockState()
                        );
                    }
                }
            }
            for (BlockPos crop : crops) {
                level.setBlockAndUpdate(
                        crop.below(),
                        Blocks.FARMLAND.defaultBlockState()
                );
                level.setBlockAndUpdate(
                        crop,
                        Blocks.WHEAT.defaultBlockState()
                            .setValue(
                                CropBlock.AGE,
                                CropBlock.MAX_AGE
                            )
                );
            }
            body.setGameMode(GameType.SURVIVAL);
            body.getInventory().clearContent();
            body.getInventory().setItem(
                    0,
                    new ItemStack(Items.WHEAT_SEEDS, 8)
            );
            body.getInventory().setSelectedSlot(0);
            body.setHealth(body.getMaxHealth());
            body.getFoodData().setFoodLevel(20);
            body.teleportTo(
                    center.getX() + 0.5,
                    center.getY() + 1.0,
                    center.getZ() - 1.5
            );
            body.setDeltaMovement(Vec3.ZERO);
        }

        private boolean latestObservationSeesMatureWheat(
                final ServerPlayer body
        ) {
            final Optional<String> semantic =
                    runtime.observations().latestSemanticJson();
            if (semantic.isEmpty()) {
                return false;
            }
            try {
                final var root = JsonParser
                        .parseString(semantic.orElseThrow())
                        .getAsJsonObject();
                final var self = root.getAsJsonObject("self");
                if (self == null
                        || !self.get("onGround").getAsBoolean()) {
                    return false;
                }
                for (var element
                        : root.getAsJsonArray("visibleBlockFaces")) {
                    final var face = element.getAsJsonObject();
                    if ("minecraft:wheat".equals(
                            face.get("type").getAsString()
                    )
                            && Integer.toString(CropBlock.MAX_AGE)
                                .equals(
                                    face.getAsJsonObject("state")
                                        .get("age").getAsString()
                                )) {
                        return true;
                    }
                }
                return false;
            } catch (RuntimeException malformedObservation) {
                return false;
            }
        }

        private String cropSummary() {
            return crops.stream()
                    .map(pos -> pos + "="
                            + helper.getLevel()
                                .getBlockState(pos))
                    .toList()
                    .toString();
        }

        private List<String> nearbyDropSummary(
                final ServerPlayer body
        ) {
            return helper.getLevel().getEntitiesOfClass(
                            ItemEntity.class,
                            body.getBoundingBox().inflate(12.0)
                    ).stream()
                    .map(drop -> drop.getItem().getItem()
                            + "x" + drop.getItem().getCount()
                            + "@" + drop.position())
                    .toList();
        }

        private void assertWithinModelDeadline(
                final String message
        ) {
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= MODEL_TIMEOUT_NANOS,
                    message
            );
        }

        private void cleanup() {
            finishScenarioGoal(runtime);
            if (humanSession != null) {
                humanSession.close();
            }
            if (AiPlayerManager.status(runtime.server()).state()
                    != SessionState.ABSENT) {
                AiPlayerManager.requestRemove(runtime.server());
            }
        }
    }

    private enum FarmStage {
        BODY,
        PROBE,
        SETTLE,
        GOAL,
        FARM,
        DONE
    }

    private static final class LiveEndVictoryScenario {
        private static final long END_ENTRY_TIMEOUT_TICKS = 300;
        private static final long END_SETTLE_TICKS = 80;
        private static final long FIGHT_TIMEOUT_NANOS =
                java.time.Duration.ofSeconds(90).toNanos();
        private static final long RETURN_TIMEOUT_NANOS =
                java.time.Duration.ofSeconds(150).toNanos();

        private final GameTestHelper helper;
        private final ServerRuntime runtime;
        private final long createdAt;

        private EndVictoryStage stage = EndVictoryStage.BODY;
        private CompletableFuture<CapabilityProbeOutcome> probe;
        private PlacedHuman humanSession;
        private EndCrystal crystal;
        private EnderDragon dragon;
        private BlockPos cageBar;
        private BlockPos returnPortalCenter;
        private UUID bodyId;
        private long enteredEndAt;
        private long goalRevisionBefore;
        private long victoryGoalRevision;
        private long stageStartedNanos;
        private boolean fightSkillObserved;
        private boolean returnSkillObserved;

        private LiveEndVictoryScenario(
                final GameTestHelper helper,
                final ServerRuntime runtime
        ) {
            this.helper = helper;
            this.runtime = runtime;
            createdAt = helper.getTick();
            stageStartedNanos = System.nanoTime();
        }

        private void start() {
            finishScenarioGoal(runtime);
            final var status = AiPlayerManager.status(runtime.server());
            if (status.state() == SessionState.ABSENT) {
                helper.assertTrue(
                        AiPlayerManager.requestSpawn(
                                runtime.server()
                        ).accepted(),
                        "End-victory companion spawn was rejected"
                );
            }
        }

        private void tick() {
            if (humanSession != null) {
                humanSession.tick();
            }
            switch (stage) {
                case BODY -> waitForBody();
                case ENTERING_END -> waitForEndEntry();
                case SETTLING_END -> waitForEndSettle();
                case PROBE -> waitForProbe();
                case VISIBLE -> waitForVisibleFight();
                case GOAL -> waitForGoal();
                case FIGHT_SKILL -> waitForFightSkill();
                case FIGHTING -> waitForDragonKill();
                case RETURNING -> waitForReturn();
                case DONE -> {
                    // GameTest is already terminal.
                }
            }
        }

        private void waitForBody() {
            final var status = AiPlayerManager.status(runtime.server());
            helper.assertTrue(
                    status.state() != SessionState.FAILED,
                    "End-victory companion body failed"
            );
            if (status.state() != SessionState.ACTIVE
                    || !status.online()) {
                helper.assertTrue(
                        helper.getTick() - createdAt
                            <= BODY_TIMEOUT_TICKS,
                        "End-victory companion body timed out"
                );
                return;
            }
            final ServerPlayer body = AiPlayerManager
                    .onlinePlayer(runtime.server())
                    .orElseThrow();
            bodyId = body.getUUID();
            prepareRealEndEntry(body);
            stage = EndVictoryStage.ENTERING_END;
        }

        private void prepareRealEndEntry(final ServerPlayer body) {
            final BlockPos entry = body.blockPosition();
            body.setGameMode(GameType.SURVIVAL);
            body.getInventory().clearContent();
            body.setHealth(body.getMaxHealth());
            body.getFoodData().setFoodLevel(20);
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    helper.getLevel().setBlockAndUpdate(
                            entry.offset(x, -1, z),
                            Blocks.OBSIDIAN.defaultBlockState()
                    );
                    helper.getLevel().setBlockAndUpdate(
                            entry.offset(x, 0, z),
                            Blocks.AIR.defaultBlockState()
                    );
                    helper.getLevel().setBlockAndUpdate(
                            entry.offset(x, 1, z),
                            Blocks.AIR.defaultBlockState()
                    );
                }
            }
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
            body.fallDistance = 0.0F;
        }

        private void waitForEndEntry() {
            final ServerPlayer body = body();
            if (body.level().dimension().equals(Level.END)) {
                enteredEndAt = helper.getTick();
                stage = EndVictoryStage.SETTLING_END;
                return;
            }
            helper.assertTrue(
                    helper.getTick() - createdAt
                        <= END_ENTRY_TIMEOUT_TICKS,
                    "Fixture body did not traverse the real End portal"
            );
        }

        private void waitForEndSettle() {
            final ServerPlayer body = body();
            helper.assertTrue(
                    body.level().dimension().equals(Level.END),
                    "Companion left the End before arena setup"
            );
            if (helper.getTick() - enteredEndAt < END_SETTLE_TICKS) {
                return;
            }
            prepareFightArena(body);
            probe = probeOrReuseVerifiedModel(runtime);
            stage = EndVictoryStage.PROBE;
            stageStartedNanos = System.nanoTime();
        }

        private void prepareFightArena(final ServerPlayer body) {
            final EndVictoryArena prepared =
                    prepareEndVictoryArena(
                            helper,
                            runtime,
                            body,
                            true,
                            true
                    );
            crystal = prepared.crystal();
            dragon = prepared.dragon();
            cageBar = prepared.cageBar();
            returnPortalCenter = prepared.returnPortalCenter();
        }

        private void waitForProbe() {
            assertWithinModelDeadline(
                    "End-victory model capability probe timed out"
            );
            if (!probe.isDone()) {
                return;
            }
            final CapabilityProbeOutcome outcome = probe.join();
            helper.assertTrue(
                    outcome
                        instanceof CapabilityProbeOutcome.Supported,
                    "Configured live model probe failed: " + outcome
            );
            stage = EndVictoryStage.VISIBLE;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForVisibleFight() {
            assertWithinModelDeadline(
                    "Dragon arena never became fair first-person evidence"
            );
            final var frame = runtime.coreFrames().current();
            if (frame.isEmpty()
                    || !frame.orElseThrow().dimension()
                            .equals(
                                    dev.mcai.companion.waypoint
                                            .DimensionRef.END
                            )
                    || !frame.orElseThrow().onGround()
                    || frame.orElseThrow().visibleEntities()
                            .stream()
                            .noneMatch(entity ->
                                    entity.entityTypeId().equals(
                                            "minecraft:end_crystal"
                                    )
                            )) {
                return;
            }
            humanSession = PlacedHuman.create(helper, runtime);
            final ServerPlayer human = humanSession.player();
            helper.assertTrue(
                    CompanionCommandAccess.mayAdmin(
                            human.createCommandSourceStack()
                    ),
                    "Logged-in End-victory test player lacked permission"
            );
            goalRevisionBefore = runtime.goals().snapshot().revision();
            final Component submitted =
                    ForgeHooks.onServerChatSubmittedEvent(
                            human,
                            Component.literal(
                                    runtime.worldData().displayName()
                                        + "，请击败末影龙，然后进入中央"
                                        + "返回传送门回到主世界。"
                            )
                    );
            helper.assertTrue(
                    submitted != null,
                    "Companion cancelled the End-victory chat command"
            );
            humanSession.close();
            humanSession = null;
            stage = EndVictoryStage.GOAL;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForGoal() {
            assertNoHumanPlayers();
            assertWithinModelDeadline(
                    "Live model did not classify the End-victory task"
            );
            final GoalSnapshot goal = runtime.goals().snapshot();
            if (goal.revision() == goalRevisionBefore) {
                return;
            }
            helper.assertTrue(
                    goal.revision() > goalRevisionBefore
                        && goal.status() == GoalStatus.RUNNING
                        && goal.goal().contains("末影龙"),
                    "End-victory chat did not become a running goal: "
                        + goal
            );
            victoryGoalRevision = goal.revision();
            stage = EndVictoryStage.FIGHT_SKILL;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForFightSkill() {
            assertNoHumanPlayers();
            assertWithinModelDeadline(
                    "Live model did not select parameterless "
                        + "fight_ender_dragon; " + diagnostics()
            );
            final var snapshot =
                    runtime.skillSupervisor().snapshot();
            if (!"fight_ender_dragon".equals(
                    snapshot.skillName()
            )) {
                return;
            }
            helper.assertTrue(
                    snapshot.boundGoalRevision()
                        == victoryGoalRevision,
                    "Dragon skill bound the wrong goal revision"
            );
            fightSkillObserved = true;
            stage = EndVictoryStage.FIGHTING;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForDragonKill() {
            assertNoHumanPlayers();
            final var milestones = runtime.worldData()
                    .verifiedRouteProgress(victoryGoalRevision)
                    .milestones();
            if (milestones.contains(
                    SurvivalMilestone.DRAGON_KILLED
            )) {
                final ServerPlayer body = body();
                helper.assertTrue(
                        fightSkillObserved
                            && (!crystal.isAlive()
                                || crystal.isRemoved())
                            && body.level()
                                .getBlockState(cageBar)
                                .isAir()
                            && body.getInventory()
                                .countItem(Items.ARROW) < 64,
                        "Dragon milestone lacked physical fight evidence: "
                            + diagnostics()
                );
                /*
                 * This release-excluded fixture freezes the full-health
                 * dragon so the production coordinator has a deterministic
                 * ranged/cage/melee target. Vanilla fires the credited death
                 * event before its 200-tick DYING animation, and a no-AI
                 * dragon otherwise remains at one health forever. Resume the
                 * ordinary death lifecycle here. Production dragons never
                 * receive no-AI, and the emergency lane may legitimately
                 * retain body ownership until this hostile corpse disappears.
                 */
                if (!dragon.isRemoved()) {
                    dragon.setNoAi(false);
                }
                activateEndReturnPortal(
                        runtime.server().getLevel(Level.END),
                        returnPortalCenter
                );
                stage = EndVictoryStage.RETURNING;
                stageStartedNanos = System.nanoTime();
                return;
            }
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= FIGHT_TIMEOUT_NANOS,
                    "Live-model dragon fight timed out: "
                        + diagnostics()
            );
        }

        private void waitForReturn() {
            assertNoHumanPlayers();
            final var supervisor =
                    runtime.skillSupervisor().snapshot();
            if ("find_and_enter_observed_portal".equals(
                    supervisor.skillName()
            )) {
                helper.assertTrue(
                        supervisor.boundGoalRevision()
                            == victoryGoalRevision,
                        "Return portal skill bound the wrong goal revision"
                );
                returnSkillObserved = true;
            }
            final ServerPlayer body = body();
            final var milestones = runtime.worldData()
                    .verifiedRouteProgress(victoryGoalRevision)
                    .milestones();
            if (body.level().dimension().equals(Level.OVERWORLD)
                    && milestones.contains(
                            SurvivalMilestone.RETURNED_FROM_END
                    )) {
                helper.assertTrue(
                        fightSkillObserved
                            && returnSkillObserved
                            && bodyId.equals(body.getUUID()),
                        "End return lacked the same live-model body/skills: "
                            + diagnostics()
                );
                /*
                 * Returning through the vanilla portal is the physical
                 * milestone. Give the normal brain tick a bounded chance to
                 * retire the portal skill and apply the server-owned
                 * completion verifier; ending the fixture on the first
                 * return tick would hide a real RUNNING/SAFE_IDLE mismatch.
                 */
                if (runtime.goals().snapshot().status()
                        != GoalStatus.COMPLETED) {
                    helper.assertTrue(
                            System.nanoTime() - stageStartedNanos
                                    <= RETURN_TIMEOUT_NANOS,
                            "End return reached but goal was not completed: "
                                    + diagnostics()
                    );
                    return;
                }
                stage = EndVictoryStage.DONE;
                helper.succeed();
                return;
            }
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= RETURN_TIMEOUT_NANOS,
                    "Live-model End return timed out: "
                        + diagnostics()
            );
        }

        private ServerPlayer body() {
            return AiPlayerManager.onlinePlayer(runtime.server())
                    .orElseThrow();
        }

        private void assertNoHumanPlayers() {
            final long humans = runtime.server()
                    .getPlayerList()
                    .getPlayers()
                    .stream()
                    .filter(player -> !player.getUUID().equals(
                            runtime.worldData().companionUuid()
                    ))
                    .count();
            helper.assertTrue(
                    humans == 0L,
                    "End-victory autonomy retained " + humans
                        + " human player(s)"
            );
        }

        private void assertWithinModelDeadline(final String message) {
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= MODEL_TIMEOUT_NANOS,
                    message
            );
        }

        private String diagnostics() {
            final ServerPlayer body = body();
            return "supervisor="
                    + runtime.skillSupervisor().snapshot()
                    + ", goal=" + runtime.goals().snapshot()
                    + ", dimension="
                    + body.level().dimension().identifier()
                    + ", body=" + body.position()
                    + ", milestones="
                    + runtime.worldData()
                        .verifiedRouteProgress(
                                Math.max(
                                        0L,
                                        victoryGoalRevision
                                )
                        )
                        .milestones()
                    + ", dragonHealth="
                    + (dragon == null ? "none" : dragon.getHealth())
                    + ", crystalAlive="
                    + (crystal != null && crystal.isAlive())
                    + ", returnPortal=" + returnPortalCenter;
        }

        private void cleanup() {
            finishScenarioGoal(runtime);
            if (humanSession != null) {
                humanSession.close();
            }
            if (AiPlayerManager.status(runtime.server()).state()
                    != SessionState.ABSENT) {
                AiPlayerManager.requestRemove(runtime.server());
            }
        }
    }

    private enum EndVictoryStage {
        BODY,
        ENTERING_END,
        SETTLING_END,
        PROBE,
        VISIBLE,
        GOAL,
        FIGHT_SKILL,
        FIGHTING,
        RETURNING,
        DONE
    }

    private static EndVictoryArena prepareEndVictoryArena(
            final GameTestHelper helper,
            final ServerRuntime runtime,
            final ServerPlayer body,
            final boolean equipBody,
            final boolean recenterBody
    ) {
        final var end = runtime.server().getLevel(Level.END);
        helper.assertTrue(end != null, "End level is unavailable");
        final BlockPos arena = body.blockPosition();
        for (int x = -20; x <= 20; x++) {
            for (int z = -20; z <= 20; z++) {
                end.setBlockAndUpdate(
                        arena.offset(x, -1, z),
                        Blocks.OBSIDIAN.defaultBlockState()
                );
                for (int y = 0; y <= 10; y++) {
                    end.setBlockAndUpdate(
                            arena.offset(x, y, z),
                            Blocks.AIR.defaultBlockState()
                    );
                }
            }
        }
        end.getEntities(
                EntityTypes.END_CRYSTAL,
                existing -> true
        ).forEach(Entity::discard);

        if (recenterBody) {
            body.teleportTo(
                    arena.getX() + 0.5D,
                    arena.getY(),
                    arena.getZ() + 0.5D
            );
        }
        body.setGameMode(GameType.SURVIVAL);
        if (equipBody) {
            equipEndVictoryBody(body, false);
        }
        body.setHealth(body.getMaxHealth());
        body.getFoodData().setFoodLevel(20);
        body.setDeltaMovement(Vec3.ZERO);
        body.fallDistance = 0.0F;
        body.inventoryMenu.broadcastChanges();

        final EndCrystal crystal = EntityTypes.END_CRYSTAL.create(
                end,
                EntitySpawnReason.COMMAND
        );
        helper.assertTrue(
                crystal != null,
                "End-victory fixture could not create crystal"
        );
        crystal.setPos(
                arena.getX() + 0.5D,
                arena.getY(),
                arena.getZ() + 7.5D
        );
        helper.assertTrue(
                end.addFreshEntity(crystal),
                "End-victory fixture could not add crystal"
        );
        final BlockPos cageBar = arena.offset(0, 1, 4);
        end.setBlockAndUpdate(
                cageBar,
                Blocks.IRON_BARS.defaultBlockState()
        );

        final List<? extends EnderDragon> dragons =
                end.getEntities(
                        EntityTypes.ENDER_DRAGON,
                        existing -> true
                );
        final EnderDragon dragon;
        if (dragons.isEmpty()) {
            dragon = EntityTypes.ENDER_DRAGON.create(
                    end,
                    EntitySpawnReason.COMMAND
            );
            helper.assertTrue(
                    dragon != null,
                    "End-victory fixture could not create dragon"
            );
            helper.assertTrue(
                    end.addFreshEntity(dragon),
                    "End-victory fixture could not add dragon"
            );
        } else {
            dragon = dragons.getFirst();
            dragons.stream()
                    .skip(1)
                    .forEach(Entity::discard);
        }
        dragon.setPos(
                arena.getX() + 0.5D,
                arena.getY() + 2.0D,
                arena.getZ() + 4.5D
        );
        dragon.setYRot(180.0F);
        dragon.setNoAi(true);
        dragon.setHealth(dragon.getMaxHealth());
        positionStaticDragonParts(helper, dragon);

        final BlockPos returnPortalCenter =
                arena.offset(0, 0, -10);
        /* The vanilla return portal activates only after dragon death. */
        clearEndReturnPortal(end, returnPortalCenter);
        body.lookAt(
                EntityAnchorArgument.Anchor.EYES,
                crystal.getEyePosition()
        );
        body.setYHeadRot(body.getYRot());
        return new EndVictoryArena(
                crystal,
                dragon,
                cageBar,
                returnPortalCenter
        );
    }

    private static void clearEndReturnPortal(
            final Level end,
            final BlockPos center
    ) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                end.setBlockAndUpdate(
                        center.offset(x, -1, z),
                        Blocks.OBSIDIAN.defaultBlockState()
                );
                end.setBlockAndUpdate(
                        center.offset(x, 0, z),
                        Blocks.AIR.defaultBlockState()
                );
            }
        }
    }

    private static void activateEndReturnPortal(
            final Level end,
            final BlockPos center
    ) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                end.setBlockAndUpdate(
                        center.offset(x, -1, z),
                        Blocks.OBSIDIAN.defaultBlockState()
                );
                end.setBlockAndUpdate(
                        center.offset(x, 0, z),
                        Blocks.END_PORTAL.defaultBlockState()
                );
            }
        }
    }

    private static void equipEndVictoryBody(
            final ServerPlayer body,
            final boolean includeEnderEyes
    ) {
        body.getInventory().clearContent();
        int slot = 0;
        if (includeEnderEyes) {
            body.getInventory().setItem(
                    slot++,
                    new ItemStack(Items.ENDER_EYE, 12)
            );
        }
        body.getInventory().setItem(
                slot++,
                new ItemStack(Items.BOW)
        );
        body.getInventory().setItem(
                slot++,
                new ItemStack(Items.ARROW, 64)
        );
        body.getInventory().setItem(
                slot++,
                new ItemStack(Items.COOKED_BEEF, 16)
        );
        body.getInventory().setItem(
                slot++,
                new ItemStack(Items.DIAMOND_SWORD)
        );
        body.getInventory().setItem(
                slot++,
                new ItemStack(Items.DIAMOND_PICKAXE)
        );
        body.getInventory().setItem(
                slot++,
                new ItemStack(Items.COBBLESTONE, 64)
        );
        body.getInventory().setItem(
                slot,
                new ItemStack(Items.WATER_BUCKET)
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
    }

    private static void positionStaticDragonParts(
            final GameTestHelper helper,
            final EnderDragon target
    ) {
        final EnderDragonPart[] parts = target.getSubEntities();
        helper.assertTrue(
                parts.length == 8,
                "Unexpected Ender Dragon part count "
                    + parts.length
        );
        final double x = target.getX();
        final double y = target.getY();
        final double z = target.getZ();
        parts[0].setPos(x, y - 1.0D, z + 6.5D);
        parts[1].setPos(x, y - 1.0D, z + 5.5D);
        parts[2].setPos(x, y, z + 0.5D);
        parts[3].setPos(x, y + 1.5D, z - 3.5D);
        parts[4].setPos(x, y + 1.5D, z - 5.5D);
        parts[5].setPos(x, y + 1.5D, z - 7.5D);
        parts[6].setPos(x - 4.5D, y + 2.0D, z);
        parts[7].setPos(x + 4.5D, y + 2.0D, z);
    }

    private record EndVictoryArena(
            EndCrystal crystal,
            EnderDragon dragon,
            BlockPos cageBar,
            BlockPos returnPortalCenter
    ) {
    }

    private static final class LiveEnderPearlScenario {
        private static final int REQUIRED_ENDER_PEARLS = 14;
        private static final double CONTROLLED_ENDERMAN_OFFSET = 2.5D;
        private static final double MAXIMUM_FIXTURE_MELEE_DISTANCE = 2.81D;
        private static final long SIMULATION_TIMEOUT_NANOS =
                java.time.Duration.ofSeconds(30).toNanos();
        private static final long ACQUISITION_TIMEOUT_NANOS =
                java.time.Duration.ofMinutes(10).toNanos();
        private static final long ASYNC_CHUNK_YIELD_NANOS = 1_000_000L;

        private final GameTestHelper helper;
        private final ServerRuntime runtime;
        private final long createdAt;

        private EnderPearlStage stage = EnderPearlStage.BODY;
        private CompletableFuture<CapabilityProbeOutcome> probe;
        private PlacedHuman humanSession;
        private BlockPos arenaOrigin;
        private Mob enderman;
        private UUID bodyId;
        private long goalRevisionBefore;
        private long goalRevision;
        private long stageStartedNanos;
        private long roofStableSince = -1L;
        private long targetRemovedAt = -1L;
        private int enderTargetsSpawned;
        private int swordDamageBefore;
        private boolean reserveSkillObserved;

        private LiveEnderPearlScenario(
                final GameTestHelper helper,
                final ServerRuntime runtime
        ) {
            this.helper = helper;
            this.runtime = runtime;
            createdAt = helper.getTick();
            stageStartedNanos = System.nanoTime();
        }

        private void start() {
            finishScenarioGoal(runtime);
            final var status = AiPlayerManager.status(runtime.server());
            if (status.state() == SessionState.ABSENT) {
                helper.assertTrue(
                        AiPlayerManager.requestSpawn(
                                runtime.server()
                        ).accepted(),
                        "Ender-route companion spawn was rejected"
                );
            }
        }

        private void tick() {
            if (humanSession != null) {
                humanSession.tick();
            }
            switch (stage) {
                case BODY -> waitForBody();
                case SIMULATION -> waitForNetherSimulation();
                case PROBE -> waitForProbe();
                case GOAL -> waitForGoal();
                case SKILL -> waitForReserveSkill();
                case ACQUIRE -> waitForReserve();
                case DONE -> {
                    // GameTest is already terminal.
                }
            }
        }

        private void waitForBody() {
            final var status = AiPlayerManager.status(runtime.server());
            helper.assertTrue(
                    status.state() != SessionState.FAILED,
                    "Ender-route companion body failed"
            );
            if (status.state() != SessionState.ACTIVE
                    || !status.online()) {
                helper.assertTrue(
                        helper.getTick() - createdAt
                            <= BODY_TIMEOUT_TICKS,
                        "Ender-route companion body timed out"
                );
                return;
            }
            final ServerPlayer body = body();
            bodyId = body.getUUID();
            prepareNetherArena(body);
            stage = EnderPearlStage.SIMULATION;
            stageStartedNanos = System.nanoTime();
        }

        /**
         * Pre-command fixture setup represents the already-completed Blaze
         * route. After ordinary player chat, this test never moves the body,
         * edits its inventory, places the safety roof, or awards a pearl.
         */
        private void prepareNetherArena(final ServerPlayer body) {
            final var nether = runtime.server().getLevel(Level.NETHER);
            helper.assertTrue(
                    nether != null,
                    "Live Ender gate could not access the Nether"
            );
            final BlockPos reference =
                    helper.absolutePos(new BlockPos(12, 4, 12));
            arenaOrigin = new BlockPos(
                    reference.getX(),
                    64,
                    reference.getZ()
            );
            for (int x = -4; x <= 4; x++) {
                for (int z = -4; z <= 10; z++) {
                    final BlockPos column =
                            arenaOrigin.offset(x, 0, z);
                    nether.setBlockAndUpdate(
                            column.below(),
                            Blocks.NETHER_BRICKS.defaultBlockState()
                    );
                    for (int y = 0; y <= 5; y++) {
                        nether.setBlockAndUpdate(
                                column.above(y),
                                Blocks.AIR.defaultBlockState()
                        );
                    }
                }
            }
            body.setGameMode(GameType.SURVIVAL);
            body.getInventory().clearContent();
            body.getEnderChestInventory().clearContent();
            body.removeAllEffects();
            body.clearFire();
            body.getInventory().setItem(
                    0,
                    new ItemStack(Items.DIAMOND_SWORD)
            );
            body.getInventory().setItem(
                    1,
                    new ItemStack(Items.DIAMOND_PICKAXE)
            );
            body.getInventory().setItem(
                    2,
                    new ItemStack(Items.CRAFTING_TABLE)
            );
            body.getInventory().setItem(
                    3,
                    new ItemStack(Items.OAK_LOG, 8)
            );
            body.getInventory().setItem(
                    4,
                    new ItemStack(Items.COOKED_BEEF, 16)
            );
            body.getInventory().setItem(
                    5,
                    new ItemStack(Items.COBBLESTONE, 64)
            );
            body.getInventory().setItem(
                    6,
                    new ItemStack(Items.WATER_BUCKET)
            );
            body.getInventory().setItem(
                    7,
                    new ItemStack(Items.BLAZE_ROD, 7)
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
            body.teleportTo(
                    nether,
                    arenaOrigin.getX() + 0.5D,
                    arenaOrigin.getY(),
                    arenaOrigin.getZ() + 0.5D,
                    java.util.Set.of(),
                    0.0F,
                    0.0F,
                    false
            );
            body.setDeltaMovement(Vec3.ZERO);
            body.fallDistance = 0.0F;
            body.inventoryMenu.broadcastChanges();
            swordDamageBefore =
                    body.getMainHandItem().getDamageValue();
            enderTargetsSpawned = 0;
            reserveSkillObserved = false;
            roofStableSince = -1L;
            targetRemovedAt = -1L;
        }

        private void waitForNetherSimulation() {
            final ServerPlayer body = body();
            helper.assertTrue(
                    body.level().dimension().equals(Level.NETHER),
                    "Ender-route body left the Nether before the command"
            );
            if (!body.level().isPositionEntityTicking(
                    body.blockPosition()
            ) || !body.onGround()) {
                java.util.concurrent.locks.LockSupport.parkNanos(
                        ASYNC_CHUNK_YIELD_NANOS
                );
                helper.assertTrue(
                        System.nanoTime() - stageStartedNanos
                            <= SIMULATION_TIMEOUT_NANOS,
                        "Headless player's ordinary Nether chunk ticket "
                            + "did not become entity-ticking"
                );
                return;
            }
            probe = probeOrReuseVerifiedModel(runtime);
            stage = EnderPearlStage.PROBE;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForProbe() {
            assertWithinModelDeadline(
                    "Ender-route model capability probe timed out"
            );
            if (!probe.isDone()) {
                return;
            }
            final CapabilityProbeOutcome outcome = probe.join();
            helper.assertTrue(
                    outcome
                        instanceof CapabilityProbeOutcome.Supported,
                    "Configured live model probe failed: " + outcome
            );
            humanSession = PlacedHuman.create(helper, runtime);
            final ServerPlayer human = humanSession.player();
            helper.assertTrue(
                    CompanionCommandAccess.mayAdmin(
                            human.createCommandSourceStack()
                    ),
                    "Logged-in Ender-route player lacked task permission"
            );
            goalRevisionBefore = runtime.goals().snapshot().revision();
            final String request = runtime.worldData().displayName()
                    + "，请继续通关Minecraft。烈焰棒已经准备好了，"
                    + "请正常搭建末影人安全屋顶、战斗和拾取，"
                    + "收集至少14颗末影珍珠。不要使用指令，"
                    + "也不要等我再次提醒。";
            final Component submitted =
                    ForgeHooks.onServerChatSubmittedEvent(
                            human,
                            Component.literal(request)
                    );
            helper.assertTrue(
                    submitted != null,
                    "Companion cancelled the Ender-route chat command"
            );
            humanSession.close();
            humanSession = null;
            stage = EnderPearlStage.GOAL;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForGoal() {
            assertNoHumanPlayers();
            assertWithinModelDeadline(
                    "Live model did not classify the Ender-route task"
            );
            final GoalSnapshot goal = runtime.goals().snapshot();
            if (goal.revision() == goalRevisionBefore) {
                return;
            }
            helper.assertTrue(
                    goal.revision() > goalRevisionBefore
                        && goal.status() == GoalStatus.RUNNING
                        && goal.goal().contains("通关Minecraft"),
                    "Ender-route chat did not become a running "
                        + "completion goal: " + goal
            );
            goalRevision = goal.revision();
            stage = EnderPearlStage.SKILL;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForReserveSkill() {
            assertNoHumanPlayers();
            final var snapshot =
                    runtime.skillSupervisor().snapshot();
            if ("secure_ender_pearl_reserve".equals(
                    snapshot.skillName()
            ) && snapshot.boundGoalRevision() == goalRevision) {
                reserveSkillObserved = true;
                stage = EnderPearlStage.ACQUIRE;
                stageStartedNanos = System.nanoTime();
                return;
            }
            if (snapshot.boundGoalRevision() == goalRevision
                    && snapshot.state()
                        == SkillSupervisor.State.RUNNING) {
                helper.fail(
                        "Live model selected the wrong Ender-route skill: "
                            + diagnostics()
                );
                return;
            }
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= MODEL_TIMEOUT_NANOS,
                    "Live model did not select the durable pearl reserve: "
                        + diagnostics()
            );
        }

        private void waitForReserve() {
            assertNoHumanPlayers();
            final ServerPlayer body = body();
            helper.assertTrue(
                    bodyId.equals(body.getUUID())
                        && body.level().dimension().equals(Level.NETHER)
                        && body.isAlive()
                        && !body.isDeadOrDying(),
                    "Pearl reserve lost the original living Nether body: "
                        + diagnostics()
            );
            final int pearls =
                    body.getInventory().countItem(Items.ENDER_PEARL);
            final boolean targetGone = enderman == null
                    || enderman.isRemoved()
                    || !enderman.isAlive();
            if (enderman != null
                    && targetGone
                    && targetRemovedAt < 0L) {
                targetRemovedAt = helper.getTick();
            }
            final boolean roofComplete = roofComplete();
            final boolean pillarRemoved = temporaryPillarRemoved();
            final boolean bodyCentered = bodyCenteredUnderRoof(body);
            final boolean observedRoof = runtime.coreFrames()
                    .current()
                    .map(SecureEnderPearlReserveSkill
                            ::hasObservedSafetyRoof)
                    .orElse(false);
            if (roofComplete
                    && pillarRemoved
                    && bodyCentered
                    && observedRoof) {
                if (roofStableSince < 0L) {
                    roofStableSince = helper.getTick();
                }
            } else {
                roofStableSince = -1L;
            }
            final boolean pearlDropPresent = body.level()
                    .getEntitiesOfClass(
                            ItemEntity.class,
                            body.getBoundingBox().inflate(16.0D)
                    )
                    .stream()
                    .anyMatch(drop ->
                            drop.getItem().is(Items.ENDER_PEARL)
                                && drop.isAlive()
                                && !drop.isRemoved()
                    );
            final boolean targetCooldownComplete =
                    enderman == null
                        || targetRemovedAt >= 0L
                            && helper.getTick() - targetRemovedAt >= 2L;
            if (pearls < REQUIRED_ENDER_PEARLS
                    && targetGone
                    && !pearlDropPresent
                    && bodyCentered
                    && roofStableSince >= 0L
                    && helper.getTick() - roofStableSince >= 20L
                    && targetCooldownComplete) {
                spawnControlledEnderman();
            }

            final var snapshot =
                    runtime.skillSupervisor().snapshot();
            if (pearls > 0
                    && !bodyCentered
                    && helper.getTick() % 20L == 0L) {
                dev.mcai.companion.MinecraftAiCompanion.LOGGER.info(
                        "Live Ender return diagnostic: {}",
                        diagnostics()
                );
            }
            if (snapshot.executedTicks() > 0L
                    && snapshot.executedTicks() % 400L == 0L) {
                dev.mcai.companion.MinecraftAiCompanion.LOGGER.info(
                        "Live Ender reserve progress: {}",
                        diagnostics()
                );
            }
            if ("secure_ender_pearl_reserve".equals(
                    snapshot.skillName()
            ) && snapshot.state()
                    == SkillSupervisor.State.FAILED) {
                helper.fail(
                        "Live-model pearl reserve failed: "
                            + diagnostics()
                );
                return;
            }
            final boolean milestone = runtime.worldData()
                    .verifiedRouteProgress(goalRevision)
                    .milestones()
                    .contains(
                            SurvivalMilestone.ENDER_PEARL_OBTAINED
                    );
            if (pearls >= REQUIRED_ENDER_PEARLS
                    && milestone
                    && "secure_ender_pearl_reserve".equals(
                        snapshot.skillName()
                    )
                    && snapshot.state()
                        == SkillSupervisor.State.COMPLETED) {
                helper.assertTrue(
                        reserveSkillObserved
                            && enderTargetsSpawned >= 7
                            && roofComplete
                            && pillarRemoved
                            && body.getMainHandItem()
                                .is(Items.DIAMOND_SWORD)
                            && body.getMainHandItem()
                                .getDamageValue()
                                    > swordDamageBefore,
                        "Pearl reserve appeared without roof construction, "
                            + "repeated combat, and durability: "
                            + diagnostics()
                );
                stage = EnderPearlStage.DONE;
                helper.succeed();
                return;
            }
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= ACQUISITION_TIMEOUT_NANOS,
                    "Live-model pearl reserve timed out: "
                        + diagnostics()
            );
        }

        private void spawnControlledEnderman() {
            final ServerPlayer body = body();
            final Mob next = EntityTypes.ENDERMAN.create(
                    body.level(),
                    EntitySpawnReason.COMMAND
            );
            helper.assertTrue(
                    next != null,
                    "Could not create the controlled live-model Enderman"
            );
            next.setPos(
                    arenaOrigin.getX() + 0.5D,
                    arenaOrigin.getY(),
                    arenaOrigin.getZ()
                        + CONTROLLED_ENDERMAN_OFFSET
            );
            /*
             * This controlled target has no AI, so it cannot close the last
             * fraction of a block in response to the production lure. It
             * remains outside the roof while reachable from any accepted
             * centre-docking position.
             */
            final double targetDistance = Math.hypot(
                    next.getX() - body.getX(),
                    next.getZ() - body.getZ()
            );
            helper.assertTrue(
                    CONTROLLED_ENDERMAN_OFFSET > 1.5D
                        && targetDistance
                            <= MAXIMUM_FIXTURE_MELEE_DISTANCE,
                    "Controlled live-model Enderman fixture is outside "
                        + "reachable sheltered melee geometry"
            );
            next.setNoAi(true);
            next.setHealth(5.0F);
            next.setPersistenceRequired();
            next.setItemSlot(
                    EquipmentSlot.MAINHAND,
                    new ItemStack(Items.ENDER_PEARL)
            );
            next.setGuaranteedDrop(EquipmentSlot.MAINHAND);
            helper.assertTrue(
                    body.level().addFreshEntity(next),
                    "Could not add the controlled live-model Enderman"
            );
            enderman = next;
            enderTargetsSpawned++;
            targetRemovedAt = -1L;
        }

        private boolean roofComplete() {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (!body().level().getBlockState(
                            arenaOrigin.offset(x, 2, z)
                    ).is(Blocks.COBBLESTONE)) {
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean temporaryPillarRemoved() {
            final int[][] offsets = {
                    {1, 0},
                    {-1, 0},
                    {0, 1},
                    {0, -1}
            };
            for (int[] offset : offsets) {
                for (int y = 0; y <= 1; y++) {
                    if (body().level().getBlockState(
                            arenaOrigin.offset(
                                    offset[0],
                                    y,
                                    offset[1]
                            )
                    ).is(Blocks.COBBLESTONE)) {
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean bodyCenteredUnderRoof(
                final ServerPlayer body
        ) {
            return body.onGround()
                    && Math.abs(
                            body.getY() - arenaOrigin.getY()
                    ) <= 0.05D
                    && Math.hypot(
                            body.getX()
                                - (arenaOrigin.getX() + 0.5D),
                            body.getZ()
                                - (arenaOrigin.getZ() + 0.5D)
                    ) <= 0.30D;
        }

        private ServerPlayer body() {
            return AiPlayerManager.onlinePlayer(runtime.server())
                    .orElseThrow(() -> helper.assertionException(
                            "Ender-route companion body disappeared"
                    ));
        }

        private void assertNoHumanPlayers() {
            final long humans = runtime.server()
                    .getPlayerList()
                    .getPlayers()
                    .stream()
                    .filter(player -> !player.getUUID().equals(
                            runtime.worldData().companionUuid()
                    ))
                    .count();
            helper.assertTrue(
                    humans == 0L,
                    "Ender-route autonomy retained " + humans
                        + " human player(s)"
            );
        }

        private void assertWithinModelDeadline(
                final String message
        ) {
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= MODEL_TIMEOUT_NANOS,
                    message
            );
        }

        private String diagnostics() {
            final ServerPlayer body = body();
            return "supervisor=" + runtime.skillSupervisor().snapshot()
                    + ", goal=" + runtime.goals().snapshot()
                    + ", dimension="
                    + body.level().dimension().identifier()
                    + ", body=" + body.position()
                    + ", velocity=" + body.getDeltaMovement()
                    + ", input=" + body.getLastClientInput()
                    + ", usingItem=" + body.isUsingItem()
                    + ", useItem=" + body.getUseItem()
                    + ", yaw=" + body.getYRot()
                    + ", pitch=" + body.getXRot()
                    + ", coreLease=" + runtime.coreActions().snapshot()
                    + ", arbiter=" + runtime.behaviorArbiter().latest()
                    + ", survival=" + runtime.survival().state()
                    + ", health=" + body.getHealth()
                    + ", pearls="
                    + body.getInventory().countItem(Items.ENDER_PEARL)
                    + ", spawned=" + enderTargetsSpawned
                    + ", roof=" + roofComplete()
                    + ", pillarRemoved="
                    + temporaryPillarRemoved()
                    + ", target="
                    + (enderman == null
                        ? "none"
                        : enderman.getUUID() + "/alive="
                            + enderman.isAlive() + "/health="
                            + enderman.getHealth())
                    + ", skillObserved=" + reserveSkillObserved
                    + ", milestones=" + runtime.worldData()
                        .verifiedRouteProgress(goalRevision)
                        .milestones();
        }

        private void cleanup() {
            finishScenarioGoal(runtime);
            if (humanSession != null) {
                humanSession.close();
            }
            if (enderman != null && !enderman.isRemoved()) {
                enderman.discard();
            }
            if (AiPlayerManager.status(runtime.server()).state()
                    != SessionState.ABSENT) {
                AiPlayerManager.requestRemove(runtime.server());
            }
        }
    }

    private enum EnderPearlStage {
        BODY,
        SIMULATION,
        PROBE,
        GOAL,
        SKILL,
        ACQUIRE,
        DONE
    }

    private static final class LiveNetherBlazeScenario {
        private static final int REQUIRED_BLAZE_RODS = 7;
        private static final long SIMULATION_TIMEOUT_NANOS =
                java.time.Duration.ofSeconds(30).toNanos();
        private static final long ACQUISITION_TIMEOUT_NANOS =
                java.time.Duration.ofMinutes(3).toNanos();
        private static final long ASYNC_CHUNK_YIELD_NANOS = 1_000_000L;

        private final GameTestHelper helper;
        private final ServerRuntime runtime;
        private final long createdAt;

        private NetherBlazeStage stage = NetherBlazeStage.BODY;
        private CompletableFuture<CapabilityProbeOutcome> probe;
        private PlacedHuman humanSession;
        private BlockPos arenaOrigin;
        private Mob blaze;
        private UUID bodyId;
        private long goalRevisionBefore;
        private long goalRevision;
        private long stageStartedNanos;
        private int blazeTargetsSpawned;
        private int swordDamageBefore;
        private boolean reserveSkillObserved;

        private LiveNetherBlazeScenario(
                final GameTestHelper helper,
                final ServerRuntime runtime
        ) {
            this.helper = helper;
            this.runtime = runtime;
            createdAt = helper.getTick();
            stageStartedNanos = System.nanoTime();
        }

        private void start() {
            finishScenarioGoal(runtime);
            final var status = AiPlayerManager.status(runtime.server());
            if (status.state() == SessionState.ABSENT) {
                helper.assertTrue(
                        AiPlayerManager.requestSpawn(
                                runtime.server()
                        ).accepted(),
                        "Blaze-route companion spawn was rejected"
                );
            }
        }

        private void tick() {
            if (humanSession != null) {
                humanSession.tick();
            }
            switch (stage) {
                case BODY -> waitForBody();
                case SIMULATION -> waitForNetherSimulation();
                case VISIBLE -> waitForVisibleBlaze();
                case PROBE -> waitForProbe();
                case GOAL -> waitForGoal();
                case SKILL -> waitForReserveSkill();
                case ACQUIRE -> waitForReserve();
                case DONE -> {
                    // GameTest is already terminal.
                }
            }
        }

        private void waitForBody() {
            final var status = AiPlayerManager.status(runtime.server());
            helper.assertTrue(
                    status.state() != SessionState.FAILED,
                    "Blaze-route companion body failed"
            );
            if (status.state() != SessionState.ACTIVE
                    || !status.online()) {
                helper.assertTrue(
                        helper.getTick() - createdAt
                            <= BODY_TIMEOUT_TICKS,
                        "Blaze-route companion body timed out"
                );
                return;
            }
            final ServerPlayer body = body();
            bodyId = body.getUUID();
            prepareNetherArena(body);
            stage = NetherBlazeStage.SIMULATION;
            stageStartedNanos = System.nanoTime();
        }

        /**
         * This is pre-command fixture setup. It establishes only a controlled
         * already-reached Nether checkpoint; after the player message, the
         * test never moves the body, edits inventory, or awards a route item.
         */
        private void prepareNetherArena(final ServerPlayer body) {
            final var nether = runtime.server().getLevel(Level.NETHER);
            helper.assertTrue(
                    nether != null,
                    "Live Blaze gate could not access the Nether"
            );
            final BlockPos reference =
                    helper.absolutePos(new BlockPos(12, 4, 12));
            arenaOrigin = new BlockPos(
                    reference.getX(),
                    64,
                    reference.getZ()
            );
            for (int x = -4; x <= 4; x++) {
                for (int z = -4; z <= 10; z++) {
                    final BlockPos column =
                            arenaOrigin.offset(x, 0, z);
                    nether.setBlockAndUpdate(
                            column.below(),
                            Blocks.NETHER_BRICKS.defaultBlockState()
                    );
                    for (int y = 0; y <= 5; y++) {
                        nether.setBlockAndUpdate(
                                column.above(y),
                                Blocks.AIR.defaultBlockState()
                        );
                    }
                }
            }
            body.setGameMode(GameType.SURVIVAL);
            body.getInventory().clearContent();
            body.getEnderChestInventory().clearContent();
            body.removeAllEffects();
            body.clearFire();
            body.getInventory().setItem(
                    0,
                    new ItemStack(Items.DIAMOND_SWORD)
            );
            body.getInventory().setItem(
                    1,
                    new ItemStack(Items.DIAMOND_PICKAXE)
            );
            body.getInventory().setItem(
                    2,
                    new ItemStack(Items.CRAFTING_TABLE)
            );
            body.getInventory().setItem(
                    3,
                    new ItemStack(Items.OAK_LOG, 8)
            );
            body.getInventory().setItem(
                    4,
                    new ItemStack(Items.COOKED_BEEF, 16)
            );
            body.getInventory().setItem(
                    5,
                    new ItemStack(Items.COBBLESTONE, 64)
            );
            body.getInventory().setItem(
                    6,
                    new ItemStack(Items.WATER_BUCKET)
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
            body.teleportTo(
                    nether,
                    arenaOrigin.getX() + 0.5D,
                    arenaOrigin.getY(),
                    arenaOrigin.getZ() + 0.5D,
                    java.util.Set.of(),
                    0.0F,
                    0.0F,
                    false
            );
            body.setDeltaMovement(Vec3.ZERO);
            body.fallDistance = 0.0F;
            body.inventoryMenu.broadcastChanges();
            swordDamageBefore =
                    body.getMainHandItem().getDamageValue();
            blazeTargetsSpawned = 0;
            reserveSkillObserved = false;
        }

        private void waitForNetherSimulation() {
            final ServerPlayer body = body();
            helper.assertTrue(
                    body.level().dimension().equals(Level.NETHER),
                    "Blaze-route body left the Nether before the command"
            );
            if (!body.level().isPositionEntityTicking(
                    body.blockPosition()
            ) || !body.onGround()) {
                java.util.concurrent.locks.LockSupport.parkNanos(
                        ASYNC_CHUNK_YIELD_NANOS
                );
                helper.assertTrue(
                        System.nanoTime() - stageStartedNanos
                            <= SIMULATION_TIMEOUT_NANOS,
                        "Headless player's ordinary Nether chunk ticket "
                            + "did not become entity-ticking"
                );
                return;
            }
            spawnControlledBlaze();
            stage = NetherBlazeStage.VISIBLE;
            stageStartedNanos = System.nanoTime();
        }

        private void spawnControlledBlaze() {
            final ServerPlayer body = body();
            final Mob next = EntityTypes.BLAZE.create(
                    body.level(),
                    EntitySpawnReason.COMMAND
            );
            helper.assertTrue(
                    next != null,
                    "Could not create the controlled live-model Blaze"
            );
            final double zOffset =
                    blazeTargetsSpawned % 2 == 0 ? 6.5D : 0.5D;
            next.setPos(
                    arenaOrigin.getX() + 0.5D,
                    arenaOrigin.getY(),
                    arenaOrigin.getZ() + zOffset
            );
            next.setNoAi(true);
            next.setHealth(5.0F);
            next.setPersistenceRequired();
            next.setItemSlot(
                    EquipmentSlot.MAINHAND,
                    new ItemStack(Items.BLAZE_ROD)
            );
            next.setGuaranteedDrop(EquipmentSlot.MAINHAND);
            helper.assertTrue(
                    body.level().addFreshEntity(next),
                    "Could not add the controlled live-model Blaze"
            );
            blaze = next;
            blazeTargetsSpawned++;
            body.lookAt(
                    EntityAnchorArgument.Anchor.EYES,
                    next.getEyePosition()
            );
            body.setYHeadRot(body.getYRot());
        }

        private void waitForVisibleBlaze() {
            final ServerPlayer body = body();
            body.lookAt(
                    EntityAnchorArgument.Anchor.EYES,
                    blaze.getEyePosition()
            );
            body.setYHeadRot(body.getYRot());
            final var frame = runtime.coreFrames().current();
            if (frame.isEmpty()
                    || !frame.orElseThrow().dimension()
                        .equals(dev.mcai.companion.waypoint.DimensionRef.NETHER)
                    || frame.orElseThrow().visibleEntities().stream()
                        .noneMatch(entity ->
                                entity.entityId().equals(blaze.getUUID())
                                    && "minecraft:blaze".equals(
                                        entity.entityTypeId()
                                    ))) {
                helper.assertTrue(
                        System.nanoTime() - stageStartedNanos
                            <= SIMULATION_TIMEOUT_NANOS,
                        "Controlled Blaze never became current "
                            + "first-person evidence"
                );
                return;
            }
            probe = probeOrReuseVerifiedModel(runtime);
            stage = NetherBlazeStage.PROBE;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForProbe() {
            assertWithinModelDeadline(
                    "Blaze-route model capability probe timed out"
            );
            if (!probe.isDone()) {
                return;
            }
            final CapabilityProbeOutcome outcome = probe.join();
            helper.assertTrue(
                    outcome
                        instanceof CapabilityProbeOutcome.Supported,
                    "Configured live model probe failed: " + outcome
            );
            humanSession = PlacedHuman.create(helper, runtime);
            final ServerPlayer human = humanSession.player();
            helper.assertTrue(
                    CompanionCommandAccess.mayAdmin(
                            human.createCommandSourceStack()
                    ),
                    "Logged-in Blaze-route player lacked task permission"
            );
            goalRevisionBefore = runtime.goals().snapshot().revision();
            final String request = runtime.worldData().displayName()
                    + "，请继续通关Minecraft。你已经在下界了，"
                    + "请从眼前的烈焰人开始，正常战斗和拾取，"
                    + "收集至少7根烈焰棒作为通关储备。"
                    + "不要使用指令，也不要等我再次提醒。";
            final Component submitted =
                    ForgeHooks.onServerChatSubmittedEvent(
                            human,
                            Component.literal(request)
                    );
            helper.assertTrue(
                    submitted != null,
                    "Companion cancelled the Blaze-route chat command"
            );
            humanSession.close();
            humanSession = null;
            stage = NetherBlazeStage.GOAL;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForGoal() {
            assertNoHumanPlayers();
            assertWithinModelDeadline(
                    "Live model did not classify the Blaze-route task"
            );
            final GoalSnapshot goal = runtime.goals().snapshot();
            if (goal.revision() == goalRevisionBefore) {
                return;
            }
            helper.assertTrue(
                    goal.revision() > goalRevisionBefore
                        && goal.status() == GoalStatus.RUNNING
                        && goal.goal().contains("通关Minecraft"),
                    "Blaze-route chat did not become a running "
                        + "completion goal: " + goal
            );
            goalRevision = goal.revision();
            stage = NetherBlazeStage.SKILL;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForReserveSkill() {
            assertNoHumanPlayers();
            final var snapshot =
                    runtime.skillSupervisor().snapshot();
            if ("secure_nether_blaze_material".equals(
                    snapshot.skillName()
            ) && snapshot.boundGoalRevision() == goalRevision) {
                reserveSkillObserved = true;
                stage = NetherBlazeStage.ACQUIRE;
                stageStartedNanos = System.nanoTime();
                return;
            }
            if (snapshot.boundGoalRevision() == goalRevision
                    && snapshot.state()
                        == SkillSupervisor.State.RUNNING) {
                helper.fail(
                        "Live model selected the wrong Blaze-route skill: "
                            + diagnostics()
                );
                return;
            }
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= MODEL_TIMEOUT_NANOS,
                    "Live model did not select the durable Blaze reserve: "
                        + diagnostics()
            );
        }

        private void waitForReserve() {
            assertNoHumanPlayers();
            final ServerPlayer body = body();
            helper.assertTrue(
                    bodyId.equals(body.getUUID())
                        && body.level().dimension().equals(Level.NETHER)
                        && body.isAlive()
                        && !body.isDeadOrDying(),
                    "Blaze reserve lost the original living Nether body: "
                        + diagnostics()
            );
            final int rods =
                    body.getInventory().countItem(Items.BLAZE_ROD);
            if ((blaze.isRemoved() || !blaze.isAlive())
                    && rods < REQUIRED_BLAZE_RODS
                    && rods >= blazeTargetsSpawned) {
                spawnControlledBlaze();
            }

            final var snapshot =
                    runtime.skillSupervisor().snapshot();
            if ("secure_nether_blaze_material".equals(
                    snapshot.skillName()
            ) && snapshot.state()
                    == SkillSupervisor.State.FAILED) {
                helper.fail(
                        "Live-model Blaze reserve failed: "
                            + diagnostics()
                );
                return;
            }
            final boolean milestone = runtime.worldData()
                    .verifiedRouteProgress(goalRevision)
                    .milestones()
                    .contains(
                            SurvivalMilestone.BLAZE_MATERIAL_OBTAINED
                    );
            if (rods >= REQUIRED_BLAZE_RODS
                    && milestone
                    && "secure_nether_blaze_material".equals(
                        snapshot.skillName()
                    )
                    && snapshot.state()
                        == SkillSupervisor.State.COMPLETED) {
                helper.assertTrue(
                        reserveSkillObserved
                            && blazeTargetsSpawned >= 4
                            && body.getMainHandItem()
                                .is(Items.DIAMOND_SWORD)
                            && body.getMainHandItem()
                                .getDamageValue()
                                    > swordDamageBefore,
                        "Blaze material appeared without repeated ordinary "
                            + "combat and durability: " + diagnostics()
                );
                stage = NetherBlazeStage.DONE;
                helper.succeed();
                return;
            }
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= ACQUISITION_TIMEOUT_NANOS,
                    "Live-model Blaze reserve timed out: "
                        + diagnostics()
            );
        }

        private ServerPlayer body() {
            return AiPlayerManager.onlinePlayer(runtime.server())
                    .orElseThrow(() -> helper.assertionException(
                            "Blaze-route companion body disappeared"
                    ));
        }

        private void assertNoHumanPlayers() {
            final long humans = runtime.server()
                    .getPlayerList()
                    .getPlayers()
                    .stream()
                    .filter(player -> !player.getUUID().equals(
                            runtime.worldData().companionUuid()
                    ))
                    .count();
            helper.assertTrue(
                    humans == 0L,
                    "Blaze-route autonomy retained " + humans
                        + " human player(s)"
            );
        }

        private void assertWithinModelDeadline(final String message) {
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= MODEL_TIMEOUT_NANOS,
                    message
            );
        }

        private String diagnostics() {
            final ServerPlayer body = body();
            return "supervisor=" + runtime.skillSupervisor().snapshot()
                    + ", goal=" + runtime.goals().snapshot()
                    + ", dimension="
                    + body.level().dimension().identifier()
                    + ", body=" + body.position()
                    + ", health=" + body.getHealth()
                    + ", rods="
                    + body.getInventory().countItem(Items.BLAZE_ROD)
                    + ", spawned=" + blazeTargetsSpawned
                    + ", target="
                    + (blaze == null
                        ? "none"
                        : blaze.getUUID() + "/alive="
                            + blaze.isAlive() + "/health="
                            + blaze.getHealth())
                    + ", skillObserved=" + reserveSkillObserved
                    + ", milestones="
                    + runtime.worldData()
                        .verifiedRouteProgress(
                                Math.max(0L, goalRevision)
                        ).milestones();
        }

        private void cleanup() {
            finishScenarioGoal(runtime);
            if (humanSession != null) {
                humanSession.close();
            }
            if (blaze != null && !blaze.isRemoved()) {
                blaze.discard();
            }
            if (AiPlayerManager.status(runtime.server()).state()
                    != SessionState.ABSENT) {
                AiPlayerManager.requestRemove(runtime.server());
            }
        }
    }

    private enum NetherBlazeStage {
        BODY,
        SIMULATION,
        VISIBLE,
        PROBE,
        GOAL,
        SKILL,
        ACQUIRE,
        DONE
    }

    private static final class LiveNetherPortalScenario {
        private static final long BUILD_TIMEOUT_NANOS =
                java.time.Duration.ofSeconds(90).toNanos();
        private static final long ENTRY_TIMEOUT_NANOS =
                java.time.Duration.ofSeconds(150).toNanos();
        private static final int SITE_SUPPORT_SAMPLES = 4;
        private static final int SITE_AIR_SAMPLES = 20;
        /*
         * A single infinitesimal ray is intentionally not enough to prove
         * portal clearance.  Revisit every support/backing target from three
         * nearby first-person aim points so the post-login body session can
         * establish the same bounded evidence a player would build by
         * looking around, without reading blocks directly.
         */
        private static final int PORTAL_SITE_SCAN_PASSES = 3;
        private static final int SAMPLE_SETTLE_TICKS = 6;

        private final GameTestHelper helper;
        private final ServerRuntime runtime;
        private final long createdAt;

        private NetherPortalStage stage = NetherPortalStage.BODY;
        private CompletableFuture<CapabilityProbeOutcome> probe;
        private PlacedHuman humanSession;
        private BlockPos portalAnchor;
        private UUID bodyId;
        private ServerPlayer bodyBeforeHumanLogin;
        private long humanAnchorReadyTick = -1L;
        private long nextSiteSampleTick;
        private int siteSampleIndex;
        private long goalRevisionBefore;
        private long goalRevision;
        private long stageStartedNanos;
        private boolean buildSkillObserved;
        private boolean entrySkillObserved;

        private LiveNetherPortalScenario(
                final GameTestHelper helper,
                final ServerRuntime runtime
        ) {
            this.helper = helper;
            this.runtime = runtime;
            createdAt = helper.getTick();
            stageStartedNanos = System.nanoTime();
        }

        private void start() {
            finishScenarioGoal(runtime);
            final var status = AiPlayerManager.status(runtime.server());
            if (status.state() == SessionState.ABSENT) {
                helper.assertTrue(
                        AiPlayerManager.requestSpawn(
                                runtime.server()
                        ).accepted(),
                        "Nether-route companion spawn was rejected"
                );
            }
        }

        private void tick() {
            if (humanSession != null) {
                humanSession.tick();
            }
            switch (stage) {
                case BODY -> waitForBody();
                case SITE_SCAN -> scanFairPortalSite();
                case POST_ANCHOR_SCAN -> scanPostAnchorPortalSite();
                case PROBE -> waitForProbe();
                case GOAL -> waitForGoal();
                case BUILD_SKILL -> waitForBuildSkill();
                case BUILD -> waitForBuild();
                case ENTRY_SKILL -> waitForEntrySkill();
                case ENTER -> waitForEntry();
                case DONE -> {
                    // GameTest is already terminal.
                }
            }
        }

        private void waitForBody() {
            final var status = AiPlayerManager.status(runtime.server());
            helper.assertTrue(
                    status.state() != SessionState.FAILED,
                    "Nether-route companion body failed"
            );
            if (status.state() != SessionState.ACTIVE
                    || !status.online()) {
                helper.assertTrue(
                        helper.getTick() - createdAt
                            <= BODY_TIMEOUT_TICKS,
                        "Nether-route companion body timed out"
                );
                return;
            }
            final ServerPlayer body = AiPlayerManager
                    .onlinePlayer(runtime.server())
                    .orElseThrow();
            bodyId = body.getUUID();
            preparePortalSite(body);
            stage = NetherPortalStage.SITE_SCAN;
            nextSiteSampleTick = helper.getTick();
        }

        /**
         * Builds only first-person evidence. The fixture turns the actual
         * body toward opaque surfaces behind each proposed air cell, then
         * waits for the normal 4 Hz semantic sampler. It never inserts a
         * portal block or edits navigation memory.
         */
        private void scanFairPortalSite() {
            final ServerPlayer body = body();
            if (helper.getTick() < nextSiteSampleTick) {
                return;
            }
            if (siteSampleIndex
                    >= SITE_SUPPORT_SAMPLES + SITE_AIR_SAMPLES) {
                prepareConstructionScaffold(body);
                probe = probeOrReuseVerifiedModel(runtime);
                stage = NetherPortalStage.PROBE;
                stageStartedNanos = System.nanoTime();
                return;
            }
            final BlockPos target;
            if (siteSampleIndex < SITE_SUPPORT_SAMPLES) {
                target = portalAnchor.offset(
                        siteSampleIndex,
                        -1,
                        0
                );
            } else {
                final int cell =
                        siteSampleIndex - SITE_SUPPORT_SAMPLES;
                target = portalAnchor.offset(
                        cell % 4,
                        cell / 4,
                        1
                );
            }
            body.lookAt(
                    EntityAnchorArgument.Anchor.EYES,
                    Vec3.atCenterOf(target)
            );
            body.setYHeadRot(body.getYRot());
            siteSampleIndex++;
            nextSiteSampleTick =
                    helper.getTick() + SAMPLE_SETTLE_TICKS;
        }

        /**
         * Rebuilds the same bounded first-person site evidence after the
         * production initial-anchor remove/relogin. Perception is tied to the
         * authoritative body session, so retaining the pre-login navigation
         * snapshot would be an unfair hidden-world shortcut and would make a
         * legitimate model decision fail with {@code observed_site_unavailable}.
         */
        private void scanPostAnchorPortalSite() {
            assertWithinModelDeadline(
                    "Nether-route post-anchor site observation timed out"
            );
            final Optional<ServerPlayer> bodyCandidate = AiPlayerManager
                    .onlinePlayer(runtime.server());
            if (bodyCandidate.isEmpty()) {
                return;
            }
            if (helper.getTick() < nextSiteSampleTick) {
                return;
            }
            final int logicalSampleCount =
                    SITE_SUPPORT_SAMPLES + SITE_AIR_SAMPLES;
            if (siteSampleIndex
                    >= logicalSampleCount * PORTAL_SITE_SCAN_PASSES) {
                stage = NetherPortalStage.PROBE;
                stageStartedNanos = System.nanoTime();
                return;
            }
            final ServerPlayer body = bodyCandidate.orElseThrow();
            final int logicalSample = siteSampleIndex
                    / PORTAL_SITE_SCAN_PASSES;
            final int pass = siteSampleIndex
                    % PORTAL_SITE_SCAN_PASSES;
            body.lookAt(
                    EntityAnchorArgument.Anchor.EYES,
                    portalSampleAimPoint(logicalSample, pass)
            );
            body.setYHeadRot(body.getYRot());
            runtime.observations().requestObservation(
                    new RequestedObservation(
                            ObservationKind.SEMANTIC_REFRESH,
                            "post_anchor_portal_site"
                    )
            );
            siteSampleIndex++;
            nextSiteSampleTick =
                    helper.getTick() + SAMPLE_SETTLE_TICKS;
        }

        private BlockPos portalSampleTarget(final int sampleIndex) {
            if (sampleIndex < SITE_SUPPORT_SAMPLES) {
                return portalAnchor.offset(sampleIndex, -1, 0);
            }
            final int cell = sampleIndex - SITE_SUPPORT_SAMPLES;
            return portalAnchor.offset(
                    cell % 4,
                    cell / 4,
                    1
            );
        }

        private Vec3 portalSampleAimPoint(
                final int logicalSample,
                final int pass
        ) {
            final BlockPos target = portalSampleTarget(logicalSample);
            final double offsetX;
            final double offsetY;
            final double offsetZ;
            switch (pass) {
                case 1 -> {
                    offsetX = 0.22D;
                    offsetY = -0.18D;
                    offsetZ = 0.16D;
                }
                case 2 -> {
                    offsetX = -0.22D;
                    offsetY = 0.18D;
                    offsetZ = -0.16D;
                }
                default -> {
                    offsetX = 0.0D;
                    offsetY = 0.0D;
                    offsetZ = 0.0D;
                }
            }
            return new Vec3(
                    target.getX() + 0.5D + offsetX,
                    target.getY() + 0.5D + offsetY,
                    target.getZ() + 0.5D + offsetZ
            );
        }

        private void waitForProbe() {
            assertWithinModelDeadline(
                    "Nether-route model capability probe timed out"
            );
            if (!probe.isDone()) {
                return;
            }
            final CapabilityProbeOutcome outcome = probe.join();
            helper.assertTrue(
                    outcome
                        instanceof CapabilityProbeOutcome.Supported,
                    "Configured live model probe failed: " + outcome
            );
            /*
             * A first human joining a dedicated world can legitimately
             * trigger the production initial-anchor remove/relogin. Keep the
             * human online and wait for the replacement ServerPlayer instead
             * of submitting the task against the stale body or treating the
             * one-tick absence as a model failure.
             */
            if (humanSession == null) {
                final Optional<ServerPlayer> bodyCandidate = AiPlayerManager
                        .onlinePlayer(runtime.server());
                if (bodyCandidate.isEmpty()) {
                    assertWithinModelDeadline(
                            "Nether-route body disappeared while the model "
                                + "probe was completing"
                    );
                    return;
                }
                bodyBeforeHumanLogin = bodyCandidate.orElseThrow();
                humanSession = PlacedHuman.create(
                        helper,
                        runtime,
                        /* Keep the login anchor on the prepared floor beside
                         * the portal. Using a real supported feet position
                         * avoids a vanilla spawn correction several blocks
                         * upward while keeping every frame block inside the
                         * ordinary 4.75-block interaction reach.
                         */
                        new Vec3(
                                portalAnchor.getX() + 2.5D,
                                portalAnchor.getY(),
                                portalAnchor.getZ() - 2.0D
                        )
                );
                humanAnchorReadyTick = helper.getTick()
                        + SAMPLE_SETTLE_TICKS;
                return;
            }
            final Optional<ServerPlayer> bodyCandidate = AiPlayerManager
                    .onlinePlayer(runtime.server());
            if (bodyCandidate.isEmpty()) {
                assertWithinModelDeadline(
                        "Nether-route body disappeared during initial "
                            + "anchor reconciliation"
                );
                return;
            }
            final ServerPlayer body = bodyCandidate.orElseThrow();
            if (bodyBeforeHumanLogin != null
                    && runtime.worldData().bodyNeedsInitialAnchor()
                    && body == bodyBeforeHumanLogin) {
                return;
            }
            body.lookAt(
                    EntityAnchorArgument.Anchor.EYES,
                    Vec3.atCenterOf(portalAnchor.offset(1, 2, 0))
            );
            body.setYHeadRot(body.getYRot());
            if (helper.getTick() < humanAnchorReadyTick) {
                return;
            }
            if (bodyBeforeHumanLogin != null) {
                bodyBeforeHumanLogin = null;
                /* The pre-login vantage scaffold is directly in front of the
                 * replacement body's first-person rays. It was only a setup
                 * aid for the old body, so remove those test-created blocks
                 * before the replacement body observes the site. */
                clearConstructionScaffold();
                siteSampleIndex = 0;
                nextSiteSampleTick = helper.getTick();
                stage = NetherPortalStage.POST_ANCHOR_SCAN;
                stageStartedNanos = System.nanoTime();
                return;
            }
            final ServerPlayer human = humanSession.player();
            helper.assertTrue(
                    CompanionCommandAccess.mayAdmin(
                            human.createCommandSourceStack()
                    ),
                    "Logged-in Nether-route player lacked task permission"
            );
            goalRevisionBefore = runtime.goals().snapshot().revision();
            final String request = """
                    %s，请继续通关Minecraft。先用背包里的14个黑曜石和打火石，
                    在你已经观察过的安全位置以X轴搭建并点燃下界传送门；
                    门框左下角坐标是 %d %d %d。完成后立刻正常走进传送门前往下界，
                    不要传送，也不要等我再次提醒。
                    """.formatted(
                            runtime.worldData().displayName(),
                            portalAnchor.getX(),
                            portalAnchor.getY(),
                            portalAnchor.getZ()
                    ).replace('\n', ' ').strip();
            final Component submitted =
                    ForgeHooks.onServerChatSubmittedEvent(
                            human,
                            Component.literal(request)
                    );
            helper.assertTrue(
                    submitted != null,
                    "Companion cancelled the Nether-route chat command"
            );
            humanSession.close();
            humanSession = null;
            stage = NetherPortalStage.GOAL;
            stageStartedNanos = System.nanoTime();
        }

        private void clearConstructionScaffold() {
            for (int u = 1; u <= 2; u++) {
                helper.getLevel().setBlockAndUpdate(
                        portalAnchor.offset(u, 1, -1),
                        Blocks.AIR.defaultBlockState()
                );
                helper.getLevel().setBlockAndUpdate(
                        portalAnchor.offset(u, 2, -1),
                        Blocks.AIR.defaultBlockState()
                );
            }
        }

        private void waitForGoal() {
            assertNoHumanPlayers();
            assertWithinModelDeadline(
                    "Live model did not classify the Nether-route task"
            );
            final GoalSnapshot goal = runtime.goals().snapshot();
            if (goal.revision() == goalRevisionBefore) {
                return;
            }
            helper.assertTrue(
                    goal.revision() > goalRevisionBefore
                        && goal.status() == GoalStatus.RUNNING
                        && goal.goal().contains("通关Minecraft"),
                    "Nether-route chat did not become a running "
                        + "completion goal: " + goal
            );
            goalRevision = goal.revision();
            stage = NetherPortalStage.BUILD_SKILL;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForBuildSkill() {
            assertNoHumanPlayers();
            final var snapshot =
                    runtime.skillSupervisor().snapshot();
            if ("build_and_light_nether_portal".equals(
                    snapshot.skillName()
            )) {
                helper.assertTrue(
                        snapshot.boundGoalRevision() == goalRevision,
                        "Nether portal builder bound the wrong goal"
                );
                buildSkillObserved = true;
                stage = NetherPortalStage.BUILD;
                stageStartedNanos = System.nanoTime();
                return;
            }
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= MODEL_TIMEOUT_NANOS,
                    "Live model did not select the ordinary Nether "
                        + "portal builder: " + diagnostics()
            );
        }

        private void waitForBuild() {
            assertNoHumanPlayers();
            if (activePortalBlocks() == 6) {
                final ServerPlayer body = body();
                helper.assertTrue(
                        buildSkillObserved
                            && body.getInventory().countItem(
                                Items.OBSIDIAN
                            ) == 0
                            && flintDamage(body) == 1,
                        "Nether portal appeared without exact item "
                            + "consumption and durability: "
                            + diagnostics()
                );
                stage = NetherPortalStage.ENTRY_SKILL;
                stageStartedNanos = System.nanoTime();
                return;
            }
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= BUILD_TIMEOUT_NANOS,
                    "Live-model Nether portal build timed out: "
                        + diagnostics()
            );
        }

        private void waitForEntrySkill() {
            assertNoHumanPlayers();
            final var snapshot =
                    runtime.skillSupervisor().snapshot();
            if (("enter_observed_portal".equals(
                        snapshot.skillName()
                    )
                    || "find_and_enter_observed_portal".equals(
                        snapshot.skillName()
                    ))
                    && snapshot.boundGoalRevision()
                        == goalRevision) {
                entrySkillObserved = true;
                stage = NetherPortalStage.ENTER;
                stageStartedNanos = System.nanoTime();
                return;
            }
            helper.assertTrue(
                    body().level().dimension().equals(Level.OVERWORLD),
                    "Body entered the Nether before an entry skill "
                        + "was observed: " + diagnostics()
            );
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= MODEL_TIMEOUT_NANOS,
                    "Live model did not continue from portal build "
                        + "to physical entry: " + diagnostics()
            );
        }

        private void waitForEntry() {
            assertNoHumanPlayers();
            final ServerPlayer body = body();
            helper.assertTrue(
                    bodyId.equals(body.getUUID()),
                    "Nether-route handoff replaced the companion body"
            );
            if (body.level().dimension().equals(Level.NETHER)) {
                final var milestones = runtime.worldData()
                        .verifiedRouteProgress(goalRevision)
                        .milestones();
                helper.assertTrue(
                        buildSkillObserved
                            && entrySkillObserved
                            && activePortalBlocks() == 6
                            && milestones.contains(
                                SurvivalMilestone.NETHER_ENTERED
                            ),
                        "Nether entry lacked the complete causal route "
                            + "evidence: " + diagnostics()
                );
                stage = NetherPortalStage.DONE;
                helper.succeed();
                return;
            }
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= ENTRY_TIMEOUT_NANOS,
                    "Live-model Nether portal entry timed out: "
                        + diagnostics()
            );
        }

        private void preparePortalSite(final ServerPlayer body) {
            final BlockPos feet = body.blockPosition();
            portalAnchor = feet.offset(-1, 0, 3);
            body.setGameMode(GameType.SURVIVAL);
            body.getInventory().clearContent();
            body.getEnderChestInventory().clearContent();
            body.removeAllEffects();
            body.clearFire();
            body.setHealth(body.getMaxHealth());
            body.getFoodData().setFoodLevel(20);
            body.getFoodData().setSaturation(5.0F);
            body.setDeltaMovement(Vec3.ZERO);
            for (int x = -6; x <= 6; x++) {
                for (int z = -5; z <= 9; z++) {
                    final BlockPos floor =
                            feet.offset(x, -1, z);
                    helper.getLevel().setBlockAndUpdate(
                            floor,
                            Blocks.SMOOTH_STONE.defaultBlockState()
                    );
                    for (int y = 1; y <= 6; y++) {
                        helper.getLevel().setBlockAndUpdate(
                                floor.above(y),
                                Blocks.AIR.defaultBlockState()
                        );
                    }
                }
            }
            for (int u = -1; u <= 4; u++) {
                for (int v = 0; v <= 5; v++) {
                    helper.getLevel().setBlockAndUpdate(
                            portalAnchor.offset(u, v, 1),
                            Blocks.SMOOTH_STONE.defaultBlockState()
                    );
                }
            }
            body.getInventory().setItem(
                    0,
                    new ItemStack(Items.OBSIDIAN, 14)
            );
            body.getInventory().setItem(
                    1,
                    new ItemStack(Items.FLINT_AND_STEEL)
            );
            body.getInventory().setItem(
                    2,
                    new ItemStack(Items.IRON_PICKAXE)
            );
            body.getInventory().setItem(
                    3,
                    new ItemStack(Items.CRAFTING_TABLE)
            );
            body.getInventory().setItem(
                    4,
                    new ItemStack(Items.OAK_LOG, 8)
            );
            body.getInventory().setItem(
                    5,
                    new ItemStack(Items.COOKED_BEEF, 16)
            );
            body.getInventory().setItem(
                    6,
                    new ItemStack(Items.WATER_BUCKET)
            );
            body.getInventory().setItem(
                    7,
                    new ItemStack(Items.COBBLESTONE, 64)
            );
            body.setItemSlot(
                    EquipmentSlot.OFFHAND,
                    new ItemStack(Items.SHIELD)
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
            body.getInventory().setSelectedSlot(0);
            body.inventoryMenu.broadcastChanges();
        }

        private void prepareConstructionScaffold(
                final ServerPlayer body
        ) {
            for (int u = 1; u <= 2; u++) {
                helper.getLevel().setBlockAndUpdate(
                        portalAnchor.offset(u, 1, -1),
                        Blocks.SMOOTH_STONE.defaultBlockState()
                );
                helper.getLevel().setBlockAndUpdate(
                        portalAnchor.offset(u, 2, -1),
                        Blocks.SMOOTH_STONE_SLAB.defaultBlockState()
                );
            }
            body.teleportTo(
                    portalAnchor.getX() + 2.0D,
                    portalAnchor.getY() + 2.5D,
                    portalAnchor.getZ() - 0.31D
            );
            body.setDeltaMovement(Vec3.ZERO);
            body.fallDistance = 0.0F;
            body.lookAt(
                    EntityAnchorArgument.Anchor.EYES,
                    Vec3.atCenterOf(
                            portalAnchor.offset(1, 2, 0)
                    )
            );
            body.setYHeadRot(body.getYRot());
        }

        private int activePortalBlocks() {
            int blocks = 0;
            for (int u = 1; u <= 2; u++) {
                for (int v = 1; v <= 3; v++) {
                    if (helper.getLevel().getBlockState(
                            portalAnchor.offset(u, v, 0)
                    ).is(Blocks.NETHER_PORTAL)) {
                        blocks++;
                    }
                }
            }
            return blocks;
        }

        private static int flintDamage(final ServerPlayer body) {
            for (int slot = 0;
                    slot < body.getInventory().getContainerSize();
                    slot++) {
                final ItemStack stack =
                        body.getInventory().getItem(slot);
                if (stack.is(Items.FLINT_AND_STEEL)) {
                    return stack.getDamageValue();
                }
            }
            return -1;
        }

        private ServerPlayer body() {
            return AiPlayerManager.onlinePlayer(runtime.server())
                    .orElseThrow(() -> helper.assertionException(
                            "Nether-route companion body disappeared"
                    ));
        }

        private void assertNoHumanPlayers() {
            final long humans = runtime.server()
                    .getPlayerList()
                    .getPlayers()
                    .stream()
                    .filter(player -> !player.getUUID().equals(
                            runtime.worldData().companionUuid()
                    ))
                    .count();
            helper.assertTrue(
                    humans == 0L,
                    "Nether-route autonomy retained " + humans
                        + " human player(s) after the command"
            );
        }

        private void assertWithinModelDeadline(final String message) {
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= MODEL_TIMEOUT_NANOS,
                    message
            );
        }

        private String diagnostics() {
            final ServerPlayer body = body();
            return "supervisor=" + runtime.skillSupervisor().snapshot()
                    + ", goal=" + runtime.goals().snapshot()
                    + ", dimension="
                    + body.level().dimension().identifier()
                    + ", body=" + body.position()
                    + ", obsidian="
                    + body.getInventory().countItem(Items.OBSIDIAN)
                    + ", flintDamage=" + flintDamage(body)
                    + ", portalBlocks=" + activePortalBlocks()
                    + ", buildSkillObserved="
                    + buildSkillObserved
                    + ", entrySkillObserved="
                    + entrySkillObserved
                    + ", milestones="
                    + runtime.worldData()
                        .verifiedRouteProgress(
                                Math.max(0L, goalRevision)
                        ).milestones();
        }

        private void cleanup() {
            finishScenarioGoal(runtime);
            if (humanSession != null) {
                humanSession.close();
            }
            if (AiPlayerManager.status(runtime.server()).state()
                    != SessionState.ABSENT) {
                AiPlayerManager.requestRemove(runtime.server());
            }
        }
    }

    private enum NetherPortalStage {
        BODY,
        SITE_SCAN,
        POST_ANCHOR_SCAN,
        PROBE,
        GOAL,
        BUILD_SKILL,
        BUILD,
        ENTRY_SKILL,
        ENTER,
        DONE
    }

    /**
     * Release-excluded causal gate for the completion phase boundary between
     * Nether resources and the stronghold search. All Overworld/Nether
     * fixture work happens before {@link #submitGoal()}. The victory variant
     * creates its deterministic release-excluded End combat arena only after
     * the body genuinely crosses the portal; it never selects a model skill,
     * credits damage, kills the dragon, or moves the body through a portal.
     */
    private static final class LiveEyeCraftReturnStrongholdScenario {
        private static final long SETUP_TIMEOUT_NANOS =
                java.time.Duration.ofSeconds(150).toNanos();
        private static final long CHAIN_TIMEOUT_NANOS =
                java.time.Duration.ofMinutes(22).toNanos();
        private static final int PORTAL_WAIT_TICKS = 400;
        private static final int COURSE_HALF_LENGTH = 270;
        private static final double COURSE_HALF_WIDTH = 11.5;
        private static final int STRONGHOLD_APPROACH_OFFSET = 192;
        private static final int STRONGHOLD_SEARCH_HALF_WIDTH = 28;
        private static final int STRONGHOLD_EVIDENCE_RADIUS = 8;
        private static final int NETHER_LANE_LENGTH = 28;
        private static final int NETHER_REMOTE_DISTANCE = 22;
        private static final int REQUIRED_EYES = 14;
        private static final int END_PORTAL_FRAME_COUNT = 12;

        private final GameTestHelper helper;
        private final ServerRuntime runtime;
        private final long createdAt;
        private final boolean originalSpawnMobs;
        private final boolean originalSpawnMonsters;
        private final boolean originalGenerateStructures;
        private final boolean requireVictory;

        private EyeReturnStage stage = EyeReturnStage.BODY;
        private CompletableFuture<CapabilityProbeOutcome> probe;
        private PlacedHuman humanSession;
        private UUID bodyId;
        private BlockPos courseCenter;
        private BlockPos portalInterior;
        private BlockPos netherPortalInterior;
        private BlockPos strongholdTarget;
        private BlockPos strongholdEvidence;
        private BlockPos endPortalCenter;
        private Vec3 netherArrival;
        private Vec3 strongholdReachStart;
        private EndVictoryArena victoryArena;
        private long enteredNetherAt = -1L;
        private long portalEntryStartedAt = -1L;
        private long goalRevisionBefore = -1L;
        private long goalRevision = -1L;
        private long stageStartedNanos;
        private CompletableFuture<VerifiedPortalEdge>
                verifiedPortalWrite;
        private boolean priorCheckpointInstalled;
        private boolean netherPrepared;
        private boolean craftSkillObserved;
        private boolean eyeCraftObserved;
        private boolean returnSkillObserved;
        private boolean overworldReturnObserved;
        private boolean triangulationSkillObserved;
        private boolean triangulationHandoffValidated;
        private boolean reachSkillObserved;
        private boolean strongholdHandoffValidated;
        private boolean portalRoomSearchObserved;
        private boolean activationSkillObserved;
        private int activationEyeCountBefore = -1;
        private int activationFilledFramesBefore = -1;
        private boolean endEntrySkillObserved;
        private boolean fightSkillObserved;
        private boolean endReturnSkillObserved;
        private boolean endArenaPrepared;
        private int strongholdReachPickaxeDamage;
        private int strongholdReachTorchCount;
        private boolean cleaned;

        private LiveEyeCraftReturnStrongholdScenario(
                final GameTestHelper helper,
                final ServerRuntime runtime,
                final boolean requireVictory
        ) {
            this.helper = helper;
            this.runtime = runtime;
            this.requireVictory = requireVictory;
            createdAt = helper.getTick();
            stageStartedNanos = System.nanoTime();
            originalSpawnMobs = helper.getLevel()
                    .getGameRules()
                    .get(GameRules.SPAWN_MOBS);
            originalSpawnMonsters = helper.getLevel()
                    .getGameRules()
                    .get(GameRules.SPAWN_MONSTERS);
            originalGenerateStructures = runtime.server()
                    .getWorldGenSettings()
                    .options()
                    .generateStructures();
        }

        private void start() {
            finishScenarioGoal(runtime);
            final var status = AiPlayerManager.status(runtime.server());
            if (status.state() == SessionState.ABSENT) {
                helper.assertTrue(
                        AiPlayerManager.requestSpawn(
                                runtime.server()
                        ).accepted(),
                        "Eye-return companion spawn was rejected"
                );
            }
        }

        private void tick() {
            if (humanSession != null) {
                humanSession.tick();
            }
            switch (stage) {
                case BODY -> waitForBody();
                case ENTERING_NETHER -> waitForNetherEntry();
                case NETHER_READY -> waitForNetherReadiness();
                case GOAL -> waitForGoal();
                case AUTONOMOUS_CHAIN -> observeAutonomousChain();
                case DONE -> {
                    // GameTest is already terminal.
                }
            }
        }

        private void waitForBody() {
            final var status = AiPlayerManager.status(runtime.server());
            helper.assertTrue(
                    status.state() != SessionState.FAILED,
                    "Eye-return companion body failed: " + status
            );
            if (status.state() != SessionState.ACTIVE
                    || !status.online()) {
                helper.assertTrue(
                        helper.getTick() - createdAt
                            <= BODY_TIMEOUT_TICKS,
                        "Eye-return companion body timed out"
                );
                return;
            }
            final ServerPlayer body = body();
            bodyId = body.getUUID();
            prepareOverworldCourseAndPortal(body);
            probe = probeOrReuseVerifiedModel(runtime);
            stage = EyeReturnStage.ENTERING_NETHER;
            stageStartedNanos = System.nanoTime();
        }

        private void prepareOverworldCourseAndPortal(
                final ServerPlayer body
        ) {
            setNaturalSpawning(false, false);
            setGenerateStructures(true);
            final BlockPos structureSearchOrigin = helper.absolutePos(
                    new BlockPos(300, 8, 300)
            );
            strongholdTarget = helper.getLevel()
                    .findNearestMapStructure(
                            StructureTags.EYE_OF_ENDER_LOCATED,
                            structureSearchOrigin,
                            256,
                            false
                    );
            helper.assertTrue(
                    strongholdTarget != null,
                    "GameTest world has no generated stronghold for the "
                        + "live Eye-return chain"
            );
            /*
             * Keep this controlled continuous gate short enough to run
             * repeatedly. The generated-structure lookup is fixture-only,
             * before ordinary chat, and is never passed to production. The
             * body and model still receive only normal Eye trajectories.
             */
            courseCenter = new BlockPos(
                    strongholdTarget.getX(),
                    structureSearchOrigin.getY(),
                    strongholdTarget.getZ()
                        + STRONGHOLD_APPROACH_OFFSET
            );
            final BlockPos nearestFromCourse = helper.getLevel()
                    .findNearestMapStructure(
                            StructureTags.EYE_OF_ENDER_LOCATED,
                            courseCenter,
                            256,
                            false
                    );
            helper.assertTrue(
                    strongholdTarget.equals(nearestFromCourse),
                    "Controlled course did not retain the same vanilla "
                        + "Eye target"
            );

            final double towardX =
                    strongholdTarget.getX() + 0.5
                        - (courseCenter.getX() + 0.5);
            final double towardZ =
                    strongholdTarget.getZ() + 0.5
                        - (courseCenter.getZ() + 0.5);
            final double length = Math.hypot(towardX, towardZ);
            helper.assertTrue(
                    length > 32.0,
                    "Generated stronghold is too close for a fair "
                        + "two-ray live gate"
            );
            final double directionX = towardX / length;
            final double directionZ = towardZ / length;
            final double baselineX = -directionZ;
            final double baselineZ = directionX;

            /*
             * Build both legal perpendicular directions. The production
             * compound receives no fixture coordinate and chooses a
             * candidate only after measuring its own first Eye trajectory.
             */
            final int radius = COURSE_HALF_LENGTH + 12;
            for (int x = courseCenter.getX() - radius;
                    x <= courseCenter.getX() + radius; x++) {
                for (int z = courseCenter.getZ() - radius;
                        z <= courseCenter.getZ() + radius; z++) {
                    final double deltaX =
                            x + 0.5 - (courseCenter.getX() + 0.5);
                    final double deltaZ =
                            z + 0.5 - (courseCenter.getZ() + 0.5);
                    final double forward =
                            deltaX * baselineX + deltaZ * baselineZ;
                    final double lateral =
                            deltaX * directionX + deltaZ * directionZ;
                    if (Math.abs(forward) > COURSE_HALF_LENGTH + 0.5
                            || Math.abs(lateral)
                                > COURSE_HALF_WIDTH) {
                        continue;
                    }
                    final BlockPos floor = new BlockPos(
                            x,
                            courseCenter.getY() - 1,
                            z
                    );
                    helper.getLevel().setBlockAndUpdate(
                            floor,
                            Blocks.SMOOTH_STONE.defaultBlockState()
                    );
                    for (int y = 1; y <= 7; y++) {
                        helper.getLevel().setBlockAndUpdate(
                                floor.above(y),
                                Blocks.AIR.defaultBlockState()
                        );
                    }
                }
            }
            prepareStrongholdApproachCourse(
                    directionX,
                    directionZ,
                    baselineX,
                    baselineZ
            );

            portalInterior = courseCenter;
            buildActivePortal(helper.getLevel(), portalInterior);
            helper.assertTrue(
                    helper.getLevel()
                        .getBlockState(portalInterior)
                        .is(Blocks.NETHER_PORTAL),
                    "Pre-command source frame did not ignite into a "
                        + "vanilla Nether portal"
            );
            prepareNetherDestinationPortal();
            body.setGameMode(GameType.SURVIVAL);
            body.stopRiding();
            body.getInventory().clearContent();
            body.getEnderChestInventory().clearContent();
            body.removeAllEffects();
            body.clearFire();
            body.setHealth(body.getMaxHealth());
            body.getFoodData().setFoodLevel(20);
            body.getFoodData().setSaturation(5.0F);
            body.fallDistance = 0.0F;
            portalEntryStartedAt = helper.getTick();
            body.teleportTo(
                    portalInterior.getX() + 0.5,
                    portalInterior.getY(),
                    portalInterior.getZ() + 0.5
            );
            body.setDeltaMovement(Vec3.ZERO);
            dev.mcai.companion.MinecraftAiCompanion.LOGGER.info(
                    "Live Eye-return source portal body={} scale={} "
                        + "course={} expectedNether={}",
                    body.position(),
                    net.minecraft.world.level.dimension.DimensionType
                        .getTeleportationScale(
                            body.level().dimensionType(),
                            runtime.server()
                                .getLevel(Level.NETHER)
                                .dimensionType()
                        ),
                    courseCenter,
                    netherPortalInterior
            );
        }

        /**
         * Builds two pre-command diagonal walking corridors from either
         * legal triangulation endpoint to a buried search volume. The
         * fixture does not expose the target or wall to production; after
         * chat submission it never changes another block.
         */
        private void prepareStrongholdApproachCourse(
                final double directionX,
                final double directionZ,
                final double baselineX,
                final double baselineZ
        ) {
            final var level = helper.getLevel();
            final double targetX = strongholdTarget.getX() + 0.5;
            final double targetZ = strongholdTarget.getZ() + 0.5;
            final double positiveX = courseCenter.getX() + 0.5
                    + baselineX * COURSE_HALF_LENGTH;
            final double positiveZ = courseCenter.getZ() + 0.5
                    + baselineZ * COURSE_HALF_LENGTH;
            final double negativeX = courseCenter.getX() + 0.5
                    - baselineX * COURSE_HALF_LENGTH;
            final double negativeZ = courseCenter.getZ() + 0.5
                    - baselineZ * COURSE_HALF_LENGTH;
            final int minimumX = (int) Math.floor(Math.min(
                    targetX,
                    Math.min(positiveX, negativeX)
            )) - STRONGHOLD_SEARCH_HALF_WIDTH;
            final int maximumX = (int) Math.ceil(Math.max(
                    targetX,
                    Math.max(positiveX, negativeX)
            )) + STRONGHOLD_SEARCH_HALF_WIDTH;
            final int minimumZ = (int) Math.floor(Math.min(
                    targetZ,
                    Math.min(positiveZ, negativeZ)
            )) - STRONGHOLD_SEARCH_HALF_WIDTH;
            final int maximumZ = (int) Math.ceil(Math.max(
                    targetZ,
                    Math.max(positiveZ, negativeZ)
            )) + STRONGHOLD_SEARCH_HALF_WIDTH;
            final double corridorRadiusSquared =
                    COURSE_HALF_WIDTH * COURSE_HALF_WIDTH;

            for (int x = minimumX; x <= maximumX; x++) {
                for (int z = minimumZ; z <= maximumZ; z++) {
                    final double sampleX = x + 0.5;
                    final double sampleZ = z + 0.5;
                    if (distanceSquaredToSegment(
                            sampleX,
                            sampleZ,
                            positiveX,
                            positiveZ,
                            targetX,
                            targetZ
                    ) > corridorRadiusSquared
                            && distanceSquaredToSegment(
                                sampleX,
                                sampleZ,
                                negativeX,
                                negativeZ,
                                targetX,
                                targetZ
                            ) > corridorRadiusSquared) {
                        continue;
                    }
                    final BlockPos floor = new BlockPos(
                            x,
                            courseCenter.getY() - 1,
                            z
                    );
                    level.setBlockAndUpdate(
                            floor,
                            Blocks.SMOOTH_STONE.defaultBlockState()
                    );
                    for (int y = 1; y <= 7; y++) {
                        level.setBlockAndUpdate(
                                floor.above(y),
                                Blocks.AIR.defaultBlockState()
                        );
                    }
                }
            }

            for (int x = -STRONGHOLD_SEARCH_HALF_WIDTH;
                    x <= STRONGHOLD_SEARCH_HALF_WIDTH; x++) {
                for (int z = -STRONGHOLD_SEARCH_HALF_WIDTH;
                        z <= STRONGHOLD_SEARCH_HALF_WIDTH; z++) {
                    for (int y = -14; y <= -2; y++) {
                        level.setBlockAndUpdate(
                                new BlockPos(
                                        strongholdTarget.getX() + x,
                                        courseCenter.getY() + y,
                                        strongholdTarget.getZ() + z
                                ),
                                Blocks.STONE.defaultBlockState()
                        );
                    }
                }
            }
            for (int offset = -STRONGHOLD_EVIDENCE_RADIUS;
                    offset <= STRONGHOLD_EVIDENCE_RADIUS;
                    offset++) {
                for (int y = -10; y <= -3; y++) {
                    level.setBlockAndUpdate(
                            new BlockPos(
                                    strongholdTarget.getX()
                                        + STRONGHOLD_EVIDENCE_RADIUS,
                                    courseCenter.getY() + y,
                                    strongholdTarget.getZ() + offset
                            ),
                            Blocks.STONE_BRICKS.defaultBlockState()
                    );
                    level.setBlockAndUpdate(
                            new BlockPos(
                                    strongholdTarget.getX()
                                        - STRONGHOLD_EVIDENCE_RADIUS,
                                    courseCenter.getY() + y,
                                    strongholdTarget.getZ() + offset
                            ),
                            Blocks.STONE_BRICKS.defaultBlockState()
                    );
                    level.setBlockAndUpdate(
                            new BlockPos(
                                    strongholdTarget.getX() + offset,
                                    courseCenter.getY() + y,
                                    strongholdTarget.getZ()
                                        + STRONGHOLD_EVIDENCE_RADIUS
                            ),
                            Blocks.STONE_BRICKS.defaultBlockState()
                    );
                    level.setBlockAndUpdate(
                            new BlockPos(
                                    strongholdTarget.getX() + offset,
                                    courseCenter.getY() + y,
                                    strongholdTarget.getZ()
                                        - STRONGHOLD_EVIDENCE_RADIUS
                            ),
                            Blocks.STONE_BRICKS.defaultBlockState()
                    );
                }
            }
            strongholdEvidence = new BlockPos(
                    strongholdTarget.getX()
                        + STRONGHOLD_EVIDENCE_RADIUS,
                    courseCenter.getY() - 5,
                    strongholdTarget.getZ()
            );
            if (requireVictory) {
                prepareBuriedStrongholdPortalMaze();
            }
            helper.assertTrue(
                    Math.abs(directionX) + Math.abs(directionZ) > 0.99,
                    "Stronghold approach lacked a normalized Eye bearing"
            );
        }

        /**
         * Gives the continuous completion variant a traversable,
         * first-person-only stronghold interior at the measured search area.
         * The measured centre remains solid so the descending search never
         * walks over a fixture-created void. A buried receiving room begins
         * one block beyond the east stronghold wall, where the production
         * wall-entry gate physically finishes. A roofed two-turn corridor
         * then hides the portal ring from that handoff frame.
         */
        private void prepareBuriedStrongholdPortalMaze() {
            final var level = helper.getLevel();
            final BlockPos chamber = new BlockPos(
                    strongholdTarget.getX()
                        + STRONGHOLD_EVIDENCE_RADIUS + 1,
                    courseCenter.getY() - 4,
                    strongholdTarget.getZ()
            );

            for (int x = 0; x <= 6; x++) {
                for (int z = -8; z <= 8; z++) {
                    level.setBlockAndUpdate(
                            chamber.offset(x, -1, z),
                            Blocks.STONE_BRICKS.defaultBlockState()
                    );
                    for (int y = 0; y <= 2; y++) {
                        level.setBlockAndUpdate(
                                chamber.offset(x, y, z),
                                Blocks.AIR.defaultBlockState()
                        );
                    }
                    level.setBlockAndUpdate(
                            chamber.offset(x, 3, z),
                            Blocks.STONE.defaultBlockState()
                    );
                }
            }

            /*
             * Build an opaque corridor shell before carving its actual
             * interior. The ring sits beyond a right-angle turn, so loaded
             * world geometry does not become fair evidence until the body
             * physically explores there.
             */
            for (int x = 7; x <= 23; x++) {
                for (int z = -2; z <= 21; z++) {
                    level.setBlockAndUpdate(
                            chamber.offset(x, -1, z),
                            Blocks.STONE_BRICKS.defaultBlockState()
                    );
                    for (int y = 0; y <= 3; y++) {
                        level.setBlockAndUpdate(
                                chamber.offset(x, y, z),
                                Blocks.STONE.defaultBlockState()
                        );
                    }
                }
            }
            for (int x = 7; x <= 20; x++) {
                carveBuriedStrongholdInterior(chamber, x, 0);
            }
            for (int z = 0; z <= 12; z++) {
                carveBuriedStrongholdInterior(chamber, 16, z);
            }
            for (int x = 13; x <= 19; x++) {
                for (int z = 11; z <= 19; z++) {
                    carveBuriedStrongholdInterior(chamber, x, z);
                }
            }

            endPortalCenter = chamber.offset(16, 0, 15);
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
            strongholdEvidence = chamber.offset(0, -1, 6);
            helper.assertTrue(
                    level.getBlockState(
                        new BlockPos(
                            strongholdTarget.getX(),
                            courseCenter.getY() - 1,
                            strongholdTarget.getZ()
                        )
                    ).is(Blocks.SMOOTH_STONE)
                        && level.getBlockState(
                            chamber.above(3)
                        ).is(Blocks.STONE),
                    "Buried portal maze exposed its chamber at the "
                        + "ordinary approach surface"
            );
        }

        private void carveBuriedStrongholdInterior(
                final BlockPos chamber,
                final int offsetX,
                final int offsetZ
        ) {
            helper.getLevel().setBlockAndUpdate(
                    chamber.offset(offsetX, -1, offsetZ),
                    Blocks.STONE_BRICKS.defaultBlockState()
            );
            for (int y = 0; y <= 2; y++) {
                helper.getLevel().setBlockAndUpdate(
                        chamber.offset(offsetX, y, offsetZ),
                        Blocks.AIR.defaultBlockState()
                );
            }
        }

        private void setEmptyEndPortalFrame(
                final BlockPos position,
                final Direction facing
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
                            false
                        )
            );
        }

        private static double distanceSquaredToSegment(
                final double sampleX,
                final double sampleZ,
                final double startX,
                final double startZ,
                final double endX,
                final double endZ
        ) {
            final double deltaX = endX - startX;
            final double deltaZ = endZ - startZ;
            final double lengthSquared =
                    deltaX * deltaX + deltaZ * deltaZ;
            if (lengthSquared <= 1.0E-9) {
                return Math.pow(sampleX - startX, 2.0)
                        + Math.pow(sampleZ - startZ, 2.0);
            }
            final double projection = Math.max(
                    0.0,
                    Math.min(
                            1.0,
                            ((sampleX - startX) * deltaX
                                + (sampleZ - startZ) * deltaZ)
                                / lengthSquared
                    )
            );
            final double closestX = startX + projection * deltaX;
            final double closestZ = startZ + projection * deltaZ;
            return Math.pow(sampleX - closestX, 2.0)
                    + Math.pow(sampleZ - closestZ, 2.0);
        }

        private void prepareNetherDestinationPortal() {
            final var nether = runtime.server().getLevel(Level.NETHER);
            helper.assertTrue(
                    nether != null,
                    "GameTest server has no Nether level"
            );
            netherPortalInterior = new BlockPos(
                    Math.floorDiv(courseCenter.getX(), 8),
                    64,
                    Math.floorDiv(courseCenter.getZ(), 8)
            );
            for (int x = -6; x <= 7; x++) {
                for (int z = -6; z <= 6; z++) {
                    final BlockPos floor =
                            netherPortalInterior.offset(x, -1, z);
                    nether.setBlockAndUpdate(
                            floor,
                            Blocks.NETHERRACK.defaultBlockState()
                    );
                    for (int y = 1; y <= 5; y++) {
                        nether.setBlockAndUpdate(
                                floor.above(y),
                                Blocks.AIR.defaultBlockState()
                        );
                    }
                }
            }
            buildActivePortal(nether, netherPortalInterior);
            helper.assertTrue(
                    nether.getBlockState(netherPortalInterior)
                            .is(Blocks.NETHER_PORTAL)
                        && nether.getBlockState(
                            netherPortalInterior.above(2)
                        ).is(Blocks.NETHER_PORTAL),
                    "Pre-command Nether destination portal did not "
                        + "remain active"
            );
            final Optional<BlockPos> indexedPortal =
                    nether.getPortalForcer()
                        .findClosestPortalPosition(
                                netherPortalInterior,
                                true,
                                nether.getWorldBorder()
                        );
            helper.assertTrue(
                    indexedPortal.filter(position ->
                            position.distSqr(netherPortalInterior)
                                <= 16.0
                    ).isPresent(),
                    "Pre-command Nether destination portal was not "
                        + "indexed by vanilla PortalForcer: "
                        + indexedPortal
            );
        }

        private void waitForNetherEntry() {
            assertSetupDeadline(
                    "Live Eye-return setup timed out before Nether entry"
            );
            assertProbeHealthy();
            final ServerPlayer body = body();
            if (!body.level().dimension().equals(Level.NETHER)) {
                if ((helper.getTick() - createdAt) % 20L == 0L) {
                    dev.mcai.companion.MinecraftAiCompanion.LOGGER.info(
                            "Live Eye-return source wait tick={} body={} "
                                + "feetState={} insidePortal={}",
                            helper.getTick(),
                            body.position(),
                            body.level().getBlockState(
                                body.blockPosition()
                            ),
                            body.portalProcess != null
                                && body.portalProcess
                                    .isInsidePortalThisTick()
                    );
                }
                helper.assertTrue(
                        helper.getTick() - createdAt
                            <= PORTAL_WAIT_TICKS,
                        "Companion did not physically enter the prepared "
                            + "Nether portal"
                );
                return;
            }
            if (enteredNetherAt < 0L) {
                enteredNetherAt = helper.getTick();
                recordObservedInitialTraversal(body);
                return;
            }
            /*
             * Let vanilla finish installing the destination player ticket
             * and let the traversal observer enqueue its durable edge.
             */
            if (helper.getTick() - enteredNetherAt < 3L) {
                return;
            }
            helper.assertTrue(
                    verifiedPortalWrite != null,
                    "Physical Nether entry did not start its verified "
                        + "portal-memory write"
            );
            if (!verifiedPortalWrite.isDone()) {
                return;
            }
            helper.assertTrue(
                    !verifiedPortalWrite.isCompletedExceptionally(),
                    "Physical Nether traversal could not be persisted"
            );
            if (netherPrepared) {
                stage = EyeReturnStage.NETHER_READY;
                return;
            }
            /*
             * Commit the phase before fixture mutations. Vanilla inventory
             * and advancement callbacks can cause another scheduled
             * GameTest callback to observe this scenario before the current
             * callback unwinds; the setup must remain exactly-once.
             */
            netherPrepared = true;
            stage = EyeReturnStage.NETHER_READY;
            stageStartedNanos = System.nanoTime();
            prepareNetherLaneAndOwnedMaterials(body);
        }

        private void recordObservedInitialTraversal(
                final ServerPlayer body
        ) {
            helper.assertTrue(
                    portalEntryStartedAt >= 0L
                        && helper.getLevel()
                            .getBlockState(portalInterior)
                            .is(Blocks.NETHER_PORTAL)
                        && body.level().dimension().equals(Level.NETHER)
                        && body.level()
                            .getBlockState(nearestPortalBlock(body))
                            .is(Blocks.NETHER_PORTAL),
                    "Initial portal traversal lacked physical endpoint "
                        + "evidence"
            );
            final PortalTraversalResult observed =
                    new PortalTraversalResult(
                            PortalKind.NETHER_PORTAL,
                            AiPlayerManager.status(runtime.server())
                                .sessionGeneration(),
                            DimensionRef.OVERWORLD,
                            new PerceptionVec3(
                                    portalInterior.getX() + 0.5,
                                    portalInterior.getY(),
                                    portalInterior.getZ() + 0.5
                            ),
                            new BlockCoordinate(
                                    portalInterior.getX(),
                                    portalInterior.getY(),
                                    portalInterior.getZ()
                            ),
                            DimensionRef.NETHER,
                            new PerceptionVec3(
                                    body.getX(),
                                    body.getY(),
                                    body.getZ()
                            ),
                            portalEntryStartedAt,
                            helper.getTick(),
                            Optional.of(DimensionRef.NETHER)
                    );
            verifiedPortalWrite = runtime.memory()
                    .portalEdges()
                    .recordTraversal(
                            runtime.worldData().companionUuid(),
                            observed,
                            Instant.now()
                    );
        }

        private void prepareNetherLaneAndOwnedMaterials(
                final ServerPlayer body
        ) {
            dev.mcai.companion.MinecraftAiCompanion.LOGGER.info(
                    "Live Eye-return Nether preparation arrival={} "
                        + "expectedPortal={} expectedState={}",
                    body.position(),
                    netherPortalInterior,
                    body.level().getBlockState(netherPortalInterior)
            );
            final BlockPos portal = nearestPortalBlock(body);
            dev.mcai.companion.MinecraftAiCompanion.LOGGER.info(
                    "Live Eye-return selected Nether portal={} "
                        + "arrival={}",
                    portal,
                    body.position()
            );
            final Direction.Axis axis = body.level()
                    .getBlockState(portal)
                    .getValue(NetherPortalBlock.AXIS);
            final Direction forward = axis == Direction.Axis.X
                    ? Direction.SOUTH
                    : Direction.EAST;
            final Direction side = forward.getClockWise();
            final BlockPos arrivalFeet = body.blockPosition();
            netherArrival = body.position();

            for (int step = -4; step <= NETHER_LANE_LENGTH; step++) {
                for (int lateral = -3; lateral <= 3; lateral++) {
                    final BlockPos column = arrivalFeet
                            .relative(forward, step)
                            .relative(side, lateral);
                    setUnlessPortal(
                            body,
                            column.below(),
                            Blocks.NETHERRACK.defaultBlockState()
                    );
                    for (int y = 0; y <= 3; y++) {
                        setUnlessPortal(
                                body,
                                column.above(y),
                                Math.abs(lateral) == 3
                                        ? Blocks.NETHERRACK
                                            .defaultBlockState()
                                        : Blocks.AIR.defaultBlockState()
                        );
                    }
                    setUnlessPortal(
                            body,
                            column.above(4),
                            Blocks.NETHERRACK.defaultBlockState()
                    );
                }
            }

            final BlockPos remote = arrivalFeet.relative(
                    forward,
                    NETHER_REMOTE_DISTANCE
            );
            body.teleportTo(
                    remote.getX() + 0.5,
                    remote.getY(),
                    remote.getZ() + 0.5
            );
            dev.mcai.companion.MinecraftAiCompanion.LOGGER.info(
                    "Live Eye-return moved pre-command body to remote={} "
                        + "from arrival={}",
                    body.position(),
                    netherArrival
            );
            body.setDeltaMovement(Vec3.ZERO);
            body.fallDistance = 0.0F;
            body.getInventory().clearContent();
            pickUpOwned(
                    body,
                    new ItemStack(Items.BLAZE_POWDER, REQUIRED_EYES)
            );
            pickUpOwned(
                    body,
                    new ItemStack(Items.ENDER_PEARL, REQUIRED_EYES)
            );
            body.getInventory().setItem(
                    4,
                    new ItemStack(Items.COOKED_BEEF, 16)
            );
            body.getInventory().setItem(
                    5,
                    new ItemStack(Items.IRON_PICKAXE)
            );
            body.getInventory().setItem(
                    6,
                    new ItemStack(Items.WATER_BUCKET)
            );
            body.getInventory().setItem(
                    7,
                    new ItemStack(Items.COBBLESTONE, 64)
            );
            body.getInventory().setItem(
                    8,
                    new ItemStack(Items.TORCH, 32)
            );
            if (requireVictory) {
                body.getInventory().setItem(
                        9,
                        new ItemStack(Items.BOW)
                );
                body.getInventory().setItem(
                        10,
                        new ItemStack(Items.ARROW, 64)
                );
                body.getInventory().setItem(
                        11,
                        new ItemStack(Items.DIAMOND_SWORD)
                );
            }
            body.setItemSlot(
                    EquipmentSlot.OFFHAND,
                    new ItemStack(Items.SHIELD)
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
            body.setHealth(body.getMaxHealth());
            body.getFoodData().setFoodLevel(20);
            body.getFoodData().setSaturation(5.0F);
            body.getInventory().setSelectedSlot(5);
            body.inventoryMenu.broadcastChanges();
            strongholdReachPickaxeDamage =
                    itemDamage(body, Items.IRON_PICKAXE);
            strongholdReachTorchCount =
                    body.getInventory().countItem(Items.TORCH);
            helper.assertTrue(
                    body.getInventory().countItem(Items.BLAZE_POWDER)
                            == REQUIRED_EYES
                        && body.getInventory().countItem(
                            Items.ENDER_PEARL
                        ) == REQUIRED_EYES
                        && body.position().distanceTo(netherArrival)
                            >= 16.0,
                    "Nether resource checkpoint was not established "
                        + "through owned pickups away from the portal"
            );
        }

        private void waitForNetherReadiness() {
            assertSetupDeadline(
                    "Live Eye-return setup timed out before ordinary chat"
            );
            if (!probe.isDone()) {
                return;
            }
            final CapabilityProbeOutcome outcome = probe.join();
            helper.assertTrue(
                    outcome
                        instanceof CapabilityProbeOutcome.Supported,
                    "Configured live model probe failed: " + outcome
            );
            final var observation = runtime.observations()
                    .observe(runtime.goals().snapshot());
            if (!observation.semanticJson().contains(
                    "\"outputItemId\":\"minecraft:ender_eye\""
            )) {
                return;
            }
            submitGoal();
        }

        private void submitGoal() {
            final ServerPlayer body = body();
            humanSession = PlacedHuman.create(
                    helper,
                    runtime,
                    Vec3.atCenterOf(courseCenter)
            );
            final ServerPlayer human = humanSession.player();
            helper.assertTrue(
                    CompanionCommandAccess.mayAdmin(
                            human.createCommandSourceStack()
                    ),
                    "Logged-in Eye-return test player lacked task "
                        + "permission"
            );
            goalRevisionBefore = runtime.goals().snapshot().revision();
            final String request = requireVictory
                    ? runtime.worldData().displayName()
                        + "，请继续通关Minecraft：用你背包里的材料"
                        + "合成足够的末影之眼，从下界正常返回主世界，"
                        + "定位并进入要塞，找到末地传送门房间并激活，"
                        + "进入末地击败末影龙，再通过中央返回传送门"
                        + "回到主世界。"
                    : runtime.worldData().displayName()
                        + "，请继续通关Minecraft："
                        + "用你背包里的材料合成足够的"
                        + "末影之眼，从下界正常返回主世界，"
                        + "然后开始定位要塞。";
            final Component submitted =
                    ForgeHooks.onServerChatSubmittedEvent(
                            human,
                            Component.literal(request)
                    );
            helper.assertTrue(
                    submitted != null,
                    "Companion cancelled the Eye-return chat command"
            );
            final GoalSnapshot installed = runtime.goals().snapshot();
            if (installed.revision() > goalRevisionBefore
                    && installed.status() == GoalStatus.RUNNING) {
                installPriorRouteCheckpoint(installed);
            }
            humanSession.close();
            humanSession = null;
            helper.assertTrue(
                    body.getInventory().countItem(Items.ENDER_EYE) == 0,
                    "Fixture crafted Eyes after the command boundary"
            );
            stage = EyeReturnStage.GOAL;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForGoal() {
            assertNoHumanPlayers();
            assertSetupDeadline(
                    "Live model did not install the Eye-return goal"
            );
            final GoalSnapshot goal = runtime.goals().snapshot();
            if (goal.revision() == goalRevisionBefore) {
                return;
            }
            helper.assertTrue(
                    goal.revision() > goalRevisionBefore
                        && goal.status() == GoalStatus.RUNNING
                        && goal.goal().contains("通关"),
                    "Eye-return chat did not become a running "
                        + "completion goal: " + goal
            );
            installPriorRouteCheckpoint(goal);
            goalRevision = goal.revision();
            stage = EyeReturnStage.AUTONOMOUS_CHAIN;
            stageStartedNanos = System.nanoTime();
        }

        private void installPriorRouteCheckpoint(
                final GoalSnapshot goal
        ) {
            if (priorCheckpointInstalled
                    && goalRevision == goal.revision()) {
                return;
            }
            runtime.worldData().markVerifiedRouteMilestones(
                    goal.revision(),
                    EnumSet.of(
                            SurvivalMilestone.BODY_ACTIVE,
                            SurvivalMilestone.WOOD_OBTAINED,
                            SurvivalMilestone.BASIC_CRAFTING_READY,
                            SurvivalMilestone.STONE_TOOL_OBTAINED,
                            SurvivalMilestone.FOOD_SECURED,
                            SurvivalMilestone.IRON_TOOLKIT_OBTAINED,
                            SurvivalMilestone.NETHER_ENTERED,
                            SurvivalMilestone.BLAZE_MATERIAL_OBTAINED,
                            SurvivalMilestone.ENDER_PEARL_OBTAINED
                    )
            );
            goalRevision = goal.revision();
            priorCheckpointInstalled = true;
        }

        private void observeAutonomousChain() {
            assertNoHumanPlayers();
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= CHAIN_TIMEOUT_NANOS,
                    "Live Eye craft/return/stronghold chain timed out: "
                        + diagnostics()
            );
            /*
             * The first real human login can deliberately exercise the
             * production initial-anchor lifecycle: the authoritative
             * ServerPlayer is removed, then re-created with the same UUID.
             * There is a small server-thread window in which PlayerList has
             * no online companion.  Treat that window as lifecycle
             * reconciliation, not as a gameplay death or a model failure;
             * the next tick must reacquire the authoritative body before any
             * observation, skill, or world assertion is evaluated.
             */
            final Optional<ServerPlayer> bodyCandidate = AiPlayerManager
                    .onlinePlayer(runtime.server());
            if (bodyCandidate.isEmpty()) {
                final var status = AiPlayerManager.status(runtime.server());
                helper.assertTrue(
                        status.state() != SessionState.FAILED,
                        "Eye-return companion body failed during lifecycle "
                            + "reconciliation: " + status
                );
                return;
            }
            final ServerPlayer body = bodyCandidate.orElseThrow();
            helper.assertTrue(
                    body.isAlive()
                        && !body.isDeadOrDying()
                        && body.getHealth() > 0.0F,
                    "Continuous Hardcore-policy completion body died; "
                        + "the run is terminal and must not respawn or "
                        + "request another model decision: "
                        + diagnostics()
            );
            final SkillSupervisor.Snapshot supervisor =
                    runtime.skillSupervisor().snapshot();
            final var milestones = runtime.worldData()
                    .verifiedRouteProgress(goalRevision)
                    .milestones();
            if ("craft_recipe".equals(supervisor.skillName())
                    && supervisor.boundGoalRevision() == goalRevision) {
                craftSkillObserved = true;
            }
            if (PortalSkills.RETURN_VIA_VERIFIED_PORTAL.equals(
                    supervisor.skillName()
            ) && supervisor.boundGoalRevision() == goalRevision) {
                returnSkillObserved = true;
            }
            if (StrongholdSkills
                    .TRIANGULATE_STRONGHOLD_SEARCH_AREA
                    .equals(supervisor.skillName())
                    && supervisor.boundGoalRevision()
                        == goalRevision) {
                triangulationSkillObserved = true;
            }
            if (StrongholdSkills.REACH_OBSERVED_STRONGHOLD.equals(
                    supervisor.skillName()
            ) && supervisor.boundGoalRevision() == goalRevision) {
                reachSkillObserved = true;
            }
            if (StrongholdSkills
                    .SEARCH_OBSERVED_STRONGHOLD_PORTAL_ROOM
                    .equals(supervisor.skillName())
                    && supervisor.boundGoalRevision()
                        == goalRevision) {
                portalRoomSearchObserved = true;
            }
            if ("activate_observed_end_portal".equals(
                    supervisor.skillName()
            ) && supervisor.boundGoalRevision() == goalRevision) {
                if (!activationSkillObserved) {
                    activationEyeCountBefore =
                            body.getInventory().countItem(
                                Items.ENDER_EYE
                            );
                    activationFilledFramesBefore =
                            filledEndPortalFrames();
                }
                activationSkillObserved = true;
            }
            if ("find_and_enter_observed_portal".equals(
                    supervisor.skillName()
            ) && supervisor.boundGoalRevision() == goalRevision) {
                if (milestones.contains(
                        SurvivalMilestone.DRAGON_KILLED
                )) {
                    endReturnSkillObserved = true;
                } else {
                    endEntrySkillObserved = true;
                }
            }
            if ("fight_ender_dragon".equals(
                    supervisor.skillName()
            ) && supervisor.boundGoalRevision() == goalRevision) {
                fightSkillObserved = true;
            }

            final int eyes =
                    body.getInventory().countItem(Items.ENDER_EYE);
            if (eyes >= REQUIRED_EYES) {
                helper.assertTrue(
                        body.getInventory().countItem(
                            Items.BLAZE_POWDER
                        ) == 0
                            && body.getInventory().countItem(
                                Items.ENDER_PEARL
                            ) == 0,
                        "Eyes appeared without consuming the normally "
                            + "owned powder and pearls"
                );
                eyeCraftObserved = true;
            }
            if (body.level().dimension().equals(Level.OVERWORLD)
                    && !endArenaPrepared) {
                helper.assertTrue(
                        eyeCraftObserved && returnSkillObserved,
                        "Body returned before the model-composed Eye and "
                            + "verified-portal phases: " + diagnostics()
                );
                overworldReturnObserved = true;
            }

            if (endArenaPrepared
                    && body.level().dimension().equals(Level.OVERWORLD)) {
                if (milestones.contains(
                        SurvivalMilestone.RETURNED_FROM_END
                )) {
                    helper.assertTrue(
                            bodyId.equals(body.getUUID())
                                && portalRoomSearchObserved
                                && activationSkillObserved
                                && endEntrySkillObserved
                                && fightSkillObserved
                                && endReturnSkillObserved,
                            "Continuous Nether-material completion lost "
                                + "one model/body handoff: "
                                + diagnostics()
                    );
                    stage = EyeReturnStage.DONE;
                    helper.succeed();
                }
                return;
            }

            if (body.level().dimension().equals(Level.END)) {
                helper.assertTrue(
                        requireVictory
                            && strongholdHandoffValidated
                            && portalRoomSearchObserved
                            && activationSkillObserved
                            && endEntrySkillObserved
                            && activeEndPortalBlocks() == 9
                            && exactActivationEyeConsumption(body),
                        "End entry skipped a stronghold, activation, or "
                            + "inventory handoff: " + diagnostics()
                );
                if (!endArenaPrepared) {
                    victoryArena = prepareEndVictoryArena(
                            helper,
                            runtime,
                            body,
                            false,
                            false
                    );
                    endArenaPrepared = true;
                    return;
                }
                if (milestones.contains(
                        SurvivalMilestone.DRAGON_KILLED
                )) {
                    helper.assertTrue(
                            fightSkillObserved
                                && victoryArena != null
                                && (!victoryArena.crystal().isAlive()
                                    || victoryArena.crystal()
                                        .isRemoved())
                                && body.level().getBlockState(
                                    victoryArena.cageBar()
                                ).isAir()
                                && body.getInventory()
                                    .countItem(Items.ARROW) < 64,
                            "Continuous dragon milestone lacked physical "
                                + "combat evidence: " + diagnostics()
                    );
                    if (!victoryArena.dragon().isRemoved()) {
                        victoryArena.dragon().setNoAi(false);
                    }
                    activateEndReturnPortal(
                            runtime.server().getLevel(Level.END),
                            victoryArena.returnPortalCenter()
                    );
                }
                return;
            }

            final Optional<EyeTraceHistorySnapshot> history =
                    runtime.eyeTraceResults().snapshot(goalRevision);
            if (history.flatMap(
                    EyeTraceHistorySnapshot::estimatedIntersection
            ).isEmpty()) {
                return;
            }
            final List<EyeTraceSnapshot> traces =
                    history.orElseThrow().traces();
            helper.assertTrue(
                    traces.size() >= 2,
                    "Stronghold intersection lacked two fair Eye traces"
            );
            final EyeTraceSnapshot first = traces.get(0);
            final EyeTraceSnapshot second = traces.get(1);
            final double baseline = Math.hypot(
                    second.throwOrigin().x()
                        - first.throwOrigin().x(),
                    second.throwOrigin().z()
                        - first.throwOrigin().z()
            );
            if (!triangulationHandoffValidated) {
                helper.assertTrue(
                        priorCheckpointInstalled
                            && craftSkillObserved
                            && eyeCraftObserved
                            && returnSkillObserved
                            && overworldReturnObserved
                            && triangulationSkillObserved
                            && bodyId.equals(body.getUUID())
                            && body.level().dimension()
                                .equals(Level.OVERWORLD)
                            && baseline >= 250.0
                            && body.getInventory().countItem(
                                Items.ENDER_EYE
                            ) == 12
                            && milestones.contains(
                                SurvivalMilestone
                                    .STRONGHOLD_SEARCH_AREA_TRIANGULATED
                            ),
                        "Live Eye-return chain lost a causal or physical "
                            + "handoff: " + diagnostics()
                );
                triangulationHandoffValidated = true;
                strongholdReachStart = body.position();
                return;
            }

            if (strongholdHandoffValidated) {
                if (activeEndPortalBlocks() == 9) {
                    helper.assertTrue(
                            activationSkillObserved
                                && exactActivationEyeConsumption(body),
                            "End portal activated without the observed "
                                + "skill and exact Eye consumption: "
                                + diagnostics()
                    );
                }
                return;
            }

            final boolean strongholdVisible = runtime.coreFrames()
                    .current()
                    .filter(frame ->
                            frame.dimension().equals(
                                DimensionRef.OVERWORLD
                            )
                    )
                    .stream()
                    .flatMap(frame ->
                            frame.visibleBlockFaces().stream()
                    )
                    .anyMatch(face ->
                            face.blockTypeId().equals(
                                "minecraft:stone_bricks"
                            )
                                || face.blockTypeId().equals(
                                    "minecraft:cracked_stone_bricks"
                                )
                                || face.blockTypeId().equals(
                                    "minecraft:mossy_stone_bricks"
                                )
                    );
            if (!strongholdVisible) {
                return;
            }
            helper.assertTrue(
                    reachSkillObserved
                        && strongholdReachStart != null
                        && Math.hypot(
                            body.getX() - strongholdReachStart.x(),
                            body.getZ() - strongholdReachStart.z()
                        ) >= 180.0
                        && body.getY()
                            <= courseCenter.getY() - 2.0
                        && helper.getLevel().getBlockState(
                            strongholdEvidence
                        ).is(Blocks.STONE_BRICKS)
                        && itemDamage(body, Items.IRON_PICKAXE)
                            > strongholdReachPickaxeDamage
                        && body.getInventory().countItem(Items.TORCH)
                            < strongholdReachTorchCount,
                    "Live stronghold reach lacked physical travel, "
                        + "excavation, lighting, or preserved visible "
                        + "evidence: " + diagnostics()
            );
            strongholdHandoffValidated = true;
            if (!requireVictory) {
                stage = EyeReturnStage.DONE;
                helper.succeed();
            }
        }

        private void assertProbeHealthy() {
            if (!probe.isDone()) {
                return;
            }
            final CapabilityProbeOutcome outcome = probe.join();
            helper.assertTrue(
                    outcome
                        instanceof CapabilityProbeOutcome.Supported,
                    "Configured live model probe failed: " + outcome
            );
        }

        private void assertSetupDeadline(final String message) {
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= SETUP_TIMEOUT_NANOS,
                    message + ": " + diagnostics()
            );
        }

        private void assertNoHumanPlayers() {
            final long humans = runtime.server()
                    .getPlayerList()
                    .getPlayers()
                    .stream()
                    .filter(player -> !player.getUUID().equals(
                            runtime.worldData().companionUuid()
                    ))
                    .count();
            helper.assertTrue(
                    humans == 0L,
                    "Eye-return autonomy retained " + humans
                        + " human player(s) after the command"
            );
        }

        private ServerPlayer body() {
            return AiPlayerManager.onlinePlayer(runtime.server())
                    .orElseThrow(() -> helper.assertionException(
                            "Eye-return companion body disappeared"
                    ));
        }

        private BlockPos nearestPortalBlock(
                final ServerPlayer body
        ) {
            final BlockPos feet = body.blockPosition();
            BlockPos nearest = null;
            double nearestDistance = Double.POSITIVE_INFINITY;
            for (int x = -8; x <= 8; x++) {
                for (int y = -8; y <= 8; y++) {
                    for (int z = -8; z <= 8; z++) {
                        final BlockPos candidate =
                                feet.offset(x, y, z);
                        if (!body.level().getBlockState(candidate)
                                .is(Blocks.NETHER_PORTAL)) {
                            continue;
                        }
                        final double distance =
                                candidate.distToCenterSqr(
                                        body.position()
                                );
                        if (distance < nearestDistance) {
                            nearest = candidate.immutable();
                            nearestDistance = distance;
                        }
                    }
                }
            }
            helper.assertTrue(
                    nearest != null,
                    "Nether arrival exposed no nearby physical portal: "
                        + "body=" + body.position()
                        + ", sourceCourse=" + courseCenter
                        + ", sourcePortal=" + portalInterior
                        + ", expected=" + netherPortalInterior
                        + ", expectedState="
                        + body.level().getBlockState(
                            netherPortalInterior
                        )
                        + ", expectedTopState="
                        + body.level().getBlockState(
                            netherPortalInterior.above(2)
                        )
            );
            return nearest;
        }

        private void pickUpOwned(
                final ServerPlayer body,
                final ItemStack stack
        ) {
            final ItemEntity dropped = new ItemEntity(
                    body.level(),
                    body.getX(),
                    body.getY(),
                    body.getZ(),
                    stack
            );
            helper.assertTrue(
                    body.level().addFreshEntity(dropped),
                    "Could not spawn an ordinary pre-command owned drop"
            );
            dropped.playerTouch(body);
            helper.assertTrue(
                    dropped.isRemoved() || dropped.getItem().isEmpty(),
                    "Companion did not normally pick up its pre-command "
                        + "resource stack"
            );
        }

        private static int itemDamage(
                final ServerPlayer body,
                final net.minecraft.world.item.Item expected
        ) {
            for (int slot = 0;
                    slot < body.getInventory().getContainerSize();
                    slot++) {
                final ItemStack stack =
                        body.getInventory().getItem(slot);
                if (stack.is(expected)) {
                    return stack.getDamageValue();
                }
            }
            return -1;
        }

        private int activeEndPortalBlocks() {
            if (endPortalCenter == null) {
                return 0;
            }
            int blocks = 0;
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (helper.getLevel().getBlockState(
                            endPortalCenter.offset(x, 0, z)
                    ).is(Blocks.END_PORTAL)) {
                        blocks++;
                    }
                }
            }
            return blocks;
        }

        private int filledEndPortalFrames() {
            if (endPortalCenter == null) {
                return 0;
            }
            int filled = 0;
            for (int offset = -1; offset <= 1; offset++) {
                filled += hasEndPortalEye(
                        endPortalCenter.offset(offset, 0, -2)
                );
                filled += hasEndPortalEye(
                        endPortalCenter.offset(offset, 0, 2)
                );
                filled += hasEndPortalEye(
                        endPortalCenter.offset(-2, 0, offset)
                );
                filled += hasEndPortalEye(
                        endPortalCenter.offset(2, 0, offset)
                );
            }
            return filled;
        }

        private int hasEndPortalEye(final BlockPos position) {
            final var state =
                    helper.getLevel().getBlockState(position);
            return state.is(Blocks.END_PORTAL_FRAME)
                    && state.getValue(
                        net.minecraft.world.level.block
                            .EndPortalFrameBlock.HAS_EYE
                    )
                    ? 1
                    : 0;
        }

        private boolean exactActivationEyeConsumption(
                final ServerPlayer body
        ) {
            if (activationEyeCountBefore < 0
                    || activationFilledFramesBefore < 0
                    || activationFilledFramesBefore
                        > END_PORTAL_FRAME_COUNT) {
                return false;
            }
            final int consumed =
                    activationEyeCountBefore
                        - body.getInventory().countItem(
                            Items.ENDER_EYE
                        );
            return consumed
                    == END_PORTAL_FRAME_COUNT
                        - activationFilledFramesBefore;
        }

        private static void buildActivePortal(
                final net.minecraft.server.level.ServerLevel level,
                final BlockPos interior
        ) {
            for (int x = -1; x <= 2; x++) {
                level.setBlockAndUpdate(
                        interior.offset(x, -1, 0),
                        Blocks.OBSIDIAN.defaultBlockState()
                );
                level.setBlockAndUpdate(
                        interior.offset(x, 3, 0),
                        Blocks.OBSIDIAN.defaultBlockState()
                );
            }
            for (int y = 0; y <= 2; y++) {
                level.setBlockAndUpdate(
                        interior.offset(-1, y, 0),
                        Blocks.OBSIDIAN.defaultBlockState()
                );
                level.setBlockAndUpdate(
                        interior.offset(2, y, 0),
                        Blocks.OBSIDIAN.defaultBlockState()
                );
                for (int x = 0; x <= 1; x++) {
                    level.setBlockAndUpdate(
                            interior.offset(x, y, 0),
                            Blocks.AIR.defaultBlockState()
                    );
                }
            }
            /*
             * Let vanilla's fire placement validate the frame and create
             * the portal surface. Directly writing NETHER_PORTAL blocks looks
             * identical but does not exercise the complete portal creation
             * lifecycle used by normal flint-and-steel ignition.
             */
            level.setBlockAndUpdate(
                    interior,
                    Blocks.FIRE.defaultBlockState()
            );
        }

        private static void setUnlessPortal(
                final ServerPlayer body,
                final BlockPos position,
                final net.minecraft.world.level.block.state.BlockState state
        ) {
            final var existing = body.level().getBlockState(position);
            if (!existing.is(Blocks.NETHER_PORTAL)
                    && !existing.is(Blocks.OBSIDIAN)) {
                body.level().setBlockAndUpdate(position, state);
            }
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

        private void setGenerateStructures(final boolean enabled) {
            final var settings = runtime.server()
                    .getWorldGenSettings();
            ((WorldGenSettingsAccessor) (Object) settings)
                    .mcai$setOptions(
                            settings.options()
                                .withStructures(enabled)
                    );
        }

        private String diagnostics() {
            final ServerPlayer body = AiPlayerManager
                    .onlinePlayer(runtime.server())
                    .orElse(null);
            return "stage=" + stage
                    + ", supervisor="
                    + runtime.skillSupervisor().snapshot()
                    + ", goal=" + runtime.goals().snapshot()
                    + ", body="
                    + (body == null
                        ? "absent"
                        : body.level().dimension().identifier()
                            + "@" + body.position())
                    + ", powder="
                    + (body == null
                        ? -1
                        : body.getInventory().countItem(
                            Items.BLAZE_POWDER
                        ))
                    + ", pearls="
                    + (body == null
                        ? -1
                        : body.getInventory().countItem(
                            Items.ENDER_PEARL
                        ))
                    + ", eyes="
                    + (body == null
                        ? -1
                        : body.getInventory().countItem(
                            Items.ENDER_EYE
                        ))
                    + ", netherArrival=" + netherArrival
                    + ", expectedNetherPortal="
                    + netherPortalInterior
                    + ", flags=[craftSkill=" + craftSkillObserved
                    + ",eyeCraft=" + eyeCraftObserved
                    + ",returnSkill=" + returnSkillObserved
                    + ",overworld=" + overworldReturnObserved
                    + ",triangulation="
                    + triangulationSkillObserved
                    + ",triangulationValidated="
                    + triangulationHandoffValidated
                    + ",reach=" + reachSkillObserved
                    + ",strongholdValidated="
                    + strongholdHandoffValidated
                    + ",portalSearch="
                    + portalRoomSearchObserved
                    + ",activation=" + activationSkillObserved
                    + ",endEntry=" + endEntrySkillObserved
                    + ",fight=" + fightSkillObserved
                    + ",endReturn=" + endReturnSkillObserved
                    + ",arena=" + endArenaPrepared + "]"
                    + ", strongholdTarget=" + strongholdTarget
                    + ", strongholdEvidence=" + strongholdEvidence
                    + ", endPortalCenter=" + endPortalCenter
                    + ", endPortalBlocks="
                    + activeEndPortalBlocks()
                    + ", activationEyesBefore="
                    + activationEyeCountBefore
                    + ", activationFilledBefore="
                    + activationFilledFramesBefore
                    + ", activationEyesConsumed="
                    + (body == null
                        || activationEyeCountBefore < 0
                        ? -1
                        : activationEyeCountBefore
                            - body.getInventory().countItem(
                                Items.ENDER_EYE
                            ))
                    + ", dragonHealth="
                    + (victoryArena == null
                        ? "none"
                        : victoryArena.dragon().getHealth())
                    + ", reachStart=" + strongholdReachStart
                    + ", pickaxeDamage="
                    + (body == null
                        ? -1
                        : itemDamage(body, Items.IRON_PICKAXE))
                    + ", torches="
                    + (body == null
                        ? -1
                        : body.getInventory().countItem(Items.TORCH))
                    + ", milestones="
                    + (goalRevision < 0L
                        ? List.of()
                        : runtime.worldData()
                            .verifiedRouteProgress(goalRevision)
                            .milestones())
                    + ", eyeHistory="
                    + (goalRevision < 0L
                        ? Optional.empty()
                        : runtime.eyeTraceResults()
                            .snapshot(goalRevision));
        }

        private void cleanup() {
            if (cleaned) {
                return;
            }
            cleaned = true;
            finishScenarioGoal(runtime);
            if (humanSession != null) {
                humanSession.close();
                humanSession = null;
            }
            setNaturalSpawning(
                    originalSpawnMobs,
                    originalSpawnMonsters
            );
            setGenerateStructures(originalGenerateStructures);
            if (AiPlayerManager.status(runtime.server()).state()
                    != SessionState.ABSENT) {
                AiPlayerManager.requestRemove(runtime.server());
            }
        }
    }

    private enum EyeReturnStage {
        BODY,
        ENTERING_NETHER,
        NETHER_READY,
        GOAL,
        AUTONOMOUS_CHAIN,
        DONE
    }

    /**
     * Release-excluded causal gate for the stronghold-interior completion
     * phase. Test setup owns only the already-discovered stronghold maze,
     * late-route inventory, and prior route checkpoint. The entry-only
     * variant performs no fixture mutation after the checkpoint. The victory
     * variant creates its deterministic release-excluded End combat arena
     * only after the body has genuinely crossed the activated portal; that
     * setup supplies targets but never selects a model skill, credits damage,
     * kills the dragon, or moves the body through a portal.
     */
    private static final class LiveStrongholdPortalRoomScenario {
        private static final long SEARCH_TIMEOUT_NANOS =
                java.time.Duration.ofMinutes(6).toNanos();
        private static final long ACTIVATION_TIMEOUT_NANOS =
                java.time.Duration.ofSeconds(60).toNanos();
        private static final long ENTRY_TIMEOUT_NANOS =
                java.time.Duration.ofSeconds(150).toNanos();
        private static final long FIGHT_TIMEOUT_NANOS =
                java.time.Duration.ofSeconds(90).toNanos();
        private static final long RETURN_TIMEOUT_NANOS =
                java.time.Duration.ofSeconds(150).toNanos();

        private final GameTestHelper helper;
        private final ServerRuntime runtime;
        private final long createdAt;
        private final boolean originalSpawnMobs;
        private final boolean originalSpawnMonsters;
        private final boolean requireVictory;

        private StrongholdPortalRoomStage stage =
                StrongholdPortalRoomStage.BODY;
        private CompletableFuture<CapabilityProbeOutcome> probe;
        private PlacedHuman humanSession;
        private EndVictoryArena victoryArena;
        private UUID bodyId;
        private BlockPos searchStart;
        private BlockPos portalCenter;
        private BlockPos mazeDeadEnd;
        private BlockPos mazeSecondTurn;
        private Vec3 physicalSearchStart;
        private long minimumFixtureObservationRevision;
        private long goalRevisionBefore = -1L;
        private long goalRevision = -1L;
        private long enteredEndAt = -1L;
        private long stageStartedNanos;
        private boolean priorRouteCheckpointInstalled;
        private boolean searchSkillObserved;
        private boolean deadEndVisited;
        private boolean secondTurnVisited;
        private boolean activationSkillObserved;
        private boolean entrySkillObserved;
        private boolean fightSkillObserved;
        private boolean returnSkillObserved;
        private boolean cleaned;

        private LiveStrongholdPortalRoomScenario(
                final GameTestHelper helper,
                final ServerRuntime runtime,
                final boolean requireVictory
        ) {
            this.helper = helper;
            this.runtime = runtime;
            this.requireVictory = requireVictory;
            createdAt = helper.getTick();
            stageStartedNanos = System.nanoTime();
            originalSpawnMobs = helper.getLevel()
                    .getGameRules()
                    .get(GameRules.SPAWN_MOBS);
            originalSpawnMonsters = helper.getLevel()
                    .getGameRules()
                    .get(GameRules.SPAWN_MONSTERS);
        }

        private void start() {
            finishScenarioGoal(runtime);
            setNaturalSpawning(false, false);
            final var status = AiPlayerManager.status(runtime.server());
            if (status.state() == SessionState.ABSENT) {
                helper.assertTrue(
                        AiPlayerManager.requestSpawn(
                                runtime.server()
                        ).accepted(),
                        "Stronghold portal-room companion spawn was "
                            + "rejected"
                );
            }
        }

        private void tick() {
            if (humanSession != null) {
                humanSession.tick();
            }
            switch (stage) {
                case BODY -> waitForBody();
                case PROBE -> waitForProbe();
                case INITIAL_FRAME -> waitForInitialFrame();
                case GOAL -> waitForGoal();
                case SEARCH_SKILL -> waitForSearchSkill();
                case SEARCH_AND_ACTIVATION ->
                        waitForSearchAndActivationHandoff();
                case ACTIVATE -> waitForActivation();
                case ENTRY_SKILL -> waitForEntrySkill();
                case ENTER -> waitForEntry();
                case VICTORY_VISIBLE -> waitForVictoryVisible();
                case FIGHT_SKILL -> waitForFightSkill();
                case FIGHT -> waitForDragonKill();
                case RETURN -> waitForReturn();
                case DONE -> {
                    // GameTest is already terminal.
                }
            }
        }

        private void waitForBody() {
            final var status = AiPlayerManager.status(runtime.server());
            helper.assertTrue(
                    status.state() != SessionState.FAILED,
                    "Stronghold portal-room companion body failed"
            );
            if (status.state() != SessionState.ACTIVE
                    || !status.online()) {
                helper.assertTrue(
                        helper.getTick() - createdAt
                            <= BODY_TIMEOUT_TICKS,
                        "Stronghold portal-room companion body timed out"
                );
                return;
            }
            final ServerPlayer body = body();
            bodyId = body.getUUID();
            prepareStrongholdMaze(body);
            probe = probeOrReuseVerifiedModel(runtime);
            stage = StrongholdPortalRoomStage.PROBE;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForProbe() {
            assertWithin(
                    MODEL_TIMEOUT_NANOS,
                    "Stronghold portal-room model probe timed out"
            );
            if (!probe.isDone()) {
                return;
            }
            final CapabilityProbeOutcome outcome = probe.join();
            helper.assertTrue(
                    outcome
                        instanceof CapabilityProbeOutcome.Supported,
                    "Configured live model probe failed: " + outcome
            );
            stage = StrongholdPortalRoomStage.INITIAL_FRAME;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForInitialFrame() {
            assertWithin(
                    MODEL_TIMEOUT_NANOS,
                    "Stronghold maze never became fair first-person "
                        + "evidence: " + diagnostics()
            );
            final ServerPlayer body = body();
            if (!body.onGround()) {
                return;
            }
            final var frame = runtime.coreFrames().current();
            if (frame.isEmpty()
                    || frame.orElseThrow().observationRevision()
                        < minimumFixtureObservationRevision
                    || !frame.orElseThrow().dimension()
                        .equals(DimensionRef.OVERWORLD)
                    || !bodyId.equals(
                        frame.orElseThrow().playerId()
                    )
                    || !hasVisibleBlock(
                        frame.orElseThrow(),
                        "minecraft:stone_bricks"
                    )) {
                return;
            }
            helper.assertTrue(
                    !hasVisibleBlock(
                            frame.orElseThrow(),
                            "minecraft:end_portal_frame"
                    ),
                    "Hidden portal frame leaked into the initial fair "
                        + "first-person frame"
            );
            humanSession = PlacedHuman.create(
                    helper,
                    runtime,
                    body.position().add(-2.0D, 0.0D, -2.0D)
            );
            final ServerPlayer human = humanSession.player();
            helper.assertTrue(
                    CompanionCommandAccess.mayAdmin(
                            human.createCommandSourceStack()
                    ),
                    "Logged-in stronghold test player lacked task "
                        + "permission"
            );
            goalRevisionBefore =
                    runtime.goals().snapshot().revision();
            final String request = requireVictory
                    ? runtime.worldData().displayName()
                        + "，请继续通关Minecraft：在这个要塞中找到"
                        + "末地传送门房间，放入末影之眼激活并进入"
                        + "末地，击败末影龙，然后进入中央返回"
                        + "传送门回到主世界。"
                    : runtime.worldData().displayName()
                        + "，请继续通关Minecraft：在这个要塞中找到"
                        + "末地传送门房间，放入末影之眼激活传送门，"
                        + "然后进入末地。";
            final Component submitted =
                    ForgeHooks.onServerChatSubmittedEvent(
                            human,
                            Component.literal(request)
                    );
            helper.assertTrue(
                    submitted != null,
                    "Companion cancelled the stronghold portal-room "
                        + "chat command"
            );
            humanSession.close();
            humanSession = null;
            stage = StrongholdPortalRoomStage.GOAL;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForGoal() {
            assertNoHumanPlayers();
            assertWithin(
                    MODEL_TIMEOUT_NANOS,
                    "Live model did not classify the stronghold "
                        + "portal-room task: " + diagnostics()
            );
            final GoalSnapshot goal = runtime.goals().snapshot();
            if (goal.revision() == goalRevisionBefore) {
                return;
            }
            helper.assertTrue(
                    goal.revision() > goalRevisionBefore
                        && goal.status() == GoalStatus.RUNNING
                        && goal.goal().contains("通关Minecraft")
                        && goal.goal().contains("末地传送门"),
                    "Stronghold chat did not become the requested "
                        + "completion goal: " + goal
            );
            installPriorRouteCheckpoint(goal);
            goalRevision = goal.revision();
            physicalSearchStart = body().position();
            stage = StrongholdPortalRoomStage.SEARCH_SKILL;
            stageStartedNanos = System.nanoTime();
        }

        private void installPriorRouteCheckpoint(
                final GoalSnapshot goal
        ) {
            runtime.worldData().markVerifiedRouteMilestones(
                    goal.revision(),
                    EnumSet.of(
                            SurvivalMilestone.BODY_ACTIVE,
                            SurvivalMilestone.WOOD_OBTAINED,
                            SurvivalMilestone.BASIC_CRAFTING_READY,
                            SurvivalMilestone.STONE_TOOL_OBTAINED,
                            SurvivalMilestone.FOOD_SECURED,
                            SurvivalMilestone.IRON_TOOLKIT_OBTAINED,
                            SurvivalMilestone.NETHER_ENTERED,
                            SurvivalMilestone.BLAZE_MATERIAL_OBTAINED,
                            SurvivalMilestone.ENDER_PEARL_OBTAINED,
                            SurvivalMilestone.EYE_OF_ENDER_CRAFTED,
                            SurvivalMilestone
                                .STRONGHOLD_BEARING_MEASURED,
                            SurvivalMilestone
                                .STRONGHOLD_SEARCH_AREA_TRIANGULATED
                    )
            );
            priorRouteCheckpointInstalled = true;
        }

        private void waitForSearchSkill() {
            assertNoHumanPlayers();
            observeMazeVisits();
            final SkillSupervisor.Snapshot snapshot =
                    runtime.skillSupervisor().snapshot();
            if ("search_stronghold_portal_room".equals(
                    snapshot.skillName()
            ) && snapshot.boundGoalRevision() == goalRevision) {
                helper.assertTrue(
                        snapshot.state()
                            == SkillSupervisor.State.RUNNING,
                        "Portal-room search became terminal before it "
                            + "was observed running: " + diagnostics()
                );
                searchSkillObserved = true;
                stage =
                        StrongholdPortalRoomStage.SEARCH_AND_ACTIVATION;
                stageStartedNanos = System.nanoTime();
                return;
            }
            if (snapshot.boundGoalRevision() == goalRevision
                    && snapshot.state()
                        == SkillSupervisor.State.RUNNING) {
                helper.fail(
                        "Live model selected the wrong stronghold "
                            + "interior skill: " + diagnostics()
                );
                return;
            }
            assertWithin(
                    MODEL_TIMEOUT_NANOS,
                    "Live model did not select "
                        + "search_stronghold_portal_room: "
                        + diagnostics()
            );
        }

        private void waitForSearchAndActivationHandoff() {
            assertNoHumanPlayers();
            final ServerPlayer body = body();
            helper.assertTrue(
                    bodyId.equals(body.getUUID())
                        && body.level().dimension()
                            .equals(Level.OVERWORLD)
                        && body.isAlive()
                        && !body.isDeadOrDying(),
                    "Stronghold search lost the original living "
                        + "Overworld body: " + diagnostics()
            );
            observeMazeVisits();
            final SkillSupervisor.Snapshot snapshot =
                    runtime.skillSupervisor().snapshot();
            if ("search_stronghold_portal_room".equals(
                    snapshot.skillName()
            ) && snapshot.boundGoalRevision() == goalRevision
                    && snapshot.state()
                        == SkillSupervisor.State.FAILED) {
                helper.fail(
                        "Physical stronghold portal-room search failed: "
                            + diagnostics()
                );
                return;
            }
            if ("activate_observed_end_portal".equals(
                    snapshot.skillName()
            ) && snapshot.boundGoalRevision() == goalRevision
                    && snapshot.state()
                        == SkillSupervisor.State.RUNNING) {
                helper.assertTrue(
                        searchSkillObserved
                            && priorRouteCheckpointInstalled
                            && deadEndVisited
                            && secondTurnVisited
                            && horizontalDistance(
                                body.position(),
                                physicalSearchStart
                            ) >= 8.0D,
                        "Activation handoff lacked physical DFS, "
                            + "dead-end backtracking, or route evidence: "
                            + diagnostics()
                );
                activationSkillObserved = true;
                stage = StrongholdPortalRoomStage.ACTIVATE;
                stageStartedNanos = System.nanoTime();
                return;
            }
            if (snapshot.boundGoalRevision() == goalRevision
                    && snapshot.state()
                        == SkillSupervisor.State.RUNNING
                    && !"search_stronghold_portal_room".equals(
                        snapshot.skillName()
                    )) {
                helper.fail(
                        "Live model skipped or replaced the portal-room "
                            + "activation handoff: " + diagnostics()
                );
                return;
            }
            assertWithin(
                    SEARCH_TIMEOUT_NANOS,
                    "Live-model stronghold search/activation handoff "
                        + "timed out: " + diagnostics()
            );
        }

        private void waitForActivation() {
            assertNoHumanPlayers();
            observeMazeVisits();
            final SkillSupervisor.Snapshot snapshot =
                    runtime.skillSupervisor().snapshot();
            if ("activate_observed_end_portal".equals(
                    snapshot.skillName()
            ) && snapshot.boundGoalRevision() == goalRevision
                    && snapshot.state()
                        == SkillSupervisor.State.FAILED) {
                helper.fail(
                        "Live-model End portal activation failed: "
                            + diagnostics()
                );
                return;
            }
            if (activePortalBlocks() == 9) {
                final ServerPlayer body = body();
                helper.assertTrue(
                        activationSkillObserved
                            && body.getInventory().countItem(
                                Items.ENDER_EYE
                            ) == 0,
                        "Portal activated without the observed model "
                            + "skill or exact Eye consumption: "
                            + diagnostics()
                );
                stage = StrongholdPortalRoomStage.ENTRY_SKILL;
                stageStartedNanos = System.nanoTime();
                return;
            }
            assertWithin(
                    ACTIVATION_TIMEOUT_NANOS,
                    "Live-model End portal activation timed out: "
                        + diagnostics()
            );
        }

        private void waitForEntrySkill() {
            assertNoHumanPlayers();
            final ServerPlayer body = body();
            helper.assertTrue(
                    bodyId.equals(body.getUUID())
                        && body.level().dimension()
                            .equals(Level.OVERWORLD),
                    "Companion entered the End before the entry skill "
                        + "was observed: " + diagnostics()
            );
            final SkillSupervisor.Snapshot snapshot =
                    runtime.skillSupervisor().snapshot();
            if ("find_and_enter_observed_portal".equals(
                    snapshot.skillName()
            ) && snapshot.boundGoalRevision() == goalRevision
                    && snapshot.state()
                        == SkillSupervisor.State.RUNNING) {
                entrySkillObserved = true;
                stage = StrongholdPortalRoomStage.ENTER;
                stageStartedNanos = System.nanoTime();
                return;
            }
            if (snapshot.boundGoalRevision() == goalRevision
                    && snapshot.state()
                        == SkillSupervisor.State.RUNNING
                    && !"activate_observed_end_portal".equals(
                        snapshot.skillName()
                    )) {
                helper.fail(
                        "Live model selected the wrong post-activation "
                            + "skill: " + diagnostics()
                );
                return;
            }
            assertWithin(
                    MODEL_TIMEOUT_NANOS,
                    "Live model did not select "
                        + "find_and_enter_observed_portal: "
                        + diagnostics()
            );
        }

        private void waitForEntry() {
            assertNoHumanPlayers();
            final ServerPlayer body = body();
            helper.assertTrue(
                    bodyId.equals(body.getUUID()),
                    "End entry replaced the companion body"
            );
            if (body.level().dimension().equals(Level.END)) {
                helper.assertTrue(
                        priorRouteCheckpointInstalled
                            && searchSkillObserved
                            && deadEndVisited
                            && secondTurnVisited
                            && activationSkillObserved
                            && entrySkillObserved
                            && activePortalBlocks() == 9
                            && body.getInventory().countItem(
                                Items.ENDER_EYE
                            ) == 0,
                        "End entry lacked the complete model/physical "
                            + "causal chain: " + diagnostics()
                );
                if (requireVictory) {
                    enteredEndAt = helper.getTick();
                    victoryArena = prepareEndVictoryArena(
                            helper,
                            runtime,
                            body,
                            false,
                            false
                    );
                    stage =
                            StrongholdPortalRoomStage.VICTORY_VISIBLE;
                    stageStartedNanos = System.nanoTime();
                } else {
                    stage = StrongholdPortalRoomStage.DONE;
                    helper.succeed();
                }
                return;
            }
            final SkillSupervisor.Snapshot snapshot =
                    runtime.skillSupervisor().snapshot();
            if ("find_and_enter_observed_portal".equals(
                    snapshot.skillName()
            ) && snapshot.boundGoalRevision() == goalRevision
                    && snapshot.state()
                        == SkillSupervisor.State.FAILED) {
                helper.fail(
                        "Live-model End portal entry failed: "
                            + diagnostics()
                );
                return;
            }
            assertWithin(
                    ENTRY_TIMEOUT_NANOS,
                    "Live-model End portal entry timed out: "
                        + diagnostics()
            );
        }

        private void waitForVictoryVisible() {
            assertNoHumanPlayers();
            final ServerPlayer body = body();
            helper.assertTrue(
                    bodyId.equals(body.getUUID())
                        && body.level().dimension().equals(Level.END),
                    "Continuous victory body left the End before combat: "
                        + diagnostics()
            );
            final var frame = runtime.coreFrames().current();
            if (frame.isEmpty()
                    || !frame.orElseThrow().dimension()
                        .equals(DimensionRef.END)
                    || !frame.orElseThrow().onGround()
                    || frame.orElseThrow().visibleEntities()
                        .stream()
                        .noneMatch(entity ->
                                entity.entityTypeId().equals(
                                    "minecraft:end_crystal"
                                )
                        )) {
                assertWithin(
                        MODEL_TIMEOUT_NANOS,
                        "Continuous victory arena never became fair "
                            + "first-person evidence: " + diagnostics()
                );
                return;
            }
            stage = StrongholdPortalRoomStage.FIGHT_SKILL;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForFightSkill() {
            assertNoHumanPlayers();
            final SkillSupervisor.Snapshot snapshot =
                    runtime.skillSupervisor().snapshot();
            if ("fight_ender_dragon".equals(
                    snapshot.skillName()
            ) && snapshot.boundGoalRevision() == goalRevision
                    && snapshot.state()
                        == SkillSupervisor.State.RUNNING) {
                fightSkillObserved = true;
                stage = StrongholdPortalRoomStage.FIGHT;
                stageStartedNanos = System.nanoTime();
                return;
            }
            if (snapshot.boundGoalRevision() == goalRevision
                    && snapshot.state()
                        == SkillSupervisor.State.RUNNING) {
                helper.fail(
                        "Live model selected the wrong post-entry "
                            + "completion skill: " + diagnostics()
                );
                return;
            }
            assertWithin(
                    MODEL_TIMEOUT_NANOS,
                    "Live model did not select fight_ender_dragon: "
                        + diagnostics()
            );
        }

        private void waitForDragonKill() {
            assertNoHumanPlayers();
            final SkillSupervisor.Snapshot snapshot =
                    runtime.skillSupervisor().snapshot();
            if ("fight_ender_dragon".equals(
                    snapshot.skillName()
            ) && snapshot.boundGoalRevision() == goalRevision
                    && snapshot.state()
                        == SkillSupervisor.State.FAILED) {
                helper.fail(
                        "Continuous live-model dragon fight failed: "
                            + diagnostics()
                );
                return;
            }
            final var milestones = runtime.worldData()
                    .verifiedRouteProgress(goalRevision)
                    .milestones();
            if (milestones.contains(
                    SurvivalMilestone.DRAGON_KILLED
            )) {
                final ServerPlayer body = body();
                helper.assertTrue(
                        fightSkillObserved
                            && victoryArena != null
                            && (!victoryArena.crystal().isAlive()
                                || victoryArena.crystal().isRemoved())
                            && body.level().getBlockState(
                                victoryArena.cageBar()
                            ).isAir()
                            && body.getInventory()
                                .countItem(Items.ARROW) < 64,
                        "Dragon milestone lacked physical combat "
                            + "evidence: " + diagnostics()
                );
                if (!victoryArena.dragon().isRemoved()) {
                    victoryArena.dragon().setNoAi(false);
                }
                activateEndReturnPortal(
                        runtime.server().getLevel(Level.END),
                        victoryArena.returnPortalCenter()
                );
                stage = StrongholdPortalRoomStage.RETURN;
                stageStartedNanos = System.nanoTime();
                return;
            }
            assertWithin(
                    FIGHT_TIMEOUT_NANOS,
                    "Continuous live-model dragon fight timed out: "
                        + diagnostics()
            );
        }

        private void waitForReturn() {
            assertNoHumanPlayers();
            final SkillSupervisor.Snapshot snapshot =
                    runtime.skillSupervisor().snapshot();
            if ("find_and_enter_observed_portal".equals(
                    snapshot.skillName()
            ) && snapshot.boundGoalRevision() == goalRevision) {
                returnSkillObserved = true;
            }
            final ServerPlayer body = body();
            final var milestones = runtime.worldData()
                    .verifiedRouteProgress(goalRevision)
                    .milestones();
            if (body.level().dimension().equals(Level.OVERWORLD)
                    && milestones.contains(
                        SurvivalMilestone.RETURNED_FROM_END
                    )) {
                helper.assertTrue(
                        bodyId.equals(body.getUUID())
                            && searchSkillObserved
                            && activationSkillObserved
                            && entrySkillObserved
                            && fightSkillObserved
                            && returnSkillObserved,
                        "Continuous completion lost one body/model "
                            + "handoff: " + diagnostics()
                );
                stage = StrongholdPortalRoomStage.DONE;
                helper.succeed();
                return;
            }
            if ("find_and_enter_observed_portal".equals(
                    snapshot.skillName()
            ) && snapshot.boundGoalRevision() == goalRevision
                    && snapshot.state()
                        == SkillSupervisor.State.FAILED) {
                helper.fail(
                        "Continuous End return skill failed: "
                            + diagnostics()
                );
                return;
            }
            assertWithin(
                    RETURN_TIMEOUT_NANOS,
                    "Continuous live-model End return timed out: "
                        + diagnostics()
            );
        }

        private void prepareStrongholdMaze(
                final ServerPlayer body
        ) {
            searchStart = body.blockPosition();
            portalCenter = searchStart.offset(9, 0, 25);
            mazeDeadEnd = searchStart.offset(0, 0, 12);
            mazeSecondTurn = searchStart.offset(9, 0, 8);
            final var level = helper.getLevel();

            body.setGameMode(GameType.SURVIVAL);
            helper.assertTrue(
                    body.gameMode.getGameModeForPlayer()
                            == GameType.SURVIVAL
                        && !body.getAbilities().instabuild,
                    "Stronghold portal-room fixture did not enter "
                        + "survival mode"
            );

            for (int x = -4; x <= 14; x++) {
                for (int z = -3; z <= 31; z++) {
                    level.setBlockAndUpdate(
                            searchStart.offset(x, -1, z),
                            Blocks.STONE_BRICKS
                                .defaultBlockState()
                    );
                    for (int y = 0; y <= 4; y++) {
                        level.setBlockAndUpdate(
                                searchStart.offset(x, y, z),
                                Blocks.STONE_BRICKS
                                    .defaultBlockState()
                        );
                    }
                }
            }
            for (int z = 0; z <= 12; z++) {
                carveStrongholdInterior(0, z);
            }
            for (int x = 0; x <= 9; x++) {
                carveStrongholdInterior(x, 8);
            }
            for (int z = 8; z <= 22; z++) {
                carveStrongholdInterior(9, z);
            }
            for (int x = 6; x <= 12; x++) {
                for (int z = 21; z <= 29; z++) {
                    carveStrongholdInterior(x, z);
                }
            }
            for (int offset = -1; offset <= 1; offset++) {
                setFrame(
                        portalCenter.offset(offset, 0, -2),
                        Direction.SOUTH
                );
                setFrame(
                        portalCenter.offset(offset, 0, 2),
                        Direction.NORTH
                );
                setFrame(
                        portalCenter.offset(-2, 0, offset),
                        Direction.EAST
                );
                setFrame(
                        portalCenter.offset(2, 0, offset),
                        Direction.WEST
                );
            }

            if (requireVictory) {
                equipEndVictoryBody(body, true);
            } else {
                body.getInventory().clearContent();
                body.getInventory().setItem(
                        0,
                        new ItemStack(Items.ENDER_EYE, 12)
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
                body.getInventory().setItem(
                        4,
                        new ItemStack(Items.COBBLESTONE, 64)
                );
                body.getInventory().setSelectedSlot(0);
                body.setItemSlot(
                        EquipmentSlot.OFFHAND,
                        new ItemStack(Items.SHIELD)
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
            }
            body.inventoryMenu.broadcastChanges();
            body.setHealth(body.getMaxHealth());
            body.getFoodData().setFoodLevel(20);
            body.getFoodData().setSaturation(5.0F);
            body.setDeltaMovement(Vec3.ZERO);
            body.lookAt(
                    EntityAnchorArgument.Anchor.EYES,
                    Vec3.atCenterOf(mazeDeadEnd)
                        .add(0.0D, 0.2D, 0.0D)
            );
            body.setYHeadRot(body.getYRot());
            minimumFixtureObservationRevision =
                    runtime.coreFrames()
                        .current()
                        .map(frame ->
                            frame.observationRevision() + 1L
                        )
                        .orElse(0L);
        }

        private void carveStrongholdInterior(
                final int offsetX,
                final int offsetZ
        ) {
            for (int y = 0; y <= 3; y++) {
                helper.getLevel().setBlockAndUpdate(
                        searchStart.offset(offsetX, y, offsetZ),
                        Blocks.AIR.defaultBlockState()
                );
            }
        }

        private void setFrame(
                final BlockPos position,
                final Direction facing
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
                            false
                        )
            );
        }

        private void observeMazeVisits() {
            final Vec3 position = body().position();
            if (horizontalDistance(
                    position,
                    Vec3.atCenterOf(mazeDeadEnd)
            ) <= 1.35D) {
                deadEndVisited = true;
            }
            if (deadEndVisited
                    && horizontalDistance(
                        position,
                        Vec3.atCenterOf(mazeSecondTurn)
                    ) <= 1.35D) {
                secondTurnVisited = true;
            }
        }

        private int activePortalBlocks() {
            int blocks = 0;
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (helper.getLevel().getBlockState(
                            portalCenter.offset(x, 0, z)
                    ).is(Blocks.END_PORTAL)) {
                        blocks++;
                    }
                }
            }
            return blocks;
        }

        private static double horizontalDistance(
                final Vec3 first,
                final Vec3 second
        ) {
            return Math.hypot(
                    first.x() - second.x(),
                    first.z() - second.z()
            );
        }

        private static boolean hasVisibleBlock(
                final dev.mcai.companion.skills.core.CoreSkillFrame
                        frame,
                final String blockTypeId
        ) {
            return frame.visibleBlockFaces()
                    .stream()
                    .anyMatch(face ->
                            face.blockTypeId().equals(blockTypeId)
                    );
        }

        private ServerPlayer body() {
            return AiPlayerManager.onlinePlayer(runtime.server())
                    .orElseThrow(() -> helper.assertionException(
                            "Stronghold portal-room companion body "
                                + "disappeared"
                    ));
        }

        private void assertNoHumanPlayers() {
            final long humans = runtime.server()
                    .getPlayerList()
                    .getPlayers()
                    .stream()
                    .filter(player -> !player.getUUID().equals(
                            runtime.worldData().companionUuid()
                    ))
                    .count();
            helper.assertTrue(
                    humans == 0L,
                    "Stronghold portal-room autonomy retained "
                        + humans + " human player(s)"
            );
        }

        private void assertWithin(
                final long timeoutNanos,
                final String message
        ) {
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= timeoutNanos,
                    message
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

        private String diagnostics() {
            final ServerPlayer body = AiPlayerManager
                    .onlinePlayer(runtime.server())
                    .orElse(null);
            return "stage=" + stage
                    + ", supervisor="
                    + runtime.skillSupervisor().snapshot()
                    + ", goal=" + runtime.goals().snapshot()
                    + ", body="
                    + (body == null
                        ? "absent"
                        : body.level().dimension().identifier()
                            + "@" + body.position())
                    + ", eyes="
                    + (body == null
                        ? -1
                        : body.getInventory().countItem(
                            Items.ENDER_EYE
                        ))
                    + ", portalBlocks=" + activePortalBlocks()
                    + ", flags=[checkpoint="
                    + priorRouteCheckpointInstalled
                    + ",search=" + searchSkillObserved
                    + ",deadEnd=" + deadEndVisited
                    + ",secondTurn=" + secondTurnVisited
                    + ",activation=" + activationSkillObserved
                    + ",entry=" + entrySkillObserved
                    + ",fight=" + fightSkillObserved
                    + ",return=" + returnSkillObserved + "]"
                    + ", start=" + searchStart
                    + ", portalCenter=" + portalCenter
                    + ", enteredEndAt=" + enteredEndAt
                    + ", dragonHealth="
                    + (victoryArena == null
                        ? "none"
                        : victoryArena.dragon().getHealth())
                    + ", milestones="
                    + (goalRevision < 0L
                        ? List.of()
                        : runtime.worldData()
                            .verifiedRouteProgress(goalRevision)
                            .milestones());
        }

        private void cleanup() {
            if (cleaned) {
                return;
            }
            cleaned = true;
            finishScenarioGoal(runtime);
            if (humanSession != null) {
                humanSession.close();
                humanSession = null;
            }
            setNaturalSpawning(
                    originalSpawnMobs,
                    originalSpawnMonsters
            );
            if (AiPlayerManager.status(runtime.server()).state()
                    != SessionState.ABSENT) {
                AiPlayerManager.requestRemove(runtime.server());
            }
        }
    }

    private enum StrongholdPortalRoomStage {
        BODY,
        PROBE,
        INITIAL_FRAME,
        GOAL,
        SEARCH_SKILL,
        SEARCH_AND_ACTIVATION,
        ACTIVATE,
        ENTRY_SKILL,
        ENTER,
        VICTORY_VISIBLE,
        FIGHT_SKILL,
        FIGHT,
        RETURN,
        DONE
    }

    private static final class LiveEndPortalActivationScenario {
        private static final long ACTIVATION_TIMEOUT_NANOS =
                java.time.Duration.ofSeconds(45).toNanos();
        private static final long ENTRY_TIMEOUT_NANOS =
                java.time.Duration.ofSeconds(120).toNanos();
        private static final long CHAIN_FIGHT_TIMEOUT_NANOS =
                java.time.Duration.ofSeconds(90).toNanos();
        private static final long CHAIN_RETURN_TIMEOUT_NANOS =
                java.time.Duration.ofSeconds(150).toNanos();

        private final GameTestHelper helper;
        private final ServerRuntime runtime;
        private final long createdAt;
        private final boolean requireEntry;
        private final boolean requireVictory;

        private EndPortalStage stage = EndPortalStage.BODY;
        private CompletableFuture<CapabilityProbeOutcome> probe;
        private PlacedHuman humanSession;
        private BlockPos portalCenter;
        private EndVictoryArena victoryArena;
        private UUID bodyId;
        private long enteredEndAt;
        private long goalRevisionBefore;
        private long activationGoalRevision;
        private long stageStartedNanos;
        private boolean activationSkillObserved;
        private boolean entrySkillObserved;
        private boolean fightSkillObserved;
        private boolean returnSkillObserved;
        private boolean priorRouteCheckpointInstalled;

        private LiveEndPortalActivationScenario(
                final GameTestHelper helper,
                final ServerRuntime runtime,
                final boolean requireEntry,
                final boolean requireVictory
        ) {
            this.helper = helper;
            this.runtime = runtime;
            this.requireEntry = requireEntry;
            this.requireVictory = requireVictory;
            if (requireVictory && !requireEntry) {
                throw new IllegalArgumentException(
                        "Victory chain requires portal entry"
                );
            }
            createdAt = helper.getTick();
            stageStartedNanos = System.nanoTime();
        }

        private void start() {
            finishScenarioGoal(runtime);
            final var status = AiPlayerManager.status(runtime.server());
            if (status.state() == SessionState.ABSENT) {
                helper.assertTrue(
                        AiPlayerManager.requestSpawn(
                                runtime.server()
                        ).accepted(),
                        "End-portal companion spawn was rejected"
                );
            }
        }

        private void tick() {
            if (humanSession != null) {
                humanSession.tick();
            }
            switch (stage) {
                case BODY -> waitForBody();
                case PROBE -> waitForProbe();
                case VISIBLE -> waitForVisibleRing();
                case GOAL -> waitForGoal();
                case SKILL -> waitForActivationSkill();
                case ACTIVATE -> waitForActivation();
                case ENTRY_SKILL -> waitForEntrySkill();
                case ENTER -> waitForEntry();
                case VICTORY_VISIBLE -> waitForVictoryVisible();
                case FIGHT_SKILL -> waitForChainedFightSkill();
                case FIGHT -> waitForChainedDragonKill();
                case RETURN -> waitForChainedReturn();
                case DONE -> {
                    // GameTest is already terminal.
                }
            }
        }

        private void waitForBody() {
            final var status = AiPlayerManager.status(runtime.server());
            helper.assertTrue(
                    status.state() != SessionState.FAILED,
                    "End-portal companion body failed"
            );
            if (status.state() != SessionState.ACTIVE
                    || !status.online()) {
                helper.assertTrue(
                        helper.getTick() - createdAt
                            <= BODY_TIMEOUT_TICKS,
                        "End-portal companion body timed out"
                );
                return;
            }
            final ServerPlayer body = AiPlayerManager
                    .onlinePlayer(runtime.server())
                    .orElseThrow();
            bodyId = body.getUUID();
            preparePortalFixture(body);
            probe = probeOrReuseVerifiedModel(runtime);
            stage = EndPortalStage.PROBE;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForProbe() {
            assertWithinModelDeadline(
                    "End-portal model capability probe timed out"
            );
            if (!probe.isDone()) {
                return;
            }
            final CapabilityProbeOutcome outcome = probe.join();
            helper.assertTrue(
                    outcome
                        instanceof CapabilityProbeOutcome.Supported,
                    "Configured live model probe failed: " + outcome
            );
            stage = EndPortalStage.VISIBLE;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForVisibleRing() {
            assertWithinModelDeadline(
                    "End portal ring never became fair first-person evidence"
            );
            final var frame = runtime.coreFrames().current();
            if (frame.isEmpty()
                    || ObservedEndPortalGeometry.uniqueCenter(
                            frame.orElseThrow().visibleBlockFaces()
                    ).filter(center ->
                            center.x() == portalCenter.getX()
                                && center.y()
                                    == portalCenter.getY()
                                && center.z()
                                    == portalCenter.getZ()
                    ).isEmpty()) {
                return;
            }
            final ServerPlayer body = AiPlayerManager
                    .onlinePlayer(runtime.server())
                    .orElseThrow();
            humanSession = PlacedHuman.create(
                    helper,
                    runtime,
                    body.position().add(-2.0D, 0.0D, -2.0D)
            );
            final ServerPlayer human = humanSession.player();
            helper.assertTrue(
                    CompanionCommandAccess.mayAdmin(
                            human.createCommandSourceStack()
                    ),
                    "Logged-in End-portal test player lacked task permission"
            );
            goalRevisionBefore = runtime.goals().snapshot().revision();
            final String request = requireVictory
                    ? runtime.worldData().displayName()
                        + "，请从眼前的末地传送门继续通关Minecraft："
                        + "放入末影之眼激活并进入末地，"
                        + "击败末影龙，然后进入中央返回传送门"
                        + "回到主世界。"
                    : requireEntry
                    ? runtime.worldData().displayName()
                        + "，请激活你眼前的末地传送门，"
                        + "把背包里的末影之眼放进框架，"
                        + "然后进入传送门前往末地。"
                    : runtime.worldData().displayName()
                        + "，请激活你眼前的末地传送门，"
                        + "把背包里的末影之眼放进框架。";
            final Component submitted =
                    ForgeHooks.onServerChatSubmittedEvent(
                            human,
                            Component.literal(request)
                    );
            helper.assertTrue(
                    submitted != null,
                    "Companion cancelled the End-portal chat command"
            );
            if (requireVictory) {
                final GoalSnapshot installed =
                        runtime.goals().snapshot();
                if (installed.revision() > goalRevisionBefore
                        && installed.status() == GoalStatus.RUNNING) {
                    installPriorRouteCheckpoint(installed);
                }
            }
            humanSession.close();
            humanSession = null;
            stage = EndPortalStage.GOAL;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForGoal() {
            assertNoHumanPlayers();
            assertWithinModelDeadline(
                    "Live model did not classify the End-portal task"
            );
            final GoalSnapshot goal = runtime.goals().snapshot();
            if (goal.revision() == goalRevisionBefore) {
                return;
            }
            helper.assertTrue(
                    goal.revision() > goalRevisionBefore
                        && goal.status() == GoalStatus.RUNNING
                        && goal.goal().contains("末地传送门"),
                    "End-portal chat did not become a running goal: "
                        + goal
            );
            if (requireVictory
                    && !priorRouteCheckpointInstalled) {
                installPriorRouteCheckpoint(goal);
            }
            activationGoalRevision = goal.revision();
            stage = EndPortalStage.SKILL;
            stageStartedNanos = System.nanoTime();
        }

        private void installPriorRouteCheckpoint(
                final GoalSnapshot goal
        ) {
            runtime.worldData().markVerifiedRouteMilestones(
                    goal.revision(),
                    java.util.EnumSet.of(
                            SurvivalMilestone.BODY_ACTIVE,
                            SurvivalMilestone.WOOD_OBTAINED,
                            SurvivalMilestone.BASIC_CRAFTING_READY,
                            SurvivalMilestone.STONE_TOOL_OBTAINED,
                            SurvivalMilestone.FOOD_SECURED,
                            SurvivalMilestone.IRON_TOOLKIT_OBTAINED,
                            SurvivalMilestone.NETHER_ENTERED,
                            SurvivalMilestone.BLAZE_MATERIAL_OBTAINED,
                            SurvivalMilestone.ENDER_PEARL_OBTAINED,
                            SurvivalMilestone.EYE_OF_ENDER_CRAFTED,
                            SurvivalMilestone
                                .STRONGHOLD_BEARING_MEASURED,
                            SurvivalMilestone
                                .STRONGHOLD_SEARCH_AREA_TRIANGULATED
                    )
            );
            priorRouteCheckpointInstalled = true;
        }

        private void waitForActivationSkill() {
            assertNoHumanPlayers();
            assertWithinModelDeadline(
                    "Live model did not select the parameterless "
                        + "activate_observed_end_portal skill; "
                        + diagnostics()
            );
            final var snapshot =
                    runtime.skillSupervisor().snapshot();
            if (!"activate_observed_end_portal".equals(
                    snapshot.skillName()
            )) {
                return;
            }
            helper.assertTrue(
                    snapshot.boundGoalRevision()
                        == activationGoalRevision,
                    "End-portal skill bound the wrong goal revision"
            );
            activationSkillObserved = true;
            stage = EndPortalStage.ACTIVATE;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForActivation() {
            assertNoHumanPlayers();
            if (activePortalBlocks() == 9) {
                final ServerPlayer body = AiPlayerManager
                        .onlinePlayer(runtime.server())
                        .orElseThrow();
                helper.assertTrue(
                        activationSkillObserved
                            && body.getInventory().countItem(
                                Items.ENDER_EYE
                            ) == 0,
                        "Portal activated without the observed live-model "
                            + "skill or exact Eye consumption: "
                            + diagnostics()
                );
                if (requireEntry) {
                    stage = EndPortalStage.ENTRY_SKILL;
                    stageStartedNanos = System.nanoTime();
                } else {
                    stage = EndPortalStage.DONE;
                    helper.succeed();
                }
                return;
            }
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= ACTIVATION_TIMEOUT_NANOS,
                    "Live-model End portal activation timed out: "
                        + diagnostics()
            );
        }

        private void waitForEntrySkill() {
            assertNoHumanPlayers();
            final ServerPlayer body = AiPlayerManager
                    .onlinePlayer(runtime.server())
                    .orElseThrow();
            helper.assertTrue(
                    bodyId.equals(body.getUUID()),
                    "End portal handoff replaced the companion body"
            );
            helper.assertTrue(
                    body.level().dimension().equals(Level.OVERWORLD),
                    "Companion entered the End before the entry skill "
                        + "was observed: " + diagnostics()
            );
            final var snapshot =
                    runtime.skillSupervisor().snapshot();
            if (!"find_and_enter_observed_portal".equals(
                    snapshot.skillName()
            )) {
                helper.assertTrue(
                        System.nanoTime() - stageStartedNanos
                            <= MODEL_TIMEOUT_NANOS,
                        "Live model did not select the parameterless "
                            + "portal finder after activation: "
                            + diagnostics()
                );
                return;
            }
            helper.assertTrue(
                    snapshot.boundGoalRevision()
                        == activationGoalRevision,
                    "Portal entry skill bound the wrong goal revision"
            );
            entrySkillObserved = true;
            stage = EndPortalStage.ENTER;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForEntry() {
            assertNoHumanPlayers();
            final ServerPlayer body = AiPlayerManager
                    .onlinePlayer(runtime.server())
                    .orElseThrow();
            helper.assertTrue(
                    bodyId.equals(body.getUUID()),
                    "End entry did not preserve the companion UUID"
            );
            if (body.level().dimension().equals(Level.END)) {
                helper.assertTrue(
                        entrySkillObserved
                            && activePortalBlocks() == 9
                            && body.getInventory().countItem(
                                Items.ENDER_EYE
                            ) == 0,
                        "End entry lacked activation/entry causal evidence: "
                            + diagnostics()
                );
                if (requireVictory) {
                    enteredEndAt = helper.getTick();
                    victoryArena = prepareEndVictoryArena(
                            helper,
                            runtime,
                            body,
                            false,
                            false
                    );
                    stage = EndPortalStage.VICTORY_VISIBLE;
                    stageStartedNanos = System.nanoTime();
                } else {
                    stage = EndPortalStage.DONE;
                    helper.succeed();
                }
                return;
            }
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= ENTRY_TIMEOUT_NANOS,
                    "Live-model End portal entry timed out: "
                        + diagnostics()
            );
        }

        private void waitForVictoryVisible() {
            assertNoHumanPlayers();
            final ServerPlayer body = AiPlayerManager
                    .onlinePlayer(runtime.server())
                    .orElseThrow();
            helper.assertTrue(
                    body.level().dimension().equals(Level.END),
                    "Late completion body left the End before combat"
            );
            final var frame = runtime.coreFrames().current();
            if (frame.isEmpty()
                    || !frame.orElseThrow().dimension().equals(
                            dev.mcai.companion.waypoint
                                .DimensionRef.END
                    )
                    || !frame.orElseThrow().onGround()
                    || frame.orElseThrow().visibleEntities()
                            .stream()
                            .noneMatch(entity ->
                                    entity.entityTypeId().equals(
                                            "minecraft:end_crystal"
                                    )
                            )) {
                helper.assertTrue(
                        System.nanoTime() - stageStartedNanos
                            <= MODEL_TIMEOUT_NANOS,
                        "Late completion dragon arena never became fair "
                            + "first-person evidence: " + diagnostics()
                );
                return;
            }
            stage = EndPortalStage.FIGHT_SKILL;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForChainedFightSkill() {
            assertNoHumanPlayers();
            final var snapshot =
                    runtime.skillSupervisor().snapshot();
            if (!"fight_ender_dragon".equals(
                    snapshot.skillName()
            )) {
                helper.assertTrue(
                        System.nanoTime() - stageStartedNanos
                            <= MODEL_TIMEOUT_NANOS,
                        "Late completion model did not select "
                            + "fight_ender_dragon: " + diagnostics()
                );
                return;
            }
            helper.assertTrue(
                    snapshot.boundGoalRevision()
                        == activationGoalRevision,
                    "Chained dragon skill bound the wrong goal revision"
            );
            fightSkillObserved = true;
            stage = EndPortalStage.FIGHT;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForChainedDragonKill() {
            assertNoHumanPlayers();
            final var milestones = runtime.worldData()
                    .verifiedRouteProgress(
                            activationGoalRevision
                    )
                    .milestones();
            if (milestones.contains(
                    SurvivalMilestone.DRAGON_KILLED
            )) {
                final ServerPlayer body = AiPlayerManager
                        .onlinePlayer(runtime.server())
                        .orElseThrow();
                helper.assertTrue(
                        fightSkillObserved
                            && victoryArena != null
                            && (!victoryArena.crystal().isAlive()
                                || victoryArena.crystal().isRemoved())
                            && body.level().getBlockState(
                                victoryArena.cageBar()
                            ).isAir()
                            && body.getInventory()
                                .countItem(Items.ARROW) < 64,
                        "Chained dragon milestone lacked physical combat "
                            + "evidence: " + diagnostics()
                );
                if (!victoryArena.dragon().isRemoved()) {
                    victoryArena.dragon().setNoAi(false);
                }
                activateEndReturnPortal(
                        runtime.server().getLevel(Level.END),
                        victoryArena.returnPortalCenter()
                );
                stage = EndPortalStage.RETURN;
                stageStartedNanos = System.nanoTime();
                return;
            }
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= CHAIN_FIGHT_TIMEOUT_NANOS,
                    "Late completion dragon fight timed out: "
                        + diagnostics()
            );
        }

        private void waitForChainedReturn() {
            assertNoHumanPlayers();
            final var snapshot =
                    runtime.skillSupervisor().snapshot();
            if ("find_and_enter_observed_portal".equals(
                    snapshot.skillName()
            ) && snapshot.boundGoalRevision()
                    == activationGoalRevision) {
                returnSkillObserved = true;
            }
            final ServerPlayer body = AiPlayerManager
                    .onlinePlayer(runtime.server())
                    .orElseThrow();
            final var milestones = runtime.worldData()
                    .verifiedRouteProgress(
                            activationGoalRevision
                    )
                    .milestones();
            if (body.level().dimension().equals(Level.OVERWORLD)
                    && milestones.contains(
                        SurvivalMilestone.RETURNED_FROM_END
                    )) {
                helper.assertTrue(
                        priorRouteCheckpointInstalled
                            && activationSkillObserved
                            && entrySkillObserved
                            && fightSkillObserved
                            && returnSkillObserved
                            && bodyId.equals(body.getUUID()),
                        "Late completion chain lost one causal handoff: "
                            + diagnostics()
                );
                stage = EndPortalStage.DONE;
                helper.succeed();
                return;
            }
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= CHAIN_RETURN_TIMEOUT_NANOS,
                    "Late completion End return timed out: "
                        + diagnostics()
            );
        }

        private void preparePortalFixture(final ServerPlayer body) {
            final BlockPos feet = body.blockPosition();
            portalCenter = feet.offset(0, 0, 3);
            body.setGameMode(GameType.SURVIVAL);
            helper.assertTrue(
                    body.gameMode.getGameModeForPlayer()
                            == GameType.SURVIVAL
                        && !body.getAbilities().instabuild,
                    "Live End-portal fixture did not enter survival mode"
            );
            for (int x = -6; x <= 6; x++) {
                for (int z = -5; z <= 9; z++) {
                    final BlockPos floor =
                            feet.offset(x, -1, z);
                    helper.getLevel().setBlockAndUpdate(
                            floor,
                            Blocks.STONE_BRICKS.defaultBlockState()
                    );
                    for (int y = 1; y <= 4; y++) {
                        helper.getLevel().setBlockAndUpdate(
                                floor.above(y),
                                Blocks.AIR.defaultBlockState()
                        );
                    }
                }
            }
            for (int offset = -1; offset <= 1; offset++) {
                setFrame(
                        portalCenter.offset(offset, 0, -2),
                        Direction.SOUTH
                );
                setFrame(
                        portalCenter.offset(offset, 0, 2),
                        Direction.NORTH
                );
                setFrame(
                        portalCenter.offset(-2, 0, offset),
                        Direction.EAST
                );
                setFrame(
                        portalCenter.offset(2, 0, offset),
                        Direction.WEST
                );
            }
            if (requireVictory) {
                equipEndVictoryBody(body, true);
            } else {
                body.getInventory().clearContent();
                body.getInventory().setItem(
                        0,
                        new ItemStack(Items.ENDER_EYE, 12)
                );
                /*
                 * This handoff represents a late completion-route body, not
                 * a naked portal laboratory. Natural slime-chunk spawns stay
                 * enabled while the provider makes the second decision.
                 */
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
                body.getInventory().setItem(
                        4,
                        new ItemStack(Items.COBBLESTONE, 64)
                );
                body.getInventory().setSelectedSlot(0);
                body.setItemSlot(
                        EquipmentSlot.OFFHAND,
                        new ItemStack(Items.SHIELD)
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
            }
            body.inventoryMenu.broadcastChanges();
            body.setHealth(body.getMaxHealth());
            body.getFoodData().setFoodLevel(20);
            body.setDeltaMovement(Vec3.ZERO);
            body.lookAt(
                    EntityAnchorArgument.Anchor.EYES,
                    Vec3.atCenterOf(portalCenter)
                        .add(0.0D, -0.15D, 0.0D)
            );
            body.setYHeadRot(body.getYRot());
        }

        private void setFrame(
                final BlockPos position,
                final Direction facing
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
                            false
                        )
            );
        }

        private int activePortalBlocks() {
            int blocks = 0;
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (helper.getLevel().getBlockState(
                            portalCenter.offset(x, 0, z)
                    ).is(Blocks.END_PORTAL)) {
                        blocks++;
                    }
                }
            }
            return blocks;
        }

        private void assertNoHumanPlayers() {
            final long humans = runtime.server()
                    .getPlayerList()
                    .getPlayers()
                    .stream()
                    .filter(player -> !player.getUUID().equals(
                            runtime.worldData().companionUuid()
                    ))
                    .count();
            helper.assertTrue(
                    humans == 0L,
                    "End-portal autonomy retained " + humans
                        + " human player(s) after the command"
            );
        }

        private void assertWithinModelDeadline(final String message) {
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= MODEL_TIMEOUT_NANOS,
                    message
            );
        }

        private String diagnostics() {
            final ServerPlayer body = AiPlayerManager
                    .onlinePlayer(runtime.server())
                    .orElseThrow();
            return "supervisor=" + runtime.skillSupervisor().snapshot()
                    + ", goal=" + runtime.goals().snapshot()
                    + ", dimension="
                    + body.level().dimension().identifier()
                    + ", body=" + body.position()
                    + ", eyes="
                    + body.getInventory().countItem(Items.ENDER_EYE)
                    + ", portalBlocks=" + activePortalBlocks()
                    + ", activationSkillObserved="
                    + activationSkillObserved
                    + ", entrySkillObserved="
                    + entrySkillObserved
                    + ", fightSkillObserved="
                    + fightSkillObserved
                    + ", returnSkillObserved="
                    + returnSkillObserved
                    + ", enteredEndAt=" + enteredEndAt
                    + ", milestones="
                    + runtime.worldData()
                        .verifiedRouteProgress(
                                Math.max(
                                        0L,
                                        activationGoalRevision
                                )
                        ).milestones()
                    + ", dragonHealth="
                    + (victoryArena == null
                        ? "none"
                        : victoryArena.dragon().getHealth());
        }

        private void cleanup() {
            finishScenarioGoal(runtime);
            if (humanSession != null) {
                humanSession.close();
            }
            if (AiPlayerManager.status(runtime.server()).state()
                    != SessionState.ABSENT) {
                AiPlayerManager.requestRemove(runtime.server());
            }
        }
    }

    private enum EndPortalStage {
        BODY,
        PROBE,
        VISIBLE,
        GOAL,
        SKILL,
        ACTIVATE,
        ENTRY_SKILL,
        ENTER,
        VICTORY_VISIBLE,
        FIGHT_SKILL,
        FIGHT,
        RETURN,
        DONE
    }

    /**
     * A real, logged-in ServerPlayer used only by the opt-in live-model
     * GameTest. Forge's convenient mock uses the intentionally invalid name
     * {@code test-mock-player} and is not present in PlayerList, so it cannot
     * exercise the same permission path as a real chat sender.
     */
    private static final class PlacedHuman implements AutoCloseable {
        private final ServerRuntime runtime;
        private final NameAndId identity;
        private final Connection connection;
        private final EmbeddedChannel channel;
        private final ServerGamePacketListenerImpl listener;
        private final ServerPlayer player;
        private long keepAlivePackets;
        private long keepAliveAccepted;
        private boolean closed;

        private PlacedHuman(
                final ServerRuntime runtime,
                final NameAndId identity,
                final Connection connection,
                final EmbeddedChannel channel,
                final ServerGamePacketListenerImpl listener,
                final ServerPlayer player
        ) {
            this.runtime = runtime;
            this.identity = identity;
            this.connection = connection;
            this.channel = channel;
            this.listener = listener;
            this.player = player;
        }

        static PlacedHuman create(
                final GameTestHelper helper,
                final ServerRuntime runtime
        ) {
            return create(helper, runtime, null);
        }

        /**
         * Creates the physical player at the supplied login position before
         * {@link net.minecraft.server.players.PlayerList#placeNewPlayer}.
         * This preserves the production ordering used by the Forge
         * PlayerLoggedInEvent and lets login-triggered systems observe the
         * same position that vanilla publishes to other players.
         */
        static PlacedHuman create(
                final GameTestHelper helper,
                final ServerRuntime runtime,
                final Vec3 loginPosition
        ) {
            final GameProfile profile = new GameProfile(
                    UUID.randomUUID(),
                    "TestHuman"
            );
            final NameAndId identity = new NameAndId(profile);
            final CommonListenerCookie cookie =
                    CommonListenerCookie.createInitial(profile, false);
            final ServerPlayer player = new ServerPlayer(
                    runtime.server(),
                    helper.getLevel(),
                    profile,
                    cookie.clientInformation()
            );
            final Connection connection =
                    new Connection(PacketFlow.SERVERBOUND);
            if (loginPosition != null) {
                player.setPos(
                        loginPosition.x(),
                        loginPosition.y(),
                        loginPosition.z()
                );
            }
            final EmbeddedChannel channel =
                    new EmbeddedChannel(connection);
            runtime.server().getPlayerList().placeNewPlayer(
                    connection,
                    player,
                    cookie
            );
            /*
             * PlayerList.placeNewPlayer is the authoritative vanilla login
             * path and installs a fresh ServerGamePacketListenerImpl. Do not
             * construct a second listener before it: acknowledgements sent
             * to that detached instance cannot clear the installed
             * listener's keepalive challenge and cause a deterministic
             * 15-second timeout.
             */
            final ServerGamePacketListenerImpl listener =
                    player.connection;
            if (listener == null) {
                throw new IllegalStateException(
                        "Vanilla login did not install a game listener"
                );
            }
            player.setGameMode(GameType.SURVIVAL);
            runtime.server().getPlayerList().op(
                    identity,
                    Optional.of(LevelBasedPermissionSet.OWNER),
                    Optional.empty()
            );
            listener.handleAcceptPlayerLoad(
                    new ServerboundPlayerLoadedPacket()
            );
            return new PlacedHuman(
                    runtime,
                    identity,
                    connection,
                    channel,
                    listener,
                    player
            );
        }

        ServerPlayer player() {
            return player;
        }

        void tick() {
            if (closed || !connection.isConnected()) {
                return;
            }
            connection.tick();
            if (!connection.isConnected()) {
                return;
            }
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            Object packet;
            while ((packet = channel.readOutbound()) != null) {
                try {
                    if (packet
                            instanceof ClientboundKeepAlivePacket keepAlive) {
                        keepAlivePackets++;
                        listener.handleKeepAlive(
                                new ServerboundKeepAlivePacket(
                                        keepAlive.getId()
                                )
                        );
                        if (connection.isConnected()) {
                            keepAliveAccepted++;
                        }
                    } else if (packet
                            instanceof ClientboundPlayerPositionPacket position) {
                        listener.handleAcceptTeleportPacket(
                                new ServerboundAcceptTeleportationPacket(
                                        position.id()
                                )
                        );
                    } else if (packet
                            instanceof ClientboundChunkBatchFinishedPacket) {
                        listener.handleChunkBatchReceived(
                                new ServerboundChunkBatchReceivedPacket(
                                        3.5F
                                )
                        );
                    }
                } finally {
                    ReferenceCountUtil.release(packet);
                }
            }
            channel.runPendingTasks();
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            runtime.server().getPlayerList().deop(identity);
            if (connection.isConnected()) {
                connection.disconnect(Component.literal(
                        "Live model GameTest complete"
                ));
            }
            connection.handleDisconnection();
            channel.finishAndReleaseAll();
        }
    }
}
