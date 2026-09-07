package dev.mcai.companion.gametest;

import java.util.Map;

import dev.mcai.companion.MinecraftAiCompanion;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.gametest.ForgeGameTestHooks;
import net.minecraftforge.registries.RegisterEvent;

/** Registers the data-driven backend black-box tests only on a GameTest run. */
public final class GameTestRegistrar {
    private static Map<Identifier, ForgeGameTestHooks.TestReference> tests = Map.of();

    private GameTestRegistrar() {
    }

    public static void register(FMLJavaModLoadingContext context) {
        if (!ForgeGameTestHooks.isGametestEnabled()) {
            return;
        }
        var gathered=new java.util.HashMap<>(ForgeGameTestHooks.gatherTests(NavigationBackendGameTests.class,null));
        gathered.putAll(ForgeGameTestHooks.gatherTests(dev.mcai.companion.codex.KnowledgeGameTests.class,null));
        gathered.putAll(ForgeGameTestHooks.gatherTests(dev.mcai.companion.codex.SoundGameTests.class,null));
        gathered.putAll(ForgeGameTestHooks.gatherTests(dev.mcai.companion.codex.FollowGameTests.class,null));
        tests = Map.copyOf(gathered);
        if (tests.isEmpty()) {
            throw new IllegalStateException("Navigation backend GameTest was not discovered");
        }
        RegisterEvent.getBus(context.getModBusGroup())
                .addListener(GameTestRegistrar::registerFunctions);
    }

    private static void registerFunctions(RegisterEvent event) {
        if (!Registries.TEST_FUNCTION.equals(event.getRegistryKey())) {
            return;
        }
        tests.forEach((id, reference) -> event.register(
                Registries.TEST_FUNCTION, id, reference::consumer));
        MinecraftAiCompanion.LOGGER.info(
                "Registered {} MinePilot backend GameTest function(s)", tests.size());
    }
}
