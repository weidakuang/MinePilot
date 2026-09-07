import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

import dev.mcai.companion.agent.navigation.AnytimeNavigationPlanner;
import dev.mcai.companion.agent.navigation.NavigationPlan;
import dev.mcai.companion.agent.navigation.NavigationPlannerConfig;
import dev.mcai.companion.agent.navigation.NavigationWorldSnapshot;
import dev.mcai.companion.agent.navigation.NavigationWorldSnapshot.BodyResources;
import dev.mcai.companion.agent.navigation.NavigationWorldSnapshot.Bounds;
import dev.mcai.companion.agent.navigation.NavigationWorldSnapshot.Cell;
import dev.mcai.companion.agent.navigation.NavigationWorldSnapshot.GridPosition;
import dev.mcai.companion.agent.navigation.NavigationWorldSnapshot.Position;

/** Regression checks for the review findings; not physical acceptance tests. */
final class NavigationReviewProbe {
    private static final Cell AIR = new Cell(false, false, false, false, false, false, 0);
    private static final Cell SOLID = new Cell(true, false, false, false, false, false, 0);

    public static void main(String[] args) {
        Bounds flat = new Bounds(0, 0, 0, 6, 3, 0);
        Map<GridPosition, Cell> flatCells = fill(flat);
        for (int x = 0; x <= 6; x++) {
            flatCells.put(new GridPosition(x, 0, 0), SOLID);
        }
        NavigationPlan arrival = plan(flat, new GridPosition(2, 1, 0),
                new Position(2.1, 1, .5),
                destination(4.4, 1, 2), flatCells, 20, 1);
        int steps = arrival.options().getFirst().steps().size();
        System.out.println("ARRIVAL: actual distance=" + Math.abs(4.4 - 2.1)
                + ", radius=2, steps=" + steps
                + ", feasible=" + arrival.options().getFirst().feasibleNow());
        if (steps == 0) {
            throw new AssertionError("Body outside the goal must receive a physical approach");
        }

        Bounds cliff = new Bounds(0, 0, 0, 1, 12, 0);
        Map<GridPosition, Cell> cliffCells = fill(cliff);
        cliffCells.put(new GridPosition(0, 9, 0), SOLID);
        cliffCells.put(new GridPosition(1, 0, 0), SOLID);
        NavigationPlan armored = plan(cliff, new GridPosition(0, 10, 0),
                new Position(.5, 10, .5), destination(1.5, 1, .5), cliffCells, 20, .2);
        System.out.println("FALL: health=20, drop=9, predicted damage="
                + armored.options().getFirst().estimatedHealthLost()
                + ", feasible=" + armored.options().getFirst().feasibleNow()
                + ", steps=" + armored.options().getFirst().steps());
        if (Math.abs(armored.options().getFirst().estimatedHealthLost() - 6.0) > 1e-9) {
            throw new AssertionError("Ordinary armor must not reduce fall damage");
        }
        for (double multiplier : new double[]{1.0, .2}) {
            try {
                plan(cliff, new GridPosition(0, 10, 0), new Position(.5, 10, .5),
                        destination(1.5, 1, .5), cliffCells, 3, multiplier);
                throw new AssertionError("Lethal fall should be rejected regardless of armor");
            } catch (AnytimeNavigationPlanner.NoRouteException expected) {
                System.out.println("CONTROL: lethal fall rejected with armor multiplier=" + multiplier);
            }
        }
    }

    private static NavigationPlan plan(Bounds bounds, GridPosition start, Position exactStart,
            NavigationPlan.ResolvedDestination destination,
            Map<GridPosition, Cell> cells, double health, double damageMultiplier) {
        return new AnytimeNavigationPlanner(
                new NavigationPlannerConfig(5, 1, 2, 10_000, 500, 12))
                .plan(UUID.randomUUID(), new NavigationWorldSnapshot(
                        0, 0, "minecraft:overworld", bounds, start, exactStart, destination,
                        new BodyResources(health, 0, 20, 5, 0,
                                damageMultiplier, 3, 1, 0, true, true), cells));
    }

    private static NavigationPlan.ResolvedDestination destination(
            double x, double y, double radius) {
        return new NavigationPlan.ResolvedDestination(
                "minecraft:overworld", x, y, .5, radius, false, "review",
                OptionalDouble.empty(), Optional.empty());
    }

    private static Map<GridPosition, Cell> fill(Bounds bounds) {
        Map<GridPosition, Cell> cells = new HashMap<>();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                    cells.put(new GridPosition(x, y, z), AIR);
                }
            }
        }
        return cells;
    }
}
