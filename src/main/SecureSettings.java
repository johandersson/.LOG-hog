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

package main;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Secure settings storage for sensitive configuration data.
 * Uses PBKDF2 for key derivation instead of single SHA-256 hash.
 * Uses deterministic salt to avoid salt bootstrapping issues.
 * Updated to use AES/GCM instead of insecure AES/ECB mode.
 */
public class SecureSettings {
    private static final String ENCRYPTED_PREFIX = "encrypted:";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final int PBKDF2_ITERATIONS = 10000; // Lower than file encryption for performance
    private final SecretKeySpec settingsKey;
    private final SecretKeySpec legacyKey; // For decrypting old SHA-256 encrypted settings
    private final SecureRandom secureRandom;

    public SecureSettings() {
        // Generate deterministic key for settings encryption using PBKDF2
        // Based on username + app identifier to ensure consistency across sessions
        String username = System.getProperty("user.name", "default");
        String appId = "LogHog_Settings_v2"; // v2 to differentiate from old SHA-256 version
        String keySeed = username + "_" + appId;

        try {
            // Use PBKDF2 instead of single SHA-256 hash for better security
            // Deterministic salt derived from app identifier
            byte[] salt = "LogHog_Settings_Salt_v2".getBytes(StandardCharsets.UTF_8);
            KeySpec spec = new PBEKeySpec(keySeed.toCharArray(), salt, PBKDF2_ITERATIONS, 128);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            SecretKey tmp = factory.generateSecret(spec);
            this.settingsKey = new SecretKeySpec(tmp.getEncoded(), "AES");
            
            // Also initialize legacy SHA-256 key for backward compatibility
            // This allows decrypting settings encrypted with the old method
            String legacyKeySeed = username + "_LogHog_Settings_v1";
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] legacyKeyBytes = digest.digest(legacyKeySeed.getBytes(StandardCharsets.UTF_8));
            this.legacyKey = new SecretKeySpec(legacyKeyBytes, 0, 16, "AES");
            
            this.secureRandom = new SecureRandom();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize secure settings", e);
        }
    }

    /**
     * Encrypt a sensitive value for storage in settings using AES/GCM.
     */
    public String encryptValue(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        try {
            // Generate random IV for GCM
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, settingsKey, spec);
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            
            // Prepend IV to encrypted data
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            
            return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            // Security: Never return plaintext on encryption failure
            // Log error for debugging but don't expose details
            System.err.println("WARNING: Failed to encrypt setting value - returning empty string");
            return ""; // Return empty string instead of plaintext
        }
    }

    /**
     * Decrypt a value from settings, handling both GCM and legacy ECB encrypted values.
     */
    public String decryptValue(String storedValue) {
        if (storedValue == null) {
            return null;
        }

        if (storedValue.startsWith(ENCRYPTED_PREFIX)) {
            // Encrypted value
            try {
                String encryptedData = storedValue.substring(ENCRYPTED_PREFIX.length());
                byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
                
                // Try ECB first for backward compatibility (most existing data)
                // ECB data doesn't have IV prefix, so try this first
                // Try new PBKDF2 key first, then legacy SHA-256 key
                try {
                    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                    cipher.init(Cipher.DECRYPT_MODE, settingsKey);
                    byte[] decrypted = cipher.doFinal(encryptedBytes);
                    return new String(decrypted, StandardCharsets.UTF_8);
                } catch (Exception ecbException) {
                    // Try with legacy key (old SHA-256 derivation)
                    try {
                        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                        cipher.init(Cipher.DECRYPT_MODE, legacyKey);
                        byte[] decrypted = cipher.doFinal(encryptedBytes);
                        // Successfully decrypted with legacy key - re-encrypt with new key
                        String value = new String(decrypted, StandardCharsets.UTF_8);
                        // Note: Auto-migration happens when setting is next saved
                        return value;
                    } catch (Exception legacyException) {
                        // Not ECB format with either key, try GCM
                    }
                }
                
                // Try GCM (new format with IV prefix)
                if (encryptedBytes.length >= GCM_IV_LENGTH + GCM_TAG_LENGTH) {
                    byte[] iv = new byte[GCM_IV_LENGTH];
                    System.arraycopy(encryptedBytes, 0, iv, 0, GCM_IV_LENGTH);
                    byte[] encrypted = new byte[encryptedBytes.length - GCM_IV_LENGTH];
                    System.arraycopy(encryptedBytes, GCM_IV_LENGTH, encrypted, 0, encrypted.length);
                    
                    Cipher cipher = Cipher.getInstance(ALGORITHM);
                    GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
                    cipher.init(Cipher.DECRYPT_MODE, settingsKey, spec);
                    byte[] decrypted = cipher.doFinal(encrypted);
                    return new String(decrypted, StandardCharsets.UTF_8);
                }
                
                // If we get here, decryption failed
                System.err.println("Failed to decrypt setting value - unknown format");
                return storedValue;
            } catch (Exception e) {
                System.err.println("Failed to decrypt setting value");
                // Return the encrypted value as-is if decryption fails
                return storedValue;
            }
        } else {
            // Plain text value (backwards compatibility)
            return storedValue;
        }
    }

    /**
     * Set an encrypted property in the settings.
     */
    public void setEncryptedProperty(Properties settings, String key, String value) {
        settings.setProperty(key, encryptValue(value));
    }

    /**
     * Get a decrypted property from settings.
     */
    public String getDecryptedProperty(Properties settings, String key) {
        return getDecryptedProperty(settings, key, null);
    }

    /**
     * Get a decrypted property from settings with default value.
     */
    public String getDecryptedProperty(Properties settings, String key, String defaultValue) {
        String storedValue = settings.getProperty(key);
        if (storedValue == null) {
            return defaultValue;
        }
        return decryptValue(storedValue);
    }
}