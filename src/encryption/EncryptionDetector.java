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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for detecting encrypted files and managing backup security.
 * Provides methods to determine if a file contains encrypted data.
 */
public class EncryptionDetector {

    private static final int MIN_ENCRYPTED_FILE_SIZE = 12 + 16; // GCM IV + GCM Tag minimum
    private static final int SALT_SIZE = 16;

    /**
     * Determines if a file appears to contain encrypted data.
     * This is a heuristic check based on file structure and content patterns.
     *
     * @param filePath Path to the file to check
     * @return true if the file appears to be encrypted, false otherwise
     */
    public static boolean isFileEncrypted(Path filePath) {
        try {
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                return false;
            }

            long fileSize = Files.size(filePath);
            if (fileSize < MIN_ENCRYPTED_FILE_SIZE) {
                return false; // Too small to be encrypted
            }

            byte[] data = Files.readAllBytes(filePath);

            // Check if it starts with a salt (16 bytes)
            if (data.length >= SALT_SIZE) {
                // If it starts with what looks like a salt, it might be encrypted
                // We can't verify the salt without the actual salt value, so we use heuristics
                return appearsToBeEncrypted(data);
            }

            return false;
        } catch (IOException e) {
            // If we can't read the file, assume it's not encrypted for safety
            return false;
        }
    }

    /**
     * Heuristic check to determine if data appears to be encrypted.
     * This checks for patterns typical of encrypted data.
     */
    private static boolean appearsToBeEncrypted(byte[] data) {
        // Encrypted files typically have random-looking byte patterns
        // Check if the data has high entropy (not plain text patterns)

        if (data.length < MIN_ENCRYPTED_FILE_SIZE) {
            return false;
        }

        // Check for common plain text patterns that would indicate unencrypted data
        String startOfFile = new String(data, 0, Math.min(100, data.length));
        if (startOfFile.startsWith(".LOG")) {
            return false; // Plain text log file
        }

        // Check for very low entropy (repeated patterns) which might indicate plain text
        // or very high entropy which indicates encryption
        int uniqueBytes = countUniqueBytes(data, Math.min(256, data.length));
        double entropy = calculateEntropy(data, Math.min(256, data.length));

        // High entropy suggests encryption, low entropy suggests plain text
        // Encrypted data typically has entropy > 7.5 bits per byte
        return entropy > 7.0;
    }

    private static int countUniqueBytes(byte[] data, int sampleSize) {
        boolean[] seen = new boolean[256];
        int unique = 0;
        for (int i = 0; i < sampleSize && i < data.length; i++) {
            int b = data[i] & 0xFF;
            if (!seen[b]) {
                seen[b] = true;
                unique++;
            }
        }
        return unique;
    }

    private static double calculateEntropy(byte[] data, int sampleSize) {
        int[] counts = new int[256];
        int total = Math.min(sampleSize, data.length);

        for (int i = 0; i < total; i++) {
            counts[data[i] & 0xFF]++;
        }

        double entropy = 0.0;
        for (int count : counts) {
            if (count > 0) {
                double p = (double) count / total;
                entropy -= p * (Math.log(p) / Math.log(2));
            }
        }

        return entropy;
    }

    /**
     * Securely deletes a file by overwriting it before deletion.
     * This helps prevent recovery of sensitive data.
     * Delegates to BackupManager for consistent secure deletion.
     *
     * @param filePath Path to the file to delete
     * @throws IOException if deletion fails
     */
    public static void secureDelete(Path filePath) throws IOException {
        main.BackupManager.secureDelete(filePath);
    }
}