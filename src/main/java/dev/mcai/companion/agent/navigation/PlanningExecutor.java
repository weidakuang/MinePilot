package dev.mcai.companion.agent.navigation;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;

/** Dedicated, bounded CPU pool for route planning. */
public final class PlanningExecutor implements AutoCloseable {
    private final AnytimeNavigationPlanner planner;
    private final dev.mcai.companion.agent.concurrent.AnalysisWorkers executor=new dev.mcai.companion.agent.concurrent.AnalysisWorkers();
    public PlanningExecutor(NavigationPlannerConfig config) {planner=new AnytimeNavigationPlanner(Objects.requireNonNull(config));}
    /** Finish snapshot math and map ownership on the same bounded worker as A*. */
    public CompletableFuture<NavigationPlan> submit(UUID id,NavigationSnapshotBuilder.Capture capture,boolean partial) {
        return executor.submit(()->planner.plan(id,capture.finish(),partial));
    }
    public CompletableFuture<NavigationPlan> submitAny(UUID id,NavigationSnapshotBuilder.Capture capture,
            java.util.List<NavigationPlan.ResolvedDestination> destinations) {
        var goals=java.util.List.copyOf(destinations);
        return executor.submit(()->planner.planAny(id,capture.finish(),goals));
    }
    public CompletableFuture<NavigationPlan> submit(
            UUID requestId,
            NavigationWorldSnapshot snapshot
    ) {
        return submit(requestId, snapshot, false);
    }

    public CompletableFuture<NavigationPlan> submit(UUID requestId, NavigationWorldSnapshot snapshot, boolean allowPartial) {
        try {
            return executor.submit(() -> planner.plan(requestId, snapshot, allowPartial));
        } catch (RejectedExecutionException rejected) {
            return CompletableFuture.failedFuture(new PlanningBusyException(
                    "Navigation planner queue is full", rejected));
        }
    }

    @Override
    public void close() {
        executor.close();
    }

    public CompletableFuture<NavigationPlan> submitAny(UUID requestId,NavigationWorldSnapshot snapshot,
            java.util.List<NavigationPlan.ResolvedDestination> destinations) {
        var goals=java.util.List.copyOf(destinations);
        try{return executor.submit(()->planner.planAny(requestId,snapshot,goals));}
        catch(RejectedExecutionException rejected){return CompletableFuture.failedFuture(new PlanningBusyException("Navigation planner queue is full",rejected));}
    }

    public static final class PlanningBusyException extends RuntimeException {
        public PlanningBusyException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
