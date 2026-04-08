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

package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;

import markdown.LinkHandler;
import markdown.MarkdownRenderer;
import markdown.MarkdownStyle;

public class InformationPanel extends JPanel {
    private String fileName;
    private JTextPane textPane;

    public InformationPanel(JTabbedPane tabPane, String fileNameForText, String title, boolean lazyLoad, boolean showSplash) {
        super(new BorderLayout(8, 8));
        this.fileName = fileNameForText;
        createPanel(tabPane, title, lazyLoad, showSplash);
    }

    private void createPanel(JTabbedPane tabPanel, String title, boolean lazyLoad, boolean showSplash) {
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        createHeader(title);

        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setBackground(Color.WHITE);
        // Ensure help/about text uses the app's standard font and size to match Full Log
        textPane.setFont(new java.awt.Font(MarkdownStyle.FONT_FAMILY_DEFAULT, java.awt.Font.PLAIN, MarkdownStyle.FONT_SIZE_DEFAULT));

        if (!lazyLoad) {
            loadText();
        }

        var sp = new JScrollPane(textPane);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // vertical scrollbar policy left to default (AS_NEEDED)
        add(sp, BorderLayout.CENTER);

        // 'tabPanel' parameter is unused, kept for compatibility (PMD fix)

        if (lazyLoad && !showSplash) {
            var bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            bottom.setOpaque(false);
            var splashButton = new AccentButton("Show Splash");
            splashButton.addActionListener(e -> {
                SplashScreen splash = new SplashScreen();
                splash.setVisible(true);
                if (splash.wasOkPressed()) {
                    loadText();
                }
            });
            bottom.add(splashButton);
            add(bottom, BorderLayout.SOUTH);
        }
    }

    public void loadText() {
        if (fileName == null) return;

        // Load panel text off the EDT to avoid blocking UI for large or slow resource reads
        new javax.swing.SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return loadPanelText(fileName);
            }

            @Override
            protected void done() {
                try {
                    String informationTextToDisplay = get();
                    java.util.List<String> lines = informationTextToDisplay == null ? java.util.List.of() : informationTextToDisplay.lines().toList();
                    MarkdownRenderer.renderMarkdownDirect(textPane, lines);
                    LinkHandler.addLinkListeners(textPane);
                    textPane.setCaretPosition(0);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.ExecutionException ee) {
                    textPane.setText("Could not load content.");
                }
            }
        }.execute();
    }

    public void unloadText() {
        textPane.setText("");
    }

    private void createHeader(String title) {
        // Header
        var header = new JLabel(title);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 16f));
        header.setForeground(new Color(0x2B3A42));
        add(header, BorderLayout.NORTH);
    }

    private String loadPanelText(String fileName) {
        // 1) Try file in several likely locations in the development/work directory
        Path p1 = Path.of(fileName);
        Path p2 = Path.of("src", fileName);
        Path p3 = Path.of("resources", fileName);
        try {
            if (Files.exists(p1)) return Files.readString(p1, StandardCharsets.UTF_8);
            if (Files.exists(p2)) return Files.readString(p2, StandardCharsets.UTF_8);
            if (Files.exists(p3)) return Files.readString(p3, StandardCharsets.UTF_8);
        } catch (IOException couldNotReadFile) {
            return "Could not read " + fileName + " from file system: " + couldNotReadFile.getMessage();
        }

        // 2) Try resource locations inside the JAR (several common packaging locations)
        String[] resourcePaths = new String[]{"/" + fileName, "/resources/" + fileName, "/src/resources/" + fileName};
        for (String rp : resourcePaths) {
            try (var is = getClass().getResourceAsStream(rp)) {
                if (is != null) {
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            } catch (IOException e) {
                return "Could not read " + fileName + " from resources: " + e.getMessage();
            }
        }

        return fileName + " not found as file or resource.";
    }

}
