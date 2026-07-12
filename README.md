# 📦 .LOG-hog

**A lightweight, cross-platform Java application for fast note-taking with built-in protection for sensitive local notes.**

.LOG-hog runs on **Windows, macOS, and Linux** and provides a simple, efficient workflow for creating timestamped log entries with encryption-first storage and practical local hardening.

***

## 🧠 Purpose

.LOG-hog is designed for **fast, uninterrupted note-taking**.

On startup, the editor is immediately focused so you can begin writing right away. Press **Ctrl+S** or click **Save** to store the entry with a timestamp. The input field is then cleared automatically, allowing you to continue writing without interruption.

This workflow is optimized for rapid, continuous logging/writing a diary or just quick notes.

***

## 🗂️ Format & Compatibility

.LOG-hog keeps the familiar `.LOG`-style workflow but now uses **encrypted storage by policy**.

* Primary storage is encrypted (AES-GCM) when initialized
* Entry workflow, timestamps, search, and markdown rendering stay integrated in the app
* Backups preserve encrypted data format

Inspired by [the .LOG functionality in Windows Notepad](https://www.howtogeek.com/258545/how-to-use-notepad-to-create-a-dated-log-or-journal-file/), .LOG-hog extends the workflow with:

* structured entries
* advanced search and filtering
* encryption-first storage
* automatic backups
* markdown rendering

***

## 🔐 Security

.LOG-hog provides **strong, practical protection for locally stored data**, going beyond what most note-taking apps offer by default.

### Highlights

* **AES-256-GCM authenticated encryption**
* **PBKDF2 (600,000 iterations)** for password-based key derivation
* Encrypted-only save path (plaintext mode removed)
* Incremental encrypted journal writes with compaction on lock/threshold
* Tamper-evident backup integrity (HMAC for numbered backups)
* Persistent lockout state with fail-closed behavior on tamper/missing state
* Tamper-evident security event log with sequence and hash-chain anchoring
* Progressive delay on failed password attempts
* No hardcoded keys or credentials
* Sensitive data cleared from memory after use
* Encrypted backups and journal sidecar handling
* Strict owner-only permission enforcement for security-critical artifacts
* Clipboard auto-clear to reduce accidental exposure
* Static analysis (SpotBugs / FindSecBugs) used to detect common vulnerability classes

👉 See `src/encryption.md` for detailed technical information.

### Why this matters

Many lightweight note tools optimize for convenience first and security second.

.LOG-hog is designed to keep fast logging UX while enforcing encrypted storage and adding practical hardening around backup integrity, lockout persistence, and local file handling.

***

## ⚠️ Security Model

.LOG-hog is designed to protect against:

* Unauthorized access to local files or backups
* Offline attacks on encrypted data
* Opportunistic tampering of local security metadata

It does **not protect against**:

* Malware or keyloggers
* System-level compromise
* Memory access during an active session
* Attackers with full access to your user profile and host metadata

Note: local lockout/audit metadata keys are protected with a host-profile-bound envelope for practical hardening. This improves resistance to casual tampering but is not equivalent to a hardware-backed secret store.

👉 Security depends on:

* using a **trusted system**
* choosing a **strong, unique password**

***

## ✨ Features

* Tabbed interface for writing and browsing logs
* Fast entry workflow with automatic timestamps
* Encrypted storage (AES-GCM)
* Secure password handling with retry delays
* Built-in password generator (random + passphrase)
* Clipboard auto-clear for sensitive content
* Manual lock/unlock
* Incremental encrypted journaling for faster secure saves
* Advanced search (case sensitivity, navigation, filtering)
* Markdown rendering in full log view
* Info panel (entries, days logged, file size)
* Automatic and manual backups
* Single-instance enforcement
* Right-click editing (delete, timestamp modification)
* Support for multiple timestamp formats (ISO, US, EU, etc.)
* System tray integration
* GPLv3 licensed

***

## ⚙️ System Requirements

* **Java 17+**
* \~25–50 MB RAM
* \~200 MB free disk space
* **Supported:** Windows, macOS, Linux

***

## 📦 Footprint

* Application JAR: \~230 KB
* No external dependencies
* Single-file distribution

This results in:

* fast startup
* easy portability
* minimal installation complexity

***

## 🔐 Encryption Notes

* Uses **AES-GCM** for authenticated encryption
* Keys derived from your password using PBKDF2
* Password is never written to disk
* Sensitive memory is cleared after use
* Lockout state is persisted with tamper-evident integrity checks
* Security events are persisted in a tamper-evident anchored chain
* Backup copies can be integrity-verified

⚠️ **Important:**  
If you forget your password, encrypted data cannot be recovered.

***

### Password Recommendations

* Use **20+ characters**
* Prefer random or passphrase-based passwords
* Avoid reuse and predictable patterns
* Use a password manager where possible

***

## ⚠️ Performance Note

When encryption is enabled, loading and saving may be slightly slower due to encryption/decryption operations. The app avoids keeping a general decrypted read cache, but active UI state and pending edits may still keep plaintext in memory during use.

***

## 🛠️ Building from Source

### Windows

```bash
cd src
build.bat
```

### Linux / macOS

```bash
cd src
chmod +x build.sh
./build.sh
```

***

### Manual Build

```bash
cd src
javac -d . $(find . -name "*.java" ! -path "*/test/*")
jar cvfm ../build/loghog.jar manifest.txt \
LogHog.class main/*.class gui/*.class filehandling/*.class \
clipboard/*.class notepad/*.class browser/*.class \
encryption/*.class markdown/*.class services/*.class \
utils/*.class resources/
```

***

### Run

```bash
java -jar build/loghog.jar
```

***

## 🧪 Testing

.LOG-hog includes tests using **JUnit 5**.

### Running Tests

* Use `run_tests_simple.bat`
* Tests output PASS/FAIL results in console

### Notes

* Test classes are in `src/test/java/`
* JUnit 5 required on classpath

***

## 📚 Documentation

* ARCHITECTURE.md
* ../javadocs/index.html
* See help file and CHANGELOG for details

***

## 📄 License

Licensed under **GNU GPL v3**.  
See `LICENSE.md` for details.

***

## 🔗 Repository

GitHub:  
<http://github.com/johandersson/.LOG-hog>

***

