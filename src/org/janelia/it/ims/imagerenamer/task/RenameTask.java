/*
 * Copyright 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.task;

import org.apache.log4j.Logger;
import org.janelia.it.ims.imagerenamer.DataTableModel;
import org.janelia.it.ims.imagerenamer.DataTableRow;
import org.janelia.it.ims.imagerenamer.Target;
import org.janelia.it.ims.imagerenamer.config.output.OutputDirectoryConfiguration;
import org.janelia.it.ims.imagerenamer.field.DataField;
import org.janelia.it.ims.imagerenamer.plugin.RenamePluginDataRow;
import org.janelia.it.ims.imagerenamer.plugin.RowListener;
import org.janelia.it.utils.filexfer.FileCopyFailedException;
import org.janelia.it.utils.filexfer.SafeFileTransfer;

import java.io.File;
import java.util.List;

/**
 * This class supports the execution of the copy and rename process as
 * a background thread.
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

    /**
     * The total number of byte "chunks" that need to be copied.
     */
    private long totalByteChunksToCopy;

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

        bytesInChunk = 1000000; // default to megabytes
        totalByteChunksToCopy = 1; // prevent rare but possible divide by zero
        for (DataTableRow modelRow : modelRows) {
            File file = getTargetFile(modelRow);
            totalByteChunksToCopy += (file.length() / bytesInChunk);
        }
        // reset to gigabytes if necessary
        if (totalByteChunksToCopy > (long) Integer.MAX_VALUE) {
            totalByteChunksToCopy = totalByteChunksToCopy / 1000;
            bytesInChunk = bytesInChunk * 1000;
        }
    }

    /**
     * Renames all files in the main view table model.
     */
    protected void doTask() {
        DataTableModel model = getModel();
        List<DataTableRow> modelRows = model.getRows();

        int rowIndex = 0;
        int chunksProcessed = 0;
        long pctComplete = 0;
        final int numberOfRows = modelRows.size();
        boolean isDerivedForSession = outputDirConfig.isDerivedForSession();
        File toDirectory = sessionOutputDirectory;

        File rowFile;
        File renamedFile;
        boolean isRenameSuccessful;
        boolean isStartNotificationSuccessful;
        List<DataField> fields;
        String toDirectoryPath;
        RenamePluginDataRow fieldRow;
        String progressMsg;

        for (DataTableRow modelRow : modelRows) {

            rowFile = getTargetFile(modelRow);
            renamedFile = null;
            isRenameSuccessful = false;
            isStartNotificationSuccessful = false;
            fields = modelRow.getFields();

            if (! isDerivedForSession) {
                toDirectoryPath =
                        outputDirConfig.getDerivedPath(rowFile, fields);
                toDirectory = new File(toDirectoryPath);
            }

            fieldRow = new RenamePluginDataRow(rowFile,
                                               modelRow.getDataRow(),
                                               toDirectory);
            try {
                fieldRow = (RenamePluginDataRow)
                        notifyRowListeners(RowListener.EventType.START,
                                            fieldRow);
                isStartNotificationSuccessful = true;
            } catch (Exception e) {
                LOG.error("Failed external start processing for " +
                          fieldRow, e);
            }

            if (isStartNotificationSuccessful) {
                renamedFile = fieldRow.getRenamedFile();
                // update progress information in the UI
                progressMsg = getProgressMessage(rowIndex,
                                                 numberOfRows,
                                                 rowFile,
                                                 renamedFile);
                publish(new TaskProgressInfo(rowIndex,
                                             numberOfRows,
                                             (int) pctComplete,
                                             progressMsg));

                isRenameSuccessful = copyFile(rowFile,
                                              renamedFile,
                                              fieldRow);
            }

            // calculate progress before we delete the file
            chunksProcessed += (int) (rowFile.length() / bytesInChunk);
            pctComplete = (100 * chunksProcessed) / totalByteChunksToCopy;

            cleanupAfterRename(rowIndex,
                               rowFile,
                               renamedFile,
                               isRenameSuccessful);

            rowIndex++;

            if (isSessionCancelled()) {
                handleCancelOfSession(rowIndex,
                                      numberOfRows, 
                                      modelRow.getTarget());
                break;
            }
        }
    }

    /**
     * Copies the specified file.
     *
     * @param  rowFile      the file to be copied and renamed.
     * @param  renamedFile  the target path for the renamed file.
     * @param  fieldRow     the captured field data for this file.
     *
     * @return true if the copy and rename was successful; otherwise false.
     */
    private boolean copyFile(File rowFile,
                             File renamedFile,
                             RenamePluginDataRow fieldRow) {

        boolean renameSuccessful = false;

        // perform the actual copy and rename
        try {
            SafeFileTransfer.copy(rowFile, renamedFile, false);
            renameSuccessful = true;
        } catch (FileCopyFailedException e) {
            LOG.error("Failed to copy " + rowFile.getAbsolutePath() +
                      " to " + renamedFile.getAbsolutePath(), e);
        }

        // notify any listeners
        try {
            if (renameSuccessful) {
                notifyRowListeners(
                        RowListener.EventType.END_SUCCESS,
                        fieldRow);
            } else {
                notifyRowListeners(
                        RowListener.EventType.END_FAIL,
                        fieldRow);
            }
        } catch (Exception e) {
            LOG.error("Failed external completion processing for " +
                      fieldRow, e);
            renameSuccessful = false;
        }
        return renameSuccessful;
    }

    /**
     * Clean up either the source file or the renamed file depending
     * upon whether the rename process succeeded.
     *
     * @param  rowIndex          the row index for the renamed file.
     * @param  rowFile           the source file.
     * @param  renamedFile       the renamed file.
     * @param  renameSuccessful  flag indicating if the rename was successful.
     */
    private void cleanupAfterRename(int rowIndex,
                                    File rowFile,
                                    File renamedFile,
                                    boolean renameSuccessful) {
        if (renameSuccessful) {

            appendToSummary("renamed ");

            // clean up the original file
            try {
                rowFile.delete();
            } catch (Exception e) {
                LOG.warn("Failed to remove " +
                         rowFile.getAbsolutePath() +
                         " after rename succeeded.", e);
            }

        } else {

            appendToSummary("ERROR: failed to rename ");
            addFailedRowIndex(rowIndex);

            // clean up the copied file if it exists and
            // it isn't the same as the source file
            if ((renamedFile != null) &&
                renamedFile.exists() &&
                (!renamedFile.equals(rowFile))) {
                try {
                    renamedFile.delete();
                } catch (Exception e) {
                    LOG.warn("Failed to remove " +
                             renamedFile.getAbsolutePath() +
                             " after rename failed.", e);
                }
            }
        }

        appendToSummary(rowFile.getName());
        if (renamedFile != null) {
            appendToSummary(" to ");
            appendToSummary(renamedFile.getAbsolutePath());
        }
        appendToSummary("\n");
    }

    private File getTargetFile(DataTableRow row) {
        Target target = row.getTarget();
        return (File) target.getInstance();
    }

    private String getProgressMessage(int lastRowProcessed,
                                      int totalRowsToProcess,
                                      File fromFile,
                                      File toFile) {
        StringBuilder sb = new StringBuilder();
        sb.append("copying file ");
        sb.append((lastRowProcessed + 1));
        sb.append(" of ");
        sb.append(totalRowsToProcess);
        sb.append(": ");
        sb.append(fromFile.getName());
        sb.append(" -> ");
        sb.append(toFile.getName());
        return sb.toString();
    }

}