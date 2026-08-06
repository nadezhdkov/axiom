package io.axiom.io;

import io.axiom.io.attribute.FileMetadata;
import io.axiom.io.hash.Sha256Hash;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileHandleTest {

    @Test
    void createWriteReadChain(@TempDir Path dir) {
        Path file = dir.resolve("note.txt");
        FileHandle handle = FileHandle.at(file).createIfNotExists().write("hello");

        assertEquals("hello", handle.readAllText());
    }

    @Test
    void writeAppendThenReadAllLines(@TempDir Path dir) {
        Path file = dir.resolve("log.txt");
        FileHandle handle = FileHandle.at(file).createIfNotExists()
                .write("line1\n")
                .append("line2\n");

        assertEquals(List.of("line1", "line2"), handle.readAllLines());
    }

    @Test
    void writeLinesThenFilter(@TempDir Path dir) {
        Path file = dir.resolve("data.txt");
        FileHandle handle = FileHandle.at(file).createIfNotExists()
                .writeLines(List.of("apple", "banana", "avocado"));

        assertEquals(List.of("apple", "avocado"), handle.filter(line -> line.startsWith("a")));
    }

    @Test
    void compressThenDecompressRoundTrips(@TempDir Path dir) {
        Path file = dir.resolve("original.txt");
        Path compressed = dir.resolve("original.gz");
        Path restored = dir.resolve("restored.txt");

        FileHandle.at(file).createIfNotExists().write("payload");
        FileHandle.at(file).compress(compressed.toString());
        FileHandle.at(compressed).decompress(restored.toString());

        assertEquals("payload", FileHandle.at(restored).readAllText());
    }

    @Test
    void metadataIsAnAtomicSnapshot(@TempDir Path dir) {
        Path file = dir.resolve("meta.txt");
        FileHandle.at(file).createIfNotExists().write("content");

        FileMetadata metadata = FileHandle.at(file).metadata();

        assertEquals("meta.txt", metadata.fileName());
        assertEquals("txt", metadata.extension());
        assertTrue(metadata.isRegularFile());
        assertFalse(metadata.isDirectory());
    }

    @Test
    void hashViaStrategyMatchesHashByAlgorithmName(@TempDir Path dir) {
        Path file = dir.resolve("hashed.txt");
        FileHandle.at(file).createIfNotExists().write("data");

        String viaStrategy = FileHandle.at(file).hash(new Sha256Hash());
        String viaName = FileHandle.at(file).hash("SHA-256");

        assertEquals(viaStrategy, viaName);
    }

    @Test
    void copyToAndBackupCreateIndependentFiles(@TempDir Path dir) {
        Path file = dir.resolve("orig.txt");
        Path copy = dir.resolve("copy.txt");
        FileHandle.at(file).createIfNotExists().write("v1");

        FileHandle.at(file).copyTo(copy.toString());
        FileHandle.at(file).backup();

        assertEquals("v1", FileHandle.at(copy).readAllText());
        assertTrue(FileHandle.at(dir).isDirectory());
    }

    @Test
    void deleteRemovesTheFile(@TempDir Path dir) {
        Path file = dir.resolve("gone.txt");
        FileHandle handle = FileHandle.at(file).createIfNotExists();
        assertTrue(handle.exists());

        handle.delete();
        assertFalse(handle.exists());
    }

    @Test
    void staticGetExtensionExtractsWithoutTheDot() {
        assertEquals("txt", FileHandle.getExtension("report.txt"));
        assertEquals("", FileHandle.getExtension("no-extension"));
    }

    @Test
    void createTempProducesAnExistingFile() {
        Path temp = FileHandle.createTemp("axiom", ".tmp");
        assertTrue(FileHandle.at(temp).exists());
        FileHandle.at(temp).delete();
    }
}
