package dev.mcai.companion.progression;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mcai.companion.control.GoalSnapshot;
import dev.mcai.companion.control.GoalSource;
import dev.mcai.companion.control.GoalStatus;
import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class SurvivalRouteTrackerTest {
    @Test
    void activatesOnlyForExplicitCompletionGoalsOrLockedEvaluation() {
        assertTrue(SurvivalRouteTracker.isCompletionGoal(goal(
                GoalSource.HARDCORE_EVALUATION,
                "anything"
        )));
        assertTrue(SurvivalRouteTracker.isCompletionGoal(goal(
                GoalSource.PLAYER_CHAT,
                "请从零通关 Minecraft"
        )));
        assertTrue(SurvivalRouteTracker.isCompletionGoal(goal(
                GoalSource.MCP,
                "Beat Minecraft in survival"
        )));
        assertFalse(SurvivalRouteTracker.isCompletionGoal(goal(
                GoalSource.PLAYER_CHAT,
                "收小麦后回家"
        )));
    }

    @Test
    void selectsIndependentFoundationAndCompletionProfiles() {
        assertEquals(
                Optional.of(SurvivalRouteProfile.FOUNDATION),
                SurvivalRouteTracker.profile(goal(
                        GoalSource.PLAYER_CHAT,
                        "建立安全据点并生存到第二天"
                ))
        );
        assertEquals(
                Optional.of(SurvivalRouteProfile.COMPLETION),
                SurvivalRouteTracker.profile(goal(
                        GoalSource.PLAYER_CHAT,
                        "从零通关 Minecraft"
                ))
        );
        assertEquals(
                Optional.empty(),
                SurvivalRouteTracker.profile(goal(
                        GoalSource.PLAYER_CHAT,
                        "跟我来"
                ))
        );
    }

    @Test
    void completionResourceReadinessCountsCraftedEyesWithoutOneDropShortcuts() {
        assertEquals(
                2,
                SurvivalRouteTracker.blazeRouteUnits(
                        Map.of("minecraft:blaze_rod", 1)
                )
        );
        assertEquals(
                1,
                SurvivalRouteTracker.enderRouteUnits(
                        Map.of("minecraft:ender_pearl", 1)
                )
        );
        assertTrue(
                SurvivalRouteTracker.blazeRouteUnits(
                        Map.of(
                                "minecraft:blaze_rod",
                                6,
                                "minecraft:blaze_powder",
                                1,
                                "minecraft:ender_eye",
                                1
                        )
                ) >= SurvivalRouteTracker
                        .COMPLETION_BLAZE_ROUTE_UNITS
        );
        assertEquals(
                14,
                SurvivalRouteTracker.enderRouteUnits(
                        Map.of(
                                "minecraft:ender_pearl",
                                2,
                                "minecraft:ender_eye",
                                12
                        )
                )
        );
    }

    @Test
    void completionMinimumTargetsRetireOnlyAfterVerifiedMilestones() {
        final Map<String, Integer> initial =
                SurvivalRouteTracker.minimumTargets(
                        SurvivalRouteProfile.COMPLETION,
                        Set.of()
                );
        assertEquals(
                SurvivalRouteTracker.COMPLETION_BLAZE_ROUTE_UNITS,
                initial.get("blaze_route_units")
        );
        assertEquals(
                SurvivalRouteTracker.COMPLETION_ENDER_ROUTE_UNITS,
                initial.get("ender_route_units")
        );
        assertEquals(
                SurvivalRouteTracker.COMPLETION_EYES_READY,
                initial.get("eyes_of_ender")
        );
        assertEquals(
                SurvivalRouteTracker.COMPLETION_END_BUILDING_BLOCKS,
                initial.get("building_blocks")
        );
        assertEquals(
                SurvivalRouteTracker.COMPLETION_END_BOWS,
                initial.get("bows")
        );
        assertEquals(
                SurvivalRouteTracker.COMPLETION_END_ARROWS,
                initial.get("arrows")
        );

        final Map<String, Integer> verified =
                SurvivalRouteTracker.minimumTargets(
                        SurvivalRouteProfile.COMPLETION,
                        Set.of(
                                SurvivalMilestone
                                        .BLAZE_MATERIAL_OBTAINED,
                                SurvivalMilestone.ENDER_PEARL_OBTAINED,
                                SurvivalMilestone.EYE_OF_ENDER_CRAFTED
                        )
                );
        assertFalse(verified.containsKey("blaze_route_units"));
        assertFalse(verified.containsKey("ender_route_units"));
        assertFalse(verified.containsKey("eyes_of_ender"));
        assertTrue(verified.containsKey("building_blocks"));
        assertTrue(verified.containsKey("bows"));
        assertTrue(verified.containsKey("arrows"));
        assertEquals(8, verified.get("food"));

        final Map<String, Integer> loadoutVerified =
                SurvivalRouteTracker.minimumTargets(
                        SurvivalRouteProfile.COMPLETION,
                        Set.of(
                                SurvivalMilestone.END_LOADOUT_PREPARED
                        )
                );
        assertTrue(loadoutVerified.containsKey("building_blocks"));
        assertTrue(loadoutVerified.containsKey("bows"));
        assertTrue(loadoutVerified.containsKey("arrows"));

        final Map<String, Integer> enteredWithLoadout =
                SurvivalRouteTracker.minimumTargets(
                        SurvivalRouteProfile.COMPLETION,
                        Set.of(
                                SurvivalMilestone.END_LOADOUT_PREPARED,
                                SurvivalMilestone.END_ENTERED
                        )
                );
        assertFalse(enteredWithLoadout.containsKey("building_blocks"));
        assertFalse(enteredWithLoadout.containsKey("bows"));
        assertFalse(enteredWithLoadout.containsKey("arrows"));
    }

    @Test
    void irreversibleEndStateSelectsLatePhaseWithoutForgingHistory() {
        final List<SurvivalMilestone> order = List.of(
                SurvivalMilestone.WOOD_OBTAINED,
                SurvivalMilestone.END_LOADOUT_PREPARED,
                SurvivalMilestone.END_ENTERED,
                SurvivalMilestone.END_ISLAND_REACHED,
                SurvivalMilestone.DRAGON_KILLED,
                SurvivalMilestone.RETURNED_FROM_END
        );
        final Set<SurvivalMilestone> onlyBody =
                Set.of(SurvivalMilestone.BODY_ACTIVE);

        assertEquals(
                Optional.of(
                        SurvivalMilestone.END_LOADOUT_PREPARED
                ),
                SurvivalRouteTracker.nextMilestone(
                        SurvivalRouteProfile.COMPLETION,
                        order,
                        onlyBody,
                        true,
                        false,
                        false,
                        false
                )
        );
        assertEquals(
                Optional.of(SurvivalMilestone.END_ISLAND_REACHED),
                SurvivalRouteTracker.nextMilestone(
                        SurvivalRouteProfile.COMPLETION,
                        order,
                        Set.of(
                                SurvivalMilestone.BODY_ACTIVE,
                                SurvivalMilestone.END_LOADOUT_PREPARED
                        ),
                        true,
                        false,
                        false,
                        true
                )
        );
        assertEquals(
                Optional.of(SurvivalMilestone.DRAGON_KILLED),
                SurvivalRouteTracker.nextMilestone(
                        SurvivalRouteProfile.COMPLETION,
                        order,
                        Set.of(
                                SurvivalMilestone.BODY_ACTIVE,
                                SurvivalMilestone.END_LOADOUT_PREPARED,
                                SurvivalMilestone.END_ISLAND_REACHED
                        ),
                        true,
                        false,
                        false,
                        true
                )
        );
        assertEquals(
                Optional.of(SurvivalMilestone.RETURNED_FROM_END),
                SurvivalRouteTracker.nextMilestone(
                        SurvivalRouteProfile.COMPLETION,
                        order,
                        onlyBody,
                        true,
                        true,
                        false,
                        false
                )
        );
        assertEquals(
                Optional.empty(),
                SurvivalRouteTracker.nextMilestone(
                        SurvivalRouteProfile.COMPLETION,
                        order,
                        onlyBody,
                        false,
                        true,
                        true,
                        false
                )
        );
        assertEquals(
                Optional.of(SurvivalMilestone.END_LOADOUT_PREPARED),
                SurvivalRouteTracker.nextMilestone(
                        SurvivalRouteProfile.COMPLETION,
                        order,
                        Set.of(
                                SurvivalMilestone.BODY_ACTIVE,
                                SurvivalMilestone.WOOD_OBTAINED,
                                SurvivalMilestone.END_LOADOUT_PREPARED
                        ),
                        false,
                        false,
                        false,
                        false
                )
        );
        assertEquals(
                onlyBody,
                Set.of(SurvivalMilestone.BODY_ACTIVE)
        );
    }

    @Test
    void endReadinessRequiresTheWholeOwnedLoadout() {
        assertTrue(SurvivalRouteTracker.endLoadoutReady(Map.of(
                "building_blocks",
                SurvivalRouteTracker.COMPLETION_END_BUILDING_BLOCKS,
                "bows",
                SurvivalRouteTracker.COMPLETION_END_BOWS,
                "arrows",
                SurvivalRouteTracker.COMPLETION_END_ARROWS
        )));
        assertFalse(SurvivalRouteTracker.endLoadoutReady(Map.of(
                "building_blocks", 64,
                "bows", 1,
                "arrows", 15
        )));
    }

    private static GoalSnapshot goal(
            final GoalSource source,
            final String text
    ) {
        return new GoalSnapshot(
                Optional.empty(),
                1,
                GoalStatus.RUNNING,
                source,
                text,
                "",
                Instant.EPOCH,
                source == GoalSource.HARDCORE_EVALUATION
        );
    }
}
