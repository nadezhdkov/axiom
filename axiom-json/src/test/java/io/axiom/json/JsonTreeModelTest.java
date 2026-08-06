package io.axiom.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonTreeModelTest {

    @Test
    void jsonObjectPreservesInsertionOrder() {
        JsonObject object = new JsonObject();
        object.addProperty("z", "first");
        object.addProperty("a", "second");

        assertEquals(java.util.List.of("z", "a"), object.keySet().stream().toList());
    }

    @Test
    void jsonObjectNullValueBecomesJsonNull() {
        JsonObject object = new JsonObject();
        object.add("key", null);

        assertTrue(object.get("key").isJsonNull());
    }

    @Test
    void jsonArraySupportsMixedElementTypes() {
        JsonArray array = new JsonArray();
        array.add("text");
        array.add(42);
        array.add(true);

        assertEquals(3, array.size());
        assertEquals("text", array.get(0).asJsonPrimitive().asString());
        assertEquals(42, array.get(1).asJsonPrimitive().asInt());
        assertTrue(array.get(2).asJsonPrimitive().getAsBoolean());
    }

    @Test
    void asJsonObjectThrowsWhenElementIsNotAnObject() {
        JsonElement primitive = new JsonPrimitive("x");
        assertThrows(IllegalStateException.class, primitive::asJsonObject);
    }

    @Test
    void deepCopyOfObjectIsIndependentOfOriginal() {
        JsonObject original = new JsonObject();
        original.addProperty("k", "v");

        JsonObject copy = original.deepCopy();
        copy.addProperty("k", "changed");

        assertEquals("v", original.get("k").asJsonPrimitive().asString());
        assertEquals("changed", copy.get("k").asJsonPrimitive().asString());
        assertNotSame(original.get("k"), copy.get("k"));
    }

    @Test
    void numericPrimitivesCompareByValueAcrossNumberTypes() {
        assertEquals(new JsonPrimitive(1), new JsonPrimitive(1.0));
    }

    @Test
    void jsonNullIsASingleton() {
        assertSame(JsonNull.INSTANCE, JsonNull.INSTANCE.deepCopy());
    }

    @Test
    void emptyJsonObjectAndArrayReportEmpty() {
        assertTrue(new JsonObject().isEmpty());
        assertTrue(new JsonArray().isEmpty());
    }
}
