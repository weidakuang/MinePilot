package dev.mcai.companion.codex;

import com.google.gson.*;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.knowledge.InventoryLedger;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

/** Source-informed real-server gate; all accepted actions use the public MCP dispatcher. */
@GameTestNamespace("mcai_companion") @GameTestDontPrefix
public final class MiningGameTests {
    private static java.util.UUID deniedActor;
    static {
        net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.BUS.addListener(event ->
                event.getEntity().getUUID().equals(deniedActor)
                && event.getAction()==net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START);
    }
    @GameTest(name="mining_tool_regressions",structure="forge:empty48x32x48",maxTicks=1600,padding=8)
    public static void run(GameTestHelper helper){
        if(!Boolean.getBoolean("minepilot.miningTest")){helper.fail("Mining gate not explicitly selected");return;}
        new Gate(helper).start();
    }
    private static final class Gate {
        final GameTestHelper h;final AgentRuntime runtime;final CodexToolService tools;final BlockPos origin,target;
        int phase;long phaseTick,eventSequence;String request,policy,pickupRequest;Vec3 start;JsonArray evidence=new JsonArray();
        Gate(GameTestHelper h){this.h=h;runtime=AgentRuntime.active(h.getLevel().getServer());tools=new CodexToolService(runtime);origin=h.absolutePos(new BlockPos(12,3,12));target=origin.offset(0,0,-1);}
        JsonObject args(String text){return JsonParser.parseString(text).getAsJsonObject();}
        JsonObject call(String name,JsonObject args,boolean expectSuccess){
            var params=new JsonObject();params.addProperty("name",name);params.add("arguments",args);var req=new JsonObject();req.add("params",params);
            var reply=tools.dispatch("tools/call",req);h.assertTrue(reply.get("isError").getAsBoolean()!=expectSuccess,"Unexpected tool outcome "+name+": "+reply);
            return expectSuccess?reply.getAsJsonObject("structuredContent"):reply;
        }
        JsonObject plan(boolean success){var a=new JsonObject();a.addProperty("x",target.getX());a.addProperty("y",target.getY());a.addProperty("z",target.getZ());return call("plan_mining",a,success);}
        void choose(){call("choose_mining",args("{\"request_id\":\""+request+"\",\"option_id\":\"held-tool\"}"),true);}
        JsonObject id(){return args("{\"request_id\":\""+request+"\"}");}
        void stage(int next){phase=next;phaseTick=h.getTick();}
        void record(String name){var row=runtime.mining().status();row.addProperty("case",name);row.add("inventory",runtime.player().inventoryLedger.inventory());evidence.add(row);}
        void start(){
            for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){
                h.getLevel().setBlock(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
                for(int y=0;y<=4;y++)h.getLevel().setBlock(origin.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);
            }
            var p=runtime.player();p.setPos(Vec3.atBottomCenterOf(origin));p.setDeltaMovement(Vec3.ZERO);p.setOnGround(true);p.stopControlling();p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.level().getChunkSource().move(p);
            p.setYRot(180);p.setXRot(0);p.stopControlling();
            var nearby=origin.offset(0,0,8);var corner=origin.offset(8,0,8);
            h.getLevel().setBlock(nearby,Blocks.EMERALD_ORE.defaultBlockState(),2);
            h.getLevel().setBlock(corner,Blocks.EMERALD_ORE.defaultBlockState(),2);
            String cursor="";boolean foundNear=false,foundCorner=false;
            for(int page=0;page<20;page++){
                var query=runtime.perception.blocks(12,"minecraft:emerald_ore","blocks",cursor,64);
                for(var value:query.getAsJsonArray("results")){
                    var row=value.getAsJsonObject();
                    foundNear|=row.get("x").getAsDouble()==nearby.getX()+.5 && row.get("z").getAsDouble()==nearby.getZ()+.5;
                    foundCorner|=row.get("x").getAsDouble()==corner.getX()+.5 && row.get("z").getAsDouble()==corner.getZ()+.5;
                }
                if(query.get("complete").getAsBoolean())break;
                cursor=query.get("cursor").getAsString();
            }
            h.assertTrue(foundNear && !foundCorner,"Proximity sphere did not see behind or leaked a cube corner outside vision");
            h.getLevel().setBlock(target,Blocks.COAL_ORE.defaultBlockState(),2);start=p.position();
            p.inventoryLedger.tick();plan(false);
            h.assertTrue(h.getLevel().getBlockState(target).is(Blocks.COAL_ORE),"Wrong-tool plan changed the world");
            p.getInventory().setItem(0,new ItemStack(Items.WOODEN_PICKAXE));call("equip_tool",args("{\"slot\":0}"),true);
            p.inventoryLedger.tick();policy=p.inventoryLedger.key(p.getMainHandItem());p.inventoryLedger.annotate(policy,0,"保留矿镐");
            eventSequence=p.inventoryLedger.events(0,32).get("latestSequence").getAsLong();
            request=plan(true).get("requestId").getAsString();
            h.assertTrue(h.getLevel().getBlockState(target).is(Blocks.COAL_ORE) && p.getMainHandItem().getDamageValue()==0,"Planning mutated target or tool");
            call("pause_mining",id(),false);
            choose();stage(0);h.onEachTick(this::tick);h.addCleanup(ignored->{deniedActor=null;runtime.mining().cancelForChat();});
        }
        void tick(){
            var p=runtime.player();long age=h.getTick()-phaseTick;
            h.assertTrue(age<500,"Mining phase timed out: "+phase+" status "+runtime.mining().status());
            if(phase==0 && runtime.mining().status().get("breakProgress").getAsDouble()>.15){
                call("say",args("{\"message\":\"我正在挖煤，也能继续聊天。\"}"),true);
                h.assertTrue(runtime.mining().running(),"Chat stopped mining");
                call("jump_once",new JsonObject(),false);
                call("equip_tool",args("{\"slot\":8}"),false);
                call("pause_mining",id(),true);record("pause during physical break");stage(1);
            } else if(phase==1 && age>=70){
                h.assertTrue(h.getLevel().getBlockState(target).is(Blocks.COAL_ORE) && p.getMainHandItem().getDamageValue()==0,"Paused break completed or wore tool");
                call("resume_mining",id(),true);stage(2);
            } else if(phase==2 && runtime.mining().phase().equals("COMPLETED")){
                h.assertTrue(h.getLevel().getBlockState(target).isAir(),"Completion lacks changed block");
                h.assertTrue(p.getMainHandItem().getDamageValue()==1,"Expected one real wooden-pickaxe damage point");
                h.assertTrue(p.inventoryLedger.key(p.getMainHandItem()).equals(policy) && p.inventoryLedger.importance(p.getMainHandItem())==0,"Wear lost tool identity or protection");
                h.assertTrue(p.position().distanceTo(start)<.02,"Single-block mining translated the body");
                stage(3);
            } else if(phase==3 && p.getInventory().countItem(Items.COAL)==0){
                if(pickupRequest==null){
                    var drops=runtime.mining().status().getAsJsonArray("emittedDrops");
                    h.assertTrue(!drops.isEmpty(),"No physical drop identity from mining");
                    var args=new JsonObject();args.addProperty("target_kind","dropped_item");args.addProperty("target_name",drops.get(0).getAsJsonObject().get("entityId").getAsString());args.addProperty("acceptance_radius",.5);args.addProperty("preferred_pace","walk");args.addProperty("player_intent","Collect the mined coal");
                    pickupRequest=call("request_navigation",args,true).get("requestId").getAsString();
                    call("say",args("{\"message\":\"我走过去捡起煤炭。\",\"navigation_request_id\":\""+pickupRequest+"\"}"),true);
                    call("plan_navigation",args("{\"request_id\":\""+pickupRequest+"\"}"),true);
                } else if(runtime.navigation().status().phase()==dev.mcai.companion.agent.navigation.NavigationToolCoordinator.Phase.PLAN_READY){
                    var option=call("navigation_status",new JsonObject(),true).getAsJsonArray("routeOptions").asList().stream().map(JsonElement::getAsJsonObject).filter(o->o.get("feasibleNow").getAsBoolean() && o.get("supportBlocksRequired").getAsInt()==0).findFirst().orElseThrow();
                    call("choose_navigation",args("{\"request_id\":\""+pickupRequest+"\",\"option_id\":\""+option.get("optionId").getAsString()+"\",\"pace\":\"walk\"}"),true);
                }
            } else if(phase==3 && p.getInventory().countItem(Items.COAL)==1){
                var events=p.inventoryLedger.events(eventSequence,32).getAsJsonArray("events");boolean coal=false;
                for(var ev:events)for(var gain:ev.getAsJsonObject().getAsJsonArray("acquired")){
                    var row=gain.getAsJsonObject();h.assertTrue(!row.get("item").getAsString().equals("minecraft:wooden_pickaxe"),"Tool wear falsely reported as acquisition");
                    if(row.get("item").getAsString().equals("minecraft:coal")){
                        var source=row.getAsJsonObject("source");coal=source.get("category").getAsString().equals("mined_block") && source.get("requestId").getAsString().equals(request);
                    }
                }
                h.assertTrue(coal,"Actual coal pickup lacks causal mining request");
                if(pickupRequest!=null && !runtime.navigation().status().phase().terminal())call("cancel_navigation",args("{\"request_id\":\""+pickupRequest+"\",\"reason\":\"Pickup physically verified\"}"),true);
                record("coal harvested and physically acquired through public navigation");
                // Separate cancellation fixture starts at the original safe body position.
                p.setPos(start);p.setDeltaMovement(Vec3.ZERO);p.setOnGround(true);p.stopControlling();
                p.inventoryLedger=new InventoryLedger(p);h.assertTrue(p.inventoryLedger.importance(p.getMainHandItem())==0,"Worn-tool protection did not survive restart");
                h.getLevel().setBlock(target,Blocks.COAL_ORE.defaultBlockState(),2);request=plan(true).get("requestId").getAsString();choose();stage(4);
            } else if(phase==4 && runtime.mining().status().get("breakProgress").getAsDouble()>.7){
                runtime.onChat("TestHuman","停下");h.assertTrue(runtime.mining().phase().equals("CANCELLED"),"Stop chat did not cancel mining on receipt");stage(5);
            } else if(phase==5 && age>=80){
                h.assertTrue(h.getLevel().getBlockState(target).is(Blocks.COAL_ORE) && p.getMainHandItem().getDamageValue()==1,"Cancelled near-complete break continued later");record("stop persists after near-complete break");
                p.setGameMode(GameType.ADVENTURE);plan(false);p.setGameMode(GameType.SURVIVAL);
                h.getLevel().setBlock(target.above(),Blocks.GRAVEL.defaultBlockState(),2);plan(false);h.getLevel().setBlock(target.above(),Blocks.AIR.defaultBlockState(),2);
                request=plan(true).get("requestId").getAsString();h.getLevel().setBlock(target,Blocks.DIRT.defaultBlockState(),2);
                call("choose_mining",args("{\"request_id\":\""+request+"\",\"option_id\":\"held-tool\"}"),false);
                h.assertTrue(h.getLevel().getBlockState(target).is(Blocks.DIRT),"Stale approval destroyed replacement block");
                call("equip_tool",args("{\"slot\":8}"),true);request=plan(true).get("requestId").getAsString();choose();stage(6);
            } else if(phase==6 && runtime.mining().phase().equals("COMPLETED")){
                h.assertTrue(h.getLevel().getBlockState(target).isAir() && p.getMainHandItem().isEmpty(),"Bare-hands dirt mining failed");record("bare hands and stale/adventure/gravity guards");
                h.getLevel().setBlock(target,Blocks.COAL_ORE.defaultBlockState(),2);call("equip_tool",args("{\"slot\":0}"),true);
                request=plan(true).get("requestId").getAsString();deniedActor=p.getUUID();choose();stage(7);
            } else if(phase==7 && runtime.mining().phase().equals("BLOCKED")){
                deniedActor=null;stage(8);
            } else if(phase==8 && age>=80){
                h.assertTrue(h.getLevel().getBlockState(target).is(Blocks.COAL_ORE) && p.getMainHandItem().getDamageValue()==1,"Denied START later destroyed a block or spent durability");
                record("Forge-denied start remains unbroken");
                var root=runtime.server().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
                try{java.nio.file.Files.writeString(root.resolve("mining-physical-evidence.json"),new GsonBuilder().setPrettyPrinting().create().toJson(evidence));}catch(java.io.IOException e){throw new IllegalStateException(e);}
                stage(9);h.succeed();
            }
        }
    }
}
