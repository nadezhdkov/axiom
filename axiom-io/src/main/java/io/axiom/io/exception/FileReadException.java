package io.axiom.io.exception;

import java.nio.file.Path;

public final class FileReadException extends FileOperationException {

    public FileReadException(Path path, Throwable cause) {
        super("Failed to read file: " + path, cause, path);
    }

    public FileReadException(String message, Throwable cause, Path path) {
        super(message, cause, path);
    }
}
