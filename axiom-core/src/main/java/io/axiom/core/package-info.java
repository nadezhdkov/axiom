/**
 * Root exceptions of {@code axiom-core}.
 *
 * <h2>Try vs Result vs Maybe</h2>
 * The three "outcome" types live in {@link io.axiom.core.result} and are drawn along one boundary,
 * documented here so it is never left implicit (the gap identified in the Axiom architecture audit):
 * <ul>
 *   <li>{@link io.axiom.core.result.Maybe} — absence of a value with no cause worth communicating.</li>
 *   <li>{@link io.axiom.core.result.Result} — an expected, typed domain failure the caller must handle.</li>
 *   <li>{@link io.axiom.core.result.Try} — capture of a Java API that throws {@link java.lang.Throwable}.</li>
 * </ul>
 */
package io.axiom.core;
