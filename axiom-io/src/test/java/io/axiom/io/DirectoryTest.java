package io.axiom.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectoryTest {

    @Test
    void createIsIdempotentAndDeep(@TempDir Path dir) {
        Path nested = dir.resolve("a/b/c");
        Directory.at(nested).create();
        Directory.at(nested).create();

        assertTrue(Files.isDirectory(nested));
    }

    @Test
    void isEmptyReflectsContents(@TempDir Path dir) throws Exception {
        Directory directory = Directory.at(dir).create();
        assertTrue(directory.isEmpty());

        Files.createFile(dir.resolve("file.txt"));
        assertFalse(directory.isEmpty());
    }

    @Test
    void listAndListNamesReturnChildren(@TempDir Path dir) throws Exception {
        Files.createFile(dir.resolve("a.txt"));
        Files.createFile(dir.resolve("b.txt"));

        assertEquals(2, Directory.at(dir).list().size());
        assertTrue(Directory.at(dir).listNames().containsAll(java.util.List.of("a.txt", "b.txt")));
    }

    @Test
    void deleteRecursivelyRemovesNestedContentAndTheDirectoryItself(@TempDir Path dir) throws Exception {
        Path nested = dir.resolve("sub");
        Files.createDirectories(nested);
        Files.createFile(nested.resolve("file.txt"));

        Directory.at(nested).deleteRecursively();

        assertFalse(Files.exists(nested));
    }

    @Test
    void deleteRecursivelyIsSafeWhenDirectoryDoesNotExist(@TempDir Path dir) {
        Directory.at(dir.resolve("missing")).deleteRecursively();
    }

    @Test
    void cleanKeepsTheDirectoryButRemovesContentsRecursively(@TempDir Path dir) throws Exception {
        Path sub = dir.resolve("sub");
        Files.createDirectories(sub);
        Files.createFile(sub.resolve("nested.txt"));
        Files.createFile(dir.resolve("top.txt"));

        Directory.at(dir).clean();

        assertTrue(Files.isDirectory(dir));
        assertTrue(Directory.at(dir).isEmpty());
    }
}
