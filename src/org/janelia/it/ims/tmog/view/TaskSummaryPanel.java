/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

/**
 * Creates a dialog panel that overlays the application window components
 * (drawing in the {@link JLayeredPane#MODAL_LAYER} of the layered pane).
 * Because it is still part of the application window, the panel will be
 * hidden when the application window is minimized/iconified.
 *
 * @author Eric Trautman
 */
public class TaskSummaryPanel {
    private JPanel contentPanel;
    private JScrollPane messageScrollPane;
    private JTextArea messageTextArea;
    private JButton okButton;
    private JLabel titleLabel;

    private JLayeredPane layeredPane;
    private ComponentListener layeredPaneResizeListener;

    /**
     * Creates a panel that is not visible.
     *
     * @param  title    dialog title.
     * @param  message  dialog message.
     * @param  parent   dialog parent component
     *                  (used to identify layered panel for application).
     */
    public TaskSummaryPanel(String title,
                            String message,
                            Component parent) {

        titleLabel.setText(title);
        messageScrollPane.setWheelScrollingEnabled(true);

        messageTextArea.setText(message);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        layeredPane = JLayeredPane.getLayeredPaneAbove(parent);
        layeredPaneResizeListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                contentPanel.setSize(layeredPane.getSize());
            }
        };

        contentPanel.setVisible(false);
    }

    /**
     * Displays this panel.
     */
    public void display() {
        contentPanel.setSize(layeredPane.getSize());
        layeredPane.add(contentPanel, JLayeredPane.MODAL_LAYER);
        layeredPane.addComponentListener(layeredPaneResizeListener);
        contentPanel.setVisible(true);
    }

    /**
     * Hides this panel and cleans up affected resources.
     */
    public void dispose() {
        contentPanel.setVisible(false);
        layeredPane.remove(contentPanel);
        layeredPane.removeComponentListener(layeredPaneResizeListener);
    }

}
