package io.axiom.io.hash;

import io.axiom.io.exception.FileHashException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class FileHasher {

    private FileHasher() {
    }

    public static String hash(Path path, HashAlgorithm algorithm) {
        try {
            byte[] data = Files.readAllBytes(path);
            return algorithm.hash(data);
        } catch (IOException e) {
            throw new FileHashException(path, algorithm.algorithmName(), e);
        }
    }

    public static String hash(Path path, String algorithmName) {
        try {
            byte[] data = Files.readAllBytes(path);
            return HexFormat.of().formatHex(MessageDigest.getInstance(algorithmName).digest(data));
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new FileHashException(path, algorithmName, e);
        }
    }
}
