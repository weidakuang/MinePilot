package dev.mcai.companion.agent.knowledge;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.world.phys.Vec3;

class SoundPerceptionTest {
    @Test void nativeCaptionsAndResourceRanges() {
        var catalog = new VanillaSoundCatalog();
        assertEquals("僵尸：低吼", catalog.resolve("minecraft:entity.zombie.ambient", 1, 42).text());
        assertEquals(16, catalog.resolve("minecraft:entity.zombie.ambient", 1, 42).range());
        assertEquals(12, catalog.resolve("minecraft:block.chest.open", 1, 42).range());
        assertEquals(24, catalog.resolve("minecraft:block.chest.open", 2, 42).range());
        assertEquals(12, catalog.resolve("minecraft:block.chest.open", 0, 42).range());
        assertEquals(28.8, catalog.resolve("minecraft:block.wooden_door.open", 2, 42).range(), .001);
        assertEquals(16, catalog.resolve("minecraft:block.note_block.imitate.zombie", 1, 42).range());
        assertEquals("箱子", catalog.label("block.minecraft.chest", "bad fallback"));
        assertNull(catalog.resolve("minecraft:music.game", 1, 42));
        assertNull(catalog.resolve("minecraft:missing_sound", 1, 42));
        assertNull(catalog.resolve("another_mod:entity.zombie.ambient", 1, 42));
    }
    @Test void packetQuantizationUsesVanillaFloatPrecisionAtLargeCoordinates() {
        var position=new Vec3(29_999_999.5,-59.51,-29_999_999.5);
        var encoded=SoundPerception.packetPosition(position);
        assertEquals(30_000_000,encoded.x);
        assertEquals(-59.5,encoded.y);
        assertEquals(-30_000_000,encoded.z);
    }
    @Test void eightDirectionsRotateWithBodyAndWrap() {
        for (int i=0; i<8; i++) {
            double angle=Math.toRadians(i*45);
            var sound=new Vec3(Math.sin(angle)*5, 7, -Math.cos(angle)*5);
            assertEquals(i, SoundPerception.directionIndex(0, Vec3.ZERO, sound));
            assertEquals((i+6)%8, SoundPerception.directionIndex(90, Vec3.ZERO, sound));
            assertEquals(i, SoundPerception.directionIndex(360, Vec3.ZERO, sound));
        }
        assertEquals(-1, SoundPerception.directionIndex(0, Vec3.ZERO, new Vec3(0,5,0)));
        assertEquals(0, SoundPerception.directionIndex(359, Vec3.ZERO, new Vec3(0,0,-5)));
    }
    @Test void subtitleBoundaryUsesStrictThreeDimensionalEarDistance() {
        assertTrue(SoundPerception.withinSubtitleRange(Vec3.ZERO, new Vec3(11.99,0,0),12));
        assertFalse(SoundPerception.withinSubtitleRange(Vec3.ZERO, new Vec3(12,0,0),12));
        assertFalse(SoundPerception.withinSubtitleRange(Vec3.ZERO, new Vec3(11,5,0),12));
        assertTrue(SoundPerception.withinSubtitleRange(Vec3.ZERO, new Vec3(11,5,0),16));
    }
}
