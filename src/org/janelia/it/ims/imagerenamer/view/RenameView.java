/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.view;

import org.apache.log4j.Logger;
import org.janelia.it.ims.imagerenamer.DataTableModel;
import org.janelia.it.ims.imagerenamer.DataTableRow;
import org.janelia.it.ims.imagerenamer.FileTarget;
import org.janelia.it.ims.imagerenamer.ImageRenamer;
import org.janelia.it.ims.imagerenamer.Target;
import org.janelia.it.ims.imagerenamer.config.InputFileFilter;
import org.janelia.it.ims.imagerenamer.config.InputFileSorter;
import org.janelia.it.ims.imagerenamer.config.ProjectConfiguration;
import org.janelia.it.ims.imagerenamer.config.output.OutputDirectoryConfiguration;
import org.janelia.it.ims.imagerenamer.field.DataField;
import org.janelia.it.ims.imagerenamer.filefilter.DirectoryOnlyFilter;
import org.janelia.it.ims.imagerenamer.plugin.ExternalDataException;
import org.janelia.it.ims.imagerenamer.plugin.ExternalSystemException;
import org.janelia.it.ims.imagerenamer.plugin.RenamePluginDataRow;
import org.janelia.it.ims.imagerenamer.plugin.RowListener;
import org.janelia.it.ims.imagerenamer.plugin.RowValidator;
import org.janelia.it.ims.imagerenamer.plugin.SessionListener;
import org.janelia.it.ims.imagerenamer.task.RenameTask;
import org.janelia.it.ims.imagerenamer.task.TaskProgressInfo;
import org.janelia.it.ims.imagerenamer.view.component.DataTable;
import org.janelia.it.ims.imagerenamer.view.component.SessionIcon;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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
public class RenameView implements SessionView, PropertyChangeListener {

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
    private boolean isRenameTaskInProgress;
    private SessionIcon sessionIcon;

    public RenameView(ProjectConfiguration projectConfig,
                      File lsmDirectory,
                      JTabbedPane parentTabbedPane) {
        this.projectConfig = projectConfig;
        this.lsmDirectory = lsmDirectory;
        this.sessionIcon = new SessionIcon(parentTabbedPane);

        this.projectLabel.setText(projectConfig.getName());
        setupInputDirectory();
        setupOutputDirectory();
        setupProcess();
    }

    public JPanel getPanel() {
        return appPanel;
    }

    public File getDefaultDirectory() {
        return lsmDirectory;
    }

    public JLabel getLsmDirectoryField() {
        return lsmDirectoryField;
    }

    public JLabel getOutputDirectoryField() {
        return outputDirectoryField;
    }

    public JTable getDataTable() {
        return dataTable;
    }

    public JProgressBar getCopyProgressBar() {
        return copyProgressBar;
    }

    public JLabel getCopyProgressLabel() {
        return copyProgressLabel;
    }

    public DataTableModel getTableModel() {
        return tableModel;
    }

    public boolean isTaskInProgress() {
        return isRenameTaskInProgress;
    }

    public void setRenameTaskInProgress(boolean renameTaskInProgress,
                                        boolean updateIcon) {
        this.isRenameTaskInProgress = renameTaskInProgress;
        if (isRenameTaskInProgress) {
            copyAndRenameBtn.setText(RENAME_CANCEL_BUTTON_TEXT);
            copyAndRenameBtn.setToolTipText(RENAME_CANCEL_TOOL_TIP_TEXT);
            if (updateIcon) {
                sessionIcon.setToWait();
            }
            copyProgressBar.setModel(new DefaultBoundedRangeModel());
            copyProgressBar.setVisible(true);
            copyProgressLabel.setText("");
            copyProgressLabel.setVisible(true);
        } else {
            copyAndRenameBtn.setText(RENAME_START_BUTTON_TEXT);
            copyAndRenameBtn.setToolTipText(RENAME_START_TOOL_TIP_TEXT);
            task = null;
            if (updateIcon) {
                sessionIcon.setToEnterValues();
            }
            copyProgressBar.setModel(new DefaultBoundedRangeModel());
            copyProgressBar.setVisible(false);
            copyProgressLabel.setText("");
            copyProgressLabel.setVisible(false);
        }
    }

    public SessionIcon getSessionIcon() {
        return sessionIcon;
    }

    public void resetFileTable() {
        lsmDirectoryField.setText("");
        OutputDirectoryConfiguration odConfig =
                projectConfig.getOutputDirectory();
        if (odConfig.isDerivedFromEarliestModifiedFile()) {
            outputDirectoryField.setText("");
        }
        dataTable.setModel(new DefaultTableModel());
        setFileTableEnabled(true, false);
    }

    public void setFileTableEnabled(boolean isEnabled,
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

    /**
     * This method gets called when a bound property is changed.
     *
     * @param evt A PropertyChangeEvent object describing the event source
     *            and the property that has changed.
     */

    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        Object newValue = evt.getNewValue();
        if (RenameTask.PROGRESS_PROPERTY.equals(propertyName)) {
            if (newValue instanceof List) {
                List list = (List) newValue;
                int size = list.size();
                if (size > 0) {
                    Object lastElement = list.get(size - 1);
                    if (lastElement instanceof TaskProgressInfo) {
                        dataTable.updateProgress(
                                (TaskProgressInfo) lastElement,
                                copyProgressBar,
                                copyProgressLabel,
                                sessionIcon);
                    }
                }
            }

        } else if (RenameTask.COMPLETION_PROPERTY.equals(propertyName)) {
            if (newValue instanceof RenameTask) {
                setRenameTaskInProgress(false, true);
                processTaskCompletion((RenameTask) newValue);
            }
        }

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
                targets.add(new FileTarget(file));
            }
            
            tableModel = new DataTableModel("File Name",
                                            targets,
                                            projectConfig);
            dataTable.setModel(tableModel);
            dataTable.sizeTable();
            copyAndRenameBtn.setEnabled(true);
        } else {
            JOptionPane.showMessageDialog(appPanel,
                                          reject.toString(),
                                          "Source File Directory Selection Error",
                                          JOptionPane.ERROR_MESSAGE);
            resetFileTable();
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

    private void setupProcess() {
        copyAndRenameBtn.setEnabled(false);
        setRenameTaskInProgress(false, true);

        copyAndRenameBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                copyAndRename();
            }
        });
        
        copyProgressBar.setVisible(false);
        copyProgressLabel.setVisible(false);
    }

    private void copyAndRename() {
        if (isRenameTaskInProgress) {
            cancelSession();
        } else {
            if (isSessionReadyToStart()) {
                startSession();
            }
        }
    }

    public void cancelSession() {
        task.cancelSession();
        setRenameTaskInProgress(false, false);
        copyAndRenameBtn.setText(RENAME_CANCELLED_BUTTON_TEXT);
        copyAndRenameBtn.setToolTipText(
                RENAME_CANCELLED_TOOL_TIP_TEXT);
        copyAndRenameBtn.setEnabled(false);
    }

    private boolean isSessionReadyToStart() {
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
                JOptionPane.showMessageDialog(appPanel,
                                              outputFailureMsg,
                                              "Error",
                                              JOptionPane.ERROR_MESSAGE);
            }
        }

        if (isOutputDirectoryValid &&
            validateAllFields(outputDirectory)) {
            int choice =
                    JOptionPane.showConfirmDialog(
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

    private void startSession() {
        setFileTableEnabled(false, true);
        task = new RenameTask(tableModel,
                              projectConfig.getOutputDirectory(),
                              outputDirectoryField.getText());
        task.addPropertyChangeListener(this);
        for (RowListener listener :
                projectConfig.getRowListeners()) {
            task.addRowListener(listener);
        }
        for (SessionListener listener :
                projectConfig.getSessionListeners()) {
            task.addSessionListener(listener);
        }
        setRenameTaskInProgress(true, true);
        ImageRenamer.submitTask(task);
    }

    private void processTaskCompletion(RenameTask task) {
        List<Integer> failedRowIndices = task.getFailedRowIndices();
        int numberOfCopyFailures = failedRowIndices.size();

        displaySummaryDialog(numberOfCopyFailures, task.getTaskSummary());

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

    private void displaySummaryDialog(int numberOfCopyFailures,
                                      String renameSummary) {
        String dialogTitle;
        if (numberOfCopyFailures > 0) {
            dialogTitle = "Rename Summary (" + numberOfCopyFailures;
            if (numberOfCopyFailures > 1) {
                dialogTitle = dialogTitle + " COPY FAILURES!)";
            } else {
                dialogTitle = dialogTitle + " COPY FAILURE!)";
            }
        } else {
            dialogTitle = "Rename Summary";
        }

        JTextArea textArea = new JTextArea();
        textArea.setLayout(new BorderLayout());
        textArea.setEditable(false);
        textArea.append(renameSummary);
        JScrollPane areaScrollPane = new JScrollPane(textArea);
        areaScrollPane.setPreferredSize(new Dimension(600, 400));
        areaScrollPane.setWheelScrollingEnabled(true);
        areaScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        areaScrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        Dimension appPanelSize = appPanel.getSize();
        int dialogWidth = (int) (appPanelSize.getWidth() * 0.8);
        int dialogHeight = (int) (appPanelSize.getHeight() * 0.8);
        Dimension dialogSize = new Dimension(dialogWidth, dialogHeight);

        JOptionPane jop = new JOptionPane(areaScrollPane,
                                          JOptionPane.INFORMATION_MESSAGE);
        jop.setPreferredSize(dialogSize);
        JDialog jd = jop.createDialog(appPanel, dialogTitle);
        jd.setModal(false);
        jd.setVisible(true);
    }

    /** The logger for this class. */
    private static final Logger LOG = Logger.getLogger(RenameView.class);

    private static final String RENAME_START_BUTTON_TEXT = "Copy and Rename";
    private static final String RENAME_START_TOOL_TIP_TEXT =
            "Copy and rename all files using specified information";
    private static final String RENAME_CANCEL_BUTTON_TEXT =
            "Cancel Rename In Progress";
    private static final String RENAME_CANCEL_TOOL_TIP_TEXT =
            "Cancel the renaming process that is currently running";
    private static final String RENAME_CANCELLED_BUTTON_TEXT =
            "Rename Session Cancelled";
    private static final String RENAME_CANCELLED_TOOL_TIP_TEXT =
            "Waiting for current file processing to complete";
}