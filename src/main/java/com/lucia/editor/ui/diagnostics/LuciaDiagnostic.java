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
