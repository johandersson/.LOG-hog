# Welcome to the Help File for .LOG-hog!

## Purpose
The purpose of .LOG-hog is to enable quick note-taking. Upon opening, the screen focuses directly on the editor window for immediate writing. After composing your note, press Ctrl+S or click Save to clear the text field and save the entry into a dated log. This clearing allows you to write a new log entry right away, facilitating rapid and efficient note-taking.

**Lightweight & Fast:** .LOG-hog is only ~230 KB (smaller than a photo!) with zero external dependencies. It starts instantly and runs efficiently because it's pure Java code with no extra libraries—making it 100x smaller than typical applications while being ultra-secure and portable.

**✨ .LOG-hog works on Windows, macOS, and Linux!** The program is fully platform-independent, providing automatic timestamp management, encryption, search, and formatting features across all operating systems. You can use any text editor on any platform to view and edit your log files.

**Already using .LOG files?** .LOG-hog is designed to work seamlessly with standard .LOG format files. Simply point the program to your log file and start using it right away. You'll get all the powerful enhancements—advanced search, optional encryption, markdown rendering, automated backups, and more—while your file stays a simple text file you can open in Notepad or any text editor.

**About the .LOG Format:** .LOG-hog is inspired by [Windows Notepad's .LOG feature](https://www.howtogeek.com/359463/what-is-a-log-file/), where files starting with '.LOG' automatically insert timestamps. .LOG-hog brings this convenient timestamping concept to all platforms with powerful enhancements—all while keeping your log files as simple text files you can open anywhere.

## Security Overview
.LOG-hog implements **enterprise-grade security** with comprehensive protection against modern threats. The application has undergone extensive security hardening to address all identified vulnerabilities.

**Key Security Features:**
- **AES-256-GCM authenticated encryption** with PBKDF2-600,000 iterations key derivation
- **Progressive brute-force protection** (3s → 15s → 30s) with cryptographically secure randomization and progress feedback
- **4-attempt limit** with application restart requirement and real-time countdown
- **Immediate memory clearing** of all sensitive data (passwords, keys, cached content)
- **Automatic clipboard security** with configurable timeout (1-3600 seconds) and educational warnings
- **Secure Ctrl+C functionality** in all text areas with automatic clearing
- **Automatic secure backups** after encryption/decryption operations with multiple overwrite deletion
- **Path validation and confinement** preventing directory traversal and command injection
- **Thread-safe operations** with proper synchronization
- **Generic error messages** preventing information disclosure
- **Comprehensive input validation** with bounds checking and sanitization
- **File operation restrictions** to user home and working directories only
- **Settings encryption** using deterministic keys for defense in depth

**Security Rating: 9.5/10** — All major vulnerabilities addressed. See [src/encryption.md](src/encryption.md) for details.

.LOG-hog is secure for daily use but not invincible against state-level threats or keyloggers. With a strong, unique password (20+ characters, random), your notes are virtually unbreakable. However, weak passwords or forgotten ones can compromise security—use a password manager.

### Clipboard Security
.LOG-hog includes advanced clipboard security features to protect sensitive log data from being inadvertently exposed through clipboard operations:

- **Automatic Clipboard Clearing**: When copying log entries or full logs to the clipboard, the content is automatically cleared after a configurable timeout (default: 30 seconds). This prevents sensitive information from remaining in the clipboard indefinitely.
- **Secure Content Marking**: Copied content is marked as secure .LOG-hog data, allowing the application to distinguish and manage it appropriately.
- **Manual Clear Option**: Users can manually clear the secure clipboard at any time through the system tray menu.
- **Educational Warnings**: Before copying encrypted or full log content, users receive detailed warnings about clipboard security risks and best practices.
- **Configurable Settings**: Clipboard auto-clear timeout and behavior can be customized in the Settings tab under "Clipboard Security".

**⚠️ Important Security Note**: If .LOG-hog is terminated unexpectedly (system crash, power outage, task manager kill), secure clipboard content may remain accessible. Always use the "Clear Clipboard" option from the system tray after unexpected terminations.

Access clipboard security settings through the system tray icon (right-click the tray icon → "Clipboard Security") or the Settings tab in the main application.

## Key Features
- **Tabbed Interface**: Effortlessly switch between writing new entries and browsing past logs.
- **Info Panel**: View real-time statistics about your log file including total number of entries, days logged, and file size in the bottom panel.
- **Quick Entry**: Add notes instantly with automatic timestamps.
- **Single-Instance Enforcement**: Only one instance of the application can run at a time to prevent conflicts.
- **Right-Click Menu in Log Entries**: Right-click on any log entry to access options like copying to clipboard, deleting, or editing the date and time.
- **Encryption (Optional but Highly Secure)**: Protect your log file with industry-standard AES-256-GCM encryption—completely optional but extremely safe when you need it. Enable via Settings tab, set a strong password, and backup your data. The password is required on startup and is never stored on disk.
- **Manual Lock/Unlock**: Instantly lock your encrypted log file for security, clearing all data from memory and disabling all operations. Unlock by clicking the button and re-entering your password.
- **Backup and Restore**: Easily backup your log file with encryption preservation. Backups are filtered to show only .LOG-hog files.
- **Performance Optimizations**: Efficient memory management for typical log file sizes.
- **Window Close Confirmation**: When closing the application, choose to lock the file or exit completely for added security.

## Keyboard Shortcuts:
- **Ctrl+S** — Save a new entry
- **Ctrl+R** — Refresh the log list to reflect external changes
- **Ctrl+N** — Quickly add a short note, when starting the app the big text area is focused for quick entry, but CTRL+N you can use anywhere in the file to add a quick note.
- **Ctrl+F** — Open advanced search dialog to find entries with options for whole word, case sensitivity, and match navigation.

## Filter entries in the Log Entries tab.
- **Search Bar**: Find specific entries by keywords.
- **Date Filter**: View entries from a specific date range.

## System tray Integration:
- View 10 most recent logs, click one and it will open the app and focus that entry.
- Add quick log entry directly from the tray menu.
- **Clear Clipboard**: Immediately clear any secure .LOG-hog content from the clipboard for security.
- Access clipboard security settings and features through the "Clipboard Security" menu option.

## Backup and Restore
- **Creating Backups**: In the Settings tab, click "Backup Log File" to create a copy of your log file. Choose a location and filename (pre-filled with date). Backups preserve the encryption state of your original file.
- **Automatic Backup**: When enabled in Settings, automatic secure backups are created after encryption or decryption operations. Configure the backup directory and enable/disable this feature in the Settings tab under "Automatic Backup".
- **Backup Security**: All backups use secure deletion (multiple overwrites) when replacing existing files to prevent data recovery.
- **Backup Filtering**: The file chooser shows only existing .LOG-hog backup files for easy management.
- **Restoring**: Manually replace your log.txt with a backup file if needed.

## Encryption
- **Enabling Encryption**: Access the Settings tab to enable encryption. You'll need to set a password (at least 20 characters, including at least one uppercase letter, one special character unless the password scores 'Strong', and must score at least 'Good' strength). Optionally, backup your unencrypted log file before proceeding.
- **Password Visibility**: When prompted for your password at startup, you can choose to always show the password in plain text by checking the "Always show password in plain text" box. This setting is saved and will apply to future password prompts.
- **Security Notes**: Your log file is encrypted using AES with a key derived from your password. The password is only kept in memory while the app runs and is never saved to disk. <span style="color:red">If you forget your password, your data cannot be recovered.</span> In terms of security, AES-GCM is a strong, industry-standard encryption method. As long as you use a strong, unique password and keep it secret, your data is very safe from unauthorized access. .LOG-hog clears passwords from memory immediately after use and adds progressive delays (1-30 seconds) after incorrect password attempts to slow down automated attacks.
- **Password Guidance**: To maximize security, use a long passphrase (20+ characters) that's random and unique. Avoid dictionary words, patterns (like "Qwerty123!"), or personal details. Use a password manager to generate and store strong passwords. Remember, even with encryption, a weak password can be cracked—treat it like a key to your safe. In terms of security, AES-GCM is a strong, industry-standard encryption method. As long as you use a strong, unique password and keep it secret, your data is very safe from unauthorized access.
- **Password Generator**: Access a built-in secure password generator from the Settings tab or password creation dialog. Generate random passwords or memorable passphrases using the EFF Diceware word list. The generator includes real-time strength indication and clipboard integration for easy copying.
- **Usage**: When encryption is enabled, you'll be prompted for your password each time you start the app. If the password is incorrect, you'll see a clear error message and can retry immediately.
- **Manual Lock/Unlock**: For immediate security, click the "Lock File" button in the Full Log tab to instantly lock your encrypted log. This clears all decrypted data from memory, empties all views, and disables all editing operations. A lock message will appear in all relevant areas. To unlock, click the "Unlock File" button and re-enter your password.
- **Performance**: Encryption adds a small delay to saving and loading, but decrypted content is cached in memory for fast access during your session. **Note**: Enabling encryption may cause the program to load slower, especially in the settings tab when applying changes and in the full log view.

## Editing Log Entries:
- **Edit Date/Time**: Right-click on a log entry and select "Edit Date/Time" to change its timestamp. Enter the new date and time in the format HH:mm yyyy-MM-dd.
- **Delete Entry**: Right-click and select "Delete Entry" to remove a log entry after confirmation.
- **Copy to Clipboard**: Right-click and select "Copy Entry to Clipboard" to copy the timestamp and content to the clipboard. For security, copied content is automatically cleared after the configured timeout (see Clipboard Security settings).

## Link to URLs and files:
- Easily create clickable links to websites and local files within your log entries. These are visible in the Full log formatted view of the log file.
- URLs: [Example Site](http://example.com)
- Local files: [My File](file:///C:/path/to/your/file.txt)

## Markdown Formatting
The Full Log tab renders your log entries with Markdown formatting for better readability. Supported features include:

- **Headers**: Use # for headings (e.g., # Header 1, ## Header 2)
- **Bold**: Wrap text with ** (e.g., **bold text**)
- **Italic**: Wrap text with * (e.g., *italic text*)
- **Links**: [Link Text](URL) for clickable links
- **Lists**: Use - for unordered lists
- **Blockquotes**: Use > for quoted text (e.g., > This is a quote) - displays with large quote marks and indentation
- **Code**: `inline code` for inline, or ``` for code blocks
- **Line Breaks**: Use two spaces at the end of a line or a blank line for paragraphs

Formatting is applied in the Full Log window for a polished view of your entries.

If a log file contains more entries than the UI rendering limit, the Full Log view will display the most recent entries only to keep the application responsive. Use the Log Entries tab's date filter to load other periods or years explicitly.

## License
.LOG-hog is licensed under the GNU General Public License version 3 (GPL3). See the ../LICENSE.md file for full license text.

## Changelog
See ../CHANGELOG.md for a detailed history of changes and new features.

## Github repo:
[GitHub Repository](http://github.com/johandersson/.LOG-hog)

## Keyboard Shortcuts:
- **Ctrl+S** — Save a new entry
- **Ctrl+R** — Refresh the log list to reflect external changes
- **Ctrl+N** — Quickly add a short note, when starting the app the big text area is focused for quick entry, but CTRL+N you can use anywhere in the file to add a quick note.
- **Ctrl+F** — Open advanced search dialog to find entries with options for whole word, case sensitivity, and match navigation.

## Filter entries in the Log Entries tab.
- **Search Bar**: Find specific entries by keywords.
- **Date Filter**: View entries from a specific date range.

## System tray Integration:
- View 10 most recent logs, click one and it will open the app and focus that entry.
- Add quick log entry directly from the tray menu.
- **Clear Clipboard**: Immediately clear any secure .LOG-hog content from the clipboard for security.
- Access clipboard security settings and features through the "Clipboard Security" menu option.