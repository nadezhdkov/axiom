package io.axiom.console.parse;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Set;

/** Stateless factory for the common {@link Parser}s. */
public final class Parsers {

    private static final Set<String> TRUE_TOKENS = Set.of("true", "t", "1", "yes", "y", "sim", "s");
    private static final Set<String> FALSE_TOKENS = Set.of("false", "f", "0", "no", "n", "nao", "não");

    private Parsers() {
    }

    public static Parser<String> string() {
        return raw -> raw;
    }

    public static Parser<Integer> i32() {
        return raw -> {
            try {
                return Integer.parseInt(raw.trim());
            } catch (NumberFormatException e) {
                throw new ParseFailureException("Expected int", e);
            }
        };
    }

    public static Parser<Long> i64() {
        return raw -> {
            try {
                return Long.parseLong(raw.trim());
            } catch (NumberFormatException e) {
                throw new ParseFailureException("Expected long", e);
            }
        };
    }

    public static Parser<Double> f64() {
        return raw -> {
            try {
                return Double.parseDouble(raw.trim());
            } catch (NumberFormatException e) {
                throw new ParseFailureException("Expected double", e);
            }
        };
    }

    public static Parser<Boolean> bool() {
        return raw -> {
            String normalized = raw.trim().toLowerCase();
            if (TRUE_TOKENS.contains(normalized)) {
                return true;
            }
            if (FALSE_TOKENS.contains(normalized)) {
                return false;
            }
            throw new ParseFailureException("Expected boolean");
        };
    }

    public static Parser<Character> ch() {
        return raw -> {
            String trimmed = raw.trim();
            if (trimmed.length() != 1) {
                throw new ParseFailureException("Expected single char");
            }
            return trimmed.charAt(0);
        };
    }

    public static Parser<BigInteger> bigInt() {
        return raw -> {
            try {
                return new BigInteger(raw.trim());
            } catch (NumberFormatException e) {
                throw new ParseFailureException("Expected BigInteger", e);
            }
        };
    }

    public static Parser<BigDecimal> bigDec() {
        return raw -> {
            try {
                return new BigDecimal(raw.trim());
            } catch (NumberFormatException e) {
                throw new ParseFailureException("Expected BigDecimal", e);
            }
        };
    }
}
