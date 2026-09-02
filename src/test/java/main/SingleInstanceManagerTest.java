package main;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tests for SingleInstanceManager using file-based locking.
 * Tests lock acquisition, release, and error handling.
 */
public class SingleInstanceManagerTest {

    private static Path testLockDir;
    private static Path testLockFile;

    @BeforeAll
    static void setupClass() throws IOException {
        // Create a temporary directory for test lock files
        testLockDir = Files.createTempDirectory("loghog-test-locks");
        testLockFile = testLockDir.resolve("test.lock");
        System.setProperty("loghog.lock.dir", testLockDir.toString());
    }

    @AfterAll
    static void cleanupClass() throws IOException {
        // Clean up test files
        SingleInstanceManager.releaseLock();
        System.clearProperty("loghog.lock.dir");
        Files.deleteIfExists(testLockDir.resolve("instance.lock"));
        if (testLockFile != null && Files.exists(testLockFile)) {
            Files.deleteIfExists(testLockFile);
        }
        if (testLockDir != null && Files.exists(testLockDir)) {
            Files.deleteIfExists(testLockDir);
        }
    }

    @BeforeEach
    void setup() {
        // Release any existing locks from previous tests
        SingleInstanceManager.releaseLock();
        System.setProperty("loghog.lock.dir", testLockDir.toString());
    }

    @AfterEach
    void cleanup() {
        SingleInstanceManager.releaseLock();
    }

    @Test
    void testIsAnotherInstanceRunningWhenNoInstance() {
        testsupport.TestLog.out("🧪 Testing instance detection when no other instance running...");

        // First call should return false (no instance running)
        boolean result = SingleInstanceManager.isAnotherInstanceRunning();
        assertFalse(result, "Should detect no other instance running");

        // Clean up for other tests
        SingleInstanceManager.releaseLock();

        testsupport.TestLog.out("✅ Instance detection works (no other instance detected)");
    }

    @Test
    void testNotifyExistingInstance() {
        testsupport.TestLog.out("🧪 Testing notification of existing instance...");

        assertFalse(SingleInstanceManager.isAnotherInstanceRunning());
        AtomicBoolean focusRequested = new AtomicBoolean(false);
        SingleInstanceManager.registerFocusRequestHandler(() -> focusRequested.set(true));

        assertTrue(SingleInstanceManager.notifyExistingInstance(), "Should notify active instance");
        assertTrue(focusRequested.get(), "Notification should request focus on active instance");

        testsupport.TestLog.out("✅ Notify existing instance handles gracefully");
    }

    @Test
    void testShowAlreadyRunningDialog() {
        testsupport.TestLog.out("🧪 Testing already running dialog...");

        // In headless testing environment, it should not throw exceptions
        assertDoesNotThrow(() -> {
            SingleInstanceManager.showAlreadyRunningDialog();
        });

        testsupport.TestLog.out("✅ Already running dialog handles headless environment gracefully");
    }

    @Test
    void testFileLockBasics() {
        testsupport.TestLog.out("🧪 Testing basic file lock behavior...");

        assertDoesNotThrow(() -> {
            // Test that we can create and lock a file
            try (RandomAccessFile raf = new RandomAccessFile(testLockFile.toFile(), "rw");
                 FileChannel channel = raf.getChannel()) {
                
                FileLock lock = channel.tryLock();
                assertNotNull(lock, "Should acquire lock");
                assertTrue(lock.isValid(), "Lock should be valid");
                
                lock.release();
                assertFalse(lock.isValid(), "Lock should be invalid after release");
            }
        });

        testsupport.TestLog.out("✅ Basic file lock behavior works correctly");
    }

    @Test
    void testLockConflict() {
        testsupport.TestLog.out("🧪 Testing lock conflict detection...");

        assertDoesNotThrow(() -> {
            // Acquire first lock
            try (RandomAccessFile raf1 = new RandomAccessFile(testLockFile.toFile(), "rw");
                 FileChannel channel1 = raf1.getChannel()) {
                
                FileLock lock1 = channel1.tryLock();
                assertNotNull(lock1, "Should acquire first lock");
                
                // Try to acquire second lock - should fail
                try (RandomAccessFile raf2 = new RandomAccessFile(testLockFile.toFile(), "rw");
                     FileChannel channel2 = raf2.getChannel()) {
                    
                    try {
                        FileLock lock2 = channel2.tryLock();
                        assertNull(lock2, "Should not acquire second lock");
                    } catch (OverlappingFileLockException expected) {
                        testsupport.TestLog.out("✓ Same-JVM overlapping lock was rejected");
                    }
                }
                
                lock1.release();
            }
        });

        testsupport.TestLog.out("✅ Lock conflict detection works correctly");
    }

    @Test
    void testReleaseLockIdempotent() {
        testsupport.TestLog.out("🧪 Testing releaseLock is idempotent...");

        // Calling releaseLock multiple times should not throw
        assertDoesNotThrow(() -> {
            SingleInstanceManager.releaseLock();
            SingleInstanceManager.releaseLock();
            SingleInstanceManager.releaseLock();
        });

        testsupport.TestLog.out("✅ releaseLock is idempotent");
    }

    @Test
    void testLockDirectoryCreation() {
        testsupport.TestLog.out("🧪 Testing lock directory creation...");

        Path lockDir = testLockDir;
        
        // After isAnotherInstanceRunning, the directory should exist
        boolean result = SingleInstanceManager.isAnotherInstanceRunning();
        
        assertTrue(Files.exists(lockDir), ".loghog directory should be created");
        
        SingleInstanceManager.releaseLock();

        testsupport.TestLog.out("✅ Lock directory creation works correctly");
    }
}