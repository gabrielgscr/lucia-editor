package com.lucia.editor.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public final class EditorConfig {

    private static final String NODE = "com/lucia/editor";
    private static final String KEY_LUCIA_ROOT = "lucia.root";
    private static final String KEY_PYTHON_EXEC = "python.exec";
    private static final String KEY_EDITOR_FONT_SIZE = "editor.font.size";
    private static final String KEY_DARK_THEME = "ui.dark.theme";
    private static final String KEY_LANGUAGE = "ui.language";
    private static final String KEY_FORMAT_ON_SAVE = "editor.format.on.save";
    private static final String KEY_WORD_WRAP = "editor.word.wrap";
    private static final String KEY_TAB_SIZE = "editor.tab.size";
    private static final String KEY_INSERT_SPACES = "editor.insert.spaces";
    private static final String KEY_SHOW_LINE_NUMBERS = "editor.show.line.numbers";
    private static final String KEY_HIGHLIGHT_CURRENT_LINE = "editor.highlight.current.line";
    private static final String KEY_SHOW_WHITESPACE = "editor.show.whitespace";
    private static final String KEY_AUTOCOMPLETE_ENABLED = "autocomplete.enabled";
    private static final String KEY_AUTOCOMPLETE_DELAY_MS = "autocomplete.delay.ms";
    private static final String KEY_SNIPPETS_IN_AUTOCOMPLETE = "snippets.in.autocomplete";
    private static final String KEY_DIAGNOSTICS_ENABLED = "diagnostics.enabled";
    private static final String KEY_DIAGNOSTICS_DEBOUNCE_MS = "diagnostics.debounce.ms";
    private static final String KEY_DIAGNOSTICS_UNDERLINE_ROWS = "diagnostics.underline.rows";
    private static final String KEY_DIAGNOSTICS_AUTO_OPEN_PROBLEMS = "diagnostics.auto.open.problems";
    private static final String KEY_SAVE_BEFORE_RUN = "run.save.before.run";
    private static final String KEY_SHOW_GENERATED_CODE = "run.show.generated.code";
    private static final String KEY_CLEAR_OUTPUT_BEFORE_RUN = "run.clear.output.before.run";
    private static final String KEY_DEFAULT_TARGET = "run.default.target";
    private static final String KEY_TERMINAL_AUTO_START = "terminal.auto.start";
    private static final String KEY_TERMINAL_SHELL_PATH = "terminal.shell.path";
    private static final String KEY_TERMINAL_WORKING_DIR_MODE = "terminal.working.dir.mode";
    private static final String KEY_RECENT_PROJECT_PREFIX = "recent.project.";
    private static final int MAX_RECENT_PROJECTS = 8;

    private final Preferences preferences;

    public EditorConfig() {
        this.preferences = Preferences.userRoot().node(NODE);
    }

    public Path getLuciaProjectRoot() {
        String value = preferences.get(KEY_LUCIA_ROOT, "../lucia");
        return Paths.get(value).toAbsolutePath().normalize();
    }

    public void setLuciaProjectRoot(Path root) {
        preferences.put(KEY_LUCIA_ROOT, root.toString());
    }

    public String getPythonExecutable() {
        return preferences.get(KEY_PYTHON_EXEC, ".venv/bin/python");
    }

    public void setPythonExecutable(String value) {
        preferences.put(KEY_PYTHON_EXEC, value);
    }

    public int getEditorFontSize() {
        return preferences.getInt(KEY_EDITOR_FONT_SIZE, 15);
    }

    public void setEditorFontSize(int fontSize) {
        preferences.putInt(KEY_EDITOR_FONT_SIZE, fontSize);
    }

    public boolean isDarkTheme() {
        return preferences.getBoolean(KEY_DARK_THEME, false);
    }

    public void setDarkTheme(boolean dark) {
        preferences.putBoolean(KEY_DARK_THEME, dark);
    }

    public String getLanguageTag() {
        return preferences.get(KEY_LANGUAGE, "en");
    }

    public void setLanguageTag(String languageTag) {
        preferences.put(KEY_LANGUAGE, languageTag);
    }

    public boolean isFormatOnSave() {
        return preferences.getBoolean(KEY_FORMAT_ON_SAVE, false);
    }

    public void setFormatOnSave(boolean formatOnSave) {
        preferences.putBoolean(KEY_FORMAT_ON_SAVE, formatOnSave);
    }

    public boolean isWordWrap() {
        return preferences.getBoolean(KEY_WORD_WRAP, false);
    }

    public void setWordWrap(boolean value) {
        preferences.putBoolean(KEY_WORD_WRAP, value);
    }

    public int getTabSize() {
        return preferences.getInt(KEY_TAB_SIZE, 4);
    }

    public void setTabSize(int value) {
        preferences.putInt(KEY_TAB_SIZE, Math.max(2, Math.min(8, value)));
    }

    public boolean isInsertSpaces() {
        return preferences.getBoolean(KEY_INSERT_SPACES, true);
    }

    public void setInsertSpaces(boolean value) {
        preferences.putBoolean(KEY_INSERT_SPACES, value);
    }

    public boolean isShowLineNumbers() {
        return preferences.getBoolean(KEY_SHOW_LINE_NUMBERS, true);
    }

    public void setShowLineNumbers(boolean value) {
        preferences.putBoolean(KEY_SHOW_LINE_NUMBERS, value);
    }

    public boolean isHighlightCurrentLine() {
        return preferences.getBoolean(KEY_HIGHLIGHT_CURRENT_LINE, true);
    }

    public void setHighlightCurrentLine(boolean value) {
        preferences.putBoolean(KEY_HIGHLIGHT_CURRENT_LINE, value);
    }

    public boolean isShowWhitespace() {
        return preferences.getBoolean(KEY_SHOW_WHITESPACE, false);
    }

    public void setShowWhitespace(boolean value) {
        preferences.putBoolean(KEY_SHOW_WHITESPACE, value);
    }

    public boolean isAutocompleteEnabled() {
        return preferences.getBoolean(KEY_AUTOCOMPLETE_ENABLED, true);
    }

    public void setAutocompleteEnabled(boolean value) {
        preferences.putBoolean(KEY_AUTOCOMPLETE_ENABLED, value);
    }

    public int getAutocompleteDelayMs() {
        return preferences.getInt(KEY_AUTOCOMPLETE_DELAY_MS, 250);
    }

    public void setAutocompleteDelayMs(int value) {
        preferences.putInt(KEY_AUTOCOMPLETE_DELAY_MS, Math.max(50, Math.min(1500, value)));
    }

    public boolean isSnippetsInAutocomplete() {
        return preferences.getBoolean(KEY_SNIPPETS_IN_AUTOCOMPLETE, true);
    }

    public void setSnippetsInAutocomplete(boolean value) {
        preferences.putBoolean(KEY_SNIPPETS_IN_AUTOCOMPLETE, value);
    }

    public boolean isDiagnosticsEnabled() {
        return preferences.getBoolean(KEY_DIAGNOSTICS_ENABLED, true);
    }

    public void setDiagnosticsEnabled(boolean value) {
        preferences.putBoolean(KEY_DIAGNOSTICS_ENABLED, value);
    }

    public int getDiagnosticsDebounceMs() {
        return preferences.getInt(KEY_DIAGNOSTICS_DEBOUNCE_MS, 450);
    }

    public void setDiagnosticsDebounceMs(int value) {
        preferences.putInt(KEY_DIAGNOSTICS_DEBOUNCE_MS, Math.max(100, Math.min(2000, value)));
    }

    public int getDiagnosticsUnderlineRows() {
        return preferences.getInt(KEY_DIAGNOSTICS_UNDERLINE_ROWS, 2);
    }

    public void setDiagnosticsUnderlineRows(int value) {
        preferences.putInt(KEY_DIAGNOSTICS_UNDERLINE_ROWS, Math.max(1, Math.min(4, value)));
    }

    public boolean isDiagnosticsAutoOpenProblems() {
        return preferences.getBoolean(KEY_DIAGNOSTICS_AUTO_OPEN_PROBLEMS, false);
    }

    public void setDiagnosticsAutoOpenProblems(boolean value) {
        preferences.putBoolean(KEY_DIAGNOSTICS_AUTO_OPEN_PROBLEMS, value);
    }

    public boolean isSaveBeforeRun() {
        return preferences.getBoolean(KEY_SAVE_BEFORE_RUN, true);
    }

    public void setSaveBeforeRun(boolean value) {
        preferences.putBoolean(KEY_SAVE_BEFORE_RUN, value);
    }

    public boolean isShowGeneratedCode() {
        return preferences.getBoolean(KEY_SHOW_GENERATED_CODE, false);
    }

    public void setShowGeneratedCode(boolean value) {
        preferences.putBoolean(KEY_SHOW_GENERATED_CODE, value);
    }

    public boolean isClearOutputBeforeRun() {
        return preferences.getBoolean(KEY_CLEAR_OUTPUT_BEFORE_RUN, false);
    }

    public void setClearOutputBeforeRun(boolean value) {
        preferences.putBoolean(KEY_CLEAR_OUTPUT_BEFORE_RUN, value);
    }

    public String getDefaultTarget() {
        return preferences.get(KEY_DEFAULT_TARGET, "python");
    }

    public void setDefaultTarget(String value) {
        String normalized = "javascript".equalsIgnoreCase(value) || "js".equalsIgnoreCase(value)
                ? "javascript"
                : "python";
        preferences.put(KEY_DEFAULT_TARGET, normalized);
    }

    public boolean isTerminalAutoStart() {
        return preferences.getBoolean(KEY_TERMINAL_AUTO_START, false);
    }

    public void setTerminalAutoStart(boolean value) {
        preferences.putBoolean(KEY_TERMINAL_AUTO_START, value);
    }

    public String getTerminalShellPath() {
        return preferences.get(KEY_TERMINAL_SHELL_PATH, "");
    }

    public void setTerminalShellPath(String value) {
        preferences.put(KEY_TERMINAL_SHELL_PATH, value == null ? "" : value.trim());
    }

    public String getTerminalWorkingDirectoryMode() {
        return preferences.get(KEY_TERMINAL_WORKING_DIR_MODE, "project");
    }

    public void setTerminalWorkingDirectoryMode(String value) {
        String normalized = "home".equalsIgnoreCase(value) ? "home" : "project";
        preferences.put(KEY_TERMINAL_WORKING_DIR_MODE, normalized);
    }

    public List<Path> getRecentProjects() {
        List<Path> projects = new ArrayList<>();
        for (int i = 0; i < MAX_RECENT_PROJECTS; i++) {
            String value = preferences.get(KEY_RECENT_PROJECT_PREFIX + i, null);
            if (value == null || value.isBlank()) {
                continue;
            }
            projects.add(Paths.get(value).toAbsolutePath().normalize());
        }
        return projects;
    }

    public void addRecentProject(Path project) {
        Path normalized = project.toAbsolutePath().normalize();
        List<Path> projects = getRecentProjects();
        projects.removeIf(p -> p.equals(normalized));
        projects.add(0, normalized);

        while (projects.size() > MAX_RECENT_PROJECTS) {
            projects.remove(projects.size() - 1);
        }

        for (int i = 0; i < MAX_RECENT_PROJECTS; i++) {
            String key = KEY_RECENT_PROJECT_PREFIX + i;
            if (i < projects.size()) {
                preferences.put(key, projects.get(i).toString());
            } else {
                preferences.remove(key);
            }
        }
    }

    public void removeRecentProject(Path project) {
        Path normalized = project.toAbsolutePath().normalize();
        List<Path> projects = getRecentProjects();
        if (!projects.removeIf(p -> p.equals(normalized))) {
            return;
        }

        for (int i = 0; i < MAX_RECENT_PROJECTS; i++) {
            String key = KEY_RECENT_PROJECT_PREFIX + i;
            if (i < projects.size()) {
                preferences.put(key, projects.get(i).toString());
            } else {
                preferences.remove(key);
            }
        }
    }

    public void clearRecentProjects() {
        for (int i = 0; i < MAX_RECENT_PROJECTS; i++) {
            preferences.remove(KEY_RECENT_PROJECT_PREFIX + i);
        }
    }
}
