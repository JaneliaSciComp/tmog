/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.view.component;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Simple JPanel for displaying background task progress in a window or dialog.
 *
 * @author Eric Trautman
 */
public class ProgressPanel
        extends JPanel implements PropertyChangeListener {
    private JProgressBar progressBar;
    private JTextArea taskOutput;

    public ProgressPanel() {
        super(new BorderLayout());

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(860, 30));

        taskOutput = new JTextArea(5, 20);
        taskOutput.setMargin(new Insets(5, 5, 5, 5));
        taskOutput.setEditable(false);

        JPanel panel = new JPanel();
        panel.add(progressBar);

        add(panel, BorderLayout.PAGE_START);
        add(new JScrollPane(taskOutput), BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        setPreferredSize(new Dimension(900, 400));
    }

    public void updateProgress(int progress) {
        progressBar.setValue(progress);
    }

    public void addTaskOutput(String message) {
        taskOutput.append(message + "\n");
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName())) {
            updateProgress((Integer) evt.getNewValue());
        }
    }
}
