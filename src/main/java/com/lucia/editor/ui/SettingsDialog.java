package com.lucia.editor.ui;

import com.lucia.editor.config.EditorConfig;
import com.lucia.editor.i18n.I18n;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;

public class SettingsDialog extends JDialog {

    private static final String CAT_GENERAL = "general";
    private static final String CAT_EDITOR = "editor";
    private static final String CAT_AUTOCOMPLETE = "autocomplete";
    private static final String CAT_DIAGNOSTICS = "diagnostics";
    private static final String CAT_RUN = "run";
    private static final String CAT_TERMINAL = "terminal";
    private static final String CAT_CLI = "cli";

    private final EditorConfig config;
    private final Runnable applyCallback;

    private final JList<CategoryItem> categoryList;
    private final CardLayout cardLayout;
    private final JPanel cardsPanel;

    private final JTextField luciaRootField;
    private final JTextField pythonField;
    private final JTextField terminalShellField;

    private final JCheckBox formatOnSaveBox;
    private final JCheckBox wordWrapBox;
    private final JCheckBox insertSpacesBox;
    private final JCheckBox showLineNumbersBox;
    private final JCheckBox highlightLineBox;
    private final JCheckBox showWhitespaceBox;
    private final JCheckBox autocompleteEnabledBox;
    private final JCheckBox snippetsInAutocompleteBox;
    private final JCheckBox diagnosticsEnabledBox;
    private final JCheckBox diagnosticsAutoOpenProblemsBox;
    private final JCheckBox saveBeforeRunBox;
    private final JCheckBox showGeneratedCodeBox;
    private final JCheckBox clearOutputBeforeRunBox;
    private final JCheckBox terminalAutoStartBox;

    private final JComboBox<OptionItem> languageCombo;
    private final JComboBox<OptionItem> themeCombo;
    private final JComboBox<OptionItem> runTargetCombo;
    private final JComboBox<OptionItem> terminalDirModeCombo;

    private final JSpinner fontSizeSpinner;
    private final JSpinner tabSizeSpinner;
    private final JSpinner autocompleteDelaySpinner;
    private final JSpinner diagnosticsDebounceSpinner;
    private final JSpinner diagnosticsUnderlineRowsSpinner;

    private boolean saved;

    public SettingsDialog(Frame owner, EditorConfig config, Runnable applyCallback) {
        super(owner, I18n.tr("settings.title"), true);
        this.config = config;
        this.applyCallback = applyCallback;
        this.saved = false;

        this.luciaRootField = new JTextField(22);
        this.pythonField = new JTextField(22);
        this.terminalShellField = new JTextField(22);

        this.formatOnSaveBox = new JCheckBox(I18n.tr("settings.formatOnSave"));
        this.wordWrapBox = new JCheckBox(I18n.tr("settings.wordWrap"));
        this.insertSpacesBox = new JCheckBox(I18n.tr("settings.insertSpaces"));
        this.showLineNumbersBox = new JCheckBox(I18n.tr("settings.showLineNumbers"));
        this.highlightLineBox = new JCheckBox(I18n.tr("settings.highlightCurrentLine"));
        this.showWhitespaceBox = new JCheckBox(I18n.tr("settings.showWhitespace"));
        this.autocompleteEnabledBox = new JCheckBox(I18n.tr("settings.autocompleteEnabled"));
        this.snippetsInAutocompleteBox = new JCheckBox(I18n.tr("settings.snippetsInAutocomplete"));
        this.diagnosticsEnabledBox = new JCheckBox(I18n.tr("settings.diagnosticsEnabled"));
        this.diagnosticsAutoOpenProblemsBox = new JCheckBox(I18n.tr("settings.diagnosticsAutoOpenProblems"));
        this.saveBeforeRunBox = new JCheckBox(I18n.tr("settings.saveBeforeRun"));
        this.showGeneratedCodeBox = new JCheckBox(I18n.tr("settings.showGeneratedCode"));
        this.clearOutputBeforeRunBox = new JCheckBox(I18n.tr("settings.clearOutputBeforeRun"));
        this.terminalAutoStartBox = new JCheckBox(I18n.tr("settings.terminalAutoStart"));

        this.languageCombo = new JComboBox<>(new OptionItem[]{
                new OptionItem(I18n.tr("language.english"), "en"),
                new OptionItem(I18n.tr("language.spanish"), "es")
        });
        this.themeCombo = new JComboBox<>(new OptionItem[]{
                new OptionItem(I18n.tr("settings.theme.light"), "light"),
                new OptionItem(I18n.tr("settings.theme.dark"), "dark")
        });
        this.runTargetCombo = new JComboBox<>(new OptionItem[]{
                new OptionItem(I18n.tr("settings.target.python"), "python"),
                new OptionItem(I18n.tr("settings.target.javascript"), "javascript")
        });
        this.terminalDirModeCombo = new JComboBox<>(new OptionItem[]{
                new OptionItem(I18n.tr("settings.terminalDir.project"), "project"),
                new OptionItem(I18n.tr("settings.terminalDir.home"), "home")
        });

        this.fontSizeSpinner = new JSpinner(new SpinnerNumberModel(15, 10, 32, 1));
        this.tabSizeSpinner = new JSpinner(new SpinnerNumberModel(4, 2, 8, 1));
        this.autocompleteDelaySpinner = new JSpinner(new SpinnerNumberModel(250, 50, 1500, 50));
        this.diagnosticsDebounceSpinner = new JSpinner(new SpinnerNumberModel(450, 100, 2000, 50));
        this.diagnosticsUnderlineRowsSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 4, 1));

        this.categoryList = new JList<>(new CategoryItem[]{
                new CategoryItem(I18n.tr("settings.category.general"), CAT_GENERAL),
                new CategoryItem(I18n.tr("settings.category.editor"), CAT_EDITOR),
                new CategoryItem(I18n.tr("settings.category.autocomplete"), CAT_AUTOCOMPLETE),
                new CategoryItem(I18n.tr("settings.category.diagnostics"), CAT_DIAGNOSTICS),
                new CategoryItem(I18n.tr("settings.category.run"), CAT_RUN),
                new CategoryItem(I18n.tr("settings.category.terminal"), CAT_TERMINAL),
                new CategoryItem(I18n.tr("settings.category.cli"), CAT_CLI)
        });
        this.cardLayout = new CardLayout();
        this.cardsPanel = new JPanel(cardLayout);

        loadFromConfig();
        buildUi();

        setMinimumSize(new Dimension(820, 520));
        setPreferredSize(new Dimension(900, 580));
        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isSaved() {
        return saved;
    }

    private void buildUi() {
        categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        categoryList.setSelectedIndex(0);
        categoryList.addListSelectionListener(e -> {
            CategoryItem item = categoryList.getSelectedValue();
            if (item != null) {
                cardLayout.show(cardsPanel, item.id());
            }
        });

        JScrollPane sidebarScroll = new JScrollPane(categoryList);
        sidebarScroll.setPreferredSize(new Dimension(185, 500));

        cardsPanel.add(wrapInScroll(buildGeneralPanel()), CAT_GENERAL);
        cardsPanel.add(wrapInScroll(buildEditorPanel()), CAT_EDITOR);
        cardsPanel.add(wrapInScroll(buildAutocompletePanel()), CAT_AUTOCOMPLETE);
        cardsPanel.add(wrapInScroll(buildDiagnosticsPanel()), CAT_DIAGNOSTICS);
        cardsPanel.add(wrapInScroll(buildRunPanel()), CAT_RUN);
        cardsPanel.add(wrapInScroll(buildTerminalPanel()), CAT_TERMINAL);
        cardsPanel.add(wrapInScroll(buildCliPanel()), CAT_CLI);

        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        center.add(cardsPanel, BorderLayout.CENTER);

        JPanel split = new JPanel(new BorderLayout(6, 0));
        split.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        split.add(sidebarScroll, BorderLayout.WEST);
        split.add(center, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton restoreDefaults = new JButton(I18n.tr("button.restoreDefaults"));
        restoreDefaults.addActionListener(e -> loadDefaults());

        JButton cancel = new JButton(I18n.tr("button.cancel"));
        cancel.addActionListener(e -> dispose());

        JButton apply = new JButton(I18n.tr("button.apply"));
        apply.addActionListener(e -> onApply(false));

        JButton save = new JButton(I18n.tr("button.save"));
        save.addActionListener(e -> onApply(true));

        actions.add(restoreDefaults);
        actions.add(cancel);
        actions.add(apply);
        actions.add(save);

        setLayout(new BorderLayout());
        add(split, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
    }

    private JScrollPane wrapInScroll(JPanel panel) {
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return scrollPane;
    }

    private JPanel buildGeneralPanel() {
        JPanel panel = createSectionPanel(I18n.tr("settings.category.general"));
        panel.add(makeComboRow(I18n.tr("settings.language"), languageCombo));
        panel.add(makeComboRow(I18n.tr("settings.theme"), themeCombo));
        panel.add(makeSpinnerRow(I18n.tr("settings.fontSize"), fontSizeSpinner));
        return panel;
    }

    private JPanel buildEditorPanel() {
        JPanel panel = createSectionPanel(I18n.tr("settings.category.editor"));
        panel.add(makeSpinnerRow(I18n.tr("settings.tabSize"), tabSizeSpinner));
        panel.add(makeCheckRow(formatOnSaveBox));
        panel.add(makeCheckRow(wordWrapBox));
        panel.add(makeCheckRow(insertSpacesBox));
        panel.add(makeCheckRow(showLineNumbersBox));
        panel.add(makeCheckRow(highlightLineBox));
        panel.add(makeCheckRow(showWhitespaceBox));
        return panel;
    }

    private JPanel buildAutocompletePanel() {
        JPanel panel = createSectionPanel(I18n.tr("settings.category.autocomplete"));
        panel.add(makeSpinnerRow(I18n.tr("settings.autocompleteDelay"), autocompleteDelaySpinner));
        panel.add(makeCheckRow(autocompleteEnabledBox));
        panel.add(makeCheckRow(snippetsInAutocompleteBox));
        return panel;
    }

    private JPanel buildDiagnosticsPanel() {
        JPanel panel = createSectionPanel(I18n.tr("settings.category.diagnostics"));
        panel.add(makeSpinnerRow(I18n.tr("settings.diagnosticsDebounce"), diagnosticsDebounceSpinner));
        panel.add(makeSpinnerRow(I18n.tr("settings.diagnosticsUnderlineRows"), diagnosticsUnderlineRowsSpinner));
        panel.add(makeCheckRow(diagnosticsEnabledBox));
        panel.add(makeCheckRow(diagnosticsAutoOpenProblemsBox));
        return panel;
    }

    private JPanel buildRunPanel() {
        JPanel panel = createSectionPanel(I18n.tr("settings.category.run"));
        panel.add(makeComboRow(I18n.tr("settings.runTarget"), runTargetCombo));
        panel.add(makeCheckRow(saveBeforeRunBox));
        panel.add(makeCheckRow(showGeneratedCodeBox));
        panel.add(makeCheckRow(clearOutputBeforeRunBox));
        return panel;
    }

    private JPanel buildTerminalPanel() {
        JPanel panel = createSectionPanel(I18n.tr("settings.category.terminal"));
        panel.add(makeComboRow(I18n.tr("settings.terminalWorkingDirMode"), terminalDirModeCombo));
        panel.add(makeTextRow(I18n.tr("settings.terminalShellPath"), terminalShellField, false));
        panel.add(makeCheckRow(terminalAutoStartBox));
        return panel;
    }

    private JPanel buildCliPanel() {
        JPanel panel = createSectionPanel(I18n.tr("settings.category.cli"));
        panel.add(makeTextRow(I18n.tr("settings.luciaRoot"), luciaRootField, true));
        panel.add(makeTextRow(I18n.tr("settings.pythonExec"), pythonField, false));
        return panel;
    }

    private JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JLabel heading = new JLabel(title);
        heading.setFont(heading.getFont().deriveFont(16f));
        panel.add(heading);
        panel.add(Box.createVerticalStrut(0));
        return panel;
    }

    private JPanel makeTextRow(String label, JTextField field, boolean withBrowse) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.add(new JLabel(label));
        row.add(field);
        if (withBrowse) {
            JButton browse = new JButton(I18n.tr("settings.browse"));
            browse.addActionListener(e -> chooseRoot());
            row.add(browse);
        }
        return row;
    }

    private JPanel makeComboRow(String label, JComboBox<?> combo) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setAlignmentX(LEFT_ALIGNMENT);
        combo.setPreferredSize(new Dimension(190, combo.getPreferredSize().height));
        row.add(new JLabel(label));
        row.add(combo);
        return row;
    }

    private JPanel makeSpinnerRow(String label, JSpinner spinner) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setAlignmentX(LEFT_ALIGNMENT);
        spinner.setPreferredSize(new Dimension(90, spinner.getPreferredSize().height));
        row.add(new JLabel(label));
        row.add(spinner);
        return row;
    }

    private JPanel makeCheckRow(JCheckBox box) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.add(box);
        return row;
    }

    private void chooseRoot() {
        JFileChooser chooser = new JFileChooser(luciaRootField.getText());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            luciaRootField.setText(chooser.getSelectedFile().toPath().toString());
        }
    }

    private void onApply(boolean closeAfterApply) {
        String rootValue = luciaRootField.getText().trim();
        String pythonValue = pythonField.getText().trim();

        if (rootValue.isEmpty() || pythonValue.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    I18n.tr("settings.validation"),
                    I18n.tr("dialog.error"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        config.setLuciaProjectRoot(Path.of(rootValue));
        config.setPythonExecutable(pythonValue);

        config.setFormatOnSave(formatOnSaveBox.isSelected());
        config.setWordWrap(wordWrapBox.isSelected());
        config.setInsertSpaces(insertSpacesBox.isSelected());
        config.setShowLineNumbers(showLineNumbersBox.isSelected());
        config.setHighlightCurrentLine(highlightLineBox.isSelected());
        config.setShowWhitespace(showWhitespaceBox.isSelected());

        config.setAutocompleteEnabled(autocompleteEnabledBox.isSelected());
        config.setSnippetsInAutocomplete(snippetsInAutocompleteBox.isSelected());
        config.setAutocompleteDelayMs((Integer) autocompleteDelaySpinner.getValue());

        config.setDiagnosticsEnabled(diagnosticsEnabledBox.isSelected());
        config.setDiagnosticsDebounceMs((Integer) diagnosticsDebounceSpinner.getValue());
        config.setDiagnosticsUnderlineRows((Integer) diagnosticsUnderlineRowsSpinner.getValue());
        config.setDiagnosticsAutoOpenProblems(diagnosticsAutoOpenProblemsBox.isSelected());

        config.setSaveBeforeRun(saveBeforeRunBox.isSelected());
        config.setShowGeneratedCode(showGeneratedCodeBox.isSelected());
        config.setClearOutputBeforeRun(clearOutputBeforeRunBox.isSelected());
        config.setDefaultTarget(selectedValue(runTargetCombo, "python"));

        config.setTerminalAutoStart(terminalAutoStartBox.isSelected());
        config.setTerminalShellPath(terminalShellField.getText());
        config.setTerminalWorkingDirectoryMode(selectedValue(terminalDirModeCombo, "project"));

        config.setEditorFontSize((Integer) fontSizeSpinner.getValue());
        config.setTabSize((Integer) tabSizeSpinner.getValue());
        config.setDarkTheme("dark".equals(selectedValue(themeCombo, "light")));
        config.setLanguageTag(selectedValue(languageCombo, "en"));

        if (applyCallback != null) {
            applyCallback.run();
        }

        if (closeAfterApply) {
            saved = true;
            dispose();
        }
    }

    private void loadFromConfig() {
        luciaRootField.setText(config.getLuciaProjectRoot().toString());
        pythonField.setText(config.getPythonExecutable());
        terminalShellField.setText(config.getTerminalShellPath());

        formatOnSaveBox.setSelected(config.isFormatOnSave());
        wordWrapBox.setSelected(config.isWordWrap());
        insertSpacesBox.setSelected(config.isInsertSpaces());
        showLineNumbersBox.setSelected(config.isShowLineNumbers());
        highlightLineBox.setSelected(config.isHighlightCurrentLine());
        showWhitespaceBox.setSelected(config.isShowWhitespace());

        autocompleteEnabledBox.setSelected(config.isAutocompleteEnabled());
        snippetsInAutocompleteBox.setSelected(config.isSnippetsInAutocomplete());
        diagnosticsEnabledBox.setSelected(config.isDiagnosticsEnabled());
        diagnosticsAutoOpenProblemsBox.setSelected(config.isDiagnosticsAutoOpenProblems());

        saveBeforeRunBox.setSelected(config.isSaveBeforeRun());
        showGeneratedCodeBox.setSelected(config.isShowGeneratedCode());
        clearOutputBeforeRunBox.setSelected(config.isClearOutputBeforeRun());
        terminalAutoStartBox.setSelected(config.isTerminalAutoStart());

        fontSizeSpinner.setValue(config.getEditorFontSize());
        tabSizeSpinner.setValue(config.getTabSize());
        autocompleteDelaySpinner.setValue(config.getAutocompleteDelayMs());
        diagnosticsDebounceSpinner.setValue(config.getDiagnosticsDebounceMs());
        diagnosticsUnderlineRowsSpinner.setValue(config.getDiagnosticsUnderlineRows());

        selectValue(languageCombo, config.getLanguageTag().startsWith("es") ? "es" : "en");
        selectValue(themeCombo, config.isDarkTheme() ? "dark" : "light");
        selectValue(runTargetCombo, "javascript".equalsIgnoreCase(config.getDefaultTarget()) ? "javascript" : "python");
        selectValue(terminalDirModeCombo,
                "home".equalsIgnoreCase(config.getTerminalWorkingDirectoryMode()) ? "home" : "project");
    }

    private void loadDefaults() {
        luciaRootField.setText("../lucia");
        pythonField.setText(".venv/bin/python");
        terminalShellField.setText("");

        formatOnSaveBox.setSelected(false);
        wordWrapBox.setSelected(false);
        insertSpacesBox.setSelected(true);
        showLineNumbersBox.setSelected(true);
        highlightLineBox.setSelected(true);
        showWhitespaceBox.setSelected(false);

        autocompleteEnabledBox.setSelected(true);
        snippetsInAutocompleteBox.setSelected(true);
        diagnosticsEnabledBox.setSelected(true);
        diagnosticsAutoOpenProblemsBox.setSelected(false);

        saveBeforeRunBox.setSelected(true);
        showGeneratedCodeBox.setSelected(false);
        clearOutputBeforeRunBox.setSelected(false);
        terminalAutoStartBox.setSelected(false);

        fontSizeSpinner.setValue(15);
        tabSizeSpinner.setValue(4);
        autocompleteDelaySpinner.setValue(250);
        diagnosticsDebounceSpinner.setValue(450);
        diagnosticsUnderlineRowsSpinner.setValue(2);

        selectValue(languageCombo, "en");
        selectValue(themeCombo, "light");
        selectValue(runTargetCombo, "python");
        selectValue(terminalDirModeCombo, "project");
    }

    private static String selectedValue(JComboBox<OptionItem> combo, String fallback) {
        OptionItem item = (OptionItem) combo.getSelectedItem();
        return item == null ? fallback : item.value();
    }

    private static void selectValue(JComboBox<OptionItem> combo, String value) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            OptionItem item = combo.getItemAt(i);
            if (item.value().equalsIgnoreCase(value)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        if (combo.getItemCount() > 0) {
            combo.setSelectedIndex(0);
        }
    }

    private record CategoryItem(String label, String id) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record OptionItem(String label, String value) {
        @Override
        public String toString() {
            return label;
        }
    }
}
