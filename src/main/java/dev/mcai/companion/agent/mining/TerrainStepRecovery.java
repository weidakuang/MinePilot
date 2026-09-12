package dev.mcai.companion.agent.mining;

import com.google.gson.*;
import java.util.*;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.navigation.NativeTravel;
import dev.mcai.companion.agent.placement.HandController;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.phys.*;

/** A bounded natural bank/ledge step, using the existing native breaker and route follower. */
final class TerrainStepRecovery {
    private static final int MAX_ACCESS_BREAKS=24;
    private final AgentRuntime r;private final NativeTravel travel;
    private final Set<BlockPos> tried=new HashSet<>();private final Set<String> children=new HashSet<>();
    private final ArrayDeque<BlockPos> breaks=new ArrayDeque<>();
    private BlockPos landing;private String phase="IDLE",reason="";private int broken,started;
    TerrainStepRecovery(AgentRuntime r,NativeTravel travel){this.r=r;this.travel=travel;}
    void reset(){cancel();tried.clear();children.clear();broken=0;}
    boolean isChild(JsonObject state){return state.has("requestId") && children.contains(state.get("requestId").getAsString());}
    boolean start(BlockPos toward){
        var p=r.player();if(broken>=MAX_ACCESS_BREAKS || tried.size()>=8 || !p.isInWater() && !p.onGround())return false;
        int y=p.getBlockY()+1;
        if(p.isInWater()){var surface=p.blockPosition();for(int n=0;n<12 && p.level().isLoaded(surface) && p.level().getFluidState(surface).is(FluidTags.WATER);n++)surface=surface.above();y=surface.getY();}
        var candidates=new ArrayList<BlockPos>();
        // A bank can overhang the existing shelf. Clear a head-height passage
        // at the current level before trying the next higher step.
        int topY=y;
        for(y=topY;y>=topY-1;y--)for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){
            if(Math.abs(dx)+Math.abs(dz)<1 || Math.abs(dx)+Math.abs(dz)>2)continue;
            var feet=new BlockPos(p.getBlockX()+dx,y,p.getBlockZ()+dz);var floor=feet.below();
            if(tried.contains(feet) || !p.level().isLoaded(feet.above(2)) || !p.level().isLoaded(floor))continue;
            var support=p.level().getBlockState(floor);
            if(!support.isCollisionShapeFullBlock(p.level(),floor) || !ExcavationCoordinator.isTerrain(support))continue;
            if(!clearable(feet,false) || !clearable(feet.above(),false) || p.level().getBlockState(feet.above(2)).getBlock() instanceof FallingBlock)continue;
            var overhead=p.blockPosition().above(2);
            boolean rising=feet.getY()>p.getBlockY();
            if(rising && (!clearable(overhead,false) || p.level().getBlockState(overhead.above()).getBlock() instanceof FallingBlock))continue;
            if(p.level().getBlockState(feet).isAir() && p.level().getBlockState(feet.above()).isAir())continue;
            var first=rising && !p.level().getBlockState(overhead).isAir()?overhead:p.level().getBlockState(feet).isAir()?feet.above():feet;if(!r.mining().reachable(first) || p.getEyePosition().distanceTo(Vec3.atCenterOf(feet.above()))>p.blockInteractionRange()+.75)continue;
            if(!p.level().getEntities(p,new AABB(feet).expandTowards(0,1,0),e->e instanceof net.minecraft.world.entity.LivingEntity).isEmpty())continue;
            candidates.add(feet);
        }
        candidates.sort(Comparator.<BlockPos>comparingInt(pos->toward!=null && toward.getY()>p.getY()+1?-pos.getY():0).thenComparingDouble(pos->pos.distToCenterSqr(p.position())+(toward==null?0:.06*pos.distSqr(toward))));
        if(candidates.isEmpty())return false;landing=candidates.getFirst();tried.add(landing);breaks.clear();
        var overhead=p.blockPosition().above(2);
        if(landing.getY()>p.getBlockY() && !p.level().getBlockState(overhead).isAir())breaks.add(overhead);
        if(!p.level().getBlockState(landing).isAir())breaks.add(landing);
        if(!p.level().getBlockState(landing.above()).isAir())breaks.add(landing.above());
        if(broken+breaks.size()>MAX_ACCESS_BREAKS)return false;
        phase="SELECT";reason="";started=r.server().getTickCount();
        dev.mcai.companion.MinecraftAiCompanion.LOGGER.info("Gather native access step at {}: {} observed natural blocks, {} of {} already cleared",landing,breaks.size(),broken,MAX_ACCESS_BREAKS);return true;
    }
    private boolean clearable(BlockPos pos,boolean checkReach){var p=r.player();var level=p.level();var state=level.getBlockState(pos);
        if(!level.isLoaded(pos) || !level.getWorldBorder().isWithinBounds(pos) || !state.getFluidState().isEmpty() || level.getBlockEntity(pos)!=null)return false;
        if(state.isAir())return true;
        if(!ExcavationCoordinator.isTerrain(state) || state.getBlock() instanceof FallingBlock || checkReach && !r.mining().reachable(pos))return false;
        for(var entry:p.inventoryLedger.treeFarms().getAsJsonObject("farms").entrySet()){var farm=entry.getValue().getAsJsonObject();if(farm.get("dimension").getAsString().equals(level.dimension().identifier().toString()) && TreeSurvey.contains(farm,pos))return false;}
        return !new AABB(pos).intersects(p.getBoundingBox());
    }
    String tick(){
        if(r.server().getTickCount()-started>800){fail("Native access step exceeded its 40-second budget");return phase;}
        if(phase.equals("BREAK")){var status=r.mining().status();String state=status.get("phase").getAsString();if(state.equals("EXECUTING"))return phase;
            if(!state.equals("COMPLETED")){fail("Native access break: "+status.get("reason").getAsString());return phase;}broken++;breaks.removeFirst();phase="SELECT";}
        if(phase.equals("SELECT")){
            if(breaks.isEmpty()){
                var feet=r.collection().standingFeet(landing);if(feet==null){fail("Cleared step is not a valid dry standing position");return phase;}
                travel.start(feet);phase="MOVE";return phase;
            }
            var pos=breaks.getFirst();if(!clearable(pos,true)){fail("Access block or reach changed before native break");return phase;}
            if(r.player().level().getBlockState(pos).isAir()){breaks.removeFirst();return phase;}
            var tool=MiningToolChoice.best(r.player(),r.player().level().getBlockState(pos),false,1);HandController.equipSlot(r.player(),tool.slot(),InteractionHand.MAIN_HAND);
            var plan=r.mining().plan(pos,false);var id=UUID.fromString(plan.get("requestId").getAsString());children.add(id.toString());r.mining().choose(id,"held-tool");phase="BREAK";return phase;
        }
        if(phase.equals("MOVE")){travel.tick();if(travel.phase().equals("COMPLETED")){travel.cancel();phase="COMPLETED";}else if(travel.phase().equals("FAILED"))fail(travel.reason());}
        return phase;
    }
    private void fail(String message){cancel();phase="FAILED";reason=message;}
    void cancel(){travel.cancel();if(isChild(r.mining().status()))r.mining().cancelForChat();phase="IDLE";}
    JsonObject status(){var o=new JsonObject();o.addProperty("phase",phase);o.addProperty("reason",reason);o.addProperty("clearedBlocks",broken);o.addProperty("maximumAccessBreaks",MAX_ACCESS_BREAKS);o.add("childMiningRequestIds",new Gson().toJsonTree(children));if(landing!=null)o.add("landing",TreeSurvey.position(landing));return o;}
}
