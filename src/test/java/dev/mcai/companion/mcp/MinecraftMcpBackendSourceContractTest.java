package dev.mcai.companion.mcp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Keeps the screenshot MCP result honest until a separately authenticated
 * first-person client capture path is implemented.  An Observer render is
 * evidence for an external gate, never the companion's model input.
 */
final class MinecraftMcpBackendSourceContractTest {
    @Test
    void unavailableScreenshotCannotBeInterpretedAsObserverOrModelVision()
            throws IOException {
        final String source = Files.readString(Path.of(
                "src/main/java/dev/mcai/companion/mcp/"
                        + "MinecraftMcpBackend.java"
        ));

        assertTrue(source.contains(
                "first_person_capture_not_ready"
        ));
        assertTrue(source.contains(
                "headless_server_player_unavailable"
        ));
        assertTrue(source.contains(
                "modelInput\", false"
        ));
        assertTrue(source.contains(
                "observerCameraAllowed\", false"
        ));
        assertTrue(source.contains(
                "requiresAuthenticatedClientCapture\", true"
        ));
        assertFalse(source.contains(
                "observer-rendered.png"
        ));
    }
}
