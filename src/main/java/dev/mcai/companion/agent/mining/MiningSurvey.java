package dev.mcai.companion.agent.mining;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import dev.mcai.companion.agent.AgentRuntime;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import static dev.mcai.companion.agent.mining.ExcavationPlanner.Pos;

/** Budgeted local cave survey. World sampling stays on the server; connected-space analysis uses immutable data. */
public final class MiningSurvey implements AutoCloseable {
    private record Cell(boolean empty,boolean full,boolean danger,String block) {}
    private final AgentRuntime r;
    private final ExecutorService worker=Executors.newSingleThreadExecutor(task->{var t=new Thread(task,"MinePilot-CaveSurvey");t.setDaemon(true);return t;});
    private final Map<Pos,Cell> cells=new HashMap<>();
    private CompletableFuture<JsonObject> future;
    private JsonObject result=new JsonObject(),generation=new JsonObject();
    private Vec3 origin;private BlockPos base;private String dimension,request,resource,phase="IDLE";
    private int radius,index,unknown,startedTick;private double captureMillis,workerMillis;
    public MiningSurvey(AgentRuntime r){this.r=r;}
    public JsonObject start(int radius,String resource){
        if(radius<2 || radius>10 || resource.length()>64 || !resource.matches("[a-z0-9_:]*"))throw new IllegalArgumentException("Survey radius 2..10 and a bounded registry resource name required");
        if(future!=null)future.cancel(true);future=null;
        this.radius=radius;this.resource=resource;origin=r.player().position();base=r.player().blockPosition();dimension=r.player().level().dimension().identifier().toString();request=UUID.randomUUID().toString();
        phase="CAPTURING";cells.clear();result=new JsonObject();index=unknown=0;captureMillis=workerMillis=0;startedTick=r.server().getTickCount();generation=generationHints(resource);return status();
    }
    public void tick(){
        if(phase.equals("CAPTURING")){
            if(!dimension.equals(r.player().level().dimension().identifier().toString()) || r.player().position().distanceToSqr(origin)>.25){phase="STALE";return;}
            long begin=System.nanoTime(),deadline=r.perception.beginWork(1_500_000L);int width=2*radius+1,volume=width*width*width,count=0;
            while(index<volume && count++<1024 && System.nanoTime()<deadline){
                int n=index++;var p=base.offset(n%width-radius,n/(width*width)-radius,(n/width)%width-radius);
                if(p.distToCenterSqr(origin)>radius*radius)continue;
                if(!r.perception.observableBlock(p)){unknown++;continue;}
                var state=r.player().level().getBlockState(p);String id=TreeSurvey.id(state);
                boolean danger=!state.getFluidState().isEmpty() || id.contains("fire") || id.equals("minecraft:magma_block") || id.equals("minecraft:powder_snow");
                cells.put(ExcavationCoordinator.pos(p),new Cell(state.getCollisionShape(r.player().level(),p).isEmpty(),state.isCollisionShapeFullBlock(r.player().level(),p),danger,id));
            }
            r.perception.recordWork(begin);captureMillis+=(System.nanoTime()-begin)/1_000_000.0;
            if(index>=volume){var snapshot=Map.copyOf(cells);var position=ExcavationCoordinator.pos(base);String filter=resource;phase="ANALYZING";
                future=CompletableFuture.supplyAsync(()->{long now=System.nanoTime();var analyzed=analyze(snapshot,position,filter);analyzed.addProperty("workerMillis",(System.nanoTime()-now)/1_000_000.0);return analyzed;},worker);}
        }else if(phase.equals("ANALYZING") && future.isDone()){
            try{result=future.join();workerMillis=result.get("workerMillis").getAsDouble();phase="COMPLETED";}catch(RuntimeException failure){phase="FAILED";result.addProperty("reason",String.valueOf(failure.getMessage()));}future=null;
        }
    }
    private JsonObject generationHints(String filter){
        var out=new JsonObject();var rows=new JsonArray();String term=filter.replace("minecraft:","").replace("deepslate_","").replace("_ore","");
        if(!term.isBlank())for(var stage:r.player().level().getBiome(base).value().getGenerationSettings().features())for(var holder:stage){
            String id=holder.unwrapKey().map(k->k.identifier().toString()).orElse("");if(!id.contains(term) || rows.size()>=12)continue;
            var encoded=net.minecraft.world.level.levelgen.placement.PlacedFeature.DIRECT_CODEC.encodeStart(r.player().registryAccess().createSerializationContext(JsonOps.INSTANCE),holder.value()).result();
            if(encoded.isEmpty() || !encoded.get().isJsonObject())continue;var value=encoded.get().getAsJsonObject();var row=new JsonObject();row.addProperty("placedFeature",id);
            var modifiers=new JsonArray();if(value.has("placement"))for(var modifier:value.getAsJsonArray("placement")){var m=modifier.getAsJsonObject();String type=m.has("type")?m.get("type").getAsString():"";if(Set.of("minecraft:height_range","minecraft:count","minecraft:rarity_filter").contains(type) && m.toString().length()<2048)modifiers.add(m);}
            row.add("generationRules",modifiers);rows.add(row);
        }
        out.add("currentBiomeFeatures",rows);out.addProperty("meaning","Current biome datapack placement rules, not locations or guaranteed ore yield. Custom generators and previously generated chunks can differ.");return out;
    }
    private static boolean open(Map<Pos,Cell> map,Pos p){var c=map.get(p);return c!=null && c.empty && !c.danger;}
    private static List<Pos> adjacent(Pos p){return List.of(p.add(1,0,0),p.add(-1,0,0),p.add(0,1,0),p.add(0,-1,0),p.add(0,0,1),p.add(0,0,-1));}
    private static JsonObject position(Pos p){var o=new JsonObject();o.addProperty("x",p.x());o.addProperty("y",p.y());o.addProperty("z",p.z());return o;}
    private static JsonObject analyze(Map<Pos,Cell> map,Pos origin,String filter){
        var feet=new HashSet<Pos>();for(var e:map.entrySet()){var p=e.getKey();var floor=map.get(p.add(0,-1,0));if(open(map,p) && open(map,p.add(0,1,0)) && floor!=null && floor.full && !floor.danger)feet.add(p);}
        var visited=new HashSet<Pos>();var caves=new ArrayList<JsonObject>();
        for(Pos seed:feet){if(visited.contains(seed))continue;var queue=new ArrayDeque<Pos>();var component=new ArrayList<Pos>();queue.add(seed);visited.add(seed);int ceiling=0,boundaries=0,hazards=0,exposed=0;var minerals=new JsonArray();
            while(!queue.isEmpty()){
                if(Thread.currentThread().isInterrupted())throw new CancellationException();var p=queue.removeFirst();component.add(p);
                boolean roof=false;for(int dy=2;dy<=8;dy++){var cell=map.get(p.add(0,dy,0));if(cell==null)break;if(cell.full){roof=true;break;}}if(roof)ceiling++;
                for(var q:adjacent(p)){var c=map.get(q);if(c==null)boundaries++;else if(c.danger)hazards++;}
                for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})for(int dy:new int[]{0,1,-1}){
                    var q=p.add(d[0],dy,d[1]);if(feet.contains(q) && (dy<=0 || open(map,p.add(0,2,0))) && visited.add(q))queue.add(q);
                }
            }
            if(component.size()<4)continue;
            var adjacentOre=new HashSet<Pos>();for(var p:component)for(int dy=0;dy<=1;dy++)for(var q:adjacent(p.add(0,dy,0))){var c=map.get(q);if(c!=null && (c.block.endsWith("_ore") || c.block.equals("minecraft:ancient_debris")) && (filter.isBlank() || c.block.contains(filter.replace("minecraft:",""))) && adjacentOre.add(q)){exposed++;if(minerals.size()<16){var row=position(q);row.addProperty("block",c.block);minerals.add(row);}}}
            var nearest=component.stream().min(Comparator.comparingDouble(origin::distance)).orElseThrow();boolean connected=component.contains(origin);
            var row=new JsonObject();row.add("approach",position(nearest));row.addProperty("walkableFloorCells",component.size());row.addProperty("roofedFloorCells",ceiling);row.addProperty("knownHazardEdges",hazards);row.addProperty("unknownBoundaryEdges",boundaries);row.addProperty("reachableInSurveyGraph",connected);
            row.addProperty("classification",ceiling>=4 && ceiling*2>=component.size()?"roofed_cave_candidate":"open_or_unconfirmed_space");
            row.addProperty("score",Math.min(40,component.size())+Math.min(40,exposed*8)+(connected?20:0)-hazards*5);
            row.addProperty("scoreMeaning","Local opportunity ranking: known floor area + exposed target ore + graph connection - observed hazards; not a survival guarantee");row.add("exposedResources",minerals);
            row.addProperty("returnPolicy",connected?"Known floor graph connects back to survey origin; live navigation must verify it":"A separate native route to this component is required");
            var frontier=component.stream().filter(p->adjacent(p).stream().anyMatch(q->!map.containsKey(q))).sorted(Comparator.comparingDouble(origin::distance)).limit(8).toList();row.add("explorationFrontiers",new Gson().toJsonTree(frontier));caves.add(row);
        }
        caves.sort(Comparator.comparingInt(o->-o.get("score").getAsInt()));var rows=new JsonArray();caves.stream().limit(8).forEach(rows::add);
        var out=new JsonObject();out.add("spaces",rows);out.addProperty("spaceCount",caves.size());out.addProperty("truncated",caves.size()>8);out.addProperty("observedCells",map.size());
        var strategies=new JsonArray();
        for(String[] strategy:new String[][]{{"cave_follow","Compare roofed spaces and exposed ore, navigate to a safe visible frontier, survey again, then use resource_radius excavation. Unknown frontier is not proof of a route."},{"local_resource","Use fixed-center resource_radius for sensed matching ore, or access to approach without breaking the final target."},{"staircase_or_tunnel","Use bounded tunnel segments toward a model-selected depth/direction based on current generator hints and live hazards; survey after each segment."},{"fishbone_optional","Only choose when it fits the player's goal and local conditions. Approve branch spacing/length and costs with plan_excavation mode fishbone; never run it for every mining request."}}){var row=new JsonObject();row.addProperty("strategy",strategy[0]);row.addProperty("nextDecision",strategy[1]);strategies.add(row);}
        out.add("strategyChoices",strategies);out.addProperty("torchPolicy","No automatic torch placement; only on player request");return out;
    }
    public JsonObject status(){var out=result.deepCopy();out.addProperty("phase",phase);if(request!=null){out.addProperty("requestId",request);out.addProperty("dimension",dimension);out.add("origin",new Gson().toJsonTree(Map.of("x",origin.x,"y",origin.y,"z",origin.z)));out.addProperty("radius",radius);out.addProperty("scanStartedTick",startedTick);out.addProperty("viewMovedSinceStart",r.player().position().distanceToSqr(origin)>.25);}
        out.addProperty("captureServerMillis",captureMillis);out.addProperty("workerMillis",workerMillis);out.addProperty("unknownCells",unknown);out.addProperty("snapshotIsAtomic",false);out.addProperty("requiresLiveRouteVerification",true);out.add("generationHints",generation.deepCopy());return out;}
    @Override public void close(){if(future!=null)future.cancel(true);worker.shutdownNow();}
}
