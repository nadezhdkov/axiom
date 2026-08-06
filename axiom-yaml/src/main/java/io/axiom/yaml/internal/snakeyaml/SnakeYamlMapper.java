package io.axiom.yaml.internal.snakeyaml;

import io.axiom.yaml.YamlConfig;
import io.axiom.yaml.YamlMapper;
import io.axiom.yaml.YamlNode;
import io.axiom.core.type.TypeReference;
import io.axiom.yaml.error.YamlIoException;
import io.axiom.yaml.error.YamlParseException;
import io.axiom.yaml.io.YamlSource;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.io.Reader;

public final class SnakeYamlMapper implements YamlMapper {

    private final Yaml yaml;
    private final ObjectBinder binder;

    public SnakeYamlMapper(YamlConfig config) {
        this.yaml = new SnakeYamlEngine(config).createYaml();
        this.binder = new ObjectBinder(config.isAnnotationsEnabled());
    }

    @Override
    public YamlNode parse(YamlSource source) {
        try (Reader reader = source.asReader()) {
            return RawBridge.toAxiom(yaml.load(reader));
        } catch (YAMLException e) {
            throw new YamlParseException("Failed to parse YAML from " + source, e);
        } catch (IOException e) {
            throw new YamlIoException("Failed to read YAML from " + source, e);
        }
    }

    @Override
    public <T> T decode(YamlSource source, TypeReference<T> type) {
        return decode(parse(source), type);
    }

    @Override
    public <T> T decode(YamlNode node, TypeReference<T> type) {
        return binder.decode(node, type.getType());
    }

    @Override
    public YamlNode encode(Object value) {
        return binder.encode(value, value == null ? Object.class : value.getClass());
    }

    @Override
    public String stringify(YamlNode node) {
        return yaml.dump(RawBridge.toRaw(node));
    }
}
