package security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
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
    private static final String SIGNING_PRIVATE_KEY_FILE = "security-events-signing-private.key";
    private static final String SIGNING_PUBLIC_KEY_FILE = "security-events-signing-public.key";
    private static final String KEY_SEQ = "seq";
    private static final String KEY_LAST_HASH = "lastHash";
    private static final String KEY_ANCHOR_MAC = "anchorMac";
    private static final String KEY_ANCHOR_SIG = "anchorSignature";

    private SecurityEventLog() {}

    public static synchronized void appendEvent(String eventType, String details) throws IOException {
        Path dir = getStorageDir();
        Files.createDirectories(dir);
        SecurityFilePolicy.ensureOwnerOnlyPermissionsOrThrow(dir);

        byte[] key = getOrCreateKey();
        try {
            KeyPair signingKeyPair;
            try {
                signingKeyPair = getOrCreateSigningKeyPair();
            } catch (GeneralSecurityException ex) {
                throw new IOException("Unable to access security event signing keypair", ex);
            }
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
            String entrySigBase64;
            try {
                byte[] entrySig = sign(signingKeyPair.getPrivate(), coreBytes);
                try {
                    entrySigBase64 = Base64.getEncoder().encodeToString(entrySig);
                } finally {
                    java.util.Arrays.fill(entrySig, (byte) 0);
                }
            } catch (GeneralSecurityException ex) {
                throw new IOException("Unable to sign security event", ex);
            }

            String line = core + "|" + entryMacBase64 + "|" + entrySigBase64 + System.lineSeparator();
            Files.write(getLogPath(), line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
            SecurityFilePolicy.ensureOwnerOnlyPermissionsOrThrow(getLogPath());

            writeAnchor(nextSeq, hashBase64, key, signingKeyPair.getPrivate());
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
                SensitiveKeyProtector.writeProtected(keyPath, generated, "security-events-hmac-key");
            } finally {
                java.util.Arrays.fill(generated, (byte) 0);
            }
        }

        try {
            return SensitiveKeyProtector.readProtected(keyPath, "security-events-hmac-key");
        } catch (IOException protectedReadFailed) {
            byte[] encoded = Files.readAllBytes(keyPath);
            try {
                byte[] decoded = decodeMaybeBase64(encoded);
                SensitiveKeyProtector.writeProtected(keyPath, decoded, "security-events-hmac-key");
                return decoded;
            } finally {
                java.util.Arrays.fill(encoded, (byte) 0);
            }
        }
    }

    private static Anchor readAnchor(byte[] key) throws IOException {
        Path anchorPath = getAnchorPath();
        if (!Files.exists(anchorPath)) {
            return new Anchor(0L, "GENESIS");
        }

        KeyPair signingKeyPair;
        try {
            signingKeyPair = getOrCreateSigningKeyPair();
        } catch (GeneralSecurityException ex) {
            throw new IOException("Unable to access security event signing keypair", ex);
        }

        Properties props = new Properties();
        try (java.io.InputStream in = Files.newInputStream(anchorPath)) {
            props.load(in);
        }

        long seq = parseLong(props.getProperty(KEY_SEQ), 0L);
        String lastHash = props.getProperty(KEY_LAST_HASH, "GENESIS");
        String anchorMac = props.getProperty(KEY_ANCHOR_MAC, "");
        String anchorSig = props.getProperty(KEY_ANCHOR_SIG, "");

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

        byte[] signature = Base64.getDecoder().decode(anchorSig);
        try {
            byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
            if (!verify(signingKeyPair.getPublic(), payloadBytes, signature)) {
                throw new IOException("Security event anchor signature validation failed");
            }
        } catch (GeneralSecurityException ex) {
            throw new IOException("Unable to verify security event anchor signature", ex);
        } finally {
            java.util.Arrays.fill(signature, (byte) 0);
        }

        return new Anchor(seq, lastHash);
    }

    private static void writeAnchor(long seq, String lastHashBase64, byte[] key, PrivateKey signingPrivateKey) throws IOException {
        Properties props = new Properties();
        props.setProperty(KEY_SEQ, Long.toString(seq));
        props.setProperty(KEY_LAST_HASH, lastHashBase64);

        String payload = seq + "|" + lastHashBase64;
        byte[] mac = HmacUtils.computeHmacSha256(key, payload.getBytes(StandardCharsets.UTF_8));
        byte[] signature = null;
        try {
            signature = sign(signingPrivateKey, payload.getBytes(StandardCharsets.UTF_8));
            props.setProperty(KEY_ANCHOR_MAC, Base64.getEncoder().encodeToString(mac));
            props.setProperty(KEY_ANCHOR_SIG, Base64.getEncoder().encodeToString(signature));
            try (java.io.OutputStream out = Files.newOutputStream(getAnchorPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                props.store(out, "LogHog security event anchor");
            }
            SecurityFilePolicy.ensureOwnerOnlyPermissionsOrThrow(getAnchorPath());
        } catch (GeneralSecurityException ex) {
            throw new IOException("Unable to sign security event anchor", ex);
        } finally {
            java.util.Arrays.fill(mac, (byte) 0);
            if (signature != null) {
                java.util.Arrays.fill(signature, (byte) 0);
            }
        }
    }

    private static KeyPair getOrCreateSigningKeyPair() throws IOException, GeneralSecurityException {
        Path privatePath = getSigningPrivateKeyPath();
        Path publicPath = getSigningPublicKeyPath();
        if (!Files.exists(privatePath) || !Files.exists(publicPath)) {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
            KeyPair generated = kpg.generateKeyPair();
            SensitiveKeyProtector.writeProtected(privatePath, generated.getPrivate().getEncoded(), "security-events-signing-private");
            Files.write(publicPath, Base64.getEncoder().encode(generated.getPublic().getEncoded()));
            SecurityFilePolicy.ensureOwnerOnlyPermissionsOrThrow(privatePath);
            SecurityFilePolicy.ensureOwnerOnlyPermissionsOrThrow(publicPath);
            return generated;
        }

        byte[] privateEncoded;
        try {
            privateEncoded = SensitiveKeyProtector.readProtected(privatePath, "security-events-signing-private");
        } catch (IOException protectedReadFailed) {
            byte[] privateEncodedB64 = Files.readAllBytes(privatePath);
            try {
                privateEncoded = decodeMaybeBase64(privateEncodedB64);
                SensitiveKeyProtector.writeProtected(privatePath, privateEncoded, "security-events-signing-private");
            } finally {
                java.util.Arrays.fill(privateEncodedB64, (byte) 0);
            }
        }
        byte[] publicEncodedB64 = Files.readAllBytes(publicPath);
        try {
            byte[] publicEncoded = Base64.getDecoder().decode(publicEncodedB64);
            try {
                KeyFactory kf = KeyFactory.getInstance("Ed25519");
                PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privateEncoded));
                PublicKey publicKey = kf.generatePublic(new X509EncodedKeySpec(publicEncoded));
                return new KeyPair(publicKey, privateKey);
            } finally {
                java.util.Arrays.fill(privateEncoded, (byte) 0);
                java.util.Arrays.fill(publicEncoded, (byte) 0);
            }
        } finally {
            java.util.Arrays.fill(publicEncodedB64, (byte) 0);
        }
    }

    private static byte[] decodeMaybeBase64(byte[] raw) {
        try {
            return Base64.getDecoder().decode(raw);
        } catch (IllegalArgumentException ex) {
            return raw.clone();
        }
    }

    private static byte[] sign(PrivateKey privateKey, byte[] payload) throws GeneralSecurityException {
        Signature signature = Signature.getInstance("Ed25519");
        signature.initSign(privateKey);
        signature.update(payload);
        return signature.sign();
    }

    private static boolean verify(PublicKey publicKey, byte[] payload, byte[] sig) throws GeneralSecurityException {
        Signature signature = Signature.getInstance("Ed25519");
        signature.initVerify(publicKey);
        signature.update(payload);
        return signature.verify(sig);
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

    private static Path getSigningPrivateKeyPath() {
        return getStorageDir().resolve(SIGNING_PRIVATE_KEY_FILE);
    }

    private static Path getSigningPublicKeyPath() {
        return getStorageDir().resolve(SIGNING_PUBLIC_KEY_FILE);
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
