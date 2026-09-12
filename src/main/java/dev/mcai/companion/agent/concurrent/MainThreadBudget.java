package dev.mcai.companion.agent.concurrent;

import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;

/** Shared cooperative budget for expensive world reads; normal physics never waits on it. */
public final class MainThreadBudget {
    private static final WeakHashMap<MinecraftServer,MainThreadBudget> SERVERS=new WeakHashMap<>();
    private static final long LIMIT=Math.clamp(Long.getLong("minepilot.worldReadBudgetMicros",3000),500,10000)*1000;
    private int tick=Integer.MIN_VALUE;
    private long used,peak;
    public static MainThreadBudget of(MinecraftServer server){
        if(!server.isSameThread())throw new IllegalStateException("World reads belong to server thread");
        var budget=SERVERS.computeIfAbsent(server,s->new MainThreadBudget());
        if(budget.tick!=server.getTickCount()){budget.tick=server.getTickCount();budget.used=0;}
        return budget;
    }
    public long deadline(long maximum){return System.nanoTime()+Math.min(Math.max(0,maximum),Math.max(0,LIMIT-used));}
    public void record(long began){used+=Math.max(0,System.nanoTime()-began);peak=Math.max(peak,used);}
    public long usedNanos(){return used;}
    public long peakNanos(){return peak;}
    public long limitNanos(){return LIMIT;}
}
