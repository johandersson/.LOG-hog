import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Standalone About panel showing license and an OK button that returns to the Entry tab.
 */
public class AboutPanel extends JPanel {
    public AboutPanel(JTabbedPane tabPane) {
        super(new BorderLayout(8, 8));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // Header
        JLabel header = new JLabel(".LOG hog — About");
        header.setFont(header.getFont().deriveFont(Font.BOLD, 16f));
        header.setForeground(new Color(0x2B3A42));
        add(header, BorderLayout.NORTH);

        // License / info text
        String licenseText =
                "LogTextEditor\n\n" +
                        "Version: 1.0\n\n" +
                        "License\n" +
                        "-------\n" +
                        "This software is provided under the MIT License.\n\n" +
                        "Copyright (c) 2025 Your Name\n\n" +
                        "Permission is hereby granted, free of charge, to any person obtaining a copy\n" +
                        "of this software and associated documentation files (the \"Software\"), to deal\n" +
                        "in the Software without restriction, including without limitation the rights\n" +
                        "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell\n" +
                        "copies of the Software, and to permit persons to whom the Software is\n" +
                        "furnished to do so, subject to the following conditions:\n\n" +
                        "[...complete MIT license text or your preferred license...]\n\n" +
                        "Replace this text with your actual license if different.";

        JTextArea ta = new JTextArea(licenseText);
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
        ta.setBackground(Color.WHITE);

        JScrollPane sp = new JScrollPane(ta);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createEmptyBorder());
        add(sp, BorderLayout.CENTER);

        // Bottom: close / OK button to return to the main tab (Entry)
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        JButton ok = new AccentButton("OK");
        ok.addActionListener(e -> {
            if (tabPane != null && tabPane.getTabCount() > 0) tabPane.setSelectedIndex(0);
        });
        bottom.add(ok);
        add(bottom, BorderLayout.SOUTH);
    }
}
