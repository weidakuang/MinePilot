package dev.mcai.companion.agent.mining;

import java.util.*;
import com.google.gson.*;
import dev.mcai.companion.agent.AgentRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Bounded observations, not a claim that Minecraft records natural-tree ownership. */
public final class TreeSurvey {
    private TreeSurvey() {}
    private static final Set<String> SPECIES=Set.of("oak","spruce","birch","jungle","acacia","dark_oak","mangrove","cherry","pale_oak","crimson","warped");
    public static String species(String id) {
        if(!id.startsWith("minecraft:"))return "";
        String path=id.substring(10);
        for(String type:SPECIES)if(path.equals(type+"_log") || path.equals(type+"_stem"))return type;
        return "";
    }
    public static String id(BlockState state){return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();}
    public record Survey(String species,List<BlockPos> logs,String classification,List<String> evidence,boolean bounded,boolean harvestable) {
        public JsonObject json(){var out=new JsonObject();out.addProperty("species",species);out.addProperty("classification",classification);
            out.addProperty("ownership","unknown_unless_controller_declared");out.addProperty("completeObservedComponent",bounded);out.addProperty("harvestable",harvestable);
            out.add("evidence",new Gson().toJsonTree(evidence));var rows=new JsonArray();for(var p:logs)rows.add(position(p));out.add("logs",rows);return out;}
    }
    public static JsonObject position(BlockPos p){var out=new JsonObject();out.addProperty("x",p.getX());out.addProperty("y",p.getY());out.addProperty("z",p.getZ());return out;}
    public static Survey inspect(AgentRuntime r,BlockPos seed) {
        var level=r.player().level();
        if(!r.perception.observableBlock(seed))throw new IllegalArgumentException("Tree seed is not currently sensed");
        String species=species(id(level.getBlockState(seed)));
        if(species.isEmpty())return new Survey("",List.of(),"not_supported_unstripped_trunk",List.of("Bamboo, roots, stripped/decorative wood and unknown mod trees need separate rules"),true,false);
        var logs=new LinkedHashSet<BlockPos>();var seen=new HashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();queue.add(seed.immutable());
        boolean complete=true,canopy=false,grounded=false,machine=false,construction=false,danger=false,declared=false;int saplings=0;
        var evidence=new LinkedHashSet<String>();var farms=r.player().inventoryLedger.treeFarms();
        boolean memoryOk=farms.get("memoryWritable").getAsBoolean();
        while(!queue.isEmpty() && logs.size()<128){
            var p=queue.removeFirst();if(!logs.add(p))continue;
            for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){
                if(x==0 && y==0 && z==0)continue;var q=p.offset(x,y,z);
                if(!level.isLoaded(q) || !r.perception.observableBlock(q)){complete=false;continue;}
                var state=level.getBlockState(q);
                if(species(id(state)).equals(species) && !logs.contains(q) && !queue.contains(q))queue.add(q.immutable());
                String sid=id(state);
                if(sid.equals("minecraft:"+species+"_leaves") && !state.getValue(BlockStateProperties.PERSISTENT))canopy=true;
                if((species.equals("crimson") && state.is(Blocks.NETHER_WART_BLOCK)) || (species.equals("warped") && state.is(Blocks.WARPED_WART_BLOCK)))canopy=true;
            }
            var below=level.getBlockState(p.below());
            if(r.perception.observableBlock(p.below()) && (below.is(BlockTags.DIRT) || below.is(Blocks.CRIMSON_NYLIUM) || below.is(Blocks.WARPED_NYLIUM) || below.is(Blocks.MANGROVE_ROOTS) || below.is(Blocks.MUDDY_MANGROVE_ROOTS)))grounded=true;
            for(var entry:farms.getAsJsonObject("farms").entrySet()){
                var f=entry.getValue().getAsJsonObject();if(!f.get("dimension").getAsString().equals(level.dimension().identifier().toString()))continue;
                if(contains(f,p)){declared=true;evidence.add("Declared farm: "+f.get("name").getAsString());if(f.get("kind").getAsString().equals("automated"))machine=true;}
            }
            // Union scan is capped by the connected trunk bound; never loads new chunks.
            for(int x=-2;x<=2;x++)for(int y=-2;y<=2;y++)for(int z=-2;z<=2;z++){
                var q=p.offset(x,y,z);if(!seen.add(q) || !r.perception.observableBlock(q))continue;
                var s=level.getBlockState(q);String sid=id(s);
                if(s.getBlock() instanceof net.minecraft.world.level.block.SaplingBlock || s.is(Blocks.MANGROVE_PROPAGULE))saplings++;
                if(isMachine(s)){machine=true;evidence.add("Production component: "+sid);}
                if(s.is(BlockTags.PLANKS) || sid.contains("stripped_") || s.is(BlockTags.BEDS)){construction=true;evidence.add("Constructed material: "+sid);}
                if(s.is(Blocks.CREAKING_HEART) || s.is(Blocks.BEE_NEST) || s.is(Blocks.BEEHIVE)){danger=true;evidence.add("Living tree fixture: "+sid);}
            }
        }
        if(!queue.isEmpty())complete=false;
        if(canopy)evidence.add("Matching non-persistent canopy / fungal cap");if(grounded)evidence.add("Trunk meets compatible ground/root");
        if(saplings>0)evidence.add("Nearby saplings: "+saplings);
        boolean rows=false;
        for(var root:logs){
            if(logs.contains(root.below()))continue;
            for(int spacing=3;spacing<=6;spacing++)for(int axis=0;axis<2;axis++)for(int start=-2;start<=0;start++){
                boolean line=true;
                for(int index=start;index<start+3;index++){
                    var p=root.offset(axis==0?spacing*index:0,0,axis==1?spacing*index:0);
                    if(!r.perception.observableBlock(p) || !r.perception.observableBlock(p.below()) || !species(id(level.getBlockState(p))).equals(species) || !level.getBlockState(p.below()).is(BlockTags.DIRT)){line=false;break;}
                }
                rows|=line;
            }
        }
        if(rows)evidence.add("Three trunks on compatible ground in a regularly spaced row (planting clue, not proof of ownership)");
        boolean grove=declared || saplings>=2 || rows;
        String classification=machine?"automated_tree_farm_candidate":danger?"inhabited_tree":construction?"constructed_wood_candidate":!complete?"incomplete_observation":!canopy || !grounded || logs.size()<2?"unconfirmed_wood":grove?"managed_grove_candidate":"mature_tree_candidate";
        return new Survey(species,List.copyOf(logs),classification,List.copyOf(evidence),complete,
                memoryOk && complete && canopy && grounded && logs.size()>=2 && !machine && !danger && !construction);
    }
    public static boolean contains(JsonObject f,BlockPos p){return p.getX()>=f.get("min_x").getAsInt() && p.getX()<=f.get("max_x").getAsInt() && p.getY()>=f.get("min_y").getAsInt() && p.getY()<=f.get("max_y").getAsInt() && p.getZ()>=f.get("min_z").getAsInt() && p.getZ()<=f.get("max_z").getAsInt();}
    public static boolean isMachine(BlockState s){String id=id(s);return s.is(Blocks.PISTON) || s.is(Blocks.STICKY_PISTON) || s.is(Blocks.MOVING_PISTON) || s.is(Blocks.PISTON_HEAD) || s.is(Blocks.OBSERVER) || s.is(Blocks.DISPENSER) || s.is(Blocks.DROPPER) || s.is(Blocks.HOPPER) || s.is(Blocks.TNT) || s.is(Blocks.REDSTONE_WIRE) || s.is(Blocks.REPEATER) || s.is(Blocks.COMPARATOR) || s.is(Blocks.SLIME_BLOCK) || s.is(Blocks.HONEY_BLOCK) || id.contains("redstone");}
}
