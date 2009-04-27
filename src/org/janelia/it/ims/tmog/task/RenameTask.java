/*
 * Copyright 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.task;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.DataTableModel;
import org.janelia.it.ims.tmog.DataTableRow;
import org.janelia.it.ims.tmog.config.output.OutputDirectoryConfiguration;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.RenamePluginDataRow;
import org.janelia.it.ims.tmog.target.Target;
import org.janelia.it.utils.filexfer.FileCopyFailedException;
import org.janelia.it.utils.filexfer.SafeFileTransfer;

import java.io.File;
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
     * @param sessionOutputDirectoryName  the session output directory name
     *                                    (for session derived configurations).
     */
    public RenameTask(DataTableModel model,
                      OutputDirectoryConfiguration outputDirConfig,
                      String sessionOutputDirectoryName) {
        super(model);

        this.outputDirConfig = outputDirConfig;
        this.sessionOutputDirectory = new File(sessionOutputDirectoryName);

        List<DataTableRow> modelRows = model.getRows();
        String fromDirectoryName = null;
        if (modelRows.size() > 0) {
            DataTableRow firstModelRow = modelRows.get(0);
            File firstFile = getTargetFile(firstModelRow);
            File fromDirectory = firstFile.getParentFile();
            fromDirectoryName = fromDirectory.getAbsolutePath();
        }

        appendToSummary("Moved and renamed the following files from ");
        appendToSummary(fromDirectoryName);
        appendToSummary(":\n\n");

        long totalBytesToCopy = 0;
        for (DataTableRow modelRow : modelRows) {
            File file = getTargetFile(modelRow);
            totalBytesToCopy += file.length();
        }

        if (totalBytesToCopy == 0) {
            this.totalByteChunksToCopy = 1; // prevent divide by zero
        } else if (totalBytesToCopy < (long) Integer.MAX_VALUE) {
            this.bytesInChunk = 1;
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
    protected PluginDataRow getPluginDataRow(DataTableRow modelRow) {
        File rowFile = getTargetFile(modelRow);
        File toDirectory = sessionOutputDirectory;

        if (! outputDirConfig.isDerivedForSession()) {
            toDirectory = new File(
                    outputDirConfig.getDerivedPath(rowFile,
                                                   modelRow.getFields()));
        }

        currentRow =  new RenamePluginDataRow(rowFile,
                                              modelRow.getDataRow(),
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
                                               DataTableRow modelRow) {

        File fromFile = currentRow.getFromFile();
        File toFile = currentRow.getRenamedFile();

        StringBuilder sb = new StringBuilder();
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
    protected boolean processRow(DataTableRow modelRow) {

        boolean renameSuccessful = false;
        File rowFile = currentRow.getFromFile();
        File renamedFile = currentRow.getRenamedFile();

        // perform the actual copy and rename
        try {
            SafeFileTransfer.copy(rowFile, renamedFile, false);
            if (outputDirConfig.isFileModeReadOnly()) {
                boolean isReadOnlySet = false;
                try {
                    isReadOnlySet = renamedFile.setReadOnly();
                } catch (Exception e) {
                    LOG.warn("Faied to set read only permissions for " +
                             renamedFile.getAbsolutePath() +
                             " - ignoring exception", e);
                }
                if (! isReadOnlySet) {
                    LOG.warn("Faied to set read only permissions for " +
                             renamedFile.getAbsolutePath());
                }
            }
            renameSuccessful = true;
        } catch (FileCopyFailedException e) {
            LOG.error("Failed to copy " + rowFile.getAbsolutePath() +
                      " to " + renamedFile.getAbsolutePath(), e);
        }

        return renameSuccessful;
    }

    /**
     * This method adds summary information for the processed row
     * and updates progress information.  It also deletes the
     * source file being renamed if it was renamed successfully.
     *
     * @param  modelRow            the current row being processed.
     *
     * @param  isSuccessful        true if the row was processed successfully
     *                             and all listeners completed their processing
     *                             successfully; otherwise false.
     */
    @Override
    protected void cleanupRow(DataTableRow modelRow,
                              boolean isSuccessful) {

        File rowFile = currentRow.getFromFile();
        File renamedFile = currentRow.getRenamedFile();

        chunksProcessed += (int) (rowFile.length() / bytesInChunk);

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
                (!renamedFile.equals(rowFile))) {
                deleteFile(renamedFile, "failed");
            }
        }

        appendToSummary(rowFile.getName());
        if (renamedFile != null) {
            appendToSummary(" to ");
            appendToSummary(renamedFile.getAbsolutePath());
        }
        appendToSummary("\n");

        currentRow = null;
    }

    private void deleteFile(File file,
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

    private File getTargetFile(DataTableRow row) {
        Target target = row.getTarget();
        return (File) target.getInstance();
    }
}