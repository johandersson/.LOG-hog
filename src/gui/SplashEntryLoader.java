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

package gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class for loading and processing splash screen entries from resources.
 */
public class SplashEntryLoader {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int SPLASH_ENTRY_COUNT = 5;
    private static final List<String> FALLBACK_ENTRIES = Collections.unmodifiableList(Arrays.asList(
        "2025-11-20 14:30: Started coding",
        "2025-11-20 14:35: Fixed bug",
        "2025-11-20 14:40: Added feature",
        "2025-11-20 14:45: Tested app",
        "2025-11-20 14:50: Committed"
    ));
    private static volatile List<String> cachedEntries;

    /**
     * Loads up to 5 random splash entries and sorts them by timestamp.
     * The backing resource is cached after the first load so repeated splash opens
     * do not pay the file I/O and parsing cost again.
     *
     * @return A sorted list of up to 5 random entries
     */
    public static List<String> loadSplashEntries() {
        List<String> selectedEntries = sampleCachedEntries();
        Collections.sort(selectedEntries, Comparator.comparing(s ->
            LocalDateTime.parse(s.substring(0, 16), DATE_FORMATTER)));
        return selectedEntries;
    }

    /**
     * Backward-compatible alias for the cached splash entry loader.
     *
     * @return A sorted list of up to 5 random entries
     */
    public static List<String> loadSplashEntriesOptimized() {
        return loadSplashEntries();
    }

    private static List<String> sampleCachedEntries() {
        List<String> allEntries = getOrLoadEntries();
        if (allEntries.size() <= SPLASH_ENTRY_COUNT) {
            return new ArrayList<>(allEntries);
        }

        List<String> shuffledEntries = new ArrayList<>(allEntries);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < SPLASH_ENTRY_COUNT; i++) {
            int swapIndex = random.nextInt(i, shuffledEntries.size());
            Collections.swap(shuffledEntries, i, swapIndex);
        }
        return new ArrayList<>(shuffledEntries.subList(0, SPLASH_ENTRY_COUNT));
    }

    private static List<String> getOrLoadEntries() {
        List<String> entries = cachedEntries;
        if (entries != null) {
            return entries;
        }
        synchronized (SplashEntryLoader.class) {
            entries = cachedEntries;
            if (entries == null) {
                cachedEntries = entries = loadAllEntries();
            }
        }
        return entries;
    }

    private static List<String> loadAllEntries() {
        try {
            var is = SplashEntryLoader.class.getResourceAsStream("/resources/entries.txt");
            if (is == null) {
                is = SplashEntryLoader.class.getResourceAsStream("/entries.txt");
            }
            if (is == null) {
                throw new IOException("Resource not found");
            }

            try (var reader = new BufferedReader(new InputStreamReader(is))) {
                List<String> entries = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        entries.add(line);
                    }
                }
                if (!entries.isEmpty()) {
                    return Collections.unmodifiableList(entries);
                }
            }
        } catch (IOException e) {
            // Fall through to built-in defaults.
        }
        return FALLBACK_ENTRIES;
    }
}