package dev.mcai.companion.agent.model;

import java.util.Arrays;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/** OpenAI-compatible function schemas kept flat for broad model compatibility. */
public final class AgentToolSchemas {
    private AgentToolSchemas() {
    }

    public static JsonArray navigationTools() {
        return navigationTools(false,
                "request_navigation", "say", "plan_navigation",
                "choose_navigation", "cancel_navigation", "listen");
    }

    public static JsonArray navigationTools(
            boolean acknowledgementRequired,
            String... allowedNames
    ) {
        Set<String> allowed = Set.copyOf(Arrays.asList(allowedNames));
        JsonArray tools = new JsonArray();
        if (allowed.contains("request_navigation")) {
            tools.add(tool("request_navigation",
                    "Validate and reserve a physical movement intent. This does not plan or move yet.",
                    requestNavigationParameters()));
        }
        if (allowed.contains("say")) {
            tools.add(tool("say",
                    acknowledgementRequired
                            ? "Emit the one required visible acknowledgement for the active navigation request."
                            : "Send a short visible server chat message as the Agent.",
                    sayParameters(acknowledgementRequired)));
        }
        if (allowed.contains("plan_navigation")) {
            tools.add(tool("plan_navigation",
                    "After SAY_SENT, calculate one to eight physically evaluated route choices.",
                    requestIdParameters()));
        }
        if (allowed.contains("choose_navigation")) {
            tools.add(tool("choose_navigation",
                    "Select one returned route and an allowed movement pace for continuous execution.",
                    chooseNavigationParameters()));
        }
        if (allowed.contains("cancel_navigation")) {
            tools.add(tool("cancel_navigation",
                    "Cancel the active navigation request.",
                    cancelParameters()));
        }
        for(var definition:dev.mcai.companion.agent.knowledge.KnowledgeTools.definitions()) {
            var value=definition.getAsJsonObject();String name=value.get("name").getAsString();
            if(allowed.contains(name))tools.add(tool(name,value.get("description").getAsString(),value.getAsJsonObject("inputSchema")));
        }
        for(var definition:dev.mcai.companion.agent.mining.MiningTools.definitions()) {
            var value=definition.getAsJsonObject();String name=value.get("name").getAsString();
            if(allowed.contains(name))tools.add(tool(name,value.get("description").getAsString(),value.getAsJsonObject("inputSchema")));
        }
        for(var definition:dev.mcai.companion.agent.mining.CollectionTools.definitions()) {
            var value=definition.getAsJsonObject();String name=value.get("name").getAsString();
            if(allowed.contains(name))tools.add(tool(name,value.get("description").getAsString(),value.getAsJsonObject("inputSchema")));
        }
        for(var definition:dev.mcai.companion.agent.placement.PlacementTools.definitions()) {
            var value=definition.getAsJsonObject();String name=value.get("name").getAsString();
            if(allowed.contains(name))tools.add(tool(name,value.get("description").getAsString(),value.getAsJsonObject("inputSchema")));
        }
        if (tools.isEmpty()) {
            throw new IllegalArgumentException("At least one model tool must be allowed");
        }
        return tools;
    }

    private static JsonObject requestNavigationParameters() {
        JsonObject properties = new JsonObject();
        JsonObject follow = new JsonObject(); follow.addProperty("type", "boolean");
        follow.addProperty("description", "Maintain following a player/entity until cancelled; false for one arrival.");
        properties.add("continuous_follow", follow);
        properties.add("replace_request_id",string("Exact active request UUID when a new destination replaces it; omit for a new idle goal."));
        properties.add("forward_blocks",number("Signed forward distance of magnitude [0.5,8]; omit x/y/z when used."));
        JsonObject partial=new JsonObject();partial.addProperty("type","boolean");partial.addProperty("description","Opt in to an evaluated partial approach; APPROACHED never means full arrival.");properties.add("allow_partial",partial);
        properties.add("target_kind", stringEnum(
                "coordinates", "player", "entity", "dropped_item", "waypoint", "world_spawn", "respawn_point", "death_point"));
        properties.add("dimension", string("Dimension identifier; omit to use the Agent's dimension."));
        properties.add("x", number("Destination X for coordinates."));
        properties.add("y", number("Destination Y for coordinates."));
        properties.add("z", number("Destination Z for coordinates."));
        properties.add("target_name", string("Exact player/entity name or UUID for named targets."));
        properties.add("acceptance_radius", number("Arrival radius from 0.5 to 32 blocks."));
        properties.add("arrival_heading", number(
                "Optional compass heading in degrees: north=0, east=90, south=180, west=270."));
        properties.add("preferred_pace", stringEnum(
                "auto", "walk", "sprint", "sprint_jump", "sneak"));
        properties.add("player_intent", string(
                "A compact faithful restatement of the player's movement request."));
        return objectSchema(properties,
                "target_kind", "preferred_pace", "player_intent");
    }

    private static JsonObject sayParameters(boolean acknowledgementRequired) {
        JsonObject properties = new JsonObject();
        properties.add("message", string("Natural chat text in the player's language."));
        properties.add("navigation_request_id", string(
                "Required when this message acknowledges NAVIGATION_ACCEPTED."));
        return acknowledgementRequired
                ? objectSchema(properties, "message", "navigation_request_id")
                : objectSchema(properties, "message");
    }

    private static JsonObject requestIdParameters() {
        JsonObject properties = new JsonObject();
        properties.add("request_id", string("Navigation request UUID from NAVIGATION_ACCEPTED."));
        return objectSchema(properties, "request_id");
    }

    private static JsonObject chooseNavigationParameters() {
        JsonObject properties = new JsonObject();
        properties.add("request_id", string("Navigation request UUID."));
        properties.add("option_id", string("One option identifier returned by plan_navigation."));
        properties.add("pace", stringEnum(
                "auto", "walk", "sprint", "sprint_jump", "sneak"));
        return objectSchema(properties, "request_id", "option_id", "pace");
    }

    private static JsonObject cancelParameters() {
        JsonObject properties = new JsonObject();
        properties.add("request_id", string("Navigation request UUID."));
        properties.add("reason", string("Short truthful cancellation reason."));
        return objectSchema(properties, "request_id", "reason");
    }

    private static JsonObject tool(String name, String description, JsonObject parameters) {
        JsonObject function = new JsonObject();
        function.addProperty("name", name);
        function.addProperty("description", description);
        function.add("parameters", parameters);
        JsonObject tool = new JsonObject();
        tool.addProperty("type", "function");
        tool.add("function", function);
        return tool;
    }

    private static JsonObject objectSchema(JsonObject properties, String... requiredNames) {
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
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "string");
        schema.addProperty("description", description);
        return schema;
    }

    private static JsonObject integer(String description) {
        JsonObject schema = number(description);
        schema.addProperty("type", "integer");
        return schema;
    }

    private static JsonObject number(String description) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "number");
        schema.addProperty("description", description);
        return schema;
    }

    private static JsonObject stringEnum(String... values) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "string");
        JsonArray choices = new JsonArray();
        for (String value : values) {
            choices.add(value);
        }
        schema.add("enum", choices);
        return schema;
    }
}
