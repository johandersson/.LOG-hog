package main;

import java.io.IOException;
import java.net.ServerSocket;
import javax.swing.JOptionPane;

public class SingleInstanceManager {
    private static final int PORT = 9999;
    private static ServerSocket serverSocket;

    public static boolean isAnotherInstanceRunning() {
        try {
            serverSocket = new ServerSocket(PORT);
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    public static void showAlreadyRunningDialog() {
        JOptionPane.showMessageDialog(null, "Another instance is already running.");
    }
}