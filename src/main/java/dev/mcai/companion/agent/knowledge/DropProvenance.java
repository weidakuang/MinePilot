package dev.mcai.companion.agent.knowledge;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Provenance travels with the physical entity through save/load. No proximity-based giver guesses. */
public final class DropProvenance {
    private DropProvenance() {}
    public static void mark(ItemEntity item,String category,Entity actor) {
        var data=item.getPersistentData();
        if(data.contains("minepilot.nativeCause"))try{var cause=com.google.gson.JsonParser.parseString(data.getString("minepilot.nativeCause").orElse("{}")).getAsJsonObject();cause.addProperty("category",category);cause.remove("actorId");cause.remove("actorName");if(actor!=null){cause.addProperty("actorId",actor.getUUID().toString());cause.addProperty("actorName",InventoryLedger.bounded(actor.getName().getString(),128));}data.putString("minepilot.nativeCause",cause.toString());}catch(RuntimeException ignored){data.remove("minepilot.nativeCause");}
        data.putString("minepilot.source",category);if(actor==null){data.remove("minepilot.actorId");data.remove("minepilot.actor");}
        if(actor!=null){data.putString("minepilot.actorId",actor.getUUID().toString());data.putString("minepilot.actor",InventoryLedger.bounded(actor.getName().getString(),128));}
    }
    private static JsonObject legacySource(ItemEntity item,Player agent) {
        var data=item.getPersistentData();String category=data.getString("minepilot.source").orElse("unknown");
        String actor=data.getString("minepilot.actorId").orElse("");
        if(category.equals("death_drop") && agent!=null && actor.equals(agent.getUUID().toString()))category="self_loot";
        JsonObject out=new JsonObject();out.addProperty("category",category);out.addProperty("confidence",category.equals("unknown")?"unknown":"entity_origin_event");
        out.addProperty("mergedStackLineageKnown",false);
        if(data.contains("minepilot.nativeCause")){try{var cause=com.google.gson.JsonParser.parseString(data.getString("minepilot.nativeCause").orElse("{}")).getAsJsonObject();cause.entrySet().forEach(e->out.add(e.getKey(),e.getValue()));}catch(RuntimeException ignored){out.addProperty("nativeCauseUnreadable",true);}}
        out.addProperty("attributionNote","The origin event belongs to this entity. Vanilla can merge stacks; attribution of every item in a merged stack is not established.");
        if(!actor.isEmpty()){out.addProperty("actorId",actor);out.addProperty("actorName",data.getString("minepilot.actor").orElse(""));}
        if(category.equals("mined_block") && data.contains("minepilot.miningRequest")) {
            out.addProperty("requestId",data.getString("minepilot.miningRequest").orElse(""));
            out.addProperty("block",data.getString("minepilot.sourceBlock").orElse(""));
            out.addProperty("dimension",data.getString("minepilot.sourceDimension").orElse(""));
            for(String axis:java.util.List.of("X","Y","Z"))out.addProperty(axis.toLowerCase(java.util.Locale.ROOT),data.getInt("minepilot.source"+axis).orElse(0));
        }
        return out;
    }

    public static void markCause(ItemEntity item,JsonObject cause){
        item.getPersistentData().putString("minepilot.source",cause.get("category").getAsString());
        item.getPersistentData().putString("minepilot.nativeCause",cause.toString());
        if(cause.has("actorId")){item.getPersistentData().putString("minepilot.actorId",cause.get("actorId").getAsString());item.getPersistentData().putString("minepilot.actor",cause.get("actorName").getAsString());}
        else {item.getPersistentData().remove("minepilot.actorId");item.getPersistentData().remove("minepilot.actor");}
    }
    private static final String LINEAGE="minepilot.lineage.v1";
    public static com.google.gson.JsonArray segments(ItemEntity item,int count){
        var rows=new com.google.gson.JsonArray();int left=Math.max(0,count);
        String stored=item.getPersistentData().getString(LINEAGE).orElse("");
        if(!stored.isEmpty() && stored.length()<=65536)try{
            for(var value:com.google.gson.JsonParser.parseString(stored).getAsJsonArray()){
                var row=value.getAsJsonObject();int n=Math.min(left,Math.max(0,row.get("count").getAsInt()));if(n==0)continue;
                if(rows.size()>=64)break;var copy=row.deepCopy();copy.addProperty("count",n);rows.add(copy);left-=n;
            }
        }catch(RuntimeException invalid){rows=new com.google.gson.JsonArray();left=count;}
        if(left>0){var row=new JsonObject();row.addProperty("count",left);var source=legacySource(item,null);source.remove("attributionNote");
            if(!stored.isEmpty()){source=new JsonObject();source.addProperty("category","unknown");source.addProperty("reason","Lineage metadata did not cover this quantity");}
            source.addProperty("originEntityId",item.getUUID().toString());row.add("source",source);rows.add(row);}
        return rows;
    }
    public static void save(ItemEntity item,com.google.gson.JsonArray rows){item.getPersistentData().putString(LINEAGE,rows.toString());}
    public static com.google.gson.JsonArray take(com.google.gson.JsonArray rows,int count){
        var result=new com.google.gson.JsonArray();int left=count;
        while(left>0 && !rows.isEmpty()){
            var first=rows.get(0).getAsJsonObject();int n=Math.min(left,first.get("count").getAsInt());var part=first.deepCopy();part.addProperty("count",n);result.add(part);
            int remainder=first.get("count").getAsInt()-n;if(remainder==0)rows.remove(0);else first.addProperty("count",remainder);left-=n;
        }return result;
    }
    public static void merged(ItemEntity to,ItemEntity from,com.google.gson.JsonArray toBefore,com.google.gson.JsonArray fromBefore,int moved){
        if(moved<=0)return;DiscardedItems.merged(to,from);var movedRows=take(fromBefore,moved);
        for(var value:movedRows){var row=value.getAsJsonObject();var source=row.getAsJsonObject("source");
            var transfers=source.has("transfers")?source.getAsJsonArray("transfers"):new com.google.gson.JsonArray();
            if(transfers.size()<8){var t=new JsonObject();t.addProperty("kind","entity_merge");t.addProperty("from",from.getUUID().toString());t.addProperty("to",to.getUUID().toString());t.addProperty("count",row.get("count").getAsInt());transfers.add(t);}else source.addProperty("transferHistoryTruncated",true);
            source.add("transfers",transfers);toBefore.add(row);
        }
        save(to,toBefore);save(from,fromBefore);
    }
    public static JsonObject summarize(com.google.gson.JsonArray rows,Player agent){
        var out=new JsonObject();String signature=null;JsonObject common=null;boolean same=true,known=true;int count=0;
        for(var v:rows){var row=v.getAsJsonObject();var s=row.getAsJsonObject("source");
            if(agent!=null && s.get("category").getAsString().equals("death_drop") && s.has("actorId") && s.get("actorId").getAsString().equals(agent.getUUID().toString()))s.addProperty("category","self_loot");
            known &= !s.get("category").getAsString().equals("unknown") && !(s.has("originCategory") && s.get("originCategory").getAsString().equals("unknown"));count+=row.get("count").getAsInt();String identity=s.toString();
            if(signature==null){signature=identity;common=s;}else same &= signature.equals(identity);
        }
        if(same && common!=null)out=common.deepCopy();else out.addProperty("category",rows.isEmpty()?"unknown":"mixed");
        out.addProperty("confidence","causal_native_events");out.addProperty("mergedStackLineageKnown",true);out.addProperty("allOriginsKnown",known && !rows.isEmpty());out.addProperty("lineageCount",count);out.add("lineage",rows);
        out.addProperty("attributionNote","Native transfers preserve counted origin segments. Unknown origins stay unknown. Equal units are allocated in recorded FIFO order on partial pickup, not individually distinguishable physical objects.");return out;
    }
    public static JsonObject source(ItemEntity item,Player agent){return summarize(segments(item,item.getItem().getCount()),agent);}
    /** Default model observations summarize long histories; full carried segments have a separate paged tool. */
    public static JsonObject compact(JsonObject source,int limit){
        var out=new JsonObject();for(String key:java.util.List.of("category","discardOwner","discardRequest","originCategory","actorId","actorName","requestId","block","dimension","x","y","z","originEntityId","sourceEntityType","confidence","mergedStackLineageKnown","allOriginsKnown","lineageCount","lastTransfer","transferHistoryTruncated"))if(source.has(key))out.add(key,source.get(key).deepCopy());
        if(source.has("lineage")){var all=source.getAsJsonArray("lineage");var rows=new com.google.gson.JsonArray();for(int i=0;i<Math.min(limit,all.size());i++){var row=all.get(i).getAsJsonObject();var copy=new JsonObject();copy.add("count",row.get("count"));copy.add("source",compact(row.getAsJsonObject("source"),0));rows.add(copy);}out.add("lineage",rows);out.addProperty("lineageSegments",all.size());out.addProperty("lineageDetailsTruncated",rows.size()<all.size());}
        return out;
    }
    public static JsonObject pickedUpSource(ItemEntity item,Player agent,com.google.gson.JsonArray before,int count){
        var picked=take(before,count);save(item,before);return summarize(picked,agent);
    }
}
