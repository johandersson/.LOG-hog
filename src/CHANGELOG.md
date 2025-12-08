# Changelog

All notable changes to LogHog will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- GPL3 copyright notices and author attribution to all Java files
- CHANGELOG.md for tracking important changes
- Manual file locking/unlocking feature with security UI
- Lock state prevents all file operations and shows lock messages
- Plain text rendering for lock messages to prevent link artifacts
- Backup directory setting to centralize all backup locations
- Improved single instance enforcement that brings existing window to front
- IPC communication between application instances
- Backward compatibility for encrypted files created with old PBKDF2 iterations
- Comprehensive encryption compatibility across all decryption operations

### Changed
- Updated splash screen entries to remove movie references and focus on themes of mom, coding, coca cola, AI, and pizza
- Removed duplicate entries from splash screen log
- Manual lock no longer prompts user, instantly locks and disables UI
- Lock messages displayed in all relevant views when file is locked
- Full log lock message ensures no HTML/markdown processing
- Password visibility toggle now requires holding button (press to show, release to hide)
- Removed "Always show password" setting for better security
- Improved decryption error messages to show user-friendly "Incorrect password" instead of technical errors
- Modernized codebase with 'var' declarations for local variables (Java 17 feature)
- Refactored SettingsPanel.java for better maintainability with extracted methods
- Moved .gitignore to project root for proper repository-wide coverage
- Enhanced .gitignore with additional patterns for logs, backups, and AI files

### Fixed
- LogListPanel now shows lock message and disables editing when locked
- All save/load/delete/edit operations blocked when file is locked
- Lock message in Full Log view no longer appears as clickable links
- Security: No file access or memory reading possible in locked state
- Backup operations now use configured default directory
- Password dialog compilation and method declaration issues
- NullPointerException in SettingsPanel when backupDirField was not initialized
- Encryption compatibility issue when PBKDF2 iterations were increased
- All decryption operations now support both old (65,536) and new (100,000) PBKDF2 iterations
- Repository cleanup: removed debug.log and backup files from version control

### Security
- Removed debug.log from repository and added to .gitignore
- Eliminated all salt logging for security
- Comprehensive operation blocking in locked state
- Password visibility requires active user interaction
- Increased PBKDF2 iterations from 65,536 to 100,000 for enhanced password-based key derivation security
- Maintained backward compatibility for existing encrypted files
- Fixed memory leak: cachedEntries now cleared during manual lock to prevent decrypted content from remaining in memory

## [1.0.0] - 2025-12-04

### Added
- File encryption/decryption with AES-GCM and PBKDF2 key derivation
- Auto-logout feature with configurable timeout
- Settings persistence with backups
- Markdown rendering for full log view
- Search functionality in full log
- Copy full log to clipboard
- Open log in Notepad
- System tray integration
- Single instance enforcement
- Splash screen with animated elements
- Help documentation with Markdown support
- Edit date/time for log entries
- Quick entry from system tray
- Link support in log entries
- Undo/redo in text areas
- Filtering and sorting of log entries

### Changed
- Improved UI with modern design and better UX
- Chronological sorting (oldest first) in full log view
- Better error messages and user feedback
- Modal dialogs for better focus

### Fixed
- Various parsing and rendering issues
- Sorting consistency
- Memory leaks and performance issues
- File handling edge cases
- UI responsiveness and layout issues

### Security
- Secure encryption implementation
- Sensitive data clearing on lock/logout
- No plaintext storage of passwords or keys