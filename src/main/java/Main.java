import javax.swing.*;

public class Main {
    //main class to load the GUI of a LogTextEditor
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LogTextEditor logTextEditor = new LogTextEditor();
            logTextEditor.setVisible(true);
        });
    }
}
