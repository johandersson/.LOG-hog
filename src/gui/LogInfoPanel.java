package gui;

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

    public LogInfoPanel() {
        super(new FlowLayout(FlowLayout.LEFT, 15, 5));

        // Initialize labels
        entriesLabel = new JLabel("Entries: --");
        daysLabel = new JLabel("Days: --");
        fileSizeLabel = new JLabel("Size: --");

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

        // Add components
        add(entriesLabel);
        add(daysLabel);
        add(fileSizeLabel);
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