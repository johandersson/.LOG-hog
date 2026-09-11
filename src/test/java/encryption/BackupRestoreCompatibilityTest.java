package encryption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import security.BackupKeyDerivation;

class BackupRestoreCompatibilityTest {

    @TempDir
    Path tempDir;

    @Test
    void restoredBackupWithAppendedHmacCanBeOpened() throws Exception {
        Path logPath = tempDir.resolve("log.txt");
        EncryptionManager manager = EncryptionManager.getInstance();
        byte[] salt = manager.generateSalt();
        char[] password = "correct horse battery staple".toCharArray();

        List<String> lines = new ArrayList<>();
        lines.add(".LOG");
        lines.add("");
        lines.add("12:34 2026-09-11");
        lines.add("Recovered entry");

        FileEncryptionManager writer = new FileEncryptionManager(logPath, manager);
        writer.setEncryption(password, salt);
        writer.encryptFileFromLines(lines);

        byte[] encryptedFile = Files.readAllBytes(logPath);
        byte[] backupArtifact = appendBackupHmac(encryptedFile, password, salt);
        Files.write(logPath, backupArtifact);

        FileEncryptionManager restored = new FileEncryptionManager(logPath, manager);
        restored.setEncryption(password, salt);
        List<String> decrypted = restored.decryptFileToLines();

        assertEquals(lines, decrypted);
    }

    @Test
    void restoredBackupWithAppendedHmacStillRejectsWrongPassword() throws Exception {
        Path logPath = tempDir.resolve("log.txt");
        EncryptionManager manager = EncryptionManager.getInstance();
        byte[] salt = manager.generateSalt();
        char[] password = "this-is-the-right-password".toCharArray();

        FileEncryptionManager writer = new FileEncryptionManager(logPath, manager);
        writer.setEncryption(password, salt);
        writer.encryptFileFromLines(List.of(".LOG", "", "09:00 2026-09-11", "Secret"));

        byte[] encryptedFile = Files.readAllBytes(logPath);
        Files.write(logPath, appendBackupHmac(encryptedFile, password, salt));

        FileEncryptionManager restored = new FileEncryptionManager(logPath, manager);
        restored.setEncryption("definitely-wrong-password".toCharArray(), salt);
        assertThrows(EncryptionException.class, restored::decryptFileToLines);
    }

    @Test
    void nonBackupTrailingBytesAreStillRejected() throws Exception {
        Path logPath = tempDir.resolve("log.txt");
        EncryptionManager manager = EncryptionManager.getInstance();
        byte[] salt = manager.generateSalt();
        char[] password = "valid-password".toCharArray();

        FileEncryptionManager writer = new FileEncryptionManager(logPath, manager);
        writer.setEncryption(password, salt);
        writer.encryptFileFromLines(List.of(".LOG", "", "15:42 2026-09-11", "Original"));

        byte[] encryptedFile = Files.readAllBytes(logPath);
        byte[] randomSuffix = new byte[32];
        new SecureRandom().nextBytes(randomSuffix);
        byte[] tampered = Arrays.copyOf(encryptedFile, encryptedFile.length + randomSuffix.length);
        System.arraycopy(randomSuffix, 0, tampered, encryptedFile.length, randomSuffix.length);
        Files.write(logPath, tampered);

        FileEncryptionManager restored = new FileEncryptionManager(logPath, manager);
        restored.setEncryption(password, salt);
        assertThrows(EncryptionException.class, restored::decryptFileToLines);
    }

    private byte[] appendBackupHmac(byte[] encryptedFileBytes, char[] password, byte[] salt) throws Exception {
        byte[] backupKey = BackupKeyDerivation.deriveV2(password, salt);
        byte[] hmac = computeHmacSha256(backupKey, encryptedFileBytes);
        byte[] result = Arrays.copyOf(encryptedFileBytes, encryptedFileBytes.length + hmac.length);
        System.arraycopy(hmac, 0, result, encryptedFileBytes.length, hmac.length);
        return result;
    }

    private byte[] computeHmacSha256(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }
}
