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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        // Extract the timestamp from the beginning of the line
        Pattern tsPattern = Pattern.compile("^(\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2})( \\([0-9]+\\))?");
        Matcher matcher = tsPattern.matcher(entry.trim());
        if (!matcher.find()) {
            throw new IllegalArgumentException("No timestamp found in: " + entry);
        }
        String dateStr = matcher.group(1);
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd", Locale.getDefault()),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault()),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault()),
                DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm", Locale.getDefault()),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale.getDefault()),
                DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale.ENGLISH)
        );
        for (DateTimeFormatter fmt : formatters) {
            try {
                return LocalDateTime.parse(dateStr, fmt);
            } catch (Exception ignored) {
            }
        }
        throw new IllegalArgumentException("Unrecognized date format: " + dateStr);
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