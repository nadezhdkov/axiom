/**
 * Persistent (immutable) data structures.
 *
 * <h2>Naming convention</h2>
 * Every persistent collection family in this module uses a {@code P} prefix
 * ({@link io.axiom.collections.PCollection}, {@link io.axiom.collections.PMap},
 * {@link io.axiom.collections.PSet}, {@link io.axiom.collections.PSequence},
 * {@link io.axiom.collections.PVector}, {@link io.axiom.collections.PStack},
 * {@link io.axiom.collections.PQueue}, {@link io.axiom.collections.PSortedMap},
 * {@link io.axiom.collections.PSortedSet}), read as "Persistent", applied 100% consistently —
 * axiom.md §4/§8 requires a single documented convention rather than the ad hoc mix of
 * prefixed/unprefixed names found across the audited reference libraries.
 *
 * <h2>Families</h2>
 * <ul>
 *   <li>{@link io.axiom.collections.PMap}/{@link io.axiom.collections.PSet} — HAMT-backed
 *       ({@link io.axiom.collections.HashTrieMap}/{@link io.axiom.collections.HashTrieSet}),
 *       property-tested for path-copying and structural-equality invariants.</li>
 *   <li>{@link io.axiom.collections.PVector} — chunked array vector
 *       ({@link io.axiom.collections.ChunkedPVector}); see its javadoc for the deliberate,
 *       documented decision not to implement a full RRB-tree.</li>
 *   <li>{@link io.axiom.collections.PStack} — cons-list stack
 *       ({@link io.axiom.collections.ConsPStack}), {@code O(1)} push with full structural
 *       sharing of the tail.</li>
 *   <li>{@link io.axiom.collections.PQueue} — Okasaki two-stack queue
 *       ({@link io.axiom.collections.AmortizedPQueue}), amortized {@code O(1)}.</li>
 *   <li>{@link io.axiom.collections.PSortedMap}/{@link io.axiom.collections.PSortedSet} —
 *       {@link java.util.TreeMap}/{@link java.util.TreeSet}-backed
 *       ({@link io.axiom.collections.TreePMap}/{@link io.axiom.collections.TreePSet}), full
 *       {@link java.util.NavigableMap}/{@link java.util.NavigableSet} contract, copy-on-write.</li>
 * </ul>
 */
package io.axiom.collections;
