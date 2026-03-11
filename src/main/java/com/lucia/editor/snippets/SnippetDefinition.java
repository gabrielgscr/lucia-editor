package com.lucia.editor.snippets;

public record SnippetDefinition(String prefix, String description, String template) {

    public SnippetDefinition {
        prefix = prefix == null ? "" : prefix.trim();
        description = description == null ? "" : description.trim();
        template = template == null ? "" : template;
    }

    @Override
    public String toString() {
        if (description.isBlank()) {
            return prefix;
        }
        return prefix + " - " + description;
    }
}
