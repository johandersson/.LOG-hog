package encryption;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import filehandling.LogFileFormat;
import security.BackupKeyDerivation;
import utils.ProgressCallback;

/**
 * Handles file encryption and decryption operations.
 */
public class FileEncryptionManager {

    private static final char[] EMPTY_PASSWORD = new char[0];
    private static final int BACKUP_HMAC_SIZE_BYTES = 32;

    private final Path filePath;
    private final Encryptor encryptor;
    private byte[] sessionKeyBytes;
    private byte[] backupHmacKeyBytes;
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
        boolean showProgress = !java.awt.GraphicsEnvironment.isHeadless();
        Path tmp = utils.SecureTempFiles.createSecureTempFile(filePath.getParent(), filePath.getFileName().toString() + "-", ".tmp", true);
        boolean completed = false;
        try (var in = new utils.LinesInputStream(toEncrypt, LogFileFormat.INTERNAL_LINE_SEPARATOR, java.nio.charset.StandardCharsets.UTF_8);
             var out = Files.newOutputStream(tmp)) {

            if (showProgress) {
                progressDialog = new gui.LoadingProgressDialog(null, "Encrypting");
                progressDialog.setStatus("Encrypting file...");
                progressDialog.setIndeterminate(false);
            }

            long total = 0;
            for (String line : toEncrypt) {
                total += line.getBytes(java.nio.charset.StandardCharsets.UTF_8).length
                    + LogFileFormat.INTERNAL_LINE_SEPARATOR.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            }
            ProgressCallback cb = null;
            if (progressDialog != null) {
                progressDialog.setTotalBytes(total);
                progressDialog.show();
                final gui.LoadingProgressDialog dlg = progressDialog;
                cb = new ProgressCallback() {
                    @Override
                    public void setTotalBytes(long bytes) {
                        javax.swing.SwingUtilities.invokeLater(() -> dlg.setTotalBytes(bytes));
                    }

                    @Override
                    public void setProcessedBytes(long bytes) {
                        javax.swing.SwingUtilities.invokeLater(() -> dlg.setProcessedBytes(bytes));
                    }
                };
            }

            sessionEncryptor.encryptStream(in, out, activeSessionKey, salt, cb);
            completed = true;
        } finally {
            if (progressDialog != null) {
                progressDialog.close();
            }
            finishTempWrite(tmp, completed);
        }
    }

    public FileEncryptionManager duplicateFor(Path targetPath) {
        FileEncryptionManager duplicate = new FileEncryptionManager(targetPath, encryptor);
        duplicate.encrypted = this.encrypted;
        duplicate.sessionKeyBytes = this.sessionKeyBytes != null ? this.sessionKeyBytes.clone() : null;
        duplicate.backupHmacKeyBytes = this.backupHmacKeyBytes != null ? this.backupHmacKeyBytes.clone() : null;
        duplicate.salt = this.salt != null ? this.salt.clone() : null;
        return duplicate;
    }

    public void setEncryption(char[] pwd, byte[] slt) throws EncryptionException {
        clearSessionKey();
        clearBackupHmacKey();
        SecretKey derivedKey = encryptor.deriveKey(pwd, slt);
        byte[] encoded = derivedKey.getEncoded();
        if (encoded == null || encoded.length == 0) {
            throw new EncryptionException("Unable to derive a usable session key.");
        }
        this.sessionKeyBytes = encoded.clone();
        CryptoUtils.zeroize(encoded);
        this.backupHmacKeyBytes = BackupKeyDerivation.deriveV2(pwd, slt);
        this.salt = slt.clone();
        this.encrypted = true;
    }

    public void disableEncryption() {
        this.encrypted = false;
        clearSessionKey();
        clearBackupHmacKey();
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
        return EMPTY_PASSWORD;
    }

    public byte[] getSalt() {
        return salt != null ? salt.clone() : null;
    }

    /**
     * Stream-decrypt the file and return lines without allocating one giant string
     * when possible.
     */
    public List<String> decryptFileToLines() throws Exception {
        try {
            return decryptLinesFromStream(Files.newInputStream(filePath));
        } catch (EncryptionException primary) {
            byte[] sanitized = null;
            try {
                sanitized = readSanitizedBackupBytes();
                if (sanitized == null) {
                    throw primary;
                }
                return decryptLinesFromStream(new java.io.ByteArrayInputStream(sanitized));
            } catch (EncryptionException secondary) {
                throw primary;
            } finally {
                CryptoUtils.zeroize(sanitized);
            }
        }
    }

    /**
     * Clear any sensitive data held by this manager. Used by higher-level handlers.
     */
    public void clearSensitiveData() {
        this.encrypted = false;
        clearSessionKey();
        clearBackupHmacKey();
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
            try {
                int r;
                while ((r = dec.read(buf)) != -1) {
                    out.write(buf, 0, r);
                }
            } finally {
                CryptoUtils.zeroize(buf);
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
        if (!encrypted || sessionKeyBytes == null) {
            throw new IllegalStateException("Encryption not set up");
        }
        return new SecretKeySpec(sessionKeyBytes, "AES");
    }

    private void clearSessionKey() {
        if (this.sessionKeyBytes == null) {
            return;
        }
        CryptoUtils.zeroize(this.sessionKeyBytes);
        this.sessionKeyBytes = null;
    }

    private void clearBackupHmacKey() {
        if (this.backupHmacKeyBytes == null) {
            return;
        }
        CryptoUtils.zeroize(this.backupHmacKeyBytes);
        this.backupHmacKeyBytes = null;
    }

    private List<String> decryptLinesFromStream(java.io.InputStream encryptedIn) throws Exception {
        try (var in = encryptedIn;
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

    private byte[] readSanitizedBackupBytes() {
        if (backupHmacKeyBytes == null || !Files.exists(filePath)) {
            return null;
        }

        byte[] all = null;
        byte[] payload = null;
        byte[] hmac = null;
        byte[] expected = null;
        try {
            all = Files.readAllBytes(filePath);
            if (all.length <= BACKUP_HMAC_SIZE_BYTES) {
                return null;
            }
            int payloadLen = all.length - BACKUP_HMAC_SIZE_BYTES;
            payload = Arrays.copyOf(all, payloadLen);
            hmac = Arrays.copyOfRange(all, payloadLen, all.length);
            expected = computeHmacSha256(backupHmacKeyBytes, payload);
            if (!MessageDigest.isEqual(expected, hmac)) {
                return null;
            }
            return payload;
        } catch (Exception e) {
            return null;
        } finally {
            if (all != null) {
                CryptoUtils.zeroize(all);
            }
            if (payload != null && (expected == null || hmac == null || !MessageDigest.isEqual(expected, hmac))) {
                CryptoUtils.zeroize(payload);
            }
            CryptoUtils.zeroize(hmac);
            CryptoUtils.zeroize(expected);
        }
    }

    private byte[] computeHmacSha256(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(key, "HmacSHA256");
        mac.init(keySpec);
        return mac.doFinal(data);
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