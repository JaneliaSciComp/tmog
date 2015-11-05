/*
 * Copyright (c) 2015 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.view;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.config.InputFileFilter;
import org.janelia.it.ims.tmog.config.InputFileSorter;
import org.janelia.it.ims.tmog.config.ProjectConfiguration;
import org.janelia.it.ims.tmog.config.preferences.PathDefault;
import org.janelia.it.ims.tmog.config.preferences.TransmogrifierPreferences;
import org.janelia.it.ims.tmog.config.preferences.ViewDefault;
import org.janelia.it.ims.tmog.filefilter.FileNamePatternFilter;
import org.janelia.it.ims.tmog.target.FileTarget;
import org.janelia.it.ims.tmog.target.FileTargetWorker;
import org.janelia.it.ims.tmog.view.component.NarrowOptionPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileFilter;
import java.util.List;

/**
 * This class manages File based input selection components.
 *
 * @author Eric Trautman
 */
public class FileInputSelectionHandler implements InputSelectionHandler {

    private JTextArea directoryField;
    private JButton setDirectoryButton;
    private JButton cancelButton;
    private int selectionMode;
    private String selectButtonText;
    private InputSelectionView view;

    private String projectName;
    private InputFileFilter inputFilter;
    private InputFileSorter inputSorter;
    private File defaultDirectory;
    private FileTargetWorker fileTargetWorker;

    /**
     * Constructs a new handler.
     *
     * @param  projectConfig       configuration for the current project.
     * @param  globalDefaultDirectory  current global default directory.
     * @param  directoryLabel      label for the directory field.
     * @param  directoryField      text area for displaying the selected
     *                             directory.
     * @param  setDirectoryButton  button that initiates input selection.
     * @param  cancelButton        button for cancelling input searches.
     * @param  selectionMode       {@link JFileChooser} selection mode that
     *                             limits what types of files/directories can
     *                             be selected for input.
     * @param  selectButtonText    the text to display for the accept button.
     *                             of the input selection dialog.
     * @param  view                the view using this handler (for callbacks).
     */
    public FileInputSelectionHandler(ProjectConfiguration projectConfig,
                                     File globalDefaultDirectory,
                                     JLabel directoryLabel,
                                     JTextArea directoryField,
                                     JButton setDirectoryButton,
                                     JButton cancelButton,
                                     int selectionMode,
                                     String selectButtonText,
                                     InputSelectionView view) {
        this.projectName = projectConfig.getName();
        this.inputFilter = projectConfig.getInputFileFilter();

        // override normal labels and modes for target data files
        if (inputFilter.hasTargetDataFile()) {
            directoryLabel.setText("Source Data File:");
            this.selectionMode = JFileChooser.FILES_ONLY;
            this.selectButtonText = "Select Data File";            
        } else {
            this.selectionMode = selectionMode;
            this.selectButtonText = selectButtonText;
        }

        this.inputSorter = projectConfig.getInputFileSorter();
        setDefaultDirectory(globalDefaultDirectory);
        this.directoryField = directoryField;
        this.setDirectoryButton = setDirectoryButton;
        this.cancelButton = cancelButton;
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

    public static void setPreferredSize(Component forComponent,
                                        Component relativeTo,
                                        double factor) {
        final Dimension relativeSize = relativeTo.getSize();
        final double height = relativeSize.height * factor;
        final double width = relativeSize.width * factor;
        forComponent.setPreferredSize(
                new Dimension((int)width, (int)height));
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

                // when choosing a target data file,
                // apply file filter to chooser (otherwise leave alone)
                if (inputFilter.hasTargetDataFile()) {
                    FileFilter fileFilter = inputFilter.getFilter(null);
                    if (fileFilter instanceof FileNamePatternFilter) {
                        FileNamePatternFilter patternFilter =
                                (FileNamePatternFilter) fileFilter;
                        patternFilter.setIncludeDirectories(true);
                        fileChooser.setFileFilter(patternFilter);
                    }
                }

                final JPanel appPanel = view.getPanel();
                setPreferredSize(fileChooser, appPanel, 0.9);

                int choice = fileChooser.showDialog(appPanel,
                                                    selectButtonText);

                if (choice == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    if ((selectedFile != null) &&
                        (! selectedFile.isDirectory()) &&
                        (! inputFilter.hasTargetDataFile())) {
                        selectedFile = selectedFile.getParentFile();
                    }

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
        updateProjectPreferences(defaultDirectory);

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
                                     inputSorter.getComparator(),
                                     inputFilter.getTargetNamer(defaultDirectory),
                                     inputFilter.isFilterDuplicates(),
                                     inputFilter.getTargetDataFile());

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

            if (fileTargetWorker.hasSummary()) {
                NarrowOptionPane.displaySummaryDialog(
                        "File Retrieval Summary",
                        fileTargetWorker.getSummary(),
                        view.getPanel());
            }

            if (targets != null) {
                final int numTargets = targets.size();
                if (numTargets > 0) {
                    directoryField.setText(defaultDirectory.getAbsolutePath());
                    directoryField.setToolTipText(getToolTip(numTargets));                    
                    view.processInputTargets(targets);
                } else {
                    resetInputRoot();
                    File rootDirectory = fileTargetWorker.getRootDirectory();
                    NarrowOptionPane.showMessageDialog(
                            view.getPanel(),
                            "No eligible files were found in: " +
                            rootDirectory.getAbsolutePath(),
                            "No Eligible Files Found",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        }

        fileTargetWorker = null;
    }

    private String getToolTip(int numTargets) {
        StringBuilder toolTip = new StringBuilder(64);
        toolTip.append(numTargets);
        toolTip.append(" file");
        if (numTargets > 1) {
            toolTip.append("s");
        }
        toolTip.append(" found");
        return toolTip.toString();
    }

    private void setDefaultDirectory(File globalDefaultDirectory) {

        defaultDirectory = null;
        ViewDefault viewDefault =
                TransmogrifierPreferences.getProjectViewPreferences(
                        projectName);
        if (viewDefault != null) {
            PathDefault pathDefault = viewDefault.getSourcePathDefault();
            if (pathDefault != null) {
                defaultDirectory = new File(pathDefault.getValue());
            }
        }

        if ((defaultDirectory == null) ||
            (! defaultDirectory.exists()) ||
            (! defaultDirectory.canRead())) {
            defaultDirectory = globalDefaultDirectory;
        } else if (defaultDirectory.isFile() &&
                   (! inputFilter.hasTargetDataFile())) {
            defaultDirectory = globalDefaultDirectory;
        }
    }

    private void updateProjectPreferences(File sourceDefaultDirectory) {
        ViewDefault viewDefault =
                TransmogrifierPreferences.getProjectViewPreferences(
                        projectName);
        if (viewDefault != null) {
            PathDefault pathDefault = viewDefault.getSourcePathDefault();
            if (pathDefault == null) {
                pathDefault = new PathDefault(PathDefault.SOURCE_DIRECTORY);
                viewDefault.addPathDefault(pathDefault);
            }
            pathDefault.setValue(sourceDefaultDirectory.getAbsolutePath());
            TransmogrifierPreferences preferences =
                    TransmogrifierPreferences.getInstance();
            if (preferences.canWrite()) {
                preferences.save();
            }
        }
    }

    /** The logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(FileInputSelectionHandler.class);
}