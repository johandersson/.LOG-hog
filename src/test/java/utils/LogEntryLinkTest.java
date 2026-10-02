package utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class LogEntryLinkTest {
    @Test
    void acceptsNativeAndDateFirstFormatsAndCanonicalizes() {
        assertEquals("13:23 2022-12-12", LogEntryLink.normalize("13:23 2022-12-12"));
        assertEquals("13:23 2022-12-12", LogEntryLink.normalize("2022-12-12 13:23"));
        assertEquals("00:00 2024-02-29", LogEntryLink.normalize("00:00 2024-02-29"));
    }

    @Test
    void rejectsInvalidDatesTimesAndLooseFormats() {
        for (String input : List.of("", "13:23 2023-02-29", "13:23 2022-04-31",
                "24:00 2022-12-12", "13:60 2022-12-12", "1:23 2022-12-12",
                "13:23 22-12-12", "[13:23 2022-12-12]", "13:23 0000-12-12",
                "2022-12-12 13:23 trailing")) {
            assertThrows(IllegalArgumentException.class, () -> LogEntryLink.normalize(input), input);
        }
        assertThrows(IllegalArgumentException.class, () -> LogEntryLink.normalize(null));
    }

    @Test
    void matchesOnlyEntryHeadersIncludingEmptyEntriesAndInternationalHeaders() {
        var entries = List.of(List.of("12:00 2022-12-12", "13:23 2022-12-12"),
                List.of("2024-02-29 00:00"));
        assertNull(LogEntryLink.findTimestamp("13:23 2022-12-12", entries));
        assertEquals("2024-02-29 00:00", LogEntryLink.findTimestamp("00:00 2024-02-29", entries));
    }
}
