package utils;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Comprehensive tests for DateHandler
 * Tests timestamp parsing, formatting, and error handling for various date formats
 */
public class DateHandlerTest {

    @Test
    void testParseTimestampLogHogFormat() {
        System.out.println("🧪 Testing LogHog timestamp format parsing...");

        // Test basic LogHog format
        LocalDateTime result1 = DateHandler.parseTimestamp("14:30 2025-12-19");
        assertEquals(2025, result1.getYear());
        assertEquals(12, result1.getMonthValue());
        assertEquals(19, result1.getDayOfMonth());
        assertEquals(14, result1.getHour());
        assertEquals(30, result1.getMinute());

        // Test with suffix (duplicate entries)
        LocalDateTime result2 = DateHandler.parseTimestamp("14:30 2025-12-19 (1)");
        assertEquals(result1, result2, "Suffix should be ignored in parsing");

        LocalDateTime result3 = DateHandler.parseTimestamp("09:15 2023-01-15 (5)");
        assertEquals(2023, result3.getYear());
        assertEquals(1, result3.getMonthValue());
        assertEquals(15, result3.getDayOfMonth());

        System.out.println("✅ LogHog timestamp format parsing works correctly");
    }

    @Test
    void testParseTimestampIsoFormats() {
        System.out.println("🧪 Testing ISO timestamp format parsing...");

        // Test various ISO formats
        LocalDateTime iso1 = DateHandler.parseTimestamp("2025-12-19 14:30");
        assertEquals(2025, iso1.getYear());
        assertEquals(14, iso1.getHour());

        LocalDateTime iso2 = DateHandler.parseTimestamp("2025-12-19 14:30:45");
        assertEquals(45, iso2.getSecond());

        LocalDateTime iso3 = DateHandler.parseTimestamp("2025-12-19T14:30:45");
        assertEquals(45, iso3.getSecond());

        System.out.println("✅ ISO timestamp format parsing works correctly");
    }

    @Test
    void testParseTimestampInternationalFormats() {
        System.out.println("🧪 Testing international timestamp format parsing...");

        // European format
        LocalDateTime eu = DateHandler.parseTimestamp("19/12/2025 14:30");
        assertEquals(19, eu.getDayOfMonth());
        assertEquals(12, eu.getMonthValue());

        // US format
        LocalDateTime us = DateHandler.parseTimestamp("12/19/2025 14:30");
        assertEquals(12, us.getMonthValue());
        assertEquals(19, us.getDayOfMonth());

        // German format
        LocalDateTime de = DateHandler.parseTimestamp("19.12.2025 14:30");
        assertEquals(19, de.getDayOfMonth());
        assertEquals(12, de.getMonthValue());

        // Dash-separated formats
        LocalDateTime dash1 = DateHandler.parseTimestamp("19-12-2025 14:30");
        assertEquals(19, dash1.getDayOfMonth());

        LocalDateTime dash2 = DateHandler.parseTimestamp("12-19-2025 14:30");
        assertEquals(12, dash2.getMonthValue());

        System.out.println("✅ International timestamp format parsing works correctly");
    }

    @Test
    void testParseTimestampTwelveHourFormats() {
        System.out.println("🧪 Testing 12-hour timestamp format parsing...");

        // US 12-hour with AM/PM
        LocalDateTime am = DateHandler.parseTimestamp("12/19/2025 02:30:45 PM");
        assertEquals(14, am.getHour()); // 2 PM = 14:00
        assertEquals(30, am.getMinute());
        assertEquals(45, am.getSecond());

        LocalDateTime pm = DateHandler.parseTimestamp("12/19/2025 02:30 PM");
        assertEquals(14, pm.getHour());

        // European 12-hour
        LocalDateTime eu12 = DateHandler.parseTimestamp("19/12/2025 02:30:45 PM");
        assertEquals(14, eu12.getHour());

        System.out.println("✅ 12-hour timestamp format parsing works correctly");
    }

    @Test
    void testParseTimestampNotepadFormats() {
        System.out.println("🧪 Testing Notepad-style timestamp format parsing...");

        // Notepad US format
        LocalDateTime notepadUS = DateHandler.parseTimestamp("Date: 12/19/2025 Time: 02:30:45 PM");
        assertEquals(14, notepadUS.getHour());

        // Notepad European format
        LocalDateTime notepadEU = DateHandler.parseTimestamp("Date: 19/12/2025 Time: 14:30:45");
        assertEquals(14, notepadEU.getHour());

        System.out.println("✅ Notepad-style timestamp format parsing works correctly");
    }

    @Test
    void testParseTimestampInvalidFormats() {
        System.out.println("🧪 Testing invalid timestamp format handling...");

        // Test various invalid formats
        String[] invalidFormats = {
            "invalid timestamp",
            "25-13-45 99:99", // Invalid date/time
            "not a date",
            "",
            "2025-12-19", // Date only, no time
            "14:30", // Time only, no date
            "random text here"
        };

        for (String invalid : invalidFormats) {
            assertThrows(IllegalArgumentException.class,
                () -> DateHandler.parseTimestamp(invalid),
                "Should reject invalid format: " + invalid);
        }

        System.out.println("✅ Invalid timestamp format handling works correctly");
    }

    @Test
    void testParseTimestampEdgeCases() {
        System.out.println("🧪 Testing timestamp parsing edge cases...");

        // Test with extra whitespace
        LocalDateTime result1 = DateHandler.parseTimestamp("  14:30 2025-12-19  ");
        assertEquals(2025, result1.getYear());

        // Test minimum valid date
        LocalDateTime result2 = DateHandler.parseTimestamp("00:00 1900-01-01");
        assertEquals(1900, result2.getYear());
        assertEquals(1, result2.getMonthValue());
        assertEquals(1, result2.getDayOfMonth());

        System.out.println("✅ Timestamp parsing edge cases handled correctly");
    }

    @Test
    void testFormatCurrentTimestamp() {
        System.out.println("🧪 Testing current timestamp formatting...");

        String timestamp = DateHandler.formatCurrentTimestamp();

        // Should match LogHog format: HH:mm yyyy-MM-dd
        assertTrue(timestamp.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}"),
                  "Timestamp should match LogHog format pattern");

        // Should be parseable back
        assertDoesNotThrow(() -> DateHandler.parseTimestamp(timestamp),
                          "Formatted timestamp should be parseable");

        System.out.println("✅ Current timestamp formatting works correctly");
    }

    @Test
    void testTimestampRoundTrip() {
        System.out.println("🧪 Testing timestamp round-trip consistency...");

        // Create a timestamp and ensure it can be parsed back
        String formatted = DateHandler.formatCurrentTimestamp();
        LocalDateTime parsed = DateHandler.parseTimestamp(formatted);

        // Should be very close (within a few seconds due to timing)
        LocalDateTime now = LocalDateTime.now();
        long secondsDiff = Math.abs(java.time.Duration.between(parsed, now).getSeconds());
        assertTrue(secondsDiff < 10, "Parsed timestamp should be within 10 seconds of current time");

        System.out.println("✅ Timestamp round-trip consistency works correctly");
    }

    @Test
    void testParseTimestampWithVariousSeparators() {
        System.out.println("🧪 Testing timestamp parsing with various separators...");

        // Test different date separators
        LocalDateTime slash = DateHandler.parseTimestamp("19/12/2025 14:30");
        LocalDateTime dot = DateHandler.parseTimestamp("19.12.2025 14:30");
        LocalDateTime dash = DateHandler.parseTimestamp("19-12-2025 14:30");

        // All should parse to the same date
        assertEquals(slash.getYear(), dot.getYear());
        assertEquals(slash.getMonth(), dot.getMonth());
        assertEquals(slash.getDayOfMonth(), dot.getDayOfMonth());
        assertEquals(slash.getHour(), dot.getHour());
        assertEquals(slash.getMinute(), dot.getMinute());

        assertEquals(slash.getYear(), dash.getYear());
        assertEquals(slash.getMonth(), dash.getMonth());

        System.out.println("✅ Timestamp parsing with various separators works correctly");
    }

    @Test
    void testParseTimestampComprehensiveFormatSupport() {
        System.out.println("🧪 Testing comprehensive format support...");

        // Test that we support a wide variety of real-world formats
        String[] testFormats = {
            "14:30 2025-12-19",           // LogHog primary
            "2025-12-19 14:30:45",        // ISO with seconds
            "19/12/2025 14:30",           // European
            "12/19/2025 02:30 PM",        // US 12-hour
            "19.12.2025 14:30:45",        // German
            "2025/12/19 14:30",           // ISO with slashes
            "19-12-2025 14:30:45",        // Dash European
            "12-19-2025 14:30"            // Dash US
        };

        for (String format : testFormats) {
            assertDoesNotThrow(() -> DateHandler.parseTimestamp(format),
                              "Should support format: " + format);
        }

        System.out.println("✅ Comprehensive format support works correctly");
    }
}