package com.lucia.editor.ui;

import com.lucia.editor.i18n.I18n;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

public class TerminalPanel extends JPanel {

    private final JTextArea terminalOutput;
    private final JTextField terminalInput;
    private volatile Process shellProcess;
    private volatile PrintWriter shellStdin;
    private Path projectRoot;

    public TerminalPanel() {
        super(new BorderLayout());

        Font monoFont = new Font(Font.MONOSPACED, Font.PLAIN, 13);

        terminalOutput = new JTextArea(8, 100);
        terminalOutput.setEditable(false);
        terminalOutput.setFont(monoFont);

        terminalInput = new JTextField();
        terminalInput.setFont(monoFont);
        terminalInput.addActionListener(e -> sendToShell());

        JButton clearBtn = new JButton(FontIcon.of(FontAwesomeSolid.TRASH, 14, resolveIconColor()));
        clearBtn.setToolTipText(I18n.tr("terminal.clear"));
        clearBtn.setFocusable(false);
        clearBtn.addActionListener(e -> clearTerminal());

        JPanel inputBar = new JPanel(new BorderLayout(4, 0));
        inputBar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        inputBar.add(new JLabel("  $"), BorderLayout.WEST);
        inputBar.add(terminalInput, BorderLayout.CENTER);
        inputBar.add(clearBtn, BorderLayout.EAST);

        add(new JScrollPane(terminalOutput), BorderLayout.CENTER);
        add(inputBar, BorderLayout.SOUTH);
    }

    public void setProjectRoot(Path projectRoot) {
        this.projectRoot = projectRoot;
    }

    /** Requests focus on the input field; starts the shell if not running. */
    public void focusTerminal() {
        ensureShellStarted();
        terminalInput.requestFocusInWindow();
    }

    /** Starts the shell only if it is not already alive. */
    public void ensureShellStarted() {
        if (shellProcess == null || !shellProcess.isAlive()) {
            startShellProcess();
        }
    }

    /** Starts (or restarts) the shell process. */
    public void startShellProcess() {
        if (shellProcess != null && shellProcess.isAlive()) {
            shellProcess.destroyForcibly();
        }
        shellStdin = null;

        String shell = System.getenv("SHELL");
        if (shell == null || shell.isBlank()) {
            shell = "/bin/bash";
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(shell, "-i");
            Path startDir = projectRoot != null ? projectRoot
                    : Path.of(System.getProperty("user.home"));
            pb.directory(startDir.toFile());
            pb.redirectErrorStream(true);
            pb.environment().put("TERM", "dumb");
            pb.environment().put("PS1", "$ ");
            pb.environment().put("PS2", "> ");

            shellProcess = pb.start();
            shellStdin = new PrintWriter(shellProcess.getOutputStream(), true);

            appendTerminal(I18n.tr("log.terminalStarted") + " [" + startDir + "]\n\n");

            Thread reader = new Thread(this::readShellOutput, "terminal-reader");
            reader.setDaemon(true);
            reader.start();
        } catch (IOException ex) {
            appendTerminal(I18n.tr("log.error") + ": " + ex.getMessage() + "\n");
        }
    }

    /** Kills the shell process; called on window close. */
    public void destroyShell() {
        if (shellProcess != null && shellProcess.isAlive()) {
            shellProcess.destroyForcibly();
        }
    }

    // ── private helpers ────────────────────────────────────────────────

    private void readShellOutput() {
        try (InputStreamReader isr = new InputStreamReader(
                shellProcess.getInputStream(), StandardCharsets.UTF_8)) {
            char[] buffer = new char[256];
            int read;
            while ((read = isr.read(buffer)) != -1) {
                String chunk = stripAnsi(new String(buffer, 0, read));
                SwingUtilities.invokeLater(() -> appendTerminal(chunk));
            }
        } catch (IOException ignored) {
            // Stream closed when shell exits.
        }
        SwingUtilities.invokeLater(() ->
                appendTerminal("\n[" + I18n.tr("log.terminalStopped") + "]\n"));
    }

    private void sendToShell() {
        PrintWriter writer = shellStdin;
        if (writer == null || !shellProcess.isAlive()) {
            startShellProcess();
            return;
        }
        String cmd = terminalInput.getText();
        terminalInput.setText("");
        if (cmd.trim().equalsIgnoreCase("clear") || cmd.trim().equalsIgnoreCase("cls")) {
            clearTerminal();
            return;
        }
        writer.println(cmd);
    }

    private void appendTerminal(String text) {
        terminalOutput.append(text);
        terminalOutput.setCaretPosition(terminalOutput.getDocument().getLength());
    }

    private void clearTerminal() {
        terminalOutput.setText("");
        PrintWriter writer = shellStdin;
        if (writer != null && shellProcess != null && shellProcess.isAlive()) {
            writer.println("");
        }
    }

    private static String stripAnsi(String text) {
        String result = text.replaceAll("\u001B\\[[^@-~]*[@-~]", "");
        result = result.replaceAll("\u001B[^\\[\u001B]", "");
        result = result.replace("\r\n", "\n").replace("\r", "\n");
        return result;
    }

    private static Color resolveIconColor() {
        Color fg = UIManager.getColor("Label.foreground");
        if (fg == null) return Color.DARK_GRAY;
        double luminance = 0.2126 * fg.getRed() + 0.7152 * fg.getGreen() + 0.0722 * fg.getBlue();
        return luminance > 180 ? new Color(200, 200, 200) : fg;
    }
}
