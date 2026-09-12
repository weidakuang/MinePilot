package dev.mcai.companion.agent.mining;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import com.google.gson.*;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.body.MinePilotServerPlayer;
import dev.mcai.companion.agent.navigation.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** One approved, bounded gather job. Reuses the normal breaker and physical route follower. */
public final class CollectionCoordinator implements AutoCloseable {
    private final AgentRuntime runtime;
    private final MinePilotServerPlayer body;
    private final PlanningExecutor planner=new PlanningExecutor(NavigationPlannerConfig.defaults());
    private final NavigationSnapshotBuilder snapshots=new NavigationSnapshotBuilder(new NavigationSnapshotBuilder.CaptureConfig(4,4,6,48,24,50_000,8));
    private final NavigationFollower follower;
    private final LinkedHashMap<String,Option> options=new LinkedHashMap<>();
    private final Set<BlockPos> done=new HashSet<>(), inaccessible=new HashSet<>();
    private final Set<String> mineRequests=new HashSet<>(),approvedDropIds=new HashSet<>();
    private final Map<String,Integer> minedSupply=new HashMap<>(),creditedSupply=new HashMap<>();
    private boolean allowGrove,wholeTree;
    private long planNanos;
    private final List<Vec3> approaches=new ArrayList<>();
    private final JsonArray breaks=new JsonArray(),receipts=new JsonArray(),rejectedTrees=new JsonArray();
    private UUID request,childRequest;
    private Vec3 center,origin,lastPosition;
    private String dimension,resource,species,outputItem,phase="IDLE",step="",reason="";
    private double radius,distance,maxDistance;
    private int desired,plannedTick,startedTick,waitUntil,received,initialCount,receivedBeforeDrop,workingSlot;
    private long inventoryCursor;
    private boolean internalAction;
    private Option selected;
    private BlockPos target;
    private ItemEntity drop;
    private CompletableFuture<NavigationPlan> pendingRoute;
    private NavigationPlan route;
    private RouteOption chosenRoute;
    private NavigationEvent routeEvent;
    private Vec3 routeStart;
    private String routeFailure="",lastDrop="";
    private final Set<String> deferredDrops=new HashSet<>();
    private int dropRepairs,dropSettleSince;
    private record Option(String id,String source,List<BlockPos> blocks,List<String> drops,JsonObject description,Map<BlockPos,BlockState> states,ItemStack tool,int slot) {}

    public CollectionCoordinator(AgentRuntime runtime){
        this.runtime=runtime;body=runtime.player();
        follower=new NavigationFollower(body,e->routeEvent=e,id->arrivalProblem(),why->followerInvalidated(why));
    }
    private void followerInvalidated(String why){follower.requestReplan(why);}
    public int reservedCount(ItemStack stack){return ownsBody() && selected!=null && !selected.tool.isEmpty() && body.inventoryLedger.key(stack).equals(body.inventoryLedger.key(selected.tool)) ? selected.tool.getCount() : 0;}
    public boolean ownsBody(){return phase.equals("EXECUTING") || phase.equals("PAUSED");}
    public boolean isChildRequest(JsonObject state){return state.has("requestId") && mineRequests.contains(state.get("requestId").getAsString());}
    public boolean internalAction(){return internalAction;}
    private <T>T child(Supplier<T> action){internalAction=true;try{return action.get();}finally{internalAction=false;}}
    private int tick(){return runtime.server().getTickCount();}
    private void thread(){if(!runtime.server().isSameThread())throw new IllegalStateException("Collection requires server thread");}
    private void idle(){
        if(runtime.excavation()!=null && runtime.excavation().ownsBody() && !runtime.excavation().internalAction())throw new IllegalStateException("Pause or cancel excavation before an independent body action");
thread();if(ownsBody() || runtime.placement()!=null && runtime.placement().ownsBody() || runtime.mining().ownsBody() || runtime.jumpActive() || runtime.turnActive() || runtime.navigation().status().phase()!=NavigationToolCoordinator.Phase.IDLE && !runtime.navigation().status().phase().terminal())throw new IllegalStateException("Finish or cancel the current body action first");}

    public JsonObject plan(String resource,String outputItem,String species,String source,Vec3 center,int radius,int count,BlockPos treeSeed,boolean allowGrove){
        return plan(resource,outputItem,species,source,center,radius,count,treeSeed,allowGrove,source.equals("tree"));
    }
    public JsonObject plan(String resource,String outputItem,String species,String source,Vec3 center,int radius,int count,BlockPos treeSeed,boolean allowGrove,boolean wholeTree){
        long scanStart=System.nanoTime();
        idle();if(body.gameMode.getGameModeForPlayer()!=GameType.SURVIVAL)throw new IllegalArgumentException("Collection requires survival mode");
        if(radius<1 || radius>10 || count<1 || count>64 || center.distanceToSqr(body.position())>100)throw new IllegalArgumentException("Radius 1..10, quantity 1..64, center within ten blocks required");
        if(!Set.of("any","tree","drops","blocks").contains(source))throw new IllegalArgumentException("Unknown source policy");
        boolean wood=resource.equals("wood");
        if(!wood && outputItem.isBlank())outputItem=switch(resource) {
            case "minecraft:stone" -> "minecraft:cobblestone";
            case "minecraft:deepslate" -> "minecraft:cobbled_deepslate";
            case "minecraft:coal_ore","minecraft:deepslate_coal_ore" -> "minecraft:coal";
            case "minecraft:crafting_table","minecraft:furnace","minecraft:chest","minecraft:barrel","minecraft:smoker","minecraft:blast_furnace" -> resource;
            default -> "";
        };
        if(wholeTree && (!wood || source.equals("drops")))throw new IllegalArgumentException("whole_tree requires wood from a tree");
        this.wholeTree=wholeTree;
        if(!wood && (source.equals("tree") || treeSeed!=null || outputItem.isBlank()))throw new IllegalArgumentException("Non-wood targets require an explicit output_item and blocks/drops/any source");
        if(wood && source.equals("blocks"))throw new IllegalArgumentException("Wood from arbitrary constructed blocks is not an approved source; select trees or drops");
        if(!wood && BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse(resource)).defaultBlockState().is(net.minecraft.tags.BlockTags.LOGS))throw new IllegalArgumentException("Use resource wood and species/source filters for logs; arbitrary log excavation cannot bypass tree checks");
        if(!wood && (!BuiltInRegistries.BLOCK.containsKey(net.minecraft.resources.Identifier.parse(resource)) || !BuiltInRegistries.ITEM.containsKey(net.minecraft.resources.Identifier.parse(outputItem))))throw new IllegalArgumentException("Unknown resource or output item");
        if(resource.equals("minecraft:air") || resource.equals("minecraft:cave_air") || resource.equals("minecraft:void_air"))throw new IllegalArgumentException("Air is omitted from collection");
        this.allowGrove=allowGrove;this.resource=resource;this.outputItem=outputItem;this.species=species;this.center=center;this.radius=radius;this.desired=count;
        this.origin=body.position();this.dimension=body.level().dimension().identifier().toString();this.request=UUID.randomUUID();plannedTick=tick();
        mineRequests.clear();approvedDropIds.clear();deferredDrops.clear();drop=null;target=null;
        rejectedTrees.asList().clear();phase="PLAN_READY";reason="";step="";options.clear();done.clear();inaccessible.clear();breaks.asList().clear();receipts.asList().clear();received=0;distance=0;selected=null;route=null;chosenRoute=null;
        var foundDrops=new ArrayList<String>();
        if(!source.equals("tree") && !source.equals("blocks") && !wholeTree){
            for(var e:body.level().getEntitiesOfClass(ItemEntity.class,new AABB(center,center).inflate(radius),e->e.isAlive() && !dev.mcai.companion.agent.knowledge.DiscardedItems.avoided(e,body) && inside(e.position()) && runtime.perception.sensed(e) && matchesItem(BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).toString())))foundDrops.add(e.getUUID().toString());
            if(!foundDrops.isEmpty())addOption("nearby-drops","drops",List.of(),foundDrops,new JsonObject());
        }
        if(!source.equals("drops")){
            var seeds=new ArrayList<BlockPos>();BlockPos base=BlockPos.containing(center);
            if(treeSeed!=null){if(!inside(Vec3.atCenterOf(treeSeed)))throw new IllegalArgumentException("Specified tree lies outside the fixed task sphere");seeds.add(treeSeed);}
            else {
                // Numen's palette scan skips whole sections lacking the resource.
                // The adapter keeps our exact task sphere and ordinary perception rules.
                var hits=new ArrayList<dev.mcai.companion.vendor.numen.scan.BlockScanner.Hit>();
                java.util.function.Predicate<BlockState> filter=state->{
                    String id=TreeSurvey.id(state);
                    return wood?(!TreeSurvey.species(id).isEmpty() && (species.equals("any") || TreeSurvey.species(id).equals(species))):id.equals(resource);
                };
                int extent=radius+1;
                for(int cx=(base.getX()-extent)>>4;cx<=(base.getX()+extent)>>4;cx++)
                    for(int cz=(base.getZ()-extent)>>4;cz<=(base.getZ()+extent)>>4;cz++){
                        var chunk=dev.mcai.companion.vendor.numen.scan.BlockScanner.loadedChunk(body.level(),cx,cz);
                        if(chunk==null)continue;
                        for(int sy=(base.getY()-extent)>>4;sy<=(base.getY()+extent)>>4;sy++)
                            dev.mcai.companion.vendor.numen.scan.BlockScanner.scanChunkSection(body.level(),chunk,cx,sy,cz,base,extent,extent*extent,filter,hits);
                    }
                for(var hit:hits)if(inside(Vec3.atCenterOf(hit.pos())) && runtime.perception.observableBlock(hit.pos()))seeds.add(hit.pos());
            }
            seeds.sort(Comparator.comparingInt((BlockPos p)->wood || exposed(p)?0:1).thenComparingDouble(p->p.distToCenterSqr(body.position())));
            if(wood){
                var inspected=new HashSet<BlockPos>();int surveys=0;
                for(var seed:seeds){if(inspected.contains(seed))continue;if(++surveys>8)break;
                    var survey=TreeSurvey.inspect(runtime,seed);inspected.addAll(survey.logs());
                    if(treeSeed!=null && !species.equals("any") && !survey.species().equals(species))throw new IllegalArgumentException("Specified tree does not match requested species");
                    if(!survey.harvestable() || survey.classification().equals("managed_grove_candidate") && !allowGrove){
                        var rejected=survey.json();rejected.add("seed",TreeSurvey.position(seed));if(rejectedTrees.size()<8)rejectedTrees.add(rejected);reason="Observed trunk candidates could not yet be approved; see rejectedTrees for exact reasons. This is not proof of no trees.";continue;
                    }
                    if(wholeTree && (!survey.bounded() || survey.roots().isEmpty() || survey.classification().equals("connected_tree_cluster"))) {reason="Whole-tree extent remains unknown; partial log collection is available.";continue;}
                    if(wholeTree && survey.logs().stream().anyMatch(p->!inside(Vec3.atCenterOf(p)))) {reason="Whole tree extends outside fixed task sphere; replan its center/radius. No tree was felled.";continue;}
                    var blocks=survey.logs().stream().filter(p->inside(Vec3.atCenterOf(p))).sorted(Comparator.comparingInt((BlockPos p)->p.getY()).thenComparingDouble(p->p.distToCenterSqr(body.position()))).limit(wholeTree?128:count).toList();
                    if(!blocks.isEmpty())addOption("tree-"+(options.size()+1),"tree",blocks,List.of(),survey.json());
                    if(options.size()>=4)break;
                }
            }else if(!seeds.isEmpty())addOption("matching-blocks","blocks",seeds.stream().limit(128).toList(),List.of(),new JsonObject());
        }
        if(options.isEmpty()){phase="BLOCKED";if(reason.isEmpty())reason="No eligible source in the observed fixed sphere; change the search or source policy. No blind excavation or fishbone mining was started.";}
        planNanos=System.nanoTime()-scanStart;return status();
    }
    private void addOption(String id,String source,List<BlockPos> blocks,List<String> drops,JsonObject survey){
        var states=new LinkedHashMap<BlockPos,BlockState>();double breakTicks=0;int wear=0;
        int maxBreaks=source.equals("blocks")?Math.min(desired,blocks.size()):blocks.size();
        MiningToolChoice choice;
        try {choice=blocks.isEmpty()?new MiningToolChoice(body.getInventory().getSelectedSlot(),body.getMainHandItem().copy()):MiningToolChoice.best(body,body.level().getBlockState(blocks.getFirst()),true,maxBreaks);}
        catch(IllegalArgumentException unavailable){reason=unavailable.getMessage();return;}
        var tool=choice.stack();var data=tool.get(net.minecraft.core.component.DataComponents.TOOL);
        if(!blocks.isEmpty() && tool.isDamageableItem() && data==null)return;
        for(var p:blocks){var state=body.level().getBlockState(p);float progress=state.getDestroyProgress(body,body.level(),p);
            if(state.getDestroySpeed(body.level(),p)<0 || state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state))return;
            states.put(p,state);if(states.size()<=maxBreaks){breakTicks+=choice.estimatedTicks(body,state,p);if(tool.isDamageableItem() && state.getDestroySpeed(body.level(),p)!=0)wear+=data.damagePerBlock();}
        }
        if(!blocks.isEmpty() && tool.isDamageableItem() && tool.getMaxDamage()-tool.getDamageValue()<=wear)return;
        var out=new JsonObject();out.addProperty("optionId",id);out.addProperty("source",source);
        if(survey.has("logs")){survey=survey.deepCopy();survey.addProperty("observedLogCount",survey.getAsJsonArray("logs").size());survey.remove("logs");}
        out.add("survey",survey);
        out.addProperty("maximumBlocksToBreak",maxBreaks);out.addProperty("targetBlocks",blocks.size());out.add("targetPositions",new Gson().toJsonTree(blocks.stream().map(TreeSurvey::position).toList()));out.add("dropEntityIds",new Gson().toJsonTree(drops));
        out.add("tool",choice.json(body));
        out.addProperty("wholeTree",wholeTree && source.equals("tree"));out.addProperty("nominalDurabilityCostMax",wear);
        if(tool.isDamageableItem())out.addProperty("remainingDurabilityAtNominalCost",tool.getMaxDamage()-tool.getDamageValue()-wear);
        out.addProperty("estimatedBreakSeconds",breakTicks/20.0);out.addProperty("timeEstimateScope","Base selected-tool estimate; effects and enchantments can change actual progress. Excludes approach, turning and pickup");
        out.addProperty("pickupTravelMargin",2);out.addProperty("blockSelectionRadiusRemainsFixed",true);
        out.addProperty("movementDistanceBudget",Math.max(64,radius*16));out.add("estimatedMovementDistance",JsonNull.INSTANCE);
        out.addProperty("routePolicy","Local evaluated zero-support routes, no predicted health loss or excavation; route details returned during execution. Stop if blocked or budget exceeded.");
        out.add("supportMaterials",new JsonArray());out.addProperty("accessBlocks",0);out.addProperty("risk","Local hazards rechecked before each block. Tree origin and ownership are not guaranteed by appearance.");
        out.addProperty("requestedItems",desired);out.addProperty("quantityIsGuaranteed",false);
        options.put(id,new Option(id,source,List.copyOf(blocks),List.copyOf(drops),out,states,tool.copy(),choice.slot()));
    }
    public JsonObject choose(UUID id,String option){
        idle();requireRequest(id);if(!phase.equals("PLAN_READY") || !options.containsKey(option))throw new IllegalArgumentException("Choose a current collection option");
        if(tick()-plannedTick>400 || body.position().distanceToSqr(origin)>.01 || !dimension.equals(body.level().dimension().identifier().toString()))throw new IllegalStateException("Plan expired or body moved; plan again");
        selected=options.get(option);
        if(!selected.blocks.isEmpty() && !ItemStack.matches(body.getInventory().getItem(selected.slot),selected.tool))throw new IllegalStateException("Held tool changed; plan again");
        for(var e:selected.states.entrySet())if(!runtime.perception.observableBlock(e.getKey()) || !body.level().getBlockState(e.getKey()).equals(e.getValue()))throw new IllegalStateException("Resource changed since planning");
        dev.mcai.companion.agent.placement.HandController.equipSlot(body,selected.slot,net.minecraft.world.InteractionHand.MAIN_HAND);
        if(wholeTree && selected.source.equals("tree"))desired=selected.blocks.size();
        workingSlot=body.getInventory().getSelectedSlot();phase="EXECUTING";step="SELECT";reason="";startedTick=tick();lastPosition=body.position();initialCount=countInventory();
        inventoryCursor=body.inventoryLedger.inventory().get("latestEventSequence").getAsLong();mineRequests.clear();minedSupply.clear();creditedSupply.clear();approvedDropIds.clear();approvedDropIds.addAll(selected.drops);
        maxDistance=Math.max(64,radius*16);target=null;drop=null;body.stopControlling();return status();
    }
    private void requireRequest(UUID id){if(request==null || !request.equals(id))throw new IllegalArgumentException("Stale collection request ID");}
    public JsonObject interrupt(UUID id,boolean pause){
        thread();requireRequest(id);if(pause && !ownsBody())throw new IllegalStateException("Only an executing or paused job can be paused");
        if(ownsBody() || phase.equals("PLAN_READY")){stopChildren();phase=pause?"PAUSED":"CANCELLED";reason=pause?"Paused by controller":"Cancelled by controller";}
        return status();
    }
    public JsonObject resume(UUID id){thread();requireRequest(id);if(!phase.equals("PAUSED"))throw new IllegalStateException("Collection is not paused");phase="EXECUTING";step="SELECT";inaccessible.clear();lastPosition=body.position();return status();}
    public void cancelForChat(){if(ownsBody() || phase.equals("PLAN_READY"))interrupt(request,false);}
    private void stopChildren(){if(pendingRoute!=null){pendingRoute.cancel(true);pendingRoute=null;}follower.cancel("Collection interrupted");child(()->{runtime.mining().cancelForChat();return null;});body.stopControlling();routeEvent=null;}
    private void block(String why){stopChildren();phase="BLOCKED";reason=why;}
    private boolean inside(Vec3 p){return p.distanceToSqr(center)<=radius*radius+1e-8;}
    private boolean insideTravel(Vec3 p){return p.distanceToSqr(center)<=(radius+2)*(radius+2)+1e-8;}
    private boolean matchesItem(String id){return resource.equals("wood")?!TreeSurvey.species(id).isEmpty() && (species.equals("any") || TreeSurvey.species(id).equals(species)):id.equals(outputItem);}
    private int countInventory(){int n=0;for(int slot=0;slot<body.getInventory().getContainerSize();slot++){var s=body.getInventory().getItem(slot);if(!s.isEmpty() && matchesItem(BuiltInRegistries.ITEM.getKey(s.getItem()).toString()))n+=s.getCount();}return n;}
    private void acquisitions(){
        var mining=runtime.mining().status();
        if(isChildRequest(mining) && mining.get("phase").getAsString().equals("COMPLETED")){
            int supply=0;for(var emitted:mining.getAsJsonArray("emittedDrops")){var row=emitted.getAsJsonObject();if(matchesItem(row.get("item").getAsString()))supply+=row.get("count").getAsInt();}
            minedSupply.putIfAbsent(mining.get("requestId").getAsString(),supply);
        }
        var batch=body.inventoryLedger.events(inventoryCursor,32);if(batch.get("historyLost").getAsBoolean()){block("Acquisition history lost; cannot prove task collection");return;}
        for(var event:batch.getAsJsonArray("events")){var e=event.getAsJsonObject();inventoryCursor=e.get("sequence").getAsLong();
            for(var item:e.getAsJsonArray("acquired")){var row=item.getAsJsonObject();if(!matchesItem(row.get("item").getAsString()))continue;
                var source=row.getAsJsonObject("source");var parts=new JsonArray();
                if(source.has("lineage"))parts=source.getAsJsonArray("lineage");else {var part=new JsonObject();part.addProperty("count",row.get("count").getAsInt());part.add("source",source);parts.add(part);}
                for(var partValue:parts){var part=partValue.getAsJsonObject();var originSource=part.getAsJsonObject("source");
                    boolean credited=selected.source.equals("drops")?row.has("entityId") && approvedDropIds.contains(row.get("entityId").getAsString()):originSource.has("requestId") && mineRequests.contains(originSource.get("requestId").getAsString());
                    if(!credited)continue;int amount=part.get("count").getAsInt();
                    if(!selected.source.equals("drops")){String id=originSource.get("requestId").getAsString();amount=Math.min(amount,Math.max(0,minedSupply.getOrDefault(id,0)-creditedSupply.getOrDefault(id,0)));creditedSupply.merge(id,amount,Integer::sum);}
                    received+=amount;if(amount>0 && receipts.size()<128){var receipt=row.deepCopy();receipt.addProperty("collectionCreditedCount",amount);receipt.add("creditedOrigin",originSource.deepCopy());receipts.add(receipt);}
                }
            }
        }
    }
    private int verifiedCount(){return Math.max(0,Math.min(received,countInventory()-initialCount));}
    public void beforePhysics(){if(phase.equals("EXECUTING"))follower.beforePhysicsTick();}
    public void tickAfterPhysics(){
        if(!phase.equals("EXECUTING"))return;
        try {
            distance+=body.position().distanceTo(lastPosition);lastPosition=body.position();
            if(!body.isAlive() || !dimension.equals(body.level().dimension().identifier().toString()) || body.gameMode.getGameModeForPlayer()!=GameType.SURVIVAL){block("Body died, changed dimension or game mode");return;}
            if(distance>maxDistance || tick()-startedTick>6000){block("Collection distance or time budget exhausted");return;}
            acquisitions();if(!phase.equals("EXECUTING"))return;
            if(verifiedCount()>=desired && (!wholeTree || done.containsAll(selected.blocks))){stopChildren();phase="COMPLETED";reason=wholeTree?"Every approved trunk block was physically broken and all matching log pickups verified":"Matching pickup receipts and current inventory increase meet the requested count; this is NOT proof of felling a whole tree";return;}
            if(step.equals("BREAK")){
                var state=runtime.mining().status();String childPhase=state.get("phase").getAsString();
                if(childPhase.equals("COMPLETED")){done.add(target);inaccessible.clear();deferredDrops.clear();breaks.add(state);for(var row:state.getAsJsonArray("emittedDrops"))approvedDropIds.add(row.getAsJsonObject().get("entityId").getAsString());step="WAIT";waitUntil=tick()+5;}
                else if(childPhase.equals("BLOCKED") || childPhase.equals("CANCELLED")){block("Block operation stopped: "+state.get("reason").getAsString());return;}else return;
            }
            if(step.equals("SETTLING_DROP")){if(tick()<waitUntil)return;step="SELECT";selectNext();return;}
            if(step.equals("PLAN_ROUTE")){resolveRoute();return;}
            if(step.equals("TRAVEL")){
                if(drop!=null && !drop.isAlive()){
                    if(received>receivedBeforeDrop){follower.cancel("Drop acquired");routeEvent=null;drop=null;step="SELECT";}
                    else block("DROPPED_ITEM_UNAVAILABLE: selected entity disappeared without a matching pickup receipt");return;}
                follower.tick();
                if(routeEvent!=null){var e=routeEvent;routeEvent=null;if(e.type()==NavigationEvent.Type.NAVIGATION_COMPLETED){step="WAIT";waitUntil=tick()+12;}
                    else if(Set.of(NavigationEvent.Type.NAVIGATION_FAILED,NavigationEvent.Type.NAVIGATION_DECISION_REQUIRED,NavigationEvent.Type.NAVIGATION_CANCELLED).contains(e.type())){
                        if(drop!=null && drop.isAlive() && dropRepairs++<3) {
                            // Drops keep moving after spawn. Refresh their working
                            // stances inside this job instead of returning to the model.
                            drop=null;step="SELECT";
                        }
                        else if(drop==null && target!=null){inaccessible.add(target);target=null;step="SELECT";}
                        else block("Collection route stopped: "+e.message());
                    }}
                return;
            }
            if(step.equals("WAIT")){if(tick()<waitUntil)return;if(drop!=null){if(drop.isAlive()){
                if(!body.getBoundingBox().inflate(1,.5,1).intersects(drop.getBoundingBox()) && dropRepairs++<2){drop=null;step="SELECT";selectNext();return;}
                block("Reached drop vicinity but it was not picked up; inventory may be full or pickup restricted");return;
            }drop=null;}step="SELECT";}
            if(step.equals("SELECT"))selectNext();
        }catch(RuntimeException failure){block("Collection cannot continue: "+failure.getMessage());}
    }
    private void selectNext(){
        // Mine from the current working position before pathing. Native pickups
        // continue during the break; an inaccessible drop must not monopolize the job.
        if(!selected.source.equals("drops") && done.size()<(wholeTree?selected.blocks.size():Math.min(desired,selected.blocks.size()))) {
            for(var candidate:selected.blocks)if(!done.contains(candidate) && !inaccessible.contains(candidate)
                    && body.level().getBlockState(candidate).equals(selected.states.get(candidate))
                    && runtime.mining().problem(candidate,true)==null && !body.isInWater()
                    && !new AABB(candidate).intersects(body.getBoundingBox().move(0,-.05,0))
                    && runtime.mining().reachable(candidate)) {drop=null;startBreak(candidate);return;}
        }
        drop=null;
        for(String id:approvedDropIds){if(deferredDrops.contains(id))continue;var e=body.level().getEntity(UUID.fromString(id));if(e instanceof ItemEntity item && item.isAlive() && !dev.mcai.companion.agent.knowledge.DiscardedItems.avoided(item,body) && insideTravel(item.position()) && runtime.perception.sensed(item) && matchesItem(BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).toString()) && (drop==null || item.distanceToSqr(body)<drop.distanceToSqr(body)))drop=item;}
        if(drop!=null){
            target=null;receivedBeforeDrop=received;approaches.clear();
            if(!lastDrop.equals(drop.getUUID().toString())){lastDrop=drop.getUUID().toString();dropRepairs=0;dropSettleSince=tick();}
            var base=drop.blockPosition();
            for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){
                var feet=standingFeet(base.offset(x,y,z));if(feet==null)continue;
                if(!body.getBoundingBox().move(feet.subtract(body.position())).inflate(.7,.3,.7).intersects(drop.getBoundingBox()) || !insideTravel(feet))continue;
                approaches.add(feet);
            }
            approaches.sort(Comparator.comparingDouble(p->p.distanceToSqr(drop.position())+.05*p.distanceToSqr(body.position())));
            if(approaches.isEmpty() && !drop.onGround() && tick()-dropSettleSince<80) {
                // High trunk drops need time to fall. Their current air cell is
                // not a standing destination; observe again instead of failing the tree.
                step="SETTLING_DROP";waitUntil=tick()+5;return;
            }
            beginRoute();return;
        }
        if(selected.source.equals("drops")){block("DROPPED_ITEM_UNAVAILABLE: planned drops disappeared or moved outside the fixed sphere; quantity not acquired");return;}
        if(selected.source.equals("blocks") && done.size()>=Math.min(desired,selected.blocks.size())){
            block("Approved break budget exhausted; verified pickups did not meet the requested count");return;
        }
        var remaining=selected.blocks.stream().filter(p->!done.contains(p) && !inaccessible.contains(p))
                .sorted(Comparator.comparingInt((BlockPos p)->exposed(p)?0:1).thenComparingInt(p->p.getY()<body.getBlockY()?1:0).thenComparingDouble(p->p.distToCenterSqr(body.position()))).toList();
        if(remaining.isEmpty()){block("No reachable approved candidates remain; collected "+verifiedCount()+" of "+desired+". Inaccessible candidates were tried without blind excavation");return;}
        for(var p:remaining){
            if(!body.level().getBlockState(p).equals(selected.states.get(p))){
                if(selected.source.equals("tree")){block("An unmined tree target changed; replan its identity");return;}
                inaccessible.add(p);continue;
            }
            if(runtime.mining().problem(p,true)!=null){inaccessible.add(p);continue;}
            if(!body.isInWater() && !new AABB(p).intersects(body.getBoundingBox().move(0,-.05,0)) && runtime.mining().reachable(p)){startBreak(p);return;}
        }
        // Test alternatives inside one native job, bounded per tick. A buried
        // nearest block must not hide a farther exposed block in the same sphere.
        int inspected=0;
        for(var candidate:remaining){
            if(inaccessible.contains(candidate))continue;
            if(runtime.mining().problem(candidate,true)!=null){inaccessible.add(candidate);continue;}
            if(inspected++>=8)return;
            target=candidate;approaches.clear();
            for(int x=-2;x<=2;x++)for(int y=-5;y<=1;y++)for(int z=-2;z<=2;z++){
                var stand=target.offset(x,y,z);if(stand.equals(target) || !inside(Vec3.atCenterOf(stand)) || !runtime.perception.observableBlock(stand))continue;
                var feet=standingFeet(stand);if(feet==null)continue;var box=body.getBoundingBox().move(feet.subtract(body.position()));
                if(new AABB(target).intersects(box.move(0,-.05,0)))continue;
                if(runtime.mining().reachableFrom(target,feet))approaches.add(feet);
            }
            approaches.sort(Comparator.comparingDouble(p->p.distanceToSqr(body.position())));
            if(approaches.size()>8)approaches.subList(8,approaches.size()).clear();
            if(!approaches.isEmpty()){beginRoute();return;}
            inaccessible.add(target);target=null;
        }
    }
    private boolean exposed(BlockPos p){
        for(var face:net.minecraft.core.Direction.values()){
            var q=p.relative(face);if(body.level().isLoaded(q) && body.level().getBlockState(q).getCollisionShape(body.level(),q).isEmpty())return true;
        }
        return false;
    }
    private void startBreak(BlockPos p){
        if(selected.source.equals("tree")){
            var farms=body.inventoryLedger.treeFarms();if(!farms.get("memoryWritable").getAsBoolean()){block("Farm memory is unavailable; preserve trees until recovered");return;}
            for(var entry:farms.getAsJsonObject("farms").entrySet()){
                var farm=entry.getValue().getAsJsonObject();
                if(farm.get("dimension").getAsString().equals(dimension) && TreeSurvey.contains(farm,p) && (farm.get("kind").getAsString().equals("automated") || !allowGrove)){block("Farm declaration changed; this target is now protected");return;}
            }
            // Canopy may decay after earlier breaks. Preserve the approved tree identity, but reject new fixtures.
            for(int x=-2;x<=2;x++)for(int y=-2;y<=2;y++)for(int z=-2;z<=2;z++){
                var q=p.offset(x,y,z);if(!runtime.perception.observableBlock(q))continue;var state=body.level().getBlockState(q);
                if(TreeSurvey.isMachine(state) || body.level().getBlockEntity(q)!=null){block("Tree work area now contains a machine/container/living fixture");return;}
            }
        }
        if(selected.tool.isEmpty() && body.getInventory().getSelectedSlot()==workingSlot && !body.getMainHandItem().isEmpty()){
            // Picking up a log can populate the previously empty selected slot.
            // Preserve the approved bare-hands method through a normal hotbar selection.
            int empty=-1;for(int slot=0;slot<9;slot++)if(body.getInventory().getItem(slot).isEmpty()){empty=slot;break;}
            if(empty<0){block("Bare-hands collection needs a free hotbar slot; organize inventory before resuming");return;}
            final int slot=empty;child(()->runtime.mining().equip(slot));workingSlot=slot;
        }
        var held=body.getMainHandItem();var expected=selected.tool.copy();
        if(expected.isDamageableItem())expected.setDamageValue(held.getDamageValue());
        if(body.getInventory().getSelectedSlot()!=workingSlot || !ItemStack.matches(held,expected)){block("Held tool changed during collection; a new tool plan is required");return;}
        target=p;var plan=child(()->runtime.mining().plan(p,true));childRequest=UUID.fromString(plan.get("requestId").getAsString());mineRequests.add(childRequest.toString());
        child(()->runtime.mining().choose(childRequest,"held-tool"));step="BREAK";
    }
    private void beginRoute(){
        if(approaches.isEmpty()){
            if(drop==null && target!=null){inaccessible.add(target);target=null;step="SELECT";return;}
            if (drop != null && clearPickupHeadroom()) return;
            if(drop!=null) {deferredDrops.add(drop.getUUID().toString());drop=null;step="SELECT";return;}
            block("No evaluated safe approach route; a new decision is required. "+routeFailure);return;
        }
        var destinations=approaches.stream().limit(128).map(dest->new NavigationPlan.ResolvedDestination(dimension,dest.x,dest.y,dest.z,.5,false,"collection:"+request,OptionalDouble.empty(),Optional.empty())).toList();
        approaches.clear();
        routeStart=body.position();pendingRoute=planner.submitAny(UUID.randomUUID(),snapshots.capture(body,destinations.getFirst(),0),destinations);step="PLAN_ROUTE";routeEvent=null;
    }
    private boolean clearPickupHeadroom(){
        // A drop in the one-block notch just mined may need its matching resource
        // above removed before a player can enter. This remains inside the exact
        // resource sphere and quantity budget; no unrelated access block is dug.
        if(!selected.source.equals("blocks") || done.size()>=desired)return false;
        var pos=drop.blockPosition().above();var state=body.level().getBlockState(pos);
        if(!inside(Vec3.atCenterOf(pos)) || !TreeSurvey.id(state).equals(resource) || body.level().getBlockEntity(pos)!=null || !state.getFluidState().isEmpty() || !runtime.mining().reachable(pos))return false;
        if(!selected.states.containsKey(pos)){
            var blocks=new ArrayList<>(selected.blocks);blocks.add(pos);var states=new LinkedHashMap<>(selected.states);states.put(pos,state);
            selected=new Option(selected.id,selected.source,List.copyOf(blocks),selected.drops,selected.description,states,selected.tool,selected.slot);
        }
        startBreak(pos);return true;
    }
    private void resolveRoute(){
        if(!pendingRoute.isDone())return;
        try{route=pendingRoute.join();}catch(RuntimeException noRoute){routeFailure=String.valueOf(noRoute.getCause()==null?noRoute.getMessage():noRoute.getCause().getMessage());pendingRoute=null;beginRoute();return;}pendingRoute=null;
        if(body.position().distanceToSqr(routeStart)>.04){block("Body moved while approach was planned");return;}
        chosenRoute=route.options().stream().filter(o->o.feasibleNow() && o.supportBlocksRequired()==0 && o.estimatedHealthLost()==0 && o.distanceBlocks()+distance<=maxDistance && o.steps().stream().allMatch(p->insideTravel(new Vec3(p.x(),p.y(),p.z())))).min(Comparator.comparingDouble(RouteOption::distanceBlocks)).orElse(null);
        if(chosenRoute==null){beginRoute();return;}
        follower.start(route,chosenRoute,TravelPace.AUTO,OptionalDouble.empty());step="TRAVEL";
    }
    private String arrivalProblem(){
        if(drop!=null)return drop.isAlive() && body.getBoundingBox().inflate(.9,.4,.9).intersects(drop.getBoundingBox())?null:"The moving destination left the planned pickup area";
        if(target!=null && new AABB(target).intersects(body.getBoundingBox().move(0,-.05,0)))return "The body still overlaps the work block; this approach did not provide a mining position";
        return target!=null && runtime.mining().reachable(target)?null:"Arrived position does not reach target block";
    }
    /** Match native path/slab/stair collision height rather than requiring full cubes. */
    public Vec3 standingFeet(BlockPos cell){
        if(!body.level().isLoaded(cell) || !body.level().getWorldBorder().isWithinBounds(cell)
                || !body.level().getFluidState(cell).isEmpty())return null;
        double top=Double.NaN;
        for(int below=0;below<=2;below++){
            var floor=cell.below(below);if(!body.level().isLoaded(floor))continue;
            for(var box:body.level().getBlockState(floor).getCollisionShape(body.level(),floor).toAabbs()){
                double y=floor.getY()+box.maxY;
                if(y>=cell.getY()-1e-7 && y<cell.getY()+1-1e-7 && box.maxX>.2 && box.minX<.8 && box.maxZ>.2 && box.minZ<.8)
                    top=Double.isNaN(top)?y:Math.max(top,y);
            }
        }
        if(Double.isNaN(top))return null;
        var feet=new Vec3(cell.getX()+.5,top,cell.getZ()+.5);
        return body.level().noCollision(body,body.getBoundingBox().move(feet.subtract(body.position())))?feet:null;
    }
    public JsonObject status(){
        var out=new JsonObject();out.addProperty("phase",phase);out.addProperty("step",step);out.addProperty("planServerMillis",planNanos/1_000_000.0);out.addProperty("reason",reason);out.addProperty("ownsBody",ownsBody());
        if(request!=null){out.addProperty("requestId",request.toString());out.addProperty("dimension",dimension);out.add("fixedCenter",new Gson().toJsonTree(Map.of("x",center.x,"y",center.y,"z",center.z)));out.addProperty("radius",radius);out.addProperty("resource",resource);out.addProperty("requestedCount",desired);out.addProperty("verifiedInventoryIncrease",selected==null?0:verifiedCount());}
        var opts=new JsonArray();if(phase.equals("PLAN_READY"))options.values().forEach(o->opts.add(o.description.deepCopy()));out.add("options",opts);
        if(selected!=null){out.addProperty("selectedOptionId",selected.id);out.addProperty("selectedSource",selected.source);out.add("activeTool",runtime.mining().heldTool());}
        out.addProperty("inaccessibleCandidateCount",inaccessible.size());out.addProperty("physicalDistance",distance);out.addProperty("brokenBlocks",done.size());out.add("pickupReceipts",receipts.deepCopy());
        out.add("childMiningRequestIds",new Gson().toJsonTree(mineRequests));
        out.addProperty("wholeTree",wholeTree);out.addProperty("wholeTreeVerified",wholeTree && phase.equals("COMPLETED") && selected!=null && selected.source.equals("tree") && done.containsAll(selected.blocks));
        if(selected!=null)out.addProperty("remainingApprovedBlocks",selected.blocks.stream().filter(p->!done.contains(p)).count());
        out.add("rejectedTrees",rejectedTrees.deepCopy());out.addProperty("sourceAttribution","Origin-event evidence only; complete merged-stack lineage is not established");
        out.addProperty("bodyX",body.getX());out.addProperty("bodyY",body.getY());out.addProperty("bodyZ",body.getZ());
        if(chosenRoute!=null){var summary=new JsonObject();summary.addProperty("optionId",chosenRoute.optionId());summary.addProperty("distanceBlocks",chosenRoute.distanceBlocks());summary.addProperty("estimatedSeconds",chosenRoute.estimatedSeconds());summary.addProperty("estimatedHealthLost",chosenRoute.estimatedHealthLost());summary.add("supportMaterials",new Gson().toJsonTree(chosenRoute.supportMaterials()));out.add("currentRoute",summary);}
        if(target!=null)out.add("currentBlock",TreeSurvey.position(target));
        if(drop!=null){var item=new JsonObject();item.addProperty("entityId",drop.getUUID().toString());item.addProperty("alive",drop.isAlive());item.addProperty("x",drop.getX());item.addProperty("y",drop.getY());item.addProperty("z",drop.getZ());out.add("currentDrop",item);}
        return out;
    }
    @Override public void close(){cancelForChat();planner.close();}
}
