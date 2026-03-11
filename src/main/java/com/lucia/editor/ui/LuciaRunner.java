package com.lucia.editor.ui;

import com.lucia.editor.config.EditorConfig;
import com.lucia.editor.i18n.I18n;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * Handles execution of Lucia CLI commands and test runs as background processes.
 * Output is forwarded via callbacks to avoid coupling with specific UI widgets.
 */
public class LuciaRunner {

    private final EditorConfig config;
    private final JFrame owner;
    private final JTextField inputField;
    private final Consumer<String> appendLine;
    private final Consumer<String> appendRaw;
    private volatile PrintWriter processStdin;

    /**
     * @param config       editor configuration (lucy root, python executable)
     * @param owner        parent frame used for dialog ownership
     * @param inputField   the input field in the output panel; runner manages enable/disable
     * @param appendLine   callback to append a full line to the output panel
     * @param appendRaw    callback to append raw (unbuffered) text to the output panel
     */
    public LuciaRunner(EditorConfig config, JFrame owner, JTextField inputField,
                       Consumer<String> appendLine, Consumer<String> appendRaw) {
        this.config      = config;
        this.owner       = owner;
        this.inputField  = inputField;
        this.appendLine  = appendLine;
        this.appendRaw   = appendRaw;

        // Install interactive-input handler directly on the field.
        inputField.addActionListener(e -> {
            PrintWriter writer = processStdin;
            if (writer == null) return;
            String text = inputField.getText();
            inputField.setText("");
            appendLine.accept("> " + text);
            writer.println(text);
        });
    }

    /** Runs the given file with the Lucia interpreter. */
    public void run(Path file) {
        executeCli("run", false, file,
                config.isShowGeneratedCode(),
                config.getDefaultTarget());
    }

    /** Compiles the given file and saves output. */
    public void compile(Path file) {
        executeCli("compile", true, file,
                config.isShowGeneratedCode(),
                config.getDefaultTarget());
    }

    /** Runs the project's test suite via pytest. */
    public void runTests() {
        Path luciaRoot   = config.getLuciaProjectRoot();
        String pythonExec = config.getPythonExecutable();
        if (!Files.exists(luciaRoot.resolve("main.py"))) {
            showError(I18n.tr("error.invalidLuciaRoot") + ": " + luciaRoot);
            return;
        }
        executeProcess(List.of(pythonExec, "-m", "pytest", "-q"), luciaRoot,
                I18n.tr("log.testsStarted"));
    }

    /** Prompts the user for arbitrary CLI args and runs them. */
    public void runCustom() {
        Path luciaRoot   = config.getLuciaProjectRoot();
        String pythonExec = config.getPythonExecutable();
        if (!Files.exists(luciaRoot.resolve("main.py"))) {
            showError(I18n.tr("error.invalidLuciaRoot") + ": " + luciaRoot);
            return;
        }

        String args = JOptionPane.showInputDialog(owner,
                I18n.tr("prompt.customCommand"), "run examples/00_features.lucia");
        if (args == null || args.trim().isEmpty()) return;

        List<String> parsed = parseArguments(args.trim());
        if (parsed.isEmpty()) return;

        List<String> command = new ArrayList<>();
        command.add(pythonExec);
        command.add("main.py");
        command.addAll(parsed);
        executeProcess(command, luciaRoot, I18n.tr("log.customStarted"));
    }

    // ── private ────────────────────────────────────────────────────────

    private void executeCli(String command, boolean saveOutput, Path file,
                            boolean showGeneratedCode, String target) {
        Path luciaRoot   = config.getLuciaProjectRoot();
        String pythonExec = config.getPythonExecutable();
        if (!Files.exists(luciaRoot.resolve("main.py"))) {
            showError(I18n.tr("error.invalidLuciaRoot") + ": " + luciaRoot);
            return;
        }

        appendLine.accept(I18n.tr("log.running") + " [" + command + "]: " + file);

        final int[] exitHolder = new int[]{-1};
        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                List<String> cmd = new ArrayList<>();
                cmd.add(pythonExec);
                cmd.add("main.py");
                cmd.add(command);
                cmd.add(file.toAbsolutePath().toString());
                if (saveOutput) {
                    cmd.add("--save");
                }
                if (showGeneratedCode) {
                    cmd.add("--show-python");
                }
                if (target != null && !target.isBlank()) {
                    cmd.add("--target");
                    cmd.add(target);
                }

                ProcessBuilder builder = new ProcessBuilder(cmd);
                builder.directory(luciaRoot.toFile());
                builder.redirectErrorStream(true);
                builder.environment().put("PYTHONUNBUFFERED", "1");

                Process process = builder.start();
                processStdin = new PrintWriter(process.getOutputStream(), true);
                SwingUtilities.invokeLater(() -> {
                    inputField.setEnabled(true);
                    inputField.requestFocusInWindow();
                });

                try (InputStreamReader reader = new InputStreamReader(
                        process.getInputStream(), StandardCharsets.UTF_8)) {
                    char[] buffer = new char[512];
                    int read;
                    while ((read = reader.read(buffer)) != -1) {
                        publish(new String(buffer, 0, read));
                    }
                }

                exitHolder[0] = process.waitFor();
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                chunks.forEach(appendRaw);
            }

            @Override
            protected void done() {
                processStdin = null;
                SwingUtilities.invokeLater(() -> inputField.setEnabled(false));
                appendLine.accept(I18n.tr("log.exitCode") + ": " + exitHolder[0]);
                appendLine.accept(I18n.tr("log.runFinished"));
            }
        };
        worker.execute();
    }

    private void executeProcess(List<String> command, Path workingDirectory, String startMessage) {
        appendLine.accept(startMessage + ": " + String.join(" ", command));

        final int[] exitHolder = new int[]{-1};
        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                ProcessBuilder builder = new ProcessBuilder(command);
                builder.directory(workingDirectory.toFile());
                builder.redirectErrorStream(true);
                builder.environment().put("PYTHONUNBUFFERED", "1");

                Process process = builder.start();
                try (InputStreamReader reader = new InputStreamReader(
                        process.getInputStream(), StandardCharsets.UTF_8)) {
                    char[] buffer = new char[512];
                    int read;
                    while ((read = reader.read(buffer)) != -1) {
                        publish(new String(buffer, 0, read));
                    }
                }
                exitHolder[0] = process.waitFor();
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                chunks.forEach(appendRaw);
            }

            @Override
            protected void done() {
                appendLine.accept(I18n.tr("log.exitCode") + ": " + exitHolder[0]);
                appendLine.accept(I18n.tr("log.runFinished"));
            }
        };
        worker.execute();
    }

    private List<String> parseArguments(String value) {
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"') { inQuotes = !inQuotes; continue; }
            if (Character.isWhitespace(c) && !inQuotes) {
                if (current.length() > 0) { args.add(current.toString()); current.setLength(0); }
                continue;
            }
            current.append(c);
        }
        if (current.length() > 0) args.add(current.toString());
        return args;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(owner, message, I18n.tr("dialog.error"),
                JOptionPane.ERROR_MESSAGE);
        appendLine.accept(I18n.tr("log.error") + ": " + message);
    }
}
