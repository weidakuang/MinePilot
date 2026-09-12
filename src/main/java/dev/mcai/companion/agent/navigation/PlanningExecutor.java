package dev.mcai.companion.agent.navigation;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Dedicated, bounded CPU pool for route planning. */
public final class PlanningExecutor implements AutoCloseable {
    private final AnytimeNavigationPlanner planner;
    private final ThreadPoolExecutor executor;

    public PlanningExecutor(NavigationPlannerConfig config) {
        Objects.requireNonNull(config, "config");
        planner = new AnytimeNavigationPlanner(config);
        AtomicInteger sequence = new AtomicInteger();
        ThreadFactory threads = task -> {
            Thread thread = new Thread(task,
                    "minepilot-navigation-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            thread.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 1));
            return thread;
        };
        executor = new ThreadPoolExecutor(
                config.workerThreads(),
                config.workerThreads(),
                30L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(config.maximumQueuedPlans()),
                threads,
                new ThreadPoolExecutor.AbortPolicy()
        );
        executor.allowCoreThreadTimeOut(true);
    }

    public CompletableFuture<NavigationPlan> submit(
            UUID requestId,
            NavigationWorldSnapshot snapshot
    ) {
        return submit(requestId, snapshot, false);
    }

    public CompletableFuture<NavigationPlan> submit(UUID requestId, NavigationWorldSnapshot snapshot, boolean allowPartial) {
        try {
            return CompletableFuture.supplyAsync(
                    () -> planner.plan(requestId, snapshot, allowPartial), executor);
        } catch (RejectedExecutionException rejected) {
            return CompletableFuture.failedFuture(new PlanningBusyException(
                    "Navigation planner queue is full", rejected));
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    public CompletableFuture<NavigationPlan> submitAny(UUID requestId,NavigationWorldSnapshot snapshot,
            java.util.List<NavigationPlan.ResolvedDestination> destinations) {
        var goals=java.util.List.copyOf(destinations);
        try{return CompletableFuture.supplyAsync(()->planner.planAny(requestId,snapshot,goals),executor);}
        catch(RejectedExecutionException rejected){return CompletableFuture.failedFuture(new PlanningBusyException("Navigation planner queue is full",rejected));}
    }

    public static final class PlanningBusyException extends RuntimeException {
        public PlanningBusyException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
