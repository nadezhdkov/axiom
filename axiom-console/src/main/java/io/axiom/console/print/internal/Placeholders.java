package io.axiom.console.print.internal;

/**
 * Positional {@code {}} substitution, resolved before style tags. Brackets coming from an
 * argument's own text are escaped to private-use sentinels so they can't be mistaken for a
 * {@code [tag]}/{@code [/]} written in the template; {@link #unescapeBrackets} restores them to
 * literal {@code [}/{@code ]} once tag resolution is done.
 */
public final class Placeholders {

    private static final char ESCAPED_OPEN = '';
    private static final char ESCAPED_CLOSE = '';

    private Placeholders() {
    }

    public static String substitute(String template, Object... args) {
        StringBuilder result = new StringBuilder();
        int argIndex = 0;
        int i = 0;
        while (i < template.length()) {
            char c = template.charAt(i);
            if (c == '{' && i + 1 < template.length() && template.charAt(i + 1) == '}') {
                if (argIndex >= args.length) {
                    throw new IllegalArgumentException(
                            "Not enough arguments for template: " + template);
                }
                result.append(escapeBrackets(String.valueOf(args[argIndex++])));
                i += 2;
            } else {
                result.append(c);
                i++;
            }
        }
        if (argIndex < args.length) {
            throw new IllegalArgumentException("Too many arguments for template: " + template);
        }
        return result.toString();
    }

    private static String escapeBrackets(String value) {
        return value.replace('[', ESCAPED_OPEN).replace(']', ESCAPED_CLOSE);
    }

    public static String unescapeBrackets(String value) {
        return value.replace(ESCAPED_OPEN, '[').replace(ESCAPED_CLOSE, ']');
    }
}
