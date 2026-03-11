package com.lucia.editor.ui;

import com.lucia.editor.config.EditorConfig;
import com.lucia.editor.i18n.I18n;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.nio.file.Path;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

public class SettingsDialog extends JDialog {

    private final EditorConfig config;
    private final JTextField luciaRootField;
    private final JTextField pythonField;
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
    private final JTextField terminalShellField;
    private final JComboBox<String> languageCombo;
    private final JComboBox<String> themeCombo;
    private final JComboBox<String> runTargetCombo;
    private final JComboBox<String> terminalDirModeCombo;
    private final JSpinner fontSizeSpinner;
    private final JSpinner tabSizeSpinner;
    private final JSpinner autocompleteDelaySpinner;
    private final JSpinner diagnosticsDebounceSpinner;
    private final JSpinner diagnosticsUnderlineRowsSpinner;
    private boolean saved;

    public SettingsDialog(Frame owner, EditorConfig config) {
        super(owner, I18n.tr("settings.title"), true);
        this.config = config;
        this.luciaRootField = new JTextField(config.getLuciaProjectRoot().toString(), 28);
        this.pythonField = new JTextField(config.getPythonExecutable(), 28);
        this.formatOnSaveBox = new JCheckBox(I18n.tr("settings.formatOnSave"), config.isFormatOnSave());
        this.wordWrapBox = new JCheckBox(I18n.tr("settings.wordWrap"), config.isWordWrap());
        this.insertSpacesBox = new JCheckBox(I18n.tr("settings.insertSpaces"), config.isInsertSpaces());
        this.showLineNumbersBox = new JCheckBox(I18n.tr("settings.showLineNumbers"), config.isShowLineNumbers());
        this.highlightLineBox = new JCheckBox(I18n.tr("settings.highlightCurrentLine"), config.isHighlightCurrentLine());
        this.showWhitespaceBox = new JCheckBox(I18n.tr("settings.showWhitespace"), config.isShowWhitespace());
        this.autocompleteEnabledBox = new JCheckBox(I18n.tr("settings.autocompleteEnabled"), config.isAutocompleteEnabled());
        this.snippetsInAutocompleteBox = new JCheckBox(I18n.tr("settings.snippetsInAutocomplete"), config.isSnippetsInAutocomplete());
        this.diagnosticsEnabledBox = new JCheckBox(I18n.tr("settings.diagnosticsEnabled"), config.isDiagnosticsEnabled());
        this.diagnosticsAutoOpenProblemsBox = new JCheckBox(I18n.tr("settings.diagnosticsAutoOpenProblems"),
                config.isDiagnosticsAutoOpenProblems());
        this.saveBeforeRunBox = new JCheckBox(I18n.tr("settings.saveBeforeRun"), config.isSaveBeforeRun());
        this.showGeneratedCodeBox = new JCheckBox(I18n.tr("settings.showGeneratedCode"), config.isShowGeneratedCode());
        this.clearOutputBeforeRunBox = new JCheckBox(I18n.tr("settings.clearOutputBeforeRun"), config.isClearOutputBeforeRun());
        this.terminalAutoStartBox = new JCheckBox(I18n.tr("settings.terminalAutoStart"), config.isTerminalAutoStart());
        this.terminalShellField = new JTextField(config.getTerminalShellPath(), 28);

        this.languageCombo = new JComboBox<>(new String[]{"en", "es"});
        this.languageCombo.setSelectedItem(config.getLanguageTag().startsWith("es") ? "es" : "en");

        this.themeCombo = new JComboBox<>(new String[]{"light", "dark"});
        this.themeCombo.setSelectedItem(config.isDarkTheme() ? "dark" : "light");

        this.runTargetCombo = new JComboBox<>(new String[]{"python", "javascript"});
        this.runTargetCombo.setSelectedItem("javascript".equalsIgnoreCase(config.getDefaultTarget()) ? "javascript" : "python");

        this.terminalDirModeCombo = new JComboBox<>(new String[]{"project", "home"});
        this.terminalDirModeCombo.setSelectedItem("home".equalsIgnoreCase(config.getTerminalWorkingDirectoryMode()) ? "home" : "project");

        this.fontSizeSpinner = new JSpinner(new SpinnerNumberModel(config.getEditorFontSize(), 10, 32, 1));
        this.tabSizeSpinner = new JSpinner(new SpinnerNumberModel(config.getTabSize(), 2, 8, 1));
        this.autocompleteDelaySpinner = new JSpinner(new SpinnerNumberModel(config.getAutocompleteDelayMs(), 50, 1500, 50));
        this.diagnosticsDebounceSpinner = new JSpinner(new SpinnerNumberModel(config.getDiagnosticsDebounceMs(), 100, 2000, 50));
        this.diagnosticsUnderlineRowsSpinner = new JSpinner(new SpinnerNumberModel(config.getDiagnosticsUnderlineRows(), 1, 4, 1));

        this.saved = false;

        buildUi();
        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isSaved() {
        return saved;
    }

    private void buildUi() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        form.add(makeTextRow(I18n.tr("settings.luciaRoot"), luciaRootField, true));
        form.add(makeTextRow(I18n.tr("settings.pythonExec"), pythonField, false));
        form.add(makeTextRow(I18n.tr("settings.terminalShellPath"), terminalShellField, false));

        form.add(makeComboRow(I18n.tr("settings.language"), languageCombo));
        form.add(makeComboRow(I18n.tr("settings.theme"), themeCombo));
        form.add(makeComboRow(I18n.tr("settings.runTarget"), runTargetCombo));
        form.add(makeComboRow(I18n.tr("settings.terminalWorkingDirMode"), terminalDirModeCombo));

        form.add(makeSpinnerRow(I18n.tr("settings.fontSize"), fontSizeSpinner));
        form.add(makeSpinnerRow(I18n.tr("settings.tabSize"), tabSizeSpinner));
        form.add(makeSpinnerRow(I18n.tr("settings.autocompleteDelay"), autocompleteDelaySpinner));
        form.add(makeSpinnerRow(I18n.tr("settings.diagnosticsDebounce"), diagnosticsDebounceSpinner));
        form.add(makeSpinnerRow(I18n.tr("settings.diagnosticsUnderlineRows"), diagnosticsUnderlineRowsSpinner));

        form.add(makeCheckRow(formatOnSaveBox));
        form.add(makeCheckRow(wordWrapBox));
        form.add(makeCheckRow(insertSpacesBox));
        form.add(makeCheckRow(showLineNumbersBox));
        form.add(makeCheckRow(highlightLineBox));
        form.add(makeCheckRow(showWhitespaceBox));
        form.add(makeCheckRow(autocompleteEnabledBox));
        form.add(makeCheckRow(snippetsInAutocompleteBox));
        form.add(makeCheckRow(diagnosticsEnabledBox));
        form.add(makeCheckRow(diagnosticsAutoOpenProblemsBox));
        form.add(makeCheckRow(saveBeforeRunBox));
        form.add(makeCheckRow(showGeneratedCodeBox));
        form.add(makeCheckRow(clearOutputBeforeRunBox));
        form.add(makeCheckRow(terminalAutoStartBox));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton(I18n.tr("button.cancel"));
        cancel.addActionListener(e -> dispose());

        JButton save = new JButton(I18n.tr("button.save"));
        save.addActionListener(e -> onSave());

        actions.add(cancel);
        actions.add(save);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
    }

    private JPanel makeTextRow(String label, JTextField field, boolean withBrowse) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row.add(new JLabel(label));
        row.add(field);
        if (withBrowse) {
            JButton browse = new JButton(I18n.tr("settings.browse"));
            browse.addActionListener(e -> chooseRoot());
            row.add(browse);
        }
        return row;
    }

    private JPanel makeComboRow(String label, JComboBox<String> combo) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row.add(new JLabel(label));
        row.add(combo);
        return row;
    }

    private JPanel makeSpinnerRow(String label, JSpinner spinner) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row.add(new JLabel(label));
        row.add(spinner);
        return row;
    }

    private JPanel makeCheckRow(JCheckBox box) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
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

    private void onSave() {
        String rootValue = luciaRootField.getText().trim();
        String pythonValue = pythonField.getText().trim();

        if (rootValue.isEmpty() || pythonValue.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    I18n.tr("settings.validation"),
                    I18n.tr("dialog.error"),
                    JOptionPane.ERROR_MESSAGE
            );
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
        config.setDefaultTarget((String) runTargetCombo.getSelectedItem());
        config.setTerminalAutoStart(terminalAutoStartBox.isSelected());
        config.setTerminalShellPath(terminalShellField.getText());
        config.setTerminalWorkingDirectoryMode((String) terminalDirModeCombo.getSelectedItem());

        config.setEditorFontSize((Integer) fontSizeSpinner.getValue());
        config.setTabSize((Integer) tabSizeSpinner.getValue());
        config.setDarkTheme("dark".equals(themeCombo.getSelectedItem()));
        config.setLanguageTag((String) languageCombo.getSelectedItem());

        saved = true;
        dispose();
    }
}
