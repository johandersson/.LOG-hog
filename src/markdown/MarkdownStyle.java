/*
 * Copyright (C) 2025 Johan Andersson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package markdown;

import java.awt.Color;

/**
 * Centralized markdown rendering style constants.
 * All font sizes, colors, indentation, and styling values are defined here
 * to ensure consistency across all markdown rendering operations.
 * 
 * SINGLE POINT OF REFERENCE for all visual styling in markdown rendering.
 * 
 * DOCUMENT_LINE_SEPARATOR is used for line breaks WITHIN entries.
 * Entry-to-entry spacing is controlled by LogFileFormat.DISPLAY_ENTRY_SEPARATOR_BLANKS.
 */
public class MarkdownStyle {
    
    // Font families
    public static final String FONT_FAMILY_DEFAULT = "Segoe UI";
    public static final String FONT_FAMILY_CODE = "Consolas";
    public static final String FONT_FAMILY_MONOSPACED = "Monospaced";
    
    // Font sizes
    public static final int FONT_SIZE_DEFAULT = 14;
    public static final int FONT_SIZE_TIMESTAMP = 16;
    public static final int FONT_SIZE_H1 = 18;
    public static final int FONT_SIZE_H2 = 16;
    public static final int FONT_SIZE_H3 = 14;
    public static final int FONT_SIZE_QUOTE_BORDER = 14;
    
    // Colors - Text
    public static final Color COLOR_DEFAULT_TEXT = Color.DARK_GRAY;
    public static final Color COLOR_TIMESTAMP = Color.BLACK;
    public static final Color COLOR_LINK = Color.BLUE;
    public static final Color COLOR_RED_TEXT = Color.RED;
    
    // Colors - Backgrounds
    public static final Color COLOR_INLINE_CODE_BG = new Color(0xF0F0F0);
    public static final Color COLOR_CODE_BLOCK_BG = new Color(180, 220, 250); // Light blue
    public static final Color COLOR_QUOTE_BG = new Color(47, 128, 237, 20); // Very light button blue
    public static final Color COLOR_QUOTE_BORDER = new Color(47, 128, 237); // Button blue
    
    // Indentation (in pixels)
    public static final int INDENT_LIST = 20;
    public static final int INDENT_QUOTE = 10;
    public static final int INDENT_CODE_LEFT = 5;
    public static final int INDENT_CODE_RIGHT = 5;
    
    // Line separator for document rendering
    // StyledDocument always uses "\n" internally regardless of platform
    public static final String DOCUMENT_LINE_SEPARATOR = "\n";
    
    // Private constructor to prevent instantiation
    private MarkdownStyle() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }
}
