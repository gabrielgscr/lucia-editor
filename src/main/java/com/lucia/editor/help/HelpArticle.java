package com.lucia.editor.help;

import java.util.List;

public record HelpArticle(
        String id,
        String title,
        String summary,
        List<String> keywords,
        String markdown,
        List<String> relatedIds) {
}
