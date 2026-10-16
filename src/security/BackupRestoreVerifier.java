package security;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Validates backup files before restore operations.
 */
public final class BackupRestoreVerifier {
    private BackupRestoreVerifier() {}

    public static boolean isValidEncryptedBackup(Path backupPath) {
        if (backupPath == null || !Files.exists(backupPath) || !Files.isRegularFile(backupPath)) {
            return false;
        }
        return encryption.EncryptionDetector.isFileEncrypted(backupPath);
    }

    /**
     * Returns the complete signed file only after verifying its trailing HMAC.
     */
    public static byte[] stripVerifiedHmac(byte[] artifact, byte[][] verificationKeys) {
        final int hmacSize = 32;
        if (artifact == null || artifact.length <= hmacSize || verificationKeys == null) return null;
        byte[] payload = Arrays.copyOf(artifact, artifact.length - hmacSize);
        byte[] signature = Arrays.copyOfRange(artifact, payload.length, artifact.length);
        boolean verified = false;
        try {
            for (byte[] key : verificationKeys) {
                byte[] expected = null;
                try {
                    Mac mac = Mac.getInstance("HmacSHA256");
                    mac.init(new SecretKeySpec(key, "HmacSHA256"));
                    expected = mac.doFinal(payload);
                    if (MessageDigest.isEqual(expected, signature)) {
                        verified = true;
                        return payload;
                    }
                } finally {
                    if (expected != null) Arrays.fill(expected, (byte) 0);
                }
            }
            return null;
        } catch (java.security.GeneralSecurityException e) {
            return null;
        } finally {
            Arrays.fill(signature, (byte) 0);
            if (!verified) Arrays.fill(payload, (byte) 0);
        }
    }
}