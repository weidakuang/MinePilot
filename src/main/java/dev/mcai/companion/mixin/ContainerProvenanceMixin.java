package dev.mcai.companion.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.mcai.companion.agent.knowledge.ProvenanceHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value=Containers.class,remap=false)
public abstract class ContainerProvenanceMixin {
    @WrapMethod(method="dropContents(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/Container;)V",remap=false)
    private static void minepilot$contents(Level level,double x,double y,double z,net.minecraft.world.Container container,Operation<Void> original) throws Exception {
        try(var scope=dev.mcai.companion.agent.knowledge.ContainerOrigins.dropping(container)){original.call(level,x,y,z,container);}
    }
    @WrapMethod(method="dropItemStack",remap=false)
    private static void minepilot$container(Level level,double x,double y,double z,ItemStack stack,Operation<Void> original){
        var pos=BlockPos.containing(x,y,z);var cause=ProvenanceHooks.cause("container_contents",level,pos,level.getBlockState(pos),null);
        var parent=ProvenanceHooks.current();if(parent!=null){cause.add("cause",parent);if(parent.has("block"))cause.add("block",parent.get("block"));if(parent.has("actorId")){cause.add("actorId",parent.get("actorId"));cause.add("actorName",parent.get("actorName"));}if(parent.get("category").getAsString().equals("player_block_break"))cause.addProperty("category","broken_container_contents");}
        if(level instanceof net.minecraft.server.level.ServerLevel serverLevel){var runtime=dev.mcai.companion.agent.AgentRuntime.active(serverLevel.getServer());if(runtime!=null){var rows=dev.mcai.companion.agent.knowledge.ContainerOrigins.dropOrigins(runtime.player(),stack);if(rows!=null)cause.add("transferredOrigins",rows);}}
        try(var scope=ProvenanceHooks.enter(cause)){original.call(level,x,y,z,stack);}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}
    }
}
