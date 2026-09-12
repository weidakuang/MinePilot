package dev.mcai.companion.agent.survival;

import com.google.gson.*;
import java.util.*;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.body.MinePilotServerPlayer;
import dev.mcai.companion.vendor.numen.tools.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Pure-server native menu actions exposed identically to both controllers. */
public final class SurvivalTools {
    public static final Set<String> NAMES = Set.of("build_camp","camp_status","cancel_camp","resume_camp","remember_context", "companion_mode", "gather", "gather_status", "cancel_gather", "find_resources", "craft", "interact_block", "inspect_container", "transfer_items", "close_container", "smelt", "eat", "survival_status", "cancel_survival");
    public static final Set<String> READ_ONLY = Set.of("camp_status","inspect_container", "survival_status", "gather_status", "find_resources");
    private SurvivalTools() {}
    public static JsonArray definitions() {
        var tools = new JsonArray();
        var camp=coordinates();camp.add("auto_gather",field("boolean","Gather missing wood/stone with native resource jobs, default true"));tools.add(tool("build_camp","Build a small 5x5 survival camp: real floor, walls, roof, entrance and workstations. Optional x/y/z specify the lower northwest floor cell; otherwise choose a nearby flat clear site. One continuous task acquires missing materials, crafts, places and verifies. Player clients need no UI.",camp));
        tools.add(tool("camp_status","Read camp phase, site, material work and child tasks.",new JsonObject()));tools.add(tool("cancel_camp","Stop camp and its children immediately, preserving blueprint and completed blocks.",new JsonObject()));tools.add(tool("resume_camp","Continue the persisted camp, rechecking actual cells and remaining materials. Does not duplicate completed blocks.",new JsonObject()));
        var memory=new JsonObject();memory.add("summary",field("string","Faithful compact memory of earlier conversation/preferences; max 2400 characters. Do not store world snapshots or invented facts."));memory.add("objective",field("string","Optional current objective, max 512 characters"));memory.add("status",field("string","ACTIVE, COMPLETED, BLOCKED or PAUSED"));memory.add("autonomous",field("boolean","True only for a self-chosen objective"));
        tools.add(tool("remember_context","Persist a compact conversation summary or current goal for the same world/body. Preserve player intent and distinguish historical statements from current facts.",memory));
        var mode=new JsonObject();mode.add("active",field("boolean","Whether autonomous survival/companionship may continue; stop chat pauses it"));tools.add(tool("companion_mode","Enable or pause autonomous companionship. This does not disable swimming self-preservation.",mode,"active"));
        var gather = new JsonObject(); gather.add("resource",field("string","wood or exact block ID; cobblestone requests use minecraft:stone"));gather.add("output_item",field("string","Optional exact output ID; stone defaults to cobblestone"));gather.add("count",field("integer","New inventory items wanted, 1..64, default 4"));gather.add("radius",field("integer","Search radius 1..150, default 150; physical interaction remains vanilla"));
        tools.add(tool("gather","Start one continuous resource job: search exposed loaded resources, approach with existing navigation, mine with the appropriate carried tool, and collect verified drops. Tries other candidates and checked exploration routes internally; may clear up to 24 observed natural bank/headroom blocks to make small access steps, using native reach and breaking. For felling a specific whole tree use plan_collection source tree/whole_tree true instead.",gather,"resource"));
        tools.add(tool("gather_status","Read continuous resource task, candidate failures, actual travel and verified pickup quantity.",new JsonObject()));
        tools.add(tool("cancel_gather","Cancel the resource parent and its current navigation/mining child immediately.",new JsonObject()));
        var find=new JsonObject();find.add("resource",field("string","wood or resource block ID"));find.add("radius",field("integer","1..150, default 150"));find.add("limit",field("integer","1..64 nearest exposed candidates, default 32"));find.add("cursor",field("string","Continue an unfinished search with unchanged arguments"));
        tools.add(tool("find_resources","Budgeted loaded-world resource search with cached, exposed candidates. Does not require the body to turn. Returns visibility separately; unknown space is not absence. No model calls per scan section.",find,"resource"));
        var craft = new JsonObject(); craft.add("item", field("string", "Exact output item ID")); craft.add("count", field("integer", "New items wanted, 1..256; actual recipe batches may round up"));
        tools.add(tool("craft", "Craft through native 2x2 or nearby workbench menus, consuming actual inventory ingredients. Returns material shortage or station coordinates when blocked. No per-slot model calls needed.", craft, "item"));
        var pos = coordinates();
        tools.add(tool("interact_block", "Look at and right-click an actually reachable block. Opens workstations/containers, uses doors and other native blocks. Stores used workstation location. Does not extend player reach.", pos, "x", "y", "z"));
        tools.add(tool("inspect_container", "Inspect the current native menu: exact slot indices, item identities and furnace progress. Player clients need no menu UI.", new JsonObject()));
        var moves = new JsonObject(); var row = new JsonObject(); row.add("from", field("integer", "Source menu slot")); row.add("to", field("integer", "Destination menu slot; omit for native whole-stack quick move")); row.add("count", field("integer", "Exact amount, 1..64; requires destination"));
        var arr = new JsonObject(); arr.addProperty("type", "array"); arr.add("items", schema(row, "from")); arr.addProperty("maxItems", 16); moves.add("moves", arr);
        tools.add(tool("transfer_items", "Move actual stacks through the open native menu. Inspect slots first. Exact amounts require a destination; output slots and capacity are respected. Returns actual before/after menu snapshots.", moves, "moves"));
        tools.add(tool("close_container", "Close the current menu through vanilla cleanup; returns crafting leftovers normally.", new JsonObject()));
        var smelt = coordinates(); smelt.add("item", field("string", "Input item ID, not desired output")); smelt.add("fuel", field("string", "Carried fuel item ID")); smelt.add("count", field("integer", "Input count, 1..64, default 1")); smelt.add("fuel_count", field("integer", "Fuel count, 1..64, default 1"));
        tools.add(tool("smelt", "Approach a furnace/smoker/blast-furnace, load input/fuel, cook and collect output as one native job. No separate open/close menu calls. Cooking uses normal server ticks. Query survival_status for actual collected result. Player may interrupt.", smelt, "x", "y", "z", "item", "fuel"));
        var eat = new JsonObject(); eat.add("item", field("string", "Optional carried food ID; otherwise chooses a safe carried food"));
        tools.add(tool("eat", "Eat one carried food through normal timed item use; completes only after real consumption. Does not create food or fill hunger directly.", eat));
        tools.add(tool("survival_status", "Read eating/smelting status and actual inventory receipts.", new JsonObject()));
        tools.add(tool("cancel_survival", "Stop eating or waiting at the furnace immediately; already loaded furnace contents keep cooking normally.", new JsonObject()));
        return tools;
    }
    public static JsonObject execute(AgentRuntime r, String name, JsonObject a) {
        if (!READ_ONLY.contains(name) && !Set.of("remember_context","companion_mode","cancel_survival","cancel_gather","cancel_camp").contains(name)) requireIdle(r);
        return switch (name) {
            case "build_camp" -> {if((a.has("x") || a.has("y") || a.has("z")) && !(a.has("x") && a.has("y") && a.has("z")))throw new IllegalArgumentException("Camp origin needs all x/y/z coordinates");yield r.camp().start(a.has("x")?position(a):null,!a.has("auto_gather") || a.get("auto_gather").getAsBoolean());}
            case "camp_status" -> r.camp().status();
            case "cancel_camp" -> r.camp().cancel();
            case "resume_camp" -> r.camp().resume();
            case "remember_context" -> r.memory.update(a.has("summary")?string(a,"summary",""):null,a.has("objective")?string(a,"objective",""):null,string(a,"status","ACTIVE"),a.has("autonomous") && a.get("autonomous").getAsBoolean());
            case "companion_mode" -> {r.memory.pause(!a.get("active").getAsBoolean());yield r.companionEvents.snapshot();}
            case "gather" -> r.gather().start(string(a,"resource",""),string(a,"output_item",""),integer(a,"count",4,1,64),integer(a,"radius",150,1,150));
            case "gather_status" -> r.gather().status();
            case "cancel_gather" -> r.gather().cancel();
            case "find_resources" -> r.resources.query(string(a,"resource",""),integer(a,"radius",150,1,150),integer(a,"limit",32,1,64),string(a,"cursor",""));
            case "craft" -> {
                var result = JsonParser.parseString(new CraftOps().craft(string(a,"item",""), integer(a,"count",1,1,256), r.player())).getAsJsonObject();
                if (result.has("crafted") && result.get("crafted").getAsInt() < integer(a,"count",1,1,256)) result.addProperty("status", "PARTIAL");
                yield result;
            }
            case "interact_block" -> { useBlock(r.player(), position(a)); yield inspect(r); }
            case "inspect_container" -> inspect(r);
            case "close_container" -> { r.player().closeContainer(); yield inspect(r); }
            case "transfer_items" -> transfer(r,a);
            case "smelt" -> r.survival().smelt(position(a), string(a,"item",""), string(a,"fuel",""), integer(a,"count",1,1,64), integer(a,"fuel_count",1,1,64));
            case "eat" -> r.survival().eat(string(a,"item",""));
            case "survival_status" -> r.survival().status();
            case "cancel_survival" -> r.survival().cancel();
            default -> throw new IllegalArgumentException("Unknown survival tool");
        };
    }
    public static void requireIdle(AgentRuntime r) {
        var n = r.navigation().status();
        if (r.survival().active() || r.mining().ownsBody() || r.collection().ownsBody() || r.placement().ownsBody() || r.excavation().ownsBody() || r.turnActive() || r.jumpActive() || !n.phase().terminal() && n.phase()!=dev.mcai.companion.agent.navigation.NavigationToolCoordinator.Phase.IDLE)
            throw new IllegalStateException("Finish or pause the current body task before using a workstation");
    }
    public static boolean canUseFrom(MinePilotServerPlayer p, BlockPos pos, Vec3 feet) {
        if(!p.level().isLoaded(pos))return false;var eyes=feet.add(0,p.getEyeHeight(),0);
        var hit=p.level().clip(new ClipContext(eyes,Vec3.atCenterOf(pos),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,p));
        return hit.getType()==HitResult.Type.BLOCK && hit.getBlockPos().equals(pos) && eyes.distanceTo(hit.getLocation())<=p.blockInteractionRange();
    }
    public static void useBlock(MinePilotServerPlayer p, BlockPos pos) {
        if (!p.level().isLoaded(pos)) throw new IllegalArgumentException("Target chunk is not loaded");
        var eyes = p.getEyePosition(); var target = Vec3.atCenterOf(pos);
        if (eyes.distanceTo(target) > p.blockInteractionRange() + .75) throw new IllegalArgumentException("Block is beyond native interaction reach; approach first");
        var hit = p.level().clip(new ClipContext(eyes, target, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p));
        if (hit.getType()!=HitResult.Type.BLOCK || !hit.getBlockPos().equals(pos) || eyes.distanceTo(hit.getLocation()) > p.blockInteractionRange())
            throw new IllegalArgumentException("No unobstructed native interaction face; approach another side");
        var direction = hit.getLocation().subtract(eyes);
        p.setYRot((float)Math.toDegrees(Math.atan2(-direction.x,direction.z))); p.setYHeadRot(p.getYRot());
        p.setXRot((float)-Math.toDegrees(Math.atan2(direction.y,Math.hypot(direction.x,direction.z)))); p.stopControlling();
        var result = dev.mcai.companion.vendor.numen.tools.BlockInteraction.use(p,hit,InteractionHand.MAIN_HAND,InteractionHand.OFF_HAND);
        if (!result.consumesAction()) throw new IllegalStateException("Native block interaction did not consume an action");
        var r = AgentRuntime.active(p.level().getServer()); if(r!=null) r.workstations.placed(pos);
    }
    public static JsonObject inspect(AgentRuntime r) {
        var p=r.player();var menu=p.containerMenu;var out=new JsonObject();out.addProperty("menu",menu.getClass().getSimpleName());out.addProperty("containerId",menu.containerId);
        out.addProperty("open",menu!=p.inventoryMenu);out.addProperty("valid",menu.stillValid(p));var slots=new JsonArray();
        for(var slot:menu.slots){var s=slot.getItem();var row=new JsonObject();row.addProperty("slot",slot.index);row.addProperty("playerSide",slot.container==p.getInventory());row.addProperty("item",s.isEmpty()?"minecraft:air":BuiltInRegistries.ITEM.getKey(s.getItem()).toString());row.addProperty("count",s.getCount());if(!s.isEmpty())row.addProperty("entryId",p.inventoryLedger.key(s));slots.add(row);}
        out.add("slots",slots);out.addProperty("cursorCount",menu.getCarried().getCount());
        if(menu instanceof AbstractFurnaceMenu furnace){out.addProperty("cookingProgress",furnace.getBurnProgress());out.addProperty("lit",furnace.isLit());}
        return out;
    }
    private static JsonObject transfer(AgentRuntime r,JsonObject a){
        var menu=r.player().containerMenu;if(!menu.stillValid(r.player()) || !menu.getCarried().isEmpty())throw new IllegalStateException("Menu invalid or cursor occupied; inspect before transfer");
        var moves=new ArrayList<ContainerOps.Move>();var rows=a.getAsJsonArray("moves");if(rows==null || rows.isEmpty() || rows.size()>16)throw new IllegalArgumentException("Require 1..16 moves");
        for(var value:rows){var m=value.getAsJsonObject();if(m.has("count") && !m.has("to"))throw new IllegalArgumentException("Exact amount requires destination slot");moves.add(new ContainerOps.Move(integer(m,"from",-1,0,menu.slots.size()-1),m.has("to")?integer(m,"to",0,0,menu.slots.size()-1):null,m.has("count")?integer(m,"count",1,1,64):null));}
        var before=inspect(r);var result=JsonParser.parseString(new ContainerOps().transfer(moves,r.player())).getAsJsonObject();menu.broadcastChanges();var after=inspect(r);result.add("before",before);result.add("after",after);
        if(before.equals(after)){result.addProperty("status","BLOCKED");result.addProperty("success",false);}return result;
    }
    static BlockPos position(JsonObject a){return new BlockPos(integer(a,"x",0,Integer.MIN_VALUE,Integer.MAX_VALUE),integer(a,"y",0,-2048,2048),integer(a,"z",0,Integer.MIN_VALUE,Integer.MAX_VALUE));}
    static String string(JsonObject a,String k,String fallback){return a.has(k)?a.get(k).getAsString():fallback;}
    static int integer(JsonObject a,String k,int fallback,int min,int max){int n=a.has(k)?a.get(k).getAsBigDecimal().intValueExact():fallback;if(n<min || n>max)throw new IllegalArgumentException("Invalid "+k);return n;}
    private static JsonObject coordinates(){var p=new JsonObject();for(String k:List.of("x","y","z"))p.add(k,field("integer","Exact block "+k));return p;}
    public static JsonObject field(String type,String description){var p=new JsonObject();p.addProperty("type",type);p.addProperty("description",description);return p;}
    public static JsonObject schema(JsonObject p,String... required){var s=new JsonObject();s.addProperty("type","object");s.add("properties",p);var keys=new JsonArray();for(String k:required)keys.add(k);s.add("required",keys);s.addProperty("additionalProperties",false);return s;}
    public static JsonObject tool(String name,String description,JsonObject p,String... required){var t=new JsonObject();t.addProperty("name",name);t.addProperty("description",description);t.add("inputSchema",schema(p,required));return t;}
}
