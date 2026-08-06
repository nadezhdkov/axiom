package io.axiom.csv;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** An immutable parsed CSV document: an optional header row plus data rows. */
public final class CsvDocument {

    private final List<String> header;
    private final List<CsvRow> rows;
    private final Map<String, Integer> headerIndex;

    CsvDocument(List<String> header, List<CsvRow> rows) {
        this.header = header == null ? null : List.copyOf(header);
        this.rows = List.copyOf(rows);
        if (this.header == null) {
            this.headerIndex = null;
        } else {
            Map<String, Integer> index = new LinkedHashMap<>();
            for (int i = 0; i < this.header.size(); i++) {
                index.put(this.header.get(i), i);
            }
            this.headerIndex = Map.copyOf(index);
        }
    }

    public Optional<List<String>> header() {
        return Optional.ofNullable(header);
    }

    public List<CsvRow> rows() {
        return rows;
    }

    /**
     * Reads a field by row index and column name.
     *
     * @throws CsvException if this document has no header, or {@code column} is not one of its
     *     names.
     */
    public String get(int rowIndex, String column) {
        if (headerIndex == null) {
            throw new CsvException("document has no header; use rows().get(i).get(columnIndex) instead");
        }
        Integer columnIndex = headerIndex.get(column);
        if (columnIndex == null) {
            throw new CsvException("unknown column: " + column);
        }
        return rows.get(rowIndex).get(columnIndex);
    }
}
