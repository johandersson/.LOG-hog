# 📘 Welcome to .LOG-hog

## Purpose

.LOG-hog is designed for **fast, focused note-taking**.

When the application starts, the editor is immediately focused so you can begin writing without interruption. Press **Ctrl+S** or click **Save** to store your entry as a timestamped log entry and automatically clear the editor for the next note.

This workflow allows rapid, continuous note-taking with minimal friction.

***

## ⚡ Lightweight & Cross-Platform

* **Small footprint (\~230 KB)** with no external runtime dependencies
* Built in **pure Java**, ensuring consistent behavior across platforms
* Runs on **Windows, macOS, and Linux**

The application starts quickly and is designed to be efficient for everyday use.

***

## 🗂️ .LOG Format Compatibility

.LOG-hog is fully compatible with standard `.LOG` files.

* Open an existing `.LOG` file and continue working immediately
* Your data remains **plain text by default**, editable in any text editor
* Advanced features (encryption, search, formatting) are layered on top

### About the Format

.LOG-hog is inspired by the classic **Notepad `.LOG` behavior**, where timestamps are appended automatically.  
It extends this concept with:

* structured entries
* search and filtering
* optional encryption
* backups
* markdown rendering

***

# 🔐 Security Overview

.LOG-hog uses **modern cryptographic techniques and secure coding practices** to protect data at rest.

### Key Security Features

* **AES-256-GCM authenticated encryption**
* **PBKDF2 (600,000 iterations)** for key derivation
* Progressive delay on failed password attempts
* Sensitive data stored in memory as mutable arrays and cleared after use
* Clipboard auto-clearing for sensitive content
* Optional encrypted backups
* Secure file handling and path validation
* Static analysis tools used to detect common vulnerability classes

👉 See `encryption.md` for detailed technical information.

***

## ⚠️ Security Model (Important)

.LOG-hog is designed to protect:

* Local files and backups against unauthorized access
* Data at rest from offline attacks

It does **not protect against**:

* Malware or keyloggers
* Attackers with access to system memory during an active session
* Clipboard access after unexpected termination

👉 Security ultimately depends on:

* a **trusted system**
* and a **strong password**

***

## 📋 Clipboard Security

Sensitive data copied from .LOG-hog is protected with:

* **Automatic clearing** after a configurable timeout (default: 15 seconds)
* Manual "Clear Clipboard" option
* User warnings when copying sensitive content

### ⚠️ Limitation

If the app is terminated unexpectedly (e.g., crash, forced quit):

* Clipboard contents are **not cleared automatically**

👉 Always manually clear the clipboard after unexpected termination.

***

# ✨ Key Features

* **Tabbed interface** for writing and browsing logs
* **Quick entry workflow** with automatic timestamps
* **Advanced search and filtering**
* **Single-instance enforcement**
* **Right-click actions** (edit date, delete, copy)
* **Optional encryption**
* **Manual lock / unlock**
* **Backup and restore support**
* **System tray integration**

***

# ⌨️ Keyboard Shortcuts

* **Ctrl+S** – Save entry
* **Ctrl+N** – New quick entry
* **Ctrl+R** – Refresh log
* **Ctrl+F** – Search

***

# 🔎 Filtering Entries

* **Search bar** for keyword filtering
* **Date range filtering** for time-based queries

***

# 🧩 System Tray Features

* Quick access to recent entries
* Add new entries directly
* Clipboard control (clear sensitive data)
* Access clipboard security settings

***

# 💾 Backup and Restore

### Backups

* Manual backup from Settings
* Optional automatic backups
* Backups preserve encryption state

### Important Notes

* File overwrite uses best-effort secure deletion
* On SSDs, deletion is not guaranteed due to hardware behavior

### Restore

Replace your log file manually with a backup file if needed.

***

# 🔐 Encryption

### Enabling Encryption

* Enable from the Settings tab
* Requires a strong password
* Password is never stored on disk

***

### Security Notes

* Encryption uses **AES-GCM**, a widely used authenticated encryption mode
* Keys are derived using PBKDF2 with a high iteration count
* Sensitive data is cleared from memory after use

⚠️ **Important:**  
If you forget your password, your data cannot be recovered.

***

### Password Recommendations

* Use **20+ characters**
* Prefer random or passphrase-based passwords
* Avoid reuse across services
* Use a password manager

***

### Password Generator

.LOG-hog includes a built-in generator:

* Random passwords
* Diceware-style passphrases
* Strength indicator

***

### Password Dialog

* Type your password in the masked field
* Click the **eye button** (or hold **ESC**) to peek at the password while typing — release to re-mask it
* Press **Enter** to confirm, **Cancel** to abort

***

### Usage

* Password is required on application startup
* Failed attempts trigger increasing delays

***

### Lock / Unlock

* Lock clears decrypted data from memory and disables all editing operations
* Unlock requires password re-entry
* Unlock from any view: click the **Unlock** link in the locked entry or log list area, or use the **Unlock File** button in the Full Log tab

***

### Performance Note

Encryption introduces a small delay during loading and saving.  
Decrypted data is cached in memory for performance during active use.

***

# ✏️ Editing Entries

* **Edit Timestamp** via right-click
* **Delete Entries** with confirmation
* **Copy to clipboard** (with auto-clear protection)

***

# 🔗 Links

You can include:

* URLs
* Local file links

Example:

```
http://example.com
file:///path/to/file
```

***

# 📝 Markdown Support

Supported formatting:

* Headers (`#`)
* Bold / Italic
* Links
* Lists
* Blockquotes
* Inline and block code

Rendering applies in the Full Log view.

***

# ⚙️ Performance Notes

* Large logs are partially rendered for responsiveness
* Use filters to access older entries

***

# 📄 License

.LOG-hog is licensed under **GPL v3**.  
See `LICENSE.md` for details.

***

# 📦 Repository

GitHub:  
<http://github.com/johandersson/.LOG-hog>

***
