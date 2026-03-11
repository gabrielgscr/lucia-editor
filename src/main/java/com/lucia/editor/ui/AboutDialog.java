package com.lucia.editor.ui;

import com.lucia.editor.i18n.I18n;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

/** Static utility that displays the About dialog for the Lucia Editor. */
public final class AboutDialog {

    private static final String CONTACT_PHONE         = "+50671077660";
    private static final String CONTACT_EMAIL         = "ggonzalezs@cuc.ac.cr";
    private static final String LUCIA_REPO_URL        = "https://github.com/gabrielgscr/lucia.git";
    private static final String LUCIA_EDITOR_REPO_URL = "https://github.com/gabrielgscr/lucia-editor.git";

    private AboutDialog() {}

    /** Shows the About dialog as a modal child of {@code owner}. */
    public static void show(JFrame owner, boolean darkTheme) {
        String version = resolveAppVersion(owner.getClass());
        JEditorPane content = new JEditorPane("text/html", buildHtml(version, darkTheme));
        content.setEditable(false);
        content.setOpaque(false);
        content.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        content.addHyperlinkListener(event -> {
            if (event.getEventType() != javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) return;
            if (!Desktop.isDesktopSupported()) return;
            try {
                Desktop.getDesktop().browse(event.getURL().toURI());
            } catch (Exception ignored) {}
        });

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setPreferredSize(new Dimension(620, 420));
        JOptionPane.showMessageDialog(owner, scroll, I18n.tr("menu.about"),
                JOptionPane.INFORMATION_MESSAGE);
    }

    // ── private ────────────────────────────────────────────────────────

    private static String buildHtml(String version, boolean darkTheme) {
        String fgHex    = toHex(UIManager.getColor("Label.foreground"), "#222222");
        String mutedHex = toHex(UIManager.getColor("Label.disabledForeground"), "#666666");
        String linkHex  = darkTheme ? "#80b7ff" : "#0b63d1";

        return "<html><body style='font-family:Segoe UI,sans-serif;color:" + fgHex + ";padding:10px;'>"
                + "<div style='font-size:22px;font-weight:700;margin-bottom:4px;'>" + I18n.tr("app.title") + "</div>"
                + "<div style='color:" + mutedHex + ";margin-bottom:14px;'>" + I18n.tr("about.tagline") + "</div>"
                + "<div style='margin-bottom:12px;'><b>Version:</b> " + escapeHtml(version) + "</div>"
                + "<div style='margin-bottom:12px;line-height:1.45;'>" + I18n.tr("about.description") + "</div>"
                + "<div style='margin-top:8px;margin-bottom:4px;font-weight:700;'>" + I18n.tr("about.contactTitle") + "</div>"
                + "<div><b>" + I18n.tr("about.phone") + ":</b> " + escapeHtml(CONTACT_PHONE) + "</div>"
                + "<div><b>" + I18n.tr("about.email") + ":</b> <a style='color:" + linkHex + ";' href='mailto:"
                + CONTACT_EMAIL + "'>" + escapeHtml(CONTACT_EMAIL) + "</a></div>"
                + "<div style='margin-top:10px;margin-bottom:4px;font-weight:700;'>" + I18n.tr("about.repositoriesTitle") + "</div>"
                + "<div><b>" + I18n.tr("about.luciaRepo") + ":</b> <a style='color:" + linkHex + ";' href='"
                + LUCIA_REPO_URL + "'>" + escapeHtml(LUCIA_REPO_URL) + "</a></div>"
                + "<div><b>" + I18n.tr("about.editorRepo") + ":</b> <a style='color:" + linkHex + ";' href='"
                + LUCIA_EDITOR_REPO_URL + "'>" + escapeHtml(LUCIA_EDITOR_REPO_URL) + "</a></div>"
                + "</body></html>";
    }

    private static String resolveAppVersion(Class<?> contextClass) {
        Package pkg = contextClass.getPackage();
        if (pkg != null) {
            String v = pkg.getImplementationVersion();
            if (v != null && !v.isBlank()) return v;
        }
        Path pomPath = Path.of("pom.xml");
        if (Files.isRegularFile(pomPath)) {
            try {
                String pom = Files.readString(pomPath, StandardCharsets.UTF_8);
                Matcher m = Pattern.compile("<version>([^<]+)</version>").matcher(pom);
                if (m.find()) return m.group(1).trim();
            } catch (IOException ignored) {}
        }
        return "unknown";
    }

    private static String toHex(Color color, String fallback) {
        if (color == null) return fallback;
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
