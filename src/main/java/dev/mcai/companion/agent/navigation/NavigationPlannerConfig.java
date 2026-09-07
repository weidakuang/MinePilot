package dev.mcai.companion.agent.navigation;

public record NavigationPlannerConfig(
        int maximumRouteOptions,
        int workerThreads,
        int maximumQueuedPlans,
        int maximumExpandedNodes,
        long planningBudgetMillis,
        int maximumDropBlocks
) {
    public NavigationPlannerConfig {
        if (maximumRouteOptions < 1 || maximumRouteOptions > 8) {
            throw new IllegalArgumentException("maximumRouteOptions must be in [1, 8]");
        }
        if (workerThreads < 1 || workerThreads > 2) {
            throw new IllegalArgumentException("workerThreads must be 1 or 2");
        }
        if (maximumQueuedPlans < 1 || maximumQueuedPlans > 16) {
            throw new IllegalArgumentException("maximumQueuedPlans must be in [1, 16]");
        }
        if (maximumExpandedNodes < 128 || maximumExpandedNodes > 500_000
                || planningBudgetMillis < 5 || planningBudgetMillis > 2_000
                || maximumDropBlocks < 1 || maximumDropBlocks > 32) {
            throw new IllegalArgumentException("Invalid planning limit");
        }
    }

    public static NavigationPlannerConfig defaults() {
        return new NavigationPlannerConfig(5, 1, 2, 80_000, 150, 12);
    }
}
