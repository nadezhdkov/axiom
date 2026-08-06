package io.axiom.io.attribute;

import io.axiom.io.exception.FileReadException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

/**
 * An atomic, point-in-time snapshot of a file's attributes — obtained via a single
 * {@link BasicFileAttributes} read to avoid the TOCTOU races and extra I/O of querying each
 * attribute independently.
 */
public record FileMetadata(
        String fileName,
        String extension,
        String absolutePath,
        long sizeBytes,
        Instant createdAt,
        Instant lastModified,
        String owner,
        String mimeType,
        boolean isRegularFile,
        boolean isDirectory,
        boolean isSymbolicLink,
        boolean isHidden
) {

    public static FileMetadata of(Path path) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            String fileName = path.getFileName() == null ? "" : path.getFileName().toString();
            return new FileMetadata(
                    fileName,
                    FileAttributes.extractExtension(fileName),
                    path.toAbsolutePath().toString(),
                    attrs.size(),
                    toInstantOrNull(attrs.creationTime()),
                    toInstantOrNull(attrs.lastModifiedTime()),
                    resolveOwner(path),
                    FileAttributes.getMimeType(path),
                    attrs.isRegularFile(),
                    attrs.isDirectory(),
                    attrs.isSymbolicLink(),
                    resolveHidden(path)
            );
        } catch (IOException e) {
            throw new FileReadException(path, e);
        }
    }

    public String sizeFormatted() {
        return FileAttributes.formatSize(sizeBytes);
    }

    private static Instant toInstantOrNull(FileTime time) {
        return time == null ? null : time.toInstant();
    }

    private static String resolveOwner(Path path) {
        try {
            return Files.getOwner(path).getName();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean resolveHidden(Path path) {
        try {
            return Files.isHidden(path);
        } catch (IOException e) {
            return false;
        }
    }
}
