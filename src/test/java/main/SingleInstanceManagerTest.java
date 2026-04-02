package main;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;

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
    }

    @AfterAll
    static void cleanupClass() throws IOException {
        // Clean up test files
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
    }

    @AfterEach
    void cleanup() {
        SingleInstanceManager.releaseLock();
    }

    @Test
    void testIsAnotherInstanceRunningWhenNoInstance() {
        System.out.println("🧪 Testing instance detection when no other instance running...");

        // First call should return false (no instance running)
        boolean result = SingleInstanceManager.isAnotherInstanceRunning();
        assertFalse(result, "Should detect no other instance running");

        // Clean up for other tests
        SingleInstanceManager.releaseLock();

        System.out.println("✅ Instance detection works (no other instance detected)");
    }

    @Test
    void testNotifyExistingInstance() {
        System.out.println("🧪 Testing notification of existing instance...");

        // This method should not throw exceptions (no-op with file locking)
        assertDoesNotThrow(() -> {
            SingleInstanceManager.notifyExistingInstance();
        });

        System.out.println("✅ Notify existing instance handles gracefully");
    }

    @Test
    void testShowAlreadyRunningDialog() {
        System.out.println("🧪 Testing already running dialog...");

        // In headless testing environment, it should not throw exceptions
        assertDoesNotThrow(() -> {
            SingleInstanceManager.showAlreadyRunningDialog();
        });

        System.out.println("✅ Already running dialog handles headless environment gracefully");
    }

    @Test
    void testFileLockBasics() {
        System.out.println("🧪 Testing basic file lock behavior...");

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

        System.out.println("✅ Basic file lock behavior works correctly");
    }

    @Test
    void testLockConflict() {
        System.out.println("🧪 Testing lock conflict detection...");

        assertDoesNotThrow(() -> {
            // Acquire first lock
            try (RandomAccessFile raf1 = new RandomAccessFile(testLockFile.toFile(), "rw");
                 FileChannel channel1 = raf1.getChannel()) {
                
                FileLock lock1 = channel1.tryLock();
                assertNotNull(lock1, "Should acquire first lock");
                
                // Try to acquire second lock - should fail
                try (RandomAccessFile raf2 = new RandomAccessFile(testLockFile.toFile(), "rw");
                     FileChannel channel2 = raf2.getChannel()) {
                    
                    FileLock lock2 = channel2.tryLock();
                    assertNull(lock2, "Should not acquire second lock");
                }
                
                lock1.release();
            }
        });

        System.out.println("✅ Lock conflict detection works correctly");
    }

    @Test
    void testReleaseLockIdempotent() {
        System.out.println("🧪 Testing releaseLock is idempotent...");

        // Calling releaseLock multiple times should not throw
        assertDoesNotThrow(() -> {
            SingleInstanceManager.releaseLock();
            SingleInstanceManager.releaseLock();
            SingleInstanceManager.releaseLock();
        });

        System.out.println("✅ releaseLock is idempotent");
    }

    @Test
    void testLockDirectoryCreation() {
        System.out.println("🧪 Testing lock directory creation...");

        Path lockDir = Path.of(System.getProperty("user.home"), ".loghog");
        
        // After isAnotherInstanceRunning, the directory should exist
        boolean result = SingleInstanceManager.isAnotherInstanceRunning();
        
        assertTrue(Files.exists(lockDir), ".loghog directory should be created");
        
        SingleInstanceManager.releaseLock();

        System.out.println("✅ Lock directory creation works correctly");
    }
}