package main;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JDialog;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import encryption.EncryptionManager;
import encryption.FileEncryptionManager;
import filehandling.LogFileHandler;
import gui.PasswordDialog;

class EncryptionHandlerRestoreTest {
    @TempDir Path directory;

    @BeforeEach
    void requireGraphics() {
        org.junit.jupiter.api.Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
    }

    @ParameterizedTest
    @CsvSource({"true,missing", "true,invalid", "true,stale", "false,missing", "false,invalid", "false,stale"})
    void startupAndReloadUseHeaderSaltAndRepairSettingsAfterUnlock(boolean startup, String metadata) throws Exception {
        Path file = directory.resolve("log.txt");
        char[] password = "restore-authentication-test".toCharArray();
        byte[] salt = EncryptionManager.getInstance().generateSalt();
        FileEncryptionManager writer = new FileEncryptionManager(file, EncryptionManager.getInstance());
        try {
            writer.setEncryption(password, salt);
            writer.encryptFileFromLines(List.of(".LOG", "", "12:34 2026-09-11", "Recovered"));
        } finally {
            writer.clearSensitiveData();
        }

        Properties settings = new Properties();
        if ("invalid".equals(metadata)) settings.setProperty("salt", "not valid base64!");
        if ("stale".equals(metadata)) settings.setProperty("salt", Base64.getEncoder().encodeToString(new byte[16]));
        AtomicInteger saves = new AtomicInteger();
        AtomicInteger loads = new AtomicInteger();
        BackupManager backups = new BackupManager(settings);
        LogFileHandler log = new LogFileHandler(file, EncryptionManager.getInstance());
        EncryptionHandler handler = new EncryptionHandler(null, log, settings, () -> {
            try {
                assertEquals(List.of("12:34 2026-09-11", "Recovered"), log.getLines());
                assertEquals(0, saves.get(), "Do not persist recovered metadata before authenticating");
                loads.incrementAndGet();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, () -> {}, () -> {}, saves::incrementAndGet, backups);
        AtomicBoolean unlocked = new AtomicBoolean();
        try {
            driveDialogs(() -> unlocked.set(startup ? handler.handleEncryptionSetup() : handler.reloadEncryptedLog()),
                "restore-authentication-test");
            assertTrue(unlocked.get());
            assertEquals(1, loads.get());
            assertEquals(1, saves.get());
            assertEquals(Base64.getEncoder().encodeToString(salt), settings.getProperty("salt"));
            assertArrayEquals(salt, log.getSalt());
        } finally {
            log.clearSensitiveData();
            backups.clearInMemoryHmacKey();
            java.util.Arrays.fill(password, '\0');
        }
    }

    @Test
    void cancellingRecoveryDoesNotPersistUnverifiedMetadata() throws Exception {
        Path file = directory.resolve("log.txt");
        byte[] salt = EncryptionManager.getInstance().generateSalt();
        char[] password = "recovery-cancel-test".toCharArray();
        FileEncryptionManager writer = new FileEncryptionManager(file, EncryptionManager.getInstance());
        try {
            writer.setEncryption(password, salt);
            writer.encryptFileFromLines(List.of(".LOG", "", "12:34 2026-09-11", "Recovered"));
        } finally {
            writer.clearSensitiveData();
            java.util.Arrays.fill(password, '\0');
        }
        Properties settings = new Properties();
        AtomicInteger saves = new AtomicInteger();
        LogFileHandler log = new LogFileHandler(file, EncryptionManager.getInstance());
        BackupManager backups = new BackupManager(settings);
        EncryptionHandler handler = new EncryptionHandler(null, log, settings,
            () -> fail("Cancelled authentication must not load entries"), () -> {}, () -> {},
            saves::incrementAndGet, backups);
        try {
            driveDialogs(() -> assertFalse(handler.reloadEncryptedLog()), null);
            assertEquals(0, saves.get());
            assertNull(settings.getProperty("salt"));
            assertFalse(log.getEncryptionManager().isEncrypted());
        } finally {
            log.clearSensitiveData();
            backups.clearInMemoryHmacKey();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void successfulEditorStartupActuallyUnlocksRestoredLog(boolean settingsExist) throws Exception {
        Path home = Files.createDirectory(directory.resolve("home"));
        Path file = home.resolve("log.txt");
        char[] password = "editor-startup-restore-test".toCharArray();
        FileEncryptionManager writer = new FileEncryptionManager(file, EncryptionManager.getInstance());
        try {
            writer.setEncryption(password, EncryptionManager.getInstance().generateSalt());
            writer.encryptFileFromLines(List.of(".LOG", "", "12:34 2026-09-11", "Recovered"));
        } finally {
            writer.clearSensitiveData();
            java.util.Arrays.fill(password, '\0');
        }
        if (settingsExist) {
            Path configuration = Files.createDirectory(home.resolve(".loghog"));
            Files.writeString(configuration.resolve("settings.properties"), "showSplashOnStartup=false\n");
        }
        String originalHome = System.getProperty("user.home");
        AtomicReference<LogTextEditor> editor = new AtomicReference<>();
        try {
            System.setProperty("user.home", home.toString());
            driveDialogs(() -> editor.set(new LogTextEditor()), "editor-startup-restore-test");
            assertFalse(editor.get().isLocked(), "Successful startup authentication must unlock the editor");
            assertEquals(List.of("12:34 2026-09-11", "Recovered"), editor.get().getLogFileHandler().getLines());
        } finally {
            if (editor.get() != null) {
                SwingUtilities.invokeAndWait(() -> {
                    editor.get().shutdown();
                    editor.get().dispose();
                });
            }
            System.setProperty("user.home", originalHome);
        }
    }

    private void driveDialogs(Runnable action, String password) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Timer driver = new Timer(50, event -> {
                for (Window window : Window.getWindows()) {
                    if (!window.isVisible()) continue;
                    if (window instanceof PasswordDialog dialog) {
                        if (password == null) {
                            dialog.dispatchEvent(new java.awt.event.WindowEvent(dialog,
                                java.awt.event.WindowEvent.WINDOW_CLOSING));
                        } else {
                            fillPassword(dialog, password);
                            dialog.getRootPane().getDefaultButton().doClick();
                        }
                    } else if (window instanceof JDialog dialog && dialog.isModal()) {
                        dialog.dispose();
                    }
                }
            });
            driver.start();
            try {
                action.run();
            } finally {
                driver.stop();
                for (Window window : Window.getWindows()) {
                    if (window instanceof JDialog) window.dispose();
                }
            }
        });
    }

    private void fillPassword(Container container, String password) {
        for (Component component : container.getComponents()) {
            if (component instanceof JPasswordField field) field.setText(password);
            else if (component instanceof Container nested) fillPassword(nested, password);
        }
    }
}
