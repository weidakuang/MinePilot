// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from Dwinovo/minecraft-numen 34ef004dac3095fbbd928a897927e277c69d02fa.
package dev.mcai.companion.vendor.numen.scan;

import dev.mcai.companion.MinecraftAiCompanion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Loaded-world search geometry, adapted from the pinned Numen source. */
public final class BlockSearch {

    /** Hard stop: convert a crawling scan into a partial answer (30s). */
    private static final int DEADLINE_TICKS = 600;
    /** Same collect cap as the synchronous scanner — bounds memory and sort. */
    private static final int MAX_COLLECT = 8_192;

    private static final List<BlockSearch> JOBS = new ArrayList<>();
    private static int nextId = 1;

    private final int id = nextId++;
    /** What was asked for, short enough for one log line: {@code iron_ore} / {@code iron_ore+1}. */
    private final String label;
    private final MinecraftServer owner;
    private final java.util.function.Predicate<BlockPos> eligible;
    private static final java.util.Map<MinecraftServer,SearchBudget> BUDGETS = new java.util.WeakHashMap<>();
    private long startTick = -1;
    private final UUID entityUuid;
    private final ResourceKey<Level> dimension;
    private final BlockPos center;
    private final int radius;
    private final double radiusSq;
    private final Predicate<BlockState> filter;
    private final Consumer<ScanResult> onDone;

    private final int centerChunkX, centerChunkZ, maxRing;
    /** Section Y values in visit order — nearest layer first ({@link SearchGeometry#sectionOrder}). */
    private final int[] sectionOrder;
    private int ring, perimIdx;
    private long deadline = -1;
    private int columnsScanned, columnsUnloaded;
    private final int columnsTotal;
    private boolean stoppedEarly;

    /** How far out the nearest {@code want} reach — the stop rule's whole input. */
    private final SearchGeometry.NearestBound bound;
    /** Watermark into {@link #matches} — everything below it is already in {@link #bound}. */
    private int fed;

    // Column in progress (budget ran dry mid-column); null = fetch next.
    private ChunkAccess currentChunk;
    private int currentChunkX, currentChunkZ, sectionCursor;

    private final List<BlockScanner.Hit> matches = new ArrayList<>();

    /**
     * One scan's answer plus its coverage ledger: how many of {@code columnsTotal}
     * chunk columns were actually read, how many were skipped for not being
     * loaded, and whether the deadline cut the walk short. The caller words the
     * reply from these — a hit list alone can't tell the model whether "nothing
     * found" means "nothing there".
     */
    public record ScanResult(List<BlockScanner.Hit> matches, int columnsScanned,
                             int columnsUnloaded, int columnsTotal,
                             boolean deadlineHit, boolean stoppedEarly) {

        /** Did the walk actually cover the whole requested sphere? */
        public boolean coveredEverything() {
            return !deadlineHit && !stoppedEarly && columnsUnloaded == 0;
        }
    }

    private BlockSearch(UUID entityUuid, ServerLevel level, BlockPos center, int radius, int want,
                          Set<Block> targets, java.util.function.Predicate<BlockPos> eligible, Consumer<ScanResult> onDone) {
        this.entityUuid = entityUuid;
        this.owner = level.getServer();
        this.eligible = eligible;
        this.dimension = level.dimension();
        this.center = center;
        this.radius = radius;
        this.radiusSq = (double) radius * radius;
        this.filter = state -> targets.contains(state.getBlock());
        this.onDone = onDone;
        this.label = describe(targets);
        this.centerChunkX = SectionPos.blockToSectionCoord(center.getX());
        this.centerChunkZ = SectionPos.blockToSectionCoord(center.getZ());
        this.maxRing = SearchGeometry.maxRing(center.getX(), center.getZ(), radius);
        this.sectionOrder = SearchGeometry.sectionOrder(
                SectionPos.blockToSectionCoord(Math.max(center.getY() - radius, level.getMinY())),
                SectionPos.blockToSectionCoord(Math.min(center.getY() + radius, level.getMaxY())),
                SectionPos.blockToSectionCoord(center.getY()));
        this.bound = new SearchGeometry.NearestBound(want);
        int side = 2 * maxRing + 1;
        this.columnsTotal = side * side;
    }

    /**
     * Register a search; the result arrives via the callback on a later tick.
     *
     * @param want how many nearest hits the caller actually needs — the stop rule's quota.
     *             Ask for what you will use: a bigger number walks further to prove itself.
     * @return a handle for {@link #cancel(int)}, per SEARCH rather than per companion —
     *         one pet can have a {@code scan_blocks} query and a {@code goto} lookup in
     *         flight at once, and abandoning one must not silence the other.
     */
    public static int start(UUID entityUuid, ServerLevel level, BlockPos center, int radius, int want,
                            Set<Block> targets, java.util.function.Predicate<BlockPos> eligible, Consumer<ScanResult> onDone) {
        BlockSearch job = new BlockSearch(entityUuid, level, center, radius, want, targets, eligible, onDone);
        JOBS.add(job);
        return job.id;
    }

    /** Abandon one search: no callback will fire. Unknown / already-finished ids are a no-op. */
    public static void cancel(int id) {
        JOBS.removeIf(job -> job.id == id);
    }

    /** Advance all pending scans under the shared budget. */
    public static void tick(MinecraftServer server) {
        if (JOBS.isEmpty()) return;
        BUDGETS.computeIfAbsent(server, key -> new SearchBudget()).refresh(server);
        Iterator<BlockSearch> it = JOBS.iterator();
        while (it.hasNext()) {
            BlockSearch job = it.next();
            if (job.owner == server && job.tickOne(server)) it.remove();
        }
    }

    /** @return true when finished (reply sent). */
    private boolean tickOne(MinecraftServer server) {
        ServerLevel level = server.getLevel(dimension);
        if (level == null) {
            finish(server, false);
            return true;
        }
        if (deadline < 0) {
            startTick = server.getTickCount();
            deadline = startTick + DEADLINE_TICKS;
        }
        if (server.getTickCount() >= deadline) {
            finish(server, true);
            return true;
        }
        while (true) {
            if (currentChunk == null && !nextColumn(level)) {
                finish(server, false);   // spiral exhausted
                return true;
            }
            // Scan the in-progress column one budgeted section at a time, nearest layer first.
            while (sectionCursor < sectionOrder.length) {
                if (!BUDGETS.get(server).trySectionScan()) return false;
                BlockScanner.scanChunkSection(level, currentChunk,
                        currentChunkX, sectionOrder[sectionCursor], currentChunkZ,
                        center, radius, radiusSq, filter, matches);
                // Keep only task-eligible exposed candidates before applying caps.
                // Buried stone must not fill the quota and starve a nearby outcrop.
                for (int i=matches.size()-1;i>=fed;i--) if(!eligible.test(matches.get(i).pos())) matches.remove(i);
                sectionCursor++;
                feedBound();
                if (matches.size() >= MAX_COLLECT) {
                    // Ring order means what we have is the nearest area anyway.
                    finish(server, false);
                    return true;
                }
            }
            currentChunk = null;
            columnsScanned++;
        }
    }

    /**
     * Resolve the next spiral column into {@link #currentChunk}, tallying and
     * skipping columns whose chunk isn't loaded. Returns false only when the
     * spiral is exhausted — walking past unloaded terrain costs one cache lookup
     * per column, so it needs no permit and never defers to the next tick.
     */
    private boolean nextColumn(ServerLevel level) {
        while (ring <= maxRing) {
            if (perimIdx >= RingSpiral.perimeter(ring)) {
                if (SearchGeometry.canStop(ring, bound)) {
                    // The nearest `want` are already closer than anything the next ring could hold.
                    stoppedEarly = true;
                    return false;
                }
                ring++;
                perimIdx = 0;
                continue;
            }
            int[] d = RingSpiral.offset(ring, perimIdx++);
            int cx = centerChunkX + d[0];
            int cz = centerChunkZ + d[1];
            ChunkAccess chunk = BlockScanner.loadedChunk(level, cx, cz);
            if (chunk == null) {
                columnsUnloaded++;
                continue;
            }
            currentChunk = chunk;
            currentChunkX = cx;
            currentChunkZ = cz;
            sectionCursor = 0;
            return true;
        }
        return false;
    }

    /** Hand the hits found since the last call to the distance bound the stop rule reads. */
    private void feedBound() {
        for (int i = fed; i < matches.size(); i++) {
            bound.offer(matches.get(i).distance());
        }
        fed = matches.size();
    }

    private void finish(MinecraftServer server, boolean deadlineHit) {
        matches.sort(Comparator.comparingDouble(BlockScanner.Hit::distance));
        // One line per search, and it has to carry everything a bug report needs: what was
        // asked, what came back, WHY it stopped, and what it cost. "She can't find X" is
        // answered by the stop reason plus the unloaded count, without a debug build.
        MinecraftAiCompanion.LOGGER.info("[numen-scan] {} r={} → {} hit(s){} | {} | {}/{} columns read,"
                        + " {} not loaded | {} tick(s)",
                label, radius, matches.size(),
                matches.isEmpty() ? "" : String.format(", nearest %.1f", matches.get(0).distance()),
                stopReason(deadlineHit), columnsScanned, columnsTotal, columnsUnloaded,
                startTick < 0 ? 0 : server.getTickCount() - startTick);
        onDone.accept(new ScanResult(List.copyOf(matches), columnsScanned, columnsUnloaded, columnsTotal,
                deadlineHit || matches.size() >= MAX_COLLECT, stoppedEarly));
    }

    private String stopReason(boolean deadlineHit) {
        if (deadlineHit) return "deadline";
        if (stoppedEarly) return "proved nearest at ring " + ring;
        if (matches.size() >= MAX_COLLECT) return "collect cap";
        return "covered the whole radius";
    }

    /** {@code iron_ore} for one target, {@code iron_ore+1} for a set — one log line, not a list. */
    private static String describe(Set<Block> targets) {
        Iterator<Block> it = targets.iterator();
        if (!it.hasNext()) return "nothing";
        String first = BuiltInRegistries.BLOCK.getKey(it.next()).getPath();
        return targets.size() > 1 ? first + "+" + (targets.size() - 1) : first;
    }
}
