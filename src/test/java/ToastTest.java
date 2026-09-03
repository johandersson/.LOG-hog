package test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;

import utils.Toast;

class ToastTest {

    @Test
    void showToastHandlesTypicalMessagesWhenGraphicsAreAvailable() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Toast requires a graphics environment");
        assertDoesNotThrow(() -> SwingUtilities.invokeAndWait(() -> Toast.showToast(null, "Test message", 1)));
        assertDoesNotThrow(() -> SwingUtilities.invokeAndWait(() -> Toast.showToast(null, "Hello from JUnit test!", 1)));
        assertDoesNotThrow(() -> SwingUtilities.invokeAndWait(() -> Toast.showToast(null, "", 1)));
        assertDoesNotThrow(() -> SwingUtilities.invokeAndWait(() -> Toast.showToast(null, null, 1)));
    }
}
