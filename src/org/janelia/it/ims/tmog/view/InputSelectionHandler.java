/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.view;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.config.InputFileFilter;
import org.janelia.it.ims.tmog.config.InputFileSorter;
import org.janelia.it.ims.tmog.config.ProjectConfiguration;
import org.janelia.it.ims.tmog.target.FileTarget;
import org.janelia.it.ims.tmog.target.FileTargetWorker;
import org.janelia.it.ims.tmog.view.component.NarrowOptionPane;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileFilter;
import java.util.Comparator;
import java.util.List;

/**
 * This class manages input selection components shared by CollectorView
 * and RenameView instances.
 *
 * @author Eric Trautman
 */
public class InputSelectionHandler {

    private JLabel directoryField;
    private JButton setDirectoryButton;
    private JButton cancelButton;
    private int selectionMode;
    private String selectButtonText;
    private InputSelectionView view;

    private InputFileFilter inputFilter;
    private Comparator<FileTarget> sortComparator;
    private File defaultDirectory;
    private FileTargetWorker fileTargetWorker;

    /**
     * Constructs a new handler.
     *
     * @param  projectConfig       configuration for the current project.
     * @param  defaultDirectory    current default directory.
     * @param  directoryField      label for displaying the selected directory.
     * @param  setDirectoryButton  button that initiates input selection.
     * @param  cancelButton        button for cancelling input searches.
     * @param  selectionMode       {@link JFileChooser} selection mode that
     *                             limits what types of files/directories can
     *                             be selected for input.
     * @param  selectButtonText    the text to display for the accept button.
     *                             of the input selection dialog.
     * @param  view                the view using this handler (for callbacks).
     */
    public InputSelectionHandler(ProjectConfiguration projectConfig,
                                 File defaultDirectory,
                                 JLabel directoryField,
                                 JButton setDirectoryButton,
                                 JButton cancelButton,
                                 int selectionMode,
                                 String selectButtonText,
                                 InputSelectionView view) {
        this.inputFilter = projectConfig.getInputFileFilter();
        InputFileSorter sorter = projectConfig.getInputFileSorter();
        this.sortComparator = sorter.getComparator();
        this.defaultDirectory = defaultDirectory;
        this.directoryField = directoryField;
        this.setDirectoryButton = setDirectoryButton;
        this.cancelButton = cancelButton;
        this.selectionMode = selectionMode;
        this.selectButtonText = selectButtonText;
        this.view = view;
        setupInputDirectory();
    }

    /**
     * @return the default directory managed by this handler.
     *         This directory is changed each time a user makes a new selection.
     */
    public File getDefaultDirectory() {
        return defaultDirectory;
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

    private void setupInputDirectory() {
        setDirectoryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                JFileChooser fileChooser = new JFileChooser();
                if (defaultDirectory != null) {
                    File parentDirectory = defaultDirectory.getParentFile();
                    if (parentDirectory != null) {
                        fileChooser.setCurrentDirectory(parentDirectory);
                    } else {
                        fileChooser.setCurrentDirectory(defaultDirectory);
                    }
                }
                fileChooser.setFileSelectionMode(selectionMode);
                int choice = fileChooser.showDialog(view.getPanel(),
                                                    selectButtonText);

                if (choice == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    if (selectedFile != null) {
                        handleDirectorySelection(selectedFile);
                    }
                }
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (fileTargetWorker != null) {
                    fileTargetWorker.cancel(true);
                }
            }
        });
    }

    private void handleDirectorySelection(File selectedFile) {
        defaultDirectory = selectedFile;

        FileFilter fileFilter;
        try {
            fileFilter = inputFilter.getFilter(defaultDirectory);
        } catch (IllegalArgumentException e) {
            LOG.error(e);
            NarrowOptionPane.showMessageDialog(
                    view.getPanel(),
                    e.getMessage(),
                    "File Filter Failure",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        fileTargetWorker =
                new FileTargetWorker(selectedFile,
                                     fileFilter,
                                     inputFilter.isRecursiveSearch(),
                                     sortComparator,
                                     inputFilter.getTargetNamer(defaultDirectory));

        fileTargetWorker.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (fileTargetWorker != null) {
                    if (fileTargetWorker.isProgressEvent(evt)) {
                        handleFileTargetWorkerUpdate(evt);
                    } else if (fileTargetWorker.isDoneEvent(evt)) {
                        handleFileTargetWorkerCompletion();
                    }
                }
            }
        });

        setDirectoryButton.setVisible(false);
        cancelButton.setVisible(true);
        view.handleInputRootSelection(selectedFile);

        fileTargetWorker.submitTask();
    }

    private void handleFileTargetWorkerUpdate(PropertyChangeEvent evt) {
        Object value = evt.getNewValue();
        if (value instanceof List) {
            List list = (List) value;
            int size = list.size();
            if (size > 0) {
                Object lastItem = list.get(size - 1);
                if (lastItem instanceof String) {
                    directoryField.setText(
                            (String) lastItem);
                }
            }
        }
    }

    private void handleFileTargetWorkerCompletion() {

        if (fileTargetWorker.isCancelled()) {

            resetInputRoot();

        } else if (fileTargetWorker.hasFailed()) {

            @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
            final Throwable failureCause =
                    fileTargetWorker.getFailureCause();
            resetInputRoot();
            NarrowOptionPane.showMessageDialog(
                    view.getPanel(),
                    "The following error occurred when " +
                    "attempting to locate files:\n" +
                    failureCause.getMessage(),
                    "File Location Failure",
                    JOptionPane.ERROR_MESSAGE);

        } else {

            List<FileTarget> targets = null;
            try {
                targets = fileTargetWorker.get();
            } catch (Exception e) {
                LOG.error(e);
                NarrowOptionPane.showMessageDialog(
                        view.getPanel(),
                        "The following error occurred when " +
                        "attempting to retrieve files:\n" +
                        e.getMessage(),
                        "File Retrieval Failure",
                        JOptionPane.ERROR_MESSAGE);
            }

            if (targets != null) {
                if (targets.size() > 0) {
                    directoryField.setText(defaultDirectory.getAbsolutePath());
                    view.processInputTargets(targets);
                } else {
                    resetInputRoot();
                    File rootDirectory = fileTargetWorker.getRootDirectory();
                    NarrowOptionPane.showMessageDialog(
                            view.getPanel(),
                            "No eligible files were found " +
                            "in the selected directory: " +
                            rootDirectory.getAbsolutePath(),
                            "No Eligible Files Found",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        }

        fileTargetWorker = null;
    }

    /** The logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(InputSelectionHandler.class);
}