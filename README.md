# Lucia Editor

Lucia Editor is a desktop IDE-style editor for Lucia language projects, built with Java Swing.

## Highlights

- 📁 Project explorer for `.lucia` files and folders.
- 🗂️ Multi-tab editing with close buttons and persisted recent projects.
- 🎨 Light/dark theme support with theme-aware icons.
- 🔤 Lucia syntax highlighting + code folding.
- ⚡ Auto-completion for keywords, types, built-ins, and snippets.
- 🔎 Local find/replace (`Ctrl+F`, `Ctrl+H`, `F3`, `Shift+F3`).
- 🌍 Global search and replace in `.lucia` files (`Ctrl+Shift+G`) with:
	- Folder filter.
	- Regex and case-sensitive options.
	- Result preview and jump-to-location.
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

## Keyboard Shortcuts

- `Ctrl+S`: Save current file
- `Ctrl+Shift+S`: Save all
- `Ctrl+N`: New Lucia file
- `Ctrl+Shift+N`: New folder
- `Ctrl+O`: Open project
- `Ctrl+W`: Close current tab
- `F5`: Run current file
- `F6`: Compile current file
- `Ctrl+```: Open terminal tab
- `Ctrl+F`: Find in current editor
- `Ctrl+H`: Replace in current editor
- `F3` / `Shift+F3`: Next/Previous match
- `Ctrl+Shift+G`: Global search/replace dialog
- `F12`: Go to Definition
- `Shift+F12`: Find References
- `Ctrl+Shift+F`: Format document
- `Ctrl+-` / `Ctrl+=` / `Ctrl+0`: Font size controls

## Stack

- `FlatLaf`
- `RSyntaxTextArea`
- `AutoComplete`
- `RSTAUI`
- `Ikonli FontAwesome5`

## RSyntaxTextArea Ecosystem Usage

Lucia Editor is built around the RSyntaxTextArea ecosystem for editing, completion, and search UX.

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

The editor executes commands from that configured root, for example:

```bash
<python_executable> main.py run <current_file>
```

## Add More UI Languages

Create a new i18n bundle under `src/main/resources/i18n`:

- `messages_<locale>.properties`

Then register/select it from the language menu wiring in `MainFrame`.
