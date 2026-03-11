package com.lucia.editor.ui;

import com.lucia.editor.i18n.I18n;
import java.awt.Toolkit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JFrame;
import org.fife.rsta.ui.search.FindDialog;
import org.fife.rsta.ui.search.ReplaceDialog;
import org.fife.rsta.ui.search.SearchEvent;
import org.fife.rsta.ui.search.SearchListener;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

public class EditorSearchSupport implements SearchListener {

    private final JFrame owner;

    private final Supplier<RSyntaxTextArea> editorSupplier;
    private final Consumer<String> appendLog;
    private final Consumer<String> showError;
    private final SearchContext searchContext;

    private FindDialog findDialog;
    private ReplaceDialog replaceDialog;

    public EditorSearchSupport(JFrame owner, Supplier<RSyntaxTextArea> editorSupplier,
                               Consumer<String> appendLog, Consumer<String> showError) {
        this.owner = owner;
        this.editorSupplier = editorSupplier;
        this.appendLog = appendLog;
        this.showError = showError;
        this.searchContext = new SearchContext();
        this.searchContext.setSearchWrap(true);
    }

    public void showFindDialog() {
        RSyntaxTextArea editor = requireEditor();
        if (editor == null) {
            return;
        }
        ensureDialogs();
        syncSearchTextFromSelection(editor);
        findDialog.setVisible(true);
    }

    public void showReplaceDialog() {
        RSyntaxTextArea editor = requireEditor();
        if (editor == null) {
            return;
        }
        ensureDialogs();
        syncSearchTextFromSelection(editor);
        replaceDialog.setVisible(true);
    }

    public void findNext() {
        doDirectionalFind(true);
    }

    public void findPrevious() {
        doDirectionalFind(false);
    }

    public void refreshUi() {
        if (findDialog != null) {
            findDialog.updateUI();
        }
        if (replaceDialog != null) {
            replaceDialog.updateUI();
        }
    }

    public void resetDialogs() {
        if (findDialog != null) {
            findDialog.dispose();
            findDialog = null;
        }
        if (replaceDialog != null) {
            replaceDialog.dispose();
            replaceDialog = null;
        }
    }

    @Override
    public void searchEvent(SearchEvent e) {
        RSyntaxTextArea editor = requireEditor();
        if (editor == null) {
            return;
        }

        SearchResult result = switch (e.getType()) {
            case MARK_ALL -> SearchEngine.markAll(editor, e.getSearchContext());
            case FIND -> SearchEngine.find(editor, e.getSearchContext());
            case REPLACE -> SearchEngine.replace(editor, e.getSearchContext());
            case REPLACE_ALL -> SearchEngine.replaceAll(editor, e.getSearchContext());
        };

        handleSearchResult(result, e.getType());
    }

    @Override
    public String getSelectedText() {
        RSyntaxTextArea editor = editorSupplier.get();
        return editor != null && editor.getSelectedText() != null ? editor.getSelectedText() : "";
    }

    private void ensureDialogs() {
        if (findDialog != null && replaceDialog != null) {
            return;
        }

        findDialog = new FindDialog(owner, this);
        replaceDialog = new ReplaceDialog(owner, this);
        findDialog.setSearchContext(searchContext);
        replaceDialog.setSearchContext(searchContext);
    }

    private void doDirectionalFind(boolean forward) {
        RSyntaxTextArea editor = requireEditor();
        if (editor == null) {
            return;
        }
        ensureDialogs();
        syncSearchTextFromSelection(editor);

        String searchFor = searchContext.getSearchFor();
        if (searchFor == null || searchFor.isBlank()) {
            showFindDialog();
            return;
        }

        searchContext.setSearchForward(forward);
        handleSearchResult(SearchEngine.find(editor, searchContext), SearchEvent.Type.FIND);
    }

    private RSyntaxTextArea requireEditor() {
        RSyntaxTextArea editor = editorSupplier.get();
        if (editor == null) {
            showError.accept(I18n.tr("error.noFile"));
        }
        return editor;
    }

    private void syncSearchTextFromSelection(RSyntaxTextArea editor) {
        String selectedText = editor.getSelectedText();
        if (selectedText != null && !selectedText.isBlank()) {
            searchContext.setSearchFor(selectedText);
        }
    }

    private void handleSearchResult(SearchResult result, SearchEvent.Type type) {
        if (result == null) {
            return;
        }

        if (!result.wasFound() && type != SearchEvent.Type.MARK_ALL && result.getCount() == 0) {
            Toolkit.getDefaultToolkit().beep();
            appendLog.accept(I18n.tr("log.searchNotFound"));
            return;
        }

        switch (type) {
            case MARK_ALL -> appendLog.accept(I18n.tr("log.searchMarked") + ": " + result.getMarkedCount());
            case REPLACE -> appendLog.accept(I18n.tr("log.searchReplaceSuccess"));
            case REPLACE_ALL -> appendLog.accept(I18n.tr("log.searchReplaceAll") + ": " + result.getCount());
            default -> {
            }
        }
    }
}
