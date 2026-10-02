package utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class LogEntryLinkTest {
    @Test
    void normalizesEverySupportedDateFormatAndMatchesAlternateHeaders() {
        for (String input : List.of("13:23 2022-12-19", "2022-12-19 13:23",
                "19/12/2022 13:23", "12/19/2022 13:23", "19.12.2022 13:23",
                "19-12-2022 13:23")) {
            assertEquals("13:23 2022-12-19", LogEntryLink.normalize(input), input);
            assertEquals(DateHandler.parseTimestamp(input),
                    DateHandler.parseTimestamp(LogEntryLink.normalize(input)), input);
            assertEquals(input, LogEntryLink.findTimestamp("13:23 2022-12-19",
                    List.of(List.of(input, "Entry body"))), input);
        }
    }

    @Test
    void ambiguousSlashDatesKeepTheApplicationsEuropeanFirstPrecedence() {
        assertEquals("13:23 2022-04-03", LogEntryLink.normalize("03/04/2022 13:23"));
    }

    @Test
    void alternateFormatsRejectInvalidCalendarDatesAndLooseInput() {
        for (String input : List.of("2023-02-29 13:23", "29/02/2023 13:23",
                "02/29/2023 13:23", "31.04.2022 13:23", "31-04-2022 13:23",
                "19/12/2022 24:00", "12/19/2022 13:60", "1/12/2022 13:23",
                "19.12.22 13:23", "19/12/2022 13:23 trailing")) {
            assertThrows(IllegalArgumentException.class, () -> LogEntryLink.normalize(input), input);
        }
        assertEquals("13:23 2024-02-29", LogEntryLink.normalize("29/02/2024 13:23"));
        assertEquals("13:23 2024-02-29", LogEntryLink.normalize("02/29/2024 13:23"));
    }

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
