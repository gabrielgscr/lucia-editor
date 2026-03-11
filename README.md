# Lucia Editor (initial version)

Lucia Editor is a desktop editor built with Java Swing for Lucia language projects.

## Features

- Open a project folder and browse `.lucia` files.
- Create Lucia files and folders from the project tree.
- Edit and save Lucia files.
- Multiple editor tabs.
- Toolbar with common actions and icons.
- Toolbar controls for editor font size (`A-` and `A+`).
- Menu icons for common actions.
- Lucia syntax highlighting using RSyntaxTextArea.
- Run the current Lucia file using Lucia CLI from the sibling `lucia` project.
- Compile the current Lucia file (`compile --save`).
- Run Lucia tests (`pytest -q`) from the editor.
- Run custom Lucia CLI commands from the editor.
- Internationalized UI (Spanish and English), extensible for more languages.

Font size can be changed from:

- `View -> Decrease/Increase/Reset font size`
- Toolbar buttons `A-` and `A+`

## UI stack

- FlatLaf for a modern Swing theme.
- Ikonli FontAwesome5 pack for toolbar icons.

## Requirements

- OpenJDK 25
- Maven 3.9+

## Run

```bash
mvn clean compile exec:java
```

If you use VS Code installed via Snap and see linker errors like:

`/snap/core20/.../libpthread.so.0: undefined symbol: __libc_pthread_init`

use the launcher script:

```bash
./run-editor.sh
```

If you see `No X11 DISPLAY variable was set`, run from a desktop session terminal (with GUI enabled), not from a headless shell.

## Configure Lucia CLI integration

From the app menu: `Tools -> Settings`.

- **Lucia project root**: folder where `main.py` of Lucia lives (for example `../lucia`).
- **Python executable**: executable path, for example `.venv/bin/python`.

The app executes:

```bash
<python_executable> main.py run <current_file>
```

using the Lucia project root as working directory.

## Add a new language

Add a new resource bundle file under `src/main/resources/i18n`:

- `messages_<locale>.properties`

Then add a language item in `MainFrame`.
