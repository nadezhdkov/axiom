package io.axiom.io.hash;

/** Strategy for computing a hex-encoded digest of file content. */
public sealed interface HashAlgorithm permits Md5Hash, Sha256Hash {

    String algorithmName();

    String hash(byte[] data);
}
