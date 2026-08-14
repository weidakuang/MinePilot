package dev.mcai.companion.skills.core;

import static dev.mcai.companion.skills.core.CoreSkillTestFixtures.PLAYER_ID;
import static dev.mcai.companion.skills.core.CoreSkillTestFixtures.corridor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mcai.companion.action.ActionHand;
import dev.mcai.companion.action.ActionOutcome;
import dev.mcai.companion.navigation.GridPos;
import dev.mcai.companion.navigation.LocalNavSnapshot;
import dev.mcai.companion.navigation.OccupancyEvidence;
import dev.mcai.companion.navigation.ObservedVoxel;
import dev.mcai.companion.navigation.VoxelKind;
import dev.mcai.companion.perception.BlockCoordinate;
import dev.mcai.companion.perception.DangerKind;
import dev.mcai.companion.perception.DangerSignal;
import dev.mcai.companion.perception.HeldItemSummary;
import dev.mcai.companion.perception.InventoryItemSummary;
import dev.mcai.companion.perception.PerceptionProvenance;
import dev.mcai.companion.perception.PerceptionVec3;
import dev.mcai.companion.perception.TopSupportAffordance;
import dev.mcai.companion.perception.VisibleBlockFace;
import dev.mcai.companion.perception.VisibleEntity;
import dev.mcai.companion.waypoint.DimensionRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class EmergencySurvivalControllerTest {
    private static final PerceptionVec3 EAST =
            new PerceptionVec3(1.0, 0.0, 0.0);

    @Test
    void preemptsTaskControlsBeforeEmergencyEquipmentMutation() {
        final CoreSkillFrame critical = withHealth(
                frameWithInventory(
                        1,
                        20,
                        HeldItemSummary.empty(),
                        HeldItemSummary.empty(),
                        List.of(new InventoryItemSummary(
                                "minecraft:golden_apple",
                                1
                        )),
                        List.of(),
                        List.of(),
                        List.of(),
                        true,
                        DimensionRef.OVERWORLD,
                        EAST
                ),
                4.0F
        );
        final CoreSkillTestFixtures.MutableFrames frames =
                new CoreSkillTestFixtures.MutableFrames(critical);
        final CoreSkillTestFixtures.RecordingActuator actuator =
                new CoreSkillTestFixtures.RecordingActuator();
        final AtomicBoolean preempted = new AtomicBoolean();
        final AtomicInteger preemptions = new AtomicInteger();
        final EmergencyEquipmentActuator equipment =
                (hand, itemId) -> {
                    assertTrue(
                            preempted.get(),
                            "task controls must be released before inventory"
                    );
                    return ActionOutcome.COMPLETED;
                };
        final EmergencySurvivalController controller =
                new EmergencySurvivalController(
                        PLAYER_ID,
                        actuator,
                        frames,
                        equipment,
                        EmergencyMeleeActuator.unavailable(),
                        () -> {
                            preempted.set(true);
                            preemptions.incrementAndGet();
                        }
                );

        final var report = controller.tick();

        assertTrue(report.intervened());
        assertEquals(
                EmergencySurvivalController.State.EQUIPPING_FOOD,
                report.state()
        );
        assertEquals(1, preemptions.get());
    }

    @Test
    void consumesHeldFoodAndReleasesAfterFoodRises() {
        CoreSkillTestFixtures.MutableFrames frames =
                new CoreSkillTestFixtures.MutableFrames(frame(
                        1,
                        0.5,
                        5,
                        new HeldItemSummary(
                                "minecraft:bread",
                                2,
                                0,
                                0
                        ),
                        HeldItemSummary.empty(),
                        List.of(),
                        List.of()
                ));
        CoreSkillTestFixtures.RecordingActuator actuator =
                new CoreSkillTestFixtures.RecordingActuator();
        EmergencySurvivalController controller =
                new EmergencySurvivalController(
                        PLAYER_ID,
                        actuator,
                        frames
                );

        EmergencySurvivalController.TickReport first = controller.tick();
        assertTrue(first.intervened());
        assertEquals(
                EmergencySurvivalController.State.EATING,
                first.state()
        );
        assertEquals(List.of(ActionHand.MAIN_HAND), actuator.itemUses);

        frames.frame = frame(
                2,
                0.5,
                11,
                new HeldItemSummary("minecraft:bread", 1, 0, 0),
                HeldItemSummary.empty(),
                List.of(),
                List.of()
        );
        EmergencySurvivalController.TickReport finished =
                controller.tick();

        assertTrue(finished.intervened());
        assertEquals(
                EmergencySurvivalController.State.CLEAR,
                finished.state()
        );
        assertEquals(1, actuator.releases);
    }

    @Test
    void holdsGoldenAppleUseAtFullHungerUntilStackIsConsumed() {
        final HeldItemSummary goldenApple =
                new HeldItemSummary(
                        "minecraft:golden_apple",
                        1,
                        0,
                        0
                );
        final CoreSkillTestFixtures.MutableFrames frames =
                new CoreSkillTestFixtures.MutableFrames(
                        withHealth(
                                frame(
                                        1,
                                        0.5,
                                        20,
                                        goldenApple,
                                        HeldItemSummary.empty(),
                                        List.of(),
                                        List.of()
                                ),
                                4.0F
                        )
                );
        final CoreSkillTestFixtures.RecordingActuator actuator =
                new CoreSkillTestFixtures.RecordingActuator();
        final EmergencySurvivalController controller =
                new EmergencySurvivalController(
                        PLAYER_ID,
                        actuator,
                        frames
                );

        final var started = controller.tick();
        assertEquals(
                EmergencySurvivalController.State.EATING,
                started.state()
        );

        frames.frame = withHealth(
                frame(
                        2,
                        0.5,
                        20,
                        goldenApple,
                        HeldItemSummary.empty(),
                        List.of(),
                        List.of()
                ),
                4.0F
        );
        final var holding = controller.tick();

        assertEquals(
                EmergencySurvivalController.State.EATING,
                holding.state()
        );
        assertEquals("eating", holding.reason());
        assertEquals(0, actuator.releases);

        frames.frame = withHealth(
                frame(
                        3,
                        0.5,
                        20,
                        HeldItemSummary.empty(),
                        HeldItemSummary.empty(),
                        List.of(),
                        List.of()
                ),
                4.0F
        );
        final var consumed = controller.tick();

        assertEquals(
                EmergencySurvivalController.State.CLEAR,
                consumed.state()
        );
        assertEquals("eating_finished", consumed.reason());
        assertEquals(1, actuator.releases);
    }

    @Test
    void retreatsOnlyIntoObservedStandableCellAwayFromVisibleThreat() {
        UUID hostileId = UUID.fromString(
                "00000000-0000-0000-0000-000000000999"
        );
        VisibleEntity hostile = new VisibleEntity(
                hostileId,
                "minecraft:zombie",
                new PerceptionVec3(0.5, 1.0, 0.5),
                new PerceptionVec3(-1.0, 0.0, 0.0),
                1.0,
                true,
                false,
                PerceptionProvenance.ENTITY_DISTANCE_FOV_AND_BLOCK_CLIP
        );
        DangerSignal danger = new DangerSignal(
                DangerKind.HOSTILE_PROXIMITY,
                0.75,
                2.0,
                Optional.empty(),
                PerceptionProvenance.PROXIMITY_THREAT
        );
        CoreSkillTestFixtures.MutableFrames frames =
                new CoreSkillTestFixtures.MutableFrames(frame(
                        1,
                        1.5,
                        20,
                        HeldItemSummary.empty(),
                        HeldItemSummary.empty(),
                        List.of(hostile),
                        List.of(danger)
                ));
        CoreSkillTestFixtures.RecordingActuator actuator =
                new CoreSkillTestFixtures.RecordingActuator();
        EmergencySurvivalController controller =
                new EmergencySurvivalController(
                        PLAYER_ID,
                        actuator,
                        frames
                );

        EmergencySurvivalController.TickReport report = controller.tick();

        assertTrue(report.intervened());
        assertEquals(
                EmergencySurvivalController.State.RETREATING,
                report.state()
        );
        assertFalse(actuator.looks.isEmpty());
        assertFalse(actuator.movements.isEmpty());
        assertTrue(actuator.movements.getLast().sprint());
    }

    @Test
    void activeCombatOwnsOrdinaryHostileProximityWithoutLosingInputs() {
        final VisibleEntity hostile = new VisibleEntity(
                UUID.fromString(
                        "00000000-0000-0000-0000-000000000997"
                ),
                "minecraft:blaze",
                new PerceptionVec3(0.5, 1.0, 6.5),
                new PerceptionVec3(0.0, 0.0, 6.0),
                6.0,
                true,
                false,
                PerceptionProvenance
                        .ENTITY_DISTANCE_FOV_AND_BLOCK_CLIP
        );
        final DangerSignal danger = new DangerSignal(
                DangerKind.HOSTILE_PROXIMITY,
                0.5,
                6.0,
                Optional.empty(),
                PerceptionProvenance.PROXIMITY_THREAT
        );
        final CoreSkillTestFixtures.MutableFrames frames =
                new CoreSkillTestFixtures.MutableFrames(frame(
                        1,
                        0.5,
                        20,
                        new HeldItemSummary(
                                "minecraft:diamond_sword",
                                1,
                                0,
                                1561
                        ),
                        new HeldItemSummary(
                                "minecraft:shield",
                                1,
                                0,
                                336
                        ),
                        List.of(hostile),
                        List.of(danger)
                ));
        final CoreSkillTestFixtures.RecordingActuator actuator =
                new CoreSkillTestFixtures.RecordingActuator();
        final EmergencySurvivalController controller =
                new EmergencySurvivalController(
                        PLAYER_ID,
                        actuator,
                        frames
                );

        final var report = controller.tick(true);

        assertFalse(report.intervened());
        assertEquals(
                EmergencySurvivalController.State.CLEAR,
                report.state()
        );
        assertEquals(0, actuator.stops);
        assertTrue(actuator.looks.isEmpty());
        assertTrue(actuator.itemUses.isEmpty());
    }

    @Test
    void activeCombatKeepsTargetLossReacquisitionOwnership() {
        final VisibleEntity hostile = new VisibleEntity(
                UUID.fromString(
                        "00000000-0000-0000-0000-000000000995"
                ),
                "minecraft:zombie",
                new PerceptionVec3(0.5, 1.0, 6.5),
                new PerceptionVec3(0.0, 0.0, 6.0),
                6.0,
                true,
                false,
                PerceptionProvenance
                        .ENTITY_DISTANCE_FOV_AND_BLOCK_CLIP
        );
        final CoreSkillTestFixtures.MutableFrames frames =
                new CoreSkillTestFixtures.MutableFrames(frame(
                        1,
                        0.5,
                        20,
                        HeldItemSummary.empty(),
                        HeldItemSummary.empty(),
                        List.of(hostile),
                        List.of()
                ));
        final CoreSkillTestFixtures.RecordingActuator actuator =
                new CoreSkillTestFixtures.RecordingActuator();
        final EmergencySurvivalController controller =
                new EmergencySurvivalController(
                        PLAYER_ID,
                        actuator,
                        frames
                );

        assertFalse(controller.tick(true).intervened());

        frames.frame = frame(
                2,
                0.5,
                20,
                HeldItemSummary.empty(),
                HeldItemSummary.empty(),
                List.of(),
                List.of()
        );
        final var targetLost = controller.tick(true);

        assertFalse(targetLost.intervened());
        assertEquals(EmergencySurvivalController.State.CLEAR,
                targetLost.state());
        assertEquals(0, actuator.stops);
        assertTrue(actuator.looks.isEmpty());
        assertTrue(actuator.movements.isEmpty());
    }

    @Test
    void continuousThreatRetreatIsBoundedThenFallsBackToShield() {
        final VisibleEntity hostile = new VisibleEntity(
                UUID.fromString(
                        "00000000-0000-0000-0000-000000000996"
                ),
                "minecraft:zombie",
                new PerceptionVec3(0.5, 1.0, 0.5),
                new PerceptionVec3(-1.0, 0.0, 0.0),
                1.0,
                true,
                false,
                PerceptionProvenance
                        .ENTITY_DISTANCE_FOV_AND_BLOCK_CLIP
        );
        final DangerSignal danger = new DangerSignal(
                DangerKind.HOSTILE_PROXIMITY,
                0.75,
                2.0,
                Optional.empty(),
                PerceptionProvenance.PROXIMITY_THREAT
        );
        final HeldItemSummary shield = new HeldItemSummary(
                "minecraft:shield",
                1,
                0,
                336
        );
        final CoreSkillTestFixtures.MutableFrames frames =
                new CoreSkillTestFixtures.MutableFrames(frame(
                        1,
                        1.5,
                        20,
                        HeldItemSummary.empty(),
                        shield,
                        List.of(hostile),
                        List.of(danger)
                ));
        final CoreSkillTestFixtures.RecordingActuator actuator =
                new CoreSkillTestFixtures.RecordingActuator();
        final EmergencySurvivalController controller =
                new EmergencySurvivalController(
                        PLAYER_ID,
                        actuator,
                        frames
                );

        assertEquals(
                EmergencySurvivalController.State.RETREATING,
                controller.tick().state()
        );
        frames.frame = frame(
                62,
                1.5,
                20,
                HeldItemSummary.empty(),
                shield,
                List.of(hostile),
                List.of(danger)
        );
        final var bounded = controller.tick();

        assertEquals(
                EmergencySurvivalController.State.GUARDING,
                bounded.state()
        );
        assertEquals(List.of(ActionHand.OFF_HAND), actuator.itemUses);
    }

    @Test
    void meleeCombatMayOwnPhysicalContactWithoutEmergencyGuardOverride() {
        final DangerSignal contact = new DangerSignal(
                DangerKind.THREAT_CONTACT,
                1.0,
                0.5,
                Optional.of(new PerceptionVec3(0.0, 0.0, 1.0)),
                PerceptionProvenance.PHYSICAL_CONTACT
        );
        final CoreSkillTestFixtures.MutableFrames frames =
                new CoreSkillTestFixtures.MutableFrames(frame(
                        1,
                        0.5,
                        20,
                        new HeldItemSummary(
                                "minecraft:iron_sword",
                                1,
                                0,
                                250
                        ),
                        new HeldItemSummary(
                                "minecraft:shield",
                                1,
                                0,
                                336
                        ),
                        List.of(),
                        List.of(contact)
                ));
        final CoreSkillTestFixtures.RecordingActuator actuator =
                new CoreSkillTestFixtures.RecordingActuator();
        final EmergencySurvivalController controller =
                new EmergencySurvivalController(
                        PLAYER_ID,
                        actuator,
                        frames
                );

        final var report = controller.tick(true, true);

        assertFalse(report.intervened());
        assertEquals(
                EmergencySurvivalController.State.CLEAR,
                report.state()
        );
        assertTrue(actuator.itemUses.isEmpty());
    }

    @Test
    void emergencyRetreatRejectsSingleRayAirAndGenericSupport() {
        final VisibleEntity hostile = new VisibleEntity(
                UUID.fromString(
                        "00000000-0000-0000-0000-000000000998"
                ),
                "minecraft:zombie",
                new PerceptionVec3(0.5, 1.0, 0.5),
                new PerceptionVec3(-1.0, 0.0, 0.0),
                1.0,
                true,
                false,
                PerceptionProvenance
                        .ENTITY_DISTANCE_FOV_AND_BLOCK_CLIP
        );
        final DangerSignal danger = new DangerSignal(
                DangerKind.HOSTILE_PROXIMITY,
                0.75,
                2.0,
                Optional.empty(),
                PerceptionProvenance.PROXIMITY_THREAT
        );
        final CoreSkillFrame base = frame(
                1,
                0.5,
                20,
                HeldItemSummary.empty(),
                HeldItemSummary.empty(),
                List.of(hostile),
                List.of(danger)
        );
        final List<LocalNavSnapshot> unsafe = List.of(
                replaceEvidence(
                        base.navigation(),
                        new GridPos(1, 1, 0),
                        OccupancyEvidence.SINGLE_RAY_CLEAR,
                        TopSupportAffordance.UNKNOWN
                ),
                replaceEvidence(
                        base.navigation(),
                        new GridPos(1, 0, 0),
                        OccupancyEvidence.SURFACE_HIT,
                        TopSupportAffordance.UNKNOWN
                )
        );

        for (LocalNavSnapshot navigation : unsafe) {
            final CoreSkillTestFixtures.MutableFrames frames =
                    new CoreSkillTestFixtures.MutableFrames(
                            withNavigation(base, navigation)
                    );
            final CoreSkillTestFixtures.RecordingActuator actuator =
                    new CoreSkillTestFixtures.RecordingActuator();
            final EmergencySurvivalController controller =
                    new EmergencySurvivalController(
                            PLAYER_ID,
                            actuator,
                            frames
                    );

            final var report = controller.tick();

            assertTrue(report.intervened());
            assertTrue(actuator.movements.isEmpty());
            assertTrue(actuator.stops > 0);
        }
    }

    @Test
    void unknownThreatDirectionNeverCausesBlindMovement() {
        DangerSignal danger = new DangerSignal(
                DangerKind.PROJECTILE_PROXIMITY,
                0.75,
                4.0,
                Optional.empty(),
                PerceptionProvenance.PROXIMITY_THREAT
        );
        CoreSkillTestFixtures.MutableFrames frames =
                new CoreSkillTestFixtures.MutableFrames(frame(
                        1,
                        0.5,
                        20,
                        HeldItemSummary.empty(),
                        HeldItemSummary.empty(),
                        List.of(),
                        List.of(danger)
                ));
        CoreSkillTestFixtures.RecordingActuator actuator =
                new CoreSkillTestFixtures.RecordingActuator();
        EmergencySurvivalController controller =
                new EmergencySurvivalController(
                        PLAYER_ID,
                        actuator,
                        frames
                );

        EmergencySurvivalController.TickReport report = controller.tick();

        assertTrue(report.intervened());
        assertTrue(actuator.stops > 0);
        // The local map does contain an east cell, but without an observed
        // threat direction no retreat direction may be invented.
        assertTrue(actuator.movements.isEmpty());
    }

    @Test
    void unknownThreatUsesAnAlreadyHeldShieldWithoutMoving() {
        DangerSignal danger = new DangerSignal(
                DangerKind.PROJECTILE_PROXIMITY,
                0.75,
                4.0,
                Optional.empty(),
                PerceptionProvenance.PROXIMITY_THREAT
        );
        CoreSkillTestFixtures.MutableFrames frames =
                new CoreSkillTestFixtures.MutableFrames(frame(
                        1,
                        0.5,
                        20,
                        HeldItemSummary.empty(),
                        new HeldItemSummary(
                                "minecraft:shield",
                                1,
                                0,
                                336
                        ),
                        List.of(),
                        List.of(danger)
                ));
        CoreSkillTestFixtures.RecordingActuator actuator =
                new CoreSkillTestFixtures.RecordingActuator();
        EmergencySurvivalController controller =
                new EmergencySurvivalController(
                        PLAYER_ID,
                        actuator,
                        frames
                );

        EmergencySurvivalController.TickReport report = controller.tick();

        assertEquals(
                EmergencySurvivalController.State.GUARDING,
                report.state()
        );
        assertEquals(List.of(ActionHand.OFF_HAND), actuator.itemUses);
        assertTrue(actuator.movements.isEmpty());
    }

    @Test
    void directionlessRecentDamageDoesNotRemainShieldOnly() {
        final DangerSignal damage = new DangerSignal(
                DangerKind.THREAT_CONTACT,
                1.0,
                0.0,
                Optional.empty(),
                PerceptionProvenance.RECENT_DAMAGE_EVENT
        );
        final HeldItemSummary shield = new HeldItemSummary(
                "minecraft:shield",
                1,
                0,
                336
        );
        final CoreSkillTestFixtures.RecordingActuator actuator =
                new CoreSkillTestFixtures.RecordingActuator();
        final EmergencySurvivalController controller =
                new EmergencySurvivalController(
                        PLAYER_ID,
                        actuator,
                        new CoreSkillTestFixtures.MutableFrames(
                                frameWithInventory(
                                        1,
                                        20,
                                        HeldItemSummary.empty(),
                                        shield,
                                        List.of(),
                                        List.of(),
                                        List.of(damage),
                                        List.of(),
                                        true,
                                        DimensionRef.END,
                                        EAST
                                )
                        )
                );

        final var report = controller.tick();

        assertEquals(
                EmergencySurvivalController.State.GUARDING,
                report.state()
        );
        assertEquals(List.of(ActionHand.OFF_HAND), actuator.itemUses);
        assertFalse(
                actuator.movements.isEmpty(),
                "A directionless magic hit must trigger bounded side-step "
                    + "even while the shield is held"
        );
        assertTrue(actuator.movements.getLast().sneak());
        assertEquals(0.0, actuator.movements.getLast().forward(), 0.001);
    }

    @Test
    void continuousGuardKeepsOneUninterruptedShieldUse() {
        final DangerSignal danger = new DangerSignal(
                DangerKind.PROJECTILE_PROXIMITY,
                0.75,
                4.0,
                Optional.empty(),
                PerceptionProvenance.PROXIMITY_THREAT
        );
        final HeldItemSummary shield = new HeldItemSummary(
                "minecraft:shield",
                1,
                0,
                336
        );
        final CoreSkillTestFixtures.MutableFrames frames =
                new CoreSkillTestFixtures.MutableFrames(frame(
                        1,
                        0.5,
                        20,
                        HeldItemSummary.empty(),
                        shield,
                        List.of(),
                        List.of(danger)
                ));
        final CoreSkillTestFixtures.RecordingActuator actuator =
                new CoreSkillTestFixtures.RecordingActuator();
        final EmergencySurvivalController controller =
                new EmergencySurvivalController(
                        PLAYER_ID,
                        actuator,
                        frames
                );

        assertEquals("guarding", controller.tick().reason());
        frames.frame = frame(
                2,
                0.5,
                20,
                HeldItemSummary.empty(),
                shield,
                List.of(),
                List.of(danger)
        );
        assertEquals("guarding", controller.tick().reason());

        assertEquals(
                List.of(ActionHand.OFF_HAND),
                actuator.itemUses,
                "A continuing guard must not restart the shield every tick"
        );
        assertEquals(
                0,
                actuator.releases,
                "Vanilla shield warmup requires uninterrupted use"
        );
    }

    @Test
    void directionalDamageDoesNotLeaveShieldedBodyStationary() {
        final VisibleEntity zombie = new VisibleEntity(
                UUID.fromString(
                        "00000000-0000-0000-0000-000000000993"
                ),
                "minecraft:zombie",
                new PerceptionVec3(0.5, 5.0, 2.0),
                new PerceptionVec3(0.0, 0.0, 1.5),
                1.5,
                true,
                false,
                PerceptionProvenance
                        .ENTITY_DISTANCE_FOV_AND_BLOCK_CLIP
        );
        final DangerSignal damage = new DangerSignal(
                DangerKind.THREAT_CONTACT,
                1.0,
                1.5,
                Optional.of(new PerceptionVec3(0.0, 0.0, 1.0)),
                PerceptionProvenance.RECENT_DAMAGE_EVENT
        );
        final HeldItemSummary shield = new HeldItemSummary(
                "minecraft:shield",
                1,
                0,
                336
        );
        final CoreSkillTestFixtures.RecordingActuator actuator =
                new CoreSkillTestFixtures.RecordingActuator();
        final EmergencySurvivalController controller =
                new EmergencySurvivalController(
                        PLAYER_ID,
                        actuator,
                        new CoreSkillTestFixtures.MutableFrames(
                                frameWithInventory(
                                        1,
                                        20,
                                        HeldItemSummary.empty(),
                                        shield,
                                        List.of(),
                                        List.of(zombie),
                                        List.of(damage),
                                        List.of(),
                                        true,
                                        DimensionRef.OVERWORLD,
                                        EAST
                                )
                        )
                );

        final var report = controller.tick();

        assertEquals(
                EmergencySurvivalController.State.GUARDING,
                report.state()
        );
        assertEquals(List.of(ActionHand.OFF_HAND), actuator.itemUses);
        assertFalse(
                actuator.movements.isEmpty(),
                "A fair directional hit must trigger a bounded backstep "
                    + "instead of a look-only shield response"
        );
        assertTrue(actuator.movements.getLast().sneak());
    }

    @Test
    void visibleContactThreatIsAimedAtThenCounterattacked() {
        final UUID zombieId = UUID.fromString(
                "00000000-0000-0000-0000-000000000991"
        );
        final VisibleEntity zombie = new VisibleEntity(
                zombieId,
                "minecraft:zombie",
                new PerceptionVec3(0.5, 5.0, 2.5),
                new PerceptionVec3(0.0, 0.0, 2.0),
                2.0,
                true,
                false,
                PerceptionProvenance
                        .ENTITY_DISTANCE_FOV_AND_BLOCK_CLIP
        );
        final DangerSignal contact = new DangerSignal(
                DangerKind.THREAT_CONTACT,
                1.0,
                2.0,
                Optional.of(new PerceptionVec3(0.0, 0.0, -1.0)),
                PerceptionProvenance.PHYSICAL_CONTACT
        );
        final CoreSkillTestFixtures.MutableFrames frames =
                new CoreSkillTestFixtures.MutableFrames(
                        frameWithInventory(
                                1,
                                20,
                                HeldItemSummary.empty(),
                                HeldItemSummary.empty(),
                                List.of(),
                                List.of(zombie),
                                List.of(contact),
                                List.of(),
                                true,
                                DimensionRef.OVERWORLD,
                                EAST
                        )
                );
        final CoreSkillTestFixtures.RecordingActuator actuator =
                new CoreSkillTestFixtures.RecordingActuator();
        final RecordingMelee melee = new RecordingMelee();
        final EmergencySurvivalController controller =
                new EmergencySurvivalController(
                       