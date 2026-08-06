package io.axiom.csv.examples;

import io.axiom.csv.Csv;
import io.axiom.csv.CsvConfig;
import io.axiom.csv.CsvDocument;

public final class CsvExamples {

    private CsvExamples() {
    }

    public static void main(String[] args) {
        String source = """
            name,city
            Ricardo,São Paulo
            "Jane, Doe","New York"
            """;

        CsvDocument document = Csv.parse(source, CsvConfig.defaults().withHeader(true));

        System.out.println("rows: " + document.rows().size());
        System.out.println("first name: " + document.get(0, "name"));
        System.out.println("second city: " + document.get(1, "city"));

        String rewritten = Csv.write(document, CsvConfig.defaults().withHeader(true));
        System.out.println(rewritten);
    }
}
