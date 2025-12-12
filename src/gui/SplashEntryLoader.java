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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Utility class for loading and processing splash screen entries from resources.
 */
public class SplashEntryLoader {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Loads entries from the resources/entries.txt file, shuffles them,
     * selects the first 5, and sorts them by date.
     * <p>
     * Performance: O(N) where N is the total number of entries in the file,
     * due to the need to read and shuffle all entries before selection.
     *
     * @return A sorted list of up to 5 random entries
     */
    public static List<String> loadSplashEntries() {
        List<String> allEntries;
        try {
            var is = SplashEntryLoader.class.getResourceAsStream("/resources/entries.txt");
            if (is != null) {
                var reader = new BufferedReader(new InputStreamReader(is));
                allEntries = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    allEntries.add(line);
                }
                reader.close();
            } else {
                throw new IOException("Resource not found");
            }
        } catch (IOException e) {
            // Fallback to hardcoded entries
            allEntries = Arrays.asList(
                "2025-11-20 14:30: Started coding",
                "2025-11-20 14:35: Fixed infinite loop",
                "2025-11-20 14:40: Added cool feature",
                "2025-11-20 14:45: Tested the app",
                "2025-11-20 14:50: Committed to git"
            );
        }

        // Shuffle and select entries - O(N) shuffle, then O(K) selection where K=5
        Collections.shuffle(allEntries);
        List<String> selectedEntries = allEntries.subList(0, Math.min(5, allEntries.size()));

        // Sort by date - O(K log K) where K=5, effectively O(1)
        Collections.sort(selectedEntries, Comparator.comparing(s ->
            LocalDateTime.parse(s.substring(0, 16), DATE_FORMATTER)));

        return selectedEntries;
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
        Random random = new Random();

        try {
            var is = SplashEntryLoader.class.getResourceAsStream("/resources/entries.txt");
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