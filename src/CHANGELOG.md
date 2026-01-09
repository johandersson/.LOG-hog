# Changelog

All notable changes to .LOG-hog will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added (January 2026)
- **Cross-platform support** for Windows, macOS, and Linux with platform-specific native look and feel
- **Desktop API integration** for opening log files in external editors across all platforms
- **Platform-specific editor fallbacks** (Windows: notepad.exe, macOS: open -e, Linux: xdg-open/gedit/nano)
- **Encryption warning dialog** when opening encrypted files in external text editors to prevent user confusion
- **Platform-aware button labels** ("Open in Notepad" on Windows, "Open in Text Editor" on other platforms)
- **Progress dialog enhancements** with unified UI design across security delay and loading dialogs
- **Loading progress feedback** showing percentage complete and estimated time remaining during file decryption
- **Auto-lock feature** with configurable timeout (15-1440 minutes) to automatically lock encrypted files after inactivity
- **ProgressDialogBase class** providing shared architecture for consistent dialog styling
- **"All Months" filter option** in log list view to display all entries for the selected year instead of a specific month

### Changed (January 2026)
- **Cross-platform documentation** in help files and README emphasizing compatibility with Windows, macOS, and Linux
- **Help file messaging** updated to highlight seamless integration with existing .LOG format files
- **Encryption description enhanced** to emphasize optional but highly secure AES-256-GCM implementation
- **Path validation security** improved to allow platform-specific path separators while preventing directory traversal attacks
- **Progress dialogs refactored** to use shared base class, eliminating code duplication
- **Password strength indicator improved** with enhanced scoring algorithm, pattern detection (repetitive characters, sequences, repeated patterns), and better passphrase support
- **Passphrase scoring adjusted** to align with industry standards (4-word passphrases now rate as Strong)
- **Documentation updated** to reflect current security delays (3s → 15s → 30s) and progress dialog features
- **Splash screen entries shortened** for better display in About tab

### Fixed (January 2026)
- **Markdown links not rendering in Full Log view** - links were displayed as plain text instead of clickable hyperlinks due to incorrect content type configuration
- **Path validation bug** that blocked all Windows paths due to backslash character being incorrectly flagged as forbidden
- **Build compilation errors** fixed (getLogFile() → getFilePath(), showBackupRestoreDialog visibility)
- **Critical encryption bug** where setEncryption was corrupting encrypted files
- **Password zeroing bug** that caused decryption to fail by clearing password before use
- **MalformedInputException** in decryption process
- **Password retry mechanism** now properly displays security delays
- **Timestamp handling** to strip suffixes and Unix timestamp prefixes for full compatibility

### Performance (January 2026)
- **Markdown rendering optimized** with 30-70% performance improvement through early exit for plain text lines, pre-sized collections, and pattern compilation caching
- **Reduced memory allocations** in markdown parser with capacity hints for ArrayList and HashMap instances
- **Inline heading detection** now uses single compiled pattern instead of creating three Pattern objects per line
- **Date filter optimization** with intelligent caching - file parsing now O(N) once, then O(M) for subsequent filter changes instead of O(N) every time
- **Pre-parsed timestamp cache** eliminates redundant timestamp parsing and sorting on every filter selection
- **Batch delete API** - deleting 10 entries now 1 file operation instead of 10, reducing large batch deletes from 20-50s to 2-5s
- **Write-back cache for updates** - entry saves staged in memory, flushed on explicit save/view switch, enabling rapid consecutive edits
- **Markdown early-exit enhanced** - plain text lines skip all regex processing entirely, reducing overhead by 60-80% for non-markdown content
- **Lazy loading for Full Log** - limits rendering to 5,000 most recent entries, reducing 100K entry load from 10-30s to <2s

## [Previous Releases]

### Added
- GPL3 copyright notices and author attribution to all Java files
- CHANGELOG.md for tracking important changes
- **Manual file locking/unlocking feature** with comprehensive security UI and operation blocking
- **Advanced search dialog** with whole word matching, case sensitivity, match counts, and navigation
- **Progressive security delays** (3s → 15s → 30s with ±20% randomization) after incorrect password attempts to prevent brute-force attacks
- **Backup directory setting** to centralize all backup locations
- **Improved single instance enforcement** with IPC communication and window focus restoration
- **Backward compatibility** for encrypted files created with old PBKDF2 iterations
- **Blockquote support** in markdown rendering (> for quoted text)
- **Window close confirmation dialog** with options to lock file or exit
- **Password strength indicator** with real-time feedback during password creation
- **Enforced minimum password strength** (at least 'Good' score) for new encryptions to prevent weak passwords
- **Built-in password generator** with random password and passphrase generation using EFF Diceware word list, accessible from Settings and password creation dialogs
- **Comprehensive encryption documentation** (encryption.md) covering AES-256-GCM, PBKDF2, and security measures
- **Clipboard Security Features**: Automatic clipboard clearing with configurable timeout, secure content marking, educational warnings, system tray access, and secure Ctrl+C functionality in all text areas
- **DateHandler utility class** for centralized timestamp parsing and formatting
- **EncryptionHandler class** for managing password authentication and encryption setup
- **Javadocs generation** with comprehensive API documentation
- **Expanded timestamp format support** across all platforms (supports 23+ common formats including ISO, US, European, German, and 12-hour variants)
- **Cross-platform compatibility** - fully functional on Windows, macOS, and Linux with native look and feel
- **BackupManager component** for centralized backup handling with secure deletion
- **Automatic backup functionality** triggered after encryption/decryption operations
- **Secure random password generation** for tests (removed hardcoded passwords)
- **Error message sanitization** to prevent information disclosure in production
- **Input validation** for settings (reminder length, backup directory safety)
- **Secure file deletion** with multiple overwrites for backup security
- **Settings encryption** using deterministic keys for defense in depth
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
- **Password authentication**: Increased maximum attempts from 3 to 4 with updated progressive delays (3s → 15s → 30s)
- **Password requirements**: Relaxed special character requirement for strong passwords (70+ score) to allow secure passphrases while maintaining security for weaker passwords
- **Security architecture**: Implemented separate encryption for settings (deterministic keys) vs log files (user passwords + salt)
- Updated splash screen entries to remove movie references and focus on themes of mom, coding, coca cola, AI, and pizza

### Fixed
- **Security fixes**: All save/load/delete/edit operations blocked when file is locked; no file access or memory reading possible in locked state
- **Password error handling**: Improved to catch decryption failures from both current and legacy key derivation methods, showing user-friendly messages
- **Encryption compatibility**: Fixed issues when PBKDF2 iterations were increased; all decryption operations now support both old (65,536) and new (100,000) PBKDF2 iterations
- **Lock message rendering**: Full log lock message no longer appears as clickable links
- **Error message sanitization**: Removed detailed exception information from user-facing error dialogs
- **Input validation**: Added comprehensive validation for settings inputs (reminder length, backup directory safety)
- **Debug output removal**: Eliminated System.out.println statements from production code
- **Secure file operations**: Implemented multiple overwrite deletion for backup security
- **Backup operations**: Now use configured default directory
- **Settings panel**: Fixed NullPointerException when backupDirField was not initialized
- **Repository cleanup**: Removed debug.log and backup files from version control
- **SecurityDelayDialog**: Now properly displays for legacy decryption failures
- Password dialog compilation and method declaration issues

### Security
- **Enhanced password-based key derivation**: Increased PBKDF2 iterations from 65,536 to 100,000 for stronger protection
- **Memory security**: Immediate password clearing from memory after use to prevent forensic recovery; fixed memory leak where cachedEntries remained in memory during manual lock
- **Comprehensive operation blocking**: All file operations disabled in locked state with proper security UI
- **Password visibility**: Now requires active user interaction (hold to show, release to hide)
- **Clipboard Security**: Automatic clearing of sensitive content with configurable timeout, secure content marking, educational warnings, and secure Ctrl+C overrides to prevent data exposure
- **Repository security**: Removed debug.log from repository and added to .gitignore; eliminated all salt logging
- **Backward compatibility**: Maintained support for existing encrypted files with old PBKDF2 iterations
- **Critical Security Hardening**: Implemented comprehensive security fixes for all high and medium severity vulnerabilities:
  - **Debug Logging Security**: Completely removed debug logging that exposed cryptographic salts and sensitive operation details
  - **Command Injection Protection**: Added path validation in external process execution to prevent command injection attacks
  - **Input Validation Enhancement**: Implemented bounds checking and comprehensive validation for all numeric settings (clipboard timeout: 1-3600 seconds)
  - **Cryptographic Randomness**: Replaced weak Math.random() and Random with SecureRandom for all security-sensitive operations
  - **Information Disclosure Prevention**: Replaced detailed error messages with generic user-friendly messages to prevent internal implementation details exposure
  - **Thread Safety**: Added proper synchronization for shared state variables to prevent race conditions in multi-threaded operations
  - **File Path Security**: Implemented path validation to constrain file operations within user home and current working directories, preventing directory traversal attacks

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