// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from Dwinovo/minecraft-numen 34ef004dac3095fbbd928a897927e277c69d02fa.
package dev.mcai.companion.vendor.numen.scan;

import java.util.Comparator;
import java.util.PriorityQueue;

/** Loaded-world search geometry, adapted from the pinned Numen source. */
public final class SearchGeometry {

    /** Loaded-world search geometry, adapted from the pinned Numen source. */
    private static final int CHUNK = 16;

    private SearchGeometry() {}

    /** Include both axes when the center lies at different offsets in its chunk. */
    public static int maxRing(int x, int z, int radius) {
        int cx = Math.floorDiv(x, CHUNK), cz = Math.floorDiv(z, CHUNK);
        return Math.max(Math.max(Math.floorDiv(x + radius, CHUNK) - cx,
                        cx - Math.floorDiv(x - radius, CHUNK)),
                Math.max(Math.floorDiv(z + radius, CHUNK) - cz,
                        cz - Math.floorDiv(z - radius, CHUNK)));
    }

    /** Loaded-world search geometry, adapted from the pinned Numen source. */
    public static int[] sectionOrder(int minSectionY, int maxSectionY, int centerSectionY) {
        if (maxSectionY < minSectionY) {
            return new int[0];
        }
        int n = maxSectionY - minSectionY + 1;
        int[] out = new int[n];
        int centre = Math.clamp(centerSectionY, minSectionY, maxSectionY);
        int at = 0;
        out[at++] = centre;
        for (int d = 1; at < n; d++) {
            if (centre - d >= minSectionY) {
                out[at++] = centre - d;
            }
            if (at < n && centre + d <= maxSectionY) {
                out[at++] = centre + d;
            }
        }
        return out;
    }

    /** Loaded-world search geometry, adapted from the pinned Numen source. */
    public static double ringFloorDistance(int ring) {
        return ring <= 0 ? 0.0 : (double) ring * CHUNK - (CHUNK - 1);
    }

    /** Loaded-world search geometry, adapted from the pinned Numen source. */
    public static boolean canStop(int ring, NearestBound bound) {
        return bound.full() && bound.worst() <= ringFloorDistance(ring + 1);
    }

    /** Loaded-world search geometry, adapted from the pinned Numen source. */
    public static final class NearestBound {

        private final int want;
        /** Loaded-world search geometry, adapted from the pinned Numen source. */
        private final PriorityQueue<Double> kept;

        public NearestBound(int want) {
            this.want = Math.max(0, want);
            this.kept = new PriorityQueue<>(Math.max(1, this.want), Comparator.reverseOrder());
        }

        /** Loaded-world search geometry, adapted from the pinned Numen source. */
        public void offer(double distance) {
            if (want == 0) {
                return;
            }
            if (kept.size() < want) {
                kept.add(distance);
            } else if (distance < kept.peek()) {
                kept.poll();
                kept.add(distance);
            }
        }

        /** Loaded-world search geometry, adapted from the pinned Numen source. */
        public boolean full() {
            return want > 0 && kept.size() >= want;
        }

        /** Loaded-world search geometry, adapted from the pinned Numen source. */
        public double worst() {
            return full() ? kept.peek() : Double.POSITIVE_INFINITY;
        }
    }
}
