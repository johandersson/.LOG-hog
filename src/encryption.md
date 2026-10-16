# .LOG-hog Security and Encryption Documentation

This document restores the full encryption documentation and aligns it with the current codebase.

## Security Review Status

- Critical cryptography regressions: not identified in current implementation.
- Primary hardening updates reflected in this revision:
  - sensitive local key files use platform-specific protection: current-user DPAPI on Windows when available, otherwise AES-GCM wrapping with a random master secret protected by file permissions,
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
When opening a restored LOGH file, its header salt takes precedence over missing, corrupt, or stale settings metadata. Recovered metadata is saved only after successful authentication; opening the file does not rewrite its encrypted contents.

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

Security key files for event signing/HMAC are protected instead of stored as raw plaintext/base64. Windows uses current-user DPAPI when available; other platforms, and Windows when DPAPI is unavailable, use AES-GCM wrapping with a random master secret stored in the app-data directory and protected by file permissions.

Copying the complete app-data directory exposes the master secret and wrapped keys on the file-permission-based path. DPAPI is tied to the current Windows user profile, not hardware-backed. Neither path is a hardware-backed secret store.

Protection flow:

1. use current-user DPAPI on Windows when available; otherwise load or create a random master secret protected by app-data file permissions,
2. derive a purpose/context-bound wrapping key using HMAC-SHA256,
3. encrypt key blob with AES-GCM + purpose-bound AAD,
4. enforce owner-only permissions on wrapped key artifacts.

## Authentication Attempts

- up to 3 password attempts per unlock session,
- startup exits after three failed attempts; manual unlock ends the current flow and leaves the file locked,
- the attempt count is not persisted and does not limit offline password guessing.

## Backup and Integrity Model

- Backups preserve encrypted bytes.
- Backup integrity uses HMAC-SHA256 append/verify.
- HMAC verification covers the complete encrypted file, including its LOGH header, before the trailing 32-byte signature.
- Signed backups are created and verified only with the v2 PBKDF2/600,000-iteration signing key. Legacy v1 SHA-256-derived and v2/120,000-iteration signatures are rejected; there is no weaker-key compatibility fallback.
- A valid backup signature never replaces AES-GCM authentication. Wrong passwords, altered encrypted contents, and invalid signatures remain rejected.
- Lock and backup flows compact encrypted journal into snapshot where needed.
- Secure deletion is best-effort and may not be absolute on SSD wear-leveling devices.

## Security Event Logging

- Selected backup-integrity events are chain-anchored with sequence/hash metadata.
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

- New encryption passwords must be at least 20 characters; choose a strong, unique password.
- Keep full-disk encryption enabled at OS level.
- Lock the app when unattended.
- Keep secure backups and test restore procedures.

## Summary

.LOG-hog currently combines modern authenticated encryption, tamper-evident event logging, and stronger at-rest handling for local security key artifacts. The design provides strong local protection for personal sensitive data when used on a trusted and hardened host.
