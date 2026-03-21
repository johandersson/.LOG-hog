package encryption;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import utils.ProgressCallback;
import filehandling.LogFileFormat;

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
                 // Create secure temp file in the same directory to allow atomic move
                 String prefix = filePath.getFileName().toString();
                 Path tmp = Files.createTempFile(filePath.getParent(), prefix + "-", ".tmp");
                 try (var in = new utils.LinesInputStream(lines, LogFileFormat.INTERNAL_LINE_SEPARATOR, java.nio.charset.StandardCharsets.UTF_8);
                     var out = Files.newOutputStream(tmp)) {

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
                try {
                    java.nio.file.Files.move(tmp, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                } catch (java.nio.file.AtomicMoveNotSupportedException amnse) {
                    java.nio.file.Files.move(tmp, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } else {
                // Fallback: build string and encrypt
                String full = String.join(LogFileFormat.INTERNAL_LINE_SEPARATOR, lines);
                progressDialog = new gui.LoadingProgressDialog(null, "Encrypting");
                progressDialog.setStatus("Encrypting file...");
                progressDialog.setIndeterminate(true);
                progressDialog.show();

                var encryptedData = encryptor.encrypt(full, pwd, salt);
                Path tmp = Files.createTempFile(filePath.getParent(), filePath.getFileName().toString() + "-", ".tmp");
                Files.write(tmp, encryptedData);
                try {
                    java.nio.file.Files.move(tmp, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                } catch (java.nio.file.AtomicMoveNotSupportedException amnse) {
                    java.nio.file.Files.move(tmp, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
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

    /**
     * Helper that opens a decrypted InputStream and passes it to the provided consumer.
     * If the underlying encryptor supports streaming, uses that; otherwise decrypts into
     * memory and supplies a ByteArrayInputStream.
     */
    public void withDecryptedStream(java.io.InputStream encryptedIn, char[] password, byte[] salt, java.util.function.Consumer<java.io.InputStream> consumer) throws EncryptionException {
        if (consumer == null) throw new IllegalArgumentException("consumer cannot be null");
        try {
            if (encryptor instanceof StreamEncryptor) {
                try (var dec = ((StreamEncryptor) encryptor).openDecryptedStream(encryptedIn, password, salt, null)) {
                    consumer.accept(dec);
                }
            } else {
                // Fallback: read all bytes from the provided stream and decrypt to memory then supply stream
                byte[] data;
                try {
                    data = encryptedIn.readAllBytes();
                } catch (NoSuchMethodError nsme) {
                    // Java versions without InputStream.readAllBytes(): fallback to manual read
                    try (var bout = new java.io.ByteArrayOutputStream()) {
                        byte[] buf = new byte[8192];
                        int r;
                        while ((r = encryptedIn.read(buf)) != -1) bout.write(buf, 0, r);
                        data = bout.toByteArray();
                    }
                }

                String decrypted = encryptor.decrypt(data, password);
                try (var bis = new java.io.ByteArrayInputStream(decrypted.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                    consumer.accept(bis);
                }
            }
        } catch (EncryptionException e) {
            throw e;
        } catch (Exception e) {
            throw new EncryptionException("Unable to open decrypted stream in FileEncryptionManager", e);
        }
    }

    public void withDecryptedReader(java.io.InputStream encryptedIn, char[] password, byte[] salt, java.util.function.Consumer<java.io.BufferedReader> consumer) throws EncryptionException {
        if (consumer == null) throw new IllegalArgumentException("consumer cannot be null");
        withDecryptedStream(encryptedIn, password, salt, (in) -> {
            try (var isr = new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8);
                 var br = new java.io.BufferedReader(isr)) {
                consumer.accept(br);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
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
                 Path tmp = Files.createTempFile(filePath.getParent(), filePath.getFileName().toString() + "-", ".tmp");
                 try (var in = new java.io.ByteArrayInputStream(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                     var out = Files.newOutputStream(tmp)) {


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
                try {
                    java.nio.file.Files.move(tmp, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                } catch (java.nio.file.AtomicMoveNotSupportedException amnse) {
                    java.nio.file.Files.move(tmp, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } else {
                // Fallback to byte[] API with indeterminate progress
                progressDialog = new gui.LoadingProgressDialog(null, "Encrypting");
                progressDialog.setStatus("Encrypting file...");
                progressDialog.setIndeterminate(true);
                progressDialog.show();

                var encryptedData = encryptor.encrypt(content, pwd, salt);
                Path tmp = Files.createTempFile(filePath.getParent(), filePath.getFileName().toString() + "-", ".tmp");
                Files.write(tmp, encryptedData);
                try {
                    java.nio.file.Files.move(tmp, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                } catch (java.nio.file.AtomicMoveNotSupportedException amnse) {
                    java.nio.file.Files.move(tmp, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
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
        try {
            long size = -1;
            try {
                size = java.nio.file.Files.size(filePath);
            } catch (Exception ignored) {}

            if (size > 0 && size > filehandling.ResourceLimits.MAX_DECRYPT_STRING_SIZE) {
                throw new EncryptionException("Refusing to decrypt file into a single String because file is too large (" + size + " bytes). Use streaming APIs like decryptFileToLines() or a streaming consumer to avoid OOM.");
            }

            if (encryptor instanceof StreamEncryptor) {
                try (var in = Files.newInputStream(filePath);
                     var dec = ((StreamEncryptor) encryptor).openDecryptedStream(in, pwd, salt, null);
                     var reader = new java.io.BufferedReader(new java.io.InputStreamReader(dec, java.nio.charset.StandardCharsets.UTF_8))) {

                    StringBuilder sb = new StringBuilder();
                    String line;
                    boolean first = true;
                    while ((line = reader.readLine()) != null) {
                        if (!first) sb.append(LogFileFormat.INTERNAL_LINE_SEPARATOR);
                        sb.append(line);
                        first = false;
                    }
                    return sb.toString();
                }
            } else {
                // Fallback: only allow reading entire file when it's reasonably small
                long fileSize = -1;
                try { fileSize = java.nio.file.Files.size(filePath); } catch (Exception ignored) {}
                if (fileSize > 0 && fileSize > filehandling.ResourceLimits.MAX_DECRYPT_STRING_SIZE) {
                    throw new EncryptionException("Refusing to decrypt large file into memory (" + fileSize + " bytes). Use decryptFileToLines() or streaming APIs instead.");
                }

                byte[] encryptedBytes = Files.readAllBytes(filePath);
                String decrypted = encryptor.decrypt(encryptedBytes, pwd);
                return decrypted;
            }
        } finally {
            // Clear sensitive data
            Arrays.fill(pwd, '\0');
            if (salt != null) {
                Arrays.fill(salt, (byte) 0);
                salt = null;
            }
        }
    }

    /**
     * Stream-decrypt the file and return lines without allocating one giant string
     * when possible. Falls back to full-string decrypt when streaming isn't
     * supported by the underlying encryptor.
     */
    public List<String> decryptFileToLines() throws Exception {
        if (!encrypted || password == null) {
            throw new IllegalStateException("Encryption not set up");
        }

        char[] pwd = password.clone();
        try {
            if (encryptor instanceof StreamEncryptor) {
                try (var in = Files.newInputStream(filePath);
                     var dec = ((StreamEncryptor) encryptor).openDecryptedStream(in, pwd, salt, null);
                     var reader = new java.io.BufferedReader(new java.io.InputStreamReader(dec, java.nio.charset.StandardCharsets.UTF_8))) {
                    List<String> lines = new ArrayList<>();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        lines.add(line);
                    }
                    return lines;
                }
            } else {
                String decrypted = decryptFile();
                return Arrays.asList(decrypted.split("\r?\n", -1));
            }
        } finally {
            Arrays.fill(pwd, '\0');
        }
    }

    /**
     * Clear any sensitive data held by this manager. Used by higher-level handlers.
     */
    public void clearSensitiveData() {
        this.encrypted = false;
        if (this.password != null) {
            Arrays.fill(this.password, '\0');
            this.password = null;
        }
        if (this.salt != null) {
            Arrays.fill(this.salt, (byte) 0);
            this.salt = null;
        }
    }
}