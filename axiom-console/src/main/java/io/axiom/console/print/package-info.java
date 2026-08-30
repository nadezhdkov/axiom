/**
 * Enhanced console output — {@link io.axiom.console.print.IO} substitutes {@code {}}
 * placeholders and {@code [tag]...[/]} tags (foreground/background color via {@link
 * io.axiom.console.print.Color}, text style via {@link io.axiom.console.print.TextStyle}) on top
 * of {@code System.out}, without depending on {@code axiom-placeholder} (kept out of this
 * module's dependency edge on purpose).
 */
package io.axiom.console.print;
