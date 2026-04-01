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
        // Use decryptWithFallback to support both new and legacy formats
        // Compose passwordAndSalt varargs for compatibility
        char[] passwordAndSalt;
        if (oldPassword != null && salt != null && salt.length == 16) {
            passwordAndSalt = new char[oldPassword.length + 16];
            System.arraycopy(oldPassword, 0, passwordAndSalt, 0, oldPassword.length);
            for (int i = 0; i < 16; i++) passwordAndSalt[oldPassword.length + i] = (char) salt[i];
        } else {
            passwordAndSalt = oldPassword;
        }
        String plaintext = encryptionManager.decryptWithFallback(encrypted, passwordAndSalt);
        byte[] newEncrypted = encryptionManager.encrypt(plaintext, newPassword, salt);
        Files.write(file, newEncrypted, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
