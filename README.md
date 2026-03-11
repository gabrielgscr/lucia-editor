# Lucia Editor

Lucia Editor is a desktop IDE-like editor built with Java Swing for Lucia language projects.

## Features

- Project explorer for `.lucia` files and folders.
- Create new Lucia files and folders from the tree context menu.
- Multi-tab editing with close buttons on tabs.
- Fixed top tab bar (scroll is applied to text area only).
- Save current file and save all open files.
- Toolbar with icons and quick actions.
- Theme support: light/dark mode toggle.
- Theme-aware icon color updates.
- Configurable editor font size (`A-`, `A+`, `Ctrl+-`, `Ctrl+=`, `Ctrl+0`).
- Internationalized UI (Spanish/English), extensible to more locales.
- Help menu with a rich About dialog.
- Recent projects menu with persistence and “clear recent projects”.

## Lucia syntax highlighting

- Custom `RSyntaxTextArea` token maker (`text/lucia`).
- English Lucia keywords and core language tokens.
- Built-in global functions highlighted (`print`, `len`, `input`, `str`, `type_of`, `abs`, `min`, `max`, `pow`, `round`, `floor`, `ceil`, `random`).
- Built-in data types highlighted (`int`, `float`, `string`, `bool`, `void`, `any`, `list`, `dict`).
- Boolean and `null` literals differentiated.
- Single-line (`//`) and block (`/* ... */`) comments highlighted, including multiline state continuation.
- Custom light/dark color palettes tuned for better token contrast.

## CLI integration

- Run current Lucia file.
- Compile current Lucia file with `--save`.
- Run Lucia tests (`pytest -q`).
- Execute custom Lucia CLI commands.
- Interactive stdin support in output panel for programs using `input(...)`.
- Unbuffered Python output handling so prompts appear in correct order.

## UI stack

- FlatLaf (light and dark themes).
- RSyntaxTextArea.
- Ikonli FontAwesome5 icons.

## Requirements

- OpenJDK 25
- Maven 3.9+

## Run

```bash
mvn clean compile exec:java
```

If you use VS Code installed via Snap and see linker errors like:

`/snap/core20/.../libpthread.so.0: undefined symbol: __libc_pthread_init`

use:

```bash
./run-editor.sh
```

If you see `No X11 DISPLAY variable was set`, run from a desktop session terminal (GUI-enabled), not from a headless shell.

## Configure Lucia integration

From the app menu: `Tools -> Settings`.

- **Lucia project root**: folder where Lucia `main.py` lives (for example `../lucia`).
- **Python executable**: executable path, for example `.venv/bin/python`.

The editor executes commands from the configured Lucia root, for example:

```bash
<python_executable> main.py run <current_file>
```

## Add a new language

Add a new resource bundle file under `src/main/resources/i18n`:

- `messages_<locale>.properties`

Then add a language item in `MainFrame`.
