package dev.mcai.companion.agent.navigation;

import java.util.Objects;
import java.util.UUID;

public record NavigationIntent(
        UUID requestId,
        long worldRevision,
        NavigationTarget target,
        TravelPace preferredPace,
        String playerIntent,
        String requestedBy,
        boolean allowPartial,
        boolean continuousFollow
) {
    public NavigationIntent(UUID requestId, long worldRevision, NavigationTarget target,
                            TravelPace preferredPace, String playerIntent, String requestedBy) {
        this(requestId, worldRevision, target, preferredPace, playerIntent, requestedBy, false, false);
    }
    public NavigationIntent(UUID requestId, long worldRevision, NavigationTarget target,
                            TravelPace preferredPace, String playerIntent, String requestedBy, boolean allowPartial) {
        this(requestId, worldRevision, target, preferredPace, playerIntent, requestedBy, allowPartial, false);
    }
    public NavigationIntent {
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(preferredPace, "preferredPace");
        Objects.requireNonNull(playerIntent, "playerIntent");
        Objects.requireNonNull(requestedBy, "requestedBy");
        // Player rendezvous is a one-shot proximity goal, independent of a
        // model-supplied precise radius. Entity/coordinate goals retain theirs.
        if (!continuousFollow && target instanceof NavigationTarget.Named named
                && named.kind() == NavigationTarget.Kind.PLAYER) {
            target = new NavigationTarget.Named(named.kind(), named.name(), 3.0, named.arrivalHeading());
        }
        if (continuousFollow && target.kind()!=NavigationTarget.Kind.PLAYER && target.kind()!=NavigationTarget.Kind.ENTITY) {
            throw new IllegalArgumentException("Continuous follow requires a player or living-entity target");
        }
        if (playerIntent.isBlank() || requestedBy.isBlank()) {
            throw new IllegalArgumentException("Intent and requester are required");
        }
    }
}
