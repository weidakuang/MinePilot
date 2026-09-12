package dev.mcai.companion.agent.knowledge;

import com.google.gson.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Causal native-action scopes. Only observation metadata is changed. */
public final class ProvenanceHooks {
    private static final ThreadLocal<Deque<JsonObject>> SCOPES=ThreadLocal.withInitial(ArrayDeque::new);
    public static JsonObject cause(String category,Level level,BlockPos pos,BlockState state,Entity actor){
        var c=new JsonObject();c.addProperty("category",category);c.addProperty("eventId",UUID.randomUUID().toString());
        c.addProperty("dimension",level.dimension().identifier().toString());c.addProperty("gameTick",level.getGameTime());
        if(pos!=null){c.addProperty("x",pos.getX());c.addProperty("y",pos.getY());c.addProperty("z",pos.getZ());}
        if(state!=null)c.addProperty("block",BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        if(actor!=null){c.addProperty("actorId",actor.getUUID().toString());c.addProperty("actorName",InventoryLedger.bounded(actor.getName().getString(),128));}return c;
    }
    public static AutoCloseable enter(JsonObject cause){SCOPES.get().push(cause);return ()->{var stack=SCOPES.get();stack.pop();if(stack.isEmpty())SCOPES.remove();};}
    public static JsonObject current(){var stack=SCOPES.get();return stack.isEmpty()?null:stack.peek().deepCopy();}
    public static void spawned(ItemEntity item){
        if(item.level().isClientSide())return;var cause=current();if(cause==null)return;
        var mutable=SCOPES.get().peek();cause.remove("transferredOrigins");DropProvenance.markCause(item,cause);
        if(mutable!=null && mutable.has("transferredOrigins")){
            var moved=DropProvenance.take(mutable.getAsJsonArray("transferredOrigins"),item.getItem().getCount());
            for(var value:moved){var source=value.getAsJsonObject().getAsJsonObject("source");
                if(!source.has("originCategory"))source.add("originCategory",source.get("category").deepCopy());
                for(String name:List.of("actorId","actorName","eventId","block","dimension","x","y","z"))if(source.has(name) && !source.has("origin_"+name))source.add("origin_"+name,source.get(name).deepCopy());
                var transfers=source.has("transfers")?source.getAsJsonArray("transfers"):new JsonArray();var transfer=cause.deepCopy();transfer.remove("cause");transfer.addProperty("kind","container_drop");if(transfers.size()<8)transfers.add(transfer);else source.addProperty("transferHistoryTruncated",true);
                cause.entrySet().forEach(e->source.add(e.getKey(),e.getValue().deepCopy()));source.add("transfers",transfers);source.add("lastTransfer",transfer);
            }
            DropProvenance.save(item,moved);
        }
    }
}
