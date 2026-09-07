package dev.mcai.companion.agent.navigation;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record RouteOption(
        String optionId,
        String label,
        TravelPace suggestedPace,
        Set<TravelPace> supportedPaces,
        double estimatedSeconds,
        double distanceBlocks,
        double estimatedExhaustion,
        double estimatedFoodPointsLost,
        double estimatedHealthLost,
        int supportBlocksRequired,
        double riskScore,
        boolean feasibleNow,
        List<String> hazards,
        List<String> requiredActions,
        List<PathStep> steps,
        List<SupportMaterial> supportMaterials
) {
    public RouteOption {
        Objects.requireNonNull(optionId, "optionId");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(suggestedPace, "suggestedPace");
        supportedPaces = Set.copyOf(supportedPaces);
        hazards = List.copyOf(hazards);
        requiredActions = List.copyOf(requiredActions);
        steps = List.copyOf(steps);
        supportMaterials = List.copyOf(supportMaterials);
        if (supportMaterials.stream().mapToInt(SupportMaterial::count).sum() != supportBlocksRequired)
            throw new IllegalArgumentException("Support material manifest must match the route budget");
        if (optionId.isBlank() || label.isBlank() || estimatedSeconds < 0.0
                || distanceBlocks < 0.0 || estimatedExhaustion < 0.0
                || estimatedFoodPointsLost < 0.0 || estimatedHealthLost < 0.0
                || supportBlocksRequired < 0 || riskScore < 0.0
                || supportedPaces.isEmpty() || !supportedPaces.contains(suggestedPace)) {
            throw new IllegalArgumentException("Invalid route estimates");
        }
    }

    public record SupportMaterial(String entryId, String item, int count, int importance) {
        public SupportMaterial {
            Objects.requireNonNull(entryId); Objects.requireNonNull(item);
            if(count < 1 || importance < 3 || importance > 5) throw new IllegalArgumentException("Protected/invalid support material");
        }
    }

    public record PathStep(
            double x,
            double y,
            double z,
            Action action,
            TravelPace recommendedPace,
            float yawTolerance
    ) {
        public PathStep {
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(recommendedPace, "recommendedPace");
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                throw new IllegalArgumentException("Path coordinates must be finite");
            }
            if (!Float.isFinite(yawTolerance) || yawTolerance < 1.0F || yawTolerance > 90.0F) {
                throw new IllegalArgumentException("Invalid yaw tolerance");
            }
        }
    }

    public enum Action {
        WALK,
        JUMP,
        GAP_JUMP,
        CLIMB,
        STEP_DOWN,
        DROP,
        SWIM,
        SNEAK_EDGE,
        OPEN_DOOR,
        PLACE_SUPPORT,
        WAIT_FOR_CLEARANCE
    }
}
