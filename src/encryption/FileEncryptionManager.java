package encryption;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Handles file encryption and decryption operations.
 */
public class FileEncryptionManager {

    private final Path filePath;
    private final Encryptor encryptor;
    private char[] password;
    private byte[] salt;
    private boolean encrypted;

    public FileEncryptionManager(Path filePath, Encryptor encryptor) {
        this.filePath = filePath;
        this.encryptor = encryptor;
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
        // Return a clone to prevent external modification of the password
        return password != null ? password.clone() : null;
    }

    public byte[] getSalt() {
        // Security: Return defensive copy to prevent external modification
        return salt != null ? salt.clone() : null;
    }

    public void encryptFile(String content) throws Exception {
        if (!encrypted || password == null) {
            throw new IllegalStateException("Encryption not set up");
        }

        // Ensure .LOG header
        if (!content.startsWith(".LOG")) {
            content = ".LOG\n\n" + content;
        }

        // Use password then immediately clear it from memory
        char[] pwd = password.clone();
        try {
            var encryptedData = encryptor.encrypt(content, pwd, salt);
            Files.write(filePath, encryptedData);
        } finally {
            // Always clear password copy from memory
            Arrays.fill(pwd, '\0');
        }
    }

    public String decryptFile() throws Exception {
        if (!encrypted || password == null) {
            throw new IllegalStateException("Encryption not set up");
        }

        // Use password then immediately clear it from memory
        char[] pwd = password.clone();
        try {
            // Protect against DoS / memory exhaustion by checking file size first
            try {
                if (Files.exists(filePath) && Files.size(filePath) > filehandling.ResourceLimits.MAX_FILE_SIZE) {
                    String shortTitle = "Encrypted File Too Large";
                    String longMessage = "The encrypted log file is larger than the allowed limit (" + (filehandling.ResourceLimits.MAX_FILE_SIZE / (1024 * 1024)) + " MB).\n\n" +
                        "Decrypting large files may use a lot of memory and could expose sensitive data in memory.";
                    filehandling.DialogHandler.showLimitExceeded(shortTitle, longMessage);
                    throw new IllegalStateException("Encrypted file too large to decrypt safely");
                }
            } catch (java.io.IOException ioe) {
                // If size check fails, let the underlying read throw a clearer exception
            }

            try (java.io.InputStream in = Files.newInputStream(filePath)) {
                return encryptor.decryptStream(in, pwd, salt);
            }
        } finally {
            // Always clear password copy from memory
            Arrays.fill(pwd, '\0');
        }
    }

    public void clearSensitiveData() {
        this.encrypted = false;
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