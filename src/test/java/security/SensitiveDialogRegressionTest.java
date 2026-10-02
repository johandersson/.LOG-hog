package security;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SensitiveDialogRegressionTest {
    @TempDir Path directory;

    @Test void generatorDisposalClearsGeneratedSecretAndCannotGenerateAgain() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            gui.PasswordGeneratorDialog dialog = new gui.PasswordGeneratorDialog(null);
            try {
                JButton generate = (JButton) field(dialog, "generateButton");
                JTextField result = (JTextField) field(dialog, "resultField");
                generate.doClick();
                assertFalse(result.getText().isEmpty());
                dialog.dispose();
                assertEquals("", result.getText());
                generate.doClick();
                assertEquals("", result.getText(), "disposed sensitive actions must not resume");
            } finally { dialog.dispose(); }
        });
    }

    @Test void failedAuthenticatedLoadWipesActualFileAndBackupKeys() throws Exception {
        authenticate(false);
    }

    @Test void successfulAuthenticationRetainsUsableSessionKeys() throws Exception {
        authenticate(true);
    }

    private void authenticate(boolean success) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        var handler = new filehandling.LogFileHandler(directory.resolve("log.txt"),
            new encryption.TestableEncryptionManager());
        var backup = new main.BackupManager(new Properties());
        var authentication = new main.EncryptionHandler(null, handler, new Properties(),
            () -> { if (!success) throw new IllegalStateException("file read failed"); },
            () -> {}, () -> {}, () -> {}, backup);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            Timer responder = new Timer(50, event -> {
                for (Window window : Window.getWindows()) {
                    if (!window.isVisible()) continue;
                    if (window instanceof gui.PasswordDialog dialog) {
                        JPasswordField password = find(dialog, JPasswordField.class);
                        password.setText("test password");
                        dialog.getRootPane().getDefaultButton().doClick();
                    } else if (window instanceof JDialog dialog && !"Loading".equals(dialog.getTitle())) {
                        window.dispose();
                    }
                }
            });
            responder.start();
            try {
                var attempt = main.EncryptionHandler.class.getDeclaredMethod("performPasswordAuthentication",
                    byte[].class, String.class, boolean.class);
                attempt.setAccessible(true);
                assertEquals(success, attempt.invoke(authentication, new byte[16], "Unlock", false));
                if (success) {
                    assertNotNull(field(handler.getEncryptionManager(), "sessionKeyBytes"));
                    assertNotNull(field(handler.getEncryptionManager(), "backupHmacKeyBytes"));
                } else {
                    assertNull(field(handler.getEncryptionManager(), "sessionKeyBytes"));
                    assertNull(field(handler.getEncryptionManager(), "backupHmacKeyBytes"));
                }
            } catch (Throwable e) { failure.set(e); }
            finally { responder.stop(); handler.clearSensitiveData(); }
        });
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    private static Object field(Object target, String name) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) { throw new AssertionError(e); }
    }
    private static <T extends Component> T find(Container parent, Class<T> type) {
        for (Component component : parent.getComponents()) {
            if (type.isInstance(component)) return type.cast(component);
            if (component instanceof Container child) {
                T found = find(child, type);
                if (found != null) return found;
            }
        }
        return null;
    }
}
