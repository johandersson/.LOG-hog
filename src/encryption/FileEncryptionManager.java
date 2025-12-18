package encryption;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import javax.crypto.SecretKey;

/**
 * Handles file encryption and decryption operations.
 */
public class FileEncryptionManager {

    private final Path filePath;
    private char[] password;
    private byte[] salt;
    private boolean encrypted;

    public FileEncryptionManager(Path filePath) {
        this.filePath = filePath;
    }

    public void setEncryption(char[] pwd, byte[] slt) {
        // Clear old sensitive data
        if (this.password != null) {
            Arrays.fill(this.password, '\0');
        }
        this.password = pwd.clone();
        this.salt = slt.clone();
        this.encrypted = true;
    }

    public void disableEncryption() {
        this.encrypted = false;
        if (this.password != null) {
            Arrays.fill(this.password, '\0');
            this.password = null;
        }
        this.salt = null;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public char[] getPassword() {
        return password;
    }

    public byte[] getSalt() {
        return salt;
    }

    public void encryptFile(String content) throws Exception {
        if (!encrypted) return;

        // Ensure .LOG header
        if (!content.startsWith(".LOG")) {
            content = ".LOG\n\n" + content;
        }

        SecretKey key = EncryptionManager.deriveKey(password, salt);
        byte[] encryptedData = EncryptionManager.encrypt(content, key);
        Files.write(filePath, encryptedData);
    }

    public String decryptFile() throws Exception {
        if (!encrypted) return new String(Files.readAllBytes(filePath));

        byte[] data = Files.readAllBytes(filePath);
        return EncryptionManager.decryptWithFallback(data, password, salt);
    }

    public void clearSensitiveData() {
        if (password != null) {
            Arrays.fill(password, '\0');
            password = null;
        }
        if (salt != null) {
            Arrays.fill(salt, (byte) 0);
            salt = null;
        }
    }
}