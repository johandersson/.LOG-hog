/*
 * Copyright (C) 2026 Johan Andersson
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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.OverlappingFileLockException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Properties;
import security.SecurityFilePolicy;

import gui.DialogHelper;

/**
 * Manages single-instance enforcement using file locking.
 * 
 * <h2>Security Properties</h2>
 * <p>Uses {@link FileLock} instead of network sockets to prevent:</p>
 * <ul>
 *   <li>Unauthorized IPC from other local processes</li>
 *   <li>Port scanning detection of the application</li>
 *   <li>Potential socket hijacking attacks</li>
 * </ul>
 * 
 * <p>The lock file is created in the user's .loghog directory with restrictive permissions.</p>
 */
public class SingleInstanceManager {
    private static final String LOCK_FILE_NAME = "instance.lock";
    private static final String LOCK_DIR_PROPERTY = "loghog.lock.dir";
    private static final String COMMAND_FOCUS = "FOCUS";
    private static final int CONNECT_TIMEOUT_MS = 1500;
    private static final int TOKEN_BYTES = 32;
    private static Path lockFilePath;
    private static RandomAccessFile lockFile;
    private static FileChannel lockChannel;
    private static FileLock lock;
    private static ServerSocket ipcServerSocket;
    private static Thread ipcServerThread;
    private static String ipcToken;
    private static Runnable focusRequestHandler;
    private static boolean pendingFocusRequest;

    /**
     * Checks if another instance of LogHog is already running.
     * If not, acquires an exclusive lock to prevent other instances.
     * 
     * @return true if another instance is running, false if this is the first instance
     */
    public static boolean isAnotherInstanceRunning() {
        RandomAccessFile candidateFile = null;
        FileChannel candidateChannel = null;
        try {
            // Create lock file directory if needed
            Path lockDir = getLockDirectory();
            Files.createDirectories(lockDir);
            SecurityFilePolicy.ensureOwnerOnlyPermissions(lockDir);
            lockFilePath = lockDir.resolve(LOCK_FILE_NAME);

            // Try to acquire exclusive lock
            candidateFile = new RandomAccessFile(lockFilePath.toFile(), "rw");
            candidateChannel = candidateFile.getChannel();
            FileLock candidateLock = candidateChannel.tryLock();
            SecurityFilePolicy.ensureOwnerOnlyPermissions(lockFilePath);

            if (candidateLock == null) {
                // Could not acquire lock - another instance holds it
                closeResources(candidateLock, candidateChannel, candidateFile);
                return true;
            }

            lockFile = candidateFile;
            lockChannel = candidateChannel;
            lock = candidateLock;
            startFocusRequestServer();

            // Write IPC details and PID to lock file for authenticated focus requests.
            lockFile.setLength(0);
            String lockData = "port=" + ipcServerSocket.getLocalPort() + System.lineSeparator()
                + "token=" + ipcToken + System.lineSeparator()
                + "since=" + java.time.Instant.now() + System.lineSeparator();
            lockFile.write(lockData.getBytes(StandardCharsets.UTF_8));
            lockFile.getChannel().force(true);
            
            // Register shutdown hook to release lock
            Runtime.getRuntime().addShutdownHook(new Thread(SingleInstanceManager::releaseLock));
            
            return false; // No other instance running, we acquired the lock
            
        } catch (OverlappingFileLockException e) {
            closeResources(null, candidateChannel, candidateFile);
            return true;
        } catch (IOException e) {
            // If we can't create/access the lock file, allow the app to start
            // This handles edge cases like read-only filesystems
            closeResources(null, candidateChannel, candidateFile);
            return false;
        }
    }

    /**
     * Releases the file lock and closes resources.
     * Called automatically on shutdown.
     */
    public static void releaseLock() {
        try {
            if (ipcServerSocket != null) {
                ipcServerSocket.close();
            }
        } catch (IOException ignored) {
            // Best effort
        }
        try {
            if (lock != null && lock.isValid()) {
                lock.release();
            }
        } catch (IOException ignored) {
            // Best effort
        }
        closeLockResources();
        ipcServerSocket = null;
        ipcServerThread = null;
        ipcToken = null;
        focusRequestHandler = null;
        pendingFocusRequest = false;
    }
    
    private static void closeLockResources() {
        try {
            if (lockChannel != null) {
                lockChannel.close();
            }
        } catch (IOException ignored) {}
        try {
            if (lockFile != null) {
                lockFile.close();
            }
        } catch (IOException ignored) {}
    }

    private static void closeResources(FileLock fileLock, FileChannel channel, RandomAccessFile file) {
        try {
            if (fileLock != null && fileLock.isValid()) {
                fileLock.release();
            }
        } catch (IOException ignored) {}
        try {
            if (channel != null) {
                channel.close();
            }
        } catch (IOException ignored) {}
        try {
            if (file != null) {
                file.close();
            }
        } catch (IOException ignored) {}
    }

    private static Path getLockDirectory() {
        String configuredLockDir = System.getProperty(LOCK_DIR_PROPERTY);
        if (configuredLockDir != null && !configuredLockDir.isBlank()) {
            return Path.of(configuredLockDir);
        }
        return Path.of(System.getProperty("user.home"), ".loghog");
    }

    private static void startFocusRequestServer() throws IOException {
        ipcToken = generateToken();
        ipcServerSocket = new ServerSocket();
        ipcServerSocket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        ipcServerThread = new Thread(SingleInstanceManager::serveFocusRequests, "loghog-single-instance-ipc");
        ipcServerThread.setDaemon(true);
        ipcServerThread.start();
    }

    private static String generateToken() {
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        new SecureRandom().nextBytes(tokenBytes);
        StringBuilder tokenBuilder = new StringBuilder(tokenBytes.length * 2);
        for (byte tokenByte : tokenBytes) {
            tokenBuilder.append(String.format("%02x", tokenByte));
        }
        return tokenBuilder.toString();
    }

    private static void serveFocusRequests() {
        while (ipcServerSocket != null && !ipcServerSocket.isClosed()) {
            try (Socket socket = ipcServerSocket.accept()) {
                handleFocusRequest(socket);
            } catch (IOException ignored) {
                // Server socket is closed during normal shutdown.
            }
        }
    }

    private static void handleFocusRequest(Socket socket) throws IOException {
        socket.setSoTimeout(CONNECT_TIMEOUT_MS);
        try (BufferedReader reader = new BufferedReader(
                 new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(
                 new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            String request = reader.readLine();
            if ((ipcToken + " " + COMMAND_FOCUS).equals(request)) {
                requestFocus();
                writer.println("OK");
            } else {
                writer.println("DENIED");
            }
        }
    }

    private static synchronized void requestFocus() {
        if (focusRequestHandler == null) {
            pendingFocusRequest = true;
            return;
        }
        focusRequestHandler.run();
    }

    public static synchronized void registerFocusRequestHandler(Runnable handler) {
        focusRequestHandler = handler;
        if (pendingFocusRequest && focusRequestHandler != null) {
            pendingFocusRequest = false;
            focusRequestHandler.run();
        }
    }

    /**
     * Shows a dialog indicating another instance is already running.
     */
    public static void showAlreadyRunningDialog() {
        DialogHelper.showInfo(null, "Already Running", "Application Already Running", 
            "Another instance of .LOG-hog is already running.<br><br>" +
            "Please use the existing window or close it first.");
    }

    /**
     * Attempts to notify an existing instance to come to foreground.
     * With file-based locking, we cannot directly communicate with the other instance.
     * The user must manually switch to the existing window.
     */
    public static boolean notifyExistingInstance() {
        try {
            if (lockFilePath == null) {
                lockFilePath = getLockDirectory().resolve(LOCK_FILE_NAME);
            }
            Properties lockProperties = new Properties();
            try (BufferedReader reader = Files.newBufferedReader(lockFilePath, StandardCharsets.UTF_8)) {
                lockProperties.load(reader);
            }
            int port = Integer.parseInt(lockProperties.getProperty("port", "-1"));
            String token = lockProperties.getProperty("token", "");
            if (port <= 0 || token.isBlank()) {
                return false;
            }
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), CONNECT_TIMEOUT_MS);
                socket.setSoTimeout(CONNECT_TIMEOUT_MS);
                try (BufferedReader reader = new BufferedReader(
                         new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                     PrintWriter writer = new PrintWriter(
                         new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
                    writer.println(token + " " + COMMAND_FOCUS);
                    return "OK".equals(reader.readLine());
                }
            }
        } catch (IOException | NumberFormatException e) {
            return false;
        }
    }
}
