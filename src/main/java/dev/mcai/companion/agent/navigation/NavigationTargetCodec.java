package dev.mcai.companion.agent.navigation;
import com.google.gson.*;
import java.util.*;
import dev.mcai.companion.agent.AgentRuntime;
import net.minecraft.server.level.ServerPlayer;
/** Identical target semantics for the internal model and public bridge. */
public final class NavigationTargetCodec {
    public static NavigationTarget decode(AgentRuntime runtime,JsonObject arguments) {
        if(requiredString(arguments,"target_kind").equals("waypoint")) {
            var point=runtime.player().inventoryLedger.remembered(requiredString(arguments,"target_name"),optionalString(arguments,"dimension",runtime.player().level().dimension().identifier().toString()));
            return new NavigationTarget.Coordinates(point.get("dimension").getAsString(),point.get("x").getAsDouble(),point.get("y").getAsDouble(),point.get("z").getAsDouble(),optionalDouble(arguments,"acceptance_radius").orElse(2),optionalDouble(arguments,"arrival_heading"));
        }
        NavigationTarget.Kind kind = NavigationTarget.Kind.valueOf(
                requiredString(arguments, "target_kind").toUpperCase(Locale.ROOT));
        OptionalDouble heading = optionalDouble(arguments, "arrival_heading");
        double acceptance = optionalDouble(arguments, "acceptance_radius").orElse(kind==NavigationTarget.Kind.DROPPED_ITEM?.5:2.0);
        if (acceptance < 0.5 || acceptance > 32.0) {
            throw new IllegalArgumentException("acceptance_radius must be in [0.5, 32]");
        }
        ServerPlayer player = runtime.player();
        OptionalDouble relative = optionalDouble(arguments, "forward_blocks");
        if (relative.isPresent()) {
            double blocks = relative.getAsDouble();
            if (kind != NavigationTarget.Kind.COORDINATES || Math.abs(blocks) > 8 || Math.abs(blocks) < .5
                    || arguments.has("x") || arguments.has("y") || arguments.has("z"))
                throw new IllegalArgumentException("Relative movement requires coordinates kind, magnitude [0.5,8], and no x/y/z");
            double yaw = Math.toRadians(player.getYRot());
            return new NavigationTarget.Coordinates(player.level().dimension().identifier().toString(),
                    player.getX() - Math.sin(yaw) * blocks, player.getY(), player.getZ() + Math.cos(yaw) * blocks, .5, heading);
        }
        return switch (kind) {
            case COORDINATES -> new NavigationTarget.Coordinates(
                    optionalString(arguments, "dimension",
                            player.level().dimension().identifier().toString()),
                    requiredDouble(arguments, "x"),
                    requiredDouble(arguments, "y"),
                    requiredDouble(arguments, "z"),
                    acceptance,
                    heading
            );
            case PLAYER, ENTITY, DROPPED_ITEM -> new NavigationTarget.Named(
                    kind,
                    requiredString(arguments, "target_name"),
                    acceptance,
                    heading
            );
            case WORLD_SPAWN, RESPAWN_POINT, DEATH_POINT ->
                    new NavigationTarget.Landmark(kind, acceptance, heading);
        };
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
}
