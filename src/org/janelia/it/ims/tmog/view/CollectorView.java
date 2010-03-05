/*
 * Copyright (c) 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.view;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.DataTableModel;
import org.janelia.it.ims.tmog.config.ProjectConfiguration;
import org.janelia.it.ims.tmog.config.preferences.ColumnDefaultSet;
import org.janelia.it.ims.tmog.config.preferences.ViewDefault;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowValidator;
import org.janelia.it.ims.tmog.target.FileTarget;
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
import java.io.File;
import java.util.List;

/**
 * This class manages the main or overall view for collecting data for
 * a set of files in a particular directory.
 *
 * @author Eric Trautman
 */
public class CollectorView implements SessionView, InputSelectionView {

    /** The name of the task supported by this view. */
    public static final String TASK_NAME = "collector";

    private JPanel appPanel;
    @SuppressWarnings({"UnusedDeclaration"})
    private JPanel directoryPanel;
    @SuppressWarnings({"UnusedDeclaration"})
    private JPanel dataPanel;
    private JScrollPane dataTableScrollPane;
    @SuppressWarnings({"UnusedDeclaration"})
    private JPanel dataButtonPanel;
    private JTextArea rootDirectoryField;
    private JButton rootDirectoryBtn;
    private JLabel projectLabel;
    private JButton saveBtn;
    private JLabel taskProgressLabel;
    private JProgressBar taskProgressBar;
    private DataTable dataTable;
    private JButton cancelTargetWorkerButton;
    private JScrollPane rootDirectoryPane;

    private ProjectConfiguration projectConfig;
    private File defaultDirectory;
    private InputSelectionHandler inputSelectionHandler;
    private DataTableModel tableModel;
    private SimpleTask task;
    private TaskComponents taskComponents;

    public CollectorView(ProjectConfiguration projectConfig,
                         File defaultDirectory,
                         JTabbedPane parentTabbedPane) {
        this.projectConfig = projectConfig;
        this.defaultDirectory = defaultDirectory;
        this.projectLabel.setText(projectConfig.getName());
        this.inputSelectionHandler =
                new InputSelectionHandler(projectConfig,
                                          defaultDirectory,
                                          rootDirectoryField,
                                          rootDirectoryBtn,
                                          cancelTargetWorkerButton,
                                          JFileChooser.DIRECTORIES_ONLY,
                                          "Select Root Directory",
                                          this);
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

    public void setPreferencesForCurrentProject() {
        if (tableModel != null) {
            dataTable.setColumnDefaultsToCurrent();
            final ColumnDefaultSet columnDefaults =
                    dataTable.getColumnDefaults();
            ViewDefault viewDefault = new ViewDefault(ViewDefault.CURRENT);
            viewDefault.deepCopyAndSetColumnDefaults(columnDefaults);

            tableModel.updateProjectViewPreferences(viewDefault);
        }
    }

    public void clearPreferencesForCurrentProject() {
        if (tableModel != null) {
            dataTable.setColumnDefaults(null, true);
            ViewDefault viewDefault = new ViewDefault(ViewDefault.CURRENT);
            tableModel.updateProjectViewPreferences(viewDefault);
        }
    }

    public void resizeDataTable(ResizeType resizeType) {
        switch (resizeType) {

            case WINDOW:
                final JScrollBar scrollBar =
                        dataTableScrollPane.getHorizontalScrollBar();
                if ((scrollBar != null) && scrollBar.isVisible()) {
                    dataTable.setColumnDefaultsToFit(dataPanel.getWidth());
                }
                break;

            case DATA:
                dataTable.setColumnDefaults(null, true);
                break;

            case PREFERENCES:
                if (tableModel != null) {
                    ViewDefault viewDefault =
                            tableModel.getProjectViewPreferences();
                    if (viewDefault != null) {
                        ColumnDefaultSet columnDefaults =
                                viewDefault.getColumnDefaultsCopy();
                        dataTable.setColumnDefaults(columnDefaults, true);
                    }
                }
                break;
        }
    }

    public void handleInputRootSelection(File selectedFile) {
        dataTable.setModel(new DefaultTableModel());
        saveBtn.setEnabled(false);
    }

    public void handleInputRootReset() {
        dataTable.setModel(new DefaultTableModel());
        enableView(false);
    }

    public void processInputTargets(List<FileTarget> targets) {
        tableModel = new DataTableModel(projectConfig.getTargetDisplayName(),
                                        targets,
                                        projectConfig);
        dataTable.setModelAndColumnDefaults(tableModel);
        enableView(true);
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

    private boolean validateAllFields() {
        boolean isValid = tableModel.verify();

        // only perform external validation if internal validation succeeds
        if (isValid) {

            List<DataRow> rows = tableModel.getRows();
            int rowIndex = 0;
            for (DataRow row : rows) {
                String externalErrorMsg = null;
                try {
                    for (RowValidator validator :
                            projectConfig.getRowValidators()) {
                        validator.validate(new PluginDataRow(row));
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
                    dataTable.selectRow(rowIndex);
                    dataTable.displayErrorDialog(externalErrorMsg);
                }

                if (! isValid) {
                    break;
                }

                rowIndex++;
            }

        } else {

            dataTable.selectErrorCell();            
            dataTable.displayErrorDialog(tableModel.getErrorMessage());

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
            inputSelectionHandler.resetInputRoot();
        } else {
            // we had errors, so remove the files copied successfully
            // and restore the rest of the model
            tableModel.removeSuccessfullyCopiedFiles(failedRowIndices);
            enableView(true);
        }
    }

    private void enableView(boolean isTaskButtonEnabled) {
        inputSelectionHandler.setEnabled(true);
        dataTable.setEnabled(true);
        saveBtn.setEnabled(isTaskButtonEnabled);
    }

    /** The logger for this class. */
    private static final Logger LOG = Logger.getLogger(CollectorView.class);
}
