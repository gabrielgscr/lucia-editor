package com.lucia.editor.ui.diagnostics;

import com.lucia.editor.config.EditorConfig;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.SwingUtilities;

/**
 * Produces diagnostics by invoking Lucia's Python pipeline on in-memory source text.
 */
public class LuciaDiagnosticsService {

    @FunctionalInterface
    public interface Listener {
        void onDiagnostics(Path file, List<LuciaDiagnostic> diagnostics);
    }

    private static final String DIAGNOSTICS_SCRIPT = """
            import json
            import sys
            from pathlib import Path

            from lucia.cli import build_code
            from lucia.frontend.parser import LuciaSyntaxErrors
            from lucia.semantics.errors import LuciaSemanticError

            source = sys.stdin.read()
            source_path = Path(sys.argv[1]).resolve()
            diagnostics = []

            try:
                build_code(source, source_path)
            except LuciaSyntaxErrors as ex:
                for issue in ex.issues:
                    diagnostics.append({
                        'severity': 'ERROR',
                        'line': issue.line or 1,
                        'column': issue.column or 1,
                        'length': 1,
                        'message': issue.message,
                    })
            except LuciaSemanticError as ex:
                diagnostics.append({
                    'severity': 'ERROR',
                    'line': ex.line or 1,
                    'column': ex.column or 1,
                    'length': 1,
                    'message': str(ex),
                })
            except Exception as ex:
                diagnostics.append({
                    'severity': 'ERROR',
                    'line': 1,
                    'column': 1,
                    'length': 1,
                    'message': f'Internal compiler error: {ex}',
                })

            print(json.dumps(diagnostics, ensure_ascii=True))
            """;

    private final EditorConfig config;
    private final Listener listener;
    private final ScheduledExecutorService scheduler;
    private final AtomicLong sequence;
    private final Map<Path, ScheduledFuture<?>> pending;
    private final Map<Path, Long> latestTokenByFile;
    private volatile int debounceMs;

    public LuciaDiagnosticsService(EditorConfig config, Listener listener) {
        this.config = config;
        this.listener = listener;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "lucia-diagnostics");
            t.setDaemon(true);
            return t;
        });
        this.sequence = new AtomicLong(0L);
        this.pending = new ConcurrentHashMap<>();
        this.latestTokenByFile = new ConcurrentHashMap<>();
        this.debounceMs = Math.max(100, config.getDiagnosticsDebounceMs());
    }

    public void setDebounceMs(int debounceMs) {
        this.debounceMs = Math.max(100, debounceMs);
    }

    public void requestAnalysis(Path file, String sourceText) {
        if (file == null) {
            return;
        }
        Path normalized = file.toAbsolutePath().normalize();
        long token = sequence.incrementAndGet();
        latestTokenByFile.put(normalized, token);

        ScheduledFuture<?> previous = pending.remove(normalized);
        if (previous != null) {
            previous.cancel(false);
        }

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            List<LuciaDiagnostic> diagnostics = analyzeSource(normalized, sourceText == null ? "" : sourceText);
            Long latest = latestTokenByFile.get(normalized);
            if (latest == null || latest != token) {
                return;
            }
            SwingUtilities.invokeLater(() -> listener.onDiagnostics(normalized, diagnostics));
        }, debounceMs, TimeUnit.MILLISECONDS);
        pending.put(normalized, future);
    }

    public void clear(Path file) {
        if (file == null) {
            return;
        }
        Path normalized = file.toAbsolutePath().normalize();
        ScheduledFuture<?> previous = pending.remove(normalized);
        if (previous != null) {
            previous.cancel(false);
        }
        latestTokenByFile.remove(normalized);
    }

    public void shutdown() {
        for (ScheduledFuture<?> future : pending.values()) {
            future.cancel(false);
        }
        pending.clear();
        latestTokenByFile.clear();
        scheduler.shutdownNow();
    }

    private List<LuciaDiagnostic> analyzeSource(Path file, String sourceText) {
        Path luciaRoot = config.getLuciaProjectRoot();
        String pythonExec = config.getPythonExecutable();

        if (!Files.exists(luciaRoot.resolve("main.py"))) {
            return List.of();
        }

        ProcessBuilder builder = new ProcessBuilder(
                pythonExec,
                "-c",
                DIAGNOSTICS_SCRIPT,
                file.toAbsolutePath().toString());
        builder.directory(luciaRoot.toFile());
        builder.redirectErrorStream(true);
        builder.environment().put("PYTHONUNBUFFERED", "1");

        try {
            Process process = builder.start();
            try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(sourceText);
            }

            String output = readAll(process.inputReader(StandardCharsets.UTF_8));
            int exitCode = process.waitFor();
            if (exitCode != 0 && output.isBlank()) {
                return List.of(new LuciaDiagnostic(
                        LuciaDiagnosticSeverity.ERROR,
                        1,
                        1,
                        1,
                        "Diagnostics command failed with exit code " + exitCode));
            }

            List<LuciaDiagnostic> parsed = parseJsonDiagnostics(output);
            if (!parsed.isEmpty()) {
                return parsed;
            }
            if (exitCode != 0) {
                return List.of(new LuciaDiagnostic(
                        LuciaDiagnosticSeverity.ERROR,
                        1,
                        1,
                        1,
                        output.isBlank() ? "Diagnostics command failed" : output.strip()));
            }
            return List.of();
        } catch (Exception ex) {
            return List.of(new LuciaDiagnostic(
                    LuciaDiagnosticSeverity.ERROR,
                    1,
                    1,
                    1,
                    "Diagnostics execution failed: " + ex.getMessage()));
        }
    }

    private String readAll(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[512];
        int read;
        while ((read = reader.read(buffer)) != -1) {
            sb.append(buffer, 0, read);
        }
        return sb.toString();
    }

    private List<LuciaDiagnostic> parseJsonDiagnostics(String json) {
        String trimmed = json == null ? "" : json.strip();
        if (trimmed.isEmpty() || "[]".equals(trimmed)) {
            return List.of();
        }
        if (!(trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return List.of();
        }

        List<LuciaDiagnostic> diagnostics = new ArrayList<>();
        String body = trimmed.substring(1, trimmed.length() - 1).trim();
        if (body.isEmpty()) {
            return List.of();
        }

        for (String objectText : splitTopLevelObjects(body)) {
            LuciaDiagnostic diagnostic = parseObject(objectText.trim());
            if (diagnostic != null) {
                diagnostics.add(diagnostic);
            }
        }
        return Collections.unmodifiableList(diagnostics);
    }

    private List<String> splitTopLevelObjects(String body) {
        List<String> objects = new ArrayList<>();
        int start = -1;
        int depth = 0;
        boolean inString = false;
        char prev = '\0';

        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '"' && prev != '\\') {
                inString = !inString;
            }
            if (!inString) {
                if (c == '{') {
                    if (depth == 0) {
                        start = i;
                    }
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0 && start >= 0) {
                        objects.add(body.substring(start, i + 1));
                        start = -1;
                    }
                }
            }
            prev = c;
        }
        return objects;
    }

    private LuciaDiagnostic parseObject(String objectText) {
        if (!(objectText.startsWith("{") && objectText.endsWith("}"))) {
            return null;
        }

        String content = objectText.substring(1, objectText.length() - 1);
        Map<String, String> values = new ConcurrentHashMap<>();

        boolean inString = false;
        char prev = '\0';
        int start = 0;
        List<String> pairs = new ArrayList<>();
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '"' && prev != '\\') {
                inString = !inString;
            }
            if (!inString && c == ',') {
                pairs.add(content.substring(start, i));
                start = i + 1;
            }
            prev = c;
        }
        if (start < content.length()) {
            pairs.add(content.substring(start));
        }

        for (String pair : pairs) {
            int idx = pair.indexOf(':');
            if (idx <= 0) {
                continue;
            }
            String key = unquote(pair.substring(0, idx).trim());
            String value = pair.substring(idx + 1).trim();
            values.put(key, value);
        }

        LuciaDiagnosticSeverity severity = "WARNING".equalsIgnoreCase(unquote(values.getOrDefault("severity", "\"ERROR\"")))
                ? LuciaDiagnosticSeverity.WARNING
                : LuciaDiagnosticSeverity.ERROR;

        int line = parseInt(values.get("line"), 1);
        int column = parseInt(values.get("column"), 1);
        int length = parseInt(values.get("length"), 1);
        String message = unescapeJson(unquote(values.getOrDefault("message", "\"Unknown error\"")));

        return new LuciaDiagnostic(severity, line, column, length, message);
    }

    private int parseInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String unquote(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String unescapeJson(String value) {
        StringBuilder sb = new StringBuilder();
        try (StringReader reader = new StringReader(value)) {
            int ch;
            boolean escaping = false;
            while ((ch = reader.read()) != -1) {
                char c = (char) ch;
                if (!escaping) {
                    if (c == '\\') {
                        escaping = true;
                    } else {
                        sb.append(c);
                    }
                    continue;
                }

                switch (c) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case '\\' -> sb.append('\\');
                    case '"' -> sb.append('"');
                    default -> sb.append(c);
                }
                escaping = false;
            }
        } catch (IOException ignored) {
            return value;
        }
        return sb.toString();
    }
}
