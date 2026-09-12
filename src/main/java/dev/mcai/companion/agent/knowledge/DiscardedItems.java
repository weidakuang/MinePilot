package dev.mcai.companion.agent.knowledge;

import com.google.gson.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

/** Entity-level avoidance deliberately covers mixed stacks; never guesses unit identity. */
public final class DiscardedItems {
    private static final String KEY="minepilot.discardOwners";
    private DiscardedItems() {}
    private static JsonObject owners(ItemEntity e){
        try{return JsonParser.parseString(e.getPersistentData().getString(KEY).orElse("{}")).getAsJsonObject();}
        catch(RuntimeException invalid){return new JsonObject();}
    }
    public static boolean avoided(ItemEntity e,Player p){return owners(e).has(p.getUUID().toString());}
    public static void mark(ItemEntity e,Player p,String request){
        var owners=owners(e);owners.addProperty(p.getUUID().toString(),request);e.getPersistentData().putString(KEY,owners.toString());
        var rows=DropProvenance.segments(e,e.getItem().getCount());for(var v:rows){var source=v.getAsJsonObject().getAsJsonObject("source");source.addProperty("discardOwner",p.getUUID().toString());source.addProperty("discardRequest",request);}DropProvenance.save(e,rows);
    }
    public static void merged(ItemEntity to,ItemEntity from){var owners=owners(to);owners(from).entrySet().forEach(e->owners.add(e.getKey(),e.getValue()));if(!owners.isEmpty())to.getPersistentData().putString(KEY,owners.toString());}
    public static void reclaim(ItemEntity e,Player p){var owners=owners(e);owners.remove(p.getUUID().toString());e.getPersistentData().putString(KEY,owners.toString());}
}
