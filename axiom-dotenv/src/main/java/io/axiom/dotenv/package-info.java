/**
 * Parsing and declarative injection of {@code .env} files, unifying what used to be three
 * separate configuration systems in the audited reference libraries ({@code dotenv/},
 * {@code config/}, {@code ioc/environment/Environment}) into this single module.
 *
 * <h2>Overview</h2>
 * <ul>
 *   <li>{@link io.axiom.dotenv.DotenvBuilder} parses a {@code .env} file into a
 *       {@link io.axiom.dotenv.Dotenv} instance (an explicit, user-owned object — never a static
 *       global engine).</li>
 *   <li>{@link io.axiom.dotenv.DotenvBinder} injects values into {@code @Env}-annotated fields
 *       using {@code axiom-reflect} internally instead of raw {@code java.lang.reflect.Field}.</li>
 *   <li>Values may reference other keys via {@code ${OTHER_KEY:default}}, resolved through
 *       {@code axiom-placeholder} instead of a bespoke resolver.</li>
 * </ul>
 *
 * <p>Profiles ({@code @Profile}) and hot reload ({@code @Reloadable}) from the JToolBox reference
 * implementation are not yet ported — a follow-up slice of this module, not silently dropped.
 */
package io.axiom.dotenv;
