package io.axiom.yaml;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlTreeModelTest {

    @Test
    void mappingPreservesInsertionOrder() {
        YamlMapping mapping = new YamlMapping();
        mapping.add("z", new YamlScalar("first"));
        mapping.add("a", new YamlScalar("second"));

        assertEquals(java.util.List.of("z", "a"), mapping.keySet().stream().toList());
    }

    @Test
    void dottedPathResolvesThroughNestedMappings() {
        YamlMapping root = new YamlMapping();
        YamlMapping db = new YamlMapping();
        db.add("host", new YamlScalar("localhost"));
        root.add("db", db);

        assertEquals("localhost", root.getString("db.host"));
    }

    @Test
    void dottedPathReturnsNullWhenIntermediateSegmentMissing() {
        YamlMapping root = new YamlMapping();
        assertNull(root.getPath("db.host"));
    }

    @Test
    void deepCopyOfMappingIsIndependentOfOriginal() {
        YamlMapping original = new YamlMapping();
        original.add("k", new YamlScalar("v"));

        YamlMapping copy = original.deepCopy();
        copy.add("k", new YamlScalar("changed"));

        assertEquals("v", original.get("k").asScalar().asString());
        assertEquals("changed", copy.get("k").asScalar().asString());
        assertNotSame(original.get("k"), copy.get("k"));
    }

    @Test
    void numericScalarsCompareByValueAcrossNumberTypes() {
        assertEquals(new YamlScalar(1), new YamlScalar(1.0));
    }

    @Test
    void yamlNullIsASingleton() {
        assertSame(YamlNull.INSTANCE, YamlNull.INSTANCE.deepCopy());
    }

    @Test
    void asMappingThrowsWhenNodeIsAScalar() {
        YamlNode scalar = new YamlScalar("x");
        assertThrows(IllegalStateException.class, scalar::asMapping);
    }

    @Test
    void sequenceSupportsMixedElementTypes() {
        YamlSequence sequence = new YamlSequence();
        sequence.add(new YamlScalar("text"));
        sequence.add(new YamlScalar(42));
        sequence.add(new YamlScalar(true));

        assertEquals(3, sequence.size());
        assertTrue(sequence.get(2).asScalar().asBoolean());
    }
}
