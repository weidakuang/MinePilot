package dev.mcai.companion.agent.survival;

import com.google.gson.*;
import java.util.*;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.navigation.NativeTravel;
import dev.mcai.companion.agent.placement.PlacementTools;
import dev.mcai.companion.agent.placement.PlacementGeometry;
import dev.mcai.companion.vendor.numen.build.BuildOrder;
import dev.mcai.companion.vendor.numen.tools.CraftOps;
import dev.mcai.companion.vendor.numen.tools.NativeInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.*;

/** A persistent small native build, composed from the existing collection and placement jobs. */
public final class CampCoordinator implements AutoCloseable {
    private final AgentRuntime r;
    private boolean loaded,autoGather=true;private String phase="IDLE",step="",reason="",dimension="";
    private UUID request;private BlockPos site;private NativeTravel travel;
    private JsonArray targets=new JsonArray();private final Set<String> childGather=new HashSet<>(),childPlacement=new HashSet<>();
    private int started,repairs,completed;private String afterTravel="PREPARE";private final Set<BlockPos> failedWorkPositions=new HashSet<>();private BlockPos workPosition;
    public CampCoordinator(AgentRuntime runtime){r=runtime;}
    private void load(){if(loaded)return;loaded=true;var saved=r.memory.camp();if(!saved.has("requestId"))return;
        request=UUID.fromString(saved.get("requestId").getAsString());site=SurvivalTools.position(saved.getAsJsonObject("site"));dimension=saved.get("dimension").getAsString();
        targets=saved.getAsJsonArray("targets").deepCopy();autoGather=saved.get("autoGather").getAsBoolean();
        completed=0;if(dimension.equals(r.player().level().dimension().identifier().toString()))for(var target:targets)if(correct(target.getAsJsonObject()))completed++;
        phase=saved.get("phase").getAsString().equals("COMPLETED") && completed==83?"COMPLETED":"PAUSED";reason="Restored camp plan; loaded cells rechecked, resume verifies remaining world cells and inventory";
    }
    public boolean active(){return loaded && phase.equals("EXECUTING");}
    public boolean isGatherChild(JsonObject s){return s.has("requestId") && childGather.contains(s.get("requestId").getAsString());}
    public boolean isPlacementChild(JsonObject s){return s.has("requestId") && childPlacement.contains(s.get("requestId").getAsString());}
    public JsonObject start(BlockPos requested,boolean auto){load();SurvivalTools.requireIdle(r);if(active())throw new IllegalStateException("A camp is already running");
        site=requested==null?findSite():requested.immutable();if(site==null || !freeSite(site))throw new IllegalArgumentException("No loaded flat 5x5 site with clear headroom nearby; explore a flatter area or supply a checked origin");
        autoGather=auto;targets=new JsonArray();request=UUID.randomUUID();dimension=r.player().level().dimension().identifier().toString();childGather.clear();childPlacement.clear();failedWorkPositions.clear();repairs=0;completed=0;
        phase="EXECUTING";reason="";started=r.server().getTickCount();go(front(),"PREPARE");save();return status();
    }
    public JsonObject resume(){load();SurvivalTools.requireIdle(r);if(request==null || active())throw new IllegalStateException("No interrupted camp to resume");
        if(!dimension.equals(r.player().level().dimension().identifier().toString()))throw new IllegalStateException("Camp belongs to another dimension");
        phase="EXECUTING";reason="";failedWorkPositions.clear();repairs=0;started=r.server().getTickCount();go(front(),targets.isEmpty()?"PREPARE":"BUILD");save();return status();
    }
    private Vec3 front(){return Vec3.atBottomCenterOf(site.offset(2,0,6));}
    private void go(Vec3 feet,String next){if(travel==null)travel=new NativeTravel(r,.2);afterTravel=next;travel.start(feet);step="RETURN";}
    public void beforePhysics(){if(active() && travel!=null)travel.beforePhysics();}
    public void tick(){if(!active())return;
        if(!r.player().isAlive() || !dimension.equals(r.player().level().dimension().identifier().toString()) || r.server().getTickCount()-started>24000){block("Camp life/dimension/20-minute budget boundary; blueprint retained");return;}
        try{switch(step){
            case "RETURN" -> {travel.tick();if(travel.phase().equals("FAILED")){if(afterTravel.equals("BUILD") && workPosition!=null){failedWorkPositions.add(workPosition);step="BUILD";}else block("Camp approach: "+travel.reason());}else if(travel.phase().equals("COMPLETED")){travel.cancel();step=afterTravel;}}
            case "PREPARE" -> prepare();
            case "GATHER" -> {var child=r.gather().status();if(child.get("phase").getAsString().equals("EXECUTING"))return;
                if(child.get("verifiedInventoryIncrease").getAsInt()==0){block("Camp material search: "+child.get("reason").getAsString());return;}go(front(),targets.isEmpty()?"PREPARE":"BUILD");}
            case "BENCH" -> {var child=r.placement().status();if(child.get("phase").getAsString().equals("EXECUTING"))return;if(!child.get("phase").getAsString().equals("COMPLETED")){block("Bootstrap workstation: "+child.get("reason").getAsString());return;}step="PREPARE";}
            case "BUILD" -> beginBuild();
            case "PLACE" -> {var child=r.placement().status();if(child.get("phase").getAsString().equals("EXECUTING"))return;
                if(child.get("phase").getAsString().equals("COMPLETED")){failedWorkPositions.clear();repairs=0;step="BUILD";return;}
                if(child.has("satisfiedTargets") && child.get("satisfiedTargets").getAsInt()>0){r.placement().cancelForChat();failedWorkPositions.clear();repairs=0;step="BUILD";return;}
                // Same blueprint, checked alternate work side; no extra clearing or invented support.
                if(repairs++<3){r.placement().cancelForChat();var sides=List.of(site.offset(-2,0,2),site.offset(6,0,2),site.offset(2,0,-2));var feet=r.collection().standingFeet(sides.get(repairs-1));if(feet!=null){go(feet,"BUILD");save();return;}}
                block("Construction paused at an actual obstruction: "+child.get("reason").getAsString());}
            default -> block("Unknown camp phase");
        }}catch(RuntimeException error){block(error.getMessage());}
    }
    private int count(Item item){return NativeInventory.count(r.player().getInventory(),item);}
    private int planks(){int n=0;for(int i=0;i<36;i++){var s=r.player().getInventory().getItem(i);if(s.is(ItemTags.PLANKS) && NativeInventory.usable(r.player(),s))n+=s.getCount();}return n;}
    private int logPotential(){int n=0;for(int i=0;i<36;i++){var s=r.player().getInventory().getItem(i);if(s.is(ItemTags.LOGS) && NativeInventory.usable(r.player(),s))n+=s.getCount()*4;}return n;}
    private boolean pick(){for(int i=0;i<36;i++){var s=r.player().getInventory().getItem(i);if(s.is(ItemTags.PICKAXES) && NativeInventory.usable(r.player(),s) && (!s.isDamageableItem() || s.getMaxDamage()-s.getDamageValue()>10))return true;}return false;}
    private void craft(String item,int count){var result=JsonParser.parseString(new CraftOps().craft(item,count,r.player())).getAsJsonObject();if(!result.get("success").getAsBoolean())throw new IllegalStateException("Camp crafting: "+result.get("message").getAsString());}
    private boolean benchReachable(){var p=r.player();for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=-2;y<=2;y++){var pos=p.blockPosition().offset(x,y,z);if(!p.level().isLoaded(pos) || !p.level().getBlockState(pos).is(Blocks.CRAFTING_TABLE))continue;
            try{SurvivalTools.useBlock(p,pos);p.closeContainer();return true;}catch(IllegalArgumentException|IllegalStateException ignored){}}
        return false;
    }
    private void prepare(){
        boolean bench=benchReachable();int needed=80+(count(Items.CRAFTING_TABLE)>0?0:4)+(count(Items.CHEST)>0?0:8)+(bench?0:4)+(pick() || count(Items.FURNACE)>0?0:5);
        if(planks()<needed){
            if(planks()+logPotential()<needed){gather("wood",Math.min(32,(needed-planks()-logPotential()+3)/4));return;}
            for(int i=0;i<36;i++){var s=r.player().getInventory().getItem(i);if(!s.is(ItemTags.LOGS) || !NativeInventory.usable(r.player(),s))continue;
                String wood=BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().replaceFirst("^stripped_","").replaceFirst("_(log|wood|stem|hyphae)$","");
                craft("minecraft:"+wood+"_planks",Math.min(s.getCount()*4,needed-planks()));return;}
        }
        if(!bench){if(count(Items.CRAFTING_TABLE)==0)craft("minecraft:crafting_table",1);var a=new JsonObject();a.addProperty("item","minecraft:crafting_table");var child=r.placement().one(a);childPlacement.add(child.get("requestId").getAsString());step="BENCH";return;}
        if(count(Items.FURNACE)==0 && count(Items.COBBLESTONE)<8){
            if(!pick()){if(count(Items.STICK)<2)craft("minecraft:stick",4);craft("minecraft:wooden_pickaxe",1);return;}
            gather("minecraft:stone",8-count(Items.COBBLESTONE));return;
        }
        if(count(Items.FURNACE)==0){craft("minecraft:furnace",1);return;}
        if(count(Items.CHEST)==0){craft("minecraft:chest",1);return;}
        if(count(Items.CRAFTING_TABLE)==0){craft("minecraft:crafting_table",1);return;}
        makeBlueprint();step="BUILD";save();
    }
    private void gather(String resource,int amount){if(!autoGather){block("Missing camp materials; automatic gathering is disabled");return;}
        var child=r.gather().start(resource,"",Math.max(1,amount),150);childGather.add(child.get("requestId").getAsString());step="GATHER";save();}
    private void makeBlueprint(){
        var solids=new ArrayList<BlockPos>();for(int x=0;x<5;x++)for(int z=0;z<5;z++){solids.add(site.offset(x,0,z));solids.add(site.offset(x,3,z));
            if(x==0 || x==4 || z==0 || z==4)for(int y=1;y<=2;y++)if(!(x==2 && z==4))solids.add(site.offset(x,y,z));}
        solids.sort((a,b)->BuildOrder.BUILD_ORDER.compare(new BuildOrder.Target(a,Blocks.OAK_PLANKS.defaultBlockState()),new BuildOrder.Target(b,Blocks.OAK_PLANKS.defaultBlockState())));
        var pool=new LinkedHashMap<String,Integer>();for(int i=0;i<36;i++){var s=r.player().getInventory().getItem(i);if(s.is(ItemTags.PLANKS) && NativeInventory.usable(r.player(),s))pool.merge(BuiltInRegistries.ITEM.getKey(s.getItem()).toString(),s.getCount(),Integer::sum);}
        targets=new JsonArray();for(var pos:solids){String id=pool.entrySet().stream().filter(e->e.getValue()>0).map(Map.Entry::getKey).findFirst().orElseThrow(()->new IllegalStateException("Plank inventory changed before blueprint reservation"));pool.put(id,pool.get(id)-1);add(pos,id);}
        add(site.offset(1,1,1),"minecraft:crafting_table");add(site.offset(2,1,1),"minecraft:furnace");add(site.offset(3,1,1),"minecraft:chest");
        // Workstations precede upper walls/roof so the body keeps a usable route through the entrance.
        targets.asList().sort((a,b)->BuildOrder.BUILD_ORDER.compare(buildTarget(a.getAsJsonObject()),buildTarget(b.getAsJsonObject())));
    }
    private BuildOrder.Target buildTarget(JsonObject a){return new BuildOrder.Target(SurvivalTools.position(a),((BlockItem)NativeInventory.parseItem(a.get("item").getAsString())).getBlock().defaultBlockState());}
    private void add(BlockPos pos,String id){var row=PlacementTools.xyz(pos);row.addProperty("item",id);if(id.equals("minecraft:chest") || id.equals("minecraft:furnace")){var state=new JsonObject();state.addProperty("facing","south");row.add("state",state);}targets.add(row);}
    private boolean correct(JsonObject a){var pos=SurvivalTools.position(a);if(!r.player().level().isLoaded(pos))return false;var actual=r.player().level().getBlockState(pos);if(!BuiltInRegistries.BLOCK.getKey(actual.getBlock()).toString().equals(a.get("item").getAsString()))return false;
        if(a.has("state")){var properties=new HashMap<String,String>();a.getAsJsonObject("state").entrySet().forEach(e->properties.put(e.getKey(),e.getValue().getAsString()));return PlacementGeometry.matches(actual,properties);}return true;}
    private void beginBuild(){var remaining=new ArrayList<JsonObject>();completed=0;for(var value:targets){var row=value.getAsJsonObject();if(correct(row)){completed++;continue;}
            var pos=SurvivalTools.position(row);if(!r.player().level().isLoaded(pos) || !r.player().level().getBlockState(pos).canBeReplaced())throw new IllegalStateException("Camp target changed or is not loaded: "+pos);remaining.add(row.deepCopy());}
        if(remaining.isEmpty()){verify();return;}
        if(!restockRemaining(remaining))return;
        int layer=remaining.stream().mapToInt(row->row.get("y").getAsInt()).min().orElseThrow();
        var ordered=remaining.stream().filter(row->row.get("y").getAsInt()==layer).toList();
        var reachable=ordered.stream().filter(row->canPlaceFrom(row,r.player().position())).toList();
        if(reachable.isEmpty()){
            // A few fixed viewpoints miss corner faces after walls/roof change.
            // Check the small actual work area, using native standing/reach
            // tests and the existing route follower for every candidate.
            var positions=new LinkedHashSet<BlockPos>(List.of(site.offset(2,0,6),site.offset(-2,0,2),site.offset(2,0,-2),site.offset(6,0,2)));
            for(int y=0;y<=2;y++)for(int x=-1;x<=5;x++)for(int z=-1;z<=5;z++)positions.add(site.offset(x,y,z));
            var orderedPositions=positions.stream().sorted(Comparator.comparingDouble(pos->pos.distToCenterSqr(r.player().position()))).toList();
            for(var candidate:orderedPositions){
                if(failedWorkPositions.contains(candidate))continue;var feet=r.collection().standingFeet(candidate);
                if(feet==null || feet.distanceToSqr(r.player().position())<.1 || ordered.stream().noneMatch(row->canPlaceFrom(row,feet)))continue;
                workPosition=candidate;go(feet,"BUILD");return;
            }
            throw new IllegalStateException("No checked camp work position can reach the remaining layer; blueprint retained");
        }
        var planned=r.placement().plan(reachable,false,0,false);
        var id=UUID.fromString(planned.get("requestId").getAsString());childPlacement.add(id.toString());r.placement().choose(id,"bounded-placement");step="PLACE";save();
    }
    /** Refill only the unfinished cells; completed world cells are never charged again. */
    private boolean restockRemaining(List<JsonObject> remaining){
        var needed=new LinkedHashMap<Item,Integer>();for(var row:remaining)needed.merge(NativeInventory.parseItem(row.get("item").getAsString()),1,Integer::sum);
        int plankCells=0,stationPlanks=0;boolean shortage=false,paletteShortage=false;
        for(var entry:needed.entrySet()){
            boolean plank=new ItemStack(entry.getKey()).is(ItemTags.PLANKS);if(plank)plankCells+=entry.getValue();
            int missing=Math.max(0,entry.getValue()-count(entry.getKey()));if(missing==0)continue;shortage=true;paletteShortage|=plank;
            if(entry.getKey()==Items.CRAFTING_TABLE)stationPlanks+=4*missing;
            else if(entry.getKey()==Items.CHEST)stationPlanks+=8*missing;
        }
        if(!shortage)return true;
        if(!autoGather){block("Missing remaining camp materials; blueprint retained and automatic gathering is disabled");return false;}
        int missingWood=Math.max(0,plankCells+stationPlanks-planks());
        if(missingWood>0){
            if(logPotential()<missingWood){gather("wood",Math.min(32,(missingWood-logPotential()+3)/4));return false;}
            for(int i=0;i<36;i++){var stack=r.player().getInventory().getItem(i);if(!stack.is(ItemTags.LOGS) || !NativeInventory.usable(r.player(),stack))continue;
                String species=BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().replaceFirst("^stripped_","").replaceFirst("_(log|wood|stem|hyphae)$","");craft("minecraft:"+species+"_planks",Math.min(stack.getCount()*4,missingWood));return false;}
        }
        if(paletteShortage){
            // Replacement wood may use another species in unbuilt generic cells;
            // the palette and materials of completed cells remain unchanged.
            var pool=new LinkedHashMap<String,Integer>();for(int slot=0;slot<36;slot++){var stack=r.player().getInventory().getItem(slot);if(stack.is(ItemTags.PLANKS) && NativeInventory.usable(r.player(),stack))pool.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),stack.getCount(),Integer::sum);}
            for(var target:targets){var row=target.getAsJsonObject();if(correct(row) || !new ItemStack(NativeInventory.parseItem(row.get("item").getAsString())).is(ItemTags.PLANKS))continue;
                String chosen=pool.entrySet().stream().filter(e->e.getValue()>0).map(Map.Entry::getKey).findFirst().orElseThrow(()->new IllegalStateException("Remaining camp wood changed during allocation"));row.addProperty("item",chosen);pool.put(chosen,pool.get(chosen)-1);}
            save();return false;
        }
        for(var entry:needed.entrySet())if(count(entry.getKey())<entry.getValue() && !new ItemStack(entry.getKey()).is(ItemTags.PLANKS)){
            if(!benchReachable() && entry.getKey()!=Items.CRAFTING_TABLE){var inside=r.collection().standingFeet(site.offset(2,1,3));if(inside!=null && inside.distanceToSqr(r.player().position())>.1){go(inside,"BUILD");return false;}}
            if(entry.getKey()==Items.FURNACE && count(Items.COBBLESTONE)<8){gather("minecraft:stone",8-count(Items.COBBLESTONE));return false;}
            craft(BuiltInRegistries.ITEM.getKey(entry.getKey()).toString(),entry.getValue()-count(entry.getKey()));return false;
        }
        return true;
    }
    private boolean canPlaceFrom(JsonObject row,Vec3 feet){
        var position=SurvivalTools.position(row);
        // Keep the entire interior roof open while standing on a workstation.
        // Closing it here would trap the body above the room's normal headroom.
        if(feet.y>=site.getY()+1.8 && position.getY()==site.getY()+3
                && position.getX()>site.getX() && position.getX()<site.getX()+4
                && position.getZ()>site.getZ() && position.getZ()<site.getZ()+4)return false;
        var item=NativeInventory.parseItem(row.get("item").getAsString());if(count(item)==0)return false;
        var constraints=new HashMap<String,String>();if(row.has("state"))row.getAsJsonObject("state").entrySet().forEach(e->constraints.put(e.getKey(),e.getValue().getAsString()));
        return !PlacementGeometry.aims(r.player(),SurvivalTools.position(row),new ItemStack(item),net.minecraft.world.InteractionHand.MAIN_HAND,constraints,feet).isEmpty();
    }
    private void verify(){completed=0;for(var value:targets)if(correct(value.getAsJsonObject()))completed++;if(completed!=83 || !r.player().level().getBlockState(site.offset(2,1,4)).isAir() || !r.player().level().getBlockState(site.offset(2,2,4)).isAir()){block("Camp world-state verification failed");return;}
        for(int x=1;x<=3;x++)r.workstations.placed(site.offset(x,1,1));phase="COMPLETED";step="";reason="83 real cells verified: 5x5 floor, walls, roof, open entrance, crafting table, furnace and chest";save();}
    private BlockPos findSite(){var p=r.player();var offsets=new ArrayList<BlockPos>();
        // Sparse radial samples missed small flat patches between directions.
        // Check a bounded two-block grid, nearest first, in already loaded columns.
        for(int dx=-22;dx<=22;dx+=2)for(int dz=-22;dz<=22;dz+=2)if(dx*dx+dz*dz>=16 && dx*dx+dz*dz<=484)offsets.add(new BlockPos(dx,0,dz));
        offsets.sort(Comparator.comparingInt(o->o.getX()*o.getX()+o.getZ()*o.getZ()));
        for(var offset:offsets){int x=p.getBlockX()+offset.getX()-2,z=p.getBlockZ()+offset.getZ()-2;
            if(!p.level().hasChunk((x+2)>>4,(z+2)>>4))continue;int y=p.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x+2,z+2);var candidate=new BlockPos(x,y,z);if(freeSite(candidate))return candidate;}return null;}
    private boolean freeSite(BlockPos at){var level=r.player().level();if(at.distToCenterSqr(r.player().position())>24*24 || !level.getWorldBorder().isWithinBounds(at) || !level.getWorldBorder().isWithinBounds(at.offset(4,3,4)))return false;
        for(int x=0;x<5;x++)for(int z=0;z<5;z++){var floor=at.offset(x,0,z);if(!level.isLoaded(floor) || !level.getBlockState(floor.below()).isCollisionShapeFullBlock(level,floor.below()))return false;
            for(int y=0;y<4;y++){var cell=floor.above(y);var state=level.getBlockState(cell);if(!level.isLoaded(cell) || !level.getFluidState(cell).isEmpty() || level.getBlockEntity(cell)!=null || !state.canBeReplaced())return false;}}
        return r.collection().standingFeet(at.offset(2,0,6))!=null && level.getEntities(r.player(),new AABB(Vec3.atLowerCornerOf(at),Vec3.atLowerCornerOf(at.offset(5,4,5))),e->e instanceof net.minecraft.world.entity.LivingEntity).isEmpty();}
    private void cancelChildren(){if(travel!=null)travel.cancel();if(isGatherChild(r.gather().status()))r.gather().cancel();if(isPlacementChild(r.placement().status()))r.placement().cancelForChat();}
    private void block(String message){cancelChildren();phase="BLOCKED";reason=message==null?"Native camp action failed":message;save();}
    public JsonObject cancel(){load();if(active()){cancelChildren();phase="PAUSED";reason="Stopped by player; use resume_camp to continue the same blueprint";save();}return status();}
    private void save(){if(request==null)return;var record=status();record.add("targets",targets.deepCopy());record.addProperty("autoGather",autoGather);r.memory.camp(record);}
    public JsonObject status(){load();var out=new JsonObject();out.addProperty("phase",phase);out.addProperty("step",step);out.addProperty("reason",reason);out.addProperty("ownsBody",active());out.addProperty("totalCells",targets.size());out.addProperty("verifiedCells",completed);out.addProperty("approachRepairs",repairs);
        if(request!=null){out.addProperty("requestId",request.toString());out.addProperty("dimension",dimension);out.add("site",PlacementTools.xyz(site));}out.add("childGatherRequestIds",new Gson().toJsonTree(childGather));out.add("childPlacementRequestIds",new Gson().toJsonTree(childPlacement));return out;}
    @Override public void close(){cancel();if(travel!=null)travel.close();}
}
