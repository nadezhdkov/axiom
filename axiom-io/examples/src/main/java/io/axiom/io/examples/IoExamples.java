package io.axiom.io.examples;

import io.axiom.io.Directory;
import io.axiom.io.FileHandle;
import io.axiom.io.attribute.FileMetadata;
import io.axiom.io.hash.Sha256Hash;

import java.nio.file.Path;

/** Minimal, compiled-by-CI usage examples for {@code axiom-io}. */
public final class IoExamples {

    private IoExamples() {
    }

    public static void main(String[] args) {
        Path tempDir = FileHandle.createTemp("axiom-io-examples", "").toAbsolutePath().getParent();
        Path file = tempDir.resolve("axiom-io-example.txt");

        FileHandle handle = FileHandle.at(file)
                .createIfNotExists()
                .write("hello")
                .append(" world");

        System.out.println("content: " + handle.readAllText());

        FileMetadata metadata = handle.metadata();
        System.out.println("size: " + metadata.sizeBytes() + " bytes");

        String hash = handle.hash(new Sha256Hash());
        System.out.println("sha256: " + hash);

        Directory.at(tempDir).list().forEach(System.out::println);

        handle.delete();
    }
}
