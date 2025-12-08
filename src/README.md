# LogHog

A secure, feature-rich Java Swing logging application.

## Features
- Tabbed interface for writing and browsing logs
- Encryption support with AES-GCM (password retry on incorrect entry)
  - Password requirements: at least 16 characters, including at least one uppercase letter and one special character (e.g., !@#$%^&*()_+-=[]{}|;':",./<>?)
  - Secure password visibility toggle (hold to show, release to hide)
- Manual lock/unlock for immediate security
- System tray integration with quick entry
- Markdown rendering in full log view with search
- Backup and restore with encryption preservation
- Performance optimizations (streaming I/O for large non-encrypted files)
- Single-instance enforcement
- Right-click menus for editing and managing entries
- Comprehensive help documentation
- GPL3 licensed

## Encryption Warning
If you enable encryption, the program may load slower, especially in the settings tab when applying changes and in the full log view. This is due to the encryption/decryption process for the log file.

## Installation
Build with `javac *.java` and `jar cvfe loghog.jar LogHog *.class resources/`

## Usage
Run with `java -jar loghog.jar`

For more details, see the help file and CHANGELOG.md.