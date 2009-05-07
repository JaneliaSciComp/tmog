/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.view;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.DataTableModel;
import org.janelia.it.ims.tmog.DataTableRow;
import org.janelia.it.ims.tmog.config.InputFileFilter;
import org.janelia.it.ims.tmog.config.ProjectConfiguration;
import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowValidator;
import org.janelia.it.ims.tmog.target.FileTarget;
import org.janelia.it.ims.tmog.target.FileTargetWorker;
import org.janelia.it.ims.tmog.target.Target;
import org.janelia.it.ims.tmog.task.SimpleTask;
import org.janelia.it.ims.tmog.task.Task;
import org.janelia.it.ims.tmog.view.component.DataTable;
import org.janelia.it.ims.tmog.view.component.NarrowOptionPane;
import org.janelia.it.ims.tmog.view.component.SessionIcon;
import org.janelia.it.ims.tmog.view.component.TaskButtonText;
import org.janelia.it.ims.tmog.view.component.TaskComponents;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;

/**
 * This class manages the main or overall view for collecting data for
 * a set of files in a particular directory.
 *
 * @author Eric Trautman
 */
public class CollectorView implements SessionView {

    /** The name of the task supported by this view. */
    public static final String TASK_NAME = "collector";

    private JPanel appPanel;
    @SuppressWarnings({"UnusedDeclaration"})
    private JPanel directoryPanel;
    private JLabel rootDirectoryField;
    private JButton rootDirectoryBtn;
    private JLabel projectLabel;
    private JButton saveBtn;
    private JLabel taskProgressLabel;
    private JProgressBar taskProgressBar;
    private DataTable dataTable;
    private JButton cancelTargetWorkerButton;

    private ProjectConfiguration projectConfig;
    private File defaultDirectory;
    private FileTargetWorker fileTargetWorker;
    private DataTableModel tableModel;
    private SimpleTask task;
    private TaskComponents taskComponents;

    public CollectorView(ProjectConfiguration projectConfig,
                         File defaultDirectory,
                         JTabbedPane parentTabbedPane) {
        this.projectConfig = projectConfig;
        this.defaultDirectory = defaultDirectory;
        this.projectLabel.setText(projectConfig.getName());
        setupInputDirectory();
        setupTaskComponents(parentTabbedPane);
    }

    /**
     * @return the primary content panel (container) for the view.
     */
    public JPanel getPanel() {
        return appPanel;
    }

    /**
     * @return the default directory for the view
     *         (used to default file chooser dialogs).
     */
    public File getDefaultDirectory() {
        return defaultDirectory;
    }

    /**
     * @return true if the session's task is in progress; otherwise false.
     */
    public boolean isTaskInProgress() {
        return taskComponents.isTaskInProgress();
    }

    /**
     * @return the session's processing icon.
     */
    public SessionIcon getSessionIcon() {
        return taskComponents.getSessionIcon();
    }


    private void setupInputDirectory() {
        rootDirectoryBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                JPanel parentPanel = getPanel();
                JFileChooser fileChooser = new JFileChooser();
                if (defaultDirectory != null) {
                    File parentDirectory = defaultDirectory.getParentFile();
                    if (parentDirectory != null) {
                        fileChooser.setCurrentDirectory(parentDirectory);
                    } else {
                        fileChooser.setCurrentDirectory(defaultDirectory);
                    }
                }
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int choice = fileChooser.showDialog(parentPanel,
                                                    "Select Root Directory");

                if (choice == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    if (selectedFile != null) {
                        handleRootDirectorySelection(selectedFile);
                    }
                }
            }
        });

        cancelTargetWorkerButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (fileTargetWorker != null) {
                    fileTargetWorker.cancel(true);
                }
            }
        });
    }

    private void handleRootDirectorySelection(File selectedFile) {
        defaultDirectory = selectedFile;

        InputFileFilter inputFilter = projectConfig.getInputFileFilter();
        fileTargetWorker =
                new FileTargetWorker(selectedFile,
                                     inputFilter.getFilter(defaultDirectory),
                                     inputFilter.isRecursiveSearch(),
                                     FileTarget.ALPHABETIC_COMPARATOR,
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

        rootDirectoryBtn.setVisible(false);
        cancelTargetWorkerButton.setVisible(true);
        dataTable.setModel(new DefaultTableModel());

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
                    rootDirectoryField.setText(
                            (String) lastItem);
                }
            }
        }
    }

    private void handleFileTargetWorkerCompletion() {

        if (fileTargetWorker.isCancelled()) {

            resetData();

        } else if (fileTargetWorker.hasFailed()) {

            @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
            final Throwable failureCause =
                    fileTargetWorker.getFailureCause();
            resetData();
            NarrowOptionPane.showMessageDialog(
                    appPanel,
                    "The following error occurred when " +
                    "attempting to locate targets:\n" +
                    failureCause.getMessage(),
                    "Target Location Failure",
                    JOptionPane.ERROR_MESSAGE);

        } else {

            List<FileTarget> targets = null;
            try {
                targets = fileTargetWorker.get();
            } catch (Exception e) {
                LOG.error(e);
                NarrowOptionPane.showMessageDialog(
                        appPanel,
                        "The following error occurred when " +
                        "attempting to retrieve targets:\n" +
                        e.getMessage(),
                        "Target Retrieval Failure",
                        JOptionPane.ERROR_MESSAGE);
            }

            if (targets != null) {
                if (targets.size() > 0) {
                    createDataTableModel(targets);
                } else {
                    resetData();
                    File rootDirectory = fileTargetWorker.getRootDirectory();
                    NarrowOptionPane.showMessageDialog(
                            appPanel,
                            "No eligible targets were found " +
                            "in the selected root directory: " +
                            rootDirectory.getAbsolutePath(),
                            "No Eligible Targets Found",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        }

        fileTargetWorker = null;
    }

    private void createDataTableModel(List<? extends Target> targets) {
        rootDirectoryField.setText(defaultDirectory.getAbsolutePath());
        tableModel = new DataTableModel(projectConfig.getTargetDisplayName(),
                                        targets,
                                        projectConfig);
        dataTable.setModel(tableModel);
        dataTable.sizeTable();
        enableView(true);
    }

    private void resetData() {
        rootDirectoryField.setText("");
        dataTable.setModel(new DefaultTableModel());
        enableView(false);
    }

    private void enableView(boolean isTaskButtonEnabled) {
        rootDirectoryBtn.setVisible(true);
        cancelTargetWorkerButton.setVisible(false);
        rootDirectoryBtn.setEnabled(true);
        dataTable.setEnabled(true);
        saveBtn.setEnabled(isTaskButtonEnabled);
    }

    private void setupTaskComponents(Component iconParent) {
        this.taskComponents =
                new TaskComponents(dataTable,
                                            saveBtn,
                                            taskProgressBar,
                                            taskProgressLabel,
                                            iconParent,
                                            projectConfig,
                                            TaskButtonText.DEFAULT) {
                    protected Task getNewTask() {
                        return getNewTaskForView();
                    }
                    protected boolean isTaskReadyToStart() {
                        return isSessionReadyToStartForView();
                    }
                    protected void processTaskCompletion() {
                        processTaskCompletionForView();
                    }
        };
    }

    private Task getNewTaskForView() {
        task = new SimpleTask(tableModel);
        return task;
    }

    protected boolean isSessionReadyToStartForView() {
        boolean isReady = false;

        dataTable.editCellAt(-1, -1); // stop any current editor
        if (validateAllFields()) {
            int choice =
                    NarrowOptionPane.showConfirmDialog(
                            appPanel,
                            "Your entries have been validated.  Do you wish to continue?",
                            "Continue with Processing?",
                            JOptionPane.YES_NO_OPTION);

            isReady = (choice == JOptionPane.YES_OPTION);
        }

        return isReady;
    }

    private boolean validateAllFields() {
        boolean isValid = true;

        List<DataTableRow> rows = tableModel.getRows();
        int rowIndex = 0;
        for (DataTableRow row : rows) {
            Target rowTarget = row.getTarget();
            List<DataField> rowFields = row.getFields();

            // validate syntax based on transmogrifier configuration
            for (int fieldIndex = 0; fieldIndex < rowFields.size(); fieldIndex++) {
                DataField field = rowFields.get(fieldIndex);
                if (!field.verify()) {
                    isValid = false;
                    String message = "The " + field.getDisplayName() +
                                     " value for " + rowTarget.getName() +
                                     " is invalid.  " + field.getErrorMessage();
                    int columnIndex =
                            tableModel.getColumnIndexForField(fieldIndex);
                    dataTable.displayErrorDialog(message,
                                                 rowIndex,
                                                 columnIndex);
                    break;
                }
            }

            // only perform external validation if internal validation succeeds
            if (isValid) {
                String externalErrorMsg = null;
                try {
                    for (RowValidator validator :
                            projectConfig.getRowValidators()) {
                        validator.validate(new PluginDataRow(row.getDataRow()));
                    }
                } catch (ExternalDataException e) {
                    externalErrorMsg = e.getMessage();
                    LOG.info("external validation failed", e);
                } catch (ExternalSystemException e) {
                    externalErrorMsg = e.getMessage();
                    LOG.error(e.getMessage(), e);
                }

                if (externalErrorMsg != null) {
                    isValid = false;
                    dataTable.displayErrorDialog(externalErrorMsg,
                                                 rowIndex,
                                                 2);
                }
            }

            if (! isValid) {
                break;
            }

            rowIndex++;
        }
        return isValid;
    }

    private void processTaskCompletionForView() {
        List<Integer> failedRowIndices = task.getFailedRowIndices();
        int numberOfFailures = failedRowIndices.size();

        TaskComponents.displaySummaryDialog("Task",
                                            numberOfFailures,
                                            task.getTaskSummary(),
                                            appPanel);

        if (numberOfFailures == 0) {
            // everything succeeded, so reset the main view
            resetData();
        } else {
            // we had errors, so remove the files copied successfully
            // and restore the rest of the model
            tableModel.removeSuccessfullyCopiedFiles(failedRowIndices);
            enableView(true);
        }
    }

    /** The logger for this class. */
    private static final Logger LOG = Logger.getLogger(CollectorView.class);
}
