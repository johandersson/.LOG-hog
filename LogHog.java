import javax.swing.*;

public class LogHog {
    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        // Let the OS draw the title bar and buttons (native chrome)
        JFrame.setDefaultLookAndFeelDecorated(false);

        SwingUtilities.invokeLater(() -> LogTextEditor.main(args));
    }
}
