package dev.mcai.companion.vendor.numen.tools;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

/** 26.2 recipe display adapter. Actual crafting still validates the live menu result. */
final class RecipeProbe {
    static ItemStack resultOf(CraftingRecipe recipe, ServerLevel level) {
        var context = SlotDisplayContext.fromLevel(level);
        return recipe.display().stream().map(d -> d.result().resolveForFirstStack(context))
                .filter(s -> !s.isEmpty()).findFirst().orElse(ItemStack.EMPTY);
    }
    static boolean usableIngredients(CraftingRecipe recipe) {
        return !recipe.placementInfo().isImpossibleToPlace() && !recipe.placementInfo().ingredients().isEmpty();
    }
}
