package dev.mcai.companion.skills.bridging;

import dev.mcai.companion.perception.PerceptionVec3;
import dev.mcai.companion.waypoint.DimensionRef;
import java.util.Objects;

public record BridgeToParameters(
        DimensionRef dimension,
        double x,
        double y,
        double z,
        double arrivalRadius,
        int maxBlocks
) {
    public BridgeToParameters {
        Objects.requireNonNull(dimension, "dimension");
        if (!Double.isFinite(x)
                || !Double.isFinite(y)
                || !Double.isFinite(z)
                || !Double.isFinite(arrivalRadius)
                || arrivalRadius < 0.5
                || arrivalRadius > 2.0
                || maxBlocks < 1
                || maxBlocks > 64) {
            throw new IllegalArgumentException(
                    "Invalid bridge target"
            );
        }
    }

    public PerceptionVec3 target() {
        return new PerceptionVec3(x, y, z);
    }
}
