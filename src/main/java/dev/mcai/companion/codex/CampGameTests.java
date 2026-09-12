package dev.mcai.companion.codex;

import com.google.gson.*;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.placement.PlacementTools;
import dev.mcai.companion.agent.knowledge.CompanionMemory;
import dev.mcai.companion.vendor.numen.tools.NativeInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

/** Real construction, interruption, blueprint persistence and inventory conservation. */
@GameTestNamespace("mcai_companion") @GameTestDontPrefix
public final class CampGameTests {
    @GameTest(name="camp_regressions",structure="forge:empty48x32x48",maxTicks=9000,padding=8)
    public static void run(GameTestHelper h){new Gate(h).start();}
    private static final class Gate {
        final GameTestHelper h;final AgentRuntime r;final BlockPos base;boolean interrupted;long since;int planksBeforeResume;
        Gate(GameTestHelper h){this.h=h;r=AgentRuntime.active(h.getLevel().getServer());base=h.absolutePos(new BlockPos(15,5,15));}
        int planks(){return NativeInventory.count(r.player().getInventory(),Items.OAK_PLANKS);}
        void start(){r.onChat("TestHuman","stop");for(int x=-10;x<=12;x++)for(int z=-10;z<=12;z++)for(int y=-2;y<=8;y++)h.getLevel().setBlock(base.offset(x,y,z),(y<0?Blocks.BEDROCK:Blocks.AIR).defaultBlockState(),2);
            var p=r.player();p.setGameMode(GameType.SURVIVAL);p.setPos(Vec3.atBottomCenterOf(base.offset(2,0,6)));p.setDeltaMovement(Vec3.ZERO);p.setOnGround(true);p.stopControlling();p.getInventory().clearContent();p.level().getChunkSource().move(p);
            p.getInventory().setItem(0,new ItemStack(Items.OAK_PLANKS,64));p.getInventory().setItem(1,new ItemStack(Items.OAK_PLANKS,20));p.getInventory().setItem(2,new ItemStack(Items.CRAFTING_TABLE));p.getInventory().setItem(3,new ItemStack(Items.FURNACE));p.getInventory().setItem(4,new ItemStack(Items.CHEST));p.inventoryLedger.tick();
            var args=PlacementTools.xyz(base);args.addProperty("auto_gather",true);var params=new JsonObject();params.addProperty("name","build_camp");params.add("arguments",args);var request=new JsonObject();request.add("params",params);var result=new CodexToolService(r).dispatch("tools/call",request);h.assertTrue(!result.get("isError").getAsBoolean(),"Public camp entry failed: "+result);since=h.getTick();h.onEachTick(this::tick);
        }
        void tick(){var state=r.camp().status();
            if(h.getTick()%200==0){var ps=r.placement().status();dev.mcai.companion.MinecraftAiCompanion.LOGGER.info("Camp progress tick {} step {} placement {} target {} body {} planks {}",h.getTick(),state.get("step"),ps.get("step"),ps.get("currentTarget"),r.player().position(),planks());}
h.assertTrue(!state.get("phase").getAsString().equals("BLOCKED"),"Camp blocked: "+state+" placement="+r.placement().status());
            if(!interrupted && state.get("step").getAsString().equals("PLACE") && planks()<74){r.onChat("TestHuman","停下");h.assertTrue(!r.camp().active() && !r.placement().executing(),"Stop left a camp child running");
                var saved=new CompanionMemory(r).camp();h.assertTrue(saved.getAsJsonArray("targets").size()==83 && saved.get("phase").getAsString().equals("PAUSED"),"Interrupted blueprint did not persist");
                var restored=new dev.mcai.companion.agent.survival.CampCoordinator(r).status();h.assertTrue(restored.get("totalCells").getAsInt()==83 && restored.get("phase").getAsString().equals("PAUSED"),"Fresh coordinator lost the saved blueprint");
                // Simulate missing unused stock across interruption, with an actual
                // replacement log that must be crafted before construction resumes.
                int remove=4;for(int slot=0;slot<36 && remove>0;slot++){var stack=r.player().getInventory().getItem(slot);if(stack.is(Items.OAK_PLANKS)){int n=Math.min(remove,stack.getCount());stack.shrink(n);remove-=n;}}
                r.player().getInventory().setItem(15,new ItemStack(Items.OAK_LOG));r.player().inventoryLedger.tick();planksBeforeResume=planks();r.camp().resume();interrupted=true;return;}
            if(state.get("phase").getAsString().equals("COMPLETED")){
                h.assertTrue(interrupted && planksBeforeResume>0,"Did not exercise interruption");h.assertTrue(planks()==0 && NativeInventory.count(r.player().getInventory(),Items.OAK_LOG)==0,"Construction/restocking duplicated or omitted material debits; remaining="+planks());
                h.assertTrue(state.get("verifiedCells").getAsInt()==83,"Unverified build reported complete");
                for(int x=0;x<5;x++)for(int z=0;z<5;z++){h.assertTrue(h.getLevel().getBlockState(base.offset(x,0,z)).is(Blocks.OAK_PLANKS),"Missing floor");h.assertTrue(h.getLevel().getBlockState(base.offset(x,3,z)).is(Blocks.OAK_PLANKS),"Missing roof");}
                h.assertTrue(h.getLevel().getBlockState(base.offset(2,1,4)).isAir() && h.getLevel().getBlockState(base.offset(2,2,4)).isAir(),"Entrance was sealed");
                dev.mcai.companion.MinecraftAiCompanion.LOGGER.info("Camp native construction/resume passed in {} ticks; 84 planks consumed including external bootstrap table",h.getTick()-since);h.succeed();}
        }
    }
}
