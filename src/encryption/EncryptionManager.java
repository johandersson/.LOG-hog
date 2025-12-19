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
import java.util.Arrays;

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

        try {
            var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            var spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, AES_KEY_LENGTH);
            var tmp = factory.generateSecret(spec);
            return new SecretKeySpec(tmp.getEncoded(), "AES");
        } catch (Exception e) {
            throw new EncryptionException("Unable to process your password. Please check your password and try again.", e);
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

        try {
            var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            var spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS_LEGACY, AES_KEY_LENGTH);
            var tmp = factory.generateSecret(spec);
            return new SecretKeySpec(tmp.getEncoded(), "AES");
        } catch (Exception e) {
            throw new EncryptionException("Unable to process your password with legacy settings. This may be a compatibility issue with older encrypted files.", e);
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
            // Return salt + encrypted
            byte[] result = new byte[salt.length + encrypted.length];
            System.arraycopy(salt, 0, result, 0, salt.length);
            System.arraycopy(encrypted, 0, result, salt.length, encrypted.length);
            return result;
        } catch (Exception e) {
            throw new EncryptionException("Unable to encrypt your data. Please try again or contact support if the problem persists.", e);
        }
    }

    private byte[] performEncryption(String data, SecretKey key) throws Exception {
        var cipher = Cipher.getInstance(ALGORITHM);
        var iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        var spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        var encrypted = cipher.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var result = new byte[iv.length + encrypted.length];
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

