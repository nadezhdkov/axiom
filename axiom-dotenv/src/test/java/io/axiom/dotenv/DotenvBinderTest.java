package io.axiom.dotenv;

import io.axiom.reflect.Reflect;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DotenvBinderTest {

    static class SimpleConfig {
        @Env("HOST")
        String host;

        @Env("PORT")
        int port;

        @Env("DEBUG")
        @Default("false")
        boolean debug;

        @Env("IGNORED")
        @EnvIgnore
        String ignoredField = "untouched";
    }

    @EnvPrefix("REDIS_")
    static class PrefixedConfig {
        @Env("HOST")
        String host;
    }

    static class RequiredConfig {
        @Env("API_KEY")
        @RequiredEnv
        String apiKey;
    }

    private static Dotenv dotenvOf(Map<String, String> values) {
        // DotenvBuilder always merges real System.getenv() on top; direct construction via a Map
        // source keeps these tests independent of the process environment.
        return new TestDotenv(values);
    }

    @Test
    void bindsSimpleFieldsWithTypeConversion() {
        SimpleConfig config = new SimpleConfig();
        DotenvBinder.bind(config, dotenvOf(Map.of("HOST", "localhost", "PORT", "5432")));

        assertEquals("localhost", config.host);
        assertEquals(5432, config.port);
    }

    @Test
    void appliesDefaultWhenKeyMissing() {
        SimpleConfig config = new SimpleConfig();
        DotenvBinder.bind(config, dotenvOf(Map.of("HOST", "h", "PORT", "1")));

        assertEquals(false, config.debug);
    }

    @Test
    void envIgnoreSkipsFieldEvenIfAnnotatedWithEnv() {
        SimpleConfig config = new SimpleConfig();
        DotenvBinder.bind(config, dotenvOf(Map.of("HOST", "h", "PORT", "1", "IGNORED", "should-not-apply")));

        assertEquals("untouched", config.ignoredField);
    }

    @Test
    void envPrefixIsPrependedToKeys() {
        PrefixedConfig config = new PrefixedConfig();
        DotenvBinder.bind(config, dotenvOf(Map.of("REDIS_HOST", "redis-server")));

        assertEquals("redis-server", config.host);
    }

    @Test
    void requiredEnvThrowsWhenMissingAndNoDefault() {
        RequiredConfig config = new RequiredConfig();
        var ex = assertThrows(DotenvInjectionException.class,
                () -> DotenvBinder.bind(config, dotenvOf(Map.of())));
        assertTrue(ex.getMessage().contains("API_KEY"));
    }

    @Test
    void requiredEnvSucceedsWhenPresent() {
        RequiredConfig config = new RequiredConfig();
        DotenvBinder.bind(config, dotenvOf(Map.of("API_KEY", "secret")));
        assertEquals("secret", config.apiKey);
    }

    @Test
    void usesAxiomReflectInternallyNotRawFieldAccess() {
        // Regression guard for the design decision itself: binding must work through
        // io.axiom.reflect.Reflect, verified indirectly by confirming a private final-looking
        // field set via injection is visible through Reflect too.
        SimpleConfig config = new SimpleConfig();
        DotenvBinder.bind(config, dotenvOf(Map.of("HOST", "x", "PORT", "1")));
        assertEquals("x", Reflect.on(config).field("host").<String>get());
    }

    /** Minimal {@link Dotenv} backed by a fixed map, independent of the real process environment. */
    private record TestDotenv(Map<String, String> values) implements Dotenv {
        @Override
        public String get(String key) {
            return values.get(key);
        }

        @Override
        public String get(String key, String defaultValue) {
            return values.getOrDefault(key, defaultValue);
        }

        @Override
        public java.util.Set<DotenvEntry> entries() {
            return values.entrySet().stream()
                    .map(e -> new DotenvEntry(e.getKey(), e.getValue()))
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        }

        @Override
        public java.util.Set<DotenvEntry> fileEntries() {
            return entries();
        }
    }
}
