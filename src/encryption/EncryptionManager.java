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

package encryption;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionManager implements Encryptor {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final int PBKDF2_ITERATIONS = 100000;
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
            throw new EncryptionException("Unable to generate encryption salt. This is a system error.", e);
        }
    }

    @Override
    public SecretKey deriveKey(char[] password, byte[] salt) throws EncryptionException {
        try {
            var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            var spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, AES_KEY_LENGTH);
            var tmp = factory.generateSecret(spec);
            return new SecretKeySpec(tmp.getEncoded(), "AES");
        } catch (Exception e) {
            throw new EncryptionException("Unable to create encryption key from password. Please check your password and try again.", e);
        }
    }

    public SecretKey deriveKeyLegacy(char[] password, byte[] salt) throws EncryptionException {
        try {
            var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            var spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS_LEGACY, AES_KEY_LENGTH);
            var tmp = factory.generateSecret(spec);
            return new SecretKeySpec(tmp.getEncoded(), "AES");
        } catch (Exception e) {
            throw new EncryptionException("Unable to create legacy encryption key. This may be a compatibility issue with older encrypted files.", e);
        }
    }

    @Override
    public byte[] encrypt(String data, SecretKey key) throws EncryptionException {
        try {
            var cipher = Cipher.getInstance(ALGORITHM);
            var iv = new byte[GCM_IV_LENGTH];
            var random = new SecureRandom();
            random.nextBytes(iv);
            var spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            var encrypted = cipher.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            return result;
        } catch (Exception e) {
            throw new EncryptionException("Unable to encrypt your data. Please try again or contact support if the problem persists.", e);
        }
    }

    public String decrypt(byte[] encryptedData, SecretKey key) throws EncryptionException {
        if (encryptedData == null) {
            throw new EncryptionException("Cannot decrypt null data. Please check if your file exists and is readable.");
        }

        if (encryptedData.length == 0) {
            throw new EncryptionException("Cannot decrypt empty data. Please check if your file contains any content.");
        }

        try {
            var cipher = Cipher.getInstance(ALGORITHM);
            var iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encryptedData, 0, iv, 0, iv.length);
            var encrypted = new byte[encryptedData.length - iv.length];
            System.arraycopy(encryptedData, iv.length, encrypted, 0, encrypted.length);
            var spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            var decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Provide more specific error messages based on the type of failure
            if (encryptedData.length < GCM_IV_LENGTH) {
                throw new EncryptionException("The file appears to be too short to be a valid encrypted LogHog file. Please check if this is the correct file or if it was corrupted.", e);
            } else if (e.getMessage() != null && e.getMessage().contains("Tag mismatch")) {
                throw new EncryptionException("Unable to decrypt your file. Please check:\n• Is your password correct?\n• Was the file corrupted during transfer?\n• Are you trying to open a file created with an older version of LogHog?\n\nIf you're sure the password is correct, the file may be corrupted and you should restore from a backup.", e);
            } else {
                throw new EncryptionException("Unable to decrypt the data. This may indicate file corruption or an incorrect password.", e);
            }
        }
    }

    @Override
    public String decryptWithFallback(byte[] encryptedData, char[] password, byte[] salt) throws EncryptionException {
        if (encryptedData == null || encryptedData.length == 0) {
            throw new EncryptionException("Cannot decrypt an empty file. Please check if your log file contains any data.");
        }

        // Check if data looks like it might be encrypted (minimum viable encrypted data size)
        if (encryptedData.length < GCM_IV_LENGTH) {
            throw new EncryptionException("This file appears to be unencrypted or uses an incompatible encryption format. Please check if you have the correct file or if encryption was disabled for this file.");
        }

        try {
            // Try with current iterations first
            SecretKey key = deriveKey(password, salt);
            return decrypt(encryptedData, key);
        } catch (Exception e) {
            try {
                // Fallback to legacy iterations for backward compatibility
                SecretKey legacyKey = deriveKeyLegacy(password, salt);
                return decrypt(encryptedData, legacyKey);
            } catch (Exception legacyException) {
                // Check the type of exception to provide better error messages
                String originalMessage = e.getMessage();
                String legacyMessage = legacyException.getMessage();

                if (originalMessage != null && originalMessage.contains("Tag mismatch")) {
                    throw new EncryptionException("Unable to decrypt your file. Please check:\n• Is your password correct?\n• Was the file corrupted during transfer?\n• Are you trying to open a file created with an older version of LogHog?\n\nIf you're sure the password is correct, the file may be corrupted and you should restore from a backup.", e);
                } else if (originalMessage != null && originalMessage.contains("too short")) {
                    throw new EncryptionException("This file appears to be corrupted or incomplete. The encrypted data is too short to be valid. Please check if the file was properly saved or restore from a backup.", e);
                } else {
                    throw new EncryptionException("Unable to decrypt your file. Please check:\n• Is your password correct?\n• Was the file corrupted during transfer?\n• Are you trying to open a file created with an older version of LogHog?\n\nIf you're sure the password is correct, the file may be corrupted and you should restore from a backup.", e);
                }
            }
        }
    }
}
