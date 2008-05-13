/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.view;

import org.apache.log4j.Logger;
import org.janelia.it.ims.imagerenamer.DataTableModel;
import org.janelia.it.ims.imagerenamer.DataTableRow;
import org.janelia.it.ims.imagerenamer.FileTarget;
import org.janelia.it.ims.imagerenamer.Target;
import org.janelia.it.ims.imagerenamer.config.InputFileSorter;
import org.janelia.it.ims.imagerenamer.config.ProjectConfiguration;
import org.janelia.it.ims.imagerenamer.field.DataField;
import org.janelia.it.ims.imagerenamer.plugin.ExternalDataException;
import org.janelia.it.ims.imagerenamer.plugin.ExternalSystemException;
import org.janelia.it.ims.imagerenamer.plugin.PluginDataRow;
import org.janelia.it.ims.imagerenamer.plugin.RowValidator;
import org.janelia.it.ims.imagerenamer.task.SimpleTask;
import org.janelia.it.ims.imagerenamer.task.Task;
import org.janelia.it.ims.imagerenamer.view.component.DataTable;
import org.janelia.it.ims.imagerenamer.view.component.SessionIcon;
import org.janelia.it.ims.imagerenamer.view.component.TaskButtonText;
import org.janelia.it.ims.imagerenamer.view.component.TaskComponents;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class manages the main or overall view for collecting data for
 * a set of files in a particular directory.
 *
 * @author Eric Trautman
 */
public class CollectorView implements SessionView {
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

    private ProjectConfiguration projectConfig;
    private File defaultDirectory;
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
                    File parentLsmDirectory = defaultDirectory.getParentFile();
                    if (parentLsmDirectory != null) {
                        fileChooser.setCurrentDirectory(parentLsmDirectory);
                    } else {
                        fileChooser.setCurrentDirectory(defaultDirectory);
                    }
                }
                fileChooser.setFileSelectionMode(
                        JFileChooser.FILES_AND_DIRECTORIES);
                int choice = fileChooser.showDialog(parentPanel,
                                                    "Select Root");

                if (choice == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    if ((selectedFile != null) &&
                        (!selectedFile.isDirectory())) {
                        selectedFile = selectedFile.getParentFile();
                    }

                    if (selectedFile != null) {
                        validateDirectorySelection(selectedFile);
                    }
                }
            }
        });
    }

    private void validateDirectorySelection(File selectedFile) {
        defaultDirectory = selectedFile;

        File[] files = defaultDirectory.listFiles();

        StringBuilder reject = new StringBuilder();
        if (files.length > 0) {
            rootDirectoryField.setText(
                    defaultDirectory.getAbsolutePath());

            InputFileSorter sorter = projectConfig.getInputFileSorter();
            Arrays.sort(files, sorter.getComparator());
            ArrayList<Target> targets = new ArrayList<Target>(files.length);
            for (File file : files) {
                targets.add(new FileTarget(file));
            }

            tableModel = new DataTableModel("File Name",
                                            targets,
                                            projectConfig);
            dataTable.setModel(tableModel);
            dataTable.sizeTable();
            saveBtn.setEnabled(true);
        } else {
            reject.append("The selected directory (");
            reject.append(defaultDirectory.getAbsolutePath());
            reject.append(") is empty.  Please choose another directory.");
            JOptionPane.showMessageDialog(appPanel,
                                          reject.toString(),
                                          "Root Directory Selection Error",
                                          JOptionPane.ERROR_MESSAGE);
            resetData();
        }
    }

    private void resetData() {
        rootDirectoryField.setText("");
        dataTable.setModel(new DefaultTableModel());
        setEnabled(true, false);
    }

    private void setEnabled(boolean isEnabled,
                            boolean isTaskButtonEnabled) {
        Component[] cList = { rootDirectoryBtn, dataTable};
        for (Component c : cList) {
            if (isEnabled != c.isEnabled()) {
                c.setEnabled(isEnabled);
            }
        }

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
                    JOptionPane.showConfirmDialog(
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

            // validate syntax based on renamer configuration
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
            setEnabled(true, true);
        }
    }

    /** The logger for this class. */
    private static final Logger LOG = Logger.getLogger(RenameView.class);
}
