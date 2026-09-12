package dev.mcai.companion.mixin;
import dev.mcai.companion.agent.knowledge.StoredOrigins;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=BlockEntity.class,remap=false)
public abstract class BlockEntityOriginsMixin implements StoredOrigins {
    @Unique private String minepilot$origins="";
    public String minepilot$getOrigins(){return minepilot$origins;}
    public void minepilot$setOrigins(String value){minepilot$origins=value.length()<=500_000?value:"";}
    @Inject(method={"loadWithComponents","loadCustomOnly"},at=@At("TAIL"),remap=false)
    private void minepilot$read(ValueInput input,CallbackInfo ci){minepilot$setOrigins(input.getStringOr("minepilot:container_origins",""));}
    @Inject(method={"saveWithoutMetadata(Lnet/minecraft/world/level/storage/ValueOutput;)V","saveCustomOnly(Lnet/minecraft/world/level/storage/ValueOutput;)V"},at=@At("TAIL"),remap=false)
    private void minepilot$write(ValueOutput output,CallbackInfo ci){if(!minepilot$origins.isEmpty())output.putString("minepilot:container_origins",minepilot$origins);}
}
