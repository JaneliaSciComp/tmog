/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.task;

import org.janelia.it.ims.tmog.DataTableModel;
import org.janelia.it.ims.tmog.plugin.RowListener;
import org.janelia.it.ims.tmog.plugin.SessionListener;

import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * This interface identifies the methods required for for all
 * data model tasks.
 *
 * @author Eric Trautman
 */
public interface Task extends Runnable {

    /** The name for the task progress property. */
    public static final String PROGRESS_PROPERTY = "TaskProgressUpdate";

    /** The name for the task completion property. */
    public static final String COMPLETION_PROPERTY = "TaskComplete";

    /**
     * @return the data model for this task.
     */
    public DataTableModel getModel();

    /**
     * @return a text summary of what this task accomplished.
     */
    public String getTaskSummary();

    /**
     * @return list of index numbers for rows that failed to be processed.
     */
    public List<Integer> getFailedRowIndices();

    /**
     * Registers the specified listener for row processing event
     * notifications during task processing.
     *
     * @param  listener  listener to be notified.
     */
    public void addRowListener(RowListener listener);

    /**
     * Registers the specified listener for session event notifications during
     * after the task completes.
     *
     * @param listener listener to be notified.
     */
    public void addSessionListener(SessionListener listener);

    /**
     * Marks this task for cancellation but does not immediately
     * stop processing.
     */
    public void cancelSession();

    /**
     * @return true if the task session has been cancelled; otherwise false.
     */
    public boolean isSessionCancelled();

    /**
     * Adds a {@code PropertyChangeListener} to the listener list.
     *
     * @param listener the {@code PropertyChangeListener} to be added
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Removes a {@code PropertyChangeListener} from the listener list.
     *
     * @param listener the {@code PropertyChangeListener} to be removed
     */
    public void removePropertyChangeListener(PropertyChangeListener listener);

}
