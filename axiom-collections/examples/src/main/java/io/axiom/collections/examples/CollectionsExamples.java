package io.axiom.collections.examples;

import io.axiom.collections.HashTrieMap;
import io.axiom.collections.HashTrieSet;
import io.axiom.collections.PMap;
import io.axiom.collections.PQueue;
import io.axiom.collections.PSet;
import io.axiom.collections.PSortedMap;
import io.axiom.collections.PStack;
import io.axiom.collections.PVector;
import io.axiom.collections.TreePMap;

/** Minimal, compiled-by-CI usage examples for {@code axiom-collections}. */
public final class CollectionsExamples {

    private CollectionsExamples() {
    }

    public static void main(String[] args) {
        PMap<String, Integer> ages = HashTrieMap.<String, Integer>empty()
                .plus("alice", 30)
                .plus("bob", 25);

        PMap<String, Integer> updated = ages.plus("carol", 40);
        System.out.println("ages:    " + ages.size() + " entries");
        System.out.println("updated: " + updated.size() + " entries (ages is unaffected)");

        PSet<String> tags = HashTrieSet.<String>empty().plus("java").plus("persistent");
        System.out.println("tags contains 'java': " + tags.contains("java"));

        PVector<Integer> vector = PVector.<Integer>empty().plus(1).plus(2).plus(3);
        System.out.println("vector: " + vector);

        PStack<Integer> stack = PStack.<Integer>empty().plus(1).plus(2).plus(3);
        System.out.println("stack (top first): " + stack);

        PQueue<Integer> queue = PQueue.<Integer>empty().plus(1).plus(2).plus(3);
        System.out.println("queue head: " + queue.peek());

        PSortedMap<Integer, String> scores = TreePMap.<Integer, String>empty()
                .plus(3, "third").plus(1, "first").plus(2, "second");
        System.out.println("sorted keys: " + scores.keySet());
    }
}
