package com.lucia.editor.help;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads help metadata and markdown articles from classpath resources.
 */
public final class HelpService {

    private static final String HELP_ROOT = "help";

    private final String language;
    private final List<ArticleMeta> orderedArticles;
    private final Map<String, ArticleMeta> articlesById;
    private final Map<String, String> markdownCache;

    public HelpService(Locale locale) {
        this.language = normalizeLanguage(locale);
        this.orderedArticles = new ArrayList<>();
        this.articlesById = new LinkedHashMap<>();
        this.markdownCache = new LinkedHashMap<>();
        loadIndex();
    }

    public String language() {
        return language;
    }

    public List<HelpArticle> listArticles() {
        List<HelpArticle> result = new ArrayList<>();
        for (ArticleMeta meta : orderedArticles) {
            String markdown = loadMarkdown(meta.fileName());
            result.add(meta.toArticle(markdown));
        }
        return result;
    }

    public List<HelpArticle> search(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return listArticles();
        }

        List<ScoredArticle> scored = new ArrayList<>();
        for (ArticleMeta meta : orderedArticles) {
            String markdown = loadMarkdown(meta.fileName());
            int score = score(meta, markdown, normalized);
            if (score > 0) {
                scored.add(new ScoredArticle(meta, markdown, score));
            }
        }

        scored.sort(Comparator
                .comparingInt(ScoredArticle::score)
                .reversed()
                .thenComparing(sa -> sa.meta().order())
                .thenComparing(sa -> sa.meta().title()));

        List<HelpArticle> result = new ArrayList<>();
        for (ScoredArticle item : scored) {
            result.add(item.meta().toArticle(item.markdown()));
        }
        return result;
    }

    public HelpArticle findById(String articleId) {
        if (articleId == null || articleId.isBlank()) {
            return null;
        }
        ArticleMeta meta = articlesById.get(articleId);
        if (meta == null) {
            return null;
        }
        return meta.toArticle(loadMarkdown(meta.fileName()));
    }

    private int score(ArticleMeta meta, String markdown, String query) {
        int score = 0;
        if (meta.id().equalsIgnoreCase(query)) score += 100;
        if (meta.title().toLowerCase(Locale.ROOT).contains(query)) score += 50;
        if (meta.summary().toLowerCase(Locale.ROOT).contains(query)) score += 30;
        for (String keyword : meta.keywords()) {
            if (keyword.toLowerCase(Locale.ROOT).contains(query)) {
                score += 20;
            }
        }
        if (markdown.toLowerCase(Locale.ROOT).contains(query)) score += 10;
        return score;
    }

    private void loadIndex() {
        String resource = HELP_ROOT + "/" + language + "/index.txt";
        String content = readResource(resource);
        if (content == null || content.isBlank()) {
            return;
        }

        int lineNumber = 0;
        for (String rawLine : content.split("\\R")) {
            lineNumber++;
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\|", -1);
            if (parts.length < 6) {
                continue;
            }

            String id = parts[0].trim();
            String title = parts[1].trim();
            String summary = parts[2].trim();
            List<String> keywords = splitCsv(parts[3]);
            String fileName = parts[4].trim();
            List<String> related = splitCsv(parts[5]);
            ArticleMeta meta = new ArticleMeta(id, title, summary, keywords, fileName, related, lineNumber);
            orderedArticles.add(meta);
            articlesById.put(id, meta);
        }
    }

    private List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String part : csv.split(",")) {
            String value = part.trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return Collections.unmodifiableList(values);
    }

    private String loadMarkdown(String fileName) {
        return markdownCache.computeIfAbsent(fileName, name -> {
            String path = HELP_ROOT + "/" + language + "/" + name;
            String content = readResource(path);
            return content == null ? "" : content;
        });
    }

    private String readResource(String path) {
        try (InputStream in = HelpService.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return null;
        }
    }

    private String normalizeLanguage(Locale locale) {
        if (locale == null) {
            return "en";
        }
        String lang = locale.getLanguage();
        if (lang == null || lang.isBlank()) {
            return "en";
        }
        return lang.toLowerCase(Locale.ROOT).startsWith("es") ? "es" : "en";
    }

    private record ScoredArticle(ArticleMeta meta, String markdown, int score) {
    }

    private record ArticleMeta(
            String id,
            String title,
            String summary,
            List<String> keywords,
            String fileName,
            List<String> related,
            int order) {

        HelpArticle toArticle(String markdown) {
            return new HelpArticle(id, title, summary, keywords, markdown, related);
        }
    }
}
