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
import org.janelia.it.ims.tmog.config.FileTransferConfiguration;
import org.janelia.it.ims.tmog.config.output.OutputDirectoryConfiguration;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.RenamePluginDataRow;
import org.janelia.it.ims.tmog.target.Target;
import org.janelia.it.utils.filexfer.FileCopyFailedException;
import org.janelia.it.utils.filexfer.FileTransferUtil;
import org.janelia.it.utils.filexfer.SafeFileTransfer;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * This class supports the execution of the copy and rename process.
 *
 * @author Eric Trautman
 */
public class RenameTask extends SimpleTask {

    /**
     * The logger for this class.
     */
    private static final Logger LOG = Logger.getLogger(RenameTask.class);

    /**
     * The output directory configuration information for the project.
     */
    private OutputDirectoryConfiguration outputDirConfig;

    /**
     * The file transfer configuration information for the project.
     */
    private FileTransferConfiguration fileTransferConfig;

    /**
     * Utility for nio file transfers.
     */
    private FileTransferUtil fileTransferUtil;
    
    /**
     * The target directory for all copied files when the output configuration
     * indicates that the same directory should be used for all files
     * in a session.
     */
    private File sessionOutputDirectory;

    /**
     * The number of bytes in a programatically sized "chunk"
     * (used to report copy progress percentages).
     */
    private int bytesInChunk;

    /** The total number of byte "chunks" that need to be copied. */
    private long totalByteChunksToCopy;

    /** The number of "chunks" that have already been processed. */
    private long chunksProcessed;

    /** The current plugin data row being processed. */
    private RenamePluginDataRow currentRow;

    /**
     * Constructs a new task.
     *
     * @param model                       data model for this rename session.
     * @param outputDirConfig             the output directory configuration.
     * @param fileTransferConfig          the file transfer configuration.
     * @param sessionOutputDirectoryName  the session output directory name
     *                                    (for session derived configurations).
     */
    public RenameTask(DataTableModel model,
                      OutputDirectoryConfiguration outputDirConfig,
                      FileTransferConfiguration fileTransferConfig,
                      String sessionOutputDirectoryName) {
        super(model);

        this.outputDirConfig = outputDirConfig;

        this.fileTransferConfig = fileTransferConfig;
        try {
            this.fileTransferUtil =
                    new FileTransferUtil(fileTransferConfig.getBufferSize(),
                                         fileTransferConfig.getDigestAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            LOG.error("failed to construct file transfer utility from config " +
                      fileTransferConfig, e);
        }

        this.sessionOutputDirectory = new File(sessionOutputDirectoryName);

        List<DataRow> modelRows = model.getRows();
        String fromDirectoryName = null;
        if (modelRows.size() > 0) {
            DataRow firstModelRow = modelRows.get(0);
            File firstFile = getTargetFile(firstModelRow);
            File fromDirectory = firstFile.getParentFile();
            fromDirectoryName = fromDirectory.getAbsolutePath();
        }

        appendToSummary(getSummaryHeader());
        appendToSummary(fromDirectoryName);
        appendToSummary(":\n\n");

        long totalBytesToCopy = 0;
        for (DataRow modelRow : modelRows) {
            File file = getTargetFile(modelRow);
            totalBytesToCopy += file.length();
        }

        this.bytesInChunk = 1;
        if (totalBytesToCopy == 0) {
            this.totalByteChunksToCopy = 1; // prevent divide by zero
        } else if (totalBytesToCopy < (long) Integer.MAX_VALUE) {
            this.totalByteChunksToCopy = (int) totalBytesToCopy;
        } else {
            this.bytesInChunk = 1000000; // use megabytes instead of bytes
            this.totalByteChunksToCopy = (int)
                    (totalBytesToCopy / this.bytesInChunk);
        }

        this.chunksProcessed = 0;
        this.currentRow = null;
    }

    /**
     * @param  modelRow  the current row being processed.
     *
     * @return a plug-in data row for the current model row.
     */
    @Override
    protected PluginDataRow getPluginDataRow(DataRow modelRow) {
        File rowFile = getTargetFile(modelRow);
        File toDirectory = sessionOutputDirectory;

        if (! outputDirConfig.isDerivedForSession()) {
            // TODO: add support for nested fields
            toDirectory = new File(
                    outputDirConfig.getDerivedPath(rowFile,
                                                   modelRow.getFields()));
        }

        currentRow =  new RenamePluginDataRow(rowFile,
                                              modelRow,
                                              toDirectory);
        return currentRow;
    }

    /**
     * @param  lastRowProcessed    index of last proceessed row (zero based).
     * @param  totalRowsToProcess  total number of rows being processed.
     * @param  modelRow            the current row being processed.
     *
     * @return a task progress object for the specified row.
     */
    @Override
    protected TaskProgressInfo getProgressInfo(int lastRowProcessed,
                                               int totalRowsToProcess,
                                               DataRow modelRow) {

        File fromFile = currentRow.getFromFile();
        File toFile = currentRow.getRenamedFile();

        @SuppressWarnings("StringBufferReplaceableByString")
        StringBuilder sb = new StringBuilder(1024);
        sb.append("copying file ");
        sb.append((lastRowProcessed + 1));
        sb.append(" of ");
        sb.append(totalRowsToProcess);
        sb.append(": ");
        sb.append(fromFile.getName());
        sb.append(" -> ");
        sb.append(toFile.getName());
        int pctComplete = (int)
                (100 *
                 ((double) chunksProcessed / (double) totalByteChunksToCopy));

        return new TaskProgressInfo(lastRowProcessed,
                                    totalRowsToProcess,
                                    pctComplete,
                                    sb.toString());
    }

    /**
     * This method renames all files in the main view table model.
     *
     * @param  modelRow            the current row being processed.
     *
     * @return true if the processing completes successfully; otherwise false.
     */
    @Override
    protected boolean processRow(DataRow modelRow) {

        boolean renameSuccessful = false;
        File rowFile = currentRow.getFromFile();
        File renamedFile = currentRow.getRenamedFile();
        String errorMsg;

        if (currentRow.isOverwriteRequiredForRename()) {

            errorMsg = renamedFile.getAbsolutePath() + " already exists.";
            appendToSummary("ERROR: " + errorMsg + "\n");
            LOG.error(errorMsg + "  Skipping copy of " + rowFile.getAbsolutePath());

        } else {

            // perform the actual transfer
            try {
                transferFile(rowFile, renamedFile);

                if (outputDirConfig.isFileModeReadOnly()) {
                    boolean isReadOnlySet = false;
                    try {
                        isReadOnlySet = renamedFile.setReadOnly();
                    } catch (Exception e) {
                        LOG.warn("Failed to set read only permissions for " +
                                 renamedFile.getAbsolutePath() +
                                 " - ignoring exception", e);
                    }
                    if (! isReadOnlySet) {
                        LOG.warn("Failed to set read only permissions for " +
                                 renamedFile.getAbsolutePath());
                    }
                }
                renameSuccessful = true;
            } catch (Exception e) {
                LOG.error("Failed to copy " + rowFile.getAbsolutePath() +
                          " to " + renamedFile.getAbsolutePath(), e);
                appendOriginalErrorMessageToSummary(e);
            }

        }

        return renameSuccessful;
    }

    /**
     * This method adds summary information for the processed row
     * and updates progress information.  It also calls {@link #cleanupFiles}
     * to remove any files that should be cleaned up for the row
     * (based upon the success or failure of row processing).
     *
     * @param  modelRow            the current row being processed.
     *
     * @param  isSuccessful        true if the row was processed successfully
     *                             and all listeners completed their processing
     *                             successfully; otherwise false.
     */
    @Override
    protected void cleanupRow(DataRow modelRow,
                              boolean isSuccessful) {

        final File rowFile = currentRow.getFromFile();
        final File renamedFile = currentRow.getRenamedFile();

        long bytesProcessed = 0;
        if (rowFile.exists()) {
            bytesProcessed = rowFile.length();
        } else if (renamedFile.exists()) {
            bytesProcessed = renamedFile.length();
        }
        chunksProcessed += (int) (bytesProcessed / bytesInChunk);

        cleanupFiles(rowFile,
                     renamedFile,
                     isSuccessful,
                     currentRow.isOverwriteRequiredForRename());

        currentRow = null;
    }

    protected void cleanupFiles(File rowFile,
                                File renamedFile,
                                boolean isSuccessful,
                                boolean isOverwriteRequiredForRename) {

        if (isSuccessful) {

            appendToSummary("renamed ");

            // clean up the original file
            deleteFile(rowFile, "succeeded");

        } else {

            appendToSummary("ERROR: failed to rename ");

            // clean up the copied file if it exists and
            // it isn't the same as the source file
            if ((renamedFile != null) &&
                renamedFile.exists() &&
                (! renamedFile.equals(rowFile)) &&
                (! isOverwriteRequiredForRename)) {
                LOG.warn("Removing " + renamedFile.getAbsolutePath() +
                         " after rename processing failed.");
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

    protected void transferFile(File rowFile,
                                File renamedFile)
            throws IOException, FileCopyFailedException {
        if (fileTransferConfig.isNioRequired() &&
            (fileTransferUtil != null)) {

            fileTransferUtil.copyAndValidate(
                    rowFile,
                    renamedFile,
                    fileTransferConfig.isValidationRequired());

        } else {

            SafeFileTransfer.copy(rowFile, renamedFile, false);

        }
    }

    protected void deleteFile(File file,
                              String status) {
        boolean isDeleteSuccessful = false;
        try {
            isDeleteSuccessful = file.delete();
        } catch (Exception e) {
            LOG.warn("Failed to remove " + file.getAbsolutePath() +
                     " after rename " + status + " - ignoring exception", e);
        }
        if (! isDeleteSuccessful) {
            LOG.warn("Failed to remove " + file.getAbsolutePath() +
                     " after rename " + status);

        }
    }

    protected String getSummaryHeader() {
        return "Moved and renamed the following files from ";
    }

    private File getTargetFile(DataRow row) {
        Target target = row.getTarget();
        return (File) target.getInstance();
    }
}