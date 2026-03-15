/*
 * Copyright (c) 2026 Gabriel González
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.lucia.editor.ui;

import com.lucia.editor.config.EditorConfig;
import com.lucia.editor.format.LuciaFormatter;
import com.lucia.editor.i18n.I18n;
import com.lucia.editor.snippets.SnippetDefinition;
import com.lucia.editor.snippets.SnippetManager;
import com.lucia.editor.ui.diagnostics.LuciaDiagnostic;
import com.lucia.editor.ui.diagnostics.LuciaDiagnosticSeverity;
import com.lucia.editor.ui.diagnostics.LuciaDiagnosticsService;
import com.lucia.editor.ui.diagnostics.ProblemsPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
import javax.swing.DefaultListModel;
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
import javax.swing.JList;
import javax.swing.KeyStroke;
import javax.swing.text.Document;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.LayeredHighlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
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
    private final EditorSearchSupport searchSupport;
    private final LuciaSymbolNavigator symbolNavigator;
    private final SnippetManager snippetManager;
    private final LuciaDiagnosticsService diagnosticsService;
    private GlobalSearchDialog globalSearchDialog;

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
    private final Map<Path, RTextScrollPane> openScrollPanes;
    private final Map<Component, Path> tabToPath;
    private final Map<RSyntaxTextArea, DocumentListener> diagnosticsListeners;
    private final Map<RSyntaxTextArea, List<Object>> diagnosticHighlightTags;
    private final Map<Path, List<LuciaDiagnostic>> diagnosticsByFile;
    private JTabbedPane bottomTabs;
    private ProblemsPanel problemsPanel;
    private int diagnosticsUnderlineRows;

    public MainFrame() {
        this.config         = new EditorConfig();
        this.editorFontSize = Math.max(MIN_EDITOR_FONT_SIZE,
                Math.min(MAX_EDITOR_FONT_SIZE, config.getEditorFontSize()));
        this.darkTheme      = config.isDarkTheme();
        this.openEditors    = new LinkedHashMap<>();
        this.openScrollPanes = new LinkedHashMap<>();
        this.tabToPath      = new HashMap<>();

        EditorFactory.registerLuciaSyntax();

        this.editorTabs       = new JTabbedPane();
        this.output           = new JTextArea(8, 100);
        this.inputField       = new JTextField();
        this.snippetManager   = new SnippetManager();
        this.editorFactory    = new EditorFactory(darkTheme, editorFontSize, snippetManager);
        this.projectTreePanel = new ProjectTreePanel(new ProjectTreePanel.ProjectTreeActions() {
            @Override
            public void openFile(Path path) {
                MainFrame.this.openFile(path);
            }

            @Override
            public void runFile(Path path) {
                MainFrame.this.runFile(path);
            }

            @Override
            public void compileFile(Path path) {
                MainFrame.this.compileFile(path);
            }

            @Override
            public void onPathChanged(Path oldPath, Path newPath) {
                MainFrame.this.handleProjectPathChanged(oldPath, newPath);
            }

            @Override
            public void onPathDeleted(Path path) {
                MainFrame.this.handleProjectPathDeleted(path);
            }

            @Override
            public void onLog(String message) {
                MainFrame.this.appendOutput(message);
            }
        });
        this.terminalPanel    = new TerminalPanel();
        this.searchSupport    = new EditorSearchSupport(this, this::getCurrentEditor,
                this::appendOutput, this::showError);
        this.symbolNavigator  = new LuciaSymbolNavigator();
        this.diagnosticsService = new LuciaDiagnosticsService(config, this::onDiagnosticsReady);
        this.runner           = new LuciaRunner(config, this, inputField,
                this::appendOutput, this::appendOutputRaw);
        this.diagnosticsListeners = new HashMap<>();
        this.diagnosticHighlightTags = new HashMap<>();
        this.diagnosticsByFile = new HashMap<>();
        this.diagnosticsUnderlineRows = config.getDiagnosticsUnderlineRows();

        I18n.setLocale(Locale.forLanguageTag(config.getLanguageTag()));
        editorFactory.setAutocompleteEnabled(config.isAutocompleteEnabled());
        editorFactory.setAutocompleteDelayMs(config.getAutocompleteDelayMs());
        editorFactory.setSnippetsInAutocomplete(config.isSnippetsInAutocomplete());
        editorFactory.refreshSnippetCompletions();
        diagnosticsService.setDebounceMs(config.getDiagnosticsDebounceMs());
        terminalPanel.setAutoStart(config.isTerminalAutoStart());
        terminalPanel.setShellPath(config.getTerminalShellPath());
        terminalPanel.setWorkingDirectoryMode(config.getTerminalWorkingDirectoryMode());

        buildUi();
        refreshTexts();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                terminalPanel.destroyShell();
                diagnosticsService.shutdown();
            }
        });
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 700));

        java.net.URL appIcon = getClass().getResource("/lucia-editor.png");
        if (appIcon != null) {
            setIconImage(new javax.swing.ImageIcon(appIcon).getImage());
        }

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

        problemsPanel = new ProblemsPanel(this::openProblem, this::applyQuickFixFromProblem);
        problemsPanel.setDarkTheme(darkTheme);

        bottomTabs = new JTabbedPane(JTabbedPane.BOTTOM);
        bottomTabs.addTab(I18n.tr("tab.output"), outputPanel);
        bottomTabs.addTab(I18n.tr("tab.terminal"), terminalPanel);
        bottomTabs.addTab(I18n.tr("tab.problems") + " (0)", problemsPanel);
        bottomTabs.putClientProperty("JTabbedPane.showTabSeparators", true);
        bottomTabs.addChangeListener(e -> {
            if (bottomTabs.getSelectedIndex() == 1) {
                terminalPanel.ensureShellStarted();
            }
        });
        if (terminalPanel.isAutoStart()) {
            terminalPanel.ensureShellStarted();
        }

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
        rootInputMap.put(KeyStroke.getKeyStroke("control O"), "openProject");
        rootActionMap.put("openProject", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { chooseProject(); }
        });
        rootInputMap.put(KeyStroke.getKeyStroke("control N"), "newFile");
        rootActionMap.put("newFile", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { projectTreePanel.createLuciaFile(); }
        });
        rootInputMap.put(KeyStroke.getKeyStroke("control shift N"), "newFolder");
        rootActionMap.put("newFolder", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { projectTreePanel.createFolder(); }
        });
        rootInputMap.put(KeyStroke.getKeyStroke("control shift S"), "saveAll");
        rootActionMap.put("saveAll", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { saveAllOpenFiles(); }
        });
        rootInputMap.put(KeyStroke.getKeyStroke("control W"), "closeTab");
        rootActionMap.put("closeTab", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { closeCurrentTab(); }
        });
        rootInputMap.put(KeyStroke.getKeyStroke("F5"), "runCurrent");
        rootActionMap.put("runCurrent", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { runCurrentFile(); }
        });
        rootInputMap.put(KeyStroke.getKeyStroke("F6"), "compileCurrent");
        rootActionMap.put("compileCurrent", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { compileCurrentFile(); }
        });
        rootInputMap.put(KeyStroke.getKeyStroke("control BACK_QUOTE"), "openTerminal");
        rootActionMap.put("openTerminal", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { focusTerminal(); }
        });
        rootInputMap.put(KeyStroke.getKeyStroke("control F"), "findText");
        rootActionMap.put("findText", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { searchSupport.showFindDialog(); }
        });
        rootInputMap.put(KeyStroke.getKeyStroke("control H"), "replaceText");
        rootActionMap.put("replaceText", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { searchSupport.showReplaceDialog(); }
        });
        rootInputMap.put(KeyStroke.getKeyStroke("F3"), "findNext");
        rootActionMap.put("findNext", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { searchSupport.findNext(); }
        });
        rootInputMap.put(KeyStroke.getKeyStroke("shift F3"), "findPrevious");
        rootActionMap.put("findPrevious", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { searchSupport.findPrevious(); }
        });
        rootInputMap.put(KeyStroke.getKeyStroke("control shift G"), "globalSearch");
        rootActionMap.put("globalSearch", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { showGlobalSearch(); }
        });
        rootInputMap.put(KeyStroke.getKeyStroke("F12"), "goToDefinition");
        rootActionMap.put("goToDefinition", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { goToDefinition(); }
        });
        rootInputMap.put(KeyStroke.getKeyStroke("shift F12"), "findReferences");
        rootActionMap.put("findReferences", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { findReferences(); }
        });
        rootInputMap.put(KeyStroke.getKeyStroke("control shift I"), "insertSnippet");
        rootActionMap.put("insertSnippet", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { insertSnippet(); }
        });
        rootInputMap.put(KeyStroke.getKeyStroke("alt P"), "openProblems");
        rootActionMap.put("openProblems", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { openProblemsPanel(); }
        });
        rootInputMap.put(KeyStroke.getKeyStroke("control PERIOD"), "quickFix");
        rootActionMap.put("quickFix", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { triggerQuickFix(); }
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
        bar.add(createToolbarButton(I18n.tr("menu.runTests"),      FontAwesomeSolid.VIAL,         this::runTestsCommand));
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
        JMenuItem openProjectItem = createMenuItem("menu.openProject", FontAwesomeSolid.FOLDER_OPEN, this::chooseProject);
        openProjectItem.setAccelerator(KeyStroke.getKeyStroke("control O"));
        JMenuItem newFileItem = createMenuItem("menu.newFile", FontAwesomeSolid.FILE_ALT, projectTreePanel::createLuciaFile);
        newFileItem.setAccelerator(KeyStroke.getKeyStroke("control N"));
        JMenuItem newFolderItem = createMenuItem("menu.newFolder", FontAwesomeSolid.FOLDER_PLUS, projectTreePanel::createFolder);
        newFolderItem.setAccelerator(KeyStroke.getKeyStroke("control shift N"));
        JMenuItem saveItem = createMenuItem("menu.save", FontAwesomeSolid.SAVE, this::saveCurrentFile);
        saveItem.setAccelerator(KeyStroke.getKeyStroke("control S"));
        JMenuItem saveAllItem = createMenuItem("menu.saveAll", FontAwesomeSolid.COPY, this::saveAllOpenFiles);
        saveAllItem.setAccelerator(KeyStroke.getKeyStroke("control shift S"));
        JMenuItem closeTabItem = createMenuItem("menu.closeTab", FontAwesomeSolid.TIMES, this::closeCurrentTab);
        closeTabItem.setAccelerator(KeyStroke.getKeyStroke("control W"));

        fileMenu.add(openProjectItem);
        fileMenu.add(buildRecentProjectsMenu());
        fileMenu.add(newFileItem);
        fileMenu.add(newFolderItem);
        fileMenu.add(createMenuItem("menu.refreshProject", FontAwesomeSolid.SYNC_ALT, projectTreePanel::rebuildTree));
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.add(saveAllItem);
        fileMenu.add(closeTabItem);
        fileMenu.addSeparator();
        fileMenu.add(createMenuItem("menu.exit", FontAwesomeSolid.SIGN_OUT_ALT, this::dispose));

        JMenu runMenu = new JMenu(I18n.tr("menu.run"));
        JMenuItem runCurrentItem = createMenuItem("menu.runCurrent", FontAwesomeSolid.PLAY, this::runCurrentFile);
        runCurrentItem.setAccelerator(KeyStroke.getKeyStroke("F5"));
        JMenuItem compileCurrentItem = createMenuItem("menu.compileCurrent", FontAwesomeSolid.COG, this::compileCurrentFile);
        compileCurrentItem.setAccelerator(KeyStroke.getKeyStroke("F6"));
        runMenu.add(runCurrentItem);
        runMenu.add(compileCurrentItem);
        runMenu.addSeparator();
        runMenu.add(createMenuItem("menu.runTests",      FontAwesomeSolid.VIAL,     this::runTestsCommand));
        runMenu.add(createMenuItem("menu.runCustom",     FontAwesomeSolid.TERMINAL, this::runCustomCommand));
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

        JMenu searchMenu = new JMenu(I18n.tr("menu.search"));
        JMenuItem findItem = createMenuItem("menu.find", FontAwesomeSolid.SEARCH, searchSupport::showFindDialog);
        findItem.setAccelerator(KeyStroke.getKeyStroke("control F"));
        JMenuItem replaceItem = createMenuItem("menu.replace", FontAwesomeSolid.EXCHANGE_ALT, searchSupport::showReplaceDialog);
        replaceItem.setAccelerator(KeyStroke.getKeyStroke("control H"));
        JMenuItem findNextItem = createMenuItem("menu.findNext", FontAwesomeSolid.CHEVRON_DOWN, searchSupport::findNext);
        findNextItem.setAccelerator(KeyStroke.getKeyStroke("F3"));
        JMenuItem findPreviousItem = createMenuItem("menu.findPrevious", FontAwesomeSolid.CHEVRON_UP, searchSupport::findPrevious);
        findPreviousItem.setAccelerator(KeyStroke.getKeyStroke("shift F3"));
        JMenuItem findInProjectItem = createMenuItem("menu.findInProject", FontAwesomeSolid.GLOBE, this::showGlobalSearch);
        findInProjectItem.setAccelerator(KeyStroke.getKeyStroke("control shift G"));
        searchMenu.add(findItem);
        searchMenu.add(replaceItem);
        searchMenu.addSeparator();
        searchMenu.add(findNextItem);
        searchMenu.add(findPreviousItem);
        searchMenu.addSeparator();
        searchMenu.add(findInProjectItem);

        JMenu navigateMenu = new JMenu(I18n.tr("menu.navigate"));
        JMenuItem goToDefinitionItem = createMenuItem("menu.goToDefinition", FontAwesomeSolid.SEARCH, this::goToDefinition);
        goToDefinitionItem.setAccelerator(KeyStroke.getKeyStroke("F12"));
        JMenuItem findReferencesItem = createMenuItem("menu.findReferences", FontAwesomeSolid.LIST_UL, this::findReferences);
        findReferencesItem.setAccelerator(KeyStroke.getKeyStroke("shift F12"));
        JMenuItem openProblemsItem = createMenuItem("menu.openProblems", FontAwesomeSolid.EXCLAMATION_TRIANGLE,
            this::openProblemsPanel);
        openProblemsItem.setAccelerator(KeyStroke.getKeyStroke("alt P"));
        navigateMenu.add(goToDefinitionItem);
        navigateMenu.add(findReferencesItem);
        navigateMenu.addSeparator();
        navigateMenu.add(openProblemsItem);

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
        JMenuItem insertSnippetItem = createMenuItem("menu.insertSnippet", FontAwesomeSolid.PUZZLE_PIECE, this::insertSnippet);
        insertSnippetItem.setAccelerator(KeyStroke.getKeyStroke("control shift I"));
        settingsMenu.add(insertSnippetItem);
        settingsMenu.add(createMenuItem("menu.manageSnippets", FontAwesomeSolid.CODE, this::openSnippetManager));
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
        menuBar.add(searchMenu);
        menuBar.add(navigateMenu);
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
        Path normalized = path.toAbsolutePath().normalize();
        if (openEditors.containsKey(normalized)) {
            selectTabByPath(normalized);
            return;
        }
        try {
            String content = Files.readString(normalized, StandardCharsets.UTF_8);
            RSyntaxTextArea editor = new RSyntaxTextArea(30, 100);
            editorFactory.configure(editor);
            editor.setText(content);
            applyEditorPreferences(editor);
            attachDiagnostics(editor, normalized);
            requestDiagnostics(normalized, editor);

            RTextScrollPane editorScroll = new RTextScrollPane(editor);
            editorScroll.setFoldIndicatorEnabled(true);
            editorScroll.setLineNumbersEnabled(config.isShowLineNumbers());
            String tabTitle = normalized.getFileName().toString();
            editorTabs.addTab(tabTitle, editorScroll);

            int tabIndex = editorTabs.indexOfComponent(editorScroll);
            if (tabIndex >= 0) {
                editorTabs.setToolTipTextAt(tabIndex, normalized.toString());
                editorTabs.setTabComponentAt(tabIndex,
                        new ClosableTabHeader(editorTabs, tabTitle, () -> closeTab(editorScroll)));
            }
            openEditors.put(normalized, editor);
            openScrollPanes.put(normalized, editorScroll);
            tabToPath.put(editorScroll, normalized);
            editorTabs.setSelectedComponent(editorScroll);
            currentFile = normalized;
            setTitle(I18n.tr("app.title") + " - " + normalized.getFileName());
            updateStatusBar();
        } catch (IOException ex) {
            showError(I18n.tr("error.openFile") + ": " + ex.getMessage());
        }
    }

    private void saveCurrentFile() {
        Path file = getCurrentFileFromTab();
        RSyntaxTextArea editor = getCurrentEditor();
        if (file == null || editor == null) { showError(I18n.tr("error.noFile")); return; }
        saveEditorToFile(file, editor);
    }

    private void saveFile(Path file) {
        if (file == null) {
            showError(I18n.tr("error.noFile"));
            return;
        }
        Path normalized = file.toAbsolutePath().normalize();
        RSyntaxTextArea editor = openEditors.get(normalized);
        if (editor == null) {
            return;
        }
        saveEditorToFile(normalized, editor);
    }

    private void saveEditorToFile(Path file, RSyntaxTextArea editor) {
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
        if (path != null) {
            RSyntaxTextArea editor = openEditors.remove(path);
            openScrollPanes.remove(path);
            detachDiagnostics(editor);
            clearDiagnosticsFor(path);
        }
        editorTabs.remove(tabContent);
        onTabChanged();
    }

    private void attachDiagnostics(RSyntaxTextArea editor, Path file) {
        if (editor == null || file == null) {
            return;
        }
        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { requestDiagnostics(findPathForEditor(editor), editor); }

            @Override
            public void removeUpdate(DocumentEvent e) { requestDiagnostics(findPathForEditor(editor), editor); }

            @Override
            public void changedUpdate(DocumentEvent e) { requestDiagnostics(findPathForEditor(editor), editor); }
        };
        editor.getDocument().addDocumentListener(listener);
        diagnosticsListeners.put(editor, listener);
    }

    private void detachDiagnostics(RSyntaxTextArea editor) {
        if (editor == null) {
            return;
        }
        DocumentListener listener = diagnosticsListeners.remove(editor);
        if (listener != null) {
            editor.getDocument().removeDocumentListener(listener);
        }
        clearEditorHighlights(editor);
    }

    private void requestDiagnostics(Path file, RSyntaxTextArea editor) {
        if (file == null || editor == null) {
            return;
        }
        if (!config.isDiagnosticsEnabled()) {
            onDiagnosticsReady(file, List.of());
            return;
        }
        diagnosticsService.requestAnalysis(file, editor.getText());
    }

    private void onDiagnosticsReady(Path file, List<LuciaDiagnostic> diagnostics) {
        Path normalized = file.toAbsolutePath().normalize();
        diagnosticsByFile.put(normalized, diagnostics == null ? List.of() : List.copyOf(diagnostics));

        RSyntaxTextArea editor = openEditors.get(normalized);
        if (editor != null) {
            applyDiagnosticsToEditor(editor, diagnosticsByFile.get(normalized));
        }

        problemsPanel.setDiagnostics(normalized, diagnosticsByFile.get(normalized));
        updateProblemsTabTitle();

        if (config.isDiagnosticsAutoOpenProblems()
                && diagnosticsByFile.get(normalized) != null
                && !diagnosticsByFile.get(normalized).isEmpty()) {
            showProblemsTabWithoutFocusChange();
        }
    }

    private void clearDiagnosticsFor(Path file) {
        if (file == null) {
            return;
        }
        Path normalized = file.toAbsolutePath().normalize();
        diagnosticsService.clear(normalized);
        diagnosticsByFile.remove(normalized);
        problemsPanel.clearFile(normalized);
        updateProblemsTabTitle();
    }

    private void applyDiagnosticsToEditor(RSyntaxTextArea editor, List<LuciaDiagnostic> diagnostics) {
        clearEditorHighlights(editor);
        if (diagnostics == null || diagnostics.isEmpty()) {
            return;
        }

        List<Object> tags = new java.util.ArrayList<>();
        for (LuciaDiagnostic diagnostic : diagnostics) {
            try {
                int start = toOffset(editor, diagnostic.line(), diagnostic.column());
                int end = Math.min(start + Math.max(1, diagnostic.length()), editor.getDocument().getLength());
                if (end <= start) {
                    end = Math.min(start + 1, editor.getDocument().getLength());
                }
                var painter = diagnostic.severity() == LuciaDiagnosticSeverity.WARNING
                    ? new DiagnosticsUnderlinePainter(new Color(0xC99700), diagnosticsUnderlineRows)
                    : new DiagnosticsUnderlinePainter(new Color(0xD73A49), diagnosticsUnderlineRows);
                Object tag = editor.getHighlighter().addHighlight(start, end, painter);
                tags.add(tag);
            } catch (BadLocationException ignored) {
                // Skip malformed locations from diagnostics.
            }
        }
        diagnosticHighlightTags.put(editor, tags);
    }

    private void clearEditorHighlights(RSyntaxTextArea editor) {
        List<Object> tags = diagnosticHighlightTags.remove(editor);
        if (tags == null || tags.isEmpty()) {
            return;
        }
        for (Object tag : tags) {
            editor.getHighlighter().removeHighlight(tag);
        }
    }

    private int toOffset(RSyntaxTextArea editor, int line, int column) throws BadLocationException {
        int lineIndex = Math.max(0, line - 1);
        int maxLine = Math.max(0, editor.getLineCount() - 1);
        if (lineIndex > maxLine) {
            lineIndex = maxLine;
        }
        int lineStart = editor.getLineStartOffset(lineIndex);
        int lineEnd = editor.getLineEndOffset(lineIndex);
        int colIndex = Math.max(0, column - 1);
        return Math.min(lineStart + colIndex, Math.max(lineStart, lineEnd - 1));
    }

    private void openProblem(ProblemsPanel.ProblemEntry entry) {
        if (entry == null) {
            return;
        }
        LuciaDiagnostic d = entry.diagnostic();
        openLocation(entry.file(), d.line(), d.column(), d.length(), "search.openResultError");
    }

    private void openProblemsPanel() {
        if (bottomTabs == null || problemsPanel == null) {
            return;
        }
        int index = bottomTabs.indexOfComponent(problemsPanel);
        if (index >= 0) {
            bottomTabs.setSelectedIndex(index);
            problemsPanel.requestFocusInWindow();
        }
    }

    /**
     * Switches the bottom tab to Problems without stealing focus from the active editor.
     * Used by automatic diagnostics so the user can keep typing uninterrupted.
     */
    private void showProblemsTabWithoutFocusChange() {
        if (bottomTabs == null || problemsPanel == null) {
            return;
        }
        int index = bottomTabs.indexOfComponent(problemsPanel);
        if (index < 0 || bottomTabs.getSelectedIndex() == index) {
            return;
        }
        // Find the currently focused editor so we can restore its focus after the tab switch.
        RSyntaxTextArea focusedEditor = null;
        java.awt.Component focused = java.awt.KeyboardFocusManager
                .getCurrentKeyboardFocusManager().getFocusOwner();
        if (focused instanceof RSyntaxTextArea rsta) {
            focusedEditor = rsta;
        }

        bottomTabs.setSelectedIndex(index);

        // Immediately hand focus back to the editor so typing is not interrupted.
        if (focusedEditor != null) {
            final RSyntaxTextArea editorToRefocus = focusedEditor;
            SwingUtilities.invokeLater(editorToRefocus::requestFocusInWindow);
        }
    }

    private void triggerQuickFix() {
        if (bottomTabs != null && bottomTabs.getSelectedComponent() == problemsPanel) {
            ProblemsPanel.ProblemEntry selected = problemsPanel.getSelectedProblem();
            if (selected == null) {
                appendOutput(I18n.tr("log.quickFixNoProblem"));
                return;
            }
            applyQuickFixFromProblem(selected);
            return;
        }

        Path file = getCurrentFileFromTab();
        if (file == null) {
            appendOutput(I18n.tr("log.quickFixNoProblem"));
            return;
        }
        List<LuciaDiagnostic> diagnostics = diagnosticsByFile.get(file.toAbsolutePath().normalize());
        if (diagnostics == null || diagnostics.isEmpty()) {
            appendOutput(I18n.tr("log.quickFixNoProblem"));
            return;
        }
        applyQuickFixFromProblem(new ProblemsPanel.ProblemEntry(file, diagnostics.get(0)));
    }

    private void applyQuickFixFromProblem(ProblemsPanel.ProblemEntry entry) {
        if (entry == null) {
            return;
        }
        Path file = entry.file().toAbsolutePath().normalize();
        RSyntaxTextArea editor = openEditors.get(file);
        if (editor == null) {
            openFile(file);
            editor = openEditors.get(file);
        }
        if (editor == null) {
            return;
        }

        String msg = entry.diagnostic().message().toLowerCase(Locale.ROOT);
        if (msg.contains("undeclared variable") || msg.contains("unknown function") || msg.contains("not found")) {
            insertTextAtLineStart(editor, entry.diagnostic().line(), "let ");
            appendOutput(I18n.tr("log.quickFixApplied") + ": " + I18n.tr("quickfix.addLet"));
            requestDiagnostics(file, editor);
            return;
        }
        if (msg.contains("syntax") || msg.contains("expected") || msg.contains("line")) {
            insertTextAtLineEnd(editor, entry.diagnostic().line(), ";");
            appendOutput(I18n.tr("log.quickFixApplied") + ": " + I18n.tr("quickfix.addSemicolon"));
            requestDiagnostics(file, editor);
            return;
        }

        appendOutput(I18n.tr("log.quickFixNotAvailable"));
        openProblem(entry);
    }

    private void insertTextAtLineStart(RSyntaxTextArea editor, int line, String text) {
        try {
            int lineIdx = Math.max(0, line - 1);
            int start = editor.getLineStartOffset(Math.min(lineIdx, Math.max(0, editor.getLineCount() - 1)));
            Document document = editor.getDocument();
            String currentPrefix = editor.getText(start, Math.min(text.length(), document.getLength() - start));
            if (currentPrefix.startsWith(text)) {
                return;
            }
            document.insertString(start, text, null);
        } catch (Exception ignored) {
            // Ignore invalid insert ranges.
        }
    }

    private void insertTextAtLineEnd(RSyntaxTextArea editor, int line, String text) {
        try {
            int lineIdx = Math.max(0, line - 1);
            int safeLine = Math.min(lineIdx, Math.max(0, editor.getLineCount() - 1));
            int end = editor.getLineEndOffset(safeLine);
            int insertAt = Math.max(0, end - 1);
            if (insertAt > 0) {
                String previous = editor.getText(insertAt - 1, 1);
                if (";".equals(previous)) {
                    return;
                }
            }
            editor.getDocument().insertString(insertAt, text, null);
        } catch (Exception ignored) {
            // Ignore invalid insert ranges.
        }
    }

    private void updateProblemsTabTitle() {
        if (bottomTabs == null) {
            return;
        }
        int index = bottomTabs.indexOfComponent(problemsPanel);
        if (index >= 0) {
            bottomTabs.setTitleAt(index, I18n.tr("tab.problems") + " (" + problemsPanel.totalProblems() + ")");
        }
    }

    // ── Run / compile ──────────────────────────────────────────────────

    private void runCurrentFile() {
        Path file = getCurrentFileFromTab();
        runFile(file);
    }

    private void compileCurrentFile() {
        Path file = getCurrentFileFromTab();
        compileFile(file);
    }

    private void runFile(Path file) {
        if (file == null) { showError(I18n.tr("error.noFile")); return; }
        if (config.isClearOutputBeforeRun()) {
            output.setText("");
        }
        if (config.isSaveBeforeRun()) {
            saveFile(file);
        }
        runner.run(file.toAbsolutePath().normalize());
    }

    private void compileFile(Path file) {
        if (file == null) { showError(I18n.tr("error.noFile")); return; }
        if (config.isClearOutputBeforeRun()) {
            output.setText("");
        }
        if (config.isSaveBeforeRun()) {
            saveFile(file);
        }
        runner.compile(file.toAbsolutePath().normalize());
    }

    private void runTestsCommand() {
        if (config.isClearOutputBeforeRun()) {
            output.setText("");
        }
        runner.runTests();
    }

    private void runCustomCommand() {
        if (config.isClearOutputBeforeRun()) {
            output.setText("");
        }
        runner.runCustom();
    }

    private void focusTerminal() {
        bottomTabs.setSelectedIndex(1);
        terminalPanel.focusTerminal();
    }

    private void showGlobalSearch() {
        if (projectRoot == null) {
            showError(I18n.tr("error.noProject"));
            return;
        }
        if (globalSearchDialog == null) {
            globalSearchDialog = new GlobalSearchDialog(this, () -> projectRoot,
                    this::openSearchMatch, this::appendOutput);
        }
        globalSearchDialog.setVisible(true);
    }

    private void openSearchMatch(GlobalSearchDialog.SearchMatch match) {
        if (match == null) {
            return;
        }
        openLocation(match.path(), match.line(), match.column(), match.length(), "search.openResultError");
    }

    private void goToDefinition() {
        if (projectRoot == null) {
            showError(I18n.tr("error.noProject"));
            return;
        }
        RSyntaxTextArea editor = getCurrentEditor();
        if (editor == null || getCurrentFileFromTab() == null) {
            showError(I18n.tr("error.noFile"));
            return;
        }

        String symbol = symbolNavigator.getSymbolAt(editor.getText(), editor.getCaretPosition());
        if (symbol == null || symbol.isBlank()) {
            showError(I18n.tr("nav.noSymbolAtCaret"));
            return;
        }

        try {
            List<LuciaSymbolNavigator.SymbolDefinition> definitions = symbolNavigator.findDefinitions(projectRoot, symbol);
            if (definitions.isEmpty()) {
                showError(I18n.tr("nav.definitionNotFound") + ": " + symbol);
                return;
            }

            LuciaSymbolNavigator.SymbolDefinition selected = chooseFromList(
                    definitions,
                    I18n.tr("nav.definitionPickerTitle") + " (" + symbol + ")");
            if (selected != null) {
                LuciaSymbolNavigator.SymbolLocation location = selected.location();
                openLocation(location.path(), location.line(), location.column(), location.length(), "nav.openDefinitionError");
            }
        } catch (IOException ex) {
            showError(I18n.tr("nav.navigationError") + ": " + ex.getMessage());
        }
    }

    private void findReferences() {
        if (projectRoot == null) {
            showError(I18n.tr("error.noProject"));
            return;
        }
        RSyntaxTextArea editor = getCurrentEditor();
        if (editor == null || getCurrentFileFromTab() == null) {
            showError(I18n.tr("error.noFile"));
            return;
        }

        String symbol = symbolNavigator.getSymbolAt(editor.getText(), editor.getCaretPosition());
        if (symbol == null || symbol.isBlank()) {
            showError(I18n.tr("nav.noSymbolAtCaret"));
            return;
        }

        try {
            List<LuciaSymbolNavigator.SymbolReference> references = symbolNavigator.findReferences(projectRoot, symbol);
            if (references.isEmpty()) {
                showError(I18n.tr("nav.referencesNotFound") + ": " + symbol);
                return;
            }

            LuciaSymbolNavigator.SymbolReference selected = chooseFromList(
                    references,
                    I18n.tr("nav.referencesPickerTitle") + " (" + symbol + ")");
            if (selected != null) {
                LuciaSymbolNavigator.SymbolLocation location = selected.location();
                openLocation(location.path(), location.line(), location.column(), location.length(), "nav.openReferenceError");
            }
            appendOutput(I18n.tr("log.referencesFound") + ": " + symbol + " = " + references.size());
        } catch (IOException ex) {
            showError(I18n.tr("nav.navigationError") + ": " + ex.getMessage());
        }
    }

    private <T> T chooseFromList(List<T> items, String title) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        DefaultListModel<T> model = new DefaultListModel<>();
        for (T item : items) {
            model.addElement(item);
        }
        JList<T> list = new JList<>(model);
        list.setSelectedIndex(0);
        JScrollPane scroller = new JScrollPane(list);
        scroller.setPreferredSize(new Dimension(820, 360));
        int result = JOptionPane.showConfirmDialog(this, scroller, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }
        return list.getSelectedValue();
    }

    private void openLocation(Path targetPath, int line, int column, int length, String errorKey) {
        Path target = targetPath.toAbsolutePath().normalize();
        openFile(target);
        RSyntaxTextArea editor = openEditors.get(target);
        if (editor == null) {
            return;
        }
        try {
            int lineIndex = Math.max(0, line - 1);
            int colIndex = Math.max(0, column - 1);
            int lineStart = editor.getLineStartOffset(lineIndex);
            int lineEnd = editor.getLineEndOffset(lineIndex);
            int offset = Math.min(lineStart + colIndex, Math.max(lineStart, lineEnd - 1));
            int end = Math.min(offset + Math.max(1, length), editor.getDocument().getLength());
            editor.setSelectionStart(offset);
            editor.setSelectionEnd(end);
            editor.setCaretPosition(offset);
            editor.requestFocusInWindow();
        } catch (BadLocationException ex) {
            showError(I18n.tr(errorKey) + ": " + ex.getMessage());
        }
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

    private void handleProjectPathChanged(Path oldPath, Path newPath) {
        Path normalizedOld = oldPath.toAbsolutePath().normalize();
        Path normalizedNew = newPath.toAbsolutePath().normalize();

        remapOpenFileReferences(normalizedOld, normalizedNew);
        remapDiagnosticsReferences(normalizedOld, normalizedNew);

        if (currentFile != null && currentFile.startsWith(normalizedOld)) {
            currentFile = remapPathPrefix(currentFile, normalizedOld, normalizedNew);
        }
        onTabChanged();
    }

    private void handleProjectPathDeleted(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        List<Component> tabsToClose = new ArrayList<>();
        for (Map.Entry<Component, Path> entry : tabToPath.entrySet()) {
            if (entry.getValue().startsWith(normalized)) {
                tabsToClose.add(entry.getKey());
            }
        }
        for (Component tab : tabsToClose) {
            closeTab(tab);
        }
        clearDiagnosticsUnder(normalized);
        onTabChanged();
    }

    private void remapOpenFileReferences(Path oldRoot, Path newRoot) {
        LinkedHashMap<Path, RSyntaxTextArea> remappedEditors = new LinkedHashMap<>();
        for (Map.Entry<Path, RSyntaxTextArea> entry : openEditors.entrySet()) {
            remappedEditors.put(remapPathPrefix(entry.getKey(), oldRoot, newRoot), entry.getValue());
        }
        openEditors.clear();
        openEditors.putAll(remappedEditors);

        LinkedHashMap<Path, RTextScrollPane> remappedScrollPanes = new LinkedHashMap<>();
        for (Map.Entry<Path, RTextScrollPane> entry : openScrollPanes.entrySet()) {
            remappedScrollPanes.put(remapPathPrefix(entry.getKey(), oldRoot, newRoot), entry.getValue());
        }
        openScrollPanes.clear();
        openScrollPanes.putAll(remappedScrollPanes);

        for (Map.Entry<Component, Path> entry : tabToPath.entrySet()) {
            Path remappedPath = remapPathPrefix(entry.getValue(), oldRoot, newRoot);
            entry.setValue(remappedPath);
            updateTabMetadata(entry.getKey(), remappedPath);
        }
    }

    private void remapDiagnosticsReferences(Path oldRoot, Path newRoot) {
        LinkedHashMap<Path, List<LuciaDiagnostic>> remappedDiagnostics = new LinkedHashMap<>();
        List<Path> affectedOldPaths = new ArrayList<>();

        for (Map.Entry<Path, List<LuciaDiagnostic>> entry : diagnosticsByFile.entrySet()) {
            Path currentPath = entry.getKey();
            Path remappedPath = remapPathPrefix(currentPath, oldRoot, newRoot);
            remappedDiagnostics.put(remappedPath, entry.getValue());
            if (!currentPath.equals(remappedPath)) {
                affectedOldPaths.add(currentPath);
            }
        }

        diagnosticsByFile.clear();
        diagnosticsByFile.putAll(remappedDiagnostics);

        for (Path oldPath : affectedOldPaths) {
            diagnosticsService.clear(oldPath);
            problemsPanel.clearFile(oldPath);
        }
        for (Map.Entry<Path, List<LuciaDiagnostic>> entry : diagnosticsByFile.entrySet()) {
            if (entry.getKey().startsWith(newRoot)) {
                problemsPanel.setDiagnostics(entry.getKey(), entry.getValue());
                RSyntaxTextArea editor = openEditors.get(entry.getKey());
                if (editor != null) {
                    requestDiagnostics(entry.getKey(), editor);
                }
            }
        }
        updateProblemsTabTitle();
    }

    private void clearDiagnosticsUnder(Path root) {
        List<Path> pathsToClear = new ArrayList<>();
        for (Path path : diagnosticsByFile.keySet()) {
            if (path.startsWith(root)) {
                pathsToClear.add(path);
            }
        }
        for (Path path : pathsToClear) {
            clearDiagnosticsFor(path);
        }
    }

    private Path remapPathPrefix(Path candidate, Path oldRoot, Path newRoot) {
        if (candidate == null) {
            return null;
        }
        Path normalized = candidate.toAbsolutePath().normalize();
        if (!normalized.startsWith(oldRoot)) {
            return normalized;
        }
        if (normalized.equals(oldRoot)) {
            return newRoot;
        }
        return newRoot.resolve(oldRoot.relativize(normalized)).toAbsolutePath().normalize();
    }

    private void updateTabMetadata(Component tabContent, Path path) {
        int index = editorTabs.indexOfComponent(tabContent);
        if (index < 0 || path == null) {
            return;
        }
        String tabTitle = path.getFileName().toString();
        editorTabs.setTitleAt(index, tabTitle);
        editorTabs.setToolTipTextAt(index, path.toString());
        editorTabs.setTabComponentAt(index,
                new ClosableTabHeader(editorTabs, tabTitle, () -> closeTab(tabContent)));
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
        SettingsDialog dialog = new SettingsDialog(this, config, this::applyConfigurationFromSettings);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            appendOutput(I18n.tr("log.settingsSaved"));
        }
    }

    private void applyConfigurationFromSettings() {
        Locale locale = Locale.forLanguageTag(config.getLanguageTag());
        if (!I18n.getLocale().equals(locale)) {
            I18n.setLocale(locale);
            searchSupport.resetDialogs();
        }

        int configuredFont = Math.max(MIN_EDITOR_FONT_SIZE,
                Math.min(MAX_EDITOR_FONT_SIZE, config.getEditorFontSize()));
        if (configuredFont != editorFontSize) {
            editorFontSize = configuredFont;
        }
        editorFactory.setEditorFontSize(editorFontSize);

        boolean configuredDark = config.isDarkTheme();
        if (configuredDark != darkTheme) {
            toggleTheme(configuredDark);
        }

        editorFactory.setAutocompleteEnabled(config.isAutocompleteEnabled());
        editorFactory.setAutocompleteDelayMs(config.getAutocompleteDelayMs());
        editorFactory.setSnippetsInAutocomplete(config.isSnippetsInAutocomplete());
        editorFactory.refreshSnippetCompletions();

        diagnosticsService.setDebounceMs(config.getDiagnosticsDebounceMs());
        diagnosticsUnderlineRows = config.getDiagnosticsUnderlineRows();

        terminalPanel.setAutoStart(config.isTerminalAutoStart());
        terminalPanel.setShellPath(config.getTerminalShellPath());
        terminalPanel.setWorkingDirectoryMode(config.getTerminalWorkingDirectoryMode());

        openEditors.values().forEach(editor -> {
            editorFactory.applyTheme(editor);
            editorFactory.applyFontSize(editor);
            applyEditorPreferences(editor);
            Path path = findPathForEditor(editor);
            if (path != null) {
                requestDiagnostics(path, editor);
            }
        });
        for (Map.Entry<Path, RTextScrollPane> entry : openScrollPanes.entrySet()) {
            entry.getValue().setLineNumbersEnabled(config.isShowLineNumbers());
        }

        refreshTexts();
    }

    private Path findPathForEditor(RSyntaxTextArea editor) {
        for (Map.Entry<Path, RSyntaxTextArea> entry : openEditors.entrySet()) {
            if (entry.getValue() == editor) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void applyEditorPreferences(RSyntaxTextArea editor) {
        editor.setTabSize(config.getTabSize());
        editor.setTabsEmulated(config.isInsertSpaces());
        editor.setLineWrap(config.isWordWrap());
        editor.setWrapStyleWord(config.isWordWrap());
        editor.setHighlightCurrentLine(config.isHighlightCurrentLine());
        editor.setWhitespaceVisible(config.isShowWhitespace());
    }

    private void openSnippetManager() {
        SnippetManagerDialog dialog = new SnippetManagerDialog(this, snippetManager, () -> {
            editorFactory.refreshSnippetCompletions();
            appendOutput(I18n.tr("log.snippetsReloaded"));
        });
        dialog.setVisible(true);
    }

    private void insertSnippet() {
        RSyntaxTextArea editor = getCurrentEditor();
        if (editor == null) {
            showError(I18n.tr("error.noFile"));
            return;
        }

        try {
            List<SnippetDefinition> snippets = snippetManager.getSnippets();
            if (snippets.isEmpty()) {
                showError(I18n.tr("snippets.noSnippets"));
                return;
            }

            SnippetDefinition selected = chooseFromList(snippets, I18n.tr("snippets.insertTitle"));
            if (selected == null) {
                return;
            }

            insertSnippetTemplate(editor, selected.template());
            appendOutput(I18n.tr("snippets.inserted") + ": " + selected.prefix());
        } catch (IOException ex) {
            showError(I18n.tr("snippets.loadError") + ": " + ex.getMessage());
        }
    }

    private void insertSnippetTemplate(RSyntaxTextArea editor, String template) {
        int selectionStart = editor.getSelectionStart();
        int selectionEnd = editor.getSelectionEnd();
        String safeTemplate = template == null ? "" : template;

        int cursorMarker = safeTemplate.indexOf("${cursor}");
        String insertion = cursorMarker >= 0
                ? safeTemplate.replace("${cursor}", "")
                : safeTemplate;

        editor.replaceRange(insertion, selectionStart, selectionEnd);

        int finalCaret = cursorMarker >= 0
                ? selectionStart + cursorMarker
                : selectionStart + insertion.length();
        finalCaret = Math.min(finalCaret, editor.getDocument().getLength());
        editor.setCaretPosition(finalCaret);
        editor.requestFocusInWindow();
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
        problemsPanel.setDarkTheme(dark);
        openEditors.values().forEach(editor -> {
            editorFactory.applyTheme(editor);
            editorFactory.applyFontSize(editor);
        });
        searchSupport.refreshUi();
        refreshTexts();
        projectTreePanel.getTree().setCellRenderer(new FileTreeCellRenderer());
        SwingUtilities.updateComponentTreeUI(projectTreePanel.getTree());
    }

    private void changeLanguage(Locale locale) {
        I18n.setLocale(locale);
        config.setLanguageTag(locale.toLanguageTag());
        searchSupport.resetDialogs();
        refreshTexts();
    }

    private void refreshTexts() {
        onTabChanged();
        setJMenuBar(buildMenu());
        if (bottomTabs != null) {
            if (bottomTabs.getTabCount() > 0) bottomTabs.setTitleAt(0, I18n.tr("tab.output"));
            if (bottomTabs.getTabCount() > 1) bottomTabs.setTitleAt(1, I18n.tr("tab.terminal"));
            updateProblemsTabTitle();
        }
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

    private static final class DiagnosticsUnderlinePainter extends LayeredHighlighter.LayerPainter {

        private final Color color;
        private final int rows;

        private DiagnosticsUnderlinePainter(Color color, int rows) {
            this.color = color;
            this.rows = Math.max(1, rows);
        }

        @Override
        public void paint(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c) {
            // Highlighter infrastructure calls paintLayer for layered highlights.
        }

        @Override
        public Shape paintLayer(Graphics g, int offs0, int offs1, Shape bounds,
                                JTextComponent c, javax.swing.text.View view) {
            g.setColor(color);
            Rectangle area;
            try {
                Shape shape = view.modelToView(offs0, Position.Bias.Forward, offs1, Position.Bias.Backward, bounds);
                area = shape instanceof Rectangle ? (Rectangle) shape : shape.getBounds();
            } catch (BadLocationException ex) {
                return null;
            }

            int y = area.y + area.height - 3;
            int startX = area.x;
            int endX = area.x + area.width;
            if (endX <= startX) {
                endX = startX + 2;
            }

            // Draw configurable stacked zig-zag rows to make the underline easier to spot.
            for (int row = 0; row < rows; row++) {
                boolean up = true;
                int rowY = y + row;
                for (int x = startX; x < endX; x += 2) {
                    int y1 = up ? rowY : rowY + 1;
                    int y2 = up ? rowY + 1 : rowY;
                    g.drawLine(x, y1, Math.min(x + 1, endX), y2);
                    up = !up;
                }
            }
            return area;
        }
    }
}
