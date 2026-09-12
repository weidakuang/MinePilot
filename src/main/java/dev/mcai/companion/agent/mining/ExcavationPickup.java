package dev.mcai.companion.agent.mining;

import com.google.gson.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.knowledge.DropProvenance;
import dev.mcai.companion.agent.navigation.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.*;

/** Physical pickup of this excavation's counted origins. Never credits unrelated merged units. */
final class ExcavationPickup implements AutoCloseable {
    private final AgentRuntime r;
    private final PlanningExecutor planner=new PlanningExecutor(NavigationPlannerConfig.defaults());
    private final NavigationSnapshotBuilder snapshots=new NavigationSnapshotBuilder(new NavigationSnapshotBuilder.CaptureConfig(4,4,6,48,24,50_000,8));
    private final NavigationFollower follower;
    private final Map<String,Integer> emitted=new LinkedHashMap<>(),received=new LinkedHashMap<>();
    private final Set<String> recorded=new HashSet<>();
    private final JsonArray receipts=new JsonArray();
    private final List<BlockPos> approaches=new ArrayList<>();
    private CompletableFuture<NavigationPlan> pending;
    private NavigationSnapshotBuilder.Capture capture;
    private NavigationEvent event;
    private ItemEntity drop;
    private Vec3 start;
    private long cursor;
    private String phase="IDLE",reason="",routeFailure="";
    private int waitUntil,attempts;
    private double budget;
    ExcavationPickup(AgentRuntime r){this.r=r;follower=new NavigationFollower(r.player(),e->{if(e.type()!=NavigationEvent.Type.NAVIGATION_PROGRESS)event=e;},id->null,this::replan);}
    private void replan(String why){follower.requestReplan(why);}
    void reset(){cancel();emitted.clear();received.clear();recorded.clear();receipts.asList().clear();cursor=r.player().inventoryLedger.events(0,1).get("latestSequence").getAsLong();phase="IDLE";reason="";}
    void record(JsonObject mining){
        if(!mining.has("requestId") || !mining.get("phase").getAsString().equals("COMPLETED"))return;
        String id=mining.get("requestId").getAsString();if(!recorded.add(id))return;
        for(var value:mining.getAsJsonArray("emittedDrops")){var row=value.getAsJsonObject();emitted.merge(id+"|"+row.get("item").getAsString(),row.get("count").getAsInt(),Integer::sum);}
    }
    void observe(){
        var batch=r.player().inventoryLedger.events(cursor,32);
        if(batch.get("historyLost").getAsBoolean()){fail("Pickup history expired; cannot prove collection");return;}
        for(var value:batch.getAsJsonArray("events")){
            var event=value.getAsJsonObject();cursor=event.get("sequence").getAsLong();
            for(var acquired:event.getAsJsonArray("acquired")){
                var row=acquired.getAsJsonObject();var source=row.getAsJsonObject("source");if(!source.has("lineage"))continue;
                for(var segment:source.getAsJsonArray("lineage")){
                    var part=segment.getAsJsonObject();var origin=part.getAsJsonObject("source");if(!origin.has("requestId"))continue;
                    String key=origin.get("requestId").getAsString()+"|"+row.get("item").getAsString();
                    int n=Math.min(part.get("count").getAsInt(),Math.max(0,emitted.getOrDefault(key,0)-received.getOrDefault(key,0)));
                    if(n>0){received.merge(key,n,Integer::sum);if(receipts.size()<64){var receipt=new JsonObject();receipt.addProperty("item",row.get("item").getAsString());receipt.addProperty("count",n);receipt.add("origin",origin.deepCopy());receipts.add(receipt);}}
                }
            }
        }
    }
    boolean verified(){return !phase.equals("BLOCKED") && emitted.entrySet().stream().allMatch(e->received.getOrDefault(e.getKey(),0)>=e.getValue());}
    void beforePhysics(){if(phase.equals("TRAVEL"))follower.beforePhysicsTick();}
    void begin(){if(verified()){phase="COMPLETED";return;}phase="SELECT";attempts=0;}
    String phase(){return phase;}String reason(){return reason;}
    void tick(double remainingBudget){
        if(Set.of("IDLE","COMPLETED","BLOCKED").contains(phase))return;budget=remainingBudget;
        if(verified()){cancel();phase="COMPLETED";return;}
        if(budget<.5){fail("Excavation travel budget leaves no room for remaining pickup");return;}
        try{
            if(phase.equals("CAPTURING")){
                boolean done=capture.advance(1_500_000L);if(done){pending=planner.submit(UUID.randomUUID(),capture,false);capture=null;phase="PLANNING";}return;
            }
            if(phase.equals("PLANNING")){
                if(!pending.isDone())return;NavigationPlan plan;
                try{plan=pending.join();}catch(RuntimeException failure){routeFailure=String.valueOf(failure.getMessage());pending=null;route();return;}pending=null;
                if(r.player().position().distanceToSqr(start)>.04){fail("Body moved during pickup planning");return;}
                var option=plan.options().stream().filter(o->o.feasibleNow() && o.supportBlocksRequired()==0 && o.estimatedHealthLost()==0 && o.distanceBlocks()<=budget).min(Comparator.comparingDouble(RouteOption::distanceBlocks)).orElse(null);
                if(option==null){routeFailure="All evaluated options exceeded health/support/travel limits: "+plan.options().stream().map(o->o.optionId()+" feasible="+o.feasibleNow()+" health="+o.estimatedHealthLost()+" supports="+o.supportBlocksRequired()+" distance="+o.distanceBlocks()).toList();route();return;}follower.start(plan,option,TravelPace.AUTO,OptionalDouble.empty());phase="TRAVEL";event=null;return;
            }
            if(phase.equals("TRAVEL")){
                if(drop==null || !drop.isAlive()){cancel();phase="SELECT";return;}
                follower.tick();if(event!=null){var e=event;event=null;
                    if(e.type()==NavigationEvent.Type.NAVIGATION_COMPLETED){phase="WAIT";waitUntil=r.server().getTickCount()+15;}
                    else if(e.type()==NavigationEvent.Type.NAVIGATION_FAILED || e.type()==NavigationEvent.Type.NAVIGATION_DECISION_REQUIRED){if(++attempts>2){fail("Pickup approach obstructed: "+e.message());return;}cancel();phase="SELECT";}}
                return;
            }
            if(phase.equals("WAIT")){if(r.server().getTickCount()<waitUntil)return;if(drop!=null && drop.isAlive() && ++attempts>2){fail("Reached dropped items but pickup is restricted or no matching inventory capacity remains");return;}phase="SELECT";}
            if(phase.equals("SELECT"))select();
        }catch(RuntimeException failure){fail("Pickup stopped: "+failure.getMessage());}
    }
    private boolean own(ItemEntity item){
        String itemId=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).toString();
        for(var v:DropProvenance.segments(item,item.getItem().getCount())){var source=v.getAsJsonObject().getAsJsonObject("source");if(source.has("requestId")){String key=source.get("requestId").getAsString()+"|"+itemId;if(emitted.getOrDefault(key,0)>received.getOrDefault(key,0))return true;}}return false;
    }
    private void select(){
        drop=r.player().level().getEntitiesOfClass(ItemEntity.class,r.player().getBoundingBox().inflate(32),e->e.isAlive() && !dev.mcai.companion.agent.knowledge.DiscardedItems.avoided(e,r.player()) && r.perception.sensed(e) && own(e)).stream().min(Comparator.comparingDouble(e->e.distanceToSqr(r.player()))).orElse(null);
        if(drop==null){fail("Remaining excavation drops are no longer sensed: they may have merged, despawned, been taken, or need a new approach decision");return;}
        int capacity=0;for(int i=0;i<36;i++){var s=r.player().getInventory().getItem(i);if(s.isEmpty())capacity+=drop.getItem().getMaxStackSize();else if(net.minecraft.world.item.ItemStack.isSameItemSameComponents(s,drop.getItem()))capacity+=Math.max(0,s.getMaxStackSize()-s.getCount());}
        if(capacity==0){fail("Inventory is full for "+net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(drop.getItem().getItem())+"; organize inventory before another pickup decision");return;}
        approaches.clear();var base=drop.blockPosition();
        for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){
            var p=base.offset(x,y,z);if(!r.perception.observableBlock(p) || !r.player().level().isLoaded(p))continue;
            var feet=Vec3.atBottomCenterOf(p);var box=r.player().getBoundingBox().move(feet.subtract(r.player().position()));
            if(box.inflate(.7,.3,.7).intersects(drop.getBoundingBox()) && r.player().level().getBlockState(p.below()).isCollisionShapeFullBlock(r.player().level(),p.below()) && r.player().level().noCollision(r.player(),box))approaches.add(p);
        }
        approaches.sort(Comparator.comparingDouble(p->p.distToCenterSqr(drop.position())+.05*p.distToCenterSqr(r.player().position())));
        if(approaches.isEmpty() && !drop.onGround() && attempts++<8){phase="WAIT";waitUntil=r.server().getTickCount()+10;drop=null;return;}
        route();
    }
    private void route(){
        if(approaches.isEmpty()){fail("No evaluated safe standing route reaches the remaining drop; excavation is done but collection is incomplete. "+routeFailure);return;}
        var feet=Vec3.atBottomCenterOf(approaches.removeFirst());start=r.player().position();
        var destination=new NavigationPlan.ResolvedDestination(r.player().level().dimension().identifier().toString(),feet.x,feet.y,feet.z,.5,false,"excavation-loot",OptionalDouble.empty(),Optional.empty());
        capture=snapshots.begin(r.player(),destination,0);phase="CAPTURING";
    }
    void cancel(){capture=null;if(pending!=null){pending.cancel(true);pending=null;}follower.cancel("Excavation pickup interrupted");event=null;phase="IDLE";}
    private void fail(String why){cancel();phase="BLOCKED";reason=why;}
    JsonObject status(){var out=new JsonObject();out.addProperty("phase",phase);out.addProperty("reason",reason);out.addProperty("verified",verified());out.addProperty("emittedCount",emitted.values().stream().mapToInt(Integer::intValue).sum());out.addProperty("pickedUpCount",received.values().stream().mapToInt(Integer::intValue).sum());out.add("receipts",receipts.deepCopy());out.addProperty("proof","Counted native pickup receipts from this task's break origins; current carried origins are in inventory");return out;}
    @Override public void close(){cancel();planner.close();}
}
