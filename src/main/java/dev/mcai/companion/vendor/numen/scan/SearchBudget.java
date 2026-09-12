// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from Dwinovo/minecraft-numen 34ef004dac3095fbbd928a897927e277c69d02fa.
package dev.mcai.companion.vendor.numen.scan;

import net.minecraft.server.MinecraftServer;

/**
 * GLOBAL per-tick budget for every sliced world search on the server —
 * structure locating ({@link LocateStructureCompanionTask}), biome locating
 * ({@link LocateBiomeCompanionTask}) and long-range block scans
 * ({@link BlockSearch}). The Explorer's Compass {@code WorldWorkerManager}
 * model: total search cost per tick is a server constant, independent of how
 * many companions are searching at once — per-task budgets would stack
 * linearly with pet count.
 *
 * <h2>Fairness</h2>
 * First-come-first-served within a tick (entities tick in a stable order), so
 * concurrent searches effectively serialize: the first finishes in a few
 * ticks, then the next drains the pool. For companion-scale concurrency
 * that's strictly better than splitting the pool — total latency is the same
 * and the implementation stays trivial. Revisit with round-robin only if
 * dozens of simultaneous searches ever become real.
 *
 * <h2>Threading</h2>
 * Server main thread only, like everything in the task layer. The tick stamp
 * uses {@link MinecraftServer#getTickCount()} (monotonic, unaffected by
 * {@code /tick freeze}) to reset the pool exactly once per server tick.
 */
public final class SearchBudget {

    /** Loaded-world search geometry, adapted from the pinned Numen source. */
    private static final int MAX_CHECKS_PER_TICK = 128;
    /**
     * Biome locator samples (pure climate-noise lookups, no chunk access; one
     * "sample" = one x/z column across all its Y probes). Cheaper than a
     * structure check, hence the larger pool — still under the shared 4ms lid.
     */
    private static final int MAX_BIOME_SAMPLES_PER_TICK = 256;
    /**
     * Block-scan section visits (one permit = one 16³ chunk section). The
     * palette pre-check makes a miss sub-microsecond and a hit ~50µs of
     * iteration, so a generous pool still sits safely under the 4ms lid.
     */
    private static final int MAX_SECTION_SCANS_PER_TICK = 256;
    /**
     * Wall-clock hard stop. The count caps bound the common case; this makes
     * the "never stalls the server" promise unconditional even when every
     * check goes cold to disk. 4ms ≈ 8% of a 50ms tick.
     */
    private static final long MAX_NANOS_PER_TICK = 4_000_000L;

    private int stampTick = Integer.MIN_VALUE;
    private int checksLeft;
    private int biomeSamplesLeft;
    private int sectionScansLeft;
    private long deadlineNanos;

    public SearchBudget() {}

    /** Reset the pool when the server tick has advanced. Call before consuming. */
    public void refresh(MinecraftServer server) {
        int now = server.getTickCount();
        if (now != stampTick) {
            resetForTick(now);
        }
    }

    /** The actual pool reset; also the test seam (no MinecraftServer needed). */
    public void resetForTick(int tick) {
        stampTick = tick;
        checksLeft = MAX_CHECKS_PER_TICK;
        biomeSamplesLeft = MAX_BIOME_SAMPLES_PER_TICK;
        sectionScansLeft = MAX_SECTION_SCANS_PER_TICK;
        deadlineNanos = System.nanoTime() + MAX_NANOS_PER_TICK;
    }

    /** Take one section-scan permit (one 16³ section); false = resume next tick. */
    public boolean trySectionScan() {
        if (sectionScansLeft <= 0 || System.nanoTime() >= deadlineNanos) return false;
        sectionScansLeft--;
        return true;
    }

    /** Take one biome-sample permit; false = pool drained, resume next tick. */
    public boolean tryBiomeSample() {
        if (biomeSamplesLeft <= 0 || System.nanoTime() >= deadlineNanos) return false;
        biomeSamplesLeft--;
        return true;
    }

    /** Take one candidate-check permit; false = pool drained, resume next tick. */
    public boolean tryCheck() {
        if (checksLeft <= 0 || System.nanoTime() >= deadlineNanos) return false;
        checksLeft--;
        return true;
    }

}
