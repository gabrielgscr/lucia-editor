/*
 * Copyright (c) 2026 Gabriel González
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.lucia.editor.ui;

import com.lucia.editor.i18n.I18n;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * Manages the project file tree and related file-creation operations.
 * Not a UI component itself — exposes the underlying JTree via {@link #getTree()}.
 */
public class ProjectTreePanel {

    private record PendingTreeTransfer(Path source, boolean cut) {}

    public interface ProjectTreeActions {
        void openFile(Path path);
        void runFile(Path path);
        void compileFile(Path path);
        void onPathChanged(Path oldPath, Path newPath);
        void onPathDeleted(Path path);
        void onLog(String message);
    }

    private final JTree tree;
    private final FileTreeCellRenderer treeRenderer;
    private Path projectRoot;
    private final ProjectTreeActions actions;
    private PendingTreeTransfer pendingTransfer;

    /**
     * @param onFileOpen callback invoked when the user selects a .lucia file or creates one
     * @param onLog      callback for log/status messages
     */
    public ProjectTreePanel(ProjectTreeActions actions) {
        this.actions = actions;

        tree = new JTree(new DefaultMutableTreeNode(I18n.tr("project.none")));
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { openOnDoubleClick(e); }
            @Override public void mousePressed(MouseEvent e)  { showContextMenu(e); }
            @Override public void mouseReleased(MouseEvent e) { showContextMenu(e); }
        });
        ToolTipManager.sharedInstance().registerComponent(tree);
        this.treeRenderer = new FileTreeCellRenderer();
        tree.setCellRenderer(treeRenderer);
        tree.setRowHeight(22);
        tree.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        tree.setDragEnabled(true);
        tree.setDropMode(DropMode.ON);
        tree.setTransferHandler(new ProjectTreeTransferHandler());

        tree.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "openSelectedFile");
        tree.getActionMap().put("openSelectedFile", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { openSelectedFile(); }
        });
        tree.getInputMap().put(KeyStroke.getKeyStroke("F2"), "renameSelectedPath");
        tree.getActionMap().put("renameSelectedPath", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { renameSelected(); }
        });
        tree.getInputMap().put(KeyStroke.getKeyStroke("DELETE"), "deleteSelectedPath");
        tree.getActionMap().put("deleteSelectedPath", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { deleteSelected(); }
        });
        tree.getInputMap().put(KeyStroke.getKeyStroke("control C"), "copySelectedPath");
        tree.getActionMap().put("copySelectedPath", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { copySelectedForPaste(); }
        });
        tree.getInputMap().put(KeyStroke.getKeyStroke("control X"), "cutSelectedPath");
        tree.getActionMap().put("cutSelectedPath", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { cutSelectedForPaste(); }
        });
        tree.getInputMap().put(KeyStroke.getKeyStroke("control V"), "pasteSelectedPath");
        tree.getActionMap().put("pasteSelectedPath", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { pasteIntoSelectedDirectory(); }
        });
        tree.getInputMap().put(KeyStroke.getKeyStroke("control D"), "duplicateSelectedPath");
        tree.getActionMap().put("duplicateSelectedPath", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { duplicateSelected(); }
        });
        tree.getInputMap().put(KeyStroke.getKeyStroke("F5"), "refreshProjectTree");
        tree.getActionMap().put("refreshProjectTree", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { rebuildTree(); }
        });
        tree.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "clearClipboardTransfer");
        tree.getActionMap().put("clearClipboardTransfer", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { clearPendingTransfer(); }
        });
    }

    public JTree getTree() {
        return tree;
    }

    /** Sets the project root and immediately rebuilds the tree. */
    public void setProjectRoot(Path projectRoot) {
        this.projectRoot = projectRoot;
        rebuildTree(projectRoot);
    }

    /** Rebuilds the tree from the current project root. */
    public void rebuildTree() {
        rebuildTree(getSelectedPath());
    }

    public Path getSelectedPath() {
        Object selected = tree.getLastSelectedPathComponent();
        if (selected instanceof DefaultMutableTreeNode node
                && node.getUserObject() instanceof Path path) {
            return path;
        }
        return null;
    }

    public void renameSelected() {
        Path selected = getSelectedPath();
        if (selected == null || projectRoot == null) {
            showError(I18n.tr("error.noProject"));
            return;
        }
        if (selected.equals(projectRoot)) {
            showError(I18n.tr("error.cannotRenameProjectRoot"));
            return;
        }

        String currentName = selected.getFileName() == null ? selected.toString() : selected.getFileName().toString();
        String name = JOptionPane.showInputDialog(
                SwingUtilities.getWindowAncestor(tree),
                I18n.tr("prompt.renamePath"), currentName);
        if (name == null || name.trim().isEmpty()) {
            return;
        }

        String normalizedName = normalizeNameForRename(selected, name.trim());
        Path target = selected.getParent() == null ? null : selected.getParent().resolve(normalizedName);
        if (target == null || target.equals(selected)) {
            return;
        }
        if (Files.exists(target)) {
            showError(I18n.tr("error.alreadyExists") + ": " + target);
            return;
        }

        try {
            Files.move(selected, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicMoveError) {
            try {
                Files.move(selected, target);
            } catch (IOException ex) {
                showError(I18n.tr("error.renamePath") + ": " + ex.getMessage());
                return;
            }
        }

        actions.onPathChanged(selected, target);
        rebuildTree(target);
        actions.onLog(I18n.tr("log.pathRenamed") + ": " + selected + " -> " + target);
    }

    public void deleteSelected() {
        Path selected = getSelectedPath();
        if (selected == null || projectRoot == null) {
            showError(I18n.tr("error.noProject"));
            return;
        }
        if (selected.equals(projectRoot)) {
            showError(I18n.tr("error.cannotDeleteProjectRoot"));
            return;
        }

        int decision = JOptionPane.showConfirmDialog(
                SwingUtilities.getWindowAncestor(tree),
                I18n.tr(Files.isDirectory(selected) ? "confirm.deleteFolder" : "confirm.deleteFile")
                        + " " + selected.getFileName() + "?",
                I18n.tr("confirm.title"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (decision != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            deleteRecursively(selected);
            actions.onPathDeleted(selected);
            rebuildTree(selected.getParent());
            actions.onLog(I18n.tr("log.pathDeleted") + ": " + selected);
        } catch (IOException ex) {
            showError(I18n.tr("error.deletePath") + ": " + ex.getMessage());
        }
    }

    public void moveSelectedToDirectory() {
        Path selected = getSelectedPath();
        if (selected == null || projectRoot == null) {
            showError(I18n.tr("error.noProject"));
            return;
        }
        if (selected.equals(projectRoot)) {
            showError(I18n.tr("error.cannotMoveProjectRoot"));
            return;
        }

        Path targetDirectory = chooseTargetDirectory(selected);
        if (targetDirectory == null) {
            return;
        }
        movePath(selected, targetDirectory);
    }

    public void runSelectedFile() {
        Path selected = getSelectedPath();
        if (!isLuciaFile(selected)) {
            showError(I18n.tr("error.onlyLuciaFile"));
            return;
        }
        actions.runFile(selected);
    }

    public void compileSelectedFile() {
        Path selected = getSelectedPath();
        if (!isLuciaFile(selected)) {
            showError(I18n.tr("error.onlyLuciaFile"));
            return;
        }
        actions.compileFile(selected);
    }

    public void duplicateSelected() {
        Path selected = getSelectedPath();
        if (selected == null || projectRoot == null) {
            showError(I18n.tr("error.noProject"));
            return;
        }
        if (selected.equals(projectRoot)) {
            showError(I18n.tr("error.cannotDuplicateProjectRoot"));
            return;
        }

        Path duplicate = buildUniqueCopyPath(selected, selected.getParent());
        try {
            copyPath(selected, duplicate);
            rebuildTree(duplicate);
            if (Files.isRegularFile(duplicate) && duplicate.toString().endsWith(".lucia")) {
                actions.openFile(duplicate);
            }
            actions.onLog(I18n.tr("log.pathDuplicated") + ": " + duplicate);
        } catch (IOException ex) {
            showError(I18n.tr("error.duplicatePath") + ": " + ex.getMessage());
        }
    }

    public void copySelectedForPaste() {
        Path selected = getSelectedPath();
        if (selected == null || projectRoot == null) {
            showError(I18n.tr("error.noProject"));
            return;
        }
        pendingTransfer = new PendingTreeTransfer(selected.toAbsolutePath().normalize(), false);
        refreshPendingTransferVisualState();
        actions.onLog(I18n.tr("log.copyPrepared") + ": " + selected);
    }

    public void cutSelectedForPaste() {
        Path selected = getSelectedPath();
        if (selected == null || projectRoot == null) {
            showError(I18n.tr("error.noProject"));
            return;
        }
        if (selected.equals(projectRoot)) {
            showError(I18n.tr("error.cannotCutProjectRoot"));
            return;
        }
        pendingTransfer = new PendingTreeTransfer(selected.toAbsolutePath().normalize(), true);
        refreshPendingTransferVisualState();
        actions.onLog(I18n.tr("log.cutPrepared") + ": " + selected);
    }

    public void pasteIntoSelectedDirectory() {
        if (pendingTransfer == null) {
            showError(I18n.tr("error.nothingToPaste"));
            return;
        }
        Path source = pendingTransfer.source();
        if (!Files.exists(source)) {
            pendingTransfer = null;
            refreshPendingTransferVisualState();
            showError(I18n.tr("error.clipboardSourceMissing"));
            return;
        }

        Path targetDirectory = getSelectedDirectoryForCreation();
        if (targetDirectory == null || !Files.isDirectory(targetDirectory)) {
            showError(I18n.tr("error.invalidMoveTarget"));
            return;
        }

        if (pendingTransfer.cut()) {
            movePath(source, targetDirectory);
            clearPendingTransfer();
            return;
        }

        Path duplicate = buildUniqueCopyPath(source, targetDirectory);
        try {
            copyPath(source, duplicate);
            rebuildTree(duplicate);
            if (Files.isRegularFile(duplicate) && duplicate.toString().endsWith(".lucia")) {
                actions.openFile(duplicate);
            }
            actions.onLog(I18n.tr("log.pathPasted") + ": " + duplicate);
        } catch (IOException ex) {
            showError(I18n.tr("error.pastePath") + ": " + ex.getMessage());
        }
    }

    public void copySelectedRelativePath() {
        Path selected = getSelectedPath();
        if (selected == null || projectRoot == null) {
            showError(I18n.tr("error.noProject"));
            return;
        }
        copyToClipboard(toProjectRelativeLabel(selected), I18n.tr("log.relativePathCopied"));
    }

    public void copySelectedAbsolutePath() {
        Path selected = getSelectedPath();
        if (selected == null) {
            showError(I18n.tr("error.noProject"));
            return;
        }
        copyToClipboard(selected.toAbsolutePath().normalize().toString(), I18n.tr("log.absolutePathCopied"));
    }

    public void revealSelectedInSystem() {
        Path selected = getSelectedPath();
        if (selected == null) {
            showError(I18n.tr("error.noProject"));
            return;
        }
        if (!Desktop.isDesktopSupported()) {
            showError(I18n.tr("error.revealInSystemUnsupported"));
            return;
        }
        try {
            Desktop desktop = Desktop.getDesktop();
            Path target = Files.isDirectory(selected) ? selected : selected.getParent();
            if (target == null) {
                showError(I18n.tr("error.revealInSystem") + ": " + selected);
                return;
            }
            desktop.open(target.toFile());
            actions.onLog(I18n.tr("log.revealedInSystem") + ": " + target);
        } catch (IOException ex) {
            showError(I18n.tr("error.revealInSystem") + ": " + ex.getMessage());
        }
    }

    private void rebuildTree(Path preferredSelection) {
        if (projectRoot == null) return;
        Set<Path> expandedPaths = collectExpandedPaths();
        tree.setModel(new DefaultTreeModel(buildNode(projectRoot, true)));
        restoreExpandedPaths(expandedPaths);
        Path selection = preferredSelection != null ? preferredSelection : projectRoot;
        TreePath treePath = findTreePath(selection);
        if (treePath != null) {
            tree.setSelectionPath(treePath);
            tree.scrollPathToVisible(treePath);
        } else {
            tree.expandRow(0);
        }
    }

    /** Returns the directory that should receive new files/folders based on tree selection. */
    public Path getSelectedDirectoryForCreation() {
        if (projectRoot == null) return null;
        Path path = getSelectedPath();
        if (path != null) {
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
            rebuildTree(newFile);
            actions.openFile(newFile);
            actions.onLog(I18n.tr("log.fileCreated") + ": " + newFile);
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
            rebuildTree(newFolder);
            actions.onLog(I18n.tr("log.folderCreated") + ": " + newFolder);
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
                actions.onLog(I18n.tr("log.error") + ": " + ex.getMessage());
            }
        }
        return node;
    }

    private void openOnDoubleClick(MouseEvent event) {
        if (event.getClickCount() != 2 || SwingUtilities.isRightMouseButton(event)) {
            return;
        }
        int row = tree.getRowForLocation(event.getX(), event.getY());
        if (row >= 0) {
            tree.setSelectionRow(row);
        }
        openSelectedFile();
    }

    private void showContextMenu(MouseEvent event) {
        if (!event.isPopupTrigger()) return;
        int row = tree.getRowForLocation(event.getX(), event.getY());
        if (row < 0) {
            return;
        }
        tree.setSelectionRow(row);

        Path selected = getSelectedPath();
        if (selected == null) {
            return;
        }

        JPopupMenu menu = new JPopupMenu();

        if (isLuciaFile(selected)) {
            menu.add(menuItem("menu.openFile", e -> openSelectedFile()));
            menu.add(menuItem("menu.runFile", e -> runSelectedFile()));
            menu.add(menuItem("menu.compileFile", e -> compileSelectedFile()));
            menu.addSeparator();
            menu.add(menuItem("menu.duplicate", e -> duplicateSelected()));
            menu.add(menuItem("menu.copy", e -> copySelectedForPaste()));
            menu.add(menuItem("menu.cut", e -> cutSelectedForPaste()));
            menu.add(pasteMenuItem());
            menu.add(menuItem("menu.clearClipboard", e -> clearPendingTransfer()));
            menu.addSeparator();
            menu.add(menuItem("menu.rename", e -> renameSelected()));
            menu.add(menuItem("menu.moveTo", e -> moveSelectedToDirectory()));
            menu.add(menuItem("menu.delete", e -> deleteSelected()));
            menu.addSeparator();
            menu.add(menuItem("menu.copyRelativePath", e -> copySelectedRelativePath()));
            menu.add(menuItem("menu.copyAbsolutePath", e -> copySelectedAbsolutePath()));
            menu.add(menuItem("menu.revealInSystem", e -> revealSelectedInSystem()));
            menu.addSeparator();
            menu.add(menuItem("menu.refreshProject", e -> rebuildTree()));
        } else {
            menu.add(menuItem("menu.newFile", e -> createLuciaFile()));
            menu.add(menuItem("menu.newFolder", e -> createFolder()));
            menu.add(pasteMenuItem());
            menu.add(menuItem("menu.clearClipboard", e -> clearPendingTransfer()));
            if (!selected.equals(projectRoot)) {
                menu.addSeparator();
                menu.add(menuItem("menu.duplicate", e -> duplicateSelected()));
                menu.add(menuItem("menu.copy", e -> copySelectedForPaste()));
                menu.add(menuItem("menu.cut", e -> cutSelectedForPaste()));
                menu.add(menuItem("menu.rename", e -> renameSelected()));
                menu.add(menuItem("menu.moveTo", e -> moveSelectedToDirectory()));
                menu.add(menuItem("menu.delete", e -> deleteSelected()));
            }
            menu.addSeparator();
            menu.add(menuItem("menu.copyRelativePath", e -> copySelectedRelativePath()));
            menu.add(menuItem("menu.copyAbsolutePath", e -> copySelectedAbsolutePath()));
            menu.add(menuItem("menu.revealInSystem", e -> revealSelectedInSystem()));
            menu.addSeparator();
            menu.add(menuItem("menu.refreshProject", e -> rebuildTree()));
        }
        menu.show(tree, event.getX(), event.getY());
    }

    private JMenuItem menuItem(String key, Consumer<java.awt.event.ActionEvent> action) {
        JMenuItem item = new JMenuItem(I18n.tr(key));
        item.addActionListener(action::accept);
        return item;
    }

    private JMenuItem pasteMenuItem() {
        JMenuItem item = new JMenuItem(pendingTransferLabel());
        item.setEnabled(pendingTransfer != null);
        item.addActionListener(e -> pasteIntoSelectedDirectory());
        return item;
    }

    private String pendingTransferLabel() {
        if (pendingTransfer == null || pendingTransfer.source() == null) {
            return I18n.tr("menu.paste");
        }
        String sourceName = pendingTransfer.source().getFileName() == null
                ? pendingTransfer.source().toString()
                : pendingTransfer.source().getFileName().toString();
        return I18n.tr("menu.paste") + " (" + sourceName + ")";
    }

    private void clearPendingTransfer() {
        if (pendingTransfer == null) {
            return;
        }
        pendingTransfer = null;
        refreshPendingTransferVisualState();
        actions.onLog(I18n.tr("log.clipboardCleared"));
    }

    private void refreshPendingTransferVisualState() {
        Path markedPath = pendingTransfer == null ? null : pendingTransfer.source();
        boolean cutMode = pendingTransfer != null && pendingTransfer.cut();
        treeRenderer.setPendingTransfer(markedPath, cutMode);
        tree.repaint();
    }

    private void openSelectedFile() {
        Path selected = getSelectedPath();
        if (isLuciaFile(selected)) {
            actions.openFile(selected);
        }
    }

    private boolean isLuciaFile(Path path) {
        return path != null && Files.isRegularFile(path) && path.toString().endsWith(".lucia");
    }

    private String normalizeNameForRename(Path selected, String candidate) {
        if (isLuciaFile(selected) && !candidate.endsWith(".lucia")) {
            return candidate + ".lucia";
        }
        return candidate;
    }

    private Path buildUniqueCopyPath(Path source, Path targetDirectory) {
        String fileName = source.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        boolean isDirectory = Files.isDirectory(source);
        String baseName = isDirectory || dotIndex < 0 ? fileName : fileName.substring(0, dotIndex);
        String extension = isDirectory || dotIndex < 0 ? "" : fileName.substring(dotIndex);

        Path candidate = targetDirectory.resolve(baseName + "_copy" + extension);
        int counter = 2;
        while (Files.exists(candidate)) {
            candidate = targetDirectory.resolve(baseName + "_copy_" + counter + extension);
            counter++;
        }
        return candidate;
    }

    private void copyPath(Path source, Path target) throws IOException {
        if (Files.isDirectory(source)) {
            try (var walk = Files.walk(source)) {
                for (Path current : walk.toList()) {
                    Path relative = source.relativize(current);
                    Path destination = target.resolve(relative);
                    if (Files.isDirectory(current)) {
                        Files.createDirectories(destination);
                    } else {
                        Files.createDirectories(destination.getParent());
                        Files.copy(current, destination);
                    }
                }
            }
            return;
        }
        Files.copy(source, target);
    }

    private void copyToClipboard(String text, String successMessage) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            actions.onLog(successMessage + ": " + text);
        } catch (IllegalStateException ex) {
            showError(I18n.tr("error.copyPath") + ": " + ex.getMessage());
        }
    }

    private Path chooseTargetDirectory(Path source) {
        List<Path> directories = collectProjectDirectories();
        directories.removeIf(dir -> dir.equals(source));
        directories.removeIf(dir -> dir.startsWith(source));
        if (directories.isEmpty()) {
            return null;
        }

        Path currentParent = Files.isDirectory(source) ? source.getParent() : source.getParent();
        Object[] options = directories.stream()
                .map(this::toProjectRelativeLabel)
                .toArray(String[]::new);
        String defaultValue = currentParent == null ? toProjectRelativeLabel(projectRoot) : toProjectRelativeLabel(currentParent);

        Object selected = JOptionPane.showInputDialog(
                SwingUtilities.getWindowAncestor(tree),
                I18n.tr("prompt.moveTarget"),
                I18n.tr("dialog.moveTitle"),
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                defaultValue);
        if (!(selected instanceof String label)) {
            return null;
        }
        return directories.stream()
                .filter(dir -> Objects.equals(toProjectRelativeLabel(dir), label))
                .findFirst()
                .orElse(null);
    }

    private void movePath(Path source, Path targetDirectory) {
        if (source == null || targetDirectory == null) {
            return;
        }
        if (!Files.isDirectory(targetDirectory)) {
            showError(I18n.tr("error.invalidMoveTarget"));
            return;
        }
        if (targetDirectory.startsWith(source)) {
            showError(I18n.tr("error.moveIntoSelf"));
            return;
        }

        Path target = targetDirectory.resolve(source.getFileName());
        if (target.equals(source)) {
            return;
        }
        if (Files.exists(target)) {
            showError(I18n.tr("error.alreadyExists") + ": " + target);
            return;
        }

        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicMoveError) {
            try {
                Files.move(source, target);
            } catch (IOException ex) {
                showError(I18n.tr("error.movePath") + ": " + ex.getMessage());
                return;
            }
        }

        actions.onPathChanged(source, target);
        rebuildTree(target);
        actions.onLog(I18n.tr("log.pathMoved") + ": " + source + " -> " + target);
    }

    private void deleteRecursively(Path target) throws IOException {
        try (var walk = Files.walk(target)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private List<Path> collectProjectDirectories() {
        List<Path> directories = new ArrayList<>();
        if (projectRoot == null) {
            return directories;
        }
        try (var walk = Files.walk(projectRoot)) {
            walk.filter(Files::isDirectory)
                    .sorted()
                    .forEach(directories::add);
        } catch (IOException ex) {
            showError(I18n.tr("error.listDirectories") + ": " + ex.getMessage());
        }
        return directories;
    }

    private String toProjectRelativeLabel(Path path) {
        if (path == null || projectRoot == null) {
            return "";
        }
        if (path.equals(projectRoot)) {
            return ".";
        }
        return projectRoot.relativize(path).toString();
    }

    private Set<Path> collectExpandedPaths() {
        Set<Path> expanded = new TreeSet<>();
        for (int i = 0; i < tree.getRowCount(); i++) {
            if (!tree.isExpanded(i)) {
                continue;
            }
            TreePath path = tree.getPathForRow(i);
            if (path == null) {
                continue;
            }
            Object last = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
            if (last instanceof Path nodePath) {
                expanded.add(nodePath);
            }
        }
        return expanded;
    }

    private void restoreExpandedPaths(Set<Path> expandedPaths) {
        if (expandedPaths == null || expandedPaths.isEmpty()) {
            tree.expandRow(0);
            return;
        }
        for (Path path : expandedPaths) {
            TreePath treePath = findTreePath(path);
            if (treePath != null) {
                tree.expandPath(treePath);
            }
        }
        tree.expandRow(0);
    }

    private TreePath findTreePath(Path target) {
        Object root = tree.getModel().getRoot();
        if (!(root instanceof DefaultMutableTreeNode rootNode) || target == null) {
            return null;
        }
        return findTreePath(rootNode, target.toAbsolutePath().normalize());
    }

    private TreePath findTreePath(DefaultMutableTreeNode node, Path target) {
        Object userObject = node.getUserObject();
        if (userObject instanceof Path path && path.toAbsolutePath().normalize().equals(target)) {
            return new TreePath(node.getPath());
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            TreePath result = findTreePath((DefaultMutableTreeNode) node.getChildAt(i), target);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(tree),
                message, I18n.tr("dialog.error"), JOptionPane.ERROR_MESSAGE);
        actions.onLog(I18n.tr("log.error") + ": " + message);
    }

    private final class ProjectTreeTransferHandler extends TransferHandler {

        @Override
        protected Transferable createTransferable(JComponent c) {
            Path selected = getSelectedPath();
            return selected == null ? null : new StringSelection(selected.toAbsolutePath().normalize().toString());
        }

        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            if (!support.isDrop() || !support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return false;
            }
            JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
            TreePath dropPath = dropLocation.getPath();
            if (dropPath == null) {
                return false;
            }
            Object last = ((DefaultMutableTreeNode) dropPath.getLastPathComponent()).getUserObject();
            if (!(last instanceof Path targetPath)) {
                return false;
            }
            Path targetDirectory = Files.isDirectory(targetPath) ? targetPath : targetPath.getParent();
            if (targetDirectory == null) {
                return false;
            }
            try {
                Path source = Path.of((String) support.getTransferable().getTransferData(DataFlavor.stringFlavor));
                return !source.equals(projectRoot)
                        && !targetDirectory.startsWith(source)
                        && !targetDirectory.equals(source.getParent());
            } catch (Exception ex) {
                return false;
            }
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            try {
                Path source = Path.of((String) support.getTransferable().getTransferData(DataFlavor.stringFlavor));
                JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
                Object last = ((DefaultMutableTreeNode) dropLocation.getPath().getLastPathComponent()).getUserObject();
                if (!(last instanceof Path targetPath)) {
                    return false;
                }
                Path targetDirectory = Files.isDirectory(targetPath) ? targetPath : targetPath.getParent();
                movePath(source, targetDirectory);
                return true;
            } catch (Exception ex) {
                showError(I18n.tr("error.movePath") + ": " + ex.getMessage());
                return false;
            }
        }
    }
}
