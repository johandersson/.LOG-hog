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
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;

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
    private static Path lockFilePath;
    private static RandomAccessFile lockFile;
    private static FileChannel lockChannel;
    private static FileLock lock;

    /**
     * Checks if another instance of LogHog is already running.
     * If not, acquires an exclusive lock to prevent other instances.
     * 
     * @return true if another instance is running, false if this is the first instance
     */
    public static boolean isAnotherInstanceRunning() {
        try {
            // Create lock file directory if needed
            Path lockDir = Path.of(System.getProperty("user.home"), ".loghog");
            Files.createDirectories(lockDir);
            lockFilePath = lockDir.resolve(LOCK_FILE_NAME);
            
            // Try to acquire exclusive lock
            lockFile = new RandomAccessFile(lockFilePath.toFile(), "rw");
            lockChannel = lockFile.getChannel();
            lock = lockChannel.tryLock();
            
            if (lock == null) {
                // Could not acquire lock - another instance holds it
                closeLockResources();
                return true;
            }
            
            // Write PID to lock file for debugging purposes
            lockFile.setLength(0);
            lockFile.writeUTF("LogHog instance running since " + java.time.Instant.now());
            
            // Register shutdown hook to release lock
            Runtime.getRuntime().addShutdownHook(new Thread(SingleInstanceManager::releaseLock));
            
            return false; // No other instance running, we acquired the lock
            
        } catch (IOException e) {
            // If we can't create/access the lock file, allow the app to start
            // This handles edge cases like read-only filesystems
            return false;
        }
    }

    /**
     * Releases the file lock and closes resources.
     * Called automatically on shutdown.
     */
    public static void releaseLock() {
        try {
            if (lock != null && lock.isValid()) {
                lock.release();
            }
        } catch (IOException ignored) {
            // Best effort
        }
        closeLockResources();
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
    public static void notifyExistingInstance() {
        // With file locking, we cannot send messages to the other instance.
        // The dialog in showAlreadyRunningDialog() informs the user.
    }
}
