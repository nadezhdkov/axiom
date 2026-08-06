/**
 * Fluent reflection with lookup caching.
 *
 * <p>Entry point: {@link io.axiom.reflect.Reflect#on(Class)} / {@link io.axiom.reflect.Reflect#on(Object)}.
 * Package intentionally named {@code io.axiom.reflect}, never {@code lang.reflect} — the latter
 * collides with {@link java.lang.reflect}, a naming mistake this module deliberately corrects.
 *
 * <p>{@code Field}/{@code Method} lookups performed through this API are cached per
 * {@code (owner class, name[, parameter types])} in {@link io.axiom.reflect.internal.LookupCache} —
 * a capability the reference implementation this was ported from did not have.
 */
package io.axiom.reflect;
