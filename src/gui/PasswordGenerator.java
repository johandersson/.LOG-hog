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

package gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class PasswordGenerator {
    private static final SecureRandom random = new SecureRandom();
    private static List<String> wordList; // default null, no initializer needed

    private static void loadWordList() {
        if (wordList != null) return;
        wordList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    PasswordGenerator.class.getResourceAsStream("/resources/dict.txt"),
                    StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length >= 2) {
                    wordList.add(parts[1]); // word is after tab
                }
            }
        } catch (IOException e) {
            // Fallback words if dict.txt not found
            wordList.addAll(List.of("correct", "horse", "battery", "staple", "random", "secure", "password", "generator"));
        }
    }

    /**
     * Generates a random password with the specified length.
     *
     * @param length the desired password length (must be between 1 and 1000)
     * @return a randomly generated password
     * @throws IllegalArgumentException if length is outside valid bounds
     */
    public static String generatePassword(int length) {
        if (length < 1 || length > 1000) {
            throw new IllegalArgumentException("Password length must be between 1 and 1000 characters");
        }
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-=[]{}|;:,.<>?";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Generates a random passphrase with the specified number of words.
     *
     * @param wordCount the number of words in the passphrase (must be between 1 and 20)
     * @return a randomly generated passphrase
     * @throws IllegalArgumentException if wordCount is outside valid bounds
     */
    public static String generatePassphrase(int wordCount) {
        if (wordCount < 1 || wordCount > 20) {
            throw new IllegalArgumentException("Word count must be between 1 and 20");
        }
        loadWordList();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < wordCount; i++) {
            if (i > 0) sb.append(' ');
            sb.append(wordList.get(random.nextInt(wordList.size())));
        }
        return sb.toString();
    }
}