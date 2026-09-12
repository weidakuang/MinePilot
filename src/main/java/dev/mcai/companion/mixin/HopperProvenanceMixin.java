package dev.mcai.companion.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.mcai.companion.agent.knowledge.ContainerOrigins;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import java.util.function.BooleanSupplier;

@Mixin(value=HopperBlockEntity.class,remap=false)
public abstract class HopperProvenanceMixin {
    @org.spongepowered.asm.mixin.Shadow private boolean isOnCooldown(){throw new AssertionError();}
    @WrapMethod(method="tryMoveItems",remap=false)
    private static boolean minepilot$transfer(Level level,BlockPos pos,BlockState state,HopperBlockEntity hopper,BooleanSupplier action,Operation<Boolean> original) throws Exception {
        if(((HopperProvenanceMixin)(Object)hopper).isOnCooldown() || !state.getValue(net.minecraft.world.level.block.HopperBlock.ENABLED))return original.call(level,pos,state,hopper,action);
        try(var scope=ContainerOrigins.hopper(level,pos,state,hopper)){return original.call(level,pos,state,hopper,action);}
    }
    @WrapMethod(method="addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/item/ItemEntity;)Z",remap=false)
    private static boolean minepilot$pickup(Container container,ItemEntity item,Operation<Boolean> original) throws Exception {
        try(var scope=ContainerOrigins.hopperPickup(container,item)){return original.call(container,item);}
    }
}
