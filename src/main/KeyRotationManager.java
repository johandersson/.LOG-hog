package main;

import java.nio.file.*;
import java.util.List;
import javax.crypto.SecretKey;
import encryption.EncryptionManager;

public class KeyRotationManager {
    private final EncryptionManager encryptionManager;

    public KeyRotationManager(EncryptionManager manager) {
        this.encryptionManager = manager;
    }

    public void rotateKey(Path file, char[] oldPassword, char[] newPassword, byte[] salt) throws Exception {
        byte[] encrypted = Files.readAllBytes(file);
        String plaintext = encryptionManager.decrypt(encrypted, oldPassword, salt);
        byte[] newEncrypted = encryptionManager.encrypt(plaintext, newPassword, salt);
        Files.write(file, newEncrypted, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
