# .LOG-hog Architecture Documentation

**Version:** 2.0\
**Last Updated:** April 2026\
**Author:** Johan Andersson

***

## System Overview



### Layers

* UI: Swing interface
* APP: Application coordination
* SVC: Core services
* DATA: File system

***

## Core Components



### Components

* `LogHog`: entry point
* `LogTextEditor`: main window
* `LogFileHandler`: file operations
* `EncryptionManager`: crypto operations
* `BackupManager`: backups

***

## Encryption Flow

```mermaid
flowchart LR
    user["User / UI"]
    editor["LogTextEditor"]
    handler["LogFileHandler"]
    encrypt["EncryptionManager"]
    pbkdf2["PBKDF2 (Key Derivation)"]
    aes["AES-GCM"]
    storage["File System (DATA)"]
    backup["BackupManager"]

    user --> editor
    editor --> handler
    handler --> encrypt
    encrypt --> pbkdf2
    pbkdf2 --> aes
    aes --> handler
    handler --> storage
    handler --> backup
    backup --> storage
```

***

## Backup Flow

```mermaid
flowchart LR
    handler["LogFileHandler"]
    backup["BackupManager"]
    storage["File System"]
    remote["Remote Backup Storage"]

    handler -->|triggers backup| backup
    backup -->|reads| storage
    backup -->|writes| remote
    backup -->|verifies| handler
```

***

## Startup Flow

```mermaid
flowchart TD
    loghog["LogHog"]
    config["Config Loader"]
    keyring["KeyringManager"]
    services["SVC (Core services)"]
    ui["LogTextEditor (UI)"]

    loghog --> config
    config --> keyring
    keyring --> services
    services --> ui
```

***

## Password Handling

* Progressive delays after failed attempts
* Limited retries
* Raw password not retained after unlock
* Restart required after limit

***

## Project Structure

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

## Design Patterns

* Singleton
* Factory
* Observer
* Facade

***

## Technology Stack

| Area     | Technology |
| -------- | ---------- |
| Language | Java 17    |
| UI       | Swing      |
| Crypto   | JDK        |
| Build    | javac      |

***

## Security Considerations

* Protects data at rest
* No protection against malware
* Memory exposure reduced, but still possible during active session
* Secure deletion is best-effort

***

## Data Flow

```mermaid
flowchart LR
    UI["UI: LogTextEditor"]
    APP["APP: LogHog / Coordinators"]
    SVC["SVC: EncryptionManager / BackupManager / LogFileHandler"]
    DATA["DATA: File System"]

    UI --> APP
    APP --> SVC
    SVC --> DATA
    SVC --> APP
```

***

## Performance

* Fast startup
* Low memory use
* Depends on file size

***

## Glossary

* AES: encryption
* GCM: integrity + encryption
* PBKDF2: key derivation
* IV: initialization vector
* Salt: random input

***

## Documentation

* encryption.md
* help.md
* README.md

***

*Architecture document v2.0 – April 2026*
