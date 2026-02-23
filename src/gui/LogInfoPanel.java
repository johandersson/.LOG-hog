package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A reusable info panel component that displays log statistics.
 * Features a clean, professional design with proper spacing and typography.
 */
public class LogInfoPanel extends JPanel {

    private final JLabel entriesLabel;
    private final JLabel daysLabel;
    private final JLabel fileSizeLabel;
    private final JLabel limitInfoLabel;

    public LogInfoPanel() {
        super(new BorderLayout());

        // Top row: entries/days/size
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));

        // Initialize labels
        entriesLabel = new JLabel("Entries: --");
        daysLabel = new JLabel("Days: --");
        fileSizeLabel = new JLabel("Size: --");

        // Bottom row: small limit/info text
        limitInfoLabel = new JLabel("");

        // Style the panel
        setOpaque(true);
        setBackground(Color.WHITE); // White background
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        // Style the labels
        Font infoFont = new Font("Segoe UI", Font.PLAIN, 12);
        Color infoColor = new Color(0x495057);

        entriesLabel.setFont(infoFont);
        entriesLabel.setForeground(infoColor);

        daysLabel.setFont(infoFont);
        daysLabel.setForeground(infoColor);

        fileSizeLabel.setFont(infoFont);
        fileSizeLabel.setForeground(infoColor);

        // Limit/info label style: smaller, italic, red, initially hidden
        Font smallItalic = new Font("Segoe UI", Font.ITALIC, 11);
        limitInfoLabel.setFont(smallItalic);
        limitInfoLabel.setForeground(new Color(0xB00020)); // red
        limitInfoLabel.setVisible(false);

        // Add components
        topRow.add(entriesLabel);
        topRow.add(daysLabel);
        topRow.add(fileSizeLabel);
        add(topRow, BorderLayout.NORTH);
        add(limitInfoLabel, BorderLayout.SOUTH);
    }

    /**
     * Updates the panel with new statistics.
     */
    public void updateStatistics(LogStatistics stats) {
        if (stats != null) {
            entriesLabel.setText("Entries: " + stats.getEntryCount());
            daysLabel.setText("Days: " + stats.getDayCount());
            fileSizeLabel.setText("Size: " + stats.getFormattedFileSize());
        } else {
            resetStatistics();
        }
    }

    /**
     * Sets the small informational text (e.g., when view is limited).
     */
    public void setLimitInfo(String text) {
        if (text == null || text.isBlank()) {
            limitInfoLabel.setVisible(false);
            limitInfoLabel.setText("");
        } else {
            limitInfoLabel.setText(text);
            limitInfoLabel.setVisible(true);
        }
    }

    /**
     * Clears the limit/info text.
     */
    public void clearLimitInfo() {
        setLimitInfo("");
    }

    /**
     * Resets the panel to show default values.
     */
    public void resetStatistics() {
        entriesLabel.setText("Entries: --");
        daysLabel.setText("Days: --");
        fileSizeLabel.setText("Size: --");
    }

    /**
     * Gets the preferred height for consistent layout.
     */
    public int getPreferredHeight() {
        return getPreferredSize().height;
    }
}