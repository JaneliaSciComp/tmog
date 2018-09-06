/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.view.component;

import org.janelia.it.ims.tmog.JaneliaTransmogrifier;
import org.janelia.it.ims.tmog.config.ProjectConfiguration;
import org.janelia.it.ims.tmog.plugin.RowListener;
import org.janelia.it.ims.tmog.plugin.SessionListener;
import org.janelia.it.ims.tmog.task.Task;
import org.janelia.it.ims.tmog.task.TaskProgressInfo;
import org.janelia.it.ims.tmog.view.TaskSummaryPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * This abstract "container" manages the group of components associated
 * with task execution.  Specific instances must provide implementations
 * of the {@link #getNewTask()}, {@link #isTaskReadyToStart()}, and
 * {@link #processTaskCompletion()} methods.
 *
 * @author Eric Trautman
 */
public abstract class TaskComponents implements PropertyChangeListener {

    private JButton executeButton;
    private JLabel progressLabel;
    private JProgressBar progressBar;
    private SessionIcon sessionIcon;
    private DataTable dataTable;

    private ProjectConfiguration projectConfig;
    private TaskButtonText buttonText;

    private boolean isTaskInProgress;
    private Task task;

    /**
     * Constructs a manager for the specified components.
     *
     * @param  dataTable      data table for the task.
     * @param  executeButton  task execution button.
     * @param  progressBar    task progress bar.
     * @param  progressLabel  task progress label.
     * @param  iconParent     session icon parent.
     * @param  projectConfig  session project configuration.
     * @param  buttonText     task execution button text.
     */
    protected TaskComponents(DataTable dataTable,
                             JButton executeButton,
                             JProgressBar progressBar,
                             JLabel progressLabel,
                             Component iconParent,
                             ProjectConfiguration projectConfig,
                             TaskButtonText buttonText) {
        this.executeButton = executeButton;
        this.progressBar = progressBar;
        this.progressLabel = progressLabel;
        this.sessionIcon = new SessionIcon(iconParent);

        this.dataTable = dataTable;

        this.projectConfig = projectConfig;
        this.buttonText = buttonText;

        executeButton.setEnabled(false);
        setTaskInProgress(false, true);

        executeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (isTaskInProgress) {
                    cancelTask();
                } else {
                    if (isTaskReadyToStart()) {
                        startTask();
                    }
                }
            }

        });

        progressBar.setVisible(false);
        progressLabel.setVisible(false);        
    }

    /**
     * @return the session's (task's) processing icon.
     */
    public SessionIcon getSessionIcon() {
        return sessionIcon;
    }

    /**
     * @return true if the task is in progress; otherwise false.
     */
    public boolean isTaskInProgress() {
        return isTaskInProgress;
    }

    /**
     * This method gets called when the task fires events.
     *
     * @param  evt  event describing the event source and the property
     *              that has changed.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        Object newValue = evt.getNewValue();
        if (Task.PROGRESS_PROPERTY.equals(propertyName)) {
            if (newValue instanceof java.util.List) {
                java.util.List list = (java.util.List) newValue;
                int size = list.size();
                if (size > 0) {
                    Object lastElement = list.get(size - 1);
                    if (lastElement instanceof TaskProgressInfo) {
                        TaskProgressInfo info = (TaskProgressInfo) lastElement;
                        dataTable.updateProgress(info);
                        updateProgress(info);
                    }
                }
            }

        } else if (Task.COMPLETION_PROPERTY.equals(propertyName)) {
            processTaskCompletion();
            setTaskInProgress(false, true);
        }
    }

    /**
     * @return a new task instance to be managed.
     */
    protected abstract Task getNewTask();

    /**
     * @return true if the task can be started
     *         (e.g. if all validation checks succeed); otherwise false.
     */
    protected abstract boolean isTaskReadyToStart();

    /**
     * Handle any user interface changes required once the task completes.
     */
    protected abstract void processTaskCompletion();

    private void startTask() {
        dataTable.setEnabled(false);
        task = getNewTask();
        task.addPropertyChangeListener(this);
        for (RowListener listener : projectConfig.getRowListeners()) {
            task.addRowListener(listener);
        }
        for (SessionListener listener :
                projectConfig.getSessionListeners()) {
            task.addSessionListener(listener);
        }
        setTaskInProgress(true, true);
        JaneliaTransmogrifier.submitTask(task);
    }

    private void cancelTask() {
        task.cancelSession();
        setTaskInProgress(false, false);
        executeButton.setText(buttonText.getCancelledText());
        executeButton.setToolTipText(buttonText.getCancelledToolTipText());
        executeButton.setEnabled(false);
    }

    private void setTaskInProgress(boolean taskInProgress,
                                   boolean updateIcon) {
        this.isTaskInProgress = taskInProgress;
        if (isTaskInProgress) {
            executeButton.setText(buttonText.getCancelText());
            executeButton.setToolTipText(buttonText.getCancelToolTipText());
            if (updateIcon) {
                sessionIcon.setToWait();
            }
            progressBar.setVisible(true);
            progressLabel.setVisible(true);        
        } else {
            executeButton.setText(buttonText.getStartText());
            executeButton.setToolTipText(buttonText.getStartToolTipText());
            if (updateIcon) {
                sessionIcon.setToEnterValues();
            }
            progressBar.setVisible(false);
            progressLabel.setVisible(false);
        }
    }

    private void updateProgress(TaskProgressInfo info) {

        int lastRowProcessed = info.getLastRowProcessed();
        if ((sessionIcon != null) && (lastRowProcessed == 0)) {
            // once the first copy has started, change tab icon
            sessionIcon.setToProcessing();
        }
        if (progressBar != null) {
            progressBar.setValue(info.getPercentOfTaskCompleted());
        }
        if (progressLabel != null) {
            progressLabel.setText(info.getMessage());
        }
    }

    /**
     * Utility to display task summary information in a dialog window.
     *
     * @param  titlePrefix             the window title prefix.
     * @param  numberOfFailures        the number of row failures that
     *                                 occurred dutring task execution.
     * @param  taskSummary             the task summary information to display.
     * @param  parent                  the dialog parent (used to size and
     *                                 position the dialog).
     */
    public static void displaySummaryDialog(String titlePrefix,
                                            int numberOfFailures,
                                            String taskSummary,
                                            Component parent) {

        StringBuilder dialogTitle = new StringBuilder();
        dialogTitle.append(titlePrefix);
        if (numberOfFailures > 0) {
            dialogTitle.append(" Summary (");
            dialogTitle.append(numberOfFailures);
            if (numberOfFailures > 1) {
                dialogTitle.append(" FAILURES!)");
            } else {
                dialogTitle.append(" FAILURE!)");
            }
        } else {
            dialogTitle.append(" Summary");
        }

        boolean isMinimized = false;
        final Component root = SwingUtilities.getRoot(parent);
        if (root instanceof JFrame) {
            final JFrame rootFrame = (JFrame) root;
            isMinimized = (rootFrame.getExtendedState() == JFrame.ICONIFIED);
        }

        // if the application window is minimized,
        if (isMinimized) {

            TaskSummaryPanel taskSummaryPanel = new TaskSummaryPanel(dialogTitle.toString(),
                                                                     taskSummary,
                                                                     parent);
            taskSummaryPanel.display();

        } else {

            NarrowOptionPane.displaySummaryDialog(dialogTitle.toString(),
                                                  taskSummary,
                                                  parent);
        }
    }

}