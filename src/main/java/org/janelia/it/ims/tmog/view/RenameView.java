/*
 * Copyright (c) 2015 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.view;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

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
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.RenamePluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowUpdater;
import org.janelia.it.ims.tmog.plugin.RowValidator;
import org.janelia.it.ims.tmog.target.FileTarget;
import org.janelia.it.ims.tmog.target.Target;
import org.janelia.it.ims.tmog.task.MoveAndLogDigestTask;
import org.janelia.it.ims.tmog.task.RenameTask;
import org.janelia.it.ims.tmog.task.RenameWithoutDeleteTask;
import org.janelia.it.ims.tmog.task.SimpleMoveTask;
import org.janelia.it.ims.tmog.task.Task;
import org.janelia.it.ims.tmog.view.component.DataTable;
import org.janelia.it.ims.tmog.view.component.NarrowOptionPane;
import org.janelia.it.ims.tmog.view.component.SessionIcon;
import org.janelia.it.ims.tmog.view.component.TaskButtonText;
import org.janelia.it.ims.tmog.view.component.TaskComponents;
import org.janelia.it.utils.FileUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * This class manages the main or overall view for renaming a set of files in a particular directory.
 *
 * @author Peter Davies
 * @author Eric Trautman
 */
public class RenameView
        implements SessionView, InputSelectionView {

    private JTextArea projectName;
    private JButton lsmDirectoryBtn;
    private JScrollPane lsmDirectoryPane;
    private JLabel lsmDirectoryLabel;
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
    private JButton loadMappedDataButton;
    private DataTableModel tableModel;

    private String sessionName;
    private ProjectConfiguration projectConfig;
    private RenameTask task;
    private TaskComponents taskComponents;
    private String projectNameText;

    public RenameView(String sessionName,
                      ProjectConfiguration projectConfig,
                      File lsmDirectory,
                      JTabbedPane parentTabbedPane) {
        this.sessionName = sessionName;
        this.projectConfig = projectConfig;
        this.projectNameText = projectConfig.getName();
        this.projectName.setText(projectNameText);

        this.inputSelectionHandler =
                new FileInputSelectionHandler(projectConfig,
                                              lsmDirectory,
                                              lsmDirectoryLabel,
                                              lsmDirectoryField,
                                              lsmDirectoryBtn,
                                              cancelInputSearch,
                                              JFileChooser.FILES_AND_DIRECTORIES,
                                              "Select Source",
                                              this);

        projectNamePane.setBorder(null);
        lsmDirectoryPane.setBorder(null);

        setupOutputDirectory();
        setupTaskComponents(parentTabbedPane);

        if (projectConfig.hasRowUpdaters()) {
            loadMappedDataButton.setVisible(true);
            loadMappedDataButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SwingWorker worker = new SwingWorker() {
                        @Override
                        protected Object doInBackground()
                                throws Exception {
                            loadMappedData();
                            return null;
                        }
                    };
                    worker.execute();
                }
            });
        }
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
                acceptSelectedFile = FileUtil.canWriteToDirectory(derivedOutputDir);
            } else {
                File outputBaseDir = derivedOutputDir.getParentFile();
                acceptSelectedFile = FileUtil.canWriteToDirectory(outputBaseDir);
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
            loadMappedDataButton.setEnabled(true);
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
        Component[] cList = {outputDirectoryBtn, dataTable};
        for (Component c : cList) {
            if (isEnabled != c.isEnabled()) {
                c.setEnabled(isEnabled);
            }
        }

        if (copyAndRenameBtn.isEnabled() != isCopyButtonEnabled) {
            copyAndRenameBtn.setEnabled(isCopyButtonEnabled);
        }

        if (loadMappedDataButton.isVisible() &&
            (loadMappedDataButton.isEnabled() != isCopyButtonEnabled)) {
            loadMappedDataButton.setEnabled(isCopyButtonEnabled);
        }
    }

    private void setupOutputDirectory() {
        OutputDirectoryConfiguration odCfg = projectConfig.getOutputDirectory();
        boolean isManuallyChosen = odCfg.isManuallyChosen();
        outputDirectoryBtn.setVisible(isManuallyChosen);
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
                    FileInputSelectionHandler.setPreferredSize(fileChooser,
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
        } else if (!odCfg.isDerivedFromEarliestModifiedFile()) {
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
        final String taskName = projectConfig.getTaskName();
        if (RenameWithoutDeleteTask.TASK_NAME.equals(taskName)) {

            task = new RenameWithoutDeleteTask(
                    tableModel,
                    projectConfig.getOutputDirectory(),
                    projectConfig.getFileTransfer(),
                    outputDirectoryField.getText());

        } else if (SimpleMoveTask.TASK_NAME.equals(taskName)) {

            task = new SimpleMoveTask(tableModel,
                                      projectConfig.getOutputDirectory(),
                                      projectConfig.getFileTransfer(),
                                      outputDirectoryField.getText());

        } else if (MoveAndLogDigestTask.TASK_NAME.equals(taskName)) {

            task = new MoveAndLogDigestTask(tableModel,
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

    private void loadMappedData() {

        final int editingRowIndex = dataTable.getEditingRow();
        final int editingColumnIndex = dataTable.getEditingColumn();

        dataTable.editCellAt(-1, -1); // stop any current editor

        String externalErrorMsg = null;
        List<DataRow> rows = tableModel.getRows();
        int rowIndex = 0;
        for (DataRow row : rows) {
            try {
                for (RowUpdater updater : projectConfig.getRowUpdaters()) {
                    updater.updateRow(new PluginDataRow(row));
                }
            } catch (ExternalDataException e) {
                externalErrorMsg = e.getMessage();
                LOG.info("external update failed", e);
            } catch (ExternalSystemException e) {
                externalErrorMsg = e.getMessage();
                LOG.error(e.getMessage(), e);
            }

            dataTable.selectRow(rowIndex);

            if (externalErrorMsg != null) {
                dataTable.displayErrorDialog(externalErrorMsg);
                break;
            }

            rowIndex++;
        }

        dataTable.repaint();

        if ((editingRowIndex > -1) && (editingColumnIndex > -1)) {
            dataTable.selectRow(editingRowIndex);
            dataTable.editCellAt(editingRowIndex, editingColumnIndex);
        }
    }

    private boolean validateAllFields(File baseOutputDirectory) {
        boolean isValid = tableModel.verify();

        // only perform other validation checks if basic field validation succeeds
        if (isValid) {
            OutputDirectoryConfiguration odCfg = projectConfig.getOutputDirectory();
            boolean isOutputDirectoryAlreadyValidated = odCfg.isDerivedForSession();
            File outputDirectory;
            String outputDirectoryPath;
            final List<DataRow> rows =
                    Collections.unmodifiableList(tableModel.getRows());
            int rowIndex = 0;

            final List<RowValidator> validators =
                    projectConfig.getRowValidators();

            // call validators to set-up for session
            try {
                for (RowValidator validator : validators) {
                    validator.startSessionValidation(sessionName, rows);
                }
            } catch (ExternalSystemException e) {
                isValid = false;
                dataTable.displayErrorDialog(e.getMessage());
            }

            // only perform row validation
            // if external start session call succeeded
            if (isValid) {

                for (DataRow row : rows) {
                    Target rowTarget = row.getTarget();
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

                    // only perform external validation
                    // if output directory validation succeeds
                    if (isValid) {
                        String externalErrorMsg = null;
                        try {
                            for (RowValidator validator : validators) {
                                validator.validate(
                                        sessionName,
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

                    if (!isValid) {
                        break;
                    }

                    rowIndex++;
                }
            }

            // always call validators to clean-up session
            for (RowValidator validator : validators) {
                validator.stopSessionValidation(sessionName);
            }

        } else {

            dataTable.selectErrorCell();
            dataTable.displayErrorDialog(tableModel.getErrorMessage());

        }

        return isValid;
    }

    private void processTaskCompletionForView() {
        List<Integer> failedRowIndices = task.getFailedRowIndices();
        final int numberOfFailures = failedRowIndices.size();

        // log completion with project name to help track which projects are being used
        LOG.info("completed '" + projectName.getText() + "' task with " +
                 numberOfFailures + " failures");

        TaskComponents.displaySummaryDialog("Rename",
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

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer >>> IMPORTANT!! <<< DO NOT edit this method OR call it in your
     * code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        appPanel = new JPanel();
        appPanel.setLayout(new BorderLayout(0, 0));
        appPanel.setMinimumSize(new Dimension(100, 249));
        appPanel.setPreferredSize(new Dimension(1280, 500));
        directoryPanel = new JPanel();
        directoryPanel.setLayout(new GridLayoutManager(3, 4, new Insets(5, 5, 2, 5), -1, 10));
        appPanel.add(directoryPanel, BorderLayout.NORTH);
        lsmDirectoryLabel = new JLabel();
        lsmDirectoryLabel.setText("Source File Directory:");
        directoryPanel.add(lsmDirectoryLabel, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                                                                  GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                  GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(74, 14), null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Output Directory:");
        directoryPanel.add(label1, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lsmDirectoryBtn = new JButton();
        lsmDirectoryBtn.setHorizontalAlignment(0);
        lsmDirectoryBtn.setText("Set");
        lsmDirectoryBtn.setToolTipText("Change Source File Directory");
        directoryPanel.add(lsmDirectoryBtn, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(20, 25), null, 0, false));
        outputDirectoryBtn = new JButton();
        outputDirectoryBtn.setText("Set");
        outputDirectoryBtn.setToolTipText("Change Output Directory");
        directoryPanel.add(outputDirectoryBtn, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(20, 25), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Project:");
        directoryPanel.add(label2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cancelInputSearch = new JButton();
        cancelInputSearch.setText("Cancel");
        cancelInputSearch.setToolTipText("Cancel Source File Search");
        cancelInputSearch.setVisible(false);
        directoryPanel.add(cancelInputSearch, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(20, 25), null, 0, false));
        outputDirectoryPane = new JScrollPane();
        outputDirectoryPane.setHorizontalScrollBarPolicy(31);
        outputDirectoryPane.setVerticalScrollBarPolicy(21);
        directoryPanel.add(outputDirectoryPane, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                                                    GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                    GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                    GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                    GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        outputDirectoryField = new JTextArea();
        outputDirectoryField.setBackground(UIManager.getColor("Label.background"));
        outputDirectoryField.setEditable(false);
        outputDirectoryField.setLineWrap(true);
        outputDirectoryField.setRequestFocusEnabled(false);
        outputDirectoryField.setWrapStyleWord(true);
        outputDirectoryPane.setViewportView(outputDirectoryField);
        lsmDirectoryPane = new JScrollPane();
        lsmDirectoryPane.setHorizontalScrollBarPolicy(31);
        lsmDirectoryPane.setVerticalScrollBarPolicy(21);
        directoryPanel.add(lsmDirectoryPane, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                                                 GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                 GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                 GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                 GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        lsmDirectoryField = new JTextArea();
        lsmDirectoryField.setBackground(UIManager.getColor("Label.background"));
        lsmDirectoryField.setEditable(false);
        lsmDirectoryField.setLineWrap(true);
        lsmDirectoryField.setRequestFocusEnabled(false);
        lsmDirectoryField.setWrapStyleWord(true);
        lsmDirectoryPane.setViewportView(lsmDirectoryField);
        projectNamePane = new JScrollPane();
        projectNamePane.setHorizontalScrollBarPolicy(31);
        projectNamePane.setVerticalScrollBarPolicy(21);
        directoryPanel.add(projectNamePane, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                                                GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        projectName = new JTextArea();
        projectName.setBackground(UIManager.getColor("Label.background"));
        projectName.setEditable(false);
        projectName.setLineWrap(true);
        projectName.setRows(0);
        projectName.setWrapStyleWord(true);
        projectNamePane.setViewportView(projectName);
        dataPanel = new JPanel();
        dataPanel.setLayout(new GridLayoutManager(2, 1, new Insets(3, 5, 5, 5), -1, -1));
        appPanel.add(dataPanel, BorderLayout.CENTER);
        dataButtonPanel = new JPanel();
        dataButtonPanel.setLayout(new GridLayoutManager(1, 6, new Insets(5, 0, 5, 0), -1, -1));
        dataPanel.add(dataButtonPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                           GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                           GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        copyAndRenameBtn = new JButton();
        copyAndRenameBtn.setEnabled(false);
        copyAndRenameBtn.setText("Copy and Rename");
        copyAndRenameBtn.setToolTipText("Copy and rename all files using specified information");
        dataButtonPanel.add(copyAndRenameBtn, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        dataButtonPanel.add(spacer1, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        dataButtonPanel.add(spacer2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, 1, null, new Dimension(10, -1), null, 0, false));
        copyProgressLabel = new JLabel();
        copyProgressLabel.setText("Label");
        dataButtonPanel.add(copyProgressLabel, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        copyProgressBar = new JProgressBar();
        copyProgressBar.setEnabled(true);
        dataButtonPanel.add(copyProgressBar, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        loadMappedDataButton = new JButton();
        loadMappedDataButton.setEnabled(false);
        loadMappedDataButton.setText("Load Mapped Data");
        loadMappedDataButton.setVisible(false);
        dataButtonPanel.add(loadMappedDataButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                                                      GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dataTableScrollPane = new JScrollPane();
        dataPanel.add(dataTableScrollPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                               GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                               GridConstraints.SIZEPOLICY_WANT_GROW,
                                                               GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                               GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        dataTableScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(), null));
        dataTable = new DataTable();
        dataTable.setAutoResizeMode(2);
        dataTableScrollPane.setViewportView(dataTable);
        label1.setLabelFor(outputDirectoryPane);
    }

    /** @noinspection ALL */
    public JComponent $$$getRootComponent$$$() {
        return appPanel;
    }
}