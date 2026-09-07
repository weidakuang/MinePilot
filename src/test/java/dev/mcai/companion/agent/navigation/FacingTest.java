package dev.mcai.companion.agent.navigation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import dev.mcai.companion.agent.knowledge.WorldPerception;
class FacingTest {
    @Test void nearbyFirstBlockScanCoversEveryCellExactlyOnce(){
        var seen=new java.util.HashSet<net.minecraft.core.BlockPos>();
        for(int i=0;i<9261;i++){
            var p=WorldPerception.shellOffset(i);assertTrue(seen.add(p));
            assertTrue(Math.abs(p.getX())<=10 && Math.abs(p.getY())<=10 && Math.abs(p.getZ())<=10);
        }
        assertEquals(9261,seen.size());
    }
    @Test void compassAndRelativeSidesAreUnambiguous(){
        assertEquals(0,WorldPerception.normalize(360));
        assertEquals(180,WorldPerception.relativeHeading(90,"left"));
        assertEquals(0,WorldPerception.relativeHeading(90,"right"));
        assertEquals(270,WorldPerception.relativeHeading(90,"back"));
        assertEquals(2,WorldPerception.difference(359,1));
    }
}
