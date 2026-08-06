/**
 * Own YAML tree model ({@link io.axiom.yaml.YamlNode} and its subtypes) plus the public
 * {@link io.axiom.yaml.Yaml} entry point and {@link io.axiom.yaml.YamlMapper} contract.
 *
 * <h2>Engine encapsulation</h2>
 * No type in this package, or in the sibling {@code annotations}/{@code codec}/{@code error}/
 * {@code io} packages, ever exposes a SnakeYAML type. The engine lives entirely behind
 * {@code io.axiom.yaml.internal.snakeyaml}, not exported by {@code module-info.java}.
 */
package io.axiom.yaml;
