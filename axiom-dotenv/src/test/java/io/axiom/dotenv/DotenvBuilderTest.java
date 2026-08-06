package io.axiom.dotenv;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DotenvBuilderTest {

    @Test
    void loadsAndParsesEntries(@TempDir Path dir) throws IOException {
        writeEnv(dir, "HOST=localhost\nPORT=5432\n");

        Dotenv dotenv = Dotenv.configure().directory(dir.toString()).load();

        assertEquals("localhost", dotenv.get("HOST"));
        assertEquals("5432", dotenv.get("PORT"));
        assertEquals(2, dotenv.fileEntries().size());
    }

    @Test
    void missingFileIsIgnoredByDefault() {
        Dotenv dotenv = Dotenv.configure().directory("/nonexistent/path/xyz").load();
        assertNull(dotenv.get("ANYTHING"));
    }

    @Test
    void throwIfMissingThrowsForAbsentFile() {
        var builder = Dotenv.configure().directory("/nonexistent/path/xyz").throwIfMissing();
        assertThrows(DotenvException.class, builder::load);
    }

    @Test
    void getWithDefaultFallsBackWhenKeyAbsent(@TempDir Path dir) throws IOException {
        writeEnv(dir, "A=1\n");
        Dotenv dotenv = Dotenv.configure().directory(dir.toString()).load();

        assertEquals("1", dotenv.get("A", "fallback"));
        assertEquals("fallback", dotenv.get("MISSING", "fallback"));
    }

    @Test
    void interpolatesReferencesBetweenKeysByDefault(@TempDir Path dir) throws IOException {
        writeEnv(dir, "HOST=localhost\nPORT=5432\nURL=jdbc://${HOST}:${PORT}/db\n");
        Dotenv dotenv = Dotenv.configure().directory(dir.toString()).load();

        assertEquals("jdbc://localhost:5432/db", dotenv.get("URL"));
    }

    @Test
    void withoutInterpolationKeepsRawPlaceholderSyntax(@TempDir Path dir) throws IOException {
        writeEnv(dir, "HOST=localhost\nURL=jdbc://${HOST}/db\n");
        Dotenv dotenv = Dotenv.configure().directory(dir.toString()).withoutInterpolation().load();

        assertEquals("jdbc://${HOST}/db", dotenv.get("URL"));
    }

    @Test
    void circularPlaceholderReferencePropagatesFromAxiomPlaceholder(@TempDir Path dir) throws IOException {
        // axiom.md §11: axiom-dotenv must surface axiom-placeholder's circular-reference
        // detection, not silently loop or overflow the stack.
        writeEnv(dir, "A=${B}\nB=${A}\n");
        var builder = Dotenv.configure().directory(dir.toString());

        assertThrows(io.axiom.placeholder.CircularPlaceholderReferenceException.class, builder::load);
    }

    @Test
    void strictModeThrowsOnMalformedLine(@TempDir Path dir) throws IOException {
        writeEnv(dir, "not a valid line\n");
        var builder = Dotenv.configure().directory(dir.toString()).strict();
        assertThrows(DotenvException.class, builder::load);
    }

    @Test
    void fileEntriesExcludeSystemEnvironmentOnlyKeys() throws IOException {
        // sanity: fileEntries() reflects only the parsed file, entries() would also include
        // real system environment variables, which we can't control from a test — just assert
        // fileEntries() stays scoped to what PATH-independent data we wrote.
        Path dir = Files.createTempDirectory("axiom-dotenv-test");
        try {
            writeEnv(dir, "ONLY_IN_FILE=yes\n");
            Dotenv dotenv = Dotenv.configure().directory(dir.toString()).load();
            assertTrue(dotenv.fileEntries().contains(new DotenvEntry("ONLY_IN_FILE", "yes")));
            assertEquals(1, dotenv.fileEntries().size());
        } finally {
            Files.deleteIfExists(dir.resolve(".env"));
            Files.deleteIfExists(dir);
        }
    }

    @Test
    void profileFileOverridesBaseFileForTheSameKey(@TempDir Path dir) throws IOException {
        writeEnv(dir, "HOST=base-host\nPORT=1111\n");
        Files.writeString(dir.resolve(".env.dev"), "HOST=dev-host\n");

        Dotenv dotenv = Dotenv.configure().directory(dir.toString()).profile("dev").load();

        assertEquals("dev-host", dotenv.get("HOST"));
        assertEquals("1111", dotenv.get("PORT"));
        assertEquals("dev", dotenv.activeProfile().orElseThrow());
    }

    @Test
    void missingProfileFileIsIgnored(@TempDir Path dir) throws IOException {
        writeEnv(dir, "HOST=base-host\n");
        Dotenv dotenv = Dotenv.configure().directory(dir.toString()).profile("staging").load();
        assertEquals("base-host", dotenv.get("HOST"));
    }

    @Test
    void nonReloadableDotenvThrowsOnReload(@TempDir Path dir) throws IOException {
        writeEnv(dir, "A=1\n");
        Dotenv dotenv = Dotenv.configure().directory(dir.toString()).load();
        assertThrows(UnsupportedOperationException.class, dotenv::reload);
    }

    @Test
    void reloadableDotenvReflectsFileChangesAfterReload(@TempDir Path dir) throws IOException {
        writeEnv(dir, "A=1\n");
        Dotenv dotenv = Dotenv.configure().directory(dir.toString()).reloadable().load();
        assertEquals("1", dotenv.get("A"));

        writeEnv(dir, "A=2\n");
        Dotenv reloaded = dotenv.reload();

        assertEquals("2", reloaded.get("A"));
        assertEquals("2", dotenv.get("A"));
    }

    private static void writeEnv(Path dir, String content) throws IOException {
        Files.writeString(dir.resolve(".env"), content);
    }
}
