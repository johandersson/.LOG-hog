package filehandling;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.lang.ref.SoftReference;
import javax.swing.text.StyledDocument;

/**
 * Data container for parsed log file information.
 * Holds both the complete parsed entries and the subset to be rendered.
 * Used to avoid re-parsing when statistics need to be calculated.
 */
public class ParsedLogData {
    public final List<List<String>> allEntries;
    public final List<List<String>> entriesToRender;
    private final int totalEntryCount;

    /**
     * Creates a new ParsedLogData instance.
     *
     * @param allEntries All parsed log entries from the file
     * @param entriesToRender The subset of entries to display (may be limited for performance)
     */
    public ParsedLogData(List<List<String>> allEntries, List<List<String>> entriesToRender) {
        this.allEntries = allEntries;
        this.entriesToRender = entriesToRender;
        this.totalEntryCount = allEntries != null ? allEntries.size() : entriesToRender.size();
    }

    /**
     * Create ParsedLogData when only a total entry count is known (streamed parser).
     */
    public ParsedLogData(int totalEntryCount, List<List<String>> entriesToRender) {
        this.allEntries = null;
        this.entriesToRender = entriesToRender;
        this.totalEntryCount = totalEntryCount;
    }

    /**
     * Optional per-entry rendered document cache keyed by the display timestamp
     * (e.g. "12:34 2024-05-20 (1)"). Stored as SoftReference to avoid
     * holding large documents in memory permanently.
     */
    private static final int DEFAULT_PER_ENTRY_CACHE_SIZE = 200;

    /**
     * A bounded LRU cache for per-entry rendered documents. Uses access-order
     * LinkedHashMap and is wrapped with Collections.synchronizedMap for thread
     * safety since rendering and GC may access it from different threads.
     * Entries are stored as SoftReferences so the JVM can reclaim memory under
     * pressure.
     */
    public final Map<String, SoftReference<StyledDocument>> perEntryDocCache =
            Collections.synchronizedMap(new LinkedHashMap<String, SoftReference<StyledDocument>>(DEFAULT_PER_ENTRY_CACHE_SIZE, 0.75f, true) {
                private static final long serialVersionUID = 1L;

                @Override
                protected boolean removeEldestEntry(Map.Entry<String, SoftReference<StyledDocument>> eldest) {
                    return size() > DEFAULT_PER_ENTRY_CACHE_SIZE;
                }
            });

    /**
     * Gets the total number of entries in the log file.
     */
    public int getTotalEntryCount() {
        return allEntries != null ? allEntries.size() : totalEntryCount;
    }

    /**
     * Gets the number of entries being rendered (may be less than total for performance).
     */
    public int getRenderedEntryCount() {
        return entriesToRender.size();
    }

    /**
     * Checks if all entries are being rendered or if there's a subset.
     */
    public boolean isShowingAllEntries() {
        if (allEntries != null) return allEntries.size() == entriesToRender.size();
        return totalEntryCount == entriesToRender.size();
    }
}