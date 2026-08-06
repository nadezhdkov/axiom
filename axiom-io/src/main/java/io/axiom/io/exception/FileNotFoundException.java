package io.axiom.io.exception;

import java.nio.file.Path;

public final class FileNotFoundException extends FileOperationException {

    public FileNotFoundException(Path path) {
        super("File not found: " + path, null, path);
    }

    public FileNotFoundException(Path path, Throwable cause) {
        super("File not found: " + path, cause, path);
    }
}
