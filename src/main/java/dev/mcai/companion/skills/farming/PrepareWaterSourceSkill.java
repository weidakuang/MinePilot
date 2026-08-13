package dev.mcai.companion.skills.farming;

import dev.mcai.companion.action.ActionOutcome;
import dev.mcai.companion.action.BlockFace;
import dev.mcai.companion.action.LookIntent;
import dev.mcai.companion.perception.InventoryItemSummary;
import dev.mcai.companion.perception.PerceptionVec3;
import dev.mcai.companion.perception.TopSupportAffordance;
import dev.mcai.companion.perception.VisibleBlockFace;
import dev.mcai.companion.skill.Skill;
import dev.mcai.companion.skill.SkillCheckpoint;
import dev.mcai.companion.skill.SkillContext;
import dev.mcai.companion.skill.SkillFailure;
import dev.mcai.companion.skill.SkillParameterParser;
import dev.mcai.companion.skill.SkillResult;
import dev.mcai.companion.skill.SkillTickResult;
import dev.mcai.companion.skills.core.CoreSkillActuator;
import dev.mcai.companion.skills.core.CoreSkillFrame;
import dev.mcai.companion.skills.core.CoreSkillFrameSource;
import dev.mcai.companion.skills.interaction.BreakBlockParameters;
import dev.mcai.companion.skills.interaction.BreakBlockSkill;
import dev.mcai.companion.skills.interaction.InteractionSkillActuator;
import dev.mcai.companion.skills.interaction.InteractionSkillFrame;
import dev.mcai.companion.skills.interaction.InteractionSkillFrameSource;
import dev.mcai.companion.skills.interaction.InteractionSkillPolicy;
import dev.mcai.companion.skills.interaction.ObservedBlockTarget;
import dev.mcai.companion.skills.interaction.UseItemParameters;
import dev.mcai.companion.skills.interaction.UseItemSkill;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

/**
 * Excavates one observed tillable surface and fills the resulting one-block
 * hole from an owned water bucket. Mining and placement are delegated to the
 * same ordinary interaction skills available to the model; completion needs
 * a later first-person water observation and the expected bucket transition.
 */
public final class PrepareWaterSourceSkill
        implements Skill<PrepareWaterSourceParameters> {
    public static final String NAME = "prepare_water_source";
    private static final double AIM_ALIGNMENT_DEGREES = 0.5;
    private static final double LOOK_EPSILON = 1.0E-12;
    private static final Set<String> EXCAVATABLE = Set.of(
            "minecraft:dirt",
            "minecraft:grass_block",
            "minecraft:farmland"
    );
    private static final List<String> SHOVEL_PREFERENCE = List.of(
            "minecraft:netherite_shovel",
            "minecraft:diamond_shovel",
            "minecraft:iron_shovel",
            "minecraft:stone_shovel",
            "minecraft:wooden_shovel",
            "minecraft:golden_shovel"
    );

    private final UUID expectedPlayerId;
    private final CoreSkillActuator coreActuator;
    private final CoreSkillFrameSource coreFrames;
    private final InteractionSkillActuator actuator;
    private final InteractionSkillFrameSource frames;
    private final FarmingSkillPolicy policy;
    private final InteractionSkillPolicy interactionPolicy;

    private Phase phase = Phase.IDLE;
    private SkillFailure failure;
    private long boundSessionGeneration = -1;
    private long startedAtGameTick = -1;
    private long placedAtGameTick = -1;
    private long placementObservationRevision = -1;
    private int waterBucketsBefore = -1;
    private int emptyBucketsBefore = -1;
    private BreakBlockSkill breakSkill;
    private BreakBlockParameters breakParameters;
    private UseItemSkill useSkill;
    private UseItemParameters useParameters;

    public PrepareWaterSourceSkill(
            final UUID expectedPlayerId,
            final CoreSkillActuator coreActuator,
            final CoreSkillFrameSource coreFrames,
            final InteractionSkillActuator actuator,
            final InteractionSkillFrameSource frames,
            final FarmingSkillPolicy policy
    ) {
        this(
                expectedPlayerId,
                coreActuator,
                coreFrames,
                actuator,
                frames,
                policy,
                InteractionSkillPolicy.defaults()
        );
    }

    PrepareWaterSourceSkill(
            final UUID expectedPlayerId,
            final CoreSkillActuator coreActuator,
            final CoreSkillFrameSource coreFrames,
            final InteractionSkillActuator actuator,
            final InteractionSkillFrameSource frames,
            final FarmingSkillPolicy policy,
            final InteractionSkillPolicy interactionPolicy
    ) {
        this.expectedPlayerId = Objects.requireNonNull(
                expectedPlayerId,
                "expectedPlayerId"
        );
        this.coreActuator = Objects.requireNonNull(
                coreActuator,
                "coreActuator"
        );
        this.coreFrames = Objects.requireNonNull(
                coreFrames,
                "coreFrames"
        );
        this.actuator = Objects.requireNonNull(actuator, "actuator");
        this.frames = Objects.requireNonNull(frames, "frames");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.interactionPolicy = Objects.requireNonNull(
                interactionPolicy,
                "interactionPolicy"
        );
    }

    @Override
    public SkillParameterParser<PrepareWaterSourceParameters>
            parameters() {
        return FarmingSkillParameters::parsePrepareWaterSource;
    }

    @Override
    public Optional<SkillFailure> preconditions(
            final SkillContext context,
            final PrepareWaterSourceParameters parameters
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(parameters, "parameters");
        if (parameters.ground().face() != BlockFace.UP) {
            return Optional.of(failure("ground_face_not_up"));
        }
        if (parameters.dimension()
                .equals(dev.mcai.companion.waypoint.DimensionRef.NETHER)) {
            return Optional.of(failure("water_invalid_dimension"));
        }
        final FrameValidation validation = validateFrame(
                parameters,
                -1
        );
        if (validation.failure().isPresent()) {
            return validation.failure();
        }
        final InteractionSkillFrame frame =
                validation.frame().orElseThrow();
        final Optional<VisibleBlockFace> ground = resolveInitialGround(
                frame,
                parameters
        );
        if (ground.isEmpty()) {
            return Optional.of(failure("ground_not_observed_tillable"));
        }
        if (itemCount(frame.inventory(), "minecraft:water_bucket") < 1) {
            return Optional.of(failure("water_bucket_unavailable"));
        }
        final CoreFrameValidation core = validateCoreFrame(parameters);
        if (core.failure().isPresent()) {
            return core.failure();
        }
        if (core.frame().orElseThrow().feet().below().equals(
                new dev.mcai.companion.navigation.GridPos(
                        parameters.ground().x(),
                        parameters.ground().y(),
                        parameters.ground().z()
                )
        )) {
            return Optional.of(failure("current_support_target"));
        }
        return Optional.empty();
    }

    @Override
    public void start(
            final SkillContext context,
            final PrepareWaterSourceParameters parameters
    ) {
        final Optional<SkillFailure> invalid = preconditions(
                context,
                parameters
        );
        if (invalid.isPresent()) {
            throw new IllegalStateException(
                    "Water-source binding changed before start"
            );
        }
        final InteractionSkillFrame frame = frames.current()
                .orElseThrow();
        phase = Phase.AIMING_GROUND;
        failure = null;
        boundSessionGeneration = frame.sessionGeneration();
        startedAtGameTick = context.gameTick();
        placedAtGameTick = -1;
        placementObservationRevision = -1;
        waterBucketsBefore = itemCount(
                frame.inventory(),
                "minecraft:water_bucket"
        );
        emptyBucketsBefore = itemCount(
                frame.inventory(),
                "minecraft:bucket"
        );
        breakSkill = null;
        breakParameters = null;
        useSkill = null;
        useParameters = null;
    }

    @Override
    public SkillTickResult tick(
            final SkillContext context,
            final PrepareWaterSourceParameters parameters
    ) {
        if (phase == Phase.COMPLETED) {
            return SkillTickResult.completed();
        }
        if (phase == Phase.FAILED) {
            return SkillTickResult.failed(Objects.requireNonNull(failure));
        }
        if (phase == Phase.IDLE || phase == Phase.CANCELLED) {
            return SkillTickResult.failed(NAME + ".invalid_state");
        }
        if (context.gameTick() - startedAtGameTick
                >= policy.totalTimeoutTicks()) {
            return fail(context, parameters, "timed_out_"
                    + phase.name().toLowerCase(Locale.ROOT));
        }
        final FrameValidation frameValidation = validateFrame(
                parameters,
                boundSessionGeneration
        );
        if (frameValidation.failure().isPresent()) {
            return fail(
                    context,
                    parameters,
                    frameValidation.failure().orElseThrow()
            );
        }
        final CoreFrameValidation coreValidation =
                validateCoreFrame(parameters);
        if (coreValidation.failure().isPresent()) {
            return fail(
                    context,
                    parameters,
                    coreValidation.failure().orElseThrow()
            );
        }
        final InteractionSkillFrame frame =
                frameValidation.frame().orElseThrow();
        final CoreSkillFrame core = coreValidation.frame().orElseThrow();
        return switch (phase) {
            case AIMING_GROUND -> aimAndStartBreak(
                    context,
                    parameters,
                    core,
                    frame
            );
            case BREAKING -> tickBreak(context, parameters);
            case REVEALING_BOTTOM -> revealBottom(
                    context,
                    parameters,
                    core,
                    frame
            );
            case PLACING -> tickPlacement(context, parameters, frame);
            case VERIFYING -> verifyWater(
                    context,
                    parameters,
                    core,
                    frame
            );
            default -> SkillTickResult.failed(NAME + ".invalid_state");
        };
    }

    @Override
    public SkillCheckpoint checkpoint(
            final SkillContext context,
            final PrepareWaterSourceParameters parameters
    ) {
        return new SkillCheckpoint(
                1,
                String.format(
                        Locale.ROOT,
                        "{\"phase\":\"%s\",\"dimension\":\"%s\","
                                + "\"x\":%d,\"y\":%d,\"z\":%d,"
                                + "\"session\":%d}",
                        phase.name(),
                        parameters.dimension().id(),
                        parameters.ground().x(),
                        parameters.ground().y(),
                        parameters.ground().z(),
                        boundSessionGeneration
                )
        );
    }

    @Override
    public void cancel(
            final SkillContext context,
            final PrepareWaterSourceParameters parameters
    ) {
        cancelChildren(context);
        coreActuator.stop();
        phase = Phase.CANCELLED;
    }

    @Override
    public SkillResult result(
            final SkillContext context,
            final PrepareWaterSourceParameters parameters
    ) {
        return switch (phase) {
            case COMPLETED -> SkillResult.completed();
            case CANCELLED -> SkillResult.cancelled();
            case FAILED -> SkillResult.failed(
                    Objects.requireNonNull(failure)
            );
            default -> SkillResult.failed(failure("invalid_state"));
        };
    }

    private SkillTickResult aimAndStartBreak(
            final SkillContext context,
            final PrepareWaterSourceParameters parameters,
            final CoreSkillFrame core,
            final InteractionSkillFrame frame
    ) {
        final Optional<VisibleBlockFace> visible = currentGround(
                frame,
                parameters
        );
        if (visible.isEmpty()) {
            return fail(
                    context,
                    parameters,
                    "ground_not_currently_visible"
            );
        }
        final VisibleBlockFace ground = visible.orElseThrow();
        final AimResult aim = aimAt(core, ground.hitPosition());
        if (!aim.accepted()) {
            return fail(context, parameters, "look_rejected");
        }
        if (aim.errorDegrees() > AIM_ALIGNMENT_DEGREES) {
            return SkillTickResult.running(true, false);
        }
        final Optional<String> shovel = selectedShovel(frame);
        if (shovel.isPresent()
                && !shovel.orElseThrow().equals(
                        frame.mainHand().itemId()
                )) {
            final ActionOutcome equipped = actuator.equipMainHand(
                    shovel.orElseThrow()
            );
            if (equipped != ActionOutcome.COMPLETED) {
                return fail(
                        context,
                        parameters,
                        actionFailure("equip_shovel", equipped)
                );
            }
        }
        breakParameters = new BreakBlockParameters(
                parameters.dimension(),
                observedTarget(frame, ground)
        );
        breakSkill = new BreakBlockSkill(
                expectedPlayerId,
                actuator,
                frames,
                interactionPolicy
        );
        final Optional<SkillFailure> rejected = breakSkill.preconditions(
                context,
                breakParameters
        );
        if (rejected.isPresent()) {
            return fail(context, parameters, rejected.orElseThrow());
        }
        breakSkill.start(context, breakParameters);
        phase = Phase.BREAKING;
        return SkillTickResult.running(true, false);
    }

    private SkillTickResult tickBreak(
            final SkillContext context,
            final PrepareWaterSourceParameters parameters
    ) {
        final SkillTickResult result = Objects.requireNonNull(
                breakSkill
        ).tick(context, Objects.requireNonNull(breakParameters));
        if (result.status() == SkillTickResult.Status.FAILED) {
            return fail(
                    context,
                    parameters,
                    result.failure().orElseThrow()
            );
        }
        if (result.status() == SkillTickResult.Status.COMPLETED) {
            breakSkill = null;
            breakParameters = null;
            phase = Phase.REVEALING_BOTTOM;
        }
        return SkillTickResult.running(result.madeProgress(), false);
    }

    private SkillTickResult revealBottom(
            final SkillContext context,
            final PrepareWaterSourceParameters parameters,
            final CoreSkillFrame core,
            final InteractionSkillFrame frame
    ) {
        final Optional<VisibleBlockFace> bottom = visibleBottom(
                frame,
                parameters
        );
        final AimResult aim = aimAt(
                core,
                bottom.map(VisibleBlockFace::hitPosition)
                        .orElseGet(() -> expectedBottomSurface(parameters))
        );
        if (!aim.accepted()) {
            return fail(context, parameters, "look_rejected");
        }
        if (bottom.isEmpty()
                || aim.errorDegrees() > AIM_ALIGNMENT_DEGREES) {
            return SkillTickResult.running(true, false);
        }
        if (!"minecraft:water_bucket".equals(
                frame.mainHand().itemId()
        )) {
            final ActionOutcome equipped = actuator.equipMainHand(
                    "minecraft:water_bucket"
            );
            if (equipped != ActionOutcome.COMPLETED) {
                return fail(
                        context,
                        parameters,
                        actionFailure("equip_water_bucket", equipped)
                );
            }
            /*
             * Inventory/menu swaps are synchronous, but the child skill must
             * bind to a fresh semantic frame that actually reports the water
             * bucket in hand. This prevents a stale non-empty hand from
             * satisfying UseItemSkill's generic held-item precondition.
             */
            return SkillTickResult.running(true, false);
        }
        useParameters = new UseItemParameters(
                parameters.dimension(),
                dev.mcai.companion.action.ActionHand.MAIN_HAND,
                0
        );
        useSkill = new UseItemSkill(
                expectedPlayerId,
                actuator,
                frames,
                interactionPolicy
        );
        final Optional<SkillFailure> rejected = useSkill.preconditions(
                context,
                useParameters
        );
        if (rejected.isPresent()) {
            return fail(context, parameters, rejected.orElseThrow());
        }
        useSkill.start(context, useParameters);
        phase = Phase.PLACING;
        return SkillTickResult.running(true, false);
    }

    private SkillTickResult tickPlacement(
            final SkillContext context,
            final PrepareWaterSourceParameters parameters,
            final InteractionSkillFrame frame
    ) {
        final SkillTickResult result = Objects.requireNonNull(useSkill)
                .tick(context, Objects.requireNonNull(useParameters));
        if (result.status() == SkillTickResult.Status.FAILED) {
            return fail(
                    context,
                    parameters,
                    result.failure().orElseThrow()
            );
        }
        if (result.status() == SkillTickResult.Status.COMPLETED) {
            useSkill = null;
            useParameters = null;
            placementObservationRevision = frame.observationRevision();
            placedAtGameTick = context.gameTick();
            phase = Phase.VERIFYING;
        }
        return SkillTickResult.running(result.madeProgress(), false);
    }

    private SkillTickResult verifyWater(
            final SkillContext context,
            final PrepareWaterSourceParameters parameters,
            final CoreSkillFrame core,
            final InteractionSkillFrame frame
    ) {
        if (frame.observationRevision()
                > placementObservationRevision) {
            final boolean waterVisible = frame.visibleBlockFaces().stream()
                    .anyMatch(face -> sameGround(
                            face,
                            parameters.ground()
                    ) && "minecraft:water".equals(face.blockTypeId()));
            final boolean bucketChanged = itemCount(
                    frame.inventory(),
                    "minecraft:water_bucket"
            ) == waterBucketsBefore - 1
                    && itemCount(frame.inventory(), "minecraft:bucket")
                            == emptyBucketsBefore + 1;
            if (waterVisible && bucketChanged) {
                phase = Phase.COMPLETED;
                coreActuator.stop();
                return SkillTickResult.completed();
            }
        }
        final AimResult reveal = aimAt(
                core,
                expectedWaterSurface(parameters)
        );
        if (!reveal.accepted()) {
            return fail…22036 tokens truncated…mes,
                        interactions,
                        interactionFrames,
                        inventory
                )
        );
        registry.register(
                PREPARE_STONE_TOOLS,
                new PrepareStoneToolsSkill(
                        playerId,
                        core,
                        coreFrames,
                        interactions,
                        interactionFrames,
                        inventory,
                        resourceInventory,
                        knownCraftingTable
                )
        );
        registry.register(
                PREPARE_IRON_TOOLKIT,
                new PrepareIronToolkitSkill(
                        playerId,
                        core,
                        coreFrames,
                        interactions,
                        interactionFrames,
                        inventory,
                        resourceInventory,
                        menus,
                        menuFrames,
                        knownCraftingTable,
                        knownFurnace
                )
        );
        registry.register(
                ESTABLISH_FOUNDATION_WORKSTATIONS,
                new EstablishFoundationWorkstationsSkill(
                        playerId,
                        core,
                        coreFrames,
                        interactions,
                        interactionFrames,
                        inventory,
                        resourceInventory,
                        menus,
                        menuFrames,
                        knownCraftingTable,
                        knownFurnace,
                        knownStorage
                )
        );
        return registry.register(
                PREPARE_FOUNDATION_SHELTER_MATERIALS,
                new PrepareFoundationShelterMaterialsSkill(
                        playerId,
                        core,
                        coreFrames,
                        interactions,
                        interactionFrames,
                        inventory,
                        resourceInventory,
                        menus,
                        menuFrames,
                        knownCraftingTable,
                        knownFurnace
                )
        );
    }

    public static String plannerGuide() {
        return """
            Foundation compounds take no args: prepare_basic_crafting,
            prepare_stone_tools, prepare_iron_toolkit,
            establish_foundation_workstations,
            prepare_foundation_shelter_materials. They use bounded legal
            actions. Iron toolkit performs furnace smelting and crafts the
            iron pickaxe/bucket/shield.
            """;
    }
}
