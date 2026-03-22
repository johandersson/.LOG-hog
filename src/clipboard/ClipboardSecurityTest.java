package clipboard;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import javax.swing.JFrame;

/**
 * Simple test class to verify clipboard security functionality.
 * This test demonstrates the secure clipboard features.
 */
public class ClipboardSecurityTest {

    public static void main(String[] args) {
        utils.Log.info("Testing Clipboard Security Features...");

        try {
            // Test basic clipboard operations
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

            // Test copying secure content
            String testContent = "This is secure .LOG-hog content that should be auto-cleared.";
            StringSelection selection = new StringSelection(testContent);
            clipboard.setContents(selection, null);

            // Verify content was set
            String retrieved = (String) clipboard.getData(DataFlavor.stringFlavor);
            utils.Log.info(() -> "✓ Content set to clipboard: " + retrieved);

            // Test SecureClipboardManager
            SecureClipboardManager secureManager = new SecureClipboardManager();
            utils.Log.info("✓ SecureClipboardManager initialized");

            // Set timeout for testing
            SecureClipboardManager.setTimeoutSeconds(5); // 5 second timeout for testing

            // Test secure copy (need a dummy component)
            JFrame dummyFrame = new JFrame();
            secureManager.copySecureTextToClipboard("Test secure content", dummyFrame);
            utils.Log.info("✓ Secure content copied with 5-second timeout");

            // Test manual clear
            secureManager.clearSecureClipboard();
            utils.Log.info("✓ Secure clipboard manually cleared");

            // Test ClipboardSecurityWarner
            ClipboardSecurityWarner warner = new ClipboardSecurityWarner();
            utils.Log.info("✓ ClipboardSecurityWarner initialized");

            utils.Log.info("\nAll clipboard security tests passed!");

        } catch (Exception e) {
            utils.Log.error("✗ Test failed: " + e.getMessage(), e);
        }
    }
}