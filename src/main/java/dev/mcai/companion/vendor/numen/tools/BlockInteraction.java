/*
 * Copyright (c) 2026 Dwinovo. SPDX-License-Identifier: LGPL-3.0-only
 * Adapted from core/act/Interaction.java, fireUseBlock, upstream
 * 34ef004dac3095fbbd928a897927e277c69d02fa.
 * Changes: extracted resolved-hit execution for a 26.2 server player;
 * callers retain physical aiming, stopping, reach checks and receipts.
 */
package dev.mcai.companion.vendor.numen.tools;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;

/** Shared native right-click sequence for resolved block faces. */
public final class BlockInteraction {
    private BlockInteraction() {}
    public static InteractionResult use(ServerPlayer player, BlockHitResult hit, InteractionHand... hands) {
        InteractionResult result = InteractionResult.PASS;
        for (InteractionHand hand : hands) {
            result = player.gameMode.useItemOn(player, player.level(), player.getItemInHand(hand), hand, hit);
            if (result.consumesAction()) {
                player.swing(hand);
                return result;
            }
        }
        return result;
    }
}
