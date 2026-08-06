/**
 * Placeholder/template string resolution ({@code ${key}}, {@code ${key:default}},
 * {@code ${key|transform}}) against a pluggable {@link io.axiom.placeholder.PlaceholderSource}.
 *
 * <p>New module with no direct equivalent in the audited reference libraries — placeholder
 * resolution appeared reimplemented independently in at least three places across them (an
 * SLF4J-style {@code {}} formatter, a {@code ${prop:default}} resolver, and ad hoc I/O message
 * formatting). This module consolidates that into one reusable piece, consumed deliberately by
 * {@code axiom-dotenv} and (optionally) {@code axiom-yaml} instead of each reinventing its own
 * mini-parser.
 *
 * <p>Circular reference detection ({@code ${a}} &rarr; {@code ${b}} &rarr; {@code ${a}}) is
 * mandatory, not optional — neither audited library handled this, and it turns into a
 * {@link StackOverflowError} in production if ignored.
 *
 * <p>Nested placeholders ({@code ${outer.${inner}}}) are out of scope for v1 (documented as an
 * advanced feature, not required).
 */
package io.axiom.placeholder;
