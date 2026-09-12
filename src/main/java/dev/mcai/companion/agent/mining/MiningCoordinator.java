package dev.mcai.companion.agent.mining;

import java.util.*;
import com.google.gson.*;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.body.AgentControlFrame;
import dev.mcai.companion.agent.body.MinePilotServerPlayer;
import dev.mcai.companion.agent.knowledge.DropProvenance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** A bounded, interruptible normal-player break operation; no direct world mutation. */
public final class MiningCoordinator {
    private static final ThreadLocal<MiningCoordinator> BREAK_CONTEXT = new ThreadLocal<>();
    private final AgentRuntime runtime;
    private final MinePilotServerPlayer body;
    private UUID request;
    private BlockPos target;
    private BlockState expectedState;
    private ItemStack expectedTool = ItemStack.EMPTY;
    private int selectedSlot;
    private boolean equipOnApproval;
    private String dimension;
    private Vec3 plannedOrigin;
    private String phase = "IDLE", reason = "";
    private int plannedTick, beganTick, actionSequence;
    private float progress;
    private boolean breaking;
    private boolean harvest;
    private JsonObject option;
    private final JsonArray emitted = new JsonArray();
    private int damageBefore, damageAfter;

    public MiningCoordinator(AgentRuntime runtime) { this.runtime=runtime;this.body=runtime.player(); }
    public int reservedCount(ItemStack stack){return ownsBody() && !expectedTool.isEmpty() && runtime.player().inventoryLedger.key(stack).equals(runtime.player().inventoryLedger.key(expectedTool)) ? expectedTool.getCount() : 0;}
    public boolean ownsBody() { return phase.equals("EXECUTING") || phase.equals("PAUSED"); }
    public boolean running() { return phase.equals("EXECUTING"); }
    public String phase() { return phase; }
    private int tick() { return runtime.server().getTickCount(); }
    private void requireThread() { if(!runtime.server().isSameThread())throw new IllegalStateException("Mining requires the server thread"); }
    private void requireIdleBody() {
        if(runtime.excavation()!=null && runtime.excavation().ownsBody() && !runtime.excavation().internalAction())throw new IllegalStateException("Pause or cancel excavation before an independent body action");

        if(runtime.collection()!=null && runtime.collection().ownsBody() && !runtime.collection().internalAction())throw new IllegalStateException("Cancel collection before an independent body action");
        if(runtime.placement()!=null && runtime.placement().ownsBody() && !runtime.placement().internalAction())throw new IllegalStateException("Cancel placement before an independent body action");
        var navigation=runtime.navigation().status();
        if(runtime.jumpActive() || runtime.turnActive() || navigation.phase()!=dev.mcai.companion.agent.navigation.NavigationToolCoordinator.Phase.IDLE && !navigation.phase().terminal())
            throw new IllegalStateException("Finish or cancel the current movement before mining");
    }
    public JsonObject equip(int slot) {
        requireThread();requireIdleBody();
        if(ownsBody())throw new IllegalStateException("Cancel mining before changing the held tool");
        if(slot<0 || slot>8)throw new IllegalArgumentException("Select an existing hotbar slot 0..8; an empty slot selects bare hands");
        body.connection.handleSetCarriedItem(new ServerboundSetCarriedItemPacket(slot));
        body.containerMenu.broadcastChanges();
        return heldTool();
    }
    public JsonObject plan(BlockPos position, boolean requireHarvest) { return plan(position,requireHarvest,false); }
    public JsonObject plan(BlockPos position, boolean requireHarvest, boolean autoTool) {
        requireThread();requireIdleBody();
        if(ownsBody())throw new IllegalStateException("Cancel the active mining request before replacing it");
        if(!runtime.perception.observableBlock(position))throw new IllegalArgumentException("Target is not currently sensed");
        String problem=problem(position,requireHarvest,!autoTool);
        if(problem!=null)throw new IllegalArgumentException(problem);
        if(aim(position)==null)throw new IllegalArgumentException("No reachable target face from this position; approach using navigation first");
        target=position.immutable();expectedState=body.level().getBlockState(target);expectedTool=body.getMainHandItem().copy();
        var choice=autoTool?MiningToolChoice.best(body,expectedState,requireHarvest,1):new MiningToolChoice(body.getInventory().getSelectedSlot(),expectedTool);
        expectedTool=choice.stack();selectedSlot=choice.slot();equipOnApproval=autoTool;
        dimension=body.level().dimension().identifier().toString();
        plannedOrigin=body.position();request=UUID.randomUUID();plannedTick=tick();phase="PLAN_READY";reason="";
        harvest=requireHarvest;breaking=false;progress=0;emitted.asList().clear();
        damageBefore=expectedTool.getDamageValue();damageAfter=damageBefore;
        float perTick=expectedState.getDestroyProgress(body,body.level(),target);
        option=new JsonObject();option.addProperty("optionId","held-tool");option.add("tool",choice.json(body));
        option.addProperty("targetBlock",BuiltInRegistries.BLOCK.getKey(expectedState.getBlock()).toString());
        option.addProperty("targetBlocks",1);option.addProperty("accessBlocks",0);option.addProperty("movementDistance",0);
        option.addProperty("estimatedBreakTicks",choice.estimatedTicks(body,expectedState,target));
        option.addProperty("estimatedSeconds",choice.estimatedTicks(body,expectedState,target)/20.0);
        option.addProperty("estimateAssumption","Base selected-tool estimate; effects/enchantments and mod hooks can change actual time. Aiming and pickup excluded");
        var tool=expectedTool.get(net.minecraft.core.component.DataComponents.TOOL);
        if(expectedTool.isEmpty() || !expectedTool.isDamageableItem())option.add("expectedDurabilityCost",JsonNull.INSTANCE);
        else if(tool!=null){
            int nominal=expectedState.getDestroySpeed(body.level(),target)==0?0:tool.damagePerBlock();
            option.addProperty("nominalDurabilityCost",nominal);
            option.addProperty("durabilityCostMin",0);option.addProperty("durabilityCostNominalMax",nominal);
            option.addProperty("remainingDurabilityAtNominalCost",Math.max(0,expectedTool.getMaxDamage()-expectedTool.getDamageValue()-nominal));
            option.addProperty("durabilityAssumption","Tool component's nominal wear; enchantments or other mods can change actual wear");
        }
        else option.addProperty("durabilityEstimate","Unknown custom tool wear; revalidated during execution");
        option.addProperty("canHarvest",!expectedState.requiresCorrectToolForDrops() || expectedTool.isCorrectToolForDrops(expectedState));
        option.add("supportMaterials",new JsonArray());option.add("hazards",new JsonArray());
        option.addProperty("riskAssessment","No current local hazard detected; checked again before each action");
        option.addProperty("pickupIncluded",false);
        return status();
    }
    public JsonObject choose(UUID id,String optionId) {
        requireThread();requireIdleBody();requireRequest(id);
        if(!phase.equals("PLAN_READY") || !optionId.equals("held-tool"))throw new IllegalArgumentException("Choose the current PLAN_READY option");
        String stale=staleProblem();
        if(stale!=null || tick()-plannedTick>200 || body.position().distanceToSqr(plannedOrigin)>.01)
            throw new IllegalStateException(stale!=null?stale:"Mining plan expired or body moved; plan again");
        if(equipOnApproval) {
            dev.mcai.companion.agent.placement.HandController.equipSlot(body,selectedSlot,InteractionHand.MAIN_HAND);
            selectedSlot=body.getInventory().getSelectedSlot();equipOnApproval=false;
        }
        String unsafe=problem(target,harvest);if(unsafe!=null)throw new IllegalStateException(unsafe);
        phase="EXECUTING";beganTick=tick();body.stopControlling();return status();
    }
    private void requireRequest(UUID id) { if(request==null || !request.equals(id))throw new IllegalArgumentException("Stale mining request ID"); }
    public JsonObject interrupt(UUID id, boolean pause, String why) {
        requireThread();requireRequest(id);
        if(pause && !running() && !phase.equals("PAUSED"))throw new IllegalStateException("Only an approved executing job can be paused");
        if(!ownsBody() && !phase.equals("PLAN_READY"))return status();
        abort();phase=pause?"PAUSED":"CANCELLED";reason=why;body.stopControlling();return status();
    }
    public JsonObject resume(UUID id) {
        requireThread();requireIdleBody();requireRequest(id);
        if(!phase.equals("PAUSED"))throw new IllegalStateException("Mining is not paused");
        String stale=staleProblem();String unsafe=problem(target,harvest);
        if(stale!=null || unsafe!=null || aim(target)==null)throw new IllegalStateException(stale!=null?stale:unsafe!=null?unsafe:"Target face is no longer reachable");
        phase="EXECUTING";beganTick=tick();progress=0;return status();
    }
    public void cancelForChat() { if(ownsBody() || phase.equals("PLAN_READY"))interrupt(request,false,"Stopped by player chat"); }
    public void tickBeforePhysics() {
        if(!running())return;
        String stale=staleProblem();String unsafe=problem(target,harvest);
        if(stale!=null || unsafe!=null){fail(stale!=null?stale:unsafe);return;}
        if(tick()-beganTick>2400){fail("Mining timed out; no automatic retry");return;}
        BlockHitResult hit=aim(target);
        if(hit==null){fail("Target face became obstructed or out of reach");return;}
        Vec3 look=hit.getLocation().subtract(body.getEyePosition());
        float yaw=(float)Math.toDegrees(Math.atan2(-look.x,look.z));
        float pitch=(float)-Math.toDegrees(Math.atan2(look.y,Math.hypot(look.x,look.z)));
        body.applyControlFrame(new AgentControlFrame(yaw,pitch,0,0,false,false,false));
        var actual=body.level().clip(new ClipContext(body.getEyePosition(),body.getEyePosition().add(body.getViewVector(1).scale(body.blockInteractionRange())),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,body));
        if(actual.getType()!=HitResult.Type.BLOCK || !actual.getBlockPos().equals(target)){
            if(breaking){abort();progress=0;}return;
        }
        float increment=expectedState.getDestroyProgress(body,body.level(),target);
        if(Float.isNaN(increment) || increment<=0){fail("Current tool/effects do not permit mining progress");return;}
        body.swing(InteractionHand.MAIN_HAND);
        if(!breaking){
            breaking=true;progress=increment;
            issue(Action.START_DESTROY_BLOCK,actual.getDirection());
            if(!body.level().getBlockState(target).equals(expectedState)){finish();return;}
            if(!body.gameMode.isDestroyingBlock || !body.gameMode.destroyPos.equals(target)){
                fail("Server did not accept the start of this block break");return;
            }
        } else {
            progress+=increment;
            // Never send STOP early: vanilla's delayed-destroy path can survive ABORT.
            if(!body.gameMode.isDestroyingBlock || !body.gameMode.destroyPos.equals(target)){
                fail("Server interrupted this block break");return;
            }
            float serverProgress=increment*(body.gameMode.gameTicks-body.gameMode.destroyProgressStart+1);
            if(progress>=1.0F && serverProgress>=1.0F){
                issue(Action.STOP_DESTROY_BLOCK,actual.getDirection());
                if(!body.level().getBlockState(target).equals(expectedState))finish();
                else fail("Server denied block destruction or target did not change");
            }
        }
    }
    private void issue(Action action,Direction face) {
        BREAK_CONTEXT.set(this);
        try {body.gameMode.handleBlockBreakAction(target,action,face,body.level().getMaxY()-1,++actionSequence);}
        finally {BREAK_CONTEXT.remove();}
    }
    private void abort() {if(breaking){issue(Action.ABORT_DESTROY_BLOCK,Direction.UP);breaking=false;}progress=0;}
    private void finish(){breaking=false;phase="COMPLETED";damageAfter=body.getMainHandItem().getDamageValue();body.stopControlling();reason="Target block changed through the player break action; emitted drops are not proof of pickup";}
    private void fail(String why){abort();phase="BLOCKED";reason=why;body.stopControlling();}
    private String staleProblem(){
        if(!body.isAlive() || !body.level().dimension().identifier().toString().equals(dimension))return "Body died or changed dimension";
        if(!body.level().isLoaded(target) || !body.level().getBlockState(target).equals(expectedState))return "Target changed since planning";
        if(equipOnApproval ? !ItemStack.matches(body.getInventory().getItem(selectedSlot),expectedTool)
                : body.getInventory().getSelectedSlot()!=selectedSlot || !ItemStack.matches(body.getMainHandItem(),expectedTool))return "Selected tool or durability changed since planning";
        return null;
    }
    public String problem(BlockPos p,boolean requireHarvest){return problem(p,requireHarvest,true);}
    private String problem(BlockPos p,boolean requireHarvest,boolean checkTool){
        // Vanilla supports airborne mining and computes its slower progress itself.
        // Reach and actual ray are rechecked every tick; transient landing is not failure.
        if(!body.isAlive())return "Mining requires a living body";
        if(body.gameMode.getGameModeForPlayer()!=GameType.SURVIVAL)return "This mining capability requires survival mode; adventure/creative restrictions are not bypassed";
        if(!body.level().isLoaded(p))return "Target chunk is not loaded";
        BlockState state=body.level().getBlockState(p);
        if(state.isAir() || !state.getFluidState().isEmpty())return "Target is air or fluid";
        if(body.level().getBlockEntity(p) instanceof net.minecraft.world.Container)return "Container excavation requires a container-aware plan";
        if(Float.isNaN(state.getDestroyProgress(body,body.level(),p)) || state.getDestroyProgress(body,body.level(),p)<=0)return "Target is not breakable with current rules";
        if(body.level().getServer().isUnderSpawnProtection(body.level(),p,body) || !body.level().mayInteract(body,p) || body.blockActionRestricted(body.level(),p,body.gameMode.getGameModeForPlayer()))return "Server forbids interaction with this block";
        if(checkTool && requireHarvest && !state.canHarvestBlock(body.level(),p,body))return "Held tool cannot harvest this block; select an appropriate tool before planning";
        var held=body.getMainHandItem();
        var toolData=held.get(net.minecraft.core.component.DataComponents.TOOL);
        if(checkTool && held.isDamageableItem() && toolData==null)return "Unknown tool wear rules; use a supported mining tool";
        if(checkTool && held.isDamageableItem() && held.getMaxDamage()-held.getDamageValue()<=Math.max(1,toolData.damagePerBlock()))return "Tool durability reserve would be exhausted";
        if(!softVegetation(state) && new AABB(p).intersects(body.getBoundingBox().move(0,-.05,0)))return "Cannot excavate the body's current support or occupied space";
        if(body.level().getBlockState(p.above()).getBlock() instanceof FallingBlock)return "Falling material above target requires a different excavation plan";
        for(Direction direction:Direction.values()){
            BlockPos adjacent=p.relative(direction);
            if(!body.level().isLoaded(adjacent))return "Unobserved adjacent space prevents a safe break";
            if(!softVegetation(state) && !body.level().getFluidState(adjacent).isEmpty())return "Adjacent fluid may enter the excavation";
        }
        if(!body.level().getEntities(body,new AABB(p).inflate(.25),e->e instanceof net.minecraft.world.entity.LivingEntity).isEmpty())return "A player or creature occupies the work area";
        return null;
    }
    public boolean reachable(BlockPos p){return aim(p)!=null;}
    /** These replaceable weeds have no collision or fluid-barrier function. */
    public static boolean softVegetation(BlockState state){return Set.of("short_grass","tall_grass","fern","large_fern","dead_bush","bush","leaf_litter").contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath());}
    public boolean reachableFrom(BlockPos p,Vec3 feet){return aimFrom(p,feet.add(0,body.getEyeHeight(),0))!=null;}
    private BlockHitResult aim(BlockPos p){return aimFrom(p,body.getEyePosition());}
    private BlockHitResult aimFrom(BlockPos p,Vec3 eyes){
        BlockHitResult best=null;
        var shape=body.level().getBlockState(p).getShape(body.level(),p);
        for(var box:shape.toAabbs())for(Direction side:Direction.values()){
            Vec3 point=box.getCenter().add(p.getX(),p.getY(),p.getZ());
            point=point.add(side.getStepX()*(box.maxX-box.minX)*.4999,side.getStepY()*(box.maxY-box.minY)*.4999,side.getStepZ()*(box.maxZ-box.minZ)*.4999);
            if(eyes.distanceToSqr(point)>body.blockInteractionRange()*body.blockInteractionRange())continue;
            var hit=body.level().clip(new ClipContext(eyes,point,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,body));
            if(hit.getType()==HitResult.Type.BLOCK && hit.getBlockPos().equals(p) && (best==null || eyes.distanceToSqr(hit.getLocation())<eyes.distanceToSqr(best.getLocation())))best=hit;
        }
        return best;
    }
    public JsonObject heldTool(){
        var stack=body.getMainHandItem();var out=new JsonObject();
        out.addProperty("slot",body.getInventory().getSelectedSlot());out.addProperty("item",stack.isEmpty()?"bare_hands":BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if(!stack.isEmpty())out.addProperty("entryId",body.inventoryLedger.key(stack));
        out.addProperty("damageable",stack.isDamageableItem());
        if(stack.isDamageableItem()){out.addProperty("damage",stack.getDamageValue());out.addProperty("maxDurability",stack.getMaxDamage());out.addProperty("remainingDurability",stack.getMaxDamage()-stack.getDamageValue());}
        return out;
    }
    public JsonObject status(){
        var out=new JsonObject();out.addProperty("phase",phase);out.addProperty("reason",reason);out.addProperty("sampledAtTick",tick());
        if(request!=null){out.addProperty("requestId",request.toString());out.addProperty("dimension",dimension);out.addProperty("x",target.getX());out.addProperty("y",target.getY());out.addProperty("z",target.getZ());}
        var options=new JsonArray();if(option!=null)options.add(option.deepCopy());out.add("options",options);
        out.addProperty("breakProgress",Math.min(1,progress));out.add("emittedDrops",emitted.deepCopy());
        out.addProperty("durabilityDamageBefore",damageBefore);out.addProperty("durabilityDamageAfter",damageAfter);
        if(target!=null && body.level().dimension().identifier().toString().equals(dimension) && body.level().isLoaded(target))
            out.addProperty("observedTargetState",body.level().getBlockState(target).toString());
        out.addProperty("collectionVerified",false);out.addProperty("bodyX",body.getX());out.addProperty("bodyY",body.getY());out.addProperty("bodyZ",body.getZ());
        return out;
    }
    public static void onItemSpawn(ItemEntity item){
        var owner=BREAK_CONTEXT.get();if(owner==null || item.level()!=owner.body.level())return;
        DropProvenance.mark(item,"mined_block",owner.body);
        var data=item.getPersistentData();data.putString("minepilot.miningRequest",owner.request.toString());data.putString("minepilot.sourceBlock",BuiltInRegistries.BLOCK.getKey(owner.expectedState.getBlock()).toString());
        data.putString("minepilot.sourceDimension",owner.dimension);data.putInt("minepilot.sourceX",owner.target.getX());data.putInt("minepilot.sourceY",owner.target.getY());data.putInt("minepilot.sourceZ",owner.target.getZ());
        var row=new JsonObject();row.addProperty("entityId",item.getUUID().toString());row.addProperty("item",BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).toString());row.addProperty("count",item.getItem().getCount());if(owner.emitted.size()<64)owner.emitted.add(row);
    }
}
