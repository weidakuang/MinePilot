package dev.mcai.companion.agent.mining;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.mcai.companion.agent.mining.ExcavationPlanner.*;
import java.util.*;
class ExcavationPlannerTest {
    final Tool pick=new Tool("pick","pickaxe",0,100,1,10,true);
    final Voxel air=new Voxel(true,false,true,false,"air",List.of());
    final Voxel rock=new Voxel(false,true,true,true,"stone",List.of(pick,new Tool("bare_hands","bare_hands",1,-1,0,150,false)));
    Map<Pos,Voxel> corridor(){var cells=new HashMap<Pos,Voxel>();for(int x=0;x<5;x++){cells.put(new Pos(x,-1,0),rock);cells.put(new Pos(x,0,0),x==0?air:rock);cells.put(new Pos(x,1,0),x==0?air:rock);}return cells;}
    Snapshot snapshot(boolean access){return new Snapshot(new Pos(0,0,0),corridor(),List.of(new Pos(4,0,0)),List.of(),access,false,true,false,30,30);}
    @Test void accessIsExplicitAndPreviewDoesNotMutateSnapshot(){var input=snapshot(false);assertTrue(alternatives(input).isEmpty());var allowed=snapshot(true);var before=new HashMap<>(allowed.cells());var plans=alternatives(allowed);assertFalse(plans.isEmpty());assertEquals(before,allowed.cells());assertTrue(plans.getFirst().accessBlocks()>0);assertTrue(plans.getFirst().wear().get("pick")>0);}
    @Test void rejectsUnbreakableTargetsAndInsufficientBudget(){var cells=corridor();cells.put(new Pos(4,0,0),new Voxel(false,true,true,false,"bedrock",List.of()));assertTrue(alternatives(new Snapshot(new Pos(0,0,0),cells,List.of(new Pos(4,0,0)),List.of(),true,false,true,false,30,30)).isEmpty());var s=snapshot(true);assertTrue(alternatives(new Snapshot(s.origin(),s.cells(),s.targets(),s.supports(),true,false,true,false,1,30)).isEmpty());}
    @Test void approachOnlyRetainsOreAndDoesNotRequireMiningTool(){var cells=corridor();cells.put(new Pos(1,0,0),new Voxel(false,true,true,false,"ore",List.of()));var plans=alternatives(new Snapshot(new Pos(0,0,0),cells,List.of(new Pos(1,0,0)),List.of(),false,false,true,true,30,30));assertEquals(1,plans.size());assertTrue(plans.getFirst().steps().isEmpty());}
    @Test void elevatedWorkDisclosesSupportedScaffoldingAndAnUnblockedReturn(){
        var cells=new HashMap<Pos,Voxel>();for(int x=-4;x<=6;x++)for(int y=-1;y<=8;y++)for(int z=-4;z<=4;z++)cells.put(new Pos(x,y,z),y<0?rock:air);
        var target=new Pos(3,7,0);cells.put(target,rock);var origin=new Pos(0,0,0);
        assertTrue(alternatives(new Snapshot(origin,cells,List.of(target),List.of(new Stock("support","cobble",32,2)),false,true,true,false,30,100,true)).isEmpty());
        var plans=alternatives(new Snapshot(origin,cells,List.of(target),List.of(new Stock("support","cobble",32,5)),false,true,true,false,30,100,true));assertFalse(plans.isEmpty());
        var plan=plans.getFirst();assertTrue(plan.limitation().isEmpty());assertTrue(plan.materials().get("support")>0);
        var current=origin;var actual=new HashMap<>(cells);int placed=0;
        for(var step:plan.steps()){
            if(step.action().equals("support")){
                assertTrue(actual.get(step.position()).empty());assertNotEquals(target,step.position());
                var q=step.position();assertTrue(List.of(q.add(1,0,0),q.add(-1,0,0),q.add(0,1,0),q.add(0,-1,0),q.add(0,0,1),q.add(0,0,-1)).stream().anyMatch(p->actual.containsKey(p) && actual.get(p).full()),"A support cannot float");
                actual.put(q,rock);placed++;
            }else if(step.action().equals("break"))actual.put(step.position(),air);
            else {assertEquals(current,step.from());assertTrue(actual.get(step.position().add(0,-1,0)).full());assertTrue(actual.get(step.position()).empty() && actual.get(step.position().add(0,1,0)).empty());current=step.position();}
        }
        assertEquals(origin,current);assertEquals(placed,plan.materials().get("support"));assertTrue(actual.get(target).empty());
    }
}
