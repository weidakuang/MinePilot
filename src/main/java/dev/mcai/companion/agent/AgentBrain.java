package dev.mcai.companion.agent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.mcai.companion.agent.body.MinePilotServerPlayer;
import dev.mcai.companion.agent.model.AgentToolSchemas;
import dev.mcai.companion.agent.model.ModelConfig;
import dev.mcai.companion.agent.model.OpenAiCompatibleChatClient;
import dev.mcai.companion.agent.model.OpenAiCompatibleChatClient.AssistantTurn;
import dev.mcai.companion.agent.model.OpenAiCompatibleChatClient.ToolCall;
import dev.mcai.companion.agent.navigation.NavigationEvent;
import dev.mcai.companion.agent.navigation.NavigationIntent;
import dev.mcai.companion.agent.navigation.NavigationTarget;
import dev.mcai.companion.agent.navigation.NavigationToolCoordinator;
import dev.mcai.companion.agent.navigation.RouteOption;
import dev.mcai.companion.agent.navigation.TravelPace;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Single-request model loop that turns natural chat into typed tool calls. */
public final class AgentBrain implements AutoCloseable {
    private static final int MAX_HISTORY_MESSAGES = 80;
    private static final int MAX_CHAT_CHARACTERS = 512;

    private final MinecraftServer server;
    private final MinePilotServerPlayer player;
    private final OpenAiCompatibleChatClient model;
    private final List<JsonObject> history = new ArrayList<>();
    private final Deque<PlayerInput> queuedInputs = new ArrayDeque<>();
    private final Deque<NavigationEvent> deferredNavigationEvents = new ArrayDeque<>();
    private final Queue<Runnable> completions = new ConcurrentLinkedQueue<>();

    private NavigationToolCoordinator navigation;
    private boolean modelBusy;
    private long modelGeneration;
    private java.util.concurrent.CompletableFuture<AssistantTurn> activeModelRequest;
    private boolean executingToolCall;
    private String pendingPlanToolCallId;
    private String currentRequester = "player";
    private int protocolRepairAttempts;
    private JsonObject pendingMiningEvent;
    private String miningMarker="",collectionMarker="",placementMarker="";

    public AgentBrain(
            MinecraftServer server,
            MinePilotServerPlayer player,
            ModelConfig config
    ) {
        this.server = Objects.requireNonNull(server, "server");
        this.player = Objects.requireNonNull(player, "player");
        this.model = new OpenAiCompatibleChatClient(config);
        history.add(message("system", AgentSystemPrompt.text()));
    }

    public void attachNavigation(NavigationToolCoordinator coordinator) {
        requireServerThread();
        if (navigation != null) {
            throw new IllegalStateException("Navigation coordinator is already attached");
        }
        navigation = Objects.requireNonNull(coordinator, "coordinator");
    }

    public void onPlayerChat(ServerPlayer speaker, String rawText) {
        requireServerThread();
        if (speaker == player || rawText == null || rawText.isBlank()) {
            return;
        }
        onChat(speaker.getGameProfile().name(), rawText);
    }

    public void onChat(String speakerName, String rawText) {
        requireServerThread();
        if (rawText == null || rawText.isBlank()) return;
        String text = rawText.length() > MAX_CHAT_CHARACTERS
                ? rawText.substring(0, MAX_CHAT_CHARACTERS)
                : rawText;
        if(queuedInputs.size()>=32)queuedInputs.removeFirst();
        queuedInputs.addLast(new PlayerInput(speakerName, text));
        // Keep physical travel running; only the obsolete model decision is replaced.
        if(modelBusy){modelGeneration++;modelBusy=false;if(activeModelRequest!=null)activeModelRequest.cancel(true);}
        if(pendingPlanToolCallId!=null){appendToolResult(pendingPlanToolCallId,"{\"status\":\"INTERRUPTED_BY_PLAYER_CHAT\"}");pendingPlanToolCallId=null;}
        startQueuedInputIfPossible();
    }

    public void onLocallyHandledStop() {
        requireServerThread();modelGeneration++;modelBusy=false;
        if(activeModelRequest!=null)activeModelRequest.cancel(true);
        queuedInputs.clear();pendingMiningEvent=null;
        if(pendingPlanToolCallId!=null){appendToolResult(pendingPlanToolCallId,"{\"status\":\"CANCELLED_BY_PLAYER\"}");pendingPlanToolCallId=null;}
        history.add(message("user","The player stopped the active action through the local control. It has stopped and been acknowledged. Do not resume an old task."));
        trimHistory();
    }

    public void tick() {
        requireServerThread();
        Runnable completion;
        while ((completion = completions.poll()) != null) {
            completion.run();
        }
        var runtime=AgentRuntime.active(server);
        if(runtime!=null){
            var placement=runtime.placement().status();String placementNext=placement.get("phase").getAsString()+placement.get("requestId")+placement.get("decisionId");
            if(!placementNext.equals(placementMarker)){placementMarker=placementNext;if(java.util.Set.of("COMPLETED","PARTIAL","BLOCKED").contains(placement.get("phase").getAsString()))pendingMiningEvent=placement;}
            var collection=runtime.collection().status();String next=collection.get("phase").getAsString()+collection.get("requestId");
            if(!next.equals(collectionMarker)){
                collectionMarker=next;
                if(java.util.Set.of("COMPLETED","BLOCKED").contains(collection.get("phase").getAsString()))pendingMiningEvent=collection;
            }
            var state=runtime.mining().status();String marker=state.get("phase").getAsString()+state.get("requestId");
            if(!marker.equals(miningMarker)){
                miningMarker=marker;
                if(!runtime.placement().ownsBody() && !runtime.placement().isChildRequest(state) && !runtime.collection().ownsBody() && !runtime.collection().isChildRequest(state) && java.util.Set.of("COMPLETED","BLOCKED").contains(state.get("phase").getAsString()))pendingMiningEvent=state;
            }
        }
        startQueuedInputIfPossible();
        if(!modelBusy && pendingPlanToolCallId==null && queuedInputs.isEmpty() && pendingMiningEvent!=null){
            history.add(message("user","BODY_JOB_RESULT (not a new player request; do not repeat the break; speech optional):\n"+pendingMiningEvent));
            pendingMiningEvent=null;trimHistory();requestModel();
        }
    }

    public void onNavigationEvent(NavigationEvent event) {
        requireServerThread();
        if (executingToolCall) {
            deferredNavigationEvents.addLast(event);
            return;
        }
        handleNavigationEvent(event);
    }

    private void handleNavigationEvent(NavigationEvent event) {
        if(navigation.status().requestId()!=null && !navigation.status().requestId().equals(event.requestId()))return;
        if (event.type() == NavigationEvent.Type.NAVIGATION_PLAN_READY
                && pendingPlanToolCallId != null) {
            appendToolResult(pendingPlanToolCallId, eventJson(event, true));
            pendingPlanToolCallId = null;
            requestModel();
            return;
        }
        if (pendingPlanToolCallId != null
                && (event.type() == NavigationEvent.Type.NAVIGATION_FAILED
                || event.type() == NavigationEvent.Type.NAVIGATION_CANCELLED
                || event.type() == NavigationEvent.Type.NAVIGATION_DECISION_REQUIRED)) {
            appendToolResult(pendingPlanToolCallId, eventJson(event, false));
            pendingPlanToolCallId = null;
            requestModel();
            return;
        }
        if (event.type() == NavigationEvent.Type.NAVIGATION_COMPLETED
                || event.type() == NavigationEvent.Type.NAVIGATION_PLAN_READY
                || event.type() == NavigationEvent.Type.NAVIGATION_FAILED
                || event.type() == NavigationEvent.Type.NAVIGATION_CANCELLED
                || event.type() == NavigationEvent.Type.NAVIGATION_DECISION_REQUIRED) {
            history.add(message("user", "NAVIGATION_EVENT\n" + eventJson(event, false)));
            trimHistory();
            requestModel();
        }
    }

    private void startQueuedInputIfPossible() {
        if (modelBusy || pendingPlanToolCallId != null || queuedInputs.isEmpty()) {
            return;
        }
        PlayerInput input = queuedInputs.removeFirst();
        currentRequester = input.playerName();
        history.add(message("user",
                "PLAYER_MESSAGE from " + input.playerName() + ":\n"
                        + input.text() + "\n\nWORLD_STATE:\n" + worldState()));
        trimHistory();
        requestModel();
    }

    private void requestModel() {
        if (modelBusy || pendingPlanToolCallId != null) {
            return;
        }
        modelBusy = true;
        List<JsonObject> requestHistory = history.stream()
                .map(JsonObject::deepCopy)
                .toList();
        long generation=++modelGeneration;
        activeModelRequest=model.complete(requestHistory, toolsForCurrentPhase());
        activeModelRequest.whenComplete((turn, failure) ->
                completions.add(() -> {if(generation==modelGeneration)completeModelTurn(turn, failure);}));
    }

    private void completeModelTurn(AssistantTurn turn, Throwable failure) {
        requireServerThread();
        modelBusy = false;
        if (failure != null) {
            speak("I could not reach the configured model just now.");
            startQueuedInputIfPossible();
            return;
        }
        history.add(turn.rawMessage());
        trimHistory();
        if (turn.toolCalls().isEmpty()) {
            if (protocolRepairAttempts++ == 0) {
                history.add(message("user",
                        "PROTOCOL_ERROR: Call exactly one of AVAILABLE_TOOLS. "
                                + "Prose without a tool is not executable and will not be shown."));
                trimHistory();
                requestModel();
            } else {
                speak("The configured model did not return an executable tool call.");
                protocolRepairAttempts = 0;
                startQueuedInputIfPossible();
            }
            return;
        }
        protocolRepairAttempts = 0;

        ToolCall first = turn.toolCalls().getFirst();
        ToolExecution execution;
        executingToolCall = true;
        try {
            execution = execute(first);
        } catch (RuntimeException error) {
            execution = ToolExecution.continueWith(errorJson(error));
        } finally {
            executingToolCall = false;
        }
        if (execution.immediateResult() != null) {
            appendToolResult(first.id(), execution.immediateResult());
        }
        for (int index = 1; index < turn.toolCalls().size(); index++) {
            appendToolResult(turn.toolCalls().get(index).id(),
                    "{\"status\":\"PROTOCOL_ERROR\","
                            + "\"message\":\"Call exactly one tool per model turn\"}");
        }
        while (!deferredNavigationEvents.isEmpty()) {
            handleNavigationEvent(deferredNavigationEvents.removeFirst());
        }
        switch (execution.nextAction()) {
            case WAIT_FOR_EVENT -> {
                return;
            }
            case COMPLETE_INPUT -> {
                startQueuedInputIfPossible();
                return;
            }
            case REQUEST_MODEL -> requestModel();
        }
    }

    private ToolExecution execute(ToolCall call) {
        if (navigation == null) {
            throw new IllegalStateException("Navigation runtime is not attached");
        }
        boolean offered=false;
        for(var tool:toolsForCurrentPhase())if(tool.getAsJsonObject().getAsJsonObject("function").get("name").getAsString().equals(call.name()))offered=true;
        if(!offered)throw new IllegalArgumentException("Tool is not available in the current phase: "+call.name());
        if(dev.mcai.companion.agent.placement.PlacementTools.NAMES.contains(call.name())) {
            var result=dev.mcai.companion.agent.placement.PlacementTools.execute(AgentRuntime.active(server),call.name(),call.arguments());
            return java.util.Set.of("place_block","choose_placement","resume_placement","resolve_placement","pause_placement","cancel_placement").contains(call.name()) && result.has("phase") && java.util.Set.of("EXECUTING","PAUSED","CANCELLED").contains(result.get("phase").getAsString()) ? ToolExecution.waiting(result.toString()) : ToolExecution.continueWith(result.toString());
        }
        if(dev.mcai.companion.agent.mining.CollectionTools.NAMES.contains(call.name()))
            return ToolExecution.continueWith(dev.mcai.companion.agent.mining.CollectionTools.execute(AgentRuntime.active(server),call.name(),call.arguments()).toString());
        if(dev.mcai.companion.agent.mining.MiningTools.NAMES.contains(call.name()))
            return ToolExecution.continueWith(dev.mcai.companion.agent.mining.MiningTools.execute(AgentRuntime.active(server),call.name(),call.arguments()).toString());
        return switch (call.name()) {
            case "request_navigation" -> requestNavigation(call.arguments());
            case "say" -> say(call.arguments());
            case "listen","turn","inventory","inventory_events","annotate_item","waypoint","sense" ->
                    ToolExecution.continueWith(new dev.mcai.companion.agent.knowledge.KnowledgeTools(AgentRuntime.active(server)).execute(call.name(),call.arguments()).toString());
            case "plan_navigation" -> planNavigation(call.id(), call.arguments());
            case "choose_navigation" -> chooseNavigation(call.arguments());
            case "cancel_navigation" -> cancelNavigation(call.arguments());
            default -> throw new IllegalArgumentException("Unknown tool: " + call.name());
        };
    }

    private ToolExecution requestNavigation(JsonObject arguments) {
        UUID requestId = UUID.randomUUID();
        TravelPace pace = pace(requiredString(arguments, "preferred_pace"));
        NavigationTarget target = target(arguments);
        NavigationEvent accepted = navigation.requestNavigation(new NavigationIntent(
                requestId,
                navigation.status().worldRevision(),
                target,
                pace,
                requiredString(arguments, "player_intent"),
                currentRequester, arguments.has("allow_partial") && arguments.get("allow_partial").getAsBoolean(),
                arguments.has("continuous_follow") && arguments.get("continuous_follow").getAsBoolean()
        ), arguments.has("replace_request_id") ? UUID.fromString(requiredString(arguments,"replace_request_id")) : null);
        return ToolExecution.continueWith(eventJson(accepted, false));
    }

    private ToolExecution say(JsonObject arguments) {
        String text = requiredString(arguments, "message").strip();
        if (text.isEmpty() || text.length() > MAX_CHAT_CHARACTERS) {
            throw new IllegalArgumentException("Chat message must contain 1 to 512 characters");
        }
        boolean acknowledgement = navigation.status().phase()
                == NavigationToolCoordinator.Phase.ACKNOWLEDGEMENT_REQUIRED;
        if (acknowledgement) {
            String rawRequestId = requiredString(arguments, "navigation_request_id");
            UUID requestId = UUID.fromString(rawRequestId);
            NavigationToolCoordinator.Status status = navigation.status();
            if (!requestId.equals(status.requestId())) {
                throw new IllegalArgumentException(
                        "Acknowledgement request id does not match active navigation");
            }
            speak(text);
            navigation.acknowledgementSent(requestId);
        } else {
            speak(text);
        }
        JsonObject result = new JsonObject();
        result.addProperty("status", "SAY_SENT");
        result.addProperty("message", text);
        return acknowledgement
                ? ToolExecution.continueWith(result.toString())
                : ToolExecution.complete(result.toString());
    }

    private ToolExecution planNavigation(String toolCallId, JsonObject arguments) {
        UUID requestId = UUID.fromString(requiredString(arguments, "request_id"));
        navigation.planNavigation(requestId);
        pendingPlanToolCallId = toolCallId;
        return ToolExecution.waiting();
    }

    private ToolExecution chooseNavigation(JsonObject arguments) {
        UUID requestId = UUID.fromString(requiredString(arguments, "request_id"));
        String optionId = requiredString(arguments, "option_id");
        TravelPace pace = pace(requiredString(arguments, "pace"));
        boolean started = navigation.chooseNavigation(requestId, optionId, pace);
        JsonObject result = new JsonObject();
        result.addProperty("status", started
                ? "NAVIGATION_STARTED"
                : "NAVIGATION_DECISION_REQUIRED");
        result.addProperty("requestId", requestId.toString());
        result.addProperty("optionId", optionId);
        return ToolExecution.waiting(result.toString());
    }

    private ToolExecution cancelNavigation(JsonObject arguments) {
        UUID requestId = UUID.fromString(requiredString(arguments, "request_id"));
        navigation.cancel(requestId, requiredString(arguments, "reason"));
        return ToolExecution.waiting(
                "{\"status\":\"NAVIGATION_CANCEL_REQUESTED\"}");
    }

    private NavigationTarget target(JsonObject arguments) {
        return dev.mcai.companion.agent.navigation.NavigationTargetCodec.decode(AgentRuntime.active(server),arguments);
    }

    private String eventJson(NavigationEvent event, boolean includeRoutes) {
        JsonObject root = new JsonObject();
        root.addProperty("status", event.type().name());
        root.addProperty("requestId", event.requestId().toString());
        root.addProperty("message", event.message());
        root.add("observedState", json(event.observedState()));
        JsonArray actions = new JsonArray();
        event.validActions().forEach(actions::add);
        root.add("validActions", actions);
        if (includeRoutes) {
            JsonArray routes = new JsonArray();
            for (RouteOption route : navigation.status().routeOptions()) {
                routes.add(routeJson(route));
            }
            root.add("routes", routes);
        }
        return root.toString();
    }

    private static JsonObject routeJson(RouteOption route) {
        JsonObject value = new JsonObject();
        value.addProperty("optionId", route.optionId());
        value.addProperty("label", route.label());
        value.addProperty("suggestedPace", route.suggestedPace().name().toLowerCase(Locale.ROOT));
        JsonArray supported = new JsonArray();
        route.supportedPaces().stream().map(Enum::name).sorted()
                .map(name -> name.toLowerCase(Locale.ROOT)).forEach(supported::add);
        value.add("supportedPaces", supported);
        value.addProperty("estimatedSeconds", route.estimatedSeconds());
        value.addProperty("distanceBlocks", route.distanceBlocks());
        value.addProperty("estimatedExhaustion", route.estimatedExhaustion());
        value.addProperty("estimatedFoodPointsLost", route.estimatedFoodPointsLost());
        value.addProperty("estimatedHealthLost", route.estimatedHealthLost());
        value.addProperty("supportBlocksRequired", route.supportBlocksRequired());
        value.add("supportMaterials",new com.google.gson.Gson().toJsonTree(route.supportMaterials()));
        value.addProperty("riskScore", route.riskScore());
        value.addProperty("feasibleNow", route.feasibleNow());
        JsonArray hazards = new JsonArray();
        route.hazards().forEach(hazards::add);
        value.add("hazards", hazards);
        JsonArray actions = new JsonArray();
        route.requiredActions().forEach(actions::add);
        value.add("requiredActions", actions);
        JsonArray steps = new JsonArray();
        for (RouteOption.PathStep step : route.steps()) {
            JsonArray compact = new JsonArray();
            compact.add(step.x());
            compact.add(step.y());
            compact.add(step.z());
            compact.add(step.action().name().toLowerCase(Locale.ROOT));
            compact.add(step.recommendedPace().name().toLowerCase(Locale.ROOT));
            steps.add(compact);
        }
        value.add("pathSteps", steps);
        return value;
    }

    private String worldState() {
        JsonObject state = new JsonObject();
        state.addProperty("agentName", player.getGameProfile().name());
        state.addProperty("dimension", player.level().dimension().identifier().toString());
        state.addProperty("x", player.getX());
        state.addProperty("y", player.getY());
        state.addProperty("z", player.getZ());
        state.addProperty("health", player.getHealth());
        state.addProperty("absorption", player.getAbsorptionAmount());
        state.addProperty("food", player.getFoodData().getFoodLevel());
        state.addProperty("saturation", player.getFoodData().getSaturationLevel());
        state.addProperty("navigationPhase", navigation == null
                ? "unavailable"
                : navigation.status().phase().name());
        state.addProperty("worldRevision", navigation == null
                ? 0L
                : navigation.status().worldRevision());

        var runtime=AgentRuntime.active(server);
        state.add("world",runtime.perception.summary());state.add("inventorySummary",player.inventoryLedger.inventory());state.add("mining",runtime.mining().status());state.add("collection",runtime.collection().status());state.add("placement",runtime.placement().status());
        if(navigation.status().requestId()!=null)state.addProperty("requestId",navigation.status().requestId().toString());
        JsonArray players=new JsonArray();int count=0;
        for(var other:server.getPlayerList().getPlayers()) {
            if(other instanceof MinePilotServerPlayer)continue;count++;if(players.size()>=64)continue;
            var entry=new JsonObject();entry.addProperty("name",other.getGameProfile().name());entry.addProperty("uuid",other.getUUID().toString());
            boolean observable=other.level()==player.level() && runtime.perception.sensed(other);entry.addProperty("observable",observable);
            if(observable){entry.addProperty("dimension",other.level().dimension().identifier().toString());entry.addProperty("x",other.getX());entry.addProperty("y",other.getY());entry.addProperty("z",other.getZ());}
            players.add(entry);
        }
        state.add("onlinePlayers",players);state.addProperty("onlinePlayersTruncated",count>players.size());
        return state.toString();
    }

    private void speak(String text) {
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("[AI] " + player.getGameProfile().name() + ": " + text),
                false
        );
    }

    private void appendToolResult(String toolCallId, String content) {
        JsonObject message = message("tool", content);
        message.addProperty("tool_call_id", toolCallId);
        history.add(message);
        trimHistory();
    }

    private void trimHistory() {
        while (history.size() > MAX_HISTORY_MESSAGES) {
            int endExclusive = 2;
            while (endExclusive < history.size()
                    && !"user".equals(role(history.get(endExclusive)))) {
                endExclusive++;
            }
            if (endExclusive >= history.size()) {
                // Keep an oversized in-flight interaction intact. Splitting
                // assistant tool_calls from tool results makes the next API
                // request invalid.
                break;
            }
            history.subList(1, endExclusive).clear();
        }
    }

    private JsonArray toolsForCurrentPhase() {
        var runtime=AgentRuntime.active(server);
        if(runtime!=null && runtime.placement().ownsBody())return AgentToolSchemas.navigationTools(false,
                "say","listen","sense","inventory","inventory_events","annotate_item","waypoint","inventory_capacity","inspect_placement",
                "placement_status","pause_placement","resume_placement","cancel_placement","set_hand");
        if(runtime!=null && runtime.collection().ownsBody())return AgentToolSchemas.navigationTools(false,
                "say","listen","sense","inventory","inventory_events","annotate_item","waypoint","inspect_tree","tree_farm",
                "collection_status","pause_collection","resume_collection","cancel_collection");
        if(runtime!=null && runtime.mining().ownsBody())return AgentToolSchemas.navigationTools(false,
                "say","listen","sense","inventory","inventory_events","annotate_item","waypoint",
                "mining_status","pause_mining","resume_mining","cancel_mining");
        NavigationToolCoordinator.Phase phase = navigation == null
                ? NavigationToolCoordinator.Phase.IDLE
                : navigation.status().phase();
        return switch (phase) {
            case ACKNOWLEDGEMENT_REQUIRED ->
                    AgentToolSchemas.navigationTools(true, "say", "cancel_navigation");
            case ACKNOWLEDGED -> AgentToolSchemas.navigationTools(
                    false, "plan_navigation", "cancel_navigation", "say", "listen", "sense", "inventory");
            case PLAN_READY -> AgentToolSchemas.navigationTools(
                    false, "choose_navigation", "cancel_navigation", "say", "listen", "sense", "inventory");
            case REPLAN_REQUIRED -> AgentToolSchemas.navigationTools(
                    false, "plan_navigation", "cancel_navigation", "say", "listen", "sense", "inventory");
            case PLANNING -> AgentToolSchemas.navigationTools(
                    false, "cancel_navigation", "say", "listen", "sense", "inventory");
            case EXECUTING, FOLLOWING -> AgentToolSchemas.navigationTools(
                    false, "request_navigation", "say", "cancel_navigation", "listen", "inventory", "inventory_events", "sense", "waypoint", "annotate_item");
            case IDLE, COMPLETED, APPROACHED, FAILED, CANCELLED ->
                    AgentToolSchemas.navigationTools(
                            false, "request_navigation", "say", "listen", "turn", "inventory", "inventory_events", "sense", "waypoint", "annotate_item",
                            "equip_tool","plan_mining","choose_mining","mining_status","pause_mining","resume_mining","cancel_mining",
                            "inspect_tree","tree_farm","plan_collection","choose_collection","collection_status","pause_collection","resume_collection","cancel_collection",
                            "set_hand","inventory_capacity","inspect_placement","place_block","plan_placement","choose_placement","placement_status","pause_placement","resume_placement","cancel_placement","resolve_placement");
        };
    }

    private static String role(JsonObject message) {
        JsonElement role = message.get("role");
        return role == null || role.isJsonNull() ? "" : role.getAsString();
    }

    private static JsonObject message(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        return message;
    }

    private static JsonObject json(Map<String, Object> source) {
        JsonObject result = new JsonObject();
        source.forEach((key, value) -> {
            if (value instanceof Number number) {
                result.addProperty(key, number);
            } else if (value instanceof Boolean bool) {
                result.addProperty(key, bool);
            } else if (value != null) {
                result.addProperty(key, value.toString());
            }
        });
        return result;
    }

    private static String errorJson(Throwable error) {
        JsonObject result = new JsonObject();
        result.addProperty("status", "TOOL_ERROR");
        result.addProperty("message", error.getMessage() == null
                ? error.getClass().getSimpleName()
                : error.getMessage());
        return result.toString();
    }

    private static String requiredString(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()
                || value.getAsString().isBlank()) {
            throw new IllegalArgumentException("Missing string argument " + name);
        }
        return value.getAsString();
    }

    private static String optionalString(JsonObject object, String name, String fallback) {
        JsonElement value = object.get(name);
        return value == null || value.isJsonNull() || value.getAsString().isBlank()
                ? fallback
                : value.getAsString();
    }

    private static double requiredDouble(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || value.isJsonNull()) {
            throw new IllegalArgumentException("Missing number argument " + name);
        }
        double number = value.getAsDouble();
        if (!Double.isFinite(number)) {
            throw new IllegalArgumentException("Invalid number argument " + name);
        }
        return number;
    }

    private static OptionalDouble optionalDouble(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || value.isJsonNull()) {
            return OptionalDouble.empty();
        }
        double number = value.getAsDouble();
        if (!Double.isFinite(number)) {
            throw new IllegalArgumentException("Invalid number argument " + name);
        }
        return OptionalDouble.of(number);
    }

    private static TravelPace pace(String name) {
        return TravelPace.valueOf(name.toUpperCase(Locale.ROOT));
    }

    private void requireServerThread() {
        if (!server.isSameThread()) {
            throw new IllegalStateException("Agent brain must run on the server thread");
        }
    }

    @Override
    public void close() {
        modelGeneration++;if(activeModelRequest!=null)activeModelRequest.cancel(true);
        model.close();
    }

    private record PlayerInput(String playerName, String text) {
    }

    private record ToolExecution(String immediateResult, NextAction nextAction) {
        private static ToolExecution continueWith(String value) {
            return new ToolExecution(value, NextAction.REQUEST_MODEL);
        }

        private static ToolExecution waiting() {
            return new ToolExecution(null, NextAction.WAIT_FOR_EVENT);
        }

        private static ToolExecution waiting(String value) {
            return new ToolExecution(value, NextAction.WAIT_FOR_EVENT);
        }

        private static ToolExecution complete(String value) {
            return new ToolExecution(value, NextAction.COMPLETE_INPUT);
        }
    }

    private enum NextAction {
        REQUEST_MODEL,
        WAIT_FOR_EVENT,
        COMPLETE_INPUT
    }
}
