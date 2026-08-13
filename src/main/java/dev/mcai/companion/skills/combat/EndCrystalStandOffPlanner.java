package dev.mcai.companion.skills.combat;

import dev.mcai.companion.navigation.GridPos;
import dev.mcai.companion.navigation.ObservedVoxel;
import dev.mcai.companion.navigation.VoxelKind;
import dev.mcai.companion.perception.PerceptionVec3;
import dev.mcai.companion.skills.core.CoreSkillFrame;
import dev.mcai.companion.skills.core.MoveToParameters;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Selects a short, observed, safety-improving retreat before an End crystal
 * is attacked.
 *
 * <p>A crystal explodes with power six, so vanilla considers entities in a
 * twelve-block diameter-of-effect radius. The firing threshold stays beyond
 * that radius and the destination includes an arrival margin. Unknown cells,
 * liquid, unsupported cells, and danger-bearing cells are never candidates.</p>
 */
final class EndCrystalStandOffPlanner {
    static final double MINIMUM_FIRE_DISTANCE = 12.5;
    static final double MINIMUM_DESTINATION_DISTANCE = 13.25;

    private static final double MAXIMUM_RETREAT_DISTANCE = 10.0;
    private static final double MINIMUM_DISTANCE_IMPROVEMENT = 0.75;

    private EndCrystalStandOffPlanner() {
    }

    static Optional<GridPos> select(
            final CoreSkillFrame frame,
            final PerceptionVec3 crystal,
            final boolean hardcore
    ) {
        final GridPos current = frame.feet();
        final PerceptionVec3 currentCenter = center(current);
        final double currentDistance =
                horizontalDistance(currentCenter, crystal);
        final PerceptionVec3 away =
                horizontalDirection(crystal, currentCenter);
        if (away.lengthSquared() <= 1.0E-12) {
            return Optional.empty();
        }
        final double maximumDanger = hardcore ? 0.10 : 0.25;
        final List<GridPos> candidates = frame.navigation()
                .observedVoxels()
                .keySet()
                .stream()
                .filter(candidate ->
                        Math.abs(candidate.y() - current.y()) <= 1
                )
                .filter(candidate ->
                        horizontalDistance(
                                currentCenter,
                                center(candidate)
                        ) <= MAXIMUM_RETREAT_DISTANCE
                )
                .filter(candidate ->
                        horizontalDistance(
                                center(candidate),
                                crystal
                        ) >= currentDistance
                                + MINIMUM_DISTANCE_IMPROVEMENT
                )
                .filter(candidate ->
                        safetyImprovingDirection(
                                currentCenter,
                                center(candidate),
                                away
                        )
                )
                .filter(candidate ->
                        safeDryStanding(
                                frame,
                                candidate,
                                maximumDanger
                        )
                )
                .toList();
        /*
         * Prefer a directly observed final firing cell. A narrow first-person
         * navigation fan may legitimately expose only part of the retreat,
         * though, especially after looking up at a cage. In that case take
         * the farthest safe, strictly safety-improving observed step and scan
         * again from there. Requiring the entire 13.25-block retreat to be in
         * one frame made a real player body fail on an otherwise open floor.
         */
        final Optional<GridPos> finalDestination =
                candidates.stream()
                        .filter(candidate ->
                                horizontalDistance(
                                        center(candidate),
                                        crystal
                                ) >= MINIMUM_DESTINATION_DISTANCE
                        )
                        .min(Comparator
                        .<GridPos>comparingDouble(candidate ->
                                horizontalDistance(
                                        currentCenter,
                                        center(candidate)
                                )
                        )
                        .thenComparing(
                                Comparator.<GridPos>comparingDouble(
                                        candidate ->
                                            horizontalDistance(
                                                center(candidate),
                                                crystal
                                            )
                                ).reversed()
                        )
                        .thenComparing(
                                Comparator.naturalOrder()
                        ));
        if (finalDestination.isPresent()) {
            return finalDestination;
        }
        return candidates.stream()
                .min(Comparator
                        .<GridPos>comparingDouble(candidate ->
                                horizontalDistance(
                                        center(candidate),
                                        crystal
                                )
                        )
                        .reversed()
                        .thenComparingDouble(candidate ->
                                horizontalDistance(
                                        currentCenter,
                                        center(candidate)
                                )
                        )
                        .thenComparing(Comparator.naturalOrder()));
    }

    static boolean authorizesAggregateRisk(
            final CoreSkillFrame frame,
            final MoveToParameters target,
            final PerceptionVec3 crystal
    ) {
        final double currentDistance = horizontalDistance(
                frame.position(),
                crystal
        );
        final double targetDistance = horizontalDistance(
                target.target(),
                crystal
        );
        return frame.onGround()
                && !frame.inWater()
                && frame.health() / frame.maxHealth() >= 0.50
                && currentDistance < MINIMUM_FIRE_DISTANCE
                && targetDistance
                    >= currentDistance + MINIMUM_DISTANCE_IMPROVEMENT
                && safetyImprovingDirection(
                        frame.position(),
                        target.target(),
                        horizontalDirection(
                                crystal,
                                frame.position()
                        )
                );
    }

    static double horizontalDistance(
            final PerceptionVec3 first,
            final PerceptionVec3 second
    ) {
        return Math.hypot(
                first.x() - second.x(),
                first.z() - second.z()
        );
    }

    private static boolean safetyImprovingDirection(
            final PerceptionVec3 current,
            final PerceptionVec3 target,
            final PerceptionVec3 away
    ) {
        final PerceptionVec3 movement = new PerceptionVec3(
                target.x() - current.x(),
                0.0,
                target.z() - current.z()
        );
        return movement.dot(away) > 0.0;
    }

    private static PerceptionVec3 horizontalDirection(
            final PerceptionVec3 from,
            final PerceptionVec3 to
    ) {
        final PerceptionVec3 direction = new PerceptionVec3(
                to.x() - from.x(),
                0.0,
                to.z() - from.z()
        );
        return direction.lengthSquared() <= 1.0E-12
                ? direction
                : direction.normalized();
    }

    private static boolean safeDryStanding(
            final CoreSkillFrame frame,
            final GridPos feet,
            final double maximumDanger
    ) {
        final Optional<ObservedVoxel> feetVoxel =
                frame.navigation().voxelAt(feet);
        final Optional<ObservedVoxel> headVoxel =
                frame.navigation().voxelAt(feet.above());
        final Optional<ObservedVoxel> supportVoxel =
                frame.navigation().voxelAt(feet.below());
        return feetVoxel.isPresent()
                && headVoxel.isPresent()
                && supportVoxel.isPresent()
                && dryPassable(
                        feetVoxel.orElseThrow(),
                        maximumDanger
                )
                && dryPassable(
                        headVoxel.orElseThrow(),
                        maximumDanger
                )
                && supportVoxel.orElseThrow()
                    .kind().supportsWeight()
                && supportVoxel.orElseThrow()
                    .effectiveDanger() <= maximumDanger;
    }

    private static boolean dryPassable(
            final ObservedVoxel voxel,
            final double maximumDanger
    ) {
        final VoxelKind kind = voxel.kind();
        return kind.isPassable()
                && !kind.isLiquid()
                && kind != VoxelKind.LAVA
                && voxel.effectiveDanger() <= maximumDanger;
    }

    private static PerceptionVec3 center(
            final GridPos position
    ) {
        return new PerceptionVec3(
                position.x() + 0.5,
                position.y(),
                position.z() + 0.5
        );
    }
}
