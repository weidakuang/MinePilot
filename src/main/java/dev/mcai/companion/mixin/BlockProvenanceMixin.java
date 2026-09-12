package dev.mcai.companion.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.mcai.companion.agent.knowledge.ProvenanceHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value=Block.class,remap=false)
public abstract class BlockProvenanceMixin {
    @WrapMethod(method="dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;Z)V",remap=false)
    private static void minepilot$drops(BlockState state,Level level,BlockPos pos,BlockEntity blockEntity,Entity actor,ItemStack tool,boolean xp,Operation<Void> original){
        if(level.isClientSide()){original.call(state,level,pos,blockEntity,actor,tool,xp);return;}
        try(var scope=ProvenanceHooks.enter(ProvenanceHooks.cause(actor==null?"natural_block_drop":"mined_block",level,pos,state,actor))){original.call(state,level,pos,blockEntity,actor,tool,xp);}
        catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}
    }
}
