package io.axiom.io.stream;

import io.axiom.io.exception.FileWriteException;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public final class FileWriter {

    private final Path path;
    private final Charset charset;

    public FileWriter(Path path, Charset charset) {
        this.path = path;
        this.charset = charset;
    }

    public void write(String content, boolean append) {
        try {
            StandardOpenOption mode = append ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING;
            Files.writeString(path, content, charset, StandardOpenOption.CREATE, mode);
        } catch (IOException e) {
            throw new FileWriteException(path, e);
        }
    }

    public void write(String content) {
        write(content, false);
    }

    public void append(String content) {
        write(content, true);
    }

    public void writeLines(List<String> lines) {
        try {
            Files.write(path, lines, charset);
        } catch (IOException e) {
            throw new FileWriteException(path, e);
        }
    }

    public void writeBytes(byte[] data) {
        try {
            Files.write(path, data);
        } catch (IOException e) {
            throw new FileWriteException(path, e);
        }
    }

    public void writeObject(Serializable object) {
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(path))) {
            out.writeObject(object);
        } catch (IOException e) {
            throw new FileWriteException(path, e);
        }
    }

    public void clear() {
        write("");
    }
}
