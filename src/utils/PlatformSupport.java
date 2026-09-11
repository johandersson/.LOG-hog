package utils;

import java.awt.Desktop;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.util.Locale;

/**
 * Cross-platform capability helpers.
 */
public final class PlatformSupport {
    private PlatformSupport() {}

    public static int menuShortcutMask() {
        try {
            return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        } catch (HeadlessException | UnsupportedOperationException ex) {
            return InputEvent.CTRL_DOWN_MASK;
        }
    }

    public static String primaryShortcutLabel() {
        return isMacOs() ? "Cmd" : "Ctrl";
    }

    public static boolean isMacOs() {
        return isMacOs(System.getProperty("os.name", ""));
    }

    static boolean isMacOs(String osName) {
        String normalized = normalizeOsName(osName);
        return normalized.contains("mac");
    }

    static boolean isWindows(String osName) {
        String normalized = normalizeOsName(osName);
        return normalized.contains("win");
    }

    static boolean isLinux(String osName) {
        String normalized = normalizeOsName(osName);
        return normalized.contains("linux");
    }

    private static String normalizeOsName(String osName) {
        return osName == null ? "" : osName.toLowerCase(Locale.ROOT);
    }

    public static Desktop getDesktopForAction(Desktop.Action action) {
        if (action == null || !Desktop.isDesktopSupported()) {
            return null;
        }
        try {
            Desktop desktop = Desktop.getDesktop();
            return desktop.isSupported(action) ? desktop : null;
        } catch (HeadlessException | UnsupportedOperationException | SecurityException ex) {
            return null;
        }
    }

    public static boolean supportsShapedWindows() {
        if (GraphicsEnvironment.isHeadless()) {
            return false;
        }
        try {
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            return gd != null && gd.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT);
        } catch (HeadlessException | UnsupportedOperationException ex) {
            return false;
        }
    }

    public static boolean supportsWindowOpacity() {
        if (GraphicsEnvironment.isHeadless()) {
            return false;
        }
        try {
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            return gd != null && gd.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT);
        } catch (HeadlessException | UnsupportedOperationException ex) {
            return false;
        }

        public static boolean isSystemTraySupported() {
            try {
                return SystemTray.isSupported();
            } catch (HeadlessException | UnsupportedOperationException | SecurityException ex) {
                return false;
            }
        }
    }
}
