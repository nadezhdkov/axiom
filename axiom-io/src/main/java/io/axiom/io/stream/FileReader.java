package io.axiom.io.stream;

import io.axiom.io.exception.FileReadException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public final class FileReader {

    private final Path path;
    private final Charset charset;

    public FileReader(Path path, Charset charset) {
        this.path = path;
        this.charset = charset;
    }

    public String readAllText() {
        try {
            return Files.readString(path, charset);
        } catch (IOException e) {
            throw new FileReadException(path, e);
        }
    }

    public byte[] readAllBytes() {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new FileReadException(path, e);
        }
    }

    public List<String> readAllLines() {
        try {
            return Files.readAllLines(path, charset);
        } catch (IOException e) {
            throw new FileReadException(path, e);
        }
    }

    /** Caller must close the returned stream. */
    public Stream<String> lines() {
        try {
            return Files.lines(path, charset);
        } catch (IOException e) {
            throw new FileReadException(path, e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T readObject(Class<T> type) {
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(path))) {
            return (T) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new FileReadException(path, e);
        }
    }

    public List<String> readFirstLines(int n) {
        try (Stream<String> lines = lines()) {
            return lines.limit(n).toList();
        } catch (UncheckedIOException e) {
            throw new FileReadException(path, e.getCause());
        }
    }

    public List<String> readLastLines(int n) {
        List<String> all = readAllLines();
        return all.subList(Math.max(0, all.size() - n), all.size());
    }
}
