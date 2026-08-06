package io.axiom.json.util;

import io.axiom.json.JsonArray;
import io.axiom.json.JsonElement;
import io.axiom.json.JsonNull;
import io.axiom.json.JsonObject;
import io.axiom.json.JsonPrimitive;

import java.util.Iterator;
import java.util.Map;

/**
 * Hand-rolled recursive-descent JSON printer, deliberately independent of the underlying engine's
 * own formatting so {@code stringify} output never drifts if the engine is swapped.
 */
public final class JsonPrettyPrinter {

    private static final String INDENT = "  ";

    private JsonPrettyPrinter() {
    }

    public static String toPrettyString(JsonElement element) {
        StringBuilder builder = new StringBuilder();
        write(element, builder, 0, true);
        return builder.toString();
    }

    public static String toCompactString(JsonElement element) {
        StringBuilder builder = new StringBuilder();
        write(element, builder, 0, false);
        return builder.toString();
    }

    private static void write(JsonElement element, StringBuilder out, int depth, boolean pretty) {
        if (element == null || element instanceof JsonNull) {
            out.append("null");
        } else if (element instanceof JsonPrimitive primitive) {
            writePrimitive(primitive, out);
        } else if (element instanceof JsonObject object) {
            writeObject(object, out, depth, pretty);
        } else if (element instanceof JsonArray array) {
            writeArray(array, out, depth, pretty);
        } else {
            throw new IllegalArgumentException("Unknown JsonElement type: " + element.getClass());
        }
    }

    private static void writePrimitive(JsonPrimitive primitive, StringBuilder out) {
        if (primitive.isString()) {
            out.append('"');
            escapeString(primitive.asString(), out);
            out.append('"');
        } else {
            out.append(primitive.asString());
        }
    }

    private static void writeObject(JsonObject object, StringBuilder out, int depth, boolean pretty) {
        if (object.isEmpty()) {
            out.append("{}");
            return;
        }
        out.append('{');
        newline(out, pretty);
        Iterator<Map.Entry<String, JsonElement>> it = object.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, JsonElement> entry = it.next();
            indent(out, depth + 1, pretty);
            out.append('"');
            escapeString(entry.getKey(), out);
            out.append("\":");
            if (pretty) {
                out.append(' ');
            }
            write(entry.getValue(), out, depth + 1, pretty);
            if (it.hasNext()) {
                out.append(',');
            }
            newline(out, pretty);
        }
        indent(out, depth, pretty);
        out.append('}');
    }

    private static void writeArray(JsonArray array, StringBuilder out, int depth, boolean pretty) {
        if (array.isEmpty()) {
            out.append("[]");
            return;
        }
        out.append('[');
        newline(out, pretty);
        Iterator<JsonElement> it = array.iterator();
        while (it.hasNext()) {
            indent(out, depth + 1, pretty);
            write(it.next(), out, depth + 1, pretty);
            if (it.hasNext()) {
                out.append(',');
            }
            newline(out, pretty);
        }
        indent(out, depth, pretty);
        out.append(']');
    }

    private static void newline(StringBuilder out, boolean pretty) {
        if (pretty) {
            out.append('\n');
        }
    }

    private static void indent(StringBuilder out, int depth, boolean pretty) {
        if (pretty) {
            out.append(INDENT.repeat(depth));
        }
    }

    private static void escapeString(String value, StringBuilder out) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
    }
}
