/**
 * Own JSON tree model ({@link io.axiom.json.JsonElement} and its subtypes) plus the public
 * {@link io.axiom.json.Json} entry point and {@link io.axiom.json.JsonMapper} contract.
 *
 * <h2>Engine encapsulation</h2>
 * No type in this package, or in {@code io.axiom.json.annotations}, {@code io.axiom.json.codec},
 * {@code io.axiom.json.error}, {@code io.axiom.json.io}, or {@code io.axiom.json.util}, ever
 * exposes a Gson type. The engine lives entirely behind {@code io.axiom.json.internal.gson},
 * which is not exported by {@code module-info.java} — swapping the engine later cannot break
 * public API.
 */
package io.axiom.json;
