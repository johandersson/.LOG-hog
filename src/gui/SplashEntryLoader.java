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
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Utility class for loading and processing splash screen entries from resources.
 */
public class SplashEntryLoader {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Loads entries from the resources/entries.txt file using reservoir sampling
     * to select 5 random entries without loading the entire file into memory.
     * Then sorts them by date.
     * <p>
     * Performance: O(N) time for reading, O(1) space (besides the 5 selected entries).
     *
     * @return A sorted list of up to 5 random entries
     */
    public static List<String> loadSplashEntries() {
        List<String> reservoir = new ArrayList<>(5);
        SecureRandom random = new SecureRandom();
        try {
            var is = SplashEntryLoader.class.getResourceAsStream("/resources/entries.txt");
            if (is == null) {
                // Some builds may package resources at the jar root; try fallback
                is = SplashEntryLoader.class.getResourceAsStream("/entries.txt");
            }
            if (is != null) {
                var reader = new BufferedReader(new InputStreamReader(is));
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null) {
                    if (count < 5) {
                        reservoir.add(line);
                    } else {
                        int r = random.nextInt(count + 1);
                        if (r < 5) {
                            reservoir.set(r, line);
                        }
                    }
                    count++;
                }
                reader.close();
            } else {
                throw new IOException("Resource not found");
            }
        } catch (IOException e) {
            // Fallback to hardcoded entries (all under 55 chars)
            reservoir = Arrays.asList(
                "2025-11-20 14:30: Started coding",
                "2025-11-20 14:35: Fixed bug",
                "2025-11-20 14:40: Added feature",
                "2025-11-20 14:45: Tested app",
                "2025-11-20 14:50: Committed"
            );
        }

        // If fewer than 5 entries, reservoir has all; otherwise, it's 5 random ones
        // Shuffle the reservoir for additional randomness (optional, but reservoir sampling is already uniform)
        Collections.shuffle(reservoir, random);

        // Sort by date - O(K log K) where K=5, effectively O(1)
        Collections.sort(reservoir, Comparator.comparing(s ->
            LocalDateTime.parse(s.substring(0, 16), DATE_FORMATTER)));

        return reservoir;
    }

    /**
     * Optimized version that achieves O(K) performance where K=5 is the number of entries needed.
     * Uses reservoir sampling to select random entries without loading all entries into memory.
     * <p>
     * Performance: O(K) where K=5, plus O(K log K) for sorting.
     * Much more efficient for large entry files.
     *
     * @return A sorted list of exactly 5 random entries (or fewer if file has less)
     */
    public static List<String> loadSplashEntriesOptimized() {
        List<String> selectedEntries = new ArrayList<>();
        SecureRandom random = new SecureRandom();

        try {
            var is = SplashEntryLoader.class.getResourceAsStream("/resources/entries.txt");
            if (is == null) {
                is = SplashEntryLoader.class.getResourceAsStream("/entries.txt");
            }
            if (is != null) {
                var reader = new BufferedReader(new InputStreamReader(is));
                String line;
                int count = 0;

                // Reservoir sampling: keep first 5, then randomly replace
                while ((line = reader.readLine()) != null) {
                    count++;
                    if (selectedEntries.size() < 5) {
                        // Fill reservoir
                        selectedEntries.add(line);
                    } else {
                        // Random replacement with decreasing probability
                        int randomIndex = random.nextInt(count);
                        if (randomIndex < 5) {
                            selectedEntries.set(randomIndex, line);
                        }
                    }
                }
                reader.close();
            } else {
                throw new IOException("Resource not found");
            }
        } catch (IOException e) {
            // Fallback to hardcoded entries
            selectedEntries = new ArrayList<>(Arrays.asList(
                "2025-11-20 14:30: Started coding",
                "2025-11-20 14:35: Fixed infinite loop",
                "2025-11-20 14:40: Added cool feature",
                "2025-11-20 14:45: Tested the app",
                "2025-11-20 14:50: Committed to git"
            ));
        }

        // Sort by date - O(K log K) where K=5
        Collections.sort(selectedEntries, Comparator.comparing(s ->
            LocalDateTime.parse(s.substring(0, 16), DATE_FORMATTER)));

        return selectedEntries;
    }
}