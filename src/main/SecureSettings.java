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
 * Uses PBKDF2 for key derivation with per-user random salt.
 * Updated to use AES/GCM instead of insecure AES/ECB mode.
 * Maintains backwards compatibility with static salt (v2) and SHA-256 (v1) encrypted values.
 *
 * <h2>Security Properties</h2>
 * <ul>
 *   <li><b>Encryption Algorithm:</b> AES/GCM with 128-bit key and 128-bit authentication tag</li>
 *   <li><b>Key Derivation:</b> PBKDF2 with 100,000 iterations and random salt per user</li>
 *   <li><b>IV Generation:</b> Cryptographically secure random IV for each encryption operation</li>
 *   <li><b>Backwards Compatibility:</b> Supports decryption of legacy encrypted values</li>
 *   <li><b>Integer Overflow Protection:</b> Uses Math.addExact() for array size calculations</li>
 * </ul>
 *
 * <h2>Security Assumptions</h2>
 * <ul>
 *   <li>PBKDF2 parameters provide adequate protection against brute force attacks</li>
 *   <li>System SecureRandom provides sufficient entropy for cryptographic operations</li>
 *   <li>Settings file is stored in a location accessible only to the user</li>
 *   <li>Memory containing decrypted values is properly managed by the JVM</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is not thread-safe. Instances should not be shared between threads.</p>
 */
public class SecureSettings {
    private static final String ENCRYPTED_PREFIX = "encrypted:";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final int PBKDF2_ITERATIONS = 100000; // Increased to match file encryption
    private static final String SETTINGS_SALT_KEY = "settingsSalt";
    private final SecretKeySpec settingsKey;
    private final SecretKeySpec legacyV2Key; // For decrypting old static salt encrypted settings
    private final SecretKeySpec legacyV1Key; // For decrypting old SHA-256 encrypted settings
    private final SecureRandom secureRandom;
    private final Properties settings;

    public SecureSettings(Properties settings) {
        this.settings = settings;
        String username = System.getProperty("user.name", "default");
        String keySeed = username + "_LogHog_Settings";

        try {
            // Generate or retrieve random salt for this user
            byte[] salt = getOrCreateSettingsSalt();
            
            // Use PBKDF2 with random per-user salt for new encryption
            KeySpec spec = new PBEKeySpec(keySeed.toCharArray(), salt, PBKDF2_ITERATIONS, 128);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            SecretKey tmp = factory.generateSecret(spec);
            this.settingsKey = new SecretKeySpec(tmp.getEncoded(), "AES");
            
            // Initialize legacy v2 key (static salt) for backward compatibility
            byte[] legacyV2Salt = "LogHog_Settings_Salt_v2".getBytes(StandardCharsets.UTF_8);
            KeySpec legacyV2Spec = new PBEKeySpec((username + "_LogHog_Settings_v2").toCharArray(), legacyV2Salt, 10000, 128);
            SecretKey legacyV2Tmp = factory.generateSecret(legacyV2Spec);
            this.legacyV2Key = new SecretKeySpec(legacyV2Tmp.getEncoded(), "AES");
            
            // Initialize legacy v1 key (SHA-256) for backward compatibility
            String legacyV1KeySeed = username + "_LogHog_Settings_v1";
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] legacyV1KeyBytes = digest.digest(legacyV1KeySeed.getBytes(StandardCharsets.UTF_8));
            this.legacyV1Key = new SecretKeySpec(legacyV1KeyBytes, 0, 16, "AES");
            
            this.secureRandom = new SecureRandom();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize secure settings", e);
        }
    }
    
    /**
     * Gets the settings salt, creating and storing a random one if it doesn't exist.
     */
    private byte[] getOrCreateSettingsSalt() {
        String saltStr = settings.getProperty(SETTINGS_SALT_KEY);
        if (saltStr != null && !saltStr.isEmpty()) {
            try {
                return Base64.getDecoder().decode(saltStr);
            } catch (IllegalArgumentException e) {
                // Invalid base64, generate new salt
            }
        }
        
        // Generate new random salt
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        
        // Store it in settings (plaintext is correct for salts!)
        settings.setProperty(SETTINGS_SALT_KEY, Base64.getEncoder().encodeToString(salt));
        
        return salt;
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
            
            // Prepend IV to encrypted data - use Math.addExact to prevent integer overflow
            byte[] result = new byte[Math.addExact(iv.length, encrypted.length)];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            
            return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            // Security: Never return plaintext on encryption failure
            // Silent failure - don't log sensitive details to console
            return ""; // Return empty string instead of plaintext
        }
    }

    /**
     * Decrypt a value from settings, handling GCM and legacy ECB/SHA-256 encrypted values.
     * Tries decryption in order: new random salt → v2 static salt → v1 SHA-256
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
                
                // Try GCM first (new format with IV prefix and random salt)
                if (encryptedBytes.length >= GCM_IV_LENGTH + GCM_TAG_LENGTH) {
                    try {
                        byte[] iv = new byte[GCM_IV_LENGTH];
                        System.arraycopy(encryptedBytes, 0, iv, 0, GCM_IV_LENGTH);
                        byte[] encrypted = new byte[encryptedBytes.length - GCM_IV_LENGTH];
                        System.arraycopy(encryptedBytes, GCM_IV_LENGTH, encrypted, 0, encrypted.length);
                        
                        Cipher cipher = Cipher.getInstance(ALGORITHM);
                        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
                        cipher.init(Cipher.DECRYPT_MODE, settingsKey, spec);
                        byte[] decrypted = cipher.doFinal(encrypted);
                        return new String(decrypted, StandardCharsets.UTF_8);
                    } catch (Exception gcmException) {
                        // Not GCM format or wrong key, try ECB fallbacks
                    }
                }
                
                // Try ECB with new key (for data encrypted during transition)
                try {
                    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                    cipher.init(Cipher.DECRYPT_MODE, settingsKey);
                    byte[] decrypted = cipher.doFinal(encryptedBytes);
                    return new String(decrypted, StandardCharsets.UTF_8);
                } catch (Exception newEcbException) {
                    // Try legacy v2 key (static salt PBKDF2)
                    try {
                        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                        cipher.init(Cipher.DECRYPT_MODE, legacyV2Key);
                        byte[] decrypted = cipher.doFinal(encryptedBytes);
                        String value = new String(decrypted, StandardCharsets.UTF_8);
                        // Note: Auto-migration to new key happens when setting is next saved
                        return value;
                    } catch (Exception legacyV2Exception) {
                        // Try legacy v1 key (SHA-256)
                        try {
                            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                            cipher.init(Cipher.DECRYPT_MODE, legacyV1Key);
                            byte[] decrypted = cipher.doFinal(encryptedBytes);
                            String value = new String(decrypted, StandardCharsets.UTF_8);
                            // Note: Auto-migration to new key happens when setting is next saved
                            return value;
                        } catch (Exception legacyV1Exception) {
                            // All decryption attempts failed
                        }
                    }
                }
                
                // If we get here, decryption failed - return encrypted value as-is
                // Silent failure for backward compatibility (setting may not be encrypted)
                return storedValue;
            } catch (Exception e) {
                // Silent failure - setting may be in old format or plaintext
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