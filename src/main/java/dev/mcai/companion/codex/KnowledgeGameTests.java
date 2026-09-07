package dev.mcai.companion.codex;

import java.util.*;
import com.google.gson.*;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.knowledge.*;
import dev.mcai.companion.agent.navigation.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

/** Source-informed fixture; actions go through the same public tool dispatcher as MCP. */
@GameTestNamespace("mcai_companion") @GameTestDontPrefix
public final class KnowledgeGameTests {
    @GameTest(name="knowledge_tool_regressions",structure="forge:empty48x32x48",maxTicks=1200,padding=8)
    public static void run(GameTestHelper helper) {
        if(!Boolean.getBoolean("minepilot.knowledgeTest")){helper.fail("Knowledge gate not explicitly selected");return;}
        new Gate(helper).start();
    }
    private static final class Gate {
        final GameTestHelper h;final AgentRuntime runtime;final CodexToolService tools;final BlockPos origin;
        final List<net.minecraft.world.entity.Entity> fixtures=new ArrayList<>();
        Vec3 start;int phase;long began;String entry;long eventSequence;ItemEntity far;String routeRequest;
        Gate(GameTestHelper h){this.h=h;runtime=AgentRuntime.active(h.getLevel().getServer());tools=new CodexToolService(runtime);origin=h.absolutePos(new BlockPos(8,2,8));}
        JsonObject call(String tool,String args) {
            JsonObject p=new JsonObject();p.addProperty("name",tool);p.add("arguments",JsonParser.parseString(args));JsonObject request=new JsonObject();request.add("params",p);
            JsonObject result=tools.dispatch("tools/call",request);
            h.assertTrue(!result.get("isError").getAsBoolean(),"Tool failed: "+tool+" "+result);return result.getAsJsonObject("structuredContent");
        }
        void start(){
            for(int x=-3;x<=24;x++)for(int z=-3;z<=3;z++){
                h.getLevel().setBlockAndUpdate(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState());
                for(int y=0;y<=4;y++)h.getLevel().setBlockAndUpdate(origin.offset(x,y,z),Blocks.AIR.defaultBlockState());
            }
            var p=runtime.player();p.getInventory().clearContent();p.setGameMode(GameType.ADVENTURE);
            p.setPos(Vec3.atBottomCenterOf(origin));p.setDeltaMovement(Vec3.ZERO);p.setOnGround(true);p.stopControlling();p.level().getChunkSource().move(p);
            runtime.player().inventoryLedger.tick();eventSequence=call("inventory","{}").get("latestEventSequence").getAsLong();
            start=p.position();began=h.getTick();call("turn","{\"heading\":90}");
            h.addCleanup(ignored->{fixtures.forEach(net.minecraft.world.entity.Entity::discard);runtime.onChat("TestHuman","停下");});h.onEachTick(this::tick);
        }
        void tick(){
            h.assertTrue(h.getTick()-began<1000,"Knowledge gate timed out at phase "+phase);
            var p=runtime.player();
            if(phase==0){
                if(!runtime.turnPhase().equals("COMPLETED"))return;
                h.assertTrue(p.position().distanceTo(start)<.01,"Turn translated the body");
                h.assertTrue(WorldPerception.difference(NavigationFollower.minecraftYawToHeading(p.getYRot()),90)<1.1,"Turn did not reach east");
                ItemEntity gift=new ItemEntity(h.getLevel(),p.getX(),p.getY()+.1,p.getZ(),new ItemStack(Items.COBBLESTONE,5));
                gift.setNoPickUpDelay();gift.setDeltaMovement(Vec3.ZERO);DropProvenance.mark(gift,"player_toss",p);h.getLevel().addFreshEntity(gift);fixtures.add(gift);
                phase=1;return;
            }
            if(phase==1){
                var inventory=call("inventory","{}");if(inventory.getAsJsonArray("entries").isEmpty())return;
                var row=inventory.getAsJsonArray("entries").get(0).getAsJsonObject();entry=row.get("entryId").getAsString();
                h.assertTrue(row.get("count").getAsInt()==5 && row.get("importance").getAsInt()==2,"Pickup/count/default protection mismatch");
                var events=call("inventory_events","{\"after_sequence\":"+eventSequence+"}").getAsJsonArray("events");if(events.isEmpty())return;
                var gain=events.get(0).getAsJsonObject().getAsJsonArray("acquired").get(0).getAsJsonObject();
                h.assertTrue(gain.get("count").getAsInt()==5 && gain.getAsJsonObject("source").get("category").getAsString().equals("player_toss"),"Pickup source/count not correlated");
                call("annotate_item","{\"entry_id\":\""+entry+"\",\"importance\":0,\"note\":\"保留材料\"}");
                int slot=row.getAsJsonArray("slots").get(0).getAsInt();var stack=p.getInventory().removeItemNoUpdate(slot);p.getInventory().setItem(7,stack);p.inventoryLedger.tick();
                h.assertTrue(!p.inventoryLedger.expendable(stack),"Slot move bypassed protection");
                p.inventoryLedger=new InventoryLedger(p);h.assertTrue(p.inventoryLedger.importance(stack)==0,"Policy did not survive memory reload");
                var dest=new NavigationPlan.ResolvedDestination(p.level().dimension().identifier().toString(),p.getX()+2,p.getY(),p.getZ(),.5,false,"fixture",OptionalDouble.empty(),Optional.empty());
                var builder=new NavigationSnapshotBuilder(NavigationSnapshotBuilder.CaptureConfig.defaults());
                h.assertTrue(builder.capture(p,dest,0).resources().supportBlocks()==0,"Protected blocks counted for support");
                call("annotate_item","{\"entry_id\":\""+entry+"\",\"importance\":4,\"note\":\"允许垫脚\"}");
                h.assertTrue(builder.capture(p,dest,0).resources().supportBlocks()==0,"Adventure mode advertised unrestricted support placement");
                p.setGameMode(GameType.SURVIVAL);
                var resources=builder.capture(p,dest,0).resources();
                h.assertTrue(resources.supportStock().size()==1 && resources.supportStock().getFirst().count()==5,"Expendable stock lacks exact manifest");
                var manifest=AnytimeNavigationPlanner.supportManifest(resources.supportStock(),3);
                h.assertTrue(manifest.getFirst().count()==3 && manifest.getFirst().item().equals("minecraft:cobblestone"),"Wrong declared route materials");
                p.setGameMode(GameType.ADVENTURE);
                call("waypoint","{\"operation\":\"save\",\"name\":\"home-test\",\"note\":\"test\"}");
                p.inventoryLedger=new InventoryLedger(p);
                var remembered=p.inventoryLedger.remembered("home-test",p.level().dimension().identifier().toString());
                h.assertTrue(Math.abs(remembered.get("x").getAsDouble()-p.getX())<.01,"Waypoint did not round-trip");
                far=new ItemEntity(h.getLevel(),p.getX()+20,p.getY(),p.getZ(),new ItemStack(Items.DIAMOND));far.setNoGravity(true);far.setNeverPickUp();far.setDeltaMovement(Vec3.ZERO);h.getLevel().addFreshEntity(far);fixtures.add(far);
                wall(Blocks.STONE.defaultBlockState());phase=2;return;
            }
            if(phase==2){
                h.assertTrue(!runtime.perception.visible(far) && !runtime.perception.sensed(far),"Stone wall leaked remote dropped item");
                for(var block:List.<net.minecraft.world.level.block.Block>of(Blocks.GLASS,Blocks.OAK_LEAVES,Blocks.OAK_DOOR,Blocks.IRON_BARS,Blocks.CHEST,
                        Blocks.BARREL,Blocks.SHULKER_BOX,Blocks.ENCHANTING_TABLE,Blocks.STONE_SLAB,Blocks.WATER,
                        Blocks.REDSTONE_LAMP,net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse("minecraft:copper_bulb")),Blocks.SCULK_SENSOR,Blocks.CRAFTER,Blocks.PISTON)) {
                    wall(block.defaultBlockState());h.assertTrue(runtime.perception.visible(far),"Pass-through category blocked vision: "+block);
                }
                wall(Blocks.REDSTONE_BLOCK.defaultBlockState());h.assertTrue(!runtime.perception.visible(far),"Redstone block incorrectly transparent");
                wall(Blocks.STONE.defaultBlockState());
                Villager near=new Villager(EntityTypes.VILLAGER,h.getLevel());near.setNoAi(true);near.setPos(p.getX()+8,p.getY(),p.getZ());h.getLevel().addFreshEntity(near);fixtures.add(near);
                h.assertTrue(runtime.perception.sensed(near) && !runtime.perception.visible(near),"Local sensor/visual separation failed");
                var sensed=call("sense","{\"kind\":\"entities\",\"filter\":\"minecraft:villager\",\"limit\":4}");h.assertTrue(sensed.getAsJsonArray("results").size()<=4,"Unbounded entity result");
                boolean villagerReported=false;
                for(var result:sensed.getAsJsonArray("results")){var row=result.getAsJsonObject();if(row.get("uuid").getAsString().equals(near.getUUID().toString())) {
                    villagerReported=true;h.assertTrue(row.has("profession") && row.get("age").getAsString().equals("adult") && !row.get("visible").getAsBoolean(),"Villager metadata/sensor attribution missing");}}
                h.assertTrue(villagerReported,"Public query omitted the nearby villager");
                h.assertTrue(call("observe","{}").getAsJsonObject("world").has("dayTime"),"Missing world clock");
                var blocks=call("sense","{\"kind\":\"blocks\",\"radius\":10,\"limit\":4}");h.assertTrue(blocks.getAsJsonArray("results").size()<=4 && blocks.get("scannedCells").getAsInt()<=4096,"Unbounded block scan");
                wall(Blocks.GLASS.defaultBlockState());
                JsonObject accepted=call("request_navigation","{\"target_kind\":\"dropped_item\",\"target_name\":\""+far.getUUID()+"\",\"preferred_pace\":\"walk\",\"player_intent\":\"test dynamic stack\"}");
                String id=accepted.get("requestId").getAsString();call("say","{\"message\":\"检查掉落物目标\",\"navigation_request_id\":\""+id+"\"}");far.discard();
                var failed=call("plan_navigation","{\"request_id\":\""+id+"\"}");
                h.assertTrue(failed.get("phase").getAsString().equals("FAILED") && failed.get("lastEventMessage").getAsString().contains("DROPPED_ITEM_UNAVAILABLE"),"Missing dynamic item silently completed or retargeted");
                h.assertTrue(p.getInventory().countItem(Items.COBBLESTONE)==5,"A knowledge action spent carried blocks");
                dev.mcai.companion.MinecraftAiCompanion.LOGGER.info("Knowledge gate physically verified heading={}, body={}, inventory=5 cobblestone; policy/manifest/reload/occlusion/drop-loss checks passed",NavigationFollower.minecraftYawToHeading(p.getYRot()),p.position());
                wall(Blocks.AIR.defaultBlockState());
                for(int x=-1;x<=4;x++) {
                    h.getLevel().setBlockAndUpdate(origin.offset(x,2,0),Blocks.STONE.defaultBlockState());
                    for(int y=0;y<=1;y++)for(int z:new int[]{-1,1})h.getLevel().setBlockAndUpdate(origin.offset(x,y,z),Blocks.STONE.defaultBlockState());
                }
                h.getLevel().setBlockAndUpdate(origin.offset(1,-1,0),Blocks.AIR.defaultBlockState());
                p.setGameMode(GameType.SURVIVAL);
                var reserved=call("request_navigation","{\"target_kind\":\"coordinates\",\"x\":"+(p.getX()+2)+",\"y\":"+p.getY()+",\"z\":"+p.getZ()+",\"acceptance_radius\":0.5,\"preferred_pace\":\"walk\",\"player_intent\":\"test declared support\"}");
                routeRequest=reserved.get("requestId").getAsString();call("say","{\"message\":\"测试垫脚清单\",\"navigation_request_id\":\""+routeRequest+"\"}");
                call("plan_navigation","{\"request_id\":\""+routeRequest+"\"}");phase=3;return;
            }
            if(phase==3 || phase==5) {
                var status=call("navigation_status","{}");
                h.assertTrue(!status.get("phase").getAsString().equals("FAILED"),"Support corridor planning failed: "+status);
                if(!status.get("phase").getAsString().equals("PLAN_READY"))return;
                var route=status.getAsJsonArray("routeOptions").get(0).getAsJsonObject();
                h.assertTrue(route.get("supportBlocksRequired").getAsInt()==1,"Expected one real support: "+route);
                var manifest=route.getAsJsonArray("supportMaterials");h.assertTrue(manifest.size()==1 && manifest.get(0).getAsJsonObject().get("count").getAsInt()==1
                        && manifest.get(0).getAsJsonObject().get("item").getAsString().equals("minecraft:cobblestone"),"Missing exact public support list");
                if(phase==3)call("annotate_item","{\"entry_id\":\""+entry+"\",\"importance\":0,\"note\":\"protected after planning\"}");
                call("choose_navigation","{\"request_id\":\""+routeRequest+"\",\"option_id\":\""+route.get("optionId").getAsString()+"\",\"pace\":\"walk\"}");phase++;return;
            }
            if(phase==4){
                var status=call("navigation_status","{}");if(!status.get("phase").getAsString().equals("REPLAN_REQUIRED"))return;
                h.assertTrue(p.getInventory().countItem(Items.COBBLESTONE)==5 && h.getLevel().getBlockState(origin.offset(1,-1,0)).isAir(),"Protected material spent after plan");
                call("annotate_item","{\"entry_id\":\""+entry+"\",\"importance\":4,\"note\":\"allow declared cost\"}");
                call("plan_navigation","{\"request_id\":\""+routeRequest+"\"}");phase=5;return;
            }
            if(phase==6){
                var status=call("navigation_status","{}");h.assertTrue(!Set.of("FAILED","REPLAN_REQUIRED").contains(status.get("phase").getAsString()),"Support execution failed: "+status);
                if(!status.get("phase").getAsString().equals("COMPLETED"))return;
                h.assertTrue(h.getLevel().getBlockState(origin.offset(1,-1,0)).is(Blocks.COBBLESTONE) && p.getInventory().countItem(Items.COBBLESTONE)==4,"Declared one-block cost was not physically observed");
                h.assertTrue(p.position().distanceTo(start.add(2,0,0))<=.5,"Support route did not physically arrive");
                dev.mcai.companion.MinecraftAiCompanion.LOGGER.info("Public support manifest verified: cobblestone 1; protected plan spent 0; authorized execution consumed 1 and arrived at {}",p.position());
                // Keep normal gravity/bounce; a no-gravity item hides bad velocity extrapolation.
                for(int x=3;x<=9;x++)for(int z=-1;z<=1;z++)for(int y=0;y<=2;y++)
                    h.getLevel().setBlockAndUpdate(origin.offset(x,y,z),Blocks.AIR.defaultBlockState());
                p.setGameMode(GameType.ADVENTURE);
                far=new ItemEntity(h.getLevel(),p.getX()+5,p.getY()+.2,p.getZ(),new ItemStack(Items.APPLE,3));
                far.setDeltaMovement(Vec3.ZERO);h.getLevel().addFreshEntity(far);fixtures.add(far);
                began=h.getTick();start=p.position();phase=7;return;
            }
            if(phase==7 && h.getTick()-began>=30){
                var accepted=call("request_navigation","{\"target_kind\":\"dropped_item\",\"target_name\":\""+far.getUUID()+"\",\"preferred_pace\":\"walk\",\"player_intent\":\"collect actual dropped apples\"}");
                routeRequest=accepted.get("requestId").getAsString();
                call("say","{\"message\":\"去捡苹果\",\"navigation_request_id\":\""+routeRequest+"\"}");
                call("plan_navigation","{\"request_id\":\""+routeRequest+"\"}");phase=8;return;
            }
            if(phase==8){
                var status=call("navigation_status","{}");
                h.assertTrue(!status.get("phase").getAsString().equals("FAILED"),"Grounded dropped-stack plan failed: "+status);
                if(!status.get("phase").getAsString().equals("PLAN_READY"))return;
                var option=status.getAsJsonArray("routeOptions").get(0).getAsJsonObject();
                call("choose_navigation","{\"request_id\":\""+routeRequest+"\",\"option_id\":\""+option.get("optionId").getAsString()+"\",\"pace\":\"walk\"}");phase=9;return;
            }
            if(phase==9 && p.getInventory().countItem(Items.APPLE)==3){
                h.assertTrue(p.position().distanceTo(start)>3 && !far.isAlive(),"Pickup lacked physical displacement/entity removal");
                h.assertTrue(p.getInventory().countItem(Items.COBBLESTONE)==4,"Adventure pickup spent carried materials");
                dev.mcai.companion.MinecraftAiCompanion.LOGGER.info("Dynamic gravity item physically collected: apples=3, start={}, final={}",start,p.position());
                var temp=p.level().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/minepilot-memory.json.tmp");
                try {
                    java.nio.file.Files.createDirectory(temp);
                    boolean rejected=false;
                    try {p.inventoryLedger.annotate(entry,0,"protect during storage failure");}
                    catch(IllegalStateException expected){rejected=true;}
                    h.assertTrue(rejected && !p.inventoryLedger.expendable(p.getInventory().getItem(7)),"Failed protection save left materials spendable");
                } catch(java.io.IOException failure){throw new IllegalStateException("Could not construct storage failure fixture",failure);}
                finally {try {java.nio.file.Files.deleteIfExists(temp);}catch(java.io.IOException ignored){}}
                phase=10;h.succeed();
            }
        }
        void wall(net.minecraft.world.level.block.state.BlockState state){for(int y=0;y<=3;y++)for(int z=-2;z<=2;z++)h.getLevel().setBlock(origin.offset(4,y,z),state,2);}
    }
}
