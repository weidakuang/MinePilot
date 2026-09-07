package dev.mcai.companion.agent.knowledge;

import com.google.gson.*;
import dev.mcai.companion.agent.body.MinePilotServerPlayer;
import dev.mcai.companion.agent.navigation.NavigationFollower;
import java.util.*;
import java.util.function.LongSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Recent received sound captions. Hearing has no visual ray/occlusion gate. */
public final class SoundPerception {
    public static final long DISPLAY_NANOS=3_000_000_000L;
    private static final String[] DIRECTIONS={"front","front_right","right","back_right","back","back_left","left","front_left"};
    private static final String[] DIRECTION_TEXT={"前方","右前方","右方","右后方","后方","左后方","左方","左前方"};
    private final MinePilotServerPlayer player;
    private final VanillaSoundCatalog catalog=new VanillaSoundCatalog();
    private final LongSupplier clock;
    private final LinkedHashMap<String,Heard> recent=new LinkedHashMap<>();
    private long sequence;
    private long evictedUntil;
    private record Heard(long sequence,long time,String dimension,String sound,String category,
                         VanillaSoundCatalog.Caption caption,Vec3 position,JsonObject source) {}
    public SoundPerception(MinePilotServerPlayer player){this(player,System::nanoTime);}
    public SoundPerception(MinePilotServerPlayer player,LongSupplier clock){this.player=player;this.clock=clock;}

    public void receive(Packet<?> packet) {
        if(packet instanceof ClientboundSoundPacket sound) {
            Vec3 position=new Vec3(sound.getX(),sound.getY(),sound.getZ());
            capture(sound.getSound().value().location().toString(),sound.getSource(),sound.getVolume(),sound.getSeed(),position,positionSource(position,sound.getSource()));
        } else if(packet instanceof ClientboundSoundEntityPacket sound) {
            Entity entity=player.level().getEntity(sound.getId());
            if(entity==null || entity.isSilent())return;
            // EntityBoundSoundInstance records float positions when playback starts.
            Vec3 position=new Vec3((float)entity.getX(),(float)entity.getY(),(float)entity.getZ());
            capture(sound.getSound().value().location().toString(),sound.getSource(),sound.getVolume(),sound.getSeed(),position,entitySource(entity,"entity_packet"));
        } else if(packet instanceof net.minecraft.network.protocol.game.ClientboundRespawnPacket) {
            recent.clear();
        }
    }
    private void capture(String sound,SoundSource category,float volume,long seed,Vec3 position,JsonObject source) {
        var caption=catalog.resolve(sound,volume,seed);if(caption==null)return;
        long now=clock.getAsLong();purge(now);
        String identity=source.has("uuid")?source.get("uuid").getAsString():position.toString();
        String key=caption.key()+"|"+identity+"|"+position;
        recent.remove(key);
        if(recent.size()>=128){var first=recent.keySet().iterator().next();evictedUntil=Math.max(evictedUntil,recent.remove(first).sequence());}
        recent.put(key,new Heard(++sequence,now,dimension(),sound,category.getName(),caption,position,source));
    }
    private void purge(long now){recent.values().removeIf(e->now-e.time()>DISPLAY_NANOS || !e.dimension().equals(dimension()));}
    private String dimension(){return player.level().dimension().identifier().toString();}

    public static int directionIndex(double heading,Vec3 listener,Vec3 sound) {
        if(Math.hypot(sound.x-listener.x,sound.z-listener.z)<1e-6)return -1;
        double relative=WorldPerception.normalize(WorldPerception.bearing(listener,sound)-heading);
        return (int)Math.floor((relative+22.5)/45)%8;
    }
    public static boolean withinSubtitleRange(Vec3 listener,Vec3 sound,double range){return listener.distanceToSqr(sound)<range*range;}

    public JsonObject query(long after,int limit) {
        if(after<0 || limit<1 || limit>64)throw new IllegalArgumentException("Sound cursor must be nonnegative and limit must be 1..64");
        long now=clock.getAsLong();purge(now);Vec3 ears=player.getEyePosition();
        double heading=NavigationFollower.minecraftYawToHeading(player.getYRot());
        JsonArray rows=new JsonArray();int matched=0;long cursor=after;
        for(var heard:recent.values()) {
            if(heard.sequence()<=after || !withinSubtitleRange(ears,heard.position(),heard.caption().range()))continue;
            matched++;if(rows.size()>=limit)continue;
            var row=new JsonObject();row.addProperty("sequence",heard.sequence());row.addProperty("subtitleKey",heard.caption().key());row.addProperty("subtitle",heard.caption().text());
            row.addProperty("sound",heard.sound());row.addProperty("category",heard.category());
            row.add("source",heard.source().deepCopy());
            int direction=directionIndex(heading,ears,heard.position());
            if(heard.source().has("self") && heard.source().get("self").getAsBoolean()) {row.add("direction",JsonNull.INSTANCE);row.addProperty("directionText","自身");}
            else if(direction>=0){row.addProperty("direction",DIRECTIONS[direction]);row.addProperty("directionText",DIRECTION_TEXT[direction]);}
            else {row.add("direction",JsonNull.INSTANCE);row.addProperty("directionText","同一水平位置");}
            double dy=heard.position().y-ears.y;row.addProperty("vertical",Math.abs(dy)<.5?"level":dy>0?"above":"below");
            row.addProperty("distanceBlocks",ears.distanceTo(heard.position()));row.addProperty("subtitleRangeBlocks",heard.caption().range());
            row.addProperty("ageSeconds",(now-heard.time())/1e9);rows.add(row);cursor=heard.sequence();
        }
        var out=new JsonObject();out.add("sounds",rows);out.addProperty("dimension",dimension());
        var listener=new JsonObject();listener.addProperty("x",ears.x);listener.addProperty("y",ears.y);listener.addProperty("z",ears.z);listener.addProperty("heading",heading);out.add("listenerEyes",listener);out.addProperty("totalMatched",matched);
        out.addProperty("truncated",matched>rows.size());out.addProperty("nextSequence",cursor);out.addProperty("latestSequence",sequence);out.addProperty("capacityHistoryLost",after>0 && after<evictedUntil);
        out.addProperty("displaySeconds",3);out.addProperty("rangePolicy","vanilla_26.2_selected_sound_attenuation_from_first_person_ears");
        out.addProperty("coverage","received_sound_and_entity_sound_packets; client-local/level-event-only sounds and resource-pack overrides not emulated");
        out.addProperty("grouping","caption_and_source_position; multiple sources retained rather than collapsed to the nearest");
        return out;
    }
    private JsonObject entitySource(Entity entity,String confidence) {
        var source=new JsonObject();String type=BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        source.addProperty("kind",entity instanceof Player?"player":"entity");source.addProperty("type",type);source.addProperty("uuid",entity.getUUID().toString());
        String name=entity instanceof Player p?p.getGameProfile().name():catalog.label(entity.getType().getDescriptionId(),entity.getType().getDescription().getString());
        source.addProperty("name",InventoryLedger.bounded(name,128));source.addProperty("label",entity instanceof Player?"Player:"+name:name);
        if(entity.hasCustomName())source.addProperty("customName",InventoryLedger.bounded(entity.getName().getString(),128));
        source.addProperty("self",entity==player);source.addProperty("confidence",confidence);return source;
    }
    static Vec3 packetPosition(Vec3 position) {
        return new Vec3((int)(position.x*8)/8.0F,(int)(position.y*8)/8.0F,(int)(position.z*8)/8.0F);
    }
    private static double packetPositionTolerance(Vec3 position) {
        return .125+Math.max(Math.ulp((float)position.x),Math.max(Math.ulp((float)position.y),Math.ulp((float)position.z)))/2;
    }
    private JsonObject positionSource(Vec3 position,SoundSource category) {
        // Positional packets have no actor ID. A unique nearby position is a
        // labelled candidate, never a claim that this actor emitted the packet.
        if(category!=SoundSource.BLOCKS && category!=SoundSource.RECORDS && category!=SoundSource.AMBIENT) {
            var candidates=player.level().getEntities((Entity)null,new AABB(position,position).inflate(packetPositionTolerance(position)),
                    e->e.getSoundSource()==category && packetPosition(e.position()).equals(position));
            if(candidates.size()==1)return entitySource(candidates.getFirst(),"position_match_unconfirmed");
        }
        if(category==SoundSource.BLOCKS || category==SoundSource.RECORDS) {
            // Packet positions are quantized to eighth-block integers, then floats.
            // At large world coordinates even a block center can round into its neighbor.
            var positions=new ArrayList<BlockPos>();
            double tolerance=packetPositionTolerance(position);
            var low=BlockPos.containing(position.subtract(tolerance,tolerance,tolerance));
            var high=BlockPos.containing(position.add(tolerance,tolerance,tolerance));
            for(var cell:BlockPos.betweenClosed(low,high)) {
                if(player.level().isLoaded(cell) && !player.level().getBlockState(cell).isAir()
                        && packetPosition(Vec3.atCenterOf(cell)).equals(position))positions.add(cell.immutable());
            }
            if(positions.size()==1) {
                var block=player.level().getBlockState(positions.getFirst()).getBlock();
                var source=new JsonObject();
                source.addProperty("kind","block");source.addProperty("type",BuiltInRegistries.BLOCK.getKey(block).toString());
                source.addProperty("name",catalog.label(block.getDescriptionId(),block.getName().getString()));source.add("label",source.get("name"));
                source.addProperty("confidence","block_at_sound_position_unconfirmed");return source;
            }
        }
        var source=new JsonObject();source.addProperty("kind","unknown");source.addProperty("label","未知声源");source.addProperty("confidence","position_only");return source;
    }
}
