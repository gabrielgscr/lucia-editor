package com.lucia.editor.snippets;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Persists user snippets in a simple properties file under user home.
 */
public class SnippetManager {

    private static final String SNIPPET_HOME = ".lucia-editor";
    private static final String SNIPPET_FILE = "snippets.properties";
    private static final String KEY_COUNT = "snippet.count";

    private final Path storageFile;

    public SnippetManager() {
        this.storageFile = Path.of(System.getProperty("user.home"), SNIPPET_HOME, SNIPPET_FILE);
    }

    public Path getStorageFile() {
        return storageFile;
    }

    public synchronized List<SnippetDefinition> getSnippets() throws IOException {
        ensureStorageExists();
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(storageFile, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        int count = Integer.parseInt(properties.getProperty(KEY_COUNT, "0"));
        List<SnippetDefinition> snippets = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String prefix = properties.getProperty("snippet." + i + ".prefix", "").trim();
            String description = properties.getProperty("snippet." + i + ".description", "").trim();
            String encodedTemplate = properties.getProperty("snippet." + i + ".template", "");
            if (prefix.isBlank() || encodedTemplate.isBlank()) {
                continue;
            }
            String template = new String(Base64.getDecoder().decode(encodedTemplate), StandardCharsets.UTF_8);
            snippets.add(new SnippetDefinition(prefix, description, template));
        }

        snippets.sort(Comparator.comparing(SnippetDefinition::prefix));
        return snippets;
    }

    public synchronized void saveSnippets(List<SnippetDefinition> snippets) throws IOException {
        ensureStorageDirectory();

        List<SnippetDefinition> normalized = normalize(snippets);
        Properties properties = new Properties();
        properties.setProperty(KEY_COUNT, String.valueOf(normalized.size()));

        for (int i = 0; i < normalized.size(); i++) {
            SnippetDefinition snippet = normalized.get(i);
            properties.setProperty("snippet." + i + ".prefix", snippet.prefix());
            properties.setProperty("snippet." + i + ".description", snippet.description());
            properties.setProperty("snippet." + i + ".template",
                    Base64.getEncoder().encodeToString(snippet.template().getBytes(StandardCharsets.UTF_8)));
        }

        try (var writer = Files.newBufferedWriter(storageFile,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            properties.store(writer, "Lucia Editor snippets");
        }
    }

    private void ensureStorageExists() throws IOException {
        if (Files.exists(storageFile)) {
            return;
        }
        saveSnippets(defaultSnippets());
    }

    private void ensureStorageDirectory() throws IOException {
        Path parent = storageFile.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    private List<SnippetDefinition> normalize(List<SnippetDefinition> snippets) {
        Map<String, SnippetDefinition> byPrefix = new LinkedHashMap<>();
        if (snippets != null) {
            for (SnippetDefinition snippet : snippets) {
                if (snippet == null || snippet.prefix().isBlank() || snippet.template().isBlank()) {
                    continue;
                }
                byPrefix.put(snippet.prefix(), snippet);
            }
        }
        return byPrefix.values().stream()
                .sorted(Comparator.comparing(SnippetDefinition::prefix))
                .toList();
    }

    private List<SnippetDefinition> defaultSnippets() {
        return List.of(
                new SnippetDefinition("func", "func name(...)",
                        "func ${name}(${params}): ${returnType} {\n\t${cursor}\n}"),
                new SnippetDefinition("if", "if (...) {...}",
                        "if (${condition}) {\n\t${cursor}\n}"),
                new SnippetDefinition("ifelse", "if (...) {...} else {...}",
                        "if (${condition}) {\n\t${cursor}\n} else {\n\t\n}"),
                new SnippetDefinition("for", "for (...) {...}",
                        "for (${init}; ${cond}; ${update}) {\n\t${cursor}\n}"),
                new SnippetDefinition("while", "while (...) {...}",
                        "while (${condition}) {\n\t${cursor}\n}"),
                new SnippetDefinition("const", "const name: type = value;",
                    "const ${name}: ${type} = ${value};"),
                new SnippetDefinition("date", "date(year, month, day)",
                    "date(${year}, ${month}, ${day})"),
                new SnippetDefinition("datetime", "datetime(year, month, day, hour, minute, second)",
                    "datetime(${year}, ${month}, ${day}, ${hour}, ${minute}, ${second})"),
                new SnippetDefinition("try", "try/catch",
                    "try {\n\t${cursor}\n} catch (${err}) {\n\t\n}"),
                new SnippetDefinition("tryf", "try/catch/finally",
                    "try {\n\t${cursor}\n} catch (${err}) {\n\t\n} finally {\n\t\n}"),
                new SnippetDefinition("interp", "interpolacion de string",
                    "\"${greeting} ${name}: ${value}\""),
                new SnippetDefinition("class", "class Name {...}",
                    "class ${Name} {\n\tconstructor(${params}) {\n\t\t${cursor}\n\t}\n}"),
                new SnippetDefinition("classext", "class Child extends Parent {...}",
                    "class ${Child} extends ${Parent} {\n\tconstructor(${params}) {\n\t\t${cursor}\n\t}\n}")
        );
    }
}
