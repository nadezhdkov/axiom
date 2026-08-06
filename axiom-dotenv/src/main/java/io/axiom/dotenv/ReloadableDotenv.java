package io.axiom.dotenv;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link Dotenv} whose {@link #reload()} actually re-reads the underlying file(s), obtained via
 * {@link DotenvBuilder#reloadable()}. The current snapshot is held explicitly in an
 * {@link AtomicReference} owned by this single instance — never a static global engine, per the
 * same principle {@link Dotenv}'s javadoc already states for the non-reloadable case.
 */
final class ReloadableDotenv implements Dotenv {

    private final DotenvBuilder builder;
    private final AtomicReference<DotenvContext> current;

    ReloadableDotenv(DotenvBuilder builder, DotenvContext initial) {
        this.builder = builder;
        this.current = new AtomicReference<>(initial);
    }

    @Override
    public String get(String key) {
        return current.get().get(key);
    }

    @Override
    public String get(String key, String defaultValue) {
        return current.get().get(key, defaultValue);
    }

    @Override
    public Set<DotenvEntry> entries() {
        return current.get().entries();
    }

    @Override
    public Set<DotenvEntry> fileEntries() {
        return current.get().fileEntries();
    }

    @Override
    public Optional<String> activeProfile() {
        return current.get().activeProfile();
    }

    @Override
    public Dotenv reload() {
        current.set(builder.loadSnapshot());
        return this;
    }
}
