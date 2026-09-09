package dev.mcai.companion.codex;

import java.util.*;
import com.google.gson.*;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.placement.PlacementTools;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTest;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.Vec3;

/** Source-informed real Forge physics/inventory gate, not an independent model acceptance claim. */
@net.minecraftforge.gametest.GameTestNamespace("mcai_companion") @net.minecraftforge.gametest.GameTestDontPrefix
public final class PlacementGameTests {
    @GameTest(name="placement_tool_regressions",structure="forge:empty48x32x48",maxTicks=3200,padding=8)
    public static void run(GameTestHelper h){if(!Boolean.getBoolean("minepilot.placementTest")){h.fail("Placement gate not selected");return;}new Gate(h).start();}
    private static final class Gate {
        final GameTestHelper h;final AgentRuntime r;final CodexToolService tools;final BlockPos origin;final JsonArray evidence=new JsonArray();
        int stage;long since;String request;BlockPos target;int materialBefore;boolean paused;double previousY;
        Gate(GameTestHelper h){this.h=h;r=AgentRuntime.active(h.getLevel().getServer());tools=new CodexToolService(r);origin=h.absolutePos(new BlockPos(18,3,18));}
        JsonObject obj(String s){return JsonParser.parseString(s).getAsJsonObject();}
        JsonObject call(String name,JsonObject a,boolean success){var params=new JsonObject();params.addProperty("name",name);params.add("arguments",a);var req=new JsonObject();req.add("params",params);var reply=tools.dispatch("tools/call",req);h.assertTrue(reply.get("isError").getAsBoolean()!=success,"Unexpected "+name+": "+reply);return success?reply.getAsJsonObject("structuredContent"):reply;}
        JsonObject id(){var a=new JsonObject();a.addProperty("request_id",request);return a;}
        JsonObject spec(BlockPos p,String item,String state){var a=PlacementTools.xyz(p);a.addProperty("item","minecraft:"+item);if(state!=null)a.add("state",obj(state));return a;}
        void stage(int value){stage=value;since=h.getTick();paused=false;}
        void fixture(){r.placement().cancelForChat();r.mining().cancelForChat();h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(origin).inflate(14)).forEach(net.minecraft.world.entity.Entity::discard);for(int x=-9;x<=10;x++)for(int z=-7;z<=7;z++){h.getLevel().setBlock(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);for(int y=0;y<=5;y++)h.getLevel().setBlock(origin.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);}var p=r.player();p.setPos(Vec3.atBottomCenterOf(origin));p.setDeltaMovement(Vec3.ZERO);p.setOnGround(true);p.stopControlling();p.getInventory().clearContent();p.getInventory().setSelectedSlot(0);p.level().getChunkSource().move(p);target=origin.east(2);}
        void give(Item item,int count){r.player().getInventory().setItem(20,new ItemStack(item,count));r.player().inventoryLedger.tick();}
        void one(String item,String state){var result=call("place_block",spec(target,item,state),true);request=result.get("requestId").getAsString();}
        int count(Item item){int n=0;for(int i=0;i<r.player().getInventory().getContainerSize();i++){var s=r.player().getInventory().getItem(i);if(s.is(item))n+=s.getCount();}return n;}
        void record(String name){var row=call("placement_status",new JsonObject(),true);row.addProperty("case",name);row.addProperty("elapsedTicks",h.getTick()-since);row.add("hands",dev.mcai.companion.agent.placement.HandController.describe(r.player()));var a=new JsonObject();var cells=new JsonArray();cells.add(PlacementTools.xyz(target));a.add("targets",cells);row.add("physicalBlocks",call("inspect_placement",a,true));evidence.add(row);}
        void plan(List<JsonObject> cells,boolean move,boolean cleanup){var a=new JsonObject();a.add("targets",new Gson().toJsonTree(cells));a.addProperty("allow_movement",move);a.addProperty("cleanup_temporary",cleanup);var result=call("plan_placement",a,true);request=result.get("requestId").getAsString();var choose=id();choose.addProperty("option_id","bounded-placement");call("choose_placement",choose,true);}
        void resolve(JsonObject status,String option,boolean success){var a=id();a.addProperty("option_id",option);a.addProperty("decision_id",status.get("decisionId").getAsString());call("resolve_placement",a,success);}
        void start(){
            fixture();var p=r.player();p.setGameMode(GameType.SURVIVAL);
            give(Items.COBBLESTONE,40);p.getInventory().setItem(0,new ItemStack(Items.WOODEN_PICKAXE));
            call("set_hand",obj("{\"slot\":20,\"hand\":\"main\"}"),true);h.assertTrue(p.getMainHandItem().is(Items.COBBLESTONE) && p.getInventory().getItem(20).is(Items.WOODEN_PICKAXE),"Storage swap lost items");
            call("set_hand",obj("{\"slot\":20,\"hand\":\"offhand\"}"),true);h.assertTrue(p.getOffhandItem().is(Items.WOODEN_PICKAXE) && p.getInventory().getItem(20).isEmpty(),"Offhand swap failed");
            call("set_hand",obj("{\"item\":\"minecraft:air\",\"hand\":\"main\"}"),true);h.assertTrue(p.getMainHandItem().isEmpty() && count(Items.COBBLESTONE)==40,"Air selection lost items");
            for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STICK,64));p.getInventory().setItem(5,new ItemStack(Items.COBBLESTONE,40));p.getInventory().setItem(6,ItemStack.EMPTY);p.getInventory().setItem(7,ItemStack.EMPTY);
            var cap=call("inventory_capacity",obj("{\"item\":\"minecraft:cobblestone\"}"),true);h.assertTrue(cap.get("additionalItemCapacity").getAsInt()==152 && cap.get("emptyStorageSlots").getAsInt()==2,"Storage capacity incorrect: "+cap);
            p.getInventory().setItem(6,new ItemStack(Items.STICK,64));p.getInventory().setItem(7,new ItemStack(Items.STICK,64));call("set_hand",obj("{\"item\":\"minecraft:air\"}"),false);h.assertTrue(count(Items.COBBLESTONE)==40,"Full bag empty-hand request deleted blocks");
            var row=new JsonObject();row.addProperty("case","native storage/offhand swaps, Air conservation, full-bag rejection and capacity");row.add("capacity",cap);evidence.add(row);
            fixture();give(Items.COBBLESTONE,4);call("place_block",spec(target,"diamond_block",null),false);call("place_block",spec(origin.east(6),"cobblestone",null),false);one("cobblestone",null);stage(1);h.onEachTick(this::tick);
        }
        void tick(){
            var p=r.player();var status=call("placement_status",new JsonObject(),true);String phase=status.get("phase").getAsString();long age=h.getTick()-since;
            if(stage==1 && phase.equals("COMPLETED")){
                h.assertTrue(h.getLevel().getBlockState(target).is(Blocks.COBBLESTONE) && count(Items.COBBLESTONE)==3,"Single placement state/debit wrong");record("single real placement from storage slot; missing item/reach denied");
                fixture();give(Items.OAK_LOG,4);h.getLevel().setBlock(target.east(),Blocks.STONE.defaultBlockState(),2);one("oak_log","{\"axis\":\"x\"}");stage(2);
            }else if(stage==2 && phase.equals("COMPLETED")){
                h.assertTrue(h.getLevel().getBlockState(target).getValue(BlockStateProperties.AXIS)==Direction.Axis.X && count(Items.OAK_LOG)==3,"Log axis wrong");record("native horizontal log orientation");
                fixture();give(Items.OAK_SLAB,4);h.getLevel().setBlock(target.east(),Blocks.STONE.defaultBlockState(),2);one("oak_slab","{\"type\":\"top\"}");stage(3);
            }else if(stage==3 && phase.equals("COMPLETED")){
                h.assertTrue(h.getLevel().getBlockState(target).getValue(BlockStateProperties.SLAB_TYPE)==SlabType.TOP,"Slab height wrong");record("top slab uses real side hit");fixture();give(Items.OAK_DOOR,2);one("oak_door","{\"facing\":\"east\"}");stage(4);
            }else if(stage==4 && phase.equals("COMPLETED")){
                h.assertTrue(h.getLevel().getBlockState(target.above()).getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)==DoubleBlockHalf.UPPER && count(Items.OAK_DOOR)==1,"Door companion/debit wrong");record("door two cells one item");fixture();give(Items.BED.red(),1);one("red_bed","{\"facing\":\"east\"}");stage(5);
            }else if(stage==5 && phase.equals("COMPLETED")){
                h.assertTrue(h.getLevel().getBlockState(target.east()).getValue(BlockStateProperties.BED_PART)==BedPart.HEAD && count(Items.BED.red())==0,"Bed companion/debit wrong");record("bed two cells one item");fixture();give(Items.COBBLESTONE,3);target=origin;var a=spec(target,"cobblestone",null);a.addProperty("jump",true);request=call("place_block",a,true).get("requestId").getAsString();previousY=p.getY();stage(6);
            }else if(stage==6){
                h.assertTrue(Math.abs(p.getY()-previousY)<.7,"Underfoot jump exceeded native continuity");previousY=p.getY();
                if(phase.equals("COMPLETED")){h.assertTrue(p.onGround() && p.getY()>=origin.getY()+.99 && h.getLevel().getBlockState(target).is(Blocks.COBBLESTONE) && count(Items.COBBLESTONE)==2,"Jump support/landing not physical");record("native underfoot jump, placement, continuous displacement and landing");fixture();give(Items.GLASS,8);plan(List.of(spec(target,"glass",null),spec(target.south(),"glass",null),spec(target.south(2),"glass",null)),false,false);stage(7);}
                else if(phase.equals("BLOCKED"))h.fail("Jump blocked: "+status);
            }else if(stage==7 && status.get("satisfiedTargets").getAsInt()>=1 && phase.equals("EXECUTING")){
                call("pause_placement",id(),true);materialBefore=count(Items.GLASS);call("say",obj("{\"message\":\"放置已暂停，我还可以和你聊天。\"}"),true);call("jump_once",new JsonObject(),false);call("set_hand",obj("{\"item\":\"minecraft:air\"}"),true);stage(8);
            }else if(stage==8 && age>=30){h.assertTrue(count(Items.GLASS)==materialBefore,"Paused placement consumed materials");call("resume_placement",id(),true);stage(9);
            }else if(stage==9 && phase.equals("COMPLETED")){
                h.assertTrue(count(Items.GLASS)==5,"Batch debit wrong after resume");record("one batch approval; pause, chat, empty-hand change, resume");fixture();give(Items.COBBLESTONE,3);h.getLevel().setBlock(target,Blocks.STONE.defaultBlockState(),2);p.getInventory().setItem(21,new ItemStack(Items.WOODEN_PICKAXE));one("cobblestone",null);stage(10);
            }else if(stage==10 && phase.equals("BLOCKED")){
                h.assertTrue(status.getAsJsonArray("decisions").asList().stream().anyMatch(v->v.getAsJsonObject().get("optionId").getAsString().equals("mine_obstacle")),"No evaluated obstacle break: "+status);record("obstruction returns actual held-tool cost and choices");var stale=id();stale.addProperty("decision_id","obsolete");stale.addProperty("option_id","mine_obstacle");call("resolve_placement",stale,false);resolve(status,"mine_obstacle",true);stage(11);
            }else if(stage==11 && phase.equals("COMPLETED")){
                h.assertTrue(count(Items.COBBLESTONE)==2 && h.getLevel().getBlockState(target).is(Blocks.COBBLESTONE),"Approved obstacle break/replace failed");h.assertTrue(java.util.stream.IntStream.range(0,36).anyMatch(slot->p.getInventory().getItem(slot).is(Items.WOODEN_PICKAXE) && p.getInventory().getItem(slot).getDamageValue()==1),"Auto-selected pickaxe was not preserved with actual wear");record("model-approved normal excavation with automatic storage-tool selection and real wear, then placement");fixture();give(Items.COBBLESTONE,8);target=origin.east(7);plan(List.of(spec(target,"cobblestone",null)),true,false);stage(12);
            }else if(stage==12 && phase.equals("COMPLETED")){
                h.assertTrue(status.get("physicalDistance").getAsDouble()>2 && count(Items.COBBLESTONE)==7,"Approach displacement or debit incorrect: "+status+"; inventory="+count(Items.COBBLESTONE));record("bounded autonomous walking to legal working reach");fixture();give(Items.COBBLESTONE,3);one("cobblestone",null);r.onChat("TestHuman","停下");stage(13);
            }else if(stage==13 && age>=40){
                h.assertTrue(phase.equals("CANCELLED") && h.getLevel().getBlockState(target).isAir() && count(Items.COBBLESTONE)==3,"Chat cancellation had delayed placement");record("chat cancellation prevents delayed placement");fixture();give(Items.COBBLESTONE,3);h.getLevel().setBlock(target,Blocks.BEDROCK.defaultBlockState(),2);one("cobblestone",null);stage(14);
            }else if(stage==14 && phase.equals("BLOCKED")){resolve(status,"skip",true);stage(15);
            }else if(stage==15 && phase.equals("PARTIAL")){
                h.assertTrue(count(Items.COBBLESTONE)==3 && h.getLevel().getBlockState(target).is(Blocks.BEDROCK),"Skip destroyed obstacle");record("unbreakable obstacle skipped yields PARTIAL");
                fixture();give(Items.TORCH,3);h.getLevel().setBlock(target.east(),Blocks.STONE.defaultBlockState(),2);one("torch","{\"facing\":\"west\"}");stage(16);
            }else if(stage==16 && phase.equals("COMPLETED")){
                h.assertTrue(h.getLevel().getBlockState(target).is(Blocks.WALL_TORCH) && count(Items.TORCH)==2,"Wall torch native variant failed");record("native wall torch variant and support face");
                fixture();give(Items.DIRT,3);var a=spec(target,"dirt",null);a.addProperty("temporary",true);
                var planArgs=new JsonObject();planArgs.add("targets",new Gson().toJsonTree(List.of(a)));String entry=p.inventoryLedger.key(p.getInventory().getItem(20));p.inventoryLedger.annotate(entry,2,"protected test material");call("plan_placement",planArgs,false);
                p.inventoryLedger.annotate(entry,4,"temporary support authorized for test");plan(List.of(a),false,true);stage(17);
            }else if(stage==17 && phase.equals("COMPLETED")){
                h.assertTrue(h.getLevel().getBlockState(target).isAir() && status.get("satisfiedTargets").getAsInt()==1 && status.get("receiptCount").getAsInt()==2,"Temporary support cleanup not physical");record("protected support rejected; expendable support normally placed and mined for cleanup");
                fixture();give(Items.COBBLESTONE,4);target=origin.east(4);for(int z=-3;z<=3;z++)for(int y=0;y<4;y++)h.getLevel().setBlock(origin.offset(1,y,z),Blocks.STONE.defaultBlockState(),2);one("cobblestone",null);stage(18);
            }else if(stage==18 && phase.equals("BLOCKED")){
                h.assertTrue(h.getLevel().getBlockState(target).isAir() && count(Items.COBBLESTONE)==4,"Placement clicked through occluding wall");record("proximity knowledge does not bypass physical wall occlusion");
                fixture();give(Items.COBBLESTONE,8);target=origin.east(2);var b=obj("{\"palette\":{\"C\":{\"item\":\"minecraft:cobblestone\"}},\"layers\":[[\"C_C\",\"...\"]]}");b.add("origin",PlacementTools.xyz(target));var a=new JsonObject();a.add("blueprint",b);a.addProperty("allow_movement",true);var preview=call("plan_placement",a,true);request=preview.get("requestId").getAsString();var choose=id();choose.addProperty("option_id","bounded-placement");call("choose_placement",choose,true);stage(19);
            }else if(stage==19 && phase.equals("COMPLETED")){
                h.assertTrue(count(Items.COBBLESTONE)==6 && h.getLevel().getBlockState(target.east()).isAir() && h.getLevel().getBlockState(target.east(2)).is(Blocks.COBBLESTONE),"Text blueprint axis/material/reserved air wrong");record("fixed-origin text blueprint preserves opening and verifies two placed cells");
                fixture();give(Items.COBBLESTONE,4);var a=spec(target,"cobblestone",null);a.addProperty("hand","offhand");request=call("place_block",a,true).get("requestId").getAsString();stage(20);
            }else if(stage==20 && phase.equals("COMPLETED")){
                h.assertTrue(p.getMainHandItem().isEmpty() && p.getOffhandItem().getCount()==3 && h.getLevel().getBlockState(target).is(Blocks.COBBLESTONE),"Offhand placement consumed wrong hand");record("offhand native placement with empty main hand");
                try{var path=r.server().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("placement-physical-evidence.json");java.nio.file.Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(evidence));}catch(java.io.IOException e){throw new IllegalStateException(e);}stage(21);h.succeed();
            }else if(phase.equals("BLOCKED") && stage!=10 && stage!=14 && stage!=18 && stage!=21){h.fail("Placement blocked at stage "+stage+": "+status);}
            if(stage<21 && age>250)h.fail("Placement stage stalled "+stage+": "+status);
        }
    }
}
