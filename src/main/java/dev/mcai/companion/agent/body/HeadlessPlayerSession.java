package dev.mcai.companion.agent.body;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
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
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;

/** Owns the in-memory connection required by the visible ServerPlayer body. */
public final class HeadlessPlayerSession implements AutoCloseable {
    private final MinecraftServer server;
    private final Connection connection;
    private final EmbeddedChannel channel;
    private final MinePilotServerPlayer player;
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
        if (!name.matches("[A-Za-z0-9_]{3,16}")) {
            throw new IllegalArgumentException("Invalid Agent player name");
        }
        UUID uuid = UUID.nameUUIDFromBytes(
                ("minepilot:" + worldIdentity + ":" + name).getBytes(StandardCharsets.UTF_8));
        GameProfile profile = new GameProfile(uuid, name);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        MinePilotServerPlayer player = new MinePilotServerPlayer(
                server, level, profile, cookie.clientInformation());
        player.setPos(x, y, z);

        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        EmbeddedChannel channel = new EmbeddedChannel(connection);
        HeadlessPlayerSession result = new HeadlessPlayerSession(
                server, connection, channel, player);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        result.pumpPackets();
        return result;
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
