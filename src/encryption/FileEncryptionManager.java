package encryption;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.SecretKey;

import filehandling.LogFileFormat;
import utils.ProgressCallback;

/**
 * Handles file encryption and decryption operations.
 */
public class FileEncryptionManager {

    private final Path filePath;
    private final Encryptor encryptor;
    private SecretKey sessionKey;
    private byte[] salt;
    private boolean encrypted;

    public FileEncryptionManager(Path filePath, Encryptor encryptor) {
        this.filePath = filePath;
        this.encryptor = encryptor;
    }

    /**
     * Encrypt content provided as lines to avoid building large byte arrays.
     */
    public void encryptFileFromLines(List<String> lines) throws Exception {
        if (lines == null) throw new IllegalArgumentException("Lines cannot be null");

        List<String> toEncrypt = lines;
        if (toEncrypt.isEmpty() || !".LOG".equalsIgnoreCase(toEncrypt.get(0).trim())) {
            List<String> withHeader = new ArrayList<>();
            withHeader.add(".LOG");
            withHeader.add("");
            withHeader.addAll(toEncrypt);
            toEncrypt = withHeader;
        }

        SessionKeyEncryptor sessionEncryptor = requireSessionEncryptor();
        SecretKey activeSessionKey = requireSessionKey();
        gui.LoadingProgressDialog progressDialog = null;
        Path tmp = utils.SecureTempFiles.createSecureTempFile(filePath.getParent(), filePath.getFileName().toString() + "-", ".tmp", true);
        boolean completed = false;
        try (var in = new utils.LinesInputStream(toEncrypt, LogFileFormat.INTERNAL_LINE_SEPARATOR, java.nio.charset.StandardCharsets.UTF_8);
             var out = Files.newOutputStream(tmp)) {

            progressDialog = new gui.LoadingProgressDialog(null, "Encrypting");
            progressDialog.setStatus("Encrypting file...");
            progressDialog.setIndeterminate(false);

            long total = 0;
            for (String line : toEncrypt) {
                total += line.getBytes(java.nio.charset.StandardCharsets.UTF_8).length
                    + LogFileFormat.INTERNAL_LINE_SEPARATOR.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            }
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

            sessionEncryptor.encryptStream(in, out, activeSessionKey, salt, cb);
            completed = true;
        } finally {
            if (progressDialog != null) {
                progressDialog.close();
            }
            finishTempWrite(tmp, completed);
        }
    }

    public void setEncryption(char[] pwd, byte[] slt) throws EncryptionException {
        clearSessionKey();
        this.sessionKey = encryptor.deriveKey(pwd, slt);
        this.salt = slt.clone();
        this.encrypted = true;
    }

    public void disableEncryption() {
        this.encrypted = false;
        clearSessionKey();
        this.salt = null;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void withDecryptedStream(java.io.InputStream encryptedIn, java.util.function.Consumer<java.io.InputStream> consumer) throws EncryptionException {
        if (consumer == null) throw new IllegalArgumentException("consumer cannot be null");
        try (var dec = requireSessionEncryptor().openDecryptedStream(encryptedIn, requireSessionKey(), null)) {
            consumer.accept(dec);
        } catch (EncryptionException e) {
            throw e;
        } catch (Exception e) {
            throw new EncryptionException("Unable to open decrypted stream in FileEncryptionManager", e);
        }
    }

    public void withDecryptedReader(java.io.InputStream encryptedIn, java.util.function.Consumer<java.io.BufferedReader> consumer) throws EncryptionException {
        if (consumer == null) throw new IllegalArgumentException("consumer cannot be null");
        withDecryptedStream(encryptedIn, in -> {
            try (var isr = new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8);
                 var br = new java.io.BufferedReader(isr)) {
                consumer.accept(br);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public char[] getPassword() {
        return null;
    }

    public byte[] getSalt() {
        return salt != null ? salt.clone() : null;
    }

    public void encryptFile(String content) throws Exception {
        String toEncrypt = content != null ? content : "";
        if (!toEncrypt.startsWith(".LOG")) {
            toEncrypt = ".LOG\n\n" + toEncrypt;
        }

        byte[] plaintextBytes = toEncrypt.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        gui.LoadingProgressDialog progressDialog = null;
        Path tmp = utils.SecureTempFiles.createSecureTempFile(filePath.getParent(), filePath.getFileName().toString() + "-", ".tmp", true);
        boolean completed = false;
        try (var in = new java.io.ByteArrayInputStream(plaintextBytes);
             var out = Files.newOutputStream(tmp)) {
            progressDialog = new gui.LoadingProgressDialog(null, "Encrypting");
            progressDialog.setStatus("Encrypting file...");
            progressDialog.setIndeterminate(false);
            progressDialog.setTotalBytes(plaintextBytes.length);
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

            requireSessionEncryptor().encryptStream(in, out, requireSessionKey(), salt, cb);
            completed = true;
        } finally {
            CryptoUtils.zeroize(plaintextBytes);
            if (progressDialog != null) {
                progressDialog.close();
            }
            finishTempWrite(tmp, completed);
        }
    }

    public String decryptFile() throws Exception {
        long size = -1;
        try {
            size = Files.size(filePath);
        } catch (Exception ignored) {}

        if (size > 0 && size > filehandling.ResourceLimits.MAX_DECRYPT_STRING_SIZE) {
            throw new EncryptionException("Refusing to decrypt file into a single String because file is too large (" + size + " bytes). Use streaming APIs like decryptFileToLines() or a streaming consumer to avoid OOM.");
        }

        try (var in = Files.newInputStream(filePath);
             var dec = requireSessionEncryptor().openDecryptedStream(in, requireSessionKey(), null);
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
    }

    /**
     * Stream-decrypt the file and return lines without allocating one giant string
     * when possible.
     */
    public List<String> decryptFileToLines() throws Exception {
        try (var in = Files.newInputStream(filePath);
             var dec = requireSessionEncryptor().openDecryptedStream(in, requireSessionKey(), null);
             var reader = new java.io.BufferedReader(new java.io.InputStreamReader(dec, java.nio.charset.StandardCharsets.UTF_8))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        }
    }

    /**
     * Clear any sensitive data held by this manager. Used by higher-level handlers.
     */
    public void clearSensitiveData() {
        this.encrypted = false;
        clearSessionKey();
        if (this.salt != null) {
            Arrays.fill(this.salt, (byte) 0);
            this.salt = null;
        }
    }

    /**
     * Decrypts the encrypted file to the specified output file, setting secure permissions on the output.
     * Uses streaming APIs if available.
     */
    public void decryptFileTo(Path outputPath) throws Exception {
        try (var in = Files.newInputStream(filePath);
             var out = Files.newOutputStream(outputPath);
             var dec = requireSessionEncryptor().openDecryptedStream(in, requireSessionKey(), null)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = dec.read(buf)) != -1) {
                out.write(buf, 0, r);
            }
        }
        CryptoUtils.setOwnerOnlyPermissions(outputPath);
    }

    private SessionKeyEncryptor requireSessionEncryptor() {
        if (!(encryptor instanceof SessionKeyEncryptor sessionEncryptor)) {
            throw new IllegalStateException("Encryptor does not support session keys");
        }
        return sessionEncryptor;
    }

    private SecretKey requireSessionKey() {
        if (!encrypted || sessionKey == null) {
            throw new IllegalStateException("Encryption not set up");
        }
        return sessionKey;
    }

    private void clearSessionKey() {
        if (this.sessionKey == null) {
            return;
        }
        try {
            byte[] encoded = this.sessionKey.getEncoded();
            CryptoUtils.zeroize(encoded);
        } catch (Exception ignored) {
        }
        this.sessionKey = null;
    }

    private void finishTempWrite(Path tmp, boolean completed) throws Exception {
        if (!completed) {
            try {
                if (tmp != null && Files.exists(tmp)) {
                    Files.deleteIfExists(tmp);
                }
            } catch (Exception ignored) {
            }
            return;
        }
        try {
            Files.move(tmp, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException amnse) {
            Files.move(tmp, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } finally {
            try {
                if (tmp != null && Files.exists(tmp)) {
                    Files.deleteIfExists(tmp);
                }
            } catch (Exception ignored) {
            }
        }
    }
}