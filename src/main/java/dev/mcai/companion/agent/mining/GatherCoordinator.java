package dev.mcai.companion.agent.mining;

import com.google.gson.*;
import java.util.*;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.knowledge.ResourceSearch;
import dev.mcai.companion.agent.navigation.NativeTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** One resource request: find an exposed source, approach, mine and verify pickup. */
public final class GatherCoordinator implements AutoCloseable {
    private final AgentRuntime runtime;private NativeTravel travel;private TerrainStepRecovery access;
    private String phase="IDLE",step="",reason="",resource,output,source="",cursor="",dimension;
    private int wanted,received,radius,started,explorations;private double walked;private Vec3 previous;
    private UUID request;private BlockPos target;private JsonObject lastSearch=new JsonObject();
    private BlockPos timedTarget;private int targetStarted,targetBudget;private double bestTargetDistance;
    private dev.mcai.companion.vendor.numen.task.RecoveryLadder<Vec3,String> approaches;
    private final Set<String> children=new HashSet<>();private final Map<BlockPos,Integer> failures=new HashMap<>();private final Set<BlockPos> visited=new HashSet<>();
    public GatherCoordinator(AgentRuntime runtime){this.runtime=runtime;}
    public boolean active(){return phase.equals("EXECUTING");}
    public boolean isChild(JsonObject state){return state.has("requestId") && children.contains(state.get("requestId").getAsString());}
    public boolean isMiningChild(JsonObject state){return access!=null && access.isChild(state);}
    public JsonObject start(String resource,String output,int count,int radius){
        dev.mcai.companion.agent.survival.SurvivalTools.requireIdle(runtime);
        if(active())throw new IllegalStateException("Gather task is already running");if(count<1 || count>64 || radius<1 || radius>150)throw new IllegalArgumentException("Gather count 1..64, radius 1..150 required");
        resource=ResourceSearch.normalize(resource);
        // Open-ended acquisition names the desired item, unlike exact block work.
        // In particular, cobblestone should come from natural stone, not a village wall.
        String sourceBlock=switch(resource){case "minecraft:cobblestone"->"minecraft:stone";case "minecraft:cobbled_deepslate"->"minecraft:deepslate";case "minecraft:raw_iron"->"minecraft:iron_ore";case "minecraft:raw_gold"->"minecraft:gold_ore";case "minecraft:raw_copper"->"minecraft:copper_ore";case "minecraft:coal"->"minecraft:coal_ore";case "minecraft:diamond"->"minecraft:diamond_ore";case "minecraft:redstone"->"minecraft:redstone_ore";default->resource;};
        if(!sourceBlock.equals(resource) && output.isBlank())output=resource;
        this.resource=sourceBlock;this.output=output;this.wanted=count;this.radius=radius;received=0;walked=0;previous=runtime.player().position();started=runtime.server().getTickCount();explorations=0;request=UUID.randomUUID();phase="EXECUTING";step="LOCAL";reason="";cursor="";target=null;children.clear();failures.clear();visited.clear();dimension=runtime.player().level().dimension().identifier().toString();
        timedTarget=null;
        if(travel==null)travel=new NativeTravel(runtime,.35,true);if(access==null)access=new TerrainStepRecovery(runtime,travel);access.reset();return status();
    }
    public void beforePhysics(){if(active() && travel!=null)travel.beforePhysics();}
    public void tick(){
        if(!active())return;var p=runtime.player();walked+=previous.distanceTo(p.position());previous=p.position();
        if(!p.isAlive() || !dimension.equals(p.level().dimension().identifier().toString()) || walked>512 || runtime.server().getTickCount()-started>7200){block("Gather stopped at its life/dimension/time/travel boundary; partial pickups are retained");return;}
        try{
            if(target!=null){
                double remaining=p.position().distanceTo(Vec3.atCenterOf(target));
                if(remaining+.5<bestTargetDistance){bestTargetDistance=remaining;targetStarted=runtime.server().getTickCount();}
                // Digging a real access passage is progress, not idle planning.
                // Its mining coordinator and 40-second step budget govern it.
                if(step.equals("TERRAIN_STEP") && rMiningActive())targetStarted++;
            }
            if(target!=null && (step.equals("APPROACH") || step.equals("TERRAIN_STEP")) && runtime.server().getTickCount()-targetStarted>targetBudget){
                rejectArea(target,2);access.cancel();target=null;step="SEARCH";cursor="";reason="This approach used its bounded time without a pickup; trying another observed candidate";
            }
            if(step.equals("TERRAIN_STEP")){String state=access.tick();if(state.equals("COMPLETED")){target=null;step="LOCAL";cursor="";}else if(state.equals("FAILED")){step="SEARCH";cursor="";}return;}
            if(step.equals("LOCAL")){if(beginCollection())return;if(target!=null)rejectArea(target,0);target=null;step="SEARCH";}
            if(step.equals("COLLECT")){
                var child=runtime.collection().status();String state=child.get("phase").getAsString();if(Set.of("EXECUTING","PAUSED").contains(state))return;
                int gained=child.has("verifiedInventoryIncrease")?child.get("verifiedInventoryIncrease").getAsInt():0;
                received+=gained;
                if(received>=wanted){phase="COMPLETED";reason="Native collection receipts account for the requested new inventory items";return;}
                // A successful local tranche can contain fewer items than the
                // open-ended request. Re-scan from the new position; excluding
                // its entire area would discard the adjacent remaining ore.
                if(gained>0){target=null;step="LOCAL";cursor="";reason="";return;}
                if(target!=null)rejectArea(target,6);else rejectArea(p.blockPosition(),9);
                if(child.get("reason").getAsString().toLowerCase(Locale.ROOT).matches(".*(tool|durability|inventory.*full).*")){block(child.get("reason").getAsString());return;}
                step="SEARCH";cursor="";
            }
            if(step.equals("APPROACH") || step.equals("EXPLORE")){
                travel.tick();if(travel.phase().equals("FAILED")){
                    if(target!=null && approaches!=null && approaches.advance("NO_PATH")){travel.start(approaches.current());return;}
                    if(access.start(target)){step="TERRAIN_STEP";reason="";return;}
                    if(target!=null)rejectArea(target,4);reason=travel.reason();step="SEARCH";cursor="";travel.cancel();
                }else if(travel.phase().equals("COMPLETED")){travel.cancel();step="LOCAL";cursor="";}return;
            }
            if(!step.equals("SEARCH"))return;
            failures.entrySet().removeIf(e->runtime.player().level().isLoaded(e.getKey()) && fingerprint(e.getKey())!=e.getValue());
            lastSearch=runtime.resources.query(resource,radius,64,cursor,failures.keySet());cursor=lastSearch.get("cursor").getAsString();
            if(!lastSearch.get("complete").getAsBoolean())return;
            var candidates=lastSearch.getAsJsonArray("results");
            for(var value:candidates){var row=value.getAsJsonObject();BlockPos pos=new BlockPos(row.get("x").getAsInt(),row.get("y").getAsInt(),row.get("z").getAsInt());
                var options=approach(pos);if(options.isEmpty()){rejectArea(pos,1);continue;}target=pos;
                if(!pos.equals(timedTarget)){timedTarget=pos;bestTargetDistance=p.position().distanceTo(Vec3.atCenterOf(pos));targetStarted=runtime.server().getTickCount();targetBudget=Math.max(400,(int)Math.ceil(bestTargetDistance*12));}
                approaches=new dev.mcai.companion.vendor.numen.task.RecoveryLadder<>(options.stream().limit(4).map(feet->new dev.mcai.companion.vendor.numen.task.RecoveryLadder.Rung<Vec3,String>(() -> feet,Set.of("NO_PATH"),1)).toList());travel.start(approaches.current());step="APPROACH";reason="";return;
            }
            cursor="";if(!candidates.isEmpty())return;
            if(explorations++<4){var next=explorationPoint();if(next!=null){target=null;visited.add(BlockPos.containing(next));travel.start(next);step="EXPLORE";return;}}
            block("No usable source was reached after bounded loaded-world searches and checked exploration routes; unsearched space remains unknown");
        }catch(RuntimeException failure){block(String.valueOf(failure.getMessage()));}
    }
    private boolean beginCollection(){
        var p=runtime.player();boolean wood=resource.equals("wood");
        var plan=runtime.collection().plan(resource,output,"any",wood?"tree":"blocks",p.position(),10,Math.max(1,wanted-received),null,false,false);
        if(plan.has("requestId"))children.add(plan.get("requestId").getAsString());
        // A rejected hanging trunk is still a visible search match. Carry the
        // survey's concrete exclusions into this task instead of approaching
        // that same unchanged component forever. Changed neighbours invalidate
        // the fingerprint in SEARCH, allowing a fresh check after terrain work.
        if(plan.has("rejectedTrees"))for(var value:plan.getAsJsonArray("rejectedTrees")){
            var tree=value.getAsJsonObject();
            if(tree.has("harvestable") && tree.get("harvestable").getAsBoolean())continue;
            if(tree.has("logs"))for(var log:tree.getAsJsonArray("logs")){
                var row=log.getAsJsonObject();var pos=new BlockPos(row.get("x").getAsInt(),row.get("y").getAsInt(),row.get("z").getAsInt());
                if(failures.size()<4096 && p.level().isLoaded(pos))failures.put(pos,fingerprint(pos));
            }
        }
        if(!plan.get("phase").getAsString().equals("PLAN_READY")){
            reason=plan.get("reason").getAsString();if(reason.toLowerCase(Locale.ROOT).matches(".*(tool|durability).*")){block(reason);return true;}return false;
        }
        var id=UUID.fromString(plan.get("requestId").getAsString());children.add(id.toString());runtime.collection().choose(id,plan.getAsJsonArray("options").get(0).getAsJsonObject().get("optionId").getAsString());step="COLLECT";reason="";return true;
    }
    private List<Vec3> approach(BlockPos pos){
        var p=runtime.player();var possibilities=new ArrayList<Vec3>();
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=-3;y<=1;y++){
            var cell=pos.offset(x,y,z);if(cell.equals(pos) || !p.level().isLoaded(cell))continue;
            var feet=runtime.collection().standingFeet(cell);if(feet==null)continue;
            if(runtime.mining().reachableFrom(pos,feet))possibilities.add(feet);
        }
        return possibilities.stream().sorted(Comparator.comparingDouble(p.position()::distanceToSqr)).toList();
    }
    private Vec3 explorationPoint(){var p=runtime.player();
        for(int distance:new int[]{24,40})for(int direction=0;direction<8;direction++){
            double angle=direction*Math.PI/4;int x=p.getBlockX()+(int)Math.round(Math.sin(angle)*distance),z=p.getBlockZ()+(int)Math.round(Math.cos(angle)*distance);
            if(!p.level().hasChunk(x>>4,z>>4))continue;int y=p.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);var at=new BlockPos(x,y,z);if(visited.stream().anyMatch(v->v.distSqr(at)<256))continue;var feet=runtime.collection().standingFeet(at);if(feet!=null)return feet;
        }return null;
    }
    private int fingerprint(BlockPos pos){var level=runtime.player().level();int hash=level.getBlockState(pos).hashCode();for(var d:net.minecraft.core.Direction.values())hash=31*hash+(level.isLoaded(pos.relative(d))?level.getBlockState(pos.relative(d)).hashCode():0);return hash;}
    private boolean rMiningActive(){return runtime.mining().phase().equals("EXECUTING");}
    private void rejectArea(BlockPos center,int spread){
        // Exclude observed matching candidates near a failed approach, not unknown terrain.
        if(lastSearch.has("results"))for(var value:lastSearch.getAsJsonArray("results")){var row=value.getAsJsonObject();var pos=new BlockPos(row.get("x").getAsInt(),row.get("y").getAsInt(),row.get("z").getAsInt());if(pos.distSqr(center)<=spread*spread)failures.put(pos,fingerprint(pos));}
        if(failures.size()<4096)failures.put(center,fingerprint(center));
    }
    private void block(String message){cancelChildren();phase="BLOCKED";reason=message;}
    private void cancelChildren(){if(access!=null)access.cancel();if(travel!=null)travel.cancel();if(isChild(runtime.collection().status()))runtime.collection().cancelForChat();}
    public JsonObject cancel(){if(active()){cancelChildren();phase="CANCELLED";reason="Stopped by player; completed pickups remain in inventory";}return status();}
    public JsonObject status(){var out=new JsonObject();out.addProperty("phase",phase);out.addProperty("step",step);out.addProperty("ownsBody",active());out.addProperty("reason",reason);out.addProperty("verifiedInventoryIncrease",received);out.addProperty("requestedCount",wanted);out.addProperty("physicalDistance",walked);out.addProperty("excludedCandidates",failures.size());out.addProperty("explorationMoves",explorations);if(travel!=null && active()){out.addProperty("travelPhase",travel.phase());out.addProperty("travelReason",travel.reason());}if(request!=null)out.addProperty("requestId",request.toString());if(resource!=null)out.addProperty("resource",resource);if(target!=null)out.add("target",TreeSurvey.position(target));out.add("childCollectionRequestIds",new Gson().toJsonTree(children));if(access!=null){var recovery=access.status();out.add("access",recovery);out.add("childMiningRequestIds",recovery.get("childMiningRequestIds"));}if(active() && step.equals("SEARCH"))out.add("search",lastSearch.deepCopy());return out;}
    @Override public void close(){cancel();if(travel!=null)travel.close();}
}
