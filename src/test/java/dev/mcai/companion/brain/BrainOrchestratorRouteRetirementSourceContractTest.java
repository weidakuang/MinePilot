package dev.mcai.companion.brain;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Locks the completion-route retirement boundary that prevents a verified
 * dragon milestone from being misreported as a stale active-skill failure.
 */
final class BrainOrchestratorRouteRetirementSourceContractTest {
    @Test
    void completionRouteRetiresOnlyTheTwoDimensionCompletionSkills() throws Exception {
        final String source = Files.readString(
                Path.of(
                        "src/main/java/dev/mcai/companion/brain/"
                                + "BrainOrchestrator.java"
                ),
                StandardCharsets.UTF_8
        );
        assertTrue(
                source.contains("final boolean completionRoute")
                        && source.contains(
                                "completionRoute && completionSkill"
                        ),
                "completion retirement must stay on the trusted route"
        );
        assertTrue(
                source.contains("case \"fight_ender_dragon\" -> \"DRAGON_KILLED\""),
                "dragon skill must retire at the server milestone"
        );
        assertTrue(
                source.contains(
                        "case \"find_and_enter_observed_portal\" ->"
                                + " \"RETURNED_FROM_END\""
                ),
                "portal skill must retire only after the verified return"
        );
    }
}
