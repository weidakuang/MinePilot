package dev.mcai.companion.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.mcai.companion.agent.knowledge.DropProvenance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value=ItemEntity.class,remap=false)
public abstract class ItemEntityLineageMixin {
    @WrapMethod(method="merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;)V",remap=false)
    private static void minepilot$merge(ItemEntity to,ItemStack toStack,ItemEntity from,ItemStack fromStack,Operation<Void> original){
        var beforeTo=DropProvenance.segments(to,toStack.getCount());var beforeFrom=DropProvenance.segments(from,fromStack.getCount());int count=fromStack.getCount();
        original.call(to,toStack,from,fromStack);
        DropProvenance.merged(to,from,beforeTo,beforeFrom,Math.max(0,count-fromStack.getCount()));
    }
    @WrapMethod(method="playerTouch",remap=false)
    private void minepilot$pickup(net.minecraft.world.entity.player.Player player,Operation<Void> original){
        var entity=(ItemEntity)(Object)this;
        if(entity.level().isClientSide()){original.call(player);return;}
        if(dev.mcai.companion.agent.knowledge.DiscardedItems.avoided(entity,player))return;
        var stack=entity.getItem().copy();int before=count(player,stack);
        var origins=DropProvenance.segments(entity,stack.getCount());
        original.call(player);
        int removed=stack.getCount()-(entity.isAlive()?entity.getItem().getCount():0);
        int acquired=Math.min(removed,Math.max(0,count(player,stack)-before));
        if(acquired>0){
            var source=DropProvenance.pickedUpSource(entity,player,origins,acquired);
            dev.mcai.companion.agent.knowledge.CarriedProvenance.acquired(player,stack.copyWithCount(acquired),source);
            if(player instanceof dev.mcai.companion.agent.body.MinePilotServerPlayer body && body.inventoryLedger!=null)
                body.inventoryLedger.pickedUp(entity,stack.copyWithCount(acquired),source);
        }
    }
    private static int count(net.minecraft.world.entity.player.Player player,ItemStack prototype){
        int n=0;for(int slot=0;slot<player.getInventory().getContainerSize();slot++){var s=player.getInventory().getItem(slot);if(ItemStack.isSameItemSameComponents(s,prototype))n+=s.getCount();}return n;
    }
}
