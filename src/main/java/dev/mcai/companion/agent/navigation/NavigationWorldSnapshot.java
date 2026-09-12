package dev.mcai.companion.agent.navigation;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable server-oracle data copied on the server thread for off-thread
 * planning. The planner must never dereference a Minecraft level off-thread.
 */
public record NavigationWorldSnapshot(
        long worldRevision,
        long observedGameTick,
        String dimension,
        Bounds bounds,
        GridPosition start,
        Position exactStart,
        NavigationPlan.ResolvedDestination destination,
        BodyResources resources,
        Map<GridPosition, Cell> cells
) {
    public NavigationWorldSnapshot {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(exactStart, "exactStart");
        if ((int) Math.floor(exactStart.x()) != start.x()
                || (int) Math.floor(exactStart.y()) != start.y()
                || (int) Math.floor(exactStart.z()) != start.z()) {
            throw new IllegalArgumentException("Exact start must belong to the start cell");
        }
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(resources, "resources");
        // Dense spatial keys made Map.copyOf's linear-probing MapN spend seconds
        // on the server thread. Keep an independently owned immutable hash map.
        cells = java.util.Collections.unmodifiableMap(new java.util.HashMap<>(cells));
        if (!dimension.equals(destination.dimension())) {
            throw new IllegalArgumentException("Cross-dimension planning requires a portal route");
        }
    }

    public Cell cell(GridPosition position) {
        return cells.getOrDefault(position, Cell.UNKNOWN);
    }

    public boolean contains(GridPosition position) {
        return bounds.contains(position);
    }

    public record GridPosition(int x, int y, int z) {
        @Override public int hashCode() {
            // Record's polynomial hash aliases neighbouring rows/planes (31*y+z).
            // Mix all axes independently for dense voxel snapshots and A* maps.
            int hash = x * 73856093 ^ y * 19349663 ^ z * 83492791;
            return hash ^ (hash >>> 16);
        }

        public GridPosition offset(int dx, int dy, int dz) {
            return new GridPosition(x + dx, y + dy, z + dz);
        }

        public double distanceTo(GridPosition other) {
            int dx = x - other.x;
            int dy = y - other.y;
            int dz = z - other.z;
            return Math.sqrt((double) dx * dx + (double) dy * dy + (double) dz * dz);
        }
    }

    public record Position(double x, double y, double z) {
        public Position {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                throw new IllegalArgumentException("Position must be finite");
            }
        }
    }

    public record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public Bounds {
            if (minX > maxX || minY > maxY || minZ > maxZ) {
                throw new IllegalArgumentException("Invalid snapshot bounds");
            }
        }

        public boolean contains(GridPosition position) {
            return position.x >= minX && position.x <= maxX
                    && position.y >= minY && position.y <= maxY
                    && position.z >= minZ && position.z <= maxZ;
        }
    }

    public record BodyResources(
            double health,
            double absorption,
            int foodLevel,
            double saturation,
            double exhaustion,
            double expectedDamageMultiplier,
            double safeFallDistance,
            double fallDamageMultiplier,
            int supportBlocks,
            boolean canSprint,
            boolean canSwim,
            java.util.List<RouteOption.SupportMaterial> supportStock
    ) {
        public BodyResources(double health,double absorption,int foodLevel,double saturation,double exhaustion,
                double expectedDamageMultiplier,double safeFallDistance,double fallDamageMultiplier,int supportBlocks,boolean canSprint,boolean canSwim) {
            this(health,absorption,foodLevel,saturation,exhaustion,expectedDamageMultiplier,safeFallDistance,fallDamageMultiplier,supportBlocks,canSprint,canSwim,java.util.List.of());
        }
        public BodyResources {
            supportStock = java.util.List.copyOf(supportStock);
            if (health < 0.0 || absorption < 0.0 || foodLevel < 0
                    || foodLevel > 20 || saturation < 0.0 || exhaustion < 0.0
                    || exhaustion >= 4.0 || expectedDamageMultiplier < 0.0
                    || expectedDamageMultiplier > 1.0 || supportBlocks < 0
                    || !Double.isFinite(safeFallDistance)
                    || !Double.isFinite(fallDamageMultiplier) || fallDamageMultiplier < 0.0) {
                throw new IllegalArgumentException("Invalid body resources");
            }
        }
    }

    /** A vanilla collision box copied as numbers, safe for off-thread planning. */
    public record CollisionBox(double minX,double minY,double minZ,double maxX,double maxY,double maxZ) {
        public static final CollisionBox FULL=new CollisionBox(0,0,0,1,1,1);
    }

    public double standingY(GridPosition node) {
        var here=cell(node);
        if(here.climbable() || here.water())return node.y();
        double top=Double.NaN;
        for(int below=0;below<=2;below++) {
            var pos=node.offset(0,-below,0);if(!contains(pos))continue;
            var candidate=cell(pos);if(!candidate.supportsStanding() || candidate.damaging())continue;
            for(var box:candidate.shape()) {
                double y=pos.y()+box.maxY();
                if(y>=node.y()-1e-7 && y<node.y()+1-1e-7 && box.maxX()>.2 && box.minX()<.8 && box.maxZ()>.2 && box.minZ()<.8)
                    top=Double.isNaN(top)?y:Math.max(top,y);
            }
        }
        return top;
    }
    public double feetY(GridPosition node) {double y=standingY(node);return Double.isNaN(y)?node.y():y;}

    public boolean bodyClear(double x,double y,double z) {
        double minX=x-.299,minZ=z-.299,maxX=x+.299,maxZ=z+.299,maxY=y+1.8;
        for(int by=(int)Math.floor(y);by<=(int)Math.floor(maxY-1e-7);by++)
            for(int bz=(int)Math.floor(minZ);bz<=(int)Math.floor(maxZ);bz++)
                for(int bx=(int)Math.floor(minX);bx<=(int)Math.floor(maxX);bx++) {
                    var pos=new GridPosition(bx,by,bz);if(!contains(pos))return false;
                    var cell=cell(pos);if(cell.lava() || cell.damaging())return false;
                    if(cell.openableDoor() || cell.climbable())continue;
                    for(var box:cell.shape())if(bx+box.maxX()>minX+1e-7 && bx+box.minX()<maxX-1e-7
                            && bz+box.maxZ()>minZ+1e-7 && bz+box.minZ()<maxZ-1e-7
                            && by+box.maxY()>y+1e-7 && by+box.minY()<maxY-1e-7)return false;
                }
        return true;
    }

    public record Cell(
            boolean collision,
            boolean water,
            boolean lava,
            boolean openableDoor,
            boolean damaging,
            boolean unstable,
            double hostileRisk,
            boolean climbable,
            boolean supportsStanding,
            java.util.List<CollisionBox> shape
    ) {
        public Cell(boolean collision, boolean water, boolean lava, boolean openableDoor,
                    boolean damaging, boolean unstable, double hostileRisk, boolean climbable, boolean supportsStanding) {
            this(collision,water,lava,openableDoor,damaging,unstable,hostileRisk,climbable,supportsStanding,
                    collision ? java.util.List.of(CollisionBox.FULL) : java.util.List.of());
        }
        public static final Cell UNKNOWN = new Cell(true, false, false, false, false, false, 0.0);

        public Cell(boolean collision, boolean water, boolean lava, boolean openableDoor,
                    boolean damaging, boolean unstable, double hostileRisk) {
            this(collision, water, lava, openableDoor, damaging, unstable, hostileRisk,
                    false, collision);
        }

        public Cell {
            shape=java.util.List.copyOf(shape);
            if (!Double.isFinite(hostileRisk) || hostileRisk < 0.0) {
                throw new IllegalArgumentException("Invalid hostile risk");
            }
        }

        public boolean passable() {
            return !collision || openableDoor || water || climbable;
        }
    }
}
