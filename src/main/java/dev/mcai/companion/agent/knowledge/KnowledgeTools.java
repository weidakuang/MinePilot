package dev.mcai.companion.agent.knowledge;

import java.util.OptionalDouble;
import com.google.gson.*;
import dev.mcai.companion.agent.AgentRuntime;

/** One schema and execution path for perception and item policy, regardless of controller transport. */
public final class KnowledgeTools {
    public static final java.util.Set<String> NAMES=java.util.Set.of("listen","turn","inventory","inventory_events","annotate_item","waypoint","sense");
    private final AgentRuntime runtime;
    public KnowledgeTools(AgentRuntime runtime){this.runtime=runtime;}
    public static JsonArray definitions(){
        JsonArray tools=new JsonArray();
        JsonObject sounds=new JsonObject();sounds.add("after_sequence",integer("Optional exclusive cursor; omit to see all currently active captions."));sounds.add("limit",integer("Maximum 1..64 captions, default 32."));
        tools.add(tool("listen","Read recent native Chinese sound captions, eight relative directions, distances and attributed sources. Vanilla sound-resource ranges and 3-second lifetime; no visual occlusion. Positional source candidates are explicitly unconfirmed.",schema(sounds),true,false));
        JsonObject turn=new JsonObject();turn.add("heading",number("Absolute compass heading [0,360], north=0, east=90. Omit for a relative reference."));
        turn.add("reference",string("Observed entity UUID/name, or sun; omit with absolute heading."));turn.add("side",enumString("facing","back","left","right"));
        tools.add(tool("turn","Turn in place using normal input; relative side specifies which side of the Agent faces the reference.",schema(turn),false,false));
        tools.add(tool("inventory","Read item identities, counts, slots, notes and protective importance.",schema(new JsonObject()),true,false));
        JsonObject invEvents=new JsonObject();invEvents.add("after_sequence",integer("Exclusive event cursor."));invEvents.add("limit",integer("1..32 batches."));
        tools.add(tool("inventory_events","Read actual acquisition events and current inventory. Unknown source means unproven, not system-given.",schema(invEvents),true,false));
        JsonObject annotation=new JsonObject();annotation.add("entry_id",string("Current inventory entryId including item components."));annotation.add("importance",integer("0 most important through 5 least important. Levels 0..2 cannot be used as navigation support."));annotation.add("note",string("Up to 256 characters."));
        tools.add(tool("annotate_item","Persist item importance and note in this world. Identical split stacks share the policy.",schema(annotation,"entry_id","importance","note"),false,false));
        JsonObject sense=new JsonObject();sense.add("kind",enumString("entities","items","blocks","trees","structures"));sense.add("radius",integer("1..96 blocks, loaded space only."));sense.add("filter",string("Type/name/id substring; empty means all."));sense.add("offset",integer("Entity result offset."));sense.add("cursor",string("Block search continuation cursor."));sense.add("limit",integer("1..64 results."));
        tools.add(tool("sense","Query bounded perception. Local sensors bypass occlusion; farther vision follows the custom transparent-block rules. Structure markers are candidates, never hidden locate results.",schema(sense,"kind"),true,false));
        JsonObject point=new JsonObject();point.add("operation",enumString("save","list","remove"));point.add("name",string("Waypoint name, up to 64 characters."));point.add("note",string("Up to 256 characters."));point.add("dimension",string("Defaults to current dimension."));point.add("x",number("Defaults to body X."));point.add("y",number("Defaults to body Y."));point.add("z",number("Defaults to body Z."));
        point.add("offset",integer("Waypoint list offset."));point.add("limit",integer("Waypoint list limit 1..32."));
        tools.add(tool("waypoint","Save, list or remove dimension-scoped coordinate memories; these are not current terrain observations.",schema(point,"operation"),false,false));
        return tools;
    }
    /** Callers must hold the current controller ownership before mutations. */
    public JsonObject execute(String name,JsonObject args){
        if(!runtime.server().isSameThread())throw new IllegalStateException("Gameplay tools require the server thread");
        return switch(name){
            case "listen"->runtime.hearing.query(optionalLong(args,"after_sequence",0),Math.toIntExact(optionalLong(args,"limit",32)));
            case "turn"->turn(args);
            case "inventory"->runtime.player().inventoryLedger.inventory();
            case "inventory_events"->runtime.player().inventoryLedger.events(optionalLong(args,"after_sequence",0),Math.toIntExact(optionalLong(args,"limit",16)));
            case "annotate_item"->annotateItem(args);
            case "waypoint"->waypoint(args);
            case "sense"->sense(args);
            default->throw new IllegalArgumentException("Unknown knowledge tool");
        };
    }
    private JsonObject turn(JsonObject a) {
        double heading;
        if(a.has("heading")) {
            if(a.has("reference") || a.has("side"))throw new IllegalArgumentException("Use absolute heading or relative reference, not both");
            heading=requiredDouble(a,"heading");if(heading<0 || heading>360)throw new IllegalArgumentException("Heading must be 0..360");
        } else {
            String reference=requiredString(a,"reference");
            if(reference.equals("sun"))heading=runtime.perception.sunBearing();
            else {var entity=runtime.perception.target(reference);if(entity.position().subtract(runtime.player().position()).horizontalDistanceSqr()<.01)throw new IllegalArgumentException("Reference has no horizontal bearing");heading=dev.mcai.companion.agent.knowledge.WorldPerception.bearing(runtime.player().position(),entity.position());}
            heading=dev.mcai.companion.agent.knowledge.WorldPerception.relativeHeading(heading,optionalString(a,"side","facing"));
        }
        runtime.turnTo(heading);var result=new JsonObject();result.addProperty("turnPhase",runtime.turnPhase());result.addProperty("requestedHeading",heading);return result;
    }
    private JsonObject annotateItem(JsonObject a){return runtime.player().inventoryLedger.annotate(requiredString(a,"entry_id"),Math.toIntExact(optionalLong(a,"importance",2)),optionalString(a,"note",""));}
    private JsonObject waypoint(JsonObject a){
        String op=requiredString(a,"operation");
        if(op.equals("list"))return runtime.player().inventoryLedger.waypoints(Math.toIntExact(optionalLong(a,"offset",0)),Math.toIntExact(optionalLong(a,"limit",32)));
        var p=runtime.player();return p.inventoryLedger.waypoint(op,optionalString(a,"name",""),optionalString(a,"dimension",p.level().dimension().identifier().toString()),optionalDouble(a,"x").orElse(p.getX()),optionalDouble(a,"y").orElse(p.getY()),optionalDouble(a,"z").orElse(p.getZ()),optionalString(a,"note",""));
    }
    private JsonObject sense(JsonObject a){
        String kind=requiredString(a,"kind"),filter=optionalString(a,"filter","");int limit=Math.toIntExact(optionalLong(a,"limit",32));
        int radius=Math.toIntExact(optionalLong(a,"radius",kind.equals("entities")?96:10));
        if(kind.equals("entities") || kind.equals("items"))return runtime.perception.entities(Math.toIntExact(optionalLong(a,"offset",0)),limit,filter,radius,kind.equals("items"));
        if(!java.util.Set.of("blocks","trees","structures").contains(kind))throw new IllegalArgumentException("Unknown sense category");
        return runtime.perception.blocks(radius,filter,kind,optionalString(a,"cursor",""),limit);
    }

    private static JsonObject tool(
            String name,
            String description,
            JsonObject inputSchema,
            boolean readOnly,
            boolean destructive
    ) {
        JsonObject result = new JsonObject();
        result.addProperty("name", name);
        result.addProperty("title", name.replace('_', ' '));
        result.addProperty("description", description);
        result.add("inputSchema", inputSchema);
        JsonObject annotations = new JsonObject();
        annotations.addProperty("readOnlyHint", readOnly);
        annotations.addProperty("destructiveHint", destructive);
        annotations.addProperty("idempotentHint", readOnly);
        annotations.addProperty("openWorldHint", false);
        result.add("annotations", annotations);
        return result;
    }
    private static JsonObject schema(JsonObject properties, String... requiredNames) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", properties);
        JsonArray required = new JsonArray();
        for (String name : requiredNames) {
            required.add(name);
        }
        schema.add("required", required);
        schema.addProperty("additionalProperties", false);
        return schema;
    }
    private static JsonObject string(String description) {
        JsonObject result = new JsonObject();
        result.addProperty("type", "string");
        result.addProperty("description", description);
        return result;
    }
    private static JsonObject integer(String description) {
        JsonObject result = new JsonObject();
        result.addProperty("type", "integer");
        result.addProperty("description", description);
        return result;
    }
    private static JsonObject number(String description) {
        JsonObject result = new JsonObject();
        result.addProperty("type", "number");
        result.addProperty("description", description);
        return result;
    }
    private static JsonObject enumString(String... values) {
        JsonObject result = new JsonObject();
        result.addProperty("type", "string");
        JsonArray choices = new JsonArray();
        for (String value : values) {
            choices.add(value);
        }
        result.add("enum", choices);
        return result;
    }
    private static String requiredString(JsonObject object, String name) {
        String value = optionalString(object, name, null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing string argument " + name);
        }
        return value;
    }
    private static String optionalString(JsonObject object, String name, String fallback) {
        JsonElement value = object.get(name);
        return value == null || value.isJsonNull() ? fallback : value.getAsString();
    }
    private static double requiredDouble(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || value.isJsonNull()) {
            throw new IllegalArgumentException("Missing number argument " + name);
        }
        double result = value.getAsDouble();
        if (!Double.isFinite(result)) {
            throw new IllegalArgumentException("Invalid number argument " + name);
        }
        return result;
    }
    private static OptionalDouble optionalDouble(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || value.isJsonNull()) {
            return OptionalDouble.empty();
        }
        double result = value.getAsDouble();
        if (!Double.isFinite(result)) {
            throw new IllegalArgumentException("Invalid number argument " + name);
        }
        return OptionalDouble.of(result);
    }
    private static long optionalLong(JsonObject object, String name, long fallback) {
        JsonElement value = object.get(name);
        return value == null || value.isJsonNull() ? fallback : value.getAsLong();
    }
}
