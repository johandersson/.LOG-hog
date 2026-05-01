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

package gui;

/**
 * Centralized UI strings and messages for the application.
 * All user-facing text should be defined here for consistency and easy localization.
 */
public class UIStrings {
    
    // Tooltips and hints
    public static final String TOOLTIP_CLICK_EDIT = "Click to edit this entry";
    public static final String TOOLTIP_SHOW_PASSWORD = "Show password";
    public static final String TOOLTIP_HIDE_PASSWORD = "Hide password";
    
    // Button labels
    public static final String BTN_OK = "OK";
    public static final String BTN_CANCEL = "Cancel";
    public static final String BTN_YES = "Yes";
    public static final String BTN_NO = "No";
    public static final String BTN_CREATE_NEW = "Create New";
    public static final String BTN_RESTORE_BACKUP = "Restore from Backup";
    public static final String BTN_EXIT = "Exit";
    public static final String BTN_REFRESH = "Refresh";
    
    // Dialog titles
    public static final String TITLE_ERROR = "Error";
    public static final String TITLE_WARNING = "Warning";
    public static final String TITLE_INFO = "Information";
    public static final String TITLE_CONFIRM = "Confirm";
    public static final String TITLE_SUCCESS = "Success";
    public static final String TITLE_BACKUP_INFO = "Backup Info";
    public static final String TITLE_NOT_ENCRYPTED = "Not Encrypted";
    public static final String TITLE_INVALID_INPUT = "Invalid Input";
    public static final String TITLE_SETTINGS_ERROR = "Settings Error";
    public static final String TITLE_CANNOT_FORMAT = "Cannot Format";
    public static final String TITLE_ENTRY_NOT_FOUND = "Entry Not Found";
    public static final String TITLE_FORMATTING_SUCCESS = "Formatting Complete";
    public static final String TITLE_SECURITY_TIP = "Security Tip";
    
    // Messages - File operations
    public static final String MSG_FILE_NOT_FOUND = "File Not Found";
    public static final String MSG_FILE_NOT_FOUND_DETAILS = "Log file does not exist.";
    public static final String MSG_FILE_LOCKED = "File is Locked";
    public static final String MSG_FILE_LOCKED_DETAILS = "Please unlock the file before formatting.";
    
    // Messages - Entry operations
    public static final String MSG_ENTRY_NOT_FOUND = "Entry Not Found";
    public static final String MSG_ENTRY_NOT_FOUND_DETAILS = 
        "This entry is not visible in the current Log List view.<br>" +
        "You may need to adjust the year/month filter to see it.";
    
    // Messages - Encryption
    public static final String MSG_DECRYPTION_FAILED = "Decryption Failed";
    public static final String MSG_DECRYPTION_FAILED_DETAILS = 
        "Unable to decrypt the file. Please check your password.";
    public static final String MSG_NOT_ENCRYPTED = "The log file is not currently encrypted.";
    public static final String MSG_ENCRYPTION_FAILED = 
        "Encryption failed. Please check your password and try again.";
    
    // Messages - Backup
    public static final String MSG_BACKUP_INFO = 
        "Backups are copies of your current log file.\n" +
        "If encrypted, the backup will remain encrypted for security.\n" +
        "Do you want to proceed?";
    public static final String MSG_BACKUP_FAILED = 
        "Backup failed. Please check file permissions and try again.";
    
    // Messages - Password validation
    public static final String MSG_PASSWORD_MIN_LENGTH = "Password must be at least 20 characters";
    public static final String MSG_PASSWORD_TOO_WEAK = 
        "Password is too weak. Please create a stronger password (aim for 'Good' or 'Strong' in the indicator).";
    public static final String MSG_PASSWORDS_NO_MATCH = "Passwords do not match";
    
    // Messages - Settings validation
    public static final String MSG_INVALID_CLIPBOARD_TIMEOUT = 
        "Clipboard timeout must be a number between 5 and 30 seconds.";
    public static final String MSG_INVALID_BACKUP_DIR = 
        "Backup directory path is invalid or contains unsafe characters.";
    public static final String MSG_CLIPBOARD_TIMEOUT_DEFAULT = 
        "Invalid clipboard timeout value. Using default.";
    public static final String MSG_CLIPBOARD_TIMEOUT_RANGE = 
        "Clipboard timeout must be between 5 and 30 seconds.";
    public static final String MSG_SETTINGS_SAVE_FAILED = 
        "Error saving settings. Please check file permissions and try again.";
    
    // Messages - Document errors
    public static final String MSG_DOC_ACCESS_ERROR = "Error accessing document";
    
    // Messages - Formatting
    public static final String MSG_FORMATTING_SUCCESS = "Log file formatting completed successfully!";
    public static final String MSG_ENTRIES_SORTED = "Entries have been sorted by timestamp.";
    public static final String MSG_BLANK_LINES_NORMALIZED = "Excessive blank lines have been removed.";
    public static final String MSG_FILE_RELOADED = "The display has been refreshed.";
    
    // Status bar messages
    public static final String STATUS_READY = "Ready";
    public static final String STATUS_LOADING = "Loading...";
    public static final String STATUS_SAVING = "Saving...";
    public static final String STATUS_FORMATTING = "Formatting...";
    
    // Private constructor to prevent instantiation
    private UIStrings() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }
}
