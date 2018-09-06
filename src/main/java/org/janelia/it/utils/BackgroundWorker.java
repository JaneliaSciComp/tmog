/*
* Copyright 2009 Howard Hughes Medical Institute.
* All rights reserved.
* Use is subject to Janelia Farm Research Center Software Copyright 1.0
* license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
*/

package org.janelia.it.utils;

import org.apache.log4j.Logger;
import org.jdesktop.swingworker.SwingWorker;

import java.beans.PropertyChangeEvent;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * This class supports task execution in a background thread.
 *
 * @author Eric Trautman
 */
public abstract class BackgroundWorker<T, V> extends SwingWorker<T, V> {

    /** The property name for progress updates. */
    public static final String IN_PROGRESS = "In Progress";

    /** The property name for completed operations. */
    public static final String STATE = "state";

    private ThreadPoolExecutor threadPoolExecutor;
    private Throwable failureCause;

    /**
     * @return the cause of failure for this task (or null if it did not fail).
     */
    public Throwable getFailureCause() {
        return failureCause;
    }

    /**
     * @return true if this task has failed; otherwise false.
     */
    public boolean hasFailed() {
        return failureCause != null;
    }

    /**
     * Sets the thread pool for this task (default behavior is to create
     * a new thread for each task instead of using a pool).
     *
     * @param  threadPoolExecutor  thread pool for execution of tasks.
     */
    public void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
    }

    /**
     * Submits this task for execution.
     */
    public void submitTask() {
        if (threadPoolExecutor == null) {
            Thread thread = new Thread(this);
            thread.start();
        } else {
            threadPoolExecutor.submit(this);
        }
    }

    /**
     * @param  event  change event to check.
     *
     * @return true if the specified event is a task progress notification;
     *         otherwise false.
     */
    public boolean isProgressEvent(PropertyChangeEvent event) {
        return IN_PROGRESS.equals(event.getPropertyName());
    }

    /**
     * @param  event  change event to check.
     *
     * @return true if the specified event is a task completion notification;
     *         otherwise false.
     */
    public boolean isDoneEvent(PropertyChangeEvent event) {
        return (STATE.equals(event.getPropertyName()) &&
                StateValue.DONE.equals(event.getNewValue()));
    }

    /**
     * Notifies all property change listeners
     * (see {@link #addPropertyChangeListener}) with the
     * specified list of progress messages.
     * This method runs in the event dispatcher thread.
     *
     * @param  list  list of progress information objects.
     */
    protected void process(List<V> list) {
        firePropertyChange(IN_PROGRESS, null, list);
    }

    /**
     * Executes this task in a background thread so
     * that the Event Dispatching thread is not blocked.
     *
     * @return the task execution results.
     */
    @Override
    protected T doInBackground() {
        T result = null;
        try {
            result = executeBackgroundOperation();
        } catch (Throwable t) {
            failureCause = t;
            LOG.error("task failed", t);
        }
        return result;
    }

    /**
     * Executes the operation in a background thread.
     *
     * @return the operation result.
     *
     * @throws Exception
     *   if any errors occur during processing.
     */
    protected abstract T executeBackgroundOperation() throws Exception;

    /** The logger for this class. */
    private static final Logger LOG = Logger.getLogger(BackgroundWorker.class);
}