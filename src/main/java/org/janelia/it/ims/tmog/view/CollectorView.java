/*
 * Copyright (c) 2017 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.view;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.DataTableModel;
import org.janelia.it.ims.tmog.config.ProjectConfiguration;
import org.janelia.it.ims.tmog.config.preferences.ColumnDefaultSet;
import org.janelia.it.ims.tmog.config.preferences.TransmogrifierPreferences;
import org.janelia.it.ims.tmog.config.preferences.ViewDefault;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowUpdater;
import org.janelia.it.ims.tmog.plugin.RowValidator;
import org.janelia.it.ims.tmog.target.FileTarget;
import org.janelia.it.ims.tmog.task.SimpleTask;
import org.janelia.it.ims.tmog.task.Task;
import org.janelia.it.ims.tmog.view.component.DataTable;
import org.janelia.it.ims.tmog.view.component.NarrowOptionPane;
import org.janelia.it.ims.tmog.view.component.SessionIcon;
import org.janelia.it.ims.tmog.view.component.TaskButtonText;
import org.janelia.it.ims.tmog.view.component.TaskComponents;

/**
 * This class manages the main or overall view for collecting data for a set of files in a particular directory.
 *
 * @author Eric Trautman
 */
public class CollectorView
        implements SessionView, InputSelectionView {

    /** The name of the task supported by this view. */
    public static final String TASK_NAME = "collector";

    /** The name of the task supported by this view. */
    public static final String SAGE_TASK_NAME = "sage-collector";

    private JPanel appPanel;
    @SuppressWarnings({"UnusedDeclaration"})
    private JPanel directoryPanel;
    @SuppressWarnings({"UnusedDeclaration"})
    private JPanel dataPanel;
    private JScrollPane dataTableScrollPane;
    @SuppressWarnings({"UnusedDeclaration"})
    private JPanel dataButtonPanel;
    private JScrollPane projectNamePane;
    private JTextArea projectName;
    private JScrollPane rootDirectoryPane;
    private JButton rootDirectoryBtn;
    private JLabel rootDirectoryLabel;
    private JTextArea rootDirectoryField;
    private JButton saveBtn;
    private JLabel taskProgressLabel;
    private JProgressBar taskProgressBar;
    private DataTable dataTable;
    private JButton cancelTargetWorkerButton;
    private JButton loadMappedDataButton;

    private String sessionName;
    private ProjectConfiguration projectConfig;
    private File defaultDirectory;
    private InputSelectionHandler inputSelectionHandler;
    private DataTableModel tableModel;
    private SimpleTask task;
    private TaskComponents taskComponents;
    private String projectNameText;


    public CollectorView(String sessionName,
                         ProjectConfiguration projectConfig,
                         File defaultDirectory,
                         boolean isSageCollector,
                         JTabbedPane parentTabbedPane) {
        this.sessionName = sessionName;
        this.projectConfig = projectConfig;
        this.defaultDirectory = defaultDirectory;
        this.projectNameText = projectConfig.getName();
        this.projectName.setText(projectNameText);

        if (isSageCollector) {
            throw new UnsupportedOperationException("SAGE collector is not supported by external distribution");
        } else {
            this.inputSelectionHandler =
                    new FileInputSelectionHandler(projectConfig,
                                                  defaultDirectory,
                                                  rootDirectoryLabel,
                                                  rootDirectoryField,
                                                  rootDirectoryBtn,
                                                  cancelTargetWorkerButton,
                                                  JFileChooser.DIRECTORIES_ONLY,
                                                  "Select Root Directory",
                                                  this);
            rootDirectoryBtn.setToolTipText("Change Root Directory");
        }

        projectNamePane.setBorder(null);
        rootDirectoryPane.setBorder(null);

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

    /**
     * @return the primary content panel (container) for the view.
     */
    public JPanel getPanel() {
        return appPanel;
    }

    /**
     * @return the default directory for the view (used to default file chooser dialogs).
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
        saveBtn.setEnabled(false);
        loadMappedDataButton.setEnabled(false);
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

    private boolean validateAllFields() {
        boolean isValid = tableModel.verify();

        // only perform external validation if internal validation succeeds
        if (isValid) {

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
            } catch (Exception e) {
                isValid = false;
                dataTable.displayErrorDialog(e.getMessage());
            }

            // only perform row validation
            // if external start session call succeeded
            if (isValid) {
                for (DataRow row : rows) {
                    String externalErrorMsg = null;
                    try {
                        for (RowValidator validator : validators) {
                            validator.validate(sessionName,
                                               new PluginDataRow(row));
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
        if (loadMappedDataButton.isVisible() &&
            (loadMappedDataButton.isEnabled() != isTaskButtonEnabled)) {
            loadMappedDataButton.setEnabled(isTaskButtonEnabled);
        }

    }

    /** The logger for this class. */
    private static final Logger LOG = Logger.getLogger(CollectorView.class);

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
        appPanel.setPreferredSize(new Dimension(1280, 500));
        directoryPanel = new JPanel();
        directoryPanel.setLayout(new GridLayoutManager(3, 7, new Insets(0, 0, 0, 0), -1, -1));
        appPanel.add(directoryPanel, BorderLayout.NORTH);
        rootDirectoryLabel = new JLabel();
        rootDirectoryLabel.setText("Root Directory:");
        directoryPanel.add(rootDirectoryLabel, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                                                                   GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                   GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(74, 14), null, 0, false));
        final Spacer spacer1 = new Spacer();
        directoryPanel.add(spacer1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, 1, new Dimension(10, -1), null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        directoryPanel.add(spacer2, new GridConstraints(2, 6, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, new Dimension(10, -1), null, null, 0, false));
        rootDirectoryBtn = new JButton();
        rootDirectoryBtn.setHorizontalAlignment(0);
        rootDirectoryBtn.setText("Set");
        directoryPanel.add(rootDirectoryBtn, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(20, 25), null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Project:");
        directoryPanel.add(label1, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        directoryPanel.add(spacer3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 10), null, 0, false));
        cancelTargetWorkerButton = new JButton();
        cancelTargetWorkerButton.setHorizontalAlignment(0);
        cancelTargetWorkerButton.setText("Cancel");
        cancelTargetWorkerButton.setToolTipText("Cancel Source File Search");
        cancelTargetWorkerButton.setVisible(false);
        directoryPanel.add(cancelTargetWorkerButton, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(20, 25), null, 0, false));
        rootDirectoryPane = new JScrollPane();
        rootDirectoryPane.setHorizontalScrollBarPolicy(31);
        rootDirectoryPane.setVerticalScrollBarPolicy(21);
        directoryPanel.add(rootDirectoryPane, new GridConstraints(2, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                                                  GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                  GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                  GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                  GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        rootDirectoryField = new JTextArea();
        rootDirectoryField.setBackground(UIManager.getColor("Label.background"));
        rootDirectoryField.setEditable(false);
        rootDirectoryField.setLineWrap(true);
        rootDirectoryField.setWrapStyleWord(true);
        rootDirectoryPane.setViewportView(rootDirectoryField);
        projectNamePane = new JScrollPane();
        projectNamePane.setHorizontalScrollBarPolicy(31);
        projectNamePane.setVerticalScrollBarPolicy(21);
        directoryPanel.add(projectNamePane, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                                                GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        projectName = new JTextArea();
        projectName.setBackground(UIManager.getColor("Label.background"));
        projectName.setEditable(false);
        projectName.setLineWrap(true);
        projectName.setWrapStyleWord(true);
        projectNamePane.setViewportView(projectName);
        dataPanel = new JPanel();
        dataPanel.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
        appPanel.add(dataPanel, BorderLayout.CENTER);
        final Spacer spacer4 = new Spacer();
        dataPanel.add(spacer4, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, 1, null, new Dimension(10, -1), null, 0, false));
        final Spacer spacer5 = new Spacer();
        dataPanel.add(spacer5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, 1, null, new Dimension(10, -1), null, 0, false));
        final Spacer spacer6 = new Spacer();
        dataPanel.add(spacer6, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 10), null, 0, false));
        dataButtonPanel = new JPanel();
        dataButtonPanel.setLayout(new GridLayoutManager(3, 6, new Insets(0, 0, 0, 0), -1, -1));
        dataPanel.add(dataButtonPanel, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                           GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                           GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        saveBtn = new JButton();
        saveBtn.setEnabled(false);
        saveBtn.setText("Save");
        saveBtn.setToolTipText("Save specified information to data store.");
        dataButtonPanel.add(saveBtn, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer7 = new Spacer();
        dataButtonPanel.add(spacer7, new GridConstraints(1, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer8 = new Spacer();
        dataButtonPanel.add(spacer8, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(-1, 10), null, 0, false));
        final Spacer spacer9 = new Spacer();
        dataButtonPanel.add(spacer9, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(-1, 10), null, 0, false));
        final Spacer spacer10 = new Spacer();
        dataButtonPanel.add(spacer10, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, 1, null, new Dimension(10, -1), null, 0, false));
        taskProgressLabel = new JLabel();
        taskProgressLabel.setText("Label");
        dataButtonPanel.add(taskProgressLabel, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        taskProgressBar = new JProgressBar();
        taskProgressBar.setEnabled(true);
        dataButtonPanel.add(taskProgressBar, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        loadMappedDataButton = new JButton();
        loadMappedDataButton.setEnabled(false);
        loadMappedDataButton.setText("Load Mapped Data");
        loadMappedDataButton.setVisible(false);
        dataButtonPanel.add(loadMappedDataButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                                                      GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dataTableScrollPane = new JScrollPane();
        dataPanel.add(dataTableScrollPane, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                               GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                               GridConstraints.SIZEPOLICY_WANT_GROW,
                                                               GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                               GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        dataTableScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(), null));
        dataTable = new DataTable();
        dataTable.setAutoResizeMode(2);
        dataTableScrollPane.setViewportView(dataTable);
        final Spacer spacer11 = new Spacer();
        dataPanel.add(spacer11, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, 1, new Dimension(10, -1), null, null, 0, false));
    }

    /** @noinspection ALL */
    public JComponent $$$getRootComponent$$$() {
        return appPanel;
    }
}
