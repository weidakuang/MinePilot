package dev.mcai.companion.agent.knowledge;

import com.google.gson.*;
import java.util.*;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.mining.TreeSurvey;
import dev.mcai.companion.vendor.numen.scan.BlockSearch;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/** Bounded exposed-resource queries, using Numen's loaded-section jobs and budgets. */
public final class ResourceSearch {
    private final AgentRuntime runtime;
    private final Map<String,Scan> scans=new LinkedHashMap<>();
    public ResourceSearch(AgentRuntime runtime){this.runtime=runtime;}
    public static String normalize(String value){return Set.of("wood","木头","树","原木").contains(value)?"wood":PerceptionQuery.normalize(value);}
    public JsonObject query(String resource,int radius,int limit,String cursor){return query(resource,radius,limit,cursor,Set.of());}
    public JsonObject query(String resource,int radius,int limit,String cursor,Set<BlockPos> exclusions){
        if(radius<1 || radius>PerceptionRange.MAX || limit<1 || limit>64)throw new IllegalArgumentException("Resource query radius 1..150 and limit 1..64 required");
        resource=normalize(resource);var p=runtime.player();long tick=p.level().getGameTime();
        scans.values().removeIf(s->{if(tick-s.tick<=600 && s.dimension.equals(p.level().dimension().identifier().toString()))return false;BlockSearch.cancel(s.job);return true;});
        Scan scan=cursor.isBlank()?null:scans.get(cursor);
        if(!cursor.isBlank() && scan==null)throw new IllegalArgumentException("Unknown resource cursor");
        if(scan==null){
            for(var cached:scans.values())if(cached.result!=null && tick-cached.tick<=100 && cached.resource.equals(resource) && cached.radius==radius && cached.limit==limit && cached.origin.distanceToSqr(p.position())<.01 && cached.exclusions.equals(exclusions)){scan=cached;break;}
            if(scan==null){if(scans.size()>=8){var old=scans.remove(scans.keySet().iterator().next());BlockSearch.cancel(old.job);}scan=new Scan(resource,radius,limit,exclusions);scans.put(scan.id,scan);}
        }
        if(!scan.resource.equals(resource) || scan.radius!=radius || scan.limit!=limit)throw new IllegalArgumentException("Continue resource cursor with unchanged resource/radius/limit");
        return scan.json();
    }
    public void tick(){BlockSearch.tick(runtime.server());}
    public void close(){for(var s:scans.values())BlockSearch.cancel(s.job);scans.clear();}
    private final class Scan {
        final String id=UUID.randomUUID().toString(),resource,dimension;
        final Vec3 origin=runtime.player().position();final long tick=runtime.player().level().getGameTime();final int radius,limit,job;
        final Set<BlockPos> exclusions;final Set<Block> targets=new HashSet<>();BlockSearch.ScanResult result;
        Scan(String resource,int radius,int limit,Set<BlockPos> exclusions){this.resource=resource;this.radius=radius;this.limit=limit;this.exclusions=Set.copyOf(exclusions);dimension=runtime.player().level().dimension().identifier().toString();
            for(var block:BuiltInRegistries.BLOCK){String id=BuiltInRegistries.BLOCK.getKey(block).toString();if(resource.equals("wood")?!TreeSurvey.species(id).isEmpty():PerceptionQuery.block(block.defaultBlockState(),resource))targets.add(block);}
            if(targets.isEmpty())throw new IllegalArgumentException("Unknown resource "+resource);
            job=BlockSearch.start(runtime.player().getUUID(),runtime.player().level(),BlockPos.containing(origin),radius+1,Math.max(32,limit),targets,
                pos->Vec3.atCenterOf(pos).distanceToSqr(origin)<=radius*(double)radius && !this.exclusions.contains(pos) && runtime.perception.exposed(pos) && surfaceCandidate(pos),r->result=r);
        }
        private boolean surfaceCandidate(BlockPos pos){
            // Prioritize a surface/cave entrance at the current elevation. A far
            // underground cavity is not an independently reachable surface source.
            if(pos.getY()>=origin.y-10)return true;
            return pos.getY()>=runtime.player().level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,pos.getX(),pos.getZ())-5;
        }
        JsonObject json(){var out=new JsonObject();out.addProperty("cursor",id);out.addProperty("resource",resource);out.addProperty("radius",radius);out.addProperty("dimension",dimension);out.addProperty("source","loaded_server_resource_query");out.addProperty("loadedOnly",true);out.addProperty("exposedOnly",true);out.addProperty("surfaceBiased",true);out.addProperty("complete",result!=null);out.addProperty("coverageComplete",result!=null && result.coveredEverything());out.addProperty("sampledAtTick",tick);var rows=new JsonArray();
            if(result!=null){
                // Revalidate cached candidates against current loaded state before exposing them.
                for(var hit:result.matches().stream().sorted(Comparator.comparingDouble(h->Vec3.atCenterOf(h.pos()).distanceToSqr(origin))).toList()){
                    if(rows.size()>=limit)break;var pos=hit.pos();if(!runtime.player().level().isLoaded(pos) || !targets.contains(runtime.player().level().getBlockState(pos).getBlock()) || !runtime.perception.exposed(pos))continue;
                    var row=TreeSurvey.position(pos);row.addProperty("block",TreeSurvey.id(runtime.player().level().getBlockState(pos)));row.addProperty("distance",Vec3.atCenterOf(pos).distanceTo(origin));row.addProperty("visible",runtime.perception.inCone(Vec3.atCenterOf(pos),radius) && runtime.perception.rayClear(runtime.player().getEyePosition(),Vec3.atCenterOf(pos),pos));row.addProperty("exposed",true);row.addProperty("reachableVerified",false);rows.add(row);
                }
                out.addProperty("unloadedColumns",result.columnsUnloaded());out.addProperty("scannedColumns",result.columnsScanned());out.addProperty("deadlineHit",result.deadlineHit());out.addProperty("stoppedAfterEnoughCandidates",result.stoppedEarly());
            }
            out.add("results",rows);out.addProperty("meaning","Surface/entrance candidates in loaded terrain; deeper resources remain available through sense. Normal navigation and mining must still verify an approach. Empty or incomplete results never prove world-wide absence.");return out;
        }
    }
}
