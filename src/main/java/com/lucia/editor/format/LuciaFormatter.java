package com.lucia.editor.format;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class LuciaFormatter {

    private static final String INDENT = "    ";
    private static final Pattern SPACE_AROUND_EQUAL =
            Pattern.compile("(?<![!<>=])\\s*=\\s*(?!=)");
    private static final Pattern SPACE_AROUND_COMPARISON =
            Pattern.compile("\\s*(==|!=|<=|>=|&&|\\|\\|)\\s*");
    private static final Pattern SPACE_AROUND_MUL =
            Pattern.compile("\\s*([+*/%])\\s*");
    private static final Pattern SPACE_AROUND_MINUS =
            Pattern.compile("(?<=[\\w\\)\\]])\\s*-\\s*(?=[\\w\\(\\[])");
        private static final Pattern SPACE_AROUND_IN =
            Pattern.compile("\\bfor\\s*\\((.*?)\\bin\\b(.*?)\\)");

    private LuciaFormatter() {
    }

    public static String format(String source) {
        String normalized = source.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        List<String> result = new ArrayList<>();
        int indentLevel = 0;
        boolean previousBlank = true;

        for (String rawLine : lines) {
            String content = stripTrailingWhitespace(rawLine.replace("\t", INDENT)).strip();
            if (content.isEmpty()) {
                if (!previousBlank && !result.isEmpty()) {
                    result.add("");
                }
                previousBlank = true;
                continue;
            }

            int effectiveIndent = Math.max(0, indentLevel - leadingClosingBraces(content));
            result.add(INDENT.repeat(effectiveIndent) + formatLine(content));

            indentLevel = Math.max(0, indentLevel + netBraceDelta(content));
            previousBlank = false;
        }

        while (!result.isEmpty() && result.get(result.size() - 1).isBlank()) {
            result.remove(result.size() - 1);
        }

        return String.join("\n", result) + "\n";
    }

    private static String formatLine(String line) {
        String comment = extractLineComment(line);
        String code = comment == null ? line : line.substring(0, line.length() - comment.length());
        String formattedCode = formatCodeSegments(code);

        if (comment == null) {
            return formattedCode;
        }
        if (formattedCode.isBlank()) {
            return comment.stripTrailing();
        }
        return formattedCode + " " + comment.stripTrailing();
    }

    private static String formatCodeSegments(String code) {
        StringBuilder out = new StringBuilder();
        StringBuilder segment = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (inString) {
                segment.append(c);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    out.append(segment);
                    segment.setLength(0);
                    inString = false;
                }
                continue;
            }

            if (c == '"') {
                if (segment.length() > 0) {
                    out.append(applySpacingRules(segment.toString()));
                    segment.setLength(0);
                }
                segment.append(c);
                inString = true;
                escaped = false;
                continue;
            }

            segment.append(c);
        }

        if (segment.length() > 0) {
            out.append(inString ? segment : applySpacingRules(segment.toString()));
        }

        return out.toString().trim();
    }

    private static String applySpacingRules(String segment) {
        String text = segment.replaceAll("\\s+", " ").trim();
        text = text.replaceAll("\\s*,\\s*", ", ");
        text = text.replaceAll("\\s*;\\s*", "; ");
        text = text.replaceAll("\\(\\s+", "(");
        text = text.replaceAll("\\s+\\)", ")");
        text = text.replaceAll("\\[\\s+", "[");
        text = text.replaceAll("\\s+\\]", "]");
        text = text.replaceAll("\\)\\s*\\{", ") {");
        text = text.replaceAll("\\belse\\s*\\{", "else {");
        text = text.replaceAll("\\bdo\\s*\\{", "do {");
        text = text.replaceAll("\\b(if|while|for|switch)\\s*\\(", "$1 (");
        text = text.replaceAll("\\belse\\s+if\\s*\\(", "else if (");
        text = text.replaceAll("\\s*\\?\\s*", " ? ");
        text = SPACE_AROUND_EQUAL.matcher(text).replaceAll(" = ");
        text = SPACE_AROUND_COMPARISON.matcher(text).replaceAll(" $1 ");
        text = SPACE_AROUND_MUL.matcher(text).replaceAll(" $1 ");
        text = SPACE_AROUND_MINUS.matcher(text).replaceAll(" - ");
        text = text.replaceAll("(?<=\\w)\\s*:(?=\\s*[^=])", ": ");
        text = text.replaceAll("\\breturn\\(", "return (");
        text = normalizeForInSpacing(text);
        text = text.replaceAll(":\\s*", ": ");
        text = text.replaceAll("\\{\\s*\\}", "{ }");
        text = text.replaceAll("\\s+", " ").trim();
        return text;
    }

    private static String normalizeForInSpacing(String text) {
        var matcher = SPACE_AROUND_IN.matcher(text);
        if (!matcher.find()) {
            return text;
        }
        String left = matcher.group(1).trim();
        String right = matcher.group(2).trim();
        return matcher.replaceFirst("for (" + left + " in " + right + ")");
    }

    private static String extractLineComment(String line) {
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < line.length() - 1; i++) {
            char c = line.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                escaped = false;
                continue;
            }
            if (c == '/' && line.charAt(i + 1) == '/') {
                return line.substring(i);
            }
        }
        return null;
    }

    private static int leadingClosingBraces(String content) {
        int count = 0;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            if (c == '}') {
                count++;
                continue;
            }
            break;
        }
        return count;
    }

    private static int netBraceDelta(String content) {
        boolean inString = false;
        boolean escaped = false;
        int delta = 0;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (!inString && c == '/' && i + 1 < content.length() && content.charAt(i + 1) == '/') {
                break;
            }
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                escaped = false;
                continue;
            }
            if (c == '{') {
                delta++;
            } else if (c == '}') {
                delta--;
            }
        }
        return delta;
    }

    private static String stripTrailingWhitespace(String value) {
        int end = value.length();
        while (end > 0 && Character.isWhitespace(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end);
    }
}