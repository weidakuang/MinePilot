package dev.mcai.companion.agent.knowledge;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Provenance travels with the physical entity through save/load. No proximity-based giver guesses. */
public final class DropProvenance {
    private DropProvenance() {}
    public static void mark(ItemEntity item,String category,Entity actor) {
        var data=item.getPersistentData();data.putString("minepilot.source",category);
        if(actor!=null){data.putString("minepilot.actorId",actor.getUUID().toString());data.putString("minepilot.actor",InventoryLedger.bounded(actor.getName().getString(),128));}
    }
    public static JsonObject source(ItemEntity item,Player agent) {
        var data=item.getPersistentData();String category=data.getString("minepilot.source").orElse("unknown");
        String actor=data.getString("minepilot.actorId").orElse("");
        if(category.equals("death_drop") && actor.equals(agent.getUUID().toString()))category="self_loot";
        JsonObject out=new JsonObject();out.addProperty("category",category);out.addProperty("confidence",category.equals("unknown")?"unknown":"entity_origin_event");
        out.addProperty("mergedStackLineageKnown",false);
        out.addProperty("attributionNote","The origin event belongs to this entity. Vanilla can merge stacks; attribution of every item in a merged stack is not established.");
        if(!actor.isEmpty()){out.addProperty("actorId",actor);out.addProperty("actorName",data.getString("minepilot.actor").orElse(""));}
        if(category.equals("mined_block")) {
            out.addProperty("requestId",data.getString("minepilot.miningRequest").orElse(""));
            out.addProperty("block",data.getString("minepilot.sourceBlock").orElse(""));
            out.addProperty("dimension",data.getString("minepilot.sourceDimension").orElse(""));
            for(String axis:java.util.List.of("X","Y","Z"))out.addProperty(axis.toLowerCase(java.util.Locale.ROOT),data.getInt("minepilot.source"+axis).orElse(0));
        }
        return out;
    }
}
