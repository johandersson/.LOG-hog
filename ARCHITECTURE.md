# .LOG-hog Architecture Documentation

**Version:** 2.0\
**Last Updated:** April 2026\
**Author:** Johan Andersson

***

## 📖 Quick Start for Developers

* Desktop application for timestamped note-taking
* Written in **Java (JDK 17+)**
* Optional encryption using **AES-256-GCM**
* Uses only standard JDK libraries (no external dependencies)

***

## 🏠 System Overview



### Layer Responsibilities

* UI Layer – Swing interface
* Application Layer – Coordinates logic
* Service Layer – Core functionality
* Data Layer – File system

***

## 🧩 Core Components



### Responsibilities

* LogHog.java – Entry point
* LogTextEditor – Main UI
* LogFileHandler – File operations
* EncryptionManager – Crypto logic
* BackupManager – Backup handling

***

## 🔐 Encryption Workflow

### Save



***

### Load



***

## 💾 Backup System



***

## 🚀 Startup Flow



***

## 🔒 Password Handling

* Progressive delays on failure
* Limited attempts
* Restart required after limit

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

***

## 🎯 Design Patterns

* Singleton
* Factory
* Observer
* Facade

***

## 🔧 Technology Stack

| Area     | Choice      |
| -------- | ----------- |
| Language | Java 17     |
| UI       | Swing       |
| Crypto   | JDK APIs    |
| Build    | javac / jar |

***

## ⚠️ Security Considerations

* Protects data at rest
* Does not protect against malware
* Memory exposed during session
* Secure deletion is best-effort

***

## 📊 Data Flow



***

## 📈 Performance

* Fast startup
* Low memory usage
* Depends on file size and encryption

***

## 📚 Glossary

* AES – encryption algorithm
* GCM – integrity + encryption
* PBKDF2 – key derivation
* IV – initialization vector
* Salt – random input
* Swing – UI framework

***

## 🔗 Related Docs

* encryption.md
* help.md
* README.md

***

*Architecture document v2.0 – April 2026*
