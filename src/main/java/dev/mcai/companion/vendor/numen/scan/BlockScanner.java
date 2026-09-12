/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Adapted from Numen, Copyright (c) 2026 Dwinovo.
 * Upstream commit: 34ef004dac3095fbbd928a897927e277c69d02fa
 * Modifications 2026-09-12 by weida / MinePilot contributors:
 * relocated package, English documentation, public loaded-chunk accessor,
 * and unused import removal. On 2026-09-13 resource jobs gained copied-section
 * worker scanning; the synchronous primitive remains for small local queries.
 * See META-INF/licenses/numen and THIRD_PARTY_NOTICES.md.
 */
package dev.mcai.companion.vendor.numen.scan;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.List;
import java.util.function.Predicate;

/** Loaded-chunk terrain scan primitives, adapted from Numen. Section palettes skip absent targets. */
public final class BlockScanner {

    private BlockScanner() {}

    /** Read only a fully loaded chunk. Never generate or synchronously load terrain. */
    public static ChunkAccess loadedChunk(Level level, int cx, int cz) {
        return level instanceof ServerLevel serverLevel
                ? serverLevel.getChunkSource().getChunkNow(cx, cz)
                : null;
    }

    /** One match: position, state and distance from the search centre. */
    public record Hit(BlockPos pos, BlockState state, double distance) {}

    /** Find the nearest matching non-air block in a small, already-loaded local box. */
    public static BlockPos nearestBlock(Level level, BlockPos base, Vec3 eye,
                                        int hr, int vr, double maxDist,
                                        java.util.function.BiPredicate<BlockPos, BlockState> match) {
        BlockPos best = null;
        double bestD = maxDist * maxDist;
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-hr, -vr, -hr), base.offset(hr, vr, hr))) {
            if (!level.isLoaded(p)) continue;
            BlockState state = level.getBlockState(p);
            if (state.isAir() || !match.test(p, state)) {
                continue;
            }
            double d = eye.distanceToSqr(Vec3.atCenterOf(p));
            if (d < bestD) {
                bestD = d;
                best = p.immutable();
            }
        }
        return best;
    }

    /** Scan one loaded section with palette filtering and spherical bounds. */
    public static void scanChunkSection(Level level, ChunkAccess chunk,
                                        int chunkX, int sectionY, int chunkZ,
                                        BlockPos center, int radius, double radiusSq,
                                        Predicate<BlockState> filter,
                                        List<Hit> out) {
        int idx = level.getSectionIndexFromSectionY(sectionY);
        if (idx < 0 || idx >= chunk.getSectionsCount()) return;
        LevelChunkSection section = chunk.getSection(idx);
        if (section == null || section.hasOnlyAir()) return;
        // Palette short-circuit: skip all 4096 inner blocks when the
        // section's palette holds no target.
        if (!section.maybeHas(filter)) return;
        scanSection(section, chunkX, sectionY, chunkZ, center, radius, radiusSq, filter, out);
    }

    /** Owns a copied palette/storage; no live chunk, level, entity or inventory. */
    public record SectionSnapshot(net.minecraft.world.level.chunk.PalettedContainer<BlockState> states,
                                  int x,int y,int z) {}
    public static SectionSnapshot snapshot(ServerLevel level,ChunkAccess chunk,int x,int y,int z,
                                            Predicate<BlockState> filter) {
        if(!level.getServer().isSameThread())throw new IllegalStateException("Section copy requires server thread");
        int index=level.getSectionIndexFromSectionY(y);
        if(index<0 || index>=chunk.getSectionsCount())return null;
        var section=chunk.getSection(index);
        if(section==null || section.hasOnlyAir() || !section.maybeHas(filter))return null;
        return new SectionSnapshot(section.getStates().copy(),x,y,z);
    }
    public static List<Hit> scanSnapshots(List<SectionSnapshot> sections,BlockPos center,int radius,
                                           Predicate<BlockState> filter) {
        var hits=new java.util.ArrayList<Hit>();
        for(var section:sections)for(int y=0;y<16;y++) {
            if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();
            for(int x=0;x<16;x++)for(int z=0;z<16;z++) {
                var state=section.states().get(x,y,z);if(!filter.test(state))continue;
                var pos=new BlockPos(section.x()*16+x,section.y()*16+y,section.z()*16+z);
                double distance=pos.distSqr(center);if(distance<=radius*(double)radius)hits.add(new Hit(pos,state,Math.sqrt(distance)));
            }
        }
        return List.copyOf(hits);
    }

    private static void scanSection(LevelChunkSection section,
                                    int chunkX, int sectionY, int chunkZ,
                                    BlockPos center, int radius, double radiusSq,
                                    Predicate<BlockState> filter,
                                    List<Hit> out) {
        int baseX = SectionPos.sectionToBlockCoord(chunkX);
        int baseY = SectionPos.sectionToBlockCoord(sectionY);
        int baseZ = SectionPos.sectionToBlockCoord(chunkZ);
        for (int dx = 0; dx < 16; dx++) {
            int worldX = baseX + dx;
            int ddx = worldX - center.getX();
            if (ddx < -radius || ddx > radius) continue;
            for (int dy = 0; dy < 16; dy++) {
                int worldY = baseY + dy;
                int ddy = worldY - center.getY();
                if (ddy < -radius || ddy > radius) continue;
                for (int dz = 0; dz < 16; dz++) {
                    int worldZ = baseZ + dz;
                    int ddz = worldZ - center.getZ();
                    if (ddz < -radius || ddz > radius) continue;
                    double distSq = (double) ddx * ddx + (double) ddy * ddy + (double) ddz * ddz;
                    if (distSq > radiusSq) continue;
                    BlockState state = section.getBlockState(dx, dy, dz);
                    if (!filter.test(state)) continue;
                    out.add(new Hit(new BlockPos(worldX, worldY, worldZ), state, Math.sqrt(distSq)));
                }
            }
        }
    }
}
