package io.axiom.placeholder;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Pluggable source of values for placeholder resolution.
 *
 * @see PlaceholderResolver
 */
@FunctionalInterface
public interface PlaceholderSource {

    Optional<String> resolve(String key);

    static PlaceholderSource of(Map<String, String> map) {
        return key -> Optional.ofNullable(map.get(key));
    }

    static PlaceholderSource environment() {
        return of(System.getenv());
    }

    static PlaceholderSource systemProperties() {
        return key -> Optional.ofNullable(System.getProperty(key));
    }

    static PlaceholderSource function(Function<String, Optional<String>> fn) {
        return fn::apply;
    }

    /** Falls back to {@code other} whenever this source has no value for a key. */
    default PlaceholderSource orElse(PlaceholderSource other) {
        return key -> this.resolve(key).or(() -> other.resolve(key));
    }
}
