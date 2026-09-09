package dev.mcai.companion.agent.mining;

import dev.mcai.companion.agent.body.MinePilotServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import com.google.gson.JsonObject;

/** Inventory-only tool preview. Equipping is deferred to approval and uses native menu swaps. */
public record MiningToolChoice(int slot, ItemStack stack) {
    public static MiningToolChoice best(MinePilotServerPlayer body, BlockState state, boolean harvest, int blocks) {
        int held=body.getInventory().getSelectedSlot(), best=-1; float speed=-1;
        for(int n=-1;n<=40;n++) {
            int slot=n==-1?held:n;
            if(n==held || slot>=36 && slot<40)continue;
            var item=body.getInventory().getItem(slot);var data=item.get(DataComponents.TOOL);
            if(harvest && state.requiresCorrectToolForDrops() && !item.isCorrectToolForDrops(state))continue;
            if(item.isDamageableItem() && (data==null || item.getMaxDamage()-item.getDamageValue()<=Math.max(1,data.damagePerBlock())*blocks))continue;
            float candidate=item.isEmpty()?1:item.getDestroySpeed(state);
            if(candidate>speed){speed=candidate;best=slot;}
        }
        if(best<0)throw new IllegalArgumentException("No inventory tool can harvest this block with the required durability reserve");
        return new MiningToolChoice(best,body.getInventory().getItem(best).copy());
    }
    public JsonObject json(MinePilotServerPlayer body) {
        var out=new JsonObject();out.addProperty("slot",slot);out.addProperty("item",stack.isEmpty()?"bare_hands":BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if(!stack.isEmpty())out.addProperty("entryId",body.inventoryLedger.key(stack));
        out.addProperty("damageable",stack.isDamageableItem());
        if(stack.isDamageableItem()){out.addProperty("damage",stack.getDamageValue());out.addProperty("maxDurability",stack.getMaxDamage());out.addProperty("remainingDurability",stack.getMaxDamage()-stack.getDamageValue());}
        return out;
    }
    public double estimatedTicks(MinePilotServerPlayer body,BlockState state,BlockPos target) {
        // Base tool component estimate. Actual vanilla progress (effects, enchantments,
        // fluid and mod hooks) is measured by the normal breaker during execution.
        double hardness=state.getDestroySpeed(body.level(),target);
        boolean harvest=!state.requiresCorrectToolForDrops() || stack.isCorrectToolForDrops(state);
        return Math.max(1,Math.ceil(hardness*(harvest?30:100)/Math.max(.001,stack.isEmpty()?1:stack.getDestroySpeed(state))));
    }
}
