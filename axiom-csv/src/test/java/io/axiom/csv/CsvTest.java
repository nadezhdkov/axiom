package io.axiom.csv;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvTest {

    @Test
    void parsesSimpleFields() {
        CsvDocument doc = Csv.parse("a,b,c\n1,2,3\n");
        assertEquals(2, doc.rows().size());
        assertEquals(List.of("a", "b", "c"), doc.rows().get(0).fields());
        assertEquals(List.of("1", "2", "3"), doc.rows().get(1).fields());
    }

    @Test
    void quotedFieldMayContainDelimiter() {
        CsvDocument doc = Csv.parse("\"a,b\",c\n");
        assertEquals(List.of("a,b", "c"), doc.rows().get(0).fields());
    }

    @Test
    void quotedFieldMayContainEscapedQuote() {
        CsvDocument doc = Csv.parse("\"say \"\"hi\"\"\",next\n");
        assertEquals(List.of("say \"hi\"", "next"), doc.rows().get(0).fields());
    }

    @Test
    void quotedFieldMayContainEmbeddedNewline() {
        CsvDocument doc = Csv.parse("\"line1\nline2\",b\n");
        assertEquals(List.of("line1\nline2", "b"), doc.rows().get(0).fields());
    }

    @Test
    void emptyLineIsARowWithOneEmptyField() {
        CsvDocument doc = Csv.parse("a,b\n\nc,d\n");
        assertEquals(3, doc.rows().size());
        assertEquals(List.of(""), doc.rows().get(1).fields());
    }

    @Test
    void emptyDocumentHasNoRows() {
        CsvDocument doc = Csv.parse("");
        assertTrue(doc.rows().isEmpty());
    }

    @Test
    void acceptsCrlfAndLfInTheSameDocument() {
        CsvDocument doc = Csv.parse("a,b\r\nc,d\n");
        assertEquals(2, doc.rows().size());
        assertEquals(List.of("a", "b"), doc.rows().get(0).fields());
        assertEquals(List.of("c", "d"), doc.rows().get(1).fields());
    }

    @Test
    void supportsCustomDelimiter() {
        CsvDocument doc = Csv.parse("a;b;c\n", CsvConfig.defaults().withDelimiter(';'));
        assertEquals(List.of("a", "b", "c"), doc.rows().get(0).fields());
    }

    @Test
    void handlesUnicodeContent() {
        CsvDocument doc = Csv.parse("nome,cidade\nRicardo,São Paulo 🎉\n", CsvConfig.defaults().withHeader(true));
        assertEquals("São Paulo 🎉", doc.get(0, "cidade"));
    }

    @Test
    void unterminatedQuoteThrows() {
        assertThrows(CsvException.class, () -> Csv.parse("\"abc"));
    }

    @Test
    void accessByColumnNameRequiresHeader() {
        CsvDocument withoutHeader = Csv.parse("a,b\n");
        assertThrows(CsvException.class, () -> withoutHeader.get(0, "a"));
    }

    @Test
    void accessByColumnNameWithHeader() {
        CsvDocument doc = Csv.parse("name,age\nAlice,30\nBob,25\n", CsvConfig.defaults().withHeader(true));
        assertEquals(2, doc.rows().size());
        assertEquals("Alice", doc.get(0, "name"));
        assertEquals("25", doc.get(1, "age"));
        assertThrows(CsvException.class, () -> doc.get(0, "unknown"));
    }

    @Test
    void parsesFromReader() {
        CsvDocument doc = Csv.parse(new StringReader("a,b\n1,2\n"));
        assertEquals(2, doc.rows().size());
    }

    @Test
    void writeThenParseRoundTripsSemanticContent() {
        CsvDocument original = Csv.parse(
            "name,note\nAlice,\"hello, world\"\nBob,\"line1\nline2\"\n",
            CsvConfig.defaults().withHeader(true));

        String written = Csv.write(original, CsvConfig.defaults().withHeader(true));
        CsvDocument reparsed = Csv.parse(written, CsvConfig.defaults().withHeader(true));

        assertEquals(original.header(), reparsed.header());
        assertEquals(original.rows().size(), reparsed.rows().size());
        for (int i = 0; i < original.rows().size(); i++) {
            assertEquals(original.rows().get(i).fields(), reparsed.rows().get(i).fields());
        }
    }

    @Test
    void writeQuotesOnlyFieldsThatNeedIt() {
        CsvDocument doc = Csv.parse("plain,\"has,comma\"\n");
        String written = Csv.write(doc);
        assertEquals("plain,\"has,comma\"\r\n", written);
    }
}
