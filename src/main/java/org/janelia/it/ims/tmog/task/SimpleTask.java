/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.task;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.DataTableModel;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowListener;
import org.janelia.it.ims.tmog.plugin.SessionListener;
import org.janelia.it.ims.tmog.target.Target;
import org.janelia.it.utils.LoggingUtils;
import org.jdesktop.swingworker.SwingWorker;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides the core methods to support background execution of a
 * task that is dependent upon {@link DataTableModel} information.
 *
 * @author Eric Trautman
 */
public class SimpleTask extends SwingWorker<Void, TaskProgressInfo> implements Task {

    /** The logger for this class. */
    private static final Logger LOG = Logger.getLogger(SimpleTask.class);

    /** The data model for this task. */
    private DataTableModel model;

    /** A text summary of what this task accomplished. */
    private StringBuilder taskSummary;

    /** List of index numbers for rows that failed to be processed. */
    private List<Integer> failedRowIndices;

    /** List of listeners registered for notification of copy events. */
    private List<RowListener> rowListenerList;

    /** List of listeners registered for notification of session events. */
    private List<SessionListener> sessionListenerList;

    private boolean isSessionCancelled;

    /**
     * Constructs a new task.
     *
     * @param  model  data model for this task.
     */
    public SimpleTask(DataTableModel model) {
        this.model = model;
        this.failedRowIndices = new ArrayList<Integer>();
        this.rowListenerList = new ArrayList<RowListener>();
        this.sessionListenerList = new ArrayList<SessionListener>();
        this.taskSummary = new StringBuilder();
    }

    /**
     * @return the data model for this task.
     */
    public DataTableModel getModel() {
        return model;
    }

    /**
     * @return a text summary of what this task accomplished.
     */
    public String getTaskSummary() {
        return taskSummary.toString();
    }

    /**
     * Appends a string representation of the specified object to this
     * task's text summary.
     *
     * @param  o  object to append.
     */
    protected void appendToSummary(Object o) {
        taskSummary.append(o);
    }

    /**
     * Appends the error message from the original (root cause) of the specified
     * {@link java.lang.Throwable} to this task's text summary.
     *
     * @param  t  throwable with message.
     */
    protected void appendOriginalErrorMessageToSummary(Throwable t) {
        Throwable original = t;
        Throwable cause = original.getCause();
        while (cause != null) {
            original = cause;
            cause = original.getCause();
        }
        taskSummary.append("ERROR: ");
        taskSummary.append(original.getMessage());
        taskSummary.append('\n');
    }

    /**
     * @return list of index numbers for rows that failed to be processed.
     */
    public List<Integer> getFailedRowIndices() {
        return failedRowIndices;
    }

    /**
     * Registers the specified listener for row processing event
     * notifications during task processing.
     *
     * @param  listener  listener to be notified.
     */
    public void addRowListener(RowListener listener) {
        rowListenerList.add(listener);
    }

    /**
     * Registers the specified listener for session event notifications during
     * after the task completes.
     *
     * @param listener listener to be notified.
     */
    public void addSessionListener(SessionListener listener) {
        sessionListenerList.add(listener);
    }

    /**
     * Marks this task for cancellation but does not immediately
     * stop processing.
     */
    public void cancelSession() {
        isSessionCancelled = true;
    }

    /**
     * @return true if the task session has been cancelled; otherwise false.
     */
    public boolean isSessionCancelled() {
        return isSessionCancelled;
    }

    /**
     * Executes the task process (and plug-in processes) in a background
     * thread so that long processes do not block the event dispatching thread.
     */
    @Override
    public Void doInBackground() {
        LoggingUtils.setLoggingContext();
        LOG.debug("starting task");

        try {
            if (isSessionCancelled()) {
                LOG.warn("Session cancelled before start.");
                taskSummary.append("Session cancelled before start.");
                markAllRowsAsFailed();
            } else {
                final boolean startSessionNotificationsSuccessfullyCompleted =
                        startSession();
                if (startSessionNotificationsSuccessfullyCompleted) {
                    processRows();
                } else {
                    markAllRowsAsFailed();
                }
            }

            endSession();

            LOG.debug("finished task");
        } catch (Throwable t) {
            // ensure errors that occur in this thread are not lost
            LOG.error("unexpected exception in background task", t);
        }
        return null;
    }

    /**
     * This method returns a plug-in data row for the current model row.
     * It can be overriden to support extended plug-in data models.
     *
     * @param  modelRow  the current row being processed.
     *
     * @return a plug-in data row for the current model row.
     */
    protected PluginDataRow getPluginDataRow(DataRow modelRow) {
        return new PluginDataRow(modelRow);
    }

    /**
     * This method returns a task progress object for the specified row.
     * It can be overriden to customize reported progress information.
     *
     * @param  lastRowProcessed    index of last proceessed row (zero based).
     * @param  totalRowsToProcess  total number of rows being processed.
     * @param  modelRow            the current row being processed.
     *
     * @return a task progress object for the specified row.
     */
    protected TaskProgressInfo getProgressInfo(int lastRowProcessed,
                                               int totalRowsToProcess,
                                               DataRow modelRow) {

        @SuppressWarnings("StringBufferReplaceableByString")
        StringBuilder sb = new StringBuilder();
        sb.append("processing ");
        sb.append((lastRowProcessed + 1));
        sb.append(" of ");
        sb.append(totalRowsToProcess);
        sb.append(": ");
        sb.append(modelRow.getTarget().getName());
        int pctComplete = (int)
                (100 *
                 ((double) lastRowProcessed / (double) totalRowsToProcess));

        return new TaskProgressInfo(lastRowProcessed,
                                    totalRowsToProcess,
                                    pctComplete,
                                    sb.toString());
    }

    /**
     * This method performs the core task process for the specified row.
     * It's default implementation here does nothing, but can be overriden
     * as needed.
     *
     * @param  modelRow            the current row being processed.
     *
     * @return true if the processing completes successfully; otherwise false.
     */
    protected boolean processRow(DataRow modelRow) {
        return true;
    }

    /**
     * This method performs any "clean-up" operations that are required
     * after core processing (see {@link #processRow}) has completed and
     * all row listeners have been notified.  It can be overriden as needed.
     *
     * @param  modelRow            the current row being processed.
     *
     * @param  isSuccessful        true if the row was processed successfully
     *                             and all listeners completed their processing
     *                             successfully; otherwise false.
     */
    protected void cleanupRow(DataRow modelRow,
                              boolean isSuccessful) {
        if (isSuccessful) {
            appendToSummary("Saved data for ");
        } else {
            appendToSummary("ERROR: Failed to save data for ");
        }

        Target target = modelRow.getTarget();
        if (target != null) {
            appendToSummary(target.getName());
        }
        appendToSummary("\n");
    }

    /**
     * Notifies registered {@link java.beans.PropertyChangeListener} objects
     * about task progress information.
     * The {@link java.beans.PropertyChangeEvent} generated by this method
     * will have the name {@link #PROGRESS_PROPERTY} and a new value that
     * is an ordered {@link List} of {@link TaskProgressInfo} objects.
     * This method runs in the event dispatcher thread.
     *
     * @param  list  list of progress information objects for display.
     */
    @Override
    protected void process(List<TaskProgressInfo> list) {
        firePropertyChange(PROGRESS_PROPERTY, null, list);
    }

    /**
     * Notifies registered {@link java.beans.PropertyChangeListener} objects
     * that the task has completed.
     * The {@link java.beans.PropertyChangeEvent} generated by this method
     * will have the name {@link #COMPLETION_PROPERTY} and a new value that
     * is this task (for querying processing results).
     * This method runs in the event dispatcher thread.
     */
    @Override
    public void done() {
        firePropertyChange(COMPLETION_PROPERTY, null, this);
    }

    /**
     * Processes each data row, handling the common functions of notifying
     * various listeners of progress and of updating failure information.
     * <p>
     * The {@link #getPluginDataRow}, {@link #getProgressInfo},
     * {@link #processRow}, and {@link #cleanupRow} methods can be
     * overriden by sub-classes to support customized task behavior
     * within this method's basic processing flow.
     * </p>
     */
    private void processRows() {

        DataTableModel model = getModel();
        List<DataRow> modelRows = model.getRows();

        int rowIndex = 0;
        final int numberOfRows = modelRows.size();

        boolean isStartRowNotificationSuccessful;
        boolean isRowProcessingSuccessful;
        PluginDataRow pluginDataRow;
        TaskProgressInfo progressInfo;

        for (DataRow modelRow : modelRows) {

            isStartRowNotificationSuccessful = false;
            isRowProcessingSuccessful = false;

            pluginDataRow = getPluginDataRow(modelRow);
            try {
                pluginDataRow =
                        notifyRowListeners(RowListener.EventType.START_ROW,
                                           pluginDataRow);
                isStartRowNotificationSuccessful = true;
            } catch (Exception e) {
                LOG.error("Failed external start processing for " +
                          pluginDataRow, e);
            }

            if (isStartRowNotificationSuccessful) {
                progressInfo = getProgressInfo(rowIndex,
                                               numberOfRows,
                                               modelRow);
                publish(progressInfo);
                isRowProcessingSuccessful = processRow(modelRow);

                // notify any listeners
                try {
                    if (isRowProcessingSuccessful) {
                        notifyRowListeners(
                                RowListener.EventType.END_ROW_SUCCESS,
                                pluginDataRow);
                    } else {
                        notifyRowListeners(
                                RowListener.EventType.END_ROW_FAIL,
                                pluginDataRow);
                    }
                } catch (Exception e) {
                    LOG.error("Failed external completion processing for " +
                              pluginDataRow, e);
                    isRowProcessingSuccessful = false;
                }

            }

            if (! isRowProcessingSuccessful) {
                addFailedRowIndex(rowIndex);
            }

            cleanupRow(modelRow, isRowProcessingSuccessful);

            rowIndex++;

            if (isSessionCancelled()) {
                handleCancelOfSession(rowIndex,
                                      numberOfRows,
                                      modelRow.getTarget());
                break;
            }
        }
    }

    /**
     * Utility method to notify registered listeners about a row event.
     *
     * @param eventType the current event type.
     * @param row       the data associated with the event.
     *
     * @return the (possibly) updated row data.
     *
     * @throws ExternalDataException
     *   if a listener detects a data error.
     * @throws ExternalSystemException
     *   if a system error occurs within a listener.
     */
    private PluginDataRow notifyRowListeners(RowListener.EventType eventType,
                                             PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {
        for (RowListener listener : rowListenerList) {
            row = listener.processEvent(eventType, row);
        }
        return row;
    }

    /**
     * Notifies registered listeners that the session has started.
     *
     * @return true if all notifications were successfully sent;
     *         otherwise false.
     */
    private boolean startSession() {
        boolean allNotificationsProcessedSuccessfully = false;
        DataTableModel model = getModel();
        List<DataRow> modelRows = model.getRows();

        try {
            for (SessionListener listener : sessionListenerList) {
                listener.startSession(modelRows);
            }
            allNotificationsProcessedSuccessfully = true;
        } catch (Exception e) {
            LOG.error("session listener startSession processing failed", e);
            taskSummary.append(e.getMessage());
        }
        return allNotificationsProcessedSuccessfully;
    }

    /**
     * Notifies registered listeners that the session has ended.
     */
    private void endSession() {
        final String message = taskSummary.toString();
        try {
            for (SessionListener listener : sessionListenerList) {
                listener.endSession(message);
            }
        } catch (Exception e) {
            LOG.error("session listener endSession processing failed, " +
                      "taskSummary is " + message, e);
        }
    }

    /**
     * Adds the specified index to the list of failed rows for this task.
     *
     * @param  index  index to add.
     */
    private void addFailedRowIndex(Integer index) {
        failedRowIndices.add(index);
    }

    /**
     * Handles clean up needed if the session is cancelled before
     * all targets have been processed.
     *
     * @param  rowIndex      the row index for the last processed target.
     * @param  numberOfRows  the total number targets being processed.
     * @param  target        the last target processed.
     */
    private void handleCancelOfSession(int rowIndex,
                                       int numberOfRows,
                                       Target target) {
        if (rowIndex < numberOfRows) {
            LOG.warn("Session cancelled after processing " +
                     target.getName() + ".");
            taskSummary.append("\nSession cancelled.");

            // mark all remaining rows as failed
            for (int i = rowIndex; i < numberOfRows; i++) {
                failedRowIndices.add(i);
            }
        }
    }

    private void markAllRowsAsFailed() {
        List<DataRow> modelRows = model.getRows();
        int numberOfRows = modelRows.size();
        for (int i = 0; i < numberOfRows; i++) {
            failedRowIndices.add(i);
        }
    }
}