/*
 * Copyright (c) 2026 Gabriel González
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.lucia.editor.ui.diagnostics;

public record LuciaDiagnostic(
        LuciaDiagnosticSeverity severity,
        int line,
        int column,
        int length,
        String message
) {
    public LuciaDiagnostic {
        line = Math.max(1, line);
        column = Math.max(1, column);
        length = Math.max(1, length);
        message = message == null ? "" : message;
        severity = severity == null ? LuciaDiagnosticSeverity.ERROR : severity;
    }
}
