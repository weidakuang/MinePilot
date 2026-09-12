package dev.mcai.companion.agent.knowledge;

import com.google.gson.*;
import java.util.*;
import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.agent.body.AgentControlFrame;
import dev.mcai.companion.agent.navigation.NavigationToolCoordinator;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** One bounded physical look-around, sampled at four real body headings without model round trips. */
public final class PerceptionSweep {
    private final AgentRuntime runtime;
    private String id, kind, filter, dimension, pageCursor = "", reason = "";
    private int radius, limit, view, viewTicks, offset, started;
    private float yaw;
    private Vec3 origin;
    private boolean active, coverage, visibleOnly;
    private final LinkedHashMap<String, JsonObject> rows = new LinkedHashMap<>();
    private final JsonArray views = new JsonArray();

    public PerceptionSweep(AgentRuntime runtime) { this.runtime = runtime; }
    public boolean active() { return active; }

    public JsonObject query(String kind, String filter, int radius, int limit, String cursor) {
        return query(kind,filter,radius,limit,cursor,false);
    }
    public JsonObject query(String kind, String filter, int radius, int limit, String cursor, boolean visibleOnly) {
        if (!Set.of("entities", "items", "blocks", "trees").contains(kind)
                || radius < 1 || radius > PerceptionRange.MAX || limit < 1 || limit > 64 || filter.length() > 128)
            throw new IllegalArgumentException("Sweep requires entities/items/blocks/trees and bounded radius/limit");
        if (cursor.isBlank()) {
            var phase = runtime.navigation().status().phase();
            if (runtime.turnActive() || runtime.jumpActive() || runtime.mining().ownsBody()
                    || runtime.collection().ownsBody() || runtime.placement().ownsBody() || runtime.excavation().ownsBody()
                    || !phase.terminal() && phase != NavigationToolCoordinator.Phase.IDLE)
                throw new IllegalStateException("Finish the current body action before a physical look-around");
            id = UUID.randomUUID().toString(); this.kind = kind; this.filter = filter;
            this.radius = radius; this.limit = limit; this.visibleOnly=visibleOnly; yaw = runtime.player().getYRot();
            origin = runtime.player().position(); dimension = runtime.player().level().dimension().identifier().toString();
            started = runtime.server().getTickCount(); view = viewTicks = offset = 0;
            rows.clear(); views.asList().clear(); pageCursor = reason = ""; active = coverage = true;
        } else if (!cursor.equals(id) || !kind.equals(this.kind) || !filter.equals(this.filter)
                || radius != this.radius || limit != this.limit || visibleOnly != this.visibleOnly) {
            throw new IllegalArgumentException("Sweep cursor must retain its original query");
        }
        return status();
    }

    public void cancel() { if (active) finish("Interrupted by a new player request", false); }

    public void tick() {
        if (!active) return;
        var p = runtime.player();
        if (!p.isAlive() || !dimension.equals(p.level().dimension().identifier().toString())
                || origin.distanceToSqr(p.position()) > .25 || runtime.server().getTickCount() - started > 80) {
            finish("View moved, became unavailable or exceeded the bounded scan time", false); return;
        }
        float target = yaw + 90 * view;
        p.applyControlFrame(new AgentControlFrame(target, 0, 0, 0, false, false, false));
        if (Math.abs(Mth.wrapDegrees(p.getYRot() - target)) > 1 || Math.abs(p.getXRot()) > 1) return;
        if (view == 4) { finish("Physical look-around finished", coverage); return; }
        JsonObject page = kind.equals("entities") || kind.equals("items")
                ? runtime.perception.entities(offset, 64, filter, radius, kind.equals("items"), pageCursor)
                : runtime.perception.blocks(radius, filter, kind, pageCursor, 64,visibleOnly);
        pageCursor = page.get("cursor").getAsString();
        if (page.has("nextOffset")) offset = page.get("nextOffset").getAsInt();
        for (var value : page.getAsJsonArray("results")) {
            var row = value.getAsJsonObject();
            String key = row.has("uuid") ? row.get("uuid").getAsString() : row.get("x")+":"+row.get("y")+":"+row.get("z");
            if (rows.size() < limit || rows.containsKey(key)) rows.put(key, row.deepCopy());
            else coverage = false;
        }
        boolean complete = page.get("complete").getAsBoolean()
                && (!page.has("truncated") || !page.get("truncated").getAsBoolean());
        if (complete || ++viewTicks >= 4) {
            var record = new JsonObject(); record.addProperty("yaw", p.getYRot());
            boolean covered = complete && page.has("coverageComplete") && page.get("coverageComplete").getAsBoolean();
            record.addProperty("coverageComplete", covered); views.add(record); coverage &= covered;
            view++; viewTicks = offset = 0; pageCursor = "";
        }
    }

    private void finish(String reason, boolean completeCoverage) {
        active = false; coverage = completeCoverage; this.reason = reason; runtime.player().stopControlling();
    }

    private JsonObject status() {
        var out = new JsonObject(); out.addProperty("cursor", id); out.addProperty("sweep", true);
        out.addProperty("complete", !active); out.addProperty("coverageComplete", !active && coverage && view == 4);
        out.addProperty("phase", active ? "SEARCHING" : "COMPLETED");
        out.addProperty("source", "physical_four_heading_sweep"); out.addProperty("dimension", dimension);
        out.addProperty("reason", reason); out.addProperty("radius", radius); out.addProperty("filter", filter); out.addProperty("visibleOnly",visibleOnly);
        out.addProperty("sampledAtTick", runtime.player().level().getGameTime());
        out.addProperty("elapsedTicks", runtime.server().getTickCount() - started); out.add("views", views.deepCopy());
        var results = new JsonArray(); if (!active) rows.values().forEach(results::add); out.add("results", results);
        out.addProperty("coverageMeaning", "Four physical horizontal views with normal proximity and occlusion rules. Per-view work and output are capped; incomplete coverage never proves absence. Occluded or unloaded space remains unknown.");
        return out;
    }
}
