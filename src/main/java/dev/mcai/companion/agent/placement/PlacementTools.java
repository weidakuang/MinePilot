package dev.mcai.companion.agent.placement;

import java.util.*;
import com.google.gson.*;
import dev.mcai.companion.agent.AgentRuntime;
import net.minecraft.core.BlockPos;

/** Shared bounded construction interface. Coordinate expansion is pure; execution remains physical. */
public final class PlacementTools {
    public static final Set<String> NAMES=Set.of("set_hand","inventory_capacity","inspect_placement","place_block","plan_placement","choose_placement","placement_status","pause_placement","resume_placement","cancel_placement","resolve_placement");
    public static final Set<String> READ_ONLY=Set.of("inventory_capacity","inspect_placement","placement_status");
    private PlacementTools() {}
    public static JsonArray definitions(){
        var out=new JsonArray();var identity=identity();
        var hands=identity.deepCopy();hands.add("hand",enums("main","offhand"));
        out.add(tool("set_hand","Move a real storage/offhand item into the selected hand using native menu swaps. Use item=minecraft:air for empty hand; needs an empty slot. Allowed idle or placement paused; never deletes or creates items.",hands));
        out.add(tool("inventory_capacity","Count free storage slots and compatible capacity for an item or exact slot/components. Offhand and armor excluded; different-item capacities share free slots.",identity));
        var cell=cell();out.add(tool("place_block","Start one real item use within five blocks, auto-turning to a reachable face. state constrains native orientation (e.g. axis=x, facing=north, half=top). jump=true permits a physical underfoot jump. Use status/inspection to verify completion; no air placement or wall clicks through obstacles.",cell,"x","y","z"));
        var inspect=new JsonObject();inspect.add("targets",array(object(coords()),"1..64 exact currently sensed block positions, including air, for physical verification"));
        out.add(tool("inspect_placement","Read exact current block states at bounded sensed coordinates. Does not expose unsensed terrain or treat intended states as actual blocks.",inspect,"targets"));
        var plan=new JsonObject();plan.add("targets",array(object(cell),"Ordered item-use cells, 1..256. Doors use lower cell once; beds use foot once. Existing matching blocks are preserved."));
        var region=cell.deepCopy();region.remove("x");region.remove("y");region.remove("z");region.add("from",object(coords()));region.add("to",object(coords()));
        plan.add("region",object(region));
        var blueprint=new JsonObject();blueprint.add("origin",object(coords()));
        var palette=field("object","Single-character symbols mapped to placement item/state/hand/temporary properties. Period means preserve; underscore means require air.");palette.add("additionalProperties",object(cell));blueprint.add("palette",palette);
        blueprint.add("layers",array(array(field("string","Rows increase z; characters increase x"),"One y layer, bottom to top"),"Rectangular fixed-origin layers, at most 4096 total cells and 256 item-use targets"));
        plan.add("blueprint",object(blueprint));
        plan.add("allow_movement",field("boolean","Default false. True allows evaluated safe local approaches inside 24 blocks of the fixed origin; no automatic excavation or bridging."));
        plan.add("movement_budget",field("integer","Total physical movement budget 0..256, default 64 if movement is allowed"));
        plan.add("cleanup_temporary",field("boolean","Default true. Normally mine temporary=true cells after use only if their removal is evaluated safe. Importance 0..2 items cannot be temporary. Unreachable/dependent cleanup blocks for a model decision."));
        out.add(tool("plan_placement","Preview a bounded batch: supply ordered targets, an inclusive from/to region, or a fixed-origin text-layer blueprint with one item/state. Reports exact material identities/counts and fixed bounds. No mutation until choose. Blocked cells require a fresh obstacle decision; chat remains available throughout.",plan));
        var id=new JsonObject();id.add("request_id",field("string","Exact current placement UUID"));var choose=id.deepCopy();choose.add("option_id",field("string","Exact returned optionId"));
        out.add(tool("choose_placement","Approve the returned material/bounds policy and execute continuously with real per-cell inventory, reach, orientation and state checks.",choose,"request_id","option_id"));
        out.add(tool("placement_status","Read phase, actual position, material debit receipts and obstruction decisions. COMPLETED verifies required cells; PARTIAL or BLOCKED is not completion.",new JsonObject()));
        for(String name:List.of("pause_placement","resume_placement","cancel_placement"))out.add(tool(name,"Pause/resume/cancel the exact job including its child movement or break. No delayed placement after cancel; already changed cells remain.",id.deepCopy(),"request_id"));
        var resolve=choose.deepCopy();resolve.add("decision_id",field("string","Exact current obstruction decisionId; stale choices are rejected"));
        out.add(tool("resolve_placement","Choose a currently returned obstacle option. Excavation is offered only with native held-tool cost/durability evaluation; no guessed alternatives. Retry rechecks; skip yields PARTIAL.",resolve,"request_id","decision_id","option_id"));
        return out;
    }
    public static JsonObject execute(AgentRuntime r,String name,JsonObject a){
        return switch(name){
            case "set_hand" -> HandController.equip(r,a);
            case "inventory_capacity" -> HandController.capacity(r.player(),a);
            case "place_block" -> r.placement().one(a);
            case "plan_placement" -> plan(r,a);
            case "choose_placement" -> r.placement().choose(id(a),string(a,"option_id",""));
            case "placement_status" -> r.placement().status();
            case "pause_placement" -> r.placement().interrupt(id(a),true);
            case "cancel_placement" -> r.placement().interrupt(id(a),false);
            case "resume_placement" -> r.placement().resume(id(a));
            case "resolve_placement" -> r.placement().resolve(id(a),string(a,"decision_id",""),string(a,"option_id",""));
            case "inspect_placement" -> inspect(r,a);
            default -> throw new IllegalArgumentException("Unknown placement tool");
        };
    }
    private static JsonObject plan(AgentRuntime r,JsonObject a){var layout=layout(a);return r.placement().plan(layout.targets(),bool(a,"allow_movement",false),integer(a,"movement_budget",bool(a,"allow_movement",false)?64:0),bool(a,"cleanup_temporary",true),layout.air());}
    public record Layout(List<JsonObject> targets,List<BlockPos> air) {}
    public static Layout layout(JsonObject a){
        int formats=(a.has("targets")?1:0)+(a.has("region")?1:0)+(a.has("blueprint")?1:0);
        if(formats!=1)throw new IllegalArgumentException("Choose one targets, region or blueprint format");
        if(!a.has("blueprint"))return new Layout(expand(a),List.of());
        var b=a.getAsJsonObject("blueprint");var origin=position(b.getAsJsonObject("origin"),"");var palette=b.getAsJsonObject("palette");var layers=b.getAsJsonArray("layers");
        if(layers.isEmpty() || layers.size()>32 || palette.size()>64)throw new IllegalArgumentException("Bounded blueprint layers/palette required");
        var targets=new ArrayList<JsonObject>();var air=new ArrayList<BlockPos>();int width=-1,depth=-1,cells=0;
        for(int y=0;y<layers.size();y++){
            var rows=layers.get(y).getAsJsonArray();if(depth<0)depth=rows.size();if(depth==0 || rows.size()!=depth)throw new IllegalArgumentException("Layers must have equal nonempty depth");
            for(int z=0;z<rows.size();z++){
                if(!rows.get(z).isJsonPrimitive() || !rows.get(z).getAsJsonPrimitive().isString())throw new IllegalArgumentException("Blueprint row must be a string");
                String row=rows.get(z).getAsString();if(width<0)width=row.length();if(width==0 || row.length()!=width || (cells+=width)>4096)throw new IllegalArgumentException("Rectangular blueprint must contain at most 4096 cells");
                for(int x=0;x<width;x++){
                    char symbol=row.charAt(x);var pos=new BlockPos(Math.addExact(origin.getX(),x),Math.addExact(origin.getY(),y),Math.addExact(origin.getZ(),z));
                    if(symbol=='.')continue;if(symbol=='_'){air.add(pos);continue;}
                    if(!palette.has(String.valueOf(symbol)))throw new IllegalArgumentException("Unmapped blueprint symbol: "+symbol);
                    var cell=palette.getAsJsonObject(String.valueOf(symbol)).deepCopy();if(cell.has("x") || cell.has("y") || cell.has("z"))throw new IllegalArgumentException("Palette coordinates are derived from the fixed origin");
                    xyz(pos).entrySet().forEach(e->cell.add(e.getKey(),e.getValue()));targets.add(cell);
                    if(targets.size()>256)throw new IllegalArgumentException("Blueprint exceeds 256 placement actions");
                }
            }
        }return new Layout(List.copyOf(targets),List.copyOf(air));
    }
    public static List<JsonObject> expand(JsonObject a){
        if(a.has("targets")==a.has("region"))throw new IllegalArgumentException("Provide exactly one targets array or region");
        var out=new ArrayList<JsonObject>();
        if(a.has("targets")){
            var rows=a.getAsJsonArray("targets");if(rows.isEmpty() || rows.size()>256)throw new IllegalArgumentException("Use 1..256 placement targets");
            for(var row:rows){position(row.getAsJsonObject(),"");out.add(row.getAsJsonObject().deepCopy());}
        }else {
            var region=a.getAsJsonObject("region");var from=position(region.getAsJsonObject("from"),"");var to=position(region.getAsJsonObject("to"),"");
            long nx=(long)to.getX()-from.getX()+1,ny=(long)to.getY()-from.getY()+1,nz=(long)to.getZ()-from.getZ()+1;
            if(nx<1 || ny<1 || nz<1 || nx>256 || ny>256 || nz>256 || nx*ny*nz>256)throw new IllegalArgumentException("Region must be ordered and contain at most 256 cells");
            for(long y=from.getY();y<=to.getY();y++)for(long z=from.getZ();z<=to.getZ();z++)for(long x=from.getX();x<=to.getX();x++){
                var row=region.deepCopy();row.remove("from");row.remove("to");row.addProperty("x",x);row.addProperty("y",y);row.addProperty("z",z);out.add(row);
            }
        }return out;
    }
    private static JsonObject inspect(AgentRuntime r,JsonObject a){
        var targets=a.getAsJsonArray("targets");if(targets.isEmpty() || targets.size()>64)throw new IllegalArgumentException("Inspect 1..64 sensed coordinates");
        var rows=new JsonArray();for(var target:targets){var p=position(target.getAsJsonObject(),"");if(!r.perception.observableBlock(p))throw new IllegalArgumentException("Target is not sensed");var row=xyz(p);row.addProperty("state",r.player().level().getBlockState(p).toString());rows.add(row);}
        var out=new JsonObject();out.addProperty("dimension",r.player().level().dimension().identifier().toString());out.addProperty("tick",r.server().getTickCount());out.add("blocks",rows);return out;
    }
    public static JsonObject xyz(BlockPos p){var o=new JsonObject();o.addProperty("x",p.getX());o.addProperty("y",p.getY());o.addProperty("z",p.getZ());return o;}
    private static UUID id(JsonObject a){return UUID.fromString(string(a,"request_id",""));}
    public static String string(JsonObject a,String key,String fallback){if(!a.has(key))return fallback;if(!a.get(key).isJsonPrimitive() || !a.getAsJsonPrimitive(key).isString())throw new IllegalArgumentException("Expected string "+key);return a.get(key).getAsString();}
    public static boolean bool(JsonObject a,String key,boolean fallback){if(!a.has(key))return fallback;if(!a.get(key).isJsonPrimitive() || !a.getAsJsonPrimitive(key).isBoolean())throw new IllegalArgumentException("Expected boolean "+key);return a.get(key).getAsBoolean();}
    public static int integer(JsonObject a,String key,int fallback){if(!a.has(key))return fallback;if(!a.get(key).isJsonPrimitive() || !a.getAsJsonPrimitive(key).isNumber())throw new IllegalArgumentException("Expected integer "+key);try{return a.get(key).getAsBigDecimal().intValueExact();}catch(ArithmeticException e){throw new IllegalArgumentException("Expected bounded integer "+key);}}
    public static BlockPos position(JsonObject a,String prefix){for(String axis:List.of("x","y","z"))if(!a.has(prefix+axis))throw new IllegalArgumentException("Three integer coordinates required");return new BlockPos(integer(a,prefix+"x",0),integer(a,prefix+"y",0),integer(a,prefix+"z",0));}
    private static JsonObject identity(){var o=new JsonObject();o.add("item",field("string","Exact registry ID; existing components must be unambiguous"));o.add("entry_id",field("string","Exact inventory identity, preserves components"));o.add("slot",field("integer","Existing storage slot 0..35 or offhand 40"));return o;}
    private static JsonObject coords(){var o=new JsonObject();for(String axis:List.of("x","y","z"))o.add(axis,field("integer","Absolute block coordinate "+axis));return o;}
    private static JsonObject cell(){var o=identity();coords().entrySet().forEach(e->o.add(e.getKey(),e.getValue()));o.add("hand",enums("main","offhand"));o.add("jump",field("boolean","Permit normal underfoot jumping; default false"));o.add("temporary",field("boolean","Declared temporary support, importance must be 3..5"));var state=field("object","Desired native block-state subset; strings such as axis=x, facing=north, half=top, type=top. Impossible states are blocked, never world-edited.");state.add("additionalProperties",field("string","Native property value"));o.add("state",state);return o;}
    private static JsonObject object(JsonObject props){var o=field("object","");o.add("properties",props);o.addProperty("additionalProperties",false);return o;}
    private static JsonObject array(JsonObject item,String description){var o=field("array",description);o.add("items",item);return o;}
    private static JsonObject enums(String... values){var o=field("string","Select a defined value");o.add("enum",new Gson().toJsonTree(values));return o;}
    private static JsonObject field(String type,String description){var o=new JsonObject();o.addProperty("type",type);o.addProperty("description",description);return o;}
    private static JsonObject tool(String name,String description,JsonObject properties,String...required){var o=new JsonObject();o.addProperty("name",name);o.addProperty("description",description);var schema=object(properties);schema.add("required",new Gson().toJsonTree(required));o.add("inputSchema",schema);var hints=new JsonObject();hints.addProperty("readOnlyHint",READ_ONLY.contains(name));hints.addProperty("destructiveHint",Set.of("place_block","choose_placement","resume_placement","resolve_placement").contains(name));o.add("annotations",hints);return o;}
}
