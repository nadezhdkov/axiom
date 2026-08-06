package io.axiom.io.attribute;

import io.axiom.io.exception.FileReadException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

/** Per-attribute lazy queries against a {@link Path}; see {@link FileMetadata} for an atomic snapshot. */
public final class FileAttributes {

    private final Path path;

    public FileAttributes(Path path) {
        this.path = path;
    }

    public boolean exists() {
        return Files.exists(path);
    }

    public long size() {
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw new FileReadException(path, e);
        }
    }

    public String sizeFormatted() {
        return formatSize(size());
    }

    public boolean isRegularFile() {
        return Files.isRegularFile(path);
    }

    public boolean isDirectory() {
        return Files.isDirectory(path);
    }

    public boolean isSymbolicLink() {
        return Files.isSymbolicLink(path);
    }

    public boolean isReadable() {
        return Files.isReadable(path);
    }

    public boolean isWritable() {
        return Files.isWritable(path);
    }

    public boolean isExecutable() {
        return Files.isExecutable(path);
    }

    public boolean isHidden() {
        try {
            return Files.isHidden(path);
        } catch (IOException e) {
            throw new FileReadException(path, e);
        }
    }

    public String getFileName() {
        return path.getFileName() == null ? "" : path.getFileName().toString();
    }

    public String getFileNameWithoutExtension() {
        String name = getFileName();
        int dot = name.lastIndexOf('.');
        return dot <= 0 ? name : name.substring(0, dot);
    }

    public String getExtension() {
        return extractExtension(getFileName());
    }

    public String getAbsolutePath() {
        return path.toAbsolutePath().toString();
    }

    public Path getParent() {
        return path.getParent();
    }

    public Instant getLastModifiedTime() {
        try {
            return Files.getLastModifiedTime(path).toInstant();
        } catch (IOException e) {
            throw new FileReadException(path, e);
        }
    }

    public void setLastModifiedTime(Instant time) {
        try {
            Files.setLastModifiedTime(path, FileTime.from(time));
        } catch (IOException e) {
            throw new io.axiom.io.exception.FileWriteException(path, e);
        }
    }

    public Instant getCreationTime() {
        try {
            return ((java.nio.file.attribute.BasicFileAttributes)
                    Files.readAttributes(path, java.nio.file.attribute.BasicFileAttributes.class))
                    .creationTime().toInstant();
        } catch (IOException e) {
            throw new FileReadException(path, e);
        }
    }

    public String getOwner() {
        try {
            return Files.getOwner(path).getName();
        } catch (IOException e) {
            throw new FileReadException(path, e);
        }
    }

    public FileMetadata snapshot() {
        return FileMetadata.of(path);
    }

    public static String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot <= 0 || dot == fileName.length() - 1 ? "" : fileName.substring(dot + 1);
    }

    public static String getMimeType(Path path) {
        try {
            return Files.probeContentType(path);
        } catch (IOException e) {
            return null;
        }
    }

    static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        }
        if (bytes < 1024L * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        }
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
