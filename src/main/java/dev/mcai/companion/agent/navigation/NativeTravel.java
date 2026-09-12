package dev.mcai.companion.agent.navigation;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import dev.mcai.companion.agent.AgentRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Task adapter for the existing snapshot planner/follower, including bounded long trips. */
public final class NativeTravel implements AutoCloseable {
    private final AgentRuntime runtime;private final double acceptanceRadius;private final boolean failWithoutProgress;
    private final PlanningExecutor planner=new PlanningExecutor(NavigationPlannerConfig.defaults());
    private final NavigationSnapshotBuilder snapshots=new NavigationSnapshotBuilder(NavigationSnapshotBuilder.CaptureConfig.defaults());
    private final NavigationFollower follower;
    private NavigationSnapshotBuilder.Capture capture;
    private CompletableFuture<NavigationPlan> pending;
    private Vec3 goal,leg,start;
    private NavigationEvent event;
    private String phase="IDLE",reason="";
    private int repairs;
    public NativeTravel(AgentRuntime runtime){this(runtime,.35);}
    public NativeTravel(AgentRuntime runtime,double acceptanceRadius){this(runtime,acceptanceRadius,false);}
    public NativeTravel(AgentRuntime runtime,double acceptanceRadius,boolean failWithoutProgress){this.runtime=runtime;this.acceptanceRadius=acceptanceRadius;this.failWithoutProgress=failWithoutProgress;follower=new NavigationFollower(runtime.player(),e->event=e,id->leg!=null && runtime.player().position().distanceTo(leg)>acceptanceRadius?"Move fully into the native work approach":null,why->followerInvalidated(why));}
    private void followerInvalidated(String why){follower.requestReplan(why);}
    public String phase(){return phase;} public String reason(){return reason;}
    public void start(Vec3 target){cancel();goal=target;repairs=0;planLeg();}
    private void planLeg(){
        var p=runtime.player();start=p.position();leg=goal;
        double distance=Math.hypot(goal.x-start.x,goal.z-start.z);
        if(distance>48){
            var middle=start.lerp(goal,40/distance);int x=(int)Math.floor(middle.x),z=(int)Math.floor(middle.z);
            if(!p.level().hasChunk(x>>4,z>>4)){fail("Intermediate travel column is not loaded");return;}
            int y=p.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
            leg=new Vec3(x+.5,y,z+.5);
        }
        var destination=new NavigationPlan.ResolvedDestination(p.level().dimension().identifier().toString(),leg.x,leg.y,leg.z,Math.max(.5,acceptanceRadius),false,"native-task",OptionalDouble.empty(),Optional.empty());
        try{capture=snapshots.begin(p,destination,0);phase="CAPTURING";reason="";p.stopControlling();}
        catch(RuntimeException failure){fail(failure.getMessage());}
    }
    public void beforePhysics(){if(phase.equals("TRAVELLING"))follower.beforePhysicsTick();}
    public void tick(){
        var p=runtime.player();
        if(phase.equals("CAPTURING")){
            if(capture.advance(2_000_000)){pending=planner.submit(UUID.randomUUID(),capture.finish());capture=null;phase="PLANNING";}return;
        }
        if(phase.equals("PLANNING")){
            if(!pending.isDone())return;
            try{var plan=pending.join();pending=null;
                if(p.position().distanceToSqr(start)>.16){if(repairs++<2){planLeg();return;}fail("Body moved while its route was captured");return;}
                var option=plan.options().stream().filter(o->o.feasibleNow() && o.supportBlocksRequired()==0 && o.estimatedHealthLost()==0 && o.hazards().stream().allMatch("water traversal"::equals)).min(Comparator.comparingDouble(RouteOption::estimatedSeconds)).orElse(null);
                if(option==null){fail("No evaluated route without support consumption or predicted damage");return;}
                event=null;follower.start(plan,option,TravelPace.AUTO,OptionalDouble.empty());phase="TRAVELLING";
            }catch(RuntimeException failure){fail(String.valueOf(failure.getMessage()));}return;
        }
        if(!phase.equals("TRAVELLING"))return;follower.tick();
        if(event==null)return;var finished=event;event=null;
        if(finished.type()==NavigationEvent.Type.NAVIGATION_COMPLETED){
            if(p.position().distanceTo(goal)<=acceptanceRadius+.05){phase="COMPLETED";p.stopControlling();}
            else if(!leg.equals(goal)){planLeg();}else fail("Arrival did not match the actual destination");
        }else if(Set.of(NavigationEvent.Type.NAVIGATION_FAILED,NavigationEvent.Type.NAVIGATION_DECISION_REQUIRED,NavigationEvent.Type.NAVIGATION_CANCELLED).contains(finished.type())){
            // Re-capturing the same blocked entrance produces the same route.
            // Let the owning recovery ladder try another checked approach when
            // this leg made no physical progress, instead of repeating it.
            if((!failWithoutProgress || p.position().distanceToSqr(start)>.0625) && repairs++<2){follower.cancel("Task route revalidation");planLeg();}else fail(finished.message());
        }
    }
    private void fail(String why){phase="FAILED";reason=why;runtime.player().stopControlling();}
    public void cancel(){if(pending!=null)pending.cancel(true);pending=null;capture=null;follower.cancel("Task travel interrupted");event=null;phase="IDLE";}
    @Override public void close(){cancel();planner.close();}
}
