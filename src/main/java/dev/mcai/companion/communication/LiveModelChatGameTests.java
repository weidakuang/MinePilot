Warning: truncated output (original token count: 175590)
Total output lines: 17076

/Users/weida/.zprofile:7: no such file or directory: /opt/homebrew/bin/brew
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    private static final long HORDE_MODEL_TIMEOUT_NANOS =
            java.time.Duration.ofSeconds(45).toNanos();
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
                        runtime,
                        false
                );
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Exercises a server-owned navigation stop/resume rather than treating a
     * spoken acknowledgement as completion.  A real chat task starts
     * {@code travel_to}, the player interrupts it at a running-skill
     * checkpoint, and a second coordinate task must start a fresh skill and
     * reach its destination through ordinary player movement.
     */
    public static void realPlayerTaskToLiveModelMovementStopResume(
            final GameTestHelper helper
    ) {
        if (!Boolean.getBoolean("mcai.liveModelTest")) {
            helper.succeed();
            return;
        }
        final ServerRuntime runtime = CompanionRuntime.active()
                .filter(candidate -> candidate.server()
                        == helper.getLevel().getServer())
                .orElseThrow(() -> new IllegalStateException(
                        "Companion runtime is unavailable"
                ));
        final LiveMovementScenario scenario = new LiveMovementScenario(
                helper,
                runtime,
                true
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
     * Exercises the teammate stop/resume contract with the configured live
     * model. The player keeps a real chat session open, cancels an active
     * follow at a safe checkpoint, verifies the idle state, and submits a
     * fresh follow request without any teleport or direct world mutation.
     */
    public static void realPlayerTaskToLiveModelFollowStopResume(
            final GameTestHelper helper
    ) {
        if (!Boolean.getBoolean("mcai.liveModelTest")) {
            helper.succeed();
            return;
        }
        final ServerRuntime runtime = CompanionRuntime.active()
                .filter(candidate ->
                        candidate.server() == helper.getLevel().getServer())
                .orElseThrow(() -> new IllegalStateException(
                        "Companion runtime is unavailable"
                ));
        final LiveFollowScenario scenario = new LiveFollowScenario(
                helper, runtime, true, true
        );
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
     * Exercises the professional-companion interruption contract on the
     * same fair, real-model combat path. The player stops the companion after
     * the melee skill is physically active, verifies that the skill reaches a
     * safe idle checkpoint, then sends a fresh ordinary chat request and
     * requires the same ServerPlayer to reacquire and defeat the visible
     * Zombie. No entity, inventory, or position is reset between phases.
     */
    public static void realPlayerTaskToLiveModelZombieDefenseStopResume(
            final GameTestHelper helper
    ) {
        if (!Boolean.getBoolean("mcai.liveModelTest")) {
            helper.succeed();
            return;
        }
        final ServerRuntime runtime = CompanionRuntime.active()
                .filter(candidate -> candidate.server()
                        == helper.getLevel().getServer())
                .orElseThrow(() -> new IllegalStateException(
                        "Companion runtime is unavailable"
                ));
        final LiveCombatScenario scenario = new LiveCombatScenario(
                helper,
                runtime,
                true
        );
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Sends one ordinary team request through the live model while several
     * visible hostile entities are present.  The fixture is deliberately
     * bounded (six mobs, not a survival-statistic claim): it verifies that a
     * model-selected combat skill can coexist with the local 20 TPS survival
     * lane instead of leaving the body stationary after the first target.
     */
    public static void realPlayerTaskToLiveModelHordeDefense(
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
        final LiveHordeCombatScenario scenario =
                new LiveHordeCombatScenario(helper, runtime);
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Extends the same fair live-model combat path to the requested ten
     * Zombies plus ten Skeletons.  The assertion is intentionally bounded to
     * target damage, movement and survival; it is not a claim that one
     * unenchanted body clears every twenty-mob encounter in a natural world.
     */
    public static void realPlayerTaskToLiveModelTenPlusTenHorde(
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
        final LiveHordeCombatScenario scenario =
                new LiveHordeCombatScenario(
                        helper,
                        runtime,
                        20,
                        10,
                        7.0D,
                        "请马上保护我，击退面前的十个僵尸和十个骷髅，"
                                + "不要只回复，要移动、格挡并攻击。"
                );
        helper.addCleanup(ignored -> scenario.cleanup());
        scenario.start();
        helper.onEachTick(scenario::tick);
    }

    /**
     * Sends an ordinary Chinese duel request through the live model while a
     * real vanilla iron golem is visible. The golem is held still only until
     * the model-selected combat skill starts; it then receives normal AI and
     * targets the companion. The bounded assertion requires movement, damage
     * to the golem, and survival, rather than a synthetic instant kill.
     */
    public static void realPlayerTaskToLiveModelIronGolemDuel(
            final GameTestHelper helper
    ) {
        if (!Boolean.getBoolean("mcai.liveModelTest")) {
            helper.succeed();
            return;
        }
        final ServerRuntime runtime = CompanionRuntime.active()
                .filter(candidate -> candidate.server()
                        == helper.getLevel().getServer())
                .orElseThrow(() -> new IllegalStateException(
                        "Companion runtime is unavailable"
                ));
        final LiveIronGolemDuelScenario scenario =
                new LiveIronGolemDuelScenario(helper, runtime);
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
        private final boolean exerciseStopResume;

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
        private double resumeTargetX;
        private double resumeTargetZ;
        private Vec3 positionAtStop;
        private Vec3 resumeStart;
        private int stopStableTicks;
        private boolean stopSubmitted;
        private boolean resumeSkillStarted;

        private LiveMovementScenario(
                final GameTestHelper helper,
                final ServerRuntime runtime
        ) {
            this(helper, runtime, false);
        }

        private LiveMovementScenario(
                final GameTestHelper helper,
                final ServerRuntime runtime,
                final boolean exerciseStopResume
        ) {
            this.helper = helper;
            this.runtime = runtime;
            this.exerciseStopResume = exerciseStopResume;
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
                case STOP -> waitForStop();
                case RESUME_GOAL -> waitForResumeGoal();
                case RESUME_MOVEMENT -> waitForResumeMovement();
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
                            humanSession.player(),
                            Component.literal(command)
                    );
            helper.assertTrue(
                    submitted != null,
                    "Companion cancelled the movement chat command"
            );
            if (!exerciseStopResume) {
                humanSession.close();
                humanSession = null;
            }
            stage = MovementStage.GOAL;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForGoal() {
            if (!exerciseStopResume) {
                assertNoHumanPlayersDuringAutonomy();
            }
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
            if (!exerciseStopResume) {
                assertNoHumanPlayersDuringAutonomy();
            }
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
            if (exerciseStopResume && !stopSubmitted) {
                final var skill = runtime.skillSupervisor().snapshot();
                if (skill.state()
                        == dev.mcai.companion.skill.SkillSupervisor.State.RUNNING
                        && skill.skillName().equals("travel_to")) {
                    final Component stop =
                            ForgeHooks.onServerChatSubmittedEvent(
                                    human,
                                    Component.literal("停下")
                            );
                    helper.assertTrue(
                            stop != null,
                            "Companion cancelled the navigation stop request"
                    );
                    stopSubmitted = true;
                    stage = MovementStage.STOP;
                    stageStartedNanos = System.nanoTime();
                    return;
                }
            }
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

        private void waitForStop() {
            assertWithinModelDeadline(
                    "Companion did not reach a safe navigation stop"
            );
            helper.assertTrue(
                    stopSubmitted,
                    "Navigation stop stage started without a request"
            );
            final GoalSnapshot goal = runtime.goals().snapshot();
            final var skill = runtime.skillSupervisor().snapshot();
            if (goal.status() == GoalStatus.RUNNING
                    || goal.status() == GoalStatus.CANCEL_PENDING
                    || skill.state()
                        == dev.mcai.companion.skill.SkillSupervisor.State.RUNNING) {
                return;
            }
            helper.assertTrue(
                    goal.status() == GoalStatus.SAFE_IDLE
                            && goal.detailCode().equals("goal_cancelled"),
                    "Navigation stop ended in an untruthful state: " + goal
            );
            final ServerPlayer body = AiPlayerManager
                    .onlinePlayer(runtime.server())
                    .orElseThrow();
            if (positionAtStop == null) {
                positionAtStop = body.position();
                return;
            }
            helper.assertTrue(
                    body.position().distanceTo(positionAtStop) <= 0.25D,
                    "Body moved after navigation stop: before="
                            + positionAtStop + ", after=" + body.position()
            );
            stopStableTicks++;
            if (stopStableTicks < 2) {
                return;
            }
            resumeTargetX = startX + 15.5D;
            resumeTargetZ = startZ + 0.5D;
            human.setPos(
                    body.getX() + 2.0D,
                    body.getY(),
                    body.getZ()
            );
            human.setDeltaMovement(Vec3.ZERO);
            goalRevisionBefore = goal.revision();
            final Component resume = ForgeHooks.onServerChatSubmittedEvent(
                    human,
                    Component.literal("继续走到坐标 %.1f %.1f %.1f，正常步行，不要传送。"
                            .formatted(resumeTargetX, targetY, resumeTargetZ))
            );
            helper.assertTrue(
                    resume != null,
                    "Companion cancelled the navigation resume request"
            );
            resumeStart = body.position();
            stage = MovementStage.RESUME_GOAL;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForResumeGoal() {
            assertWithinModelDeadline(
                    "Live model did not accept navigation resume"
            );
            final GoalSnapshot goal = runtime.goals().snapshot();
            if (goal.revision() == goalRevisionBefore) {
                return;
            }
            helper.assertTrue(
                    goal.revision() > goalRevisionBefore
                            && goal.status() == GoalStatus.RUNNING
                            && goal.goal().contains("继续走到坐标"),
                    "Navigation resume did not become a running goal: " + goal
            );
            stage = MovementStage.RESUME_MOVEMENT;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForResumeMovement() {
            assertWithinModelDeadline(
                    "Live model did not start navigation after stop"
            );
            final Optional<ServerPlayer> bodyCandidate =
                    AiPlayerManager.onlinePlayer(runtime.server());
            if (bodyCandidate.isEmpty()) {
                return;
            }
            final ServerPlayer body = bodyCandidate.orElseThrow();
            final var skill = runtime.skillSupervisor().snapshot();
            if (skill.state()
                    == dev.mcai.companion.skill.SkillSupervisor.State.RUNNING
                    && skill.skillName().equals("travel_to")) {
                resumeSkillStarted = true;
            }
            if (!resumeSkillStarted) {
                return;
            }
            final double remaining = Math.sqrt(
                    Math.pow(body.getX() - resumeTargetX, 2.0D)
                            + Math.pow(body.getY() - targetY, 2.0D)
                            + Math.pow(body.getZ() - resumeTargetZ, 2.0D)
            );
            if (remaining <= ARRIVAL_RADIUS) {
                final double moved = body.position().distanceTo(resumeStart);
                helper.assertTrue(
                        moved >= MINIMUM_REAL_MOVEMENT,
                        "Resumed navigation reached target without material movement: "
                                + moved
                );
                stage = MovementStage.DONE;
                helper.succeed();
                return;
            }
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
            for (int dx = -2; dx <= 20; dx++) {
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
        STOP,
        RESUME_GOAL,
        RESUME_MOVEMENT,
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
        private final boolean exerciseStopResume;

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
        private boolean stopSubmitted;
        private Vec3 positionAtStop;
        private int stopStableTicks;
        private HoldingModelGateway holdingGateway;
        private boolean followCourseRepositioned;

        private LiveFollowScenario(
                final GameTestHelper helper,
                final ServerRuntime runtime,
                final boolean requireModelProbe
        ) {
            this(helper, runtime, requireModelProbe, false);
        }

        private LiveFollowScenario(
                final GameTestHelper helper,
                final ServerRuntime runtime,
                final boolean requireModelProbe,
                final boolean exerciseStopResume
        ) {
            this.helper = helper;
            this.runtime = runtime;
            this.requireModelProbe = requireModelProbe;
            this.exerciseStopResume = exerciseStopResume;
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
                            + "model gateway at stage " + stage
                );
            }
            if (humanSession != null) {
                humanSession.tick();
            }
            switch (stage) {
                case BODY -> waitForBody();
                case PROBE -> waitForProbe();
                case VISIBLE -> waitForVisibleHuman();
                case GOAL -> waitForGoal();
                case SKILL -> waitForFollowSkill();
                case STOP -> waitForStop();
                case RESUME_GOAL -> waitForResumeGoal();
                case RESUME_SKILL -> waitForResumeSkill();
                case FOLLOW -> waitForPhysicalFollow();
                case DONE -> {
                    // GameTest is already terminal.
                }
            }
        }

        private void waitForBody() {
            final var status = AiPlayerManager.status(runtime.server());
            helper.assertTrue(
                    status.state() != SessionState.FAILED,
                    "Live-follow companion body failed"
            );
            if (status.state() != SessionState.ACTIVE
                    || !status.online()) {
                helper.assertTrue(
                        helper.getTick() - createdAt
                            <= BODY_TIMEOUT_TICKS,
                        "Live-follow companion body timed out"
                );
                return;
            }
            final ServerPlayer body = AiPlayerManager
                    .onlinePlayer(runtime.server())
                    .orElseThrow();
            prepareSafeFollowCourse(body);
            if (!requireModelProbe) {
                /*
                 * Ordinary chat is intentionally rejected when no verified
                 * provider is installed.  This release-excluded physical
                 * fixture supplies only the readiness precondition, then
                 * proves the immediate path never invokes the provider.
                 */
                holdingGateway = new HoldingModelGateway();
                runtime.model().gateway().install(holdingGateway);
                placeHumanInFairView(body);
                return;
            }
            probe = probeOrReuseVerifiedModel(runtime);
            stage = FollowStage.PROBE;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForProbe() {
            assertWithinModelDeadline(
                    "Live follow model capability probe timed out"
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
            final ServerPlayer body = AiPlayerManager
                    .onlinePlayer(runtime.server())
                    .orElseThrow();
            placeHumanInFairView(body);
        }

        private void placeHumanInFairView(final ServerPlayer body) {
            humanSession = PlacedHuman.create(
                    helper,
                    runtime,
                    body.position().add(INITIAL_LEAD, 0.0, 0.0)
            );
            human = humanSession.player();
            body.lookAt(
                    EntityAnchorArgument.Anchor.EYES,
                    human.getEyePosition()
            );
            body.setYHeadRot(body.getYRot());
            stage = FollowStage.VISIBLE;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForVisibleHuman() {
            assertWithinModelDeadline(
                    "Human never entered the companion's fair semantic view"
            );
            /*
             * A first human login can legitimately trigger the one-time
             * initial-anchor remove/relogin while the body is still
             * unanchored.  PlayerLoggedInEvent is synchronous, but the
             * replacement ServerPlayer completes on subsequent server
             * ticks.  Do not turn that bounded lifecycle gap into the
             * misleading "No value present" failure or submit chat to a
             * body that is between vanilla login sessions.
             */
            final Optional<ServerPlayer> bodyCandidate =
                    AiPlayerManager.onlinePlayer(runtime.server());
            if (bodyCandidate.isEmpty()) {
                return;
            }
            final ServerPlayer body = bodyCandidate.orElseThrow();
            if (!followCourseRepositioned) {
                /*
                 * The production first-login anchor intentionally places the
                 * body at a safe 1--2 block offset from the human.  The
                 * follow course needs a visible lead before it can measure a
                 * walk, otherwise the correct "already close" controller
                 * state would deadlock this fixture's second-leg trigger.
                 */
                human.setPos(
                        body.getX() + INITIAL_LEAD,
                        body.getY(),
                        body.getZ()
                );
                human.setDeltaMovement(Vec3.ZERO);
                followCourseRepositioned = true;
            }
            body.lookAt(
                    EntityAnchorArgument.Anchor.EYES,
                    human.getEyePosition()
            );
            body.setYHeadRot(body.getYRot());
            if (!latestObservationContainsVisiblePlayer()) {
                return;
            }
            helper.assertTrue(
                    CompanionCommandAccess.mayAdmin(
                            human.createCommandSourceStack()
                    ),
                    "Logged-in follow test player lacked task permission"
            );
            final var priorGoal = runtime.goals().setGoal(
                    "帮我砍树",
                    GoalSource.PLAYER_CHAT
            );
            helper.assertTrue(
                    priorGoal.accepted()
                            && priorGoal.snapshot().status()
                                == GoalStatus.RUNNING,
                    "Could not install the pre-existing wood goal"
            );
            goalRevisionBefore = priorGoal.snapshot().revision();
            final Component submitted =
                    ForgeHooks.onServerChatSubmittedEvent(
                            human,
                            Component.literal(
                                    "跟我来，保持两三格距离，正常走，不要传送。"
                            )
                    );
            helper.assertTrue(
                    submitted != null,
                    "Companion cancelled the ordinary follow chat"
            );
            bodyStart = body.position();
            previousBodyPosition = bodyStart;
            stage = FollowStage.GOAL;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForGoal() {
            assertWithinModelDeadline(
                    requireModelProbe
                            ? "Live model did not classify the follow task"
                            : "Immediate player follow chat did not install "
                                + "its bound goal"
            );
            final GoalSnapshot goal = runtime.goals().snapshot();
            if (goal.revision() == goalRevisionBefore) {
                return;
            }
            helper.assertTrue(
                    goal.revision() > goalRevisionBefore
                        && goal.status() == GoalStatus.RUNNING
                        && goal.goal().contains("跟我来"),
                    "Ordinary follow chat did not become a running goal: "
                        + goal
            );
            followGoalRevision = goal.revision();
            stage = FollowStage.SKILL;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForFollowSkill() {
            assertWithinModelDeadline(
                    (requireModelProbe
                            ? "Live model did not start physical "
                                + "follow_entity; "
                            : "Immediate bound follow did not start physical "
                                + "follow_entity; ")
                        + followDiagnostics()
            );
            final GoalSnapshot goal = runtime.goals().snapshot();
            helper.assertTrue(
                    goal.revision() == followGoalRevision
                        && goal.status() == GoalStatus.RUNNING,
                    "Follow goal terminated before movement: " + goal
            );
            final var skill = runtime.skillSupervisor().snapshot();
            if (!skill.skillName().equals("follow_entity")
                    || skill.state()
                        != dev.mcai.companion.skill.SkillSupervisor
                            .State.RUNNING) {
                return;
            }
            helper.assertTrue(
                    skill.boundGoalRevision() == followGoalRevision,
                    "follow_entity bound the wrong goal revision"
            );
            if (exerciseStopResume) {
                final Component stop = ForgeHooks.onServerChatSubmittedEvent(
                        human,
                        Component.literal("停下")
                );
                helper.assertTrue(
                        stop != null,
                        "Companion cancelled the local stop request"
                );
                stopSubmitted = true;
                stage = FollowStage.STOP;
                stageStartedNanos = System.nanoTime();
                return;
            }
            if (!continuationNudgeSent) {
                final Component nudge =
                        ForgeHooks.onServerChatSubmittedEvent(
                                human,
                                Component.literal("走啊")
                        );
                helper.assertTrue(
                        nudge != null,
                        "Companion cancelled the ordinary follow nudge"
                );
                helper.assertTrue(
                        runtime.goals().snapshot().revision()
                                == followGoalRevision
                            && runtime.goals().snapshot().goal()
                                .contains("跟我来"),
                        "A short follow nudge replaced the bound follow goal: "
                                + runtime.goals().snapshot()
                );
                final var afterNudge =
                        runtime.skillSupervisor().snapshot();
                helper.assertTrue(
                        afterNudge.state()
                            == dev.mcai.companion.skill.SkillSupervisor
                                .State.RUNNING
                            && afterNudge.skillName().equals(
                                    "follow_entity"
                            )
                            && afterNudge.boundGoalRevision()
                                == followGoalRevision,
                        "A short follow nudge cancelled physical follow: "
                                + afterNudge
                );
                continuationNudgeSent = true;
            }
            previousBodyPosition = AiPlayerManager
                    .onlinePlayer(runtime.server())
                    .orElseThrow()
                    .position();
            stage = FollowStage.FOLLOW;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForStop() {
            assertWithinModelDeadline(
                    "Companion did not reach a safe stop checkpoint"
            );
            helper.assertTrue(
                    stopSubmitted,
                    "Stop stage started without a submitted player request"
            );
            final GoalSnapshot goal = runtime.goals().snapshot();
            final var skill = runtime.skillSupervisor().snapshot();
            if (goal.status() == GoalStatus.RUNNING
                    || goal.status() == GoalStatus.CANCEL_PENDING
                    || skill.state()
                        == dev.mcai.companion.skill.SkillSupervisor.State.RUNNING) {
                return;
            }
            helper.assertTrue(
                    goal.status() == GoalStatus.SAFE_IDLE
                            && goal.detailCode().equals("goal_cancelled"),
                    "Stop request ended in an untruthful state: " + goal
            );
            final ServerPlayer body = AiPlayerManager
                    .onlinePlayer(runtime.server())
                    .orElseThrow();
            if (positionAtStop == null) {
                positionAtStop = body.position();
                return;
            }
            helper.assertTrue(
                    body.position().distanceTo(positionAtStop) <= 0.25D,
                    "Body moved after the stop checkpoint: before="
                            + positionAtStop + ", after=" + body.position()
            );
            stopStableTicks++;
            if (stopStableTicks < 2) {
                return;
            }
            body.setDeltaMovement(Vec3.ZERO);
            human.setPos(
                    body.getX() + INITIAL_LEAD,
                    body.getY(),
                    body.getZ()
            );
            human.setDeltaMovement(Vec3.ZERO);
            body.lookAt(
                    EntityAnchorArgument.Anchor.EYES,
                    human.getEyePosition()
            );
            body.setYHeadRot(body.getYRot());
            goalRevisionBefore = goal.revision();
            final Component resume = ForgeHooks.onServerChatSubmittedEvent(
                    human,
                    Component.literal(
                            "跟我来，继续保持两三格距离，正常走，不要传送。"
                    )
            );
            helper.assertTrue(
                    resume != null,
                    "Companion cancelled the follow-up request after stop"
            );
            bodyStart = body.position();
            previousBodyPosition = bodyStart;
            bodyPath = 0.0D;
            secondLegStarted = false;
            continuationNudgeSent = true;
            stage = FollowStage.RESUME_GOAL;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForResumeGoal() {
            assertWithinModelDeadline(
                    "Live model did not accept the follow-up after stop"
            );
            final GoalSnapshot goal = runtime.goals().snapshot();
            if (goal.revision() == goalRevisionBefore) {
                return;
            }
            helper.assertTrue(
                    goal.revision() > goalRevisionBefore
                            && goal.status() == GoalStatus.RUNNING
                            && goal.goal().contains("跟我来"),
                    "Follow-up chat did not become a running goal: " + goal
            );
            followGoalRevision = goal.revision();
            stage = FollowStage.RESUME_SKILL;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForResumeSkill() {
            assertWithinModelDeadline(
                    "Live model did not resume follow_entity after stop"
            );
            final var skill = runtime.skillSupervisor().snapshot();
            if (!skill.skillName().equals("follow_entity")
                    || skill.state()
                        != dev.mcai.companion.skill.SkillSupervisor.State.RUNNING) {
                return;
            }
            helper.assertTrue(
                    skill.boundGoalRevision() == followGoalRevision,
                    "Resumed follow bound the wrong goal revision: " + skill
            );
            stage = FollowStage.FOLLOW;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForPhysicalFollow() {
            helper.assertTrue(
                    System.nanoTime() - stageStartedNanos
                        <= PHYSICAL_FOLLOW_TIMEOUT_NANOS,
                    "Companion did not continuously follow the moving "
                        + "player; " + followDiagnostics()
            );
            final ServerPlayer body = AiPlayerManager
                    .onlinePlayer(runtime.server())
                    .orElseThrow();
            helper.assertTrue(
                    body.isAlive()
                        && human.isAlive()
                        && human.connection != null
                        && human.connection.isAcceptingMessages(),
                    "A follow participant left the safe course: "
                        + "bodyAlive=" + body.isAlive()
                        + ", humanAlive=" + human.isAlive()
                        + ", humanRemoved=" + human.isRemoved()
                        + ", body=" + body.position()
                        + ", human=" + human.position()
                        + ", bodyPath=" + bodyPath
                        + ", distance=" + body.distanceTo(human)
                        + ", keepAliveSeen="
                        + humanSession.keepAlivePackets
                        + ", keepAliveAccepted="
                        + humanSession.keepAliveAccepted
            );
            final var continuousSkill =
                    runtime.ski…115590 tokens truncated…sion) {
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
                    if (enteredEndAt < 0L) {
                        enteredEndAt = helper.getTick();
                        return;
                    }
                    if (helper.getTick() - enteredEndAt
                            < END_SETTLE_TICKS) {
                        return;
                    }
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
                                && hasPhysicalDragonDamageEvidence(
                                    body,
                                    victoryArena
                                ),
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
                    + ", dragonParts="
                    + (victoryArena == null
                        ? "none"
                        : java.util.Arrays.stream(
                                victoryArena.dragon().getSubEntities()
                            )
                            .map(part -> part.name + "@" + part.position())
                            .toList())
                    + ", perception="
                    + runtime.coreFrames().current()
                        .map(frame -> "visibleEntities="
                            + frame.visibleEntities()
                            + ", dangers=" + frame.dangerSignals()
                            + ", revision=" + frame.observationRevision())
                        .orElse("unavailable")
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
        private static final long END_SETTLE_TICKS = 80L;
        private static final long FIGHT_TIMEOUT_NANOS =
                java.time.Duration.ofSeconds(150).toNanos();
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
        private long controlledRallyMarkedAt = -1L;
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
                case SETTLING_END -> waitForEndSettle();
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
                    /*
                     * SafeCompanionSpawnLocator examines the anchor ring in
                     * a deterministic (-1,-1) first position.  The fixture
                     * maze is authored around the body's pre-login block;
                     * placing the human one block forward therefore keeps
                     * the ordinary remove/relogin anchor inside that same
                     * observed corridor instead of moving the body outside
                     * the release-excluded structure.
                     */
                    body.position().add(1.0D, 0.0D, 1.0D)
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
            stage = StrongholdPortalRoomStage.GOAL;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForGoal() {
            /*
             * The production login path may need one remove-and-relogin
             * cycle when the server spawned the body before any human was
             * online.  Keep the real chat sender connected until that
             * bounded initial anchor is committed; closing it in the same
             * tick would cancel the pending anchor and make the body
             * disappear for a reason unrelated to the model or the route.
             */
            if (humanSession != null) {
                if (runtime.worldData().bodyNeedsInitialAnchor()) {
                    assertWithin(
                            MODEL_TIMEOUT_NANOS,
                            "Initial human anchor did not settle: "
                                + diagnostics()
                    );
                    return;
                }
                humanSession.close();
                humanSession = null;
            }
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
            final EnumSet<SurvivalMilestone> checkpoint = EnumSet.of(
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
                    SurvivalMilestone.STRONGHOLD_BEARING_MEASURED,
                    SurvivalMilestone.STRONGHOLD_SEARCH_AREA_TRIANGULATED
            );
            if (requireVictory) {
                /*
                 * This variant is deliberately a dragon-control slice.  Its
                 * natural-ingress proof is a separate gate, so bind the
                 * explicit rally precondition before portal entry; otherwise
                 * the production planner correctly starts reach_end_island
                 * during the first End tick, before this fixture can install
                 * its bounded target arena.
                 */
                checkpoint.add(SurvivalMilestone.END_ISLAND_REACHED);
            }
            runtime.worldData().markVerifiedRouteMilestones(
                    goal.revision(),
                    checkpoint
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
                    /* Move only the focused dragon-control body onto its
                     * explicit central rally before the first post-entry
                     * planner tick.  Otherwise production correctly starts
                     * natural ingress while this release-excluded fixture is
                     * still waiting for vanilla's dragon-fight scan. */
                    prepareControlledCentralRally(
                            runtime,
                            body
                    );
                    enteredEndAt = helper.getTick();
                    /* Let the vanilla EnderDragonFight finish its one-time
                     * legacy-state scan before installing the release-
                     * excluded combat target.  Installing a custom dragon
                     * in the same tick as portal travel makes that scan see
                     * a live dragon without an exit portal and discard the
                     * fixture entity, which is exactly what vanilla does in
                     * a real world. */
                    stage = StrongholdPortalRoomStage.SETTLING_END;
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

        private static void prepareControlledCentralRally(
                final ServerRuntime runtime,
                final ServerPlayer body
        ) {
            final var end = runtime.server().getLevel(Level.END);
            if (end == null) {
                return;
            }
            final BlockPos entry = body.blockPosition();
            final BlockPos rally = new BlockPos(0, entry.getY(), 0);
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    end.setBlockAndUpdate(
                            rally.offset(x, -1, z),
                            Blocks.OBSIDIAN.defaultBlockState()
                    );
                    for (int y = 0; y <= 2; y++) {
                        end.setBlockAndUpdate(
                                rally.offset(x, y, z),
                                Blocks.AIR.defaultBlockState()
                        );
                    }
                }
            }
            end.setBlockAndUpdate(
                    rally.offset(0, -1, 0),
                    Blocks.END_STONE.defaultBlockState()
            );
            end.getChunkAt(rally);
            body.teleportTo(
                    rally.getX() + 0.5D,
                    rally.getY(),
                    rally.getZ() + 0.5D
            );
            body.setDeltaMovement(Vec3.ZERO);
            body.fallDistance = 0.0F;
        }

        private void waitForEndSettle() {
            assertNoHumanPlayers();
            final ServerPlayer body = body();
            helper.assertTrue(
                    bodyId.equals(body.getUUID())
                        && body.level().dimension().equals(Level.END),
                    "Companion left the End during vanilla fight-state "
                        + "settling: " + diagnostics()
            );
            if (helper.getTick() - enteredEndAt < END_SETTLE_TICKS) {
                return;
            }
            victoryArena = prepareEndVictoryArena(
                    helper,
                    runtime,
                    body,
                    requireVictory,
                    requireVictory
            );
            /*
             * This release-excluded method is a focused dragon-control
             * slice, not the natural End ingress gate.  The body was placed
             * on the explicit central rally immediately after real portal
             * entry, and the bounded dragon arena is installed only after
             * the vanilla fight-state scan.  The separate ingress GameTest
             * owns bridge/tower/landfall proof.  Bind this test-only
             * precondition to the goal so production current-pose ingress
             * checks remain unchanged.
             */
            runtime.worldData().markVerifiedRouteMilestones(
                    goalRevision,
                    EnumSet.of(SurvivalMilestone.END_ISLAND_REACHED)
            );
            controlledRallyMarkedAt = helper.getTick();
            stage = StrongholdPortalRoomStage.VICTORY_VISIBLE;
            stageStartedNanos = System.nanoTime();
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
            /* The test-only rally attestation is persisted on the server
             * thread, while the next model observation is assembled from a
             * later route snapshot.  Do not issue a fight/ingress request
             * against the one-tick stale snapshot: wait until the attestation
             * is visible in the same goal-bound progress record that the
             * planner receives. */
            if (!runtime.worldData()
                    .verifiedRouteProgress(goalRevision)
                    .milestones()
                    .contains(SurvivalMilestone.END_ISLAND_REACHED)) {
                assertWithin(
                        MODEL_TIMEOUT_NANOS,
                        "Controlled End rally attestation was not visible: "
                            + diagnostics()
                );
                return;
            }
            /* Route JSON is assembled on the runtime observation cadence,
             * not synchronously with SavedData writes.  Hold the fixture for
             * two observation windows so the next model request cannot race
             * the attestation and choose ingress on a stale snapshot. */
            if (controlledRallyMarkedAt < 0L
                    || helper.getTick() - controlledRallyMarkedAt < 40L) {
                assertWithin(
                        MODEL_TIMEOUT_NANOS,
                        "Controlled End rally observation window has not "
                            + "settled: " + diagnostics()
                );
                return;
            }
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
                            + ", dragonDiagnostics="
                            + dragonDiagnostics()
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
                            && hasPhysicalDragonDamageEvidence(
                                body,
                                victoryArena
                            ),
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
                        + ", dragonDiagnostics="
                        + dragonDiagnostics()
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

        /**
         * Keep the release-excluded dragon gate diagnosable when a real model
         * run loses the target. This reads only the bounded loaded entity
         * window around the actual body and the current fair frame; it is not
         * used by production decisions or by the skill under test.
         */
        private String dragonDiagnostics() {
            final ServerPlayer body = AiPlayerManager
                    .onlinePlayer(runtime.server())
                    .orElse(null);
            if (body == null) {
                return "body=absent";
            }
            final var end = runtime.server().getLevel(Level.END);
            if (end == null || body.level() != end) {
                return "bodyLevel=" + body.level().dimension().identifier();
            }
            final List<? extends EnderDragon> loadedDragons = end.getDragons()
                    .stream()
                    .filter(Entity::isAlive)
                    .filter(dragon -> body.distanceToSqr(dragon) <= 64.0D * 64.0D)
                    .toList();
            final String dragonPositions = loadedDragons.stream()
                    .map(dragon -> String.format(
                            Locale.ROOT,
                            "%s@%.1f,%.1f,%.1f/alive=%s/noAi=%s",
                            dragon.getUUID(),
                            dragon.getX(),
                            dragon.getY(),
                            dragon.getZ(),
                            dragon.isAlive(),
                            dragon.isNoAi()
                    ))
                    .toList()
                    .toString();
            final String fixtureDragon = victoryArena == null
                    ? "none"
                    : "uuid=" + victoryArena.dragon().getUUID()
                        + ",removed=" + victoryArena.dragon().isRemoved()
                        + ",level=" + (victoryArena.dragon().level() == null
                            ? "null"
                            : victoryArena.dragon().level().dimension()
                                .identifier());
            final var frame = runtime.coreFrames().current();
            final String visible = frame.map(current -> current
                    .visibleEntities()
                    .stream()
                    .map(entity -> entity.entityTypeId()
                            + "@" + String.format(
                                Locale.ROOT,
                                "%.1f",
                                entity.distance()
                            ))
                    .toList()
                    .toString())
                    .orElse("<no-frame>");
            return "body=" + body.position()
                    + ",bodyChunk=" + body.chunkPosition()
                    + ",requestedView=" + body.requestedViewDistance()
                    + ",trackingView=" + body.getChunkTrackingView()
                    + ",fixtureDragon=" + fixtureDragon
                    + ",loadedDragons=" + dragonPositions
                    + ",visible=" + visible;
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
        SETTLING_END,
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
                java.time.Duration.ofSeconds(150).toNanos();
        private static final long END_SETTLE_TICKS = 80L;
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
        private boolean chatSubmitted;

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
                case SETTLING_END -> waitForEndSettle();
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
            /*
             * The first human login may synchronously remove and relogin the
             * unanchored companion. Keep this real chat connection alive and
             * defer submission until the replacement ServerPlayer is visible
             * again. Closing it in the same tick as placeNewPlayer races the
             * authoritative body transaction and produced a misleading
             * Optional.get/"No value present" fixture failure before the
             * model was ever called.
             */
            stage = EndPortalStage.GOAL;
            stageStartedNanos = System.nanoTime();
        }

        private void waitForGoal() {
            assertWithinModelDeadline(
                    "Live model did not classify the End-portal task"
            );
            final Optional<ServerPlayer> bodyCandidate =
                    AiPlayerManager.onlinePlayer(runtime.server());
            if (bodyCandidate.isEmpty()) {
                return;
            }
            helper.assertTrue(
                    bodyId.equals(bodyCandidate.orElseThrow().getUUID()),
                    "Initial-anchor relogin changed the companion UUID"
            );
            if (!chatSubmitted) {
                final ServerPlayer human = humanSession.player();
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
                chatSubmitted = true;
                return;
            }
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
            humanSession.close();
            humanSession = null;
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
                    stage = EndPortalStage.SETTLING_END;
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

        private void waitForEndSettle() {
            assertNoHumanPlayers();
            final ServerPlayer body = AiPlayerManager
                    .onlinePlayer(runtime.server())
                    .orElseThrow();
            helper.assertTrue(
                    bodyId.equals(body.getUUID())
                        && body.level().dimension().equals(Level.END),
                    "Late completion body left the End during vanilla "
                        + "fight-state settling: " + diagnostics()
            );
            if (helper.getTick() - enteredEndAt < END_SETTLE_TICKS) {
                return;
            }
            victoryArena = prepareEndVictoryArena(
                    helper,
                    runtime,
                    body,
                    false,
                    false
            );
            stage = EndPortalStage.VICTORY_VISIBLE;
            stageStartedNanos = System.nanoTime();
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
                            && hasPhysicalDragonDamageEvidence(
                                body,
                                victoryArena
                            ),
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
        SETTLING_END,
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
