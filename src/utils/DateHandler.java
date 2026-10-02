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
import java.time.format.ResolverStyle;
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
    // Checked by both isTimestamp() and parseTimestamp() as fallbacks after the primary format.
    private static final List<String> INTERNATIONAL_FORMATS = List.of(
        "yyyy-MM-dd HH:mm",  // ISO reversed
        "dd/MM/yyyy HH:mm",  // European slash
        "MM/dd/yyyy HH:mm",  // US slash
        "dd.MM.yyyy HH:mm",  // German / Central-European dot
        "dd-MM-yyyy HH:mm"   // European dash
    );
    private static final List<DateTimeFormatter> INTERNATIONAL_FORMATTERS = INTERNATIONAL_FORMATS.stream()
        .map(pattern -> DateTimeFormatter.ofPattern(pattern, Locale.ROOT)).toList();
    private static final List<DateTimeFormatter> STRICT_INTERNATIONAL_FORMATTERS = INTERNATIONAL_FORMATS.stream()
        .map(pattern -> DateTimeFormatter.ofPattern(pattern.replace("yyyy", "uuuu"), Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT)).toList();
    private static final DateTimeFormatter STRICT_NATIVE_FORMATTER = DateTimeFormatter
        .ofPattern("HH:mm uuuu-MM-dd", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);

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
     * Validates an exact, unsuffixed timestamp using the supported formats and real
     * calendar dates. Ambiguous slash dates use European-first precedence.
     */
    public static LocalDateTime parseTimestampStrict(String timestamp) {
        if (timestamp == null || !timestamp.equals(timestamp.trim())
                || timestamp.endsWith(")") || !isTimestamp(timestamp)) {
            throw new IllegalArgumentException("Invalid timestamp format");
        }
        if (TIMESTAMP_PATTERN.matcher(timestamp).matches()) {
            return parseStrict(timestamp, STRICT_NATIVE_FORMATTER);
        }
        for (DateTimeFormatter formatter : STRICT_INTERNATIONAL_FORMATTERS) {
            try {
                return parseStrict(timestamp, formatter);
            } catch (IllegalArgumentException ignored) {
                // Try the next supported format.
            }
        }
        throw new IllegalArgumentException("Invalid date or time");
    }

    private static LocalDateTime parseStrict(String timestamp, DateTimeFormatter formatter) {
        try {
            LocalDateTime date = LocalDateTime.parse(timestamp, formatter);
            if (date.getYear() < 1) throw new IllegalArgumentException("Invalid year");
            return date;
        } catch (java.time.DateTimeException ex) {
            throw new IllegalArgumentException("Invalid date or time", ex);
        }
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