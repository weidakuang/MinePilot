package dev.mcai.companion.agent.placement;

import java.util.*;
import dev.mcai.companion.agent.body.MinePilotServerPlayer;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;

/** Computes real clickable faces. Planning never changes the player's coordinates or look. */
public final class PlacementGeometry {
    public record Aim(BlockHitResult hit,float yaw,float pitch,BlockState state) {}
    private PlacementGeometry() {}
    public static boolean matches(BlockState state,Map<String,String> constraints){
        for(var e:constraints.entrySet()){
            var p=state.getBlock().getStateDefinition().getProperty(e.getKey());
            if(p==null || !name(state,p).equals(e.getValue()))return false;
        }return true;
    }
    private static <T extends Comparable<T>> String name(BlockState s,Property<T> p){return p.getName(s.getValue(p));}
    public static void validate(Block block,Map<String,String> constraints){
        for(var e:constraints.entrySet()){
            var p=block.getStateDefinition().getProperty(e.getKey());
            // Wall-mounted variants (torch/sign) are resolved by the item context.
            if(p==null && e.getKey().equals("facing"))continue;
            if(p==null || p.getValue(e.getValue()).isEmpty())throw new IllegalArgumentException("Unsupported block property/value: "+e.getKey()+"="+e.getValue());
        }
    }
    public static List<Aim> aims(MinePilotServerPlayer p,BlockPos target,ItemStack stack,InteractionHand hand,Map<String,String> constraints,Vec3 feet){
        if(!(stack.getItem() instanceof BlockItem item) || target.distToCenterSqr(feet)>25)return List.of();
        var eyes=feet.add(0,p.getEyeHeight(),0);var result=new ArrayList<Aim>();
        var anchors=new ArrayList<BlockPos>();anchors.add(target);for(var d:Direction.values())anchors.add(target.relative(d));
        for(var anchor:anchors){
            if(!p.level().isLoaded(anchor))continue;
            var shape=p.level().getBlockState(anchor).getShape(p.level(),anchor);
            for(var box:shape.toAabbs())for(var face:Direction.values())for(double u:new double[]{.5,.2,.8})for(double v:new double[]{.5,.2,.8}){
                // Each face needs two independent surface coordinates. Sharing u
                // between x/z sampled only a diagonal on horizontal faces and
                // missed the visible side of an already-built row.
                Vec3 point=switch(face.getAxis()){
                    case X -> new Vec3(anchor.getX()+box.minX,anchor.getY()+box.minY+(box.maxY-box.minY)*u,anchor.getZ()+box.minZ+(box.maxZ-box.minZ)*v);
                    case Y -> new Vec3(anchor.getX()+box.minX+(box.maxX-box.minX)*u,anchor.getY()+box.minY,anchor.getZ()+box.minZ+(box.maxZ-box.minZ)*v);
                    case Z -> new Vec3(anchor.getX()+box.minX+(box.maxX-box.minX)*u,anchor.getY()+box.minY+(box.maxY-box.minY)*v,anchor.getZ()+box.minZ);
                };
                point=switch(face){case EAST -> new Vec3(anchor.getX()+box.maxX-.00001,point.y,point.z);case WEST -> new Vec3(anchor.getX()+box.minX+.00001,point.y,point.z);case UP -> new Vec3(point.x,anchor.getY()+box.maxY-.00001,point.z);case DOWN -> new Vec3(point.x,anchor.getY()+box.minY+.00001,point.z);case SOUTH -> new Vec3(point.x,point.y,anchor.getZ()+box.maxZ-.00001);case NORTH -> new Vec3(point.x,point.y,anchor.getZ()+box.minZ+.00001);};
                if(eyes.distanceToSqr(point)>Math.pow(Math.min(5,p.blockInteractionRange()),2))continue;
                var hit=p.level().clip(new ClipContext(eyes,point,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,p));
                if(hit.getType()!=HitResult.Type.BLOCK || !hit.getBlockPos().equals(anchor) || hit.getDirection()!=face)continue;
                var look=hit.getLocation().subtract(eyes);float yaw=(float)Math.toDegrees(Math.atan2(-look.x,look.z));float pitch=(float)-Math.toDegrees(Math.atan2(look.y,Math.hypot(look.x,look.z)));
                // Reject grazing corners: the finite-precision native view ray must
                // still hit this face with a small aiming margin, not a neighbor.
                if(!stableRay(p,eyes,hit,yaw,pitch))continue;
                var context=new PreviewContext(p,hand,stack,hit,yaw,pitch);
                if(!context.getClickedPos().equals(target) || !context.canPlace())continue;
                BlockState state=item.getPlacementState(context);
                if(state==null || !matches(state,constraints))continue;
                result.add(new Aim(hit,yaw,pitch,state));
                if(result.size()>=12)return result;
            }
        }return result;
    }
    private static boolean stableRay(MinePilotServerPlayer p,Vec3 eyes,BlockHitResult expected,float yaw,float pitch){
        for(float offset:new float[]{0,-.04f,.04f}){
            var end=eyes.add(Vec3.directionFromRotation(pitch+offset,yaw+offset).scale(Math.min(5,p.blockInteractionRange())));
            var hit=p.level().clip(new ClipContext(eyes,end,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,p));
            if(hit.getType()!=HitResult.Type.BLOCK || !hit.getBlockPos().equals(expected.getBlockPos()) || hit.getDirection()!=expected.getDirection())return false;
        }return true;
    }
    public static Map<BlockPos,BlockState> footprint(BlockPos target,BlockState state){
        var result=new LinkedHashMap<BlockPos,BlockState>();result.put(target,state);
        if(state.getBlock() instanceof DoorBlock)result.put(target.above(),state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,DoubleBlockHalf.UPPER));
        if(state.getBlock() instanceof BedBlock)result.put(target.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING)),state.setValue(BlockStateProperties.BED_PART,BedPart.HEAD));
        return result;
    }
    public static boolean clearActualRay(MinePilotServerPlayer p,Aim aim){
        var eyes=p.getEyePosition();
        if(eyes.distanceToSqr(aim.hit.getLocation())>Math.pow(Math.min(5,p.blockInteractionRange()),2))return false;
        var end=eyes.add(p.getViewVector(1).scale(Math.min(5,p.blockInteractionRange())));
        var hit=p.level().clip(new ClipContext(eyes,end,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,p));
        return hit.getType()==HitResult.Type.BLOCK && hit.getBlockPos().equals(aim.hit.getBlockPos()) && hit.getDirection()==aim.hit.getDirection() && hit.getLocation().distanceToSqr(aim.hit.getLocation())<.003;
    }
    private static final class PreviewContext extends BlockPlaceContext {
        private final float yaw,pitch;
        PreviewContext(MinePilotServerPlayer p,InteractionHand h,ItemStack s,BlockHitResult hit,float yaw,float pitch){super(p,h,s,hit);this.yaw=yaw;this.pitch=pitch;}
        @Override public float getRotation(){return yaw;}
        @Override public Direction getHorizontalDirection(){return Direction.fromYRot(yaw);}
        @Override public Direction getNearestLookingDirection(){return directions()[0];}
        @Override public Direction getNearestLookingVerticalDirection(){return pitch<0?Direction.UP:Direction.DOWN;}
        private Direction[] directions(){
            var look=Vec3.directionFromRotation(pitch,yaw);var d=Direction.values();
            Arrays.sort(d,Comparator.comparingDouble((Direction side)->-(side.getStepX()*look.x+side.getStepY()*look.y+side.getStepZ()*look.z)));return d;
        }
        @Override public Direction[] getNearestLookingDirections(){
            var d=directions();if(replacingClickedOnBlock())return d;
            var list=new ArrayList<>(Arrays.asList(d));list.remove(getClickedFace().getOpposite());list.addFirst(getClickedFace().getOpposite());return list.toArray(Direction[]::new);
        }
    }
}
