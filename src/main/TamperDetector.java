package main;

import java.nio.file.Path;
import java.util.Arrays;

import security.StreamingHash;

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
        return StreamingHash.sha256(file);
    }
}
