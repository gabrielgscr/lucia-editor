/*
 * Copyright (c) 2026 Gabriel González
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.lucia.editor.ui;

import com.lucia.editor.i18n.I18n;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * Project-wide search dialog for all .lucia files.
 */
public class GlobalSearchDialog extends JDialog {

    private final Supplier<Path> projectRootSupplier;
    private final Consumer<SearchMatch> onNavigate;
    private final Consumer<String> onLog;

    private final JTextField queryField;
    private final JTextField replacementField;
    private final JTextField folderFilterField;
    private final JCheckBox caseSensitive;
    private final JCheckBox useRegex;
    private final JButton searchButton;
    private final JButton replaceSelectedButton;
    private final JButton replaceAllButton;
    private final JLabel resultLabel;
    private final JList<SearchMatch> resultList;

    public GlobalSearchDialog(java.awt.Frame owner,
                              Supplier<Path> projectRootSupplier,
                              Consumer<SearchMatch> onNavigate,
                              Consumer<String> onLog) {
        super(owner, I18n.tr("search.globalTitle"), false);
        this.projectRootSupplier = projectRootSupplier;
        this.onNavigate = onNavigate;
        this.onLog = onLog;

        queryField = new JTextField();
        replacementField = new JTextField();
        folderFilterField = new JTextField();
        caseSensitive = new JCheckBox(I18n.tr("search.caseSensitive"));
        useRegex = new JCheckBox(I18n.tr("search.useRegex"));
        searchButton = new JButton(I18n.tr("search.searchButton"));
        replaceSelectedButton = new JButton(I18n.tr("search.replaceSelected"));
        replaceAllButton = new JButton(I18n.tr("search.replaceAllButton"));
        resultLabel = new JLabel(I18n.tr("search.results") + ": 0");
        resultList = new JList<>();

        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout(8, 8));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new GridLayout(3, 1, 0, 6));
        JPanel queryRow = new JPanel(new BorderLayout(6, 6));
        JPanel replacementRow = new JPanel(new BorderLayout(6, 6));
        JPanel filterRow = new JPanel(new BorderLayout(6, 6));

        JPanel options = new JPanel(new BorderLayout(6, 6));
        JPanel checks = new JPanel();
        JPanel actions = new JPanel();
        checks.add(caseSensitive);
        checks.add(useRegex);
        actions.add(searchButton);
        actions.add(replaceSelectedButton);
        actions.add(replaceAllButton);

        queryRow.add(new JLabel(I18n.tr("search.query")), BorderLayout.WEST);
        queryRow.add(queryField, BorderLayout.CENTER);
        replacementRow.add(new JLabel(I18n.tr("search.replacement")), BorderLayout.WEST);
        replacementRow.add(replacementField, BorderLayout.CENTER);
        filterRow.add(new JLabel(I18n.tr("search.folderFilter")), BorderLayout.WEST);
        filterRow.add(folderFilterField, BorderLayout.CENTER);

        top.add(queryRow);
        top.add(replacementRow);
        top.add(filterRow);
        options.add(checks, BorderLayout.WEST);
        options.add(actions, BorderLayout.EAST);

        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultList.setVisibleRowCount(16);

        JPanel north = new JPanel(new BorderLayout(6, 6));
        north.add(top, BorderLayout.NORTH);
        north.add(options, BorderLayout.SOUTH);

        add(north, BorderLayout.NORTH);
        add(new JScrollPane(resultList), BorderLayout.CENTER);
        add(resultLabel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(860, 480));
        pack();
        setLocationRelativeTo(getOwner());

        queryField.addActionListener(e -> search());
        replacementField.addActionListener(e -> replaceSelected());
        folderFilterField.addActionListener(e -> search());
        searchButton.addActionListener(e -> search());
        replaceSelectedButton.addActionListener(e -> replaceSelected());
        replaceAllButton.addActionListener(e -> replaceAll());
        resultList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedResult();
                }
            }
        });
        resultList.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke("ENTER"), "openResult");
        resultList.getActionMap().put("openResult", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                openSelectedResult();
            }
        });
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            setTitle(I18n.tr("search.globalTitle"));
            caseSensitive.setText(I18n.tr("search.caseSensitive"));
            useRegex.setText(I18n.tr("search.useRegex"));
            searchButton.setText(I18n.tr("search.searchButton"));
            replaceSelectedButton.setText(I18n.tr("search.replaceSelected"));
            replaceAllButton.setText(I18n.tr("search.replaceAllButton"));
            resultLabel.setText(I18n.tr("search.results") + ": 0");
            SwingUtilities.invokeLater(queryField::requestFocusInWindow);
        }
        super.setVisible(b);
    }

    private void search() {
        SearchInput input = validateInputForSearch();
        if (input == null) {
            return;
        }

        searchButton.setEnabled(false);
        replaceAllButton.setEnabled(false);
        replaceSelectedButton.setEnabled(false);
        resultList.setListData(new SearchMatch[0]);
        resultLabel.setText(I18n.tr("search.searching"));

        new SwingWorker<List<SearchMatch>, Void>() {
            @Override
            protected List<SearchMatch> doInBackground() throws Exception {
                return findMatches(input);
            }

            @Override
            protected void done() {
                searchButton.setEnabled(true);
                replaceAllButton.setEnabled(true);
                replaceSelectedButton.setEnabled(true);
                try {
                    List<SearchMatch> matches = get();
                    resultList.setListData(matches.toArray(SearchMatch[]::new));
                    resultLabel.setText(I18n.tr("search.results") + ": " + matches.size());
                    onLog.accept(I18n.tr("log.globalSearchDone") + ": " + matches.size());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(GlobalSearchDialog.this,
                            I18n.tr("search.globalError") + ": " + ex.getMessage(),
                            I18n.tr("dialog.error"), JOptionPane.ERROR_MESSAGE);
                    onLog.accept(I18n.tr("log.error") + ": " + ex.getMessage());
                    resultLabel.setText(I18n.tr("search.results") + ": 0");
                }
            }
        }.execute();
    }

    private void replaceSelected() {
        SearchMatch selected = resultList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, I18n.tr("search.noSelection"),
                    I18n.tr("dialog.error"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        SearchInput input = validateInputForSearch();
        if (input == null) {
            return;
        }

        int decision = JOptionPane.showConfirmDialog(this,
                I18n.tr("search.confirmReplaceSelected"),
                I18n.tr("search.confirmTitle"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (decision != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            int replaced = replaceOneMatch(input, selected, replacementField.getText());
            onLog.accept(I18n.tr("log.globalReplaceDone") + ": " + replaced);
            search();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    I18n.tr("search.globalReplaceError") + ": " + ex.getMessage(),
                    I18n.tr("dialog.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void replaceAll() {
        SearchInput input = validateInputForSearch();
        if (input == null) {
            return;
        }

        int decision = JOptionPane.showConfirmDialog(this,
                I18n.tr("search.confirmReplaceAll"),
                I18n.tr("search.confirmTitle"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (decision != JOptionPane.OK_OPTION) {
            return;
        }

        String replacement = replacementField.getText();
        String effectiveReplacement = input.useRegex()
                ? replacement
                : Matcher.quoteReplacement(replacement);

        replaceAllButton.setEnabled(false);
        replaceSelectedButton.setEnabled(false);
        searchButton.setEnabled(false);
        resultLabel.setText(I18n.tr("search.replacing"));

        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                return replaceAllMatches(input, effectiveReplacement);
            }

            @Override
            protected void done() {
                replaceAllButton.setEnabled(true);
                replaceSelectedButton.setEnabled(true);
                searchButton.setEnabled(true);
                try {
                    int total = get();
                    onLog.accept(I18n.tr("log.globalReplaceDone") + ": " + total);
                    search();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(GlobalSearchDialog.this,
                            I18n.tr("search.globalReplaceError") + ": " + ex.getMessage(),
                            I18n.tr("dialog.error"), JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private SearchInput validateInputForSearch() {
        Path root = projectRootSupplier.get();
        if (root == null || !Files.isDirectory(root)) {
            JOptionPane.showMessageDialog(this, I18n.tr("error.noProject"),
                    I18n.tr("dialog.error"), JOptionPane.ERROR_MESSAGE);
            return null;
        }

        String query = queryField.getText();
        if (query == null || query.isBlank()) {
            JOptionPane.showMessageDialog(this, I18n.tr("search.emptyQuery"),
                    I18n.tr("dialog.error"), JOptionPane.ERROR_MESSAGE);
            return null;
        }

        Pattern pattern;
        try {
            pattern = buildPattern(query);
        } catch (PatternSyntaxException ex) {
            JOptionPane.showMessageDialog(this,
                    I18n.tr("search.invalidRegex") + ": " + ex.getDescription(),
                    I18n.tr("dialog.error"), JOptionPane.ERROR_MESSAGE);
            return null;
        }

        String folderFilter = normalizeFolderFilter(folderFilterField.getText());
        return new SearchInput(root, pattern, useRegex.isSelected(), folderFilter);
    }

    private Pattern buildPattern(String query) {
        int flags = caseSensitive.isSelected() ? Pattern.MULTILINE : Pattern.MULTILINE | Pattern.CASE_INSENSITIVE;
        if (useRegex.isSelected()) {
            return Pattern.compile(query, flags);
        }
        return Pattern.compile(Pattern.quote(query), flags);
    }

    private List<SearchMatch> findMatches(SearchInput input) throws Exception {
        List<SearchMatch> matches = new ArrayList<>();
        try (var stream = Files.walk(input.root())) {
            List<Path> luciaFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".lucia"))
                    .filter(path -> matchesFolderFilter(input.root(), path, input.folderFilter()))
                    .sorted()
                    .toList();

            for (Path file : luciaFiles) {
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                    String line = lines.get(lineIndex);
                    Matcher matcher = input.pattern().matcher(line);
                    while (matcher.find()) {
                        int start = matcher.start() + 1;
                        int length = Math.max(1, matcher.end() - matcher.start());
                        matches.add(new SearchMatch(input.root(), file, lineIndex + 1, start, length, line));
                    }
                }
            }
        }
        matches.sort(Comparator
                .comparing((SearchMatch m) -> m.path().toString())
                .thenComparingInt(SearchMatch::line)
                .thenComparingInt(SearchMatch::column));
        return matches;
    }

    private int replaceAllMatches(SearchInput input, String replacement) throws Exception {
        int totalReplacements = 0;
        try (var stream = Files.walk(input.root())) {
            List<Path> luciaFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".lucia"))
                    .filter(path -> matchesFolderFilter(input.root(), path, input.folderFilter()))
                    .sorted()
                    .toList();

            for (Path file : luciaFiles) {
                String original = Files.readString(file, StandardCharsets.UTF_8);
                Matcher matcher = input.pattern().matcher(original);
                if (!matcher.find()) {
                    continue;
                }
                matcher.reset();
                String replaced = matcher.replaceAll(replacement);
                int replacedInFile = countMatches(input.pattern(), original);
                if (replacedInFile > 0) {
                    Files.writeString(file, replaced, StandardCharsets.UTF_8);
                    totalReplacements += replacedInFile;
                }
            }
        }
        return totalReplacements;
    }

    private int replaceOneMatch(SearchInput input, SearchMatch selected, String replacementRaw) throws Exception {
        Path file = selected.path();
        String original = Files.readString(file, StandardCharsets.UTF_8);
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        int lineIndex = selected.line() - 1;
        if (lineIndex < 0 || lineIndex >= lines.size()) {
            return 0;
        }
        String line = lines.get(lineIndex);
        Matcher lineMatcher = input.pattern().matcher(line);
        StringBuffer rewrittenLine = new StringBuffer();
        boolean replaced = false;
        while (lineMatcher.find()) {
            if (lineMatcher.start() == selected.column() - 1) {
                String replacement = input.useRegex()
                        ? replacementRaw
                        : Matcher.quoteReplacement(replacementRaw);
                lineMatcher.appendReplacement(rewrittenLine, replacement);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            return 0;
        }
        lineMatcher.appendTail(rewrittenLine);
        lines.set(lineIndex, rewrittenLine.toString());
        String updated = String.join(System.lineSeparator(), lines);
        if (original.endsWith(System.lineSeparator())) {
            updated += System.lineSeparator();
        }
        Files.writeString(file, updated, StandardCharsets.UTF_8);
        return 1;
    }

    private int countMatches(Pattern pattern, String content) {
        Matcher matcher = pattern.matcher(content);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private String normalizeFolderFilter(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace('\\', '/').toLowerCase(Locale.ROOT);
    }

    private boolean matchesFolderFilter(Path root, Path file, String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        Path relative = root.toAbsolutePath().normalize().relativize(file.toAbsolutePath().normalize());
        String rel = relative.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        return rel.contains(filter);
    }

    private void openSelectedResult() {
        SearchMatch selected = resultList.getSelectedValue();
        if (selected == null) {
            return;
        }
        onNavigate.accept(selected);
    }

    public record SearchMatch(Path root, Path path, int line, int column, int length, String text) {
        @Override
        public String toString() {
            Path normalizedRoot = root.toAbsolutePath().normalize();
            Path normalizedPath = path.toAbsolutePath().normalize();
            String relative = normalizedRoot.relativize(normalizedPath).toString();
            String preview = text == null ? "" : text.trim();
            return relative + ":" + line + ":" + column + "  |  " + preview;
        }
    }

    private record SearchInput(Path root, Pattern pattern, boolean useRegex, String folderFilter) {
    }
}
