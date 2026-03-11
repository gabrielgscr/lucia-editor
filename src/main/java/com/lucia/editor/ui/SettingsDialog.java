package com.lucia.editor.ui;

import com.lucia.editor.config.EditorConfig;
import com.lucia.editor.i18n.I18n;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.nio.file.Path;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class SettingsDialog extends JDialog {

    private final EditorConfig config;
    private final JTextField luciaRootField;
    private final JTextField pythonField;
    private final JCheckBox formatOnSaveBox;
    private boolean saved;

    public SettingsDialog(Frame owner, EditorConfig config) {
        super(owner, I18n.tr("settings.title"), true);
        this.config = config;
        this.luciaRootField = new JTextField(config.getLuciaProjectRoot().toString(), 30);
        this.pythonField = new JTextField(config.getPythonExecutable(), 30);
        this.formatOnSaveBox = new JCheckBox(I18n.tr("settings.formatOnSave"), config.isFormatOnSave());
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
        form.setLayout(new javax.swing.BoxLayout(form, javax.swing.BoxLayout.Y_AXIS));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.add(new JLabel(I18n.tr("settings.luciaRoot")));
        row1.add(luciaRootField);

        JButton browse = new JButton(I18n.tr("settings.browse"));
        browse.addActionListener(e -> chooseRoot());
        row1.add(browse);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row2.add(new JLabel(I18n.tr("settings.pythonExec")));
        row2.add(pythonField);

        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row3.add(formatOnSaveBox);

        form.add(row1);
        form.add(row2);
        form.add(row3);

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
        saved = true;
        dispose();
    }
}
