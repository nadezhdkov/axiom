package io.axiom.console.print;

/** ANSI colors usable as foreground (e.g. {@code [green]}) or background (e.g. {@code [bg-green]}) tags. */
public enum Color {
    BLACK("black", 0),
    RED("red", 1),
    GREEN("green", 2),
    YELLOW("yellow", 3),
    BLUE("blue", 4),
    MAGENTA("magenta", 5),
    CYAN("cyan", 6),
    WHITE("white", 7);

    private final String token;
    private final int base;

    Color(String token, int base) {
        this.token = token;
        this.base = base;
    }

    public String token() {
        return token;
    }

    public int foregroundSgr() {
        return 30 + base;
    }

    public int backgroundSgr() {
        return 40 + base;
    }

    public static Color fromToken(String token) {
        for (Color color : values()) {
            if (color.token.equalsIgnoreCase(token)) {
                return color;
            }
        }
        throw new IllegalArgumentException("Unknown color token: " + token);
    }
}
