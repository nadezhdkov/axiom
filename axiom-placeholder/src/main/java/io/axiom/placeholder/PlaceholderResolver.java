package io.axiom.placeholder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves {@code ${key}}, {@code ${key:default}} and {@code ${key|transform}} placeholders in a
 * template string against a {@link PlaceholderSource}.
 *
 * <h2>Syntax</h2>
 * <ul>
 *   <li>{@code ${key}} — required; throws {@link UnresolvedPlaceholderException} if unresolved</li>
 *   <li>{@code ${key:default}} — {@code default} used verbatim if {@code key} is unresolved</li>
 *   <li>{@code ${key|transform}} — applies a registered transform (e.g. {@code upper}) to the resolved value</li>
 *   <li>{@code ${key:default|transform}} — both together</li>
 * </ul>
 *
 * <p>Resolved values are themselves recursively resolved (so a source value of {@code "${b}"}
 * for key {@code a} is followed through), with cycle detection: resolving the same key twice in
 * one resolution chain throws {@link CircularPlaceholderReferenceException} instead of
 * overflowing the stack.
 *
 * <p>Nested placeholders ({@code ${outer.${inner}}}) are not supported in v1.
 */
public final class PlaceholderResolver {

    private static final Pattern CONTENT_PATTERN = Pattern.compile("^([^:|]+)(?::([^|]*))?(?:\\|(.+))?$");

    private final PlaceholderSource source;
    private final Map<String, UnaryOperator<String>> transforms;

    private PlaceholderResolver(PlaceholderSource source, Map<String, UnaryOperator<String>> transforms) {
        this.source = source;
        this.transforms = transforms;
    }

    public static PlaceholderResolver of(PlaceholderSource source) {
        Objects.requireNonNull(source, "source");
        Map<String, UnaryOperator<String>> builtins = new HashMap<>();
        builtins.put("upper", String::toUpperCase);
        builtins.put("lower", String::toLowerCase);
        builtins.put("trim", String::trim);
        return new PlaceholderResolver(source, builtins);
    }

    public static PlaceholderResolver of(Map<String, String> map) {
        return of(PlaceholderSource.of(map));
    }

    /** Returns a resolver with an additional (or overriding) named transform available to {@code |name}. */
    public PlaceholderResolver withTransform(String name, UnaryOperator<String> transform) {
        Map<String, UnaryOperator<String>> copy = new HashMap<>(transforms);
        copy.put(name, transform);
        return new PlaceholderResolver(source, copy);
    }

    /** Resolves every {@code ${...}} occurrence in {@code template}. */
    public String resolve(String template) {
        Objects.requireNonNull(template, "template");
        return resolve(template, new ArrayDeque<>());
    }

    private String resolve(String template, Deque<String> stack) {
        StringBuilder out = new StringBuilder();
        int i = 0;

        while (i < template.length()) {
            int start = template.indexOf("${", i);
            if (start < 0) {
                out.append(template, i, template.length());
                break;
            }

            int end = template.indexOf('}', start + 2);
            if (end < 0) {
                out.append(template, i, template.length());
                break;
            }

            out.append(template, i, start);
            String content = template.substring(start + 2, end);
            out.append(resolvePlaceholder(content, stack));
            i = end + 1;
        }

        return out.toString();
    }

    private String resolvePlaceholder(String content, Deque<String> stack) {
        Matcher m = CONTENT_PATTERN.matcher(content);
        if (!m.matches()) {
            throw new PlaceholderException("Malformed placeholder: \"${" + content + "}\"");
        }

        String key = m.group(1);
        String defaultValue = m.group(2);
        String transformName = m.group(3);

        String rawValue = source.resolve(key)
                .map(value -> resolveKeyValue(key, value, stack))
                .orElseGet(() -> {
                    if (defaultValue != null) {
                        return resolve(defaultValue, stack);
                    }
                    throw new UnresolvedPlaceholderException(key);
                });

        if (transformName == null) {
            return rawValue;
        }
        UnaryOperator<String> transform = transforms.get(transformName);
        if (transform == null) {
            throw new PlaceholderException("Unknown placeholder transform: \"" + transformName + "\"");
        }
        return transform.apply(rawValue);
    }

    private String resolveKeyValue(String key, String value, Deque<String> stack) {
        if (stack.contains(key)) {
            List<String> cycle = new ArrayList<>(stack);
            cycle.add(key);
            throw new CircularPlaceholderReferenceException(cycle);
        }
        stack.push(key);
        try {
            return resolve(value, stack);
        } finally {
            stack.pop();
        }
    }
}
