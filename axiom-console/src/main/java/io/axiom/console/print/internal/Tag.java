package io.axiom.console.print.internal;

import io.axiom.console.print.Color;
import io.axiom.console.print.TextStyle;

/** A single parsed {@code [token]}: a foreground color, a background color, or a text style. */
public sealed interface Tag {

    int sgrCode();

    record Foreground(Color color) implements Tag {
        @Override
        public int sgrCode() {
            return color.foregroundSgr();
        }
    }

    record Background(Color color) implements Tag {
        @Override
        public int sgrCode() {
            return color.backgroundSgr();
        }
    }

    record Style(TextStyle style) implements Tag {
        @Override
        public int sgrCode() {
            return style.sgr();
        }
    }

    static Tag parse(String token) {
        if (token.startsWith("bg-")) {
            return new Background(Color.fromToken(token.substring("bg-".length())));
        }
        TextStyle style = TextStyle.tryFromToken(token);
        if (style != null) {
            return new Style(style);
        }
        return new Foreground(Color.fromToken(token));
    }
}
