/*
 * Copyright (c) 2026 Gabriel González
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.lucia.editor.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * Tab header component with a close (×) button.
 */
class ClosableTabHeader extends JPanel {

    ClosableTabHeader(JTabbedPane tabs, String title, Runnable onClose) {
        super(new BorderLayout(4, 0));
        setOpaque(false);

        JLabel label = new JLabel(title);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));

        JButton closeButton = new JButton("×");
        closeButton.setFont(closeButton.getFont().deriveFont(11f));
        closeButton.setPreferredSize(new Dimension(18, 18));
        closeButton.setBorder(BorderFactory.createEmptyBorder());
        closeButton.setContentAreaFilled(false);
        closeButton.setFocusable(false);
        closeButton.setToolTipText("Close");

        closeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeButton.setForeground(Color.RED);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeButton.setForeground(null);
            }
        });

        closeButton.addActionListener(e -> {
            int index = tabs.indexOfTabComponent(ClosableTabHeader.this);
            if (index >= 0) {
                onClose.run();
            }
        });

        add(label, BorderLayout.CENTER);
        add(closeButton, BorderLayout.EAST);
        setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
    }
}
