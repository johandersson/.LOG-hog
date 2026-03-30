package main;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SecurityEventLogger {
    private static final String LOG_FILE = System.getProperty("user.home") + "/loghog-security-events.log";

    public static void log(String event, String details) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String entry = String.format("[%s] %s: %s\n", timestamp, event, details);
        try (java.io.BufferedWriter bw = new java.io.BufferedWriter(new FileWriter(LOG_FILE, true))) {
            bw.write(entry);
        } catch (IOException e) {
            // Silent fail
        }
    }
}
