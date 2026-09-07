package dev.mcai.companion.agent.navigation;

import java.util.Objects;
import java.util.OptionalDouble;

public sealed interface NavigationTarget permits NavigationTarget.Coordinates,
        NavigationTarget.Named, NavigationTarget.Landmark {
    enum Kind {
        COORDINATES,
        PLAYER,
        ENTITY,
        DROPPED_ITEM,
        WORLD_SPAWN,
        RESPAWN_POINT,
        DEATH_POINT
    }

    Kind kind();

    record Coordinates(
            String dimension,
            double x,
            double y,
            double z,
            double acceptanceRadius,
            OptionalDouble arrivalHeading
    ) implements NavigationTarget {
        public Coordinates {
            Objects.requireNonNull(dimension, "dimension");
            Objects.requireNonNull(arrivalHeading, "arrivalHeading");
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || !Double.isFinite(acceptanceRadius)
                    || acceptanceRadius < 0.5 || acceptanceRadius > 32.0) {
                throw new IllegalArgumentException("Coordinates must be finite");
            }
            arrivalHeading.ifPresent(NavigationTarget::validateHeading);
        }

        @Override
        public Kind kind() {
            return Kind.COORDINATES;
        }
    }

    record Named(
            Kind kind,
            String name,
            double acceptanceRadius,
            OptionalDouble arrivalHeading
    ) implements NavigationTarget {
        public Named {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(arrivalHeading, "arrivalHeading");
            if (kind != Kind.PLAYER && kind != Kind.ENTITY && kind != Kind.DROPPED_ITEM) {
                throw new IllegalArgumentException("Named targets must be PLAYER or ENTITY");
            }
            if (name.isBlank() || !Double.isFinite(acceptanceRadius)
                    || acceptanceRadius < 0.5 || acceptanceRadius > 32.0) {
                throw new IllegalArgumentException("Invalid named target");
            }
            arrivalHeading.ifPresent(NavigationTarget::validateHeading);
        }
    }

    record Landmark(
            Kind kind,
            double acceptanceRadius,
            OptionalDouble arrivalHeading
    ) implements NavigationTarget {
        public Landmark {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(arrivalHeading, "arrivalHeading");
            if (kind != Kind.WORLD_SPAWN && kind != Kind.RESPAWN_POINT
                    && kind != Kind.DEATH_POINT) {
                throw new IllegalArgumentException("Invalid landmark target kind");
            }
            if (!Double.isFinite(acceptanceRadius)
                    || acceptanceRadius < 0.5 || acceptanceRadius > 32.0) {
                throw new IllegalArgumentException("Invalid acceptance radius");
            }
            arrivalHeading.ifPresent(NavigationTarget::validateHeading);
        }
    }

    private static void validateHeading(double heading) {
        if (!Double.isFinite(heading) || heading < 0.0 || heading >= 360.0) {
            throw new IllegalArgumentException("Heading must be in [0, 360)");
        }
    }
}
