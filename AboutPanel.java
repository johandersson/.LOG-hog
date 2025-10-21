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
       //gpl3 text
        return "GNU GENERAL PUBLIC LICENSE\n" +
                "Version 3, 29 June 2007\n\n" +
                "Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>\n" +
                "Everyone is permitted to copy and distribute verbatim copies\n" +
                "of this license document, but changing it is not allowed.\n\n" +
                "Preamble\n\n" +
                "The GNU General Public License is a free, copyleft license for\n" +
                "software and other kinds of works.\n\n" +
                "[Full license text not found. Please refer to https://www.gnu.org/licenses/gpl-3.0.en.html for the complete license.]\n";

    }
}
