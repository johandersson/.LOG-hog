# .LOG-hog

**A secure, feature-rich Java Swing logging application that works on Windows, macOS, and Linux.**

## Security
**Security Rating: HIGH (9.5/10)** - Enterprise-grade encryption with AES-256-GCM, PBKDF2 key derivation, progressive delays, enforced password strength, automatic secure backups, and comprehensive input validation. Suitable for sensitive personal and professional data.

**🔒 Oracle Secure Coding Guidelines Conformance** - .LOG-hog has been hardened to conform to [Oracle's Secure Coding Guidelines for Java SE](https://www.oracle.com/java/technologies/javase/seccodeguide.html), addressing all CRITICAL, HIGH, and MEDIUM priority security requirements:
- ✅ **Information Disclosure Prevention**: Generic error messages without internal details
- ✅ **Immutable Static Fields**: Prevented runtime state corruption
- ✅ **Defensive Copying**: Protected mutable internal state from external modification
- ✅ **Resource Management**: File size limits (50MB) prevent memory exhaustion attacks
- ✅ **Secure Exception Handling**: All exceptions logged, never swallowed
- ✅ **Fail-Secure Design**: Empty string on encryption failure, never plaintext fallback

See [encryption.md](encryption.md) for comprehensive security documentation.

## Purpose
The purpose of .LOG-hog is to enable quick note-taking. Upon opening, the screen focuses directly on the editor window for immediate writing. After composing your note, press Ctrl+S or click Save to clear the text field and save the entry into a dated log. This clearing allows you to write a new log entry right away, facilitating rapid and efficient note-taking.

**✨ Works seamlessly on Windows, macOS, and Linux!** Use your favorite text editor on any platform to view and edit your log files. .LOG-hog is inspired by [Windows Notepad's .LOG feature](https://www.howtogeek.com/359463/what-is-a-log-file/) but brings this convenient timestamping concept to all platforms with powerful enhancements like encryption, search, and advanced backup systems.

## Features
- Tabbed interface for writing and browsing logs
- Encryption support (optional) with AES-GCM (password retry on incorrect entry)
  - Password requirements: at least 20 characters, including at least one uppercase letter and one special character (unless password scores 'Strong'), and must score at least 'Good' strength
  - Secure password visibility toggle (hold to show, release to hide)
  - **Built-in password generator** for secure random passwords and passphrases
- **Clipboard Security**: Automatic clearing of copied content with configurable timeout and educational warnings
- Manual lock/unlock for immediate security
- System tray integration with quick entry and clipboard security access
- Markdown rendering in full log view with advanced search (whole word, case sensitivity, match navigation)
- **Info panel** displaying real-time statistics (total entries, days logged, file size) in the bottom panel
- **Automatic Backup**: Secure automatic backups after encryption/decryption operations with configurable settings
- Backup and restore with encryption preservation
- Performance optimizations (efficient memory management)
- Single-instance enforcement
- Right-click menus for editing and managing entries
- **Comprehensive timestamp format support**: Loads files with 23+ common date formats from various locales (ISO, US, European, German, etc.) while using HH:mm yyyy-MM-dd as the native format
- Comprehensive help documentation
- GPL3 licensed

## Third-Party Components
- **EFF Diceware Word List**: Used for secure passphrase generation. Released under Creative Commons Zero (CC0) license (public domain).

## System Requirements & Footprint

**Minimal System Requirements:**
- Java 17 or higher
- 25 MB RAM (recommended 50 MB)
- 200 MB free disk space
- **Supports:** Windows, macOS, and Linux

**Tiny Disk Footprint:**
- **Application JAR**: Only ~230 KB - smaller than a single photo!
- **Settings file**: ~1 KB (varies with configuration)
- **Log files**: Variable (typically 100-500 bytes per entry)
- **Total installation**: < 250 KB

**Why So Small?**
.LOG-hog is 100x smaller than typical Java applications because it has **zero external dependencies**—no libraries, no frameworks, just pure Java code. Everything you need is built into the Java runtime, making the application:
- ✅ **Lightning fast** to download and start
- ✅ **Highly portable** - runs anywhere Java runs
- ✅ **Ultra secure** - no third-party code to audit or update
- ✅ **Self-contained** - single JAR file, nothing else needed

## Security Overview
.LOG-hog implements **enterprise-grade security** with comprehensive protection against modern threats. The application has been hardened according to [Oracle's Secure Coding Guidelines for Java SE](https://www.oracle.com/java/technologies/javase/seccodeguide.html), ensuring robust defense against information disclosure, state corruption, resource exhaustion, and cryptographic vulnerabilities.

**Key Security Features:**
- **AES-256-GCM authenticated encryption** with PBKDF2-100,000 iterations key derivation
- **Progressive brute-force protection** (3s → 15s → 30s) with cryptographically secure randomization
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

## Encryption Warning
If you enable encryption, the program may load slower, especially in the settings tab when applying changes and in the full log view. This is due to the encryption/decryption process for the log file.

In terms of security, .LOG-hog uses AES-GCM encryption, which is a strong, industry-standard method. As long as you use a strong, unique password and keep it secret, your data is very safe from unauthorized access. However, if you forget your password, there's no way to recover your data, so choose a memorable password.

**Password Security Tips:** Use a long, random password (at least 20 characters recommended) generated by a password manager. Avoid common words, patterns, or personal info. .LOG-hog clears passwords from memory immediately after use and adds delays after incorrect attempts to prevent brute-force attacks.

## Building from Source

### Windows
```bash
cd src
build.bat
```

### Linux/macOS
```bash
cd src
chmod +x build.sh
./build.sh
```

The build script automatically:
- Syncs help.md to resources folder
- Stops any running loghog instances
- Compiles all Java files (excluding tests)
- Creates the JAR file with all required resources

### Manual Build
If you prefer to build manually:
```bash
cd src
javac -d . $(find . -name "*.java" ! -path "*/test/*")
jar cvfm ../build/loghog.jar manifest.txt LogHog.class main/*.class gui/*.class filehandling/*.class clipboard/*.class notepad/*.class browser/*.class encryption/*.class markdown/*.class services/*.class utils/*.class resources/
```

### Running
```bash
java -jar build/loghog.jar
```

## Testing
.LOG-hog includes comprehensive pure Java testing without external dependencies.

### Running Tests
- **All tests**: Run `run_tests_simple.bat` for quick test execution
- Tests are self-contained Java classes with main methods

### Test Structure
- Tests are located in `src/test/java/`
- Package-based organization (e.g., `filehandling.FileHandlingTest`)
- Each test class contains a main method that runs all test cases
- Test results printed to console with PASS/FAIL indicators

### Test Dependencies
- No external dependencies required
- Tests run with standard `java` command
- All testing logic implemented in pure Java

### Adding New Tests
1. Create test classes in `src/test/java/` with package declarations
2. Implement test methods and a main method that calls them
3. Use System.out.println for test results
4. Compile and run with standard Java commands

## Documentation
- [Architecture Documentation](ARCHITECTURE.md) - Comprehensive technical documentation with system diagrams, workflows, and design patterns
- [API Documentation (Javadocs)](../javadocs/index.html)

## Usage
Run with `java -jar loghog.jar`

For more details, see the help file and CHANGELOG.md.