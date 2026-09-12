package dev.mcai.companion.codex;

import com.google.gson.*;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.knowledge.CompanionMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

/** Native water strokes, loaded-radius boundary and persistent conversational memory. */
@GameTestNamespace("mcai_companion") @GameTestDontPrefix
public final class ResourceWaterGameTests {
    @GameTest(name="resource_water_regressions",structure="forge:empty48x32x48",maxTicks=10000,padding=8)
    public static void run(GameTestHelper h){new Gate(h).start();}
    private static final class Gate {
        final GameTestHelper h;final AgentRuntime r;final BlockPos base;int stage;long since;String cursor="";float health;boolean submerged;String nav;int waterTicks;final CodexToolService tools;
        Gate(GameTestHelper h){this.h=h;r=AgentRuntime.active(h.getLevel().getServer());base=h.absolutePos(new BlockPos(15,8,15));tools=new CodexToolService(r);}
        JsonObject call(String name,JsonObject args){var params=new JsonObject();params.addProperty("name",name);params.add("arguments",args);var request=new JsonObject();request.add("params",params);var result=tools.dispatch("tools/call",request);h.assertTrue(!result.get("isError").getAsBoolean(),name+": "+result);return result.getAsJsonObject("structuredContent");}
        JsonObject obj(String text){return JsonParser.parseString(text).getAsJsonObject();}
        void body(BlockPos pos){var p=r.player();p.setGameMode(GameType.SURVIVAL);p.setPos(Vec3.atBottomCenterOf(pos));p.setDeltaMovement(Vec3.ZERO);p.stopControlling();p.level().getChunkSource().move(p);}
        void start(){r.onChat("TestHuman","stop");var l=h.getLevel();for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)for(int y=-2;y<=8;y++)l.setBlock(base.offset(x,y,z),(y<0?Blocks.BEDROCK:Blocks.AIR).defaultBlockState(),2);
            body(base);r.player().setOnGround(true);r.memory.chat("user","TestHuman","Remember the camp next to the river.");r.memory.update("The player prefers a riverside camp.","Build the camp","ACTIVE",false);
            var restored=new CompanionMemory(r).snapshot();h.assertTrue(restored.get("summary").getAsString().contains("riverside") && restored.getAsJsonArray("recent").toString().contains("next to the river"),"Dialogue did not survive memory reload");
            var station=base.east(2);l.setBlock(station,Blocks.CRAFTING_TABLE.defaultBlockState(),2);r.workstations.placed(station);l.setBlock(station,Blocks.AIR.defaultBlockState(),2);h.assertTrue(!r.workstations.snapshot().get("knownBlocks").getAsString().contains("crafting_table"),"Destroyed station remained available");
            // Loading is fixture setup. The search itself is forbidden from generating terrain.
            r.server().getCommands().performPrefixedCommand(r.server().createCommandSourceStack(),"forceload add "+(base.getX()+128)+" "+(base.getZ()-16)+" "+(base.getX()+160)+" "+(base.getZ()+16));
            for(int x=130;x<=160;x++)for(int z=-1;z<=1;z++)for(int y=-1;y<=2;y++)l.setBlock(base.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);
            l.setBlock(base.offset(149,0,0),Blocks.ANCIENT_DEBRIS.defaultBlockState(),2);l.setBlock(base.offset(151,0,0),Blocks.ANCIENT_DEBRIS.defaultBlockState(),2);
            h.onEachTick(this::tick);since=h.getTick();
        }
        void next(int n){dev.mcai.companion.MinecraftAiCompanion.LOGGER.info("Resource/water stage {} passed in {} ticks",stage,h.getTick()-since);stage=n;since=h.getTick();}
        void pool(boolean roof){var l=h.getLevel();for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=0;y<=5;y++){
                boolean wall=Math.abs(x)==4 || Math.abs(z)==4;var block=wall?Blocks.GLASS:y<5?Blocks.WATER:roof && !(x>=2 && z>=-1 && z<=1)?Blocks.GLASS:Blocks.AIR;l.setBlock(base.offset(x,y,z),block.defaultBlockState(),2);}
            body(base.above());r.player().setAirSupply(100);health=r.player().getHealth();submerged=false;
        }
        void tick(){h.assertTrue(h.getTick()-since<(stage==7?8000:900),"Resource/water timeout stage "+stage+" at "+r.player().position()+" breath="+r.breath.status()+" gather="+r.gather().status());var p=r.player();
            if(stage==0){if(h.getTick()-since<40)return;var query=r.resources.query("minecraft:ancient_debris",150,64,cursor);cursor=query.get("cursor").getAsString();if(!query.get("complete").getAsBoolean())return;
                boolean near=false,far=false;for(var value:query.getAsJsonArray("results")){var row=value.getAsJsonObject();near|=row.get("x").getAsInt()==base.getX()+149;far|=row.get("x").getAsInt()==base.getX()+151;}
                h.assertTrue(near && !far,"150-block search boundary failed: "+query);h.assertTrue(!r.mining().reachable(base.offset(149,0,0)),"Perception changed native reach");pool(false);next(1);
            }else if(stage==1 || stage==2){submerged|=p.isEyeInFluid(FluidTags.WATER);if(!submerged || p.isEyeInFluid(FluidTags.WATER))return;
                h.assertTrue(p.getY()>base.getY()+3,"Head cleared without a real ascent");h.assertTrue(p.getHealth()>=health,"Lost health before native breath recovery");
                if(stage==1){next(6);}else{
                    for(int x=-6;x<=16;x++)for(int z=-6;z<=6;z++)for(int y=-5;y<=7;y++)h.getLevel().setBlock(base.offset(x,y,z),(y>=0?Blocks.AIR:x>=2 && x<=7 && y>=-4?Blocks.WATER:Blocks.BEDROCK).defaultBlockState(),2);
                    body(base);p.setOnGround(true);var a=new JsonObject();a.addProperty("x",base.getX()+10.5);a.addProperty("y",base.getY());a.addProperty("z",base.getZ()+.5);a.addProperty("target_kind","coordinates");a.addProperty("acceptance_radius",.75);a.addProperty("player_intent","Cross deep water and exit onto the far bank");a.addProperty("preferred_pace","walk");nav=call("request_navigation",a).get("requestId").getAsString();call("say",obj("{\"message\":\"Crossing deep water\",\"navigation_request_id\":\""+nav+"\"}"));call("plan_navigation",obj("{\"request_id\":\""+nav+"\"}"));next(3);
                }
            }else if(stage==6){h.assertTrue(p.getY()>base.getY()+3,"Idle body sank back into deep water after surfacing");if(h.getTick()-since<120)return;h.assertTrue(p.getAirSupply()>=240,"Idle surface float did not restore air");pool(true);r.onChat("TestHuman","停下");h.assertTrue(r.memory.paused(),"Stop did not pause autonomy");next(2);
            }else if(stage==3){var state=call("navigation_status",new JsonObject());String phase=state.get("phase").getAsString();if(phase.equals("PLANNING"))return;h.assertTrue(phase.equals("PLAN_READY"),"Deep-water route planning failed: "+state);String option=state.getAsJsonArray("routeOptions").get(0).getAsJsonObject().get("optionId").getAsString();call("choose_navigation",obj("{\"request_id\":\""+nav+"\",\"option_id\":\""+option+"\",\"pace\":\"walk\"}"));next(4);
            }else if(stage==4){if(p.isInWater())waterTicks++;var state=call("navigation_status",new JsonObject());if(state.get("phase").getAsString().equals("EXECUTING"))return;h.assertTrue(state.get("phase").getAsString().equals("COMPLETED") && !p.isInWater() && waterTicks>=4 && p.position().distanceTo(Vec3.atBottomCenterOf(base.east(10)))<.8,"Deep-water traversal or bank exit failed: "+state);
                // A four-high natural bank has no walking route. The resource job
                // must use native breaks to form steps, then collect real drops.
                for(int x=-6;x<=18;x++)for(int z=-6;z<=6;z++)for(int y=-1;y<=10;y++)h.getLevel().setBlock(base.offset(x,y,z),(y<0?Blocks.DIRT:x>=1 && y<4?Blocks.STONE:Blocks.AIR).defaultBlockState(),2);
                for(int x=12;x<16;x++)h.getLevel().setBlock(base.offset(x,4,0),Blocks.COAL_ORE.defaultBlockState(),2);
                body(base);p.setOnGround(true);p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(Items.STONE_PICKAXE));p.getInventory().setItem(1,new ItemStack(Items.WOODEN_AXE));
                call("gather",obj("{\"resource\":\"minecraft:coal_ore\",\"output_item\":\"minecraft:coal\",\"count\":4,\"radius\":30}"));next(7);
            }else if(stage==7){var state=call("gather_status",new JsonObject());if(state.has("travelPhase") && state.get("travelPhase").getAsString().equals("PLANNING"))java.util.concurrent.locks.LockSupport.parkNanos(5_000_000);if(state.get("phase").getAsString().equals("EXECUTING"))return;
                h.assertTrue(state.get("phase").getAsString().equals("COMPLETED") && state.get("verifiedInventoryIncrease").getAsInt()>=4 && p.getY()>=base.getY()+4,"Natural bank access/collection failed: "+state);
                int cleared=state.getAsJsonObject("access").get("clearedBlocks").getAsInt();h.assertTrue(cleared>0 && cleared<=24,"Bank recovery bypassed bounded physical breaks");next(5);h.succeed();}
        }
    }
}
