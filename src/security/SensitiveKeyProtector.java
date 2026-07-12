package security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Protects sensitive key blobs at rest using a host-bound wrapping key.
 */
public final class SensitiveKeyProtector {
    private static final byte[] MAGIC = new byte[] { 'L', 'H', 'K', '1' };
    private static final byte[] DPAPI_MAGIC = new byte[] { 'L', 'H', 'D', '1' };
    private static final int PBKDF2_ITERATIONS = 600000;
    private static final int KEY_BITS = 256;
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final String KDF = "PBKDF2WithHmacSHA256";
    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final String SALT_FILE = "key-protection.salt";
    private static final long DPAPI_TIMEOUT_SECONDS = 10;

    private SensitiveKeyProtector() {}

    public static void writeProtected(Path path, byte[] plaintext, String purpose) throws IOException {
        if (path == null || plaintext == null) {
            throw new IOException("Protected key write failed: invalid input");
        }
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
            SecurityFilePolicy.ensureOwnerOnlyPermissionsOrThrow(parent);
        }

        if (isWindows()) {
            byte[] dpapiWrapped = null;
            try {
                dpapiWrapped = wrapDpapi(plaintext);
                Files.write(path, dpapiWrapped);
                SecurityFilePolicy.ensureOwnerOnlyPermissionsOrThrow(path);
                return;
            } catch (IOException ignored) {
                // Fall back to cross-platform envelope if DPAPI is unavailable.
            } finally {
                zeroize(dpapiWrapped);
            }
        }

        byte[] wrappingSalt = getOrCreateWrappingSalt();
        byte[] wrapped = null;
        try {
            wrapped = encrypt(plaintext, purpose, wrappingSalt);
            Files.write(path, wrapped);
            SecurityFilePolicy.ensureOwnerOnlyPermissionsOrThrow(path);
        } finally {
            zeroize(wrapped);
            zeroize(wrappingSalt);
        }
    }

    public static byte[] readProtected(Path path, String purpose) throws IOException {
        if (path == null || !Files.exists(path)) {
            throw new IOException("Protected key not found");
        }
        byte[] raw = Files.readAllBytes(path);
        if (startsWith(raw, DPAPI_MAGIC)) {
            try {
                return unwrapDpapi(raw);
            } finally {
                zeroize(raw);
            }
        }

        byte[] wrappingSalt = getOrCreateWrappingSalt();
        byte[] wrapped = raw;
        try {
            byte[] plaintext = decrypt(wrapped, purpose, wrappingSalt);
            if (isWindows()) {
                // Opportunistic migration to DPAPI-backed envelope on Windows.
                writeProtected(path, plaintext, purpose);
            }
            return plaintext;
        } finally {
            zeroize(wrapped);
            zeroize(wrappingSalt);
        }
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data == null || prefix == null || data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }

    private static byte[] wrapDpapi(byte[] plaintext) throws IOException {
        byte[] protectedPayload = dpapiProtect(plaintext);
        byte[] out = new byte[DPAPI_MAGIC.length + protectedPayload.length];
        System.arraycopy(DPAPI_MAGIC, 0, out, 0, DPAPI_MAGIC.length);
        System.arraycopy(protectedPayload, 0, out, DPAPI_MAGIC.length, protectedPayload.length);
        zeroize(protectedPayload);
        return out;
    }

    private static byte[] unwrapDpapi(byte[] wrapped) throws IOException {
        if (wrapped.length <= DPAPI_MAGIC.length) {
            throw new IOException("DPAPI payload is empty");
        }
        byte[] payload = Arrays.copyOfRange(wrapped, DPAPI_MAGIC.length, wrapped.length);
        try {
            return dpapiUnprotect(payload);
        } finally {
            zeroize(payload);
        }
    }

    private static byte[] dpapiProtect(byte[] plaintext) throws IOException {
        String inputB64 = java.util.Base64.getEncoder().encodeToString(plaintext);
        String output = runPowerShellDpapi(inputB64, true);
        try {
            return java.util.Base64.getDecoder().decode(output);
        } catch (IllegalArgumentException ex) {
            throw new IOException("Invalid DPAPI output", ex);
        }
    }

    private static byte[] dpapiUnprotect(byte[] protectedPayload) throws IOException {
        String inputB64 = java.util.Base64.getEncoder().encodeToString(protectedPayload);
        String output = runPowerShellDpapi(inputB64, false);
        try {
            return java.util.Base64.getDecoder().decode(output);
        } catch (IllegalArgumentException ex) {
            throw new IOException("Invalid DPAPI output", ex);
        }
    }

    private static String runPowerShellDpapi(String inputB64, boolean protect) throws IOException {
        if (inputB64 == null || inputB64.isEmpty()) {
            throw new IOException("DPAPI input is empty");
        }
        if (!inputB64.matches("^[A-Za-z0-9+/=]+$")) {
            throw new IOException("DPAPI input format is invalid");
        }

        String script;
        if (protect) {
            script = "$rawIn=[Console]::In.ReadToEnd().Trim();"
                + "$bytes=[Convert]::FromBase64String($rawIn);"
                + "$out=[Security.Cryptography.ProtectedData]::Protect($bytes,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser);"
                + "[Console]::Out.Write([Convert]::ToBase64String($out))";
        } else {
            script = "$rawIn=[Console]::In.ReadToEnd().Trim();"
                + "$bytes=[Convert]::FromBase64String($rawIn);"
                + "$out=[Security.Cryptography.ProtectedData]::Unprotect($bytes,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser);"
                + "[Console]::Out.Write([Convert]::ToBase64String($out))";
        }

        Process process;
        try {
            process = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-Command", script)
                .start();
        } catch (IOException ex) {
            throw new IOException("PowerShell unavailable for DPAPI", ex);
        }

        byte[] outputBytes = null;
        byte[] errorBytes = null;
        try (java.io.OutputStream stdin = process.getOutputStream()) {
            stdin.write(inputB64.getBytes(StandardCharsets.US_ASCII));
            stdin.flush();
        }
        try (java.io.InputStream in = process.getInputStream()) {
            outputBytes = in.readAllBytes();
        }
        try (java.io.InputStream err = process.getErrorStream()) {
            errorBytes = err.readAllBytes();
        }
        try {
            boolean finished = process.waitFor(DPAPI_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("DPAPI command timed out");
            }
            int code = process.exitValue();
            String output = new String(outputBytes, StandardCharsets.UTF_8).trim();
            if (code != 0 || output.isEmpty()) {
                throw new IOException("DPAPI command failed");
            }
            if (output.length() > 32768 || !output.matches("^[A-Za-z0-9+/=]+$")) {
                throw new IOException("DPAPI command failed");
            }
            return output;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("DPAPI command interrupted", ex);
        } finally {
            zeroize(outputBytes);
            zeroize(errorBytes);
        }
    }

    private static byte[] getOrCreateWrappingSalt() throws IOException {
        Path saltPath = AppPathPolicy.appDataDirectory().resolve(SALT_FILE);
        Files.createDirectories(saltPath.getParent());

        if (!Files.exists(saltPath)) {
            byte[] generated = new byte[16];
            new SecureRandom().nextBytes(generated);
            try {
                Files.write(saltPath, generated);
                SecurityFilePolicy.ensureOwnerOnlyPermissionsOrThrow(saltPath);
            } finally {
                zeroize(generated);
            }
        }

        return Files.readAllBytes(saltPath);
    }

    private static byte[] encrypt(byte[] plaintext, String purpose, byte[] wrappingSalt) throws IOException {
        byte[] hostKey = null;
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] ciphertext;
        try {
            hostKey = deriveHostWrappingKey(wrappingSalt);
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, iv);
            SecretKeySpec key = new SecretKeySpec(hostKey, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            cipher.updateAAD(aad(purpose));
            ciphertext = cipher.doFinal(plaintext);
        } catch (GeneralSecurityException ex) {
            throw new IOException("Failed to protect sensitive key", ex);
        } finally {
            zeroize(hostKey);
        }

        byte[] output = new byte[MAGIC.length + 1 + iv.length + ciphertext.length];
        System.arraycopy(MAGIC, 0, output, 0, MAGIC.length);
        output[MAGIC.length] = (byte) iv.length;
        System.arraycopy(iv, 0, output, MAGIC.length + 1, iv.length);
        System.arraycopy(ciphertext, 0, output, MAGIC.length + 1 + iv.length, ciphertext.length);
        zeroize(iv);
        zeroize(ciphertext);
        return output;
    }

    private static byte[] decrypt(byte[] wrapped, String purpose, byte[] wrappingSalt) throws IOException {
        if (wrapped.length < MAGIC.length + 1 + GCM_IV_LENGTH + 1) {
            throw new IOException("Protected key data is too short");
        }
        for (int i = 0; i < MAGIC.length; i++) {
            if (wrapped[i] != MAGIC[i]) {
                throw new IOException("Protected key format mismatch");
            }
        }
        int ivLen = wrapped[MAGIC.length] & 0xFF;
        if (ivLen < 12 || wrapped.length <= MAGIC.length + 1 + ivLen) {
            throw new IOException("Protected key IV is invalid");
        }

        byte[] iv = Arrays.copyOfRange(wrapped, MAGIC.length + 1, MAGIC.length + 1 + ivLen);
        byte[] ciphertext = Arrays.copyOfRange(wrapped, MAGIC.length + 1 + ivLen, wrapped.length);
        byte[] hostKey = null;
        try {
            hostKey = deriveHostWrappingKey(wrappingSalt);
            Cipher cipher = Cipher.getInstance(CIPHER);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, iv);
            SecretKeySpec key = new SecretKeySpec(hostKey, "AES");
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            cipher.updateAAD(aad(purpose));
            return cipher.doFinal(ciphertext);
        } catch (GeneralSecurityException ex) {
            throw new IOException("Unable to unprotect sensitive key", ex);
        } finally {
            zeroize(iv);
            zeroize(ciphertext);
            zeroize(hostKey);
        }
    }

    private static byte[] deriveHostWrappingKey(byte[] wrappingSalt) throws IOException {
        String fingerprint = String.join("|",
            "loghog-key-protection-v1",
            System.getProperty("user.name", ""),
            System.getProperty("user.home", ""),
            System.getProperty("os.name", ""),
            System.getProperty("os.arch", ""),
            System.getProperty("java.vendor", ""));

        char[] chars = fingerprint.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(chars, wrappingSalt, PBKDF2_ITERATIONS, KEY_BITS);
        Arrays.fill(chars, '\0');
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(KDF);
            return skf.generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IOException("Unable to derive key protection material", ex);
        } finally {
            spec.clearPassword();
        }
    }

    private static byte[] aad(String purpose) {
        String normalized = purpose == null ? "generic" : purpose.trim().toLowerCase();
        return ("loghog-aad|" + normalized).getBytes(StandardCharsets.UTF_8);
    }

    private static void zeroize(byte[] data) {
        if (data != null) {
            Arrays.fill(data, (byte) 0);
        }
    }
}
