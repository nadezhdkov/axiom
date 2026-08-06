package io.axiom.reflect.examples;

import io.axiom.reflect.Reflect;

/** Minimal, compiled-by-CI usage examples for {@code axiom-reflect}. */
public final class ReflectExamples {

    private ReflectExamples() {
    }

    static class User {
        private String name;
        private int age;
    }

    public static void main(String[] args) {
        User user = Reflect.on(User.class).create();
        Reflect.on(user).field("name").set("Ada");
        Reflect.on(user).field("age").set(36);

        String name = Reflect.on(user).field("name").get();
        System.out.println("name: " + name);

        var fieldNames = Reflect.on(user).fields().notStatic().names();
        System.out.println("fields: " + fieldNames);
    }
}
