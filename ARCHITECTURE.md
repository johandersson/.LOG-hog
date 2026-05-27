# .LOG-hog Architecture Documentation

**Version:** 2.0\
**Last Updated:** April 2026\
**Author:** Johan Andersson

***

## 📖 Quick Start for New Developers

This document describes the internal design of .LOG-hog.

* A **desktop application** for timestamped note-taking
* Written in **Java (JDK 17+)**
* Uses **AES-256-GCM for optional encryption**
* Built using **standard Java libraries (no external dependencies)**

***

### What is “Architecture”?

Software architecture describes:

* The main components of the system
* How they interact
* How responsibilities are separated

***

## 🏠 System Overview

.LOG-hog follows a **layered architecture**:



### Layer Responsibilities

* **UI Layer** – User interface (Swing components)
* **Application Layer** – Coordinates workflows
* **Service Layer** – Implements business logic
* **Data Layer** – File system interaction

***

### Why This Design?

Layering helps:

* isolate responsibilities
* simplify testing
* reduce coupling between components

***

## 🧩 Core Components



### Responsibilities

| Component             | Description                     |
| --------------------- | ------------------------------- |
| **LogHog.java**       | Application entry point         |
| **LogTextEditor**     | Main UI window                  |
| **LogFileHandler**    | File I/O and orchestration      |
| **EncryptionManager** | Encryption and decryption logic |
| **BackupManager**     | Backup creation and rotation    |

***

## 🔐 Encryption Workflow

### Save Flow



***

### Load Flow



***

### Key Concepts

| Term    | Description                               |
| ------- | ----------------------------------------- |
| AES-256 | Symmetric encryption algorithm            |
| GCM     | Provides confidentiality + integrity      |
| PBKDF2  | Password-based key derivation             |
| Salt    | Randomized input to prevent reuse attacks |
| IV      | Unique nonce for encryption operations    |

***

## 💾 Backup System

.LOG-hog uses **rotating backups**:

* `.bak`, `.bak.1`, `.bak.2`, etc.
* Optional timestamped backups

***

### Secure Deletion (Best-effort)

Files are overwritten multiple times before deletion:

1. Random data
2. Fixed pattern
3. Zeros

⚠️ **Note:** This approach is not fully reliable on SSDs due to wear-leveling.

***

## 🚀 Startup Flow



***

## 🔒 Password Handling

* Password required for encrypted logs
* Progressive delay on failed attempts
* Maximum attempt limit before restart

This reduces automated brute-force attempts but does not prevent attacks on extracted data.

***

## 📦 Project Structure

```
src/
├── main/
├── encryption/
├── filehandling/
├── gui/
├── clipboard/
├── utils/
└── resources/
```

### Key Modules

* **encryption/** – cryptographic operations
* **filehandling/** – log parsing and storage
* **gui/** – user interface
* **clipboard/** – clipboard handling
* **utils/** – shared utilities

***

## 🎯 Design Patterns

### 1. Singleton

Used for shared services where only one instance is required.

### 2. Factory

Centralizes creation of service objects.

### 3. Observer

Used for UI updates when data changes.

### 4. Facade

Simplifies interaction with complex subsystems (e.g., `LogFileHandler`).

***

## 🔧 Technology Stack

| Area     | Choice                |
| -------- | --------------------- |
| Language | Java 17               |
| UI       | Swing                 |
| Crypto   | JDK cryptography APIs |
| Build    | javac / jar           |

***

## ⚠️ Security Considerations

.LOG-hog is designed to:

* Protect data at rest
* Prevent unauthorized file access

Limitations:

* Does not protect against malware or keyloggers
* Does not protect memory during active sessions
* Secure deletion is best-effort only
* Clipboard contents may persist after crashes

***

## 📊 Data Flow



***

## 📈 Performance

* Low startup time
* Moderate memory usage (\~tens of MB)
* Performance depends on file size and encryption state

***

## 📚 Glossary

(kept – already good)

***

## 🔗 Documentation

* encryption.md
* help.md
* README.md

***

Just say 👍
