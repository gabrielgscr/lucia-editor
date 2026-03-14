package com.lucia.editor.ui.diagnostics;

import com.lucia.editor.i18n.I18n;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * Simple navigable problems list for diagnostics.
 */
public class ProblemsPanel extends JPanel {

    // Light theme: deep reds/ambers readable on white backgrounds.
    // Dark theme: lighter, more saturated tones visible on dark backgrounds.
    private static final Color ERROR_COLOR_LIGHT   = new Color(0xB42318);
    private static final Color WARNING_COLOR_LIGHT = new Color(0xA46A00);
    private static final Color ERROR_COLOR_DARK    = new Color(0xF47067);
    private static final Color WARNING_COLOR_DARK  = new Color(0xE5A243);

    private boolean darkTheme;

    public record ProblemEntry(Path file, LuciaDiagnostic diagnostic) {
        @Override
        public String toString() {
            String sev = diagnostic.severity() == LuciaDiagnosticSeverity.WARNING ? "Warning" : "Error";
            return "[" + sev + "] " + file.getFileName() + ":" + diagnostic.line() + ":" + diagnostic.column()
                    + " - " + diagnostic.message();
        }
    }

    private final Consumer<ProblemEntry> onOpen;
    private final Consumer<ProblemEntry> onQuickFix;
    private final DefaultListModel<ProblemEntry> model;
    private final JList<ProblemEntry> list;
    private final Map<Path, List<LuciaDiagnostic>> diagnosticsByFile;
    private final JPopupMenu popup;

    public void setDarkTheme(boolean dark) {
        this.darkTheme = dark;
        list.repaint();
    }

    public ProblemsPanel(Consumer<ProblemEntry> onOpen, Consumer<ProblemEntry> onQuickFix) {
        super(new BorderLayout());
        this.onOpen = onOpen;
        this.onQuickFix = onQuickFix;
        this.model = new DefaultListModel<>();
        this.list = new JList<>(model);
        this.diagnosticsByFile = new LinkedHashMap<>();
        this.popup = new JPopupMenu();

        add(new JScrollPane(list), BorderLayout.CENTER);

        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> jList, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(jList, value, index, isSelected, cellHasFocus);
                if (value instanceof ProblemEntry entry) {
                    setText(formatEntry(entry));
                        boolean isWarning = entry.diagnostic().severity() == LuciaDiagnosticSeverity.WARNING;
                        Color fg = isWarning
                            ? (darkTheme ? WARNING_COLOR_DARK : WARNING_COLOR_LIGHT)
                            : (darkTheme ? ERROR_COLOR_DARK   : ERROR_COLOR_LIGHT);
                        setIcon(isWarning
                            ? FontIcon.of(FontAwesomeSolid.EXCLAMATION_TRIANGLE, 12, fg)
                            : FontIcon.of(FontAwesomeSolid.TIMES_CIRCLE, 12, fg));
                        if (!isSelected) {
                        setForeground(fg);
                        }
                }
                return c;
            }
        });

        JMenuItem openItem = new JMenuItem(I18n.tr("problems.open"));
        openItem.addActionListener(e -> openSelected());
        JMenuItem quickFixItem = new JMenuItem(I18n.tr("problems.quickFix"));
        quickFixItem.addActionListener(e -> runQuickFixSelected());
        popup.add(openItem);
        popup.add(quickFixItem);

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelected();
                    return;
                }
                if (SwingUtilities.isRightMouseButton(e)) {
                    int index = list.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        list.setSelectedIndex(index);
                        popup.show(list, e.getX(), e.getY());
                    }
                }
            }
        });

        list.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "openProblem");
        list.getActionMap().put("openProblem", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                openSelected();
            }
        });
        list.getInputMap().put(KeyStroke.getKeyStroke("control PERIOD"), "quickFixProblem");
        list.getActionMap().put("quickFixProblem", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                runQuickFixSelected();
            }
        });
    }

    public void setDiagnostics(Path file, List<LuciaDiagnostic> diagnostics) {
        if (file == null) {
            return;
        }
        Path normalized = file.toAbsolutePath().normalize();
        if (diagnostics == null || diagnostics.isEmpty()) {
            diagnosticsByFile.remove(normalized);
        } else {
            diagnosticsByFile.put(normalized, List.copyOf(diagnostics));
        }
        rebuildModel();
    }

    public void clearFile(Path file) {
        if (file == null) {
            return;
        }
        diagnosticsByFile.remove(file.toAbsolutePath().normalize());
        rebuildModel();
    }

    public int totalProblems() {
        int count = 0;
        for (List<LuciaDiagnostic> diagnostics : diagnosticsByFile.values()) {
            count += diagnostics.size();
        }
        return count;
    }

    public ProblemEntry getSelectedProblem() {
        return list.getSelectedValue();
    }

    private void rebuildModel() {
        model.clear();
        List<ProblemEntry> entries = new ArrayList<>();

        for (Map.Entry<Path, List<LuciaDiagnostic>> entry : diagnosticsByFile.entrySet()) {
            for (LuciaDiagnostic diagnostic : entry.getValue()) {
                entries.add(new ProblemEntry(entry.getKey(), diagnostic));
            }
        }

        entries.sort(Comparator
                .comparing((ProblemEntry e) -> e.file().toString())
                .thenComparingInt(e -> e.diagnostic().line())
                .thenComparingInt(e -> e.diagnostic().column()));

        for (ProblemEntry entry : entries) {
            model.addElement(entry);
        }
    }

    private void openSelected() {
        ProblemEntry selected = list.getSelectedValue();
        if (selected != null) {
            onOpen.accept(selected);
        }
    }

    private void runQuickFixSelected() {
        ProblemEntry selected = list.getSelectedValue();
        if (selected != null) {
            onQuickFix.accept(selected);
        }
    }

    private String formatEntry(ProblemEntry entry) {
        String sev = entry.diagnostic().severity() == LuciaDiagnosticSeverity.WARNING ? "WARN" : "ERR";
        return "[" + sev + "] " + entry.file().getFileName() + ":" + entry.diagnostic().line() + ":"
                + entry.diagnostic().column() + " - " + entry.diagnostic().message();
    }
}
