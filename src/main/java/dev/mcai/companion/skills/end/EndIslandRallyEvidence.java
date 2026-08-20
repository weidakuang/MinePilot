package dev.mcai.companion.skills.end;

import dev.mcai.companion.navigation.GridPos;
import dev.mcai.companion.navigation.NavigationEvidence;
import dev.mcai.companion.navigation.ObservedVoxel;
import dev.mcai.companion.perception.BlockCoordinate;
import dev.mcai.companion.skills.core.CoreSkillFrame;
import java.util.Objects;

/**
 * Fair, current-pose evidence for handing the End island from ingress to
 * dragon combat. A sticky route milestone is not sufficient: the body must
 * still be standing on a freshly observed natural End-stone cell with two
 * blocks of clear space in the same navigation revision.
 */
public final class EndIslandRallyEvidence {
    private static final String END_STONE = "minecraft:end_stone";
    private static final double CENTER_TOLERANCE = 0.72;

    private EndIslandRallyEvidence() {
    }

    public static boolean supportsCurrentPose(
            final CoreSkillFrame frame,
            final double maximumRadius
    ) {
        Objects.requireNonNull(frame, "frame");
        if (!frame.onGround()
                || frame.inWater()
                || !Double.isFinite(maximumRadius)
                || EndArenaTopology.horizontalRadius(frame.position())
                    > maximumRadius
                || frame.navigation().revision()
                    != frame.observationRevision()) {
            return false;
        }
        final GridPos support = frame.feet().below();
        final double centerDistance = Math.hypot(
                frame.position().x() - (support.x() + 0.5),
                frame.position().z() - (support.z() + 0.5)
        );
        if (centerDistance > CENTER_TOLERANCE
                || !safeFreshDestination(frame, support)) {
            return false;
        }
        return frame.visibleBlockFaces().stream().anyMatch(face ->
                END_STONE.equals(face.blockTypeId())
                        && "up".equals(face.face())
                        && face.topSupportAffordance()
                            .safelySupportsStanding()
                        && grid(face.block()).equals(support)
        );
    }

    private static boolean safeFreshDestination(
            final CoreSkillFrame frame,
            final GridPos support
    ) {
        final long revision = frame.navigation().revision();
        return frame.navigation().voxelAt(support).filter(voxel ->
                        NavigationEvidence.isFreshStandingSupport(
                                voxel,
                                revision
                        )).isPresent()
                && frame.navigation().voxelAt(support.above()).filter(voxel ->
                        NavigationEvidence.hasFreshTraversalClearance(
                                voxel,
                                revision
                        )).isPresent()
                && frame.navigation().voxelAt(support.above(2)).filter(voxel ->
                        NavigationEvidence.hasFreshTraversalClearance(
                                voxel,
                                revision
                        )).isPresent();
    }

    private static GridPos grid(final BlockCoordinate block) {
        return new GridPos(block.x(), block.y(), block.z());
    }
}
