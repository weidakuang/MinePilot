package dev.mcai.companion.agent.body;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.ProblemReporter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/** Owns the in-memory connection required by the visible ServerPlayer body. */
public final class HeadlessPlayerSession implements AutoCloseable {
    private final MinecraftServer server;
    private final Connection connection;
    private final EmbeddedChannel channel;
    private final MinePilotServerPlayer player;
    private net.minecraft.world.phys.Vec3 pendingMotion;
    private boolean loadedAcknowledged;
    private boolean closed;
    public java.util.function.Consumer<String> visibleSystemChat = text -> {};
    public java.util.function.Consumer<net.minecraft.network.protocol.Packet<?>> receivedSound = packet -> {};

    private HeadlessPlayerSession(
            MinecraftServer server,
            Connection connection,
            EmbeddedChannel channel,
            MinePilotServerPlayer player
    ) {
        this.server = server;
        this.connection = connection;
        this.channel = channel;
        this.player = player;
    }

    public static HeadlessPlayerSession join(
            MinecraftServer server,
            ServerLevel level,
            String worldIdentity,
            String name,
            double x,
            double y,
            double z
    ) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(level, "level");
        if (!server.isSameThread()) {
            throw new IllegalStateException("Headless login must run on the server thread");
        }
        if (!name.matches("[A-Za-z0-9_]{3,16}")) {
            throw new IllegalArgumentException("Invalid Agent player name");
        }
        UUID uuid = UUID.nameUUIDFromBytes(
                ("minepilot:" + worldIdentity + ":" + name).getBytes(StandardCharsets.UTF_8));
        GameProfile profile = new GameProfile(uuid, name);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        // In 26.2, PrepareSpawnTask loads player data before placeNewPlayer.
        // Our embedded login must perform that same native restoration boundary.
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(
                dev.mcai.companion.MinecraftAiCompanion.LOGGER)) {
            Optional<ValueInput> saved = server.getPlayerList().loadPlayerData(new NameAndId(profile))
                    .map(tag -> TagValueInput.create(reporter, server.registryAccess(), tag));
            ServerPlayer.SavedPosition position = saved
                    .flatMap(input -> input.read(ServerPlayer.SavedPosition.MAP_CODEC))
                    .orElse(ServerPlayer.SavedPosition.EMPTY);
            ServerLevel spawnLevel = position.dimension().map(server::getLevel).orElse(level);
            Vec3 spawn = position.position().orElse(new Vec3(x, y, z));
            Vec2 rotation = position.rotation().orElse(Vec2.ZERO);
            ChunkPos chunk = ChunkPos.containing(BlockPos.containing(spawn));
            var ready = spawnLevel.getChunkSource().addTicketAndLoadWithRadius(TicketType.PLAYER_SPAWN, chunk, 3);
            server.managedBlock(ready::isDone);
            ready.join();
            spawnLevel.waitForEntities(chunk, 3);
            MinePilotServerPlayer player = new MinePilotServerPlayer(
                    server, spawnLevel, profile, cookie.clientInformation());
            saved.ifPresent(player::load);
            saved.ifPresent(input -> net.minecraftforge.event.ForgeEventFactory.firePlayerLoadingEvent(
                    player, server.getPlayerList().getPlayerIo().getPlayerDataFolder(), uuid.toString()));
            player.snapTo(spawn, rotation.x, rotation.y);
            player.stopControlling();

            Connection connection = new Connection(PacketFlow.SERVERBOUND);
            EmbeddedChannel channel = new EmbeddedChannel(connection);
            HeadlessPlayerSession result = new HeadlessPlayerSession(
                    server, connection, channel, player);
            server.getPlayerList().placeNewPlayer(connection, player, cookie);
            if (saved.isEmpty()) player.setGameMode(GameType.SURVIVAL);
            saved.ifPresent(input -> {
                player.loadAndSpawnEnderPearls(input);
                player.loadAndSpawnParentVehicle(input);
            });
            result.pumpPackets();
            return result;
        }
    }

    public MinePilotServerPlayer player() {
        return player;
    }

    public void tick() {
        if (closed) {
            return;
        }
        // A real client's packet listener calls player.doTick() and then snaps
        // the body back to the last client-reported position. A headless player
        // has no movement packets, so ticking Connection would undo every local
        // control frame. Tick the authoritative player physics directly and
        // pump only outbound channel work instead.
        pumpPackets();
        if (pendingMotion != null) {
            // Vanilla attacks send the victim a velocity packet and restore the
            // old server velocity. Consume that authoritative impulse as a client would.
            player.setDeltaMovement(pendingMotion);
            pendingMotion = null;
        }
        player.doTick();
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();
        channel.flushOutbound();
        pumpPackets();
    }

    private void pumpPackets() {
        if (!loadedAcknowledged && player.connection != null) {
            loadedAcknowledged = true;
            player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        }
        Object outbound;
        while ((outbound = channel.readOutbound()) != null) {
            try {
                if(outbound instanceof net.minecraft.network.protocol.Packet<?> packet)receivedSound.accept(packet);
                if (outbound instanceof ClientboundKeepAlivePacket keepAlive) {
                    player.connection.handleKeepAlive(
                            new ServerboundKeepAlivePacket(keepAlive.getId()));
                } else if (outbound instanceof net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket motion
                        && motion.id() == player.getId()) {
                    pendingMotion = motion.movement();
                } else if (outbound instanceof ClientboundPlayerPositionPacket position) {
                    player.connection.handleAcceptTeleportPacket(
                            new ServerboundAcceptTeleportationPacket(position.id()));
                } else if(outbound instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket chat && !chat.overlay()) {
                    visibleSystemChat.accept(chat.content().getString());
                } else if(outbound instanceof net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket chat) {
                    visibleSystemChat.accept(chat.chatType().decorate(chat.message()).getString());
                }
            } finally {
                ReferenceCountUtil.release(outbound);
            }
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        player.stopControlling();
        if (server.getPlayerList().getPlayer(player.getUUID()) == player) {
            server.getPlayerList().remove(player);
        }
        channel.finishAndReleaseAll();
    }
}
