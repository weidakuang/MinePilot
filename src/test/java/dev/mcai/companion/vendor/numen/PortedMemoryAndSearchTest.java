package dev.mcai.companion.vendor.numen;

import dev.mcai.companion.vendor.numen.scan.RingSpiral;
import dev.mcai.companion.vendor.numen.memory.WorkBlockMemory;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PortedMemoryAndSearchTest {
    @TempDir Path root;
    @Test void searchIncludesTheFarthestAxisAtUnequalChunkOffsets(){
        assertEquals(10,dev.mcai.companion.vendor.numen.scan.SearchGeometry.maxRing(8,0,150));
        assertEquals(10,dev.mcai.companion.vendor.numen.scan.SearchGeometry.maxRing(-8,-16,150));
        assertEquals(0,dev.mcai.companion.vendor.numen.scan.SearchGeometry.maxRing(8,8,1));
    }
    @Test void ringsCoverEveryCellExactlyOnce(){
        var visited=new HashSet<String>();
        for(int ring=0;ring<=7;ring++)for(int i=0;i<RingSpiral.perimeter(ring);i++){
            var offset=RingSpiral.offset(ring,i);
            assertEquals(ring,Math.max(Math.abs(offset[0]),Math.abs(offset[1])));
            assertTrue(visited.add(offset[0]+","+offset[1]));
        }
        assertEquals(225,visited.size());
    }
    @Test void stationsReloadAndSeparateDimensions(){
        var overworld=new WorkBlockMemory(root.resolve("overworld.json"));
        overworld.record("visualworkbench:minecraft/crafting_table",new BlockPos(1,64,3));
        var loaded=new WorkBlockMemory(root.resolve("overworld.json"));
        assertTrue(loaded.formatXml(null,p->false).contains("crafting_table"));
        assertEquals("",new WorkBlockMemory(root.resolve("nether.json")).formatXml(null,p->false));
        assertFalse(WorkBlockMemory.isTracked("minecraft:oak_planks"));
    }
    @Test void corruptMemoryIsPreservedAfterNewPlacements() throws Exception {
        var file=root.resolve("corrupt.json");Files.writeString(file,"broken-json");
        var memory=new WorkBlockMemory(file);memory.record("minecraft:furnace",new BlockPos(1,2,3));
        assertFalse(memory.writable());assertEquals("broken-json",Files.readString(file));
    }
    @Test void recencyBoundSurvivesReload(){
        var file=root.resolve("bounded.json");var memory=new WorkBlockMemory(file);
        for(int i=0;i<20;i++)memory.record("minecraft:furnace",new BlockPos(i,64,0));
        var loaded=new WorkBlockMemory(file);assertEquals(16,loaded.describeAll().size());
        assertFalse(loaded.describeAll().contains("furnace @ 0,64,0"));
    }
}
