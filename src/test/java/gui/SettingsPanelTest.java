package gui;

import java.nio.file.Path;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SettingsPanelTest {
    private SettingsPanel panel;
    private JButton applyButton;
    private Properties settings;

    private void createPanel() throws Exception {
        settings = new Properties();
        settings.setProperty("encrypted", "true");
        panel = new SettingsPanel(null, settings, Path.of("unused-settings.properties"), null);
        applyButton = field("applyButton", JButton.class);
    }

    private <T> T field(String name, Class<T> type) throws Exception {
        var field = SettingsPanel.class.getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(panel));
    }

    @Test
    void unchangedSettingsCannotBeAppliedAndEditsClearStatus() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                createPanel();
                assertFalse(applyButton.isEnabled());
                applyButton.doClick();
                var status = field("statusLabel", JLabel.class);
                assertEquals("", status.getText());
                status.setText("No changes to apply.");
                field("splashOnStartupCheckBox", JCheckBox.class).doClick();
                assertTrue(applyButton.isEnabled());
                assertEquals("", status.getText());
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        });
    }

    @Test
    void eachEditableSettingEnablesApplyAndRevertingDisablesIt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                createPanel();
                for (String name : new String[] {"autoBackupCheckBox", "splashOnStartupCheckBox"}) {
                    var checkbox = field(name, JCheckBox.class);
                    checkbox.doClick();
                    assertTrue(applyButton.isEnabled(), name);
                    checkbox.doClick();
                    assertFalse(applyButton.isEnabled(), name);
                }
                for (String name : new String[] {"backupDirField", "clipboardTimeoutField"}) {
                    var text = field(name, JTextField.class);
                    String original = text.getText();
                    text.setText(original + "1");
                    assertTrue(applyButton.isEnabled(), name);
                    text.setText(original);
                    assertFalse(applyButton.isEnabled(), name);
                }
                var spinner = field("autoLockTimeoutSpinner", JSpinner.class);
                spinner.setValue(20);
                assertTrue(applyButton.isEnabled());
                spinner.setValue(15);
                assertFalse(applyButton.isEnabled());
                var text = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
                String original = text.getText();
                text.setText("25");
                assertTrue(applyButton.isEnabled());
                spinner.commitEdit();
                assertEquals(25, spinner.getValue());
                text.setText(original);
                spinner.commitEdit();
                assertFalse(applyButton.isEnabled());
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        });
    }

    @Test
    void reloadResetsBaselineAndTracksAllPendingEdits() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                createPanel();
                var directory = field("backupDirField", JTextField.class);
                var splash = field("splashOnStartupCheckBox", JCheckBox.class);
                directory.setText("backups");
                splash.doClick();
                directory.setText("");
                assertTrue(applyButton.isEnabled());
                settings.setProperty("showSplashOnStartup", "false");
                panel.loadCurrentSettings();
                assertFalse(applyButton.isEnabled());
                assertFalse(splash.isSelected());
                splash.doClick();
                assertTrue(applyButton.isEnabled());
                panel.loadCurrentSettings();
                assertFalse(applyButton.isEnabled());
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        });
    }
}
