package dev.mcai.companion.agent.knowledge;

import com.google.gson.*;
import java.util.*;
import dev.mcai.companion.agent.AgentRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Bounded, source-labelled shared attention. A marker is an observation, not an action authorization. */
public final class PlayerFocus {
    private final AgentRuntime runtime;
    private final Map<UUID,JsonObject> recent=new LinkedHashMap<>();
    private final Map<UUID,Integer> lastMark=new HashMap<>();
    private final Map<UUID,JsonObject> markers=new LinkedHashMap<>();
    public PlayerFocus(AgentRuntime runtime){this.runtime=runtime;}
    public JsonObject capture(ServerPlayer human,boolean explicit){
        if(human.level()!=runtime.player().level())return new JsonObject();
        int now=runtime.server().getTickCount();
        if(explicit && now-lastMark.getOrDefault(human.getUUID(),-100)<10)return new JsonObject();
        if(explicit)lastMark.put(human.getUUID(),now);
        // Use the player's packet-updated aim; LivingEntity.getViewVector uses
        // head animation yaw, which can lag behind a just-received mouse turn.
        Vec3 eyes=human.getEyePosition(),end=eyes.add(human.getLookAngle().scale(PerceptionRange.MAX));
        boolean unloaded=false;
        // Level.clip reads block states: cap the ray before the first unloaded
        // chunk so a marker cannot synchronously load/generate terrain.
        Vec3 direction=human.getLookAngle();
        for(double d=0;d<=PerceptionRange.MAX;d+=.25){
            var sample=eyes.add(direction.scale(d));
            if(!human.level().isLoaded(BlockPos.containing(sample))){end=eyes.add(direction.scale(Math.max(0,d-.25)));unloaded=true;break;}
        }
        var block=human.level().clip(new ClipContext(eyes,end,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,human));
        double distance=block.getType()==HitResult.Type.BLOCK?eyes.distanceToSqr(block.getLocation()):PerceptionRange.MAX*PerceptionRange.MAX;
        net.minecraft.world.entity.Entity entity=null;Vec3 hitPoint=null;
        for(var e:human.level().getEntities(human,human.getBoundingBox().expandTowards(end.subtract(eyes)).inflate(1),e->e.isAlive() && !e.isSpectator() && (e.isPickable() || e instanceof net.minecraft.world.entity.item.ItemEntity))){
            var hit=e.getBoundingBox().inflate(e.getPickRadius()).clip(eyes,end);
            if(hit.isPresent() && eyes.distanceToSqr(hit.get())<distance){entity=e;hitPoint=hit.get();distance=eyes.distanceToSqr(hitPoint);}
        }
        var out=new JsonObject();out.addProperty("speaker",human.getGameProfile().name());out.addProperty("speakerId",human.getUUID().toString());
        out.addProperty("dimension",human.level().dimension().identifier().toString());out.addProperty("gameTick",human.level().getGameTime());out.addProperty("explicitMarker",explicit);
        out.addProperty("source","human_crosshair_server_ray");out.addProperty("meaning","Human pointing evidence, not Agent eyesight or permission to act. Recheck before changing the world.");
        out.addProperty("limitedByUnloadedSpace",unloaded);
        if(human.getMainHandItem().isEmpty()){var empty=new JsonObject();empty.addProperty("item","minecraft:air");empty.addProperty("count",0);out.add("heldItem",empty);}
        else out.add("heldItem",runtime.player().inventoryLedger.describe(human.getMainHandItem()));
        BlockPos at=null;
        if(entity!=null){out.addProperty("kind","entity");out.addProperty("entityId",entity.getUUID().toString());out.addProperty("type",BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());out.addProperty("name",entity.getName().getString());out.add("position",xyz(entity.position()));at=entity.blockPosition();}
        else if(block.getType()==HitResult.Type.BLOCK && human.level().isLoaded(block.getBlockPos())){
            at=block.getBlockPos();out.addProperty("kind","block");out.addProperty("block",BuiltInRegistries.BLOCK.getKey(human.level().getBlockState(at).getBlock()).toString());
            var pos=new JsonObject();pos.addProperty("x",at.getX());pos.addProperty("y",at.getY());pos.addProperty("z",at.getZ());out.add("position",pos);out.addProperty("face",block.getDirection().getSerializedName());
        }else out.addProperty("kind","miss");
        if(entity instanceof net.minecraft.world.entity.item.ItemEntity item){
            out.add("stack",runtime.player().inventoryLedger.describe(item.getItem()));
            out.add("itemSource",DropProvenance.compact(DropProvenance.source(item,runtime.player()),4));
        }
        if(at!=null){
            out.addProperty("distance",Math.sqrt(distance));var nearby=new JsonArray();
            var blockGroups=new LinkedHashMap<String,JsonObject>();
            for(var q:BlockPos.betweenClosed(at.offset(-1,-1,-1),at.offset(1,1,1))){
                if(!human.level().isLoaded(q))continue;var state=human.level().getBlockState(q);if(state.isAir())continue;
                if(!runtime.perception.observableBlock(q) && !runtime.perception.rayClear(eyes,Vec3.atCenterOf(q),q))continue;
                String type=BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();var row=blockGroups.get(type);
                if(row==null){row=new JsonObject();row.addProperty("x",q.getX());row.addProperty("y",q.getY());row.addProperty("z",q.getZ());row.addProperty("block",type);row.addProperty("sampleCount",0);blockGroups.put(type,row);}
                row.addProperty("sampleCount",row.get("sampleCount").getAsInt()+1);
            }out.add("nearbyBlocks",nearby);out.addProperty("nearbyIsComplete",false);
            blockGroups.values().stream().limit(12).forEach(nearby::add);
            var entities=new JsonArray();
            var neighbors=human.level().getEntities(human,new AABB(at).inflate(4),e->e.isAlive() && (e instanceof net.minecraft.world.entity.LivingEntity || e instanceof net.minecraft.world.entity.item.ItemEntity));
            final Vec3 center=Vec3.atCenterOf(at);neighbors.sort(Comparator.comparingDouble(e->e.position().distanceToSqr(center)));
            for(var e:neighbors){
                if(entities.size()>=8)break;
                if(!runtime.perception.sensed(e) && !runtime.perception.rayClear(eyes,e.getEyePosition(),null))continue;
                var row=new JsonObject();row.addProperty("entityId",e.getUUID().toString());row.addProperty("type",BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString());row.add("position",xyz(e.position()));
                if(e instanceof net.minecraft.world.entity.item.ItemEntity item)row.add("stack",runtime.player().inventoryLedger.describe(item.getItem()));
                entities.add(row);
            }out.add("nearbyEntities",entities);
        }
        if(recent.size()>=16 && !recent.containsKey(human.getUUID())){var id=recent.keySet().iterator().next();recent.remove(id);lastMark.remove(id);markers.remove(id);}
        recent.put(human.getUUID(),out);if(explicit && !out.get("kind").getAsString().equals("miss"))markers.put(human.getUUID(),out.deepCopy());return out.deepCopy();
    }
    public JsonArray snapshot(){
        var out=new JsonArray();var rows=new ArrayList<JsonObject>(markers.values());rows.addAll(recent.values().stream().filter(r->!r.get("explicitMarker").getAsBoolean()).toList());for(var row:rows){if(!row.get("dimension").getAsString().equals(runtime.player().level().dimension().identifier().toString()))continue;
            long age=runtime.player().level().getGameTime()-row.get("gameTick").getAsLong();if(age<0 || age>1200)continue;
            var copy=row.deepCopy();copy.addProperty("ageTicks",age);out.add(copy);
        }return out;
    }
    private static JsonObject xyz(Vec3 p){var out=new JsonObject();out.addProperty("x",p.x);out.addProperty("y",p.y);out.addProperty("z",p.z);return out;}
}
