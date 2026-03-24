package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import filehandling.ResourceLimits;

/**
 * A reusable info panel component that displays log statistics.
 * Features a clean, professional design with proper spacing and typography.
 */
public class LogInfoPanel extends JPanel {

    private final JLabel entriesLabel;
    private final JLabel daysLabel;
    private final JLabel fileSizeLabel;
    private final JLabel yearsScopeLabel;
    private final JLabel limitInfoLabel;

    public LogInfoPanel() {
        super(new BorderLayout());

        // Top row: entries/days/size
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        // Bottom row: small limit/info text (initialized later)
        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));

        // Initialize labels
        entriesLabel = new JLabel("Entries: --");
        daysLabel = new JLabel("Days: --");
        fileSizeLabel = new JLabel("Size: --");

        yearsScopeLabel = new JLabel("");
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

        yearsScopeLabel.setFont(infoFont);
        yearsScopeLabel.setForeground(infoColor);

        limitInfoLabel.setFont(infoFont);
        limitInfoLabel.setForeground(infoColor);

        // Add labels to rows
        topRow.setOpaque(false);
        topRow.add(entriesLabel);
        topRow.add(daysLabel);
        topRow.add(fileSizeLabel);

        bottomRow.setOpaque(false);
        bottomRow.add(yearsScopeLabel);
        bottomRow.add(limitInfoLabel);

        // Add rows to this panel using BorderLayout positions
        add(topRow, BorderLayout.NORTH);
        add(bottomRow, BorderLayout.SOUTH);
    }

    /**
     * Updates the panel with new statistics.
     */
    public void updateStatistics(LogStatistics stats) {
        if (stats != null) {
            int count = stats.getEntryCount();
            if (count > ResourceLimits.MAX_ENTRIES_TO_RENDER) {
                String total = String.format("%,d", count);
                String cap = String.format("%,d", ResourceLimits.MAX_ENTRIES_TO_RENDER);
                entriesLabel.setText("Entries: " + total + " (" + cap + ") – limit reached");
            } else {
                entriesLabel.setText("Entries: " + stats.getEntryCount());
            }
            daysLabel.setText("Days: " + stats.getDayCount());
            fileSizeLabel.setText("Size: " + stats.getFormattedFileSize());
        } else {
            resetStatistics();
        }
    }

    /**
     * Updates the year-scope indicator shown in the info panel (e.g. "Tail-only").
     */
    public void updateYearScope(String scopeText) {
        if (scopeText == null || scopeText.isEmpty()) {
            yearsScopeLabel.setText("");
        } else {
            yearsScopeLabel.setText("Years: " + scopeText);
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