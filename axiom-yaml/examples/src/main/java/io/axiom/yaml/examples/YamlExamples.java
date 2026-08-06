package io.axiom.yaml.examples;

import io.axiom.yaml.Yaml;
import io.axiom.yaml.YamlMapper;
import io.axiom.yaml.YamlNode;
import io.axiom.yaml.annotations.YamlDefault;
import io.axiom.yaml.annotations.YamlName;
import io.axiom.yaml.annotations.YamlRequired;
import io.axiom.yaml.io.YamlSource;

/** Minimal, compiled-by-CI usage examples for {@code axiom-yaml}. */
public final class YamlExamples {

    private YamlExamples() {
    }

    static class ServerConfig {
        @YamlName("host_name")
        String host;

        @YamlDefault("8080")
        int port;

        @YamlRequired
        String apiKey;
    }

    public static void main(String[] args) {
        YamlMapper mapper = Yaml.defaultMapper();

        ServerConfig config = new ServerConfig();
        config.host = "localhost";
        config.apiKey = "secret";

        String yaml = mapper.toYaml(config);
        System.out.println("encoded:\n" + yaml);

        ServerConfig decoded = mapper.decode(YamlSource.of(yaml), ServerConfig.class);
        System.out.println("decoded host: " + decoded.host);

        YamlNode tree = mapper.parse(YamlSource.of("db:\n  host: localhost\n  port: 5432"));
        System.out.println("dotted path: " + tree.asMapping().getString("db.host"));
    }
}
