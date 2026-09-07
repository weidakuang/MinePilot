package dev.mcai.companion.agent.model;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Minimal dependency-free OpenAI-compatible Chat Completions client. */
public final class OpenAiCompatibleChatClient implements AutoCloseable {
    private static final Gson GSON = new Gson();

    private final ModelConfig config;
    private final HttpClient http;
    private final URI endpoint;

    public OpenAiCompatibleChatClient(ModelConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.endpoint = chatCompletionsEndpoint(config.baseUri());
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public CompletableFuture<AssistantTurn> complete(
            List<JsonObject> messages,
            JsonArray tools
    ) {
        JsonObject body = new JsonObject();
        body.addProperty("model", config.model());
        body.addProperty("temperature", config.temperature());
        body.addProperty("max_tokens", 2_048);
        body.addProperty("parallel_tool_calls", false);
        JsonArray messageArray = new JsonArray();
        messages.forEach(message -> messageArray.add(message.deepCopy()));
        body.add("messages", messageArray);
        if (tools != null && !tools.isEmpty()) {
            body.add("tools", tools.deepCopy());
            body.addProperty("tool_choice", "required");
        }

        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(90))
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();
        var transport=http.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        var result=transport.thenApply(this::parseResponse);
        result.whenComplete((ignored,failure)->{if(result.isCancelled())transport.cancel(true);});
        return result;
    }

    private AssistantTurn parseResponse(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ModelRequestException(
                    "Model endpoint returned HTTP " + response.statusCode());
        }
        JsonObject root;
        try {
            root = JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (RuntimeException malformed) {
            throw new ModelRequestException("Model endpoint returned invalid JSON", malformed);
        }
        JsonArray choices = root.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new ModelRequestException("Model response contained no choices");
        }
        JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
        if (message == null) {
            throw new ModelRequestException("Model response contained no assistant message");
        }
        List<ToolCall> calls = new ArrayList<>();
        JsonArray rawCalls = message.getAsJsonArray("tool_calls");
        if (rawCalls != null) {
            for (JsonElement element : rawCalls) {
                JsonObject call = element.getAsJsonObject();
                JsonObject function = call.getAsJsonObject("function");
                if (function == null || !call.has("id") || !function.has("name")
                        || !function.has("arguments")) {
                    throw new ModelRequestException("Malformed model tool call");
                }
                JsonObject arguments;
                try {
                    JsonElement parsed = JsonParser.parseString(
                            function.get("arguments").getAsString());
                    if (parsed.isJsonPrimitive() && parsed.getAsJsonPrimitive().isString()) {
                        parsed = JsonParser.parseString(parsed.getAsString());
                    }
                    arguments = parsed.getAsJsonObject();
                } catch (RuntimeException malformed) {
                    throw new ModelRequestException(
                            "Tool arguments were not a JSON object", malformed);
                }
                calls.add(new ToolCall(
                        call.get("id").getAsString(),
                        function.get("name").getAsString(),
                        arguments
                ));
            }
        }
        Optional<String> content = Optional.empty();
        if (message.has("content") && !message.get("content").isJsonNull()) {
            String text = message.get("content").getAsString();
            if (!text.isBlank()) {
                content = Optional.of(text);
            }
        }
        return new AssistantTurn(message.deepCopy(), content, calls);
    }

    private static URI chatCompletionsEndpoint(URI baseUri) {
        String value = baseUri.toString().replaceAll("/+$", "");
        if (value.endsWith("/chat/completions")) {
            return URI.create(value);
        }
        if (value.endsWith("/v1")) {
            return URI.create(value + "/chat/completions");
        }
        return URI.create(value + "/v1/chat/completions");
    }

    @Override
    public void close() {
        // Java 25 exposes a nonblocking shutdown; world stop must not wait for a provider.
        http.shutdownNow();
    }

    public record AssistantTurn(
            JsonObject rawMessage,
            Optional<String> content,
            List<ToolCall> toolCalls
    ) {
        public AssistantTurn {
            rawMessage = rawMessage.deepCopy();
            content = Objects.requireNonNull(content, "content");
            toolCalls = List.copyOf(toolCalls);
        }
    }

    public record ToolCall(String id, String name, JsonObject arguments) {
        public ToolCall {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(name, "name");
            arguments = arguments.deepCopy();
        }
    }

    public static final class ModelRequestException extends RuntimeException {
        public ModelRequestException(String message) {
            super(message);
        }

        public ModelRequestException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
