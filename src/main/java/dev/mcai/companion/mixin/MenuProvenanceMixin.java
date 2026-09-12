package dev.mcai.companion.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.mcai.companion.agent.knowledge.ContainerOrigins;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value=AbstractContainerMenu.class,remap=false)
public abstract class MenuProvenanceMixin {
    @WrapMethod(method="clicked",remap=false)
    private void minepilot$click(int slot,int button,ContainerInput input,Player player,Operation<Void> original){
        if(player.level().isClientSide()){original.call(slot,button,input,player);return;}
        var tx=ContainerOrigins.before((AbstractContainerMenu)(Object)this,player);original.call(slot,button,input,player);tx.finish();
    }
    @WrapMethod(method="removed",remap=false)
    private void minepilot$close(Player player,Operation<Void> original){
        if(player.level().isClientSide()){original.call(player);return;}
        var tx=ContainerOrigins.before((AbstractContainerMenu)(Object)this,player);original.call(player);tx.finish();
    }
}
