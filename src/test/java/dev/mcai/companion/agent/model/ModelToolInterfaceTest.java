package dev.mcai.companion.agent.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import dev.mcai.companion.agent.knowledge.KnowledgeTools;

/** Loopback protocol tests with synthetic replies, never provider or gameplay acceptance. */
final class ModelToolInterfaceTest {
    @Test void sharedKnowledgeSchemasAndNavigationArgumentsSurviveTheProviderWire() throws Exception {
        var captured=new ArrayBlockingQueue<JsonObject>(1);
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v1/chat/completions",exchange->{
            captured.add(JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject());
            byte[] response="{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":null,\"tool_calls\":[{\"id\":\"test-call\",\"type\":\"function\",\"function\":{\"name\":\"sense\",\"arguments\":\"{\\\"kind\\\":\\\"entities\\\",\\\"limit\\\":4}\"}}]}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,response.length);exchange.getResponseBody().write(response);exchange.close();
        });server.start();
        try(var client=new OpenAiCompatibleChatClient(new ModelConfig(URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/v1"),"synthetic-test-credential","synthetic-protocol-test",.2))){
            var names=new ArrayList<>(KnowledgeTools.NAMES);names.addAll(dev.mcai.companion.agent.mining.MiningTools.NAMES);names.addAll(dev.mcai.companion.agent.mining.CollectionTools.NAMES);names.addAll(dev.mcai.companion.agent.placement.PlacementTools.NAMES);names.add("request_navigation");names.add("say");
            var tools=AgentToolSchemas.navigationTools(false,names.toArray(String[]::new));
            var result=client.complete(List.of(),tools).get(5,TimeUnit.SECONDS);
            assertEquals("sense",result.toolCalls().getFirst().name());assertEquals(4,result.toolCalls().getFirst().arguments().get("limit").getAsInt());
            var request=captured.poll(1,TimeUnit.SECONDS);assertNotNull(request);assertFalse(request.get("parallel_tool_calls").getAsBoolean());
            var functions=new HashMap<String,JsonObject>();
            for(var tool:request.getAsJsonArray("tools")){var function=tool.getAsJsonObject().getAsJsonObject("function");functions.put(function.get("name").getAsString(),function);}
            for(var definition:KnowledgeTools.definitions()){var d=definition.getAsJsonObject();assertEquals(d.get("inputSchema"),functions.get(d.get("name").getAsString()).get("parameters"));}
            var radius=functions.get("sense").getAsJsonObject("parameters").getAsJsonObject("properties").getAsJsonObject("radius");
            assertEquals(1,radius.get("minimum").getAsInt());assertEquals(96,radius.get("maximum").getAsInt());
            for(var definition:dev.mcai.companion.agent.mining.MiningTools.definitions()){var d=definition.getAsJsonObject();assertEquals(d.get("inputSchema"),functions.get(d.get("name").getAsString()).get("parameters"));}
            for(var definition:dev.mcai.companion.agent.mining.CollectionTools.definitions()){var d=definition.getAsJsonObject();assertEquals(d.get("inputSchema"),functions.get(d.get("name").getAsString()).get("parameters"));}
            for(var definition:dev.mcai.companion.agent.placement.PlacementTools.definitions()){var d=definition.getAsJsonObject();assertEquals(d.get("inputSchema"),functions.get(d.get("name").getAsString()).get("parameters"));}
            var nav=functions.get("request_navigation").getAsJsonObject("parameters").getAsJsonObject("properties");
            for(String key:List.of("continuous_follow","replace_request_id","forward_blocks","allow_partial"))assertTrue(nav.has(key));
            var targets=nav.getAsJsonObject("target_kind").getAsJsonArray("enum");assertTrue(targets.contains(new JsonPrimitive("waypoint")));assertTrue(targets.contains(new JsonPrimitive("dropped_item")));
        }finally{server.stop(0);}
    }
}
