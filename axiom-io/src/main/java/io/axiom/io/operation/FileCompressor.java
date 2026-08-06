package io.axiom.io.operation;

import io.axiom.io.exception.FileCompressionException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class FileCompressor {

    private final Path path;

    public FileCompressor(Path path) {
        this.path = path;
    }

    public void compress(String targetPath) {
        Path target = Path.of(targetPath);
        try (InputStream in = Files.newInputStream(path);
             OutputStream out = new GZIPOutputStream(Files.newOutputStream(target))) {
            in.transferTo(out);
        } catch (IOException e) {
            throw new FileCompressionException(path, e);
        }
    }

    public void decompress(String targetPath) {
        Path target = Path.of(targetPath);
        try (InputStream in = new GZIPInputStream(Files.newInputStream(path));
             OutputStream out = Files.newOutputStream(target)) {
            in.transferTo(out);
        } catch (IOException e) {
            throw new FileCompressionException(path, e);
        }
    }
}
