package dev.mcai.companion.codex;

import com.google.gson.*;
import dev.mcai.companion.MinecraftAiCompanion;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.knowledge.StructurePerception;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

/** Native generated structure blocks/records in a real world, queried via public MCP dispatch. */
@GameTestNamespace("mcai_companion") @GameTestDontPrefix
public final class StructureGameTests {
    @GameTest(name="structure_record_regressions",structure="forge:empty48x32x48",maxTicks=40000,padding=8)
    public static void run(GameTestHelper helper) {
        if (!Boolean.getBoolean("minepilot.structureTest")) { helper.fail("Structure gate not explicitly selected"); return; }
        new Gate(helper).start();
    }
    private static final class Gate {
        final GameTestHelper h;
        final AgentRuntime runtime;
        final CodexToolService tools;
        final long began = System.nanoTime();
        final JsonArray evidence = new JsonArray();
        BoundingBox nearBox;
        String cursor = "";
        int phase;
        Vec3 expectedPosition;
        double maxCallMillis;
        Gate(GameTestHelper h) { this.h=h; runtime=AgentRuntime.active(h.getLevel().getServer()); tools=new CodexToolService(runtime); }

        JsonObject call(JsonObject args) {
            var params=new JsonObject(); params.addProperty("name","sense"); params.add("arguments",args);
            var request=new JsonObject(); request.add("params",params);
            long start=System.nanoTime(); var result=tools.dispatch("tools/call",request);
            maxCallMillis=Math.max(maxCallMillis,(System.nanoTime()-start)/1_000_000.0);
            return result;
        }
        JsonObject args(int radius, String filter) {
            var a=new JsonObject(); a.addProperty("kind","structures"); a.addProperty("radius",radius);
            a.addProperty("filter",filter); a.addProperty("cursor",cursor); a.addProperty("limit",1); return a;
        }
        void start() {
            var origin=h.absolutePos(new BlockPos(8,2,8));
            var near=generate(ChunkPos.containing(origin.east(40)));
            generate(ChunkPos.containing(origin.east(300)));
            nearBox=near.getPieces().getFirst().getBoundingBox();
            runtime.server().getCommands().performPrefixedCommand(runtime.server().createCommandSourceStack(),"forceload add "+(nearBox.minX()-16)+" "+(nearBox.minZ()-16)+" "+(nearBox.maxX()+16)+" "+(nearBox.maxZ()+16));
            long sandstone=0;
            for (int x=nearBox.minX();x<=nearBox.maxX();x++) for (int z=nearBox.minZ();z<=nearBox.maxZ();z++)
                for (int y=Math.max(h.getLevel().getMinY(),nearBox.minY());y<=nearBox.maxY();y++) {
                    var state=h.getLevel().getBlockState(new BlockPos(x,y,z));
                    if (state.is(Blocks.SANDSTONE) || state.is(Blocks.CUT_SANDSTONE) || state.is(Blocks.CHISELED_SANDSTONE)) sandstone++;
                }
            h.assertTrue(sandstone>100,"Fixture did not physically generate a pyramid");
            setBody(nearBox.minX()-20,nearBox.minY()+8,nearBox.minZ()+5);
            var rejected=call(args(151,"minecraft:desert_pyramid"));
            h.assertTrue(rejected.get("isError").getAsBoolean(),"Radius above 150 was accepted");
            var unknown=call(args(96,"unregistered_structure_id"));
            h.assertTrue(unknown.get("isError").getAsBoolean(),"Unknown type reported false absence");
            h.addCleanup(ignored -> runtime.player().stopControlling());
            h.onEachTick(this::tick);
        }
        StructureStart generate(ChunkPos chunkPos) {
            var level=h.getLevel(); var registry=level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            var holder=registry.get(Identifier.parse("minecraft:desert_pyramid")).orElseThrow();
            var generator=level.getChunkSource().getGenerator();
            var start=holder.value().generate(holder,level.dimension(),level.registryAccess(),generator,
                    generator.getBiomeSource(),level.getChunkSource().randomState(),level.getStructureManager(),
                    level.getSeed(),chunkPos,0,level,biome -> true);
            h.assertTrue(start.isValid(),"Native structure generation failed");
            var box=start.getBoundingBox();
            for (int x=box.minX()>>4;x<=box.maxX()>>4;x++) for(int z=box.minZ()>>4;z<=box.maxZ()>>4;z++) {
                var c=new ChunkPos(x,z);level.getChunk(x,z);
                start.placeInChunk(level,level.structureManager(),generator,level.getRandom(),
                        new BoundingBox(c.getMinBlockX(),level.getMinY(),c.getMinBlockZ(),c.getMaxBlockX(),level.getMaxY(),c.getMaxBlockZ()),c);
            }
            // Fixture-only native index insertion, matching generation's stored start/reference data.
            level.getChunk(chunkPos.x(),chunkPos.z()).setStartForStructure(holder.value(),start);
            for (var piece:start.getPieces()) {
                var b=piece.getBoundingBox();
                for (int x=b.minX()>>4;x<=b.maxX()>>4;x++) for (int z=b.minZ()>>4;z<=b.maxZ()>>4;z++)
                    level.getChunk(x,z).addReferenceForStructure(holder.value(),chunkPos.pack());
            }
            return start;
        }
        void setBody(double x,double y,double z) {
            var p=runtime.player(); var feet=BlockPos.containing(x,y,z);
            h.getLevel().setBlockAndUpdate(feet.below(),Blocks.STONE.defaultBlockState());
            h.getLevel().setBlockAndUpdate(feet,Blocks.AIR.defaultBlockState());
            h.getLevel().setBlockAndUpdate(feet.above(),Blocks.AIR.defaultBlockState());
            p.getInventory().clearContent(); p.setGameMode(GameType.ADVENTURE);p.setPos(x,y,z);
            p.setDeltaMovement(Vec3.ZERO);p.setOnGround(true);p.stopControlling();p.level().getChunkSource().move(p);
            expectedPosition=p.position();cursor="";
        }
        void tick() {
            h.assertTrue(System.nanoTime()-began<30_000_000_000L,"Structure gate timed out at "+phase);
            h.assertTrue(runtime.player().position().distanceTo(expectedPosition)<.05,"A structure query moved the body");
            h.assertTrue(runtime.player().getInventory().isEmpty(),"A structure query changed inventory");
            if(h.getTick()<40)return;
            var raw=call(args(phase==3?10:96,phase==4?"村庄":"minecraft:desert_pyramid"));
            h.assertTrue(!raw.get("isError").getAsBoolean(),"Query failed: "+raw);
            var result=raw.getAsJsonObject("structuredContent");cursor=result.get("cursor").getAsString();
            if (!result.get("complete").getAsBoolean()) return;
            h.assertTrue(result.get("scheduledChunks").getAsInt()==0,"Search scheduled terrain generation: "+result);
            if(result.get("unavailableChunksOrRecords").getAsInt()>0)h.assertTrue(!result.get("coverageComplete").getAsBoolean(),"Unloaded terrain was falsely reported complete");
            var rows=result.getAsJsonArray("results");
            if (phase==0 || phase==2 || phase==6) {
                h.assertTrue(rows.size()==1,"Nearby native pyramid missing or outside pyramid leaked: "+result);
                var row=rows.get(0).getAsJsonObject();
                var point=new Vec3(row.get("x").getAsDouble(),row.get("y").getAsDouble(),row.get("z").getAsDouble());
                h.assertTrue(point.distanceTo(expectedPosition)<=96 && row.get("distance").getAsDouble()<=96,
                        "Structure result escaped the sphere");
                h.assertTrue(row.get("source").getAsString().equals("server_structure_record")
                        && !row.get("safeToStandVerified").getAsBoolean() && !row.get("currentBlocksVerified").getAsBoolean(),
                        "Structure record was presented as visual/standing proof");
                if (phase==2) h.assertTrue(result.get("referencedStartChunks").getAsInt()>0,
                        "Structure part near the boundary did not exercise a start outside the search sphere");
                if (phase==6) h.assertTrue(Math.abs(row.get("distance").getAsDouble()-96)<.001,
                        "Exact 96-block boundary did not match");
            } else h.assertTrue(rows.isEmpty(),"Outside range or bell falsely became a structure: "+result);
            evidence.add(result.deepCopy());
            if (phase==0) setBody(nearBox.minX()-97,nearBox.minY()+8,nearBox.minZ()+5);
            if (phase==1) setBody(nearBox.maxX()+1+95,nearBox.minY()+8,nearBox.minZ()+5);
            if (phase==2) setBody(nearBox.minX()-20,nearBox.minY()+8,nearBox.minZ()+5);
            if (phase==3) {
                setBody(nearBox.minX()-20,nearBox.minY()+8,nearBox.minZ()+5);
                h.getLevel().setBlockAndUpdate(BlockPos.containing(expectedPosition).east(2),Blocks.BELL.defaultBlockState());
            }
            if (phase==4) setBody(nearBox.minX()+5,nearBox.maxY()+98,nearBox.minZ()+5);
            if (phase==5) setBody(nearBox.maxX()+1+96,nearBox.minY()+8,nearBox.minZ()+5);
            if (phase++==6) {
                try {
                    var path=h.getLevel().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("structure-physical-evidence.json");
                    var out=new JsonObject();out.add("queries",evidence);out.addProperty("maxCallMillis",maxCallMillis);
                    out.addProperty("nativePyramidPhysicallyGenerated",true);out.addProperty("worldMutationDuringQueries",false);
                    java.nio.file.Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(out));
                } catch (java.io.IOException failure) { throw new RuntimeException(failure); }
                MinecraftAiCompanion.LOGGER.info("Structure gate physically verified: native pyramid, 95/97-block cutoff, vertical cutoff, no bell false positive, unchanged body/inventory; maxCallMs={}",maxCallMillis);
                h.succeed();
            }
        }
    }
}
