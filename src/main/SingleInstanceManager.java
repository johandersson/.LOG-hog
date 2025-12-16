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
    private static final int PORT = 29999;
    private static ServerSocket serverSocket;

    public static boolean isAnotherInstanceRunning() {
        // First try to connect to see if an instance is actually running
        try {
            Socket socket = new Socket("localhost", PORT);
            socket.close();
            return true; // Connected successfully, instance is running
        } catch (IOException e) {
            // Can't connect, try to bind the port
            try {
                serverSocket = new ServerSocket(PORT);
                return false; // Bound successfully, no instance running
            } catch (IOException e2) {
                return false; // Can't bind, but since can't connect, assume no instance
            }
        }
    }

    public static void showAlreadyRunningDialog() {
        JOptionPane.showMessageDialog(null, "Another instance is already running. Switching to it.");
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
            // Could not connect, instance might have closed
        }
    }
}
