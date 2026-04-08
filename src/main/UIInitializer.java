/*
 * Copyright (C) 2025 Johan Andersson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import gui.HiddenTabUI;
import gui.InformationPanel;
import gui.NavItem;
import gui.SplashScreen;
import gui.StatusBar;
import gui.LoadingProgressDialog;

public class UIInitializer {
    private final LogTextEditor editor;
    private final JTabbedPane tabPane;
    private final List<NavItem> navItems;

    public UIInitializer(LogTextEditor editor, JTabbedPane tabPane, List<NavItem> navItems, java.util.Properties settings) {
        this.editor = editor;
        this.tabPane = tabPane;
        this.navItems = navItems;
    }

    public void initializeUI() {
        setupFrame();
        setupContent();
        setupStatusBar();
        setupLookAndFeel();
    }

    private void setupFrame() {
        editor.setUndecorated(false);
        editor.setTitle(".LOG hog");
        editor.setSize(1200, 750);
        editor.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        editor.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                try {
                    showSecurityProgressDialog(editor);
                    // Clean shutdown of all background processes
                    editor.shutdown();
                    // Dispose the window
                    editor.dispose();
                    // Clean up before exit
                    clipboard.SecureClipboardManager.clearSecureClipboard();
                    editor.getLogFileHandler().clearSensitiveData();
                    // Clear UI text areas
                    editor.getLogListPanel().getEntryArea().setText("");
                    editor.getFullLogPanel().getFullLogPane().setText("");
                } finally {
                    // Always exit, even if cleanup throws
                    System.exit(0);
                }
            }
        });
        editor.setLocationRelativeTo(null);
        addIcon();
    }

    private void addIcon() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(Color.BLUE);
        g2.fillRect(0, 0, 16, 16);
        g2.setColor(Color.WHITE);
        g2.drawString("L", 4, 12);
        g2.dispose();
        editor.setIconImage(image);
    }

    private void setupContent() {
        // Root panel with subtle border to emulate card area
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(0xF3F6F9));
        editor.setContentPane(root);

        // Main content area with left rail + center cards
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(new Color(0xF7FAFC));
        center.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel leftRail = createLeftRail();
        center.add(leftRail, BorderLayout.WEST);

        // content area (tabs wrapped in a card-like panel)
        createContentCardWithTabs(center);

        root.add(center, BorderLayout.CENTER);
    }

    private void setupStatusBar() {
        StatusBar statusBar = new StatusBar();
        editor.setStatusBar(statusBar);
        editor.getContentPane().add(statusBar, BorderLayout.SOUTH);
        
        // Update status bar based on selected tab
        tabPane.addChangeListener(e -> {
            int selectedIndex = tabPane.getSelectedIndex();
            // Show "Write something" message only on editing tabs (index 0 and 1)
            if (selectedIndex == 0 || selectedIndex == 1) {
                statusBar.setMessage("Write something and hit CTRL+S");
            } else {
                statusBar.setMessage("");
            }
        });
    }

    private void setupLookAndFeel() {
        // Custom colors for Windows look and feel
        UIManager.put("Panel.background", new Color(0xF3F6F9));
        UIManager.put("Button.background", new Color(0xF3F6F9));
        UIManager.put("TextField.background", new Color(0xF3F6F9));
        UIManager.put("Label.foreground", new Color(0x22282B));
        UIManager.put("Button.foreground", new Color(0x22282B));
        UIManager.put("TextField.foreground", new Color(0x22282B));
        Font uiFont = new Font("Segoe UI", Font.PLAIN, 13);
        UIManager.put("Label.font", uiFont);
        UIManager.put("Button.font", uiFont);
        UIManager.put("ComboBox.font", uiFont);
        UIManager.put("TabbedPane.font", uiFont);
    }

    private JPanel createLeftRail() {
        JPanel left = new JPanel();
        left.setPreferredSize(new Dimension(170, 0));
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(true);
        left.setBackground(new Color(0xF7FAFC));
        left.setBorder(new EmptyBorder(12, 10, 12, 10));

        // create NavItems bound to tab indices using the extracted NavItem class (title, tabIndex, tabPane)
        NavItem n0 = new NavItem("Entry", 0, tabPane, null);
        NavItem n1 = new NavItem("Log Entries", 1, tabPane, null);
        NavItem n2 = new NavItem("Full Log", 2, tabPane, null);
        NavItem n3 = new NavItem("Settings", 3, tabPane, null);
        NavItem n4 = new NavItem("Help", 4, tabPane, null);
        NavItem n5 = new NavItem("About", 5, tabPane, null);

        navItems.clear();
        navItems.add(n0);
        navItems.add(n1);
        navItems.add(n2);
        navItems.add(n3);
        navItems.add(n4);
        navItems.add(n5);

        left.add(n0);
        left.add(Box.createVerticalStrut(6));
        left.add(n1);
        left.add(Box.createVerticalStrut(6));
        left.add(n2);
        left.add(Box.createVerticalStrut(6));
        left.add(n3);
        left.add(Box.createVerticalStrut(6));
        left.add(n4);
        left.add(Box.createVerticalStrut(6));
        left.add(n5);
        left.add(Box.createVerticalGlue());

        JLabel ver = new JLabel("v1.0");
        ver.setFont(ver.getFont().deriveFont(11f));
        ver.setForeground(new Color(0x6B7A80));
        ver.setAlignmentX(Component.LEFT_ALIGNMENT);
        ver.setOpaque(false);
        left.add(ver);

        // ensure nav visuals update when user changes tabs programmatically
        tabPane.addChangeListener(e -> updateNavActiveStates());
        updateNavActiveStates(); // sync visuals initially

        return left;
    }

    private void updateNavActiveStates() {
        // NavItem instances listen to tabPane changes themselves; just repaint to force visual refresh
        int sel = tabPane.getSelectedIndex();
        for (NavItem ni : navItems) {
            ni.repaint();
            // ensure accessibility/visual sync for the selected item
            if (navItems.indexOf(ni) == sel) {
                ni.getAccessibleContext().firePropertyChange(
                        javax.accessibility.AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                        null, null);
            }
        }
    }

    private void createContentCardWithTabs(JPanel center) {
        JPanel contentCard = new JPanel(new BorderLayout());
        contentCard.setBackground(Color.WHITE);
        contentCard.setBorder(new EmptyBorder(12, 12, 12, 12));

        tabPane.setUI(new HiddenTabUI());
        tabPane.setBorder(null);
        tabPane.addTab("Entry", editor.getEntryPanel());
        tabPane.addTab("Log Entries", editor.getLogListPanel());
        tabPane.addTab("Full Log", editor.getFullLogPanel());
        tabPane.addTab("Settings", editor.getSettingsPanel());
        tabPane.addTab("Help", new InformationPanel(tabPane, "help.md", "Help", false, true));
        // Load About immediately (no lazy splash gating) so license is always available
        tabPane.addTab("About", new InformationPanel(tabPane, "LICENSE.md", "About", false, false));
        tabPane.addChangeListener(e -> {
            int idx = tabPane.getSelectedIndex();
            if (idx == 2) {
                if (!editor.getFullLogPanel().isSuppressAutoLoad()) {
                    LoadingProgressDialog progress = new LoadingProgressDialog(editor, "Loading");
                    // Delay showing the dialog to avoid flashing for fast loads
                    final javax.swing.Timer showTimer = new javax.swing.Timer(150, ev -> progress.show());
                    showTimer.setRepeats(false);
                    showTimer.start();
                    editor.getFullLogPanel().loadFullLog(() -> {
                        if (showTimer.isRunning()) showTimer.stop();
                        progress.close();
                    });
                }
            } else if (idx == 1) {
                // Log Entries tab - load only filtered entries (default to current year/month)
                LoadingProgressDialog progress = new LoadingProgressDialog(editor, "Loading");
                final javax.swing.Timer showTimer = new javax.swing.Timer(150, ev -> progress.show());
                showTimer.setRepeats(false);
                showTimer.start();
                // Determine default filter: current year and month
                int currentYear = java.time.Year.now().getValue();
                int currentMonth = java.time.LocalDate.now().getMonthValue();
                DefaultListModel<String> model = editor.getLogListPanel().getListModel();

                // Load filtered entries in background then update view on EDT.
                // If there are no entries for the current month, fall back to
                // the month/year of the latest entry in the file.
                Thread loaderThread = new Thread(() -> {
                    int filterYear = currentYear;
                    int filterMonth = currentMonth;
                    try {
                        editor.getLogFileHandler().loadFilteredEntries(model, filterYear, filterMonth);

                        if (model.getSize() == 0) {
                            // Try to find the latest entry and use its month/year
                            java.util.List<String> recent = editor.getLogFileHandler().getRecentLogEntries(1);
                            if (!recent.isEmpty()) {
                                try {
                                    java.time.LocalDateTime dt = utils.DateHandler.parseTimestamp(recent.get(0));
                                    filterYear = dt.getYear();
                                    filterMonth = dt.getMonthValue();
                                    // reload with fallback filter
                                    editor.getLogFileHandler().loadFilteredEntries(model, filterYear, filterMonth);
                                } catch (Exception ignore) {
                                    // keep original filter if parse fails
                                }
                            }
                        }

                        int fYear = filterYear;
                        int fMonth = filterMonth;
                        SwingUtilities.invokeLater(() -> {
                            editor.updateLogListView();
                            // Update the UI controls to reflect the active filter
                            editor.getLogListPanel().setFilterSelection(fYear, fMonth);
                        });
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> editor.getLogFileHandler().showErrorDialog("<html><b>🔄 Load Failed</b><br><br>Unable to load log entries.</html>"));
                    } finally {
                        SwingUtilities.invokeLater(() -> {
                            if (showTimer.isRunning()) showTimer.stop();
                            progress.close();
                        });
                    }
                }, "LogEntriesFilteredLoader");
                loaderThread.setDaemon(true);
                loaderThread.start();
            } else if (idx == 3) {
                editor.getSettingsPanel().loadCurrentSettings();
            } else if (idx == 5) {
                // Ensure About tab content is loaded immediately without showing the splash
                ((InformationPanel) tabPane.getComponentAt(5)).loadText();
            } else if (idx == 0) {
                // Focus the entry text area when switching to the Entry tab
                SwingUtilities.invokeLater(() -> {
                    var textArea = editor.getEntryPanel().getTextArea();
                    textArea.requestFocusInWindow();
                    textArea.setCaretPosition(textArea.getDocument().getLength()); // Place cursor at the end
                });
            }
        });
        contentCard.add(tabPane, BorderLayout.CENTER);

        center.add(contentCard, BorderLayout.CENTER);
    }

    private void showSecurityProgressDialog(JFrame parent) {
        var dialog = new JDialog(parent, "Securing Data", true);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setLayout(new BorderLayout());
        dialog.add(new JLabel("Securing sensitive data before exit...", SwingConstants.CENTER), BorderLayout.CENTER);
        var progressBar = new JProgressBar(0, 100);
        progressBar.setIndeterminate(true);
        dialog.add(progressBar, BorderLayout.SOUTH);
        dialog.setSize(300, 100);
        dialog.setLocationRelativeTo(parent);

        // Show dialog (non-modal) then perform delay off the EDT so UI stays responsive
        dialog.setModal(false);
        SwingUtilities.invokeLater(() -> dialog.setVisible(true));
        Thread delayThread = new Thread(() -> {
            try {
                Thread.sleep(3000); // 3 seconds delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            SwingUtilities.invokeLater(() -> {
                dialog.setVisible(false);
                dialog.dispose();
            });
        }, "SecurityDelayCloser");
        delayThread.setDaemon(true);
        delayThread.start();
    }
}