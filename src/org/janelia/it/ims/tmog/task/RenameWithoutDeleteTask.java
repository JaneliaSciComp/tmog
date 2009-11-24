/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.task;

import org.janelia.it.ims.tmog.DataTableModel;
import org.janelia.it.ims.tmog.config.output.OutputDirectoryConfiguration;

import java.io.File;

/**
 * This is simply an extension of the rename task that does not delete
 * source files after they have been renamed.
 *
 * @author Eric Trautman
 */
public class RenameWithoutDeleteTask extends RenameTask {

    /** The name of the task supported by this view. */
    public static final String TASK_NAME = "rename-without-delete";

    /**
     * Constructs a new task.
     *
     * @param model                       data model for this rename session.
     * @param outputDirConfig             the output directory configuration.
     * @param sessionOutputDirectoryName  the session output directory name
     *                                    (for session derived configurations).
     */
    public RenameWithoutDeleteTask(DataTableModel model,
                                   OutputDirectoryConfiguration outputDirConfig,
                                   String sessionOutputDirectoryName) {
        super(model, outputDirConfig, sessionOutputDirectoryName);
    }

    protected void deleteFile(File file,
                              String status) {
        // don't delete anything
    }
}