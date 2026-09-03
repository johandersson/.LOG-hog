package main;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class SingleInstanceManagerTest {

    @TempDir
    Path tempDir;

    private Path testLockFile;

    @BeforeEach
    void setup() {
        testLockFile = tempDir.resolve("test.lock");
        SingleInstanceManager.releaseLock();
    }

    @AfterEach
    void cleanup() {
        SingleInstanceManager.releaseLock();
    }

    @Test
    void testIsAnotherInstanceRunningWhenNoInstance() {
        boolean result = SingleInstanceManager.isAnotherInstanceRunning();
        assertFalse(result);
        SingleInstanceManager.releaseLock();
    }

    @Test
    void testNotifyExistingInstance() {
        assertDoesNotThrow(SingleInstanceManager::notifyExistingInstance);
    }

    @Test
    void testShowAlreadyRunningDialog() {
        assertDoesNotThrow(SingleInstanceManager::showAlreadyRunningDialog);
    }

    @Test
    void testFileLockBasics() {
        assertDoesNotThrow(() -> {
            try (RandomAccessFile raf = new RandomAccessFile(testLockFile.toFile(), "rw");
                 FileChannel channel = raf.getChannel()) {
                FileLock lock = channel.tryLock();
                assertNotNull(lock);
                assertTrue(lock.isValid());
                lock.release();
                assertFalse(lock.isValid());
            }
        });
    }

    @Test
    void testLockConflict() throws Exception {
        try (RandomAccessFile raf1 = new RandomAccessFile(testLockFile.toFile(), "rw");
             FileChannel channel1 = raf1.getChannel()) {
            FileLock lock1 = channel1.tryLock();
            assertNotNull(lock1);

            try (RandomAccessFile raf2 = new RandomAccessFile(testLockFile.toFile(), "rw");
                 FileChannel channel2 = raf2.getChannel()) {
                try {
                    FileLock lock2 = channel2.tryLock();
                    assertNull(lock2, "Second lock should not be acquired");
                } catch (OverlappingFileLockException expected) {
                    assertTrue(true);
                }
            } finally {
                lock1.release();
            }
        }
    }

    @Test
    void testReleaseLockIdempotent() {
        assertDoesNotThrow(() -> {
            SingleInstanceManager.releaseLock();
            SingleInstanceManager.releaseLock();
            SingleInstanceManager.releaseLock();
        });
    }

    @Test
    void testLockDirectoryCreation() {
        Path lockDir = Path.of(System.getProperty("user.home"), ".loghog");
        SingleInstanceManager.isAnotherInstanceRunning();
        assertTrue(Files.exists(lockDir));
        SingleInstanceManager.releaseLock();
    }
}
