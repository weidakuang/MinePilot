package dev.mcai.companion.agent.navigation;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

public record NavigationPlan(
        UUID requestId,
        long plannedWorldRevision,
        Instant createdAt,
        ResolvedDestination destination,
        List<RouteOption> options,
        Optional<ResolvedDestination> partialDestination
) {
    public NavigationPlan(UUID requestId, long plannedWorldRevision, Instant createdAt,
                          ResolvedDestination destination, List<RouteOption> options) {
        this(requestId, plannedWorldRevision, createdAt, destination, options, Optional.empty());
    }
    public NavigationPlan {
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(partialDestination, "partialDestination");
        options = options.stream()
                .sorted(Comparator.comparing(RouteOption::optionId))
                .toList();
        if (options.isEmpty() || options.size() > 8) {
            throw new IllegalArgumentException("A plan must contain one to eight routes");
        }
    }

    public RouteOption requireOption(String optionId) {
        return options.stream()
                .filter(option -> option.optionId().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown route option: " + optionId));
    }

    public record ResolvedDestination(
            String dimension,
            double x,
            double y,
            double z,
            double acceptanceRadius,
            boolean dynamic,
            String targetIdentity,
            OptionalDouble arrivalHeading,
            Optional<TargetMotion> targetMotion
    ) {
        public ResolvedDestination {
            Objects.requireNonNull(dimension, "dimension");
            Objects.requireNonNull(targetIdentity, "targetIdentity");
            Objects.requireNonNull(arrivalHeading, "arrivalHeading");
            Objects.requireNonNull(targetMotion, "targetMotion");
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || !Double.isFinite(acceptanceRadius) || acceptanceRadius < 0.5) {
                throw new IllegalArgumentException("Invalid resolved destination");
            }
            arrivalHeading.ifPresent(heading -> {
                if (!Double.isFinite(heading) || heading < 0.0 || heading >= 360.0) {
                    throw new IllegalArgumentException("Arrival heading must be in [0, 360)");
                }
            });
            if (!dynamic && targetMotion.isPresent()) {
                throw new IllegalArgumentException("Only dynamic targets may have motion");
            }
        }
    }

    public record TargetMotion(
            long observedGameTick,
            double velocityX,
            double velocityY,
            double velocityZ
    ) {
        public TargetMotion {
            if (!Double.isFinite(velocityX) || !Double.isFinite(velocityY)
                    || !Double.isFinite(velocityZ)) {
                throw new IllegalArgumentException("Invalid target velocity");
            }
        }
    }
}
