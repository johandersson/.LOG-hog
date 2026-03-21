package encryption;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import utils.ProgressCallback;

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

    /**
     * Encrypt content provided as lines to avoid building large byte arrays.
     */
    public void encryptFileFromLines(java.util.List<String> lines) throws Exception {
        if (!encrypted || password == null) {
            throw new IllegalStateException("Encryption not set up");
        }

        // Ensure header
        if (lines == null) throw new IllegalArgumentException("Lines cannot be null");
        if (lines.isEmpty() || !lines.get(0).trim().equalsIgnoreCase(".LOG")) {
            java.util.List<String> withHeader = new java.util.ArrayList<>();
            withHeader.add(".LOG");
            withHeader.add("");
            withHeader.addAll(lines);
            lines = withHeader;
        }

        char[] pwd = password.clone();
        gui.LoadingProgressDialog progressDialog = null;
        try {
            // Use streaming encryptor if available
            if (encryptor instanceof StreamEncryptor) {
                try (var in = new utils.LinesInputStream(lines, LogFileFormat.INTERNAL_LINE_SEPARATOR, java.nio.charset.StandardCharsets.UTF_8);
                     var out = Files.newOutputStream(filePath)) {

                    progressDialog = new gui.LoadingProgressDialog(null, "Encrypting");
                    progressDialog.setStatus("Encrypting file...");
                    progressDialog.setIndeterminate(false);

                    // set total bytes estimate by summing lengths (safe and avoids single allocation)
                    long total = 0;
                    for (String l : lines) total += l.getBytes(java.nio.charset.StandardCharsets.UTF_8).length + LogFileFormat.INTERNAL_LINE_SEPARATOR.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                    progressDialog.setTotalBytes(total);
                    progressDialog.show();

                    final gui.LoadingProgressDialog dlg = progressDialog;
                    ProgressCallback cb = new ProgressCallback() {
                        @Override
                        public void setTotalBytes(long bytes) {
                            javax.swing.SwingUtilities.invokeLater(() -> dlg.setTotalBytes(bytes));
                        }

                        @Override
                        public void setProcessedBytes(long bytes) {
                            javax.swing.SwingUtilities.invokeLater(() -> dlg.setProcessedBytes(bytes));
                        }
                    };

                    ((StreamEncryptor) encryptor).encryptStream(in, out, pwd, salt, cb);
                }
            } else {
                // Fallback: build string and encrypt
                String full = String.join(LogFileFormat.INTERNAL_LINE_SEPARATOR, lines);
                progressDialog = new gui.LoadingProgressDialog(null, "Encrypting");
                progressDialog.setStatus("Encrypting file...");
                progressDialog.setIndeterminate(true);
                progressDialog.show();

                var encryptedData = encryptor.encrypt(full, pwd, salt);
                Files.write(filePath, encryptedData);
            }
        } finally {
            Arrays.fill(pwd, '\0');
            if (progressDialog != null) {
                progressDialog.close();
            }
        }
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
        gui.LoadingProgressDialog progressDialog = null;
        try {
            // Try to stream encryption when the encryptor supports it to allow progress reporting
            if (encryptor instanceof StreamEncryptor) {
                try (var in = new java.io.ByteArrayInputStream(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                     var out = Files.newOutputStream(filePath)) {


                    progressDialog = new gui.LoadingProgressDialog(null, "Encrypting");
                    progressDialog.setStatus("Encrypting file...");
                    progressDialog.setIndeterminate(false);
                    progressDialog.setTotalBytes(content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
                    progressDialog.show();

                    final gui.LoadingProgressDialog dlg = progressDialog;

                    ProgressCallback cb = new ProgressCallback() {
                        @Override
                        public void setTotalBytes(long bytes) {
                            javax.swing.SwingUtilities.invokeLater(() -> dlg.setTotalBytes(bytes));
                        }

                        @Override
                        public void setProcessedBytes(long bytes) {
                            javax.swing.SwingUtilities.invokeLater(() -> dlg.setProcessedBytes(bytes));
                        }
                    };

                    ((StreamEncryptor) encryptor).encryptStream(in, out, pwd, salt, cb);
                }
            } else {
                // Fallback to byte[] API with indeterminate progress
                progressDialog = new gui.LoadingProgressDialog(null, "Encrypting");
                progressDialog.setStatus("Encrypting file...");
                progressDialog.setIndeterminate(true);
                progressDialog.show();

                var encryptedData = encryptor.encrypt(content, pwd, salt);
                Files.write(filePath, encryptedData);
            }
        } finally {
            // Always clear password copy from memory
            Arrays.fill(pwd, '\0');
            if (progressDialog != null) {
                progressDialog.close();
            }
        }
    }

    public String decryptFile() throws Exception {
        if (!encrypted || password == null) {
            throw new IllegalStateException("Encryption not set up");
        }

        // Use password then immediately clear it from memory
        char[] pwd = password.clone();
                    if (encryptor instanceof StreamEncryptor) {
                        // Fall through to new streaming API when encryptor supports it
                        encryptFileFromLines(java.util.Arrays.asList(content.split("\r?\n", -1)));
                        return;
                    } else {
        }
        if (salt != null) {
            Arrays.fill(salt, (byte) 0);
            salt = null;
        }
    }
}