# .LOG-hog Architecture Documentation

**Version:** 2.0  
**Last Updated:** April 2026  
**Author:** Johan Andersson

---

## 📖 Quick Start for New Developers

Welcome! This document explains how .LOG-hog is built. If you're new to coding, here's what you need to know:

- **.LOG-hog** is a **desktop app** for writing secure, timestamped notes
- It's written in **Java** (a popular programming language)
- It uses **AES-256 encryption** (the same security used by governments and banks)
- It has **zero external dependencies** - everything is built with standard Java

### What is "Architecture"?

Think of architecture like a building blueprint. It shows:
- **What parts exist** (like rooms in a house)
- **How they connect** (like hallways between rooms)
- **What each part does** (like "this is the kitchen")

---

## 🏠 The Big Picture

.LOG-hog is organized in **layers**, like a cake. Each layer has a specific job:

```mermaid
graph TB
    subgraph "🖥️ What You See - UI Layer"
        UI[GUI - Buttons, Text Areas, Menus]
    end
    
    subgraph "🧠 The Brain - Application Layer"
        APP[Coordinates everything]
    end
    
    subgraph "⚙️ The Workers - Service Layer"
        SVC[Does the actual work]
    end
    
    subgraph "💾 The Storage - Data Layer"
        DATA[Files on your computer]
    end
    
    UI --> APP
    APP --> SVC
    SVC --> DATA
```

**Why layers?** Each layer only talks to the layer below it. This makes the code:
- ✅ Easier to understand (one thing at a time)
- ✅ Easier to fix (problems are isolated)
- ✅ Easier to test (test each layer separately)

---

## 🧩 Main Components

Here are the most important parts of .LOG-hog and what they do:

```mermaid
graph LR
    subgraph "Entry Point"
        LH[LogHog.java<br/>Starts the app]
    end
    
    subgraph "Main Window"
        LTE[LogTextEditor.java<br/>The main window]
    end
    
    subgraph "Core Logic"
        LFH[LogFileHandler.java<br/>Manages your log file]
        EM[EncryptionManager.java<br/>Encrypts/decrypts]
        BM[BackupManager.java<br/>Creates backups]
    end
    
    subgraph "UI Tabs"
        EP[EntryPanel<br/>Write entries]
        LLP[LogListPanel<br/>Browse entries]
        FLP[FullLogPanel<br/>View full log]
        SP[SettingsPanel<br/>Settings]
    end
    
    LH --> LTE
    LTE --> EP
    LTE --> LLP
    LTE --> FLP
    LTE --> SP
    LTE --> LFH
    LFH --> EM
    LFH --> BM
```

### What Each Part Does (Simple Explanation)

| Component | What it does | Real-world analogy |
|-----------|-------------|-------------------|
| **LogHog.java** | Starts the application | The "ON" button |
| **LogTextEditor.java** | The main window with tabs | A tabbed notebook |
| **LogFileHandler.java** | Reads/writes your log file | A librarian who finds and stores books |
| **EncryptionManager.java** | Scrambles data so only you can read it | A safe with a combination lock |
| **BackupManager.java** | Creates safety copies | A photocopy machine |
| **EntryPanel** | Where you type new entries | A blank page in your diary |
| **LogListPanel** | Shows all your entries in a list | The table of contents |
| **FullLogPanel** | Shows your entire log | Reading the whole book |
| **SettingsPanel** | Change how the app works | Control panel |

---

## 🔐 How Encryption Works

Encryption is how we keep your data secret. Here's the process:

```mermaid
sequenceDiagram
    participant You
    participant App as LOG-hog
    participant Crypto as Encryption Engine
    participant File as Your Log File
    
    Note over You,File: SAVING AN ENTRY
    You->>App: Type "Dear diary..."
    You->>App: Click Save
    App->>Crypto: Please encrypt this
    Crypto->>Crypto: 1. Generate random salt
    Crypto->>Crypto: 2. Derive key from password 600k rounds
    Crypto->>Crypto: 3. Encrypt with AES-256-GCM
    Crypto->>App: Here is the encrypted data
    App->>File: Save encrypted data
    
    Note over You,File: READING AN ENTRY
    You->>App: Open log file
    App->>You: Enter password?
    You->>App: MySecretPassword123
    App->>File: Read encrypted data
    App->>Crypto: Please decrypt this
    Crypto->>Crypto: 1. Derive key from password
    Crypto->>Crypto: 2. Decrypt with AES-256-GCM
    Crypto->>Crypto: 3. Verify data was not tampered
    Crypto->>App: Here is your original text
    App->>You: Display Dear diary...
```

### Encryption Terms Explained

| Term | What it means | Why it matters |
|------|--------------|----------------|
| **AES-256** | Advanced Encryption Standard with 256-bit key | Military-grade security, NSA approved |
| **GCM** | Galois/Counter Mode | Detects if someone tampered with your data |
| **PBKDF2** | Password-Based Key Derivation Function 2 | Makes password cracking very slow |
| **600,000 iterations** | How many times we process your password | Takes centuries to brute-force |
| **Salt** | Random data added to your password | Same password = different encryption each time |
| **IV** | Initialization Vector | Random starting point for encryption |

---

## 💾 How Backups Work

.LOG-hog automatically protects against data loss with a 6-layer backup system:

```mermaid
flowchart TD
    subgraph "6 Backup Layers"
        B0[.bak - Last save]
        B1[.bak.1 - Previous save]
        B2[.bak.2 - 2 saves ago]
        B3[.bak.3 - 3 saves ago]
        B4[.bak.4 - 4 saves ago]
        B5[.bak.5 - 5 saves ago]
        TS[Timestamped backups]
    end
    
    SAVE[You save an entry] --> ROTATE
    ROTATE[Backups rotate] --> B0
    B0 --> B1
    B1 --> B2
    B2 --> B3
    B3 --> B4
    B4 --> B5
    B5 --> DELETE[Secure Delete<br/>3-pass wipe]
```

### Secure Deletion (3-Pass Wipe)

When old backups are deleted, we **securely erase** them:

```mermaid
flowchart LR
    subgraph "3-Pass Secure Deletion"
        P1["Pass 1<br/>Random Data"]
        P2["Pass 2<br/>Pattern 0x55"]
        P3["Pass 3<br/>All Zeros"]
        DEL["Delete File"]
    end
    
    FILE[Old Backup] --> P1 --> P2 --> P3 --> DEL
```

| Pass | What's Written | Why |
|------|---------------|-----|
| 1 | Random bytes (SecureRandom) | Obscures the original data |
| 2 | 0x55 (01010101 binary) | Breaks up patterns |
| 3 | 0x00 (zeros) | Final clean wipe |

> ⚠️ **SSD Note:** On solid-state drives, this isn't 100% effective due to wear-leveling. Use full-disk encryption (BitLocker/FileVault/LUKS) for maximum SSD security.

---

## 🚀 Application Startup Flow

What happens when you launch .LOG-hog:

```mermaid
flowchart TD
    START[Launch LOG-hog] --> PLATFORM
    PLATFORM[Detect OS<br/>Windows/Mac/Linux] --> LAF
    LAF[Set native look and feel] --> SINGLE
    
    SINGLE{Another instance<br/>running?}
    SINGLE -->|Yes| FOCUS[Focus existing window]
    FOCUS --> EXIT[Exit new instance]
    SINGLE -->|No| INIT
    
    INIT[Initialize security] --> CHECK
    CHECK{Log file<br/>encrypted?}
    CHECK -->|Yes| PWD[Ask for password]
    CHECK -->|No| LOAD
    
    PWD --> VERIFY{Password<br/>correct?}
    VERIFY -->|No| DELAY[Security delay<br/>3s then 15s then 30s]
    DELAY --> PWD
    VERIFY -->|Yes| DECRYPT[Decrypt file]
    DECRYPT --> LOAD
    
    LOAD[Load entries] --> SHOW[Show main window]
    SHOW --> READY[Ready to use]
```

---

## 🔒 Password Security System

.LOG-hog protects against password guessing with progressive delays:

```mermaid
flowchart TD
    TRY1[Attempt 1] --> WRONG1{Correct?}
    WRONG1 -->|No| WAIT1[Wait 3 seconds]
    WAIT1 --> TRY2[Attempt 2]
    
    TRY2 --> WRONG2{Correct?}
    WRONG2 -->|No| WAIT2[Wait 15 seconds]
    WAIT2 --> TRY3[Attempt 3]
    
    TRY3 --> WRONG3{Correct?}
    WRONG3 -->|No| WAIT3[Wait 30 seconds]
    WAIT3 --> TRY4[Attempt 4]
    
    TRY4 --> WRONG4{Correct?}
    WRONG4 -->|No| LOCKED[App restart required]
    
    WRONG1 -->|Yes| SUCCESS[Access granted]
    WRONG2 -->|Yes| SUCCESS
    WRONG3 -->|Yes| SUCCESS
    WRONG4 -->|Yes| SUCCESS
```

**Why progressive delays?**
- Attempt 1: 3 seconds → Allows fixing typos
- Attempt 2: 15 seconds → Slows down guessing
- Attempt 3: 30 seconds → Automated attacks become impractical
- Attempt 4+: App restart required → Completely stops scripts

---

## 📦 Package Structure

Here's how the code is organized:

```
src/
├── LogHog.java                 ← App entry point
│
├── main/                       ← Core application
│   ├── LogTextEditor.java      ← Main window
│   ├── BackupManager.java      ← Backup system
│   ├── SecureDeletionUtils.java← 3-pass file wipe
│   ├── SingleInstanceManager.java ← Prevent duplicate instances
│   └── ...
│
├── encryption/                 ← Security
│   ├── EncryptionManager.java  ← AES-256-GCM
│   ├── Encryptor.java          ← Interface
│   └── FileEncryptionManager.java
│
├── filehandling/               ← File operations
│   ├── LogFileHandler.java     ← Read/write logs
│   ├── EntryLoader.java        ← Parse entries
│   └── LogParser.java          ← Timestamp parsing (23+ formats)
│
├── gui/                        ← User interface
│   ├── EntryPanel.java         ← Write entries tab
│   ├── LogListPanel.java       ← Entry list tab
│   ├── FullLogPanel.java       ← Full log view tab
│   ├── SettingsPanel.java      ← Settings tab
│   ├── PasswordDialog.java     ← Password prompts
│   └── ...
│
├── clipboard/                  ← Clipboard security
│   ├── ClipboardHandler.java
│   └── SecureClipboardManager.java ← Auto-clear clipboard
│
├── security/                   ← Security utilities
│   └── SecureTempFiles.java    ← Secure temp file creation
│
├── utils/                      ← Helpers
│   ├── DateHandler.java        ← Date/time formatting
│   ├── CryptoUtils.java        ← Memory zeroization
│   └── ...
│
└── resources/                  ← Static files
    ├── help.md
    └── dict.txt                ← EFF Diceware wordlist
```

---

## 🎯 Design Patterns Used

Design patterns are proven solutions to common programming problems:

### 1. Singleton Pattern
**Problem:** We need exactly ONE instance of something  
**Solution:** Create it once, reuse everywhere

```mermaid
flowchart LR
    A[Code A] --> S[Single Instance]
    B[Code B] --> S
    C[Code C] --> S
```

**Used in:** `ServiceFactory`, `SingleInstanceManager`

### 2. Factory Pattern  
**Problem:** Creating objects is complex  
**Solution:** Have a "factory" create them for you

```mermaid
flowchart LR
    REQ[Request a service] --> FAC[ServiceFactory]
    FAC --> FS[FileService]
    FAC --> ES[EncryptionService]
    FAC --> LS[LogEntryService]
```

### 3. Observer Pattern
**Problem:** One thing changes, others need to know  
**Solution:** "Subscribe" to updates

```mermaid
flowchart LR
    DATA[Data changes] --> NOTIFY[Notify observers]
    NOTIFY --> UI1[Update Tab 1]
    NOTIFY --> UI2[Update Tab 2]
    NOTIFY --> UI3[Update List]
```

### 4. Facade Pattern
**Problem:** Complex system with many parts  
**Solution:** Simple interface hides complexity

```mermaid
flowchart TD
    USER[Your Code] --> FACADE[LogFileHandler<br/>Simple Interface]
    FACADE --> A[BackupManager]
    FACADE --> B[EncryptionManager]
    FACADE --> C[EntryLoader]
    FACADE --> D[FileSystem]
```

---

## 🔧 Technology Stack

| Component | Technology | Why we chose it |
|-----------|------------|-----------------|
| Language | Java 17 | Cross-platform, secure, mature |
| UI | Swing | Built into Java, no downloads needed |
| Encryption | JDK Crypto | Audited, government-approved |
| Build | javac/jar | Simple, no build tools required |
| Dependencies | **ZERO** | Smaller, more secure, maintainable |

### Zero-Dependency Benefits

| App Type | Typical Size | Notes |
|----------|-------------|-------|
| **.LOG-hog** | **230 KB** | Pure JDK, zero dependencies |
| JavaFX App | 5-20 MB | JavaFX + libraries |
| Electron App | 100-200 MB | Bundles Chrome browser |

**Why zero dependencies matter:**
- ✅ No supply chain attacks (malicious libraries)
- ✅ No version conflicts
- ✅ No dependency updates to track
- ✅ Sub-second startup time
- ✅ ~25 MB memory usage (vs 100-500 MB for Electron)

---

## 🛡️ Security Architecture

```mermaid
flowchart TB
    subgraph "Encryption Layer"
        AES[AES-256-GCM]
        PBKDF2[PBKDF2-SHA256<br/>600,000 iterations]
        RNG[SecureRandom]
    end
    
    subgraph "Protection Layer"
        PATH[Path Validation]
        INPUT[Input Sanitization]
        MEM[Memory Clearing]
        DELAY[Progressive Delays]
    end
    
    subgraph "Storage Layer"
        WIPE[3-Pass Secure Delete]
        BACKUP[Encrypted Backups]
        PERM[Owner-Only Permissions]
    end
    
    USER[Your Data] --> AES
    AES --> PBKDF2
    PBKDF2 --> RNG
    
    USER --> PATH
    PATH --> INPUT
    INPUT --> MEM
    
    AES --> WIPE
    AES --> BACKUP
    BACKUP --> PERM
```

### Security Checklist

| Feature | Status | Description |
|---------|--------|-------------|
| Encryption | ✅ | AES-256-GCM (military grade) |
| Key Derivation | ✅ | PBKDF2 with 600,000 iterations |
| Memory Safety | ✅ | Passwords zeroed immediately after use |
| Secure Deletion | ✅ | 3-pass wipe (random, 0x55, zeros) |
| Path Traversal | ✅ | Blocked (no `../` attacks) |
| Input Validation | ✅ | All inputs sanitized |
| Brute Force | ✅ | Progressive delays + lockout |
| Single Instance | ✅ | FileLock prevents conflicts |
| File Permissions | ✅ | Owner-only on encrypted files |
| No Serialization | ✅ | Eliminates deserialization attacks |

---

## 📊 Data Flow

How data moves through the application:

```mermaid
flowchart TD
    subgraph "Input"
        TYPE[You type an entry]
        LOAD[You open a file]
    end
    
    subgraph "Processing"
        PARSE[Parse timestamp]
        ENCRYPT[Encrypt]
        DECRYPT[Decrypt]
        CACHE[Cache in memory]
    end
    
    subgraph "Storage"
        FILE[Log file .LOG]
        BACKUP[Backup files .bak]
        SETTINGS[Settings file]
    end
    
    TYPE --> PARSE
    PARSE --> ENCRYPT
    ENCRYPT --> FILE
    FILE --> BACKUP
    
    LOAD --> FILE
    FILE --> DECRYPT
    DECRYPT --> CACHE
    CACHE --> DISPLAY[Display to you]
```

---

## 📈 Performance

| Metric | Value | Notes |
|--------|-------|-------|
| Startup Time | < 1 second | Cold start |
| Memory Usage | ~25 MB | Typical usage |
| File Size Limit | 50 MB | DoS protection |
| Entry Limit | 100,000 | DoS protection |
| JAR Size | 230 KB | Zero dependencies |
| Encryption Time | ~1 second | For key derivation |

---

## 🎓 Oracle Secure Coding Compliance

.LOG-hog follows [Oracle's Secure Coding Guidelines for Java SE](https://www.oracle.com/java/technologies/javase/seccodeguide.html):

| Guideline | Implementation |
|-----------|---------------|
| Purge sensitive info from exceptions | Generic error messages shown to users |
| Resource exhaustion prevention | 50MB file limit, 100k entry limit |
| Make static fields final | Immutable configuration |
| Defensive copies of mutable objects | Salt/key arrays cloned before return |
| Use SecureRandom | All randomness is cryptographic |
| No Java serialization | Eliminates entire attack class |

---

## 🤝 Contributing

### Code Style
- Use clear, descriptive variable names
- Comment complex logic
- Follow existing patterns
- Write tests for new features
- Never log passwords or keys

### Security Rules
- Always clear sensitive data from memory
- Validate all user inputs
- Use SecureRandom (never java.util.Random for security)
- Defensive copies for mutable returns

---

## 📚 Glossary

| Term | Definition |
|------|------------|
| **AES** | Advanced Encryption Standard - the encryption algorithm |
| **GCM** | Galois/Counter Mode - provides integrity verification |
| **PBKDF2** | Password-Based Key Derivation Function 2 |
| **IV** | Initialization Vector - random starting point |
| **Salt** | Random data added to password before hashing |
| **Swing** | Java's built-in GUI framework |
| **JDK** | Java Development Kit |
| **DoS** | Denial of Service - attack that overwhelms resources |
| **Singleton** | Design pattern ensuring only one instance |
| **Facade** | Design pattern that hides complexity |
| **FileLock** | Java mechanism for exclusive file access |

---

## 🔗 Related Documentation

- [encryption.md](src/encryption.md) - Detailed security documentation
- [help.md](src/help.md) - User guide
- [CHANGELOG.md](CHANGELOG.md) - Version history
- [README.md](README.md) - Project overview

---

*Architecture document v2.0 - April 2026*
