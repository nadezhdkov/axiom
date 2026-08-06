package io.axiom.csv.internal;

import io.axiom.csv.CsvException;

import java.util.ArrayList;
import java.util.List;

/**
 * Hand-written RFC 4180-style parser (plus common real-world tolerance): quoted fields with
 * {@code delimiter}/quote/newline embedded, {@code ""} as an escaped quote inside a quoted
 * field, and both {@code \r\n} and {@code \n} accepted as line terminators regardless of which
 * one a given line actually uses. Not exported — {@link io.axiom.csv.Csv} is the only public
 * entry point.
 */
public final class CsvParser {

    private CsvParser() {
    }

    public static List<List<String>> parse(String text, char delimiter, char quote) {
        List<List<String>> rows = new ArrayList<>();
        List<String> currentRow = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean rowHasContent = false;

        int i = 0;
        int n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == quote) {
                    if (i + 1 < n && text.charAt(i + 1) == quote) {
                        field.append(quote);
                        i += 2;
                    } else {
                        inQuotes = false;
                        i++;
                    }
                } else {
                    field.append(c);
                    i++;
                }
                continue;
            }

            if (c == quote && field.length() == 0) {
                inQuotes = true;
                rowHasContent = true;
                i++;
            } else if (c == delimiter) {
                currentRow.add(field.toString());
                field.setLength(0);
                rowHasContent = true;
                i++;
            } else if (c == '\r' || c == '\n') {
                currentRow.add(field.toString());
                field.setLength(0);
                rows.add(currentRow);
                currentRow = new ArrayList<>();
                rowHasContent = false;
                i++;
                if (c == '\r' && i < n && text.charAt(i) == '\n') {
                    i++;
                }
            } else {
                field.append(c);
                rowHasContent = true;
                i++;
            }
        }

        if (inQuotes) {
            throw new CsvException("unterminated quoted field");
        }
        if (rowHasContent || field.length() > 0 || !currentRow.isEmpty()) {
            currentRow.add(field.toString());
            rows.add(currentRow);
        }
        return rows;
    }
}
