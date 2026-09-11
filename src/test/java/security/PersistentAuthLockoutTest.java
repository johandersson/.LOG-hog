package security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PersistentAuthLockoutTest {
    private static final long MAX_LOCKOUT_MS = 30L * 60L * 1000L;

    @TempDir
    Path tempHome;

    private String originalUserHome;

    @BeforeEach
    void setUp() {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempHome.toString());
    }

    @AfterEach
    void tearDown() {
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        }
    }

    @Test
    void bootstrapWithNoArtifactsDoesNotLockUser() {
        Properties settings = new Properties();
        long remaining = PersistentAuthLockout.getRemainingLockoutMillis(settings);
        assertEquals(0L, remaining);
    }

    @Test
    void missingAnchorWithExistingStateMigratesWithoutLockout() throws Exception {
        Properties settings = new Properties();
        PersistentAuthLockout.getRemainingLockoutMillis(settings);
        Path lockoutDir = tempHome.resolve(".loghog");
        Path anchorPath = lockoutDir.resolve("auth-lockout.anchor");

        Files.deleteIfExists(anchorPath);
        long remaining = PersistentAuthLockout.getRemainingLockoutMillis(settings);
        assertEquals(0L, remaining, "Expected legacy state migration when only anchor is missing");
    }

    @Test
    void missingKeyArtifactFailsClosed() throws Exception {
        Properties settings = new Properties();
        PersistentAuthLockout.getRemainingLockoutMillis(settings);
        PersistentAuthLockout.registerFailure(settings);
        Path lockoutDir = tempHome.resolve(".loghog");
        Path keyPath = lockoutDir.resolve("auth-lockout.key");

        Files.deleteIfExists(keyPath);
        long remaining = PersistentAuthLockout.getRemainingLockoutMillis(settings);
        assertTrue(remaining >= MAX_LOCKOUT_MS - 1000L, "Expected fail-closed maximum lockout when key artifact is missing");
    }

    @Test
    void invalidMacEncodingWithFailedAttemptsFailsClosed() throws Exception {
        Properties settings = new Properties();
        PersistentAuthLockout.getRemainingLockoutMillis(settings);
        PersistentAuthLockout.registerFailure(settings);

        Path statePath = tempHome.resolve(".loghog").resolve("auth-lockout.properties");
        Properties state = new Properties();
        try (var in = Files.newInputStream(statePath)) {
            state.load(in);
        }
        state.setProperty("authLockoutMac", "%%%");
        try (OutputStream out = Files.newOutputStream(statePath)) {
            state.store(out, "corrupt for test");
        }

        long remaining = PersistentAuthLockout.getRemainingLockoutMillis(settings);
        assertTrue(remaining >= MAX_LOCKOUT_MS - 1000L,
            "Expected fail-closed maximum lockout when state is corrupt and failures are recorded");
    }

    @Test
    void corruptStateMacWithCleanHistoryRecoversWithoutLockout() throws Exception {
        Properties settings = new Properties();
        PersistentAuthLockout.getRemainingLockoutMillis(settings);

        Path statePath = tempHome.resolve(".loghog").resolve("auth-lockout.properties");
        Properties state = new Properties();
        try (var in = Files.newInputStream(statePath)) {
            state.load(in);
        }
        state.setProperty("authLockoutMac", "%%%");
        try (OutputStream out = Files.newOutputStream(statePath)) {
            state.store(out, "corrupt for test");
        }

        long remaining = PersistentAuthLockout.getRemainingLockoutMillis(settings);
        assertEquals(0L, remaining,
            "Expected recovery without lockout when state MAC is corrupt but there are no recorded failures");
    }

    @Test
    void missingAnchorAndKeyFilesOnStartupDoesNotLockUser() throws Exception {
        Properties settings = new Properties();
        // First initialization
        PersistentAuthLockout.getRemainingLockoutMillis(settings);
        Path lockoutDir = tempHome.resolve(".loghog");
        Path keyPath = lockoutDir.resolve("auth-lockout.key");
        Path anchorPath = lockoutDir.resolve("auth-lockout.anchor");

        // Simulate file deletion (e.g., user deleted files, or permission issues)
        Files.deleteIfExists(keyPath);
        Files.deleteIfExists(anchorPath);
        
        // This is the user's issue: startup should not lock them out even if some files are missing
        long remaining = PersistentAuthLockout.getRemainingLockoutMillis(settings);
        assertEquals(0L, remaining, "Expected no lockout when key and anchor are missing but state exists (recovery scenario)");
    }
}
