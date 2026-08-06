package io.axiom.io.exception;

import java.nio.file.Path;

public final class FileCompressionException extends FileOperationException {

    public FileCompressionException(Path path, Throwable cause) {
        super("Compression/decompression failed for file: " + path, cause, path);
    }
}
