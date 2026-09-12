package dev.mcai.companion.agent.mining;

import com.google.gson.*;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.placement.PlacementTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import java.util.*;
import static dev.mcai.companion.agent.placement.PlacementTools.*;

/** Shared internal/MCP interface. All relative references bind once at planning time. */
public final class ExcavationTools {
    public static final Set<String> NAMES=Set.of("survey_mining","mining_survey_status","plan_excavation","choose_excavation","excavation_status","pause_excavation","resume_excavation","cancel_excavation");
    public static JsonArray definitions(){
        var out=new JsonArray();var survey=new JsonObject();survey.add("radius",field("integer","Local sensed sphere 2..10, default 10"));survey.add("resource",field("string","Optional native resource name, e.g. coal or diamond; used to filter observed opportunities and current biome generation hints"));
        out.add(tool("survey_mining","Asynchronously assess local caves, exposed resources, hazards, exploration frontiers, return connectivity and optional mining strategies. Reads no ore beyond perception and does not move/mine. Poll mining_survey_status or await mining_survey_event; moving during capture makes it STALE.",survey));
        out.add(tool("mining_survey_status","Read cave-survey progress and bounded strategy choices. Generation hints are current biome datapack rules, never ore coordinates or guaranteed yields.",new JsonObject()));
        var p=new JsonObject();
        p.add("mode",enums("access","resource_radius","region","tunnel","fishbone","tree"));
        p.add("allow_managed_grove",field("boolean","Tree mode only: explicit permission to harvest evaluated manual groves; automated farms remain protected."));
        p.add("replant",field("boolean","Tree mode only: reserve and normally place the species-correct sapling/root layout after whole-tree collection. Default true for managed groves, false otherwise; missing materials block approval."));
        p.add("relative",field("boolean","Coordinates are offsets from the current integer body block position, captured once. Default false."));
        p.add("target",coordinates());p.add("from",coordinates());p.add("to",coordinates());
        p.add("radius",field("integer","Fixed resource sphere radius 1..10, default 5. Center is target or current body position."));
        p.add("block",field("string","Exact block ID, required for resource_radius. Region optionally filters this exact block; air is omitted."));
        p.add("direction",enums("north","south","east","west"));p.add("length",field("integer","Tunnel trunk length 1..12, default 6"));
        p.add("slope",field("integer","Tunnel height change per block: -1, 0 or 1. Default 0; no vertical shaft beneath the body."));
        p.add("branch_length",field("integer","Optional fishbone side branches 1..8, default 3"));p.add("branch_spacing",field("integer","Fishbone branch spacing 2..6, default 3"));
        p.add("allow_access",field("boolean","Explicitly allow listed natural-terrain access breaks outside resource targets. Default false. Buildings, machines and declared farms excluded."));
        p.add("allow_supports",field("boolean","Allow evaluated ordinary support placement using listed inventory materials of importance 3..5 only; default false. Supports remain for safe return."));
        p.add("require_harvest",field("boolean","Default true for resource_radius; false for clearance. Wrong-tool clearance may lose block drops; costs disclose tools including bare hands."));
        p.add("collect_drops",field("boolean","Default true except access mode. Physically collect this task’s emitted drops within the travel budget; full inventory, lost drops or inaccessible pickup yields PARTIAL with reason."));
        p.add("max_breaks",field("integer","Approved target plus access break cap 1..4096; default 512"));p.add("movement_budget",field("integer","Total travel cap 1..512, default 128"));
        out.add(tool("plan_excavation","Capture a local observed excavation segment asynchronously, then compare real access/movement/tool/support alternatives. Modes: approach a target without breaking it, fixed-radius matching resources, inclusive region, tunnel, or OPTIONAL fishbone. Poll excavation_status until PLAN_READY. No blocks change before model choice. Partial coverage is explicit; no automatic torches or blind mineral discovery.",p));
        var id=new JsonObject();id.add("request_id",field("string","Exact current excavation request UUID"));var choose=id.deepCopy();choose.add("option_id",field("string","Exact returned optionId"));choose.add("confirm_destructive",field("boolean","Model confirmation of large listed excavation, required when destructiveConfirmationRequired; this is not a mandatory player prompt."));
        out.add(tool("choose_excavation","Approve a concrete evaluated scope and resources. Executes break/walk/support steps while chat stays available. World/resource changes block for a fresh decision; no teleport or direct world edit.",choose,"request_id","option_id"));
        out.add(tool("excavation_status","Read planning, actual body position, current action, progress and physical break receipts. Empty cells prove excavation only; inventory events separately prove collection.",new JsonObject()));
        for(String name:List.of("pause_excavation","resume_excavation","cancel_excavation"))out.add(tool(name,"Interrupt/resume the exact excavation and its child movement/mining/placement. Changed blocks remain; canceled work never resumes itself.",id.deepCopy(),"request_id"));
        return out;
    }
    public static JsonObject execute(AgentRuntime r,String name,JsonObject a){
        return switch(name){
            case "survey_mining" -> r.miningSurvey.start(integer(a,"radius",10),string(a,"resource",""));
            case "mining_survey_status" -> r.miningSurvey.status();
            case "plan_excavation" -> plan(r,a);
            case "excavation_status" -> r.excavation().status();
            case "choose_excavation" -> r.excavation().choose(id(a),string(a,"option_id",""),bool(a,"confirm_destructive",false));
            case "pause_excavation" -> r.excavation().interrupt(id(a),true);
            case "cancel_excavation" -> r.excavation().interrupt(id(a),false);
            case "resume_excavation" -> r.excavation().resume(id(a));
            default -> throw new IllegalArgumentException("Unknown excavation tool");
        };
    }
    private static JsonObject plan(AgentRuntime r,JsonObject a){
        String mode=string(a,"mode","resource_radius");BlockPos base=r.player().blockPosition();boolean relative=bool(a,"relative",false);
        BlockPos center=a.has("target")?resolve(a.getAsJsonObject("target"),base,relative):base;
        String filter=string(a,"block","");
        if(!filter.isEmpty() && (!BuiltInRegistries.BLOCK.containsKey(Identifier.parse(filter)) || filter.equals("minecraft:air")))throw new IllegalArgumentException("Unknown or air block filter");
        var requested=new LinkedHashSet<BlockPos>();
        if(mode.equals("tree")){
            if(!a.has("target"))throw new IllegalArgumentException("Tree mode needs a sensed trunk target");var survey=TreeSurvey.inspect(r,center);
            return r.excavation().planTree(survey,bool(a,"allow_managed_grove",false),bool(a,"allow_supports",false),bool(a,"allow_access",false),bool(a,"replant",survey.classification().equals("managed_grove_candidate")),integer(a,"max_breaks",512),integer(a,"movement_budget",128));
        }
        switch(mode){
            case "access" -> {if(!a.has("target"))throw new IllegalArgumentException("Access needs target coordinates");requested.add(center);}
            case "resource_radius" -> {
                if(filter.isBlank())throw new IllegalArgumentException("Select an exact resource block ID");int radius=integer(a,"radius",5);if(radius<1 || radius>10)throw new IllegalArgumentException("Radius must be 1..10");
                var actualCenter=a.has("target")?net.minecraft.world.phys.Vec3.atCenterOf(center):r.player().position();
                for(int x=-radius;x<=radius;x++)for(int y=-radius;y<=radius;y++)for(int z=-radius;z<=radius;z++){
                    var p=center.offset(x,y,z);if(p.distToCenterSqr(actualCenter)>radius*radius || !r.perception.observableBlock(p))continue;
                    if(TreeSurvey.id(r.player().level().getBlockState(p)).equals(filter))requested.add(p);
                }
            }
            case "region" -> {
                if(!a.has("from") || !a.has("to"))throw new IllegalArgumentException("Region needs from/to coordinates");
                var from=resolve(a.getAsJsonObject("from"),base,relative);var to=resolve(a.getAsJsonObject("to"),base,relative);
                if(from.distSqr(base)>14*14*3 || to.distSqr(base)>14*14*3)throw new IllegalArgumentException("Region must be a local bounded segment");
                long volume=(Math.abs((long)from.getX()-to.getX())+1)*(Math.abs((long)from.getY()-to.getY())+1)*(Math.abs((long)from.getZ()-to.getZ())+1);
                if(volume>4096 || volume<1)throw new IllegalArgumentException("Region cap is 4096 cells");
                for(int y=Math.max(from.getY(),to.getY());y>=Math.min(from.getY(),to.getY());y--)for(int x=Math.min(from.getX(),to.getX());x<=Math.max(from.getX(),to.getX());x++)for(int z=Math.min(from.getZ(),to.getZ());z<=Math.max(from.getZ(),to.getZ());z++){
                    var p=new BlockPos(x,y,z);if(filter.isBlank() || !r.perception.observableBlock(p) || TreeSurvey.id(r.player().level().getBlockState(p)).equals(filter))requested.add(p);
                }
            }
            case "tunnel","fishbone" -> {
                int length=integer(a,"length",6),slope=integer(a,"slope",0),branch=integer(a,"branch_length",3),spacing=integer(a,"branch_spacing",3);
                if(length<1 || length>12 || Math.abs(slope)>1 || branch<1 || branch>8 || spacing<2 || spacing>6)throw new IllegalArgumentException("Invalid bounded tunnel geometry");
                String d=string(a,"direction","north");int dx=switch(d){case "east"->1;case "west"->-1;case "north","south"->0;default->throw new IllegalArgumentException("Cardinal direction required");};int dz=d.equals("north")?-1:d.equals("south")?1:0;
                for(int n=1;n<=length;n++){
                    var p=center.offset(dx*n,slope*n,dz*n);requested.add(p.above());requested.add(p);if(slope>0)requested.add(p.offset(-dx,1-slope,-dz));
                    if(mode.equals("fishbone") && n%spacing==0)for(int side:new int[]{-1,1})for(int k=1;k<=branch;k++){var q=p.offset(-dz*k*side,0,dx*k*side);requested.add(q.above());requested.add(q);}
                }
            }
            default -> throw new IllegalArgumentException("Unknown excavation mode");
        }
        if(requested.isEmpty())throw new IllegalArgumentException("No matching sensed target blocks; no excavation was started");
        return r.excavation().plan(mode,List.copyOf(requested),bool(a,"require_harvest",mode.equals("resource_radius")),bool(a,"allow_access",false),bool(a,"allow_supports",false),integer(a,"max_breaks",512),integer(a,"movement_budget",128),bool(a,"collect_drops",!mode.equals("access")));
    }
    private static BlockPos resolve(JsonObject a,BlockPos base,boolean relative){var p=PlacementTools.position(a,"");return relative?new BlockPos(Math.addExact(base.getX(),p.getX()),Math.addExact(base.getY(),p.getY()),Math.addExact(base.getZ(),p.getZ())):p;}
    private static UUID id(JsonObject a){return UUID.fromString(string(a,"request_id",""));}
    private static JsonObject coordinates(){var p=new JsonObject();for(String axis:List.of("x","y","z"))p.add(axis,field("integer","Block coordinate "+axis));var s=object(p);s.add("required",new Gson().toJsonTree(List.of("x","y","z")));return s;}
    private static JsonObject field(String t,String text){var o=new JsonObject();o.addProperty("type",t);o.addProperty("description",text);return o;}
    private static JsonObject enums(String...v){var o=field("string","Choose a defined value");o.add("enum",new Gson().toJsonTree(v));return o;}
    private static JsonObject object(JsonObject p){var o=field("object","");o.add("properties",p);o.addProperty("additionalProperties",false);return o;}
    private static JsonObject tool(String n,String d,JsonObject p,String...req){var o=new JsonObject();o.addProperty("name",n);o.addProperty("description",d);var s=object(p);s.add("required",new Gson().toJsonTree(req));o.add("inputSchema",s);return o;}
}
