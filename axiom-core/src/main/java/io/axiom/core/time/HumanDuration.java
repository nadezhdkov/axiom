package io.axiom.core.time;

import java.time.Duration;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts {@link Duration} to/from a compact human-readable form (e.g. {@code "2h 30m"}).
 *
 * <p>{@link Duration} has no built-in support for this — {@link Duration#toString()} produces
 * ISO-8601 ({@code "PT2H30M"}), not something meant for a UI or a config file. This is a small,
 * self-contained utility, not a module: {@code axiom.md} explicitly defers creating
 * {@code axiom-datetime} unless enough API volume accumulates to justify it, and wrapping
 * {@link java.time.ZonedDateTime} in a bespoke {@code DateTime} type (as JToolBox did) was
 * rejected outright as reinventing an API the JDK already solves well.
 */
public final class HumanDuration {

    private static final Pattern COMPONENT = Pattern.compile("(\\d+)([dhms])");

    private HumanDuration() {
    }

    /** Formats using only the units that have a non-zero value, largest to smallest (e.g. {@code "2h 30m"}). */
    public static String format(Duration duration) {
        Objects.requireNonNull(duration, "duration must not be null");
        long totalSeconds = duration.getSeconds();
        if (totalSeconds == 0) {
            return "0s";
        }

        StringBuilder builder = new StringBuilder();
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (days > 0) {
            builder.append(days).append('d').append(' ');
        }
        if (hours > 0) {
            builder.append(hours).append('h').append(' ');
        }
        if (minutes > 0) {
            builder.append(minutes).append('m').append(' ');
        }
        if (seconds > 0 || builder.isEmpty()) {
            builder.append(seconds).append('s').append(' ');
        }
        return builder.substring(0, builder.length() - 1);
    }

    /** Parses a string of the form {@code "1d 2h 30m 15s"} (any subset, any order, space-separated). */
    public static Duration parse(String text) {
        Objects.requireNonNull(text, "text must not be null");
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Cannot parse empty duration string");
        }

        Matcher matcher = COMPONENT.matcher(trimmed);
        long totalSeconds = 0;
        int matchedChars = 0;
        while (matcher.find()) {
            matchedChars += matcher.group().length();
            long value = Long.parseLong(matcher.group(1));
            totalSeconds += switch (matcher.group(2)) {
                case "d" -> value * 86400;
                case "h" -> value * 3600;
                case "m" -> value * 60;
                case "s" -> value;
                default -> throw new IllegalStateException("Unreachable");
            };
        }

        String withoutWhitespace = trimmed.replaceAll("\\s+", "");
        if (matchedChars != withoutWhitespace.length()) {
            throw new IllegalArgumentException("Invalid duration string: \"" + text + "\"");
        }

        return Duration.ofSeconds(totalSeconds);
    }
}
