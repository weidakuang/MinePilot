package dev.mcai.companion.mixin;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only access to the two physical stores of a double chest. */
@Mixin(value=CompoundContainer.class,remap=false)
public interface CompoundContainerAccess {
    @Accessor("container1") Container minepilot$first();
    @Accessor("container2") Container minepilot$second();
}
