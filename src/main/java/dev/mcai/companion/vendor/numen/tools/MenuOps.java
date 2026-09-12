// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from Dwinovo/minecraft-numen 34ef004dac3095fbbd928a897927e277c69d02fa.
package dev.mcai.companion.vendor.numen.tools;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;

/** Adapted native menu operation; see the pinned upstream source in THIRD_PARTY_NOTICES. */
public final class MenuOps {

    private MenuOps() {}

    /** Adapted native menu operation; see the pinned upstream source in THIRD_PARTY_NOTICES. */
    public static void dripInto(AbstractContainerMenu menu, Player who, int from, int to, int count) {
        menu.clicked(from, 0, ContainerInput.PICKUP, who);            // grab the stack
        int drops = Math.min(count, menu.getCarried().getCount());
        for (int i = 0; i < drops; i++) {
            menu.clicked(to, 1, ContainerInput.PICKUP, who);          // drop ONE (merges / fills)
        }
        if (!menu.getCarried().isEmpty()) {
            menu.clicked(from, 0, ContainerInput.PICKUP, who);        // return the remainder
        }
    }
}
