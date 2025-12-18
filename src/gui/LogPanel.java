package gui;

import javax.swing.JPanel;

public abstract class LogPanel extends JPanel {
    public abstract void loadLog();
    public abstract void copyToClipboard();
}