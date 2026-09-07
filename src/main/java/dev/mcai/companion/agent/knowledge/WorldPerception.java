package dev.mcai.companion.agent.knowledge;

import java.util.*;
import com.google.gson.*;
import dev.mcai.companion.agent.body.MinePilotServerPlayer;
import dev.mcai.companion.agent.navigation.NavigationFollower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.attribute.EnvironmentAttributes;

/** Bounded loaded-world sensors; privileged local knowledge is never described as line of sight. */
public final class WorldPerception {
    private static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> PASS_THROUGH =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,net.minecraft.resources.Identifier.fromNamespaceAndPath("mcai_companion","perception_passthrough"));
    private final MinePilotServerPlayer player;
    private final List<net.minecraft.core.component.DataComponentType<?>> variantTypes=new ArrayList<>();
    private final Set<UUID> observedEntities=new HashSet<>();
    private long lastEntityTick=-100;
    private JsonObject entitySnapshot;
    private final LinkedHashMap<String,ScanPage> scans=new LinkedHashMap<>();
    public WorldPerception(MinePilotServerPlayer player){
        this.player=player;
        for(var type:BuiltInRegistries.DATA_COMPONENT_TYPE){String id=BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type).getPath();
            if(id.endsWith("/variant") || id.endsWith("_variant"))variantTypes.add(type);}
    }

    public static double normalize(double heading) {
        if(!Double.isFinite(heading))throw new IllegalArgumentException("Heading must be finite");
        return (heading%360+360)%360;
    }
    public static double bearing(Vec3 from,Vec3 to){return normalize(Math.toDegrees(Math.atan2(to.x-from.x,from.z-to.z)));}
    public static double difference(double a,double b){return Math.abs((normalize(a-b)+180)%360-180);}
    public static double relativeHeading(double bearing,String side){return normalize(bearing+switch(side){case "facing"->0;case "back"->180;case "left"->90;case "right"->-90;default->throw new IllegalArgumentException("Unknown relative facing");});}
    public boolean inCone(Vec3 point,double range) {
        Vec3 delta=point.subtract(player.getEyePosition());if(delta.lengthSqr()>range*range)return false;
        double pitch=-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z)));
        return difference(bearing(player.getEyePosition(),point),NavigationFollower.minecraftYawToHeading(player.getYRot()))<=60
                && Math.abs(pitch-player.getXRot())<=60;
    }
    public boolean visible(Entity entity) {
        if(entity.isInvisible() || entity instanceof Player p && p.isSpectator())return false;
        var box=entity.getBoundingBox();
        for(Vec3 point:List.of(entity.getEyePosition(),box.getCenter(),new Vec3(entity.getX(),box.minY+.1,entity.getZ())))
            if(inCone(point,96) && rayClear(player.getEyePosition(),point,null))return true;
        return false;
    }
    public boolean sensed(Entity entity) {
        Vec3 d=entity.position().subtract(player.position());
        return entity.level()==player.level() && (entity instanceof ItemEntity?d.lengthSqr()<=100:
                entity instanceof LivingEntity && Math.abs(d.x)<=16 && Math.abs(d.y)<=16 && Math.abs(d.z)<=16) || entity.level()==player.level() && visible(entity);
    }
    public Entity target(String reference) {
        Entity match=null;
        for(var entity:player.level().getEntities(player,player.getBoundingBox().inflate(96))) {
            if(!entity.isAlive() || !(entity.getUUID().toString().equals(reference) || entity.getName().getString().equalsIgnoreCase(reference)) || !sensed(entity))continue;
            if(match!=null)throw new IllegalArgumentException("Observed entity name is ambiguous; use its UUID");match=entity;
        }
        if(match==null)throw new IllegalArgumentException("Reference entity is not currently observable");return match;
    }

    public boolean transparent(BlockState state) { return transparent(state,player.blockPosition()); }
    public boolean transparent(BlockState state,BlockPos position) {
        if(state.is(Blocks.REDSTONE_BLOCK))return false;
        if(state.is(PASS_THROUGH))return true;
        String id=BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return state.isAir() || !state.getFluidState().isEmpty() && state.getCollisionShape(player.level(),position).isEmpty()
                || state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS) || state.is(BlockTags.SLABS)
                || state.is(BlockTags.FENCES) || state.is(BlockTags.FENCE_GATES) || state.is(BlockTags.DOORS)
                || id.contains("glass") || id.endsWith("chest") || id.endsWith("shulker_box") || id.equals("barrel") || id.endsWith("bars") || id.endsWith("sign") || id.endsWith("_carpet")
                || id.contains("candle") || id.contains("bamboo") || id.endsWith("button") || id.endsWith("pressure_plate")
                || id.contains("piston") || id.contains("trapdoor") || id.endsWith("rail")
                || Set.of("enchanting_table","sugar_cane","lily_pad","redstone_wire","repeater","comparator","lever","observer",
                "hopper","dispenser","dropper","daylight_detector","redstone_lamp","redstone_torch","redstone_wall_torch","target","tripwire","tripwire_hook").contains(id);
    }
    /** Exact voxel traversal with per-block shape intersection. Empty/unloaded space are distinguished. */
    public boolean rayClear(Vec3 from,Vec3 to,BlockPos endpoint) {
        Vec3 d=to.subtract(from);int x=(int)Math.floor(from.x),y=(int)Math.floor(from.y),z=(int)Math.floor(from.z);
        int sx=d.x>0?1:d.x<0?-1:0,sy=d.y>0?1:d.y<0?-1:0,sz=d.z>0?1:d.z<0?-1:0;
        double tx=boundary(from.x,d.x,x,sx),ty=boundary(from.y,d.y,y,sy),tz=boundary(from.z,d.z,z,sz);
        double dx=sx==0?Double.POSITIVE_INFINITY:Math.abs(1/d.x),dy=sy==0?Double.POSITIVE_INFINITY:Math.abs(1/d.y),dz=sz==0?Double.POSITIVE_INFINITY:Math.abs(1/d.z);
        for(int n=0;n<512;n++) {
            BlockPos p=new BlockPos(x,y,z);if(!player.level().isLoaded(p))return false;
            if(endpoint!=null && p.equals(endpoint))return true;
            var state=player.level().getBlockState(p);
            if(!transparent(state,p) && state.getShape(player.level(),p).clip(from,to,p)!=null)return false;
            double t=Math.min(tx,Math.min(ty,tz));if(t>1)return true;
            if(tx<=t){x+=sx;tx+=dx;}if(ty<=t){y+=sy;ty+=dy;}if(tz<=t){z+=sz;tz+=dz;}
        }return false;
    }
    private static double boundary(double start,double delta,int cell,int step){return step==0?Double.POSITIVE_INFINITY:((step>0?cell+1:cell)-start)/delta;}

    public JsonObject basics() {
        var level=player.level();long time=level.getDefaultClockTime();JsonObject out=new JsonObject();
        out.addProperty("gameTick",level.getGameTime());out.addProperty("dayTime",Math.floorMod(time,24000));out.addProperty("day",Math.floorDiv(time,24000));
        out.addProperty("clockSource","dimension_default_clock");out.addProperty("weather",!level.canHaveWeather()?"none":level.isThundering()?"thunder":level.isRaining()?"rain":"clear");
        long dayTick=Math.floorMod(time,24000);out.addProperty("clock24h",String.format(Locale.ROOT,"%02d:%02d",(dayTick/1000+6)%24,(dayTick%1000)*60/1000));
        out.addProperty("rainingAtBody",level.isRainingAt(player.blockPosition()));out.addProperty("difficulty",level.getDifficulty().getSerializedName());
        out.addProperty("gameMode",player.gameMode.getGameModeForPlayer().getName());out.addProperty("dimension",level.dimension().identifier().toString());
        out.addProperty("biome",level.getBiome(player.blockPosition()).unwrapKey().map(k->k.identifier().toString()).orElse("unknown"));
        return out;
    }
    public double sunBearing() {
        var level=player.level();
        if(!level.canHaveWeather() || level.isRaining())throw new IllegalArgumentException("The sun is unavailable in this dimension/weather");
        double angle=Math.toRadians(level.environmentAttributes().getValue(EnvironmentAttributes.SUN_ANGLE,player.getEyePosition(),null));
        Vec3 direction=new Vec3(-Math.sin(angle),Math.cos(angle),0);
        if(direction.y<=0 || Math.abs(direction.x)<.01)throw new IllegalArgumentException("The sun is below the horizon or too close to the zenith for a horizontal bearing");
        if(!rayClear(player.getEyePosition(),player.getEyePosition().add(direction.scale(96)),null))throw new IllegalArgumentException("The sun is occluded or its ray crosses unloaded space");
        return direction.x>0?90:270;
    }
    public JsonObject entities(int offset,int limit,String filter,int radius) {
        return entities(offset,limit,filter,radius,false);
    }
    public JsonObject entities(int offset,int limit,String filter,int radius,boolean itemsOnly) {
        if(offset<0 || limit<1 || limit>64 || radius<1 || radius>96 || filter.length()>128)throw new IllegalArgumentException("Invalid entity query bounds");
        JsonArray all=new JsonArray();int examined=0;boolean capped=false;
        var groups=new java.util.TreeMap<String,int[]>();
        var candidates=new ArrayList<Entity>();
        player.level().getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(Entity.class),
                player.getBoundingBox().inflate(radius),e->e!=player && e.isAlive() && (e instanceof LivingEntity || e instanceof ItemEntity),candidates,2049);
        candidates.sort(Comparator.<Entity>comparingDouble(player::distanceToSqr).thenComparing(e->e.getUUID().toString()));
        observedEntities.clear();
        for(Entity e:candidates) {
            if(++examined>2048){capped=true;break;}
            if(!(e instanceof LivingEntity || e instanceof ItemEntity) || !e.isAlive())continue;
            if(itemsOnly && !(e instanceof ItemEntity))continue;
            Vec3 delta=e.position().subtract(player.position());
            if(Math.max(Math.abs(delta.x),Math.max(Math.abs(delta.y),Math.abs(delta.z)))>radius)continue;
            boolean nearby=e instanceof ItemEntity?delta.lengthSqr()<=100:Math.abs(delta.x)<=16 && Math.abs(delta.y)<=16 && Math.abs(delta.z)<=16;
            boolean visual=inCone(e.getEyePosition(),radius) && visible(e);
            if(!nearby && !visual)continue;
            observedEntities.add(e.getUUID());
            String type=BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
            String text=(type+" "+e.getName().getString()+(e instanceof ItemEntity item?" "+BuiltInRegistries.ITEM.getKey(item.getItem().getItem()):"")).toLowerCase(Locale.ROOT);
            if(!text.contains(filter.toLowerCase(Locale.ROOT)))continue;
            JsonObject row=new JsonObject();row.addProperty("uuid",e.getUUID().toString());row.addProperty("type",type);position(row,e.position());
            row.addProperty("sense",visual?"vision": "proximity_occlusion_bypass");row.addProperty("visible",visual);
            double heading=NavigationFollower.minecraftYawToHeading(e instanceof LivingEntity living?living.yBodyRot:e.getYRot());row.addProperty("heading",heading);
            row.addProperty("headHeading",NavigationFollower.minecraftYawToHeading(e.getYHeadRot()));
            double side=normalize(bearing(e.position(),player.position())-heading);
            row.addProperty("relativeFacing",side<45 || side>=315?"facing":side<135?"right":side<225?"back":"left");
            if(e instanceof Player || e.hasCustomName())row.addProperty("name",InventoryLedger.bounded(e.getName().getString(),128));
            if(e instanceof LivingEntity living)row.addProperty("age",living.isBaby()?"baby":"adult");
            JsonObject variants=new JsonObject();for(var componentType:variantTypes){var value=variant(e,componentType);if(value!=null)variants.add(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(componentType).toString(),value);}
            if(!variants.isEmpty())row.add("variants",variants);
            var data=e instanceof Villager villager?villager.getVillagerData():e instanceof ZombieVillager zombie?zombie.getVillagerData():null;
            if(data!=null){row.addProperty("profession",data.profession().unwrapKey().map(k->k.identifier().toString()).orElse("unknown"));row.addProperty("variant",data.type().unwrapKey().map(k->k.identifier().toString()).orElse("unknown"));row.addProperty("professionLevel",data.level());}
            if(e instanceof ItemEntity item){row.add("stack",player.inventoryLedger.describe(item.getItem()));row.add("source",DropProvenance.source(item,player));}
            String group=e instanceof ItemEntity item?"item/"+BuiltInRegistries.ITEM.getKey(item.getItem().getItem()):type;
            int[] totals=groups.computeIfAbsent(group,k->new int[2]);totals[0]++;if(e instanceof ItemEntity item)totals[1]+=item.getItem().getCount();
            all.add(row);
        }
        JsonObject out=page(all,offset,limit);JsonArray summary=new JsonArray();
        for(var entry:groups.entrySet()){if(summary.size()>=32)break;var row=new JsonObject();row.addProperty("type",entry.getKey());row.addProperty("entityOrStackCount",entry.getValue()[0]);row.addProperty("itemCount",entry.getValue()[1]);summary.add(row);}
        out.add("groupSummary",summary);out.addProperty("groupSummaryTruncated",groups.size()>summary.size());
        out.addProperty("dimension",player.level().dimension().identifier().toString());out.addProperty("scanCapped",capped);out.addProperty("loadedOnly",true);out.addProperty("radius",radius);out.addProperty("sampledAtTick",player.level().getGameTime());return out;
    }
    public JsonObject summary() {
        if(entitySnapshot==null || player.level().getGameTime()-lastEntityTick>=20){entitySnapshot=entities(0,8,"",96);lastEntityTick=player.level().getGameTime();}
        JsonObject out=basics();out.add("nearbySummary",entitySnapshot.deepCopy());out.addProperty("detailsTool","sense");return out;
    }
    public JsonObject blocks(int radius,String filter,String kind,String cursor,int limit) {
        if(radius<1 || radius>96 || limit<1 || limit>64 || filter.length()>128)throw new IllegalArgumentException("Invalid block query bounds");
        ScanPage scan;
        if(cursor.isBlank()) {
            if(scans.size()>=8)scans.remove(scans.keySet().iterator().next());
            scan=new ScanPage(UUID.randomUUID().toString(),player.blockPosition(),player.level().dimension().identifier().toString(),radius,filter,kind,player.level().getGameTime());scans.put(scan.id,scan);
        } else {scan=scans.get(cursor);if(scan==null)throw new IllegalArgumentException("Unknown or expired scan cursor");}
        if(player.level().getGameTime()-scan.tick>1200 || !player.level().dimension().identifier().toString().equals(scan.dimension)) {scans.remove(scan.id);throw new IllegalArgumentException("Scan expired or dimension changed");}
        JsonArray found=new JsonArray();int width=2*scan.radius+1;long total=(long)width*width*width;int checked=0,unloaded=0;
        long deadline=System.nanoTime()+5_000_000L;
        while(scan.index<total && checked<4096 && found.size()<limit && System.nanoTime()<deadline) {
            BlockPos offset=shellOffset(scan.index++);
            BlockPos p=scan.origin.offset(offset);checked++;
            if(!player.level().isLoaded(p) || p.getY()<player.level().getMinY() || p.getY()>=player.level().getMaxY()){unloaded++;continue;}
            var state=player.level().getBlockState(p);if(state.isAir())continue;
            String id=BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            if(!id.contains(scan.filter))continue;
            if(scan.kind.equals("trees") && !state.is(BlockTags.LOGS))continue;
            if(scan.kind.equals("structures") && !Set.of("minecraft:nether_portal","minecraft:end_portal_frame","minecraft:spawner","minecraft:bell").contains(id))continue;
            Vec3 point=Vec3.atCenterOf(p);Vec3 d=point.subtract(player.position());boolean proximity=Math.abs(d.x)<=10.5 && Math.abs(d.y)<=10.5 && Math.abs(d.z)<=10.5;
            boolean visible=inCone(point,scan.radius) && rayClear(player.getEyePosition(),point,p);
            if(!proximity && !visible)continue;
            JsonObject row=new JsonObject();row.addProperty("block",id);position(row,point);row.addProperty("state",state.toString());row.addProperty("sense",visible?"vision":"proximity_occlusion_bypass");row.addProperty("rayPassThrough",transparent(state,p));
            if(!scan.kind.equals("blocks"))row.addProperty("classification",scan.kind.equals("trees")?"observed_log_candidate":"observed_structure_marker_not_confirmed_structure");found.add(row);
        }
        JsonObject out=new JsonObject();out.add("results",found);out.addProperty("cursor",scan.id);out.addProperty("complete",scan.index>=total);out.addProperty("scannedCells",checked);out.addProperty("unloadedCells",unloaded);out.addProperty("examinedSoFar",scan.index);out.addProperty("totalCandidateCells",total);out.addProperty("loadedOnly",true);out.addProperty("sampledAtTick",player.level().getGameTime());
        out.addProperty("dimension",scan.dimension);out.addProperty("scanStartedAtTick",scan.tick);out.addProperty("snapshotIsAtomic",false);
        scan.unloaded+=unloaded;out.addProperty("unloadedCellsSoFar",scan.unloaded);
        if(scan.index>=total)scans.remove(scan.id);return out;
    }
    /** Enumerate each voxel once, in expanding Chebyshev shells, so early pages inspect nearby space first. */
    public static BlockPos shellOffset(long index) {
        if(index==0)return BlockPos.ZERO;
        int r=(int)Math.ceil((Math.cbrt(index+1)-1)/2);
        while((long)(2*r+1)*(2*r+1)*(2*r+1)<=index)r++;
        while(r>0 && (long)(2*r-1)*(2*r-1)*(2*r-1)>index)r--;
        int w=2*r+1,inner=w-2;long n=index-(long)inner*inner*inner;
        if(n<2L*w*w){int face=(int)(n/(w*w));n%=w*w;return new BlockPos((int)(n%w)-r,face==0?-r:r,(int)(n/w)-r);}
        n-=2L*w*w;
        if(n<2L*w*inner){int face=(int)(n/(w*inner));n%=w*inner;return new BlockPos((int)(n%w)-r,(int)(n/w)-r+1,face==0?-r:r);}
        n-=2L*w*inner;int face=(int)(n/(inner*inner));n%=inner*inner;return new BlockPos(face==0?-r:r,(int)(n%inner)-r+1,(int)(n/inner)-r+1);
    }
    private static JsonObject page(JsonArray rows,int offset,int limit){JsonObject out=new JsonObject();JsonArray page=new JsonArray();for(int i=offset;i<rows.size() && page.size()<limit;i++)page.add(rows.get(i));out.add("results",page);out.addProperty("totalMatched",rows.size());out.addProperty("nextOffset",offset+page.size());out.addProperty("truncated",offset+page.size()<rows.size());return out;}
    private <T> JsonElement variant(Entity entity,net.minecraft.core.component.DataComponentType<T> type){
        T value=entity.get(type);if(value==null || type.codec()==null)return null;
        var encoded=type.codec().encodeStart(player.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE),value).result();
        return encoded.filter(v->v.toString().length()<=256).orElse(null);
    }
    private static void position(JsonObject row,Vec3 p){row.addProperty("x",p.x);row.addProperty("y",p.y);row.addProperty("z",p.z);}
    private static final class ScanPage {
        final String id,dimension,filter,kind;final BlockPos origin;final int radius;final long tick;long index,unloaded;
        ScanPage(String id,BlockPos origin,String dimension,int radius,String filter,String kind,long tick){this.id=id;this.origin=origin;this.dimension=dimension;this.radius=radius;this.filter=filter;this.kind=kind;this.tick=tick;}
    }
}
