package gui;

import javax.swing.JPanel;

/**
 * Abstract base class for log panels to enable LSP compliance in UI components.
 */
public abstract class LogPanel extends JPanel {
    public abstract void loadLog();
    public abstract void copyToClipboard();
}