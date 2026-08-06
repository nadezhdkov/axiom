package io.axiom.io.exception;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileOperationExceptionTest {

    @Test
    void subtypesCarryTheTargetPath() {
        Path path = Path.of("a.txt");
        FileReadException ex = new FileReadException(path, new RuntimeException());
        assertEquals(path, ex.getTargetPath());
    }

    @Test
    void fileNotFoundExceptionWorksWithoutACause() {
        Path path = Path.of("missing.txt");
        FileNotFoundException ex = new FileNotFoundException(path);
        assertTrue(ex.getMessage().contains("missing.txt"));
    }

    @Test
    void fileHashExceptionIncludesTheAlgorithmNameInTheMessage() {
        FileHashException ex = new FileHashException(Path.of("f.txt"), "SHA-256", new RuntimeException());
        assertTrue(ex.getMessage().contains("SHA-256"));
    }

    @Test
    void everySubtypeIsCatchableAsTheSealedBase() {
        FileOperationException ex = new FileWriteException(Path.of("f.txt"), new RuntimeException());
        assertInstanceOf(FileOperationException.class, ex);
    }
}
