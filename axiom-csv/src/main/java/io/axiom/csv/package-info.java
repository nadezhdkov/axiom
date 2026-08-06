/**
 * CSV parsing and writing with a model of its own ({@link io.axiom.csv.CsvDocument},
 * {@link io.axiom.csv.CsvRow}) — the JDK has no CSV support at all, and naive
 * {@code String.split(",")} mishandles quoted fields, embedded delimiters/newlines, and escaped
 * quotes. Unlike {@code axiom-json}/{@code axiom-yaml}, CSV is simple enough that no third-party
 * engine needs to be hidden behind {@code internal.*} — the parser in
 * {@link io.axiom.csv.internal.CsvParser} is the whole implementation.
 */
package io.axiom.csv;
