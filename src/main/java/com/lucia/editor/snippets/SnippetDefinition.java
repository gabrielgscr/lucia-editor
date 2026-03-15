/*
 * Copyright (c) 2026 Gabriel González
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

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
