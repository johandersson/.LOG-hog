package encryption;



import java.security.SecureRandom;


import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/*
 * Copyright (C) 2026 Johan Andersson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

/**
 * AES/GCM encryption manager for secure file encryption.
 * Provides authenticated encryption with PBKDF2 key derivation.
 *
 * <h2>Security Properties</h2>
 * <ul>
 *   <li><b>Encryption Algorithm:</b> AES/GCM with 256-bit key and 128-bit authentication tag</li>
 *   <li><b>Key Derivation:</b> PBKDF2 with 600,000 iterations </li>
 *   <li><b>IV Generation:</b> Cryptographically secure random 96-bit IV for each encryption operation</li>
 *   <li><b>Authenticated Encryption:</b> GCM provides both confidentiality and integrity</li>
 * </ul>
 *
 * <h2>Security Assumptions</h2>
 * <ul>
 *   <li>PBKDF2 parameters provide adequate protection against brute force attacks</li>
 *   <li>System SecureRandom provides sufficient entropy for cryptographic operations</li>
 *   <li>Encrypted files are stored in locations accessible only to authorized users</li>
 *   <li>Memory containing keys and plaintext is properly managed by the JVM</li>
 *   <li>Users choose strong passwords that resist dictionary attacks</li>
 * </ul>

import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES/GCM encryption manager for secure file encryption.
 * Provides authenticated encryption with PBKDF2 key derivation.
 *
 * <h2>Security Properties</h2>
 * <ul>
 *   <li><b>Encryption Algorithm:</b> AES/GCM with 256-bit key and 128-bit authentication tag</li>
 *   <li><b>Key Derivation:</b> PBKDF2 with 600,000 iterations </li>
 *   <li><b>IV Generation:</b> Cryptographically secure random 96-bit IV for each encryption operation</li>
 *   <li><b>Authenticated Encryption:</b> GCM provides both confidentiality and integrity</li>
 * </ul>
 *
 * <h2>Security Assumptions</h2>
 * <ul>
 *   <li>PBKDF2 parameters provide adequate protection against brute force attacks</li>
 *   <li>System SecureRandom provides sufficient entropy for cryptographic operations</li>
 *   <li>Encrypted files are stored in locations accessible only to authorized users</li>
 *   <li>Memory containing keys and plaintext is properly managed by the JVM</li>
 *   <li>Users choose strong passwords that resist dictionary attacks</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. The singleton instance can be safely used from multiple threads.</p>
 */
public class EncryptionManager implements encryption.StreamEncryptor {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final int PBKDF2_ITERATIONS = 600000;
    private static final int AES_KEY_LENGTH = 256; // bits

    private static final EncryptionManager INSTANCE = new EncryptionManager();
    private static final byte[] FILE_MAGIC = new byte[] { 'L', 'O', 'G', 'H' };
    private static final byte FILE_VERSION = 1;

    public static EncryptionManager getInstance() {
        return INSTANCE;
    }

    @Override
    public byte[] generateSalt() throws EncryptionException {
        try {
            byte[] salt = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(salt);
            return salt;
        } catch (Exception e) {
            throw new EncryptionException("Unable to prepare encryption security settings. This is a system error.", e);
        }
    }

    @Override
    public SecretKey deriveKey(char[] password, byte[] salt) throws EncryptionException {
        if (password == null) {
            throw new EncryptionException("Password cannot be null.");
        }
        if (password.length == 0) {
            throw new EncryptionException("Password cannot be empty.");
        }
        if (salt == null) {
            throw new EncryptionException("Salt cannot be null.");
        }
        if (salt.length != 16) {
            throw new EncryptionException("Salt must be 16 bytes long.");
        }

        PBEKeySpec spec = null;
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, AES_KEY_LENGTH);
            SecretKey tmp = factory.generateSecret(spec);
            byte[] raw = tmp.getEncoded();
            try {
                return new SecretKeySpec(raw, "AES");
            } finally {
                if (raw != null) java.util.Arrays.fill(raw, (byte)0);
            }
        } catch (Exception e) {
            throw new EncryptionException("Unable to process your password. Please check your password and try again.", e);
        } finally {
            if (spec != null) {
                try {
                    spec.clearPassword();
                } catch (Exception ignored) {
                }
            }
        }
    }

    // ...existing code...
    @Override
    public byte[] encrypt(String data, char[] password, byte[] salt) throws EncryptionException {
        if (data == null) {
            throw new EncryptionException("Data to encrypt cannot be null.");
        }
        if (password == null) {
            throw new EncryptionException("Password cannot be null.");
        }
        if (salt == null) {
            throw new EncryptionException("Salt cannot be null.");
        }

        try {
            SecretKey key = deriveKey(password, salt);
            byte[] encrypted = performEncryption(data, key);
            // Build header: MAGIC(4) + VERSION(1) + saltLen(1) + salt + ivLen(1) + iv + ciphertext
            // Note: performEncryption returns iv + ciphertext
            int ivLen = GCM_IV_LENGTH;
            int saltLen = salt.length;
            int headerLen = FILE_MAGIC.length + 1 + 1 + saltLen + 1 + ivLen;
            byte[] result = new byte[headerLen + (encrypted.length - ivLen)];
            int pos = 0;
            System.arraycopy(FILE_MAGIC, 0, result, pos, FILE_MAGIC.length); pos += FILE_MAGIC.length;
            result[pos++] = FILE_VERSION;
            result[pos++] = (byte) (saltLen & 0xFF);
            System.arraycopy(salt, 0, result, pos, saltLen); pos += saltLen;
            result[pos++] = (byte) (ivLen & 0xFF);
            // copy IV from encrypted
            System.arraycopy(encrypted, 0, result, pos, ivLen); pos += ivLen;
            // copy ciphertext
            System.arraycopy(encrypted, ivLen, result, pos, encrypted.length - ivLen);
            return result;
        } catch (Exception e) {
            throw new EncryptionException("Unable to encrypt your data. Please try again or contact support if the problem persists.", e);
        }
    }

    @Override
    public void encryptStream(java.io.InputStream in, java.io.OutputStream out, char[] password, byte[] salt, utils.ProgressCallback progress) throws EncryptionException {
        if (in == null) throw new EncryptionException("Input stream cannot be null");
        if (out == null) throw new EncryptionException("Output stream cannot be null");
        if (password == null) throw new EncryptionException("Password cannot be null");
        if (salt == null) throw new EncryptionException("Salt cannot be null");

        try {
            // Write header: MAGIC, version, salt length, salt, iv length, iv
            SecretKey key = deriveKey(password, salt);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            out.write(FILE_MAGIC);
            out.write(FILE_VERSION);
            out.write((byte) (salt.length & 0xFF));
            out.write(salt);
            out.write((byte) (iv.length & 0xFF));
            out.write(iv);

            // Wrap output in CipherOutputStream - use try-with-resources
            try (java.io.BufferedInputStream bin = new java.io.BufferedInputStream(in);
                 javax.crypto.CipherOutputStream cos = new javax.crypto.CipherOutputStream(out, cipher)) {
                byte[] buf = new byte[8192];
                int n;
                long total = -1;
                try {
                    if (in instanceof java.io.ByteArrayInputStream) {
                        total = bin.available();
                    }
                } catch (Exception ignored) {}
                if (progress != null && total > 0) progress.setTotalBytes(total + 0);
                while ((n = bin.read(buf)) != -1) {
                    cos.write(buf, 0, n);
                }
            }
        } catch (Exception e) {
            throw new EncryptionException("Unable to encrypt stream.", e);
        }
    }

    @Override
    public String decryptStream(java.io.InputStream in, char... passwordAndSalt) throws EncryptionException {
        // Remove duplicate implementation. Use openDecryptedStream for actual stream decryption.
        char[] password;
        byte[] salt = null;
        if (passwordAndSalt != null && passwordAndSalt.length > 16) {
            password = new char[passwordAndSalt.length - 16];
            char[] saltChars = new char[16];
            System.arraycopy(passwordAndSalt, 0, password, 0, password.length);
            System.arraycopy(passwordAndSalt, password.length, saltChars, 0, 16);
            salt = new byte[16];
            for (int i = 0; i < 16; i++) salt[i] = (byte) saltChars[i];
        } else {
            password = passwordAndSalt;
        }
        try (java.io.InputStream dec = openDecryptedStream(in, password, salt, null)) {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = dec.read(buffer)) != -1) {
                baos.write(buffer, 0, n);
            }
            return new String(baos.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (EncryptionException e) {
            throw e;
        } catch (Exception e) {
            throw new EncryptionException("Unable to open decrypted stream.", e);
        }
    }

    /**
     * Convenience helper: open a decrypted InputStream and pass it to the given consumer.
     * Ensures the stream is closed after the consumer returns.
     */
    public void withDecryptedStream(java.io.InputStream encryptedIn, char[] password, byte[] salt, java.util.function.Consumer<java.io.InputStream> consumer) throws EncryptionException {
        if (consumer == null) throw new IllegalArgumentException("consumer cannot be null");
        try (java.io.InputStream dec = openDecryptedStream(encryptedIn, password, salt, null)) {
            consumer.accept(dec);
        } catch (RuntimeException e) {
            throw e;
        } catch (EncryptionException e) {
            throw e;
        } catch (Exception e) {
            throw new EncryptionException("Error while processing decrypted stream.", e);
        }
    }

    /**
     * Convenience helper: open a decrypted Reader (UTF-8) and pass it to the consumer.
     */
    public void withDecryptedReader(java.io.InputStream encryptedIn, char[] password, byte[] salt, java.util.function.Consumer<java.io.BufferedReader> consumer) throws EncryptionException {
        if (consumer == null) throw new IllegalArgumentException("consumer cannot be null");
        try (java.io.InputStream dec = openDecryptedStream(encryptedIn, password, salt, null);
             java.io.InputStreamReader isr = new java.io.InputStreamReader(dec, java.nio.charset.StandardCharsets.UTF_8);
             java.io.BufferedReader br = new java.io.BufferedReader(isr)) {
            consumer.accept(br);
        } catch (EncryptionException e) {
            throw e;
        } catch (Exception e) {
            throw new EncryptionException("Error while processing decrypted reader.", e);
        }
    }

    // Validate encrypted data before decryption - called by decrypt methods
    void validateEncryptedData(byte[] encryptedData) throws EncryptionException {
        if (encryptedData == null) {
            throw new EncryptionException("Cannot decrypt null data. Please check if your file exists and is readable.");
        }
        if (encryptedData.length == 0) {
            throw new EncryptionException("Cannot decrypt empty data. Please check if your file contains any content.");
        }
        if (encryptedData.length < GCM_IV_LENGTH + GCM_TAG_LENGTH) {
            throw new EncryptionException("This file appears to be corrupted or incomplete. It doesn't contain enough data to be a valid encrypted LogHog file. Please check if the file was properly saved or restore from a backup.");
        }
    }

    private String performDecryption(byte[] encryptedData, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encryptedData, 0, iv, 0, iv.length);
        byte[] encrypted = new byte[encryptedData.length - iv.length];
        System.arraycopy(encryptedData, iv.length, encrypted, 0, encrypted.length);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        byte[] decrypted = cipher.doFinal(encrypted);
        try {
            return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new EncryptionException("The decrypted data contains invalid characters. This usually means the file is corrupted or you're using the wrong password.", e);
        } finally {
            encryption.CryptoUtils.zeroize(decrypted);
        }
    }

    // Implement varargs interface methods for compatibility
    @Override
    public String decrypt(byte[] data, char... password) throws EncryptionException {
        if (data == null) {
            throw new EncryptionException("Data to decrypt cannot be null.");
        }
        if (password == null) {
            throw new EncryptionException("Password cannot be null.");
        }
        try {
            // Only support new header format
            int pos = 0;
            boolean hasHeader = false;
            if (data.length >= FILE_MAGIC.length + 1) {
                hasHeader = true;
                for (int i = 0; i < FILE_MAGIC.length; i++) {
                    if (data[i] != FILE_MAGIC[i]) { hasHeader = false; break; }
                }
            }
            if (hasHeader) {
                pos += FILE_MAGIC.length;
                int version = data[pos++] & 0xFF;
                utils.Log.debug(() -> "Decrypting data with header version: " + version);
                int saltLen = data[pos++] & 0xFF;
                if (saltLen < 0 || pos + saltLen > data.length) throw new EncryptionException("Truncated header: missing salt");
                byte[] salt = new byte[saltLen];
                System.arraycopy(data, pos, salt, 0, saltLen); pos += saltLen;
                int ivLen = data[pos++] & 0xFF;
                if (ivLen != GCM_IV_LENGTH) throw new EncryptionException("Unexpected IV length in header");
                byte[] encrypted = new byte[(data.length - pos)];
                System.arraycopy(data, pos, encrypted, 0, encrypted.length);
                SecretKey key = deriveKey(password, salt);
                return performDecryption(encrypted, key);
            } else {
                throw new EncryptionException("Unsupported or legacy encrypted data format. Only current format is supported.");
            }
        } catch (EncryptionException e) {
            throw e;
        } catch (Exception e) {
            throw new EncryptionException("Unable to decrypt your data. Please check your password and try again.", e);
        }
    }

    @Override
    public java.io.InputStream openDecryptedStream(java.io.InputStream encryptedIn, char[] password, byte[] salt, utils.ProgressCallback progress) throws EncryptionException {
        if (encryptedIn == null) throw new EncryptionException("Input stream cannot be null");
        if (password == null) throw new EncryptionException("Password cannot be null");
        // salt can be null if reading from header
        try {
            java.io.BufferedInputStream bin = new java.io.BufferedInputStream(encryptedIn);
            
            // Check for LOGH header format
            bin.mark(32);
            byte[] magicCheck = new byte[FILE_MAGIC.length];
            int magicRead = bin.read(magicCheck);
            boolean hasHeader = (magicRead == FILE_MAGIC.length);
            if (hasHeader) {
                for (int i = 0; i < FILE_MAGIC.length; i++) {
                    if (magicCheck[i] != FILE_MAGIC[i]) {
                        hasHeader = false;
                        break;
                    }
                }
            }
            
            byte[] actualSalt = salt;
            if (hasHeader) {
                // Read header: version, salt_len, salt, iv_len
                int version = bin.read() & 0xFF;
                int saltLen = bin.read() & 0xFF;
                if (saltLen <= 0 || saltLen > 64) {
                    throw new EncryptionException("Invalid salt length in header: " + saltLen);
                }
                actualSalt = new byte[saltLen];
                int gotSalt = bin.read(actualSalt);
                if (gotSalt != saltLen) {
                    throw new EncryptionException("Truncated header: missing salt");
                }
                int ivLen = bin.read() & 0xFF;
                if (ivLen != GCM_IV_LENGTH) {
                    throw new EncryptionException("Unexpected IV length in header: " + ivLen);
                }
                // IV will be read below
            } else {
                // No header - reset and use passed salt
                bin.reset();
                if (salt == null) {
                    throw new EncryptionException("Salt cannot be null for files without header");
                }
            }
            
            SecretKey key = deriveKey(password, actualSalt);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            byte[] iv = new byte[GCM_IV_LENGTH];
            int got = bin.read(iv);
            if (got != GCM_IV_LENGTH) {
                throw new EncryptionException("Encrypted data missing IV or is corrupted.");
            }
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            return new javax.crypto.CipherInputStream(bin, cipher);
        } catch (EncryptionException e) {
            throw e;
        } catch (Exception e) {
            throw new EncryptionException("Unable to open decrypted stream.", e);
        }
    }
    // Helper for encrypt(String, ...)
    private byte[] performEncryption(String data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] plaintext = data.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try {
            byte[] ciphertext = cipher.doFinal(plaintext);
            byte[] result = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
            return result;
        } finally {
            CryptoUtils.zeroize(plaintext);
        }
    }

    /** Encrypt from raw bytes (no String conversion, caller zeroizes input). */
    private byte[] performEncryptionFromBytes(byte[] data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] ciphertext = cipher.doFinal(data);
        byte[] result = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
        return result;
    }

    /** Decrypt to raw bytes without String conversion (caller must zeroize the returned array). */
    private byte[] performDecryptionRaw(byte[] encryptedData, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encryptedData, 0, iv, 0, iv.length);
        byte[] encrypted = new byte[encryptedData.length - iv.length];
        System.arraycopy(encryptedData, iv.length, encrypted, 0, encrypted.length);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        return cipher.doFinal(encrypted);
    }

    /**
     * Re-encrypts file data with a new password and salt without ever converting plaintext to String.
     * The intermediate plaintext byte[] is zeroized before returning.
     */
    public byte[] reEncrypt(byte[] data, char[] oldPassword, char[] newPassword, byte[] newSalt) throws EncryptionException {
        if (data == null || oldPassword == null || newPassword == null || newSalt == null)
            throw new EncryptionException("Arguments cannot be null");
        try {
            // Parse existing header
            int pos = 0;
            if (data.length < FILE_MAGIC.length + 3) throw new EncryptionException("Data too short");
            for (int i = 0; i < FILE_MAGIC.length; i++) {
                if (data[i] != FILE_MAGIC[i]) throw new EncryptionException("Invalid file format");
            }
            pos += FILE_MAGIC.length;
            pos++; // version byte
            int saltLen = data[pos++] & 0xFF;
            if (pos + saltLen > data.length) throw new EncryptionException("Truncated header: missing salt");
            byte[] oldSalt = new byte[saltLen];
            System.arraycopy(data, pos, oldSalt, 0, saltLen); pos += saltLen;
            int ivLen = data[pos++] & 0xFF;
            if (ivLen != GCM_IV_LENGTH) throw new EncryptionException("Unexpected IV length");
            byte[] encryptedWithIv = new byte[data.length - pos];
            System.arraycopy(data, pos, encryptedWithIv, 0, encryptedWithIv.length);

            // Decrypt to raw bytes — never converted to String
            SecretKey oldKey = deriveKey(oldPassword, oldSalt);
            byte[] plaintext = performDecryptionRaw(encryptedWithIv, oldKey);
            try {
                // Re-encrypt from raw bytes with new key
                SecretKey newKey = deriveKey(newPassword, newSalt);
                byte[] reEncrypted = performEncryptionFromBytes(plaintext, newKey);
                // Build new header
                int newSaltLen = newSalt.length;
                int headerLen = FILE_MAGIC.length + 1 + 1 + newSaltLen + 1;
                byte[] result = new byte[headerLen + reEncrypted.length];
                int p = 0;
                System.arraycopy(FILE_MAGIC, 0, result, p, FILE_MAGIC.length); p += FILE_MAGIC.length;
                result[p++] = FILE_VERSION;
                result[p++] = (byte)(newSaltLen & 0xFF);
                System.arraycopy(newSalt, 0, result, p, newSaltLen); p += newSaltLen;
                result[p++] = (byte)(GCM_IV_LENGTH & 0xFF);
                System.arraycopy(reEncrypted, 0, result, p, reEncrypted.length);
                return result;
            } finally {
                CryptoUtils.zeroize(plaintext);
            }
        } catch (EncryptionException e) {
            throw e;
        } catch (Exception e) {
            throw new EncryptionException("Key rotation failed", e);
        }
    }

    // ...existing code...
}

