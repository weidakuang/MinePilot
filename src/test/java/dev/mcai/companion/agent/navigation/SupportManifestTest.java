package dev.mcai.companion.agent.navigation;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SupportManifestTest {
    @Test void exactQuantitiesPreferLeastImportantAndDoNotHideMaterialShortage() {
        var manifest=AnytimeNavigationPlanner.supportManifest(List.of(
                new RouteOption.SupportMaterial("stone","minecraft:cobblestone",3,3),
                new RouteOption.SupportMaterial("dirt","minecraft:dirt",2,5)),4);
        assertEquals(List.of(new RouteOption.SupportMaterial("dirt","minecraft:dirt",2,5),
                new RouteOption.SupportMaterial("stone","minecraft:cobblestone",2,3)),manifest);
        assertThrows(IllegalStateException.class,()->AnytimeNavigationPlanner.supportManifest(manifest,5));
        assertTrue(AnytimeNavigationPlanner.supportManifest(manifest,0).isEmpty());
        assertThrows(IllegalArgumentException.class,()->new RouteOption.SupportMaterial("valuable","minecraft:diamond_block",1,2));
    }
}
