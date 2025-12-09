# LogHog

A secure, feature-rich Java Swing logging application.

## Features
- Tabbed interface for writing and browsing logs
- Encryption support with AES-GCM (password retry on incorrect entry)
  - Password requirements: at least 16 characters, including at least one uppercase letter and one special character (e.g., !@#$%^&*()_+-=[]{}|;':",./<>?)
  - Secure password visibility toggle (hold to show, release to hide)
- Manual lock/unlock for immediate security
- System tray integration with quick entry and recent logs view
- Markdown rendering in full log view with search and formatting support (headers, bold, italic, lists, blockquotes, code blocks, links)
- Backup and restore with encryption preservation (manual and automatic backups)
- Performance optimizations (streaming I/O for large non-encrypted files)
- Single-instance enforcement
- Right-click menus for editing, deleting, copying, and managing entries
- Advanced search and date filtering for log entries
- Keyboard shortcuts (Ctrl+S save, Ctrl+R refresh, Ctrl+N quick note, Ctrl+F search)
- Quick entry with automatic timestamps
- Clickable links to URLs and local files
- Undo/redo functionality in text areas
- Clipboard integration for copying entries
- Open entries in external notepad or browser
- Toast notifications for user feedback
- Automatic timestamping with .LOG feature (compatible with Notepad's journal mode)

## Encryption Warning
If you enable encryption, the program may load slower with really large logs, especially in the settings tab when applying changes and in the full log view. This is due to the encryption/decryption process for the log file.

## Encryption Details
Encryption is an optional feature that protects your log files with strong security using AES encryption. When enabled, your password must be at least 16 characters long and include uppercase letters, lowercase letters, and special characters. This keeps your data safe, but remember that security depends on using a strong, unique password.

## Installation
Build with `javac *.java` and `jar cvfe loghog.jar LogHog *.class resources/`

## Usage
Run with `java -jar loghog.jar`

For more details, see the help file and CHANGELOG.md.