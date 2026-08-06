package io.axiom.io.attribute;

import io.axiom.io.exception.FileWriteException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Set;

public final class FilePermissions {

    private final Path path;

    public FilePermissions(Path path) {
        this.path = path;
    }

    public String getOwner() {
        try {
            return Files.getOwner(path).getName();
        } catch (IOException e) {
            throw new FileWriteException("Failed to read owner of: " + path, e, path);
        }
    }

    public void setOwner(String username) {
        try {
            UserPrincipal principal = path.getFileSystem().getUserPrincipalLookupService()
                    .lookupPrincipalByName(username);
            Files.setOwner(path, principal);
        } catch (IOException e) {
            throw new FileWriteException(path, e);
        }
    }

    public Set<PosixFilePermission> getPosixPermissions() {
        try {
            return Files.getPosixFilePermissions(path);
        } catch (IOException e) {
            throw new FileWriteException(path, e);
        }
    }

    public void setPosixPermissions(Set<PosixFilePermission> permissions) {
        try {
            Files.setPosixFilePermissions(path, permissions);
        } catch (IOException e) {
            throw new FileWriteException(path, e);
        }
    }

    public void setReadOnly() {
        path.toFile().setWritable(false);
    }

    public void setWritable(boolean writable) {
        path.toFile().setWritable(writable);
    }

    public void setExecutable(boolean executable) {
        path.toFile().setExecutable(executable);
    }
}
