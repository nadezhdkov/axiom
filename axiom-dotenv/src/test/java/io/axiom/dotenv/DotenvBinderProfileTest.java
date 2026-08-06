package io.axiom.dotenv;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DotenvBinderProfileTest {

    @Profile({"dev", "test"})
    static class DevOnlyConfig {
        @Env("HOST")
        String host;
    }

    static class UnrestrictedConfig {
        @Env("HOST")
        String host;
    }

    @Reloadable
    static class ReloadableConfig {
        @Env("A")
        String a;
    }

    static class NotReloadableConfig {
        @Env("A")
        String a;
    }

    private static Dotenv dotenvOf(Map<String, String> values, String profile) {
        return new TestDotenv(values, profile);
    }

    @Test
    void bindSkipsAProfileRestrictedTargetWhenNoActiveProfile() {
        DevOnlyConfig config = new DevOnlyConfig();
        DotenvBinder.bind(config, dotenvOf(Map.of("HOST", "x"), null));
        assertNull(config.host);
    }

    @Test
    void bindSkipsAProfileRestrictedTargetWhenActiveProfileDoesNotMatch() {
        DevOnlyConfig config = new DevOnlyConfig();
        DotenvBinder.bind(config, dotenvOf(Map.of("HOST", "x"), "prod"));
        assertNull(config.host);
    }

    @Test
    void bindAppliesAProfileRestrictedTargetWhenActiveProfileMatches() {
        DevOnlyConfig config = new DevOnlyConfig();
        DotenvBinder.bind(config, dotenvOf(Map.of("HOST", "x"), "dev"));
        assertEquals("x", config.host);
    }

    @Test
    void bindAppliesAnUnrestrictedTargetRegardlessOfProfile() {
        UnrestrictedConfig config = new UnrestrictedConfig();
        DotenvBinder.bind(config, dotenvOf(Map.of("HOST", "x"), "prod"));
        assertEquals("x", config.host);
    }

    @Test
    void reloadThrowsWhenTargetClassIsNotAnnotatedReloadable() {
        NotReloadableConfig config = new NotReloadableConfig();
        Dotenv dotenv = dotenvOf(Map.of("A", "1"), null);
        assertThrows(DotenvInjectionException.class, () -> DotenvBinder.reload(config, dotenv));
    }

    @Test
    void reloadRebindsAReloadableTargetFromFreshValues() {
        ReloadableConfig config = new ReloadableConfig();
        ReloadableTestDotenv dotenv = new ReloadableTestDotenv(Map.of("A", "1"));
        DotenvBinder.bind(config, dotenv);
        assertEquals("1", config.a);

        dotenv.setValues(Map.of("A", "2"));
        DotenvBinder.reload(config, dotenv);

        assertEquals("2", config.a);
    }

    private record TestDotenv(Map<String, String> values, String profile) implements Dotenv {
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
            return java.util.Set.of();
        }

        @Override
        public java.util.Set<DotenvEntry> fileEntries() {
            return java.util.Set.of();
        }

        @Override
        public java.util.Optional<String> activeProfile() {
            return java.util.Optional.ofNullable(profile);
        }
    }

    private static final class ReloadableTestDotenv implements Dotenv {
        private Map<String, String> values;

        ReloadableTestDotenv(Map<String, String> values) {
            this.values = values;
        }

        void setValues(Map<String, String> values) {
            this.values = values;
        }

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
            return java.util.Set.of();
        }

        @Override
        public java.util.Set<DotenvEntry> fileEntries() {
            return java.util.Set.of();
        }

        @Override
        public Dotenv reload() {
            return this;
        }
    }
}
