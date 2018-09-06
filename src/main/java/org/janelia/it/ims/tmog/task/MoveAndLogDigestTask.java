/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.task;

import org.janelia.it.ims.tmog.DataTableModel;
import org.janelia.it.ims.tmog.config.FileTransferConfiguration;
import org.janelia.it.ims.tmog.config.output.OutputDirectoryConfiguration;
import org.janelia.it.utils.filexfer.SafeFileTransfer;

import java.io.File;

/**
 * This task should only be used as an optimized way to move files
 * from one place to another on the <u>SAME</u> file system.
 * It uses the much faster but less flexible
 * {@link java.io.File#renameTo(java.io.File)} method to move files.
 * After moving each file, this task calculates and logs the file digest/hash.
 *
 * @author Eric Trautman
 */
public class MoveAndLogDigestTask
        extends SimpleMoveTask {

    /** The name of the task supported by this view. */
    public static final String TASK_NAME = "move-and-log-digest";

    /**
     * Constructs a new task.
     *
     * @param model                       data model for this session.
     * @param outputDirConfig             the output directory configuration.
     * @param fileTransferConfig          the file transfer configuration.
     * @param sessionOutputDirectoryName  the session output directory name
     *                                    (for session derived configurations).
     */
    public MoveAndLogDigestTask(DataTableModel model,
                                OutputDirectoryConfiguration outputDirConfig,
                                FileTransferConfiguration fileTransferConfig,
                                String sessionOutputDirectoryName) {
        super(model,
              outputDirConfig,
              fileTransferConfig,
              sessionOutputDirectoryName);
    }

    @Override
    protected void handleSuccessfulMove(File rowFile,
                                        File renamedFile) {
        final long startTime = System.currentTimeMillis();
        final byte[] digest = SafeFileTransfer.getDigest(renamedFile);
        final int elapsedSeconds =
                ((int) (System.currentTimeMillis() - startTime)) / 1000;
        SafeFileTransfer.logTransferStats("moved",
                                          digest,
                                          rowFile,
                                          renamedFile,
                                          0,
                                          elapsedSeconds);
    }
        
}