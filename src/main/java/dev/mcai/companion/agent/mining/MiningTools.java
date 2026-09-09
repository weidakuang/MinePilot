package dev.mcai.companion.agent.mining;

import java.util.UUID;
import com.google.gson.*;
import dev.mcai.companion.agent.AgentRuntime;
import net.minecraft.core.BlockPos;

/** Shared public/internal tool definitions for the first legal mining capability. */
public final class MiningTools {
    public static final java.util.Set<String> NAMES=java.util.Set.of("equip_tool","plan_mining","choose_mining","mining_status","pause_mining","resume_mining","cancel_mining");
    public static JsonArray definitions(){
        var result=new JsonArray();
        var equip=new JsonObject();equip.add("slot",field("integer","Existing hotbar slot 0..8. An empty slot selects bare hands; no item is created."));
        result.add(tool("equip_tool","Select an existing hotbar tool while idle; inspect inventory equipment for durability.",equip,"slot"));
        var plan=new JsonObject();for(String axis:java.util.List.of("x","y","z"))plan.add(axis,field("integer","Absolute block coordinate "+axis));
        plan.add("auto_tool",field("boolean","Default true. Preview the best available inventory tool and equip it only after approval; false keeps the current hand."));
        plan.add("require_harvest",field("boolean","Default true: require the held tool to harvest drops. False permits explicitly requested clearance without harvest."));
        result.add(tool("plan_mining","Evaluate ONE sensed, reachable block with a previewed inventory tool. No movement or destruction. Other positions require navigation first; no region mining yet.",plan,"x","y","z"));
        var choose=new JsonObject();choose.add("request_id",field("string","Current PLAN_READY request UUID"));choose.add("option_id",field("string","Exact returned optionId"));
        result.add(tool("choose_mining","Approve and start the evaluated single-block normal-player break. Runs asynchronously; chat remains available. Completion does not prove pickup.",choose,"request_id","option_id"));
        result.add(tool("mining_status","Read mining progress, physical body position, block outcome, emitted drop identities and tool wear. Verify acquisition independently with inventory_events.",new JsonObject()));
        for(String action:java.util.List.of("pause_mining","resume_mining","cancel_mining")){
            var params=new JsonObject();params.add("request_id",field("string","Exact current request UUID"));
            result.add(tool(action,action.equals("resume_mining")?"Revalidate and restart a paused break from zero progress.":"Abort the unfinished block break immediately. Does not restore already broken blocks.",params,"request_id"));
        }
        return result;
    }
    public static JsonObject execute(AgentRuntime runtime,String name,JsonObject args){
        if(!name.equals("mining_status") && (runtime.collection().ownsBody() || runtime.placement().ownsBody()))throw new IllegalStateException("Use collection pause/cancel; its child mining operation is owned by that job");
        var mining=runtime.mining();
        return switch(name){
            case "equip_tool" -> mining.equip(integer(args,"slot"));
            case "plan_mining" -> mining.plan(new BlockPos(integer(args,"x"),integer(args,"y"),integer(args,"z")),!args.has("require_harvest") || args.get("require_harvest").getAsBoolean(),!args.has("auto_tool") || args.get("auto_tool").getAsBoolean());
            case "choose_mining" -> mining.choose(id(args),args.get("option_id").getAsString());
            case "mining_status" -> mining.status();
            case "pause_mining" -> mining.interrupt(id(args),true,"Paused by controller");
            case "cancel_mining" -> mining.interrupt(id(args),false,"Cancelled by controller");
            case "resume_mining" -> mining.resume(id(args));
            default -> throw new IllegalArgumentException("Unknown mining tool");
        };
    }
    private static UUID id(JsonObject args){return UUID.fromString(args.get("request_id").getAsString());}
    private static int integer(JsonObject args,String name){
        if(!args.has(name) || !args.get(name).isJsonPrimitive() || !args.getAsJsonPrimitive(name).isNumber())throw new IllegalArgumentException("Missing integer "+name);
        try{return args.get(name).getAsBigDecimal().intValueExact();}catch(ArithmeticException e){throw new IllegalArgumentException("Invalid integer "+name);}
    }
    private static JsonObject field(String type,String description){var out=new JsonObject();out.addProperty("type",type);out.addProperty("description",description);return out;}
    private static JsonObject tool(String name,String description,JsonObject properties,String... required){
        var out=new JsonObject();out.addProperty("name",name);out.addProperty("description",description);
        var schema=new JsonObject();schema.addProperty("type","object");schema.add("properties",properties);schema.addProperty("additionalProperties",false);
        var req=new JsonArray();for(var r:required)req.add(r);schema.add("required",req);out.add("inputSchema",schema);
        var annotation=new JsonObject();annotation.addProperty("readOnlyHint",name.equals("mining_status"));annotation.addProperty("destructiveHint",name.equals("choose_mining") || name.equals("resume_mining"));out.add("annotations",annotation);return out;
    }
}
