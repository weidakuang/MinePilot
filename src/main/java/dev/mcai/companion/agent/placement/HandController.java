package dev.mcai.companion.agent.placement;

import java.util.*;
import com.google.gson.*;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.body.MinePilotServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

/** Model-visible hands backed by native inventory/menu swaps, never synthetic stacks. */
public final class HandController {
    private HandController() {}
    public static InteractionHand hand(String value) {
        return switch(value){case "main" -> InteractionHand.MAIN_HAND;case "offhand" -> InteractionHand.OFF_HAND;default -> throw new IllegalArgumentException("Hand must be main or offhand");};
    }
    private static int menuSlot(int slot){return slot==40?45:slot<9?slot+36:slot;}
    public static int resolve(MinePilotServerPlayer p, JsonObject args) {
        if(args.has("slot")){
            int slot=PlacementTools.integer(args,"slot",-1);
            if(slot<0 || slot>35 && slot!=40)throw new IllegalArgumentException("Use storage slot 0..35 or offhand slot 40");
            var s=p.getInventory().getItem(slot);
            if(!matches(p,s,args))throw new IllegalArgumentException("Slot no longer contains the requested item identity");
            return slot;
        }
        if(!args.has("item") && !args.has("entry_id"))throw new IllegalArgumentException("Provide item, entry_id or slot");
        int found=-1;ItemStack identity=null;
        for(int slot=0;slot<=40;slot++){
            if(slot>=36 && slot<40)continue;
            var s=p.getInventory().getItem(slot);
            if(!matches(p,s,args))continue;
            if(identity!=null && !ItemStack.isSameItemSameComponents(identity,s))throw new IllegalArgumentException("Item has distinct components or durability; select an exact slot");
            if(found<0){found=slot;identity=s;}
        }
        if(found<0)throw new IllegalArgumentException("Requested item is absent; no item was created");return found;
    }
    private static boolean matches(MinePilotServerPlayer p,ItemStack s,JsonObject a){
        String item=PlacementTools.string(a,"item","");
        if(item.equals("air") || item.equals("minecraft:air"))return s.isEmpty();
        if(s.isEmpty())return false;
        return (item.isEmpty() || BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(item))
                && (!a.has("entry_id") || p.inventoryLedger.key(s).equals(PlacementTools.string(a,"entry_id","")));
    }
    public static JsonObject equip(AgentRuntime r,JsonObject args){
        if(!r.server().isSameThread())throw new IllegalStateException("Hands require the server thread");
        var n=r.navigation().status();
        if(r.placement().executing() || r.mining().ownsBody() || r.collection().ownsBody() || r.jumpActive() || r.turnActive() || !n.phase().terminal() && n.phase()!=dev.mcai.companion.agent.navigation.NavigationToolCoordinator.Phase.IDLE)
            throw new IllegalStateException("Pause/cancel the active body job before changing hands");
        equipSlot(r.player(),resolve(r.player(),args),hand(PlacementTools.string(args,"hand","main")));
        return describe(r.player());
    }
    public static void equipSlot(MinePilotServerPlayer p,int source,InteractionHand hand){
        if(p.containerMenu!=p.inventoryMenu || !p.inventoryMenu.getCarried().isEmpty())throw new IllegalStateException("Close the external menu and clear its cursor before switching hands");
        int target=hand==InteractionHand.MAIN_HAND?p.getInventory().getSelectedSlot():40;
        if(source==target)return;
        if(hand==InteractionHand.MAIN_HAND && source<9){
            p.connection.handleSetCarriedItem(new ServerboundSetCarriedItemPacket(source));return;
        }
        var before=p.getInventory().getItem(source).copy();var previous=p.getInventory().getItem(target).copy();
        p.inventoryMenu.clicked(menuSlot(source),target,ContainerInput.SWAP,p);
        p.inventoryMenu.broadcastChanges();
        if(!ItemStack.matches(before,p.getInventory().getItem(target)) || !ItemStack.matches(previous,p.getInventory().getItem(source)))
            throw new IllegalStateException("Native inventory swap did not preserve the expected stacks; inspect inventory before continuing");
    }
    public static JsonObject describe(MinePilotServerPlayer p){
        var result=new JsonObject();
        for(var h:InteractionHand.values()){
            var s=p.getItemInHand(h);var row=new JsonObject();row.addProperty("slot",h==InteractionHand.MAIN_HAND?p.getInventory().getSelectedSlot():40);
            row.addProperty("item",s.isEmpty()?"air":BuiltInRegistries.ITEM.getKey(s.getItem()).toString());row.addProperty("count",s.getCount());
            if(!s.isEmpty())row.addProperty("entryId",p.inventoryLedger.key(s));result.add(h==InteractionHand.MAIN_HAND?"main":"offhand",row);
        }return result;
    }
    public static JsonObject capacity(MinePilotServerPlayer p,JsonObject args){
        ItemStack wanted=null;
        if(args.has("item") || args.has("entry_id") || args.has("slot")){
            if(args.has("entry_id") || args.has("slot"))wanted=p.getInventory().getItem(resolve(p,args)).copyWithCount(1);
            else {
                var id=net.minecraft.resources.Identifier.parse(PlacementTools.string(args,"item",""));
                if(!BuiltInRegistries.ITEM.containsKey(id))throw new IllegalArgumentException("Unknown item ID");
                wanted=new ItemStack(BuiltInRegistries.ITEM.getValue(id));
                if(wanted.isEmpty())throw new IllegalArgumentException("Air has no storage capacity");
            }
        }
        int empty=0,partial=0,free=0;
        for(int i=0;i<36;i++){
            var s=p.getInventory().getItem(i);
            if(s.isEmpty()){empty++;if(wanted!=null)free+=p.inventoryMenu.getSlot(menuSlot(i)).getMaxStackSize(wanted);}
            else if(wanted!=null && ItemStack.isSameItemSameComponents(s,wanted))partial+=Math.max(0,p.inventoryMenu.getSlot(menuSlot(i)).getMaxStackSize(wanted)-s.getCount());
        }
        var out=new JsonObject();out.addProperty("storageSlots",36);out.addProperty("emptyStorageSlots",empty);out.add("hands",describe(p));
        if(wanted!=null){out.addProperty("item",BuiltInRegistries.ITEM.getKey(wanted.getItem()).toString());out.addProperty("compatibleStackSpace",partial);out.addProperty("additionalItemCapacity",partial+free);out.addProperty("assumption","These free slots are dedicated to this exact item/components; capacities for different items cannot be summed. Armor/offhand are excluded.");}
        return out;
    }
}
