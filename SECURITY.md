# Security Architecture & Vulnerability Reporting

Thanks for helping keep **.LOG-hog** safe. 

.LOG-hog is a local-first, encrypted desktop application built with security as a core design principle. This document describes the security architecture, threat model, and reporting procedures.

---

## Table of Contents

1. [Security Architecture](#security-architecture)
2. [Encryption & Key Management](#encryption--key-management)
3. [Authentication & Attempt Limiting](#authentication--attempt-limiting)
4. [Backup & Restore Security](#backup--restore-security)
5. [File Permissions & Hardening](#file-permissions--hardening)
6. [Password Policy & Data Cleanup](#password-policy--data-cleanup)
7. [Security Scope Guidance](#security-scope-guidance)
8. [Reporting a Vulnerability](#reporting-a-vulnerability)
9. [Supported Versions](#supported-versions)
10. [Disclosure Policy](#disclosure-policy)

---

## Security Architecture

### Overview

.LOG-hog uses a **multi-layer security model** combining encryption, authentication, access control, and secure memory management:

```mermaid
graph TB
    A["LogTextEditor<br/>(Main Entry Point)"]
    
    B["EncryptionHandler<br/>(Session Management)"]
    B1["• Authentication & password prompting<br/>• Attempt limiting (3 failures → exit on startup)<br/>• Session key management from password<br/>• Progressive delays for failed auth"]
    
    C["EncryptionManager<br/>(Cryptographic Core)"]
    C1["• AES-256-GCM encryption/decryption<br/>• PBKDF2 key derivation: 600K iterations<br/>• Secure random IV generation: 96-bit<br/>• File format: MAGIC + VERSION + SALT + IV + CIPHERTEXT + TAG"]
    
    D["BackupManager & BackupRestoreVerifier"]
    D1["• HMAC-SHA256 backup signing (v2)<br/>• Encrypted backup verification<br/>• Rejection of legacy v1 (120K iterations)<br/>• Format: ENCRYPTED_BACKUP + HMAC(32B)"]
    
    E["SecurityFilePolicy<br/>(File System Hardening)"]
    E1["• POSIX: owner-only (rw-) files, (rwx) dirs<br/>• Windows: ACL-based enforcement<br/>• Verification: isOwnerOnlyAccessEnforced()<br/>• Fail-secure: ensureOwnerOnlyPermissionsOrThrow()"]
    
    F["CryptoUtils<br/>(Secure Cleanup & Validation)"]
    F1["• zeroize(byte[]) - overwrite with zeros<br/>• zeroize(char[]) - password cleanup<br/>• constantTimeEquals(a, b) - safe comparison<br/>• validateGcmParams(iv, tag)"]
    
    A --> B
    B --> B1
    A --> C
    C --> C1
    A --> D
    D --> D1
    A --> E
    E --> E1
    A --> F
    F --> F1
    
    style A fill:#4A90E2,stroke:#333,color:#fff,font-weight:bold
    style B fill:#7B68EE,stroke:#333,color:#fff
    style C fill:#7B68EE,stroke:#333,color:#fff
    style D fill:#7B68EE,stroke:#333,color:#fff
    style E fill:#7B68EE,stroke:#333,color:#fff
    style F fill:#7B68EE,stroke:#333,color:#fff
```

### Key Principles

1. **Local-First**: All data stays on the user's machine; no cloud sync
2. **Encrypt-By-Default**: User logs must be password-encrypted on the first run
3. **Defense in Depth**: Multiple layers (encryption, permissions, rate limiting, cleanup)
4. **Fail-Secure**: On permission or encryption errors, deny access rather than downgrade
5. **Constant-Time Operations**: MAC and tag comparisons use `MessageDigest.isEqual()`
6. **Memory Cleanup**: Sensitive arrays (keys, passwords) are zeroized after use

---

## Encryption & Key Management

### Algorithm & Parameters

| Property | Value | Rationale |
|----------|-------|-----------|
| **Symmetric Encryption** | AES-256-GCM | Authenticated encryption with 128-bit auth tag |
| **Key Derivation Function** | PBKDF2-HMAC-SHA256 | Standard, vetted KDF resistant to GPU attacks |
| **KDF Iterations** | 600,000 | High iteration count (as of 2026) to resist brute force |
| **IV (Nonce)** | 96 bits, random per encryption | GCM-compliant; uniqueness prevents replay/forgery |
| **Salt** | 128 bits, random per password | Prevents rainbow tables; unique per encrypted file |
| **Key Length** | 256 bits | Maximum AES key size for maximum security margin |

### Encryption Flow

```
User Password (e.g., 20+ chars)
          ↓
   ┌──────────────────┐
   │ Generate Salt    │ ← 128-bit random
   └─────────┬────────┘
             ↓
   ┌─────────────────────────────────────────┐
   │ PBKDF2 (600K iterations, HMAC-SHA256)   │
   │ Input:  password + salt                  │
   │ Output: 256-bit session key              │
   └─────────┬─────────────────────────────────┘
             ↓
   ┌──────────────────┐
   │ Generate IV      │ ← 96-bit random
   └─────────┬────────┘
             ↓
   ┌────────────────────────────────────────────┐
   │ AES-256-GCM Encrypt                         │
   │ Key:       derived from password            │
   │ IV:        random per operation             │
   │ AAD:       (optional, not used currently)   │
   │ Plaintext: log entry content                │
   │ Output:    ciphertext + 128-bit auth tag    │
   └─────────┬──────────────────────────────────┘
             ↓
   ┌─────────────────────────────────────────────────┐
   │ File Format:                                    │
   │   MAGIC(4: "LOGH") + VERSION(1) +              │
   │   SALT_LEN(1) + SALT(16) +                     │
   │   IV_LEN(1) + IV(12) +                         │
   │   CIPHERTEXT + AUTH_TAG(16)                    │
   └─────────┬──────────────────────────────────────┘
             ↓
      Encrypted Log File
```

### Key Derivation Details

**Session Key Derivation:**
- Algorithm: PBKDF2-HMAC-SHA256
- Iterations: 600,000
- Salt: 16 random bytes per file
- Password: User input (minimum 20 characters, see Password Policy)
- Output: 256-bit AES key

**Backup Signing Key Derivation (BackupKeyDerivation.java):**
- Context string: "loghog-backup-hmac-v2"
- Same iteration count (600,000) as primary encryption
- Used to derive HMAC-SHA256 key for backup integrity
- **Legacy support removed**: v1 (120,000 iterations) signatures are rejected

### Decryption & Verification

```
Encrypted File
      ↓
Parse Header:
  • Verify MAGIC = "LOGH"
  • Read VERSION (must be 1)
  • Extract SALT
  • Extract IV
      ↓
User Password
      ↓
PBKDF2 (600K iterations) → Session Key (256-bit)
      ↓
AES-256-GCM Decrypt:
  • Input:  ciphertext, IV, key, auth tag
  • Output: plaintext (if tag verifies)
  • On tag mismatch: throw EncryptionException
      ↓
Plaintext Log Content
```

---

## Authentication & Attempt Limiting

### Startup Authentication Flow

```mermaid
flowchart TD
    Start["Application Launch"]
    Setup["EncryptionHandler.handleEncryptionSetup()"]
    FileCheck{Is log file present?}
    
    NoFile["Show: Create / Browse / Restore<br/>If user creates: set encrypted=true<br/>If user browses: continue"]
    YesFile["Continue to password prompt"]
    
    AuthPolicy["AuthenticationAttemptPolicy<br/>MAX_ATTEMPTS = 3"]
    PasswordDialog["PasswordDialog.show()<br/>User enters password"]
    
    DecryptAttempt["Decrypt with user password"]
    DecryptSuccess{Decryption<br/>succeeds?}
    
    LoadSuccess["Load log entries<br/>Unlock UI<br/>Return success"]
    
    IncrementAttempt["Increment attempt counter<br/>Remaining = MAX - attempts"]
    CheckRemaining{Remaining &gt; 0?}
    
    ShowAttempts["Show: 'N attempts left'<br/>Apply security delay<br/>Loop back"]
    Exhausted["Show: 'Locked. Exit.'<br/>Call exitHandler<br/>System.exit(1)"]
    
    Start --> Setup
    Setup --> FileCheck
    FileCheck -->|NO| NoFile
    FileCheck -->|YES| YesFile
    
    NoFile --> AuthPolicy
    YesFile --> AuthPolicy
    AuthPolicy --> PasswordDialog
    
    PasswordDialog --> DecryptAttempt
    DecryptAttempt --> DecryptSuccess
    
    DecryptSuccess -->|YES| LoadSuccess
    DecryptSuccess -->|NO| IncrementAttempt
    
    IncrementAttempt --> CheckRemaining
    CheckRemaining -->|YES| ShowAttempts
    ShowAttempts --> PasswordDialog
    CheckRemaining -->|NO| Exhausted
    
    style Start fill:#4A90E2,stroke:#333,color:#fff
    style LoadSuccess fill:#50C878,stroke:#333,color:#fff
    style Exhausted fill:#E74C3C,stroke:#333,color:#fff
```

### Manual Unlock (Lock/Unlock during session)

```mermaid
flowchart TD
    UserLock["User clicks Lock button"]
    PerformLock["LogTextEditor.performLock()"]
    LockActions["• Save current state<br/>• Show password prompt<br/>• Disable UI: entries, editing<br/>• Clear sensitive runtime state<br/>• No exit on password failure"]
    
    UserUnlock["User clicks Unlock button"]
    PasswordDialog["PasswordDialog.show()"]
    DecryptAttempt["Attempt to decrypt"]
    
    Success{Decryption<br/>succeeds?}
    SuccessUI["Re-enable UI"]
    FailUI["Remain locked<br/>User can retry"]
    
    UserLock --> PerformLock
    PerformLock --> LockActions
    LockActions --> UserUnlock
    
    UserUnlock --> PasswordDialog
    PasswordDialog --> DecryptAttempt
    DecryptAttempt --> Success
    
    Success -->|YES| SuccessUI
    Success -->|NO| FailUI
    FailUI --> UserUnlock
    
    style UserLock fill:#FF9500,stroke:#333,color:#fff
    style SuccessUI fill:#50C878,stroke:#333,color:#fff
    style FailUI fill:#FFA500,stroke:#333,color:#fff
```

### Attempt Limits & Exit Behavior

| Scenario | Attempt Limit | Exit on 3rd Failure? | Notes |
|----------|---------------|---------------------|-------|
| Startup authentication | 3 per startup | ✅ Yes (exit 1) | Prevents brute-force; user can retry app |
| Manual lock/unlock | None currently | ❌ No | User remains locked but app stays open |
| Lock → Unlock transition | None | ❌ No | User can retry as many times as needed |

---

## Backup & Restore Security

### Backup Creation & Signing

```mermaid
flowchart TD
    Start["User chooses: Create Backup"]
    CreateBackup["BackupManager.createBackup()"]
    ReadEntries["Read plaintext log entries"]
    Serialize["Serialize to backup format"]
    Encrypt["Encrypt using AES-256-GCM<br/>with new random salt and IV"]
    
    EncryptedBlob["Encrypted Backup Blob"]
    
    DeriveSigning["Derive Backup Signing Key"]
    DeriveDetails["BackupKeyDerivation.deriveV2():<br/>• Input: user password + salt + context string<br/>• Context: 'loghog-backup-hmac-v2'<br/>• Iterations: 600,000<br/>• Output: 256-bit HMAC key"]
    
    ComputeHmac["HMAC-SHA256(backup_key, ciphertext)"]
    
    FileFormat["Backup File Format:<br/>[Encrypted Backup Blob] + [HMAC Signature 32B]"]
    
    Start --> CreateBackup
    CreateBackup --> ReadEntries
    ReadEntries --> Serialize
    Serialize --> Encrypt
    Encrypt --> EncryptedBlob
    
    EncryptedBlob --> DeriveSigning
    DeriveSigning --> DeriveDetails
    DeriveDetails --> ComputeHmac
    ComputeHmac --> FileFormat
    
    style Start fill:#4A90E2,stroke:#333,color:#fff
    style FileFormat fill:#50C878,stroke:#333,color:#fff
```

### Backup Restore & Verification

```mermaid
flowchart TD
    Start["User chooses: Restore from Backup"]
    SelectFile["1. User selects backup file"]
    ValidCheck["2. Check: isValidEncryptedBackup()<br/>Must be encrypted with valid magic bytes"]
    PromptPassword["3. User provides password<br/>for HMAC derivation"]
    
    DeriveKey["4. Derive backup signing key:<br/>BackupKeyDerivation.deriveV2()<br/>Same 600K iterations + context string"]
    
    VerifyHmac["5. Verify HMAC:<br/>BackupRestoreVerifier.stripVerifiedHmac()"]
    HmacSteps["• Separate ciphertext from HMAC (last 32B)<br/>• Compute HMAC-SHA256(key, ciphertext)<br/>• Constant-time compare with stored HMAC"]
    
    HmacCheck{HMAC<br/>match?}
    HmacFail["Reject backup<br/>Report error"]
    
    Decrypt["6. Decrypt ciphertext:<br/>AES-256-GCM with password"]
    VerifyGcm["7. Verify decryption succeeded<br/>GCM tag must be valid"]
    
    DecryptCheck{GCM<br/>valid?}
    DecryptFail["Report decryption error"]
    
    Restore["8. Restore entries to current log file"]
    Cleanup["9. Zeroize: sensitive arrays cleared<br/>password, HMAC key, decrypted data"]
    Success["Restore Complete or Error"]
    
    Start --> SelectFile
    SelectFile --> ValidCheck
    ValidCheck --> PromptPassword
    PromptPassword --> DeriveKey
    DeriveKey --> VerifyHmac
    VerifyHmac --> HmacSteps
    HmacSteps --> HmacCheck
    
    HmacCheck -->|NO| HmacFail
    HmacCheck -->|YES| Decrypt
    
    Decrypt --> VerifyGcm
    VerifyGcm --> DecryptCheck
    
    DecryptCheck -->|NO| DecryptFail
    DecryptCheck -->|YES| Restore
    
    Restore --> Cleanup
    Cleanup --> Success
    
    HmacFail --> Success
    DecryptFail --> Success
    
    style Start fill:#4A90E2,stroke:#333,color:#fff
    style Success fill:#50C878,stroke:#333,color:#fff
    style HmacFail fill:#E74C3C,stroke:#333,color:#fff
    style DecryptFail fill:#E74C3C,stroke:#333,color:#fff
```

### Legacy Backup Rejection

**Policy**: .LOG-hog **NEVER** supports legacy backup formats:
- v1 PBKDF2 (120,000 iterations) → **REJECTED**
- Weaker key derivations → **REJECTED**
- Fallback cryptography → **NOT PRESENT**

**Rationale**: Accepting legacy formats would create downgrade attacks and maintainability burden. New backups always use v2 (600K).

---

## File Permissions & Hardening

### POSIX File Permissions (Linux/macOS)

**For Regular Files (logs, backups):**
```
Target:   -rw------- (0600)
          Owner: read + write
          Group: (none)
          Others: (none)
```

**For Directories (.loghog, backups/):**
```
Target:   drwx------ (0700)
          Owner: read + write + execute (traverse)
          Group: (none)
          Others: (none)
```

**Implementation:**
```java
// src/security/SecurityFilePolicy.java
if (Files.isDirectory(path)) {
    perms.add(PosixFilePermission.OWNER_EXECUTE);  // ← Required for traversal
}
Files.setPosixFilePermissions(path, perms);
```

### Windows File Permissions

- Best-effort NTFS ACL hardening
- Restricts access to Owner (current user)
- Falls back to `File.setReadable(true, true)` if ACLs unavailable
- **Note**: Some filesystems or USB drives may not support strict ACLs

### Verification & Enforcement

```java
// Check if permissions are correctly set
boolean secure = SecurityFilePolicy.isOwnerOnlyAccessEnforced(path);

// Fail-secure: enforce or throw exception
SecurityFilePolicy.ensureOwnerOnlyPermissionsOrThrow(path);
```

### Platform-Specific Behavior

| Platform | Method | Reliability |
|----------|--------|------------|
| Linux/macOS (ext4, HFS+) | POSIX setPermissions() | High ✅ |
| Windows (NTFS) | ACL setting + setReadable/setWritable | Medium ⚠️ |
| USB/FAT32 | Fallback (no ACLs) | Low ❌ |
| Network shares | Variable | Not supported |

---

## Password Policy & Data Cleanup

### Password Requirements

**For New Encryption:**
- **Minimum length**: 20 characters (enforced by `PasswordPolicy.validateForNewEncryption()`)
- **Character set**: No restrictions (user chose strong passphrases)
- **Entropy**: User responsible; password strength indicator provided in UI

**Example valid passwords:**
```
correct-horse-battery-staple
MyDogAteMyHomework123!@#
supercalifragilisticexpialidocious
```

**Example invalid:**
```
short123        (too short, < 20 chars)
password1234567 (too short, < 20 chars)
```

### Sensitive Data Cleanup (Memory Zeroization)

**What gets zeroized:**
- User passwords (char[]) after KDF
- Derived keys (byte[]) after encryption/decryption
- Plaintext content during decryption
- HMAC keys and computed MACs
- Authentication attempts tracking

**Implementation:**
```java
// CryptoUtils.zeroize()
public static void zeroize(byte[] arr) {
    if (arr != null) Arrays.fill(arr, (byte) 0);
}

public static void zeroize(char[] arr) {
    if (arr != null) Arrays.fill(arr, '\0');
}
```

**On Lock/Shutdown (LogTextEditor.clearSensitiveRuntimeState):**
```
1. Save settings (if needed)
2. Stop clipboard monitoring
3. Clear recent entries from memory
4. Shutdown lock held (released only in finally after clipboard cleanup)
5. Flush encryption context
6. Exit JVM (if on startup exhaustion) or remain locked (if manual lock)
```

---

## Security Scope Guidance

### ✅ In Scope (Report These)

Security vulnerabilities requiring cryptographic or architectural fixes:

- **Encryption/authentication weaknesses**: AES-GCM usage errors, IV collision, nonce reuse
- **Key handling bugs**: Passwords not cleared, keys exposed in logs
- **Plaintext leakage**: Data written unencrypted to disk, logs, temp files, swapfiles
- **PBKDF2 flow issues**: Wrong iteration count, missing salt validation
- **Backup integrity bypass**: HMAC verification skipped, v1 legacy acceptance
- **Lockout bypass**: Attempt counter reset, authentication skipped
- **File permission bypass**: Sensitive files readable by others (when POSIX enforced)
- **Tamper detection**: Corruption not detected during decryption
- **Replay attacks**: Same backup used twice without detection
- **Constant-time failures**: Timing attacks on MAC/tag comparison
- **Clipboard security**: Sensitive data exposed during copy/paste operations
- **Session management**: Session key leaked, reused, or not cleaned up

### ❌ Out of Scope (Do Not Report)

- Issues requiring system compromise (malware, kernel exploit, hardware keylogger)
- User self-exposure from weak OS-level security (e.g., group-readable .loghog/ folder)
- Denial-of-service without security impact (slow encryption, storage exhaustion)
- Social engineering attacks (user tricks self into revealing password)
- Physical attacks (theft of hard drive, cold boot attacks)
- Spectre/Meltdown class issues (mitigation outside app scope)
- Third-party library vulnerabilities with no direct security implication
- Documentation/website typos

### Supported Versions

| Version | Security Support |
|---------|-----------------|
| Latest release | ✅ Receives fixes |
| `main` branch | ✅ Receives fixes |
| Older releases | ❌ No fixes |

---

## Reporting a Vulnerability

### Use Private Vulnerability Reporting

**GitHub provides a private channel:**

1. Navigate to: **https://github.com/johandersson/.LOG-hog/security/advisories/new**
2. Submit your report with:
   - Clear description of the issue and its impact
   - Affected version/commit/OS (Windows/macOS/Linux) and Java version
   - Detailed reproduction steps or proof-of-concept code
   - Expected vs. actual behavior
   - Any logs, stack traces, or screenshots (with secrets/credentials **redacted**)

### If Private Reporting Unavailable

- Open a minimal public issue **without sensitive details**
- Request a private contact path in the issue
- Do **NOT** include reproduction code, credentials, or full stack traces

### What to Include

- **Severity**: Critical / High / Medium / Low (explain impact)
- **Category**: Encryption / Authentication / Permissions / Backup / Other
- **Version affected**: e.g., commit hash, release tag, or "main"
- **Environment**: OS, Java version, filesystem type
- **Reproduction**:
  ```
  1. Create log file with password "xyz"
  2. Lock the app
  3. Attempt to unlock with wrong password
  4. [Observe unexpected behavior]
  ```
- **Expected behavior**: "App should exit after 3 failed attempts"
- **Actual behavior**: "App allows unlimited attempts"
- **Logs/Traces**: `~/.loghog/loghog.log` (with sensitive data removed)

---

## Disclosure Policy

We follow **responsible disclosure**:

1. **Private Report Received** → We acknowledge within 2 business days
2. **Investigation** → We investigate and confirm the issue
3. **Fix Development** → We develop and test a patch
4. **Pre-Release Notification** → We notify you of the fix (optional)
5. **Release** → We publish a security fix update
6. **Public Advisory** → We post a CVE/advisory and credit the reporter (if desired)

**Expected timeline**: 30–90 days from report to fix, depending on complexity

**Responsible Disclosure Terms:**
- Allow time for a fix before public disclosure
- Do not publicly disclose until a fix is released (or 90 days pass)
- Avoid exploiting the vulnerability for access, data exfiltration, or disruption
- Do not contact users or third parties before we publish the fix

---

## Safe Harbor

We appreciate good-faith security research. **We will not pursue legal action against researchers who:**

- Discover vulnerabilities through authorized testing
- Report issues privately and in good faith
- Avoid privacy violations, data destruction, disruption, or social engineering
- Give us reasonable time to develop a fix before disclosure

**Thank you for helping keep .LOG-hog secure! 🔒**
