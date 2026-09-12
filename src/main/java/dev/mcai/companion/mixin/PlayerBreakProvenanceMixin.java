package dev.mcai.companion.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.mcai.companion.agent.knowledge.ProvenanceHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.*;

@Mixin(value=ServerPlayerGameMode.class,remap=false)
public abstract class PlayerBreakProvenanceMixin {
    @Shadow @Final protected ServerPlayer player;
    @WrapMethod(method="destroyBlock",remap=false)
    private boolean minepilot$break(BlockPos pos,Operation<Boolean> original){
        try(var scope=ProvenanceHooks.enter(ProvenanceHooks.cause("player_block_break",player.level(),pos,player.level().getBlockState(pos),player))){return original.call(pos);}
        catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}
    }
}
