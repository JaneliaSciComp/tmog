/*
 * Copyright 2008 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.task;

import org.apache.log4j.Logger;
import org.janelia.it.ims.imagerenamer.DataTableModel;
import org.janelia.it.ims.imagerenamer.DataTableRow;
import org.janelia.it.ims.imagerenamer.Target;
import org.janelia.it.ims.imagerenamer.plugin.ExternalDataException;
import org.janelia.it.ims.imagerenamer.plugin.ExternalSystemException;
import org.janelia.it.ims.imagerenamer.plugin.PluginDataRow;
import org.janelia.it.ims.imagerenamer.plugin.RowListener;
import org.janelia.it.ims.imagerenamer.plugin.SessionListener;
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
public class SimpleTask extends SwingWorker<Void, TaskProgressInfo> {

    /** The name for the task progress property. */
    public static final String PROGRESS_PROPERTY = "TaskProgressUpdate";

    /** The name for the task progress property. */
    public static final String COMPLETION_PROPERTY = "TaskComplete";

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
     * @return list of index numbers for rows that failed to be processed.
     */
    public List<Integer> getFailedRowIndices() {
        return failedRowIndices;
    }

    /**
     * Adds the specified index to the list of failed rows for this task.
     *
     * @param  index  index to add.
     */
    protected void addFailedRowIndex(Integer index) {
        failedRowIndices.add(index);
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
     * Executes the task process in a background thread so that long
     * processes do not block the event dispatching thread.
     */
    @Override
    public Void doInBackground() {
        LoggingUtils.setLoggingContext();
        LOG.debug("starting task");

        try {
            if (isSessionCancelled()) {
                LOG.warn("Session cancelled before start.");
                taskSummary.append("Session cancelled before start.");

                // mark all rows as failed
                List<DataTableRow> modelRows = model.getRows();
                int numberOfRows = modelRows.size();
                for (int i = 0; i < numberOfRows; i++) {
                    failedRowIndices.add(i);
                }
            } else {
                doTask();
            }

            // notify any session listeners
            try {
                notifySessionListeners(
                        SessionListener.EventType.END,
                        taskSummary.toString());
            } catch (Exception e) {
                LOG.error("session listener processing failed", e);
            }

            LOG.debug("finished task");
        } catch (Throwable t) {
            // ensure errors that occur in this thread are not lost
            LOG.error("unexpected exception in background task", t);
        }
        return null;
    }

    /**
     * Perform this task's core processing (in a background thread).
     * Implementations of this
     */
    protected void doTask() {
        // TODO: add standard notifications for row listeners
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
     * Handles clean up needed if the session is cancelled while files
     * are being renamed.
     *
     * @param  rowIndex      the row index for the last renamed file.
     * @param  numberOfRows  the total number files being renamed.
     * @param  target        the last target processed.
     */
    protected void handleCancelOfSession(int rowIndex,
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

    /**
     * Utility method to notify registered listeners about a copy event.
     *
     * @param eventType the current event type.
     * @param row       the rename data associated with the event.
     *
     * @return the (possibly) updated rename data.
     *
     * @throws ExternalDataException
     *   if a listener detects a data error.
     * @throws ExternalSystemException
     *   if a system error occurs within a listener.
     */
    protected PluginDataRow notifyRowListeners(RowListener.EventType eventType,
                                               PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {
        for (RowListener listener : rowListenerList) {
            row = listener.processEvent(eventType, row);
        }
        return row;
    }

    /**
     * Utility method to notify registered listeners about a session event.
     *
     * @param eventType the current event type.
     *
     * @param message   the event message.
     *
     * @throws ExternalDataException
     *   if a listener detects a data error.
     * @throws ExternalSystemException
     *   if a system error occurs within a listener.
     */
    private void notifySessionListeners(SessionListener.EventType eventType,
                                        String message)
            throws ExternalDataException, ExternalSystemException {
        for (SessionListener listener : sessionListenerList) {
            listener.processEvent(eventType, message);
        }
    }
}