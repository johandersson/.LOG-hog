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

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
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

    public static ServerSocket getServerSocket() {
        return serverSocket;
    }

    public static void notifyExistingInstance() {
        try {
            Socket socket = new Socket("localhost", PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("BRING_TO_FRONT");
            out.close();
            socket.close();
        } catch (IOException e) {
            // If we can't connect, just show the dialog
            showAlreadyRunningDialog();
        }
    }
}
