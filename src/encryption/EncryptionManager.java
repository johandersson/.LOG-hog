
package encryption;

import java.security.SecureRandom;
import java.util.Arrays;

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
public class EncryptionManager implements StreamEncryptor {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final int PBKDF2_ITERATIONS = 600000;
    private static final int PBKDF2_ITERATIONS_LEGACY = 65536; // For backward compatibility
    private static final int AES_KEY_LENGTH = 256; // bits

    private static final EncryptionManager INSTANCE = new EncryptionManager();

    public static EncryptionManager getInstance() {
        return INSTANCE;
    }

    @Override
    public byte[] generateSalt() throws EncryptionException {
        try {
            var salt = new byte[16];
            var random = new SecureRandom();
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
            var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, AES_KEY_LENGTH);
            var tmp = factory.generateSecret(spec);
            byte[] raw = tmp.getEncoded();
            try {
                return new SecretKeySpec(raw, "AES");
            } finally {
                if (raw != null) Arrays.fill(raw, (byte)0);
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

    public SecretKey deriveKeyLegacy(char[] password, byte[] salt) throws EncryptionException {
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
            var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS_LEGACY, AES_KEY_LENGTH);
            var tmp = factory.generateSecret(spec);
            byte[] raw = tmp.getEncoded();
            try {
                return new SecretKeySpec(raw, "AES");
            } finally {
                if (raw != null) Arrays.fill(raw, (byte)0);
            }
        } catch (Exception e) {
            throw new EncryptionException("Unable to process your password with legacy settings. This may be a compatibility issue with older encrypted files.", e);
        } finally {
            if (spec != null) {
                try {
                    spec.clearPassword();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public byte[] encryptLegacy(String data, SecretKey key) throws EncryptionException {
        if (data == null) {
            throw new EncryptionException("Data to encrypt cannot be null.");
        }
        if (key == null) {
            throw new EncryptionException("Encryption key cannot be null.");
        }

        try {
            return performEncryption(data, key);
        } catch (Exception e) {
            throw new EncryptionException("Unable to encrypt your data. Please try again or contact support if the problem persists.", e);
        }
    }
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
            // Return salt + encrypted - use Math.addExact to prevent integer overflow
            byte[] result = new byte[Math.addExact(salt.length, encrypted.length)];
            System.arraycopy(salt, 0, result, 0, salt.length);
            System.arraycopy(encrypted, 0, result, salt.length, encrypted.length);
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
            // Write salt prefix
            out.write(salt);

            SecretKey key = deriveKey(password, salt);
            var cipher = Cipher.getInstance(ALGORITHM);
            var iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            var spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            // Write IV
            out.write(iv);

            // Wrap output in CipherOutputStream
            try (var cos = new javax.crypto.CipherOutputStream(out, cipher);
                 var bin = new java.io.BufferedInputStream(in)) {

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
            }
        } catch (EncryptionException e) {
            throw e;
        } catch (Exception e) {
            throw new EncryptionException("Unable to encrypt stream", e);
        }
    }

    private byte[] performEncryption(String data, SecretKey key) throws Exception {
        var cipher = Cipher.getInstance(ALGORITHM);
        var iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        var spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        var encrypted = cipher.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var result = new byte[Math.addExact(iv.length, encrypted.length)];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
        return result;
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
            in = new java.io.BufferedInputStream(in);
            in.mark(16);
            byte[] prefix = new byte[16];
            int read = in.read(prefix);
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
                in.reset();
            }

            // If startsWithSalt is true, we've already consumed the salt bytes; the stream
            // is now positioned after the salt. Otherwise it's at the beginning.

            // Derive key and set up cipher
            SecretKey key = deriveKey(password, salt);
            var cipher = Cipher.getInstance(ALGORITHM);
            // Need to read IV from stream: IV length is GCM_IV_LENGTH
            byte[] iv = new byte[GCM_IV_LENGTH];
            int got = in.read(iv);
            if (got != GCM_IV_LENGTH) {
                throw new EncryptionException("Encrypted data missing IV or is corrupted.");
            }
            var spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            try (var cis = new javax.crypto.CipherInputStream(in, cipher)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = cis.read(buf)) != -1) {
                    baos.write(buf, 0, n);
                }
            }

            byte[] decrypted = baos.toByteArray();
            try {
                String result = new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
                return result;
            } finally {
                Arrays.fill(decrypted, (byte)0);
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
            java.io.BufferedInputStream in = new java.io.BufferedInputStream(encryptedIn);
            in.mark(16);
            byte[] prefix = new byte[16];
            int read = in.read(prefix);
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
                in.reset();
            }

            // Read IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            int got = in.read(iv);
            if (got != GCM_IV_LENGTH) {
                throw new EncryptionException("Encrypted data missing IV or is corrupted.");
            }

            SecretKey key = deriveKey(password, salt);
            var cipher = Cipher.getInstance(ALGORITHM);
            var spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            return new javax.crypto.CipherInputStream(in, cipher);
        } catch (EncryptionException e) {
            throw e;
        } catch (Exception e) {
            throw new EncryptionException("Unable to open decrypted stream.", e);
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

    @Override
    public String decrypt(byte[] data, char[] password) throws EncryptionException {
        if (data == null) {
            throw new EncryptionException("Data to decrypt cannot be null.");
        }
        if (password == null) {
            throw new EncryptionException("Password cannot be null.");
        }

        try {
            // Read salt from the beginning
            if (data.length < 16) {
                throw new EncryptionException("Encrypted data is too short.");
            }
            byte[] salt = new byte[16];
            System.arraycopy(data, 0, salt, 0, 16);
            byte[] encrypted = new byte[data.length - 16];
            System.arraycopy(data, 16, encrypted, 0, encrypted.length);
            SecretKey key = deriveKey(password, salt);
            return performDecryption(encrypted, key);
        } catch (EncryptionException e) {
            throw e;
        } catch (Exception e) {
            throw new EncryptionException("Unable to decrypt your data. Please check your password and try again.", e);
        }
    }

    private String decryptLegacy(byte[] encryptedData, SecretKey key) throws EncryptionException {
        validateEncryptedData(encryptedData);

        try {
            return performDecryption(encryptedData, key);
        } catch (EncryptionException e) {
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

    private String performDecryption(byte[] encryptedData, SecretKey key) throws Exception {
        var cipher = Cipher.getInstance(ALGORITHM);
        var iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encryptedData, 0, iv, 0, iv.length);
        var encrypted = new byte[encryptedData.length - iv.length];
        System.arraycopy(encryptedData, iv.length, encrypted, 0, encrypted.length);
        var spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        var decrypted = cipher.doFinal(encrypted);
        try {
            return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new EncryptionException("The decrypted data contains invalid characters. This usually means the file is corrupted or you're using the wrong password.", e);
        }
    }

    @Override
    public String decryptWithFallback(byte[] encryptedData, char[] password, byte[] salt) throws EncryptionException {
        validateEncryptedDataForFallback(encryptedData);

        try {
            byte[] dataToDecrypt = stripSaltPrefixIfPresent(encryptedData, salt);

            // Try with current iterations first
            SecretKey key = deriveKey(password, salt);
            return decrypt(dataToDecrypt, key);
        } catch (Exception e) {
            try {
                byte[] dataToDecrypt = stripSaltPrefixIfPresent(encryptedData, salt);
                SecretKey legacyKey = deriveKeyLegacy(password, salt);
                return decryptLegacy(dataToDecrypt, legacyKey);
            } catch (Exception legacyException) {
                throw new EncryptionException("Decryption failed: invalid password or corrupted file", legacyException);
            }
        }
    }

    private byte[] stripSaltPrefixIfPresent(byte[] encryptedData, byte[] salt) {
        if (salt == null || salt.length != 16 || encryptedData == null || encryptedData.length <= 16) {
            return encryptedData;
        }
        boolean startsWithSalt = true;
        for (int i = 0; i < 16; i++) {
            if (encryptedData[i] != salt[i]) {
                startsWithSalt = false;
                break;
            }
        }
        if (startsWithSalt) {
            return Arrays.copyOfRange(encryptedData, 16, encryptedData.length);
        }
        return encryptedData;
    }

    private void validateEncryptedDataForFallback(byte[] encryptedData) throws EncryptionException {
        if (encryptedData == null || encryptedData.length == 0) {
            throw new EncryptionException("Cannot open an empty file. Please check if your log file contains any data.");
        }
        if (encryptedData.length < GCM_IV_LENGTH + GCM_TAG_LENGTH) {
            throw new EncryptionException("This file appears to be damaged or uses an incompatible format. It doesn't contain enough data to be a valid LogHog file. Please check if this is the correct file or try restoring from a backup.");
        }
    }
}

