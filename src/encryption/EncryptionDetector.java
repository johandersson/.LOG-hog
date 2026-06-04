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

package encryption;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for detecting encrypted files.
 * Provides methods to determine if a file contains encrypted data.
 */
public class EncryptionDetector {

    /**
     * Returns true if the file is an encrypted LogHog file.
     * Delegates to {@link #hasMagicHeader(Path)} — every file encrypted by this
     * application carries the LOGH magic header.
     *
     * @param filePath Path to the file to check
     * @return true if the file is a LogHog encrypted file, false otherwise
     */
    public static boolean isFileEncrypted(Path filePath) {
        return hasMagicHeader(filePath);
    }

    /**
     * Returns true if the file starts with the LOGH magic header.
     */
    public static boolean hasMagicHeader(Path filePath) {
        try {
            if (!Files.exists(filePath) || Files.size(filePath) < 6) return false;
            byte[] header = new byte[4];
            try (var in = Files.newInputStream(filePath)) {
                return in.read(header) == 4
                    && header[0] == 'L' && header[1] == 'O'
                    && header[2] == 'G' && header[3] == 'H';
            }
        } catch (Exception e) {
            return false;
        }
    }

}