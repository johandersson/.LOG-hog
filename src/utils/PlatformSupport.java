package utils;

import java.awt.Desktop;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.util.Locale;
import java.util.function.IntSupplier;

/**
 * Cross-platform capability helpers.
 */
public final class PlatformSupport {
    private PlatformSupport() {}

    public static int menuShortcutMask() {
        return menuShortcutMask(() -> Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    static int menuShortcutMask(IntSupplier maskSupplier) {
        try {
            return maskSupplier.getAsInt();
        } catch (UnsupportedOperationException ex) {
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
        return normalized.startsWith("windows") || normalized.startsWith("win");
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
        } catch (UnsupportedOperationException | SecurityException ex) {
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
        } catch (UnsupportedOperationException ex) {
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
        } catch (UnsupportedOperationException ex) {
            return false;
        }
    }

    public static boolean isSystemTraySupported() {
        try {
            return SystemTray.isSupported();
        } catch (UnsupportedOperationException | SecurityException ex) {
            return false;
        }
    }
}
