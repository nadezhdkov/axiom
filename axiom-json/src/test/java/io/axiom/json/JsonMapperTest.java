package io.axiom.json;

import io.axiom.json.annotations.JsonDefault;
import io.axiom.json.annotations.JsonIgnore;
import io.axiom.json.annotations.JsonName;
import io.axiom.json.annotations.JsonRequired;
import io.axiom.json.error.JsonValidationException;
import io.axiom.json.io.JsonSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonMapperTest {

    static class Person {
        @JsonName("full_name")
        String name;

        int age;

        @JsonIgnore
        String secret = "untouched";
    }

    static class RequiredConfig {
        @JsonRequired
        String apiKey;
    }

    static class DefaultedConfig {
        @JsonDefault("8080")
        int port;
    }

    @Test
    void roundTripsAJavaObjectThroughEncodeAndDecode() {
        JsonMapper mapper = Json.defaultMapper();
        Person original = new Person();
        original.name = "Ada";
        original.age = 30;

        String json = mapper.toJson(original);
        Person decoded = mapper.decode(JsonSource.of(json), Person.class);

        assertEquals("Ada", decoded.name);
        assertEquals(30, decoded.age);
    }

    @Test
    void jsonNameOverridesTheSerializedPropertyName() {
        JsonMapper mapper = Json.defaultMapper();
        Person original = new Person();
        original.name = "Grace";
        original.age = 40;

        JsonElement encoded = mapper.encode(original);
        assertTrue(encoded.asJsonObject().has("full_name"));
        assertFalse(encoded.asJsonObject().has("name"));
    }

    @Test
    void jsonIgnoreExcludesFieldFromEncoding() {
        JsonMapper mapper = Json.defaultMapper();
        Person original = new Person();
        original.name = "Linus";
        original.age = 20;

        JsonElement encoded = mapper.encode(original);
        assertFalse(encoded.asJsonObject().has("secret"));
    }

    @Test
    void jsonRequiredThrowsWhenFieldMissing() {
        JsonMapper mapper = Json.defaultMapper();
        assertThrows(JsonValidationException.class,
                () -> mapper.decode(JsonSource.of("{}"), RequiredConfig.class));
    }

    @Test
    void jsonRequiredThrowsWhenFieldIsNull() {
        JsonMapper mapper = Json.defaultMapper();
        assertThrows(JsonValidationException.class,
                () -> mapper.decode(JsonSource.of("{\"apiKey\": null}"), RequiredConfig.class));
    }

    @Test
    void jsonDefaultAppliesWhenFieldMissing() {
        JsonMapper mapper = Json.defaultMapper();
        DefaultedConfig decoded = mapper.decode(JsonSource.of("{}"), DefaultedConfig.class);
        assertEquals(8080, decoded.port);
    }

    @Test
    void jsonDefaultDoesNotOverrideAnExplicitValue() {
        JsonMapper mapper = Json.defaultMapper();
        DefaultedConfig decoded = mapper.decode(JsonSource.of("{\"port\": 9090}"), DefaultedConfig.class);
        assertEquals(9090, decoded.port);
    }

    @Test
    void stringifyProducesCompactOutputByDefault() {
        JsonObject object = new JsonObject();
        object.addProperty("a", 1);

        assertEquals("{\"a\":1}", Json.defaultMapper().stringify(object));
    }

    @Test
    void stringifyProducesIndentedOutputWhenPrettyPrintEnabled() {
        JsonMapper mapper = Json.configure().prettyPrint(true).buildMapper();
        JsonObject object = new JsonObject();
        object.addProperty("a", 1);

        assertEquals("{\n  \"a\": 1\n}", mapper.stringify(object));
    }
}
