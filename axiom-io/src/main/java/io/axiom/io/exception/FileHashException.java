package io.axiom.io.exception;

import java.nio.file.Path;

public final class FileHashException extends FileOperationException {

    public FileHashException(Path path, String algorithm, Throwable cause) {
        super("Failed to compute " + algorithm + " hash for file: " + path, cause, path);
    }
}
