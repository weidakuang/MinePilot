package dev.mcai.companion.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.mcai.companion.agent.knowledge.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value=Entity.class,remap=false)
public abstract class EntityDropProvenanceMixin {
    @WrapMethod(method="spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/entity/item/ItemEntity;",remap=false)
    private ItemEntity minepilot$drop(ServerLevel level,ItemStack stack,Vec3 offset,Operation<ItemEntity> original) throws Exception {
        if(ProvenanceHooks.current()!=null)return original.call(level,stack,offset);
        var entity=(Entity)(Object)this;var cause=ProvenanceHooks.cause("entity_drop",level,entity.blockPosition(),null,entity);
        cause.addProperty("sourceEntityType",net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        try(var scope=ProvenanceHooks.enter(cause)){var result=original.call(level,stack,offset);if(result!=null && entity.captureDrops()!=null)DropProvenance.markCause(result,cause);return result;}
    }
}
