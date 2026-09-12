package dev.mcai.companion.codex;

import dev.mcai.companion.agent.AgentRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

@GameTestNamespace("mcai_companion") @GameTestDontPrefix
public final class RespawnGameTests {
    @GameTest(name="respawn_regressions",structure="forge:empty48x32x48",maxTicks=400,padding=8)
    public static void run(GameTestHelper h) {
        var r=AgentRuntime.active(h.getLevel().getServer());var old=r.player();
        var spawn=h.absolutePos(new BlockPos(6,3,6));var death=spawn.east(12);
        for(int x=-2;x<=16;x++)for(int z=-2;z<=2;z++)h.getLevel().setBlock(spawn.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
        old.setGameMode(GameType.SURVIVAL);old.setPos(Vec3.atBottomCenterOf(death));old.setDeltaMovement(Vec3.ZERO);
        old.setRespawnPosition(new ServerPlayer.RespawnConfig(new LevelData.RespawnData(GlobalPos.of(h.getLevel().dimension(),spawn),0,0),true),false);
        old.getInventory().clearContent();old.getInventory().setItem(0,new ItemStack(Items.EMERALD,3));old.inventoryLedger.tick();
        r.memory.update(null,"Old mining job","ACTIVE",false);
        old.hurtServer(h.getLevel(),old.damageSources().genericKill(),1000);
        h.assertTrue(!old.isAlive(),"Native death did not happen");
        final int[] phase={0};final Vec3[] start={null};final ServerPlayer[] first={null};
        h.onEachTick(()->{
            var p=r.player();
            if(phase[0]==0 && p!=old && p.isAlive()) {
                h.assertTrue(p.getUUID().equals(old.getUUID()) && r.server().getPlayerList().getPlayer(p.getUUID())==p,"Respawn changed identity or left old player registered");
                h.assertTrue(p.connection.player==p,"Packet listener still owns dead player");
                h.assertTrue(p.position().distanceTo(Vec3.atBottomCenterOf(spawn))<3,"Native spawn point ignored");
                h.assertTrue(r.memory.snapshot().getAsJsonObject("goal").get("status").getAsString().equals("PAUSED"),"Death revived the old objective");
                h.assertTrue(p.getInventory().isEmpty(),"Death loot duplicated into respawned inventory");
                h.assertTrue(p.getLastDeathLocation().orElseThrow().equals(GlobalPos.of(h.getLevel().dimension(),death)),"Death coordinates missing");
                int drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(death).inflate(3)).stream().filter(e->e.getItem().is(Items.EMERALD)).mapToInt(e->e.getItem().getCount()).sum();
                h.assertTrue(drops==3,"Real death drops missing or duplicated");
                for(var item:h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(death).inflate(3))) {
                    var source=dev.mcai.companion.agent.knowledge.DropProvenance.source(item,p);
                    if(item.getItem().is(Items.EMERALD))h.assertTrue(source.get("category").getAsString().equals("self_loot") && source.get("actorId").getAsString().equals(p.getUUID().toString()) && source.has("damageType"),"Death emitter/cause misattributed: "+source);
                }
                h.assertTrue(r.lifecycle().get("phase").getAsString().equals("RESPAWNED") && r.lifecycle().has("deathPoint"),"Public lifecycle lacks respawn evidence");
                p.applyControlFrame(new dev.mcai.companion.agent.body.AgentControlFrame(-90,0,1,0,false,false,false));start[0]=p.position();first[0]=p;phase[0]=1;
            } else if(phase[0]==1 && p.position().distanceTo(start[0])>1) {
                p.stopControlling();p.hurtServer(h.getLevel(),p.damageSources().genericKill(),1000);phase[0]=2;
            } else if(phase[0]==2 && p!=first[0] && p.isAlive()) {
                h.assertTrue(p.getUUID().equals(old.getUUID()) && r.navigation().status().phase()==dev.mcai.companion.agent.navigation.NavigationToolCoordinator.Phase.IDLE,"Second death retained stale controller");
                h.succeed();
            }
        });
    }
}
