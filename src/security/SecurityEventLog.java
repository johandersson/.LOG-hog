package security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Base64;
import java.util.Properties;

import main.HmacUtils;

/**
 * Tamper-evident security event log with sequence and hash-chain anchoring.
 */
public final class SecurityEventLog {
    private static final String LOCKOUT_DIR_NAME = ".loghog";
    private static final String LOG_FILE = "security-events.log";
    private static final String ANCHOR_FILE = "security-events.anchor";
    private static final String KEY_FILE = "security-events.key";
    private static final String KEY_SEQ = "seq";
    private static final String KEY_LAST_HASH = "lastHash";
    private static final String KEY_ANCHOR_MAC = "anchorMac";

    private SecurityEventLog() {}

    public static synchronized void appendEvent(String eventType, String details) throws IOException {
        Path dir = getStorageDir();
        Files.createDirectories(dir);
        SecurityFilePolicy.ensureOwnerOnlyPermissionsOrThrow(dir);

        byte[] key = getOrCreateKey();
        try {
            Anchor anchor = readAnchor(key);
            long nextSeq = anchor.seq + 1L;
            String prevHash = anchor.lastHashBase64;
            String timestamp = Instant.now().toString();
            String safeType = sanitizeEventType(eventType);
            String safeDetails = SecurityFilePolicy.sanitizeLogValue(details);

            String core = timestamp + "|" + nextSeq + "|" + safeType + "|" + safeDetails + "|" + prevHash;
            byte[] coreBytes = core.getBytes(StandardCharsets.UTF_8);
            byte[] entryMac = HmacUtils.computeHmacSha256(key, coreBytes);
            byte[] hashBytes;
            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                hashBytes = digest.digest(coreBytes);
            } catch (java.security.GeneralSecurityException ex) {
                throw new IOException("Unable to hash security event", ex);
            }
            String entryMacBase64 = Base64.getEncoder().encodeToString(entryMac);
            String hashBase64 = Base64.getEncoder().encodeToString(hashBytes);

            String line = core + "|" + entryMacBase64 + System.lineSeparator();
            Files.write(getLogPath(), line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
            SecurityFilePolicy.ensureOwnerOnlyPermissionsOrThrow(getLogPath());

            writeAnchor(nextSeq, hashBase64, key);
        } finally {
            java.util.Arrays.fill(key, (byte) 0);
        }
    }

    private static byte[] getOrCreateKey() throws IOException {
        Path keyPath = getKeyPath();
        if (!Files.exists(keyPath)) {
            byte[] generated = new byte[32];
            new java.security.SecureRandom().nextBytes(generated);
            try {
                Files.write(keyPath, Base64.getEncoder().encode(generated));
                SecurityFilePolicy.ensureOwnerOnlyPermissionsOrThrow(keyPath);
            } finally {
                java.util.Arrays.fill(generated, (byte) 0);
            }
        }

        byte[] encoded = Files.readAllBytes(keyPath);
        try {
            return Base64.getDecoder().decode(encoded);
        } finally {
            java.util.Arrays.fill(encoded, (byte) 0);
        }
    }

    private static Anchor readAnchor(byte[] key) throws IOException {
        Path anchorPath = getAnchorPath();
        if (!Files.exists(anchorPath)) {
            return new Anchor(0L, "GENESIS");
        }

        Properties props = new Properties();
        try (java.io.InputStream in = Files.newInputStream(anchorPath)) {
            props.load(in);
        }

        long seq = parseLong(props.getProperty(KEY_SEQ), 0L);
        String lastHash = props.getProperty(KEY_LAST_HASH, "GENESIS");
        String anchorMac = props.getProperty(KEY_ANCHOR_MAC, "");

        String payload = seq + "|" + lastHash;
        byte[] expected = HmacUtils.computeHmacSha256(key, payload.getBytes(StandardCharsets.UTF_8));
        byte[] actual = Base64.getDecoder().decode(anchorMac);
        try {
            if (!java.security.MessageDigest.isEqual(expected, actual)) {
                throw new IOException("Security event anchor integrity check failed");
            }
        } finally {
            java.util.Arrays.fill(expected, (byte) 0);
            java.util.Arrays.fill(actual, (byte) 0);
        }

        return new Anchor(seq, lastHash);
    }

    private static void writeAnchor(long seq, String lastHashBase64, byte[] key) throws IOException {
        Properties props = new Properties();
        props.setProperty(KEY_SEQ, Long.toString(seq));
        props.setProperty(KEY_LAST_HASH, lastHashBase64);

        String payload = seq + "|" + lastHashBase64;
        byte[] mac = HmacUtils.computeHmacSha256(key, payload.getBytes(StandardCharsets.UTF_8));
        try {
            props.setProperty(KEY_ANCHOR_MAC, Base64.getEncoder().encodeToString(mac));
            try (java.io.OutputStream out = Files.newOutputStream(getAnchorPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                props.store(out, "LogHog security event anchor");
            }
            SecurityFilePolicy.ensureOwnerOnlyPermissionsOrThrow(getAnchorPath());
        } finally {
            java.util.Arrays.fill(mac, (byte) 0);
        }
    }

    private static String sanitizeEventType(String input) {
        String value = SecurityFilePolicy.sanitizeLogValue(input);
        String compact = value.replace('|', '_').trim();
        if (compact.isEmpty()) {
            return "UNKNOWN";
        }
        return compact.length() > 64 ? compact.substring(0, 64) : compact;
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static Path getStorageDir() {
        return Paths.get(System.getProperty("user.home"), LOCKOUT_DIR_NAME);
    }

    private static Path getLogPath() {
        return getStorageDir().resolve(LOG_FILE);
    }

    private static Path getAnchorPath() {
        return getStorageDir().resolve(ANCHOR_FILE);
    }

    private static Path getKeyPath() {
        return getStorageDir().resolve(KEY_FILE);
    }

    private static final class Anchor {
        private final long seq;
        private final String lastHashBase64;

        private Anchor(long seq, String lastHashBase64) {
            this.seq = seq;
            this.lastHashBase64 = lastHashBase64;
        }
    }
}
