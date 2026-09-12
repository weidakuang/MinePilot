package dev.mcai.companion.agent.knowledge;

import com.google.gson.*;
import java.util.*;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.placement.HandController;
import dev.mcai.companion.agent.navigation.NavigationToolCoordinator;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Server-thread native tosses; unrelated work retains its body/controller ownership. */
public final class ItemDropService {
    private final AgentRuntime r;
    private final LinkedHashMap<String,JsonObject> receipts=new LinkedHashMap<>();
    private static final String CACHE="minepilot.dropReceipts.v1";
    private record Part(int slot,ItemStack before,int amount) {}
    public ItemDropService(AgentRuntime r){this.r=r;
        try{var saved=JsonParser.parseString(r.player().getPersistentData().getString(CACHE).orElse("{}")).getAsJsonObject();for(var e:saved.entrySet())if(receipts.size()<32)receipts.put(e.getKey(),e.getValue().getAsJsonObject());}catch(RuntimeException ignored){receipts.clear();}
    }
    private void remember(String key,JsonObject value){
        receipts.put(key,value);while(receipts.size()>32)receipts.remove(receipts.keySet().iterator().next());
        var all=new JsonObject();receipts.forEach(all::add);while(all.toString().length()>250000 && receipts.size()>1){String first=receipts.keySet().iterator().next();receipts.remove(first);all.remove(first);}r.player().getPersistentData().putString(CACHE,all.toString());
    }
    private void thread(){if(!r.server().isSameThread())throw new IllegalStateException("Inventory mutations require the server thread");}
    public int reserved(ItemStack stack){
        int n=Math.max(r.mining().reservedCount(stack),r.collection().reservedCount(stack));n=Math.max(n,r.placement().reservedCount(stack));n=Math.max(n,r.excavation().reservedCount(stack));
        var nav=r.navigation().status();if(!nav.phase().terminal() && nav.phase()!=NavigationToolCoordinator.Phase.IDLE && nav.selectedOption()!=null){
            String key=r.player().inventoryLedger.key(stack);for(var option:nav.routeOptions())if(option.optionId().equals(nav.selectedOption()))for(var material:option.supportMaterials())if(material.entryId().equals(key))n=Math.max(n,material.count());
        }return n;
    }
    private boolean lockedSlot(int slot){var p=r.player();var stack=p.getInventory().getItem(slot);return slot==p.getInventory().getSelectedSlot() && (r.mining().reservedCount(stack)>0 || r.collection().reservedCount(stack)>0);}
    private int count(ItemStack prototype,boolean policy){int n=0;var p=r.player();for(int i=0;i<=40;i++){if(i>=36 && i<40)continue;var s=p.getInventory().getItem(i);if(!s.isEmpty() && (policy?p.inventoryLedger.key(s).equals(p.inventoryLedger.key(prototype)):ItemStack.isSameItemSameComponents(s,prototype)))n+=s.getCount();}return n;}
    public JsonObject drop(JsonObject a){
        thread();String key=a.has("request_key")?a.get("request_key").getAsString():"";
        if(key.isBlank() || key.length()>96)throw new IllegalArgumentException("request_key must contain 1..96 characters; reuse exactly for uncertain retries");
        if(receipts.containsKey(key)){var saved=receipts.get(key);if(!saved.getAsJsonObject("arguments").equals(a))throw new IllegalArgumentException("request_key already used for different arguments");var out=saved.getAsJsonObject("result").deepCopy();out.addProperty("replayed",true);return out;}
        var p=r.player();if(!p.isAlive() || p.isSpectator() || !p.canDropItems())throw new IllegalStateException("Body cannot drop items now");
        if(p.containerMenu!=p.inventoryMenu || !p.inventoryMenu.getCarried().isEmpty())throw new IllegalStateException("Close the container and clear its cursor before dropping carried items");
        String reason=a.has("reason")?a.get("reason").getAsString():"capacity";if(!Set.of("capacity","player_request").contains(reason))throw new IllegalArgumentException("reason must be capacity or player_request");
        String context=a.has("player_request")?InventoryLedger.bounded(a.get("player_request").getAsString(),512):"";
        if(reason.equals("player_request") && context.isBlank())throw new IllegalArgumentException("Record the actual player instruction in player_request");
        var items=a.getAsJsonArray("items");if(items==null || items.isEmpty() || items.size()>8)throw new IllegalArgumentException("Supply 1..8 exact item selections");
        var parts=new ArrayList<Part>();var used=new HashSet<Integer>();var requested=new HashMap<String,Integer>();
        for(var value:items){var selection=value.getAsJsonObject();int amount=selection.get("count").getAsBigDecimal().intValueExact();if(amount<1 || amount>4096)throw new IllegalArgumentException("count must be 1..4096");
            if(selection.has("slot") && !selection.has("entry_id"))throw new IllegalArgumentException("A slot selection also requires its observed entry_id to reject stale slot contents");
            int first=HandController.resolve(p,selection);if(selection.has("slot") && lockedSlot(first))throw new IllegalArgumentException("RESERVED_SLOT: active task requires the exact held stack in slot "+first+"; select an unreserved copy or cancel that job");var prototype=p.getInventory().getItem(first).copy();if(prototype.isEmpty())throw new IllegalArgumentException("Cannot drop air");
            if(selection.has("expected_damage") && prototype.getDamageValue()!=selection.get("expected_damage").getAsBigDecimal().intValueExact())throw new IllegalArgumentException("Tool durability changed; observe the exact slot again");
            if(!reason.equals("player_request") && p.inventoryLedger.importance(prototype)<=2)throw new IllegalArgumentException("PROTECTED_ITEM: importance 0..2 requires an actual player instruction; no items dropped");
            String identity=p.inventoryLedger.key(prototype);requested.merge(identity,amount,Integer::sum);int free=Math.max(0,count(prototype,true)-reserved(prototype));
            if(requested.get(identity)>free)throw new IllegalArgumentException("RESERVED_ITEM: "+identity+", reserved="+reserved(prototype)+", free="+free+". Cancel the affected job to release its remaining resources; no items dropped");
            int remaining=amount;for(int slot=0;slot<=40 && remaining>0;slot++){if(slot>=36 && slot<40 || selection.has("slot") && slot!=first || lockedSlot(slot))continue;var stack=p.getInventory().getItem(slot);if(!ItemStack.isSameItemSameComponents(prototype,stack))continue;
                if(!used.add(slot))throw new IllegalArgumentException("Overlapping item selections; use one count per identity or distinct slots");int take=Math.min(remaining,stack.getCount());parts.add(new Part(slot,stack.copy(),take));remaining-=take;
            }if(remaining>0)throw new IllegalArgumentException("Requested exact quantity is absent; no items dropped");
        }
        CarriedProvenance.sync(p);var result=new JsonObject();result.addProperty("requestKey",key);result.addProperty("phase","INDETERMINATE");result.addProperty("reason",reason);result.addProperty("playerRequest",context);result.addProperty("requestedCount",parts.stream().mapToInt(Part::amount).sum());result.addProperty("actualDroppedCount",0);result.addProperty("replayed",false);result.addProperty("retryRetention","Last 32 receipts, bounded to 250 KB, stored with the body; keys must never be reused for a new action");
        var rows=new JsonArray();result.add("receipts",rows);var cached=new JsonObject();cached.add("arguments",a.deepCopy());cached.add("result",result);remember(key,cached);
        int total=0;boolean failed=false;
        try{for(var part:parts){
            if(!ItemStack.matches(p.getInventory().getItem(part.slot),part.before))throw new IllegalStateException("Native hook changed a remaining slot; batch stopped");
            int before=count(part.before,false);var removed=p.getInventory().removeItem(part.slot,part.amount);ItemEntity entity=null;
            entity=p.drop(removed,true);
            boolean spawned=entity!=null && entity.isAlive() && p.level().getEntity(entity.getUUID())==entity;
            var row=new JsonObject();row.addProperty("slot",part.slot);row.addProperty("entryId",p.inventoryLedger.key(part.before));row.addProperty("attempted",part.amount);row.addProperty("inventoryBefore",before);
            if(!spawned){restoreMissing(part,before);row.addProperty("outcome","NATIVE_TOSS_CANCELLED");row.addProperty("actualDropped",0);failed=true;}
            else {DiscardedItems.mark(entity,p,key);row.addProperty("entityId",entity.getUUID().toString());row.addProperty("x",entity.getX());row.addProperty("y",entity.getY());row.addProperty("z",entity.getZ());row.add("stack",p.inventoryLedger.describe(entity.getItem()));row.add("origins",DropProvenance.compact(DropProvenance.source(entity,p),4));
                boolean matched=ItemStack.isSameItemSameComponents(part.before,entity.getItem()) && entity.getItem().getCount()==part.amount && before-count(part.before,false)==part.amount;
                row.addProperty("outcome",matched?"DROPPED":"NATIVE_HOOK_CHANGED_OUTCOME");row.addProperty("actualDropped",entity.getItem().getCount());total+=entity.getItem().getCount();failed|=!matched;
            }
            row.addProperty("inventoryAfter",count(part.before,false));rows.add(row);result.addProperty("actualDroppedCount",total);remember(key,cached);if(failed)break;
        }result.addProperty("phase",failed?(total>0?"PARTIAL":"CANCELLED"):"COMPLETED");}
        catch(RuntimeException failure){result.addProperty("phase","INDETERMINATE");result.addProperty("error",InventoryLedger.bounded(failure.getMessage(),512));}
        p.inventoryMenu.broadcastChanges();CarriedProvenance.sync(p);p.inventoryLedger.tick();result.add("capacity",HandController.capacity(p,new JsonObject()));result.addProperty("receiptMeaning","Physical toss only; recipient pickup is not proven. Mixed merged stacks remain avoided by this Agent until reclaim_drop.");remember(key,cached);return result.deepCopy();
    }
    private void restoreMissing(Part part,int before){
        var p=r.player();int missing=Math.min(part.amount,Math.max(0,before-count(part.before,false)));if(missing==0)return;
        var current=p.getInventory().getItem(part.slot);if(current.isEmpty())p.getInventory().setItem(part.slot,part.before.copyWithCount(missing));
        else if(ItemStack.isSameItemSameComponents(current,part.before) && current.getCount()+missing<=current.getMaxStackSize())current.grow(missing);
        else throw new IllegalStateException("Cancelled native hook moved the source slot; inspect inventory before recovery");
    }
    public JsonObject reclaim(JsonObject a){thread();var entity=r.player().level().getEntity(UUID.fromString(a.get("entity_id").getAsString()));
        if(!(entity instanceof ItemEntity drop) || !drop.isAlive() || !r.perception.sensed(drop))throw new IllegalArgumentException("Reclaim requires a currently sensed dropped item UUID");
        DiscardedItems.reclaim(drop,r.player());var out=new JsonObject();out.addProperty("entityId",drop.getUUID().toString());out.addProperty("pickupAvoided",false);out.addProperty("count",drop.getItem().getCount());out.addProperty("meaning","Entire current merged stack is eligible for normal pickup; no item was moved");return out;
    }
}
