package utils;

import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;

public final class LogEntryLink {
    public static final String PREFIX = "loghog:";
    private static final DateTimeFormatter NATIVE = DateTimeFormatter
            .ofPattern("HH:mm uuuu-MM-dd", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);

    private LogEntryLink() {}

    public static String normalize(String timestamp) {
        return DateHandler.parseTimestampStrict(timestamp).format(NATIVE);
    }

    public static String findTimestamp(String timestamp, List<List<String>> entries) {
        String normalized = normalize(timestamp);
        for (List<String> entry : entries) {
            if (entry.isEmpty()) continue;
            String header = entry.get(0).trim().replaceAll(" \\(\\d+\\)$", "");
            try {
                if (normalize(header).equals(normalized)) return header;
            } catch (IllegalArgumentException ignored) {
                // Ignore headers that cannot be addressed by a log link.
            }
        }
        return null;
    }
}
