package main;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import encryption.EncryptionDetector;
import encryption.EncryptionException;
import encryption.EncryptionManager;
import encryption.FileEncryptionManager;
import filehandling.LogFileHandler;

class BackupRestoreWorkflowTest {
    private static final String TIMESTAMP = "12:34 2026-09-11";
    private static final List<String> CONTENT = List.of(TIMESTAMP, "Recovered entry");

    enum BackupType { AUTOMATIC, MANUAL_COPY }

    @TempDir
    Path tempDir;

    private String originalHome;
    private Path logPath;
    private Path backupDir;
    private byte[] salt;
    private final char[] password = "backup-restore-test-password".toCharArray();

    @BeforeEach
    void setUp() throws Exception {
        originalHome = System.getProperty("user.home");
        Path sourceHome = Files.createDirectory(tempDir.resolve("source-home"));
        System.setProperty("user.home", sourceHome.toString());
        logPath = sourceHome.resolve("log.txt");
        backupDir = Files.createDirectory(tempDir.resolve("backups"));
        salt = EncryptionManager.getInstance().generateSalt();

        FileEncryptionManager writer = new FileEncryptionManager(logPath, EncryptionManager.getInstance());
        try {
            writer.setEncryption(password, salt);
            writer.encryptFileFromLines(List.of(".LOG", "", TIMESTAMP, "Recovered entry"));
        } finally {
            writer.clearSensitiveData();
        }
    }

    @AfterEach
    void tearDown() {
        if (originalHome == null) {
            System.clearProperty("user.home");
        } else {
            System.setProperty("user.home", originalHome);
        }
        Arrays.fill(password, '\0');
        if (salt != null) {
            Arrays.fill(salt, (byte) 0);
        }
    }

    @ParameterizedTest
    @EnumSource(BackupType.class)
    void renamedBackupLoadsWithCorrectPasswordAndMatchingSalt(BackupType type) throws Exception {
        Path restoredPath = restoreAsHomeLog(createBackup(type));
        assertTrue(EncryptionDetector.isFileEncrypted(restoredPath));
        assertArrayEquals(salt, EncryptionDetector.extractSaltFromHeader(restoredPath));

        LogFileHandler restored = new LogFileHandler(restoredPath, EncryptionManager.getInstance());
        try {
            restored.setEncryption(password, salt);
            assertRecoveredEntry(restored);
        } finally {
            restored.clearSensitiveData();
        }
    }

    @ParameterizedTest
    @EnumSource(BackupType.class)
    void correctPasswordOpensBackupEvenWhenSettingsSaltDiffersFromHeader(BackupType type) throws Exception {
        Path restoredPath = restoreAsHomeLog(createBackup(type));
        byte[] differentSalt = salt.clone();
        differentSalt[0] ^= 1;
        Properties currentSettings = new Properties();
        currentSettings.setProperty("salt", Base64.getEncoder().encodeToString(differentSalt));

        LogFileHandler restored = new LogFileHandler(restoredPath, EncryptionManager.getInstance());
        try {
            restored.setEncryption(password, Base64.getDecoder().decode(currentSettings.getProperty("salt")));
            assertRecoveredEntry(restored);
            assertArrayEquals(salt, restored.getSalt());
        } finally {
            restored.clearSensitiveData();
            Arrays.fill(differentSalt, (byte) 0);
        }
    }

    @ParameterizedTest
    @EnumSource(BackupType.class)
    void backupCanRecoverSaltWithoutSettings(BackupType type) throws Exception {
        Path restoredPath = restoreAsHomeLog(createBackup(type));
        byte[] beforeUnlock = Files.readAllBytes(restoredPath);
        LogFileHandler restored = new LogFileHandler(restoredPath, EncryptionManager.getInstance());
        try {
            restored.setEncryption(password, null);
            assertRecoveredEntry(restored);
            assertArrayEquals(salt, restored.getSalt());
            assertArrayEquals(beforeUnlock, Files.readAllBytes(restoredPath));
        } finally {
            restored.clearSensitiveData();
        }
    }

    @org.junit.jupiter.api.Test
    void settingsRestoreReplacesActiveSnapshotAndDiscardsItsJournal() throws Exception {
        Path backup = backupDir.resolve("restore-source.enc");
        FileEncryptionManager backupWriter = new FileEncryptionManager(backup, EncryptionManager.getInstance());
        try {
            backupWriter.setEncryption(password, salt);
            backupWriter.encryptFileFromLines(List.of(".LOG", "", "09:10 2026-09-12", "Restored entry"));
        } finally {
            backupWriter.clearSensitiveData();
        }

        Path journal = logPath.resolveSibling(logPath.getFileName().toString() + ".journal.enc");
        Files.writeString(journal, "stale journal");
        LogFileHandler handler = new LogFileHandler(logPath, EncryptionManager.getInstance());
        handler.restoreBackup(backup);
        assertFalse(Files.exists(journal));

        try {
            handler.setEncryption(password, salt);
            assertEquals(List.of("09:10 2026-09-12", "Restored entry"), handler.getLines());
        } finally {
            handler.clearSensitiveData();
        }
    }

    @ParameterizedTest
    @EnumSource(BackupType.class)
    void renamedBackupStillRejectsWrongPassword(BackupType type) throws Exception {
        Path restoredPath = restoreAsHomeLog(createBackup(type));
        char[] wrongPassword = "wrong-backup-test-password".toCharArray();
        LogFileHandler restored = new LogFileHandler(restoredPath, EncryptionManager.getInstance());
        try {
            restored.setEncryption(wrongPassword, salt);
            assertThrows(EncryptionException.class,
                () -> restored.loadLogEntries(new DefaultListModel<>()));
            SwingUtilities.invokeAndWait(() -> {});
        } finally {
            restored.clearSensitiveData();
            Arrays.fill(wrongPassword, '\0');
        }
    }

    private Path createBackup(BackupType type) throws Exception {
        byte[] original = Files.readAllBytes(logPath);
        Path backup;
        if (type == BackupType.AUTOMATIC) {
            Properties settings = new Properties();
            settings.setProperty("autoBackupEnabled", "true");
            settings.setProperty("backupDirectory", backupDir.toString());
            BackupManager manager = new BackupManager(settings);
            try {
                manager.deriveAndSetHmacKey(password, salt);
                manager.performAutomaticBackup();
                try (var files = Files.list(backupDir)) {
                    backup = files.filter(path -> path.getFileName().toString().endsWith(".enc"))
                        .findFirst().orElseThrow();
                }
            } finally {
                manager.clearInMemoryHmacKey();
            }
            assertEquals(original.length + 32L, Files.size(backup));
        } else {
            // Mock only SettingsPanel's chooser/background task; its manual backup is a raw copy.
            backup = backupDir.resolve("loghog-backup-manual.enc");
            Files.copy(logPath, backup);
            assertArrayEquals(original, Files.readAllBytes(backup));
        }
        assertArrayEquals(original, Arrays.copyOf(Files.readAllBytes(backup), original.length));
        return backup;
    }

    private Path restoreAsHomeLog(Path backup) throws Exception {
        Path restoredHome = Files.createDirectory(tempDir.resolve("restored-home"));
        Path restoredPath = restoredHome.resolve("log.txt");
        Files.copy(backup, restoredPath, StandardCopyOption.REPLACE_EXISTING);
        System.setProperty("user.home", restoredHome.toString());
        return restoredPath;
    }

    private void assertRecoveredEntry(LogFileHandler restored) throws Exception {
        DefaultListModel<String> model = new DefaultListModel<>();
        restored.loadLogEntries(model);
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals(List.of(TIMESTAMP), java.util.Collections.list(model.elements()));
        assertEquals(CONTENT, restored.getLines());
    }
}
