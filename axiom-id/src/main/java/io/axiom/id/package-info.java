/**
 * Time-sortable identifier generation: {@link io.axiom.id.Id#uuidV7()} (RFC 9562 UUIDv7, reusing
 * {@link java.util.UUID} as the return type — the JDK's representation is already fine, only its
 * generation is missing v7 support) and {@link io.axiom.id.Id#ulid()} ({@link io.axiom.id.Ulid},
 * a type of its own since ULID's 26-character Base32-Crockford string form has no JDK
 * equivalent). Both are monotonic within the same millisecond; see {@code README.md} for the
 * design rationale.
 */
package io.axiom.id;
