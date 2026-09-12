package dev.mcai.companion.codex;

import com.google.gson.*;
import dev.mcai.companion.agent.AgentRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

/** Native Forge regressions, not independent model gameplay. Fixture setup is explicitly administrative. */
@GameTestNamespace("mcai_companion") @GameTestDontPrefix
public final class ExcavationGameTests {
    @GameTest(name="excavation_regressions",structure="forge:empty48x32x48",maxTicks=100000,padding=8)
    public static void run(GameTestHelper h){if(!Boolean.getBoolean("minepilot.excavationTest")){h.fail("Explicit excavation gate required");return;}new Gate(h).start();}
    private static final class Gate {
        final GameTestHelper h;final AgentRuntime r;final CodexToolService tools;final BlockPos origin;
        int scenario,phase,wait;String request;long began;int beforeWear;Vec3 start;JsonArray evidence=new JsonArray();
        Gate(GameTestHelper h){this.h=h;r=AgentRuntime.active(h.getLevel().getServer());tools=new CodexToolService(r);origin=h.absolutePos(new BlockPos(16,10,16));}
        JsonObject call(String name,JsonObject a){var params=new JsonObject();params.addProperty("name",name);params.add("arguments",a);var req=new JsonObject();req.add("params",params);var result=tools.dispatch("tools/call",req);h.assertTrue(!result.get("isError").getAsBoolean(),name+": "+result);return result.getAsJsonObject("structuredContent");}
        JsonObject json(String s){return JsonParser.parseString(s).getAsJsonObject();}JsonObject id(){var a=new JsonObject();a.addProperty("request_id",request);return a;}
        void start(){h.addCleanup(ignored->r.excavation().cancelForChat());setup();h.onEachTick(this::tick);}
        void setup(){
            r.excavation().cancelForChat();r.collection().cancelForChat();r.placement().cancelForChat();r.mining().cancelForChat();
            for(int x=-7;x<=7;x++)for(int y=-5;y<=5;y++)for(int z=-7;z<=7;z++)h.getLevel().setBlock(origin.offset(x,y,z),(y<0?Blocks.STONE:Blocks.AIR).defaultBlockState(),2);
            for(var e:h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(origin).inflate(20)))e.discard();
            var p=r.player();p.setPos(Vec3.atBottomCenterOf(origin));p.setDeltaMovement(Vec3.ZERO);p.setOnGround(true);p.setGameMode(GameType.SURVIVAL);p.stopControlling();p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(Items.IRON_PICKAXE));p.level().getChunkSource().move(p);p.inventoryLedger.tick();
            start=p.position();beforeWear=0;phase=0;began=System.nanoTime();
            JsonObject a;
            if(scenario==0){h.getLevel().setBlock(origin.offset(3,-2,0),Blocks.COAL_ORE.defaultBlockState(),2);a=json("{\"mode\":\"resource_radius\",\"radius\":5,\"block\":\"minecraft:coal_ore\",\"allow_access\":true}");}
            else if(scenario==1){for(int x=1;x<=3;x++)for(int y=0;y<2;y++)h.getLevel().setBlock(origin.offset(x,y,0),Blocks.STONE.defaultBlockState(),2);a=json("{\"mode\":\"region\",\"relative\":true,\"from\":{\"x\":1,\"y\":0,\"z\":0},\"to\":{\"x\":3,\"y\":1,\"z\":0}}");}
            else if(scenario==2){for(int x=1;x<=6;x++)for(int y=0;y<2;y++)h.getLevel().setBlock(origin.offset(x,y,0),Blocks.STONE.defaultBlockState(),2);a=json("{\"mode\":\"tunnel\",\"direction\":\"east\",\"length\":6}");}
            else if(scenario==3){for(int x=1;x<=3;x++)for(int y=0;y<2;y++)h.getLevel().setBlock(origin.offset(x,y,0),Blocks.STONE.defaultBlockState(),2);a=json("{\"mode\":\"access\",\"relative\":true,\"target\":{\"x\":3,\"y\":0,\"z\":0},\"allow_access\":true}");}
            else {
                var trunk=origin.offset(3,0,0);h.getLevel().setBlock(trunk.below(),Blocks.DIRT.defaultBlockState(),2);
                for(int y=0;y<8;y++)h.getLevel().setBlock(trunk.above(y),Blocks.SPRUCE_LOG.defaultBlockState(),2);h.getLevel().setBlock(trunk.above(8),Blocks.SPRUCE_LEAVES.defaultBlockState(),2);
                p.getInventory().setItem(0,new ItemStack(Items.IRON_AXE));p.getInventory().setItem(1,new ItemStack(Items.COBBLESTONE,32));p.getInventory().setItem(2,new ItemStack(Items.SPRUCE_SAPLING));p.inventoryLedger.tick();
                p.inventoryLedger.annotate(p.inventoryLedger.key(p.getInventory().getItem(1)),5,"Approved scaffold material");
                a=json("{\"mode\":\"tree\",\"relative\":true,\"target\":{\"x\":3,\"y\":0,\"z\":0},\"allow_supports\":true,\"allow_access\":true,\"replant\":true}");
            }
            request=call("plan_excavation",a).get("requestId").getAsString();
        }
        void record(String name){var s=r.excavation().status();s.addProperty("case",name);s.addProperty("pickaxeDamage",r.player().getMainHandItem().getDamageValue());s.add("inventory",r.player().inventoryLedger.inventory());evidence.add(s);}
        void succeed(){try{var file=r.server().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("excavation-physical-evidence.json");java.nio.file.Files.writeString(file,new GsonBuilder().setPrettyPrinting().create().toJson(evidence));}catch(Exception e){throw new IllegalStateException(e);}phase=99;h.succeed();}
        void tick(){
            if(phase==10){var survey=call("mining_survey_status",new JsonObject());h.assertTrue(!java.util.Set.of("FAILED","STALE").contains(survey.get("phase").getAsString()),"Cave survey failed "+survey);if(!survey.get("phase").getAsString().equals("COMPLETED"))return;
                boolean cave=false;for(var v:survey.getAsJsonArray("spaces"))cave|=v.getAsJsonObject().get("classification").getAsString().equals("roofed_cave_candidate");h.assertTrue(cave && survey.get("observedCells").getAsInt()>500,"Roofed native space was not assessed");h.assertTrue(r.player().position().distanceTo(start)<.02 && r.player().getInventory().isEmpty(),"Read-only survey moved the body or changed inventory");evidence.add(survey);succeed();return;}

            h.assertTrue(System.nanoTime()-began<120_000_000_000L,"Excavation wall-clock timeout "+r.excavation().status());
            var s=r.excavation().status();String state=s.get("phase").getAsString();
            h.assertTrue(!state.equals("BLOCKED") && !state.equals("PARTIAL"),"Excavation failed: "+s);
            if(phase==0 && state.equals("PLAN_READY")){
                h.assertTrue(r.player().position().distanceTo(start)<.02 && r.player().getMainHandItem().getDamageValue()==0,"Planning changed body or tool");
                var options=s.getAsJsonArray("options");h.assertTrue(!options.isEmpty(),"No evaluated alternatives");
                var choice=options.get(0).getAsJsonObject();if(scenario==0)h.assertTrue(choice.get("accessBlocks").getAsInt()>0,"Buried ore lacked disclosed access excavation");
                var a=id();a.addProperty("option_id",choice.get("optionId").getAsString());call("choose_excavation",a);phase=1;
            } else if(phase==1 && scenario==1 && r.mining().running() && r.mining().status().get("breakProgress").getAsDouble()>.1){
                call("say",json("{\"message\":\"我在挖掘，也能继续和你聊天。\"}"));call("pause_excavation",id());beforeWear=r.player().getMainHandItem().getDamageValue();wait=(int)h.getTick()+80;phase=2;
            } else if(phase==2 && h.getTick()>=wait){h.assertTrue(r.player().getMainHandItem().getDamageValue()==beforeWear,"Paused excavation spent durability");call("resume_excavation",id());phase=3;
            } else if((phase==1 || phase==3) && state.equals("COMPLETED")){
                if(scenario==0){h.assertTrue(h.getLevel().getBlockState(origin.offset(3,-2,0)).isAir(),"Buried coal still exists");h.assertTrue(r.player().position().distanceTo(start)>1,"No real access movement");h.assertTrue(s.get("collectionVerified").getAsBoolean() && r.player().getInventory().countItem(Items.COAL)==1,"Coal was broken but not physically collected");record("buried coal access and normal break");}
                else if(scenario==1){for(int x=1;x<=3;x++)for(int y=0;y<2;y++)h.assertTrue(h.getLevel().getBlockState(origin.offset(x,y,0)).isAir(),"Region cell remained");record("relative region with pause chat resume");}
                else if(scenario==2){for(int x=1;x<=6;x++)for(int y=0;y<2;y++)h.assertTrue(h.getLevel().getBlockState(origin.offset(x,y,0)).isAir(),"Tunnel cell remained");record("bounded tunnel");}
                else if(scenario==3){h.assertTrue(h.getLevel().getBlockState(origin.offset(3,0,0)).is(Blocks.STONE) && r.mining().reachable(origin.offset(3,0,0)),"Access destroyed target or did not reach it");record("approach-only preserves final target");}
                else {h.assertTrue(s.get("wholeTreeVerified").getAsBoolean() && s.get("replantVerified").getAsBoolean() && r.player().getInventory().countItem(Items.SPRUCE_LOG)==8,"Tall tree, pickup or replant not verified");h.assertTrue(h.getLevel().getBlockState(origin.offset(3,0,0)).is(Blocks.SPRUCE_SAPLING),"Correct sapling missing");record("eight-log tree with approved scaffolding and native replanting");}
                if(++scenario<5){setup();return;}
                r.player().getInventory().clearContent();r.player().setPos(Vec3.atBottomCenterOf(origin));r.player().setDeltaMovement(Vec3.ZERO);r.player().stopControlling();r.player().setOnGround(true);start=r.player().position();
                for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)for(int y=-1;y<=3;y++)h.getLevel().setBlock(origin.offset(x,y,z),(y==-1 || y==3?Blocks.STONE:Blocks.AIR).defaultBlockState(),2);
                for(var e:h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(origin).inflate(20)))e.discard();
                call("survey_mining",json("{\"radius\":8,\"resource\":\"coal\"}"));phase=10;
            }
        }
    }
}
