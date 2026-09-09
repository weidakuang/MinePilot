package dev.mcai.companion.agent.placement;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import com.google.gson.*;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.body.*;
import dev.mcai.companion.agent.navigation.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** A bounded placement job. All mutations use native inventory/use/break actions. */
public final class PlacementCoordinator implements AutoCloseable {
    public record Cell(BlockPos pos,ItemStack item,String entry,Map<String,String> state,InteractionHand hand,boolean jump,boolean temporary,BlockState before) {}
    private final AgentRuntime r;private final MinePilotServerPlayer p;
    private final PlanningExecutor planner=new PlanningExecutor(NavigationPlannerConfig.defaults());
    private final NavigationSnapshotBuilder snapshots=new NavigationSnapshotBuilder(new NavigationSnapshotBuilder.CaptureConfig(4,4,6,48,24,50_000,8));
    private final NavigationFollower follower;
    private List<BlockPos> reservedAir=List.of();
    private final List<Cell> cells=new ArrayList<>();private final Set<BlockPos> finished=new HashSet<>(),skipped=new HashSet<>(),cleaned=new HashSet<>();
    private final Map<BlockPos,BlockState> placed=new LinkedHashMap<>();private final Map<BlockPos,BlockState> expected=new LinkedHashMap<>();
    private final Set<String> childMining=new HashSet<>();private final List<BlockPos> approaches=new ArrayList<>();
    private final JsonArray receipts=new JsonArray();private JsonArray decisions=new JsonArray();
    private UUID request,mineId;private String phase="IDLE",step="",reason="",dimension,decisionId;
    private Vec3 origin,lastPosition,routeStart;private double distance,maxDistance;
    private int started,stepStarted,jumpStarted=-1,revision;private boolean allowMove,internal,cleaning,cleanup=true,jumpSawAirborne;
    private Cell active;private PlacementGeometry.Aim aim;private Map<BlockPos,BlockState> pendingFootprint;
    private BlockPos landingTarget;
    private int plannedTick;
    private int beforeCount;private CompletableFuture<NavigationPlan> pendingRoute;private NavigationEvent routeEvent;
    private NavigationPlan route;private RouteOption chosenRoute;
    public PlacementCoordinator(AgentRuntime runtime){r=runtime;p=r.player();follower=new NavigationFollower(p,e->{if(e.type()!=NavigationEvent.Type.NAVIGATION_PROGRESS)routeEvent=e;},id->null,why->invalidateRoute(why));}
    private void invalidateRoute(String why){follower.requestReplan(why);}
    public boolean executing(){return phase.equals("EXECUTING");}
    public boolean ownsBody(){return Set.of("EXECUTING","PAUSED").contains(phase);}
    public boolean internalAction(){return internal;}
    public boolean isChildRequest(JsonObject state){return state.has("requestId") && childMining.contains(state.get("requestId").getAsString());}
    private <T>T child(Supplier<T> action){internal=true;try{return action.get();}finally{internal=false;}}
    private int tick(){return r.server().getTickCount();}
    private void thread(){if(!r.server().isSameThread())throw new IllegalStateException("Placement requires the server thread");}
    private void idle(){
        thread();var n=r.navigation().status();
        if(r.collection().ownsBody() || r.mining().ownsBody() || r.jumpActive() || r.turnActive() || !n.phase().terminal() && n.phase()!=NavigationToolCoordinator.Phase.IDLE)
            throw new IllegalStateException("Finish or cancel the current body action first");
        if(!p.isAlive() || p.gameMode.getGameModeForPlayer()!=GameType.SURVIVAL)throw new IllegalStateException("Placement requires a living survival player; game-mode restrictions are not bypassed");
    }
    public JsonObject plan(List<JsonObject> specifications,boolean move,double travelBudget,boolean clean){
        return plan(specifications,move,travelBudget,clean,List.of());
    }
    public JsonObject plan(List<JsonObject> specifications,boolean move,double travelBudget,boolean clean,List<BlockPos> air){
        idle();if(ownsBody())throw new IllegalStateException("Cancel the current placement job before replacing it");
        if(specifications.isEmpty() || specifications.size()>256)throw new IllegalArgumentException("Placement accepts 1..256 item-use targets");
        if(!Double.isFinite(travelBudget) || travelBudget<0 || travelBudget>256)throw new IllegalArgumentException("Movement budget must be 0..256 blocks");
        var prepared=new ArrayList<Cell>();var occupied=new HashSet<BlockPos>();
        for(var a:specifications){
            var pos=PlacementTools.position(a,"");
            if(!occupied.add(pos))throw new IllegalArgumentException("Duplicate placement coordinate");
            if(!r.perception.observableBlock(pos) || pos.distToCenterSqr(p.position())>(move?24*24:25))throw new IllegalArgumentException("Target is outside the sensed bounded work area");
            var stack=p.getInventory().getItem(HandController.resolve(p,a)).copyWithCount(1);
            if(!(stack.getItem() instanceof BlockItem item))throw new IllegalArgumentException("This placement tool requires a real block item");
            var state=new LinkedHashMap<String,String>();
            if(a.has("state"))for(var e:a.getAsJsonObject("state").entrySet()){
                if(!e.getValue().isJsonPrimitive() || !e.getValue().getAsJsonPrimitive().isString())throw new IllegalArgumentException("Block-state values must be strings");
                state.put(e.getKey(),e.getValue().getAsString());
            }
            PlacementGeometry.validate(item.getBlock(),state);
            if("upper".equals(state.get("half")) && item.getBlock() instanceof DoorBlock || "head".equals(state.get("part")) && item.getBlock() instanceof BedBlock)
                throw new IllegalArgumentException("Specify the door's lower cell or bed's foot once; the other cell is generated by vanilla");
            boolean temporary=PlacementTools.bool(a,"temporary",false);
            if(temporary && (!item.getBlock().defaultBlockState().isCollisionShapeFullBlock(p.level(),pos) || item.getBlock() instanceof FallingBlock))throw new IllegalArgumentException("Temporary supports require a stable full-cube block");
            if(temporary && !p.inventoryLedger.expendable(stack))throw new IllegalArgumentException("Importance 0..2 items cannot be temporary/support materials");
            prepared.add(new Cell(pos,stack,p.inventoryLedger.key(stack),Map.copyOf(state),HandController.hand(PlacementTools.string(a,"hand","main")),PlacementTools.bool(a,"jump",false),temporary,p.level().getBlockState(pos)));
        }
        for(var q:air)if(!r.perception.observableBlock(q) || q.distToCenterSqr(p.position())>(move?24*24:25) || !p.level().getBlockState(q).isAir())throw new IllegalArgumentException("Reserved air must be sensed, in bounds and already empty; this placement plan does not authorize clearing it");
        stopChildren();landingTarget=null;reservedAir=List.copyOf(air);
        cells.clear();cells.addAll(prepared);finished.clear();skipped.clear();cleaned.clear();placed.clear();expected.clear();childMining.clear();receipts.asList().clear();
        for(var c:cells)expected.put(c.pos,c.before);
        request=UUID.randomUUID();phase="PLAN_READY";step="";reason="";dimension=p.level().dimension().identifier().toString();origin=p.position();plannedTick=tick();
        allowMove=move;maxDistance=travelBudget;cleanup=clean;distance=0;cleaning=false;active=null;decisions=new JsonArray();revision++;return status();
    }
    public JsonObject choose(UUID id,String option){
        idle();require(id);if(!phase.equals("PLAN_READY") || !option.equals("bounded-placement"))throw new IllegalArgumentException("Choose the current bounded placement option");
        if(tick()-plannedTick>1200 || !p.level().dimension().identifier().toString().equals(dimension))throw new IllegalStateException("Placement plan expired or changed dimension; create a new plan");
        String missing=resources();if(missing!=null)throw new IllegalStateException(missing);
        for(var c:cells)if(!p.level().getBlockState(c.pos).equals(c.before))throw new IllegalStateException("A target changed since planning");
        decisions=new JsonArray();decisionId=null;phase="EXECUTING";started=tick();lastPosition=p.position();advance();return status();
    }
    public JsonObject one(JsonObject a){var result=plan(List.of(a),false,0,false);return choose(UUID.fromString(result.get("requestId").getAsString()),"bounded-placement");}
    private void require(UUID id){if(request==null || !request.equals(id))throw new IllegalArgumentException("Stale placement request");}
    private String resources(){
        for(var e:materialCounts().entrySet())if(count(e.getKey())<e.getValue())return "Missing reserved placement material "+e.getKey()+"; need "+e.getValue()+", have "+count(e.getKey());return null;
    }
    private Map<String,Integer> materialCounts(){var m=new LinkedHashMap<String,Integer>();for(var c:cells)if(!finished.contains(c.pos) && !skipped.contains(c.pos) && !alreadyCorrect(c))m.merge(c.entry,1,Integer::sum);return m;}
    private int count(String entry){int n=0;for(int i=0;i<=40;i++){if(i>=36 && i<40)continue;var s=p.getInventory().getItem(i);if(!s.isEmpty() && p.inventoryLedger.key(s).equals(entry))n+=s.getCount();}return n;}
    private boolean alreadyCorrect(Cell c){
        if(!p.level().isLoaded(c.pos))return false;
        var actual=p.level().getBlockState(c.pos);if(!(c.item.getItem() instanceof BlockItem b) || actual.getBlock().asItem()!=c.item.getItem() || !PlacementGeometry.matches(actual,c.state))return false;
        if(actual.getBlock() instanceof DoorBlock && actual.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)!=net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER || actual.getBlock() instanceof BedBlock && actual.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.BED_PART)!=net.minecraft.world.level.block.state.properties.BedPart.FOOT)return false;
        if(actual.getBlock() instanceof DoorBlock || actual.getBlock() instanceof BedBlock)for(var e:PlacementGeometry.footprint(c.pos,actual).entrySet())if(!p.level().getBlockState(e.getKey()).equals(e.getValue()))return false;
        return true;
    }
    private void advance(){active=null;aim=null;jumpStarted=-1;step="SELECT";stepStarted=tick();p.stopControlling();}
    public void beforePhysics(){if(phase.equals("EXECUTING") && step.equals("TRAVEL"))follower.beforePhysicsTick();}
    public void afterPhysics(){
        if(!phase.equals("EXECUTING"))return;
        if(!p.isAlive() || p.gameMode.getGameModeForPlayer()!=GameType.SURVIVAL || !p.level().dimension().identifier().toString().equals(dimension)){block("BODY_UNAVAILABLE",null);return;}
        if(lastPosition!=null)distance+=lastPosition.distanceTo(p.position());lastPosition=p.position();
        if(tick()-started>12000 || distance>maxDistance+2 && allowMove){block("JOB_BUDGET_EXHAUSTED",null);return;}
        try{
            switch(step){
                case "SELECT" -> select();
                case "AIM","JUMP" -> placeTick();
                case "VERIFY" -> verify();
                case "LAND" -> {if(p.onGround()){if(p.getY()<active.pos.getY()+.95){block("JUMP_LANDING_NOT_VERIFIED",active.pos);return;}advance();}else if(tick()-stepStarted>60)block("LANDING_TIMEOUT",active.pos);}
                case "PLAN_ROUTE" -> resolveRoute();
                case "TRAVEL" -> {follower.tick();if(routeEvent!=null){var e=routeEvent;routeEvent=null;if(e.type()==NavigationEvent.Type.NAVIGATION_COMPLETED){step="SELECT";active=null;}else if(e.type()==NavigationEvent.Type.NAVIGATION_DECISION_REQUIRED || e.type()==NavigationEvent.Type.NAVIGATION_FAILED)block("APPROACH_BLOCKED: "+e.message(),active==null?null:active.pos);}}
                case "MINE" -> {
                    var m=r.mining().status();String phase=m.get("phase").getAsString();
                    if(phase.equals("COMPLETED")){
                        var row=m.deepCopy();row.addProperty("operation",cleaning?"temporary_cleanup":"approved_obstacle_break");receipts.add(row);
                        expected.put(active.pos,p.level().getBlockState(active.pos));if(cleaning)cleaned.add(active.pos);cleaning=false;advance();
                    }else if(phase.equals("BLOCKED") || phase.equals("CANCELLED"))block("MINING_CHILD_BLOCKED: "+m.get("reason").getAsString(),active.pos);
                }
                default -> block("Unknown placement execution step",null);
            }
        }catch(RuntimeException failure){block("ACTION_REJECTED: "+String.valueOf(failure.getMessage()),active==null?null:active.pos);}
    }
    private void select(){
        if(landingTarget!=null){
            if(!p.onGround()){if(tick()-stepStarted>60)block("LANDING_TIMEOUT",landingTarget);return;}
            if(p.getY()<landingTarget.getY()+.95){block("JUMP_LANDING_NOT_VERIFIED",landingTarget);return;}landingTarget=null;
        }
        for(var c:cells){
            if(finished.contains(c.pos) || skipped.contains(c.pos))continue;
            active=c;
            if(!p.level().isLoaded(c.pos) || !p.level().getBlockState(c.pos).equals(expected.get(c.pos))){block("TARGET_CHANGED",c.pos);return;}
            if(alreadyCorrect(c)){finished.add(c.pos);advance();return;}
            var a=new JsonObject();a.addProperty("entry_id",c.entry);int slot=HandController.resolve(p,a);
            if(c.temporary && !p.inventoryLedger.expendable(p.getInventory().getItem(slot))){block("MATERIAL_NOW_PROTECTED",c.pos);return;}
            HandController.equipSlot(p,slot,c.hand);
            var candidates=PlacementGeometry.aims(p,c.pos,p.getItemInHand(c.hand),c.hand,c.state,p.position());
            if(!candidates.isEmpty()){aim=candidates.getFirst();step="AIM";stepStarted=tick();return;}
            if(c.jump && c.pos.equals(p.blockPosition()) && p.level().getBlockState(c.pos).isAir()){
                if(!p.onGround()){if(tick()-stepStarted>20)block("JUMP_TAKEOFF_NOT_STABLE",c.pos);return;}
                if(!p.level().noCollision(p,p.getBoundingBox().expandTowards(0,1.25,0))){block("JUMP_HEADROOM_BLOCKED",c.pos);return;}
                step="JUMP";stepStarted=tick();jumpStarted=tick();jumpSawAirborne=false;return;
            }
            if(allowMove){prepareApproach();return;}
            block("NO_LEGAL_PLACEMENT_FACE_OR_STATE",c.pos);return;
        }
        if(cleanup)for(var c:cells)if(c.temporary && placed.containsKey(c.pos) && !cleaned.contains(c.pos)){
            active=c;cleaning=true;
            if(!p.level().getBlockState(c.pos).equals(placed.get(c.pos))){block("TEMPORARY_BLOCK_CHANGED",c.pos);return;}
            for(var d:Direction.values()){
                var q=c.pos.relative(d);var s=p.level().getBlockState(q);
                if(!s.isAir() && (s.getBlock() instanceof FallingBlock || !s.isCollisionShapeFullBlock(p.level(),q))){block("CLEANUP_MAY_REMOVE_AN_ATTACHMENT_SUPPORT",c.pos);return;}
            }
            beginMine();return;
        }
        for(var c:cells)if(finished.contains(c.pos) && !skipped.contains(c.pos) && !cleaned.contains(c.pos) && !alreadyCorrect(c)){block("REQUIRED_TARGET_CHANGED_BEFORE_FINAL_VERIFICATION",c.pos);return;}
        for(var q:reservedAir)if(!p.level().isLoaded(q) || !p.level().getBlockState(q).isAir()){block("RESERVED_AIR_CHANGED",q);return;}
        phase=skipped.isEmpty()?"COMPLETED":"PARTIAL";reason=skipped.isEmpty()?"Required cells and companion states verified through native placement; see actual material receipts":"Some required targets were explicitly skipped";step="";p.stopControlling();
    }
    private void placeTick(){
        var c=active;
        if(tick()-stepStarted>80){block("AIM_OR_JUMP_TIMEOUT",c.pos);return;}
        if(!p.inventoryLedger.key(p.getItemInHand(c.hand)).equals(c.entry) || c.temporary && !p.inventoryLedger.expendable(p.getItemInHand(c.hand))){block("HELD_ITEM_OR_POLICY_CHANGED",c.pos);return;}
        if(!p.level().getBlockState(c.pos).equals(expected.get(c.pos))){block("TARGET_CHANGED",c.pos);return;}
        if(step.equals("AIM")){var candidates=PlacementGeometry.aims(p,c.pos,p.getItemInHand(c.hand),c.hand,c.state,p.position());if(candidates.isEmpty()){block("PLACEMENT_FACE_CHANGED",c.pos);return;}aim=candidates.getFirst();}
        if(step.equals("JUMP")){
            jumpSawAirborne|=!p.onGround();
            if(p.onGround() && jumpSawAirborne){block("JUMP_DID_NOT_CREATE_PLACEMENT_WINDOW",c.pos);return;}
            var candidates=PlacementGeometry.aims(p,c.pos,p.getItemInHand(c.hand),c.hand,c.state,p.position());
            if(!candidates.isEmpty())aim=candidates.getFirst();
            if(aim==null){p.applyControlFrame(new AgentControlFrame(p.getYRot(),90,0,0,!jumpSawAirborne && p.onGround(),false,false));return;}
        }
        p.applyControlFrame(new AgentControlFrame(aim.yaw(),aim.pitch(),0,0,false,false,true));
        if(!p.isShiftKeyDown() || !PlacementGeometry.clearActualRay(p,aim))return;
        if(!p.level().mayInteract(p,c.pos) || p.blockActionRestricted(p.level(),c.pos,p.gameMode.getGameModeForPlayer()) || r.server().isUnderSpawnProtection(p.level(),c.pos,p)){block("SERVER_FORBIDS_TARGET",c.pos);return;}
        var refreshed=PlacementGeometry.aims(p,c.pos,p.getItemInHand(c.hand),c.hand,c.state,p.position());
        var current=refreshed.stream().filter(a->PlacementGeometry.clearActualRay(p,a)).findFirst().orElse(null);
        if(current==null)return;
        pendingFootprint=PlacementGeometry.footprint(c.pos,current.state());
        for(var q:pendingFootprint.keySet()){
            if(!q.equals(c.pos) && (!p.level().getBlockState(q).canBeReplaced() || expected.containsKey(q))){block("COMPOUND_PLACEMENT_CELL_OCCUPIED_OR_DUPLICATED",q);return;}
            if(!p.level().mayInteract(p,q) || r.server().isUnderSpawnProtection(p.level(),q,p)){block("SERVER_FORBIDS_COMPANION_CELL",q);return;}
        }
        if(pendingFootprint.keySet().stream().anyMatch(reservedAir::contains)){block("COMPOUND_CONFLICTS_WITH_RESERVED_AIR",c.pos);return;}
        beforeCount=count(c.entry);var before=p.level().getBlockState(c.pos);
        p.swing(c.hand);p.gameMode.useItemOn(p,p.level(),p.getItemInHand(c.hand),c.hand,current.hit());
        var row=new JsonObject();row.addProperty("operation","place");row.addProperty("tick",tick());row.add("target",PlacementTools.xyz(c.pos));row.addProperty("before",before.toString());row.addProperty("after",p.level().getBlockState(c.pos).toString());row.addProperty("entryId",c.entry);row.addProperty("consumed",beforeCount-count(c.entry));receipts.add(row);
        if(beforeCount-count(c.entry)!=1){block("NATIVE_USE_DID_NOT_CONSUME_ONE_PLACEMENT_ITEM",c.pos);return;}
        step="VERIFY";stepStarted=tick();verify();
    }
    private void verify(){
        for(var e:pendingFootprint.entrySet())if(!p.level().getBlockState(e.getKey()).equals(e.getValue())){block("PLACED_STATE_NOT_VERIFIED",e.getKey());return;}
        placed.putAll(pendingFootprint);finished.add(active.pos);
        if(jumpStarted>=0){landingTarget=active.pos;step="LAND";stepStarted=tick();p.stopControlling();}else advance();
    }
    private void prepareApproach(){
        approaches.clear();var c=active;
        for(int x=-3;x<=3;x++)for(int y=-3;y<=1;y++)for(int z=-3;z<=3;z++){
            var q=c.pos.offset(x,y,z);if(q.equals(c.pos) || !r.perception.observableBlock(q) || q.distToCenterSqr(origin)>24*24)continue;
            var feet=Vec3.atBottomCenterOf(q);if(!p.level().getBlockState(q.below()).isCollisionShapeFullBlock(p.level(),q.below()) || !p.level().noCollision(p,p.getBoundingBox().move(feet.subtract(p.position()))))continue;
            approaches.add(q);
        }
        approaches.sort(Comparator.comparingDouble(q->q.distToCenterSqr(p.position())));if(approaches.size()>16)approaches.subList(16,approaches.size()).clear();approaches.removeIf(q->PlacementGeometry.aims(p,c.pos,c.item,c.hand,c.state,Vec3.atBottomCenterOf(q)).isEmpty());if(approaches.size()>4)approaches.subList(4,approaches.size()).clear();beginRoute();
    }
    private void beginRoute(){
        if(approaches.isEmpty()){block("NO_EVALUATED_SAFE_APPROACH",active.pos);return;}
        var dest=Vec3.atBottomCenterOf(approaches.removeFirst());routeStart=p.position();
        var destination=new NavigationPlan.ResolvedDestination(dimension,dest.x,dest.y,dest.z,.5,false,"placement:"+request,OptionalDouble.empty(),Optional.empty());
        pendingRoute=planner.submit(UUID.randomUUID(),snapshots.capture(p,destination,0));step="PLAN_ROUTE";routeEvent=null;
    }
    private void resolveRoute(){
        if(!pendingRoute.isDone())return;
        try{route=pendingRoute.join();}catch(RuntimeException failure){pendingRoute=null;beginRoute();return;}pendingRoute=null;
        if(routeStart.distanceToSqr(p.position())>.04){block("BODY_MOVED_DURING_ROUTE_PLANNING",active.pos);return;}
        chosenRoute=route.options().stream().filter(o->o.feasibleNow() && o.supportBlocksRequired()==0 && o.estimatedHealthLost()==0 && o.distanceBlocks()+distance<=maxDistance && o.steps().stream().allMatch(s->new Vec3(s.x(),s.y(),s.z()).distanceToSqr(origin)<=24*24)).min(Comparator.comparingDouble(RouteOption::distanceBlocks)).orElse(null);
        if(chosenRoute==null){beginRoute();return;}follower.start(route,chosenRoute,TravelPace.AUTO,OptionalDouble.empty());step="TRAVEL";
    }
    private void stopChildren(){
        if(pendingRoute!=null){pendingRoute.cancel(true);pendingRoute=null;}follower.cancel("Placement action stopped");
        if(mineId!=null && childMining.contains(mineId.toString()) && r.mining().status().has("requestId") && r.mining().status().get("requestId").getAsString().equals(mineId.toString()))child(()->r.mining().interrupt(mineId,false,"Placement parent stopped"));
        mineId=null;p.stopControlling();
    }
    private void block(String why,BlockPos obstacle){
        stopChildren();phase="BLOCKED";step="";reason=why;decisionId=request+":"+(++revision);decisions=new JsonArray();
        for(String action:List.of("cancel","retry","skip")){var o=new JsonObject();o.addProperty("optionId",action);o.addProperty("description",action.equals("retry")?"Recheck the same bounded target after the observed obstruction changes; no hidden excavation":action.equals("skip")?"Omit the current target; final outcome remains PARTIAL":"Cancel remaining work; already changed cells remain");o.add("estimatedSeconds",action.equals("cancel")?new JsonPrimitive(0):JsonNull.INSTANCE);decisions.add(o);}
        if(obstacle!=null && active!=null && obstacle.equals(active.pos) && !p.level().getBlockState(obstacle).isAir() && !placed.containsKey(obstacle)){
            try{
                selectBreakTool(obstacle);var preview=child(()->r.mining().plan(obstacle,true));mineId=UUID.fromString(preview.get("requestId").getAsString());childMining.add(mineId.toString());
                var o=preview.getAsJsonArray("options").get(0).getAsJsonObject().deepCopy();o.addProperty("optionId","mine_obstacle");o.add("target",PlacementTools.xyz(obstacle));o.addProperty("description","Approve this exact normal break with the selected real inventory tool and displayed durability cost; then retry the original placement");decisions.add(o);
            }catch(RuntimeException unsupported){/* Only offer physically evaluated excavation. */}
        }
    }
    private void selectBreakTool(BlockPos target){
        var state=p.level().getBlockState(target);int best=-1;float speed=-1;
        // Equal-speed choices prefer the existing hand; damageable items are not
        // needlessly selected for blocks the current hand can harvest equally fast.
        int held=p.getInventory().getSelectedSlot();var order=new ArrayList<Integer>();order.add(held);
        for(int i=0;i<=40;i++)if(i!=held && (i<36 || i==40))order.add(i);
        for(int slot:order){
            var candidate=p.getInventory().getItem(slot);
            if(state.requiresCorrectToolForDrops() && !candidate.isCorrectToolForDrops(state))continue;
            float current=candidate.isEmpty()?1:candidate.getDestroySpeed(state);
            if(current>speed){best=slot;speed=current;}
        }
        if(best<0)throw new IllegalStateException("No available tool can harvest this obstruction");
        HandController.equipSlot(p,best,InteractionHand.MAIN_HAND);
    }
    private void beginMine(){selectBreakTool(active.pos);var plan=child(()->r.mining().plan(active.pos,true));mineId=UUID.fromString(plan.get("requestId").getAsString());childMining.add(mineId.toString());child(()->r.mining().choose(mineId,"held-tool"));step="MINE";}
    public JsonObject resolve(UUID id,String version,String option){
        idle();require(id);if(!phase.equals("BLOCKED") || !Objects.equals(version,decisionId))throw new IllegalArgumentException("Stale obstacle decision");
        if(decisions.asList().stream().noneMatch(o->o.getAsJsonObject().get("optionId").getAsString().equals(option)))throw new IllegalArgumentException("Choose a returned obstacle option");
        if(option.equals("cancel"))return interrupt(id,false);
        if(option.equals("mine_obstacle")){child(()->r.mining().choose(mineId,"held-tool"));phase="EXECUTING";step="MINE";}
        else {
            if(option.equals("skip")){if(active==null)throw new IllegalStateException("No single active target to skip");skipped.add(active.pos);if(cleaning){cleaned.add(active.pos);cleaning=false;}}
            stopChildren();phase="EXECUTING";advance();
        }
        started=tick();lastPosition=p.position();return status();
    }
    public JsonObject interrupt(UUID id,boolean pause){thread();require(id);if(pause && !ownsBody())throw new IllegalStateException("Only executing work can be paused");stopChildren();phase=pause?"PAUSED":"CANCELLED";step="";reason=pause?"Paused by controller":"Cancelled; completed cells and remaining temporary supports are retained";return status();}
    public JsonObject resume(UUID id){idle();require(id);if(!phase.equals("PAUSED"))throw new IllegalStateException("Placement is not paused");decisions=new JsonArray();decisionId=null;phase="EXECUTING";started=tick();lastPosition=p.position();advance();return status();}
    public void cancelForChat(){if(request!=null && (ownsBody() || phase.equals("PLAN_READY") || phase.equals("BLOCKED")))interrupt(request,false);}
    public JsonObject status(){
        var out=new JsonObject();out.addProperty("phase",phase);out.addProperty("step",step);out.addProperty("reason",reason);out.addProperty("ownsBody",ownsBody());out.addProperty("physicalDistance",distance);
        out.addProperty("bodyX",p.getX());out.addProperty("bodyY",p.getY());out.addProperty("bodyZ",p.getZ());out.addProperty("dimension",p.level().dimension().identifier().toString());
        if(request!=null){out.addProperty("requestId",request.toString());out.addProperty("revision",revision);out.addProperty("requestedTargets",cells.size());out.addProperty("satisfiedTargets",finished.size());out.addProperty("skippedTargets",skipped.size());}
        if(active!=null)out.add("currentTarget",PlacementTools.xyz(active.pos));
        if(phase.equals("BLOCKED") && active!=null && p.level().isLoaded(active.pos)){
            var obstruction=new JsonObject();obstruction.addProperty("targetState",p.level().getBlockState(active.pos).toString());obstruction.addProperty("eyeDistance",p.getEyePosition().distanceTo(Vec3.atCenterOf(active.pos)));obstruction.addProperty("interactionRange",Math.min(5,p.blockInteractionRange()));
            var hit=p.level().clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(),Vec3.atCenterOf(active.pos),net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
            if(hit.getType()==HitResult.Type.BLOCK){obstruction.add("firstBlockTowardTargetCenter",PlacementTools.xyz(hit.getBlockPos()));obstruction.addProperty("firstBlockState",p.level().getBlockState(hit.getBlockPos()).toString());}
            obstruction.addProperty("coverage","This is the current center ray, not proof that every possible face or approach is blocked");out.add("obstruction",obstruction);
        }
        out.add("childMiningRequestIds",new Gson().toJsonTree(childMining));out.addProperty("receiptCount",receipts.size());var recent=new JsonArray();for(int i=Math.max(0,receipts.size()-16);i<receipts.size();i++)recent.add(receipts.get(i).deepCopy());out.add("receipts",recent);out.add("decisions",decisions.deepCopy());if(decisionId!=null)out.addProperty("decisionId",decisionId);
        var options=new JsonArray();if(phase.equals("PLAN_READY")){
            var option=new JsonObject();option.addProperty("optionId","bounded-placement");option.addProperty("targets",cells.size());option.addProperty("allowMovement",allowMove);option.addProperty("movementBudget",maxDistance);option.addProperty("cleanupTemporary",cleanup);
            option.addProperty("routeCoverage","Exact local routes are evaluated at each work position; no unsearched route or total execution time is claimed");option.add("estimatedSeconds",JsonNull.INSTANCE);
            var materials=new JsonArray();for(var e:materialCounts().entrySet()){
                var c=cells.stream().filter(t->t.entry.equals(e.getKey())).findFirst().orElseThrow();var row=new JsonObject();row.addProperty("entryId",e.getKey());row.addProperty("item",BuiltInRegistries.ITEM.getKey(c.item.getItem()).toString());row.addProperty("count",e.getValue());row.addProperty("available",count(e.getKey()));materials.add(row);
            }option.add("materials",materials);var targets=new JsonArray();for(var c:cells){var t=PlacementTools.xyz(c.pos);t.addProperty("item",BuiltInRegistries.ITEM.getKey(c.item.getItem()).toString());t.addProperty("temporary",c.temporary);t.add("state",new Gson().toJsonTree(c.state));targets.add(t);}option.add("targetCells",targets);var air=new JsonArray();for(var q:reservedAir)air.add(PlacementTools.xyz(q));option.add("reservedAir",air);options.add(option);
        }out.add("options",options);return out;
    }
    @Override public void close(){cancelForChat();planner.close();}
}
