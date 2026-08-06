package io.axiom.csv.internal;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/** Writes rows with minimal quoting: a field is quoted only if it needs to be. */
public final class CsvWriter {

    private CsvWriter() {
    }

    public static void writeRow(Writer writer, List<String> fields, char delimiter, char quote)
        throws IOException {
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                writer.write(delimiter);
            }
            writer.write(escape(fields.get(i), delimiter, quote));
        }
        writer.write("\r\n");
    }

    private static String escape(String field, char delimiter, char quote) {
        boolean needsQuoting = field.indexOf(delimiter) >= 0
            || field.indexOf(quote) >= 0
            || field.indexOf('\n') >= 0
            || field.indexOf('\r') >= 0;
        if (!needsQuoting) {
            return field;
        }
        StringBuilder out = new StringBuilder(field.length() + 2);
        out.append(quote);
        for (int i = 0; i < field.length(); i++) {
            char c = field.charAt(i);
            if (c == quote) {
                out.append(quote);
            }
            out.append(c);
        }
        out.append(quote);
        return out.toString();
    }
}
