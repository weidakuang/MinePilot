package dev.mcai.companion.agent.knowledge;

import com.google.gson.*;
import java.util.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Counted FIFO origins pooled by item components; slot moves do not change origin.
 * Stored in native player persistent data, without modifying stack components or merge rules. */
public final class CarriedProvenance {
    private static final String DATA="minepilot.carriedOrigins.v1";
    private static final Map<Player,State> STATES=new WeakHashMap<>();
    private static final class State {
        final JsonObject items;
        Map<String,Integer> lastCounts=Map.of();
        final LinkedHashMap<String,ItemStack> variants=new LinkedHashMap<>();
        State(Player p){JsonObject parsed=new JsonObject();String text=p.getPersistentData().getString(DATA).orElse("");
            if(!text.isEmpty() && text.length()<=500_000)try{parsed=JsonParser.parseString(text).getAsJsonObject();}catch(RuntimeException ignored){}
            items=parsed;
        }
        String key(Player p,ItemStack stack){
            var normalized=stack.copyWithCount(1);if(normalized.isDamageableItem())normalized.setDamageValue(0);
            for(var e:variants.entrySet())if(ItemStack.isSameItemSameComponents(e.getValue(),normalized))return e.getKey();
            String key=InventoryLedger.fingerprint(p,normalized);if(variants.size()>=128)variants.remove(variants.keySet().iterator().next());variants.put(key,normalized);return key;
        }
        void save(Player p){String value=items.toString();if(value.length()<=500_000)p.getPersistentData().putString(DATA,value);
            else {items.entrySet().removeIf(e->true);p.getPersistentData().remove(DATA);}}
    }
    private static State state(Player p){return STATES.computeIfAbsent(p,State::new);}
    private static JsonArray rows(State s,String key,int count){
        JsonArray out=new JsonArray();int left=Math.max(0,count);
        try{if(s.items.has(key))for(var v:s.items.getAsJsonArray(key)){
            var row=v.getAsJsonObject();if(!row.has("source") || !row.getAsJsonObject("source").has("category"))throw new IllegalArgumentException();
            int n=Math.min(left,Math.max(0,row.get("count").getAsInt()));if(n>0 && out.size()<64){var copy=row.deepCopy();copy.addProperty("count",n);out.add(copy);left-=n;}
        }}catch(RuntimeException invalid){out=new JsonArray();left=count;}
        if(left>0){var row=new JsonObject();row.addProperty("count",left);var unknown=new JsonObject();unknown.addProperty("category","unknown");unknown.addProperty("reason","No observed transfer covers these carried units");row.add("source",unknown);out.add(row);}return out;
    }
    private static int total(JsonArray rows){int n=0;for(var v:rows)n+=v.getAsJsonObject().get("count").getAsInt();return n;}
    private static int count(Player p,State s,String key){int n=0;for(int slot=0;slot<p.getInventory().getContainerSize();slot++){var stack=p.getInventory().getItem(slot);if(!stack.isEmpty() && s.key(p,stack).equals(key))n+=stack.getCount();}return n;}
    private static JsonArray reconcile(State s,String key,int count){
        int old=0;try{if(s.items.has(key))old=total(s.items.getAsJsonArray(key));}catch(RuntimeException ignored){}
        var result=rows(s,key,Math.max(old,count));if(old>count)DropProvenance.take(result,old-count);
        return result;
    }
    public static void sync(Player p){
        var s=state(p);var counts=new HashMap<String,Integer>();for(int slot=0;slot<p.getInventory().getContainerSize();slot++){var stack=p.getInventory().getItem(slot);if(!stack.isEmpty())counts.merge(s.key(p,stack),stack.getCount(),Integer::sum);}
        if(s.lastCounts.equals(counts))return;s.lastCounts=Map.copyOf(counts);
        String before=s.items.toString();s.items.entrySet().removeIf(e->!counts.containsKey(e.getKey()));
        for(var e:counts.entrySet())s.items.add(e.getKey(),reconcile(s,e.getKey(),e.getValue()));
        if(!before.equals(s.items.toString()))s.save(p);
    }
    public static void acquired(Player p,ItemStack stack,JsonObject source){
        var s=state(p);String key=s.key(p,stack);int current=count(p,s,key),amount=Math.min(stack.getCount(),current);
        var rows=reconcile(s,key,current-amount);var incoming=new JsonArray();
        if(source.has("lineage"))incoming=source.getAsJsonArray("lineage").deepCopy();
        else {var row=new JsonObject();row.addProperty("count",amount);row.add("source",source.deepCopy());incoming.add(row);}
        for(var v:DropProvenance.take(incoming,amount))rows.add(v);
        s.items.add(key,rows);sync(p);s.save(p);
    }
    /** Only consume a recorded balance decrease. Synthetic drops never take an unrelated carried origin. */
    public static void tossed(Player p,ItemEntity item){
        var s=state(p);String key=s.key(p,item.getItem());if(!s.items.has(key))return;
        var available=rows(s,key,total(s.items.getAsJsonArray(key)));int decrease=total(available)-count(p,s,key);
        if(decrease!=item.getItem().getCount())return;
        var moved=DropProvenance.take(available,decrease);
        for(var v:moved){var source=v.getAsJsonObject().getAsJsonObject("source");
            var chain=source.has("transfers")?source.getAsJsonArray("transfers"):new JsonArray();
            var event=new JsonObject();event.addProperty("kind","player_toss");event.addProperty("actorId",p.getUUID().toString());event.addProperty("actorName",InventoryLedger.bounded(p.getName().getString(),128));event.addProperty("gameTick",p.level().getGameTime());
            if(chain.size()<8)chain.add(event);else source.addProperty("transferHistoryTruncated",true);source.add("transfers",chain);source.add("lastTransfer",event);
        }
        DropProvenance.save(item,moved);s.items.add(key,available);s.save(p);
    }
    public static JsonObject describe(Player p,String key,int count){
        var s=state(p);return DropProvenance.summarize(rows(s,key,count),p);
    }
    public static void assign(Player p,String key,JsonArray origins){var s=state(p);s.items.add(key,origins.deepCopy());s.save(p);}
    public static void forgetCache(Player p){STATES.remove(p);}
}
