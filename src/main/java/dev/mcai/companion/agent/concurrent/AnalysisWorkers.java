package dev.mcai.companion.agent.concurrent;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/** Bounded shared CPU workers. Submitted work must own snapshots, never live worlds. */
public final class AnalysisWorkers implements AutoCloseable {
    private static final int THREADS = Math.clamp(Integer.getInteger("minepilot.analysisThreads",
            Runtime.getRuntime().availableProcessors()>3 ? 2 : 1),1,4);
    private static final AtomicInteger IDS = new AtomicInteger();
    private static final ThreadPoolExecutor POOL = new ThreadPoolExecutor(THREADS,THREADS,30,TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(32), task -> {
                var thread=new Thread(task,"minepilot-analysis-"+IDS.incrementAndGet());
                thread.setDaemon(true);thread.setPriority(Thread.NORM_PRIORITY-1);return thread;
            },new ThreadPoolExecutor.AbortPolicy());
    static { POOL.allowCoreThreadTimeOut(true); }
    private final Set<CompletableFuture<?>> pending = ConcurrentHashMap.newKeySet();
    private boolean closed;
    public synchronized <T> CompletableFuture<T> submit(Supplier<T> work) {
        if(closed)return CompletableFuture.failedFuture(new RejectedExecutionException("Analysis owner closed"));
        if(pending.size()>=4)return CompletableFuture.failedFuture(new RejectedExecutionException("Analysis owner queue full"));
        var result=new CompletableFuture<T>();
        var task=new FutureTask<Void>(() -> {
            if(!result.isCancelled())try {result.complete(work.get());}catch(Throwable error){result.completeExceptionally(error);}
            return null;
        });
        pending.add(result);
        result.whenComplete((value,error)->{pending.remove(result);if(result.isCancelled()){task.cancel(true);POOL.remove(task);}});
        try{POOL.execute(task);}catch(RejectedExecutionException full){result.completeExceptionally(full);}
        return result;
    }
    public static int workerCount(){return THREADS;}
    public static int queued(){return POOL.getQueue().size();}
    public static int active(){return POOL.getActiveCount();}
    @Override public synchronized void close(){closed=true;for(var task:pending)task.cancel(true);}
}
