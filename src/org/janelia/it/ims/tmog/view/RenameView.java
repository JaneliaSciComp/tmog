/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.view;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.DataTableModel;
import org.janelia.it.ims.tmog.config.ProjectConfiguration;
import org.janelia.it.ims.tmog.config.output.OutputDirectoryConfiguration;
import org.janelia.it.ims.tmog.config.preferences.ColumnDefaultSet;
import org.janelia.it.ims.tmog.config.preferences.PathDefault;
import org.janelia.it.ims.tmog.config.preferences.TransmogrifierPreferences;
import org.janelia.it.ims.tmog.config.preferences.ViewDefault;
import org.janelia.it.ims.tmog.filefilter.DirectoryOnlyFilter;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.RenamePluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowValidator;
import org.janelia.it.ims.tmog.target.FileTarget;
import org.janelia.it.ims.tmog.target.Target;
import org.janelia.it.ims.tmog.task.RenameTask;
import org.janelia.it.ims.tmog.task.RenameWithoutDeleteTask;
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
import java.io.File;
import java.util.List;

/**
 * This class manages the main or overall view for renaming a set of
 * files in a particular directory.
 *
 * @author Peter Davies
 * @author Eric Trautman
 */
public class RenameView implements SessionView, InputSelectionView {

    private JTextArea projectName;
    private JButton lsmDirectoryBtn;
    private JScrollPane lsmDirectoryPane;
    private JTextArea lsmDirectoryField;
    private InputSelectionHandler inputSelectionHandler;
    private JButton outputDirectoryBtn;
    private JScrollPane outputDirectoryPane;
    private JTextArea outputDirectoryField;
    private JPanel appPanel;
    @SuppressWarnings({"UnusedDeclaration"})
    private JPanel directoryPanel;
    @SuppressWarnings({"UnusedDeclaration"})
    private JPanel dataPanel;
    private JScrollPane dataTableScrollPane;
    @SuppressWarnings({"UnusedDeclaration"})
    private JPanel dataButtonPanel;
    private JButton copyAndRenameBtn;
    private DataTable dataTable;
    private JProgressBar copyProgressBar;
    private JLabel copyProgressLabel;
    private JButton cancelInputSearch;
    private JScrollPane projectNamePane;
    private DataTableModel tableModel;
    private ProjectConfiguration projectConfig;
    private RenameTask task;
    private TaskComponents taskComponents;
    private String projectNameText;

    public RenameView(ProjectConfiguration projectConfig,
                      File lsmDirectory,
                      JTabbedPane parentTabbedPane) {
        this.projectConfig = projectConfig;
        this.projectNameText = projectConfig.getName();
        this.projectName.setText(projectNameText);

        this.inputSelectionHandler =
                new InputSelectionHandler(projectConfig,
                                          lsmDirectory,
                                          lsmDirectoryField,
                                          lsmDirectoryBtn,
                                          cancelInputSearch,
                                          JFileChooser.FILES_AND_DIRECTORIES,
                                          "Select Source",
                                          this);

        projectName.setBackground(directoryPanel.getBackground());
        projectNamePane.setBorder(null);
        
        lsmDirectoryField.setBackground(directoryPanel.getBackground());
        lsmDirectoryPane.setBorder(null);

        setupOutputDirectory();
        setupTaskComponents(parentTabbedPane);
    }

    public JPanel getPanel() {
        return appPanel;
    }

    public File getDefaultDirectory() {
        return inputSelectionHandler.getDefaultDirectory();
    }

    public boolean isTaskInProgress() {
        return taskComponents.isTaskInProgress();
    }

    public SessionIcon getSessionIcon() {
        return taskComponents.getSessionIcon();
    }

    // TODO: refactor duplicated view preferences methods into abstract base class    
    public void setPreferencesForCurrentProject() {
        if (tableModel != null) {
            dataTable.setColumnDefaultsToCurrent();
            final ColumnDefaultSet columnDefaults =
                    dataTable.getColumnDefaults();
            ViewDefault viewDefault = new ViewDefault(ViewDefault.CURRENT);
            viewDefault.deepCopyAndSetColumnDefaults(columnDefaults);

            TransmogrifierPreferences.updateProjectViewPreferences(
                                    projectNameText,
                                    viewDefault);
        }
    }

    public void clearPreferencesForCurrentProject() {
        if (tableModel != null) {
            dataTable.setColumnDefaults(null, true);
            ViewDefault viewDefault = new ViewDefault(ViewDefault.CURRENT);
            TransmogrifierPreferences.updateProjectViewPreferences(
                                    projectNameText,
                                    viewDefault);
        }
    }


    public void resizeDataTable(ResizeType resizeType) {
        switch (resizeType) {

            case WINDOW:
                final JScrollBar scrollBar =
                        dataTableScrollPane.getHorizontalScrollBar();
                if ((scrollBar != null) && scrollBar.isVisible()) {
                    int fitWidth = dataPanel.getWidth();
                    // HACK: reduce data panel width by 20% to ensure 
                    // data table completely fits in displayable area
                    final int magicFactor = fitWidth / 5;
                    fitWidth = fitWidth - magicFactor;
                    dataTable.setColumnDefaultsToFit(fitWidth);
                }
                break;

            case DATA:
                dataTable.setColumnDefaults(null, true);
                break;

            case PREFERENCES:
                if (tableModel != null) {
                    ViewDefault viewDefault =
                            TransmogrifierPreferences.getProjectViewPreferences(
                                    projectNameText);
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
    }

    public void handleInputRootReset() {
        OutputDirectoryConfiguration odConfig =
                projectConfig.getOutputDirectory();
        if (odConfig.isDerivedFromEarliestModifiedFile()) {
            outputDirectoryField.setText("");
        }
        dataTable.setModel(new DefaultTableModel());
        setFileTableEnabled(true, false);
    }

    public void processInputTargets(List<FileTarget> targets) {

        boolean acceptSelectedFile = true;
        OutputDirectoryConfiguration odConfig =
                projectConfig.getOutputDirectory();
        boolean isOutputDerivedFromSelection =
                odConfig.isDerivedFromEarliestModifiedFile();

        StringBuilder reject = new StringBuilder();
        String outputPath = null;
        if (isOutputDerivedFromSelection) {
            outputPath =
                    odConfig.getDerivedPathForEarliestFile(
                            inputSelectionHandler.getDefaultDirectory(),
                            targets);
            File derivedOutputDir = new File(outputPath);
            boolean outputDirExists = derivedOutputDir.exists();
            if (outputDirExists) {
                acceptSelectedFile =
                        derivedOutputDir.canWrite();
            } else {
                File outputBaseDir =
                        derivedOutputDir.getParentFile();
                acceptSelectedFile =
                        (outputBaseDir != null) &&
                        outputBaseDir.canWrite();
            }

            if (! acceptSelectedFile) {
                reject.append("The derived output directory for your selection (");
                reject.append(outputPath);
                if (outputDirExists) {
                    reject.append(") does not allow you write access.  ");
                } else {
                    reject.append(") can not be created.  ");
                }
                reject.append("Please verify your access privileges and the ");
                reject.append("constraints set for this filesystem.");
            }
        }

        if (acceptSelectedFile) {
            inputSelectionHandler.setEnabled(true);            
            if (isOutputDerivedFromSelection) {
                outputDirectoryField.setText(outputPath);
            }
            tableModel = new DataTableModel("File Name",
                                            targets,
                                            projectConfig);
            dataTable.setModelAndColumnDefaults(tableModel);
            copyAndRenameBtn.setEnabled(true);
        } else {
            NarrowOptionPane.showMessageDialog(
                    appPanel,
                    reject.toString(),
                    "Source File Directory Selection Error",
                    JOptionPane.ERROR_MESSAGE);
            inputSelectionHandler.resetInputRoot();
        }
    }

    private void setFileTableEnabled(boolean isEnabled,
                                     boolean isCopyButtonEnabled) {
        inputSelectionHandler.setEnabled(isEnabled);
        Component[] cList = { outputDirectoryBtn, dataTable };
        for (Component c : cList) {
            if (isEnabled != c.isEnabled()) {
                c.setEnabled(isEnabled);
            }
        }

        if (copyAndRenameBtn.isEnabled() != isCopyButtonEnabled) {
            copyAndRenameBtn.setEnabled(isCopyButtonEnabled);
        }
    }

    private void setupOutputDirectory() {
        OutputDirectoryConfiguration odCfg = projectConfig.getOutputDirectory();
        boolean isManuallyChosen = odCfg.isManuallyChosen();
        outputDirectoryBtn.setVisible(isManuallyChosen);
        outputDirectoryField.setBackground(directoryPanel.getBackground());
        outputDirectoryPane.setBorder(null);

        if (isManuallyChosen) {
            outputDirectoryBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.addChoosableFileFilter(
                            new DirectoryOnlyFilter(
                                    lsmDirectoryField.getText()));
                    fileChooser.setFileSelectionMode(
                            JFileChooser.DIRECTORIES_ONLY);

                    setFileChooserCurrentDirectory(fileChooser);
                    InputSelectionHandler.setPreferredSize(fileChooser,
                                                           appPanel,
                                                           0.9);

                    fileChooser.showDialog(appPanel, "Select Output Directory");
                    File selectedDirectory = fileChooser.getSelectedFile();
                    if (selectedDirectory != null) {
                        outputDirectoryField.setText(selectedDirectory.getPath());
                        saveTransferDirectoryPreference(selectedDirectory);
                    }
                }
            });
        } else if (! odCfg.isDerivedFromEarliestModifiedFile()) {
            // add space after description to work around Metal clipping error 
            outputDirectoryField.setText(odCfg.getDescription() + " ");
        }

    }

    private void setupTaskComponents(Component iconParent) {
        this.taskComponents =
                new TaskComponents(dataTable,
                                   copyAndRenameBtn,
                                   copyProgressBar,
                                   copyProgressLabel,
                                   iconParent,
                                   projectConfig,
                                   TaskButtonText.RENAME) {
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
        if (RenameWithoutDeleteTask.TASK_NAME.equals(
                projectConfig.getTaskName())) {
            task = new RenameWithoutDeleteTask(
                    tableModel,
                    projectConfig.getOutputDirectory(),
                    projectConfig.getFileTransfer(),
                    outputDirectoryField.getText());
        } else {
            task = new RenameTask(tableModel,
                                  projectConfig.getOutputDirectory(),
                                  projectConfig.getFileTransfer(),
                                  outputDirectoryField.getText());
        }
        return task;
    }

    private boolean isSessionReadyToStartForView() {
        boolean isReady = false;
        boolean isOutputDirectoryValid = true;

        dataTable.editCellAt(-1, -1); // stop any current editor
        OutputDirectoryConfiguration odCfg =
                projectConfig.getOutputDirectory();
        File outputDirectory = null;
        if (odCfg.isDerivedForSession()) {
            outputDirectory = new File(outputDirectoryField.getText());
            String outputFailureMsg =
                    OutputDirectoryConfiguration.validateDirectory(
                            outputDirectory);

            if (outputFailureMsg != null) {
                isOutputDirectoryValid = false;
                NarrowOptionPane.showMessageDialog(appPanel,
                                                   outputFailureMsg,
                                                   "Error",
                                                   JOptionPane.ERROR_MESSAGE);
            }
        }

        if (isOutputDirectoryValid &&
            validateAllFields(outputDirectory)) {
            int choice =
                    NarrowOptionPane.showConfirmDialog(
                            appPanel,
                            "Your entries have been validated.  Do you wish to continue?",
                            "Continue with Rename?",
                            JOptionPane.YES_NO_OPTION);

            isReady = (choice == JOptionPane.YES_OPTION);
        }

        return isReady;
    }

    private boolean validateAllFields(File baseOutputDirectory) {
        boolean isValid = tableModel.verify();

        // only perform other validation checks if basic field validation succeeds
        if (isValid) {
            OutputDirectoryConfiguration odCfg = projectConfig.getOutputDirectory();
            boolean isOutputDirectoryAlreadyValidated = odCfg.isDerivedForSession();
            File outputDirectory;
            String outputDirectoryPath;
            List<DataRow> rows = tableModel.getRows();
            int rowIndex = 0;
            for (DataRow row : rows) {
                Target rowTarget= row.getTarget();
                File rowFile = (File) rowTarget.getInstance();

                if (isOutputDirectoryAlreadyValidated) {
                    outputDirectory = baseOutputDirectory;
                } else {
                    // setup and validate the directories for each file
                    // TODO: add support for nested fields
                    outputDirectoryPath = odCfg.getDerivedPath(rowFile,
                                                               row.getFields());
                    outputDirectory = new File(outputDirectoryPath);
                    String outputFailureMsg =
                            OutputDirectoryConfiguration.validateDirectory(
                                    outputDirectory);
                    if (outputFailureMsg != null) {
                        isValid = false;
                        dataTable.selectRow(rowIndex);
                        dataTable.displayErrorDialog(outputFailureMsg);
                    }
                }

                // only perform external validation if output directory validation succeeds
                if (isValid) {
                    String externalErrorMsg = null;
                    try {
                        for (RowValidator validator : projectConfig.getRowValidators()) {
                            validator.validate(
                                    new RenamePluginDataRow(rowFile,
                                                            row,
                                                            outputDirectory));
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
        int numberOfCopyFailures = failedRowIndices.size();

        TaskComponents.displaySummaryDialog("Rename",
                                            numberOfCopyFailures,
                                            task.getTaskSummary(),
                                            appPanel);

        if (numberOfCopyFailures == 0) {
            // everything succeeded, so reset the main view
            inputSelectionHandler.resetInputRoot();
        } else {
            // we had errors, so remove the files copied successfully
            // and restore the rest of the model
            tableModel.removeSuccessfullyCopiedFiles(failedRowIndices);
            setFileTableEnabled(true, true);
        }
    }

    private void setFileChooserCurrentDirectory(JFileChooser fileChooser) {

        File directory = null;
        ViewDefault viewDefault =
                TransmogrifierPreferences.getProjectViewPreferences(
                        projectNameText);
        if (viewDefault != null) {
            PathDefault pathDefault = viewDefault.getTransferPathDefault();
            if (pathDefault != null) {
                directory = new File(pathDefault.getValue());
            }
        }

        if ((directory != null) &&
            directory.exists() &&
            directory.canRead() &&
            directory.isDirectory()) {
            fileChooser.setCurrentDirectory(directory);
        }
    }

    private void saveTransferDirectoryPreference(File directory) {

        ViewDefault viewDefault =
                TransmogrifierPreferences.getProjectViewPreferences(
                        projectNameText);
        if (viewDefault != null) {
            PathDefault pathDefault = viewDefault.getTransferPathDefault();
            if (pathDefault == null) {
                pathDefault = new PathDefault(PathDefault.TRANSFER_DIRECTORY);
                viewDefault.addPathDefault(pathDefault);
            }
            File value = directory.getParentFile();
            if (value == null) {
                value = directory;
            }
            pathDefault.setValue(value.getAbsolutePath());
            TransmogrifierPreferences preferences =
                    TransmogrifierPreferences.getInstance();
            if (preferences.canWrite()) {
                preferences.save();
            }
        }
    }

    /** The logger for this class. */
    private static final Logger LOG = Logger.getLogger(RenameView.class);

}