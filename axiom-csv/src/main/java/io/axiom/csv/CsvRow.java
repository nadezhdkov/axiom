package io.axiom.csv;

import java.util.List;
import java.util.Objects;

/** One immutable CSV row: an ordered list of fields. */
public final class CsvRow {

    private final List<String> fields;

    CsvRow(List<String> fields) {
        this.fields = List.copyOf(fields);
    }

    public int size() {
        return fields.size();
    }

    public String get(int index) {
        if (index < 0 || index >= fields.size()) {
            throw new CsvException(
                "column index " + index + " out of bounds for row of size " + fields.size());
        }
        return fields.get(index);
    }

    public List<String> fields() {
        return fields;
    }

    @Override
    public String toString() {
        return fields.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CsvRow other && fields.equals(other.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fields);
    }
}
