package dev.mcai.companion.agent.navigation;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record NavigationEvent(
        Type type,
        UUID requestId,
        String message,
        Map<String, Object> observedState,
        List<String> validActions
) {
    public NavigationEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(message, "message");
        observedState = Map.copyOf(observedState);
        validActions = List.copyOf(validActions);
    }

    public enum Type {
        NAVIGATION_ACCEPTED,
        NAVIGATION_PLAN_READY,
        NAVIGATION_STARTED,
        NAVIGATION_PROGRESS,
        NAVIGATION_DECISION_REQUIRED,
        NAVIGATION_REPLANNED,
        NAVIGATION_COMPLETED,
        NAVIGATION_FOLLOWING,
        NAVIGATION_APPROACHED,
        NAVIGATION_FAILED,
        NAVIGATION_CANCELLED
    }
}
