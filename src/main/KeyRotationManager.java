package main;

import java.nio.file.*;
// Unused imports removed for PMD compliance
import encryption.EncryptionManager;

public class KeyRotationManager {
    private final EncryptionManager encryptionManager;

    public KeyRotationManager(EncryptionManager manager) {
        this.encryptionManager = manager;
    }

    public void rotateKey(Path file, char[] oldPassword, char[] newPassword, byte[] salt) throws Exception {
        byte[] encrypted = Files.readAllBytes(file);
        String plaintext = encryptionManager.decrypt(encrypted, oldPassword);
        byte[] newEncrypted = encryptionManager.encrypt(plaintext, newPassword, salt);
        Files.write(file, newEncrypted, StandardOpenOption.TRUNCATE_EXISTING);
        try { encryption.CryptoUtils.setOwnerOnlyPermissions(file); } catch (Exception ignored) {}
    }
}
