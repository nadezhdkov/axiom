package io.axiom.io.exception;

import java.nio.file.Path;

public final class FileWriteException extends FileOperationException {

    public FileWriteException(Path path, Throwable cause) {
        super("Failed to write to file: " + path, cause, path);
    }

    public FileWriteException(String message, Throwable cause, Path path) {
        super(message, cause, path);
    }
}
