package dev.mcai.companion.vendor.numen.tools;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.Map;

/** Small JSON adapter for the ported Numen tool results. */
final class ActionResult {
    private final JsonObject value = new JsonObject();
    private ActionResult(boolean success, String message, Map<String, ?> data) {
        value.addProperty("status", success ? "COMPLETED" : "BLOCKED");
        value.addProperty("success", success);
        value.addProperty("message", message);
        new Gson().toJsonTree(data).getAsJsonObject().entrySet().forEach(e -> value.add(e.getKey(), e.getValue()));
    }
    static ActionResult fail(String message) { return new ActionResult(false, message, Map.of()); }
    static ActionResult ok(String message) { return ok(message, Map.of()); }
    static ActionResult ok(String message, Map<String, ?> data) { return new ActionResult(true, message, data); }
    String toJson() { return value.toString(); }
}
