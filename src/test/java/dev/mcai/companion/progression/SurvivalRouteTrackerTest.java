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
        assertEquals(8, verified.get("food"));
    }

    @Test
    void irreversibleEndStateSelectsLatePhaseWithoutForgingHistory() {
        final List<SurvivalMilestone> order = List.of(
                SurvivalMilestone.WOOD_OBTAINED,
                SurvivalMilestone.END_ENTERED,
                SurvivalMilestone.DRAGON_KILLED,
                SurvivalMilestone.RETURNED_FROM_END
        );
        final Set<SurvivalMilestone> onlyBody =
                Set.of(SurvivalMilestone.BODY_ACTIVE);

        assertEquals(
                Optional.of(SurvivalMilestone.DRAGON_KILLED),
                SurvivalRouteTracker.nextMilestone(
                        SurvivalRouteProfile.COMPLETION,
                        order,
                        onlyBody,
                        true,
                        false,
                        false
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
                        true
                )
        );
        assertEquals(
                onlyBody,
                Set.of(SurvivalMilestone.BODY_ACTIVE)
        );
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
