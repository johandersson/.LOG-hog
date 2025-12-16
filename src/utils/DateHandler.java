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

package utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Utility class for handling date parsing and formatting operations.
 */
public class DateHandler {

    /**
     * Parses a timestamp string from a log entry into a LocalDateTime object.
     * Supports multiple date formats for backward compatibility.
     *
     * @param entry the log entry string containing a timestamp
     * @return the parsed LocalDateTime
     * @throws IllegalArgumentException if the timestamp format is unrecognized
     */
    public static LocalDateTime parseTimestamp(String entry) {
        String trimmed = entry.trim();
        
        // Quick check for LogHog's primary format
        if (trimmed.matches("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\(\\d+\\))?$")) {
            return LocalDateTime.parse(trimmed.replaceAll(" \\(\\d+\\)", ""), DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd", Locale.ROOT));
        }
        
        // For other formats, try a comprehensive set of common patterns
        List<DateTimeFormatter> formatters = List.of(
                // LogHog variations
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT), // ISO with T
                // Common international formats
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ROOT),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.ROOT),
                DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm", Locale.ROOT),
                DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss", Locale.ROOT),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale.ROOT),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss", Locale.ROOT),
                // German-style
                DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.ROOT),
                DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss", Locale.ROOT),
                // US 12-hour with AM/PM (requires English locale)
                DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss a", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a", Locale.ENGLISH),
                // Notepad styles
                DateTimeFormatter.ofPattern("'Date: 'MM/dd/yyyy' Time: 'hh:mm:ss a", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("'Date: 'dd/MM/yyyy' Time: 'HH:mm:ss", Locale.ROOT),
                // Additional common variations
                DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a", Locale.ENGLISH), // 12-hour ISO
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.ROOT),
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss", Locale.ROOT),
                DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm", Locale.ROOT),
                DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss", Locale.ROOT)
        );
        
        for (DateTimeFormatter fmt : formatters) {
            try {
                return LocalDateTime.parse(trimmed, fmt);
            } catch (Exception ignored) {
            }
        }
        
        // If all fail, throw with helpful message
        throw new IllegalArgumentException("Unsupported timestamp format: '" + trimmed + "'. Use LogHog's format (HH:mm yyyy-MM-dd) or reformat the file.");
    }

    /**
     * Formats the current date and time into a timestamp string.
     *
     * @return the formatted timestamp string
     */
    public static String formatCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd"));
    }
}