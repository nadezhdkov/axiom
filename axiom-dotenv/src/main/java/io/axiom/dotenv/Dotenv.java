package io.axiom.dotenv;

import java.util.Optional;
import java.util.Set;

/**
 * Resolved {@code .env} + system environment values. Instances are built explicitly via
 * {@link DotenvBuilder} — never a static global engine, unlike the {@code EnvEngine} static
 * mutable singleton this design deliberately avoids repeating.
 */
public interface Dotenv {

    static DotenvBuilder configure() {
        return new DotenvBuilder();
    }

    /** Value for {@code key}, preferring the real system environment over the parsed file. */
    String get(String key);

    String get(String key, String defaultValue);

    /** All entries, file values overridden by system environment variables of the same key. */
    Set<DotenvEntry> entries();

    /** Only the entries parsed from the {@code .env} file, unaffected by system environment overrides. */
    Set<DotenvEntry> fileEntries();

    /** The active profile this instance was loaded with, if {@link DotenvBuilder#profile(String)} was used. */
    default Optional<String> activeProfile() {
        return Optional.empty();
    }

    /**
     * Re-reads the underlying {@code .env} file(s) and returns a fresh {@link Dotenv} reflecting
     * the current file content. Only instances built via {@link DotenvBuilder#reloadable()}
     * support this; every other instance is an immutable one-shot snapshot by design.
     */
    default Dotenv reload() {
        throw new UnsupportedOperationException(
                "This Dotenv instance does not support reload(); build it via DotenvBuilder#reloadable() to opt in.");
    }
}
