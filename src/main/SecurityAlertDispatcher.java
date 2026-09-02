package main;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import security.SecurityFilePolicy;

/**
 * Lightweight local alert sink for high-severity security events.
 */
public final class SecurityAlertDispatcher {
    private static final Path ALERT_DIR = Path.of(System.getProperty("user.home"), ".loghog");
    private static final Path ALERT_FILE = ALERT_DIR.resolve("security-alerts.log");
    private static final Set<String> CRITICAL_EVENTS = Set.of(
        "TamperDetected",
        "BackupVerificationFailed"
    );

    private SecurityAlertDispatcher() {}

    public static void dispatchIfCritical(String event, String details) {
        if (event == null || !CRITICAL_EVENTS.contains(event)) {
            return;
        }

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String safeDetails = SecurityFilePolicy.sanitizeLogValue(details);
        String entry = "[" + ts + "] level=CRITICAL event=" + event + " details=" + safeDetails + System.lineSeparator();

        try {
            Files.createDirectories(ALERT_DIR);
            try (java.io.BufferedWriter bw = Files.newBufferedWriter(
                    ALERT_FILE,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND)) {
                bw.write(entry);
            }
            SecurityFilePolicy.ensureOwnerOnlyPermissions(ALERT_FILE);
        } catch (IOException ignored) {
            // best effort
        }
    }
}
