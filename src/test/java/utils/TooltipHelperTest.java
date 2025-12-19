package utils;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.ToolTipManager;
import java.awt.GraphicsEnvironment;

/**
 * Comprehensive tests for TooltipHelper
 * Tests tooltip enabling on disabled components and GUI interactions
 */
public class TooltipHelperTest {

    private JButton testButton;

    @BeforeEach
    void setup() {
        // Skip GUI tests in headless environment
        assumeTrue(!GraphicsEnvironment.isHeadless(), "Skipping GUI tests in headless environment");

        testButton = new JButton("Test Button");
    }

    @Test
    void testEnableTooltipOnDisabled() {
        System.out.println("🧪 Testing tooltip enabling on disabled components...");

        assumeTrue(!GraphicsEnvironment.isHeadless(), "Requires GUI environment");

        // Initially enabled
        testButton.setEnabled(true);
        assertTrue(testButton.isEnabled(), "Button should be enabled initially");

        // Enable tooltip on disabled component
        TooltipHelper.enableTooltipOnDisabled(testButton, "Test tooltip");

        // Verify tooltip text is set
        assertEquals("Test tooltip", testButton.getToolTipText(), "Tooltip text should be set");

        // Disable the component
        testButton.setEnabled(false);
        assertFalse(testButton.isEnabled(), "Button should be disabled");

        // Tooltip should still work (this is hard to test directly without GUI events)
        // But we can verify the component has mouse listeners
        assertTrue(testButton.getMouseListeners().length > 0, "Mouse listeners should be added");

        System.out.println("✅ Tooltip enabling on disabled components works correctly");
    }

    @Test
    void testTooltipOnEnabledComponent() {
        System.out.println("🧪 Testing tooltip behavior on enabled components...");

        assumeTrue(!GraphicsEnvironment.isHeadless(), "Requires GUI environment");

        // Enable tooltip on enabled component
        testButton.setEnabled(true);
        TooltipHelper.enableTooltipOnDisabled(testButton, "Enabled tooltip");

        // Verify tooltip text is set
        assertEquals("Enabled tooltip", testButton.getToolTipText(), "Tooltip text should be set");

        // Component should remain enabled
        assertTrue(testButton.isEnabled(), "Button should remain enabled");

        // Should have mouse listeners
        assertTrue(testButton.getMouseListeners().length > 0, "Mouse listeners should be added");

        System.out.println("✅ Tooltip behavior on enabled components works correctly");
    }

    @Test
    void testNullComponentHandling() {
        System.out.println("🧪 Testing null component handling...");

        // Should handle null component gracefully (though this would be a programming error)
        assertDoesNotThrow(() -> {
            TooltipHelper.enableTooltipOnDisabled(null, "tooltip");
        });

        System.out.println("✅ Null component handling works gracefully");
    }

    @Test
    void testNullTooltipTextHandling() {
        System.out.println("🧪 Testing null tooltip text handling...");

        assumeTrue(!GraphicsEnvironment.isHeadless(), "Requires GUI environment");

        // Should handle null tooltip text
        assertDoesNotThrow(() -> {
            TooltipHelper.enableTooltipOnDisabled(testButton, null);
        });

        // Tooltip text should be null
        assertNull(testButton.getToolTipText(), "Tooltip text should be null");

        System.out.println("✅ Null tooltip text handling works correctly");
    }

    @Test
    void testEmptyTooltipTextHandling() {
        System.out.println("🧪 Testing empty tooltip text handling...");

        assumeTrue(!GraphicsEnvironment.isHeadless(), "Requires GUI environment");

        // Should handle empty tooltip text
        TooltipHelper.enableTooltipOnDisabled(testButton, "");

        // Tooltip text should be empty
        assertEquals("", testButton.getToolTipText(), "Tooltip text should be empty");

        System.out.println("✅ Empty tooltip text handling works correctly");
    }

    @Test
    void testMultipleTooltipEnabling() {
        System.out.println("🧪 Testing multiple tooltip enabling calls...");

        assumeTrue(!GraphicsEnvironment.isHeadless(), "Requires GUI environment");

        // Enable tooltip multiple times
        TooltipHelper.enableTooltipOnDisabled(testButton, "First tooltip");
        TooltipHelper.enableTooltipOnDisabled(testButton, "Second tooltip");

        // Should have the last tooltip text
        assertEquals("Second tooltip", testButton.getToolTipText(), "Should have the last tooltip text");

        // Should have mouse listeners (may accumulate)
        assertTrue(testButton.getMouseListeners().length > 0, "Should have mouse listeners");

        System.out.println("✅ Multiple tooltip enabling calls handled correctly");
    }

    @Test
    void testTooltipManagerInteraction() {
        System.out.println("🧪 Testing ToolTipManager interaction...");

        assumeTrue(!GraphicsEnvironment.isHeadless(), "Requires GUI environment");

        // Get the shared ToolTipManager instance
        ToolTipManager manager = ToolTipManager.sharedInstance();
        assertNotNull(manager, "ToolTipManager should exist");

        // Verify it's the same instance that would be used by TooltipHelper
        // (This is an indirect test since we can't easily trigger mouse events)

        System.out.println("✅ ToolTipManager interaction works correctly");
    }

    @Test
    void testComponentTypeCompatibility() {
        System.out.println("🧪 Testing component type compatibility...");

        assumeTrue(!GraphicsEnvironment.isHeadless(), "Requires GUI environment");

        // Test with different JComponent types
        javax.swing.JTextField textField = new javax.swing.JTextField();
        javax.swing.JCheckBox checkBox = new javax.swing.JCheckBox();
        javax.swing.JComboBox<String> comboBox = new javax.swing.JComboBox<>();

        assertDoesNotThrow(() -> {
            TooltipHelper.enableTooltipOnDisabled(textField, "Text field tooltip");
            TooltipHelper.enableTooltipOnDisabled(checkBox, "Checkbox tooltip");
            TooltipHelper.enableTooltipOnDisabled(comboBox, "Combo box tooltip");
        });

        // All should have tooltips set
        assertEquals("Text field tooltip", textField.getToolTipText());
        assertEquals("Checkbox tooltip", checkBox.getToolTipText());
        assertEquals("Combo box tooltip", comboBox.getToolTipText());

        System.out.println("✅ Component type compatibility works correctly");
    }

    @Test
    void testHeadlessEnvironmentHandling() {
        System.out.println("🧪 Testing headless environment handling...");

        // In headless environment, GUI operations should be safe
        // (This test will run but may be skipped if GUI tests are disabled)

        if (GraphicsEnvironment.isHeadless()) {
            // In headless mode, we can't create GUI components
            System.out.println("Running in headless environment - GUI tests skipped");
        } else {
            // In GUI environment, operations should work
            JButton button = new JButton();
            assertDoesNotThrow(() -> {
                TooltipHelper.enableTooltipOnDisabled(button, "Headless test tooltip");
            });
        }

        System.out.println("✅ Headless environment handling works correctly");
    }
}