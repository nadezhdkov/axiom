package io.axiom.io;

import io.axiom.io.exception.FileWriteException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Directory {

    private final Path path;

    private Directory(Path path) {
        this.path = path;
    }

    public static Directory at(String path) {
        return new Directory(Path.of(path));
    }

    public static Directory at(Path path) {
        return new Directory(path);
    }

    public Directory create() {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new FileWriteException(path, e);
        }
        return this;
    }

    public boolean exists() {
        return Files.isDirectory(path);
    }

    public boolean isEmpty() {
        try (Stream<Path> children = Files.list(path)) {
            return children.findAny().isEmpty();
        } catch (IOException e) {
            throw new FileWriteException(path, e);
        }
    }

    public List<Path> list() {
        try (Stream<Path> children = Files.list(path)) {
            return children.toList();
        } catch (IOException e) {
            throw new FileWriteException(path, e);
        }
    }

    public List<String> listNames() {
        return list().stream().map(p -> p.getFileName().toString()).collect(Collectors.toList());
    }

    public void deleteRecursively() {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(Directory::deletePathUnchecked);
        } catch (IOException e) {
            throw new FileWriteException(path, e);
        }
    }

    public void clean() {
        for (Path child : list()) {
            if (Files.isDirectory(child)) {
                Directory.at(child).deleteRecursively();
            } else {
                deletePathUnchecked(child);
            }
        }
    }

    private static void deletePathUnchecked(Path target) {
        try {
            Files.delete(target);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
