package io.axiom.json.examples;

import io.axiom.json.Json;
import io.axiom.json.JsonElement;
import io.axiom.json.JsonMapper;
import io.axiom.json.annotations.JsonDefault;
import io.axiom.json.annotations.JsonName;
import io.axiom.json.annotations.JsonRequired;

/** Minimal, compiled-by-CI usage examples for {@code axiom-json}. */
public final class JsonExamples {

    private JsonExamples() {
    }

    static class ServerConfig {
        @JsonName("host_name")
        String host;

        @JsonDefault("8080")
        int port;

        @JsonRequired
        String apiKey;
    }

    public static void main(String[] args) {
        JsonMapper mapper = Json.defaultMapper();

        ServerConfig config = new ServerConfig();
        config.host = "localhost";
        config.apiKey = "secret";

        String json = mapper.toJson(config);
        System.out.println("encoded: " + json);

        ServerConfig decoded = mapper.decode(io.axiom.json.io.JsonSource.of(json), ServerConfig.class);
        System.out.println("port (applied default? no, was present): " + decoded.port);

        JsonElement tree = mapper.parse(io.axiom.json.io.JsonSource.of("{\"a\": [1, 2, 3]}"));
        System.out.println("first element: " + tree.asJsonObject().getAsJsonArray("a").get(0));
    }
}
