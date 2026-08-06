package io.axiom.dotenv.internal;

import io.axiom.dotenv.DotenvEntry;
import io.axiom.dotenv.DotenvException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DotenvLineParserTest {

    @Test
    void parsesSimpleKeyValue() {
        List<DotenvEntry> entries = DotenvLineParser.parse(".env", List.of("HOST=localhost"), false);
        assertEquals(List.of(new DotenvEntry("HOST", "localhost")), entries);
    }

    @Test
    void skipsBlankLinesAndComments() {
        List<DotenvEntry> entries = DotenvLineParser.parse(".env", 
                List.of("", "  ", "# a comment", "PORT=5432"), false);
        assertEquals(List.of(new DotenvEntry("PORT", "5432")), entries);
    }

    @Test
    void supportsExportPrefix() {
        List<DotenvEntry> entries = DotenvLineParser.parse(".env", List.of("export NAME=Ada"), false);
        assertEquals(List.of(new DotenvEntry("NAME", "Ada")), entries);
    }

    @Test
    void doubleQuotedValuePreservesInnerHashAndSpaces() {
        List<DotenvEntry> entries = DotenvLineParser.parse(".env", 
                List.of("MSG=\"hello # world  \""), false);
        assertEquals("hello # world  ", entries.get(0).value());
    }

    @Test
    void singleQuotedValueIsLiteralNoEscaping() {
        List<DotenvEntry> entries = DotenvLineParser.parse(".env", List.of("RAW='no \\n escapes here'"), false);
        assertEquals("no \\n escapes here", entries.get(0).value());
    }

    @Test
    void doubleQuotedValueProcessesEscapes() {
        List<DotenvEntry> entries = DotenvLineParser.parse(".env", List.of("MULTI=\"line1\\nline2\""), false);
        assertEquals("line1\nline2", entries.get(0).value());
    }

    @Test
    void unquotedValueStripsInlineComment() {
        List<DotenvEntry> entries = DotenvLineParser.parse(".env", List.of("DEBUG=true # enable verbose logging"), false);
        assertEquals("true", entries.get(0).value());
    }

    @Test
    void unquotedValueWithoutTrailingCommentIsTrimmed() {
        List<DotenvEntry> entries = DotenvLineParser.parse(".env", List.of("NAME=  Ada  "), false);
        assertEquals("Ada", entries.get(0).value());
    }

    @Test
    void malformedLineIsSkippedByDefault() {
        List<DotenvEntry> entries = DotenvLineParser.parse(".env", List.of("not a valid line", "OK=1"), false);
        assertEquals(List.of(new DotenvEntry("OK", "1")), entries);
    }

    @Test
    void malformedLineThrowsInStrictMode() {
        DotenvException ex = assertThrows(DotenvException.class,
                () -> DotenvLineParser.parse(".env", List.of("not a valid line"), true));
        assertTrue(ex.getMessage().contains("not a valid line"));
    }

    @Test
    void malformedLineErrorIncludesSourceNameAndLineNumber() {
        DotenvException ex = assertThrows(DotenvException.class, () -> DotenvLineParser.parse(
                "config/database.env", List.of("HOST=ok", "not a valid line"), true));
        assertTrue(ex.getMessage().contains("config/database.env"), ex.getMessage());
        assertTrue(ex.getMessage().contains("line 2"), ex.getMessage());
    }

    @Test
    void malformedLineWithColonInsteadOfEqualsHintsTheFix() {
        DotenvException ex = assertThrows(DotenvException.class,
                () -> DotenvLineParser.parse(".env", List.of("host: localhost"), true));
        assertTrue(ex.getMessage().toLowerCase().contains("hint"), ex.getMessage());
    }

    @Test
    void emptyValueIsAllowed() {
        List<DotenvEntry> entries = DotenvLineParser.parse(".env", List.of("EMPTY="), false);
        assertEquals("", entries.get(0).value());
    }
}
