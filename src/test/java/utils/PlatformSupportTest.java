package utils;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Desktop;

import org.junit.jupiter.api.Test;

class PlatformSupportTest {

    @Test
    void osDetectionHandlesCommonNames() {
        assertTrue(PlatformSupport.isMacOs("Mac OS X"));
        assertTrue(PlatformSupport.isWindows("Windows 11"));
        assertTrue(PlatformSupport.isLinux("Linux"));
        assertFalse(PlatformSupport.isMacOs("Windows 10"));
    }

    @Test
    void menuShortcutMaskReturnsValidModifier() {
        int mask = PlatformSupport.menuShortcutMask();
        assertNotEquals(0, mask);
    }

    @Test
    void desktopActionLookupIsNullSafe() {
        assertNull(PlatformSupport.getDesktopForAction(null));
        assertDoesNotThrow(() -> PlatformSupport.getDesktopForAction(Desktop.Action.BROWSE));
        assertDoesNotThrow(() -> PlatformSupport.getDesktopForAction(Desktop.Action.OPEN));
    }

    @Test
    void capabilityChecksDoNotThrow() {
        assertDoesNotThrow(PlatformSupport::supportsShapedWindows);
        assertDoesNotThrow(PlatformSupport::supportsWindowOpacity);
        assertDoesNotThrow(PlatformSupport::isSystemTraySupported);
    }
}
