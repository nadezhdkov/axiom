package io.axiom.yaml.internal.snakeyaml;

import io.axiom.yaml.YamlConfig;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public final class SnakeYamlEngine {

    private final YamlConfig config;

    public SnakeYamlEngine(YamlConfig config) {
        this.config = config;
    }

    public Yaml createYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(config.isPrettyFlowStyle()
                ? DumperOptions.FlowStyle.FLOW
                : DumperOptions.FlowStyle.BLOCK);
        options.setIndent(config.getIndent());
        return new Yaml(options);
    }
}
