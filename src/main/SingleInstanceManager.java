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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import gui.DialogHelper;

public class SingleInstanceManager {
    private static final int PORT = 9999;
    private static ServerSocket serverSocket;

    public static boolean isAnotherInstanceRunning() {
        // First try to connect and ping to see if a LogHog instance is actually running
        try {
            Socket socket = new Socket("localhost", PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("LOGHOG_PING");
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String response = in.readLine();
            in.close();
            out.close();
            socket.close();
            return "LOGHOG_PONG".equals(response); // Only if it's LogHog responding
        } catch (IOException e) {
            // Can't connect or communicate, try to bind the port
            try {
                serverSocket = new ServerSocket(PORT);
                serverSocket.setReuseAddress(true);
                // Start the listening thread immediately to handle incoming connections
                startServerThread();
                return false; // Bound successfully, no instance running
            } catch (IOException e2) {
                // Port is already bound by another process, but we couldn't connect to LogHog
                // This could be another application using the port, so we assume no LogHog instance
                return false;
            }
        }
    }

    private static void startServerThread() {
        Thread serverThread = new Thread(() -> {
            try {
                while (!serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();
                    handleClientRequest(clientSocket);
                }
            } catch (IOException e) {
                // Socket closed, which is expected when the application exits
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // Add shutdown hook to close the socket gracefully on exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException ex) {
                    // Ignore
                }
            }
        }));
    }

    private static void handleClientRequest(Socket clientSocket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String message = in.readLine();
            if ("BRING_TO_FRONT".equals(message)) {
                // This will be handled by the main application
            } else if ("LOGHOG_PING".equals(message)) {
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                out.println("LOGHOG_PONG");
                out.close();
            }
            in.close();
        } catch (IOException e) {
            // Ignore communication errors
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    public static void showAlreadyRunningDialog() {
        DialogHelper.showInfo(null, "Already Running", "Application Already Running", "Another instance is already running. Switching to it.");
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
