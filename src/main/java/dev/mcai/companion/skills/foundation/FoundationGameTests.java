package dev.mcai.companion.skills.foundation;

import dev.mcai.companion.embodiment.AiPlayerManager;
import dev.mcai.companion.embodiment.GameTestCompanionSpawn;
import dev.mcai.companion.embodiment.SessionState;
import dev.mcai.companion.perception.FairPerceptionSampler;
import dev.mcai.companion.progression.VerifiedFixtureLocation;
import dev.mcai.companion.skill.SkillContext;
import dev.mcai.companion.skill.SkillTickResult;
import dev.mcai.companion.skills.core.NoParameters;
import dev.mcai.companion.skills.core.ServerCoreSkillFrameSource;
import dev.mcai.companion.skills.core.ServerOwnedCoreSkillActuator;
import dev.mcai.companion.skills.gathering
        .ServerResourceInventorySource;
import dev.mcai.companion.skills.interaction
        .ServerInteractionSkillFrameSource;
import dev.mcai.companion.skills.interaction
        .ServerOwnedInteractionSkillActuator;
import dev.mcai.companion.skills.inventory
        .ServerInventorySkillActuator;
import dev.mcai.companion.skills.menu.ServerMenuSkillActuator;
import dev.mcai.companion.skills.menu.ServerMenuSkillFrameSource;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Focused real-server contracts for early survival compound skills.
 */
public final class FoundationGameTests {
    private static final BlockPos TEST_ORIGIN =
            new BlockPos(16, 8, 16);
    private static final int BODY_START_TIMEOUT_TICKS = 3_000;

    private FoundationGameTests() {
    }

    /**
     * Runs the production basic-crafting state machine through ordinary
     * recipes, first-person table placement, menu opening, and result clicks.
     * A closer high-column top is deliberately visible; the skill must reject
     * it and place a reachable table on the local floor.
     */
    public static void reachableBasicCrafting(
            final GameTestHelper helper
    ) {
        final var server = helper.getLevel().getServer();
        final AtomicReference<Scenario> scenario =
                new AtomicReference<>();
        final long startedAt = helper.getTick();

        helper.addCleanup(ignored -> {
            final Scenario current = scenario.get();
            if (current != null) {
                current.cleanup();
            }
            if (AiPlayerManager.status(server).state()
                    != SessionState.ABSENT) {
                AiPlayerManager.requestRemove(server);
            }
        });
        GameTestCompanionSpawn.resetForIsolatedFixture(server);

        helper.assertTrue(
                AiPlayerManager.status(server).state()
                        == SessionState.ABSENT,
                "Basic-crafting gate requires an isolated body"
        );
        helper.assertTrue(
                GameTestCompanionSpawn.request(
                        helper,
                        TEST_ORIGIN
                ).accepted(),
                "Basic-crafting companion spawn was rejected"
        );

        helper.onEachTick(() -> {
            final Scenario current = scenario.get();
            if (current != null) {
                current.tick();
                return;
            }
            final var status = AiPlayerManager.status(server);
            helper.assertTrue(
                    status.state() != SessionState.FAILED,
                    "Basic-crafting companion body failed"
            );
            if (status.state() != SessionState.ACTIVE
                    || !status.online()) {
                helper.assertTrue(
                        helper.getTick() - startedAt
                                <= BODY_START_TIMEOUT_TICKS,
                        "Basic-crafting companion did not become active"
                );
                return;
            }
            scenario.set(new Scenario(
                    helper,
                    AiPlayerManager.onlinePlayer(server).orElseThrow(),
                    helper.absolutePos(TEST_ORIGIN)
            ));
        });
    }

    /**
     * Reproduces the post-smelting field failure: a verified table is within
     * raw reach but hidden by the newly placed furnace and adjacent blocks.
     * Success requires an ordinary side-step route, fresh first-person table
     * verification, a real menu, and vanilla recipe result clicks.
     */
    public static void occludedIronToolkitTable(
            final GameTestHelper helper
    ) {
        final var server = helper.getLevel().getServer();
        final AtomicReference<OccludedTableScenario> scenario =
                new AtomicReference<>();
        final long startedAt = helper.getTick();

        helper.addCleanup(ignored -> {
            final OccludedTableScenario current = scenario.get();
            if (current != null) {
                current.cleanup();
            }
            if (AiPlayerManager.status(server).state()
                    != SessionState.ABSENT) {
                AiPlayerManager.requestRemove(server);
            }
        });
        GameTestCompanionSpawn.resetForIsolatedFixture(server);

        helper.assertTrue(
                AiPlayerManager.status(server).state()
                        == SessionState.ABSENT,
                "Occluded-table gate requires an isolated body"
        );
        helper.assertTrue(
                GameTestCompanionSpawn.request(
                        helper,
                        TEST_ORIGIN
                ).accepted(),
                "Occluded-table companion spawn was rejected"
        );

        helper.onEachTick(() -> {
            final OccludedTableScenario current =
                    scenario.get();
            if (current != null) {
                current.tick();
                return;
            }
            final var status = AiPlayerManager.status(server);
            helper.assertTrue(
                    status.state() != SessionState.FAILED,
                    "Occluded-table companion body failed"
            );
            if (status.state() != SessionState.ACTIVE
                    || !status.online()) {
                helper.assertTrue(
                        helper.getTick() - startedAt
                                <= BODY_START_TIMEOUT_TICKS,
                        "Occluded-table companion did not become active"
                );
                return;
            }
            scenario.set(new OccludedTableScenario(
                    helper,
                    AiPlayerManager.onlinePlayer(server).orElseThrow(),
                    helper.absolutePos(TEST_ORIGIN)
            ));
        });
    }

    private static final class Scenario {
        private static final int EXECUTION_TIMEOUT_TICKS = 1_200;

        private final GameTestHelper helper;
        private final ServerPlayer player;
        private final BlockPos origin;
        private final FairPerceptionSampler sampler =
                new FairPerceptionSampler();
        private final ServerCoreSkillFrameSource coreFrames;
        private final ServerInteractionSkillFrameSource
                interactionFrames;
        private final ServerOwnedCoreSkillActuator core;
        private final PrepareBasicCraftingSkill skill;
        private final long createdAt;
        private final int initialCraftedTables;
        private final int initialCraftedPickaxes;

        private boolean started;
        private boolean finished;
        private boolean cleaned;

        private Scenario(
                final GameTestHelper helper,
                final ServerPlayer player,
                final BlockPos origin
        ) {
            this.helper = helper;
            this.player = player;
            this.origin = origin.immutable();
            createdAt = helper.getTick();
            prepareFixture();
            initialCraftedTables = player.getStats().getValue(
                    net.minecraft.stats.Stats.ITEM_CRAFTED.get(
                            Items.CRAFTING_TABLE
                    )
            );
            initialCraftedPickaxes = player.getStats().getValue(
                    net.minecraft.stats.Stats.ITEM_CRAFTED.get(
                            Items.WOODEN_PICKAXE
                    )
            );
            final var server = helper.getLevel().getServer();
            coreFrames = new ServerCoreSkillFrameSource(
                    server,
                    player.getUUID()
            );
            interactionFrames =
                    new ServerInteractionSkillFrameSource(
                            server,
                            player.getUUID()
                    );
            core = new ServerOwnedCoreSkillActuator(
                    server,
                    player.getUUID()
            );
            skill = new PrepareBasicCraftingSkill(
                    player.getUUID(),
                    core,
                    coreFrames,
                    new ServerOwnedInteractionSkillActuator(
                            server,
                            player.getUUID()
                    ),
                    interactionFrames,
                    new ServerInventorySkillActuator(
                            server,
                            player.getUUID()
                    )
            );
        }

        private void prepareFixture() {
            final var level = helper.getLevel();
            for (int x = -8; x <= 8; x++) {
                for (int z = -8; z <= 8; z++) {
                    level.setBlockAndUpdate(
                            origin.offset(x, -1, z),
                            Blocks.DIRT.defaultBlockState()
                    );
                    for (int y = 0; y <= 6; y++) {
                        level.setBlockAndUpdate(
                                origin.offset(x, y, z),
                                Blocks.AIR.defaultBlockState()
                        );
                    }
                }
            }
            final BlockPos highColumn = origin.south(2);
            for (int y = -1; y <= 2; y++) {
                level.setBlockAndUpdate(
                        highColumn.above(y),
                        Blocks.STONE.defaultBlockState()
                );
            }
            /*
             * Match the real zero-human foundation fixture that exposed the
             * regression: nearby livestock occupies the most attractive
             * floor supports. A normal player sees the animals and chooses a
             * clear square instead of sending six placement packets into
             * entity collision and waiting for the whole skill deadline.
             */
            for (int index = 0; index < 8; index++) {
                final Cow cow = EntityTypes.COW.create(
                        level,
                        EntitySpawnReason.COMMAND
                );
                helper.assertTrue(
                        cow != null,
                        "Basic-crafting fixture could not create cow"
                );
                cow.setBaby(false);
                cow.setNoAi(true);
                final int column = index % 4;
                final int row = index / 4;
                cow.setPos(
                        origin.getX() - 3.0 + column * 1.75,
                        origin.getY(),
                        origin.getZ() + 1.0 + row * 1.75
                );
                helper.assertTrue(
                        level.addFreshEntity(cow),
                        "Basic-crafting fixture could not add cow"
                );
            }
            player.setGameMode(GameType.SURVIVAL);
            player.getInventory().clearContent();
            player.getInventory().setSelectedSlot(0);
            player.setItemInHand(
                    InteractionHand.MAIN_HAND,
                    new ItemStack(Items.OAK_LOG, 3)
            );
            unlock("minecraft:oak_planks");
            unlock("minecraft:crafting_table");
            unlock("minecraft:stick");
            unlock("minecraft:wooden_pickaxe");
            player.inventoryMenu.broadcastChanges();
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.setDeltaMovement(Vec3.ZERO);
            player.teleportTo(
                    origin.getX() + 0.5,
                    origin.getY(),
                    origin.getZ() + 0.5
            );
            player.setXRot(48.0F);
            player.setYRot(0.0F);
            player.setYHeadRot(0.0F);
        }

        private void unlock(final String recipeId) {
            final ResourceKey<Recipe<?>> key = ResourceKey.create(
                    Registries.RECIPE,
                    Identifier.parse(recipeId)
            );
            player.getRecipeBook().add(
                    player.level()
                            .getServer()
                            .getRecipeManager()
                            .byKey(key)
                            .orElseThrow()
                            .id()
            );
        }

        private void tick() {
            if (finished) {
                return;
            }
            helper.assertTrue(
                    helper.getTick() - createdAt
                            <= EXECUTION_TIMEOUT_TICKS,
                    "Basic-crafting skill timed out: "
                            + checkpoint()
            );
            try {
                final var observation = sampler.sample(player);
                coreFrames.publish(observation);
                interactionFrames.publish(observation);
                final SkillContext context = new SkillContext(
                        1,
                        observation.sequence(),
                        helper.getTick(),
                        true,
                        true,
                        0.0
                );
                if (!started) {
                    final var rejected = skill.preconditions(
                            context,
                            NoParameters.INSTANCE
                    );
                    helper.assertTrue(
                            rejected.isEmpty(),
                            "Basic-crafting start rejected: " + rejected
                    );
                    skill.start(context, NoParameters.INSTANCE);
                    started = true;
                }
                final SkillTickResult result = skill.tick(
                        context,
                        NoParameters.INSTANCE
                );
                if (result.status()
                        == SkillTickResult.Status.FAILED) {
                    finished = true;
                    helper.assertTrue(
                            false,
                            "Basic-crafting skill failed: "
                                    + result.failure()
                                    + ", checkpoint=" + checkpoint()
                    );
                    return;
                }
                if (result.status()
                        == SkillTickResult.Status.COMPLETED) {
                    verifyCompletion();
                }
            } finally {
                if (!finished) {
                    core.postServerTick();
                }
            }
        }

        private void verifyCompletion() {
            helper.assertTrue(
                    player.getInventory().countItem(
                            Items.WOODEN_PICKAXE
                    ) == 1,
                    "Basic crafting completed without an owned pickaxe"
            );
            helper.assertTrue(
                    player.getStats().getValue(
                            net.minecraft.stats.Stats.ITEM_CRAFTED.get(
                                    Items.CRAFTING_TABLE
                            )
                    ) > initialCraftedTables,
                    "Crafting table did not come from a vanilla recipe"
            );
            helper.assertTrue(
                    player.getStats().getValue(
                            net.minecraft.stats.Stats.ITEM_CRAFTED.get(
                                    Items.WOODEN_PICKAXE
                            )
                    ) > initialCraftedPickaxes,
                    "Wooden pickaxe did not come from a vanilla recipe"
            );
            final BlockPos table = BlockPos.betweenClosedStream(
                    origin.offset(-10, -1, -10),
                    origin.offset(10, 2, 10)
            ).filter(pos ->
                    helper.getLevel().getBlockState(pos)
                            .is(Blocks.CRAFTING_TABLE)
            ).findFirst().orElseThrow(() ->
                    helper.assertionException(
                            "No physical crafting table was placed"
                    )
            );
            helper.assertTrue(
                    Math.abs(table.getY() - origin.getY()) <= 1,
                    "Crafting table was placed on the high-column trap: "
                            + table
            );
            finished = true;
            helper.succeed();
        }

        private String checkpoint() {
            return skill.checkpoint(
                    new SkillContext(
                            1,
                            0,
                            helper.getTick(),
                            true,
                            true,
                            0.0
                    ),
                    NoParameters.INSTANCE
            ).payload();
        }

        private void cleanup() {
            if (cleaned) {
                return;
            }
            cleaned = true;
            core.quiesceNow();
            if (player.containerMenu != player.inventoryMenu) {
                player.closeContainer();
            }
        }
    }

    private static final class OccludedTableScenario {
        private static final int EXECUTION_TIMEOUT_TICKS = 2_000;

        private final GameTestHelper helper;
        private final ServerPlayer player;
        private final BlockPos origin;
        private final BlockPos table;
        private final BlockPos furnace;
        private final Vec3 initialPosition;
        private final FairPerceptionSampler sampler =
                new FairPerceptionSampler();
        private final ServerCoreSkillFrameSource coreFrames;
        private final ServerInteractionSkillFrameSource
                interactionFrames;
        private final ServerMenuSkillFrameSource menuFrames;
        private final ServerOwnedCoreSkillActuator core;
        private final PrepareIronToolkitSkill skill;
        private final long createdAt;
        private final int initialIronPickaxes;
        private final int initialBuckets;
        private final int initialShields;

        private boolean initialOcclusionVerified;
        private boolean started;
        private boolean finished;
        private boolean cleaned;

        private OccludedTableScenario(
                final GameTestHelper helper,
                final ServerPlayer player,
                final BlockPos origin
        ) {
            this.helper = helper;
            this.player = player;
            this.origin = origin.immutable();
            table = this.origin.immutable();
            furnace = this.origin.south();
            createdAt = helper.getTick();
            prepareFixture();
            initialPosition = player.position();
            initialIronPickaxes = crafted(Items.IRON_PICKAXE);
            initialBuckets = crafted(Items.BUCKET);
            initialShields = crafted(Items.SHIELD);
            final var server = helper.getLevel().getServer();
            coreFrames = new ServerCoreSkillFrameSource(
                    server,
                    player.getUUID()
            );
            interactionFrames =
                    new ServerInteractionSkillFrameSource(
                            server,
                            player.getUUID()
                    );
            menuFrames = new ServerMenuSkillFrameSource(
                    server,
                    player.getUUID()
            );
            core = new ServerOwnedCoreSkillActuator(
                    server,
                    player.getUUID()
            );
            final var interaction =
                    new ServerOwnedInteractionSkillActuator(
                            server,
                            player.getUUID()
                    );
            final var inventory =
                    new ServerInventorySkillActuator(
                            server,
                            player.getUUID()
                    );
            skill = new PrepareIronToolkitSkill(
                    player.getUUID(),
                    core,
                    coreFrames,
                    interaction,
                    interactionFrames,
                    inventory,
                    new ServerResourceInventorySource(
                            server,
                            player.getUUID()
                    ),
                    new ServerMenuSkillActuator(
                            server,
                            player.getUUID(),
                            menuFrames
                    ),
                    menuFrames,
                    ignored -> Optional.of(location(table)),
                    ignored -> Optional.of(location(furnace))
            );
        }

        private void prepareFixture() {
            final var level = helper.getLevel();
            for (int x = -7; x <= 7; x++) {
                for (int z = -7; z <= 7; z++) {
                    level.setBlockAndUpdate(
                            origin.offset(x, -1, z),
                            Blocks.DIRT.defaultBlockState()
                    );
                    for (int y = 0; y <= 4; y++) {
                        level.setBlockAndUpdate(
                                origin.offset(x, y, z),
                                Blocks.AIR.defaultBlockState()
                        );
                    }
                }
            }
            level.setBlockAndUpdate(
                    table,
                    Blocks.CRAFTING_TABLE.defaultBlockState()
            );
            level.setBlockAndUpdate(
                    furnace,
                    Blocks.FURNACE.defaultBlockState()
            );
            level.setBlockAndUpdate(
                    furnace.west(),
                    Blocks.STONE.defaultBlockState()
            );
            level.setBlockAndUpdate(
                    furnace.east(),
                    Blocks.STONE.defaultBlockState()
            );
            for (BlockPos lower : List.of(
                    furnace.west(),
                    furnace,
                    furnace.east()
            )) {
                level.setBlockAndUpdate(
                        lower.above(),
                        Blocks.STONE.defaultBlockState()
                );
            }
            player.setGameMode(GameType.SURVIVAL);
            player.getInventory().clearContent();
            player.getInventory().setSelectedSlot(0);
            player.setItemInHand(
                    InteractionHand.MAIN_HAND,
                    new ItemStack(Items.STONE_PICKAXE)
            );
            player.getInventory().setItem(
                    1,
                    new ItemStack(Items.IRON_INGOT, 7)
            );
            player.getInventory().setItem(
                    2,
                    new ItemStack(Items.STICK, 2)
            );
            player.getInventory().setItem(
                    3,
                    new ItemStack(Items.OAK_PLANKS, 6)
            );
            unlock("minecraft:iron_pickaxe");
            unlock("minecraft:bucket");
            unlock("minecraft:shield");
            player.inventoryMenu.broadcastChanges();
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.setDeltaMovement(Vec3.ZERO);
            player.teleportTo(
                    origin.getX() + 0.5,
                    origin.getY(),
                    origin.getZ() + 3.5
            );
            player.setXRot(12.0F);
            player.setYRot(180.0F);
            player.setYHeadRot(180.0F);
        }

        private void unlock(final String recipeId) {
            final ResourceKey<Recipe<?>> key = ResourceKey.create(
                    Registries.RECIPE,
                    Identifier.parse(recipeId)
            );
            player.getRecipeBook().add(
                    player.level()
                            .getServer()
                            .getRecipeManager()
                            .byKey(key)
                            .orElseThrow()
                            .id()
            );
        }

        private void tick() {
            if (finished) {
                return;
            }
            helper.assertTrue(
                    helper.getTick() - createdAt
                            <= EXECUTION_TIMEOUT_TICKS,
                    "Occluded-table iron toolkit timed out: "
                            + checkpoint()
            );
            try {
                final var observation = sampler.sample(player);
                if (!initialOcclusionVerified) {
                    helper.assertTrue(
                            observation.visibleBlockFaces()
                                    .stream()
                                    .noneMatch(face ->
                                            face.blockTypeId().equals(
                                                    "minecraft:"
                                                            + "crafting_table"
                                            )
                                    ),
                            "Fixture did not initially occlude the table"
                    );
                    initialOcclusionVerified = true;
                }
                coreFrames.publish(observation);
                interactionFrames.publish(observation);
                menuFrames.publish(observation);
                final SkillContext context = new SkillContext(
                        1,
                        observation.sequence(),
                        helper.getTick(),
                        true,
                        true,
                        0.0
                );
                if (!started) {
                    final var rejected = skill.preconditions(
                            context,
                            NoParameters.INSTANCE
                    );
                    helper.assertTrue(
                            rejected.isEmpty(),
                            "Occluded-table start rejected: "
                                    + rejected
                    );
                    skill.start(context, NoParameters.INSTANCE);
                    started = true;
                }
                final SkillTickResult result = skill.tick(
                        context,
                        NoParameters.INSTANCE
                );
                if (result.status()
                        == SkillTickResult.Status.FAILED) {
                    finished = true;
                    helper.assertTrue(
                            false,
                            "Occluded-table skill failed: "
                                    + result.failure()
                                    + ", checkpoint=" + checkpoint()
                    );
                    return;
                }
                if (result.status()
                        == SkillTickResult.Status.COMPLETED) {
                    verifyCompletion();
                }
            } finally {
                if (!finished) {
                    core.postServerTick();
                }
            }
        }

        private void verifyCompletion() {
            helper.assertTrue(
                    initialOcclusionVerified,
                    "Initial table occlusion was not verified"
            );
            helper.assertTrue(
                    player.position().distanceTo(initialPosition)
                            >= 1.0,
                    "Body never physically left the occluded stance"
            );
            helper.assertTrue(
                    player.getInventory().countItem(
                            Items.IRON_PICKAXE
                    ) == 1
                            && player.getInventory().countItem(
                                    Items.BUCKET
                            ) == 1
                            && player.getInventory().countItem(
                                    Items.SHIELD
                            ) == 1,
                    "Iron toolkit outputs are incomplete"
            );
            helper.assertTrue(
                    crafted(Items.IRON_PICKAXE)
                            > initialIronPickaxes
                            && crafted(Items.BUCKET)
                                > initialBuckets
                            && crafted(Items.SHIELD)
                                > initialShields,
                    "Toolkit outputs did not come from vanilla recipes"
            );
            helper.assertTrue(
                    helper.getLevel().getBlockState(table)
                            .is(Blocks.CRAFTING_TABLE)
                            && helper.getLevel()
                                .getBlockState(furnace)
                                .is(Blocks.FURNACE),
                    "Fixture blocks changed during recovery"
            );
            finished = true;
            helper.succeed();
        }

        private int crafted(
                final net.minecraft.world.item.Item item
        ) {
            return player.getStats().getValue(
                    net.minecraft.stats.Stats.ITEM_CRAFTED.get(item)
            );
        }

        private VerifiedFixtureLocation location(
                final BlockPos position
        ) {
            return new VerifiedFixtureLocation(
                    helper.getLevel().dimension()
                            .identifier().toString(),
                    position.getX(),
                    position.getY(),
                    position.getZ()
            );
        }

        private String checkpoint() {
            return skill.checkpoint(
                    new SkillContext(
                            1,
                            0,
                            helper.getTick(),
                            true,
                            true,
                            0.0
                    ),
                    NoParameters.INSTANCE
            ).payload();
        }

        private void cleanup() {
            if (cleaned) {
                return;
            }
            cleaned = true;
            core.quiesceNow();
            if (player.containerMenu != player.inventoryMenu) {
                player.closeContainer();
            }
        }
    }
}
