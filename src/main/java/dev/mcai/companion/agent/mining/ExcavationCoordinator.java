package dev.mcai.companion.agent.mining;

import com.google.gson.*;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.navigation.*;
import dev.mcai.companion.agent.placement.HandController;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static dev.mcai.companion.agent.mining.ExcavationPlanner.*;

/** Approved excavation scope plus native, interruptible break/place/walk execution. */
public final class ExcavationCoordinator implements AutoCloseable {
    private final AgentRuntime r;
    private final dev.mcai.companion.agent.concurrent.AnalysisWorkers executor=new dev.mcai.companion.agent.concurrent.AnalysisWorkers();
    private final NavigationFollower follower;
    private final ExcavationPickup pickup;
    private boolean collectDrops,allowGrove,replant,treeCleared,replanted;
    private TreeSurvey.Survey treeSurvey;
    private String saplingId="";
    private JsonObject farmSnapshot=new JsonObject();private int farmTick=Integer.MIN_VALUE;
    private final Map<Pos,Voxel> cells=new HashMap<>();
    private final Map<Pos,BlockState> expected=new HashMap<>();
    private final Map<String,ItemStack> tools=new HashMap<>();
    private final Map<String,List<Tool>> toolCache=new HashMap<>();
    private final List<Stock> supports=new ArrayList<>();
    private final List<Pos> targets=new ArrayList<>();
    private final Set<String> childMining=new HashSet<>(),childPlacement=new HashSet<>();
    private final JsonArray receipts=new JsonArray();
    private List<Plan> options=List.of();private Plan chosen;
    private CompletableFuture<List<Plan>> future;
    private NavigationEvent routeEvent;
    private UUID request;private String phase="IDLE",step="",reason="",dimension,mode;
    private Vec3 origin,lastPosition;private Pos base;
    private List<Pos> requested=List.of();private boolean requireHarvest,allowAccess,allowSupports,internal,destructive;
    private int scanIndex,actionIndex,plannedTick,startedTick,maxBreaks,skippedUnknown;
    private double maxDistance,distance,captureMillis;private volatile double workerMillis;
    private static final int EXTENT=16, WIDTH=33, VOLUME=WIDTH*WIDTH*WIDTH;
    public ExcavationCoordinator(AgentRuntime runtime){r=runtime;pickup=new ExcavationPickup(r);follower=new NavigationFollower(r.player(),e->{if(e.type()!=NavigationEvent.Type.NAVIGATION_PROGRESS)routeEvent=e;},id->null,why->invalidate(why));}
    private void invalidate(String why){follower.requestReplan(why);}
    public int reservedCount(ItemStack stack){
        if(!ownsBody() || stack.isEmpty())return 0;
        String key=r.player().inventoryLedger.key(stack);int material=0,tool=0;
        if(chosen!=null)for(int i=actionIndex;i<chosen.steps().size();i++){var s=chosen.steps().get(i);if(key.equals(s.material()))material++;if(s.tool()!=null && key.equals(s.tool().identity()))tool=Math.max(tool,tools.containsKey(key)?tools.get(key).getCount():1);}
        if(replant && !replanted && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(saplingId))material+=(int)neededSaplings();
        return Math.max(material,tool);
    }
    public boolean ownsBody(){return Set.of("CAPTURING","PLANNING","EXECUTING","PAUSED").contains(phase);}
    public boolean internalAction(){return internal;}
    public boolean isChild(JsonObject state){return state.has("requestId") && (childMining.contains(state.get("requestId").getAsString()) || childPlacement.contains(state.get("requestId").getAsString()));}
    private <T>T child(Supplier<T> action){internal=true;try{return action.get();}finally{internal=false;}}
    private int tick(){return r.server().getTickCount();}
    private void thread(){if(!r.server().isSameThread())throw new IllegalStateException("Excavation world access requires server thread");}
    private void idle(){thread();var n=r.navigation().status();if(ownsBody() || r.collection().ownsBody() || r.placement().ownsBody() || r.mining().ownsBody() || r.jumpActive() || r.turnActive() || !n.phase().terminal() && n.phase()!=NavigationToolCoordinator.Phase.IDLE)throw new IllegalStateException("Finish or cancel the active body task first");}
    public JsonObject plan(String mode,List<BlockPos> requested,boolean harvest,boolean access,boolean support,int maxBreaks,double maxDistance,boolean collectDrops){
        idle();this.collectDrops=collectDrops;treeSurvey=null;allowGrove=replant=treeCleared=replanted=false;saplingId="";if(r.player().gameMode.getGameModeForPlayer()!=GameType.SURVIVAL)throw new IllegalArgumentException("Excavation requires survival mode");
        if(requested.isEmpty() || requested.size()>4096 || maxBreaks<1 || maxBreaks>4096 || maxDistance<1 || maxDistance>512)throw new IllegalArgumentException("Bounded targets, 1..4096 breaks and 1..512 travel blocks required");
        origin=r.player().position();base=pos(r.player().blockPosition());
        for(var p:requested)if(Math.abs(p.getX()-base.x())>14 || Math.abs(p.getY()-base.y())>14 || Math.abs(p.getZ()-base.z())>14)throw new IllegalArgumentException("Plan a local excavation segment within 14 blocks of the current body");
        this.mode=mode;this.requested=requested.stream().map(ExcavationCoordinator::pos).distinct().toList();
        requireHarvest=harvest;allowAccess=access;allowSupports=support;this.maxBreaks=maxBreaks;this.maxDistance=maxDistance;
        dimension=r.player().level().dimension().identifier().toString();request=UUID.randomUUID();plannedTick=tick();phase="CAPTURING";step="SNAPSHOT";reason="";
        cells.clear();expected.clear();tools.clear();toolCache.clear();targets.clear();supports.clear();childMining.clear();childPlacement.clear();receipts.asList().clear();
        options=List.of();chosen=null;actionIndex=scanIndex=skippedUnknown=0;distance=captureMillis=workerMillis=0;
        pickup.reset();destructive=false;
        r.player().stopControlling();
        var p=r.player();for(int slot=0;slot<36;slot++){
            var item=p.getInventory().getItem(slot);if(item.isEmpty())continue;
            if(allowSupports && p.inventoryLedger.expendable(item) && item.getItem() instanceof BlockItem b && b.getBlock().defaultBlockState().isCollisionShapeFullBlock(p.level(),p.blockPosition()) && !(b.getBlock() instanceof FallingBlock)){
                String key=p.inventoryLedger.key(item);supports.add(new Stock(key,BuiltInRegistries.ITEM.getKey(item.getItem()).toString(),item.getCount(),p.inventoryLedger.importance(item)));tools.put(key,item.copy());
            }
        }
        return status();
    }
    public JsonObject planTree(TreeSurvey.Survey survey,boolean grove,boolean supports,boolean clearCanopy,boolean replant,int maxBreaks,double travel){
        if(!survey.harvestable() || survey.classification().equals("managed_grove_candidate") && !grove)throw new IllegalArgumentException("Tree cannot be safely separated/harvested under this farm policy: "+survey.json());
        if(replant && survey.roots().isEmpty())throw new IllegalArgumentException("A supported replanting site could not be identified; inspect roots before requesting replanting");
        plan("tree",survey.logs(),true,clearCanopy,supports,maxBreaks,travel,true);
        treeSurvey=survey;allowGrove=grove;this.replant=replant;
        saplingId="minecraft:"+switch(survey.species()){case "mangrove"->"mangrove_propagule";case "crimson","warped"->survey.species()+"_fungus";default->survey.species()+"_sapling";};return status();
    }
    private boolean leafAccess(BlockPos p,BlockState state){
        if(treeSurvey==null)return false;String id=TreeSurvey.id(state);
        boolean matching=id.equals("minecraft:"+treeSurvey.species()+"_leaves") && !state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.PERSISTENT)
            || treeSurvey.species().equals("crimson") && state.is(Blocks.NETHER_WART_BLOCK) || treeSurvey.species().equals("warped") && state.is(Blocks.WARPED_WART_BLOCK);
        return matching && treeSurvey.logs().stream().anyMatch(log->Math.abs(log.getX()-p.getX())<=2 && Math.abs(log.getY()-p.getY())<=2 && Math.abs(log.getZ()-p.getZ())<=2);
    }
    private boolean rootPlanted(BlockPos p){return TreeSurvey.id(r.player().level().getBlockState(p)).equals(saplingId);}
    private long neededSaplings(){return treeSurvey.roots().stream().filter(p->!rootPlanted(p)).count();}
    private JsonObject farms(){if(farmTick!=tick()){farmSnapshot=r.player().inventoryLedger.treeFarms().getAsJsonObject("farms");farmTick=tick();}return farmSnapshot;}
    private boolean protectedFarm(BlockPos bp){for(var e:farms().entrySet()){var f=e.getValue().getAsJsonObject();if(f.get("dimension").getAsString().equals(dimension) && TreeSurvey.contains(f,bp) && !(treeSurvey!=null && allowGrove && f.get("kind").getAsString().equals("manual")))return true;}return false;}
    private int saplingSlot(){int n=0,slot=-1;for(int i=0;i<36;i++){var s=r.player().getInventory().getItem(i);if(!s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(saplingId)){n+=s.getCount();slot=i;}}if(n<neededSaplings())throw new IllegalStateException("Replanting needs "+neededSaplings()+" "+saplingId+", currently "+n);return slot;}
    public void beforePhysics(){if(phase.equals("EXECUTING")){if(step.equals("MOVE"))follower.beforePhysicsTick();else if(step.equals("PICKUP"))pickup.beforePhysics();}}
    public void afterPhysics(){
        thread();try{
            if(phase.equals("CAPTURING")){capture();return;}
            if(phase.equals("PLANNING")){if(future.isDone()){options=future.join();if(treeSurvey!=null)options=options.stream().filter(p->p.targets().size()==targets.size() && p.limitation().isEmpty()).toList();future=null;phase=options.isEmpty()?"BLOCKED":"PLAN_READY";reason=options.isEmpty()?"No evaluated safe excavation plan in the sensed area":"";plannedTick=tick();}return;}
            if(!phase.equals("EXECUTING"))return;
            var body=r.player();if(!body.isAlive() || !dimension.equals(body.level().dimension().identifier().toString()) || body.gameMode.getGameModeForPlayer()!=GameType.SURVIVAL){block("Body died, changed dimension or mode");return;}
            distance+=body.position().distanceTo(lastPosition);lastPosition=body.position();
            if(distance>maxDistance+1 || tick()-startedTick>72000){block("Excavation travel/time budget exhausted");return;}
            var miningState=r.mining().status();if(isChild(miningState))pickup.record(miningState);pickup.observe();
            if(step.equals("REPLANT")){
                var state=r.placement().status();if(state.get("phase").getAsString().equals("COMPLETED")){
                    replanted=treeSurvey.roots().stream().allMatch(p->TreeSurvey.id(body.level().getBlockState(p)).equals(saplingId));
                    phase=replanted?"COMPLETED":"PARTIAL";reason=replanted?"Whole approved tree was felled, its drops collected, and the correct sapling layout physically replanted":"Replanting state differs from the approved species/layout";
                }else if(Set.of("BLOCKED","PARTIAL","CANCELLED").contains(state.get("phase").getAsString())){phase="PARTIAL";reason="Tree harvest ended but replanting stopped: "+state.get("reason").getAsString();}return;
            }
            if(step.equals("PICKUP")){pickup.tick(maxDistance-distance);if(pickup.phase().equals("COMPLETED"))completeCells();else if(pickup.phase().equals("BLOCKED")){phase="PARTIAL";reason=pickup.reason();body.stopControlling();}return;}
            if(step.equals("BREAK")){
                var s=r.mining().status();if(s.get("phase").getAsString().equals("COMPLETED")){
                    if(!body.level().getBlockState(blockPos(current().position())).isAir()){block("Broken target is not empty; live state needs a new decision");return;}
                    receipts.add(s);actionIndex++;step="SELECT";
                }else if(Set.of("BLOCKED","CANCELLED").contains(s.get("phase").getAsString()))block("Break stopped: "+s.get("reason").getAsString());return;
            }
            if(step.equals("SUPPORT")){
                var s=r.placement().status();if(s.get("phase").getAsString().equals("COMPLETED")){receipts.add(s);actionIndex++;step="SELECT";}
                else if(Set.of("BLOCKED","PARTIAL","CANCELLED").contains(s.get("phase").getAsString()))block("Support placement stopped: "+s.get("reason").getAsString());return;
            }
            if(step.equals("MOVE")){
                follower.tick();if(routeEvent!=null){var event=routeEvent;routeEvent=null;if(event.type()==NavigationEvent.Type.NAVIGATION_COMPLETED){actionIndex++;step="SELECT";}
                    else if(event.type()==NavigationEvent.Type.NAVIGATION_FAILED || event.type()==NavigationEvent.Type.NAVIGATION_DECISION_REQUIRED)block("Excavation movement stopped: "+event.message());}return;
            }
            if(actionIndex>=chosen.steps().size()){finish();return;}
            execute();
        }catch(RuntimeException failure){block("Excavation requires a new decision: "+String.valueOf(failure.getMessage()));}
    }
    private void capture(){
        if(r.player().position().distanceToSqr(origin)>.04 || !dimension.equals(r.player().level().dimension().identifier().toString())){block("Body moved during snapshot capture");return;}
        long started=System.nanoTime(),deadline=r.perception.beginWork(2_000_000L);int examined=0;
        while(scanIndex<VOLUME && examined++<2048 && System.nanoTime()<deadline){
            int n=scanIndex++;Pos p=base.add(n%WIDTH-EXTENT,n/(WIDTH*WIDTH)-EXTENT,(n/WIDTH)%WIDTH-EXTENT);var bp=blockPos(p);
            if(!r.perception.observableBlock(bp))continue;
            var level=r.player().level();var state=level.getBlockState(bp);boolean air=state.isAir(),safe=state.getFluidState().isEmpty() && level.getBlockEntity(bp)==null;
            if(state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS) || state.is(Blocks.POWDER_SNOW) || TreeSurvey.isMachine(state))safe=false;
            if(!air){
                if(level.getBlockState(bp.above()).getBlock() instanceof FallingBlock)safe=false;
                for(Direction d:Direction.values())if(!level.isLoaded(bp.relative(d)) || !level.getFluidState(bp.relative(d)).isEmpty())safe=false;
                for(var f:farms().entrySet()){
                    var farm=f.getValue().getAsJsonObject();if(farm.get("dimension").getAsString().equals(dimension) && TreeSurvey.contains(farm,bp) && !(treeSurvey!=null && allowGrove && farm.get("kind").getAsString().equals("manual")))safe=false;
                }
            }
            List<Tool> choices=air?List.of():previewTools(state,bp);
            boolean accessAllowed=treeSurvey==null?isTerrain(state):leafAccess(bp,state);
            cells.put(p,new Voxel(air,state.isCollisionShapeFullBlock(level,bp),safe,accessAllowed,state.toString(),choices));expected.put(p,state);
        }
        r.perception.recordWork(started);captureMillis+=(System.nanoTime()-started)/1_000_000.0;
        if(scanIndex<VOLUME)return;
        for(Pos p:requested){var v=cells.get(p);if(v==null){skippedUnknown++;continue;}if(!v.empty())targets.add(p);}
        if(targets.isEmpty()){phase=skippedUnknown==0?"COMPLETED":"BLOCKED";reason=skippedUnknown==0?"Requested cells are already empty; nothing was mined":"Requested targets are not sensed";return;}
        destructive=targets.size()>128;
        var snapshot=new Snapshot(base,cells,targets,supports,allowAccess,allowSupports,requireHarvest,mode.equals("access"),maxBreaks,maxDistance,treeSurvey!=null,r.player().blockInteractionRange());
        phase="PLANNING";step="PLAN";
        future=executor.submit(()->{long begin=System.nanoTime();var value=ExcavationPlanner.alternatives(snapshot);workerMillis=(System.nanoTime()-begin)/1_000_000.0;return value;});
    }
    private List<Tool> previewTools(BlockState state,BlockPos bp){
        String key=state.toString();if(toolCache.containsKey(key))return toolCache.get(key);
        double hardness=state.getDestroySpeed(r.player().level(),bp);if(hardness<0)return List.of();
        var out=new ArrayList<Tool>();var seen=new HashSet<String>();
        for(int slot=0;slot<=40;slot++){
            if(slot>=36 && slot<40)continue;var item=r.player().getInventory().getItem(slot);var data=item.get(net.minecraft.core.component.DataComponents.TOOL);
            if(item.isDamageableItem() && data==null)continue;
            String id=item.isEmpty()?"bare_hands":r.player().inventoryLedger.key(item);if(!seen.add(id))continue;
            int wear=item.isDamageableItem() && hardness>0?data.damagePerBlock():0;
            int remaining=item.isDamageableItem()?item.getMaxDamage()-item.getDamageValue():-1;
            if(remaining>=0 && remaining<=wear)continue;
            boolean harvest=!state.requiresCorrectToolForDrops() || item.isCorrectToolForDrops(state);
            double ticks=Math.max(1,Math.ceil(hardness*(harvest?30:100)/Math.max(.001,item.isEmpty()?1:item.getDestroySpeed(state))));
            out.add(new Tool(id,item.isEmpty()?"bare_hands":BuiltInRegistries.ITEM.getKey(item.getItem()).toString(),slot,remaining,wear,ticks,harvest));tools.putIfAbsent(id,item.copy());
        }
        out.sort(Comparator.comparingDouble(Tool::ticks));var result=List.copyOf(out);toolCache.put(key,result);return result;
    }
    public static boolean isTerrain(BlockState s){
        String id=BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        return Set.of("stone","deepslate","granite","diorite","andesite","tuff","calcite","dirt","coarse_dirt","rooted_dirt","grass_block","podzol","mycelium","sand","red_sand","gravel","clay","mud","snow","snow_block","netherrack","basalt","blackstone","end_stone").contains(id);
    }
    public JsonObject choose(UUID id,String option,boolean confirmDestructive){
        thread();require(id);if(!phase.equals("PLAN_READY"))throw new IllegalStateException("Wait for PLAN_READY");
        if(tick()-plannedTick>1200 || r.player().position().distanceToSqr(origin)>.04)throw new IllegalStateException("Plan expired or body moved");
        if(destructive && !confirmDestructive)throw new IllegalArgumentException("Large excavation: model must explicitly confirm the listed destructive scope");
        var candidate=options.stream().filter(p->p.id().equals(option)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown option"));
        for(var s:candidate.steps())if(!s.action().equals("move") && expected.containsKey(s.position()) && !r.player().level().getBlockState(blockPos(s.position())).equals(expected.get(s.position())))throw new IllegalStateException("Planned world changed");
        for(var e:candidate.wear().entrySet())findTool(e.getKey(),e.getValue());
        for(var e:candidate.materials().entrySet())if(stock(e.getKey())<e.getValue())throw new IllegalStateException("Support material changed or is protected");
        if(replant)saplingSlot();
        chosen=candidate;phase="EXECUTING";step="SELECT";reason="";actionIndex=0;distance=0;lastPosition=r.player().position();startedTick=tick();return status();
    }
    private int stock(String id){int count=0;for(int slot=0;slot<36;slot++){var s=r.player().getInventory().getItem(slot);if(!s.isEmpty() && r.player().inventoryLedger.key(s).equals(id) && r.player().inventoryLedger.expendable(s))count+=s.getCount();}return count;}
    private int findTool(String id,int reserve){
        for(int slot=0;slot<=40;slot++){if(slot>=36 && slot<40)continue;var s=r.player().getInventory().getItem(slot);
            if(id.equals("bare_hands")?s.isEmpty():!s.isEmpty() && r.player().inventoryLedger.key(s).equals(id)){
                if(s.isDamageableItem() && s.getMaxDamage()-s.getDamageValue()<=reserve)continue;return slot;
            }
        }throw new IllegalStateException("Approved tool is unavailable or has insufficient durability: "+id);
    }
    private Step current(){return chosen.steps().get(actionIndex);}
    private void execute(){
        var a=current();var p=r.player();var bp=blockPos(a.position());
        if(p.position().distanceToSqr(feet(a.from()))>4){block("Body left the evaluated working position");return;}
        if(a.action().equals("break")){
            if(protectedFarm(bp)){block("Farm declaration now protects this block");return;}
            if(!p.level().getBlockState(bp).toString().equals(a.expectedState())){block("Target state changed before break");return;}
            int slot=findTool(a.tool().identity(),a.tool().wear());child(()->{HandController.equipSlot(p,slot,InteractionHand.MAIN_HAND);return null;});
            var plan=child(()->r.mining().plan(bp,a.resource() && requireHarvest));UUID id=UUID.fromString(plan.get("requestId").getAsString());childMining.add(id.toString());
            child(()->r.mining().choose(id,"held-tool"));step="BREAK";
        }else if(a.action().equals("support")){
            if(stock(a.material())<1){block("Approved support is unavailable or newly protected");return;}
            var spec=new JsonObject();spec.addProperty("entry_id",a.material());spec.addProperty("x",bp.getX());spec.addProperty("y",bp.getY());spec.addProperty("z",bp.getZ());spec.addProperty("temporary",true);
            var plan=child(()->r.placement().plan(List.of(spec),false,0,false));UUID id=UUID.fromString(plan.get("requestId").getAsString());childPlacement.add(id.toString());
            child(()->r.placement().choose(id,"bounded-placement"));step="SUPPORT";
        }else{
            var dest=feet(a.position());var box=p.getBoundingBox().move(dest.subtract(p.position()));
            if(!p.level().isLoaded(bp) || !p.level().noCollision(p,box) || !p.level().getBlockState(bp.below()).isCollisionShapeFullBlock(p.level(),bp.below())){block("Planned walking cell is no longer clear and supported");return;}
            var action=a.position().y()>a.from().y()?RouteOption.Action.JUMP:a.position().y()<a.from().y()?RouteOption.Action.STEP_DOWN:RouteOption.Action.WALK;
            var route=new RouteOption("excavation-step","Approved excavation corridor",TravelPace.WALK,Set.of(TravelPace.WALK),a.from().distance(a.position())/3,a.from().distance(a.position()),0,0,0,0,0,true,List.of(),List.of("walk"),List.of(new RouteOption.PathStep(p.getX(),p.getY(),p.getZ(),RouteOption.Action.WALK,TravelPace.WALK,45),new RouteOption.PathStep(dest.x,dest.y,dest.z,action,TravelPace.WALK,45)),List.of());
            var destination=new NavigationPlan.ResolvedDestination(dimension,dest.x,dest.y,dest.z,.5,false,"excavation:"+request,OptionalDouble.empty(),Optional.empty());
            follower.start(new NavigationPlan(UUID.randomUUID(),0,java.time.Instant.now(),destination,List.of(route)),route,TravelPace.WALK,OptionalDouble.empty());routeEvent=null;step="MOVE";
        }
    }
    private void finish(){
        if(mode.equals("access")){var target=blockPos(requested.getFirst());if(!r.mining().reachable(target)){block("Arrival does not reach the requested target");return;}var look=Vec3.atCenterOf(target).subtract(r.player().getEyePosition());
            r.player().setYRot((float)Math.toDegrees(Math.atan2(-look.x,look.z)));r.player().setXRot((float)-Math.toDegrees(Math.atan2(look.y,Math.hypot(look.x,look.z))));r.player().setYHeadRot(r.player().getYRot());
            phase="COMPLETED";reason="Physically reached and faced a working target within native reach; target remains unmined";r.player().stopControlling();return;}
        if(collectDrops && !pickup.verified()){pickup.begin();step="PICKUP";return;}
        completeCells();
    }
    private void completeCells(){
        int remaining=0;for(Pos target:requested){var bp=blockPos(target);if(!r.player().level().isLoaded(bp) || !r.player().level().getBlockState(bp).isAir() && !(treeCleared && replant && treeSurvey.roots().contains(bp) && rootPlanted(bp)))remaining++;}
        treeCleared=treeSurvey!=null && remaining==0 && skippedUnknown==0;
        if(treeCleared && replant && !replanted){
            if(neededSaplings()==0){replanted=true;phase="COMPLETED";reason="Tree harvest, collection and replanting verified";return;}
            int slot=saplingSlot();var specifications=new ArrayList<JsonObject>();for(var root:treeSurvey.roots()){
                if(rootPlanted(root))continue;
                var spec=TreeSurvey.position(root);spec.addProperty("entry_id",r.player().inventoryLedger.key(r.player().getInventory().getItem(slot)));specifications.add(spec);
            }
            var plan=child(()->r.placement().plan(specifications,true,Math.min(256,Math.max(0,maxDistance-distance)),false));var id=UUID.fromString(plan.get("requestId").getAsString());childPlacement.add(id.toString());child(()->r.placement().choose(id,"bounded-placement"));step="REPLANT";return;
        }
        phase=remaining==0 && skippedUnknown==0?"COMPLETED":"PARTIAL";
        reason=phase.equals("COMPLETED")?"Every requested cell is physically empty. Collection is separately proven by counted native pickup receipts when enabled":"Evaluated work ended with remaining/unknown targets: "+remaining;
        r.player().stopControlling();
    }
    public JsonObject interrupt(UUID id,boolean pause){thread();require(id);if(pause && !phase.equals("EXECUTING"))throw new IllegalStateException("Only an executing excavation can pause");if(ownsBody() || phase.equals("PLAN_READY")){
            if(phase.equals("EXECUTING") && step.equals("BREAK") && r.mining().phase().equals("COMPLETED")){receipts.add(r.mining().status());actionIndex++;}
            if(phase.equals("EXECUTING") && step.equals("SUPPORT") && r.placement().status().get("phase").getAsString().equals("COMPLETED")){receipts.add(r.placement().status());actionIndex++;}
            stopChildren();phase=pause?"PAUSED":"CANCELLED";reason=pause?"Paused by controller":"Cancelled by controller";}return status();}
    public JsonObject resume(UUID id){thread();require(id);if(!phase.equals("PAUSED"))throw new IllegalStateException("Excavation is not paused");phase="EXECUTING";step="SELECT";lastPosition=r.player().position();return status();}
    public void cancelForChat(){if(request!=null && (ownsBody() || phase.equals("PLAN_READY")))interrupt(request,false);}
    private void require(UUID id){if(request==null || !request.equals(id))throw new IllegalArgumentException("Stale excavation request ID");}
    private void stopChildren(){pickup.cancel();if(future!=null){future.cancel(true);future=null;}follower.cancel("Excavation interrupted");child(()->{r.mining().cancelForChat();r.placement().cancelForChat();return null;});r.player().stopControlling();routeEvent=null;}
    private void block(String why){stopChildren();phase="BLOCKED";reason=why;}
    public JsonObject status(){
        var o=new JsonObject();o.addProperty("phase",phase);o.addProperty("step",step);o.addProperty("reason",reason);o.addProperty("ownsBody",ownsBody());
        if(request!=null){o.addProperty("requestId",request.toString());o.addProperty("dimension",dimension);o.add("fixedOrigin",new Gson().toJsonTree(base));o.addProperty("mode",mode);}
        o.addProperty("snapshotProgress",scanIndex/(double)VOLUME);o.addProperty("captureServerMillis",captureMillis);o.addProperty("plannerMillis",workerMillis);o.addProperty("unknownTargets",skippedUnknown);o.addProperty("destructiveConfirmationRequired",destructive);
        var choices=new JsonArray();if(phase.equals("PLAN_READY"))for(var plan:options){var value=new Gson().toJsonTree(plan).getAsJsonObject();value.remove("steps");value.addProperty("optionId",plan.id());value.addProperty("plannedActions",plan.steps().size());value.addProperty("requestedTargetCount",requested.size());
            var manifests=new JsonArray();for(var entry:plan.materials().entrySet()){var m=new JsonObject();m.addProperty("entryId",entry.getKey());m.addProperty("count",entry.getValue());supports.stream().filter(s->s.identity().equals(entry.getKey())).findFirst().ifPresent(s->{m.addProperty("item",s.item());m.addProperty("importance",s.importance());});manifests.add(m);}value.add("supportMaterials",manifests);
            var toolRows=new JsonArray();for(var entry:plan.wear().entrySet()){var item=tools.get(entry.getKey());var row=new JsonObject();row.addProperty("entryId",entry.getKey());row.addProperty("item",item==null || item.isEmpty()?"bare_hands":BuiltInRegistries.ITEM.getKey(item.getItem()).toString());row.addProperty("nominalWear",entry.getValue());if(item!=null && item.isDamageableItem()){row.addProperty("remainingDurability",item.getMaxDamage()-item.getDamageValue());row.addProperty("afterNominalWear",item.getMaxDamage()-item.getDamageValue()-entry.getValue());}toolRows.add(row);}value.add("tools",toolRows);
            value.addProperty("pickupIncluded",collectDrops);value.addProperty("pickupTravelBudgetRemaining",Math.max(0,maxDistance-plan.distance()));value.addProperty("timeEstimateScope","Base tool wear/time and evaluated local travel, excludes unknown effects, network/model delay and pickup");value.addProperty("hazards","Rejects unknown cells, fluids, falling ceilings, block entities and declared farms; live checks can block execution");choices.add(value);}o.add("options",choices);
        if(treeSurvey!=null){o.add("tree",treeSurvey.json());o.addProperty("wholeTreeVerified",treeCleared && pickup.verified());o.addProperty("replantRequested",replant);o.addProperty("replantVerified",replanted);o.addProperty("replantItem",saplingId);o.addProperty("replantCount",treeSurvey.roots().size());}
        o.addProperty("completedActions",actionIndex);o.addProperty("physicalDistance",distance);o.addProperty("bodyX",r.player().getX());o.addProperty("bodyY",r.player().getY());o.addProperty("bodyZ",r.player().getZ());
        if(chosen!=null){o.addProperty("selectedOptionId",chosen.id());o.addProperty("totalActions",chosen.steps().size());if(actionIndex<chosen.steps().size())o.add("currentAction",new Gson().toJsonTree(current()));}
        o.add("childMiningRequestIds",new Gson().toJsonTree(childMining));o.add("childPlacementRequestIds",new Gson().toJsonTree(childPlacement));o.addProperty("physicalBreakReceipts",receipts.size());o.addProperty("collectionVerified",collectDrops && pickup.verified());o.addProperty("collectDrops",collectDrops);o.add("collection",pickup.status());return o;
    }
    public static Pos pos(BlockPos p){return new Pos(p.getX(),p.getY(),p.getZ());}
    public static BlockPos blockPos(Pos p){return new BlockPos(p.x(),p.y(),p.z());}
    private static Vec3 feet(Pos p){return new Vec3(p.x()+.5,p.y(),p.z()+.5);}
    @Override public void close(){cancelForChat();pickup.close();executor.close();}
}
