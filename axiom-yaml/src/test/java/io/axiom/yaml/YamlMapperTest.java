package io.axiom.yaml;

import io.axiom.yaml.annotations.YamlDefault;
import io.axiom.yaml.annotations.YamlIgnore;
import io.axiom.yaml.annotations.YamlName;
import io.axiom.yaml.annotations.YamlRequired;
import io.axiom.yaml.error.YamlValidationException;
import io.axiom.yaml.io.YamlSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlMapperTest {

    static class Person {
        @YamlName("full_name")
        String name;

        int age;

        @YamlIgnore
        String secret = "untouched";
    }

    static class RequiredConfig {
        @YamlRequired
        String apiKey;
    }

    static class DefaultedConfig {
        @YamlDefault("8080")
        int port;
    }

    @Test
    void roundTripsAJavaObjectThroughEncodeAndDecode() {
        YamlMapper mapper = Yaml.defaultMapper();
        Person original = new Person();
        original.name = "Ada";
        original.age = 30;

        String yaml = mapper.toYaml(original);
        Person decoded = mapper.decode(YamlSource.of(yaml), Person.class);

        assertEquals("Ada", decoded.name);
        assertEquals(30, decoded.age);
    }

    @Test
    void yamlNameOverridesTheSerializedKeyName() {
        YamlMapper mapper = Yaml.defaultMapper();
        Person original = new Person();
        original.name = "Grace";
        original.age = 40;

        YamlNode encoded = mapper.encode(original);
        assertTrue(encoded.asMapping().has("full_name"));
        assertFalse(encoded.asMapping().has("name"));
    }

    @Test
    void yamlIgnoreExcludesFieldFromEncoding() {
        YamlMapper mapper = Yaml.defaultMapper();
        Person original = new Person();
        original.name = "Linus";
        original.age = 20;

        YamlNode encoded = mapper.encode(original);
        assertFalse(encoded.asMapping().has("secret"));
    }

    @Test
    void yamlRequiredThrowsWhenKeyMissing() {
        YamlMapper mapper = Yaml.defaultMapper();
        assertThrows(YamlValidationException.class,
                () -> mapper.decode(YamlSource.of("{}"), RequiredConfig.class));
    }

    @Test
    void yamlDefaultAppliesWhenKeyMissing() {
        YamlMapper mapper = Yaml.defaultMapper();
        DefaultedConfig decoded = mapper.decode(YamlSource.of("{}"), DefaultedConfig.class);
        assertEquals(8080, decoded.port);
    }

    @Test
    void yamlDefaultDoesNotOverrideAnExplicitValue() {
        YamlMapper mapper = Yaml.defaultMapper();
        DefaultedConfig decoded = mapper.decode(YamlSource.of("port: 9090"), DefaultedConfig.class);
        assertEquals(9090, decoded.port);
    }

    @Test
    void decodesAGenericListOfStrings() {
        YamlMapper mapper = Yaml.defaultMapper();
        List<String> decoded = mapper.decode(YamlSource.of("- a\n- b\n- c"),
                io.axiom.core.type.TypeReference.listOf(String.class));

        assertEquals(List.of("a", "b", "c"), decoded);
    }

    @Test
    void parseThenGetPathReadsANestedDottedKey() {
        YamlNode tree = Yaml.defaultMapper().parse(YamlSource.of("db:\n  host: localhost\n  port: 5432"));
        assertEquals("localhost", tree.asMapping().getString("db.host"));
        assertEquals(5432, tree.asMapping().getInt("db.port"));
    }
}
