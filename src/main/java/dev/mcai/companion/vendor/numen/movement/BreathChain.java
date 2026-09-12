// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from Dwinovo/minecraft-numen 34ef004dac3095fbbd928a897927e277c69d02fa.
package dev.mcai.companion.vendor.numen.movement;

import java.util.*;
import com.google.gson.JsonObject;
import dev.mcai.companion.agent.body.AgentControlFrame;
import dev.mcai.companion.agent.body.MinePilotServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Numen's float/air-opening search, adapted to our single native input frame. */
public final class BreathChain {
    private static final int CEILING_PROBE=16, AIR_SEARCH_BUDGET=400, AIR_SEARCH_RADIUS=16, RETARGET_TICKS=20;
    private boolean active,trapped;private int worstAir=300,cooldown;private BlockPos airStep;
    public boolean tick(MinePilotServerPlayer body,boolean idle){
        boolean submerged=body.isEyeInFluid(FluidTags.WATER);
        if(!body.isAlive() || !submerged && !(idle && body.isInWater())){active=false;trapped=false;airStep=null;cooldown=0;return false;}
        // Hold the native swim-up input while waiting at the surface. Releasing it
        // as soon as the eyes leave water causes repeated unnecessary dives.
        if(!submerged){active=false;trapped=false;airStep=null;cooldown=0;body.applyControlFrame(new AgentControlFrame(body.getYRot(),body.getXRot(),0,0,true,false,false));return true;}
        if(!active && !idle && body.getAirSupply()>240)return false;
        active=true;worstAir=Math.min(worstAir,body.getAirSupply());float yaw=body.getYRot(),forward=0;
        if(!ceilingSealed(body)){airStep=null;trapped=false;}
        else if(airStep==null || --cooldown<=0 || Vec3.atCenterOf(airStep).subtract(body.getEyePosition()).horizontalDistanceSqr()<.2){
            airStep=findAirStep(body);cooldown=RETARGET_TICKS;trapped=airStep==null;
        }
        if(airStep!=null){var delta=Vec3.atCenterOf(airStep).subtract(body.getEyePosition());
            if(delta.horizontalDistanceSqr()>.04){yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));if(Math.abs(net.minecraft.util.Mth.wrapDegrees(body.getYRot()-yaw))<60)forward=.8f;}
        }
        body.applyControlFrame(new AgentControlFrame(yaw,-35,forward,0,true,false,false));return true;
    }
    private static boolean ceilingSealed(MinePilotServerPlayer body){Level level=body.level();BlockPos pos=BlockPos.containing(body.getEyePosition());
        for(int i=0;i<CEILING_PROBE;i++){pos=pos.above();if(!level.isLoaded(pos))return true;var state=level.getBlockState(pos);if(state.getFluidState().is(FluidTags.WATER))continue;return !breathable(level,pos,state);}return false;
    }
    private static boolean breathable(Level level,BlockPos pos,BlockState state){return level.isLoaded(pos) && state.getFluidState().isEmpty() && state.getCollisionShape(level,pos).isEmpty();}
    private static boolean breathableAbove(Level level,BlockPos water){return level.isLoaded(water) && level.isLoaded(water.above()) && level.getFluidState(water).is(FluidTags.WATER) && breathable(level,water.above(),level.getBlockState(water.above()));}
    /** Return the first step of the connected-water route, avoiding straight-line shortcuts through walls. */
    private static BlockPos findAirStep(MinePilotServerPlayer body){var level=body.level();var start=BlockPos.containing(body.getEyePosition());
        if(!level.getFluidState(start).is(FluidTags.WATER))start=body.blockPosition();
        var queue=new ArrayDeque<BlockPos>();var parents=new HashMap<BlockPos,BlockPos>();queue.add(start);parents.put(start,start);int budget=AIR_SEARCH_BUDGET;
        while(!queue.isEmpty() && budget-->0){var cell=queue.removeFirst();if(breathableAbove(level,cell)){
                var next=cell;while(!parents.get(next).equals(start) && !parents.get(next).equals(next))next=parents.get(next);return next;
            }
            for(var direction:Direction.values()){var next=cell.relative(direction);if(Math.abs(next.getX()-start.getX())>AIR_SEARCH_RADIUS || Math.abs(next.getZ()-start.getZ())>AIR_SEARCH_RADIUS || !level.isLoaded(next) || !level.getFluidState(next).is(FluidTags.WATER) || parents.containsKey(next))continue;
                var feet=Vec3.atCenterOf(next).add(0,-body.getEyeHeight(),0);if(!level.noCollision(body,body.getBoundingBox().move(feet.subtract(body.position()))))continue;
                parents.put(next,cell);queue.addLast(next);
            }
        }return null;
    }
    public JsonObject status(){var out=new JsonObject();out.addProperty("active",active);out.addProperty("trapped",trapped);out.addProperty("lowestAir",worstAir);out.addProperty("meaning","Native swim-up strokes; no oxygen or position is manufactured");return out;}
}
