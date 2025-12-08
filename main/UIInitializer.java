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

import gui.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class UIInitializer {
    private final LogTextEditor editor;
    private final JTabbedPane tabPane;
    private final List<NavItem> navItems;

    public UIInitializer(LogTextEditor editor, JTabbedPane tabPane, List<NavItem> navItems) {
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
        editor.setSize(1200, 660);
        editor.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        editor.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                editor.getLogFileHandler().clearSensitiveData();
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
        root.setBorder(BorderFactory.createLineBorder(new Color(0xD6DCE0)));
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
        // small status/footer area
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(new EmptyBorder(8, 12, 8, 12));
        statusBar.setBackground(new Color(0xFFFFFF));
        JLabel footer = new JLabel("Write something and hit Ctrl+S! Search with Ctrl+F. For a quick short entry, use Ctrl+N anywhere.");
        footer.setFont(footer.getFont().deriveFont(Font.PLAIN, 12f));
        footer.setForeground(new Color(0x394B54));
        statusBar.add(footer, BorderLayout.WEST);

        editor.getContentPane().add(statusBar, BorderLayout.SOUTH);
    }

    private void setupLookAndFeel() {
        UIManager.put("control", new Color(0xF3F6F9));
        UIManager.put("nimbusBase", new Color(0x2E3A3F));
        UIManager.put("text", new Color(0x22282B));
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
        Runnable aboutOnClick = () -> {
            new gui.SplashScreen(); // modal, shows splash
            tabPane.setSelectedIndex(5);
        };
        NavItem n5 = new NavItem("About", 5, tabPane, aboutOnClick);

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
        contentCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE7EBEF)),
                new EmptyBorder(12, 12, 12, 12)
        ));

        tabPane.setUI(new HiddenTabUI());
        tabPane.addTab("Entry", editor.getEntryPanel());
        tabPane.addTab("Log Entries", editor.getLogListPanel());
        tabPane.addTab("Full Log", editor.getFullLogPanel());
        tabPane.addTab("Settings", editor.getSettingsPanel());
        tabPane.addTab("Help", new InformationPanel(tabPane, "help.md", "Help"));
        tabPane.addTab("About", new InformationPanel(tabPane, "license.md", "About"));
        tabPane.addChangeListener(e -> {
            if (tabPane.getSelectedIndex() == 2) {
                editor.getFullLogPanel().loadFullLog();
            } else if (tabPane.getSelectedIndex() == 3) {
                editor.getSettingsPanel().loadCurrentSettings();
            }
        });
        contentCard.add(tabPane, BorderLayout.CENTER);

        center.add(contentCard, BorderLayout.CENTER);
    }
}