package com.lucia.editor.ui;

import com.lucia.editor.i18n.I18n;
import com.lucia.editor.snippets.SnippetDefinition;
import com.lucia.editor.snippets.SnippetManager;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

public class SnippetManagerDialog extends JDialog {

    private final SnippetManager snippetManager;
    private final Runnable onSnippetsChanged;

    private final DefaultListModel<SnippetDefinition> listModel;
    private final JList<SnippetDefinition> snippetList;
    private final JTextField prefixField;
    private final JTextField descriptionField;
    private final JTextArea templateArea;

    public SnippetManagerDialog(Frame owner, SnippetManager snippetManager, Runnable onSnippetsChanged) {
        super(owner, I18n.tr("snippets.title"), true);
        this.snippetManager = snippetManager;
        this.onSnippetsChanged = onSnippetsChanged;
        this.listModel = new DefaultListModel<>();
        this.snippetList = new JList<>(listModel);
        this.prefixField = new JTextField(24);
        this.descriptionField = new JTextField(24);
        this.templateArea = new JTextArea(10, 36);

        buildUi();
        loadSnippets();
        setPreferredSize(new Dimension(900, 520));
        pack();
        setLocationRelativeTo(owner);
    }

    private void buildUi() {
        setLayout(new BorderLayout(8, 8));

        snippetList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        snippetList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelected();
            }
        });

        JScrollPane listScroll = new JScrollPane(snippetList);
        listScroll.setPreferredSize(new Dimension(300, 420));

        JPanel editorPanel = new JPanel();
        editorPanel.setLayout(new javax.swing.BoxLayout(editorPanel, javax.swing.BoxLayout.Y_AXIS));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.add(new JLabel(I18n.tr("snippets.prefix")));
        row1.add(prefixField);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row2.add(new JLabel(I18n.tr("snippets.description")));
        row2.add(descriptionField);

        JPanel row3 = new JPanel(new BorderLayout());
        row3.add(new JLabel(I18n.tr("snippets.template")), BorderLayout.NORTH);
        row3.add(new JScrollPane(templateArea), BorderLayout.CENTER);

        editorPanel.add(row1);
        editorPanel.add(row2);
        editorPanel.add(row3);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(listScroll, BorderLayout.WEST);
        center.add(editorPanel, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton newButton = new JButton(I18n.tr("snippets.new"));
        newButton.addActionListener(e -> clearEditor());

        JButton saveButton = new JButton(I18n.tr("snippets.save"));
        saveButton.addActionListener(e -> saveSnippet());

        JButton deleteButton = new JButton(I18n.tr("snippets.delete"));
        deleteButton.addActionListener(e -> deleteSnippet());

        JButton openFileButton = new JButton(I18n.tr("snippets.openFile"));
        openFileButton.addActionListener(e -> openSnippetsFile());

        JButton closeButton = new JButton(I18n.tr("button.cancel"));
        closeButton.addActionListener(e -> dispose());

        actions.add(newButton);
        actions.add(saveButton);
        actions.add(deleteButton);
        actions.add(openFileButton);
        actions.add(closeButton);

        add(center, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
    }

    private void loadSnippets() {
        listModel.clear();
        try {
            for (SnippetDefinition snippet : snippetManager.getSnippets()) {
                listModel.addElement(snippet);
            }
            if (!listModel.isEmpty()) {
                snippetList.setSelectedIndex(0);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    I18n.tr("snippets.loadError") + ": " + ex.getMessage(),
                    I18n.tr("dialog.error"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSelected() {
        SnippetDefinition selected = snippetList.getSelectedValue();
        if (selected == null) {
            return;
        }
        prefixField.setText(selected.prefix());
        descriptionField.setText(selected.description());
        templateArea.setText(selected.template());
    }

    private void clearEditor() {
        snippetList.clearSelection();
        prefixField.setText("");
        descriptionField.setText("");
        templateArea.setText("");
        prefixField.requestFocusInWindow();
    }

    private void saveSnippet() {
        String prefix = prefixField.getText().trim();
        String description = descriptionField.getText().trim();
        String template = templateArea.getText();

        if (prefix.isBlank() || template.isBlank()) {
            JOptionPane.showMessageDialog(this,
                    I18n.tr("snippets.validation"),
                    I18n.tr("dialog.error"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<SnippetDefinition> snippets = new ArrayList<>();
        for (int i = 0; i < listModel.size(); i++) {
            SnippetDefinition current = listModel.get(i);
            if (!current.prefix().equals(prefix)) {
                snippets.add(current);
            }
        }
        SnippetDefinition updated = new SnippetDefinition(prefix, description, template);
        snippets.add(updated);

        try {
            snippetManager.saveSnippets(snippets);
            loadSnippets();
            selectByPrefix(prefix);
            if (onSnippetsChanged != null) {
                onSnippetsChanged.run();
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    I18n.tr("snippets.saveError") + ": " + ex.getMessage(),
                    I18n.tr("dialog.error"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSnippet() {
        SnippetDefinition selected = snippetList.getSelectedValue();
        if (selected == null) {
            return;
        }

        int confirmation = JOptionPane.showConfirmDialog(
                this,
                I18n.tr("snippets.deleteConfirm") + " " + selected.prefix() + "?",
                I18n.tr("snippets.title"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirmation != JOptionPane.OK_OPTION) {
            return;
        }

        List<SnippetDefinition> snippets = new ArrayList<>();
        for (int i = 0; i < listModel.size(); i++) {
            SnippetDefinition current = listModel.get(i);
            if (!current.prefix().equals(selected.prefix())) {
                snippets.add(current);
            }
        }

        try {
            snippetManager.saveSnippets(snippets);
            loadSnippets();
            if (onSnippetsChanged != null) {
                onSnippetsChanged.run();
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    I18n.tr("snippets.saveError") + ": " + ex.getMessage(),
                    I18n.tr("dialog.error"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openSnippetsFile() {
        try {
            snippetManager.getSnippets();
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(snippetManager.getStorageFile().toFile());
            } else {
                JOptionPane.showMessageDialog(this,
                        snippetManager.getStorageFile().toString(),
                        I18n.tr("snippets.filePath"),
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    I18n.tr("snippets.openError") + ": " + ex.getMessage(),
                    I18n.tr("dialog.error"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void selectByPrefix(String prefix) {
        for (int i = 0; i < listModel.size(); i++) {
            if (listModel.get(i).prefix().equals(prefix)) {
                snippetList.setSelectedIndex(i);
                return;
            }
        }
    }
}
