package encryption;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class HistoricalBackupCompatibilityTest {
    // Independent JCE fixtures reproduce historical writers, not the current encrypt() implementation.
    // 4c50fe1: v1 HMAC; 49f6415: v2/120K; 8eeef7d: v2/600K.
    // July/August snapshots use LOGH v1 and AES PBKDF2/600K in all three cases.
    enum Format { SNAPSHOT, V1_SHA256_BACKUP, V2_120K_BACKUP, V2_600K_BACKUP }

    private static final byte[] SALT = "historical-salt!".getBytes(StandardCharsets.UTF_8);
    private static final List<String> LINES = List.of(".LOG", "", "12:34 2026-07-13", "Historical entry åäö");
    private static final String TEXT = String.join("\n", LINES) + "\n";

    @TempDir Path directory;

    @ParameterizedTest
    @EnumSource(Format.class)
    void historicalLogAndBackupOpenThroughFileLoader(Format format) throws Exception {
        char[] password = "historical-password-åäö".toCharArray();
        byte[] artifact = historicalArtifact(format, password);
        Path file = directory.resolve("log.txt");
        Files.write(file, artifact);
        FileEncryptionManager reader = new FileEncryptionManager(file, EncryptionManager.getInstance());
        try {
            reader.setEncryption(password, SALT);
            assertEquals(LINES, reader.decryptFileToLines());
            assertArrayEquals(artifact, Files.readAllBytes(file), "Opening must not rewrite historical files");
        } finally {
            reader.clearSensitiveData();
            Arrays.fill(password, '\0');
        }
    }

    @ParameterizedTest
    @EnumSource(Format.class)
    void historicalLogAndBackupOpenThroughPasswordByteApi(Format format) throws Exception {
        char[] password = "historical-password-åäö".toCharArray();
        try {
            assertEquals(TEXT, EncryptionManager.getInstance().decrypt(historicalArtifact(format, password), password));
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    @ParameterizedTest
    @EnumSource(Format.class)
    void historicalArtifactsRejectWrongPasswordAndTampering(Format format) throws Exception {
        char[] password = "historical-password-åäö".toCharArray();
        byte[] artifact = historicalArtifact(format, password);
        char[] wrong = "wrong-historical-password".toCharArray();
        Path file = directory.resolve("log.txt");
        FileEncryptionManager reader = new FileEncryptionManager(file, EncryptionManager.getInstance());
        try {
            Files.write(file, artifact);
            reader.setEncryption(wrong, SALT);
            assertThrows(EncryptionException.class, reader::decryptFileToLines);
            assertThrows(EncryptionException.class, () -> EncryptionManager.getInstance().decrypt(artifact, wrong));

            reader.setEncryption(password, SALT);
            byte[] tampered = artifact.clone();
            tampered[35] ^= 1; // First ciphertext byte, after the LOGH/salt/IV header.
            Files.write(file, tampered);
            assertThrows(EncryptionException.class, reader::decryptFileToLines);
            assertThrows(EncryptionException.class, () -> EncryptionManager.getInstance().decrypt(tampered, password));

            if (format != Format.SNAPSHOT) {
                byte[] badSignature = artifact.clone();
                badSignature[badSignature.length - 1] ^= 1;
                Files.write(file, badSignature);
                assertThrows(EncryptionException.class, reader::decryptFileToLines);
                assertThrows(EncryptionException.class, () -> EncryptionManager.getInstance().decrypt(badSignature, password));
            }
        } finally {
            reader.clearSensitiveData();
            Arrays.fill(password, '\0');
            Arrays.fill(wrong, '\0');
        }
    }

    @ParameterizedTest
    @EnumSource(value = Format.class, names = {"V1_SHA256_BACKUP", "V2_120K_BACKUP", "V2_600K_BACKUP"})
    void validBackupSignatureCannotBypassGcmAuthentication(Format format) throws Exception {
        char[] password = "historical-password-åäö".toCharArray();
        byte[] artifact = historicalArtifact(format, password, true);
        Path file = directory.resolve("log.txt");
        Files.write(file, artifact);
        FileEncryptionManager reader = new FileEncryptionManager(file, EncryptionManager.getInstance());
        try {
            reader.setEncryption(password, SALT);
            assertThrows(EncryptionException.class, reader::decryptFileToLines);
            assertThrows(EncryptionException.class, () -> EncryptionManager.getInstance().decrypt(artifact, password));
        } finally {
            reader.clearSensitiveData();
            Arrays.fill(password, '\0');
        }
    }

    private byte[] historicalArtifact(Format format, char[] password) throws Exception {
        return historicalArtifact(format, password, false);
    }

    private byte[] historicalArtifact(Format format, char[] password, boolean corruptCiphertext) throws Exception {
        byte[] aesKey = derive(password, SALT, 600000);
        byte[] iv = "fixture-iv12".getBytes(StandardCharsets.UTF_8);
        byte[] backupKey = null;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(128, iv));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(new byte[] {'L', 'O', 'G', 'H', 1, 16});
            out.write(SALT);
            out.write(12);
            out.write(iv);
            out.write(cipher.doFinal(TEXT.getBytes(StandardCharsets.UTF_8)));
            byte[] snapshot = out.toByteArray();
            if (corruptCiphertext) {
                snapshot[35] ^= 1;
                out.reset();
                out.write(snapshot);
            }
            if (format == Format.SNAPSHOT) return snapshot;

            if (format == Format.V1_SHA256_BACKUP) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                java.nio.ByteBuffer encoded = StandardCharsets.UTF_8.encode(java.nio.CharBuffer.wrap(password));
                byte[] bytes = new byte[encoded.remaining()];
                encoded.get(bytes);
                try {
                    digest.update(bytes);
                    digest.update(SALT);
                    backupKey = digest.digest("loghog-backup-hmac-v1".getBytes(StandardCharsets.UTF_8));
                } finally {
                    Arrays.fill(bytes, (byte) 0);
                    if (encoded.hasArray()) Arrays.fill(encoded.array(), (byte) 0);
                }
            } else {
                byte[] context = "loghog-backup-hmac-v2".getBytes(StandardCharsets.UTF_8);
                byte[] contextualSalt = Arrays.copyOf(SALT, SALT.length + context.length);
                System.arraycopy(context, 0, contextualSalt, SALT.length, context.length);
                backupKey = derive(password, contextualSalt, format == Format.V2_120K_BACKUP ? 120000 : 600000);
            }
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(backupKey, "HmacSHA256"));
            out.write(mac.doFinal(snapshot)); // BackupManager signs the entire file, including LOGH.
            return out.toByteArray();
        } finally {
            Arrays.fill(aesKey, (byte) 0);
            if (backupKey != null) Arrays.fill(backupKey, (byte) 0);
        }
    }

    private byte[] derive(char[] password, byte[] salt, int iterations) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }
}
