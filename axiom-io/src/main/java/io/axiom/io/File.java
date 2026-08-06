package io.axiom.io;

import io.axiom.io.attribute.FileAttributes;
import io.axiom.io.attribute.FileMetadata;
import io.axiom.io.hash.FileHasher;
import io.axiom.io.hash.HashAlgorithm;

import java.nio.file.Path;

/** Static-method facade over {@link FileHandle}/{@link Directory} for one-shot calls. */
public final class File {

    private File() {
    }

    public static FileHandle at(String path) {
        return FileHandle.at(path);
    }

    public static FileHandle at(Path path) {
        return FileHandle.at(path);
    }

    public static Directory directory(String path) {
        return Directory.at(path);
    }

    public static Path get(String path) {
        return Path.of(path);
    }

    public static Path combine(String first, String... more) {
        return Path.of(first, more);
    }

    public static Path toAbsolutePath(Path path) {
        return path.toAbsolutePath();
    }

    public static String read(String path) {
        return at(path).readAllText();
    }

    public static byte[] readBytes(String path) {
        return at(path).readAllBytes();
    }

    public static void write(String path, String content) {
        write(path, content, false);
    }

    public static void write(String path, String content, boolean append) {
        at(path).write(content, append);
    }

    public static String getFileHash(String filePath, HashAlgorithm algorithm) {
        return FileHasher.hash(Path.of(filePath), algorithm);
    }

    public static String hash(String filePath, HashAlgorithm algorithm) {
        return getFileHash(filePath, algorithm);
    }

    public static FileMetadata metadata(String filePath) {
        return FileMetadata.of(Path.of(filePath));
    }

    public static String getMimeType(String filePath) {
        return FileAttributes.getMimeType(Path.of(filePath));
    }
}
