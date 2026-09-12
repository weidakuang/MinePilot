// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from Dwinovo/minecraft-numen 34ef004dac3095fbbd928a897927e277c69d02fa.
package dev.mcai.companion.vendor.numen.build;

import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/** Upstream support-first passes and alternating row order; native use controls pacing. */
public final class BuildOrder {
    public record Target(BlockPos pos, BlockState desiredState) {}
    private BuildOrder() {}
    public static final Comparator<Target> BUILD_ORDER = Comparator
            .comparingInt((Target t) -> needsSupport(t.desiredState()) ? 1 : 0)
            .thenComparingInt(t -> t.pos().getY())
            .thenComparingInt(BuildOrder::stage)
            .thenComparingInt(t -> t.pos().getZ())
            .thenComparingInt(t -> (t.pos().getZ() & 1) == 0 ? t.pos().getX() : -t.pos().getX());
    public static boolean needsSupport(BlockState state) {
        if (state == null) return false;
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HANGING)) return true;
        var b=state.getBlock();
        return b instanceof net.minecraft.world.level.block.LadderBlock
            || b instanceof net.minecraft.world.level.block.TorchBlock
            || b instanceof net.minecraft.world.level.block.SignBlock
            || b instanceof net.minecraft.world.level.block.BasePressurePlateBlock
            || b instanceof net.minecraft.world.level.block.BaseRailBlock
            || b instanceof net.minecraft.world.level.block.DiodeBlock
            || b instanceof net.minecraft.world.level.block.RedStoneWireBlock
            || b instanceof net.minecraft.world.level.block.CarpetBlock
            || b instanceof net.minecraft.world.level.block.FlowerPotBlock
            || b instanceof net.minecraft.world.level.block.SnowLayerBlock
            || state.is(net.minecraft.tags.BlockTags.FLOWERS)
            || (b instanceof net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock
                && !(b instanceof net.minecraft.world.level.block.GrindstoneBlock));
    }
    public static int stage(Target target) {
        var state=target.desiredState();
        if(state==null || state.isAir()) return 0;
        return state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE,BlockPos.ZERO)?1:2;
    }
}
