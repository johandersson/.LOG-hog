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

package utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility class for handling date parsing and formatting operations.
 */
public class DateHandler {

    /**
     * Pre-compiled pattern for LogHog's primary timestamp format (HH:mm yyyy-MM-dd).
     * Much faster than String.matches() which compiles the regex each time.
     */
    public static final Pattern TIMESTAMP_PATTERN = Pattern.compile("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( *\\(\\d+\\))?$");

    // International timestamp patterns (for log files written on different locales).
    // These are checked only as fallbacks in parseTimestamp(); isTimestamp() still uses
    // the primary pattern so that entry-header detection stays fast and unambiguous.
    private static final List<DateTimeFormatter> INTERNATIONAL_FORMATTERS = List.of(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT),  // ISO reversed
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ROOT),  // European slash
        DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm", Locale.ROOT),  // US slash
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.ROOT),  // German / Central-European dot
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.ROOT)   // European dash
    );

    // Patterns that match each international format (used in isTimestamp).
    private static final List<Pattern> INTERNATIONAL_PATTERNS = List.of(
        Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}$"),        // yyyy-MM-dd HH:mm
        Pattern.compile("^\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}$"),        // dd/MM/yyyy or MM/dd/yyyy
        Pattern.compile("^\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}$"),    // dd.MM.yyyy HH:mm
        Pattern.compile("^\\d{2}-\\d{2}-\\d{4} \\d{2}:\\d{2}$")         // dd-MM-yyyy HH:mm
    );

    /**
     * Checks if a string matches LogHog's primary timestamp format or any supported
     * international timestamp format.
     *
     * @param line the string to check
     * @return true if the string looks like a timestamp
     */
    public static boolean isTimestamp(String line) {
        String t = line.trim();
        if (TIMESTAMP_PATTERN.matcher(t).matches()) return true;
        for (Pattern p : INTERNATIONAL_PATTERNS) {
            if (p.matcher(t).matches()) return true;
        }
        return false;
    }

    /**
     * Parses a timestamp string from a log entry into a LocalDateTime object.
     * Tries LogHog's primary format first, then each international format in order.
     *
     * @param entry the log entry string containing a timestamp
     * @return the parsed LocalDateTime
     * @throws IllegalArgumentException if the timestamp format is unrecognized
     */
    public static LocalDateTime parseTimestamp(String entry) {
        String trimmed = entry.trim();
        // Strip duplicate-suffix annotation (e.g. " (2)") before parsing
        String clean = trimmed.replaceAll(" \\(\\d+\\)$", "");
        if (TIMESTAMP_PATTERN.matcher(trimmed).matches()) {
            return LocalDateTime.parse(clean, DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd", Locale.ROOT));
        }
        for (DateTimeFormatter fmt : INTERNATIONAL_FORMATTERS) {
            try {
                return LocalDateTime.parse(clean, fmt);
            } catch (Exception ignored) {
                // try next format
            }
        }
        throw new IllegalArgumentException("Unsupported timestamp format: '" + trimmed + "'.");
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