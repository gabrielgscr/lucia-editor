package com.lucia.editor.ui;

import com.lucia.editor.i18n.I18n;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private final JCheckBox caseSensitive;
    private final JCheckBox useRegex;
    private final JButton searchButton;
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
        caseSensitive = new JCheckBox(I18n.tr("search.caseSensitive"));
        useRegex = new JCheckBox(I18n.tr("search.useRegex"));
        searchButton = new JButton(I18n.tr("search.searchButton"));
        resultLabel = new JLabel(I18n.tr("search.results") + ": 0");
        resultList = new JList<>();

        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout(8, 8));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new BorderLayout(6, 6));
        JPanel options = new JPanel(new BorderLayout(6, 6));
        JPanel checks = new JPanel();
        checks.add(caseSensitive);
        checks.add(useRegex);

        top.add(new JLabel(I18n.tr("search.query")), BorderLayout.WEST);
        top.add(queryField, BorderLayout.CENTER);
        options.add(checks, BorderLayout.WEST);
        options.add(searchButton, BorderLayout.EAST);

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
        searchButton.addActionListener(e -> search());
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
            resultLabel.setText(I18n.tr("search.results") + ": 0");
            SwingUtilities.invokeLater(queryField::requestFocusInWindow);
        }
        super.setVisible(b);
    }

    private void search() {
        Path root = projectRootSupplier.get();
        if (root == null || !Files.isDirectory(root)) {
            JOptionPane.showMessageDialog(this, I18n.tr("error.noProject"),
                    I18n.tr("dialog.error"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        String query = queryField.getText();
        if (query == null || query.isBlank()) {
            JOptionPane.showMessageDialog(this, I18n.tr("search.emptyQuery"),
                    I18n.tr("dialog.error"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        Pattern pattern;
        try {
            pattern = buildPattern(query);
        } catch (PatternSyntaxException ex) {
            JOptionPane.showMessageDialog(this,
                    I18n.tr("search.invalidRegex") + ": " + ex.getDescription(),
                    I18n.tr("dialog.error"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        searchButton.setEnabled(false);
        resultList.setListData(new SearchMatch[0]);
        resultLabel.setText(I18n.tr("search.searching"));

        new SwingWorker<List<SearchMatch>, Void>() {
            @Override
            protected List<SearchMatch> doInBackground() throws Exception {
                return findMatches(root, pattern);
            }

            @Override
            protected void done() {
                searchButton.setEnabled(true);
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

    private Pattern buildPattern(String query) {
        int flags = caseSensitive.isSelected() ? Pattern.MULTILINE : Pattern.MULTILINE | Pattern.CASE_INSENSITIVE;
        if (useRegex.isSelected()) {
            return Pattern.compile(query, flags);
        }
        return Pattern.compile(Pattern.quote(query), flags);
    }

    private List<SearchMatch> findMatches(Path root, Pattern pattern) throws Exception {
        List<SearchMatch> matches = new ArrayList<>();
        try (var stream = Files.walk(root)) {
            List<Path> luciaFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".lucia"))
                    .sorted()
                    .toList();

            for (Path file : luciaFiles) {
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                    String line = lines.get(lineIndex);
                    Matcher matcher = pattern.matcher(line);
                    while (matcher.find()) {
                        int start = matcher.start() + 1;
                        int length = Math.max(1, matcher.end() - matcher.start());
                        matches.add(new SearchMatch(file, lineIndex + 1, start, length, line));
                    }
                }
            }
        }
        return matches;
    }

    private void openSelectedResult() {
        SearchMatch selected = resultList.getSelectedValue();
        if (selected == null) {
            return;
        }
        onNavigate.accept(selected);
    }

    public record SearchMatch(Path path, int line, int column, int length, String text) {
        @Override
        public String toString() {
            String trimmed = Objects.requireNonNullElse(text, "").trim();
            return path + ":" + line + ":" + column + "  |  " + trimmed;
        }
    }
}
