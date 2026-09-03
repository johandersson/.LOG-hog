package utils;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateHandlerTest {

    @Test
    void testParseTimestampLogHogFormat() {
        LocalDateTime result1 = DateHandler.parseTimestamp("14:30 2025-12-19");
        assertEquals(2025, result1.getYear());
        assertEquals(12, result1.getMonthValue());
        assertEquals(19, result1.getDayOfMonth());
        assertEquals(14, result1.getHour());
        assertEquals(30, result1.getMinute());

        LocalDateTime result2 = DateHandler.parseTimestamp("14:30 2025-12-19 (1)");
        assertEquals(result1, result2);
    }

    @Test
    void testParseTimestampSupportedAlternateFormats() {
        assertEquals(45, DateHandler.parseTimestamp("2025-12-19 14:45").getMinute());
        assertEquals(19, DateHandler.parseTimestamp("19/12/2025 14:30").getDayOfMonth());
        assertEquals(12, DateHandler.parseTimestamp("12/19/2025 14:30").getMonthValue());
        assertEquals(19, DateHandler.parseTimestamp("19.12.2025 14:30").getDayOfMonth());
        assertEquals(19, DateHandler.parseTimestamp("19-12-2025 14:30").getDayOfMonth());
    }

    @Test
    void testParseTimestampRejectsUnsupportedFormats() {
        String[] unsupported = {
            "2025-12-19 14:30:45",
            "2025-12-19T14:30:45",
            "12/19/2025 02:30 PM",
            "Date: 12/19/2025 Time: 02:30:45 PM",
            "12-19-2025 14:30",
            "invalid timestamp",
            "",
            "2025-12-19",
            "14:30"
        };
        for (String invalid : unsupported) {
            assertThrows(IllegalArgumentException.class, () -> DateHandler.parseTimestamp(invalid), invalid);
        }
    }

    @Test
    void testParseTimestampEdgeCases() {
        LocalDateTime result1 = DateHandler.parseTimestamp("  14:30 2025-12-19  ");
        assertEquals(2025, result1.getYear());
        LocalDateTime result2 = DateHandler.parseTimestamp("00:00 1900-01-01");
        assertEquals(1900, result2.getYear());
    }

    @Test
    void testFormatCurrentTimestamp() {
        String timestamp = DateHandler.formatCurrentTimestamp();
        assertTrue(timestamp.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}"));
        assertDoesNotThrow(() -> DateHandler.parseTimestamp(timestamp));
    }

    @Test
    void testTimestampRoundTripIsMinutePrecise() {
        String formatted = DateHandler.formatCurrentTimestamp();
        LocalDateTime parsed = DateHandler.parseTimestamp(formatted);
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        long minuteDiff = Math.abs(Duration.between(parsed, now).toMinutes());
        assertTrue(minuteDiff <= 1, "Round-trip should stay within a minute boundary");
        assertEquals(formatted, parsed.format(DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd")));
    }

    @Test
    void testIsTimestampMatchesSupportedFormats() {
        assertTrue(DateHandler.isTimestamp("14:30 2025-12-19"));
        assertTrue(DateHandler.isTimestamp("14:30 2025-12-19 (2)"));
        assertTrue(DateHandler.isTimestamp("2025-12-19 14:30"));
        assertTrue(DateHandler.isTimestamp("19/12/2025 14:30"));
        assertFalse(DateHandler.isTimestamp("2025-12-19T14:30:45"));
        assertFalse(DateHandler.isTimestamp("not a timestamp"));
    }
}
