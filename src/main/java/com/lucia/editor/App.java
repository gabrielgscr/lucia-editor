package com.lucia.editor;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.lucia.editor.config.EditorConfig;
import com.lucia.editor.i18n.I18n;
import com.lucia.editor.ui.MainFrame;
import java.util.Locale;
import javax.swing.SwingUtilities;

public final class App {

    private App() {
    }

    public static void main(String[] args) {
        Locale locale = Locale.getDefault().getLanguage().equals("es")
                ? Locale.forLanguageTag("es")
                : Locale.ENGLISH;
        I18n.setLocale(locale);

        EditorConfig config = new EditorConfig();
        if (config.isDarkTheme()) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
