package io.axiom.dotenv.internal;

import io.axiom.dotenv.DotenvEntry;
import io.axiom.dotenv.DotenvException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Line-oriented {@code .env} parser: {@code KEY=value} pairs, optional {@code export } prefix,
 * {@code #} comments (full-line and inline for unquoted values), and single/double-quoted values.
 *
 * <p>Multi-line quoted values are not supported (a documented v1 limitation, unlike the
 * reference implementation this was inspired by) — every entry must fit on one line.
 */
public final class DotenvLineParser {

    private static final Pattern LINE_PATTERN =
            Pattern.compile("^(?:export\\s+)?([A-Za-z_][A-Za-z0-9_.\\-]*)\\s*=\\s*(.*)$");

    private DotenvLineParser() {
    }

    public static List<DotenvEntry> parse(String sourceName, List<String> lines, boolean strict) {
        List<DotenvEntry> entries = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String rawLine = lines.get(i);
            String trimmed = rawLine.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            Matcher m = LINE_PATTERN.matcher(trimmed);
            if (!m.matches()) {
                if (strict) {
                    throw new DotenvException(malformedLineMessage(sourceName, i + 1, rawLine));
                }
                continue;
            }

            entries.add(new DotenvEntry(m.group(1), parseValue(m.group(2))));
        }

        return entries;
    }

    private static String malformedLineMessage(String sourceName, int lineNumber, String rawLine) {
        StringBuilder message = new StringBuilder("Malformed line in '")
            .append(sourceName).append("' (line ").append(lineNumber).append("): \"")
            .append(rawLine).append('"');
        if (rawLine.indexOf(':') >= 0 && rawLine.indexOf('=') < 0) {
            message.append(" — hint: .env entries use 'KEY=value', not 'KEY: value'");
        }
        return message.toString();
    }

    private static String parseValue(String rest) {
        String trimmed = rest.strip();
        if (trimmed.length() >= 1 && (trimmed.charAt(0) == '"' || trimmed.charAt(0) == '\'')) {
            char quote = trimmed.charAt(0);
            int end = findClosingQuote(trimmed, quote);
            String content = end >= 0 ? trimmed.substring(1, end) : trimmed.substring(1);
            return quote == '"' ? unescapeDouble(content) : content;
        }

        int hashIndex = indexOfUnquotedHash(trimmed);
        String value = hashIndex >= 0 ? trimmed.substring(0, hashIndex) : trimmed;
        return value.strip();
    }

    private static int findClosingQuote(String s, char quote) {
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                i++;
                continue;
            }
            if (c == quote) return i;
        }
        return -1;
    }

    private static String unescapeDouble(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case 'n' -> out.append('\n');
                    case 't' -> out.append('\t');
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    default -> out.append(next);
                }
                i++;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static int indexOfUnquotedHash(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '#' && (i == 0 || Character.isWhitespace(s.charAt(i - 1)))) {
                return i;
            }
        }
        return -1;
    }
}
