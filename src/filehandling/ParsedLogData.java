package filehandling;

import java.util.List;

/**
 * Data container for parsed log file information.
 * Holds both the complete parsed entries and the subset to be rendered.
 * Used to avoid re-parsing when statistics need to be calculated.
 */
public class ParsedLogData {
    public final List<List<String>> allEntries;
    public final List<List<String>> entriesToRender;

    /**
     * Creates a new ParsedLogData instance.
     *
     * @param allEntries All parsed log entries from the file
     * @param entriesToRender The subset of entries to display (may be limited for performance)
     */
    public ParsedLogData(List<List<String>> allEntries, List<List<String>> entriesToRender) {
        this.allEntries = allEntries;
        this.entriesToRender = entriesToRender;
    }

    /**
     * Gets the total number of entries in the log file.
     */
    public int getTotalEntryCount() {
        return allEntries.size();
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
        return allEntries.size() == entriesToRender.size();
    }
}