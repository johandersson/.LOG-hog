package main;

import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Arrays;

public class TamperDetector {
    private byte[] lastKnownHash;

    public void recordBaseline(Path file) throws Exception {
        lastKnownHash = computeHash(file);
    }

    public boolean isTampered(Path file) throws Exception {
        byte[] currentHash = computeHash(file);
        return !Arrays.equals(lastKnownHash, currentHash);
    }

    private byte[] computeHash(Path file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] data = Files.readAllBytes(file);
        return md.digest(data);
    }
}
