package io.axiom.io.hash;

import io.axiom.io.exception.FileHashException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileHasherTest {

    @Test
    void md5AndSha256ProduceDifferentDigests(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("data.txt");
        Files.writeString(file, "axiom");

        String md5 = FileHasher.hash(file, new Md5Hash());
        String sha256 = FileHasher.hash(file, new Sha256Hash());

        assertNotEquals(md5, sha256);
        assertEquals(32, md5.length());
        assertEquals(64, sha256.length());
    }

    @Test
    void hashByNameMatchesHashByStrategy(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("data.txt");
        Files.writeString(file, "axiom");

        assertEquals(FileHasher.hash(file, new Sha256Hash()), FileHasher.hash(file, "SHA-256"));
    }

    @Test
    void sameContentProducesSameHashDifferentContentDiffers(@TempDir Path dir) throws Exception {
        Path a = dir.resolve("a.txt");
        Path b = dir.resolve("b.txt");
        Files.writeString(a, "same");
        Files.writeString(b, "same");
        Path c = dir.resolve("c.txt");
        Files.writeString(c, "different");

        assertEquals(FileHasher.hash(a, new Sha256Hash()), FileHasher.hash(b, new Sha256Hash()));
        assertNotEquals(FileHasher.hash(a, new Sha256Hash()), FileHasher.hash(c, new Sha256Hash()));
    }

    @Test
    void missingFileThrowsFileHashException(@TempDir Path dir) {
        Path missing = dir.resolve("missing.txt");
        assertThrows(FileHashException.class, () -> FileHasher.hash(missing, new Sha256Hash()));
    }

    @Test
    void unsupportedAlgorithmNameThrowsFileHashException(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("data.txt");
        Files.writeString(file, "axiom");
        assertThrows(FileHashException.class, () -> FileHasher.hash(file, "NOT-AN-ALGORITHM"));
    }
}
