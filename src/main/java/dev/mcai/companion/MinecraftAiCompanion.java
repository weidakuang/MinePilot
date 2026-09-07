package dev.mcai.companion;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import dev.mcai.companion.agent.AgentRuntime;
import dev.mcai.companion.codex.CodexMcpServer;
import dev.mcai.companion.gametest.GameTestRegistrar;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Forge entry point for the incremental MinePilot Agent rebuild.
 */
@Mod(MinecraftAiCompanion.MOD_ID)
public final class MinecraftAiCompanion {
    public static final String MOD_ID = "mcai_companion";
    public static final Logger LOGGER = LogUtils.getLogger();
    private AgentRuntime runtime;
    private CodexMcpServer codexMcp;

    public MinecraftAiCompanion(FMLJavaModLoadingContext context) {
        GameTestRegistrar.register(context);
        ServerStartedEvent.BUS.addListener(this::onServerStarted);
        ServerStoppingEvent.BUS.addListener(this::onServerStopping);
        TickEvent.ServerTickEvent.Post.BUS.addListener(this::onServerTick);
        ServerChatEvent.BUS.addListener(this::onServerChat);
        RegisterCommandsEvent.BUS.addListener(this::registerBackendChat);
        net.minecraftforge.event.entity.player.PlayerEvent.ItemPickupEvent.BUS.addListener(event -> {
            if(event.getEntity() instanceof dev.mcai.companion.agent.body.MinePilotServerPlayer body && body.inventoryLedger != null)
                body.inventoryLedger.pickedUp(event.getOriginalEntity(),event.getStack());
        });
        net.minecraftforge.event.entity.item.ItemTossEvent.BUS.addListener(event -> {
                dev.mcai.companion.agent.knowledge.DropProvenance.mark(event.getEntity(),"player_toss",event.getPlayer()); });
        net.minecraftforge.event.entity.living.LivingDropsEvent.BUS.addListener(event -> {
            for(var item:event.getDrops())dev.mcai.companion.agent.knowledge.DropProvenance.mark(item,"death_drop",event.getSource().getEntity());
        });
        LOGGER.info("MinePilot {} navigation rebuild initialized", BuildInfo.VERSION);
    }

    private void onServerStarted(ServerStartedEvent event) {
        try {
            runtime = AgentRuntime.start(event.getServer());
        } catch (RuntimeException failure) {
            LOGGER.error("MinePilot Agent runtime failed to start", failure);
            return;
        }
        try {
            codexMcp = CodexMcpServer.start(runtime);
        } catch (Exception failure) {
            LOGGER.error(
                    "MinePilot body is online, but its local Codex MCP endpoint failed to start",
                    failure
            );
        }
    }

    private void onServerStopping(ServerStoppingEvent event) {
        if (runtime != null && runtime.server() == event.getServer()) {
            if (codexMcp != null) {
                codexMcp.close();
                codexMcp = null;
            }
            runtime.close();
            runtime = null;
        }
    }

    private void onServerTick(TickEvent.ServerTickEvent.Post event) {
        if (runtime != null && runtime.server() == event.server()) {
            runtime.tick();
        }
    }

    private void registerBackendChat(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("minepilot_chat")
                .requires(source -> source.getEntity() == null)
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(context -> {
                            if (runtime == null) return 0;
                            String text = StringArgumentType.getString(context, "message");
                            if (text.length() > 512) return 0;
                            runtime.server().getPlayerList().broadcastSystemMessage(
                                    Component.literal("[Server] " + text), false);
                            runtime.onChat("Server", text);
                            return 1;
                        })));
    }

    private void onServerChat(ServerChatEvent event) {
        if (runtime != null && runtime.server() == event.getPlayer().level().getServer()) {
            runtime.onPlayerChat(event.getPlayer(), event.getRawText());
        }
    }

}
