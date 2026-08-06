package io.axiom.io.search;

import io.axiom.io.exception.FileReadException;
import io.axiom.io.exception.FileWriteException;
import io.axiom.io.stream.FileReader;
import io.axiom.io.stream.FileWriter;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class FileSearch {

    private final Path path;
    private final Charset charset;

    public FileSearch(Path path, Charset charset) {
        this.path = path;
        this.charset = charset;
    }

    public List<String> filter(Predicate<String> predicate) {
        try (Stream<String> lines = new FileReader(path, charset).lines()) {
            return lines.filter(predicate).toList();
        } catch (java.io.UncheckedIOException e) {
            throw new FileReadException(path, e.getCause());
        }
    }

    public void filterAndSave(Predicate<String> predicate, String targetPath) {
        List<String> matches = filter(predicate);
        new FileWriter(Path.of(targetPath), charset).writeLines(matches);
    }

    public List<String> grep(String regex) {
        Pattern pattern = Pattern.compile(regex);
        return filter(line -> pattern.matcher(line).find());
    }

    public void replaceAll(String regex, String replacement) {
        String text = new FileReader(path, charset).readAllText();
        new FileWriter(path, charset).write(text.replaceAll(regex, replacement));
    }

    public void processLines(Consumer<String> consumer) {
        try (Stream<String> lines = new FileReader(path, charset).lines()) {
            lines.forEach(consumer);
        }
    }

    public long countLines() {
        try (Stream<String> lines = new FileReader(path, charset).lines()) {
            return lines.count();
        }
    }

    public long count(String searchString) {
        try (Stream<String> lines = new FileReader(path, charset).lines()) {
            return lines.mapToLong(line -> countOccurrences(line, searchString)).sum();
        }
    }

    public boolean contentEquals(String otherPath) {
        try {
            byte[] a = Files.readAllBytes(path);
            byte[] b = Files.readAllBytes(Path.of(otherPath));
            return java.util.Arrays.equals(a, b);
        } catch (Exception e) {
            return false;
        }
    }

    private static long countOccurrences(String line, String searchString) {
        if (searchString.isEmpty()) {
            return 0;
        }
        long count = 0;
        int index = 0;
        while ((index = line.indexOf(searchString, index)) != -1) {
            count++;
            index += searchString.length();
        }
        return count;
    }
}
