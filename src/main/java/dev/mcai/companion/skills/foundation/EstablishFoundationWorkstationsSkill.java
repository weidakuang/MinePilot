package dev.mcai.companion.skills.foundation;

import dev.mcai.companion.MinecraftAiCompanion;
import dev.mcai.companion.action.ActionHand;
import dev.mcai.companion.action.ActionOutcome;
import dev.mcai.companion.action.ActionVec3;
import dev.mcai.companion.action.BlockFace;
import dev.mcai.companion.action.BlockInteractionTarget;
import dev.mcai.companion.action.LookIntent;
import dev.mcai.companion.model.SkillArgument;
import dev.mcai.companion.navigation.GridPos;
import dev.mcai.companion.navigation.NavigationEvidence;
import dev.mcai.companion.navigation.VoxelKind;
import dev.mcai.companion.perception.BlockCoordinate;
import dev.mcai.companion.perception.InventoryItemSummary;
import dev.mcai.companion.perception.MenuSlotSummary;
import dev.mcai.companion.perception.PerceptionVec3;
import dev.mcai.companion.perception.TopSupportAffordance;
import dev.mcai.companion.perception.VisibleBlockFace;
import dev.mcai.companion.progression.VerifiedFixtureLocation;
import dev.mcai.companion.skill.Skill;
import dev.mcai.companion.skill.SkillCheckpoint;
import dev.mcai.companion.skill.SkillContext;
import dev.mcai.companion.skill.SkillFailure;
import dev.mcai.companion.skill.SkillParameterParser;
import dev.mcai.companion.skill.SkillParameterResult;
import dev.mcai.companion.skill.SkillResult;
import dev.mcai.companion.skill.SkillTickResult;
import dev.mcai.companion.skills.core.CoreSkillActuator;
import dev.mcai.companion.skills.core.CoreSkillFrame;
import dev.mcai.companion.skills.core.CoreSkillFrameSource;
import dev.mcai.companion.skills.core.MoveToParameters;
import dev.mcai.companion.skills.core.MoveToSkill;
import dev.mcai.companion.skills.core.NoParameters;
import dev.mcai.companion.skills.gathering.ResourceInventorySource;
import dev.mcai.companion.skills.interaction.InteractionSkillActuator;
import dev.mcai.companion.skills.interaction.InteractionSkillFrameSource;
import dev.mcai.companion.skills.inventory.CraftRecipeParameters;
import dev.mcai.companion.skills.inventory.EquipItemParameters;
import dev.mcai.companion.skills.inventory.EquipmentTarget;
import dev.mcai.companion.skills.inventory.InventoryOperationResult;
import dev.mcai.companion.skills.inventory.InventorySkillActuator;
import dev.mcai.companion.skills.menu.CloseMenuParameters;
import dev.mcai.companion.skills.menu.MenuBinding;
import dev.mcai.companion.skills.menu.MenuOperationResult;
import dev.mcai.companion.skills.menu.MenuSkillActuator;
import dev.mcai.companion.skills.menu.MenuSkillFrame;
import dev.mcai.companion.skills.menu.MenuSkillFrameSource;
import dev.mcai.companion.skills.menu.TransferMenuItemParameters;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongFunction;

/**
 * Establishes the persistent workstation/storage boundary after the iron
 * toolkit is ready.
 *
 * <p>The model makes one high-level choice. This bounded state machine then
 * performs ordinary recipe, aiming, placement, block-use and chest-menu
 * transactions at server tick speed. It never edits a block, inventory or
 * container directly, and it can use only a table or chest that the body
 * previously opened or can currently see through first-person rays.</p>
 */
public final class EstablishFoundationWorkstationsSkill
        implements Skill<NoParameters> {
    public static final String NAME =
            "establish_foundation_workstations";

    private static final String CRAFTING_TABLE =
            "minecraft:crafting_table";
    private static final String CHEST = "minecraft:chest";
    private static final int REQUIRED_CHEST_PLANKS = 8;
    private static final int MAXIMUM_TICKS = 10_200;
    private static final int MAXIMUM_RECIPE_WAIT_TICKS = 100;
    private static final int MAXIMUM_AIM_TICKS = 80;
    private static final int MAXIMUM_CONFIRM_TICKS = 120;
    private static final int MAXIMUM_RECLAIM_TICKS = 180;
    private static final int MAXIMUM_CHEST_OPEN_ATTEMPTS = 2;
    private static final int MAXIMUM_FIXTURE_MOVE_TICKS = 300;
    private static final int MAXIMUM_FIXTURE_NO_PROGRESS_TICKS = 100;
    private static final double RELIABLE_INTERACTION_DISTANCE = 3.75;
    private static final double APPROACH_RADIUS = 3.0;
    private static final int SCAN_INTERVAL_TICKS = 4;
    private static final int MAXIMUM_SCAN_TURNS = 48;
    private static final double MINIMUM_PLACEMENT_DISTANCE = 1.35;
    private static final double MAXIMUM_PLACEMENT_DISTANCE = 4.25;
    private static final float[] SCAN_YAW_OFFSETS = {
            0.0F,
            -45.0F,
            -90.0F,
            -135.0F,
            180.0F,
            135.0F,
            90.0F,
            45.0F
    };
    private static final float[] SCAN_PITCHES = {
            25.0F,
            45.0F,
            65.0F
    };
    private static final List<String> SURPLUS_PRIORITY = List.of(
            "minecraft:wooden_pickaxe",
            "minecraft:stone_pickaxe"
    );

    private final UUID expectedPlayerId;
    private final CoreSkillActuator core;
    private final CoreSkillFrameSource coreFrames;
    private final InteractionSkillActuator interactions;
    private final InteractionSkillFrameSource interactionFrames;
    private final InventorySkillActuator inventory;
    private final MenuSkillActuator menus;
    private final MenuSkillFrameSource menuFrames;
    private final PrepareFoundationShelterMaterialsSkill materialPreparer;
    private final LongFunction<Optional<VerifiedFixtureLocation>>
            knownCraftingTable;
    private final LongFunction<Optional<VerifiedFixtureLocation>>
            knownStorage;

    private Phase phase = Phase.IDLE;
    private SkillFailure failure;
    private long startedAtTick = -1;
    private long phaseStartedAtTick = -1;
    private long nextScanTick = -1;
    private int scanTurns;
    private float scanBaseYaw;
    private VisibleBlockFace selectedTarget;
    private VisibleBlockFace selectedSupport;
    private BlockCoordinate expectedChestPosition;
    private long depositSampleSequence = -1;
    private String depositedItemId = "";
    private int chestOpenAttempts;
    private MoveToSkill fixtureMovement;
    private MoveToParameters fixtureMovementParameters;
    private Fixture movementTarget;
    private double fixtureMovementBestDistance = Double.POSITIVE_INFINITY;
    private long fixtureMovementLastProgressTick = -1L;
    private final Set<BlockCoordinate> rejectedSupports =
            new HashSet<>();
    private long lastLiveDiagnosticTick = Long.MIN_VALUE;

    public EstablishFoundationWorkstationsSkill(
            final UUID expectedPlayerId,
            final CoreSkillActuator core,
            final CoreSkillFrameSource coreFrames,
            final InteractionSkillActuator interactions,
            final InteractionSkillFrameSource interactionFrames,
            final InventorySkillActuator inventory,
            final ResourceInventorySource resourceInventory,
            final MenuSkillActuator menus,
            final MenuSkillFrameSource menuFrames,
            final LongFunction<Optional<VerifiedFixtureLocation>>
                    knownCraftingTable,
            final LongFunction<Optional<VerifiedFixtureLocation>>
                    knownFurnace,
            final LongFunction<Optional<VerifiedFixtureLocation>>
                    knownStorage
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
        this.menus = Objects.requireNonNull(menus, "menus");
        this.menuFrames = Objects.requireNonNull(
                menuFrames,
                "menuFrames"
        );
        this.knownCraftingTable = Objects.requireNonNull(
                knownCraftingTable,
                "knownCraftingTable"
        );
        this.knownStorage = Objects.requireNonNull(
                knownStorage,
                "knownStorage"
        );
        materialPreparer =
                new PrepareFoundationShelterMaterialsSkill(
                        expectedPlayerId,
                        core,
                        coreFrames,
                        interactions,
                        interactionFrames,
                        inventory,
                        Objects.requireNonNull(
                                resourceInventory,
                                "resourceInventory"
                        ),
                        menus,
                        menuFrames,
                        knownCraftingTable,
                        Objects.requireNonNull(
                                knownFurnace,
                                "knownFurnace"
                        ),
                        REQUIRED_CHEST_PLANKS
                );
    }

    @Override
    public SkillParameterParser<NoParameters> parameters() {
        return EstablishFoundationWorkstationsSkill::parseNone;
    }

    @Override
    public Optional<SkillFailure> preconditions(
            final SkillContext context,
            final NoParameters parameters
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(parameters, "parameters");
        final Optional<CoreSkillFrame> maybeFrame = ownedFrame();
        if (maybeFrame.isEmpty()) {
            return Optional.of(SkillFailure.of(
                    NAME + ".body_unavailable"
            ));
        }
        final CoreSkillFrame frame = maybeFrame.orElseThrow();
        final Optional<SkillFailure> unsafe = safetyFailure(
                context,
                frame
        );
        if (unsafe.isPresent()) {
            return unsafe;
        }
        if (hasStorageEvidence(context, frame)) {
            return Optional.empty();
        }
        if (!hasCraftingTableEvidence(context, frame)) {
            return Optional.of(SkillFailure.of(
                    NAME + ".crafting_table_required"
            ));
        }
        if (ownsChest(frame)
                || potentialPlanks(frame) >= REQUIRED_CHEST_PLANKS) {
            return Optional.empty();
        }
        return materialPreparer.preconditions(
                context,
                NoParameters.INSTANCE
        );
    }

    @Override
    public void start(
            final SkillContext context,
            final NoParameters parameters
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(parameters, "parameters");
        final CoreSkillFrame frame = ownedFrame().orElseThrow(
                () -> new IllegalStateException(
                        "Companion body changed before workstation setup"
                )
        );
        failure = null;
        startedAtTick = context.gameTick();
        phaseStartedAtTick = context.gameTick();
        selectedTarget = null;
        selectedSupport = null;
        expectedChestPosition = null;
        depositSampleSequence = -1;
        depositedItemId = "";
        chestOpenAttempts = 0;
        fixtureMovement = null;
        fixtureMovementParameters = null;
        movementTarget = null;
        fixtureMovementBestDistance = Double.POSITIVE_INFINITY;
        fixtureMovementLastProgressTick = -1L;
        rejectedSupports.clear();
        lastLiveDiagnosticTick = Long.MIN_VALUE;
        if (hasStorageEvidence(context, frame)) {
            beginScan(context, frame, Phase.FIND_CHEST);
        } else if (ownsChest(frame)) {
            phase = Phase.EQUIP_CHEST;
        } else if (potentialPlanks(frame)
                < REQUIRED_CHEST_PLANKS) {
            materialPreparer.start(
                    context,
                    NoParameters.INSTANCE
            );
            phase = Phase.PREPARE_MATERIALS;
        } else {
            beginScan(context, frame, Phase.PREPARE_PLANKS);
        }
    }

    @Override
    public SkillTickResult tick(
            final SkillContext context,
            final NoParameters parameters
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(parameters, "parameters");
        if (!phase.active()) {
            return SkillTickResult.failed(NAME + ".invalid_state");
        }
        try {
            return tickSafely(context);
        } catch (RuntimeException exception) {
            cancelMaterialPreparation(context);
            return fail(NAME + ".internal_failure");
        }
    }

    @Override
    public SkillCheckpoint checkpoint(
            final SkillContext context,
            final NoParameters parameters
    ) {
        return new SkillCheckpoint(
                1,
                String.format(
                        Locale.ROOT,
                        "{\"phase\":\"%s\",\"scanTurns\":%d,"
                                + "\"depositedItem\":\"%s\","
                                + "\"elapsedTicks\":%d,"
                                + "\"preparingMaterials\":%s,"
                                + "\"materialCheckpoint\":%s}",
                        phase.name(),
                        scanTurns,
                        depositedItemId,
                        Math.max(
                                0L,
                                context.gameTick() - startedAtTick
                        ),
                        phase == Phase.PREPARE_MATERIALS,
                        materialCheckpoint(context)
                )
        );
    }

    @Override
    public void cancel(
            final SkillContext context,
            final NoParameters parameters
    ) {
        cancelMaterialPreparation(context);
        cancelFixtureMovement(context);
        if (phase == Phase.RECLAIM_CHEST
                || phase == Phase.START_RECLAIM_CHEST) {
            interactions.abortMining();
        }
        cancelFixtureMovement(null);
        core.stop();
        closeOpenMenuBestEffort();
        phase = Phase.CANCELLED;
    }

    @Override
    public SkillResult result(
            final SkillContext context,
            final NoParameters parameters
    ) {
        return switch (phase) {
            case COMPLETED -> SkillResult.completed();
            case CANCELLED -> SkillResult.cancelled();
            case FAILED -> SkillResult.failed(
                    Objects.requireNonNull(failure)
            );
            default -> SkillResult.failed(SkillFailure.of(
                    NAME + ".invalid_state"
            ));
        };
    }

    private SkillTickResult tickSafely(
            final SkillContext context
    ) {
        if (context.gameTick() - startedAtTick >= MAXIMUM_TICKS) {
            cancelMaterialPreparation(context);
            return fail(NAME + ".timed_out");
        }
        final CoreSkillFrame frame = ownedFrame().orElse(null);
        if (frame == null) {
            cancelMaterialPreparation(context);
            return fail(NAME + ".body_unavailable");
        }
        if (Boolean.getBoolean("mcai.liveModelTest")
                && (lastLiveDiagnosticTick == Long.MIN_VALUE
                    || context.gameTick() - lastLiveDiagnosticTick >= 100L)) {
            lastLiveDiagnosticTick = context.gameTick();
            MinecraftAiCompanion.LOGGER.info(
                    "Live workstation trace: phase={}, tick={}, logs={}, "
                            + "planks={}, chest={}, tableMenu={}, chestMenu={}, "
                            + "visibleFaces={}, position={}",
                    phase,
                    context.gameTick(),
                    frame.inventory().stream()
                            .filter(item -> item.itemId().endsWith("_log"))
                            .mapToInt(InventoryItemSummary::count)
                            .sum(),
                    plankCount(frame),
                    itemCount(frame, CHEST),
                    inventory.hasThreeByThreeCraftingMenu(),
                    currentChestMenu().isPresent(),
                    frame.visibleBlockFaces().size(),
                    frame.position()
            );
        }
        final Optional<SkillFailure> unsafe = safetyFailure(
                context,
                frame
        );
        if (unsafe.isPresent()) {
            cancelMaterialPreparation(context);
            return fail(unsafe.orElseThrow());
        }
        return switch (phase) {
            case PREPARE_MATERIALS ->
                    prepareMaterials(context);
            case PREPARE_PLANKS -> preparePlanks(context, frame);
            case FIND_TABLE -> findTarget(
                    context,
                    frame,
                    Fixture.TABLE
            );
            case MOVE_TO_FIXTURE -> moveToFixture(context, frame);
            case AIM_TABLE -> aimTarget(
                    context,
                    frame,
                    Fixture.TABLE
            );
            case OPEN_TABLE -> openTable(context);
            case CONFIRM_TABLE_MENU ->
                    confirmTableMenu(context);
            case CRAFT_CHEST -> craftChest(context);
            case CONFIRM_CHEST_ITEM ->
                    confirmChestItem(context, frame);
            case CLOSE_TABLE -> closeTable(context);
            case EQUIP_CHEST -> equipChest(context, frame);
            case FIND_SUPPORT -> findSupport(context, frame);
            case AIM_SUPPORT -> aimSupport(context, frame);
            case PLACE_CHEST -> placeChest(context);
            case CONFIRM_CHEST ->
                    confirmChest(context, frame);
            case FIND_CHEST -> findTarget(
                    context,
                    frame,
                    Fixture.CHEST
            );
            case AIM_CHEST -> aimTarget(
                    context,
                    frame,
                    Fixture.CHEST
            );
            case OPEN_CHEST -> openChest(context);
            case CONFIRM_CHEST_MENU ->
                    confirmChestMenu(context, frame);
            case AIM_RECLAIM_CHEST ->
                    aimReclaimChest(context, frame);
            case START_RECLAIM_CHEST ->
                    startReclaimChest(context);
            case RECLAIM_CHEST ->
                    reclaimChest(context, frame);
            case DEPOSIT_SURPLUS -> depositSurplus(context);
            case CLOSE_CHEST -> closeChest(context);
            case FINISH -> complete();
            default -> fail(NAME + ".invalid_state");
        };
    }

    private SkillTickResult prepareMaterials(
            final SkillContext context
    ) {
        final SkillTickResult child = materialPreparer.tick(
                context,
                NoParameters.INSTANCE
        );
        if (child.status() == SkillTickResult.Status.FAILED) {
            cancelMaterialPreparation(context);
            return fail(child.failure().orElseGet(() ->
                    SkillFailure.of(
                            NAME + ".material_preparation_failed"
                    )
            ));
        }
        if (child.status()
                == SkillTickResult.Status.COMPLETED) {
            final CoreSkillFrame frame = ownedFrame().orElse(null);
            if (frame == null) {
                return fail(NAME + ".body_unavailable");
            }
            if (ownsChest(frame)) {
                return transition(context, Phase.EQUIP_CHEST);
            }
            beginScan(context, frame, Phase.PREPARE_PLANKS);
            return SkillTickResult.running(true, true);
        }
        return SkillTickResult.running(
                child.madeProgress(),
                child.safeCheckpoint()
        );
    }

    private SkillTickResult preparePlanks(
            final SkillContext context,
            final CoreSkillFrame frame
    ) {
        if (plankCount(frame) >= REQUIRED_CHEST_PLANKS) {
            beginScan(context, frame, Phase.FIND_TABLE);
            return SkillTickResult.running(true, true);
        }
        if (context.gameTick() < nextScanTick) {
            return SkillTickResult.running(false, true);
        }
        final InventoryOperationResult crafted =
                craftOnePlank(frame);
        if (crafted.succeeded()) {
            nextScanTick = context.gameTick() + 5L;
            phaseStartedAtTick = context.gameTick();
            return SkillTickResult.running(true, true);
        }
        if (context.gameTick() - phaseStartedAtTick
                < MAXIMUM_RECIPE_WAIT_TICKS) {
            return SkillTickResult.running(false, true);
        }
        return fail(NAME + ".plank_recipe_unavailable");
    }

    private SkillTickResult findTarget(
            final SkillContext context,
            final CoreSkillFrame frame,
            final Fixture fixture
    ) {
        if (fixture == Fixture.TABLE
                && inventory.hasThreeByThreeCraftingMenu()) {
            return transition(context, Phase.CRAFT_CHEST);
        }
        final Optional<VisibleBlockFace> crosshair =
                interactionFrames.currentCrosshairBlock()
                        .filter(face -> isFixture(face, fixture))
                        .filter(face -> matchesExpectedFixture(
                                context,
                                frame,
                                face,
                                fixture
                        ));
        if (crosshair.isPresent()) {
            selectedTarget = crosshair.orElseThrow();
            return transition(
                    context,
                    fixture == Fixture.TABLE
                            ? Phase.OPEN_TABLE
                            : Phase.OPEN_CHEST
            );
        }
        final Optional<VisibleBlockFace> visible =
                visibleFixture(context, frame, fixture);
        if (visible.isPresent()) {
            final VisibleBlockFace target = visible.orElseThrow();
            if (target.distance() > RELIABLE_INTERACTION_DISTANCE) {
                return beginFixtureApproach(
                        context,
                        frame,
                        fixture,
                        target.block().x(),
                        target.block().y(),
                        target.block().z()
                );
            }
            selectedTarget = target;
            return transition(
                    context,
                    fixture == Fixture.TABLE
                            ? Phase.AIM_TABLE
                            : Phase.AIM_CHEST
            );
        }
        final Optional<VerifiedFixtureLocation> remembered =
                fixture == Fixture.TABLE
                        ? knownCraftingTable(context, frame)
                        : knownStorage(context, frame);
        if (remembered.isPresent()) {
            final VerifiedFixtureLocation location =
                    remembered.orElseThrow();
            final double distance = center(new BlockCoordinate(
                            location.x(),
                            location.y(),
                            location.z()
                    ))
                    .subtract(frame.position())
                    .length();
            if (distance > APPROACH_RADIUS) {
                return beginFixtureApproach(
                        context,
                        frame,
                        fixture,
                        location.x(),
                        location.y(),
                        location.z()
                );
            }
        }
        return scan(
                context,
                frame,
                fixture == Fixture.TABLE
                        ? Phase.FIND_TABLE
                        : Phase.FIND_CHEST,
                fixture.failureStem()
        );
    }

    /**
     * A remembered fixture is a navigation hint, never an interaction
     * authority. Walk toward it with the normal bounded movement skill; the
     * next phase still requires a fresh first-person ray before opening it.
     */
    private SkillTickResult beginFixtureApproach(
            final SkillContext context,
            final CoreSkillFrame frame,
            final Fixture fixture,
            final int x,
            final int y,
            final int z
    ) {
        cancelFixtureMovement(context);
        fixtureMovementParameters = new MoveToParameters(
                frame.dimension(),
                x + 0.5,
                y + 1.0,
                z + 0.5,
                APPROACH_RADIUS
        );
        fixtureMovement = new MoveToSkill(
                expectedPlayerId,
                core,
                coreFrames
        );
        final Optional<SkillFailure> rejected = fixtureMovement.preconditions(
                context,
                fixtureMovementParameters
        );
        if (rejected.isPresent()) {
            cancelFixtureMovement(context);
            return fail(NAME + "." + fixture.failureStem()
                    + "_approach_rejected");
        }
        fixtureMovement.start(context, fixtureMovementParameters);
        movementTarget = fixture;
        fixtureMovementBestDistance = frame.position()
                .subtract(fixtureMovementParameters.target())
                .length();
        fixtureMovementLastProgressTick = context.gameTick();
        phase = Phase.MOVE_TO_FIXTURE;
        phaseStartedAtTick = context.gameTick();
        return SkillTickResult.running(true, true);
    }

    private SkillTickResult moveToFixture(
            final SkillContext context,
            final CoreSkillFrame frame
    ) {
        if (fixtureMovement == null
                || fixtureMovementParameters == null
                || movementTarget == null) {
            return fail(NAME + ".fixture_approach_binding_missing");
        }
        final double distance = frame.position()
                .subtract(fixtureMovementParameters.target())
                .length();
        if (distance + 0.05 < fixtureMovementBestDistance) {
            fixtureMovementBestDistance = distance;
            fixtureMovementLastProgressTick = context.gameTick();
        }
        if (context.gameTick() - phaseStartedAtTick
                        >= MAXIMUM_FIXTURE_MOVE_TICKS
                || context.gameTick() - fixtureMovementLastProgressTick
                        >= MAXIMUM_FIXTURE_NO_PROGRESS_TICKS) {
            final Fixture failedTarget = movementTarget;
            cancelFixtureMovement(context);
            beginScan(
                    context,
                    frame,
                    failedTarget == Fixture.TABLE
                            ? Phase.FIND_TABLE
                            : Phase.FIND_CHEST
            );
            return SkillTickResult.running(true, true);
        }
        final SkillTickResult result = fixtureMovement.tick(
                context,
                fixtureMovementParameters
        );
        if (result.status() == SkillTickResult.Status.FAILED) {
            final Fixture failedTarget = movementTarget;
            cancelFixtureMovement(context);
            beginScan(
                    context,
                    frame,
                    failedTarget == Fixture.TABLE
                            ? Phase.FIND_TABLE
                            : Phase.FIND_CHEST
            );
            return SkillTickResult.running(true, true);
        }
        if (result.status() == SkillTickResult.Status.COMPLETED) {
            final Fixture arrived = movementTarget;
            cancelFixtureMovement(context);
            beginScan(
                    context,
                    frame,
                    arrived == Fixture.TABLE
                            ? Phase.FIND_TABLE
                            : Phase.FIND_CHEST
            );
            return SkillTickResult.running(true, true);
        }
        return SkillTickResult.running(
                result.madeProgress(),
                result.safeCheckpoint()
        );
    }

    private SkillTickResult aimTarget(
            final SkillContext context,
            final CoreSkillFrame frame,
            final Fixture fixture
    ) {
        final Optional<VisibleBlockFace> crosshair =
                interactionFrames.currentCrosshairBlock()
                        .filter(face -> isFixture(face, fixture))
                        .filter(face -> matchesExpectedFixture(
                                context,
                                frame,
                                face,
                                fixture
                        ));
        if (crosshair.isPresent()) {
            selectedTarget = crosshair.orElseThrow();
            return transition(
                    context,
                    fixture == Fixture.TABLE
                            ? Phase.OPEN_TABLE
                            : Phase.OPEN_CHEST
            );
        }
        if (selectedTarget == null
                || context.gameTick() - phaseStartedAtTick
                        >= MAXIMUM_AIM_TICKS) {
            beginScan(
                    context,
                    frame,
                    fixture == Fixture.TABLE
                            ? Phase.FIND_TABLE
                            : Phase.FIND_CHEST
            );
            return SkillTickResult.running(true, true);
        }
        return aimAt(frame, selectedTarget.hitPosition());
    }

    private SkillTickResult openTable(
            final SkillContext context
    ) {
        if (inventory.hasThreeByThreeCraftingMenu()) {
            return transition(context, Phase.CRAFT_CHEST);
        }
        final Optional<VisibleBlockFace> crosshair =
                interactionFrames.currentCrosshairBlock()
                        .filter(face -> isFixture(
                                face,
                                Fixture.TABLE
                        ));
        if (crosshair.isEmpty()) {
            return transition(context, Phase.FIND_TABLE);
        }
        final ActionOutcome opened = interactions.useOnBlock(
                ActionHand.MAIN_HAND,
                target(crosshair.orElseThrow())
        );
        return opened.accepted()
                ? transition(context, Phase.CONFIRM_TABLE_MENU)
                : fail(NAME + ".table_open_rejected");
    }

    private SkillTickResult confirmTableMenu(
            final SkillContext context
    ) {
        if (inventory.hasThreeByThreeCraftingMenu()) {
            return transition(context, Phase.CRAFT_CHEST);
        }
        if (context.gameTick() - phaseStartedAtTick
                >= MAXIMUM_CONFIRM_TICKS) {
            return fail(NAME + ".table_menu_unconfirmed");
        }
        return SkillTickResult.running(false, true);
    }

    private SkillTickResult craftChest(
            final SkillContext context
    ) {
        final CraftRecipeParameters recipe =
                new CraftRecipeParameters(CHEST, 1);
        final InventoryOperationResult checked =
                inventory.checkCraft(recipe);
        if (checked.succeeded()) {
            final InventoryOperationResult crafted =
                    inventory.craftOnce(recipe);
            if (crafted.succeeded()) {
                return transition(
                        context,
                        Phase.CONFIRM_CHEST_ITEM
                );
            }
        }
        if (context.gameTick() - phaseStartedAtTick
                < MAXIMUM_RECIPE_WAIT_TICKS) {
            return SkillTickResult.running(false, true);
        }
        return fail(NAME + ".chest_recipe_unavailable");
    }

    private SkillTickResult confirmChestItem(
            final SkillContext context,
            final CoreSkillFrame frame
    ) {
        if (itemCount(frame, CHEST) > 0
                || frame.mainHand().itemId().equals(CHEST)
                    && frame.mainHand().count() > 0) {
            return transition(context, Phase.CLOSE_TABLE);
        }
        if (context.gameTick() - phaseStartedAtTick
                >= MAXIMUM_CONFIRM_TICKS) {
            return fail(NAME + ".chest_craft_unconfirmed");
        }
        return SkillTickResult.running(false, true);
    }

    private SkillTickResult closeTable(
            final SkillContext context
    ) {
        if (!inventory.hasThreeByThreeCraftingMenu()) {
            return transition(context, Phase.EQUIP_CHEST);
        }
        final InventoryOperationResult closed =
                inventory.closeThreeByThreeCraftingMenu();
        return closed.succeeded()
                ? transition(context, Phase.EQUIP_CHEST)
                : fail(NAME + ".table_close_failed");
    }

    private SkillTickResult equipChest(
            final SkillContext context,
            final CoreSkillFrame frame
    ) {
        if (frame.mainHand().itemId().equals(CHEST)
                && frame.mainHand().count() > 0) {
            beginScan(context, frame, Phase.FIND_SUPPORT);
            return SkillTickResult.running(true, true);
        }
        if (itemCount(frame, CHEST) <= 0) {
            return fail(NAME + ".chest_item_missing");
        }
        final InventoryOperationResult equipped = inventory.equip(
                new EquipItemParameters(
                        CHEST,
                        EquipmentTarget.MAINHAND
                )
        );
        return equipped.succeeded()
                ? SkillTickResult.running(true, true)
                : fail(NAME + ".chest_equip_failed");
    }

    private SkillTickResult findSupport(
            final SkillContext context,
            final CoreSkillFrame frame
    ) {
        final Optional<VisibleBlockFace> support =
                selectPlacementSupport(frame);
        if (support.isPresent()) {
            selectedSupport = support.orElseThrow();
            return transition(context, Phase.AIM_SUPPORT);
        }
        return scan(
                context,
                frame,
                Phase.FIND_SUPPORT,
                "safe_chest_support"
        );
    }

    private SkillTickResult aimSupport(
            final SkillContext context,
            final CoreSkillFrame frame
    ) {
        final Optional<VisibleBlockFace> crosshair =
                interactionFrames.currentCrosshairBlock();
        if (crosshair.isPresent()
                && selectedSupport != null
                && sameBlockAndFace(
                        crosshair.orElseThrow(),
                        selectedSupport
                )) {
            selectedSupport = crosshair.orElseThrow();
            return transition(context, Phase.PLACE_CHEST);
        }
        if (selectedSupport == null
                || context.gameTick() - phaseStartedAtTick
                        >= MAXIMUM_AIM_TICKS) {
            beginScan(context, frame, Phase.FIND_SUPPORT);
            return SkillTickResult.running(true, true);
        }
        return aimAt(frame, selectedSupport.hitPosition());
    }

    private SkillTickResult placeChest(
            final SkillContext context
    ) {
        final Optional<VisibleBlockFace> crosshair =
                interactionFrames.currentCrosshairBlock();
        if (crosshair.isEmpty()
                || selectedSupport == null
                || !sameBlockAndFace(
                        crosshair.orElseThrow(),
                        selectedSupport
                )) {
            return transition(context, Phase.AIM_SUPPORT);
        }
        final VisibleBlockFace actual = crosshair.orElseThrow();
        final ActionOutcome placed = interactions.useOnBlock(
                ActionHand.MAIN_HAND,
                target(actual)
        );
        if (!placed.accepted()) {
            rejectedSupports.add(actual.block());
            selectedSupport = null;
            return transition(context, Phase.FIND_SUPPORT);
        }
        expectedChestPosition = new BlockCoordinate(
                actual.block().x(),
                actual.block().y() + 1,
                actual.block().z()
        );
        selectedTarget = null;
        return transition(context, Phase.CONFIRM_CHEST);
    }

    private SkillTickResult confirmChest(
            final SkillContext context,
            final CoreSkillFrame frame
    ) {
        final Optional<VisibleBlockFace> chest =
                visibleExpectedChest(frame);
        if (chest.isPresent()) {
            selectedTarget = chest.orElseThrow();
            return transition(context, Phase.AIM_CHEST);
        }
        final Optional<VisibleBlockFace> crosshair =
                interactionFrames.currentCrosshairBlock()
                        .filter(face -> isFixture(
                                face,
                                Fixture.CHEST
                        ))
                        .filter(face -> expectedChestPosition == null
                                || face.block().equals(
                                        expectedChestPosition
                                ));
        if (crosshair.isPresent()) {
            selectedTarget = crosshair.orElseThrow();
            return transition(context, Phase.OPEN_CHEST);
        }
        if (context.gameTick() - phaseStartedAtTick
                >= MAXIMUM_CONFIRM_TICKS) {
            if (itemCount(frame, CHEST) > 0
                    && selectedSupport != null) {
                rejectedSupports.add(selectedSupport.block());
                expectedChestPosition = null;
                selectedSupport = null;
                beginScan(context, frame, Phase.FIND_SUPPORT);
                return SkillTickResult.running(true, true);
            }
            return fail(NAME + ".chest_placement_unconfirmed");
        }
        if (expectedChestPosition != null) {
            return aimAt(
                    frame,
                    center(expectedChestPosition)
            );
        }
        return SkillTickResult.running(false, true);
    }

    private SkillTickResult openChest(
            final SkillContext context
    ) {
        final Optional<VisibleBlockFace> crosshair =
                interactionFrames.currentCrosshairBlock()
                        .filter(face -> isFixture(
                                face,
                                Fixture.CHEST
                        ));
        if (crosshair.isEmpty()) {
            return transition(context, Phase.FIND_CHEST);
        }
        final ActionOutcome opened = interactions.useOnBlock(
                ActionHand.MAIN_HAND,
                target(crosshair.orElseThrow())
        );
        if (opened.accepted()) {
            chestOpenAttempts++;
        }
        return opened.accepted()
                ? transition(context, Phase.CONFIRM_CHEST_MENU)
                : fail(NAME + ".chest_open_rejected");
    }

    private SkillTickResult confirmChestMenu(
            final SkillContext context,
            final CoreSkillFrame frame
    ) {
        if (currentChestMenu().isPresent()) {
            return transition(context, Phase.DEPOSIT_SURPLUS);
        }
        if (context.gameTick() - phaseStartedAtTick
                >= MAXIMUM_CONFIRM_TICKS) {
            /*
             * Dispatch is not proof that the chest opened. A newly placed
             * chest can still be blocked by a full block above it, or an
             * otherwise legal interaction can transiently fail. Retry once;
             * if this skill placed the still-unopenable empty chest, recover
             * that exact block through ordinary mining and choose another
             * observed-clear support. This preserves the crafted item instead
             * of demanding eight new planks after a real placement.
             */
            if (expectedChestPosition != null
                    && (knownChestOpeningObstruction(
                            frame,
                            expectedChestPosition
                        )
                        || chestOpenAttempts
                            >= MAXIMUM_CHEST_OPEN_ATTEMPTS)) {
                selectedTarget = visibleExpectedChest(frame)
                        .orElse(selectedTarget);
                return transition(
                        context,
                        Phase.AIM_RECLAIM_CHEST
                );
            }
            if (chestOpenAttempts
                    < MAXIMUM_CHEST_OPEN_ATTEMPTS) {
                selectedTarget = visibleExpectedChest(frame)
                        .orElse(selectedTarget);
                return transition(context, Phase.AIM_CHEST);
            }
            return fail(NAME + ".chest_menu_unconfirmed");
        }
        return SkillTickResult.running(false, true);
    }

    private SkillTickResult aimReclaimChest(
            final SkillContext context,
            final CoreSkillFrame frame
    ) {
        if (expectedChestPosition == null) {
            return fail(NAME + ".chest_reclaim_target_missing");
        }
        if (ownsChest(frame)) {
            rejectPreviousSupport();
            return transition(context, Phase.EQUIP_CHEST);
        }
        final Optional<VisibleBlockFace> crosshair =
                interactionFrames.currentCrosshairBlock()
                        .filter(face -> isFixture(
                                face,
                                Fixture.CHEST
                        ))
                        .filter(face -> face.block().equals(
                                expectedChestPosition
                        ));
        if (crosshair.isPresent()) {
            selectedTarget = crosshair.orElseThrow();
            return transition(
                    context,
                    Phase.START_RECLAIM_CHEST
            );
        }
        final Optional<VisibleBlockFace> visible =
                visibleExpectedChest(frame);
        if (visible.isPresent()) {
            selectedTarget = visible.orElseThrow();
        }
        if (selectedTarget == null
                || context.gameTick() - phaseStartedAtTick
                        >= MAXIMUM_AIM_TICKS) {
            return fail(NAME + ".chest_reclaim_target_not_visible");
        }
        return aimAt(frame, selectedTarget.hitPosition());
    }

    private SkillTickResult startReclaimChest(
            final SkillContext context
    ) {
        if (expectedChestPosition == null) {
            return fail(NAME + ".chest_reclaim_target_missing");
        }
        final Optional<VisibleBlockFace> crosshair =
                interactionFrames.currentCrosshairBlock()
                        .filter(face -> isFixture(
                                face,
                                Fixture.CHEST
                        ))
                        .filter(face -> face.block().equals(
                                expectedChestPosition
                        ));
        if (crosshair.isEmpty()) {
            return transition(
                    context,
                    Phase.AIM_RECLAIM_CHEST
            );
        }
        final ActionOutcome started = interactions.beginMining(
                target(crosshair.orElseThrow())
        );
        return started.accepted()
                ? transition(context, Phase.RECLAIM_CHEST)
                : fail(NAME + ".chest_reclaim_start_rejected");
    }

    private SkillTickResult reclaimChest(
            final SkillContext context,
            final CoreSkillFrame frame
    ) {
        if (ownsChest(frame)) {
            interactions.abortMining();
            rejectPreviousSupport();
            return transition(context, Phase.EQUIP_CHEST);
        }
        if (context.gameTick() - phaseStartedAtTick
                >= MAXIMUM_RECLAIM_TICKS) {
            interactions.abortMining();
            return fail(NAME + ".chest_reclaim_unconfirmed");
        }
        final ActionOutcome continued =
                interactions.continueMining();
        if (continued.accepted()
                || continued == ActionOutcome.NO_ACTIVE_ACTION) {
            /*
             * NO_ACTIVE_ACTION is normal between vanilla block-break
             * completion and the next semantic inventory sample/pickup.
             */
            return SkillTickResult.running(
                    continued.accepted(),
                    true
            );
        }
        return fail(NAME + ".chest_reclaim_failed");
    }

    private SkillTickResult depositSurplus(
            final SkillContext context
    ) {
        final Optional<MenuSkillFrame> maybeMenu =
                currentChestMenu();
        if (maybeMenu.isEmpty()) {
            return fail(NAME + ".chest_menu_lost");
        }
        final MenuSkillFrame frame = maybeMenu.orElseThrow();
        final Optional<MenuSlotSummary> source =
                selectSurplusSource(frame);
        final Optional<MenuSlotSummary> destination =
                frame.menu().slots().stream()
                        .filter(slot -> !slot.playerInventory())
                        .filter(slot -> slot.count() == 0)
                        .min(Comparator.comparingInt(
                                MenuSlotSummary::slot
                        ));
        if (source.isEmpty()) {
            return fail(NAME + ".no_surplus_supply");
        }
        if (destination.isEmpty()) {
            return fail(NAME + ".storage_full");
        }
        final MenuBinding binding = binding(frame);
        final TransferMenuItemParameters transfer =
                new TransferMenuItemParameters(
                        binding,
                        source.orElseThrow().slot(),
                        destination.orElseThrow().slot(),
                        1
                );
        final MenuOperationResult moved = menus.transfer(transfer);
        if (!moved.succeeded()) {
            if (context.gameTick() - phaseStartedAtTick
                    < MAXIMUM_CONFIRM_TICKS) {
                return SkillTickResult.running(false, true);
            }
            return fail(NAME + ".storage_deposit_rejected");
        }
        depositedItemId = source.orElseThrow().itemId();
        depositSampleSequence = frame.sampleSequence();
        return transition(context, Phase.CLOSE_CHEST);
    }

    private SkillTickResult closeChest(
            final SkillContext context
    ) {
        final Optional<MenuSkillFrame> maybeMenu =
                currentChestMenu();
        if (maybeMenu.isEmpty()) {
            return transition(context, Phase.FINISH);
        }
        final MenuSkillFrame frame = maybeMenu.orElseThrow();
        final MenuBinding binding = binding(frame);
        if (frame.sampleSequence() <= depositSampleSequence
                || !menus.checkBinding(binding).succeeded()) {
            if (context.gameTick() - phaseStartedAtTick
                    >= MAXIMUM_CONFIRM_TICKS) {
                return fail(NAME + ".deposit_observation_unconfirmed");
            }
            return SkillTickResult.running(false, true);
        }
        final MenuOperationResult closed = menus.close(
                new CloseMenuParameters(binding)
        );
        return closed.succeeded()
                ? transition(context, Phase.FINISH)
                : fail(NAME + ".chest_close_failed");
    }

    private SkillTickResult scan(
            final SkillContext context,
            final CoreSkillFrame frame,
            final Phase samePhase,
            final String failureStem
    ) {
        if (scanTurns >= MAXIMUM_SCAN_TURNS) {
            return fail(NAME + "." + failureStem + "_not_visible");
        }
        if (context.gameTick() < nextScanTick) {
            return SkillTickResult.running(false, true);
        }
        final int yawIndex = scanTurns / SCAN_PITCHES.length;
        final int pitchIndex = scanTurns % SCAN_PITCHES.length;
        final float yaw = scanBaseYaw
                + SCAN_YAW_OFFSETS[
                        yawIndex % SCAN_YAW_OFFSETS.length
                ];
        final float pitch = SCAN_PITCHES[pitchIndex];
        if (!core.stop().accepted()
                || !core.look(
                        new LookIntent(yaw, pitch)
                ).accepted()) {
            return fail(NAME + ".scan_rejected");
        }
        scanTurns++;
        nextScanTick =
                context.gameTick() + SCAN_INTERVAL_TICKS;
        phase = samePhase;
        return SkillTickResult.running(true, true);
    }

    private SkillTickResult aimAt(
            final CoreSkillFrame frame,
            final PerceptionVec3 target
    ) {
        final PerceptionVec3 delta = target.subtract(
                frame.eyePosition()
        );
        if (delta.lengthSquared() <= 1.0E-12) {
            return SkillTickResult.running(false, true);
        }
        final float yaw = (float) Math.toDegrees(
                Math.atan2(-delta.x(), delta.z())
        );
        final float pitch = (float) Math.toDegrees(Math.atan2(
                -delta.y(),
                Math.hypot(delta.x(), delta.z())
        ));
        if (!core.stop().accepted()
                || !core.look(new LookIntent(yaw, pitch)).accepted()) {
            return fail(NAME + ".aim_rejected");
        }
        return SkillTickResult.running(true, true);
    }

    private SkillTickResult transition(
            final SkillContext context,
            final Phase next
    ) {
        phase = next;
        phaseStartedAtTick = context.gameTick();
        return SkillTickResult.running(true, true);
    }

    private void beginScan(
            final SkillContext context,
            final CoreSkillFrame frame,
            final Phase next
    ) {
        phase = next;
        phaseStartedAtTick = context.gameTick();
        nextScanTick = context.gameTick();
        scanTurns = 0;
        scanBaseYaw = yaw(frame.lookDirection());
        selectedTarget = null;
        selectedSupport = null;
    }

    private SkillTickResult complete() {
        core.stop();
        phase = Phase.COMPLETED;
        return SkillTickResult.completed();
    }

    private SkillTickResult fail(final String code) {
        return fail(SkillFailure.of(code));
    }

    private SkillTickResult fail(
            final SkillFailure rejected
    ) {
        if (phase == Phase.RECLAIM_CHEST
                || phase == Phase.START_RECLAIM_CHEST) {
            interactions.abortMining();
        }
        core.stop();
        closeOpenMenuBestEffort();
        failure = rejected;
        phase = Phase.FAILED;
        return SkillTickResult.failed(rejected);
    }

    private String materialCheckpoint(
            final SkillContext context
    ) {
        if (phase != Phase.PREPARE_MATERIALS) {
            return "{}";
        }
        return materialPreparer.checkpoint(
                context,
                NoParameters.INSTANCE
        ).payload();
    }

    private void cancelMaterialPreparation(
            final SkillContext context
    ) {
        if (phase == Phase.PREPARE_MATERIALS) {
            materialPreparer.cancel(
                    context,
                    NoParameters.INSTANCE
            );
        }
    }

    private void cancelFixtureMovement(
            final SkillContext context
    ) {
        if (fixtureMovement != null
                && fixtureMovementParameters != null
                && context != null) {
            fixtureMovement.cancel(context, fixtureMovementParameters);
        }
        fixtureMovement = null;
        fixtureMovementParameters = null;
        movementTarget = null;
        fixtureMovementBestDistance = Double.POSITIVE_INFINITY;
        fixtureMovementLastProgressTick = -1L;
    }

    private void closeOpenMenuBestEffort() {
        if (inventory.hasThreeByThreeCraftingMenu()) {
            inventory.closeThreeByThreeCraftingMenu();
            return;
        }
        currentChestMenu().ifPresent(frame ->
                menus.close(new CloseMenuParameters(binding(frame)))
        );
    }

    private Optional<CoreSkillFrame> ownedFrame() {
        return coreFrames.current().filter(frame ->
                expectedPlayerId.equals(frame.playerId())
        );
    }

    private Optional<MenuSkillFrame> currentChestMenu() {
        return menuFrames.current()
                .filter(frame -> expectedPlayerId.equals(
                        frame.playerId()
                ))
                .filter(frame -> {
                    final String type = frame.menu().menuType();
                    final String clazz = frame.menu().menuClass();
                    return type.contains("generic_9x")
                            || clazz.contains("ChestMenu");
                });
    }

    private Optional<VisibleBlockFace> visibleFixture(
            final SkillContext context,
            final CoreSkillFrame frame,
            final Fixture fixture
    ) {
        final Optional<VerifiedFixtureLocation> remembered =
                fixture == Fixture.TABLE
                        ? knownCraftingTable(context, frame)
                        : knownStorage(context, frame);
        return frame.visibleBlockFaces().stream()
                .filter(face -> isFixture(face, fixture))
                .filter(face -> remembered.isEmpty()
                        || matches(
                                face.block(),
                                remembered.orElseThrow()
                        ))
                .min(Comparator.comparingDouble(
                        VisibleBlockFace::distance
                ));
    }

    private boolean matchesExpectedFixture(
            final SkillContext context,
            final CoreSkillFrame frame,
            final VisibleBlockFace face,
            final Fixture fixture
    ) {
        if (fixture == Fixture.CHEST
                && expectedChestPosition != null) {
            return face.block().equals(expectedChestPosition);
        }
        final Optional<VerifiedFixtureLocation> remembered =
                fixture == Fixture.TABLE
                        ? knownCraftingTable(context, frame)
                        : knownStorage(context, frame);
        return remembered.isEmpty()
                || matches(
                        face.block(),
                        remembered.orElseThrow()
                );
    }

    private Optional<VisibleBlockFace> visibleExpectedChest(
            final CoreSkillFrame frame
    ) {
        return frame.visibleBlockFaces().stream()
                .filter(face -> isFixture(face, Fixture.CHEST))
                .filter(face -> expectedChestPosition == null
                        || face.block().equals(
                                expectedChestPosition
                        ))
                .min(Comparator.comparingDouble(
                        VisibleBlockFace::distance
                ));
    }

    private Optional<VisibleBlockFace> selectPlacementSupport(
            final CoreSkillFrame frame
    ) {
        return frame.visibleBlockFaces().stream()
                .filter(face -> face.face().equals("up"))
                .filter(face ->
                        !rejectedSupports.contains(face.block()))
                .filter(face -> face.topSupportAffordance()
                        == TopSupportAffordance.STURDY_FULL_TOP)
                .filter(face ->
                        !face.blockTypeId().equals(CRAFTING_TABLE)
                                && !face.blockTypeId()
                                    .equals("minecraft:furnace")
                                && !isFixture(
                                    face,
                                    Fixture.CHEST
                                ))
                .filter(face ->
                        face.distance() >= MINIMUM_PLACEMENT_DISTANCE
                                && face.distance()
                                    <= MAXIMUM_PLACEMENT_DISTANCE)
                .filter(face ->
                        hasObservedChestPlacementClearance(
                                frame,
                                face
                        ))
                .filter(face -> {
                    final double centerX = face.block().x() + 0.5;
                    final double centerZ = face.block().z() + 0.5;
                    return Math.hypot(
                            centerX - frame.position().x(),
                            centerZ - frame.position().z()
                    ) >= MINIMUM_PLACEMENT_DISTANCE;
                })
                .min(Comparator.comparingDouble(
                        VisibleBlockFace::distance
                ));
    }

    static boolean hasObservedChestPlacementClearance(
            final CoreSkillFrame frame,
            final VisibleBlockFace support
    ) {
        Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(support, "support");
        final GridPos chest = new GridPos(
                support.block().x(),
                support.block().y() + 1,
                support.block().z()
        );
        return observedAirClearance(frame, chest)
                && observedAirClearance(frame, chest.above());
    }

    private static boolean observedAirClearance(
            final CoreSkillFrame frame,
            final GridPos position
    ) {
        return frame.navigation()
                .voxelAt(position)
                .filter(voxel -> voxel.kind() == VoxelKind.AIR)
                .filter(NavigationEvidence::hasTraversalClearance)
                .isPresent();
    }

    private static boolean knownChestOpeningObstruction(
            final CoreSkillFrame frame,
            final BlockCoordinate chest
    ) {
        final GridPos above = new GridPos(
                chest.x(),
                chest.y() + 1,
                chest.z()
        );
        return frame.navigation()
                .voxelAt(above)
                .filter(voxel -> !voxel.kind().isPassable())
                .isPresent();
    }

    private boolean hasStorageEvidence(
            final SkillContext context,
            final CoreSkillFrame frame
    ) {
        return knownStorage(context, frame).isPresent()
                || hasVisibleStorage(frame);
    }

    static boolean hasVisibleStorage(
            final CoreSkillFrame frame
    ) {
        Objects.requireNonNull(frame, "frame");
        return frame.visibleBlockFaces().stream()
                .anyMatch(face ->
                        isFixture(face, Fixture.CHEST));
    }

    private static boolean ownsChest(
            final CoreSkillFrame frame
    ) {
        return itemCount(frame, CHEST) > 0
                || frame.mainHand().itemId().equals(CHEST)
                    && frame.mainHand().count() > 0;
    }

    private void rejectPreviousSupport() {
        if (selectedSupport != null) {
            rejectedSupports.add(selectedSupport.block());
        }
        expectedChestPosition = null;
        selectedSupport = null;
        selectedTarget = null;
        chestOpenAttempts = 0;
    }

    private Optional<MenuSlotSummary> selectSurplusSource(
            final MenuSkillFrame frame
    ) {
        for (String itemId : SURPLUS_PRIORITY) {
            final Optional<MenuSlotSummary> priority =
                    frame.menu().slots().stream()
                            .filter(MenuSlotSummary::playerInventory)
                            .filter(MenuSlotSummary::mayPickup)
                            .filter(slot -> slot.count() > 0)
                            .filter(slot -> slot.itemId().equals(itemId))
                            .min(Comparator.comparingInt(
                                    MenuSlotSummary::slot
                            ));
            if (priority.isPresent()) {
                return priority;
            }
        }
        return frame.menu().slots().stream()
                .filter(MenuSlotSummary::playerInventory)
                .filter(MenuSlotSummary::mayPickup)
                .filter(slot -> isSafeSurplus(
                        slot.itemId(),
                        slot.count()
                ))
                .min(Comparator
                        .comparingInt(MenuSlotSummary::count)
                        .reversed()
                        .thenComparingInt(MenuSlotSummary::slot));
    }

    private static boolean isSafeSurplus(
            final String itemId,
            final int count
    ) {
        if (count < 2) {
            return false;
        }
        return itemId.equals("minecraft:cobblestone")
                || itemId.equals("minecraft:dirt")
                || itemId.endsWith("_log")
                || itemId.endsWith("_planks");
    }

    private InventoryOperationResult craftOnePlank(
            final CoreSkillFrame frame
    ) {
        for (InventoryItemSummary item : frame.inventory().stream()
                .sorted(Comparator.comparing(
                        InventoryItemSummary::itemId
                ))
                .toList()) {
            final Optional<String> recipe =
                    PrepareBasicCraftingSkill.plankRecipeFor(
                            item.itemId()
                    );
            if (recipe.isEmpty()) {
                continue;
            }
            final CraftRecipeParameters parameters =
                    new CraftRecipeParameters(
                            recipe.orElseThrow(),
                            1
                    );
            if (inventory.checkCraft(parameters).succeeded()) {
                return inventory.craftOnce(parameters);
            }
        }
        return InventoryOperationResult.rejected(
                NAME + ".plank_recipe_unavailable"
        );
    }

    private Optional<VerifiedFixtureLocation> knownCraftingTable(
            final SkillContext context,
            final CoreSkillFrame frame
    ) {
        return knownFixture(
                knownCraftingTable,
                context,
                frame
        );
    }

    private Optional<VerifiedFixtureLocation> knownStorage(
            final SkillContext context,
            final CoreSkillFrame frame
    ) {
        return knownFixture(knownStorage, context, frame);
    }

    private static Optional<VerifiedFixtureLocation> knownFixture(
            final LongFunction<Optional<VerifiedFixtureLocation>> source,
            final SkillContext context,
            final CoreSkillFrame frame
    ) {
        try {
            return Objects.requireNonNull(
                    source.apply(context.goalRevision()),
                    "known fixture result"
            ).filter(location -> location.dimension().equals(
                    frame.dimension().id()
            ));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private boolean hasCraftingTableEvidence(
            final SkillContext context,
            final CoreSkillFrame frame
    ) {
        return inventory.hasThreeByThreeCraftingMenu()
                || frame.visibleBlockFaces().stream().anyMatch(
                        face -> isFixture(face, Fixture.TABLE)
                )
                || knownCraftingTable(context, frame).isPresent();
    }

    private static Optional<SkillFailure> safetyFailure(
            final SkillContext context,
            final CoreSkillFrame frame
    ) {
        if (frame.inWater()) {
            return Optional.of(SkillFailure.of(
                    NAME + ".unsafe_pose"
            ));
        }
        final double maximumDanger =
                context.hardcore() ? 0.08 : 0.25;
        if (Math.max(context.riskScore(), frame.danger())
                > maximumDanger) {
            return Optional.of(SkillFailure.of(
                    NAME + ".danger_detected"
            ));
        }
        if (frame.health() / frame.maxHealth()
                < (context.hardcore() ? 0.75F : 0.40F)) {
            return Optional.of(SkillFailure.of(
                    NAME + ".health_too_low"
            ));
        }
        return Optional.empty();
    }

    private static int potentialPlanks(
            final CoreSkillFrame frame
    ) {
        return plankCount(frame)
                + frame.inventory().stream()
                        .filter(item ->
                                PrepareBasicCraftingSkill
                                    .plankRecipeFor(
                                            item.itemId()
                                    )
                                    .isPresent()
                        )
                        .mapToInt(InventoryItemSummary::count)
                        .sum() * 4;
    }

    private static int plankCount(
            final CoreSkillFrame frame
    ) {
        return frame.inventory().stream()
                .filter(item -> item.itemId().endsWith("_planks"))
                .mapToInt(InventoryItemSummary::count)
                .sum();
    }

    private static int itemCount(
            final CoreSkillFrame frame,
            final String itemId
    ) {
        return frame.inventory().stream()
                .filter(item -> item.itemId().equals(itemId))
                .mapToInt(InventoryItemSummary::count)
                .sum();
    }

    private static boolean isFixture(
            final VisibleBlockFace face,
            final Fixture fixture
    ) {
        return switch (fixture) {
            case TABLE -> face.blockTypeId().equals(CRAFTING_TABLE);
            case CHEST -> face.blockTypeId().equals(CHEST)
                    || face.blockTypeId().equals(
                            "minecraft:trapped_chest"
                    );
        };
    }

    private static boolean matches(
            final BlockCoordinate position,
            final VerifiedFixtureLocation location
    ) {
        return position.x() == location.x()
                && position.y() == location.y()
                && position.z() == location.z();
    }

    private static boolean sameBlockAndFace(
            final VisibleBlockFace first,
            final VisibleBlockFace second
    ) {
        return first.block().equals(second.block())
                && first.face().equals(second.face());
    }

    private static BlockInteractionTarget target(
            final VisibleBlockFace visible
    ) {
        return new BlockInteractionTarget(
                visible.block().x(),
                visible.block().y(),
                visible.block().z(),
                BlockFace.valueOf(
                        visible.face().toUpperCase(Locale.ROOT)
                ),
                new ActionVec3(
                        visible.hitPosition().x(),
                        visible.hitPosition().y(),
                        visible.hitPosition().z()
                )
        );
    }

    private static PerceptionVec3 center(
            final BlockCoordinate position
    ) {
        return new PerceptionVec3(
                position.x() + 0.5,
                position.y() + 0.5,
                position.z() + 0.5
        );
    }

    private static MenuBinding binding(
            final MenuSkillFrame frame
    ) {
        return new MenuBinding(
                frame.sampleSequence(),
                frame.menu().containerId(),
                frame.menu().stateId()
        );
    }

    private static float yaw(
            final PerceptionVec3 direction
    ) {
        return (float) Math.toDegrees(
                Math.atan2(-direction.x(), direction.z())
        );
    }

    private static SkillParameterResult<NoParameters> parseNone(
            final List<SkillArgument> arguments
    ) {
        return arguments != null && arguments.isEmpty()
                ? SkillParameterResult.valid(NoParameters.INSTANCE)
                : SkillParameterResult.invalid(
                        NAME + ".invalid_arguments"
                );
    }

    private enum Fixture {
        TABLE("table"),
        CHEST("chest");

        private final String failureStem;

        Fixture(final String failureStem) {
            this.failureStem = failureStem;
        }

        private String failureStem() {
            return failureStem;
        }
    }

    private enum Phase {
        IDLE,
        PREPARE_MATERIALS,
        PREPARE_PLANKS,
        FIND_TABLE,
        MOVE_TO_FIXTURE,
        AIM_TABLE,
        OPEN_TABLE,
        CONFIRM_TABLE_MENU,
        CRAFT_CHEST,
        CONFIRM_CHEST_ITEM,
        CLOSE_TABLE,
        EQUIP_CHEST,
        FIND_SUPPORT,
        AIM_SUPPORT,
        PLACE_CHEST,
        CONFIRM_CHEST,
        FIND_CHEST,
        AIM_CHEST,
        OPEN_CHEST,
        CONFIRM_CHEST_MENU,
        AIM_RECLAIM_CHEST,
        START_RECLAIM_CHEST,
        RECLAIM_CHEST,
        DEPOSIT_SURPLUS,
        CLOSE_CHEST,
        FINISH,
        COMPLETED,
        FAILED,
        CANCELLED;

        private boolean active() {
            return this != IDLE
                    && this != COMPLETED
                    && this != FAILED
                    && this != CANCELLED;
        }
    }
}
