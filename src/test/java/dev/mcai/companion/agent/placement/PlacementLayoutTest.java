package dev.mcai.companion.agent.placement;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PlacementLayoutTest {
    JsonObject json(String s){return JsonParser.parseString(s).getAsJsonObject();}
    @Test void blueprintKeepsOriginAxesAndAirSeparateFromActions(){
        var layout=PlacementTools.layout(json("""
          {"blueprint":{"origin":{"x":10,"y":64,"z":-3},"palette":{"L":{"item":"minecraft:oak_log","state":{"axis":"x"}}},"layers":[["L_",".."],["..",".L"]]}}
          """));
        assertEquals(2,layout.targets().size());assertEquals(1,layout.air().size());
        var top=layout.targets().getLast();assertEquals(11,top.get("x").getAsInt());assertEquals(65,top.get("y").getAsInt());assertEquals(-2,top.get("z").getAsInt());assertEquals("x",top.getAsJsonObject("state").get("axis").getAsString());
        assertEquals(11,layout.air().getFirst().getX());assertEquals(64,layout.air().getFirst().getY());
    }
    @Test void malformedAndExcessiveRegionsFailBeforeExpansion(){
        for(String region:new String[]{"{\"from\":{\"x\":5,\"y\":0,\"z\":0},\"to\":{\"x\":4,\"y\":0,\"z\":0}}","{\"from\":{\"x\":0,\"y\":0,\"z\":0},\"to\":{\"x\":2147483647,\"y\":0,\"z\":0}}","{\"from\":{\"x\":0.5,\"y\":0,\"z\":0},\"to\":{\"x\":1,\"y\":0,\"z\":0}}"})
            assertThrows(IllegalArgumentException.class,()->PlacementTools.layout(json("{\"region\":"+region+"}")));
    }
    @Test void blueprintRejectsUnknownSymbolsAndRaggedLayers(){
        for(String rows:new String[]{"[[\"LQ\"]]","[[\"LL\",\"L\"]]"})assertThrows(IllegalArgumentException.class,()->PlacementTools.layout(json("{\"blueprint\":{\"origin\":{\"x\":0,\"y\":64,\"z\":0},\"palette\":{\"L\":{\"item\":\"minecraft:oak_log\"}},\"layers\":"+rows+"}}")));
    }
}
