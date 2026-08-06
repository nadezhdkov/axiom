package io.axiom.io;

import io.axiom.io.attribute.FileAttributes;
import io.axiom.io.attribute.FileMetadata;
import io.axiom.io.attribute.FilePermissions;
import io.axiom.io.hash.FileHasher;
import io.axiom.io.hash.HashAlgorithm;
import io.axiom.io.operation.FileCompressor;
import io.axiom.io.operation.FileOperations;
import io.axiom.io.search.FileSearch;
import io.axiom.io.stream.FileReader;
import io.axiom.io.stream.FileWriter;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Fluent delegation core over a {@link Path}: lazily-initialized {@link FileReader},
 * {@link FileWriter}, {@link FileAttributes}, {@link FilePermissions}, {@link FileOperations},
 * {@link FileCompressor}, and {@link FileSearch}. Ported from {@code io.obsidian.file.FileHandle}
 * (the only subsystem with real test coverage in either audited reference library).
 */
public final class FileHandle {

    private final Path path;
    private Charset charset = StandardCharsets.UTF_8;

    private FileReader reader;
    private FileWriter writer;
    private FileAttributes attributes;
    private FilePermissions permissions;
    private FileOperations operations;
    private FileCompressor compressor;
    private FileSearch search;

    private FileHandle(Path path) {
        this.path = path;
    }

    public static FileHandle at(String path) {
        return new FileHandle(Path.of(path));
    }

    public static FileHandle at(Path path) {
        return new FileHandle(path);
    }

    public FileHandle charset(Charset charset) {
        this.charset = charset;
        this.reader = null;
        this.writer = null;
        this.search = null;
        return this;
    }

    public Path path() {
        return path;
    }

    // ── Lifecycle ──────────────────────────────────────────────

    public FileHandle createIfNotExists() {
        operations().createIfNotExists();
        return this;
    }

    public FileHandle createDirectoriesIfNeeded() {
        operations().createDirectoriesIfNeeded();
        return this;
    }

    public FileHandle create() {
        operations().create();
        return this;
    }

    public void delete() {
        operations().delete();
    }

    public FileHandle deleteIf(Predicate<Path> condition) {
        operations().deleteIf(condition);
        return this;
    }

    // ── Write ──────────────────────────────────────────────────

    public FileHandle write(String content, boolean append) {
        writer().write(content, append);
        return this;
    }

    public FileHandle write(String content) {
        writer().write(content);
        return this;
    }

    public FileHandle append(String content) {
        writer().append(content);
        return this;
    }

    public FileHandle writeLines(List<String> lines) {
        writer().writeLines(lines);
        return this;
    }

    public FileHandle writeBytes(byte[] data) {
        writer().writeBytes(data);
        return this;
    }

    public FileHandle writeObject(Serializable object) {
        writer().writeObject(object);
        return this;
    }

    public FileHandle clear() {
        writer().clear();
        return this;
    }

    // ── Read ───────────────────────────────────────────────────

    public String readAllText() {
        return reader().readAllText();
    }

    public byte[] readAllBytes() {
        return reader().readAllBytes();
    }

    public List<String> readAllLines() {
        return reader().readAllLines();
    }

    /** Caller must close the returned stream. */
    public Stream<String> lines() {
        return reader().lines();
    }

    public <T> T readObject(Class<T> type) {
        return reader().readObject(type);
    }

    public List<String> readFirstLines(int n) {
        return reader().readFirstLines(n);
    }

    public List<String> readLastLines(int n) {
        return reader().readLastLines(n);
    }

    // ── Search / filter ───────────────────────────────────────

    public List<String> filter(Predicate<String> predicate) {
        return search().filter(predicate);
    }

    public FileHandle filterAndSave(Predicate<String> predicate, String targetPath) {
        search().filterAndSave(predicate, targetPath);
        return this;
    }

    public List<String> grep(String regex) {
        return search().grep(regex);
    }

    public FileHandle replaceAll(String regex, String replacement) {
        search().replaceAll(regex, replacement);
        return this;
    }

    public FileHandle processLines(java.util.function.Consumer<String> consumer) {
        search().processLines(consumer);
        return this;
    }

    public long countLines() {
        return search().countLines();
    }

    public long count(String searchString) {
        return search().count(searchString);
    }

    public boolean contentEquals(String otherPath) {
        return search().contentEquals(otherPath);
    }

    // ── Manipulation ──────────────────────────────────────────

    public FileHandle copyTo(String targetPath) {
        operations().copyTo(targetPath);
        return this;
    }

    public FileHandle copyToIfNotExists(String targetPath) {
        operations().copyToIfNotExists(targetPath);
        return this;
    }

    public FileHandle moveTo(String targetPath) {
        operations().moveTo(targetPath);
        return this;
    }

    public FileHandle renameTo(String newFileName) {
        return FileHandle.at(operations().renameTo(newFileName));
    }

    public FileHandle backup() {
        operations().backup();
        return this;
    }

    public FileHandle createHardLink(String targetPath) {
        operations().createHardLink(targetPath);
        return this;
    }

    public FileHandle createSymbolicLink(String targetPath) {
        operations().createSymbolicLink(targetPath);
        return this;
    }

    // ── Compression ────────────────────────────────────────────

    public FileHandle compress(String targetPath) {
        compressor().compress(targetPath);
        return this;
    }

    public FileHandle decompress(String targetPath) {
        compressor().decompress(targetPath);
        return this;
    }

    // ── Attributes ─────────────────────────────────────────────

    public FileMetadata metadata() {
        return attributes().snapshot();
    }

    public boolean exists() {
        return attributes().exists();
    }

    public long size() {
        return attributes().size();
    }

    public String sizeFormatted() {
        return attributes().sizeFormatted();
    }

    public boolean isRegularFile() {
        return attributes().isRegularFile();
    }

    public boolean isDirectory() {
        return attributes().isDirectory();
    }

    public boolean isSymbolicLink() {
        return attributes().isSymbolicLink();
    }

    public boolean isReadable() {
        return attributes().isReadable();
    }

    public boolean isWritable() {
        return attributes().isWritable();
    }

    public boolean isExecutable() {
        return attributes().isExecutable();
    }

    public boolean isHidden() {
        return attributes().isHidden();
    }

    public String getFileName() {
        return attributes().getFileName();
    }

    public String getFileNameWithoutExtension() {
        return attributes().getFileNameWithoutExtension();
    }

    public String getExtension() {
        return attributes().getExtension();
    }

    public String getAbsolutePath() {
        return attributes().getAbsolutePath();
    }

    public Path getParent() {
        return attributes().getParent();
    }

    // ── Permissions ────────────────────────────────────────────

    public String getOwner() {
        return permissions().getOwner();
    }

    public FileHandle setOwner(String username) {
        permissions().setOwner(username);
        return this;
    }

    public java.util.Set<java.nio.file.attribute.PosixFilePermission> getPosixPermissions() {
        return permissions().getPosixPermissions();
    }

    public FileHandle setPosixPermissions(java.util.Set<java.nio.file.attribute.PosixFilePermission> perms) {
        permissions().setPosixPermissions(perms);
        return this;
    }

    public FileHandle setReadOnly() {
        permissions().setReadOnly();
        return this;
    }

    // ── Hashing ────────────────────────────────────────────────

    public String hash(HashAlgorithm algorithm) {
        return FileHasher.hash(path, algorithm);
    }

    public String hash(String algorithmName) {
        return FileHasher.hash(path, algorithmName);
    }

    // ── Statics ────────────────────────────────────────────────

    public static Path createTemp(String prefix, String suffix) {
        return FileOperations.createTemp(prefix, suffix);
    }

    public static boolean isSameFile(Path path1, Path path2) {
        return FileOperations.isSameFile(path1, path2);
    }

    public static String getExtension(String fileName) {
        return FileAttributes.extractExtension(fileName);
    }

    public static String getMimeType(String path) {
        return FileAttributes.getMimeType(Path.of(path));
    }

    // ── Lazy delegate accessors ───────────────────────────────

    private FileReader reader() {
        if (reader == null) {
            reader = new FileReader(path, charset);
        }
        return reader;
    }

    private FileWriter writer() {
        if (writer == null) {
            writer = new FileWriter(path, charset);
        }
        return writer;
    }

    private FileAttributes attributes() {
        if (attributes == null) {
            attributes = new FileAttributes(path);
        }
        return attributes;
    }

    private FilePermissions permissions() {
        if (permissions == null) {
            permissions = new FilePermissions(path);
        }
        return permissions;
    }

    private FileOperations operations() {
        if (operations == null) {
            operations = new FileOperations(path);
        }
        return operations;
    }

    private FileCompressor compressor() {
        if (compressor == null) {
            compressor = new FileCompressor(path);
        }
        return compressor;
    }

    private FileSearch search() {
        if (search == null) {
            search = new FileSearch(path, charset);
        }
        return search;
    }
}
