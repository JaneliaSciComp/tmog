/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.task;

/**
 * This class encapsulates task progress information.
 *
 * @author Eric Trautman
 */
public class TaskProgressInfo {

    private int lastRowProcessed;
    private int totalRowsToProcess;
    private int percentOfTaskCompleted;
    private String message;

    public TaskProgressInfo(int lastRowProcessed,
                            int totalRowsToProcess,
                            int percentOfTaskCompleted,
                            String message) {
        this.lastRowProcessed = lastRowProcessed;
        this.totalRowsToProcess = totalRowsToProcess;
        this.percentOfTaskCompleted = percentOfTaskCompleted;
        this.message = message;
    }

    public int getLastRowProcessed() {
        return lastRowProcessed;
    }

    public int getTotalRowsToProcess() {
        return totalRowsToProcess;
    }

    public int getPercentOfTaskCompleted() {
        return percentOfTaskCompleted;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return message;
    }
}