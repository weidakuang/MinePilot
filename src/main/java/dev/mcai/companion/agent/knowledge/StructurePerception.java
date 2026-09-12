package dev.mcai.companion.agent.knowledge;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import com.google.gson.*;
import dev.mcai.companion.agent.body.MinePilotServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;

/** Bounded native structure records, not visual markers or unrestricted /locate. */
public final class StructurePerception {
    private static final int MAX_STARTS = 512;
    private final MinePilotServerPlayer player;
    private final LinkedHashMap<String, Scan> scans = new LinkedHashMap<>();
    private final java.util.concurrent.ExecutorService scheduler = java.util.concurrent.Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "MinePilot-Structure-Scheduler"); thread.setDaemon(true); return thread;
    });
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("村庄", "#minecraft:village"), Map.entry("village", "#minecraft:village"),
            Map.entry("要塞", "minecraft:stronghold"), Map.entry("下界要塞", "minecraft:fortress"),
            Map.entry("地狱要塞", "minecraft:fortress"), Map.entry("堡垒遗迹", "minecraft:bastion_remnant"),
            Map.entry("沙漠神殿", "minecraft:desert_pyramid"), Map.entry("沙漠神庙", "minecraft:desert_pyramid"),
            Map.entry("丛林神庙", "minecraft:jungle_pyramid"), Map.entry("林地府邸", "minecraft:mansion"),
            Map.entry("海底神殿", "minecraft:monument"), Map.entry("远古城市", "minecraft:ancient_city"),
            Map.entry("试炼密室", "minecraft:trial_chambers"), Map.entry("末地城", "minecraft:end_city"),
            Map.entry("掠夺者前哨站", "minecraft:pillager_outpost"), Map.entry("矿井", "mineshaft"),
            Map.entry("沉船", "shipwreck"), Map.entry("废弃传送门", "ruined_portal"));

    public StructurePerception(MinePilotServerPlayer player) { this.player = player; }

    public JsonObject query(int radius, String filter, String cursor, int offset, int limit) {
        requireServerThread();
        if (radius < 1 || radius > PerceptionRange.MAX || offset < 0 || offset > MAX_STARTS || limit < 1 || limit > 64
                || filter.length() > 128) throw new IllegalArgumentException("Structure query: radius 1..150, limit 1..64, offset 0..512");
        long now = System.nanoTime();
        scans.values().removeIf(s -> now - s.created > 60_000_000_000L);
        Scan scan = cursor.isBlank() ? null : scans.get(cursor);
        if (!cursor.isBlank() && scan == null) throw new IllegalArgumentException("Unknown or expired structure cursor");
        String normalized = ALIASES.getOrDefault(filter.trim().toLowerCase(Locale.ROOT), filter.trim().toLowerCase(Locale.ROOT));
        if (scan != null && (scan.radius != radius || !scan.filter.equals(normalized)))
            throw new IllegalArgumentException("Continue a structure cursor with its original radius/filter, or omit cursor to start a new query");
        if (scan == null) {
            Set<Structure> wanted = resolve(normalized);
            // Reuse a recent identical completed query, with independently indexed pages.
            for (Scan cached : scans.values()) if (cached.done() && now - cached.finishedNanos < 5_000_000_000L
                    && cached.level == player.level() && cached.origin.distanceToSqr(player.position()) < 0.000001
                    && cached.radius == radius && cached.filter.equals(normalized)) { scan = cached; break; }
            if (scan == null) {
                if (scans.size() >= 4) scans.remove(scans.keySet().iterator().next());
                scan = new Scan(player.level(), player.position(), radius, normalized, wanted);
                scans.put(scan.id, scan);
            }
        }
        if (scan.level != player.level()) throw new IllegalArgumentException("Structure query belongs to a different dimension");
        // Repeated tool polls cannot multiply the per-tick native scheduling budget.
        tick();
        return scan.result(offset, limit);
    }

    private Set<Structure> resolve(String filter) {
        var registry = player.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Set<Structure> wanted = new HashSet<>();
        if (filter.startsWith("#")) {
            var tag = registry.get(TagKey.create(Registries.STRUCTURE, Identifier.parse(filter.substring(1))));
            if (tag.isPresent()) tag.get().forEach(holder -> wanted.add(holder.value()));
        } else for (Structure structure : registry) {
            String id = registry.getKey(structure).toString();
            if (filter.isBlank() || (filter.contains(":") ? id.equals(filter) : id.contains(filter))) wanted.add(structure);
        }
        if (wanted.isEmpty()) throw new IllegalArgumentException("Unknown generated structure filter; use village, stronghold, fortress, a registered structure id or #tag. Player buildings/portals are not generated structures.");
        return wanted;
    }

    private int lastTick = Integer.MIN_VALUE;
    public void tick() {
        requireServerThread();
        int tick = player.level().getServer().getTickCount();
        if (tick == lastTick) return;
        lastTick = tick;
        long deadline = System.nanoTime() + 2_000_000L;
        boolean scheduled = false;
        for (Scan scan : scans.values()) {
            if (scan.done()) continue;
            if (scan.level != player.level() || System.nanoTime() - scan.created > 30_000_000_000L) {
                scan.unavailable += scan.queue.size() + scan.pending.size();
                scan.queue.clear(); scan.pending.clear(); scan.finish(); continue;
            }
            var pending = scan.pending.entrySet().iterator();
            while (pending.hasNext() && System.nanoTime() < deadline) {
                var entry = pending.next();
                if (!entry.getValue().isDone()) continue;
                try {
                    var result = entry.getValue().getNow(null);
                    ChunkAccess chunk = result == null ? null : result.orElse(null);
                    if (chunk == null) scan.unavailable++; else scan.read(entry.getKey(), chunk);
                } catch (RuntimeException unavailable) { scan.unavailable++; }
                pending.remove();
            }
            while (!scan.queue.isEmpty() && System.nanoTime() < deadline) {
                Work work = scan.queue.peekFirst();
                var loaded = scan.level.getChunkSource().getChunkNow(work.pos.x(), work.pos.z());
                if (loaded != null) { scan.queue.removeFirst(); scan.read(work, loaded); continue; }
                // Searching must never generate or load missing chunks. The missing
                // record is reported as unknown and can be retried after travelling.
                scan.queue.removeFirst();
                scan.unavailable++;
            }
            if (scan.queue.isEmpty() && scan.pending.isEmpty()) scan.finish();
            if (System.nanoTime() >= deadline) break;
        }
    }

    public void close() { scans.clear(); scheduler.shutdown(); } // Native shared chunk futures must not be cancelled.
    private void requireServerThread() {
        if (!player.level().getServer().isSameThread()) throw new IllegalStateException("Structure records require the server thread");
    }

    /** Closest point in a recorded piece's volume; not a safe standing location. */
    public static Vec3 nearestPoint(Vec3 origin, BoundingBox box) {
        return new Vec3(Math.clamp(origin.x, box.minX(), box.maxX() + 1.0),
                Math.clamp(origin.y, box.minY(), box.maxY() + 1.0),
                Math.clamp(origin.z, box.minZ(), box.maxZ() + 1.0));
    }

    private record Work(ChunkPos pos, boolean references) {}
    private final class Scan {
        final String id = UUID.randomUUID().toString();
        final ServerLevel level;
        final Vec3 origin;
        final int radius;
        final String filter;
        final Set<Structure> wanted;
        final long created = System.nanoTime();
        final long beganTick;
        final ArrayDeque<Work> queue = new ArrayDeque<>();
        final Map<Work, CompletableFuture<ChunkResult<ChunkAccess>>> pending = new LinkedHashMap<>();
        final Set<Long> referenceChunks = new HashSet<>();
        final Set<String> seen = new HashSet<>();
        final List<JsonObject> results = new ArrayList<>();
        int primaryChunks, checkedChunks, scheduledChunks, unavailable;
        boolean finished;
        long finishedNanos;

        Scan(ServerLevel level, Vec3 origin, int radius, String filter, Set<Structure> wanted) {
            this.level = level; this.origin = origin; this.radius = radius; this.filter = filter; this.wanted = wanted;
            beganTick = level.getGameTime();
            List<ChunkPos> chunks = new ArrayList<>();
            for (int x = (int)Math.floor((origin.x-radius)/16); x <= (int)Math.floor((origin.x+radius)/16); x++)
                for (int z = (int)Math.floor((origin.z-radius)/16); z <= (int)Math.floor((origin.z+radius)/16); z++) {
                    double nx = Math.clamp(origin.x,x*16.0,x*16.0+16), nz = Math.clamp(origin.z,z*16.0,z*16.0+16);
                    if ((nx-origin.x)*(nx-origin.x)+(nz-origin.z)*(nz-origin.z) <= radius*radius) chunks.add(new ChunkPos(x,z));
                }
            chunks.sort(Comparator.comparingDouble(c -> Math.hypot(c.getMiddleBlockX()-origin.x,c.getMiddleBlockZ()-origin.z)));
            for (ChunkPos chunk : chunks) { queue.add(new Work(chunk,true)); referenceChunks.add(chunk.pack()); }
            primaryChunks = chunks.size();
        }

        void read(Work work, ChunkAccess chunk) {
            checkedChunks++;
            for (var entry : chunk.getAllStarts().entrySet()) if (wanted.contains(entry.getKey())) record(entry.getValue());
            if (!work.references) return;
            for (var entry : chunk.getAllReferences().entrySet()) if (wanted.contains(entry.getKey()))
                for (long packed : entry.getValue()) if (!referenceChunks.contains(packed)) {
                    if (referenceChunks.size() >= MAX_STARTS) { unavailable++; continue; }
                    referenceChunks.add(packed);
                    queue.addLast(new Work(ChunkPos.unpack(packed),false));
                }
        }

        void record(StructureStart start) {
            if (start == null || !start.isValid()) return;
            String type = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(start.getStructure()).toString();
            String key = type+":"+start.getChunkPos().pack();
            if (!seen.add(key)) return;
            if (seen.size() > MAX_STARTS || start.getPieces().size() > 4096) { unavailable++; return; }
            Vec3 nearest = null; double distance = Double.POSITIVE_INFINITY;
            for (var piece : start.getPieces()) {
                Vec3 point = nearestPoint(origin,piece.getBoundingBox()); double candidate = point.distanceToSqr(origin);
                if (candidate < distance) { distance = candidate; nearest = point; }
            }
            if (nearest == null || distance > radius*(double)radius) return;
            JsonObject row = new JsonObject();
            row.addProperty("structure",type);
            row.addProperty("recordId",UUID.nameUUIDFromBytes((level.dimension().identifier()+key).getBytes(StandardCharsets.UTF_8)).toString());
            row.addProperty("x",nearest.x); row.addProperty("y",nearest.y); row.addProperty("z",nearest.z);
            row.addProperty("distance",Math.sqrt(distance));
            row.addProperty("source","server_structure_record");
            row.addProperty("positionKind","nearest_recorded_piece_volume_point");
            row.addProperty("safeToStandVerified",false); row.addProperty("currentBlocksVerified",false);
            results.add(row);
        }

        boolean done() { return finished; }
        void finish() { finished=true; finishedNanos=System.nanoTime(); }
        JsonObject result(int offset, int limit) {
            JsonObject out = new JsonObject(); JsonArray rows = new JsonArray();
            if (done()) {
                results.sort(Comparator.<JsonObject>comparingDouble(r -> r.get("distance").getAsDouble()).thenComparing(r -> r.get("recordId").getAsString()));
                for (int i = offset; i < results.size() && rows.size() < limit; i++) rows.add(results.get(i).deepCopy());
            }
            JsonObject center = new JsonObject(); center.addProperty("x",origin.x); center.addProperty("y",origin.y); center.addProperty("z",origin.z);
            out.add("searchCenter",center); out.add("results",rows); out.addProperty("cursor",id);
            out.addProperty("dimension",level.dimension().identifier().toString()); out.addProperty("radius",radius);
            out.addProperty("distanceMetric","3d_sphere_to_nearest_recorded_piece_volume");
            out.addProperty("source","server_structure_records"); out.addProperty("filter",filter);
            out.addProperty("status",!done()?"SEARCHING":unavailable==0?"COMPLETE":"PARTIAL");
            out.addProperty("complete",done() && offset+rows.size()>=results.size());
            out.addProperty("coverageComplete",done() && unavailable==0);
            out.addProperty("truncated",!done() || offset+rows.size()<results.size());
            out.addProperty("nextOffset",offset+rows.size()); out.addProperty("totalMatched",done()?results.size():0);
            out.addProperty("checkedChunks",checkedChunks); out.addProperty("primaryChunks",primaryChunks);
            out.addProperty("referencedStartChunks",referenceChunks.size()-primaryChunks);
            out.addProperty("scheduledChunks",scheduledChunks); out.addProperty("unavailableChunksOrRecords",unavailable);
            out.addProperty("sampledAtTick",beganTick);
            out.addProperty("elapsedMillis",((done()?finishedNanos:System.nanoTime())-created)/1_000_000);
            out.addProperty("snapshotAgeMillis",(System.nanoTime()-created)/1_000_000);
            out.addProperty("retryAfterMs",done()?0:100);
            out.addProperty("limitation","Generated structure records may survive destruction. Positions are not entrances or safe navigation destinations; player buildings are not indexed. Only loaded structure records are read; no chunks are generated for this query. Returned positions never exceed its radius.");
            return out;
        }
    }
}
