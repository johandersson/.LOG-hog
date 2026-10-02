package utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;

public final class LogEntryLink {
    public static final String PREFIX = "loghog:";
    private static final DateTimeFormatter NATIVE = DateTimeFormatter
            .ofPattern("HH:mm uuuu-MM-dd", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DATE_FIRST = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH:mm", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);

    private LogEntryLink() {}

    public static String normalize(String timestamp) {
        if (timestamp == null) throw new IllegalArgumentException("Missing timestamp");
        DateTimeFormatter format;
        if (timestamp.matches("[0-9]{2}:[0-9]{2} [0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            format = NATIVE;
        } else if (timestamp.matches("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}")) {
            format = DATE_FIRST;
        } else {
            throw new IllegalArgumentException("Invalid timestamp format");
        }
        try {
            LocalDateTime date = LocalDateTime.parse(timestamp, format);
            if (date.getYear() < 1) throw new IllegalArgumentException("Invalid year");
            return date.format(NATIVE);
        } catch (java.time.DateTimeException ex) {
            throw new IllegalArgumentException("Invalid date or time", ex);
        }
    }

    public static String findTimestamp(String timestamp, List<List<String>> entries) {
        String normalized = normalize(timestamp);
        for (List<String> entry : entries) {
            if (entry.isEmpty()) continue;
            String header = entry.get(0);
            try {
                if (normalize(header.trim()).equals(normalized)) return header.trim();
            } catch (IllegalArgumentException ignored) {
                // Ignore headers that cannot be addressed by a log link.
            }
        }
        return null;
    }
}
