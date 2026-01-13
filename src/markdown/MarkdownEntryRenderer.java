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

import java.util.List;

import javax.swing.text.BadLocationException;

/**
 * Handles rendering of individual markdown entries.
 * Extracted from MarkdownRenderer for better modularity.
 */
public class MarkdownEntryRenderer {

    public static void renderEntry(List<String> entry, MarkdownRenderingContext context) throws BadLocationException {
        MarkdownEntryProcessor processor = new MarkdownEntryProcessor(entry, context);
        processor.processEntry();
    }
}