import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class InformationPanel extends JPanel {
    public InformationPanel(JTabbedPane tabPane, String fileNameForText, String title) {
        super(new BorderLayout(8, 8));
        createPanel(tabPane, fileNameForText, title);
    }

    private void createPanel(JTabbedPane tabPanel, String fileNameForText, String title) {
        // Load license text
        String informationTextToDisplay = loadPanelText(fileNameForText);
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        createHeader(title);


        JTextPane tp = new JTextPane();
        tp.setEditable(false);
        tp.setBackground(Color.WHITE);
        MarkdownRenderer.renderMarkdown(tp, informationTextToDisplay.lines().toList());
        MarkdownRenderer.addLinkListeners(tp);

        JScrollPane sp = new JScrollPane(tp);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(sp, BorderLayout.CENTER);

        // Bottom: OK button returns to Entry tab (index 0)
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        JButton ok = new AccentButton("OK");
        ok.addActionListener(e -> {
            if (tabPanel != null && tabPanel.getTabCount() > 0) tabPanel.setSelectedIndex(0);
        });
        bottom.add(ok);
        add(bottom, BorderLayout.SOUTH);
    }

    private void createHeader(String title) {
        // Header
        JLabel header = new JLabel(title);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 16f));
        header.setForeground(new Color(0x2B3A42));
        add(header, BorderLayout.NORTH);
    }

    private String loadPanelText(String fileName) {
        // 1) Try file in current working directory
        Path p = Path.of(fileName);
        try {
            if (Files.exists(p)) return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException couldNotReadFile) {
            return "Could not read " + fileName + " from file system: " + couldNotReadFile.getMessage();
        }

        // 2) Try resource from JAR
        try (InputStream is = getClass().getResourceAsStream("/" + fileName)) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } else {
                return fileName + " not found as file or resource.";
            }
        } catch (IOException e) {
            return "Could not read " + fileName + " from resources: " + e.getMessage();
        }
    }

}
