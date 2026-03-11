package com.lucia.editor.ui;

import com.lucia.editor.config.EditorConfig;
import com.lucia.editor.format.LuciaFormatter;
import com.lucia.editor.i18n.I18n;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

public class MainFrame extends JFrame {

    private static final int MIN_EDITOR_FONT_SIZE     = 10;
    private static final int MAX_EDITOR_FONT_SIZE     = 32;
    private static final int DEFAULT_EDITOR_FONT_SIZE = 15;

    private final EditorConfig config;
    private final EditorFactory editorFactory;
    private final LuciaRunner runner;
    private final ProjectTreePanel projectTreePanel;
    private final TerminalPanel terminalPanel;

    private Path projectRoot;
    private Path currentFile;

    private final JTabbedPane editorTabs;
    private final JTextArea output;
    private final JTextField inputField;
    private JToolBar toolBar;
    private JLabel statusLabel;
    private int editorFontSize;
    private boolean darkTheme;
    private final Map<Path, RSyntaxTextArea> openEditors;
    private final Map<Component, Path> tabToPath;
    private JTabbedPane bottomTabs;

    public MainFrame() {
        this.config         = new EditorConfig();
        this.editorFontSize = Math.max(MIN_EDITOR_FONT_SIZE,
                Math.min(MAX_EDITOR_FONT_SIZE, config.getEditorFontSize()));
        this.darkTheme      = config.isDarkTheme();
        this.openEditors    = new LinkedHashMap<>();
        this.tabToPath      = new HashMap<>();

        EditorFactory.registerLuciaSyntax();

        this.editorTabs       = new JTabbedPane();
        this.output           = new JTextArea(8, 100);
        this.inputField       = new JTextField();
        this.editorFactory    = new EditorFactory(darkTheme, editorFontSize);
        this.projectTreePanel = new ProjectTreePanel(this::openFile, this::appendOutput);
        this.terminalPanel    = new TerminalPanel();
        this.runner           = new LuciaRunner(config, this, inputField,
                this::appendOutput, this::appendOutputRaw);

        buildUi();
        refreshTexts();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                terminalPanel.destroyShell();
            }
        });
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(null);
    }

    // ── UI construction ────────────────────────────────────────────────

    private void buildUi() {
        editorTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        editorTabs.addChangeListener(e -> onTabChanged());
        editorTabs.putClientProperty("JTabbedPane.showTabSeparators", true);
        editorTabs.putClientProperty("JTabbedPane.tabInsets", new java.awt.Insets(4, 8, 4, 8));

        output.setEditable(false);
        output.setFont(output.getFont().deriveFont(Font.PLAIN, 13f));

        inputField.setEnabled(false);
        inputField.setFont(output.getFont());

        JPanel inputBar = new JPanel(new BorderLayout(4, 0));
        inputBar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        inputBar.add(new JLabel("  >"), BorderLayout.WEST);
        inputBar.add(inputField, BorderLayout.CENTER);

        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.add(new JScrollPane(output), BorderLayout.CENTER);
        outputPanel.add(inputBar, BorderLayout.SOUTH);

        bottomTabs = new JTabbedPane(JTabbedPane.BOTTOM);
        bottomTabs.addTab(I18n.tr("tab.output"), outputPanel);
        bottomTabs.addTab(I18n.tr("tab.terminal"), terminalPanel);
        bottomTabs.putClientProperty("JTabbedPane.showTabSeparators", true);
        bottomTabs.addChangeListener(e -> {
            if (bottomTabs.getSelectedIndex() == 1) {
                terminalPanel.ensureShellStarted();
            }
        });

        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                editorTabs, bottomTabs);
        rightSplit.setResizeWeight(0.8);
        rightSplit.setDividerSize(10);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(projectTreePanel.getTree()), rightSplit);
        mainSplit.setResizeWeight(0.25);
        mainSplit.setDividerSize(10);

        setLayout(new BorderLayout());
        toolBar = buildToolBar();
        add(toolBar, BorderLayout.NORTH);
        add(mainSplit, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
        setJMenuBar(buildMenu());

        var rootInputMap  = getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        var rootActionMap = getRootPane().getActionMap();

        rootInputMap.put(KeyStroke.getKeyStroke("control S"), "saveFile");
        rootActionMap.put("saveFile", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { saveCurrentFile(); }
        });
        rootInputMap.put(KeyStroke.getKeyStroke("control EQUALS"), "fontIncrease");
        rootInputMap.put(KeyStroke.getKeyStroke("control shift EQUALS"), "fontIncrease");
        rootActionMap.put("fontIncrease", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { changeEditorFontSize(1); }
        });
        rootInputMap.put(KeyStroke.getKeyStroke("control MINUS"), "fontDecrease");
        rootActionMap.put("fontDecrease", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { changeEditorFontSize(-1); }
        });
        rootInputMap.put(KeyStroke.getKeyStroke("control 0"), "fontReset");
        rootActionMap.put("fontReset", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { setEditorFontSize(DEFAULT_EDITOR_FONT_SIZE); }
        });
        rootInputMap.put(KeyStroke.getKeyStroke("control shift F"), "formatDocument");
        rootActionMap.put("formatDocument", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { formatCurrentDocument(); }
        });
    }

    private JToolBar buildToolBar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        bar.add(createToolbarButton(I18n.tr("menu.openProject"),   FontAwesomeSolid.FOLDER_OPEN,  this::chooseProject));
        bar.add(createToolbarButton(I18n.tr("menu.newFile"),       FontAwesomeSolid.FILE_ALT,     projectTreePanel::createLuciaFile));
        bar.add(createToolbarButton(I18n.tr("menu.newFolder"),     FontAwesomeSolid.FOLDER_PLUS,  projectTreePanel::createFolder));
        bar.addSeparator();
        bar.add(createToolbarButton(I18n.tr("menu.save"),          FontAwesomeSolid.SAVE,         this::saveCurrentFile));
        bar.add(createToolbarButton(I18n.tr("menu.saveAll"),       FontAwesomeSolid.COPY,         this::saveAllOpenFiles));
        bar.addSeparator();
        bar.add(createToolbarButton(I18n.tr("menu.runCurrent"),    FontAwesomeSolid.PLAY,         this::runCurrentFile));
        bar.add(createToolbarButton(I18n.tr("menu.compileCurrent"),FontAwesomeSolid.COG,          this::compileCurrentFile));
        bar.add(createToolbarButton(I18n.tr("menu.runTests"),      FontAwesomeSolid.VIAL,         runner::runTests));
        bar.add(createToolbarButton(I18n.tr("menu.formatDocument"),FontAwesomeSolid.MAGIC,        this::formatCurrentDocument));
        bar.addSeparator();
        bar.add(createToolbarButton(I18n.tr("menu.fontDecrease"),  FontAwesomeSolid.SEARCH_MINUS, () -> changeEditorFontSize(-1), "A-"));
        bar.add(createToolbarButton(I18n.tr("menu.fontIncrease"),  FontAwesomeSolid.SEARCH_PLUS,  () -> changeEditorFontSize(1),  "A+"));
        bar.addSeparator();
        bar.add(createToolbarButton(I18n.tr("menu.settings"),      FontAwesomeSolid.SLIDERS_H,    this::openSettings));
        return bar;
    }

    private JButton createToolbarButton(String tooltip, Ikon iconCode, Runnable action) {
        return createToolbarButton(tooltip, iconCode, action, null);
    }

    private JButton createToolbarButton(String tooltip, Ikon iconCode, Runnable action, String text) {
        JButton button = new JButton(createIcon(iconCode));
        if (text != null && !text.isBlank()) button.setText(text);
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.addActionListener(e -> action.run());
        return button;
    }

    private Icon createIcon(Ikon iconCode) {
        return FontIcon.of(iconCode, 14, resolveIconColor());
    }

    private Color resolveIconColor() {
        Color fg = UIManager.getColor("Label.foreground");
        if (fg == null) return Color.DARK_GRAY;
        double luminance = 0.2126 * fg.getRed() + 0.7152 * fg.getGreen() + 0.0722 * fg.getBlue();
        return luminance > 180 ? new Color(200, 200, 200) : fg;
    }

    private Component buildStatusBar() {
        statusLabel = new JLabel();
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        updateStatusBar();
        return statusLabel;
    }

    private JMenuBar buildMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu(I18n.tr("menu.file"));
        fileMenu.add(createMenuItem("menu.openProject",    FontAwesomeSolid.FOLDER_OPEN, this::chooseProject));
        fileMenu.add(buildRecentProjectsMenu());
        fileMenu.add(createMenuItem("menu.newFile",        FontAwesomeSolid.FILE_ALT,    projectTreePanel::createLuciaFile));
        fileMenu.add(createMenuItem("menu.newFolder",      FontAwesomeSolid.FOLDER_PLUS, projectTreePanel::createFolder));
        fileMenu.add(createMenuItem("menu.refreshProject", FontAwesomeSolid.SYNC_ALT,    projectTreePanel::rebuildTree));
        fileMenu.addSeparator();
        fileMenu.add(createMenuItem("menu.save",           FontAwesomeSolid.SAVE,        this::saveCurrentFile));
        fileMenu.add(createMenuItem("menu.saveAll",        FontAwesomeSolid.COPY,        this::saveAllOpenFiles));
        fileMenu.add(createMenuItem("menu.closeTab",       FontAwesomeSolid.TIMES,       this::closeCurrentTab));
        fileMenu.addSeparator();
        fileMenu.add(createMenuItem("menu.exit",           FontAwesomeSolid.SIGN_OUT_ALT, this::dispose));

        JMenu runMenu = new JMenu(I18n.tr("menu.run"));
        runMenu.add(createMenuItem("menu.runCurrent",    FontAwesomeSolid.PLAY,     this::runCurrentFile));
        runMenu.add(createMenuItem("menu.compileCurrent",FontAwesomeSolid.COG,      this::compileCurrentFile));
        runMenu.addSeparator();
        runMenu.add(createMenuItem("menu.runTests",      FontAwesomeSolid.VIAL,     runner::runTests));
        runMenu.add(createMenuItem("menu.runCustom",     FontAwesomeSolid.TERMINAL, runner::runCustom));
        runMenu.addSeparator();
        JMenuItem openTerminal = createMenuItem("menu.openTerminal",
                FontAwesomeSolid.TERMINAL, this::focusTerminal);
        openTerminal.setAccelerator(KeyStroke.getKeyStroke("control BACK_QUOTE"));
        runMenu.add(openTerminal);
        runMenu.add(createMenuItem("menu.restartTerminal",
                FontAwesomeSolid.SYNC_ALT, terminalPanel::startShellProcess));

        JMenu viewMenu = new JMenu(I18n.tr("menu.view"));
        JMenuItem fontDecrease = createMenuItem("menu.fontDecrease",
                FontAwesomeSolid.SEARCH_MINUS, () -> changeEditorFontSize(-1));
        fontDecrease.setAccelerator(KeyStroke.getKeyStroke("control MINUS"));
        JMenuItem fontIncrease = createMenuItem("menu.fontIncrease",
                FontAwesomeSolid.SEARCH_PLUS, () -> changeEditorFontSize(1));
        fontIncrease.setAccelerator(KeyStroke.getKeyStroke("control EQUALS"));
        JMenuItem fontReset = createMenuItem("menu.fontReset",
                FontAwesomeSolid.TEXT_HEIGHT, () -> setEditorFontSize(DEFAULT_EDITOR_FONT_SIZE));
        fontReset.setAccelerator(KeyStroke.getKeyStroke("control 0"));
        JCheckBoxMenuItem darkModeItem = new JCheckBoxMenuItem(
                I18n.tr("menu.darkTheme"), createIcon(FontAwesomeSolid.MOON));
        darkModeItem.setSelected(darkTheme);
        darkModeItem.addActionListener(e -> toggleTheme(darkModeItem.isSelected()));
        viewMenu.add(fontDecrease);
        viewMenu.add(fontIncrease);
        viewMenu.add(fontReset);
        viewMenu.addSeparator();
        viewMenu.add(darkModeItem);

        JMenu settingsMenu = new JMenu(I18n.tr("menu.tools"));
        JMenuItem formatDocument = createMenuItem("menu.formatDocument",
            FontAwesomeSolid.MAGIC, this::formatCurrentDocument);
        formatDocument.setAccelerator(KeyStroke.getKeyStroke("control shift F"));
        JCheckBoxMenuItem formatOnSaveItem = new JCheckBoxMenuItem(
            I18n.tr("menu.formatOnSave"), config.isFormatOnSave());
        formatOnSaveItem.addActionListener(e -> toggleFormatOnSave(formatOnSaveItem.isSelected()));
        settingsMenu.add(formatDocument);
        settingsMenu.add(formatOnSaveItem);
        settingsMenu.addSeparator();
        settingsMenu.add(createMenuItem("menu.settings", FontAwesomeSolid.SLIDERS_H, this::openSettings));

        JMenu languageMenu = new JMenu(I18n.tr("menu.language"));
        JMenuItem spanish = new JMenuItem(I18n.tr("language.spanish"), createIcon(FontAwesomeSolid.FLAG));
        spanish.addActionListener(e -> changeLanguage(Locale.forLanguageTag("es")));
        JMenuItem english = new JMenuItem(I18n.tr("language.english"), createIcon(FontAwesomeSolid.FLAG_CHECKERED));
        english.addActionListener(e -> changeLanguage(Locale.ENGLISH));
        languageMenu.add(spanish);
        languageMenu.add(english);

        JMenu helpMenu = new JMenu(I18n.tr("menu.help"));
        helpMenu.add(createMenuItem("menu.about", FontAwesomeSolid.INFO_CIRCLE,
                () -> AboutDialog.show(this, darkTheme)));

        menuBar.add(fileMenu);
        menuBar.add(runMenu);
        menuBar.add(viewMenu);
        menuBar.add(settingsMenu);
        menuBar.add(languageMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private JMenu buildRecentProjectsMenu() {
        JMenu recentMenu = new JMenu(I18n.tr("menu.openRecent"));
        List<Path> recentProjects = config.getRecentProjects();
        if (recentProjects.isEmpty()) {
            JMenuItem empty = new JMenuItem(I18n.tr("menu.noRecentProjects"));
            empty.setEnabled(false);
            recentMenu.add(empty);
            return recentMenu;
        }
        for (Path path : recentProjects) {
            JMenuItem item = new JMenuItem(path.toString(), createIcon(FontAwesomeSolid.FOLDER));
            item.addActionListener(e -> openProject(path));
            recentMenu.add(item);
        }
        recentMenu.addSeparator();
        JMenuItem clearRecent = new JMenuItem(I18n.tr("menu.clearRecentProjects"),
                createIcon(FontAwesomeSolid.TRASH));
        clearRecent.addActionListener(e -> {
            config.clearRecentProjects();
            appendOutput(I18n.tr("log.recentProjectsCleared"));
            refreshTexts();
        });
        recentMenu.add(clearRecent);
        return recentMenu;
    }

    private JMenuItem createMenuItem(String labelKey, Ikon icon, Runnable action) {
        JMenuItem item = new JMenuItem(I18n.tr(labelKey), createIcon(icon));
        item.addActionListener(e -> action.run());
        return item;
    }

    // ── Project / file operations ──────────────────────────────────────

    private void chooseProject() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION
                && chooser.getSelectedFile() != null) {
            openProject(chooser.getSelectedFile().toPath());
        }
    }

    private void openProject(Path path) {
        if (path == null || !Files.isDirectory(path)) {
            showError(I18n.tr("error.invalidProjectFolder") + ": " + path);
            if (path != null) config.removeRecentProject(path);
            refreshTexts();
            return;
        }
        projectRoot = path.toAbsolutePath().normalize();
        config.addRecentProject(projectRoot);
        projectTreePanel.setProjectRoot(projectRoot);
        terminalPanel.setProjectRoot(projectRoot);
        appendOutput(I18n.tr("log.projectOpened") + ": " + projectRoot);
        updateStatusBar();
        refreshTexts();
    }

    private void openFile(Path path) {
        if (openEditors.containsKey(path)) {
            selectTabByPath(path);
            return;
        }
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            RSyntaxTextArea editor = new RSyntaxTextArea(30, 100);
            editorFactory.configure(editor);
            editor.setText(content);

            RTextScrollPane editorScroll = new RTextScrollPane(editor);
            editorScroll.setFoldIndicatorEnabled(true);
            String tabTitle = path.getFileName().toString();
            editorTabs.addTab(tabTitle, editorScroll);

            int tabIndex = editorTabs.indexOfComponent(editorScroll);
            if (tabIndex >= 0) {
                editorTabs.setToolTipTextAt(tabIndex, path.toAbsolutePath().toString());
                editorTabs.setTabComponentAt(tabIndex,
                        new ClosableTabHeader(editorTabs, tabTitle, () -> closeTab(editorScroll)));
            }
            openEditors.put(path, editor);
            tabToPath.put(editorScroll, path);
            editorTabs.setSelectedComponent(editorScroll);
            currentFile = path;
            setTitle(I18n.tr("app.title") + " - " + path.getFileName());
            updateStatusBar();
        } catch (IOException ex) {
            showError(I18n.tr("error.openFile") + ": " + ex.getMessage());
        }
    }

    private void saveCurrentFile() {
        Path file = getCurrentFileFromTab();
        RSyntaxTextArea editor = getCurrentEditor();
        if (file == null || editor == null) { showError(I18n.tr("error.noFile")); return; }
        try {
            maybeFormatOnSave(editor);
            Files.writeString(file, editor.getText(), StandardCharsets.UTF_8);
            appendOutput(I18n.tr("log.saved") + ": " + file);
        } catch (IOException ex) {
            showError(I18n.tr("error.saveFile") + ": " + ex.getMessage());
        }
    }

    private void saveAllOpenFiles() {
        for (Map.Entry<Path, RSyntaxTextArea> entry : openEditors.entrySet()) {
            try {
                maybeFormatOnSave(entry.getValue());
                Files.writeString(entry.getKey(), entry.getValue().getText(), StandardCharsets.UTF_8);
            } catch (IOException ex) {
                showError(I18n.tr("error.saveFile") + ": " + ex.getMessage());
                return;
            }
        }
        appendOutput(I18n.tr("log.savedAll"));
    }

    private void closeCurrentTab() {
        Component selected = editorTabs.getSelectedComponent();
        if (selected != null) closeTab(selected);
    }

    private void closeTab(Component tabContent) {
        Path path = tabToPath.remove(tabContent);
        if (path != null) openEditors.remove(path);
        editorTabs.remove(tabContent);
        onTabChanged();
    }

    // ── Run / compile ──────────────────────────────────────────────────

    private void runCurrentFile() {
        Path file = getCurrentFileFromTab();
        if (file == null) { showError(I18n.tr("error.noFile")); return; }
        saveCurrentFile();
        runner.run(file);
    }

    private void compileCurrentFile() {
        Path file = getCurrentFileFromTab();
        if (file == null) { showError(I18n.tr("error.noFile")); return; }
        saveCurrentFile();
        runner.compile(file);
    }

    private void focusTerminal() {
        bottomTabs.setSelectedIndex(1);
        terminalPanel.focusTerminal();
    }

    private void formatCurrentDocument() {
        RSyntaxTextArea editor = getCurrentEditor();
        if (editor == null) {
            showError(I18n.tr("error.noFile"));
            return;
        }
        boolean changed = formatEditor(editor);
        appendOutput(changed ? I18n.tr("log.documentFormatted") : I18n.tr("log.documentAlreadyFormatted"));
    }

    private void maybeFormatOnSave(RSyntaxTextArea editor) {
        if (config.isFormatOnSave()) {
            formatEditor(editor);
        }
    }

    private boolean formatEditor(RSyntaxTextArea editor) {
        String original = editor.getText();
        String formatted = LuciaFormatter.format(original);
        if (formatted.equals(original)) {
            return false;
        }
        int caret = Math.min(editor.getCaretPosition(), formatted.length());
        editor.setText(formatted);
        editor.setCaretPosition(caret);
        return true;
    }

    private void toggleFormatOnSave(boolean enabled) {
        config.setFormatOnSave(enabled);
        appendOutput(enabled
                ? I18n.tr("log.formatOnSaveEnabled")
                : I18n.tr("log.formatOnSaveDisabled"));
    }

    // ── Tab / editor helpers ───────────────────────────────────────────

    private void onTabChanged() {
        Path file = getCurrentFileFromTab();
        currentFile = file;
        setTitle(file == null
                ? I18n.tr("app.title")
                : I18n.tr("app.title") + " - " + file.getFileName());
        updateStatusBar();
    }

    private RSyntaxTextArea getCurrentEditor() {
        Path file = getCurrentFileFromTab();
        return file == null ? null : openEditors.get(file);
    }

    private Path getCurrentFileFromTab() {
        Component selected = editorTabs.getSelectedComponent();
        return selected == null ? null : tabToPath.get(selected);
    }

    private void selectTabByPath(Path path) {
        for (Map.Entry<Component, Path> entry : tabToPath.entrySet()) {
            if (entry.getValue().equals(path)) {
                editorTabs.setSelectedComponent(entry.getKey());
                currentFile = path;
                return;
            }
        }
    }

    // ── View / theme ───────────────────────────────────────────────────

    private void changeEditorFontSize(int delta) {
        setEditorFontSize(editorFontSize + delta);
    }

    private void setEditorFontSize(int newSize) {
        int clamped = Math.max(MIN_EDITOR_FONT_SIZE, Math.min(MAX_EDITOR_FONT_SIZE, newSize));
        if (clamped == editorFontSize) return;
        editorFontSize = clamped;
        config.setEditorFontSize(editorFontSize);
        editorFactory.setEditorFontSize(editorFontSize);
        openEditors.values().forEach(editorFactory::applyFontSize);
        updateStatusBar();
        appendOutput(I18n.tr("log.fontSizeChanged") + ": " + editorFontSize);
    }

    private void openSettings() {
        SettingsDialog dialog = new SettingsDialog(this, config);
        dialog.setVisible(true);
        if (dialog.isSaved()) appendOutput(I18n.tr("log.settingsSaved"));
    }

    private void toggleTheme(boolean dark) {
        darkTheme = dark;
        config.setDarkTheme(dark);
        try {
            if (dark) FlatDarkLaf.setup(); else FlatLightLaf.setup();
        } catch (Exception ignored) {}
        SwingUtilities.updateComponentTreeUI(this);
        pack();
        setSize(Math.max(getWidth(), 1000), Math.max(getHeight(), 700));
        editorFactory.setDarkTheme(dark);
        openEditors.values().forEach(editor -> {
            editorFactory.applyTheme(editor);
            editorFactory.applyFontSize(editor);
        });
        refreshTexts();
        projectTreePanel.getTree().setCellRenderer(new FileTreeCellRenderer());
        SwingUtilities.updateComponentTreeUI(projectTreePanel.getTree());
    }

    private void changeLanguage(Locale locale) {
        I18n.setLocale(locale);
        refreshTexts();
    }

    private void refreshTexts() {
        onTabChanged();
        setJMenuBar(buildMenu());
        remove(toolBar);
        toolBar = buildToolBar();
        add(toolBar, BorderLayout.NORTH);
        updateStatusBar();
        revalidate();
        repaint();
    }

    private void updateStatusBar() {
        if (statusLabel == null) return;
        String project = projectRoot == null
                ? I18n.tr("status.none") : projectRoot.getFileName().toString();
        String file = currentFile == null
                ? I18n.tr("status.none") : currentFile.getFileName().toString();
        statusLabel.setText(
                I18n.tr("status.project")  + ": " + project  + "   |   "
                + I18n.tr("status.file")   + ": " + file     + "   |   "
                + I18n.tr("status.language") + ": " + I18n.getLocale().getLanguage() + "   |   "
                + I18n.tr("status.font")   + ": " + editorFontSize);
    }

    // ── Output panel helpers ───────────────────────────────────────────

    private void appendOutput(String line) {
        output.append(line + System.lineSeparator());
        output.setCaretPosition(output.getDocument().getLength());
    }

    private void appendOutputRaw(String text) {
        output.append(text);
        output.setCaretPosition(output.getDocument().getLength());
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, I18n.tr("dialog.error"),
                JOptionPane.ERROR_MESSAGE);
        appendOutput(I18n.tr("log.error") + ": " + message);
    }
}
