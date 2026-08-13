package dev.mcai.companion.perception;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Locks the conservative support affordance to the already ray-hit block.
 *
 * <p>A sturdy face alone is not proof of a full standing surface: slabs,
 * stairs, fences, and other partial collision shapes must remain fail-closed
 * for mining movement.</p>
 */
final class FairPerceptionSupportSourceContractTest {
    private static final Pattern FULL_STURDY_TOP = Pattern.compile(
            "if\\s*\\(\\s*state\\.isCollisionShapeFullBlock"
                + "\\(\\s*level,\\s*position\\)\\s*"
                + "&&\\s*state\\.isFaceSturdy\\s*"
                + "\\(\\s*level,\\s*position,\\s*Direction\\.UP\\s*\\)",
            Pattern.DOTALL
    );

    @Test
    void supportRequiresVisibleUpFaceFullCollisionAndSturdiness()
            throws Exception {
        final String source = Files.readString(Path.of(
                "src/main/java/dev/mcai/companion/perception/"
                    + "FairPerceptionSampler.java"
        ));
        final String affordanceSource = Files.readString(Path.of(
                "src/main/java/dev/mcai/companion/perception/"
                    + "BlockShapeAffordances.java"
        ));

        assertTrue(source.contains("BlockShapeAffordances.topSupport"));
        assertTrue(affordanceSource.contains("if (hitFace != Direction.UP)"));
        assertTrue(
                FULL_STURDY_TOP.matcher(affordanceSource).find(),
                "Standing support must require both a full collision shape "
                    + "and a sturdy UP face"
        );
    }

    @Test
    void everyLoadedBlockHitPreservesItsClearSegmentBeforeFaceDeduplication()
            throws Exception {
        final String source = Files.readString(Path.of(
                "src/main/java/dev/mcai/companion/perception/"
                    + "FairPerceptionSampler.java"
        ));
        final int validBlockHit = source.indexOf(
            "if (hit.getType() != HitResult.Type.BLOCK"
        );
        final int clearSegment = source.indexOf(
            "clearSegmentBeforeHit(",
            validBlockHit
        );
        final int faceDeduplication = source.indexOf(
            "if (uniqueFaces.containsKey(key))",
            validBlockHit
        );

        assertTrue(validBlockHit >= 0);
        assertTrue(clearSegment > validBlockHit);
        assertTrue(
            clearSegment < faceDeduplication,
            "A duplicate visible face must not discard the independently "
                + "cast ray's clear segment"
        );
    }

    @Test
    void dragonPartsPublishOneStableParentIdentity()
            throws Exception {
        final String source = Files.readString(Path.of(
                "src/main/java/dev/mcai/companion/perception/"
                    + "FairPerceptionSampler.java"
        ));

        assertTrue(source.contains(
                "candidate instanceof EnderDragonPart part"
        ));
        assertTrue(source.contains("? part.parentMob"));
        assertTrue(source.contains(
                "emittedEntityIds.add(perceived.getUUID())"
        ));
        assertTrue(source.contains("\"interactionAimX\""));
        assertTrue(source.contains("\"interactionAimY\""));
        assertTrue(source.contains("\"interactionAimZ\""));
        assertTrue(source.contains(
                "visibleEntityProperties("
        ));
    }
}
