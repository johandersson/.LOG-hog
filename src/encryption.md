# .LOG-hog Security and Encryption Documentation

This document restores the full encryption documentation and aligns it with the current codebase.

## Security Review Status

- Critical cryptography regressions: not identified in current implementation.
- Primary hardening updates reflected in this revision:
  - sensitive local key files are now protected at rest with AES-GCM wrapping derived from a protected per-user random master secret,
  - settings persistence is unified to a single canonical path,
  - user-facing error messages are sanitized in key UI paths.

## Overview

.LOG-hog protects local logs and notes with authenticated encryption, integrity-checked backups, and security event anchoring.

It is designed for local data-at-rest protection on a trusted machine. It is not designed to defend against active malware, keyloggers, or a fully compromised OS account.

## Threat Model

Designed to protect against:

- offline theft/copying of log and backup files,
- accidental exposure through weak filesystem defaults,
- silent corruption of backups.

Not designed to protect against:

- host compromise during runtime,
- memory scraping with elevated local privileges,
- clipboard interception by malware,
- social engineering/password disclosure.

## Cryptography

### Core profile

- Encryption: AES-256-GCM (AES/GCM/NoPadding)
- KDF: PBKDF2WithHmacSHA256
- PBKDF2 iterations: 600,000
- AES key size: 256-bit
- GCM IV length: 12 bytes
- GCM tag length: 16 bytes

### File format

Encrypted file header format:

MAGIC(4) | VERSION(1) | SALT_LEN(1) | SALT | IV_LEN(1) | IV | CIPHERTEXT

Salt is embedded in the encrypted file header and can be recovered for disaster-recovery decryption with the user password.

### Security properties

- Authenticated encryption (confidentiality + integrity) with GCM
- Unique random IV per encryption operation
- Random per-file salt for KDF hardening
- Constant-time integrity comparisons in critical verification paths

## Settings and Metadata Storage

### Canonical settings path

Settings are now unified at:

- ~/.loghog/settings.properties

Legacy loghog_settings.properties is treated as migration input when present.

### Sensitive key material at rest

Security key files for event signing/HMAC are protected with AES-GCM wrapping instead of raw plaintext/base64 storage.

Red-team caveat: this is stronger than plaintext/base64 key storage but is still not a hardware-backed secret store.

Protection flow:

1. load or create a protected per-user random master secret,
2. derive a purpose/context-bound wrapping key using HMAC-SHA256,
3. encrypt key blob with AES-GCM + purpose-bound AAD,
4. enforce owner-only permissions on wrapped key artifacts.

## Authentication Attempts

- 3 password attempts per unlock session,
- the unlock flow exits after all 3 attempts fail.

## Backup and Integrity Model

- Backups preserve encrypted bytes.
- Backup integrity uses HMAC-SHA256 append/verify.
- Lock and backup flows compact encrypted journal into snapshot where needed.
- Secure deletion is best-effort and may not be absolute on SSD wear-leveling devices.

## Security Event Logging

- Security events are chain-anchored with sequence/hash metadata.
- Anchor integrity is checked with HMAC and Ed25519 signature verification.
- Signing private key and HMAC key are stored as protected key blobs.

## Memory Handling

- Sensitive byte arrays are zeroized after use where practical.
- Password handling uses mutable arrays where possible.
- Runtime plaintext exposure remains possible in normal JVM process memory during active use.

## Data Flow Diagram

```mermaid
flowchart LR
    PW[Password char[]] --> KDF[PBKDF2-HMAC-SHA256]
    KDF --> ENC[AES-GCM Encrypt or Decrypt]
    ENC --> SNAP[(Encrypted snapshot file)]
    ENC --> JOUR[(Encrypted journal sidecar)]

    SNAP --> BKP[Backup Manager]
    JOUR --> BKP
    BKP --> HMAC[HMAC append and verify]
    HMAC --> BFILE[(Encrypted backup artifact)]

    SEVT[Security events] --> SELOG[SecurityEventLog]
    SELOG --> ELOG[(security-events.log)]
    SELOG --> EANCH[(security-events.anchor)]
    SELOG --> EKEY[(security-events.key wrapped)]
    SELOG --> EPRIV[(signing-private.key wrapped)]
```

## Key Protection Diagram

```mermaid
sequenceDiagram
    participant App as App
    participant Salt as key-protection.salt
    participant Master as key-protection.master
    participant KDF as HMAC-SHA256 key derivation
    participant AES as AES-GCM
    participant File as Wrapped key file

    App->>Salt: load or create random salt
    App->>Master: load or create random master secret
    App->>KDF: derive context-bound wrapping key
    App->>AES: encrypt sensitive key blob with AAD purpose
    AES->>File: write LHK1 + IV + ciphertext
    App->>File: enforce owner-only permissions
```

## Operational Limits

Locking invalidates outstanding loads, clears editor undo histories and rendered documents,
closes registered sensitive dialogs, and releases session/backup keys and caches. Failed or
cancelled authentication also releases installed keys. Clipboard clearing retains a digest-only
recovery marker and retries transient native failures; replacement clipboard content is preserved.
These measures remove application references and zero owned key arrays, not all physical copies
of plaintext in the Java heap.

- Host compromise can defeat local-at-rest controls.
- Memory dumps from an unlocked session can expose plaintext UI buffers and live session keys.
- Clipboard security cannot guarantee clearing after forced process termination.
- File-level secure deletion is best-effort on SSD/flash media.

## User Guidance

- Use a strong password (20+ chars recommended).
- Keep full-disk encryption enabled at OS level.
- Lock the app when unattended.
- Keep secure backups and test restore procedures.

## Summary

.LOG-hog currently combines modern authenticated encryption, tamper-evident event logging, and stronger at-rest handling for local security key artifacts. The design provides strong local protection for personal sensitive data when used on a trusted and hardened host.
