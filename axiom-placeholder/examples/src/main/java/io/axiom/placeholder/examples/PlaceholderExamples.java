package io.axiom.placeholder.examples;

import io.axiom.placeholder.PlaceholderResolver;

import java.util.Map;

/** Minimal, compiled-by-CI usage examples for {@code axiom-placeholder}. */
public final class PlaceholderExamples {

    private PlaceholderExamples() {
    }

    public static void main(String[] args) {
        PlaceholderResolver resolver = PlaceholderResolver.of(Map.of(
                "host", "localhost",
                "port", "5432",
                "name", "ada"
        ));

        System.out.println(resolver.resolve("jdbc:postgresql://${host}:${port}/${db:mydb}"));
        System.out.println(resolver.resolve("Hello, ${name|upper}!"));
    }
}
