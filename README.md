# LogHog - Your Personal Log Management Companion

LogHog is a powerful, user-friendly desktop application designed for efficient log file management and note-taking. Whether you're a developer tracking bugs, a writer journaling ideas, or anyone needing to organize personal logs, LogHog offers an intuitive interface with advanced features to streamline your workflow.

## What is .log?

.log is not a specific file format, but a feature in Windows Notepad. When you save a text file with the .log extension, Notepad automatically prepends the current date and time at the top of the file. This creates a simple log by appending timestamps to your notes.

For more details, see [How to Use Notepad to Create a Dated Log or Journal File](https://www.howtogeek.com/258545/how-to-use-notepad-to-create-a-dated-log-or-journal-file/).

LogHog builds on this concept by providing a dedicated application for managing, editing, and viewing .log files, making it much easier to handle personal logs and notes with advanced features like search, filtering, and markdown rendering.

## Key Features

- **Tabbed Interface**: Seamlessly switch between writing new entries and browsing past logs.
- **Quick Entry**: Add notes instantly with automatic timestamps.
- **Single-Instance Enforcement**: Prevents conflicts by ensuring only one instance runs at a time.
- **Advanced Search & Filtering**: Find entries by keywords or date ranges with lightning-fast search.
- **Markdown Rendering**: View your logs with rich formatting for better readability.
- **System Tray Integration**: Access recent logs and add quick notes directly from the tray.
- **Editable Entries**: Modify dates, delete entries, or copy to clipboard with right-click options.
- **Link Support**: Create clickable links to URLs and local files within your logs.
- **Keyboard Shortcuts**: Boost productivity with shortcuts like Ctrl+S (save), Ctrl+F (search), and more.

## Installation & Usage

### Prerequisites
- Java 21 or newer
- Windows (primary support)

### Running LogHog
1. Clone the repository: `git clone https://github.com/johandersson/.LOG-hog.git`
2. Navigate to the `src` directory.
3. Run `build.bat` to compile the project.
4. Execute `run.bat` to launch the application.

### Building from Source
- Ensure Java 21 or newer is installed.
- Run `build.bat` in the `src` folder to build the JAR.
- The output will be in the project root.

## Why Choose LogHog?

- **Free & Open Source**: No hidden costs, fully customizable.
- **Lightweight**: Minimal resource usage, fast startup.
- **Privacy-Focused**: All data stored locally, no cloud dependencies.
- **Extensible**: Built with Java, easy to modify and extend.
- **Cross-Platform Potential**: While currently Windows-focused, can be adapted for other OS.

## Documentation

For detailed usage instructions, keyboard shortcuts, and advanced features, see the [Help File](help.md).

## Contributing

We welcome contributions! Please fork the repository and submit pull requests. For major changes, open an issue first to discuss.

## License

This project is licensed under the GPL 3 License - see the [LICENSE](src/license.md) file for details.

## Contact

- **Author**: Johan Andersson
- **GitHub**: [johandersson](https://github.com/johandersson)
- **Repository**: [LogHog on GitHub](https://github.com/johandersson/.LOG-hog)

---

*LogHog - Tame your logs, unleash your productivity!*
