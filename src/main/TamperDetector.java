package main;

import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class TamperDetector {
    private byte[] lastKnownHash;

    public void recordBaseline(Path file) throws java.io.IOException {
        lastKnownHash = computeHash(file);
    }

    public boolean isTampered(Path file) throws java.io.IOException {
        byte[] currentHash = computeHash(file);
        return !Arrays.equals(lastKnownHash, currentHash);
    }

    private byte[] computeHash(Path file) throws java.io.IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] data = Files.readAllBytes(file);
            return md.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", e);
        }
    }
}
