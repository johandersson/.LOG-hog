# .LOG-hog

.LOG-hog is a modern, lightweight Java application for fast, distraction-free journaling and note-taking using the classic `.LOG` format from Notepad. This tool is designed for users who want a simple, efficient, and human-readable way to manage personal logs, diaries, or daily notes—while remaining fully compatible with Notepad's `.LOG` files.

---

## Key Features

- **Tabbed Interface:** Effortlessly switch between writing new entries and browsing past logs.
- **Quick Entry:** Add notes instantly with automatic timestamps.
- **Keyboard Shortcuts:**
  - `Ctrl+S` — Save a new entry
  - `Ctrl+R` — Refresh the log list to reflect external changes
- **Sorted Log List:** View all entries in descending order, newest first.
- **Delete Log Entry:** Right-click any entry to delete it instantly.
- **Simple Storage:** All logs are stored in a plain text file (`log.txt`) in your home directory.
- **Distraction-Free UI:** Minimal, clean design for focused journaling.
- **Easy Backup & Sharing:** Entries are in a portable, human-readable format.
- **Full Notepad `.LOG` Compatibility:** Seamlessly use your existing Notepad `.LOG` file or switch between Notepad and .LOG-hog as needed.
- **Robust Reload:** Edit your log file in Notepad and reload in .LOG-hog instantly with `Ctrl+R`.
- **Cross-Platform:** Built with Java, runs anywhere Java is available.

---

## Use Cases

- Personal journaling and diary keeping
- Daily work logs or stand-up notes
- Quick thoughts and reminders
- Developers or sysadmins keeping timestamped logs
- Anyone wanting a simple, timestamped note system with full transparency and control

---

## Getting Started

1. **Download & Run:**  
   Download the latest release from the [Releases page](https://github.com/johandersson/.LOG-hog/releases) and run with Java 17+:
   ```sh
   java -jar LOG-hog.jar
   ```

2. **Open or Create a `.LOG` File:**  
   The app will create (or use) `log.txt` in your home directory. You can replace this with your own `.LOG` file used in Notepad.

3. **Write & Organize:**  
   - Use the "New Entry" tab to jot down your thoughts.
   - Browse, search, and delete past entries as needed.

4. **Sync with Notepad:**  
   - Make edits in Notepad if you wish.
   - Hit `Ctrl+R` in .LOG-hog to reload and stay in sync.

---

## Screenshots

![loghog](https://github.com/user-attachments/assets/3f63d31a-c6ad-432a-8c50-623280a3fc61)
![loghog2](https://github.com/user-attachments/assets/970b6805-367f-450f-96f2-aeac0937d968)

---

## Why .LOG-hog?

- **No vendor lock-in:** Your data is always accessible and editable.
- **Portable:** Works wherever Java works. Take your logs anywhere.
- **Zero friction:** No signups, no cloud, no complexity—just your notes.

---

## Changelog Highlights

- Major UI redesign for a more modern, tabbed interface
- Enhanced compatibility with Notepad `.LOG` files
- Fast entry, instant log sorting, and improved keyboard navigation
- Robust log reload and deletion features
- Numerous bugfixes and performance improvements

> For the full commit history and all updates, see the [GitHub commits page](https://github.com/johandersson/.LOG-hog/commits).

---

## License

GPL3

---

## Contributing

Pull requests and suggestions are welcome! See [issues](https://github.com/johandersson/.LOG-hog/issues) for things to work on or to report bugs.

**Happy logging!**
