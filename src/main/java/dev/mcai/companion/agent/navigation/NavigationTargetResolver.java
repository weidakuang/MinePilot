package dev.mcai.companion.agent.navigation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

import dev.mcai.companion.agent.body.MinePilotServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;

/** Resolves typed model targets exclusively from authoritative server state. */
public final class NavigationTargetResolver {

    public NavigationPlan.ResolvedDestination resolve(
            MinecraftServer server,
            MinePilotServerPlayer agent,
            NavigationTarget target
    ) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(agent, "agent");
        Objects.requireNonNull(target, "target");
        if (!server.isSameThread()) {
            throw new IllegalStateException("Navigation targets must be resolved on the server thread");
        }
        return switch (target) {
            case NavigationTarget.Coordinates coordinates -> coordinates(coordinates);
            case NavigationTarget.Named named -> named(server, agent, named);
            case NavigationTarget.Landmark landmark -> landmark(server, agent, landmark);
        };
    }

    private static NavigationPlan.ResolvedDestination coordinates(
            NavigationTarget.Coordinates coordinates
    ) {
        return new NavigationPlan.ResolvedDestination(
                coordinates.dimension(),
                coordinates.x(),
                coordinates.y(),
                coordinates.z(),
                coordinates.acceptanceRadius(),
                false,
                "coordinates",
                coordinates.arrivalHeading(),
                Optional.empty()
        );
    }

    private static NavigationPlan.ResolvedDestination named(
            MinecraftServer server,
            MinePilotServerPlayer agent,
            NavigationTarget.Named named
    ) {
        Entity target = switch (named.kind()) {
            case PLAYER -> findPlayer(server, agent, named.name());
            case ENTITY -> findEntity(server, agent, named.name());
            case DROPPED_ITEM -> {
                try {
                    var runtime=dev.mcai.companion.agent.AgentRuntime.active(server);
                    Entity item=runtime.perception.target(named.name());
                    if(!(item instanceof net.minecraft.world.entity.item.ItemEntity))throw new IllegalArgumentException("Target is not a dropped stack");
                    yield item;
                } catch(IllegalArgumentException failure){throw new UnresolvedTargetException("DROPPED_ITEM_UNAVAILABLE: The stack disappeared, merged, was picked up or left observable scope; decide whether to continue. "+failure.getMessage());}
            }
            default -> throw new IllegalArgumentException("Unsupported named target: " + named.kind());
        };
        Vec3 velocity = target.getDeltaMovement();
        return new NavigationPlan.ResolvedDestination(
                dimensionId(target.level()),
                target.getX(),
                target.getY(),
                target.getZ(),
                named.acceptanceRadius(),
                true,
                target.getUUID().toString(),
                named.arrivalHeading(),
                Optional.of(new NavigationPlan.TargetMotion(
                        target.level().getGameTime(),
                        velocity.x,
                        velocity.y,
                        velocity.z
                ))
        );
    }

    private static NavigationPlan.ResolvedDestination landmark(
            MinecraftServer server,
            MinePilotServerPlayer agent,
            NavigationTarget.Landmark landmark
    ) {
        GlobalPos position;
        OptionalDouble heading = landmark.arrivalHeading();
        switch (landmark.kind()) {
            case WORLD_SPAWN -> {
                LevelData.RespawnData spawn = server.overworld().getRespawnData();
                position = spawn.globalPos();
                if (heading.isEmpty()) {
                    heading = OptionalDouble.of(
                            NavigationFollower.minecraftYawToHeading(spawn.yaw()));
                }
            }
            case RESPAWN_POINT -> {
                ServerPlayer.RespawnConfig config = agent.getRespawnConfig();
                if (config == null) {
                    LevelData.RespawnData spawn = server.overworld().getRespawnData();
                    position = spawn.globalPos();
                } else {
                    position = config.respawnData().globalPos();
                }
            }
            case DEATH_POINT -> position = agent.getLastDeathLocation()
                    .orElseThrow(() -> new UnresolvedTargetException(
                            "The Agent has no recorded death point"));
            default -> throw new IllegalArgumentException(
                    "Unsupported landmark target: " + landmark.kind());
        }
        BlockPos block = position.pos();
        return new NavigationPlan.ResolvedDestination(
                position.dimension().identifier().toString(),
                block.getX() + 0.5,
                block.getY(),
                block.getZ() + 0.5,
                landmark.acceptanceRadius(),
                false,
                landmark.kind().name().toLowerCase(Locale.ROOT),
                heading,
                Optional.empty()
        );
    }

    private static ServerPlayer findPlayer(
            MinecraftServer server,
            MinePilotServerPlayer agent,
            String name
    ) {
        List<ServerPlayer> matches = server.getPlayerList().getPlayers().stream()
                .filter(player -> player != agent && player.isAlive())
                .filter(player -> player.getGameProfile().name().equalsIgnoreCase(name)
                        || player.getUUID().toString().equalsIgnoreCase(name))
                .toList();
        if (matches.isEmpty()) {
            throw new UnresolvedTargetException("No online player named " + name);
        }
        if (matches.size() > 1) {
            throw new UnresolvedTargetException("Player name is ambiguous: " + name);
        }
        return matches.getFirst();
    }

    private static Entity findEntity(
            MinecraftServer server,
            MinePilotServerPlayer agent,
            String name
    ) {
        List<Entity> matches = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity != agent && entity.isAlive()
                        && (entity.getUUID().toString().equalsIgnoreCase(name)
                        || entity.getPlainTextName().equalsIgnoreCase(name))) {
                    matches.add(entity);
                }
            }
        }
        return matches.stream()
                .min(Comparator.comparingDouble(entity -> distanceScore(agent, entity)))
                .orElseThrow(() -> new UnresolvedTargetException(
                        "No live entity named " + name));
    }

    private static double distanceScore(MinePilotServerPlayer agent, Entity entity) {
        if (agent.level().dimension() != entity.level().dimension()) {
            return Double.POSITIVE_INFINITY;
        }
        return agent.distanceToSqr(entity);
    }

    private static String dimensionId(net.minecraft.world.level.Level level) {
        return level.dimension().identifier().toString();
    }

    public static final class UnresolvedTargetException extends RuntimeException {
        public UnresolvedTargetException(String message) {
            super(message);
        }
    }
}
