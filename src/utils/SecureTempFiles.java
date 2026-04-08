package utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Helper for creating secure temporary files with restrictive permissions.
 * Best-effort: sets POSIX perms when available, falls back to java.io.File owner-only flags.
 */
public final class SecureTempFiles {
    private SecureTempFiles() {}

    private static final Set<Path> DELETE_ON_EXIT = Collections.synchronizedSet(new HashSet<>());
    private static volatile boolean SHUTDOWN_HOOK_REGISTERED = false;

    private static void registerShutdownHookIfNeeded() {
        if (SHUTDOWN_HOOK_REGISTERED) return;
        synchronized (SecureTempFiles.class) {
            if (SHUTDOWN_HOOK_REGISTERED) return;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                synchronized (DELETE_ON_EXIT) {
                    for (Path p : DELETE_ON_EXIT) {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception ignored) {
                        }
                    }
                    DELETE_ON_EXIT.clear();
                }
            }));
            SHUTDOWN_HOOK_REGISTERED = true;
        }
    }

    public static Path createSecureTempFile(Path dir, String prefix, String suffix) throws java.io.IOException {
        return createSecureTempFile(dir, prefix, suffix, false);
    }

    /**
     * Create a secure temporary file and optionally mark it for deletion on JVM exit / shutdown.
     * This is best-effort and attempts to delete files in a shutdown hook to cover crash/abnormal exits.
     */
    public static Path createSecureTempFile(Path dir, String prefix, String suffix, boolean deleteOnExit) throws java.io.IOException {
        Path parent = dir != null ? dir : Path.of(System.getProperty("java.io.tmpdir"));
        Files.createDirectories(parent);
        Path tmp = Files.createTempFile(parent, prefix, suffix);

        // Try to set restrictive permissions
        try {
            try {
                var perms = java.nio.file.attribute.PosixFilePermissions.fromString("rw-------");
                Files.setPosixFilePermissions(tmp, perms);
            } catch (UnsupportedOperationException | SecurityException ignored) {
                // Fallback for non-POSIX filesystems
                File f = tmp.toFile();
                f.setReadable(true, true);
                f.setWritable(true, true);
                f.setExecutable(false, true);
            }
        } catch (Exception ignored) {
            // Best-effort only
        }

        if (deleteOnExit) {
            try {
                // mark for deletion via File API (best-effort) and track for shutdown cleanup
                tmp.toFile().deleteOnExit();
            } catch (Throwable ignored) {
            }
            DELETE_ON_EXIT.add(tmp);
            registerShutdownHookIfNeeded();
        }

        return tmp;
    }

    /**
     * Mark an existing path for best-effort deletion on JVM exit/shutdown.
     */
    public static void markForDeleteOnExit(Path p) {
        if (p == null) return;
        try {
            p.toFile().deleteOnExit();
        } catch (Throwable ignored) {
        }
        DELETE_ON_EXIT.add(p);
        registerShutdownHookIfNeeded();
    }
}
