package io.axiom.csv;

import io.axiom.csv.internal.CsvParser;
import io.axiom.csv.internal.CsvWriter;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/** Static facade for parsing and writing CSV. */
public final class Csv {

    private Csv() {
    }

    public static CsvDocument parse(String text) {
        return parse(text, CsvConfig.defaults());
    }

    public static CsvDocument parse(String text, CsvConfig config) {
        List<List<String>> rawRows = CsvParser.parse(text, config.delimiter(), config.quote());
        return toDocument(rawRows, config);
    }

    public static CsvDocument parse(Reader reader) {
        return parse(reader, CsvConfig.defaults());
    }

    public static CsvDocument parse(Reader reader, CsvConfig config) {
        return parse(readAll(reader), config);
    }

    public static String write(CsvDocument document) {
        return write(document, CsvConfig.defaults());
    }

    public static String write(CsvDocument document, CsvConfig config) {
        StringWriter out = new StringWriter();
        write(document, out, config);
        return out.toString();
    }

    public static void write(CsvDocument document, Writer writer) {
        write(document, writer, CsvConfig.defaults());
    }

    public static void write(CsvDocument document, Writer writer, CsvConfig config) {
        try {
            if (document.header().isPresent()) {
                CsvWriter.writeRow(writer, document.header().get(), config.delimiter(), config.quote());
            }
            for (CsvRow row : document.rows()) {
                CsvWriter.writeRow(writer, row.fields(), config.delimiter(), config.quote());
            }
        } catch (IOException e) {
            throw new CsvException("failed to write CSV", e);
        }
    }

    private static CsvDocument toDocument(List<List<String>> rawRows, CsvConfig config) {
        List<String> header = null;
        int start = 0;
        if (config.header() && !rawRows.isEmpty()) {
            header = rawRows.get(0);
            start = 1;
        }
        List<CsvRow> rows = new ArrayList<>();
        for (int i = start; i < rawRows.size(); i++) {
            rows.add(new CsvRow(rawRows.get(i)));
        }
        return new CsvDocument(header, rows);
    }

    private static String readAll(Reader reader) {
        StringBuilder out = new StringBuilder();
        char[] buffer = new char[4096];
        int read;
        try {
            while ((read = reader.read(buffer)) != -1) {
                out.append(buffer, 0, read);
            }
        } catch (IOException e) {
            throw new CsvException("failed to read CSV source", e);
        }
        return out.toString();
    }
}
