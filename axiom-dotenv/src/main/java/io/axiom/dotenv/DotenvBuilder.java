package io.axiom.dotenv;

import io.axiom.dotenv.internal.DotenvLineParser;
import io.axiom.placeholder.PlaceholderResolver;
import io.axiom.placeholder.PlaceholderSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a {@link Dotenv} instance by parsing a {@code .env} file. Never a static global — each
 * call to {@link #load()} produces a fresh, explicit, user-owned {@link Dotenv}.
 */
public final class DotenvBuilder {

    private String filename = ".env";
    private String directory = ".";
    private boolean throwIfMissing = false;
    private boolean strict = false;
    private boolean interpolate = true;
    private boolean exportToSystemProperties = false;
    private String profile;
    private boolean reloadable = false;

    public DotenvBuilder filename(String filename) {
        this.filename = filename;
        return this;
    }

    public DotenvBuilder directory(String directory) {
        this.directory = directory;
        return this;
    }

    /** Throws {@link DotenvException} on {@link #load()} if the file does not exist. */
    public DotenvBuilder throwIfMissing() {
        this.throwIfMissing = true;
        return this;
    }

    public DotenvBuilder ignoreIfMissing() {
        this.throwIfMissing = false;
        return this;
    }

    /** Throws {@link DotenvException} on any line that doesn't match {@code KEY=value}. */
    public DotenvBuilder strict() {
        this.strict = true;
        return this;
    }

    /** Disables {@code ${OTHER_KEY:default}} interpolation of parsed values; they are used verbatim. */
    public DotenvBuilder withoutInterpolation() {
        this.interpolate = false;
        return this;
    }

    /** Also copies every resolved file entry into {@link System#setProperty}. */
    public DotenvBuilder exportToSystemProperties() {
        this.exportToSystemProperties = true;
        return this;
    }

    /**
     * Sets the active profile: after the base file loads, {@code <filename>.<profile>} (e.g.
     * {@code .env.dev}) is also read from the same directory, if present, and its entries
     * override the base file's for the same key. Also makes {@link Dotenv#activeProfile()}
     * report this value, which {@link Profile @Profile}-annotated targets are checked against.
     */
    public DotenvBuilder profile(String profile) {
        this.profile = profile;
        return this;
    }

    /**
     * Makes {@link #load()} return a {@link Dotenv} whose {@link Dotenv#reload()} actually
     * re-reads the file(s) from disk, instead of throwing {@link UnsupportedOperationException}.
     */
    public DotenvBuilder reloadable() {
        this.reloadable = true;
        return this;
    }

    public Dotenv load() {
        DotenvContext snapshot = loadSnapshot();
        return reloadable ? new ReloadableDotenv(this, snapshot) : snapshot;
    }

    DotenvContext loadSnapshot() {
        Map<String, String> fileValues = readFileValues(filename);
        if (profile != null && !profile.isBlank()) {
            fileValues.putAll(readFileValues(filename + "." + profile));
        }

        Map<String, String> resolvedValues = interpolate ? interpolate(fileValues) : fileValues;

        if (exportToSystemProperties) {
            resolvedValues.forEach(System::setProperty);
        }

        return new DotenvContext(resolvedValues, System.getenv(), profile);
    }

    private Map<String, String> readFileValues(String name) {
        Path path = Path.of(directory, name);
        List<String> lines = readLines(path, name);
        List<DotenvEntry> rawEntries = DotenvLineParser.parse(path.toString(), lines, strict);

        Map<String, String> values = new LinkedHashMap<>();
        for (DotenvEntry entry : rawEntries) {
            values.put(entry.key(), entry.value());
        }
        return values;
    }

    private Map<String, String> interpolate(Map<String, String> fileValues) {
        PlaceholderSource source = PlaceholderSource.of(fileValues).orElse(PlaceholderSource.environment());
        PlaceholderResolver resolver = PlaceholderResolver.of(source);

        Map<String, String> resolved = new LinkedHashMap<>();
        fileValues.forEach((key, value) -> resolved.put(key, resolver.resolve(value)));
        return resolved;
    }

    private List<String> readLines(Path path, String name) {
        try {
            return Files.readAllLines(path);
        } catch (IOException e) {
            if (throwIfMissing && name.equals(filename)) {
                throw new DotenvException("Failed to read .env file: " + path, e);
            }
            return List.of();
        }
    }
}
