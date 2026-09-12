package dev.mcai.companion.agent.navigation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.mcai.companion.agent.navigation.NavigationWorldSnapshot.BodyResources;
import dev.mcai.companion.agent.navigation.NavigationWorldSnapshot.Bounds;
import dev.mcai.companion.agent.navigation.NavigationWorldSnapshot.Cell;
import dev.mcai.companion.agent.navigation.NavigationWorldSnapshot.GridPosition;
import dev.mcai.companion.agent.navigation.NavigationWorldSnapshot.Position;

final class AnytimeNavigationPlannerTest {
    private static final Cell AIR = new Cell(false, false, false, false, false, false, 0.0);
    private static final Cell SOLID = new Cell(true, false, false, false, false, false, 0.0);

    @Test
    void compositeSearchFindsAReachableStanceBehindAnUnreachableNearestChoice() {
        for(int direction:new int[]{-1,1}) {
            Bounds bounds=new Bounds(-8,0,-8,8,5,8);var cells=fill(bounds,AIR);
            for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)cells.put(new GridPosition(x,0,z),SOLID);
            for(int x=1;x<=3;x++)for(int y=1;y<=4;y++)for(int z=-1;z<=1;z++)cells.put(new GridPosition(direction*x,y,z),SOLID);
            var inaccessible=destination(direction*2+.5,1,.5);
            var reachable=destination(direction*6+.5,1,4.5);
            var world=snapshot(bounds,new GridPosition(0,1,0),inaccessible,0,cells);
            var result=planner().planAny(UUID.randomUUID(),world,java.util.List.of(inaccessible,reachable));
            assertEquals(reachable,result.destination());
            var option=result.options().getFirst();var end=option.steps().getLast();
            assertEquals(reachable.x(),end.x(),.5);assertEquals(reachable.z(),end.z(),.5);
            assertEquals(0,option.supportBlocksRequired());assertEquals(0,option.estimatedHealthLost());
            assertTrue(option.steps().stream().noneMatch(step->cells.get(new GridPosition((int)Math.floor(step.x()),(int)Math.floor(step.y()),(int)Math.floor(step.z()))).equals(SOLID)));
        }
    }

    @Test
    void denseSnapshotRemainsDetachedFromItsMutableCapture() {
        Bounds bounds=new Bounds(-400,50,100,-350,75,140);var cells=fill(bounds,AIR);
        var snapshot=snapshot(bounds,new GridPosition(-375,60,120),destination(-360.5,60,120.5),0,cells);
        cells.clear();assertEquals(51*26*41,snapshot.cells().size());
        assertEquals(AIR,snapshot.cell(new GridPosition(-355,70,130)));
        assertThrows(UnsupportedOperationException.class,()->snapshot.cells().clear());
    }

    @Test
    void fractionalSupportUsesItsActualHeightAndRejectsLowCeilings() {
        Bounds bounds=new Bounds(0,0,0,6,4,0);var cells=fill(bounds,AIR);
        for(int x=0;x<=6;x++)cells.put(new GridPosition(x,0,0),SOLID);
        var slab=new Cell(true,false,false,false,false,false,0,false,true,
                java.util.List.of(new NavigationWorldSnapshot.CollisionBox(0,0,0,1,.5,1)));
        for(int x=1;x<=6;x++)cells.put(new GridPosition(x,1,0),slab);
        var snap=snapshot(bounds,new GridPosition(0,1,0),destination(6.5,1.5,.5),0,cells);
        var option=planner().plan(UUID.randomUUID(),snap).options().getFirst();
        assertEquals(1.5,option.steps().getLast().y());assertEquals(0,option.supportBlocksRequired());
        cells.put(new GridPosition(3,3,0),SOLID);
        var blocked=snapshot(bounds,new GridPosition(0,1,0),destination(6.5,1.5,.5),0,cells);
        assertFalse(blocked.bodyClear(3.5,1.5,.5));
        assertThrows(AnytimeNavigationPlanner.NoRouteException.class,()->planner().plan(UUID.randomUUID(),blocked));
    }

    @Test
    void plannedEndpointLeavesRoomInsideThePhysicalAcceptanceRadius() {
        var plan=planner().plan(UUID.randomUUID(),flatSnapshot(new Position(2.5,1,.5),6.5,2));
        for(var option:plan.options()) {
            var end=option.steps().getLast();
            assertTrue(Math.abs(end.x()-6.5)<2,"Boundary endpoint can leave a braking body outside the radius");
        }
    }

    @Test
    void restingDynamicTargetVelocityCannotMoveItsGoalUnderground() {
        Bounds bounds = new Bounds(0, 0, 0, 6, 3, 0);
        var cells = fill(bounds, AIR);
        for (int x = 0; x <= 6; x++) cells.put(new GridPosition(x, 0, 0), SOLID);
        var target = new NavigationPlan.ResolvedDestination("minecraft:overworld", 5.5, 1, .5,
                .5, true, "dropped-stack", OptionalDouble.empty(),
                Optional.of(new NavigationPlan.TargetMotion(0, 0, -.08, 0)));
        var plan = planner().plan(UUID.randomUUID(), snapshot(bounds, new GridPosition(0, 1, 0), target, 0, cells));
        var end = plan.options().getFirst().steps().getLast();
        assertEquals(5.5, end.x(), 1e-9);
        assertEquals(1, end.y(), 1e-9);
        assertEquals(0, plan.options().getFirst().supportBlocksRequired());
    }

    @Test
    void partialApproachRequiresOptInAndNeverReplacesTheOriginalDestination() {
        Bounds bounds = new Bounds(0, 0, 0, 6, 8, 0);
        var cells = fill(bounds, AIR);
        for (int x = 0; x <= 6; x++) cells.put(new GridPosition(x, 0, 0), SOLID);
        var snapshot = snapshot(bounds, new GridPosition(0, 1, 0), destination(6.5, 6, .5), 0, cells);
        assertThrows(AnytimeNavigationPlanner.NoRouteException.class,
                () -> planner().plan(UUID.randomUUID(), snapshot));
        var plan = planner().plan(UUID.randomUUID(), snapshot, true);
        assertEquals(6, plan.destination().y());
        assertEquals(1, plan.partialDestination().orElseThrow().y());
        assertEquals(6.5, plan.partialDestination().orElseThrow().x());
        assertFalse(plan.options().getFirst().steps().isEmpty());
        var closest = snapshot(bounds, new GridPosition(6, 1, 0), destination(6.5, 6, .5), 0, cells);
        assertThrows(AnytimeNavigationPlanner.NoRouteException.class,
                () -> planner().plan(UUID.randomUUID(), closest, true));
    }

    @Test
    void outOfBoundsUnknownCellIsNeverTreatedAsFloor() {
        Bounds bounds = new Bounds(0, 0, 0, 1, 2, 0);
        Map<GridPosition, Cell> cells = fill(bounds, AIR);
        NavigationWorldSnapshot snapshot = snapshot(
                bounds,
                new GridPosition(0, 0, 0),
                destination(1.5, 0.0, 0.5),
                0,
                cells
        );

        assertThrows(AnytimeNavigationPlanner.NoRouteException.class,
                () -> planner().plan(UUID.randomUUID(), snapshot));
    }

    @Test
    void routeThatNeedsMoreSupportBlocksThanInventoryIsNotReturned() {
        Bounds bounds = new Bounds(0, 0, 0, 5, 3, 0);
        Map<GridPosition, Cell> cells = fill(bounds, AIR);
        cells.put(new GridPosition(0, 0, 0), SOLID);
        cells.put(new GridPosition(5, 0, 0), SOLID);
        NavigationWorldSnapshot snapshot = snapshot(
                bounds,
                new GridPosition(0, 1, 0),
                destination(5.5, 1.0, 0.5),
                1,
                cells
        );

        assertThrows(AnytimeNavigationPlanner.NoRouteException.class,
                () -> planner().plan(UUID.randomUUID(), snapshot));
    }

    @Test
    void roundedStartInsideGoalStillRequiresPhysicalApproach() {
        NavigationPlan plan = planner().plan(UUID.randomUUID(), flatSnapshot(
                new Position(2.1, 1.0, 0.5), 4.4, 2.0));
        for (RouteOption option : plan.options()) {
            assertFalse(option.steps().isEmpty(), "Body remains 2.3 blocks from target");
            assertTrue(option.distanceBlocks() > 0.0);
            assertEquals(2.5, option.steps().getLast().x(), 1.0e-9);
        }
    }

    @Test
    void exactStartInsideGoalDoesNotNeedToReachCellCenter() {
        NavigationPlan plan = planner().plan(UUID.randomUUID(), flatSnapshot(
                new Position(2.9, 1.0, 0.5), 4.85, 2.0));
        assertTrue(plan.options().stream().allMatch(option -> option.steps().isEmpty()));
    }

    @Test
    void armorDoesNotMakeLethalFallFeasible() {
        for (double armorMultiplier : new double[]{1.0, 0.2}) {
            assertThrows(AnytimeNavigationPlanner.NoRouteException.class,
                    () -> planner().plan(UUID.randomUUID(), cliffSnapshot(
                            3.0, armorMultiplier, 3.0, 1.0)));
        }
    }

    @Test
    void ordinaryArmorDoesNotChangeFallEstimate() {
        for (double armorMultiplier : new double[]{1.0, 0.2}) {
            NavigationPlan plan = planner().plan(UUID.randomUUID(), cliffSnapshot(
                    20.0, armorMultiplier, 3.0, 1.0));
            assertEquals(6.0, plan.options().getFirst().estimatedHealthLost(), 1.0e-9);
        }
    }

    @Test
    void fallAttributesAreAccountedForIndependentlyOfArmor() {
        NavigationPlan plan = planner().plan(UUID.randomUUID(), cliffSnapshot(
                20.0, 0.2, 1.0, 2.0));
        assertEquals(16.0, plan.options().getFirst().estimatedHealthLost(), 1.0e-9);
    }

    @Test
    void routeCannotUseHazardousFootingRejectedByTheFollower() {
        Bounds bounds = new Bounds(0, 0, 0, 2, 3, 0);
        Map<GridPosition, Cell> cells = fill(bounds, AIR);
        cells.put(new GridPosition(0, 0, 0), SOLID);
        cells.put(new GridPosition(1, 0, 0),
                new Cell(true, false, false, false, true, false, 0));
        cells.put(new GridPosition(2, 0, 0), SOLID);
        cells.put(new GridPosition(1, 3, 0), SOLID); // Low ceiling prevents jumping over it.
        assertThrows(AnytimeNavigationPlanner.NoRouteException.class,
                () -> planner().plan(UUID.randomUUID(), snapshot(bounds,
                        new GridPosition(0, 1, 0), destination(2.5, 1, .5), 0, cells)));
    }

    @Test
    void exactIntegerTargetWithSmallRadiusGetsAnExactFinalStep() {
        Bounds b = new Bounds(0, 0, 0, 5, 3, 2);
        var cells = fill(b, AIR);
        for (int x = 0; x <= 5; x++) for (int z = 0; z <= 2; z++) cells.put(new GridPosition(x, 0, z), SOLID);
        NavigationPlan plan = planner().plan(UUID.randomUUID(), snapshot(b, new GridPosition(2, 1, 0),
                destination(4, 1, 1), 0, cells));
        var end = plan.options().getFirst().steps().getLast();
        assertEquals(4.0, end.x(), 1e-9);
    }

    @Test
    void emptyInventoryCanJumpAcrossTwoAirBlocks() {
        Bounds b = new Bounds(0, 0, 0, 3, 4, 0);
        var cells = fill(b, AIR);
        cells.put(new GridPosition(0, 0, 0), SOLID);
        cells.put(new GridPosition(3, 0, 0), SOLID);
        var plan = planner().plan(UUID.randomUUID(), snapshot(b, new GridPosition(0, 1, 0),
                destination(3.5, 1, .5), 0, cells));
        assertTrue(plan.options().getFirst().steps().stream().anyMatch(p -> p.action() == RouteOption.Action.GAP_JUMP));
        assertEquals(0, plan.options().getFirst().supportBlocksRequired());
    }

    @Test
    void ladderHasVerticalEdgesWithoutInventedFloorBlocks() {
        Bounds b = new Bounds(0, 0, 0, 0, 7, 0);
        var cells = fill(b, AIR);
        cells.put(new GridPosition(0, 0, 0), SOLID);
        for (int y = 1; y <= 5; y++) cells.put(new GridPosition(0, y, 0),
                new Cell(true, false, false, false, false, false, 0, true, false));
        var plan = planner().plan(UUID.randomUUID(), snapshot(b, new GridPosition(0, 1, 0),
                destination(.5, 6, .5), 0, cells));
        assertTrue(plan.options().getFirst().steps().stream().allMatch(p -> p.action() == RouteOption.Action.CLIMB));
    }

    private static NavigationWorldSnapshot flatSnapshot(Position exactStart, double x, double radius) {
        Bounds bounds = new Bounds(0, 0, 0, 6, 3, 0);
        Map<GridPosition, Cell> cells = fill(bounds, AIR);
        for (int floorX = 0; floorX <= 6; floorX++) {
            cells.put(new GridPosition(floorX, 0, 0), SOLID);
        }
        return new NavigationWorldSnapshot(0, 0, "minecraft:overworld", bounds,
                new GridPosition(2, 1, 0), exactStart,
                new NavigationPlan.ResolvedDestination("minecraft:overworld", x, 1, .5,
                        radius, false, "test", OptionalDouble.empty(), Optional.empty()),
                new BodyResources(20, 0, 20, 5, 0, 1, 3, 1, 0, true, true), cells);
    }

    private static NavigationWorldSnapshot cliffSnapshot(
            double health, double armorMultiplier, double safeFallDistance, double fallMultiplier) {
        Bounds bounds = new Bounds(0, 0, 0, 1, 12, 0);
        Map<GridPosition, Cell> cells = fill(bounds, AIR);
        cells.put(new GridPosition(0, 9, 0), SOLID);
        cells.put(new GridPosition(1, 0, 0), SOLID);
        return new NavigationWorldSnapshot(0, 0, "minecraft:overworld", bounds,
                new GridPosition(0, 10, 0), new Position(.5, 10, .5),
                destination(1.5, 1.0, .5),
                new BodyResources(health, 0, 20, 5, 0, armorMultiplier,
                        safeFallDistance, fallMultiplier, 0, true, true), cells);
    }

    private static AnytimeNavigationPlanner planner() {
        return new AnytimeNavigationPlanner(
                new NavigationPlannerConfig(5, 1, 2, 10_000, 500, 12));
    }

    private static NavigationWorldSnapshot snapshot(
            Bounds bounds,
            GridPosition start,
            NavigationPlan.ResolvedDestination destination,
            int supportBlocks,
            Map<GridPosition, Cell> cells
    ) {
        return new NavigationWorldSnapshot(
                0L,
                0L,
                "minecraft:overworld",
                bounds,
                start,
                new Position(start.x() + 0.5, start.y(), start.z() + 0.5),
                destination,
                new BodyResources(20.0, 0.0, 20, 5.0, 0.0, 1.0, 3.0, 1.0,
                        supportBlocks, true, true),
                cells
        );
    }

    private static NavigationPlan.ResolvedDestination destination(double x, double y, double z) {
        return new NavigationPlan.ResolvedDestination(
                "minecraft:overworld", x, y, z, 0.5, false, "test",
                OptionalDouble.empty(), Optional.empty());
    }

    private static Map<GridPosition, Cell> fill(Bounds bounds, Cell cell) {
        Map<GridPosition, Cell> cells = new HashMap<>();
        for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                    cells.put(new GridPosition(x, y, z), cell);
                }
            }
        }
        return cells;
    }
}
