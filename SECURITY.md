# Security Policy

Thanks for helping keep **.LOG-hog** safe.

.LOG-hog is a local-first, encrypted desktop app. Security reports are especially valuable for issues involving encryption, key handling, local data exposure, tamper resistance, backup integrity, and lock/unlock flows.

## Supported Versions

We currently provide security fixes for:

| Version | Supported |
|---|---|
| Latest release | ✅ |
| `main` branch (unreleased) | ✅ |
| Older releases | ❌ |

## Reporting a Vulnerability (GitHub)

Please use **GitHub Private Vulnerability Reporting** for this repository:

- Go to: **https://github.com/johandersson/.LOG-hog/security/advisories/new**
- Submit the report privately with reproduction details.
- **Do not open public issues** for suspected vulnerabilities.

If private reporting is unavailable in your view, open a minimal public issue without sensitive details and request a private contact path.

## What to Include

Please include:

- A clear description of the issue and impact
- Affected version/commit/OS (Windows/macOS/Linux, Java version)
- Reproduction steps or proof of concept
- Expected vs actual behavior
- Any logs, stack traces, or screenshots (with secrets/redacted private data)

## Scope Guidance

In scope (examples):

- Encryption/authentication weaknesses (AES-GCM usage, nonce/key handling, PBKDF2 flow)
- Plaintext data leakage to disk/logs/backups/temp files
- Bypass of lockout/authentication protections
- Tampering/integrity bypass in security metadata or backups
- Unsafe file permission handling for sensitive artifacts

Out of scope (examples):

- Issues requiring full system compromise, malware, or keyloggers
- Local-only self-exposure caused solely by unsafe user environment configuration
- Denial-of-service risks with no security impact

## Disclosure Policy

Please allow time for investigation and a fix before public disclosure.  
After a fix is available, we may publish a security advisory and credit the reporter (if desired).

## Safe Harbor

We support good-faith security research.  
Please avoid privacy violations, data destruction, service disruption, and social engineering.
