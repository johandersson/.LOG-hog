# LogHog

A secure, feature-rich Java Swing logging application.

## Features
- Tabbed interface for writing and browsing logs
- Encryption support with AES (password retry on incorrect entry)
- System tray integration
- Markdown rendering in full log view
- Backup and restore with encryption preservation
- Auto-clear for security (inactivity timeout for encrypted logs)
- Performance optimizations (streaming I/O for large non-encrypted files)
- Single-instance enforcement
- Right-click menus for editing and managing entries

## Encryption Warning
If you enable encryption, the program may load slower, especially in the settings tab when applying changes and in the full log view. This is due to the encryption/decryption process for the log file. For security, encrypted logs can auto-clear after inactivity.

## Installation
Build with `javac *.java` and `jar cvfe loghog.jar LogHog *.class resources/`

## Usage
Run with `java -jar loghog.jar`

For more details, see the help file.