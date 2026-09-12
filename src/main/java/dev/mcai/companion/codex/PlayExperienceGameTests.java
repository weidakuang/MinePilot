package dev.mcai.companion.codex;

import com.google.gson.*;
import java.util.*;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.placement.PlacementTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

/** Reproductions of reported real-play failures on native Forge, source-informed. */
@GameTestNamespace("mcai_companion") @GameTestDontPrefix
public final class PlayExperienceGameTests {
    @GameTest(name="play_experience_regressions",structure="forge:empty48x32x48",maxTicks=2400,padding=8)
    public static void run(GameTestHelper h){new Gate(h).start();}
    private static final class Gate {
        final GameTestHelper h;final AgentRuntime r;final CodexToolService tools;final BlockPos origin;
        final List<net.minecraft.world.entity.Entity> sweepEntities=new ArrayList<>();
        int stage;long since;String request,cursor="";boolean found;BlockPos target;int treeIndex;String nav;int waterTicks;Vec3 previous;double walked,lateral;net.minecraft.world.entity.animal.golem.IronGolem golem;
        Gate(GameTestHelper h){this.h=h;r=AgentRuntime.active(h.getLevel().getServer());tools=new CodexToolService(r);origin=h.absolutePos(new BlockPos(15,5,15));}
        JsonObject obj(String s){return JsonParser.parseString(s).getAsJsonObject();}
        JsonObject call(String name,JsonObject a){var params=new JsonObject();params.addProperty("name",name);params.add("arguments",a);var req=new JsonObject();req.add("params",params);var out=tools.dispatch("tools/call",req);h.assertTrue(!out.get("isError").getAsBoolean(),name+": "+out);return out.getAsJsonObject("structuredContent");}
        void stage(int n){dev.mcai.companion.MinecraftAiCompanion.LOGGER.info("Play regression stage {} verified after {} ticks; actual position {}",stage,h.getTick()-since,r.player().position());stage=n;since=h.getTick();}
        void fixture(){
            r.onChat("TestHuman","取消");var p=r.player();
            for(int x=-6;x<=16;x++)for(int z=-6;z<=6;z++)for(int y=-3;y<=8;y++)h.getLevel().setBlock(origin.offset(x,y,z),(y<0?Blocks.STONE:Blocks.AIR).defaultBlockState(),2);
            p.getInventory().clearContent();p.setGameMode(GameType.SURVIVAL);p.setPos(Vec3.atBottomCenterOf(origin));p.setDeltaMovement(Vec3.ZERO);p.setOnGround(true);p.stopControlling();p.setYRot(-90);p.setXRot(0);p.level().getChunkSource().move(p);p.inventoryLedger.tick();target=origin.east(2);
        }
        void start(){fixture();var p=r.player();p.getInventory().setItem(0,new ItemStack(Items.WOODEN_PICKAXE));p.getInventory().setItem(10,new ItemStack(Items.OAK_LOG));p.inventoryLedger.tick();h.getLevel().setBlock(target,Blocks.SHORT_GRASS.defaultBlockState(),2);h.getLevel().setBlock(origin.below(),Blocks.DIRT.defaultBlockState(),2);h.getLevel().setBlock(origin,Blocks.TALL_GRASS.defaultBlockState(),2);h.getLevel().setBlock(origin.above(),Blocks.TALL_GRASS.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),2);
            var a=PlacementTools.xyz(target);a.addProperty("item","minecraft:crafting_table");call("place_block",a);stage(1);h.onEachTick(this::tick);
        }
        int count(Item item){int n=0;for(var stack:r.player().getInventory().getNonEquipmentItems())if(stack.is(item))n+=stack.getCount();return n;}
        void tick(){
            h.assertTrue(h.getTick()-since<700,"Timed out at play stage "+stage);var p=r.player();
            if(stage==1){var state=r.placement().status();if(state.get("phase").getAsString().equals("BLOCKED"))h.fail("Grass placement blocked "+state);if(!state.get("phase").getAsString().equals("COMPLETED"))return;
                h.assertTrue(h.getLevel().getBlockState(target).is(Blocks.CRAFTING_TABLE) && count(Items.CRAFTING_TABLE)==0,"Grass replacement/debit not physical");h.assertTrue(count(Items.OAK_LOG)==0 && count(Items.OAK_PLANKS)==0,"Workbench prerequisites did not conserve the one starting log");h.assertTrue(h.getTick()-since<=40 && h.getLevel().getBlockState(origin).isAir() && h.getLevel().getBlockState(origin.above()).isAir(),"Own tall grass was not natively cleared within two seconds");
                h.assertTrue(r.workstations.snapshot().get("knownBlocks").getAsString().contains("crafting_table"),"Placed workbench was not remembered");
                h.assertTrue(new dev.mcai.companion.agent.knowledge.WorkstationMemory(r).snapshot().get("knownBlocks").getAsString().contains("crafting_table"),"Station memory was not persisted for reload");
                var human=(net.minecraft.server.level.ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);human.setPos(p.position());human.setYRot(-90);human.setXRot(20);
                var focus=r.playerFocus.capture(human,true);h.assertTrue(focus.has("position") && focus.get("kind").getAsString().equals("block"),"Point ray did not resolve block: "+focus);
                var a=PlacementTools.xyz(target);a.addProperty("require_harvest",true);var plan=call("plan_mining",a);h.assertTrue(plan.getAsJsonArray("options").get(0).getAsJsonObject().getAsJsonObject("tool").get("item").getAsString().equals("bare_hands"),"Useless pickaxe was chosen over hand");
                request=plan.get("requestId").getAsString();call("cancel_mining",obj("{\"request_id\":\""+request+"\"}"));
                a=PlacementTools.xyz(target);a.addProperty("resource","工作台");a.addProperty("output_item","minecraft:crafting_table");a.addProperty("source","blocks");a.addProperty("count",1);a.addProperty("radius",2);
                var collection=call("plan_collection",a);request=collection.get("requestId").getAsString();String option=collection.getAsJsonArray("options").get(0).getAsJsonObject().get("optionId").getAsString();call("choose_collection",obj("{\"request_id\":\""+request+"\",\"option_id\":\""+option+"\"}"));stage(2);
            }else if(stage==2){var state=r.collection().status();if(state.get("phase").getAsString().equals("BLOCKED"))h.fail("Workbench reclaim blocked "+state);if(!state.get("phase").getAsString().equals("COMPLETED"))return;
                h.assertTrue(h.getLevel().getBlockState(target).isAir() && count(Items.CRAFTING_TABLE)==1,"Break+pickup not complete");
                h.assertTrue(!r.workstations.snapshot().get("knownBlocks").getAsString().contains("crafting_table"),"Reclaimed workbench remained in observed station memory");
                for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(Items.WOODEN_PICKAXE))h.assertTrue(p.getInventory().getItem(i).getDamageValue()==0,"Workbench wasted pickaxe durability");
                fixture();target=origin.above(4);h.getLevel().setBlock(target,Blocks.OAK_LOG.defaultBlockState(),2);
                var a=PlacementTools.xyz(target);a.addProperty("auto_tool",true);var state2=call("plan_mining",a);request=state2.get("requestId").getAsString();call("choose_mining",obj("{\"request_id\":\""+request+"\",\"option_id\":\"held-tool\"}"));stage(3);
            }else if(stage==3){if(r.mining().phase().equals("BLOCKED"))h.fail("Overhead log blocked "+r.mining().status());if(!r.mining().phase().equals("COMPLETED"))return;
                h.assertTrue(h.getLevel().getBlockState(target).isAir(),"Overhead break missing");
                target=origin.east(1);h.getLevel().setBlock(target,Blocks.BED.yellow().defaultBlockState(),2);stage(4);
            }else if(stage==4){var a=obj("{\"kind\":\"blocks\",\"filter\":\"床\",\"radius\":10,\"limit\":64}");a.addProperty("cursor",cursor);var page=call("sense",a);cursor=page.get("cursor").getAsString();for(var row:page.getAsJsonArray("results"))if(row.getAsJsonObject().get("x").getAsInt()==target.getX())found=true;if(!page.get("complete").getAsBoolean())return;
                h.assertTrue(found,"Chinese bed query missed nearby bed");h.assertTrue(!call("sense",obj("{\"kind\":\"standing_positions\"}")).getAsJsonArray("results").isEmpty(),"No move-aside alternatives on open ground");
                fixture();h.getLevel().setBlock(origin.below(),Blocks.DIRT_PATH.defaultBlockState(),2);p.setPos(p.position().add(0,-.0625,0));p.getInventory().setItem(10,new ItemStack(Items.CRAFTING_TABLE));p.inventoryLedger.tick();call("place_block",obj("{\"item\":\"minecraft:crafting_table\"}"));stage(5);
            }else if(stage==5){var state=r.placement().status();if(state.get("phase").getAsString().equals("BLOCKED"))h.fail("Nearby placement blocked "+state);if(!state.get("phase").getAsString().equals("COMPLETED"))return;
                h.assertTrue(count(Items.CRAFTING_TABLE)==0,"Automatic nearby place did not debit inventory");h.assertTrue(h.getTick()-since<=40,"Nearby placement exceeded two seconds of native ticks");fixture();
                for(int x=2;x<=7;x++)for(int z=-6;z<=6;z++)for(int y=-2;y<=-1;y++)h.getLevel().setBlock(origin.offset(x,y,z),Blocks.WATER.defaultBlockState(),2);
                var a=PlacementTools.xyz(origin.east(10));a.addProperty("x",origin.getX()+10.5);a.addProperty("z",origin.getZ()+.5);a.addProperty("target_kind","coordinates");a.addProperty("acceptance_radius",.75);a.addProperty("player_intent","Cross the pond");a.addProperty("preferred_pace","walk");var accepted=call("request_navigation",a);nav=accepted.get("requestId").getAsString();call("say",obj("{\"message\":\"我过水池。\",\"navigation_request_id\":\""+nav+"\"}"));call("plan_navigation",obj("{\"request_id\":\""+nav+"\"}"));stage(6);
            }else if(stage==6){var state=call("navigation_status",new JsonObject());String phase=state.get("phase").getAsString();if(phase.equals("PLANNING"))return;
                h.assertTrue(phase.equals("PLAN_READY"),"Pond plan failed "+state);var options=state.getAsJsonArray("routeOptions");String option=options.get(0).getAsJsonObject().get("optionId").getAsString();call("choose_navigation",obj("{\"request_id\":\""+nav+"\",\"option_id\":\""+option+"\",\"pace\":\"walk\"}"));stage(7);
            }else if(stage==7){if(p.isInWater())waterTicks++;var state=call("navigation_status",new JsonObject());String phase=state.get("phase").getAsString();if(phase.equals("EXECUTING"))return;h.assertTrue(phase.equals("COMPLETED"),"Pond traversal failed "+state);h.assertTrue(p.position().distanceTo(Vec3.atBottomCenterOf(origin.east(10)))<=.75,"Pond completion lacks final coordinates");h.assertTrue(waterTicks>=4,"Pond route bypassed swimming");
                dev.mcai.companion.MinecraftAiCompanion.LOGGER.info("Pond physically crossed: waterTicks={}, end={}",waterTicks,p.position());
                fixture();previous=p.position();var a=PlacementTools.xyz(origin.offset(10,0,3));a.addProperty("x",origin.getX()+10.5);a.addProperty("z",origin.getZ()+3.5);a.addProperty("target_kind","coordinates");a.addProperty("acceptance_radius",.5);a.addProperty("player_intent","Straight open-ground approach");a.addProperty("preferred_pace","walk");nav=call("request_navigation",a).get("requestId").getAsString();call("say",obj("{\"message\":\"我直接过来。\",\"navigation_request_id\":\""+nav+"\"}"));call("plan_navigation",obj("{\"request_id\":\""+nav+"\"}"));stage(8);
            }else if(stage==8){var state=call("navigation_status",new JsonObject());if(state.get("phase").getAsString().equals("PLANNING"))return;String option=state.getAsJsonArray("routeOptions").get(0).getAsJsonObject().get("optionId").getAsString();call("choose_navigation",obj("{\"request_id\":\""+nav+"\",\"option_id\":\""+option+"\",\"pace\":\"walk\"}"));stage(9);
            }else if(stage==9){walked+=p.position().distanceTo(previous);previous=p.position();var delta=p.position().subtract(Vec3.atBottomCenterOf(origin));lateral=Math.max(lateral,Math.abs(delta.x*3-delta.z*10)/Math.sqrt(109));var state=call("navigation_status",new JsonObject());if(state.get("phase").getAsString().equals("EXECUTING"))return;
                h.assertTrue(state.get("phase").getAsString().equals("COMPLETED") && walked<=Math.sqrt(109)*1.15 && lateral<.35,"Open-ground path curved or failed: "+state+" distance="+walked+" lateral="+lateral);
                dev.mcai.companion.MinecraftAiCompanion.LOGGER.info("Straight physical route: distance={}, lateral={}, end={}",walked,lateral,p.position());
                fixture();golem=new net.minecraft.world.entity.animal.golem.IronGolem(net.minecraft.world.entity.EntityTypes.IRON_GOLEM,h.getLevel());golem.setNoAi(true);golem.setPos(p.getX()+9,p.getY(),p.getZ());h.getLevel().addFreshEntity(golem);cursor="";found=false;stage(10);
            }else if(stage==10){var a=obj("{\"kind\":\"entities\",\"filter\":\"铁傀儡\",\"radius\":96}");a.addProperty("cursor",cursor);var state=call("sense",a);cursor=state.get("cursor").getAsString();for(var row:state.getAsJsonArray("results"))if(row.getAsJsonObject().get("uuid").getAsString().equals(golem.getUUID().toString()))found=true;if(!state.get("complete").getAsBoolean())return;h.assertTrue(found,"Chinese iron golem query missed nearby entity");golem.discard();
                fixture();
                var human=(net.minecraft.server.level.ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);
                human.setPos(p.position());human.setYRot(-90);
                var emerald=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),p.getX()+2,p.getY()+.2,p.getZ(),new ItemStack(Items.EMERALD,3));
                emerald.setNoGravity(true);emerald.setNeverPickUp();emerald.setDeltaMovement(Vec3.ZERO);h.getLevel().addFreshEntity(emerald);
                human.setXRot((float)Math.toDegrees(Math.atan2(human.getEyeY()-emerald.getBoundingBox().getCenter().y,2)));
                var mark=r.playerFocus.capture(human,true);
                h.assertTrue(mark.has("stack") && mark.getAsJsonObject("stack").get("item").getAsString().equals("minecraft:emerald") && mark.getAsJsonObject("stack").get("count").getAsInt()==3,"Pointed emerald stack was not identified: "+mark);
                human.setXRot(-90);r.playerFocus.capture(human,false);
                h.assertTrue(r.playerFocus.snapshot().asList().stream().map(JsonElement::getAsJsonObject).anyMatch(v->v.get("explicitMarker").getAsBoolean() && v.has("entityId") && v.get("entityId").getAsString().equals(emerald.getUUID().toString())),"Chat gaze discarded the explicit marker");emerald.discard();
                for(int x=-22;x<=22;x++)for(int z=-22;z<=22;z++)for(int y=0;y<5;y++)h.getLevel().setBlock(origin.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);
                for(var delta:List.of(new Vec3(20,0,0),new Vec3(-20,0,0),new Vec3(0,0,20),new Vec3(0,0,-20))){
                    var mob=new net.minecraft.world.entity.animal.golem.IronGolem(net.minecraft.world.entity.EntityTypes.IRON_GOLEM,h.getLevel());mob.setNoAi(true);mob.setNoGravity(true);mob.setPos(p.position().add(delta));h.getLevel().addFreshEntity(mob);sweepEntities.add(mob);
                }
                var sweep=call("sense",obj("{\"kind\":\"entities\",\"filter\":\"铁傀儡\",\"radius\":32,\"sweep\":true}"));cursor=sweep.get("cursor").getAsString();stage(11);
            }else if(stage==11){
                var a=obj("{\"kind\":\"entities\",\"filter\":\"铁傀儡\",\"radius\":32,\"sweep\":true}");a.addProperty("cursor",cursor);var result=call("sense",a);if(!result.get("complete").getAsBoolean())return;
                var ids=new HashSet<String>();for(var row:result.getAsJsonArray("results"))ids.add(row.getAsJsonObject().get("uuid").getAsString());
                for(var mob:sweepEntities)h.assertTrue(ids.contains(mob.getUUID().toString()),"Physical look-around missed a cardinal-direction golem: "+result);
                h.assertTrue(result.getAsJsonArray("views").size()==4 && h.getTick()-since<=60,"Look-around did not cover four actual headings promptly: "+result);
                h.assertTrue(p.position().distanceTo(Vec3.atBottomCenterOf(origin))<.05,"Look-around moved the body");
                dev.mcai.companion.MinecraftAiCompanion.LOGGER.info("Physical four-heading sweep verified: ticks={}, entities={}",h.getTick()-since,ids.size());
                sweepEntities.forEach(net.minecraft.world.entity.Entity::discard);
                fixture();for(int x=-5;x<=8;x++)for(int z=-5;z<=5;z++)h.getLevel().setBlock(origin.offset(x,-1,z),Blocks.DIRT_PATH.defaultBlockState(),2);
                p.setPos(p.position().add(0,-.0625,0));
                var drop=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),p.getX()+3,p.getY(),p.getZ(),new ItemStack(Items.CRAFTING_TABLE));drop.setNoPickUpDelay();drop.setDeltaMovement(Vec3.ZERO);h.getLevel().addFreshEntity(drop);
                var plan=call("plan_collection",obj("{\"resource\":\"工作台\",\"output_item\":\"minecraft:crafting_table\",\"source\":\"drops\",\"radius\":5,\"count\":1}"));
                var select=new JsonObject();select.addProperty("request_id",plan.get("requestId").getAsString());select.addProperty("option_id",plan.getAsJsonArray("options").get(0).getAsJsonObject().get("optionId").getAsString());call("choose_collection",select);stage(12);
            }else if(stage==12){
                var state=r.collection().status();if(state.get("phase").getAsString().equals("BLOCKED"))h.fail("Pickup on path failed: "+state);if(!state.get("phase").getAsString().equals("COMPLETED"))return;
                h.assertTrue(count(Items.CRAFTING_TABLE)==1 && Math.abs(p.getY()-(origin.getY()-.0625))<.01 && p.getX()>origin.getX()+1.5,"Path pickup lacked physical movement/inventory evidence: "+state);
                dev.mcai.companion.MinecraftAiCompanion.LOGGER.info("Path-height physical pickup verified in {} ticks",h.getTick()-since);
                fixture();for(int x=-5;x<=8;x++)for(int z=-5;z<=5;z++)h.getLevel().setBlock(origin.offset(x,-1,z),(z>=-1?Blocks.DIRT_PATH:Blocks.AIR).defaultBlockState(),2);
                p.setPos(p.position().add(0,-.0625,.395));
                var candidates=call("sense",obj("{\"kind\":\"standing_positions\"}")).getAsJsonArray("results");h.assertTrue(!candidates.isEmpty(),"Path-edge yield lost all usable sides");
                for(var value:candidates){var row=value.getAsJsonObject();double x=row.get("x").getAsDouble(),z=row.get("z").getAsDouble();h.assertTrue(Math.abs(x-Math.floor(x)-.5)<1e-7 && Math.abs(z-Math.floor(z)-.5)<1e-7,"Yield returned a unsupported grid-edge target: "+row);}
                var a=candidates.get(0).getAsJsonObject().deepCopy();a.remove("heading");a.remove("distance");a.addProperty("target_kind","coordinates");a.addProperty("acceptance_radius",.5);a.addProperty("player_intent","Yield on a path edge");a.addProperty("preferred_pace","walk");previous=p.position();nav=call("request_navigation",a).get("requestId").getAsString();call("say",obj("{\"message\":\"我让到旁边。\",\"navigation_request_id\":\""+nav+"\"}"));call("plan_navigation",obj("{\"request_id\":\""+nav+"\"}"));stage(13);
            }else if(stage==13){var state=call("navigation_status",new JsonObject());String phase=state.get("phase").getAsString();if(phase.equals("PLANNING"))return;h.assertTrue(phase.equals("PLAN_READY"),"Path-edge yield plan failed: "+state);String option=state.getAsJsonArray("routeOptions").get(0).getAsJsonObject().get("optionId").getAsString();call("choose_navigation",obj("{\"request_id\":\""+nav+"\",\"option_id\":\""+option+"\",\"pace\":\"walk\"}"));stage(14);
            }else if(stage==14){var state=call("navigation_status",new JsonObject());if(state.get("phase").getAsString().equals("EXECUTING"))return;h.assertTrue(state.get("phase").getAsString().equals("COMPLETED") && p.position().distanceTo(previous)>1,"Path-edge yield did not physically arrive: "+state);dev.mcai.companion.MinecraftAiCompanion.LOGGER.info("Path-edge yield physically verified in {} movement ticks",h.getTick()-since);h.succeed();}
        }
    }
}
