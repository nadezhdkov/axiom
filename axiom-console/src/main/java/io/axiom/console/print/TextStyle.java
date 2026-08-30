package io.axiom.console.print;

/** Text styles usable as tags, e.g. {@code [bold]}, alongside foreground/background color tags. */
public enum TextStyle {
    BOLD("bold", 1),
    DIM("dim", 2),
    ITALIC("italic", 3),
    UNDERLINE("underline", 4),
    STRIKETHROUGH("strikethrough", 9);

    private final String token;
    private final int sgr;

    TextStyle(String token, int sgr) {
        this.token = token;
        this.sgr = sgr;
    }

    public String token() {
        return token;
    }

    public int sgr() {
        return sgr;
    }

    /** Returns {@code null} instead of throwing — callers use this to tell a style token from a color token. */
    public static TextStyle tryFromToken(String token) {
        for (TextStyle style : values()) {
            if (style.token.equalsIgnoreCase(token)) {
                return style;
            }
        }
        return null;
    }
}
