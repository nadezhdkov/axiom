package io.axiom.io.exception;

import java.nio.file.Path;

/** Base of the {@code axiom-io} exception hierarchy; every failure carries the target {@link Path}. */
public sealed class FileOperationException extends RuntimeException
        permits FileReadException, FileWriteException, FileNotFoundException,
                FileCompressionException, FileHashException {

    private final Path targetPath;

    protected FileOperationException(String message, Throwable cause, Path targetPath) {
        super(message, cause);
        this.targetPath = targetPath;
    }

    public Path getTargetPath() {
        return targetPath;
    }
}
