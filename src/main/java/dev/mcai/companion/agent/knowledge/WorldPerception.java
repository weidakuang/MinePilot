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
    public final StructurePerception structures;
    private final List<net.minecraft.core.component.DataComponentType<?>> variantTypes=new ArrayList<>();
    private final LinkedHashMap<String,EntityScan> entityScans=new LinkedHashMap<>();
    private int budgetTick=Integer.MIN_VALUE;private long scanNanos;private double peakScanMillis;
    public long beginWork(long maximumNanos){int tick=player.level().getServer().getTickCount();if(tick!=budgetTick){budgetTick=tick;scanNanos=0;}return dev.mcai.companion.agent.concurrent.MainThreadBudget.of(player.level().getServer()).deadline(maximumNanos);}
    public void recordWork(long began){int tick=player.level().getServer().getTickCount();if(tick!=budgetTick){budgetTick=tick;scanNanos=0;}dev.mcai.companion.agent.concurrent.MainThreadBudget.of(player.level().getServer()).record(began);scanNanos+=System.nanoTime()-began;peakScanMillis=Math.max(peakScanMillis,scanNanos/1_000_000.0);}
    public JsonObject performance(){var out=new JsonObject();var shared=dev.mcai.companion.agent.concurrent.MainThreadBudget.of(player.level().getServer());
        out.addProperty("sharedWorldReadBudgetMillis",shared.limitNanos()/1_000_000.0);out.addProperty("sharedWorldReadMillis",shared.usedNanos()/1_000_000.0);out.addProperty("peakSharedWorldReadMillis",shared.peakNanos()/1_000_000.0);
        out.addProperty("analysisWorkers",dev.mcai.companion.agent.concurrent.AnalysisWorkers.workerCount());out.addProperty("analysisActive",dev.mcai.companion.agent.concurrent.AnalysisWorkers.active());out.addProperty("analysisQueued",dev.mcai.companion.agent.concurrent.AnalysisWorkers.queued());out.addProperty("scanBudgetMillisPerTick",3);out.addProperty("currentTickScanMillis",scanNanos/1_000_000.0);out.addProperty("peakTickScanMillis",peakScanMillis);out.addProperty("activeBlockScans",scans.size());out.addProperty("activeEntityScans",entityScans.size());return out;}
    private long lastEntityTick=-100;
    private JsonObject entitySnapshot;
    private final LinkedHashMap<String,ScanPage> scans=new LinkedHashMap<>();
    public WorldPerception(MinePilotServerPlayer player){
        this.player=player;
        this.structures=new StructurePerception(player);
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
    public static boolean insideProximity(Vec3 origin, Vec3 point, double requestedRadius) {
        double radius = Math.min(10, requestedRadius);
        return point.distanceToSqr(origin) <= radius * radius;
    }
    public boolean observableBlock(BlockPos position) {
        if (!player.level().isLoaded(position)) return false;
        Vec3 point = Vec3.atCenterOf(position);
        return insideProximity(player.position(), point, 10)
                || inCone(point,PerceptionRange.MAX) && rayClear(player.getEyePosition(),point,position);
    }
    public boolean visible(Entity entity) {return visible(entity,PerceptionRange.MAX);}
    private boolean visible(Entity entity,int radius) {
        if(entity.isInvisible() || entity instanceof Player p && p.isSpectator())return false;
        var box=entity.getBoundingBox();
        for(Vec3 point:List.of(entity.getEyePosition(),box.getCenter(),new Vec3(entity.getX(),box.minY+.1,entity.getZ())))
            if(inCone(point,radius) && rayClear(player.getEyePosition(),point,null))return true;
        return false;
    }
    public boolean sensed(Entity entity) {
        return entity.level()==player.level() && entity.isAlive() && player.level().isLoaded(entity.blockPosition())
                && entity.position().distanceToSqr(player.position())<=PerceptionRange.MAX*(double)PerceptionRange.MAX;
    }
    public Entity target(String reference) {
        Entity match=null;
        for(var entity:player.level().getEntities(player,player.getBoundingBox().inflate(PerceptionRange.MAX))) {
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
        if(!rayClear(player.getEyePosition(),player.getEyePosition().add(direction.scale(PerceptionRange.MAX)),null))throw new IllegalArgumentException("The sun is occluded or its ray crosses unloaded space");
        return direction.x>0?90:270;
    }
    public JsonObject entities(int offset,int limit,String filter,int radius) {
        return entities(offset,limit,filter,radius,false);
    }
    public JsonObject entities(int offset,int limit,String filter,int radius,boolean itemsOnly) {return entities(offset,limit,filter,radius,itemsOnly,"");}
    public JsonObject entities(int offset,int limit,String filter,int radius,boolean itemsOnly,String cursor) {
        if(offset<0 || limit<1 || limit>64 || radius<1 || radius>PerceptionRange.MAX || filter.length()>128)throw new IllegalArgumentException("Invalid entity query bounds");
        long began=System.nanoTime(),deadline=beginWork(3_000_000L);
        EntityScan scan=null;
        if(!cursor.isBlank())scan=entityScans.get(cursor);
        else if(offset>0)for(var candidate:entityScans.values())if(candidate.radius==radius && candidate.itemsOnly==itemsOnly && candidate.filter.equals(filter))scan=candidate;
        if(scan==null){
            if(!cursor.isBlank() || offset>0)throw new IllegalArgumentException("Unknown/expired entity scan; begin a fresh query at offset 0");
            var candidates=new ArrayList<Entity>();player.level().getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(Entity.class),player.getBoundingBox().inflate(radius),e->e!=player && e.isAlive() && (e instanceof LivingEntity || e instanceof ItemEntity) && (!itemsOnly || e instanceof ItemEntity),candidates,2049);
            candidates.sort(Comparator.<Entity>comparingDouble(player::distanceToSqr).thenComparing(e->e.getUUID().toString()));
            scan=new EntityScan(radius,filter,itemsOnly,candidates.stream().limit(2048).map(Entity::getUUID).toList(),candidates.size()>2048);
            if(entityScans.size()>=8)entityScans.remove(entityScans.keySet().iterator().next());entityScans.put(scan.id,scan);
        }
        if(scan.itemsOnly!=itemsOnly)throw new IllegalArgumentException("Entity cursor kind does not match this query");
        if(player.level().getGameTime()-scan.tick>1200 || !scan.dimension.equals(player.level().dimension().identifier().toString())){entityScans.remove(scan.id);throw new IllegalArgumentException("Entity scan expired or changed dimension");}
        int checked=0;
        while(scan.index<scan.candidates.size() && scan.rows.size()<offset+limit && checked++<256 && System.nanoTime()<deadline){
            Entity e=player.level().getEntity(scan.candidates.get(scan.index++));if(e==null || !e.isAlive())continue;
            Vec3 scopeDelta=e.position().subtract(scan.origin);
            if(scopeDelta.lengthSqr()>scan.radius*(double)scan.radius)continue;
            String type=BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();String text=(type+" "+e.getName().getString()+(e instanceof ItemEntity item?" "+BuiltInRegistries.ITEM.getKey(item.getItem().getItem()):"")).toLowerCase(Locale.ROOT);
            if(!PerceptionQuery.entity(e,scan.filter))continue;
            Vec3 delta=e.position().subtract(player.position());boolean nearby=e instanceof ItemEntity?delta.lengthSqr()<=100:Math.abs(delta.x)<=16 && Math.abs(delta.y)<=16 && Math.abs(delta.z)<=16;
            boolean visual=visible(e,scan.radius);
            scan.rows.add(describeEntity(e,type,visual));String group=e instanceof ItemEntity item?"item/"+BuiltInRegistries.ITEM.getKey(item.getItem().getItem()):type;
            int[] totals=scan.groups.computeIfAbsent(group,k->new int[2]);totals[0]++;if(e instanceof ItemEntity item)totals[1]+=item.getItem().getCount();
        }
        recordWork(began);JsonObject out=page(scan.rows,offset,limit);JsonArray summary=new JsonArray();
        for(var entry:scan.groups.entrySet()){if(summary.size()>=32)break;var row=new JsonObject();row.addProperty("type",entry.getKey());row.addProperty("entityOrStackCount",entry.getValue()[0]);row.addProperty("itemCount",entry.getValue()[1]);summary.add(row);}
        boolean complete=scan.index>=scan.candidates.size();boolean moved=player.position().distanceToSqr(scan.origin)>.25 || difference(scan.heading,NavigationFollower.minecraftYawToHeading(player.getYRot()))>4 || Math.abs(scan.pitch-player.getXRot())>4;
        out.add("groupSummary",summary);out.addProperty("groupSummaryTruncated",scan.groups.size()>summary.size());out.addProperty("groupSummaryScope","Observed candidates so far in this cursor, not all nearby entities until complete");
        out.addProperty("cursor",scan.id);out.addProperty("complete",complete);out.addProperty("totalIsFinal",complete && !scan.capped);out.addProperty("truncated",out.get("truncated").getAsBoolean() || !complete);out.addProperty("scanCapped",scan.capped);out.addProperty("coverageComplete",complete && !scan.capped && !moved);
        out.addProperty("dimension",scan.dimension);out.addProperty("loadedOnly",true);out.addProperty("radius",scan.radius);out.addProperty("scanStartedAtTick",scan.tick);out.addProperty("sampledAtTick",player.level().getGameTime());out.addProperty("snapshotIsAtomic",false);out.addProperty("viewChangedSinceStart",moved);out.addProperty("examinedCandidates",scan.index);out.addProperty("candidateCount",scan.candidates.size());out.addProperty("pageServerMillis",(System.nanoTime()-began)/1_000_000.0);return out;
    }
    private JsonObject describeEntity(Entity e,String type,boolean visual){
        JsonObject row=new JsonObject();row.addProperty("uuid",e.getUUID().toString());row.addProperty("type",type);position(row,e.position());
        row.addProperty("sense",visual?"vision": "loaded_server_query");row.addProperty("visible",visual);
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
        if(e instanceof ItemEntity item){row.addProperty("pickupAvoided",DiscardedItems.avoided(item,player));row.add("stack",player.inventoryLedger.describe(item.getItem()));row.add("source",DropProvenance.compact(DropProvenance.source(item,player),4));}
        row.addProperty("sampledAtTick",player.level().getGameTime());return row;
    }
    private final class EntityScan {
        final String id=UUID.randomUUID().toString(),dimension=player.level().dimension().identifier().toString(),filter;
        final Vec3 origin=player.position();final float pitch=player.getXRot();final double heading=NavigationFollower.minecraftYawToHeading(player.getYRot());final long tick=player.level().getGameTime();final int radius;final boolean itemsOnly,capped;final List<UUID> candidates;
        final JsonArray rows=new JsonArray();final TreeMap<String,int[]> groups=new TreeMap<>();int index;
        EntityScan(int radius,String filter,boolean itemsOnly,List<UUID> candidates,boolean capped){this.radius=radius;this.filter=filter;this.itemsOnly=itemsOnly;this.candidates=candidates;this.capped=capped;}
    }
    /** Short dry corridors for yielding space; every sample checks native body collision/support. */
    public JsonObject standingPositions(){
        var out=new JsonObject();var rows=new JsonArray();
        for(double distance:new double[]{2,3,1.25})for(int heading=0;heading<360;heading+=45){
            double dx=Math.sin(Math.toRadians(heading))*distance,dz=-Math.cos(Math.toRadians(heading))*distance;
            Vec3 wanted=player.position().add(dx,0,dz);
            // Use the same horizontal standing centers as the route graph. A body
            // can balance on a path's edge while that endpoint's grid cell has no
            // floor; returning that edge made a two-block yield search thousands
            // of unrelated nodes before failing.
            Vec3 end=new Vec3(Math.floor(wanted.x())+.5,wanted.y(),Math.floor(wanted.z())+.5);
            double actualDistance=player.position().distanceTo(end);boolean clear=true;
            for(int i=1;i<=Math.ceil(actualDistance*4);i++){
                var at=player.position().lerp(end,Math.min(1,i/(actualDistance*4)));
                var box=player.getBoundingBox().move(at.subtract(player.position()));
                if(!player.level().isLoaded(BlockPos.containing(at)) || !player.level().getWorldBorder().isWithinBounds(BlockPos.containing(at))
                    || !player.level().noCollision(player,box) || !player.level().getFluidState(BlockPos.containing(at)).isEmpty()
                    || !player.level().getBlockCollisions(player,new AABB(box.minX+.01,box.minY-.04,box.minZ+.01,box.maxX-.01,box.minY+.001,box.maxZ-.01)).iterator().hasNext()) {clear=false;break;}
            }
            if(clear){var row=new JsonObject();position(row,end);row.addProperty("heading",(Math.toDegrees(Math.atan2(end.x()-player.getX(),player.getZ()-end.z()))+360)%360);row.addProperty("distance",actualDistance);rows.add(row);}
        }
        out.add("results",rows);out.addProperty("complete",true);out.addProperty("dimension",player.level().dimension().identifier().toString());out.addProperty("sampledAtTick",player.level().getGameTime());
        out.addProperty("meaning","Currently clear short level dry corridors. Choose a candidate away from the player/work target; normal navigation must still verify arrival. Empty results do not exclude slopes or longer detours.");return out;
    }
    public JsonObject summary() {
        if(entitySnapshot==null || player.level().getGameTime()-lastEntityTick>=20){entitySnapshot=entities(0,32,"",PerceptionRange.MAX);lastEntityTick=player.level().getGameTime();}
        JsonObject out=basics();out.add("performance",performance());out.add("nearbySummary",entitySnapshot.deepCopy());out.addProperty("detailsTool","sense");return out;
    }
    public JsonObject blocks(int radius,String filter,String kind,String cursor,int limit) {
        return blocks(radius,filter,kind,cursor,limit,false);
    }
    public JsonObject blocks(int radius,String filter,String kind,String cursor,int limit,boolean visibleOnly) {
        if(radius<1 || radius>PerceptionRange.MAX || limit<1 || limit>64 || filter.length()>128)throw new IllegalArgumentException("Invalid block query bounds");
        ScanPage scan;
        if(cursor.isBlank()) {
            if(scans.size()>=8)scans.remove(scans.keySet().iterator().next());
            scan=new ScanPage(UUID.randomUUID().toString(),player.blockPosition(),player.level().dimension().identifier().toString(),radius,filter,kind,player.level().getGameTime(),visibleOnly);scans.put(scan.id,scan);
        } else {scan=scans.get(cursor);if(scan==null)throw new IllegalArgumentException("Unknown or expired scan cursor");}
        if(player.level().getGameTime()-scan.tick>1200 || !player.level().dimension().identifier().toString().equals(scan.dimension)) {scans.remove(scan.id);throw new IllegalArgumentException("Scan expired or dimension changed");}
        JsonArray found=new JsonArray();long total=(long)scan.sections.size()*4096;int checked=0,unloaded=0;
        long began=System.nanoTime(),deadline=beginWork(3_000_000L);
        while(scan.index<total && checked<4096 && found.size()<limit && System.nanoTime()<deadline) {
            BlockPos section=scan.sections.get((int)(scan.index/4096));
            var chunk=player.level().getChunkSource().getChunkNow(section.getX(),section.getZ());
            if(chunk==null || section.getY()*16<player.level().getMinY() || section.getY()*16>=player.level().getMaxY()) {
                int skipped=4096-(int)(scan.index%4096);scan.index+=skipped;unloaded+=skipped;continue;
            }
            var nativeSection=chunk.getSection(chunk.getSectionIndex(section.getY()*16));
            if(scan.index%4096==0 && !nativeSection.maybeHas(state->matchesBlockQuery(state,scan.filter,scan.kind))) {
                scan.index+=4096;continue;
            }
            int local=(int)(scan.index++%4096);
            BlockPos p=new BlockPos(section.getX()*16+(local&15),section.getY()*16+(local>>8),section.getZ()*16+((local>>4)&15));checked++;
            if(Vec3.atCenterOf(p).distanceToSqr(scan.eyeOrigin.subtract(0,scan.eyeHeight,0))>scan.radius*(double)scan.radius)continue;
            if(!player.level().isLoaded(p) || p.getY()<player.level().getMinY() || p.getY()>=player.level().getMaxY()){unloaded++;continue;}
            var state=player.level().getBlockState(p);if(state.isAir())continue;
            String id=BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            if(!matchesBlockQuery(state,scan.filter,scan.kind))continue;
            Vec3 point=Vec3.atCenterOf(p);boolean proximity=insideProximity(player.position(),point,scan.radius);
            boolean visible=inCone(point,scan.radius) && rayClear(player.getEyePosition(),point,p);
            if(scan.visibleOnly && !visible)continue;
            JsonObject row=new JsonObject();row.addProperty("block",id);row.addProperty("x",p.getX());row.addProperty("y",p.getY());row.addProperty("z",p.getZ());var center=new JsonObject();position(center,point);row.add("center",center);row.addProperty("state",state.toString());row.addProperty("visible",visible);row.addProperty("sense",visible?"vision":"loaded_server_query");row.addProperty("exposed",exposed(p));row.addProperty("rayPassThrough",transparent(state,p));
            if(!scan.kind.equals("blocks"))row.addProperty("classification",scan.kind.equals("trees")?"observed_log_candidate":"observed_structure_marker_not_confirmed_structure");found.add(row);
        }
        recordWork(began);
        JsonObject out=new JsonObject();out.addProperty("pageServerMillis",(System.nanoTime()-began)/1_000_000.0);out.add("results",found);out.addProperty("cursor",scan.id);out.addProperty("complete",scan.index>=total);out.addProperty("scannedCells",checked);out.addProperty("unloadedCells",unloaded);out.addProperty("examinedSoFar",scan.index);out.addProperty("totalCandidateCells",total);out.addProperty("loadedOnly",true);out.addProperty("sampledAtTick",player.level().getGameTime());
        out.addProperty("dimension",scan.dimension);out.addProperty("scanStartedAtTick",scan.tick);out.addProperty("snapshotIsAtomic",false);
        scan.unloaded+=unloaded;out.addProperty("unloadedCellsSoFar",scan.unloaded);out.addProperty("visibleOnly",scan.visibleOnly);
        boolean viewChanged=player.position().distanceToSqr(scan.eyeOrigin.subtract(0,scan.eyeHeight,0))>.25 || difference(scan.heading,NavigationFollower.minecraftYawToHeading(player.getYRot()))>4 || Math.abs(scan.pitch-player.getXRot())>4;
        out.addProperty("viewChangedSinceStart",viewChanged);out.addProperty("coverageComplete",scan.index>=total && scan.unloaded==0 && (!scan.visibleOnly || !viewChanged));out.addProperty("coverageMeaning","Loaded blocks in a fixed 3D sphere; visibility is labelled separately. Unloaded space remains unknown.");
        if(scan.index>=total)scans.remove(scan.id);return out;
    }
    public boolean exposed(BlockPos p) {
        for(var side:net.minecraft.core.Direction.values()) {
            var q=p.relative(side);if(!player.level().isLoaded(q))continue;
            var state=player.level().getBlockState(q);
            if(state.getCollisionShape(player.level(),q).isEmpty() && state.getFluidState().isEmpty())return true;
        }return false;
    }
    private static boolean matchesBlockQuery(BlockState state,String filter,String kind) {
        if(state.isAir())return false;
        String id=BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        // User-facing structure names describe marker classes, not block IDs.
        if(kind.equals("structures")) {
            if(filter.equals("village") || filter.equals("村庄"))return state.is(net.minecraft.world.level.block.Blocks.BELL);
            return id.contains(filter) && Set.of("minecraft:nether_portal","minecraft:end_portal_frame","minecraft:spawner","minecraft:bell").contains(id);
        }
        if(kind.equals("trees")){
            String species=dev.mcai.companion.agent.mining.TreeSurvey.species(id);if(species.isEmpty())return false;
            String term=filter.strip().toLowerCase(Locale.ROOT);
            if(Set.of("","tree","trees","wood","log","logs","树","树木","木头","原木","any","任意").contains(term))return true;
            term=Map.ofEntries(Map.entry("橡木","oak"),Map.entry("橡树","oak"),Map.entry("云杉","spruce"),Map.entry("白桦","birch"),Map.entry("丛林","jungle"),Map.entry("金合欢","acacia"),Map.entry("深色橡木","dark_oak"),Map.entry("红树","mangrove"),Map.entry("樱花","cherry"),Map.entry("苍白橡木","pale_oak"),Map.entry("绯红","crimson"),Map.entry("诡异","warped")).getOrDefault(term,term);
            return species.equals(term) || id.equals(term) || id.equals("minecraft:"+term);
        }
        return PerceptionQuery.block(state,filter);
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
    private final class ScanPage {
        final Vec3 eyeOrigin=player.getEyePosition();final float eyeHeight=player.getEyeHeight(),pitch=player.getXRot();final double heading=NavigationFollower.minecraftYawToHeading(player.getYRot());
        final String id,dimension,filter,kind;final boolean visibleOnly;final BlockPos origin;final int radius;final long tick;long index,unloaded;
        final java.util.List<BlockPos> sections=new java.util.ArrayList<>();
        ScanPage(String id,BlockPos origin,String dimension,int radius,String filter,String kind,long tick,boolean visibleOnly){this.visibleOnly=visibleOnly;this.id=id;this.origin=origin;this.dimension=dimension;this.radius=radius;this.filter=filter;this.kind=kind;this.tick=tick;
            int cx=origin.getX()>>4,cz=origin.getZ()>>4,cy=origin.getY()>>4;
            int minX=(origin.getX()-radius)>>4,maxX=(origin.getX()+radius)>>4;
            int minZ=(origin.getZ()-radius)>>4,maxZ=(origin.getZ()+radius)>>4;
            int minY=Math.max(player.level().getMinY()>>4,(origin.getY()-radius)>>4);
            int maxY=Math.min((player.level().getMaxY()-1)>>4,(origin.getY()+radius)>>4);
            int rings=Math.max(Math.max(cx-minX,maxX-cx),Math.max(cz-minZ,maxZ-cz));
            // Numen ring enumeration: each horizontal search cell appears once.
            // Prioritize nearby vertical bands; never spend early pages scanning
            // deep underground before surveying surface-height space farther out.
            for(int band=0;band<=Math.max(cy-minY,maxY-cy);band++)
                for(int y:band==0?new int[]{cy}:new int[]{cy-band,cy+band}){
                    if(y<minY || y>maxY)continue;
                    for(int ring=0;ring<=rings;ring++)
                        for(int index=0;index<dev.mcai.companion.vendor.numen.scan.RingSpiral.perimeter(ring);index++){
                            var offset=dev.mcai.companion.vendor.numen.scan.RingSpiral.offset(ring,index);
                            int x=cx+offset[0],z=cz+offset[1];
                            if(x>=minX && x<=maxX && z>=minZ && z<=maxZ)sections.add(new BlockPos(x,y,z));
                        }
                }
        }
    }
}
