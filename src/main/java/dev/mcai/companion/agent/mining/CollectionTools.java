package dev.mcai.companion.agent.mining;

import java.util.*;
import com.google.gson.*;
import dev.mcai.companion.agent.AgentRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Identical collection interface for the internal model and authenticated external controller. */
public final class CollectionTools {
    public static final Set<String> NAMES=Set.of("inspect_tree","tree_farm","plan_collection","choose_collection","collection_status","pause_collection","resume_collection","cancel_collection");
    public static JsonArray definitions(){
        var result=new JsonArray();var inspect=new JsonObject();coords(inspect,"","A currently sensed trunk block");
        result.add(tool("inspect_tree","Inspect a connected mature-tree candidate, species, exact logs, farm/machine/construction evidence and uncertainty. Read only; never infers player ownership from looks.",inspect,"x","y","z"));
        var farms=new JsonObject();farms.add("operation",enumField("list","save","remove"));farms.add("name",field("string","Farm name (1..64 characters). Save only an area actually identified by the player or observed evidence; declarations are not discoveries."));
        farms.add("kind",enumField("manual","automated"));coords(farms,"min_","Inclusive farm minimum");coords(farms,"max_","Inclusive farm maximum");
        result.add(tool("tree_farm","Remember/list/remove bounded farm areas in this world. Automated areas remain protected; manual harvesting still requires a plan with allow_managed_grove=true. No blocks are changed.",farms,"operation"));
        var plan=new JsonObject();plan.add("resource",field("string","wood, or an exact block registry ID such as minecraft:coal_ore. No automatic fishbone strategy."));
        plan.add("output_item",field("string","Expected collectible item ID, e.g. minecraft:coal. Vanilla stone/deepslate/coal defaults are supplied; other targets require an explicit ID. Never assume block destruction proves acquisition."));
        plan.add("species",enumField("any","oak","spruce","birch","jungle","acacia","dark_oak","mangrove","cherry","pale_oak","crimson","warped"));
        plan.add("source",enumField("any","tree","drops","blocks"));plan.add("count",field("integer","Desired new inventory items, 1..64, default 4; can exceed available resources and then returns partial BLOCKED, never fake completion."));
        plan.add("radius",field("integer","Fixed task sphere radius 1..10, default 5. Does not move with the Agent or expand as it mines."));coords(plan,"","Optional fixed center, defaults to body position; provide all three coordinates or none");coords(plan,"tree_","Optional specific tree trunk; no substitution of a different tree");
        plan.add("whole_tree",field("boolean","Default true for source tree. Harvest every observed connected trunk block of the chosen tree, regardless of count. Completion requires all approved logs broken and collected. False explicitly requests only a quantity."));
        plan.add("allow_managed_grove",field("boolean","Default false. True allows evaluated mature trees in manual groves; never permits automated machinery or sapling destruction."));
        result.add(tool("plan_collection","Preview bounded continuous wood/ore collection with selected inventory-tool durability, target list, source alternatives, break-time estimate and movement budget. For wood source any offers loose logs or mature trees; source tree enforces felling. Normal movement, mining and pickup; no containers, crafting, scaffolding, access excavation or torches.",plan,"resource"));
        var choose=new JsonObject();choose.add("request_id",field("string","Current collection request UUID"));choose.add("option_id",field("string","Exact returned optionId"));
        result.add(tool("choose_collection","Approve one source/target/cost policy and start its continuous normal-player collection. No per-block model commands needed; chat and item notes remain usable.",choose,"request_id","option_id"));
        result.add(tool("collection_status","Read job progress, physical coordinates, route, pickup receipts and actual matching inventory increase. BLOCKED reports partial progress; never means success.",new JsonObject()));
        for(String name:List.of("pause_collection","resume_collection","cancel_collection")){var args=new JsonObject();args.add("request_id",field("string","Exact collection request UUID"));result.add(tool(name,"Interrupt/resume the bounded job including its break or approach route. Resume revalidates current resources; cancellation never rolls back already broken blocks.",args,"request_id"));}
        return result;
    }
    public static JsonObject execute(AgentRuntime r,String name,JsonObject a){
        return switch(name){
            case "inspect_tree" -> TreeSurvey.inspect(r,position(a,"")).json();
            case "tree_farm" -> farm(r,a);
            case "plan_collection" -> r.collection().plan(string(a,"resource",""),string(a,"output_item",""),string(a,"species","any"),string(a,"source","any"),coordinates(a,"")?new Vec3(integer(a,"x",0),integer(a,"y",0),integer(a,"z",0)):r.player().position(),integer(a,"radius",5),integer(a,"count",4),coordinates(a,"tree_")?position(a,"tree_"):null,bool(a,"allow_managed_grove",false),bool(a,"whole_tree",string(a,"source","any").equals("tree")));
            case "choose_collection" -> r.collection().choose(id(a),string(a,"option_id",""));
            case "collection_status" -> r.collection().status();
            case "pause_collection" -> r.collection().interrupt(id(a),true);
            case "cancel_collection" -> r.collection().interrupt(id(a),false);
            case "resume_collection" -> r.collection().resume(id(a));
            default -> throw new IllegalArgumentException("Unknown collection tool");
        };
    }
    private static JsonObject farm(AgentRuntime r,JsonObject a){String op=string(a,"operation","");var f=new JsonObject();if(op.equals("save")){
        if(!coordinates(a,"min_") || !coordinates(a,"max_"))throw new IllegalArgumentException("Farm bounds required");
        String kind=string(a,"kind","");if(!Set.of("manual","automated").contains(kind))throw new IllegalArgumentException("Farm kind required");f.addProperty("kind",kind);
        for(String axis:List.of("x","y","z")){int min=integer(a,"min_"+axis,0),max=integer(a,"max_"+axis,0);if(min>max || (long)max-min>128)throw new IllegalArgumentException("Farm bounds must be ordered and at most 129 blocks per axis");f.addProperty("min_"+axis,min);f.addProperty("max_"+axis,max);}}
        return r.player().inventoryLedger.treeFarm(op,string(a,"name",""),f);
    }
    private static UUID id(JsonObject a){return UUID.fromString(string(a,"request_id",""));}
    private static String string(JsonObject a,String key,String fallback){if(!a.has(key))return fallback;if(!a.get(key).isJsonPrimitive() || !a.getAsJsonPrimitive(key).isString())throw new IllegalArgumentException("Expected string "+key);return a.get(key).getAsString();}
    private static boolean bool(JsonObject a,String key,boolean fallback){if(!a.has(key))return fallback;if(!a.get(key).isJsonPrimitive() || !a.getAsJsonPrimitive(key).isBoolean())throw new IllegalArgumentException("Expected boolean "+key);return a.get(key).getAsBoolean();}
    private static int integer(JsonObject a,String key,int fallback){if(!a.has(key))return fallback;if(!a.get(key).isJsonPrimitive() || !a.getAsJsonPrimitive(key).isNumber())throw new IllegalArgumentException("Expected integer "+key);try{return a.get(key).getAsBigDecimal().intValueExact();}catch(ArithmeticException e){throw new IllegalArgumentException("Expected bounded integer "+key);}}
    private static boolean coordinates(JsonObject a,String prefix){int n=0;for(String axis:List.of("x","y","z"))if(a.has(prefix+axis))n++;if(n!=0 && n!=3)throw new IllegalArgumentException("Provide all three "+prefix+"coordinates");return n==3;}
    private static BlockPos position(JsonObject a,String prefix){if(!coordinates(a,prefix))throw new IllegalArgumentException("Coordinates required");return new BlockPos(integer(a,prefix+"x",0),integer(a,prefix+"y",0),integer(a,prefix+"z",0));}
    private static void coords(JsonObject a,String prefix,String purpose){for(String axis:List.of("x","y","z"))a.add(prefix+axis,field("integer",purpose+": "+axis));}
    private static JsonObject field(String type,String description){var o=new JsonObject();o.addProperty("type",type);o.addProperty("description",description);return o;}
    private static JsonObject enumField(String...values){var o=field("string","Select one of the defined values");var v=new JsonArray();for(String s:values)v.add(s);o.add("enum",v);return o;}
    private static JsonObject tool(String name,String description,JsonObject props,String...required){var o=new JsonObject();o.addProperty("name",name);o.addProperty("description",description);var schema=new JsonObject();schema.addProperty("type","object");schema.add("properties",props);schema.add("required",new Gson().toJsonTree(required));schema.addProperty("additionalProperties",false);o.add("inputSchema",schema);var hints=new JsonObject();hints.addProperty("readOnlyHint",name.equals("inspect_tree") || name.equals("collection_status"));hints.addProperty("destructiveHint",name.equals("choose_collection") || name.equals("resume_collection"));o.add("annotations",hints);return o;}
}
