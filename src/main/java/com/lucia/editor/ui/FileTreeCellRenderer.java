package com.lucia.editor.ui;

import java.awt.Component;
import java.awt.Font;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * Renders tree nodes with Font Awesome icons distinguishing folders from .lucia files.
 */
class FileTreeCellRenderer extends DefaultTreeCellRenderer {

    private Path pendingTransferPath;
    private boolean pendingTransferCut;

    void setPendingTransfer(Path path, boolean cutMode) {
        this.pendingTransferPath = path == null ? null : path.toAbsolutePath().normalize();
        this.pendingTransferCut = cutMode;
    }

    @Override
    public Component getTreeCellRendererComponent(
            JTree tree, Object value, boolean selected,
            boolean expanded, boolean leaf, int row, boolean hasFocus) {

        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

        if (value instanceof DefaultMutableTreeNode node
                && node.getUserObject() instanceof Path path) {
            Path normalized = path.toAbsolutePath().normalize();
            // Always display only the last segment; full path lives in userObject.
            String name = path.getFileName() != null
                ? path.getFileName().toString()
                : normalized.toString();

            if (pendingTransferPath != null && pendingTransferPath.equals(normalized)) {
            name = pendingTransferCut ? name + " [CUT]" : name + " [COPY]";
            setFont(getFont().deriveFont(pendingTransferCut ? Font.ITALIC : Font.BOLD));
            }
            setText(name);

            if (Files.isDirectory(path)) {
                setIcon(FontIcon.of(
                        expanded ? FontAwesomeSolid.FOLDER_OPEN : FontAwesomeSolid.FOLDER,
                        14));
            } else {
                setIcon(FontIcon.of(FontAwesomeSolid.FILE_CODE, 14));
            }
        } else if (value instanceof DefaultMutableTreeNode node
                && node.getUserObject() instanceof String) {
            setIcon(FontIcon.of(FontAwesomeSolid.FOLDER, 14));
        }

        return this;
    }
}
