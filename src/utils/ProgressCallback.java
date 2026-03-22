package utils;

/**
 * Simple callback for reporting total and processed bytes for progress UI.
 */
public interface ProgressCallback {
    void setTotalBytes(long bytes);
    void setProcessedBytes(long bytes);
}
