# Welcome to the Help File for .LOG-hog!

## Key Features
- **Tabbed Interface**: Effortlessly switch between writing new entries and browsing past logs.
- **Quick Entry**: Add notes instantly with automatic timestamps.
- **Single-Instance Enforcement**: Only one instance of the application can run at a time to prevent conflicts.
- **Right-Click Menu in Log Entries**: Right-click on any log entry to access options like copying to clipboard, deleting, or editing the date and time.
- **Encryption**: Secure your log file with AES encryption. Enable via File > Settings, set a strong password, and backup your data. The password is required on startup and is never stored on disk.

## Keyboard Shortcuts:
- **Ctrl+S** — Save a new entry
- **Ctrl+R** — Refresh the log list to reflect external changes
- **Ctrl+N** — Quickly add a short note, when starting the app the big text area is focused for quick entry, but CTRL+N you can use anywhere in the file to add a quick note.
- **Ctrl+F** — Focus the search bar to quickly find entries.

## Filter entries in the Log Entries tab.
- **Search Bar**: Find specific entries by keywords.
- **Date Filter**: View entries from a specific date range.

## System tray Integration:
- View 10 most recent logs, click one and it will open the app and focus that entry.
- Add quick log entry directly from the tray menu.

## Encryption
- **Enabling Encryption**: Access File > Settings to enable encryption. You'll need to set a password (at least 16 characters, including at least one uppercase letter and one special character like !@#$%^&*()_+-=[]{}|;':",./<>?) and confirm it. Optionally, backup your unencrypted log file before proceeding.
- **Security Notes**: Your log file is encrypted using AES with a key derived from your password. The password is only kept in memory while the app runs and is never saved to disk. If you forget your password, your data cannot be recovered.
- **Usage**: When encryption is enabled, you'll be prompted for your password each time you start the app. If decryption fails, you can retry or abort loading the logs.
- **Performance**: Encryption adds a small delay to saving and loading, but decrypted content is cached in memory for fast access during your session.

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
- **Code**: `inline code` for inline, or ``` for code blocks
- **Line Breaks**: Use two spaces at the end of a line or a blank line for paragraphs

Formatting is applied in the Full Log window for a polished view of your entries.

## Github repo:
[GitHub Repository](http://github.com/johandersson/.LOG-hog)