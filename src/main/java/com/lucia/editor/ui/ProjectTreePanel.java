package com.lucia.editor.ui;

import com.lucia.editor.i18n.I18n;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * Manages the project file tree and related file-creation operations.
 * Not a UI component itself — exposes the underlying JTree via {@link #getTree()}.
 */
public class ProjectTreePanel {

    private final JTree tree;
    private Path projectRoot;
    private final Consumer<Path> onFileOpen;
    private final Consumer<String> onLog;

    /**
     * @param onFileOpen callback invoked when the user selects a .lucia file or creates one
     * @param onLog      callback for log/status messages
     */
    public ProjectTreePanel(Consumer<Path> onFileOpen, Consumer<String> onLog) {
        this.onFileOpen = onFileOpen;
        this.onLog      = onLog;

        tree = new JTree(new DefaultMutableTreeNode(I18n.tr("project.none")));
        tree.addTreeSelectionListener(this::onTreeSelection);
        tree.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e)  { showContextMenu(e); }
            @Override public void mouseReleased(MouseEvent e) { showContextMenu(e); }
        });
        ToolTipManager.sharedInstance().registerComponent(tree);
        tree.setCellRenderer(new FileTreeCellRenderer());
        tree.setRowHeight(22);
        tree.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    public JTree getTree() {
        return tree;
    }

    /** Sets the project root and immediately rebuilds the tree. */
    public void setProjectRoot(Path projectRoot) {
        this.projectRoot = projectRoot;
        rebuildTree();
    }

    /** Rebuilds the tree from the current project root. */
    public void rebuildTree() {
        if (projectRoot == null) return;
        tree.setModel(new DefaultTreeModel(buildNode(projectRoot, true)));
    }

    /** Returns the directory that should receive new files/folders based on tree selection. */
    public Path getSelectedDirectoryForCreation() {
        if (projectRoot == null) return null;
        Object selected = tree.getLastSelectedPathComponent();
        if (selected instanceof DefaultMutableTreeNode node
                && node.getUserObject() instanceof Path path) {
            if (Files.isDirectory(path)) return path;
            Path parent = path.getParent();
            if (parent != null) return parent;
        }
        return projectRoot;
    }

    /** Prompts the user for a filename and creates a new .lucia file. */
    public void createLuciaFile() {
        Path directory = getSelectedDirectoryForCreation();
        if (directory == null) { showError(I18n.tr("error.noProject")); return; }

        String name = JOptionPane.showInputDialog(
                SwingUtilities.getWindowAncestor(tree),
                I18n.tr("prompt.newFileName"), "nuevo.lucia");
        if (name == null || name.trim().isEmpty()) return;

        String normalizedName = name.trim();
        if (!normalizedName.endsWith(".lucia")) normalizedName += ".lucia";

        Path newFile = directory.resolve(normalizedName);
        if (Files.exists(newFile)) { showError(I18n.tr("error.alreadyExists") + ": " + newFile); return; }

        try {
            Files.createFile(newFile);
            rebuildTree();
            onFileOpen.accept(newFile);
            onLog.accept(I18n.tr("log.fileCreated") + ": " + newFile);
        } catch (IOException ex) {
            showError(I18n.tr("error.createFile") + ": " + ex.getMessage());
        }
    }

    /** Prompts the user for a name and creates a new folder inside the project. */
    public void createFolder() {
        Path directory = getSelectedDirectoryForCreation();
        if (directory == null) { showError(I18n.tr("error.noProject")); return; }

        String name = JOptionPane.showInputDialog(
                SwingUtilities.getWindowAncestor(tree),
                I18n.tr("prompt.newFolderName"), "nuevo_modulo");
        if (name == null || name.trim().isEmpty()) return;

        Path newFolder = directory.resolve(name.trim());
        if (Files.exists(newFolder)) { showError(I18n.tr("error.alreadyExists") + ": " + newFolder); return; }

        try {
            Files.createDirectory(newFolder);
            rebuildTree();
            onLog.accept(I18n.tr("log.folderCreated") + ": " + newFolder);
        } catch (IOException ex) {
            showError(I18n.tr("error.createFolder") + ": " + ex.getMessage());
        }
    }

    // ── private helpers ────────────────────────────────────────────────

    private DefaultMutableTreeNode buildNode(Path path, boolean isRoot) {
        String label = isRoot
                ? path.toAbsolutePath().normalize().toString()
                : (path.getFileName() != null ? path.getFileName().toString() : path.toString());

        DefaultMutableTreeNode node = new DefaultMutableTreeNode(label);
        node.setUserObject(path);

        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path).sorted()) {
                stream.forEach(child -> {
                    if (Files.isDirectory(child) || child.toString().endsWith(".lucia")) {
                        node.add(buildNode(child, false));
                    }
                });
            } catch (IOException ex) {
                onLog.accept(I18n.tr("log.error") + ": " + ex.getMessage());
            }
        }
        return node;
    }

    private void onTreeSelection(TreeSelectionEvent event) {
        Object selected = tree.getLastSelectedPathComponent();
        if (!(selected instanceof DefaultMutableTreeNode node)) return;
        if (!(node.getUserObject() instanceof Path path)) return;
        if (Files.isRegularFile(path) && path.toString().endsWith(".lucia")) {
            onFileOpen.accept(path);
        }
    }

    private void showContextMenu(MouseEvent event) {
        if (!event.isPopupTrigger()) return;
        int row = tree.getRowForLocation(event.getX(), event.getY());
        if (row >= 0) tree.setSelectionRow(row);

        JPopupMenu menu = new JPopupMenu();
        JMenuItem newFile = new JMenuItem(I18n.tr("menu.newFile"));
        newFile.addActionListener(e -> createLuciaFile());
        JMenuItem newFolder = new JMenuItem(I18n.tr("menu.newFolder"));
        newFolder.addActionListener(e -> createFolder());
        JMenuItem refresh = new JMenuItem(I18n.tr("menu.refreshProject"));
        refresh.addActionListener(e -> rebuildTree());

        menu.add(newFile);
        menu.add(newFolder);
        menu.add(refresh);
        menu.show(tree, event.getX(), event.getY());
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(tree),
                message, I18n.tr("dialog.error"), JOptionPane.ERROR_MESSAGE);
        onLog.accept(I18n.tr("log.error") + ": " + message);
    }
}
