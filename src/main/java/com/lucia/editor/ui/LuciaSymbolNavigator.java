/*
 * Copyright (c) 2026 Gabriel González
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.lucia.editor.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight project-wide symbol navigation for Lucia files.
 * It discovers definitions with simple patterns and finds references with word-boundary matching.
 */
public class LuciaSymbolNavigator {

    private static final Pattern DEF_FUNC = Pattern.compile("^\\s*func\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\(");
    private static final Pattern DEF_CLASS = Pattern.compile("^\\s*class\\s+([A-Za-z_][A-Za-z0-9_]*)\\b");
    private static final Pattern DEF_LET = Pattern.compile("^\\s*let\\s+([A-Za-z_][A-Za-z0-9_]*)\\b");
    private static final Pattern DEF_CONST = Pattern.compile("^\\s*const\\s+([A-Za-z_][A-Za-z0-9_]*)\\b");

    public String getSymbolAt(String text, int caretPosition) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        int pos = Math.max(0, Math.min(caretPosition, text.length() - 1));
        if (!isSymbolChar(text.charAt(pos)) && pos > 0 && isSymbolChar(text.charAt(pos - 1))) {
            pos--;
        }
        if (!isSymbolChar(text.charAt(pos))) {
            return null;
        }

        int start = pos;
        int end = pos;
        while (start > 0 && isSymbolChar(text.charAt(start - 1))) {
            start--;
        }
        while (end + 1 < text.length() && isSymbolChar(text.charAt(end + 1))) {
            end++;
        }
        String symbol = text.substring(start, end + 1);
        return symbol.isBlank() ? null : symbol;
    }

    public List<SymbolDefinition> findDefinitions(Path projectRoot, String symbol) throws IOException {
        List<SymbolDefinition> definitions = new ArrayList<>();
        if (projectRoot == null || symbol == null || symbol.isBlank()) {
            return definitions;
        }

        try (var stream = Files.walk(projectRoot)) {
            List<Path> luciaFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".lucia"))
                    .sorted()
                    .toList();

            for (Path file : luciaFiles) {
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    SymbolDefinition found = findDefinitionInLine(projectRoot, file, i + 1, line, symbol);
                    if (found != null) {
                        definitions.add(found);
                    }
                }
            }
        }

        definitions.sort(Comparator
            .comparing((SymbolDefinition d) -> d.location().path().toString())
            .thenComparingInt(d -> d.location().line())
            .thenComparingInt(d -> d.location().column()));
        return definitions;
    }

    public List<SymbolReference> findReferences(Path projectRoot, String symbol) throws IOException {
        List<SymbolReference> references = new ArrayList<>();
        if (projectRoot == null || symbol == null || symbol.isBlank()) {
            return references;
        }

        Pattern symbolPattern = Pattern.compile("\\b" + Pattern.quote(symbol) + "\\b");
        List<SymbolDefinition> definitions = findDefinitions(projectRoot, symbol);

        try (var stream = Files.walk(projectRoot)) {
            List<Path> luciaFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".lucia"))
                    .sorted()
                    .toList();

            for (Path file : luciaFiles) {
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    Matcher matcher = symbolPattern.matcher(line);
                    while (matcher.find()) {
                        int lineNumber = i + 1;
                        int colNumber = matcher.start() + 1;
                        boolean isDefinition = matchesDefinition(definitions, file, lineNumber, colNumber);
                        SymbolLocation location = new SymbolLocation(projectRoot, file, lineNumber, colNumber,
                                Math.max(1, matcher.end() - matcher.start()), line);
                        references.add(new SymbolReference(symbol, isDefinition, location));
                    }
                }
            }
        }

        references.sort(Comparator
                .comparing((SymbolReference r) -> r.location().path().toString())
                .thenComparingInt(r -> r.location().line())
                .thenComparingInt(r -> r.location().column()));
        return references;
    }

    private boolean matchesDefinition(List<SymbolDefinition> definitions, Path file, int line, int column) {
        for (SymbolDefinition definition : definitions) {
            SymbolLocation location = definition.location();
            if (location.path().equals(file)
                    && location.line() == line
                    && location.column() == column) {
                return true;
            }
        }
        return false;
    }

    private SymbolDefinition findDefinitionInLine(Path root, Path file, int lineNumber, String line, String symbol) {
        Matcher funcMatcher = DEF_FUNC.matcher(line);
        if (funcMatcher.find() && symbol.equals(funcMatcher.group(1))) {
            int start = funcMatcher.start(1);
            return new SymbolDefinition(symbol, SymbolKind.FUNCTION,
                    new SymbolLocation(root, file, lineNumber, start + 1, symbol.length(), line));
        }

        Matcher classMatcher = DEF_CLASS.matcher(line);
        if (classMatcher.find() && symbol.equals(classMatcher.group(1))) {
            int start = classMatcher.start(1);
            return new SymbolDefinition(symbol, SymbolKind.CLASS,
                    new SymbolLocation(root, file, lineNumber, start + 1, symbol.length(), line));
        }

        Matcher letMatcher = DEF_LET.matcher(line);
        if (letMatcher.find() && symbol.equals(letMatcher.group(1))) {
            int start = letMatcher.start(1);
            return new SymbolDefinition(symbol, SymbolKind.VARIABLE,
                    new SymbolLocation(root, file, lineNumber, start + 1, symbol.length(), line));
        }

        Matcher constMatcher = DEF_CONST.matcher(line);
        if (constMatcher.find() && symbol.equals(constMatcher.group(1))) {
            int start = constMatcher.start(1);
            return new SymbolDefinition(symbol, SymbolKind.VARIABLE,
                new SymbolLocation(root, file, lineNumber, start + 1, symbol.length(), line));
        }

        return null;
    }

    private boolean isSymbolChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    public enum SymbolKind {
        FUNCTION,
        CLASS,
        VARIABLE
    }

    public record SymbolLocation(Path root, Path path, int line, int column, int length, String preview) {
        @Override
        public String toString() {
            Path normalizedRoot = root.toAbsolutePath().normalize();
            Path normalizedPath = path.toAbsolutePath().normalize();
            String relative = normalizedRoot.relativize(normalizedPath).toString();
            String cleanPreview = preview == null ? "" : preview.trim();
            return relative + ":" + line + ":" + column + "  |  " + cleanPreview;
        }
    }

    public record SymbolDefinition(String symbol, SymbolKind kind, SymbolLocation location) {
        @Override
        public String toString() {
            return kind.name().toLowerCase(Locale.ROOT) + " - " + location;
        }
    }

    public record SymbolReference(String symbol, boolean definition, SymbolLocation location) {
        @Override
        public String toString() {
            String marker = definition ? "[def]" : "[ref]";
            return marker + " " + location;
        }
    }
}
