package main;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import security.SecurityFilePolicy;

public class SecurityEventLogger {
    private static final Path LOG_DIR = Path.of(System.getProperty("user.home"), ".loghog");
    private static final Path LOG_FILE = LOG_DIR.resolve("security-events.log");

    public static void log(String event, String details) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String safeEvent = SecurityFilePolicy.sanitizeLogValue(event);
        String safeDetails = SecurityFilePolicy.sanitizeLogValue(details);
        String entry = String.format("[%s] event=%s details=%s%n", timestamp, safeEvent, safeDetails);
        try {
            Files.createDirectories(LOG_DIR);
        } catch (IOException e) {
            return;
        }
        try (java.io.BufferedWriter bw = Files.newBufferedWriter(LOG_FILE, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            bw.write(entry);
            SecurityFilePolicy.ensureOwnerOnlyPermissions(LOG_FILE);
            SecurityAlertDispatcher.dispatchIfCritical(safeEvent, safeDetails);
        } catch (IOException e) {
            // Silent fail
        }
    }
}
