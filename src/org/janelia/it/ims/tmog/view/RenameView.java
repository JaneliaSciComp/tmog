/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.view;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.DataTableModel;
import org.janelia.it.ims.tmog.DataTableRow;
import org.janelia.it.ims.tmog.config.InputFileFilter;
import org.janelia.it.ims.tmog.config.InputFileSorter;
import org.janelia.it.ims.tmog.config.ProjectConfiguration;
import org.janelia.it.ims.tmog.config.output.OutputDirectoryConfiguration;
import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.filefilter.DirectoryOnlyFilter;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.RenamePluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowValidator;
import org.janelia.it.ims.tmog.target.FileTarget;
import org.janelia.it.ims.tmog.target.Target;
import org.janelia.it.ims.tmog.task.RenameTask;
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
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class manages the main or overall view for renaming a set of
 * files in a particular directory.
 *
 * @author Peter Davies
 * @author Eric Trautman
 */
public class RenameView implements SessionView {

    private File lsmDirectory;
    private JLabel lsmDirectoryField;
    private JButton lsmDirectoryBtn;
    private JButton outputDirectoryBtn;
    private JLabel outputDirectoryField;
    private JPanel appPanel;
    @SuppressWarnings({"UnusedDeclaration"})
    private JPanel directoryPanel; // referenced by RenameView.formw.form
    private JButton copyAndRenameBtn;
    private DataTable dataTable;
    private JProgressBar copyProgressBar;
    private JLabel copyProgressLabel;
    private JLabel projectLabel;
    private DataTableModel tableModel;
    private ProjectConfiguration projectConfig;
    private RenameTask task;
    private TaskComponents taskComponents;

    public RenameView(ProjectConfiguration projectConfig,
                      File lsmDirectory,
                      JTabbedPane parentTabbedPane) {
        this.projectConfig = projectConfig;
        this.lsmDirectory = lsmDirectory;
        this.projectLabel.setText(projectConfig.getName());
        setupInputDirectory();
        setupOutputDirectory();
        setupTaskComponents(parentTabbedPane);
    }

    public JPanel getPanel() {
        return appPanel;
    }

    public File getDefaultDirectory() {
        return lsmDirectory;
    }

    public boolean isTaskInProgress() {
        return taskComponents.isTaskInProgress();
    }

    public SessionIcon getSessionIcon() {
        return taskComponents.getSessionIcon();
    }

    private void setupInputDirectory() {
        lsmDirectoryBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                JPanel parentPanel = getPanel();
                JFileChooser fileChooser = new JFileChooser();
                if (lsmDirectory != null) {
                    File parentLsmDirectory = lsmDirectory.getParentFile();
                    if (parentLsmDirectory != null) {
                        fileChooser.setCurrentDirectory(parentLsmDirectory);
                    } else {
                        fileChooser.setCurrentDirectory(lsmDirectory);
                    }
                }
                fileChooser.setFileSelectionMode(
                        JFileChooser.FILES_AND_DIRECTORIES);
                int choice = fileChooser.showDialog(parentPanel,
                                                    "Select Source");

                if (choice == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    if ((selectedFile != null) &&
                        (!selectedFile.isDirectory())) {
                        selectedFile = selectedFile.getParentFile();
                    }

                    if (selectedFile != null) {
                        validateLsmDirectorySelection(selectedFile);
                    }
                }
            }
        });
    }

    private void validateLsmDirectorySelection(File selectedFile) {
        lsmDirectory = selectedFile;

        InputFileFilter inputFilter = projectConfig.getInputFileFilter();
        FileFilter fileFilter = inputFilter.getFilter();
        File[] files = lsmDirectory.listFiles(fileFilter);
        boolean acceptSelectedFile = (files.length > 0);
        OutputDirectoryConfiguration odConfig = projectConfig.getOutputDirectory();
        boolean isOutputDerivedFromSelection =
                odConfig.isDerivedFromEarliestModifiedFile();

        StringBuilder reject = new StringBuilder();
        String outputPath = null;
        if (acceptSelectedFile) {
            if (isOutputDerivedFromSelection) {
                outputPath = odConfig.getDerivedPathForEarliestFile(
                        lsmDirectory, files);
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
                if (!acceptSelectedFile) {
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
        } else {
            reject.append("The selected directory (");
            reject.append(lsmDirectory.getAbsolutePath());
            reject.append(") does not contain any files with names that match the pattern '");
            reject.append(inputFilter.getPatternString());
            reject.append("'.  Please choose another directory.");
        }

        if (acceptSelectedFile) {
            lsmDirectoryField.setText(
                    lsmDirectory.getAbsolutePath());
            if (isOutputDerivedFromSelection) {
                outputDirectoryField.setText(outputPath);
            }

            InputFileSorter sorter = projectConfig.getInputFileSorter();
            Arrays.sort(files, sorter.getComparator());
            ArrayList<Target> targets = new ArrayList<Target>(files.length);
            for (File file : files) {
                targets.add(new FileTarget(file, lsmDirectory));
            }
            
            tableModel = new DataTableModel("File Name",
                                            targets,
                                            projectConfig);
            dataTable.setModel(tableModel);
            dataTable.sizeTable();
            copyAndRenameBtn.setEnabled(true);
        } else {
            NarrowOptionPane.showMessageDialog(
                    appPanel,
                    reject.toString(),
                    "Source File Directory Selection Error",
                    JOptionPane.ERROR_MESSAGE);
            resetFileTable();
        }
    }

    private void resetFileTable() {
        lsmDirectoryField.setText("");
        OutputDirectoryConfiguration odConfig =
                projectConfig.getOutputDirectory();
        if (odConfig.isDerivedFromEarliestModifiedFile()) {
            outputDirectoryField.setText("");
        }
        dataTable.setModel(new DefaultTableModel());
        setFileTableEnabled(true, false);
    }

    private void setFileTableEnabled(boolean isEnabled,
                                     boolean isCopyButtonEnabled) {
        Component[] cList = {
                lsmDirectoryBtn, outputDirectoryBtn, dataTable};
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

        if (isManuallyChosen) {
            outputDirectoryBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.addChoosableFileFilter(
                            new DirectoryOnlyFilter(
                                    lsmDirectoryField.getText()));
                    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    fileChooser.showDialog(getPanel(), "Select Output Directory");
                    File selectedDirectory = fileChooser.getSelectedFile();
                    if (selectedDirectory != null) {
                        outputDirectoryField.setText(selectedDirectory.getPath());
                    }
                }
            });
        } else if (! odCfg.isDerivedFromEarliestModifiedFile()) {
            outputDirectoryField.setText(odCfg.getDescription());
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
        task = new RenameTask(tableModel,
                              projectConfig.getOutputDirectory(),
                              outputDirectoryField.getText());
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
                    OutputDirectoryConfiguration.createAndValidateDirectory(
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
        boolean isValid = true;

        OutputDirectoryConfiguration odCfg = projectConfig.getOutputDirectory();
        boolean isOutputDirectoryAlreadyValidated = odCfg.isDerivedForSession();
        File outputDirectory = null;
        String outputDirectoryPath;
        List<DataTableRow> rows = tableModel.getRows();
        int rowIndex = 0;
        for (DataTableRow row : rows) {
            Target rowTarget= row.getTarget();
            File rowFile = (File) rowTarget.getInstance();
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

            // only perform output directory validation if field validation
            if (isValid) {
                if (isOutputDirectoryAlreadyValidated) {
                    outputDirectory = baseOutputDirectory;
                } else {
                    // setup and validate the directories for each file
                    outputDirectoryPath = odCfg.getDerivedPath(rowFile,
                                                               rowFields);
                    outputDirectory = new File(outputDirectoryPath);
                    String outputFailureMsg =
                            OutputDirectoryConfiguration.createAndValidateDirectory(
                                    outputDirectory);
                    if (outputFailureMsg != null) {
                        isValid = false;
                        dataTable.displayErrorDialog(outputFailureMsg,
                                                     rowIndex,
                                                     2);
                    }
                }
            }

            // only perform external validation if internal validation succeeds
            if (isValid) {
                String externalErrorMsg = null;
                try {
                    for (RowValidator validator : projectConfig.getRowValidators()) {
                        validator.validate(
                                new RenamePluginDataRow(rowFile,
                                                        row.getDataRow(),
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
        int numberOfCopyFailures = failedRowIndices.size();

        TaskComponents.displaySummaryDialog("Rename",
                                            numberOfCopyFailures,
                                            task.getTaskSummary(),
                                            appPanel);

        if (numberOfCopyFailures == 0) {
            // everything succeeded, so reset the main view
            resetFileTable();
        } else {
            // we had errors, so remove the files copied successfully
            // and restore the rest of the model
            tableModel.removeSuccessfullyCopiedFiles(failedRowIndices);
            setFileTableEnabled(true, true);
        }
    }

    /** The logger for this class. */
    private static final Logger LOG = Logger.getLogger(RenameView.class);

}