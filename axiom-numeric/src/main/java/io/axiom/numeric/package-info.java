/**
 * Fixed-width integers ({@link io.axiom.numeric.I8}..{@link io.axiom.numeric.I64},
 * {@link io.axiom.numeric.U8}..{@link io.axiom.numeric.U64}) with explicit overflow semantics
 * (checked by default — see each type's {@code add}/{@code subtract}/{@code multiply} — with
 * {@code *Wrapping}/{@code *Saturating} alternatives), plus {@link io.axiom.numeric.Bytes}/
 * {@link io.axiom.numeric.Endian} for endianness-aware binary reads/writes. Fills a real gap in
 * the JDK (no unsigned types, no explicit overflow policy) without reimplementing what the JDK
 * already does well — see {@code docs/axiom-numeric.md} for the full design rationale and
 * {@code README.md}'s "Notas de Design" for decisions made during implementation.
 */
package io.axiom.numeric;
