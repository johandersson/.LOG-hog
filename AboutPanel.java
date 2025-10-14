import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * AboutPanel loads license text from ./license.txt and shows it with an OK button
 * that switches the provided JTabbedPane back to the main tab.
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

        // Load license text from current folder license.txt
        String licenseText = loadLicenseText();

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

        // Bottom: OK button returns to Entry tab (index 0)
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        JButton ok = new AccentButton("OK");
        ok.addActionListener(e -> {
            if (tabPane != null && tabPane.getTabCount() > 0) tabPane.setSelectedIndex(0);
        });
        bottom.add(ok);
        add(bottom, BorderLayout.SOUTH);
    }

    private String loadLicenseText() {
        // 1) Try file in current working directory
        Path p = Path.of("license.txt");
        try {
            if (Files.exists(p)) return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException ignored) {}

        // 2) Try loading as a classpath resource relative to this class
        //    If license.txt is located in the same package as AboutPanel.java, use getResourceAsStream("license.txt").
        //    If it was placed at the project root resources folder, use getResourceAsStream("/license.txt").
        try (InputStream in = AboutPanel.class.getResourceAsStream("license.txt")) {
            if (in != null) return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {}

        // 3) Try loading from root of classpath
        try (InputStream in = AboutPanel.class.getResourceAsStream("/license.txt")) {
            if (in != null) return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {}

        // 4) Fallback message
        return defaultLicenseMissingText();
    }


    private String defaultLicenseMissingText() {
        return "Copyright 2025 Johan Andersson\n" +
                "\n" +
                "Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\n" +
                "\n" +
                "The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n" +
                "\n" +
                "THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.\n" +
                "\n";
    }
}
