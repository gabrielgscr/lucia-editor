package com.lucia.editor.ui;

import com.lucia.editor.snippets.SnippetDefinition;
import com.lucia.editor.snippets.SnippetManager;
import com.lucia.editor.syntax.LuciaTokenMaker;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.KeyStroke;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.fife.ui.autocomplete.TemplateCompletion;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rsyntaxtextarea.folding.CurlyFoldParser;
import org.fife.ui.rsyntaxtextarea.folding.FoldParserManager;

/**
 * Configures RSyntaxTextArea instances for the Lucia language:
 * syntax highlighting, code folding, font size, and autocomplete.
 */
public class EditorFactory {

    private boolean darkTheme;
    private int editorFontSize;
    private boolean autocompleteEnabled;
    private int autocompleteDelayMs;
    private boolean snippetsInAutocomplete;
    private CompletionProvider completionProvider;
    private final SnippetManager snippetManager;
    private final Map<RSyntaxTextArea, AutoCompletion> completionsByEditor;

    public EditorFactory(boolean darkTheme, int editorFontSize, SnippetManager snippetManager) {
        this.darkTheme          = darkTheme;
        this.editorFontSize     = editorFontSize;
        this.autocompleteEnabled = true;
        this.autocompleteDelayMs = 250;
        this.snippetsInAutocomplete = true;
        this.snippetManager     = snippetManager;
        this.completionsByEditor = new WeakHashMap<>();
        this.completionProvider = buildCompletionProvider();
    }

    /** Registers the Lucia token maker and fold parser (call once at startup). */
    public static void registerLuciaSyntax() {
        TokenMakerFactory factory = TokenMakerFactory.getDefaultInstance();
        if (factory instanceof AbstractTokenMakerFactory atmf) {
            atmf.putMapping("text/lucia", LuciaTokenMaker.class.getName());
        }
        FoldParserManager.get().addFoldParserMapping("text/lucia", new CurlyFoldParser());
    }

    public void setDarkTheme(boolean darkTheme) {
        this.darkTheme = darkTheme;
    }

    public void setEditorFontSize(int editorFontSize) {
        this.editorFontSize = editorFontSize;
    }

    public void setAutocompleteEnabled(boolean autocompleteEnabled) {
        this.autocompleteEnabled = autocompleteEnabled;
    }

    public void setAutocompleteDelayMs(int autocompleteDelayMs) {
        this.autocompleteDelayMs = Math.max(50, autocompleteDelayMs);
    }

    public void setSnippetsInAutocomplete(boolean snippetsInAutocomplete) {
        this.snippetsInAutocomplete = snippetsInAutocomplete;
    }

    public void refreshSnippetCompletions() {
        this.completionProvider = buildCompletionProvider();
        for (AutoCompletion completion : completionsByEditor.values()) {
            completion.setCompletionProvider(completionProvider);
        }
    }

    /** Fully configures a freshly created editor. */
    public void configure(RSyntaxTextArea editor) {
        editor.setSyntaxEditingStyle("text/lucia");
        editor.setCodeFoldingEnabled(true);
        editor.setAntiAliasingEnabled(true);
        applyTheme(editor);
        applyFontSize(editor);
        installAutoCompletion(editor);
    }

    /** Re-applies the current theme and syntax colors to an existing editor. */
    public void applyTheme(RSyntaxTextArea editor) {
        String themeResource = darkTheme
                ? "/org/fife/ui/rsyntaxtextarea/themes/dark.xml"
                : "/org/fife/ui/rsyntaxtextarea/themes/idea.xml";
        try {
            Theme theme = Theme.load(EditorFactory.class.getResourceAsStream(themeResource));
            theme.apply(editor);
        } catch (Exception ex) {
            // Keep default appearance if theme file is unavailable.
        }
        applySyntaxColors(editor);
    }

    /** Applies the current font size to an existing editor. */
    public void applyFontSize(RSyntaxTextArea editor) {
        Font current = editor.getFont();
        editor.setFont(current.deriveFont((float) editorFontSize));
    }

    // ── private ────────────────────────────────────────────────────────

    /**
     * Overrides token colors on top of the base RSyntaxTextArea theme.
     * Light mode: IntelliJ-like palette.  Dark mode: VS Code-like palette.
     */
    private void applySyntaxColors(RSyntaxTextArea editor) {
        SyntaxScheme scheme = (SyntaxScheme) editor.getSyntaxScheme().clone();

        if (darkTheme) {
            scheme.getStyle(TokenTypes.RESERVED_WORD).foreground                = new Color(0x569CD6);
            scheme.getStyle(TokenTypes.RESERVED_WORD_2).foreground              = new Color(0xCE9178);
            scheme.getStyle(TokenTypes.DATA_TYPE).foreground                    = new Color(0xC586C0);
            scheme.getStyle(TokenTypes.FUNCTION).foreground                     = new Color(0xDCDCAA);
            scheme.getStyle(TokenTypes.LITERAL_BOOLEAN).foreground              = new Color(0x569CD6);
            scheme.getStyle(TokenTypes.LITERAL_NUMBER_DECIMAL_INT).foreground   = new Color(0xB5CEA8);
            scheme.getStyle(TokenTypes.LITERAL_STRING_DOUBLE_QUOTE).foreground  = new Color(0xCE9178);
            scheme.getStyle(TokenTypes.COMMENT_EOL).foreground                  = new Color(0x6A9955);
            scheme.getStyle(TokenTypes.COMMENT_MULTILINE).foreground            = new Color(0x6A9955);
            scheme.getStyle(TokenTypes.OPERATOR).foreground                     = new Color(0xD4D4D4);
        } else {
            scheme.getStyle(TokenTypes.RESERVED_WORD).foreground                = new Color(0x0033B3);
            scheme.getStyle(TokenTypes.RESERVED_WORD_2).foreground              = new Color(0x7B36A8);
            scheme.getStyle(TokenTypes.DATA_TYPE).foreground                    = new Color(0x7B36A8);
            scheme.getStyle(TokenTypes.FUNCTION).foreground                     = new Color(0x00627A);
            scheme.getStyle(TokenTypes.LITERAL_BOOLEAN).foreground              = new Color(0x0033B3);
            scheme.getStyle(TokenTypes.LITERAL_NUMBER_DECIMAL_INT).foreground   = new Color(0x1750EB);
            scheme.getStyle(TokenTypes.LITERAL_STRING_DOUBLE_QUOTE).foreground  = new Color(0x067D17);
            scheme.getStyle(TokenTypes.COMMENT_EOL).foreground                  = new Color(0x5F826B);
            scheme.getStyle(TokenTypes.COMMENT_MULTILINE).foreground            = new Color(0x5F826B);
            scheme.getStyle(TokenTypes.OPERATOR).foreground                     = new Color(0xC77600);
        }

        editor.setSyntaxScheme(scheme);
    }

    private void installAutoCompletion(RSyntaxTextArea editor) {
        AutoCompletion completion = new AutoCompletion(completionProvider);
        completion.setAutoActivationEnabled(autocompleteEnabled);
        completion.setAutoActivationDelay(autocompleteDelayMs);
        completion.setParameterAssistanceEnabled(true);
        completion.setShowDescWindow(true);
        completion.setTriggerKey(KeyStroke.getKeyStroke("control SPACE"));
        completion.install(editor);
        completionsByEditor.put(editor, completion);
    }

    private CompletionProvider buildCompletionProvider() {
        DefaultCompletionProvider provider = new DefaultCompletionProvider();

        // Keywords
        for (String kw : List.of("func", "if", "else", "while", "do", "for", "in", "return",
                "import", "break", "continue", "switch", "case", "default",
                "class", "constructor", "let", "this", "not", "null")) {
            provider.addCompletion(new BasicCompletion(provider, kw));
        }

        // Type names
        for (String type : List.of("int", "float", "string", "bool", "void", "any", "list", "dict", "date", "datetime")) {
            provider.addCompletion(new BasicCompletion(provider, type));
        }

        // Built-in functions with tab-stop placeholders
        provider.addCompletion(new TemplateCompletion(provider, "print",   "print(value)",    "print(${value})"));
        provider.addCompletion(new TemplateCompletion(provider, "len",     "len(value)",      "len(${value})"));
        provider.addCompletion(new TemplateCompletion(provider, "input",   "input(prompt)",   "input(${prompt})"));
        provider.addCompletion(new TemplateCompletion(provider, "str",     "str(value)",      "str(${value})"));
        provider.addCompletion(new TemplateCompletion(provider, "type_of", "type_of(value)",  "type_of(${value})"));
        provider.addCompletion(new TemplateCompletion(provider, "abs",     "abs(value)",      "abs(${value})"));
        provider.addCompletion(new TemplateCompletion(provider, "min",     "min(a, b)",       "min(${a}, ${b})"));
        provider.addCompletion(new TemplateCompletion(provider, "max",     "max(a, b)",       "max(${a}, ${b})"));
        provider.addCompletion(new TemplateCompletion(provider, "pow",     "pow(base, exp)",  "pow(${base}, ${exp})"));
        provider.addCompletion(new TemplateCompletion(provider, "round",   "round(value)",    "round(${value})"));
        provider.addCompletion(new TemplateCompletion(provider, "floor",   "floor(value)",    "floor(${value})"));
        provider.addCompletion(new TemplateCompletion(provider, "ceil",    "ceil(value)",     "ceil(${value})"));
        provider.addCompletion(new TemplateCompletion(provider, "random",  "random(min, max)","random(${min}, ${max})"));
        provider.addCompletion(new TemplateCompletion(provider, "date",    "date(year, month, day)",
            "date(${year}, ${month}, ${day})"));
        provider.addCompletion(new TemplateCompletion(provider, "datetime", "datetime(year, month, day, hour, minute, second)",
            "datetime(${year}, ${month}, ${day}, ${hour}, ${minute}, ${second})"));
        provider.addCompletion(new TemplateCompletion(provider, "today",   "today()",          "today()"));
        provider.addCompletion(new TemplateCompletion(provider, "now",     "now()",            "now()"));

        if (snippetsInAutocomplete) {
            try {
                List<SnippetDefinition> snippets = snippetManager.getSnippets();
                for (SnippetDefinition snippet : snippets) {
                    provider.addCompletion(new ShorthandCompletion(provider,
                            snippet.prefix(),
                            snippet.description().isBlank() ? snippet.prefix() : snippet.description(),
                            snippet.template()));
                }
            } catch (IOException ex) {
                // Fallback to no custom snippets if storage cannot be read.
            }
        }

        provider.setAutoActivationRules(true, null);
        return provider;
    }
}
