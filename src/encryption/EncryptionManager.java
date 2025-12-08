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

import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionManager {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final int PBKDF2_ITERATIONS = 100000;
    private static final int AES_KEY_LENGTH = 256; // bits
    private static void logToFile(String message) {
        try (FileWriter fw = new FileWriter("debug.log", true)) {
            fw.write(message + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static byte[] generateSalt() {
        var salt = new byte[16];
        var random = new SecureRandom();
        random.nextBytes(salt);
        return salt;
    }

    public static SecretKey deriveKey(char[] password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        var spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, AES_KEY_LENGTH);
        var tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    public static byte[] encrypt(String data, SecretKey key) throws Exception {
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
    }

    public static String decrypt(byte[] encryptedData, SecretKey key) throws Exception {
        var cipher = Cipher.getInstance(ALGORITHM);
        var iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encryptedData, 0, iv, 0, iv.length);
        var encrypted = new byte[encryptedData.length - iv.length];
        System.arraycopy(encryptedData, iv.length, encrypted, 0, encrypted.length);
        var spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        var decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
    }
}
