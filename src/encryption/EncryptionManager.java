package encryption;



import java.security.SecureRandom;


import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/*
 * Copyright (C) 2025 Johan Andersson
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
 *   <li><b>Key Derivation:</b> PBKDF2 with 600,000 iterations (65,536 for legacy compatibility)</li>
 *   <li><b>IV Generation:</b> Cryptographically secure random 96-bit IV for each encryption operation</li>
 *   <li><b>Authenticated Encryption:</b> GCM provides both confidentiality and integrity</li>
 *   <li><b>Integer Overflow Protection:</b> Uses Math.addExact() for array size calculations</li>
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
 *   <li><b>Key Derivation:</b> PBKDF2 with 600,000 iterations (65,536 for legacy compatibility)</li>
 *   <li><b>IV Generation:</b> Cryptographically secure random 96-bit IV for each encryption operation</li>
 *   <li><b>Authenticated Encryption:</b> GCM provides both confidentiality and integrity</li>
 *   <li><b>Integer Overflow Protection:</b> Uses Math.addExact() for array size calculations</li>
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

            // Wrap output in CipherOutputStream
            java.io.BufferedInputStream bin = new java.io.BufferedInputStream(in);
            javax.crypto.CipherOutputStream cos = new javax.crypto.CipherOutputStream(out, cipher);
            try {
                byte[] buf = new byte[8192];
                int n;
                long total = -1;
                try {
                    if (in instanceof java.io.ByteArrayInputStream) {
                        total = bin.available();
                    }
                } catch (Exception ignored) {}
                if (progress != null && total > 0) progress.setTotalBytes(total + 0);

                long processed = 0;
                while ((n = bin.read(buf)) != -1) {
                    cos.write(buf, 0, n);
                    processed += n;
                    if (progress != null) progress.setProcessedBytes(processed);
                }
                cos.flush();
            } finally {
                cos.close();
            }
        } catch (EncryptionException e) {
            throw e;
        } catch (Exception e) {
            throw new EncryptionException("Unable to encrypt stream", e);
        }
    }

    private byte[] performEncryption(String data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] encrypted = cipher.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        byte[] result = new byte[Math.addExact(iv.length, encrypted.length)];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
        return result; // iv + ciphertext
    }

    public String decrypt(byte[] encryptedData, SecretKey key) throws EncryptionException {
        validateEncryptedData(encryptedData);

        try {
            return performDecryption(encryptedData, key);
        } catch (EncryptionException e) {
            // Re-throw our custom exceptions as-is
            throw e;
        } catch (javax.crypto.AEADBadTagException e) {
            throw new EncryptionException("Unable to open your file. This usually means:\n• Your password might be incorrect\n• The file may have been damaged during transfer or storage\n• You might be trying to open a file created with an older version of LogHog\n\nIf you're sure your password is correct, the file may be corrupted and you should restore from a backup.", e);
        } catch (javax.crypto.BadPaddingException e) {
            throw new EncryptionException("Unable to open this file. It may be corrupted or use an incompatible format. Please check if this is the correct file or try restoring from a backup.", e);
        } catch (javax.crypto.IllegalBlockSizeException e) {
            throw new EncryptionException("Unable to open this file. It may be corrupted or use an incompatible format. Please check if this is the correct file or try restoring from a backup.", e);
        } catch (java.security.InvalidKeyException e) {
            throw new EncryptionException("Unable to process the encryption key. This may be a system compatibility issue.", e);
        } catch (java.security.InvalidAlgorithmParameterException e) {
            throw new EncryptionException("Unable to initialize encryption parameters. This may be a system compatibility issue.", e);
        } catch (Exception e) {
            throw new EncryptionException("Unable to open this file due to an unexpected error. Please try again or contact support.", e);
        }
    }

    @Override
    public String decryptStream(java.io.InputStream in, char[] password, byte[] salt) throws EncryptionException {
        // Stream-based decryption to avoid reading the entire encrypted file into memory first.
        try {
            // Read the full stream via CipherInputStream — Implementation will attempt to
            // detect and strip a salt prefix if present, similar to decryptWithFallback.
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

            // Read initial bytes to check for salt prefix
            java.io.BufferedInputStream bin = new java.io.BufferedInputStream(in);
            bin.mark(16);
            byte[] prefix = new byte[16];
            int read = bin.read(prefix);
            boolean startsWithSalt = false;
            if (read == 16 && salt != null && salt.length == 16) {
                startsWithSalt = true;
                for (int i = 0; i < 16; i++) {
                    if (prefix[i] != salt[i]) {
                        startsWithSalt = false;
                        break;
                    }
                }
            }
            if (!startsWithSalt) {
                // Reset to stream start
                bin.reset();
            }

            // If startsWithSalt is true, we've already consumed the salt bytes; the stream
            // is now positioned after the salt. Otherwise it's at the beginning.

            // Derive key and set up cipher
            SecretKey key = deriveKey(password, salt);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            // Need to read IV from stream: IV length is GCM_IV_LENGTH
            byte[] iv = new byte[GCM_IV_LENGTH];
            int got = bin.read(iv);
            if (got != GCM_IV_LENGTH) {
                throw new EncryptionException("Encrypted data missing IV or is corrupted.");
            }
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            javax.crypto.CipherInputStream cis = new javax.crypto.CipherInputStream(bin, cipher);
            try {
                byte[] buf = new byte[8192];
                int n;
                while ((n = cis.read(buf)) != -1) {
                    baos.write(buf, 0, n);
                }
            } finally {
                cis.close();
            }

            byte[] decrypted = baos.toByteArray();
            try {
                String result = new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
                return result;
            } finally {
                encryption.CryptoUtils.zeroize(decrypted);
            }
        } catch (EncryptionException e) {
            throw e;
        } catch (Exception e) {
            throw new EncryptionException("Unable to decrypt stream.", e);
        }
    }

    @Override
    public java.io.InputStream openDecryptedStream(java.io.InputStream encryptedIn, char[] password, byte[] salt, utils.ProgressCallback progress) throws EncryptionException {
        if (encryptedIn == null) throw new EncryptionException("Encrypted input cannot be null");
        if (password == null) throw new EncryptionException("Password cannot be null");

        try {
            java.io.BufferedInputStream bin = new java.io.BufferedInputStream(encryptedIn);
            bin.mark(64);
            byte[] hdr = new byte[FILE_MAGIC.length];
            int got = bin.read(hdr);
            boolean hasHeader = false;
            if (got == FILE_MAGIC.length) {
                hasHeader = true;
                for (int i = 0; i < FILE_MAGIC.length; i++) {
                    if (hdr[i] != FILE_MAGIC[i]) { hasHeader = false; break; }
                }
            }

            if (hasHeader) {
                // Read version
                int version = bin.read();
                utils.Log.debug(() -> "Encryption header version: " + version);
                int saltLen = bin.read();
                if (saltLen < 0) throw new EncryptionException("Truncated header: missing salt length");
                byte[] saltBytes = new byte[saltLen];
                int r = bin.read(saltBytes);
                if (r != saltLen) throw new EncryptionException("Truncated header: missing salt");
                int ivLen = bin.read();
                if (ivLen != GCM_IV_LENGTH) throw new EncryptionException("Unexpected IV length in header");
                byte[] iv = new byte[ivLen];
                r = bin.read(iv);
                if (r != ivLen) throw new EncryptionException("Truncated header: missing IV");

                SecretKey key = deriveKey(password, saltBytes);
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
                cipher.init(Cipher.DECRYPT_MODE, key, spec);
                return new javax.crypto.CipherInputStream(bin, cipher);
            } else {
                // Fallback: reset and attempt legacy salt-prefix detection using provided salt
                bin.reset();
                bin.mark(32);
                byte[] prefix = new byte[16];
                int read = bin.read(prefix);
                boolean startsWithSalt = false;
                if (read == 16 && salt != null && salt.length == 16) {
                    startsWithSalt = true;
                    for (int i = 0; i < 16; i++) {
                        if (prefix[i] != salt[i]) {
                            startsWithSalt = false;
                            break;
                        }
                    }
                }
                if (!startsWithSalt) {
                    bin.reset();
                }

                // Read IV
                byte[] iv = new byte[GCM_IV_LENGTH];
                int gotIv = bin.read(iv);
                if (gotIv != GCM_IV_LENGTH) {
                    throw new EncryptionException("Encrypted data missing IV or is corrupted.");
                }

                SecretKey key = deriveKey(password, salt);
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
                cipher.init(Cipher.DECRYPT_MODE, key, spec);

                return new javax.crypto.CipherInputStream(bin, cipher);
            }
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
        java.io.InputStream dec = openDecryptedStream(encryptedIn, password, salt, null);
        try {
            try {
                consumer.accept(dec);
            } finally {
                if (dec != null) dec.close();
            }
        } catch (RuntimeException e) {
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
        java.io.InputStream dec = openDecryptedStream(encryptedIn, password, salt, null);
        java.io.InputStreamReader isr = null;
        java.io.BufferedReader br = null;
        try {
            isr = new java.io.InputStreamReader(dec, java.nio.charset.StandardCharsets.UTF_8);
            br = new java.io.BufferedReader(isr);
            consumer.accept(br);
        } catch (Exception e) {
            throw new EncryptionException("Error while processing decrypted reader.", e);
        } finally {
            try { if (br != null) br.close(); } catch (Exception ignored) {}
            try { if (isr != null) isr.close(); } catch (Exception ignored) {}
            try { if (dec != null) dec.close(); } catch (Exception ignored) {}
        }
    }

    private void validateEncryptedData(byte[] encryptedData) throws EncryptionException {
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
    public String decryptWithFallback(byte[] data, char... passwordAndSalt) throws EncryptionException {
        if (passwordAndSalt == null || passwordAndSalt.length < 1) {
            throw new EncryptionException("Password is required.");
        }
        // If 17+ chars, last 16 bytes are salt
        if (passwordAndSalt.length > 16) {
            char[] password = new char[passwordAndSalt.length - 16];
            char[] saltChars = new char[16];
            System.arraycopy(passwordAndSalt, 0, password, 0, password.length);
            System.arraycopy(passwordAndSalt, password.length, saltChars, 0, 16);
            byte[] salt = new byte[16];
            for (int i = 0; i < 16; i++) salt[i] = (byte) saltChars[i];
            try {
                return decrypt(data, password);
            } catch (EncryptionException e) {
                try {
                    javax.crypto.SecretKey key = deriveKey(password, salt);
                    return performDecryption(data, key);
                } catch (Exception ex) {
                    throw e;
                }
            }
        } else {
            return decrypt(data, passwordAndSalt);
        }
    }

    @Override
    public String decryptStream(java.io.InputStream in, char... passwordAndSalt) throws EncryptionException {
        if (passwordAndSalt == null || passwordAndSalt.length < 1) {
            throw new EncryptionException("Password is required.");
        }
        if (passwordAndSalt.length > 16) {
            char[] password = new char[passwordAndSalt.length - 16];
            char[] saltChars = new char[16];
            System.arraycopy(passwordAndSalt, 0, password, 0, password.length);
            System.arraycopy(passwordAndSalt, password.length, saltChars, 0, 16);
            byte[] salt = new byte[16];
            for (int i = 0; i < 16; i++) salt[i] = (byte) saltChars[i];
            return decryptStream(in, password, salt);
        } else {
            return decryptStream(in, passwordAndSalt, null);
        }
    }

    // ...existing code...
}

