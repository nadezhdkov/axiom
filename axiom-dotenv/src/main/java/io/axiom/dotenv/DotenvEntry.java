package io.axiom.dotenv;

/** A single resolved {@code KEY=value} entry. */
public record DotenvEntry(String key, String value) {
}
