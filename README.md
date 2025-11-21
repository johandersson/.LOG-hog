# LogHog

A secure, feature-rich Java Swing logging application.

## Features
- Tabbed interface for writing and browsing logs
- Encryption support with AES
- System tray integration
- Markdown rendering
- And more...

## Encryption Warning
If you enable encryption, the program may load slower, especially in the settings tab when applying changes and in the full log view. This is due to the encryption/decryption process for the log file.

## Installation
Build with `javac *.java` and `jar cvfe loghog.jar LogHog *.class resources/`

## Usage
Run with `java -jar loghog.jar`

For more details, see the help file.