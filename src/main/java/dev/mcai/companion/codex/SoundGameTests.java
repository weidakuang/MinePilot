package dev.mcai.companion.codex;

import com.google.gson.*;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.knowledge.SoundPerception;
import dev.mcai.companion.agent.navigation.NavigationFollower;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

/** Real server sound delivery, queried through the public MCP dispatcher. */
@GameTestNamespace("mcai_companion") @GameTestDontPrefix
public final class SoundGameTests {
    @GameTest(name="sound_tool_regressions",structure="forge:empty48x32x48",maxTicks=400,padding=8)
    public static void run(GameTestHelper helper) {
        if (!Boolean.getBoolean("minepilot.soundTest")) {helper.fail("Sound gate not explicitly selected"); return;}
        new Gate(helper).start();
    }
    private static final class Gate {
        final GameTestHelper h;
        final AgentRuntime runtime;
        final CodexToolService tools;
        final JsonArray evidence = new JsonArray();
        Vec3 start;
        Zombie zombie;
        long cursor;
        long began;
        int phase;
        Gate(GameTestHelper h) {this.h=h; runtime=AgentRuntime.active(h.getLevel().getServer()); tools=new CodexToolService(runtime);}
        JsonObject call(String name, JsonObject args) {
            var params=new JsonObject(); params.addProperty("name",name); params.add("arguments",args);
            var request=new JsonObject(); request.add("params",params);
            var result=tools.dispatch("tools/call",request);
            h.assertTrue(!result.get("isError").getAsBoolean(),"Public sound tool failed: "+result);
            return result.getAsJsonObject("structuredContent");
        }
        JsonObject listen(long after, int limit) {
            var args=new JsonObject();args.addProperty("after_sequence",after);args.addProperty("limit",limit);return call("listen",args);
        }
        List<JsonObject> named(JsonObject data,String name) {
            var result=new ArrayList<JsonObject>();
            for(var value:data.getAsJsonArray("sounds")) {
                var row=value.getAsJsonObject();if(row.get("sound").getAsString().equals("minecraft:"+name))result.add(row);
            }
            return result;
        }
        void sound(String id, SoundSource source, Vec3 position, float volume) {
            var sound=SoundEvent.createVariableRangeEvent(Identifier.withDefaultNamespace(id));
            h.getLevel().playSeededSound(null,position.x,position.y,position.z,Holder.direct(sound),source,volume,1,123);
        }
        void start() {
            var origin=h.absolutePos(new BlockPos(22,2,22));
            for(int x=-18;x<=18;x++)for(int z=-18;z<=18;z++){
                h.getLevel().setBlockAndUpdate(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState());
                for(int y=0;y<4;y++)h.getLevel().setBlockAndUpdate(origin.offset(x,y,z),Blocks.AIR.defaultBlockState());
            }
            var p=runtime.player();p.stopControlling();p.setGameMode(GameType.ADVENTURE);p.getInventory().clearContent();
            p.setPos(Vec3.atBottomCenterOf(origin));p.setDeltaMovement(Vec3.ZERO);p.setOnGround(true);
            p.setYRot(NavigationFollower.headingToMinecraftYaw(0));p.stopControlling();p.level().getChunkSource().move(p);start=p.position();
            // A real wall must not turn hearing into the custom visual sensor.
            for(int y=0;y<3;y++)for(int z=-1;z<=1;z++)h.getLevel().setBlockAndUpdate(origin.offset(2,y,z),Blocks.STONE.defaultBlockState());
            zombie=new Zombie(h.getLevel());zombie.setNoAi(true);zombie.setNoGravity(true);zombie.setInvulnerable(true);zombie.setPos(start.add(4,0,0));h.getLevel().addFreshEntity(zombie);
            h.addCleanup(ignored->zombie.discard());began=h.getTick();h.onEachTick(this::tick);
        }
        void tick() {
            var p=runtime.player();
            h.assertTrue(p.position().distanceTo(start)<.01 && p.getInventory().isEmpty(),"Listening changed body or inventory");
            if(phase==0 && h.getTick()-began>=3) {
                cursor=listen(0,64).get("latestSequence").getAsLong();
                for(int i=0;i<8;i++){
                    double a=Math.toRadians(i*45);
                    sound("entity.cow.ambient",SoundSource.AMBIENT,start.add(Math.sin(a)*5,1,-Math.cos(a)*5),1);
                }
                zombie.playSound(SoundEvents.ZOMBIE_AMBIENT,1,1);
                sound("block.chest.open",SoundSource.BLOCKS,start.add(13,0,0),1);
                sound("block.chest.open",SoundSource.BLOCKS,start.add(20,0,0),2);
                sound("entity.sheep.ambient",SoundSource.AMBIENT,start.add(0,0,13),1);
                sound("entity.pig.ambient",SoundSource.NEUTRAL,start.add(17,0,0),1);
                sound("music.game",SoundSource.MUSIC,start.add(1,0,0),1);
                var chestPos=BlockPos.containing(start.add(0,0,4));h.getLevel().setBlockAndUpdate(chestPos,Blocks.CHEST.defaultBlockState());
                ((ChestBlockEntity)h.getLevel().getBlockEntity(chestPos)).startOpen(p);
                phase=1;began=h.getTick();return;
            }
            if(phase==1 && h.getTick()>began) {
                var data=listen(cursor,64);if(named(data,"entity.cow.ambient").size()<8)return;
                evidence.add(data);
                var directions=new HashSet<String>();for(var row:named(data,"entity.cow.ambient"))directions.add(row.get("direction").getAsString());
                h.assertTrue(directions.size()==8,"Eight body-relative directions not received: "+directions);
                var zombies=named(data,"entity.zombie.ambient");h.assertTrue(zombies.size()==1,"Real zombie playback missing behind wall");
                var row=zombies.getFirst();h.assertTrue(row.get("subtitle").getAsString().equals("僵尸：低吼"),"Caption is not native Chinese");
                h.assertTrue(row.getAsJsonObject("source").get("confidence").getAsString().equals("position_match_unconfirmed"),"Positional packet fabricated certain attribution");
                h.assertTrue(row.getAsJsonObject("source").get("uuid").getAsString().equals(zombie.getUUID().toString()),"Wrong candidate");
                var chests=named(data,"block.chest.open");h.assertTrue(chests.size()==2,"Chest range/volume filtering mismatch");
                var nearChest=chests.stream().filter(c->c.get("distanceBlocks").getAsDouble()<12).findFirst().orElseThrow();
                var loudChest=chests.stream().filter(c->c.get("distanceBlocks").getAsDouble()>16).findFirst().orElseThrow();
                h.assertTrue(nearChest.getAsJsonObject("source").get("name").getAsString().equals("箱子"),"Missing native block name");
                h.assertTrue(loudChest.get("subtitleRangeBlocks").getAsDouble()==24,"Loud chest lost native scaled range");
                h.assertTrue(named(data,"entity.sheep.ambient").size()==1,"Ordinary 16-block sound incorrectly limited to chest range");
                h.assertTrue(named(data,"entity.pig.ambient").isEmpty() && named(data,"music.game").isEmpty(),"Undelivered or unsubtitled sound leaked");
                var page=listen(cursor,2);h.assertTrue(page.get("truncated").getAsBoolean() && page.getAsJsonArray("sounds").size()==2,"Sound response is unbounded");
                h.assertTrue(listen(page.get("nextSequence").getAsLong(),64).getAsJsonArray("sounds").size()==data.getAsJsonArray("sounds").size()-2,"Cursor lost captions");
                cursor=data.get("latestSequence").getAsLong();
                h.getLevel().playSeededSound(null,zombie,Holder.direct(SoundEvents.ZOMBIE_AMBIENT),SoundSource.HOSTILE,1,1,123);
                h.getLevel().playSeededSound(null,p,Holder.direct(SoundEvents.PLAYER_BURP),SoundSource.PLAYERS,1,1,123);
                var turn=new JsonObject();turn.addProperty("heading",90);call("turn",turn);phase=2;return;
            }
            if(phase==2 && runtime.turnPhase().equals("COMPLETED")) {
                var data=listen(cursor,64);var zombies=named(data,"entity.zombie.ambient");if(zombies.isEmpty())return;
                evidence.add(data);var row=zombies.getFirst();
                h.assertTrue(row.getAsJsonObject("source").get("confidence").getAsString().equals("entity_packet"),"Entity packet failed exact attribution");
                h.assertTrue(row.get("direction").getAsString().equals("front"),"Sound direction did not rotate with real body");
                var burp=named(data,"entity.player.burp").getFirst();
                h.assertTrue(burp.get("direction").isJsonNull() && burp.get("directionText").getAsString().equals("自身"),"Own sound fabricated a horizontal direction");
                var playerSound=burp.getAsJsonObject("source");
                h.assertTrue(playerSound.get("kind").getAsString().equals("player") && playerSound.get("label").getAsString().equals("Player:MinePilot") && playerSound.get("uuid").getAsString().equals(p.getUUID().toString()),"Player name/UUID missing");
                clockBoundaries();
                try {
                    var root=p.level().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
                    java.nio.file.Files.writeString(root.resolve("sound-tool-evidence.json"),new GsonBuilder().setPrettyPrinting().create().toJson(evidence));
                }catch(java.io.IOException error){throw new IllegalStateException(error);}
                dev.mcai.companion.MinecraftAiCompanion.LOGGER.info("Sound gate: real packet delivery, native captions/ranges, 8 directions, wall hearing, entity/player/block sources and read-only body verified; snapshots saved");
                phase=3;h.succeed();
            }
        }
        void clockBoundaries() {
            // Boundary tests use a fake monotonic clock; the network cases above do not.
            var now=new AtomicLong();var hearing=new SoundPerception(runtime.player(),now::get);
            var ears=runtime.player().getEyePosition();
            var packet=new ClientboundSoundPacket(Holder.direct(SoundEvents.ZOMBIE_AMBIENT),SoundSource.HOSTILE,ears.x+1,ears.y,ears.z,1,1,123);
            hearing.receive(packet);now.set(SoundPerception.DISPLAY_NANOS);h.assertTrue(hearing.query(0,64).getAsJsonArray("sounds").size()==1,"Caption expired too early");
            hearing.receive(packet);h.assertTrue(hearing.query(0,64).getAsJsonArray("sounds").size()==1,"Repeated playback duplicated same position");
            now.addAndGet(SoundPerception.DISPLAY_NANOS+1);h.assertTrue(hearing.query(0,64).getAsJsonArray("sounds").isEmpty(),"Expired caption leaked");
            int sent=0;
            for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)if(x*x+z*z<=42 && sent<130) {
                hearing.receive(new ClientboundSoundPacket(Holder.direct(SoundEvents.ZOMBIE_AMBIENT),SoundSource.HOSTILE,ears.x+x*2,ears.y,ears.z+z*2,1,1,sent++));
            }
            h.assertTrue(hearing.query(0,64).get("totalMatched").getAsInt()==128,"Caption memory not bounded");
            h.assertTrue(hearing.query(1,64).get("capacityHistoryLost").getAsBoolean(),"Overflow hidden from cursor consumer");
        }
    }
}
