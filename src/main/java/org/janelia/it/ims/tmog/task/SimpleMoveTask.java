/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.task;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.DataTableModel;
import org.janelia.it.ims.tmog.config.FileTransferConfiguration;
import org.janelia.it.ims.tmog.config.output.OutputDirectoryConfiguration;
import org.janelia.it.utils.filexfer.FileCopyFailedException;

import java.io.File;
import java.io.IOException;

/**
 * This task should only be used as an optimized way to move files
 * from one place to another on the <u>SAME</u> file system.
 * It uses the much faster but less flexible
 * {@link File#renameTo(java.io.File)} method to move files.
 * Because this task does not calculate or log a file digest,
 * the task should only be used when a digest has already been logged by
 * an earlier process (or is simply not needed).
 *
 * @author Eric Trautman
 */
public class SimpleMoveTask
        extends RenameTask {

    /** The name of the task supported by this view. */
    public static final String TASK_NAME = "simple-move";

    /**
     * Constructs a new task.
     *
     * @param model                       data model for this session.
     * @param outputDirConfig             the output directory configuration.
     * @param fileTransferConfig          the file transfer configuration.
     * @param sessionOutputDirectoryName  the session output directory name
     *                                    (for session derived configurations).
     */
    public SimpleMoveTask(DataTableModel model,
                          OutputDirectoryConfiguration outputDirConfig,
                          FileTransferConfiguration fileTransferConfig,
                          String sessionOutputDirectoryName) {
        super(model,
              outputDirConfig,
              fileTransferConfig,
              sessionOutputDirectoryName);
    }

    @Override
    protected void transferFile(File rowFile,
                                File renamedFile)
            throws IOException, FileCopyFailedException {

        final String fromFileToFile = getFromFileToFileText(rowFile,
                                                            renamedFile);
        LOG.info("starting move of " + fromFileToFile);

        File parent = renamedFile.getParentFile();
        if (parent != null) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }

        final boolean renameSucceeded = rowFile.renameTo(renamedFile);
        if (renameSucceeded) {
            handleSuccessfulMove(rowFile, renamedFile);
        } else {
            throw new IOException("failed to move " + fromFileToFile);
        }
    }

    @Override
    protected void cleanupFiles(File rowFile,
                                File renamedFile,
                                boolean isSuccessful,
                                boolean isOverwriteRequiredForRename) {
        if (isSuccessful) {

            appendToSummary("moved ");

        } else {

            appendToSummary("ERROR: failed to move ");

            // try to move the renamed file back if it exists
            if ((renamedFile != null) && renamedFile.exists()) {
                final boolean moveSucceeded = renamedFile.renameTo(rowFile);
                String statusMessage;
                if (moveSucceeded) {
                    statusMessage = "successfully moved ";
                } else {
                    statusMessage = "FAILED to move ";
                }
                LOG.error(statusMessage + renamedFile.getAbsolutePath() +
                          " back to " + rowFile.getAbsolutePath() +
                          " after processing failed");
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
        return "Moved the following files from ";
    }

    protected void handleSuccessfulMove(File rowFile,
                                        File renamedFile) {
        LOG.info("Successfully moved " + getFromFileToFileText(rowFile,
                                                               renamedFile));
    }

    private String getFromFileToFileText(File rowFile,
                                           File renamedFile) {
        return rowFile.getAbsolutePath() + " to " +
               renamedFile.getAbsolutePath();
    }

    private static final Logger LOG = Logger.getLogger(SimpleMoveTask.class);
}