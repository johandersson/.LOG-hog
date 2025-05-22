import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class LogTextEditor extends JFrame {

    private final JTextArea textArea = new JTextArea();
    private final JList<String> logList = new JList<>(new DefaultListModel<>());
    private final JTextArea entryArea = new JTextArea();
    private final LogFileHandler logFileHandler = new LogFileHandler();

    public LogTextEditor() {
        setupUI();
        setupEventListeners();
        logFileHandler.loadLogEntries((DefaultListModel<String>) logList.getModel());
    }

    private void setupUI() {
        setTitle("Log Text Editor");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Entry", createEntryPanel());
        tabbedPane.add("Log Entries", createLogPanel());
        //add ? menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem saveMenuItem = new JMenuItem("Save all CTRL+S");
        saveMenuItem.addActionListener(e -> {
            logFileHandler.saveText(textArea.getText(), (DefaultListModel<String>) logList.getModel());
            textArea.setText("");
        });
        fileMenu.add(saveMenuItem);
        JMenuItem loadMenuItem = new JMenuItem("Reload all CTRL+R");
        loadMenuItem.addActionListener(e -> logFileHandler.loadLogEntries((DefaultListModel<String>) logList.getModel()));
        fileMenu.add(loadMenuItem);
        menuBar.add(fileMenu);

        setJMenuBar(menuBar);
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        mainPanel.add(new JLabel("Press Ctrl+S to save and Ctrl+R to load"), BorderLayout.SOUTH);
        setContentPane(mainPanel);
        setJMenuBar(menuBar);
        setVisible(true);
        setFocusable(true);

        add(tabbedPane);
        SwingUtilities.invokeLater(textArea::requestFocus);
    }

    private JPanel createEntryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        textArea.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        entryArea.setFont(new Font("Arial", Font.PLAIN, 14));
        entryArea.setEditable(false);
        panel.add(new JScrollPane(logList), BorderLayout.NORTH);
        panel.add(new JScrollPane(entryArea), BorderLayout.CENTER);
        return panel;
    }

    private void setupEventListeners() {
        textArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                System.out.println("Key pressed: " + e.getKeyCode());
                if (e.isControlDown()) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_S -> {
                            logFileHandler.saveText(textArea.getText(), (DefaultListModel<String>) logList.getModel());
                            //clear text area after saving
                            textArea.setText("");
                        }
                        case KeyEvent.VK_R -> logFileHandler.loadLogEntries((DefaultListModel<String>) logList.getModel());
                    }
                }
            }
        });


        logList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedEntry = logList.getSelectedValue();
                if (selectedEntry != null) {
                    entryArea.setText(logFileHandler.loadEntry(selectedEntry));
                }
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LogTextEditor().setVisible(true));
    }
}


