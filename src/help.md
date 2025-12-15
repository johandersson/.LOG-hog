# Welcome to the Help File for .LOG-hog!

## Purpose
The purpose of .LOG-hog is to enable quick note-taking. Upon opening, the screen focuses directly on the editor window for immediate writing. After composing your note, press Ctrl+S or click Save to clear the text field and save the entry into a dated log. This clearing allows you to write a new log entry right away, facilitating rapid and efficient note-taking.

.LOG-hog is compatible with Notepad's .LOG feature. In Notepad, creating a file that starts with '.LOG' on the first line enables automatic timestamp insertion on each open or save (see [how .LOG works in Notepad](https://www.howtogeek.com/359463/what-is-a-log-file/)). .LOG-hog can read, edit, and manage such log files, offering advanced features like encryption, search, and formatting while preserving the timestamped structure.

## Security Overview
.LOG-hog prioritizes security for personal logging with enterprise-grade encryption and comprehensive anti-brute-force protection. For detailed technical information, see [encryption.md](encryption.md).

**Security Rating: 8.5/10**
- **Cryptography**: 9.5/10 - AES-256-GCM with PBKDF2
- **Password Protection**: 8.5/10 - Progressive delays with randomization
- **Data Protection**: 8/10 - Full file encryption with authentication
- **Network Security**: 10/10 - No network features (air-gapped)

**Key Features:**
- AES-256-GCM authenticated encryption
- PBKDF2 key derivation (100,000 iterations)
- Progressive security delays (3s → 15s → 60s)
- 4-attempt limit with app restart requirement
- Real-time countdown during delays
- Immediate memory clearing

## System Requirements

**Minimal Requirements:**
- Java 17 or higher installed
- 25 MB available RAM (50 MB recommended)
- 200 MB free disk space

**Extremely Small Footprint:**
- **Application**: Only ~145 KB JAR file
- **Settings**: ~1 KB configuration file
- **Log Data**: Variable (typically 100-500 bytes per entry)
- **Total**: Less than 200 KB for complete installation

**No External Dependencies:**
LogHog requires no additional libraries or frameworks beyond the standard Java runtime. It's completely self-contained and portable.

## Key Features
- **Tabbed Interface**: Effortlessly switch between writing new entries and browsing past logs.
- **Quick Entry**: Add notes instantly with automatic timestamps.
- **Single-Instance Enforcement**: Only one instance of the application can run at a time to prevent conflicts.
- **Right-Click Menu in Log Entries**: Right-click on any log entry to access options like copying to clipboard, deleting, or editing the date and time.
- **Encryption (Optional)**: Secure your log file with AES encryption. Enable via Settings tab, set a strong password, and backup your data. The password is required on startup and is never stored on disk.
- **Manual Lock/Unlock**: Instantly lock your encrypted log file for security, clearing all data from memory and disabling all operations. Unlock by clicking the button and re-entering your password.
- **Backup and Restore**: Easily backup your log file with encryption preservation. Backups are filtered to show only LogHog files.
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

## Backup and Restore
- **Creating Backups**: In the Settings tab, click "Backup Log File" to create a copy of your log file. Choose a location and filename (pre-filled with date). Backups preserve the encryption state of your original file.
- **Backup Filtering**: The file chooser shows only existing LogHog backup files for easy management.
- **Restoring**: Manually replace your log.txt with a backup file if needed.

## Encryption
LogHog uses industry-standard AES-256-GCM encryption for maximum security. For complete technical details including cryptographic parameters, implementation specifics, and security analysis, see [encryption.md](encryption.md).

**Quick Facts:**
- **Algorithm**: AES-256-GCM (authenticated encryption)
- **Key Derivation**: PBKDF2 with 100,000 iterations
- **Security Delays**: 3s → 15s → 60s with randomization
- **Attempt Limit**: 4 attempts, then app restart required

**Enabling Encryption**: Access the Settings tab to enable encryption. You'll need to set a password (at least 16 characters, including at least one uppercase letter and one special character from: !@#$%^&*()_+-=[]{}|;':",./<>?). Optionally, backup your unencrypted log file before proceeding.
- **Password Guidance**: To maximize security, use a long passphrase (20+ characters) that's random and unique. Avoid dictionary words, patterns (like "Qwerty123!"), or personal details. Use a password manager to generate and store strong passwords. Remember, even with encryption, a weak password can be cracked—treat it like a key to your safe.
- **Usage**: When encryption is enabled, you'll be prompted for your password each time you start the app. If the password is incorrect, you'll see a clear error message and can retry immediately.
- **Manual Lock/Unlock**: For immediate security, click the "Lock File" button in the Full Log tab to instantly lock your encrypted log. This clears all decrypted data from memory, empties all views, and disables all editing operations. A lock message will appear in all relevant areas. To unlock, click the "Unlock File" button and re-enter your password.
- **Performance**: Encryption adds a small delay to saving and loading, but decrypted content is cached in memory for fast access during your session. **Note**: Enabling encryption may cause the program to load slower, especially in the settings tab when applying changes and in the full log view.

## Editing Log Entries:
- **Edit Date/Time**: Right-click on a log entry and select "Edit Date/Time" to change its timestamp. Enter the new date and time in the format HH:mm yyyy-MM-dd.
- **Delete Entry**: Right-click and select "Delete Entry" to remove a log entry after confirmation.
- **Copy to Clipboard**: Right-click and select "Copy Entry to Clipboard" to copy the timestamp and content to the clipboard.

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

## License
LogHog is licensed under the GNU General Public License version 3 (GPL3). See the license.md file for full license text.

## Changelog
See CHANGELOG.md for a detailed history of changes and new features.

## Github repo:
[GitHub Repository](http://github.com/johandersson/.LOG-hog)