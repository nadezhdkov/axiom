package io.axiom.console.print.internal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Parses {@code [token]...[/]} tags (color, background, style — see {@link Tag#parse}), nestable
 * and combinable by nesting (e.g. {@code [bold][green]...[/][/]}). Each open/close re-emits the
 * full SGR state (always prefixed with {@code 0;}) from every tag still on the stack, rather than
 * trying to undo a single attribute — ANSI has no per-attribute "pop", so replaying the whole
 * stack is what keeps nested closes correct.
 */
public final class TagRenderer {

    private static final String RESET = "[0m";

    private TagRenderer() {
    }

    public static String render(String text, boolean stylingEnabled) {
        StringBuilder result = new StringBuilder();
        Deque<Tag> stack = new ArrayDeque<>();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '[') {
                int close = text.indexOf(']', i);
                if (close < 0) {
                    throw new IllegalArgumentException("Unclosed tag in: " + text);
                }
                String token = text.substring(i + 1, close);
                if (token.equals("/")) {
                    if (stack.isEmpty()) {
                        throw new IllegalArgumentException("Unmatched [/] in: " + text);
                    }
                    stack.pop();
                } else {
                    stack.push(Tag.parse(token));
                }
                if (stylingEnabled) {
                    result.append(currentState(stack));
                }
                i = close + 1;
            } else {
                result.append(c);
                i++;
            }
        }
        if (!stack.isEmpty()) {
            throw new IllegalArgumentException("Unclosed tag(s) in: " + text);
        }
        return result.toString();
    }

    private static String currentState(Deque<Tag> stack) {
        if (stack.isEmpty()) {
            return RESET;
        }
        StringBuilder codes = new StringBuilder("0");
        Iterator<Tag> bottomToTop = stack.descendingIterator();
        while (bottomToTop.hasNext()) {
            codes.append(';').append(bottomToTop.next().sgrCode());
        }
        return "[" + codes + "m";
    }
}
