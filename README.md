# Lucia Editor

Lucia Editor is a desktop IDE-style editor for Lucia language projects, built with Java Swing.

## Highlights

- 📁 Project explorer for `.lucia` files and folders.
- 🗂️ Multi-tab editing with close buttons and persisted recent projects.
- 🎨 Light/dark theme support with theme-aware icons.
- 🔤 Lucia syntax highlighting + code folding.
- ⚡ Auto-completion for keywords, types, built-ins, and snippets.
- 🧩 Snippet manager with user-editable templates (add/edit/delete without touching Java code).
- 🔎 Local find/replace (`Ctrl+F`, `Ctrl+H`, `F3`, `Shift+F3`).
- 🌍 Global search and replace in `.lucia` files (`Ctrl+Shift+G`) with:
	- Folder filter.
	- Regex and case-sensitive options.
	- Result preview and jump-to-location.
- 🩺 Real-time diagnostics (syntax/semantic) while typing with debounced background analysis.
- 🚨 Problems panel with navigable entries (double click / `Enter`) and per-file counters.
- 🛠️ Quick fixes (`Ctrl+.`) for common issues, including context menu action in Problems panel.
- 🧭 Go to Definition / Find References (`F12`, `Shift+F12`) for:
	- `func`
	- `class`
	- `let` variables
- 🧹 Code formatter + `Ctrl+Shift+F` + optional format-on-save.
- 🖥️ Integrated terminal panel.
- ▶️ Lucia CLI integration (run/compile/tests/custom command).
- 🌐 Internationalized UI (Spanish/English).

## Lucia Language Support

- Custom `RSyntaxTextArea` token maker (`text/lucia`).
- Keyword, type, literals, comments, and operator highlighting.
- Built-in functions highlighted:
	- `print`, `len`, `input`, `str`, `type_of`, `abs`, `min`, `max`, `pow`, `round`, `floor`, `ceil`, `random`
- Code folding with curly blocks.

## Snippet Management

Snippets are no longer hardcoded in Java source. Lucia Editor now loads them from a user-managed storage file.

- Open manager from: `Tools -> Manage snippets`
- Supported actions:
	- Create snippet
	- Update snippet
	- Delete snippet
	- Open the snippets storage file
- Changes are reloaded in autocomplete immediately.

### Storage format

- File path: `~/.lucia-editor/snippets.properties`
- Internal module:
	- `src/main/java/com/lucia/editor/snippets/SnippetManager.java`
	- `src/main/java/com/lucia/editor/snippets/SnippetDefinition.java`
- Management UI:
	- `src/main/java/com/lucia/editor/ui/SnippetManagerDialog.java`

On first run, the file is auto-created with default snippets (`func`, `class`, `if`, `ifelse`, `for`, `while`).

## Keyboard Shortcuts

- `Ctrl+Shift+I`: Insert snippet picker
- `Ctrl+Shift+G`: Global search in project
- `F12`: Go to definition
- `Shift+F12`: Find references
- `Alt+P`: Open Problems panel
- `Ctrl+.`: Apply quick fix (selected problem or first problem in current file)

## Stack

- `FlatLaf`
- `RSyntaxTextArea`
- `Ikonli FontAwesome5`

## RSyntaxTextArea Ecosystem Usage


- `RSyntaxTextArea`
	- Main text editor component for each tab.
	- Lucia-specific syntax style is configured as `text/lucia`.
	- Integration reference: `src/main/java/com/lucia/editor/ui/EditorFactory.java`
- `RTextScrollPane`
	- Wraps each editor tab and enables fold indicators.
	- Integration reference: `src/main/java/com/lucia/editor/ui/MainFrame.java`
- `TokenMaker` (`LuciaTokenMaker`)
	- Custom lexical highlighting for keywords, types, literals, comments, and separators/operators.
	- Integration reference: `src/main/java/com/lucia/editor/syntax/LuciaTokenMaker.java`
- `FoldParser` (`CurlyFoldParser`)
	- Enables code folding based on curly blocks.
	- Integration reference: `src/main/java/com/lucia/editor/ui/EditorFactory.java`
- `AutoComplete` (`org.fife.ui.autocomplete`)
	- Provides language keywords, built-in functions, and snippets (`TemplateCompletion` / `ShorthandCompletion`).
	- Integration reference: `src/main/java/com/lucia/editor/ui/EditorFactory.java`
- `RSTAUI Search` (`FindDialog`, `ReplaceDialog`, `SearchEngine`)
	- Powers in-editor find/replace behavior.
	- Integration references:
		- `src/main/java/com/lucia/editor/ui/EditorSearchSupport.java`
		- `src/main/java/com/lucia/editor/ui/MainFrame.java`
- `RSTAUI Custom Global Search UI`
	- Project-wide `.lucia` search/replace dialog with filters and previews.
	- Integration reference: `src/main/java/com/lucia/editor/ui/GlobalSearchDialog.java`
- `Real-time Diagnostics`
	- Runs Lucia analysis in the background and returns structured diagnostics.
	- Paints warning/error underlines and feeds the Problems panel.
	- Integration references:
		- `src/main/java/com/lucia/editor/ui/diagnostics/LuciaDiagnosticsService.java`
		- `src/main/java/com/lucia/editor/ui/diagnostics/ProblemsPanel.java`
		- `src/main/java/com/lucia/editor/ui/MainFrame.java`

## Requirements

- OpenJDK 25
- Maven 3.9+

## Run

```bash
mvn clean compile exec:java
```

If you are using VS Code installed via Snap and hit linker errors such as:

`/snap/core20/.../libpthread.so.0: undefined symbol: __libc_pthread_init`

run with:

```bash
./run-editor.sh
```

If you see `No X11 DISPLAY variable was set`, run from a GUI-enabled desktop terminal.

## Lucia CLI Configuration

Open: `Tools -> Settings`

- **Lucia project root**: folder containing Lucia `main.py` (for example `../lucia`).
- **Python executable**: interpreter path (for example `.venv/bin/python`).
- **UI settings**:
	- Language (`en`/`es`)
	- Theme (`light`/`dark`)
	- Editor font size
- **Editor behavior**:
	- Format on save
	- Word wrap
	- Tab size
	- Insert spaces instead of tabs
	- Show line numbers
	- Highlight current line
	- Show whitespace
- **Autocomplete**:
	- Enable/disable autocomplete
	- Autocomplete delay (ms)
	- Include/exclude snippets in autocomplete
- **Diagnostics**:
	- Enable/disable real-time diagnostics
	- Diagnostics debounce delay (ms)
	- Diagnostics underline thickness
	- Auto-open Problems panel on diagnostics
- **Run/Compile defaults**:
	- Save before run/compile
	- Show generated code
	- Clear output before run
	- Default target (`python` / `javascript`)
- **Terminal**:
	- Auto-start integrated terminal
	- Optional custom shell path
	- Startup directory mode (`project` / `home`)

The editor executes commands from that configured root, for example:

```bash
<python_executable> main.py run <current_file>
```

With run/compile target configuration enabled, the editor also forwards the selected target:

```bash
<python_executable> main.py run <current_file> --target <python|javascript>
```

## Add More UI Languages

Create a new i18n bundle under `src/main/resources/i18n`:

- `messages_<locale>.properties`

Then register/select it from the language menu wiring in `MainFrame`.
