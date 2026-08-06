package io.axiom.dotenv;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Default {@link Dotenv} implementation: real system environment overrides file values. */
final class DotenvContext implements Dotenv {

    private final Map<String, String> fileValues;
    private final Map<String, String> merged;
    private final String profile;

    DotenvContext(Map<String, String> fileValues, Map<String, String> systemEnv) {
        this(fileValues, systemEnv, null);
    }

    DotenvContext(Map<String, String> fileValues, Map<String, String> systemEnv, String profile) {
        this.fileValues = Map.copyOf(fileValues);
        Map<String, String> m = new LinkedHashMap<>(fileValues);
        m.putAll(systemEnv);
        this.merged = Map.copyOf(m);
        this.profile = profile;
    }

    @Override
    public String get(String key) {
        return merged.get(key);
    }

    @Override
    public String get(String key, String defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : value;
    }

    @Override
    public Set<DotenvEntry> entries() {
        return toEntrySet(merged);
    }

    @Override
    public Set<DotenvEntry> fileEntries() {
        return toEntrySet(fileValues);
    }

    @Override
    public Optional<String> activeProfile() {
        return Optional.ofNullable(profile);
    }

    private static Set<DotenvEntry> toEntrySet(Map<String, String> map) {
        return map.entrySet().stream()
                .map(e -> new DotenvEntry(e.getKey(), e.getValue()))
                .collect(Collectors.toUnmodifiableSet());
    }
}
