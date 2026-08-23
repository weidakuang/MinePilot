package dev.mcai.companion.vision;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

final class VisionCaptureServerBridge {
    private static final Map<MinecraftServer, VisionCaptureService> SERVICES =
            new ConcurrentHashMap<>();

    private VisionCaptureServerBridge() {
    }

    static void register(
            final MinecraftServer server,
            final VisionCaptureService service
    ) {
        if (SERVICES.putIfAbsent(
                Objects.requireNonNull(server, "server"),
                Objects.requireNonNull(service, "service")
        ) != null) {
            throw new IllegalStateException(
                    "Vision capture service is already registered"
            );
        }
    }

    static void unregister(
            final MinecraftServer server,
            final VisionCaptureService service
    ) {
        SERVICES.remove(server, service);
    }

    static void accept(
            final ServerPlayer sender,
            final ServerboundVisionCaptureResult result
    ) {
        final VisionCaptureService service = SERVICES.get(
                sender.level().getServer()
        );
        if (service != null) {
            service.accept(sender, result);
        }
    }

    static void registerRenderer(
            final ServerPlayer sender,
            final boolean available
    ) {
        final VisionCaptureService service = SERVICES.get(
                sender.level().getServer()
        );
        if (service != null) {
            service.registerRenderer(sender, available);
        }
    }
}
