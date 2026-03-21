package gui;

public final class InputLimits {
    // Maximum characters allowed in a single log entry (~64 KiB)
    public static final int ENTRY_MAX_CHARS = 65536;
    // Generic max for auxiliary text fields (URLs, paths, etc.)
    public static final int FIELD_MAX_CHARS = 8192;
    // Display text when inserting links
    public static final int DISPLAY_TEXT_MAX = 2048;

    private InputLimits() { }
}
