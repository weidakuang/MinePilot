package dev.mcai.companion.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.mcai.companion.agent.body.MinePilotServerPlayer;
import dev.mcai.companion.agent.knowledge.ProvenanceHooks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.server.commands.GiveCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import java.util.*;

@Mixin(value=GiveCommand.class,remap=false)
public abstract class GiveProvenanceMixin {
    @WrapMethod(method="giveItem",remap=false)
    private static int minepilot$give(CommandSourceStack source,ItemInput input,Collection<ServerPlayer> players,int count,Operation<Integer> original) throws Exception {
        var before=new HashMap<ServerPlayer,Integer>();ItemStack prototype=input.createItemStack(1);
        for(var p:players)before.put(p,count(p,prototype));
        var cause=ProvenanceHooks.cause("system_grant",source.getLevel(),null,null,source.getEntity());cause.addProperty("giver",source.getTextName());
        try(var scope=ProvenanceHooks.enter(cause)){
            int result=original.call(source,input,players,count);
            for(var entry:before.entrySet()){int gained=count(entry.getKey(),prototype)-entry.getValue();if(gained>0){var stack=prototype.copyWithCount(gained);dev.mcai.companion.agent.knowledge.CarriedProvenance.acquired(entry.getKey(),stack,cause);if(entry.getKey() instanceof MinePilotServerPlayer body && body.inventoryLedger!=null)body.inventoryLedger.granted(stack,cause);}}
            return result;
        }
    }
    private static int count(ServerPlayer p,ItemStack prototype){int count=0;for(int slot=0;slot<p.getInventory().getContainerSize();slot++){var s=p.getInventory().getItem(slot);if(ItemStack.isSameItemSameComponents(s,prototype))count+=s.getCount();}return count;}
}
