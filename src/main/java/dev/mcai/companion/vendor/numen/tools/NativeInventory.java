package dev.mcai.companion.vendor.numen.tools;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import dev.mcai.companion.agent.body.MinePilotServerPlayer;

/** Current-version inventory adapter; never manufactures an inventory stack. */
public final class NativeInventory {
    private NativeInventory() {}
    public static Item parseItem(String id) {
        Identifier key = Identifier.parse(id);
        if (!BuiltInRegistries.ITEM.containsKey(key) || key.toString().equals("minecraft:air"))
            throw new IllegalArgumentException("Unknown or empty item: " + id);
        return BuiltInRegistries.ITEM.getValue(key);
    }
    public static int count(Inventory inventory, Item item) {
        int count = 0;
        for (int slot = 0; slot < 36; slot++) if (inventory.getItem(slot).is(item)) count += inventory.getItem(slot).getCount();
        return count;
    }
    public static boolean usable(MinePilotServerPlayer player, ItemStack stack) {
        return !stack.isEmpty() && (player.inventoryLedger == null || player.inventoryLedger.importance(stack) >= 2);
    }
}
