package com.lucia.editor.help;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight Markdown to HTML transformer for bundled help content.
 */
public final class HelpMarkdown {

    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)");
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]+)`");
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*([^*]+)\\*\\*");

    private HelpMarkdown() {
    }

    public static String toHtml(String markdown, boolean darkTheme, Color foreground) {
        String fg = toHex(foreground, darkTheme ? "#e6e6e6" : "#222222");
        String codeBg = darkTheme ? "#2a2d36" : "#f5f6f8";
        String border = darkTheme ? "#404552" : "#d4d7dd";
        String link = darkTheme ? "#80b7ff" : "#0b63d1";

        StringBuilder out = new StringBuilder();
        out.append("<html><body style='font-family:Segoe UI,sans-serif;font-size:13px;color:")
                .append(fg)
                .append(";padding:10px;line-height:1.45;'>");

        String[] lines = markdown == null ? new String[0] : markdown.split("\\R", -1);
        boolean inList = false;
        boolean inCode = false;
        StringBuilder codeBuffer = new StringBuilder();
        StringBuilder paragraph = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.strip();

            if (trimmed.startsWith("```")) {
                flushParagraph(out, paragraph, link, codeBg);
                if (inList) {
                    out.append("</ul>");
                    inList = false;
                }
                if (!inCode) {
                    inCode = true;
                    codeBuffer.setLength(0);
                } else {
                    out.append("<pre style='background:").append(codeBg)
                            .append(";border:1px solid ").append(border)
                            .append(";padding:8px;border-radius:6px;overflow:auto;'>")
                            .append(escapeHtml(codeBuffer.toString()))
                            .append("</pre>");
                    inCode = false;
                }
                continue;
            }

            if (inCode) {
                codeBuffer.append(line).append('\n');
                continue;
            }

            if (trimmed.isEmpty()) {
                flushParagraph(out, paragraph, link, codeBg);
                if (inList) {
                    out.append("</ul>");
                    inList = false;
                }
                continue;
            }

            if (trimmed.startsWith("### ")) {
                flushParagraph(out, paragraph, link, codeBg);
                if (inList) {
                    out.append("</ul>");
                    inList = false;
                }
                out.append("<h3 style='margin:14px 0 6px 0;'>")
                    .append(applyInline(trimmed.substring(4), link, codeBg))
                        .append("</h3>");
                continue;
            }

            if (trimmed.startsWith("## ")) {
                flushParagraph(out, paragraph, link, codeBg);
                if (inList) {
                    out.append("</ul>");
                    inList = false;
                }
                out.append("<h2 style='margin:16px 0 8px 0;'>")
                    .append(applyInline(trimmed.substring(3), link, codeBg))
                        .append("</h2>");
                continue;
            }

            if (trimmed.startsWith("# ")) {
                flushParagraph(out, paragraph, link, codeBg);
                if (inList) {
                    out.append("</ul>");
                    inList = false;
                }
                out.append("<h1 style='margin:0 0 10px 0;'>")
                    .append(applyInline(trimmed.substring(2), link, codeBg))
                        .append("</h1>");
                continue;
            }

            if (trimmed.startsWith("- ")) {
                flushParagraph(out, paragraph, link, codeBg);
                if (!inList) {
                    out.append("<ul style='margin:6px 0 10px 18px;padding:0;'>");
                    inList = true;
                }
                out.append("<li style='margin:2px 0;'>")
                    .append(applyInline(trimmed.substring(2), link, codeBg))
                        .append("</li>");
                continue;
            }

            if (paragraph.length() > 0) {
                paragraph.append(' ');
            }
            paragraph.append(trimmed);
        }

        flushParagraph(out, paragraph, link, codeBg);
        if (inList) {
            out.append("</ul>");
        }
        if (inCode) {
            out.append("<pre style='background:").append(codeBg)
                    .append(";border:1px solid ").append(border)
                    .append(";padding:8px;border-radius:6px;overflow:auto;'>")
                    .append(escapeHtml(codeBuffer.toString()))
                    .append("</pre>");
        }

        out.append("</body></html>");
        return out.toString();
    }

    private static void flushParagraph(StringBuilder out, StringBuilder paragraph,
                                       String linkColor, String codeBackground) {
        if (paragraph.isEmpty()) {
            return;
        }
        String text = paragraph.toString();
        out.append("<p style='margin:8px 0;'>")
                .append(applyInline(text, linkColor, codeBackground))
                .append("</p>");
        paragraph.setLength(0);
    }

    private static String applyInline(String input, String linkColor, String codeBackground) {
        String text = escapeHtml(input);

        Matcher linkMatcher = LINK_PATTERN.matcher(text);
        StringBuffer linked = new StringBuffer();
        while (linkMatcher.find()) {
            String replacement = "<a style='color:" + linkColor + ";' href='" + linkMatcher.group(2) + "'>"
                    + linkMatcher.group(1) + "</a>";
            linkMatcher.appendReplacement(linked, Matcher.quoteReplacement(replacement));
        }
        linkMatcher.appendTail(linked);

        Matcher codeMatcher = INLINE_CODE_PATTERN.matcher(linked.toString());
        StringBuffer coded = new StringBuffer();
        while (codeMatcher.find()) {
            String replacement = "<code style='background:" + codeBackground + ";padding:1px 4px;border-radius:4px;'>"
                    + codeMatcher.group(1) + "</code>";
            codeMatcher.appendReplacement(coded, Matcher.quoteReplacement(replacement));
        }
        codeMatcher.appendTail(coded);

        Matcher boldMatcher = BOLD_PATTERN.matcher(coded.toString());
        StringBuffer bolded = new StringBuffer();
        while (boldMatcher.find()) {
            String replacement = "<b>" + boldMatcher.group(1) + "</b>";
            boldMatcher.appendReplacement(bolded, Matcher.quoteReplacement(replacement));
        }
        boldMatcher.appendTail(bolded);

        return bolded.toString();
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String toHex(Color color, String fallback) {
        if (color == null) return fallback;
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
