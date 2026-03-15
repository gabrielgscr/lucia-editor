package com.lucia.editor.help;

import com.lucia.editor.i18n.I18n;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Searchable help center dialog.
 */
public final class HelpDialog extends JDialog {

    private final JTextField searchField;
    private final DefaultListModel<HelpArticle> articleModel;
    private final JList<HelpArticle> articleList;
    private final JEditorPane viewer;
    private final JLabel summaryLabel;

    private HelpService service;
    private boolean darkTheme;
    private List<HelpArticle> currentArticles;

    public HelpDialog(JFrame owner, HelpService service, boolean darkTheme) {
        super(owner, I18n.tr("help.title"), false);
        this.service = service;
        this.darkTheme = darkTheme;
        this.currentArticles = new ArrayList<>();

        this.searchField = new JTextField();
        this.articleModel = new DefaultListModel<>();
        this.articleList = new JList<>(articleModel);
        this.viewer = new JEditorPane("text/html", "");
        this.summaryLabel = new JLabel();

        buildUi();
        refreshTexts();
        reloadData();
    }

    public void setDarkTheme(boolean darkTheme) {
        this.darkTheme = darkTheme;
        HelpArticle selected = articleList.getSelectedValue();
        if (selected != null) {
            render(selected);
        }
    }

    public void setService(HelpService service) {
        this.service = service;
        reloadData();
    }

    public void openArticle(String articleId) {
        if (articleId != null && !articleId.isBlank()) {
            showAndSelect(articleId);
        } else {
            showAndSelect(null);
        }
    }

    public void refreshTexts() {
        setTitle(I18n.tr("help.title"));
    }

    private void buildUi() {
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new BorderLayout(6, 0));
        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        top.add(new JLabel(I18n.tr("help.search")), BorderLayout.WEST);
        top.add(searchField, BorderLayout.CENTER);
        JButton clearButton = new JButton(I18n.tr("help.clear"));
        clearButton.addActionListener(e -> {
            searchField.setText("");
            applySearch();
        });
        top.add(clearButton, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        articleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        articleList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value.title());
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
            if (isSelected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            } else {
                label.setBackground(list.getBackground());
                label.setForeground(list.getForeground());
            }
            return label;
        });
        articleList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                HelpArticle selected = articleList.getSelectedValue();
                if (selected != null) {
                    render(selected);
                }
            }
        });

        viewer.setEditable(false);
        viewer.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        viewer.addHyperlinkListener(event -> {
            if (event.getEventType() != javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) return;
            if (!Desktop.isDesktopSupported()) return;
            try {
                Desktop.getDesktop().browse(event.getURL().toURI());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), I18n.tr("dialog.error"),
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        JScrollPane left = new JScrollPane(articleList);
        JScrollPane right = new JScrollPane(viewer);
        left.setMinimumSize(new Dimension(220, 380));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.3);
        split.setDividerLocation(250);
        add(split, BorderLayout.CENTER);

        summaryLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
        add(summaryLabel, BorderLayout.SOUTH);

        searchField.addActionListener(e -> applySearch());
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applySearch(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applySearch(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applySearch(); }
        });

        setPreferredSize(new Dimension(980, 620));
        pack();
        setLocationRelativeTo(getOwner());
    }

    private void reloadData() {
        currentArticles = service.listArticles();
        fillModel(currentArticles);
        if (!currentArticles.isEmpty()) {
            articleList.setSelectedIndex(0);
        } else {
            viewer.setText("<html><body>No help articles found.</body></html>");
            summaryLabel.setText(I18n.tr("help.noResults"));
        }
    }

    private void applySearch() {
        String query = searchField.getText();
        currentArticles = service.search(query);
        fillModel(currentArticles);
        if (!currentArticles.isEmpty()) {
            articleList.setSelectedIndex(0);
            summaryLabel.setText(I18n.tr("help.results") + ": " + currentArticles.size());
        } else {
            viewer.setText("<html><body style='padding:10px;'><b>" + I18n.tr("help.noResults") + "</b></body></html>");
            summaryLabel.setText(I18n.tr("help.noResults"));
        }
    }

    private void fillModel(List<HelpArticle> articles) {
        articleModel.clear();
        for (HelpArticle article : articles) {
            articleModel.addElement(article);
        }
    }

    private void render(HelpArticle article) {
        Color fg = UIManager.getColor("Label.foreground");
        viewer.setText(HelpMarkdown.toHtml(article.markdown(), darkTheme, fg));
        viewer.setCaretPosition(0);

        String related = article.relatedIds().isEmpty()
                ? ""
                : " | " + I18n.tr("help.related") + ": " + String.join(", ", article.relatedIds());
        summaryLabel.setText(article.summary() + related);
    }

    private void showAndSelect(String articleId) {
        SwingUtilities.invokeLater(() -> {
            setVisible(true);
            toFront();
            if (articleId == null || articleId.isBlank()) {
                if (!articleModel.isEmpty()) {
                    articleList.setSelectedIndex(0);
                }
                return;
            }
            for (int i = 0; i < articleModel.size(); i++) {
                if (articleModel.get(i).id().equals(articleId)) {
                    articleList.setSelectedIndex(i);
                    articleList.ensureIndexIsVisible(i);
                    return;
                }
            }
            HelpArticle fallback = service.findById(articleId);
            if (fallback != null) {
                articleModel.add(0, fallback);
                articleList.setSelectedIndex(0);
            }
        });
    }
}
