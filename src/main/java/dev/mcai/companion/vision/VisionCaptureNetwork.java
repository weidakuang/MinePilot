package dev.mcai.companion.vision;

import dev.mcai.companion.MinecraftAiCompanion;
import dev.mcai.companion.client.vision.ClientVisionCaptureRuntime;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class VisionCaptureNetwork {
    static final int MAX_PNG_BYTES = 2_500_000;
    private static final int MAX_CODE_CHARACTERS = 64;
    private static final int PROTOCOL_VERSION = 1;
    private static final SimpleChannel CHANNEL = createChannel();

    private VisionCaptureNetwork() {
    }

    public static void initialize() {
    }

    public static boolean canSendTo(final ServerPlayer player) {
        return CHANNEL.isRemotePresent(player.connection.getConnection());
    }

    public static void request(
            final ServerPlayer player,
            final ClientboundVisionCaptureRequest request
    ) {
        if (canSendTo(player)) {
            CHANNEL.send(request, PacketDistributor.PLAYER.with(player));
        }
    }

    public static void reply(
            final ServerboundVisionCaptureResult result
    ) {
        CHANNEL.send(result, PacketDistributor.SERVER.noArg());
    }

    public static void registerRenderer(final boolean available) {
        CHANNEL.send(
                new ServerboundVisionRendererRegistration(available),
                PacketDistributor.SERVER.noArg()
        );
    }

    private static SimpleChannel createChannel() {
        final SimpleChannel channel = ChannelBuilder.named(
                Identifier.fromNamespaceAndPath(
                        MinecraftAiCompanion.MOD_ID,
                        "vision_capture"
                )
        ).networkProtocolVersion(PROTOCOL_VERSION).simpleChannel();
        channel.messageBuilder(
                ClientboundVisionCaptureRequest.class,
                0,
                NetworkDirection.PLAY_TO_CLIENT
        ).encoder((message, buffer) -> {
            buffer.writeVarLong(message.requestId());
            buffer.writeUUID(message.companionId());
        }).decoder(buffer -> new ClientboundVisionCaptureRequest(
                buffer.readVarLong(),
                buffer.readUUID()
        )).consumerMainThread((message, context) -> {
            if (context.isClientSide()) {
                ClientVisionCaptureRuntime.request(message);
            }
        }).add();
        channel.messageBuilder(
                ServerboundVisionCaptureResult.class,
                1,
                NetworkDirection.PLAY_TO_SERVER
        ).encoder(VisionCaptureNetwork::encodeResult)
        .decoder(VisionCaptureNetwork::decodeResult)
        .consumerMainThread((message, context) -> {
            try {
                if (context.isServerSide()
                        && context.getSender() != null) {
                    VisionCaptureServerBridge.accept(
                            context.getSender(),
                            message
                    );
                }
            } finally {
                message.destroy();
            }
        }).add();
        channel.messageBuilder(
                ServerboundVisionRendererRegistration.class,
                2,
                NetworkDirection.PLAY_TO_SERVER
        ).encoder((message, buffer) ->
                buffer.writeBoolean(message.available())
        ).decoder(buffer -> new ServerboundVisionRendererRegistration(
                buffer.readBoolean()
        )).consumerMainThread((message, context) -> {
            if (context.isServerSide() && context.getSender() != null) {
                VisionCaptureServerBridge.registerRenderer(
                        context.getSender(),
                        message.available()
                );
            }
        }).add();
        return channel.build();
    }

    private static void encodeResult(
            final ServerboundVisionCaptureResult message,
            final FriendlyByteBuf buffer
    ) {
        try {
            buffer.writeVarLong(message.requestId());
            buffer.writeUUID(message.companionId());
            buffer.writeUtf(message.code(), MAX_CODE_CHARACTERS);
            buffer.writeByteArray(message.pngUnsafe());
        } finally {
            message.destroy();
        }
    }

    private static ServerboundVisionCaptureResult decodeResult(
            final FriendlyByteBuf buffer
    ) {
        return new ServerboundVisionCaptureResult(
                buffer.readVarLong(),
                buffer.readUUID(),
                buffer.readUtf(MAX_CODE_CHARACTERS),
                buffer.readByteArray(MAX_PNG_BYTES)
        );
    }
}
