package filehandling;

/**
 * Centralized resource limit constants to protect against DoS and memory exhaustion.
 */
public final class ResourceLimits {
    private ResourceLimits() {}
    // Recommended defaults (not configurable yet)
    // Max file size allowed for full-file reads (200 MB)
    public static final long MAX_FILE_SIZE = 200L * 1024L * 1024L;

    // Average entry size assumption (bytes) used to derive collection cap
    public static final int AVG_ENTRY_BYTES = 1000;

    // Safety factor to leave headroom for parsing/JSwing structures
    public static final double COLLECTION_SAFETY_FACTOR = 0.5;

    // Derived max number of entries allowed when parsing files
    public static final int MAX_COLLECTION_SIZE = (int) Math.floor(MAX_FILE_SIZE / (double) AVG_ENTRY_BYTES * COLLECTION_SAFETY_FACTOR);

    // UI render cap for FullLog view (limit how many entries are passed to renderer)
    // Increased to support large log files while still protecting memory.
    public static final int MAX_ENTRIES_TO_RENDER = 600_000;
}
