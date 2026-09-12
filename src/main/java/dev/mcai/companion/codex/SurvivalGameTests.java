package dev.mcai.companion.codex;

import com.google.gson.*;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.placement.PlacementTools;
import dev.mcai.companion.vendor.numen.tools.NativeInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

/** Public-tool survival chain, checked against real menu, inventory and world changes. */
@GameTestNamespace("mcai_companion") @GameTestDontPrefix
public final class SurvivalGameTests {
    @GameTest(name="survival_regressions",structure="forge:empty48x32x48",maxTicks=3000,padding=8)
    public static void run(GameTestHelper h){new Gate(h).start();}
    private static final class Gate {
        final GameTestHelper h;final AgentRuntime r;final CodexToolService tools;final BlockPos base;
        int stage;long since;BlockPos bench,furnace,chest;
        Gate(GameTestHelper h){this.h=h;r=AgentRuntime.active(h.getLevel().getServer());tools=new CodexToolService(r);base=h.absolutePos(new BlockPos(16,5,16));}
        JsonObject obj(String s){return JsonParser.parseString(s).getAsJsonObject();}
        JsonObject call(String name,JsonObject a){var params=new JsonObject();params.addProperty("name",name);params.add("arguments",a);var req=new JsonObject();req.add("params",params);var out=tools.dispatch("tools/call",req);h.assertTrue(!out.get("isError").getAsBoolean(),name+": "+out);return out.getAsJsonObject("structuredContent");}
        int count(Item item){return NativeInventory.count(r.player().getInventory(),item);}
        void next(int n){dev.mcai.companion.MinecraftAiCompanion.LOGGER.info("Survival stage {} passed in {} ticks; inventory={}",stage,h.getTick()-since,r.player().getInventory().getNonEquipmentItems());stage=n;since=h.getTick();}
        void craft(String id,int count){var a=obj("{\"item\":\""+id+"\",\"count\":"+count+"}");var result=call("craft",a);h.assertTrue(result.get("status").getAsString().equals("COMPLETED"),"Craft blocked: "+result);}
        void place(String id,BlockPos at){var a=PlacementTools.xyz(at);a.addProperty("item",id);call("place_block",a);}
        void start(){r.onChat("TestHuman","取消");var p=r.player();
            for(int x=-8;x<=12;x++)for(int z=-8;z<=8;z++)for(int y=-3;y<=7;y++)h.getLevel().setBlock(base.offset(x,y,z),(y<0?Blocks.BEDROCK:Blocks.AIR).defaultBlockState(),2);
            p.getInventory().clearContent();p.setGameMode(GameType.SURVIVAL);p.setPos(Vec3.atBottomCenterOf(base));p.setDeltaMovement(Vec3.ZERO);p.setOnGround(true);p.stopControlling();p.level().getChunkSource().move(p);
            p.getInventory().setItem(0,new ItemStack(Items.OAK_LOG,7));p.inventoryLedger.tick();
            craft("minecraft:oak_planks",24);h.assertTrue(count(Items.OAK_LOG)==1 && count(Items.OAK_PLANKS)==24,"Log-to-plank material conservation failed");
            craft("minecraft:crafting_table",1);craft("minecraft:stick",4);
            h.assertTrue(count(Items.CRAFTING_TABLE)==1 && count(Items.STICK)==4 && count(Items.OAK_PLANKS)==18,"Backpack recipes had incorrect material flow");
            bench=base.east(2);place("minecraft:crafting_table",bench);next(1);h.onEachTick(this::tick);
        }
        boolean placed(BlockPos at,net.minecraft.world.level.block.Block block){var s=r.placement().status();h.assertTrue(!s.get("phase").getAsString().equals("BLOCKED"),"Placement blocked "+s);return s.get("phase").getAsString().equals("COMPLETED") && h.getLevel().getBlockState(at).is(block);}
        void tick(){h.assertTrue(h.getTick()-since<1000,"Survival stage timeout "+stage);var p=r.player();
            if(stage==1){if(!placed(bench,Blocks.CRAFTING_TABLE))return;craft("minecraft:wooden_pickaxe",1);h.assertTrue(count(Items.WOODEN_PICKAXE)==1 && count(Items.OAK_PLANKS)==15 && count(Items.STICK)==2,"Workbench recipe failed to debit native grid");
                for(int x=0;x<4;x++)for(int z=0;z<2;z++)h.getLevel().setBlock(base.offset(x+1,0,z+3),Blocks.STONE.defaultBlockState(),2);
                call("gather",obj("{\"resource\":\"minecraft:cobblestone\",\"radius\":150,\"count\":8}"));next(2);
            }else if(stage==2){var state=r.gather().status();h.assertTrue(!state.get("phase").getAsString().equals("BLOCKED"),"Stone gathering blocked "+state);if(!state.get("phase").getAsString().equals("COMPLETED"))return;h.assertTrue(count(Items.COBBLESTONE)==8,"Collection did not produce eight actual pickups");
                // The gate moves only its fixture body back; crafting still requires native reach.
                p.setPos(Vec3.atBottomCenterOf(base));p.setDeltaMovement(Vec3.ZERO);p.setOnGround(true);p.stopControlling();craft("minecraft:furnace",1);h.assertTrue(count(Items.COBBLESTONE)==0 && count(Items.FURNACE)==1,"Furnace recipe conservation failed");furnace=base.north(2);place("minecraft:furnace",furnace);next(3);
            }else if(stage==3){if(!placed(furnace,Blocks.FURNACE))return;p.setPos(Vec3.atBottomCenterOf(base.south(5)));p.setDeltaMovement(Vec3.ZERO);p.setOnGround(true);var a=PlacementTools.xyz(furnace);a.addProperty("item","minecraft:oak_log");a.addProperty("fuel","minecraft:oak_planks");var state=call("smelt",a);h.assertTrue(state.get("step").getAsString().equals("APPROACH"),"Furnace did not start its checked native approach");next(4);
            }else if(stage==4){var state=r.survival().status();h.assertTrue(!state.get("phase").getAsString().equals("BLOCKED"),"Smelting blocked "+state);if(!state.get("phase").getAsString().equals("COMPLETED"))return;
                h.assertTrue(count(Items.CHARCOAL)==1 && count(Items.OAK_LOG)==0 && count(Items.OAK_PLANKS)==14,"Native smelting/pickup conservation failed");h.assertTrue(h.getTick()-since>=200,"Furnace tick duration was bypassed");craft("minecraft:chest",1);chest=base.west(2);place("minecraft:chest",chest);next(5);
            }else if(stage==5){if(!placed(chest,Blocks.CHEST))return;var menu=call("interact_block",PlacementTools.xyz(chest));int from=-1;for(var row:menu.getAsJsonArray("slots")){var slot=row.getAsJsonObject();if(slot.get("playerSide").getAsBoolean() && slot.get("item").getAsString().equals("minecraft:charcoal"))from=slot.get("slot").getAsInt();}h.assertTrue(from>=0,"Charcoal inventory slot missing");
                call("transfer_items",obj("{\"moves\":[{\"from\":"+from+",\"to\":0,\"count\":1}]}"));h.assertTrue(count(Items.CHARCOAL)==0 && p.containerMenu.slots.get(0).getItem().is(Items.CHARCOAL),"Chest deposit did not move actual stack");
                call("transfer_items",obj("{\"moves\":[{\"from\":0}]}"));h.assertTrue(count(Items.CHARCOAL)==1 && p.containerMenu.slots.get(0).getItem().isEmpty(),"Chest withdrawal did not conserve stack");call("close_container",new JsonObject());
                var remembered=r.workstations.snapshot().get("knownBlocks").getAsString();h.assertTrue(remembered.contains("crafting_table") && remembered.contains("furnace") && remembered.contains("chest"),"Used station memory missing "+remembered);
                p.getInventory().setItem(8,new ItemStack(Items.BREAD));p.getFoodData().setFoodLevel(8);p.getFoodData().setSaturation(0);p.inventoryLedger.tick();call("eat",obj("{\"item\":\"minecraft:bread\"}"));next(6);
            }else if(stage==6){var state=r.survival().status();if(state.get("phase").getAsString().equals("EXECUTING"))return;h.assertTrue(state.get("phase").getAsString().equals("COMPLETED") && count(Items.BREAD)==0 && p.getFoodData().getFoodLevel()>8,"Native food did not finish "+state);next(7);h.succeed();}
        }
    }
}
