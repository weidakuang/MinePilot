package dev.mcai.companion.agent.navigation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.mcai.companion.agent.body.MinePilotServerPlayer;
import dev.mcai.companion.agent.navigation.NavigationWorldSnapshot.Bounds;
import dev.mcai.companion.agent.navigation.NavigationWorldSnapshot.Cell;
import dev.mcai.companion.agent.navigation.NavigationWorldSnapshot.GridPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Copies a bounded collision/hazard oracle snapshot on the server thread. */
public final class NavigationSnapshotBuilder {
    private final CaptureConfig config;

    public NavigationSnapshotBuilder(CaptureConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public NavigationWorldSnapshot capture(MinePilotServerPlayer player,NavigationPlan.ResolvedDestination destination,long revision) {
        var capture=begin(player,destination,revision);capture.advance(Long.MAX_VALUE);return capture.finish();
    }

    public Capture begin(
            MinePilotServerPlayer player,
            NavigationPlan.ResolvedDestination destination,
            long worldRevision
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(destination, "destination");
        if (!player.level().getServer().isSameThread()) {
            throw new IllegalStateException("World snapshots must be captured on the server thread");
        }
        ServerLevel level = player.level();
        String currentDimension = level.dimension().identifier().toString();
        if (!currentDimension.equals(destination.dimension())) {
            throw new SnapshotUnavailableException(
                    "Cross-dimension movement requires a verified portal route");
        }

        GridPosition start = new GridPosition(
                player.getBlockX(), player.getBlockY(), player.getBlockZ());
        GridPosition goal = new GridPosition(
                (int) Math.floor(destination.x()),
                (int) Math.floor(destination.y()),
                (int) Math.floor(destination.z()));
        Bounds bounds = bounds(level, start, goal);
        return new Capture(player, destination, worldRevision, bounds, start);
    }

    /** Capture in bounded tick slices; immutable geometry is then searched on a worker. */
    public final class Capture {
        private final MinePilotServerPlayer player;
        private final ServerLevel level;
        private final NavigationPlan.ResolvedDestination destination;
        private final long worldRevision, observedTick;
        private final String dimension;
        private final Bounds bounds;
        private final GridPosition start;
        private final NavigationWorldSnapshot.Position exactStart;
        private final NavigationWorldSnapshot.BodyResources resources;
        private final List<Threat> threats;
        private final Map<GridPosition,Cell> cells=new HashMap<>();
        private final BlockPos.MutableBlockPos cursor=new BlockPos.MutableBlockPos();
        private long index;
        private Capture(MinePilotServerPlayer player, NavigationPlan.ResolvedDestination destination,long revision,Bounds bounds,GridPosition start) {
            this.dimension=player.level().dimension().identifier().toString();
            this.player=player;this.level=player.level();this.destination=destination;this.worldRevision=revision;this.bounds=bounds;this.start=start;
            observedTick=level.getGameTime();exactStart=new NavigationWorldSnapshot.Position(player.getX(),player.getY(),player.getZ());
            resources=resources(player);threats=captureThreats(level,player,bounds);
        }
        public boolean advance(long budgetNanos) {
            if(!level.getServer().isSameThread())throw new IllegalStateException("Capture requires server thread");
            if(index>=volume(bounds))return true;
            var budget=dev.mcai.companion.agent.concurrent.MainThreadBudget.of(level.getServer());
            long began=System.nanoTime(),deadline=budgetNanos==Long.MAX_VALUE ? Long.MAX_VALUE : budget.deadline(budgetNanos);int width=bounds.maxX()-bounds.minX()+1,depth=bounds.maxZ()-bounds.minZ()+1;
            try { while(index<volume(bounds) && System.nanoTime()<deadline) {
                long n=index++;
                readCell(bounds.minX()+(int)(n%width),bounds.minY()+(int)(n/(width*depth)),bounds.minZ()+(int)((n/width)%depth));
            }} finally {budget.record(began);}
            return index>=volume(bounds);
        }
        private void readCell(int x,int y,int z) {
                    cursor.set(x, y, z);
                    GridPosition position = new GridPosition(x, y, z);
                    if (!level.isLoaded(cursor)
                            || !level.getWorldBorder().isWithinBounds(cursor)) {
                        cells.put(position, Cell.UNKNOWN);
                        return;
                    }
                    BlockState state = level.getBlockState(cursor);
                    boolean water = state.getFluidState().is(FluidTags.WATER);
                    boolean lava = state.getFluidState().is(FluidTags.LAVA);
                    boolean openable = (state.getBlock() instanceof DoorBlock door
                            && door.type().canOpenByHand()) || state.getBlock() instanceof FenceGateBlock;
                    var shape=state.getCollisionShape(level,cursor);
                    boolean collision = !shape.isEmpty();
                    boolean damaging = NavigationTerrain.damaging(state);
                    boolean unstable = state.is(BlockTags.SAND)
                            || state.is(BlockTags.CONCRETE_POWDERS)
                            || state.getBlock() == Blocks.GRAVEL
                            || state.is(BlockTags.LEAVES);
                    cells.put(position, new Cell(
                            collision,
                            water,
                            lava,
                            openable,
                            damaging,
                            unstable,
                            0,
                            state.is(BlockTags.CLIMBABLE),
                            collision && !openable || state.is(Blocks.SCAFFOLDING),
                            shape.toAabbs().stream().map(b->new NavigationWorldSnapshot.CollisionBox(b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ)).toList()
                    ));
        }
        public NavigationWorldSnapshot.Position startPosition(){return exactStart;}
        public NavigationWorldSnapshot finish() {
            if(index<volume(bounds))throw new IllegalStateException("Capture is incomplete");
            var finished=cells;
            if(!threats.isEmpty()) {
                finished=new HashMap<>();
                for(var entry:cells.entrySet()) {
                    if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();
                    var c=entry.getValue();
                    finished.put(entry.getKey(),new Cell(c.collision(),c.water(),c.lava(),c.openableDoor(),c.damaging(),c.unstable(),
                            hostileRisk(entry.getKey(),threats),c.climbable(),c.supportsStanding(),c.shape()));
                }
            }
            return new NavigationWorldSnapshot(worldRevision,observedTick,dimension,bounds,start,exactStart,destination,resources,finished);
        }
    }

    private Bounds bounds(ServerLevel level, GridPosition start, GridPosition goal) {
        int minX = Math.min(start.x(), goal.x()) - config.horizontalMargin();
        int maxX = Math.max(start.x(), goal.x()) + config.horizontalMargin();
        int minZ = Math.min(start.z(), goal.z()) - config.horizontalMargin();
        int maxZ = Math.max(start.z(), goal.z()) + config.horizontalMargin();
        int minY = Math.max(level.getMinY(),
                Math.min(start.y(), goal.y()) - config.verticalMarginBelow());
        int maxY = Math.min(level.getMaxY() - 1,
                Math.max(start.y(), goal.y()) + config.verticalMarginAbove());
        Bounds result = new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
        long volume = volume(result);
        if (maxX - minX + 1 > config.maximumHorizontalSpan()
                || maxZ - minZ + 1 > config.maximumHorizontalSpan()
                || maxY - minY + 1 > config.maximumVerticalSpan()
                || volume > config.maximumCells()) {
            throw new SnapshotUnavailableException(
                    "Target exceeds the bounded local planner; a global corridor is required");
        }
        return result;
    }

    private List<Threat> captureThreats(
            ServerLevel level,
            MinePilotServerPlayer player,
            Bounds bounds
    ) {
        AABB region = new AABB(
                bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX() + 1.0, bounds.maxY() + 1.0, bounds.maxZ() + 1.0
        ).inflate(config.threatPredictionRadius());
        List<Threat> threats = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof LivingEntity living) || entity == player
                    || !living.isAlive() || !region.contains(entity.position())) {
                continue;
            }
            boolean hostile = living instanceof Enemy
                    || living instanceof Mob mob && mob.getTarget() == player;
            if (!hostile) {
                continue;
            }
            Vec3 velocity = entity.getDeltaMovement();
            threats.add(new Threat(
                    entity.getX(), entity.getY(), entity.getZ(),
                    velocity.x, velocity.y, velocity.z
            ));
        }
        return threats;
    }

    private double hostileRisk(GridPosition position, List<Threat> threats) {
        double risk = 0.0;
        for (Threat threat : threats) {
            for (int predictionTick : new int[]{0, 10, 20}) {
                double x = threat.x() + threat.velocityX() * predictionTick;
                double y = threat.y() + threat.velocityY() * predictionTick;
                double z = threat.z() + threat.velocityZ() * predictionTick;
                double dx = position.x() + 0.5 - x;
                double dy = position.y() - y;
                double dz = position.z() + 0.5 - z;
                double distance = Math.sqrt(dx * dx + dz * dz + dy * dy * 0.35);
                if (distance < config.threatPredictionRadius()) {
                    risk += (config.threatPredictionRadius() - distance)
                            / config.threatPredictionRadius() * 3.0;
                }
            }
        }
        return risk;
    }

    private static NavigationWorldSnapshot.BodyResources resources(
            MinePilotServerPlayer player
    ) {
        int supports = 0;
        var stock = new java.util.ArrayList<RouteOption.SupportMaterial>();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && player.gameMode.getGameModeForPlayer()==net.minecraft.world.level.GameType.SURVIVAL
                    && player.inventoryLedger != null && player.inventoryLedger.expendable(stack)
                    && stack.getItem() instanceof BlockItem blockItem
                    && blockItem.getBlock().defaultBlockState().isCollisionShapeFullBlock(
                    player.level(), player.blockPosition())) {
                supports += stack.getCount();
                stock.add(new RouteOption.SupportMaterial(player.inventoryLedger.key(stack),
                        net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),stack.getCount(),player.inventoryLedger.importance(stack)));
            }
        }
        double armorMultiplier = Math.max(0.2,
                1.0 - Math.min(20, player.getArmorValue()) * 0.04);
        return new NavigationWorldSnapshot.BodyResources(
                player.getHealth(),
                player.getAbsorptionAmount(),
                player.getFoodData().getFoodLevel(),
                player.getFoodData().getSaturationLevel(),
                player.foodExhaustionLevel(),
                armorMultiplier,
                player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE),
                player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER),
                supports,
                player.canSprint(),
                true,
                stock
        );
    }

    private static long volume(Bounds bounds) {
        return (long) (bounds.maxX() - bounds.minX() + 1)
                * (bounds.maxY() - bounds.minY() + 1)
                * (bounds.maxZ() - bounds.minZ() + 1);
    }

    public record CaptureConfig(
            int horizontalMargin,
            int verticalMarginBelow,
            int verticalMarginAbove,
            int maximumHorizontalSpan,
            int maximumVerticalSpan,
            int maximumCells,
            double threatPredictionRadius
    ) {
        public CaptureConfig {
            if (horizontalMargin < 1 || verticalMarginBelow < 2
                    || verticalMarginAbove < 2 || maximumHorizontalSpan < 16
                    || maximumVerticalSpan < 8 || maximumCells < 4_096
                    || !Double.isFinite(threatPredictionRadius)
                    || threatPredictionRadius < 1.0) {
                throw new IllegalArgumentException("Invalid snapshot capture limits");
            }
        }

        public static CaptureConfig defaults() {
            return new CaptureConfig(16, 14, 6, 96, 48, 350_000, 12.0);
        }
    }

    private record Threat(
            double x,
            double y,
            double z,
            double velocityX,
            double velocityY,
            double velocityZ
    ) {
    }

    public static final class SnapshotUnavailableException extends RuntimeException {
        public SnapshotUnavailableException(String message) {
            super(message);
        }
    }
}
