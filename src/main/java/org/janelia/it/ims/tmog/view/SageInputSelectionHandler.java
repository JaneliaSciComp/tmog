/*
 * Copyright (c) 2015 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.view;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * This class manages SAGE database input selection components.
 *
 * @author Eric Trautman
 */
public class SageInputSelectionHandler
        implements InputSelectionHandler {

    private final JTextArea directoryField;
    private final JButton setDirectoryButton;
    private final JButton cancelButton;
    private final InputSelectionView view;
    private final SelectSlideDialog dialog;

    /**
     * Constructs a new handler.
     *
     * @param  directoryLabel      label for the directory field.
     * @param  directoryField      text area for displaying the selected
     *                             directory.
     * @param  setDirectoryButton  button that initiates input selection.
     * @param  cancelButton        button for cancelling input searches.
     * @param  view                the view using this handler (for callbacks).
     */
    public SageInputSelectionHandler(final String family,
                                     final JLabel directoryLabel,
                                     final JTextArea directoryField,
                                     final JButton setDirectoryButton,
                                     final JButton cancelButton,
                                     final InputSelectionView view) {
        directoryLabel.setText("Source:");
        this.directoryField = directoryField;
        this.setDirectoryButton = setDirectoryButton;
        this.cancelButton = cancelButton;
        this.view = view;
        this.dialog = new SelectSlideDialog(family, view, directoryField);
        this.dialog.pack();

        setDirectoryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(true);
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resetInputRoot();
            }
        });

    }

    /**
     * @return the default directory managed by this handler.
     *         This directory is changed each time a user makes a new selection.
     */
    public File getDefaultDirectory() {
        return null;
    }

    /**
     * Enables or disables the setDirectoryButton.
     * Always hides the cancelButton.
     *
     * @param  isEnabled  indicates whether the setDirectoryButton should
     *                    be enabled.
     */
    public void setEnabled(boolean isEnabled) {
        setDirectoryButton.setVisible(true);
        cancelButton.setVisible(false);
        setDirectoryButton.setEnabled(isEnabled);
    }

    /**
     * Resets (blanks out) the input directory label,
     * enables the setDirectoryButton, hides the cancelButton,
     * and notifies the parent view.
     */
    public void resetInputRoot() {
        directoryField.setText("");
        setEnabled(true);
        view.handleInputRootReset();
    }

}