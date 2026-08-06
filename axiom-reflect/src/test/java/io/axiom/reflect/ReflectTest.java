package io.axiom.reflect;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReflectTest {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    @interface Marker {
        String value() default "";
    }

    @Marker("class-level")
    static class Base {
        private static int staticCounter = 0;
        private final String name;

        @Marker("field-level")
        private int age;

        Base(String name) {
            this.name = name;
        }

        private String greet(String greeting) {
            return greeting + ", " + name;
        }

        static int nextId() {
            return ++staticCounter;
        }
    }

    static class Derived extends Base {
        Derived(String name) {
            super(name);
        }
    }

    @Test
    void createInstantiatesViaNoArgConstructor() {
        record Simple() {
        }
        Simple s = Reflect.on(Simple.class).create();
        assertEquals(new Simple(), s);
    }

    @Test
    void createWithArgsUsesMatchingConstructor() {
        Base base = Reflect.on(Base.class).create("Ada");
        assertEquals("Ada", Reflect.on(base).field("name").get());
    }

    static class PrivateNoArgConstructor {
        private PrivateNoArgConstructor() {
        }
    }

    @Test
    void createInstantiatesViaPrivateNoArgConstructor() {
        // Regression guard: create() previously never called setAccessible on the resolved
        // constructor, so any non-public no-arg constructor threw IllegalAccessException.
        PrivateNoArgConstructor instance = Reflect.on(PrivateNoArgConstructor.class).create();
        assertNotNull(instance);
    }

    static class PrivateArgConstructor {
        final String name;

        private PrivateArgConstructor(String name) {
            this.name = name;
        }
    }

    @Test
    void createWithArgsInstantiatesViaPrivateConstructor() {
        PrivateArgConstructor instance = Reflect.on(PrivateArgConstructor.class).create("Ada");
        assertEquals("Ada", instance.name);
    }

    @Test
    void fieldGetSetOnInstance() {
        Base base = new Base("Grace");
        Reflect.on(base).field("age").set(30);
        assertEquals(30, (int) Reflect.on(base).field("age").<Integer>get());
    }

    @Test
    void fieldLookupWalksUpSuperclassHierarchy() {
        Derived derived = new Derived("Linus");
        assertEquals("Linus", Reflect.on(derived).field("name").get());
    }

    @Test
    void fieldNotFoundThrowsReflectException() {
        Base base = new Base("x");
        assertThrows(ReflectException.class, () -> Reflect.on(base).field("doesNotExist"));
    }

    @Test
    void staticFieldAccessibleWithoutInstance() {
        // staticCounter is shared mutable state across test methods (also mutated by
        // methodInvokeStaticWithoutInstance), so only its non-negativity is a safe assertion
        // under JUnit5's unspecified test execution order.
        int value = Reflect.on(Base.class).field("staticCounter").get();
        assertTrue(value >= 0);
    }

    @Test
    void methodInvokeWithInferredArgTypes() {
        Base base = new Base("Ada");
        String result = Reflect.on(base).method("greet").invoke("Hello");
        assertEquals("Hello, Ada", result);
    }

    @Test
    void methodInvokeStaticWithoutInstance() {
        int id = Reflect.on(Base.class).method("nextId").invoke();
        assertTrue(id > 0);
    }

    @Test
    void methodInvokeSafeReturnsNullOnFailure() {
        Base base = new Base("x");
        String result = Reflect.on(base).method("doesNotExist").invokeSafe();
        assertNull(result);
    }

    @Test
    void fieldsFilterByAnnotationAndCollectNames() {
        var names = Reflect.on(new Base("x")).fields().annotated(Marker.class).names();
        assertEquals(java.util.List.of("age"), names);
    }

    @Test
    void fieldsMapReflectsCurrentValues() {
        Base base = new Base("Turing");
        Reflect.on(base).field("age").set(41);
        var map = Reflect.on(base).fields().notStatic().map();
        assertEquals("Turing", map.get("name"));
        assertEquals(41, map.get("age"));
    }

    @Test
    void methodsFilterByNamePrefix() {
        var count = Reflect.on(Base.class).methods().startWith("greet").count();
        assertEquals(1, count);
    }

    @Test
    void classLevelAnnotationInspection() {
        var annotations = Reflect.on(Base.class).annotations();
        assertTrue(annotations.has(Marker.class));
        assertEquals("class-level", annotations.get(Marker.class).value());
    }

    @Test
    void reflectBuilderConstructsAndSetsFields() {
        Base built = ReflectBuilder.of(Base.class)
                .build("placeholder");
        assertEquals("placeholder", Reflect.on(built).field("name").get());
    }

    @Test
    void reflectBuilderFromCapturesCurrentFieldValues() {
        Base base = new Base("Hopper");
        Reflect.on(base).field("age").set(52);

        var builder = ReflectBuilder.from(base);
        assertEquals("Hopper", builder.get("name"));
        assertEquals(52, builder.get("age"));
    }

    @Test
    void fieldLookupIsCachedAcrossMultipleAccesses() {
        Base a = new Base("a");
        Base b = new Base("b");

        Field fieldA = Reflect.on(a).field("name");
        Field fieldB = Reflect.on(b).field("name");

        // Same underlying java.lang.reflect.Field instance served from LookupCache for the same
        // (owner class, name) key — axiom.md §5/§6's explicit gap fix over the reference impl.
        assertSame(fieldA.unwrap(), fieldB.unwrap());
    }

    @Test
    void proxyDelegatesToInvocationHandler() {
        Runnable proxy = Reflect.on(Object.class).proxy(Runnable.class, (p, method, args) -> {
            if (method.getName().equals("run")) return null;
            throw new UnsupportedOperationException();
        });
        proxy.run(); // does not throw
        assertFalse(proxy instanceof Base);
    }
}
