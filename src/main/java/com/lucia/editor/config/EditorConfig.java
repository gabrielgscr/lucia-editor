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
