package dev.mcai.companion.codex;

import com.google.gson.*;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.knowledge.InventoryLedger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTest;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/** Source-informed physical regression gate. Fixture setup is not Agent gameplay. */
@net.minecraftforge.gametest.GameTestNamespace("mcai_companion") @net.minecraftforge.gametest.GameTestDontPrefix
public final class CollectionGameTests {
    @GameTest(name="collection_tool_regressions",structure="forge:empty48x32x48",maxTicks=2600,padding=8)
    public static void run(GameTestHelper h){if(!Boolean.getBoolean("minepilot.collectionTest")){h.fail("Collection gate not selected");return;}new Gate(h).start();}
    private static final class Gate {
        final GameTestHelper h;final AgentRuntime r;final CodexToolService tools;final BlockPos origin,tree;final JsonArray evidence=new JsonArray();
        String request;int stage;long since;int before;ItemEntity disappearing;
        Gate(GameTestHelper h){this.h=h;r=AgentRuntime.active(h.getLevel().getServer());tools=new CodexToolService(r);origin=h.absolutePos(new BlockPos(18,3,18));tree=origin.offset(5,0,0);}
        JsonObject obj(String s){return JsonParser.parseString(s).getAsJsonObject();}
        JsonObject call(String name,JsonObject args,boolean success){var params=new JsonObject();params.addProperty("name",name);params.add("arguments",args);var req=new JsonObject();req.add("params",params);var reply=tools.dispatch("tools/call",req);h.assertTrue(reply.get("isError").getAsBoolean()!=success,"Unexpected "+name+": "+reply);return success?reply.getAsJsonObject("structuredContent"):reply;}
        JsonObject xyz(BlockPos p){return dev.mcai.companion.agent.mining.TreeSurvey.position(p);}
        JsonObject id(){return obj("{\"request_id\":\""+request+"\"}");}
        void choose(JsonObject plan,String option){h.assertTrue(plan.get("phase").getAsString().equals("PLAN_READY"),"No collection plan: "+plan);request=plan.get("requestId").getAsString();var a=id();a.addProperty("option_id",option==null?plan.getAsJsonArray("options").get(0).getAsJsonObject().get("optionId").getAsString():option);call("choose_collection",a,true);}
        JsonObject planTree(boolean allow,int count){var a=obj("{\"resource\":\"wood\",\"source\":\"tree\",\"radius\":10}");a.addProperty("count",count);a.addProperty("allow_managed_grove",allow);a.addProperty("tree_x",tree.getX());a.addProperty("tree_y",tree.getY());a.addProperty("tree_z",tree.getZ());return call("plan_collection",a,true);}
        Block block(String name){return BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:"+name));}
        void makeTree(String species){boolean fungus=species.equals("crimson") || species.equals("warped");for(int y=0;y<3;y++)h.getLevel().setBlock(tree.above(y),block(species+(fungus?"_stem":"_log")).defaultBlockState(),2);
            var cap=block(fungus?(species.equals("crimson")?"nether_wart_block":"warped_wart_block"):species+"_leaves").defaultBlockState();if(cap.hasProperty(BlockStateProperties.PERSISTENT))cap=cap.setValue(BlockStateProperties.PERSISTENT,false);
            h.getLevel().setBlock(tree.above(3),cap,2);h.getLevel().setBlock(tree.below(),fungus?block(species+"_nylium").defaultBlockState():Blocks.DIRT.defaultBlockState(),2);}
        void resetBodyForFixture(){var p=r.player();p.setPos(Vec3.atBottomCenterOf(origin));p.setDeltaMovement(Vec3.ZERO);p.setOnGround(true);p.stopControlling();}
        void stage(int n){dev.mcai.companion.MinecraftAiCompanion.LOGGER.info("Collection regression stage {} finished after {} ticks",stage,h.getTick()-since);stage=n;since=h.getTick();}
        void record(String name){var row=call("collection_status",new JsonObject(),true);row.addProperty("case",name);row.add("inventory",r.player().inventoryLedger.inventory());evidence.add(row);}
        int count(Item item){int n=0;for(int i=0;i<r.player().getInventory().getContainerSize();i++){var s=r.player().getInventory().getItem(i);if(s.is(item))n+=s.getCount();}return n;}
        void start(){
            for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++){h.getLevel().setBlock(origin.offset(x,-1,z),Blocks.DIRT.defaultBlockState(),2);for(int y=0;y<=10;y++)h.getLevel().setBlock(origin.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);}
            var p=r.player();p.setPos(Vec3.atBottomCenterOf(origin));p.setDeltaMovement(Vec3.ZERO);p.setOnGround(true);p.setGameMode(GameType.SURVIVAL);p.stopControlling();p.getInventory().clearContent();p.level().getChunkSource().move(p);
            for(String species:java.util.List.of("oak","spruce","birch","jungle","acacia","dark_oak","mangrove","cherry","pale_oak","crimson","warped")){
                makeTree(species);var survey=call("inspect_tree",xyz(tree),true);h.assertTrue(survey.get("species").getAsString().equals(species) && survey.get("harvestable").getAsBoolean(),"Tree species fixture rejected: "+survey);
            }
            makeTree("oak");
            for(int dz:new int[]{-3,3})h.getLevel().setBlock(tree.offset(0,0,dz),Blocks.OAK_LOG.defaultBlockState(),2);
            h.assertTrue(call("inspect_tree",xyz(tree),true).get("classification").getAsString().equals("managed_grove_candidate"),"Regular planting row not detected");
            for(int dz:new int[]{-3,3})h.getLevel().setBlock(tree.offset(0,0,dz),Blocks.AIR.defaultBlockState(),2);
            h.getLevel().setBlock(tree.east(),Blocks.OAK_PLANKS.defaultBlockState(),2);h.assertTrue(!call("inspect_tree",xyz(tree),true).get("harvestable").getAsBoolean(),"Building wood accepted");h.getLevel().setBlock(tree.east(),Blocks.PISTON.defaultBlockState(),2);
            h.assertTrue(call("inspect_tree",xyz(tree),true).get("classification").getAsString().equals("automated_tree_farm_candidate"),"Piston farm not identified");h.assertTrue(planTree(true,3).get("phase").getAsString().equals("BLOCKED"),"Machine tree accepted");
            h.getLevel().setBlock(tree.east(),Blocks.BEE_NEST.defaultBlockState(),2);h.assertTrue(!call("inspect_tree",xyz(tree),true).get("harvestable").getAsBoolean(),"Bee fixture accepted");h.getLevel().setBlock(tree.east(),Blocks.CREAKING_HEART.defaultBlockState(),2);h.assertTrue(!call("inspect_tree",xyz(tree),true).get("harvestable").getAsBoolean(),"Creaking heart accepted");h.getLevel().setBlock(tree.east(),Blocks.AIR.defaultBlockState(),2);
            var farm=obj("{\"operation\":\"save\",\"name\":\"Test orchard\",\"kind\":\"manual\"}");for(String axis:java.util.List.of("x","y","z")){int coordinate=xyz(tree).get(axis).getAsInt();farm.addProperty("min_"+axis,coordinate-1);farm.addProperty("max_"+axis,coordinate+4);}call("tree_farm",farm,true);p.inventoryLedger=new InventoryLedger(p);
            h.assertTrue(call("inspect_tree",xyz(tree),true).get("classification").getAsString().equals("managed_grove_candidate"),"Declared farm lost on reload");h.assertTrue(planTree(false,3).get("phase").getAsString().equals("BLOCKED"),"Manual farm silently authorized");
            var loose=new ItemEntity(h.getLevel(),origin.getX()+3.5,origin.getY(),origin.getZ()+.5,new ItemStack(Items.OAK_LOG,4));h.getLevel().addFreshEntity(loose);
            var plan=call("plan_collection",obj("{\"resource\":\"wood\",\"source\":\"any\",\"count\":4,\"radius\":10}"),true);choose(plan,"nearby-drops");stage(1);h.onEachTick(this::tick);
        }
        void tick(){
            var p=r.player();var status=call("collection_status",new JsonObject(),true);String phase=status.get("phase").getAsString();long age=h.getTick()-since;
            if(age>0 && age%500==0)dev.mcai.companion.MinecraftAiCompanion.LOGGER.info("Collection regression waiting at stage {}: {}",stage,status);
            if(stage==1 && phase.equals("COMPLETED")){
                h.assertTrue(count(Items.OAK_LOG)==4 && h.getLevel().getBlockState(tree).is(Blocks.OAK_LOG),"Loose log collection destroyed tree or failed inventory");record("wood any acquired loose logs without felling");
                // Separate empty-inventory fixture reproduces automatic filling of the selected empty hand.
                p.getInventory().clearContent();p.inventoryLedger.tick();
                call("equip_tool",obj("{\"slot\":0}"),true);choose(planTree(true,1),null);stage(2);
            }else if(stage==2 && r.mining().running() && r.mining().status().get("breakProgress").getAsDouble()>.25){
                call("pause_collection",id(),true);before=count(Items.OAK_LOG);call("say",obj("{\"message\":\"我还在，采集已暂停，可以继续聊天。\"}"),true);call("jump_once",new JsonObject(),false);call("equip_tool",obj("{\"slot\":0}"),false);call("resume_mining",obj("{\"request_id\":\""+r.mining().status().get("requestId").getAsString()+"\"}"),false);stage(3);
            }else if(stage==3 && age>=60){
                h.assertTrue(count(Items.OAK_LOG)==before && h.getLevel().getBlockState(tree).is(Blocks.OAK_LOG),"Paused collection kept breaking");call("resume_collection",id(),true);stage(4);
            }else if(stage==4 && phase.equals("COMPLETED")){
                h.assertTrue(status.get("wholeTreeVerified").getAsBoolean(),"Tree completion lacks full trunk proof");h.assertTrue(count(Items.OAK_LOG)==3 && status.get("brokenBlocks").getAsInt()==3 && status.get("verifiedInventoryIncrease").getAsInt()==3,"Bare hands tree collection not physically proven");for(int y=0;y<3;y++)h.assertTrue(h.getLevel().getBlockState(tree.above(y)).isAir(),"Tree log remains");record("one approval, three normal bare-hand breaks and pickups; pause/chat/resume");
                resetBodyForFixture();makeTree("birch");h.getLevel().setBlock(tree.above(2).north(),Blocks.BIRCH_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS,net.minecraft.core.Direction.Axis.Z),2);p.getInventory().setItem(0,new ItemStack(Items.WOODEN_AXE));call("equip_tool",obj("{\"slot\":0}"),true);choose(planTree(true,4),null);stage(5);
            }else if(stage==5 && phase.equals("COMPLETED")){
                h.assertTrue(count(Items.BIRCH_LOG)==4 && p.getMainHandItem().getDamageValue()==4,"Axe wear or birch pickup incorrect");record("branched birch tree uses actual axe durability");
                resetBodyForFixture();makeTree("oak");choose(planTree(true,3),null);stage(6);
            }else if(stage==6 && r.mining().running() && r.mining().status().get("breakProgress").getAsDouble()>.35){
                r.onChat("TestHuman","停下");h.assertTrue(r.collection().status().get("phase").getAsString().equals("CANCELLED"),"Chat failed to cancel collection");stage(7);
            }else if(stage==7 && age>=80){
                h.assertTrue(h.getLevel().getBlockState(tree).is(Blocks.OAK_LOG) && p.getMainHandItem().getDamageValue()==4,"Cancelled break resumed later");record("chat cancellation prevents delayed destruction");
                resetBodyForFixture(); // Do not insert the next fixture's ore inside the body's collision box.
                var coal=p.blockPosition().west();h.getLevel().setBlock(coal,Blocks.COAL_ORE.defaultBlockState(),2);h.getLevel().setBlock(coal.west(5),Blocks.COAL_ORE.defaultBlockState(),2);p.getInventory().setItem(20,new ItemStack(Items.WOODEN_PICKAXE));
                var a=obj("{\"resource\":\"minecraft:coal_ore\",\"output_item\":\"minecraft:coal\",\"source\":\"blocks\",\"radius\":3,\"count\":2}");var plan=call("plan_collection",a,true);h.assertTrue(plan.getAsJsonArray("options").get(0).getAsJsonObject().get("targetBlocks").getAsInt()==1,"Fixed sphere included outside ore");choose(plan,null);stage(8);
            }else if(stage==8 && phase.equals("BLOCKED")){
                h.assertTrue(count(Items.COAL)==1 && status.get("verifiedInventoryIncrease").getAsInt()==1,"Partial ore pickup not reported: "+status);record("fixed-radius ore collection reports honest partial result; no fishbone prerequisite");
                disappearing=new ItemEntity(h.getLevel(),p.getX()+5,p.getY(),p.getZ(),new ItemStack(Items.JUNGLE_LOG));h.getLevel().addFreshEntity(disappearing);var a=obj("{\"resource\":\"wood\",\"species\":\"jungle\",\"source\":\"drops\",\"count\":1,\"radius\":8}");choose(call("plan_collection",a,true),null);disappearing.discard();stage(9);
            }else if(stage==9 && phase.equals("BLOCKED")){
                h.assertTrue(count(Items.JUNGLE_LOG)==0 && status.get("verifiedInventoryIncrease").getAsInt()==0,"Disappeared drop falsely acquired");record("disappearing drop returns decision with zero pickup");
                resetBodyForFixture();
                var first=origin.north();var second=origin.north().east();
                h.getLevel().setBlock(first,Blocks.STONE.defaultBlockState(),2);h.getLevel().setBlock(second,Blocks.STONE.defaultBlockState(),2);
                p.getInventory().setItem(0,ItemStack.EMPTY);p.getInventory().setItem(22,new ItemStack(Items.WOODEN_PICKAXE));p.getInventory().setItem(8,ItemStack.EMPTY);call("equip_tool",obj("{\"slot\":8}"),true);
                before=count(Items.COBBLESTONE);
                var stone=call("plan_collection",obj("{\"resource\":\"minecraft:stone\",\"source\":\"blocks\",\"radius\":2,\"count\":2}"),true);
                h.assertTrue(p.getMainHandItem().isEmpty(),"Tool preview equipped a pickaxe before approval");
                choose(stone,null);stage(10);
            }else if(stage==10 && phase.equals("COMPLETED")){
                h.assertTrue(count(Items.COBBLESTONE)==before+2,"Two stones were not physically harvested as cobblestone");
                h.assertTrue(p.getMainHandItem().is(Items.WOODEN_PICKAXE) && p.getMainHandItem().getDamageValue()==2,"Inventory tool was not selected/debited correctly");
                record("stone command defaults to cobblestone and equips a storage-slot wooden pickaxe on approval");
                resetBodyForFixture();makeTree("spruce");
                for(int y=0;y<6;y++)h.getLevel().setBlock(tree.above(y),Blocks.SPRUCE_LOG.defaultBlockState(),2);
                h.getLevel().setBlock(tree.above(6),Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT,false),2);
                before=count(Items.SPRUCE_LOG);choose(planTree(true,1),null);stage(11);
            }else if(stage==11 && phase.equals("COMPLETED")){
                h.assertTrue(status.get("wholeTreeVerified").getAsBoolean() && count(Items.SPRUCE_LOG)==before+6,"Six-log spruce was truncated to the requested item count");
                for(int y=0;y<6;y++)h.assertTrue(h.getLevel().getBlockState(tree.above(y)).isAir(),"Spruce trunk remains after whole-tree completion");
                record("six-log spruce fully felled and acquired despite count=1");
                resetBodyForFixture();
                // Nearest stone is sealed below the body. Farther surface stone
                // must remain a candidate even though count is only four items.
                var buried=origin.below(3);
                for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)
                    h.getLevel().setBlock(buried.offset(x,y,z),Blocks.BEDROCK.defaultBlockState(),2);
                h.getLevel().setBlock(buried,Blocks.STONE.defaultBlockState(),2);
                for(int z=-1;z<=2;z++)h.getLevel().setBlock(origin.offset(6,0,z),Blocks.STONE.defaultBlockState(),2);
                p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(Items.WOODEN_PICKAXE));p.inventoryLedger.tick();
                p.setYRot(-90);p.setXRot(0);p.setYHeadRot(-90);
                var plan=call("plan_collection",obj("{\"resource\":\"stone\",\"source\":\"blocks\",\"radius\":10,\"count\":4}"),true);
                var option=plan.getAsJsonArray("options").get(0).getAsJsonObject();
                h.assertTrue(option.get("targetBlocks").getAsInt()>=5 && option.get("maximumBlocksToBreak").getAsInt()==4,"Alternatives lost or break budget expanded: "+option);
                choose(plan,null);stage(12);
            }else if(stage==12 && phase.equals("COMPLETED")){
                h.assertTrue(count(Items.COBBLESTONE)==4 && status.get("brokenBlocks").getAsInt()==4,"Surface alternatives were not physically harvested and acquired: "+status);
                h.assertTrue(h.getLevel().getBlockState(origin.below(3)).is(Blocks.STONE),"Collector excavated the sealed nearest target");
                h.assertTrue(p.getMainHandItem().is(Items.WOODEN_PICKAXE) && p.getMainHandItem().getDamageValue()==4,"Alternative count consumed excessive durability");
                for(int z=-1;z<=2;z++)h.assertTrue(h.getLevel().getBlockState(origin.offset(6,0,z)).isAir(),"Exposed target remains");
                record("Numen palette scan and alternate surface targets: four cobblestone receipts; buried nearest stone untouched");
                try{var path=r.server().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("collection-physical-evidence.json");java.nio.file.Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(evidence));}catch(java.io.IOException e){throw new IllegalStateException(e);}stage(13);h.succeed();
            }else if(phase.equals("BLOCKED") && stage!=8 && stage!=9 && stage!=13){h.fail("Collection blocked at stage "+stage+": "+status);}
        }
    }
}
