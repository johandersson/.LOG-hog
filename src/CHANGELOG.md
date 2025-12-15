# Changelog

All notable changes to LogHog will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- GPL3 copyright notices and author attribution to all Java files
- CHANGELOG.md for tracking important changes
- **Manual file locking/unlocking feature** with comprehensive security UI and operation blocking
- **Advanced search dialog** with whole word matching, case sensitivity, match counts, and navigation
- **Progressive security delays** (3s → 15s → 60s with randomization) after incorrect password attempts to prevent brute-force attacks
- **Backup directory setting** to centralize all backup locations
- **Improved single instance enforcement** with IPC communication and window focus restoration
- **Backward compatibility** for encrypted files created with old PBKDF2 iterations
- **Blockquote support** in markdown rendering (> for quoted text)
- **Window close confirmation dialog** with options to lock file or exit
- **Comprehensive encryption documentation** (encryption.md) covering AES-256-GCM, PBKDF2, and security measures
- Updated README.md with security overview and link to detailed encryption documentation
- Updated help.md to reference centralized security documentation and remove redundant sections
- Added non-technical introduction to encryption.md for better user accessibility

### Changed
- **Security improvements**: Password visibility toggle now requires holding button (press to show, release to hide); removed "Always show password" setting
- **Manual lock behavior**: No longer prompts user, instantly locks and disables UI with comprehensive operation blocking
- **Lock messages**: Displayed in all relevant views when file is locked, with plain text rendering to prevent link artifacts
- **Error handling**: Improved decryption error messages to show user-friendly "Incorrect password" instead of technical errors
- **Search shortcut**: Ctrl+F now opens advanced search dialog instead of focusing simple search bar
- **Code modernization**: Refactored SettingsPanel.java for better maintainability; modernized codebase with 'var' declarations (Java 17 feature)
- **Repository management**: Moved .gitignore to project root with enhanced patterns for logs, backups, and AI files
- Updated splash screen entries to remove movie references and focus on themes of mom, coding, coca cola, AI, and pizza

### Fixed
- **Security fixes**: All save/load/delete/edit operations blocked when file is locked; no file access or memory reading possible in locked state
- **Password error handling**: Improved to catch decryption failures from both current and legacy key derivation methods, showing user-friendly messages
- **Encryption compatibility**: Fixed issues when PBKDF2 iterations were increased; all decryption operations now support both old (65,536) and new (100,000) PBKDF2 iterations
- **Lock message rendering**: Full log lock message no longer appears as clickable links
- **Backup operations**: Now use configured default directory
- **Settings panel**: Fixed NullPointerException when backupDirField was not initialized
- **Repository cleanup**: Removed debug.log and backup files from version control
- Password dialog compilation and method declaration issues

### Security
- **Enhanced password-based key derivation**: Increased PBKDF2 iterations from 65,536 to 100,000 for stronger protection
- **Memory security**: Immediate password clearing from memory after use to prevent forensic recovery; fixed memory leak where cachedEntries remained in memory during manual lock
- **Comprehensive operation blocking**: All file operations disabled in locked state with proper security UI
- **Password visibility**: Now requires active user interaction (hold to show, release to hide)
- **Repository security**: Removed debug.log from repository and added to .gitignore; eliminated all salt logging
- **Backward compatibility**: Maintained support for existing encrypted files with old PBKDF2 iterations

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