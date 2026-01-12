package filehandling;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralized LogHog file format specification.
 * This class defines the standard format rules for LogHog files to ensure consistency
 * across all operations (saving, parsing, rendering, formatting).
 * 
 * Format Rules:
 * - File format: Entries separated by ONE blank line (2 newlines after content)
 * - Display format: Markdown renderer adds 2 blank lines between entries for visual readability
 * - Entry structure: timestamp line + content lines (no trailing blanks)
 * - Blank lines within entry content are preserved during parsing
 * 
 * Note: The display format is independent of file format - the renderer always shows
 * 2 blank lines between entries for comfortable reading, regardless of file content.
 */
public class LogFileFormat {
    
    /**
     * Line separator for file operations.
     * Uses platform-specific line separator for file writes.
     */
    public static final String LINE_SEPARATOR = System.lineSeparator();
    
    /**
     * Line separator for internal string operations.
     * Uses \n for consistency in string joins/splits (Files API handles conversion).
     */
    public static final String INTERNAL_LINE_SEPARATOR = "\n";
    
    /**
     * Number of blank lines between entries in the FILE.
     * One blank line between entries provides consistent formatting.
     */
    public static final int FILE_ENTRY_SEPARATOR_BLANKS = 1;
    
    /**
     * Number of blank lines the RENDERER adds between entries for visual spacing.
     * This creates comfortable reading space in the display.
     */
    public static final int DISPLAY_ENTRY_SEPARATOR_BLANKS = 2;
    
    /**
     * Maximum consecutive blank lines allowed before normalization reduces them.
     * Used during file cleanup operations.
     */
    public static final int MAX_CONSECUTIVE_BLANKS = 2;
    
    /**
     * Creates an entry string with proper formatting.
     * @param timestamp The timestamp line
     * @param content The entry content
     * @return Formatted entry string with correct line breaks
     */
    public static String createEntry(String timestamp, String content) {
        // Entry format: timestamp + newline + content + newline + blank line
        // The blank line ensures proper separation between entries in the file
        return timestamp + LINE_SEPARATOR + content + LINE_SEPARATOR + LINE_SEPARATOR;
    }
    
    /**
     * Removes trailing blank lines from an entry.
     * Entries should not have trailing blanks before the next entry.
     * @param entry List of lines in the entry
     */
    public static void removeTrailingBlanks(List<String> entry) {
        while (!entry.isEmpty() && entry.get(entry.size() - 1).trim().isEmpty()) {
            entry.remove(entry.size() - 1);
        }
    }
    
    /**
     * Normalizes spacing in a list of lines by removing excessive blank lines.
     * Allows up to MAX_CONSECUTIVE_BLANKS consecutive blanks, removes anything beyond that.
     * @param lines The lines to normalize
     * @return Normalized list with controlled blank line spacing
     */
    public static List<String> normalizeSpacing(List<String> lines) {
        List<String> normalized = new ArrayList<>();
        int consecutiveBlanks = 0;
        
        for (String line : lines) {
            boolean isBlank = line.trim().isEmpty();
            if (isBlank) {
                consecutiveBlanks++;
                // Allow up to MAX_CONSECUTIVE_BLANKS consecutive blank lines
                if (consecutiveBlanks <= MAX_CONSECUTIVE_BLANKS) {
                    normalized.add("");
                }
                // Skip any blanks beyond the limit
            } else {
                normalized.add(line);
                consecutiveBlanks = 0;
            }
        }
        
        return normalized;
    }
    
    /**
     * Formats a list of entries into file format.
     * Removes trailing blanks from each entry and adds proper spacing between them.
     * @param entries List of entries (each entry is a list of lines)
     * @return Formatted lines ready to write to file
     */
    public static List<String> formatEntries(List<List<String>> entries) {
        List<String> formatted = new ArrayList<>();
        
        for (int i = 0; i < entries.size(); i++) {
            List<String> entry = entries.get(i);
            
            // Remove trailing blank lines from entry
            removeTrailingBlanks(entry);
            
            // Add entry lines to output
            formatted.addAll(entry);
            
            // Add separator blanks between entries (FILE format uses compact spacing)
            if (i < entries.size() - 1) {
                for (int j = 0; j < FILE_ENTRY_SEPARATOR_BLANKS; j++) {
                    formatted.add("");
                }
            }
        }
        
        return formatted;
    }
    
    /**
     * Checks if a line should be included when parsing entry content.
     * Blank lines within entries are skipped to prevent accumulation.
     * @param line The line to check
     * @param isInEntry Whether we're currently inside an entry
     * @return true if the line should be included
     */
    public static boolean shouldIncludeInEntry(String line, boolean isInEntry) {
        String trimmed = line.trim();
        // Only include non-blank lines when inside an entry
        return isInEntry && !trimmed.isEmpty();
    }
}
