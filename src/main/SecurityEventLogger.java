package main;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SecurityEventLogger {
    private static final Path LOG_FILE = Path.of(System.getProperty("user.home"), "loghog-security-events.log");

    public static void log(String event, String details) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String entry = String.format("[%s] %s: %s%n", timestamp, event, details);
        try (java.io.BufferedWriter bw = Files.newBufferedWriter(LOG_FILE, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            bw.write(entry);
        } catch (IOException e) {
            // Silent fail
        }
    }
}
