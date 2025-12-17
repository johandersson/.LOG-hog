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
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Secure settings storage for sensitive configuration data.
 * Uses deterministic key derivation to avoid salt bootstrapping issues.
 */
public class SecureSettings {
    private static final String ENCRYPTED_PREFIX = "encrypted:";
    private final SecretKeySpec settingsKey;

    public SecureSettings() {
        // Generate deterministic key for settings encryption
        // Based on username + app identifier to ensure consistency across sessions
        String username = System.getProperty("user.name", "default");
        String appId = "LogHog_Settings_v1";
        String keySeed = username + "_" + appId;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(keySeed.getBytes(StandardCharsets.UTF_8));
            this.settingsKey = new SecretKeySpec(keyBytes, 0, 16, "AES"); // Use first 128 bits
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize secure settings", e);
        }
    }

    /**
     * Encrypt a sensitive value for storage in settings.
     */
    public String encryptValue(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, settingsKey);
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            System.err.println("Failed to encrypt setting value: " + e.getMessage());
            return value; // Return plain text as fallback
        }
    }

    /**
     * Decrypt a value from settings, handling both encrypted and plain text values.
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

                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, settingsKey);
                byte[] decrypted = cipher.doFinal(encryptedBytes);
                return new String(decrypted, StandardCharsets.UTF_8);
            } catch (Exception e) {
                System.err.println("Failed to decrypt setting value: " + e.getMessage());
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