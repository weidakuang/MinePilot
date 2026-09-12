package dev.mcai.companion.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.mcai.companion.agent.knowledge.CarriedProvenance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value=ServerLevel.class,remap=false)
public abstract class SpawnProvenanceMixin {
    @WrapMethod(method="addFreshEntity",remap=false)
    private boolean minepilot$spawn(Entity entity,Operation<Boolean> original){
        boolean added=original.call(entity);
        if(added && entity instanceof ItemEntity item && item.getPersistentData().getString("minepilot.source").orElse("").equals("player_toss")){
            var actor=item.getPersistentData().getString("minepilot.actorId").orElse("");
            if(!actor.isEmpty())try{var player=((ServerLevel)(Object)this).getServer().getPlayerList().getPlayer(java.util.UUID.fromString(actor));if(player!=null)CarriedProvenance.tossed(player,item);}catch(IllegalArgumentException ignored){}
        }
        return added;
    }
}
