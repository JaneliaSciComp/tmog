/*
 * Copyright © 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer;

import org.apache.log4j.Logger;
import org.janelia.it.ims.imagerenamer.plugin.CopyListener;
import org.janelia.it.ims.imagerenamer.plugin.CopyListener.EventType;
import org.janelia.it.ims.imagerenamer.plugin.ExternalDataException;
import org.janelia.it.ims.imagerenamer.plugin.ExternalSystemException;
import org.janelia.it.ims.imagerenamer.plugin.RenameFieldRow;
import org.janelia.it.utils.filexfer.FileCopyFailedException;
import org.janelia.it.utils.filexfer.SafeFileTransfer;
import org.jdesktop.swingworker.SwingWorker;

import javax.swing.*;
import java.awt.*;
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

    /**
     * The logger for this class.
     */
    private static final Logger LOG = Logger.getLogger(CopyAndRenameTask.class);

    /**
     * The main view for this rename session.
     */
    private MainView mainView;

    /**
     * The target directory for all copied files.
     */
    private File toDirectory;

    /**
     * A text summary of what was copied/renamed.
     */
    private StringBuffer renameSummary;

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
     * List of index numbers for copy failures.
     */
    private ArrayList<Integer> failedCopyRowIndices;

    /**
     * List of listeners registered for notification of successful
     * copy completions.
     */
    private ArrayList<CopyListener> copyListenerLists;

    /**
     * Constructs a new task.
     *
     * @param mainView the main view for this rename session.
     */
    public CopyAndRenameTask(MainView mainView) {

        this.mainView = mainView;
        JLabel outputDirLabel = mainView.getOutputDirectoryField();
        this.toDirectory = new File(outputDirLabel.getText());
        this.failedCopyRowIndices = new ArrayList<Integer>();
        this.copyListenerLists = new ArrayList<CopyListener>();
        this.renameSummary = new StringBuffer();

        FileTableModel model = mainView.getTableModel();
        List<FileTableRow> modelRows = model.getRows();
        String toDirectoryName = toDirectory.getAbsolutePath();
        String fromDirectoryName = null;
        if (modelRows.size() > 0) {
            FileTableRow firstModelRow = modelRows.get(0);
            File firstFile = firstModelRow.getFile();
            File fromDirectory = firstFile.getParentFile();
            fromDirectoryName = fromDirectory.getAbsolutePath();
        }

        renameSummary.append("Copied and renamed the following files from\n     ");
        renameSummary.append(fromDirectoryName);
        renameSummary.append(" to\n     ");
        renameSummary.append(toDirectoryName);
        renameSummary.append(":\n\n");

        bytesInChunk = 1000000; // default to megabytes
        totalByteChunksToCopy = 1; // prevent rare but possible divide by zero
        for (FileTableRow modelRow : modelRows) {
            File file = modelRow.getFile();
            totalByteChunksToCopy += (file.length() / bytesInChunk);
        }
        // reset to gigabytes if necessary
        if (totalByteChunksToCopy > (long) Integer.MAX_VALUE) {
            totalByteChunksToCopy = totalByteChunksToCopy / 1000;
            bytesInChunk = bytesInChunk * 1000;
        }

        resetCopyProgressComponents(true);
        JTable fileTable = mainView.getFileTable();
        fileTable.changeSelection(0, 1, false, false);
    }

    /**
     * Registers the specified listener for notifications during
     * the file copy process.
     *
     * @param listener listener to be notified.
     */
    public void addCopyListener(CopyListener listener) {
        copyListenerLists.add(listener);
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
            mainView.getCopyAndRenameBtn().setText(mainView.COPY_BUTTON_TEXT);
            mainView.getCopyAndRenameBtn().setToolTipText(mainView.COPY_BUTTON_TOOL_TIP_TEXT);
            mainView.getCopyAndRenameBtn().setEnabled(false);
            FileTableModel model = mainView.getTableModel();
            List<FileTableRow> modelRows = model.getRows();

            int rowIndex = 0;
            int chunksProcessed = 0;
            long pctComplete = 0;
            final int numberOfRows = modelRows.size();
            for (FileTableRow modelRow : modelRows) {

                File rowFile = modelRow.getFile();
                String originalFileName = rowFile.getName();
                File renamedFile = null;
                boolean isRenameSuccessful = false;
                boolean isStartNotificationSuccessful = false;

                RenameFieldRow fieldRow = new RenameFieldRow(rowFile,
                        modelRow.getFields(),
                        toDirectory);
                try {
                    fieldRow = notifyCopyListeners(EventType.START, fieldRow);
                    isStartNotificationSuccessful = true;
                } catch (Exception e) {
                    LOG.error("Failed external start processing for " +
                            fieldRow, e);
                }

                if (isStartNotificationSuccessful) {
                    renamedFile = fieldRow.getRenamedFile();

                    // update progress information in the UI
                    publish(new CopyProgressInfo(rowFile,
                            renamedFile,
                            pctComplete,
                            rowIndex,
                            numberOfRows));

                    // perform the actual copy and rename
                    try {
                        SafeFileTransfer.copy(rowFile, renamedFile, false);
                        isRenameSuccessful = true;
                    } catch (FileCopyFailedException e) {
                        LOG.error("Failed to copy " + rowFile.getAbsolutePath() +
                                " to " + renamedFile.getAbsolutePath(), e);
                    }

                    // notify any listeners
                    try {
                        if (isRenameSuccessful) {
                            notifyCopyListeners(EventType.END_SUCCESS,
                                    fieldRow);
                        } else {
                            notifyCopyListeners(EventType.END_FAIL,
                                    fieldRow);
                        }
                    } catch (Exception e) {
                        LOG.error("Failed external completion processing for " +
                                fieldRow, e);
                        isRenameSuccessful = false;
                    }
                }

                // calculate progress before we delete the file
                chunksProcessed += (int) (rowFile.length() / bytesInChunk);
                pctComplete = (100 * chunksProcessed) / totalByteChunksToCopy;

                if (isRenameSuccessful) {

                    renameSummary.append("Renamed ");

                    // clean up the original file
                    try {
                        rowFile.delete();
                    } catch (Exception e) {
                        LOG.warn("Failed to remove " +
                                rowFile.getAbsolutePath() +
                                " after rename succeeded.", e);
                    }

                } else {

                    renameSummary.append("ERROR: failed to rename ");
                    failedCopyRowIndices.add(rowIndex);

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

                renameSummary.append(originalFileName);
                if (renamedFile != null) {
                    renameSummary.append(" to ");
                    renameSummary.append(renamedFile.getName());
                }
                renameSummary.append("\n");

                rowIndex++;
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
     * @param list list of progress information objects for display.
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
        if (!isCancelled()) {
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
            JTextArea textArea = new JTextArea();
            textArea.setLayout(new BorderLayout());
            textArea.setEditable(false);
            textArea.append(renameSummary.toString());
            JScrollPane areaScrollPane = new JScrollPane(textArea);
            areaScrollPane.setPreferredSize(new Dimension(600, 400));
            areaScrollPane.setWheelScrollingEnabled(true);
            areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            areaScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            JOptionPane.showMessageDialog(mainView.getPanel(),
                    areaScrollPane,
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
        }
        mainView.setRenameTaskInProgress(false);
    }

    /**
     * Utility method to enable or disable the copy progress components
     * in the UI.
     *
     * @param isVisible identifies if the components should be made visible.
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
     * Utility method to notify registered listeners about a copy event.
     *
     * @param eventType the current event type.
     * @param row       the rename data associated with the event.
     * @return the (possibly) updated rename data.
     * @throws ExternalDataException   if a listener detects a data error.
     * @throws ExternalSystemException if a system error occurs within a listener.
     */
    private RenameFieldRow notifyCopyListeners(EventType eventType,
                                               RenameFieldRow row)
            throws ExternalDataException, ExternalSystemException {
        for (CopyListener listener : copyListenerLists) {
            row = listener.processEvent(eventType, row);
        }
        return row;
    }
}