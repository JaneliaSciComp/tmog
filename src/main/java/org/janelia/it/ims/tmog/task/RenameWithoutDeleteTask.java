/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.task;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.DataTableModel;
import org.janelia.it.ims.tmog.config.FileTransferConfiguration;
import org.janelia.it.ims.tmog.config.output.OutputDirectoryConfiguration;

import java.io.File;

/**
 * This is simply an extension of the rename task that does not delete
 * source files after they have been renamed.
 *
 * @author Eric Trautman
 */
public class RenameWithoutDeleteTask extends RenameTask {

    private static final Logger LOG =
            Logger.getLogger(RenameWithoutDeleteTask.class);

    /** The name of the task supported by this view. */
    public static final String TASK_NAME = "rename-without-delete";

    /**
     * Constructs a new task.
     *
     * @param model                       data model for this rename session.
     * @param outputDirConfig             the output directory configuration.
     * @param fileTransferConfig          the file transfer configuration.
     * @param sessionOutputDirectoryName  the session output directory name
     *                                    (for session derived configurations).
     */
    public RenameWithoutDeleteTask(DataTableModel model,
                                   OutputDirectoryConfiguration outputDirConfig,
                                   FileTransferConfiguration fileTransferConfig,
                                   String sessionOutputDirectoryName) {
        super(model,
              outputDirConfig,
              fileTransferConfig,
              sessionOutputDirectoryName);
    }

    @Override
    protected void cleanupFiles(File rowFile,
                                File renamedFile,
                                boolean isSuccessful,
                                boolean isOverwriteRequiredForRename) {

        if (isSuccessful) {

            appendToSummary("copied ");

        } else {

            appendToSummary("ERROR: failed to copy ");

            // clean up the copied file if it exists and
            // it isn't the same as the source file
            if ((renamedFile != null) &&
                renamedFile.exists() &&
                (! renamedFile.equals(rowFile)) &&
                (! isOverwriteRequiredForRename)) {
                LOG.warn("Removing " + renamedFile.getAbsolutePath() +
                         " after copy processing failed.");
                deleteFile(renamedFile, "failed");
            }
        }

        appendToSummary(rowFile.getName());

        if (renamedFile != null) {
            appendToSummary(" to ");
            appendToSummary(renamedFile.getAbsolutePath());
        }

        appendToSummary("\n");
    }

    @Override
    protected String getSummaryHeader() {
        return "Copied the following files from ";
    }
}