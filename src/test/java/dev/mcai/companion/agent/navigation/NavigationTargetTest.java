package dev.mcai.companion.agent.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.OptionalDouble;

import org.junit.jupiter.api.Test;

final class NavigationTargetTest {
    @Test
    void playerRendezvousUsesThreeBlocksWhileFollowRetainsItsRadius() {
        var target=new NavigationTarget.Named(NavigationTarget.Kind.PLAYER,"Human",.5,OptionalDouble.empty());
        var once=new NavigationIntent(java.util.UUID.randomUUID(),0,target,TravelPace.AUTO,"come here","Human");
        var follow=new NavigationIntent(java.util.UUID.randomUUID(),0,target,TravelPace.AUTO,"follow","Human",false,true);
        assertEquals(3,((NavigationTarget.Named)once.target()).acceptanceRadius());
        assertEquals(.5,((NavigationTarget.Named)follow.target()).acceptanceRadius());
    }

    @Test
    void coordinateTargetPreservesRequestedAcceptanceRadius() {
        NavigationTarget.Coordinates target = new NavigationTarget.Coordinates(
                "minecraft:overworld", 1.0, 64.0, -2.0, 3.25,
                OptionalDouble.empty());

        assertEquals(3.25, target.acceptanceRadius());
    }

    @Test
    void coordinateTargetRejectsOutOfRangeAcceptanceRadius() {
        assertThrows(IllegalArgumentException.class,
                () -> new NavigationTarget.Coordinates(
                        "minecraft:overworld", 1.0, 64.0, -2.0, 0.25,
                        OptionalDouble.empty()));
    }
}
