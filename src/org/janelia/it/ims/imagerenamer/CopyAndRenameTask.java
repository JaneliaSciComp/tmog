/*
 * Copyright © 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer;

import org.apache.log4j.Logger;
import org.janelia.it.ims.imagerenamer.field.RenameField;
import org.janelia.it.ims.imagerenamer.plugin.CopyCompleteInfo;
import org.janelia.it.ims.imagerenamer.plugin.CopyCompleteListener;
import org.janelia.it.ims.imagerenamer.plugin.ExternalDataException;
import org.janelia.it.ims.imagerenamer.plugin.ExternalSystemException;
import org.janelia.it.utils.filexfer.FileCopyFailedException;
import org.janelia.it.utils.filexfer.SafeFileTransfer;
import org.jdesktop.swingworker.SwingWorker;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import java.awt.Toolkit;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class supports the execution of the copy and rename process as
 * a background thread.  It is tightly coupled with the
 * {@link MainView} and {@link FileTableModel} classes because
 * it updates progress using {@link MainView} components and
 * it is dependent upon {@link FileTableModel} data to derive new
 * file names.
 *
 * @author Eric Trautman
 */
public class CopyAndRenameTask extends SwingWorker<Void, CopyProgressInfo> {

    /** The logger for this class. */
    private static final Logger LOG = Logger.getLogger(CopyAndRenameTask.class);

    /** The main view for this rename session. */
    private MainView mainView;

    /** The target directory for all copied files. */
    private File toDirectory;

    /** A text summary of what was copied/renamed. */
    private StringBuffer renameSummary;

    /**
     * The number of bytes in a programatically sized "chunk"
     * (used to report copy progress percentages).
     */
    private int bytesInChunk;

    /** The total number of byte "chunks" that need to be copied. */
    private long totalByteChunksToCopy;

    /** List of index numbers for copy failures. */
    private ArrayList<Integer> failedCopyRowIndices;

    /**
     * List of listeners registered for notification of successful
     * copy completions.
     */
    private ArrayList<CopyCompleteListener> copyCompleteListenerList;

    /**
     * Constructs a new task.
     *
     * @param  mainView  the main view for this rename session.
     */
    public CopyAndRenameTask(MainView mainView) {

        this.mainView = mainView;
        JLabel outputDirLabel = mainView.getOutputDirectoryField();
        this.toDirectory = new File(outputDirLabel.getText());
        this.failedCopyRowIndices = new ArrayList<Integer>();
        this.copyCompleteListenerList = new ArrayList<CopyCompleteListener>();
        this.renameSummary = new StringBuffer();

        FileTableModel model = mainView.getTableModel();
        File[] files = model.getFiles();
        String toDirectoryName = toDirectory.getAbsolutePath();
        String fromDirectoryName = null;
        if (files.length > 0) {
            File fromDirectory = files[0].getParentFile();
            fromDirectoryName = fromDirectory.getAbsolutePath();
        }

        renameSummary.append("Copied and renamed the following files from\n     ");
        renameSummary.append(fromDirectoryName);
        renameSummary.append(" to\n     ");
        renameSummary.append(toDirectoryName);
        renameSummary.append(":\n\n");

        bytesInChunk = 1000000; // default to megabytes
        totalByteChunksToCopy = 1; // prevent rare but possible divide by zero
        for (File file : files) {
            totalByteChunksToCopy += (file.length() / bytesInChunk);
        }
        // reset to gigabytes if necessary
        if (totalByteChunksToCopy > (long)Integer.MAX_VALUE) {
            totalByteChunksToCopy = totalByteChunksToCopy / 1000;
            bytesInChunk = bytesInChunk * 1000;
        }

        resetCopyProgressComponents(true);
        JTable fileTable = mainView.getFileTable();
        fileTable.changeSelection(0, 1, false, false);
    }

    /**
     * Registers the specified listener for notification when
     * each successful file copy completes.
     *
     * @param  listener  listener to be notified.
     */
    public void addCopyCompleteListener(CopyCompleteListener listener) {
        copyCompleteListenerList.add(listener);
    }

    /**
     * Executes the copy and rename process in a background thread so
     * that long copies do not block the event dispatching thread.
     */
    @Override
    public Void doInBackground() {
        LoggingUtils.setLoggingContext();
        LOG.debug("starting copy");

        try {
            FileTableModel model = mainView.getTableModel();
            File[] files = model.getFiles();
            RenameField[][] fields = model.getFields();

            int chunksProcessed = 0;
            long pctComplete = 0;
            final int numberOfRows = fields.length;
            for (int rowIndex = 0; rowIndex < numberOfRows; rowIndex++) {

                // build the new name for the file
                StringBuilder newFileName = new StringBuilder();
                RenameField[] rowFields = fields[rowIndex];
                for (RenameField field : rowFields) {
                    newFileName.append(field.getFileNameValue());
                }
                newFileName.append(MainView.LSM_EXTENSION);

                File rowFile = files[rowIndex];
                String originalFileName = rowFile.getName();
                File renamedFile = new File(toDirectory,
                                            newFileName.toString());

                // update progress information in the UI
                publish(new CopyProgressInfo(rowFile,
                                             renamedFile,
                                             pctComplete,
                                             rowIndex,
                                             numberOfRows));

                // perform the actual copy and rename
                boolean isRenameSuccessful = false;
                try {
                    SafeFileTransfer.copy(rowFile, renamedFile, false);
                    isRenameSuccessful = true;
                } catch (FileCopyFailedException e) {
                    LOG.error("Failed to copy " + rowFile.getAbsolutePath() +
                            " to " + renamedFile.getAbsolutePath(), e);
                    failedCopyRowIndices.add(rowIndex);
                }

                // notify any listeners
                if (isRenameSuccessful) {
                    isRenameSuccessful =
                       notifyCopyComplete(new CopyCompleteInfo(rowFile,
                                                               renamedFile,
                                                               rowFields));
                    if (! isRenameSuccessful) {
                        failedCopyRowIndices.add(rowIndex);
                    }
                }

                // calculate progress before we delete the file
                chunksProcessed += (int) (rowFile.length() / bytesInChunk);
                pctComplete = (100 * chunksProcessed) / totalByteChunksToCopy;

                if (isRenameSuccessful) {
                    // clean up the original file
                    try {
                        rowFile.delete();
                    } catch (Exception e) {
                        LOG.warn("Failed to remove " +
                                 rowFile.getAbsolutePath() +
                                 " after rename succeeded.", e);
                    }
                    renameSummary.append("Renamed ");
                } else {
                    renameSummary.append("ERROR: failed to rename ");
                }

                renameSummary.append(originalFileName);
                renameSummary.append(" to ");
                renameSummary.append(renamedFile.getName());
                renameSummary.append("\n");
            }

            LOG.debug("finished copy");
        } catch (Throwable t) {
            // ensure errors that occur in this thread are not lost
            LOG.error("unexpected exception in background task", t);
        }

        return null;
    }

    /**
     * Updates the copy progress information displayed in the UI.
     * This method runs in the event dispatcher thread.
     *
     * @param  list  list of progress information objects for display.
     */
    @Override
    protected void process(List<CopyProgressInfo> list) {
        JProgressBar copyProgressBar = mainView.getCopyProgressBar();
        JLabel copyProgressLabel = mainView.getCopyProgressLabel();
        for (CopyProgressInfo info : list) {
            copyProgressBar.setValue(info.getProgress());
            copyProgressLabel.setText(info.toString());
            JTable fileTable = mainView.getFileTable();
            fileTable.changeSelection(info.getFileNumber(), 1, false, false);
        }
    }

    /**
     * The method is called when the background thread copy process completes.
     * It updates the UI with a rename summary and cleans up any progress
     * reporting components.
     * This method runs in the event dispatcher thread.
     */
    @Override
    public void done() {
        Toolkit.getDefaultToolkit().beep();
        resetCopyProgressComponents(false);
        String dialogTitle;
        int numberOfCopyFailures = failedCopyRowIndices.size();
        if (numberOfCopyFailures > 0) {
            dialogTitle = "Rename Summary (" + numberOfCopyFailures;
            if (numberOfCopyFailures > 1) {
               dialogTitle = dialogTitle + " COPY FAILURES!)";
            } else {
               dialogTitle = dialogTitle + " COPY FAILURE!)";                
            }
        } else {
            dialogTitle = "Rename Summary";
        }
        
        JOptionPane.showMessageDialog(mainView.getPanel(),
                                      renameSummary.toString(),
                                      dialogTitle,
                                      JOptionPane.INFORMATION_MESSAGE);

        if (numberOfCopyFailures == 0) {
            // everything succeeded, so reset the main view
            mainView.resetFileTable();
        } else {
            // we had errors, so remove the files copied successfully
            // and restore the rest of the model
            FileTableModel tableModel = mainView.getTableModel();
            tableModel.removeSuccessfullyCopiedFiles(failedCopyRowIndices);
            mainView.setFileTableEnabled(true);
        }

        mainView.setRenameTaskInProgress(false);
    }

    /**
     * Utility method to enable or disable the copy progress components
     * in the UI.
     *
     * @param  isVisible  identifies if the components should be made visible.
     */
    private void resetCopyProgressComponents(boolean isVisible) {
        JProgressBar copyProgressBar = mainView.getCopyProgressBar();
        JLabel copyProgressLabel = mainView.getCopyProgressLabel();
        copyProgressBar.setModel(new DefaultBoundedRangeModel());
        copyProgressBar.setVisible(isVisible);
        copyProgressLabel.setText("");
        copyProgressLabel.setVisible(isVisible);
    }

    /**
     * Utility method to notify all registered listeners.
     *
     * @param  info  the copy completion event information to send.
     *
     * @return true if all listeners successfully process the event;
     *         otherwise false.
     *
     * @throws ExternalDataException
     *   if a listener detects a data error.
     *
     * @throws ExternalSystemException
     *   if a system error occurs within a listener.
     */
    private boolean notifyCopyComplete(CopyCompleteInfo info)
            throws ExternalDataException, ExternalSystemException {
        boolean isListenerProcessingSuccessful = true;
        for (CopyCompleteListener listener : copyCompleteListenerList) {
            try {
                listener.completedSuccessfulCopy(info);
            } catch (ExternalDataException e) {
                LOG.error("Failed external processing for " + info, e);
                isListenerProcessingSuccessful = false;
            } catch (ExternalSystemException e) {
                LOG.error("Failed external processing for " + info, e);
                isListenerProcessingSuccessful = false;
            }
        }
        return isListenerProcessingSuccessful;
    }
}