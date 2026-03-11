package com.lucia.editor.ui;

import com.lucia.editor.config.EditorConfig;
import com.lucia.editor.i18n.I18n;
import com.lucia.editor.syntax.LuciaTokenMaker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.JTextArea;
import javax.swing.JEditorPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

public class MainFrame extends JFrame {

    private static final int MIN_EDITOR_FONT_SIZE = 10;
    private static final int MAX_EDITOR_FONT_SIZE = 32;
    private static final int DEFAULT_EDITOR_FONT_SIZE = 15;
    private static final String CONTACT_PHONE = "+50671077660";
    private static final String CONTACT_EMAIL = "ggonzalezs@cuc.ac.cr";
    private static final String LUCIA_REPO_URL = "https://github.com/gabrielgscr/lucia.git";
    private static final String LUCIA_EDITOR_REPO_URL = "https://github.com/gabrielgscr/lucia-editor.git";

    private final EditorConfig config;

    private Path projectRoot;
    private Path currentFile;

    private final JTree tree;
    private final JTabbedPane editorTabs;
    private final JTextArea output;
    private JTextField inputField;
    /** Stdin of the currently running process, or null when idle. */
    private volatile PrintWriter processStdin;
    private JToolBar toolBar;
    private JLabel statusLabel;
    private int editorFontSize;
    private boolean darkTheme;
    private final Map<Path, RSyntaxTextArea> openEditors;
    private final Map<Component, Path> tabToPath;

    public MainFrame() {
        this.config = new EditorConfig();
        this.editorFontSize = Math.max(MIN_EDITOR_FONT_SIZE,
                Math.min(MAX_EDITOR_FONT_SIZE, config.getEditorFontSize()));
        this.darkTheme = config.isDarkTheme();

        registerLuciaSyntax();

        this.tree = new JTree(new DefaultMutableTreeNode(I18n.tr("project.none")));
        this.editorTabs = new JTabbedPane();
        this.output = new JTextArea(8, 100);
        this.openEditors = new LinkedHashMap<>();
        this.tabToPath = new HashMap<>();

        buildUi();
        refreshTexts();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(null);
    }

    private void buildUi() {
        editorTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        editorTabs.addChangeListener(e -> onTabChanged());

        output.setEditable(false);

        tree.addTreeSelectionListener(this::onTreeSelection);
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showTreeContextMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showTreeContextMenu(e);
            }
        });
        ToolTipManager.sharedInstance().registerComponent(tree);
        tree.setCellRenderer(new FileTreeCellRenderer());
        tree.setRowHeight(22);
        tree.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        output.setFont(output.getFont().deriveFont(Font.PLAIN, 13f));

        editorTabs.putClientProperty("JTabbedPane.showTabSeparators", true);
        editorTabs.putClientProperty("JTabbedPane.tabInsets", new java.awt.Insets(4, 8, 4, 8));

        inputField = new JTextField();
        inputField.setEnabled(false);
        inputField.setFont(output.getFont());
        inputField.addActionListener(e -> sendInputToProcess());

        JScrollPane treeScroll = new JScrollPane(tree);
        JScrollPane outputScroll = new JScrollPane(output);

        JLabel inputPromptLabel = new JLabel("  >");
        JPanel inputBar = new JPanel(new BorderLayout(4, 0));
        inputBar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        inputBar.add(inputPromptLabel, BorderLayout.WEST);
        inputBar.add(inputField, BorderLayout.CENTER);

        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.add(outputScroll, BorderLayout.CENTER);
        outputPanel.add(inputBar, BorderLayout.SOUTH);

        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorTabs, outputPanel);
        rightSplit.setResizeWeight(0.8);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, rightSplit);
        mainSplit.setResizeWeight(0.25);
        mainSplit.setDividerSize(10);
        rightSplit.setDividerSize(10);

        setLayout(new BorderLayout());
        toolBar = buildToolBar();
        add(toolBar, BorderLayout.NORTH);
        add(mainSplit, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        setJMenuBar(buildMenu());

        var rootInputMap = getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
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
    }

    private void openProject(Path path) {
        if (path == null || !Files.isDirectory(path)) {
            showError(I18n.tr("error.invalidProjectFolder") + ": " + path);
            if (path != null) {
                config.removeRecentProject(path);
            }
            refreshTexts();
            return;
        }

        projectRoot = path.toAbsolutePath().normalize();
        config.addRecentProject(projectRoot);
        rebuildTree();
        appendOutput(I18n.tr("log.projectOpened") + ": " + projectRoot);
        updateStatusBar();
        refreshTexts();
    }

    private JToolBar buildToolBar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        bar.add(createToolbarButton(I18n.tr("menu.openProject"), FontAwesomeSolid.FOLDER_OPEN, this::chooseProject));
        bar.add(createToolbarButton(I18n.tr("menu.newFile"), FontAwesomeSolid.FILE_ALT, this::createLuciaFile));
        bar.add(createToolbarButton(I18n.tr("menu.newFolder"), FontAwesomeSolid.FOLDER_PLUS, this::createFolder));
        bar.addSeparator();
        bar.add(createToolbarButton(I18n.tr("menu.save"), FontAwesomeSolid.SAVE, this::saveCurrentFile));
        bar.add(createToolbarButton(I18n.tr("menu.saveAll"), FontAwesomeSolid.COPY, this::saveAllOpenFiles));
        bar.addSeparator();
        bar.add(createToolbarButton(I18n.tr("menu.runCurrent"), FontAwesomeSolid.PLAY, this::runCurrentFile));
        bar.add(createToolbarButton(I18n.tr("menu.compileCurrent"), FontAwesomeSolid.COG, this::compileCurrentFile));
        bar.add(createToolbarButton(I18n.tr("menu.runTests"), FontAwesomeSolid.VIAL, this::runLuciaTests));
        bar.addSeparator();
        bar.add(createToolbarButton(I18n.tr("menu.fontDecrease"), FontAwesomeSolid.SEARCH_MINUS, () -> changeEditorFontSize(-1), "A-"));
        bar.add(createToolbarButton(I18n.tr("menu.fontIncrease"), FontAwesomeSolid.SEARCH_PLUS, () -> changeEditorFontSize(1), "A+"));
        bar.addSeparator();
        bar.add(createToolbarButton(I18n.tr("menu.settings"), FontAwesomeSolid.SLIDERS_H, this::openSettings));

        return bar;
    }

    private JButton createToolbarButton(String tooltip, Ikon iconCode, Runnable action) {
        return createToolbarButton(tooltip, iconCode, action, null);
    }

    private JButton createToolbarButton(String tooltip, Ikon iconCode, Runnable action, String text) {
        JButton button = new JButton(createIcon(iconCode));
        if (text != null && !text.isBlank()) {
            button.setText(text);
        }
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
        if (fg == null) {
            return Color.DARK_GRAY;
        }
        // In dark themes the foreground is very light (almost white).
        // Use a slightly dimmed version so icons feel softer.
        double luminance = 0.2126 * fg.getRed() + 0.7152 * fg.getGreen() + 0.0722 * fg.getBlue();
        if (luminance > 180) {
            // Dark theme: use muted light gray (#C8C8C8) instead of near-white.
            return new Color(200, 200, 200);
        }
        return fg;
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

        JMenuItem openProject = createMenuItem("menu.openProject", FontAwesomeSolid.FOLDER_OPEN, this::chooseProject);
        JMenu recentProjects = buildRecentProjectsMenu();

        JMenuItem newFile = createMenuItem("menu.newFile", FontAwesomeSolid.FILE_ALT, this::createLuciaFile);

        JMenuItem newFolder = createMenuItem("menu.newFolder", FontAwesomeSolid.FOLDER_PLUS, this::createFolder);

        JMenuItem refreshProject = createMenuItem("menu.refreshProject", FontAwesomeSolid.SYNC_ALT, this::rebuildTree);

        JMenuItem saveFile = createMenuItem("menu.save", FontAwesomeSolid.SAVE, this::saveCurrentFile);

        JMenuItem saveAll = createMenuItem("menu.saveAll", FontAwesomeSolid.COPY, this::saveAllOpenFiles);

        JMenuItem closeTab = createMenuItem("menu.closeTab", FontAwesomeSolid.TIMES, this::closeCurrentTab);

        JMenuItem exit = createMenuItem("menu.exit", FontAwesomeSolid.SIGN_OUT_ALT, this::dispose);

        fileMenu.add(openProject);
        fileMenu.add(recentProjects);
        fileMenu.add(newFile);
        fileMenu.add(newFolder);
        fileMenu.add(refreshProject);
        fileMenu.addSeparator();
        fileMenu.add(saveFile);
        fileMenu.add(saveAll);
        fileMenu.add(closeTab);
        fileMenu.addSeparator();
        fileMenu.add(exit);

        JMenu runMenu = new JMenu(I18n.tr("menu.run"));
        JMenuItem runFile = createMenuItem("menu.runCurrent", FontAwesomeSolid.PLAY, this::runCurrentFile);
        JMenuItem compileFile = createMenuItem("menu.compileCurrent", FontAwesomeSolid.COG, this::compileCurrentFile);
        JMenuItem runTests = createMenuItem("menu.runTests", FontAwesomeSolid.VIAL, this::runLuciaTests);
        JMenuItem runCustom = createMenuItem("menu.runCustom", FontAwesomeSolid.TERMINAL, this::runCustomCliCommand);
        runMenu.add(runFile);
        runMenu.add(compileFile);
        runMenu.addSeparator();
        runMenu.add(runTests);
        runMenu.add(runCustom);

        JMenu viewMenu = new JMenu(I18n.tr("menu.view"));
        JMenuItem fontDecrease = createMenuItem("menu.fontDecrease", FontAwesomeSolid.SEARCH_MINUS, () -> changeEditorFontSize(-1));
        fontDecrease.setAccelerator(KeyStroke.getKeyStroke("control MINUS"));
        JMenuItem fontIncrease = createMenuItem("menu.fontIncrease", FontAwesomeSolid.SEARCH_PLUS, () -> changeEditorFontSize(1));
        fontIncrease.setAccelerator(KeyStroke.getKeyStroke("control EQUALS"));
        JMenuItem fontReset = createMenuItem("menu.fontReset", FontAwesomeSolid.TEXT_HEIGHT, () -> setEditorFontSize(DEFAULT_EDITOR_FONT_SIZE));
        fontReset.setAccelerator(KeyStroke.getKeyStroke("control 0"));

        JCheckBoxMenuItem darkModeItem = new JCheckBoxMenuItem(I18n.tr("menu.darkTheme"), createIcon(FontAwesomeSolid.MOON));
        darkModeItem.setSelected(darkTheme);
        darkModeItem.addActionListener(e -> toggleTheme(darkModeItem.isSelected()));

        viewMenu.add(fontDecrease);
        viewMenu.add(fontIncrease);
        viewMenu.add(fontReset);
        viewMenu.addSeparator();
        viewMenu.add(darkModeItem);

        JMenu settingsMenu = new JMenu(I18n.tr("menu.tools"));
        JMenuItem configItem = createMenuItem("menu.settings", FontAwesomeSolid.SLIDERS_H, this::openSettings);
        settingsMenu.add(configItem);

        JMenu languageMenu = new JMenu(I18n.tr("menu.language"));
        JMenuItem spanish = new JMenuItem(I18n.tr("language.spanish"), createIcon(FontAwesomeSolid.FLAG));
        spanish.addActionListener(e -> changeLanguage(Locale.forLanguageTag("es")));

        JMenuItem english = new JMenuItem(I18n.tr("language.english"), createIcon(FontAwesomeSolid.FLAG_CHECKERED));
        english.addActionListener(e -> changeLanguage(Locale.ENGLISH));

        languageMenu.add(spanish);
        languageMenu.add(english);

        JMenu helpMenu = new JMenu(I18n.tr("menu.help"));
        JMenuItem aboutItem = createMenuItem("menu.about", FontAwesomeSolid.INFO_CIRCLE, this::showAboutDialog);
        helpMenu.add(aboutItem);

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
        JMenuItem clearRecent = new JMenuItem(I18n.tr("menu.clearRecentProjects"), createIcon(FontAwesomeSolid.TRASH));
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

    private void registerLuciaSyntax() {
        TokenMakerFactory factory = TokenMakerFactory.getDefaultInstance();
        if (factory instanceof AbstractTokenMakerFactory atmf) {
            atmf.putMapping("text/lucia", LuciaTokenMaker.class.getName());
        }
    }

    private void chooseProject() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            openProject(chooser.getSelectedFile().toPath());
        }
    }

    private void rebuildTree() {
        if (projectRoot == null) {
            return;
        }

        DefaultMutableTreeNode rootNode = createTreeNode(projectRoot);
        tree.setModel(new DefaultTreeModel(rootNode));
    }

    private DefaultMutableTreeNode createTreeNode(Path path) {
        return createTreeNode(path, true);
    }

    private DefaultMutableTreeNode createTreeNode(Path path, boolean isRoot) {
        // Root node shows full path; children show only the file/folder name.
        String label = isRoot
                ? path.toAbsolutePath().normalize().toString()
                : (path.getFileName() != null ? path.getFileName().toString() : path.toString());

        DefaultMutableTreeNode node = new DefaultMutableTreeNode(label);
        node.setUserObject(path);

        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path).sorted()) {
                stream.forEach(child -> {
                    if (Files.isDirectory(child) || child.toString().endsWith(".lucia")) {
                        node.add(createTreeNode(child, false));
                    }
                });
            } catch (IOException ex) {
                appendOutput(I18n.tr("log.error") + ": " + ex.getMessage());
            }
        }

        return node;
    }

    private void onTreeSelection(TreeSelectionEvent event) {
        Object selectedNode = tree.getLastSelectedPathComponent();
        if (!(selectedNode instanceof DefaultMutableTreeNode node)) {
            return;
        }

        Object userObject = node.getUserObject();
        if (!(userObject instanceof Path path)) {
            return;
        }

        if (Files.isRegularFile(path) && path.toString().endsWith(".lucia")) {
            openFile(path);
        }
    }

    private void openFile(Path path) {
        if (openEditors.containsKey(path)) {
            selectTabByPath(path);
            return;
        }

        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);

            RSyntaxTextArea editor = new RSyntaxTextArea(30, 100);
            configureEditorComponent(editor);
            editor.setText(content);

            RTextScrollPane editorScroll = new RTextScrollPane(editor);
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

        if (file == null || editor == null) {
            showError(I18n.tr("error.noFile"));
            return;
        }

        try {
            Files.writeString(file, editor.getText(), StandardCharsets.UTF_8);
            appendOutput(I18n.tr("log.saved") + ": " + file);
        } catch (IOException ex) {
            showError(I18n.tr("error.saveFile") + ": " + ex.getMessage());
        }
    }

    private void saveAllOpenFiles() {
        for (Map.Entry<Path, RSyntaxTextArea> entry : openEditors.entrySet()) {
            try {
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
        if (selected != null) {
            closeTab(selected);
        }
    }

    private void closeTab(Component tabContent) {
        Path path = tabToPath.remove(tabContent);
        if (path != null) {
            openEditors.remove(path);
        }
        editorTabs.remove(tabContent);
        onTabChanged();
    }

    private void runCurrentFile() {
        executeCli("run", false);
    }

    private void compileCurrentFile() {
        executeCli("compile", true);
    }

    private void executeCli(String command, boolean saveOutput) {
        Path file = getCurrentFileFromTab();
        currentFile = file;

        if (file == null) {
            showError(I18n.tr("error.noFile"));
            return;
        }

        saveCurrentFile();

        Path luciaRoot = config.getLuciaProjectRoot();
        String pythonExec = config.getPythonExecutable();

        if (!Files.exists(luciaRoot.resolve("main.py"))) {
            showError(I18n.tr("error.invalidLuciaRoot") + ": " + luciaRoot);
            return;
        }

        appendOutput(I18n.tr("log.running") + " [" + command + "]: " + file);

        final int[] exitHolder = new int[] { -1 };
        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
            ProcessBuilder builder;
            if (saveOutput) {
                builder = new ProcessBuilder(
                    pythonExec,
                    "main.py",
                    command,
                            file.toAbsolutePath().toString(),
                    "--save"
                );
            } else {
                builder = new ProcessBuilder(
                    pythonExec,
                    "main.py",
                    command,
                            file.toAbsolutePath().toString()
                );
            }
                builder.directory(luciaRoot.toFile());
                builder.redirectErrorStream(true);
                // Force unbuffered Python output so prompts appear immediately.
                builder.environment().put("PYTHONUNBUFFERED", "1");

                Process process = builder.start();
                processStdin = new PrintWriter(process.getOutputStream(), true);
                SwingUtilities.invokeLater(() -> {
                    inputField.setEnabled(true);
                    inputField.requestFocusInWindow();
                });
                try (InputStreamReader reader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)) {
                    char[] buffer = new char[512];
                    int read;
                    while ((read = reader.read(buffer)) != -1) {
                        publish(new String(buffer, 0, read));
                    }
                }

                exitHolder[0] = process.waitFor();
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String chunk : chunks) {
                    appendOutputRaw(chunk);
                }
            }

            @Override
            protected void done() {
                processStdin = null;
                SwingUtilities.invokeLater(() -> inputField.setEnabled(false));
                appendOutput(I18n.tr("log.exitCode") + ": " + exitHolder[0]);
                appendOutput(I18n.tr("log.runFinished"));
            }
        };

        worker.execute();
    }

    private void sendInputToProcess() {
        PrintWriter writer = processStdin;
        if (writer == null) return;
        String text = inputField.getText();
        inputField.setText("");
        appendOutput("> " + text);
        writer.println(text);
    }

    private void runLuciaTests() {
        Path luciaRoot = config.getLuciaProjectRoot();
        String pythonExec = config.getPythonExecutable();

        if (!Files.exists(luciaRoot.resolve("main.py"))) {
            showError(I18n.tr("error.invalidLuciaRoot") + ": " + luciaRoot);
            return;
        }

        List<String> command = List.of(pythonExec, "-m", "pytest", "-q");
        executeProcess(command, luciaRoot, I18n.tr("log.testsStarted"));
    }

    private void runCustomCliCommand() {
        Path luciaRoot = config.getLuciaProjectRoot();
        String pythonExec = config.getPythonExecutable();

        if (!Files.exists(luciaRoot.resolve("main.py"))) {
            showError(I18n.tr("error.invalidLuciaRoot") + ": " + luciaRoot);
            return;
        }

        String args = JOptionPane.showInputDialog(
                this,
                I18n.tr("prompt.customCommand"),
                "run examples/00_features.lucia"
        );

        if (args == null || args.trim().isEmpty()) {
            return;
        }

        List<String> parsed = parseArguments(args.trim());
        if (parsed.isEmpty()) {
            return;
        }

        List<String> command = new ArrayList<>();
        command.add(pythonExec);
        command.add("main.py");
        command.addAll(parsed);

        executeProcess(command, luciaRoot, I18n.tr("log.customStarted"));
    }

    private void executeProcess(List<String> command, Path workingDirectory, String startMessage) {
        appendOutput(startMessage + ": " + String.join(" ", command));

        final int[] exitHolder = new int[] { -1 };
        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                ProcessBuilder builder = new ProcessBuilder(command);
                builder.directory(workingDirectory.toFile());
                builder.redirectErrorStream(true);
                builder.environment().put("PYTHONUNBUFFERED", "1");

                Process process = builder.start();
                try (InputStreamReader reader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)) {
                    char[] buffer = new char[512];
                    int read;
                    while ((read = reader.read(buffer)) != -1) {
                        publish(new String(buffer, 0, read));
                    }
                }

                exitHolder[0] = process.waitFor();
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String chunk : chunks) {
                    appendOutputRaw(chunk);
                }
            }

            @Override
            protected void done() {
                appendOutput(I18n.tr("log.exitCode") + ": " + exitHolder[0]);
                appendOutput(I18n.tr("log.runFinished"));
            }
        };

        worker.execute();
    }

    private List<String> parseArguments(String value) {
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }

            if (Character.isWhitespace(c) && !inQuotes) {
                if (current.length() > 0) {
                    args.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }

            current.append(c);
        }

        if (current.length() > 0) {
            args.add(current.toString());
        }

        return args;
    }

    private void createLuciaFile() {
        Path directory = getSelectedDirectoryForCreation();
        if (directory == null) {
            showError(I18n.tr("error.noProject"));
            return;
        }

        String name = JOptionPane.showInputDialog(this, I18n.tr("prompt.newFileName"), "nuevo.lucia");
        if (name == null || name.trim().isEmpty()) {
            return;
        }

        String normalizedName = name.trim();
        if (!normalizedName.endsWith(".lucia")) {
            normalizedName += ".lucia";
        }

        Path newFile = directory.resolve(normalizedName);
        if (Files.exists(newFile)) {
            showError(I18n.tr("error.alreadyExists") + ": " + newFile);
            return;
        }

        try {
            Files.createFile(newFile);
            rebuildTree();
            openFile(newFile);
            appendOutput(I18n.tr("log.fileCreated") + ": " + newFile);
        } catch (IOException ex) {
            showError(I18n.tr("error.createFile") + ": " + ex.getMessage());
        }
    }

    private void createFolder() {
        Path directory = getSelectedDirectoryForCreation();
        if (directory == null) {
            showError(I18n.tr("error.noProject"));
            return;
        }

        String name = JOptionPane.showInputDialog(this, I18n.tr("prompt.newFolderName"), "nuevo_modulo");
        if (name == null || name.trim().isEmpty()) {
            return;
        }

        Path newFolder = directory.resolve(name.trim());
        if (Files.exists(newFolder)) {
            showError(I18n.tr("error.alreadyExists") + ": " + newFolder);
            return;
        }

        try {
            Files.createDirectory(newFolder);
            rebuildTree();
            appendOutput(I18n.tr("log.folderCreated") + ": " + newFolder);
        } catch (IOException ex) {
            showError(I18n.tr("error.createFolder") + ": " + ex.getMessage());
        }
    }

    private Path getSelectedDirectoryForCreation() {
        if (projectRoot == null) {
            return null;
        }

        Object selectedNode = tree.getLastSelectedPathComponent();
        if (selectedNode instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof Path path) {
            if (Files.isDirectory(path)) {
                return path;
            }
            Path parent = path.getParent();
            if (parent != null) {
                return parent;
            }
        }
        return projectRoot;
    }

    private void showTreeContextMenu(MouseEvent event) {
        if (!event.isPopupTrigger()) {
            return;
        }

        int row = tree.getRowForLocation(event.getX(), event.getY());
        if (row >= 0) {
            tree.setSelectionRow(row);
        }

        JPopupMenu menu = new JPopupMenu();
        JMenuItem newFile = new JMenuItem(I18n.tr("menu.newFile"));
        newFile.addActionListener(e -> createLuciaFile());
        JMenuItem newFolder = new JMenuItem(I18n.tr("menu.newFolder"));
        newFolder.addActionListener(e -> createFolder());
        JMenuItem refresh = new JMenuItem(I18n.tr("menu.refreshProject"));
        refresh.addActionListener(e -> rebuildTree());

        menu.add(newFile);
        menu.add(newFolder);
        menu.add(refresh);
        menu.show(tree, event.getX(), event.getY());
    }

    private void onTabChanged() {
        Path file = getCurrentFileFromTab();
        currentFile = file;
        if (file == null) {
            setTitle(I18n.tr("app.title"));
        } else {
            setTitle(I18n.tr("app.title") + " - " + file.getFileName());
        }
        updateStatusBar();
    }

    private RSyntaxTextArea getCurrentEditor() {
        Path file = getCurrentFileFromTab();
        if (file == null) {
            return null;
        }
        return openEditors.get(file);
    }

    private Path getCurrentFileFromTab() {
        Component selected = editorTabs.getSelectedComponent();
        if (selected == null) {
            return null;
        }
        return tabToPath.get(selected);
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

    private void configureEditorComponent(RSyntaxTextArea editor) {
        editor.setSyntaxEditingStyle("text/lucia");
        editor.setCodeFoldingEnabled(true);
        editor.setAntiAliasingEnabled(true);
        applyEditorTheme(editor);
        applyEditorFontSize(editor);
    }

    private void applyEditorTheme(RSyntaxTextArea editor) {
        String themeResource = darkTheme
                ? "/org/fife/ui/rsyntaxtextarea/themes/dark.xml"
                : "/org/fife/ui/rsyntaxtextarea/themes/idea.xml";
        try {
            Theme theme = Theme.load(getClass().getResourceAsStream(themeResource));
            theme.apply(editor);
        } catch (Exception ex) {
            // If the theme file is not found, keep the default editor appearance.
        }
        applyLuciaSyntaxColors(editor);
    }

    /**
     * Overrides token colors on top of the base theme with a curated Lucia palette.
     * Light: IntelliJ-like.  Dark: VS Code-like.
     */
    private void applyLuciaSyntaxColors(RSyntaxTextArea editor) {
        SyntaxScheme scheme = (SyntaxScheme) editor.getSyntaxScheme().clone();

        if (darkTheme) {
            // Control-flow keywords  →  VS Code blue
            scheme.getStyle(TokenTypes.RESERVED_WORD).foreground    = new Color(0x569CD6);            // null                  → VS Code orange (distinct from other keywords)
            scheme.getStyle(TokenTypes.RESERVED_WORD_2).foreground  = new Color(0xCE9178);            // Built-in types        →  VS Code pink/purple
            scheme.getStyle(TokenTypes.DATA_TYPE).foreground         = new Color(0xC586C0);
            // Built-in functions    →  VS Code pale yellow
            scheme.getStyle(TokenTypes.FUNCTION).foreground          = new Color(0xDCDCAA);
            // Booleans              →  same as keywords
            scheme.getStyle(TokenTypes.LITERAL_BOOLEAN).foreground   = new Color(0x569CD6);
            // Numbers               →  VS Code light green
            scheme.getStyle(TokenTypes.LITERAL_NUMBER_DECIMAL_INT).foreground = new Color(0xB5CEA8);
            // Strings               →  VS Code orange-brown
            scheme.getStyle(TokenTypes.LITERAL_STRING_DOUBLE_QUOTE).foreground = new Color(0xCE9178);
            // Comments (// and /* */) → VS Code muted green
            scheme.getStyle(TokenTypes.COMMENT_EOL).foreground        = new Color(0x6A9955);
            scheme.getStyle(TokenTypes.COMMENT_MULTILINE).foreground  = new Color(0x6A9955);
            // Operators             →  light gray
            scheme.getStyle(TokenTypes.OPERATOR).foreground          = new Color(0xD4D4D4);
        } else {
            // Control-flow keywords  →  IntelliJ strong blue
            scheme.getStyle(TokenTypes.RESERVED_WORD).foreground    = new Color(0x0033B3);            // null                  → IntelliJ purple (distinct from other keywords)
            scheme.getStyle(TokenTypes.RESERVED_WORD_2).foreground  = new Color(0x7B36A8);            // Built-in types        →  medium purple
            scheme.getStyle(TokenTypes.DATA_TYPE).foreground         = new Color(0x7B36A8);
            // Built-in functions    →  dark teal
            scheme.getStyle(TokenTypes.FUNCTION).foreground          = new Color(0x00627A);
            // Booleans              →  same as keywords
            scheme.getStyle(TokenTypes.LITERAL_BOOLEAN).foreground   = new Color(0x0033B3);
            // Numbers               →  bright blue (IntelliJ style)
            scheme.getStyle(TokenTypes.LITERAL_NUMBER_DECIMAL_INT).foreground = new Color(0x1750EB);
            // Strings               →  dark green
            scheme.getStyle(TokenTypes.LITERAL_STRING_DOUBLE_QUOTE).foreground = new Color(0x067D17);
            // Comments (// and /* */) → gray-green
            scheme.getStyle(TokenTypes.COMMENT_EOL).foreground        = new Color(0x5F826B);
            scheme.getStyle(TokenTypes.COMMENT_MULTILINE).foreground  = new Color(0x5F826B);
            // Operators             →  amber
            scheme.getStyle(TokenTypes.OPERATOR).foreground          = new Color(0xC77600);
        }

        editor.setSyntaxScheme(scheme);
    }

    private void applyEditorFontSize(RSyntaxTextArea editor) {
        Font current = editor.getFont();
        editor.setFont(current.deriveFont((float) editorFontSize));
    }

    private void changeEditorFontSize(int delta) {
        setEditorFontSize(editorFontSize + delta);
    }

    private void setEditorFontSize(int newSize) {
        int clamped = Math.max(MIN_EDITOR_FONT_SIZE, Math.min(MAX_EDITOR_FONT_SIZE, newSize));
        if (clamped == editorFontSize) {
            return;
        }

        editorFontSize = clamped;
        config.setEditorFontSize(editorFontSize);

        for (RSyntaxTextArea editor : openEditors.values()) {
            applyEditorFontSize(editor);
        }

        updateStatusBar();
        appendOutput(I18n.tr("log.fontSizeChanged") + ": " + editorFontSize);
    }

    private void openSettings() {
        SettingsDialog dialog = new SettingsDialog(this, config);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            appendOutput(I18n.tr("log.settingsSaved"));
        }
    }

    private void showAboutDialog() {
        String version = resolveAppVersion();
        JEditorPane content = new JEditorPane("text/html", buildAboutHtml(version));
        content.setEditable(false);
        content.setOpaque(false);
        content.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        content.addHyperlinkListener(event -> {
            if (event.getEventType() != javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                return;
            }
            if (!Desktop.isDesktopSupported()) {
                return;
            }
            try {
                Desktop.getDesktop().browse(event.getURL().toURI());
            } catch (Exception ignored) {
                // If browsing fails, keep dialog open and continue.
            }
        });

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setPreferredSize(new Dimension(620, 420));
        JOptionPane.showMessageDialog(
                this,
                scroll,
                I18n.tr("menu.about"),
                JOptionPane.INFORMATION_MESSAGE);
    }

    private String buildAboutHtml(String version) {
        String fgHex = toHex(UIManager.getColor("Label.foreground"), "#222222");
        String mutedHex = toHex(UIManager.getColor("Label.disabledForeground"), "#666666");
        String linkHex = darkTheme ? "#80b7ff" : "#0b63d1";

        return "<html><body style='font-family:Segoe UI,sans-serif;color:" + fgHex + ";padding:10px;'>"
                + "<div style='font-size:22px;font-weight:700;margin-bottom:4px;'>" + I18n.tr("app.title") + "</div>"
                + "<div style='color:" + mutedHex + ";margin-bottom:14px;'>"
                + I18n.tr("about.tagline") + "</div>"
                + "<div style='margin-bottom:12px;'><b>Version:</b> " + escapeHtml(version) + "</div>"
                + "<div style='margin-bottom:12px;line-height:1.45;'>" + I18n.tr("about.description") + "</div>"
                + "<div style='margin-top:8px;margin-bottom:4px;font-weight:700;'>" + I18n.tr("about.contactTitle") + "</div>"
                + "<div><b>" + I18n.tr("about.phone") + ":</b> " + escapeHtml(CONTACT_PHONE) + "</div>"
                + "<div><b>" + I18n.tr("about.email") + ":</b> <a style='color:" + linkHex + ";' href='mailto:"
                + CONTACT_EMAIL + "'>" + escapeHtml(CONTACT_EMAIL) + "</a></div>"
                + "<div style='margin-top:10px;margin-bottom:4px;font-weight:700;'>" + I18n.tr("about.repositoriesTitle") + "</div>"
                + "<div><b>" + I18n.tr("about.luciaRepo") + ":</b> <a style='color:" + linkHex + ";' href='"
                + LUCIA_REPO_URL + "'>" + escapeHtml(LUCIA_REPO_URL) + "</a></div>"
                + "<div><b>" + I18n.tr("about.editorRepo") + ":</b> <a style='color:" + linkHex + ";' href='"
                + LUCIA_EDITOR_REPO_URL + "'>" + escapeHtml(LUCIA_EDITOR_REPO_URL) + "</a></div>"
                + "</body></html>";
    }

    private static String toHex(Color color, String fallback) {
        if (color == null) {
            return fallback;
        }
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String resolveAppVersion() {
        Package pkg = getClass().getPackage();
        if (pkg != null) {
            String implementationVersion = pkg.getImplementationVersion();
            if (implementationVersion != null && !implementationVersion.isBlank()) {
                return implementationVersion;
            }
        }

        Path pomPath = Path.of("pom.xml");
        if (Files.isRegularFile(pomPath)) {
            try {
                String pomText = Files.readString(pomPath, StandardCharsets.UTF_8);
                Matcher matcher = Pattern.compile("<version>([^<]+)</version>").matcher(pomText);
                if (matcher.find()) {
                    return matcher.group(1).trim();
                }
            } catch (IOException ignored) {
                // Ignore and fall back to unknown.
            }
        }

        return "unknown";
    }

    private void toggleTheme(boolean dark) {
        darkTheme = dark;
        config.setDarkTheme(dark);
        try {
            if (dark) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }
        } catch (Exception ignored) {
            // Fallback: continue with previous theme.
        }
        SwingUtilities.updateComponentTreeUI(this);
        pack();
        setSize(Math.max(getWidth(), 1000), Math.max(getHeight(), 700));

        // Re-apply RSyntaxTextArea theme to all open editors.
        for (RSyntaxTextArea editor : openEditors.values()) {
            applyEditorTheme(editor);
            applyEditorFontSize(editor);
        }

        // Rebuild menu/toolbar so icon colors are recreated for the active UI theme.
        refreshTexts();
        tree.setCellRenderer(new FileTreeCellRenderer());
        SwingUtilities.updateComponentTreeUI(tree);
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
        if (statusLabel == null) {
            return;
        }

        String project = projectRoot == null ? I18n.tr("status.none") : projectRoot.getFileName().toString();
        String file = currentFile == null ? I18n.tr("status.none") : currentFile.getFileName().toString();
        statusLabel.setText(I18n.tr("status.project") + ": " + project
                + "   |   "
                + I18n.tr("status.file") + ": " + file
                + "   |   "
            + I18n.tr("status.language") + ": " + I18n.getLocale().getLanguage()
            + "   |   "
            + I18n.tr("status.font") + ": " + editorFontSize);
    }

    private void appendOutput(String line) {
        output.append(line + System.lineSeparator());
        output.setCaretPosition(output.getDocument().getLength());
    }

    private void appendOutputRaw(String text) {
        output.append(text);
        output.setCaretPosition(output.getDocument().getLength());
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, I18n.tr("dialog.error"), JOptionPane.ERROR_MESSAGE);
        appendOutput(I18n.tr("log.error") + ": " + message);
    }
}
