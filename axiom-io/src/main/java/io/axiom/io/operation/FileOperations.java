package io.axiom.io.operation;

import io.axiom.io.exception.FileWriteException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Predicate;

public final class FileOperations {

    private final Path path;

    public FileOperations(Path path) {
        this.path = path;
    }

    public void createIfNotExists() {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
        } catch (IOException e) {
            throw new FileWriteException(path, e);
        }
    }

    public void createDirectoriesIfNeeded() {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
        } catch (IOException e) {
            throw new FileWriteException(path, e);
        }
    }

    public void create() {
        createDirectoriesIfNeeded();
        createIfNotExists();
    }

    public void delete() {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new FileWriteException(path, e);
        }
    }

    public void deleteIf(Predicate<Path> condition) {
        if (condition.test(path)) {
            delete();
        }
    }

    public Path copyTo(String targetPath) {
        try {
            Path target = Path.of(targetPath);
            return Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileWriteException(path, e);
        }
    }

    public Path copyToIfNotExists(String targetPath) {
        Path target = Path.of(targetPath);
        if (Files.exists(target)) {
            return target;
        }
        return copyTo(targetPath);
    }

    public Path moveTo(String targetPath) {
        try {
            Path target = Path.of(targetPath);
            return Files.move(path, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileWriteException(path, e);
        }
    }

    public Path renameTo(String newFileName) {
        Path target = path.getParent() == null ? Path.of(newFileName) : path.getParent().resolve(newFileName);
        try {
            return Files.move(path, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileWriteException(path, e);
        }
    }

    public Path backup() {
        String backupName = path.getFileName() + "." + System.currentTimeMillis() + ".bak";
        Path target = path.getParent() == null ? Path.of(backupName) : path.getParent().resolve(backupName);
        return copyTo(target.toString());
    }

    public Path createHardLink(String targetPath) {
        try {
            Path target = Path.of(targetPath);
            return Files.createLink(target, path);
        } catch (IOException e) {
            throw new FileWriteException(path, e);
        }
    }

    public Path createSymbolicLink(String targetPath) {
        try {
            Path target = Path.of(targetPath);
            return Files.createSymbolicLink(target, path);
        } catch (IOException e) {
            throw new FileWriteException(path, e);
        }
    }

    public static Path createTemp(String prefix, String suffix) {
        try {
            return Files.createTempFile(prefix, suffix);
        } catch (IOException e) {
            throw new FileWriteException("Failed to create temp file", e, null);
        }
    }

    public static boolean isSameFile(Path path1, Path path2) {
        try {
            return Files.isSameFile(path1, path2);
        } catch (IOException e) {
            return false;
        }
    }
}
