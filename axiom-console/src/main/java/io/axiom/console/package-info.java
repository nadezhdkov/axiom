/**
 * Layered, testable console scanning — graduated from {@code obsidian.experimental.io.scan} to a
 * stable module. {@link io.axiom.console.InputHandler} is the core session contract, obtained via
 * {@link io.axiom.console.InputScanner} over a pluggable
 * {@link io.axiom.console.source.InputSource} ({@code ConsoleSource}/{@code ReaderSource}/
 * {@code StringSource} — the latter is what makes tests never touch {@code System.in}).
 * {@link io.axiom.console.Scan} is an optional static convenience facade over a swappable
 * default engine.
 */
package io.axiom.console;
