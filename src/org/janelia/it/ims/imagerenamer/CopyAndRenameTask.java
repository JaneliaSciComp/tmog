/*
 * Copyright © 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer;

import org.apache.log4j.Logger;
import org.janelia.it.ims.imagerenamer.config.output.OutputDirectoryConfiguration;
import org.janelia.it.ims.imagerenamer.field.RenameField;
import org.janelia.it.ims.imagerenamer.plugin.CopyListener;
import org.janelia.it.ims.imagerenamer.plugin.ExternalDataException;
import org.janelia.it.ims.imagerenamer.plugin.ExternalSystemException;
import org.janelia.it.ims.imagerenamer.plugin.RenameFieldRow;
import org.janelia.it.ims.imagerenamer.plugin.SessionListener;
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
     * List of listeners registered for notification of copy events.
     */
    private ArrayList<CopyListener> copyListenerList;

    /**
     * List of listeners registered for notification of session events.
     */
    private ArrayList<SessionListener> sessionListenerList;

    /**
     * Indicates whether the running task should be cancelled.
     */
    private boolean isSessionCancelled;

    /**
     * Constructs a new task.
     *
     * @param mainView                    the main view for this rename session.
     * @param outputDirConfig             the output directory configuration.
     * @param sessionOutputDirectoryName  the session output directory name
     *                                    (for session derived configurations).
     */
    public CopyAndRenameTask(MainView mainView,
                             OutputDirectoryConfiguration outputDirConfig,
                             String sessionOutputDirectoryName) {

        this.mainView = mainView;
        this.outputDirConfig = outputDirConfig;
        this.sessionOutputDirectory = new File(sessionOutputDirectoryName);
        this.failedCopyRowIndices = new ArrayList<Integer>();
        this.copyListenerList = new ArrayList<CopyListener>();
        this.sessionListenerList = new ArrayList<SessionListener>();
        this.isSessionCancelled = false;
        this.renameSummary = new StringBuffer();

        FileTableModel model = mainView.getTableModel();
        List<FileTableRow> modelRows = model.getRows();
        String fromDirectoryName = null;
        if (modelRows.size() > 0) {
            FileTableRow firstModelRow = modelRows.get(0);
            File firstFile = firstModelRow.getFile();
            File fromDirectory = firstFile.getParentFile();
            fromDirectoryName = fromDirectory.getAbsolutePath();
        }

        renameSummary.append("Moved and renamed the following files from ");
        renameSummary.append(fromDirectoryName);
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

    public void cancelSession() {
        this.isSessionCancelled = true;
    }

    /**
     * Registers the specified listener for copy event notifications during
     * the file copy process.
     *
     * @param listener listener to be notified.
     */
    public void addCopyListener(CopyListener listener) {
        copyListenerList.add(listener);
    }

    /**
     * Registers the specified listener for session event notifications during
     * the file copy process.
     *
     * @param listener listener to be notified.
     */
    public void addSessionListener(SessionListener listener) {
        sessionListenerList.add(listener);
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
            if (isSessionCancelled) {
                LOG.warn("Rename session cancelled before start.");
                renameSummary.append("Rename session cancelled before start.");

                // mark all rows as failed
                FileTableModel model = mainView.getTableModel();
                List<FileTableRow> modelRows = model.getRows();
                int numberOfRows = modelRows.size();
                for (int i = 0; i < numberOfRows; i++) {
                    failedCopyRowIndices.add(i);
                }
            } else {
                renameFiles();
            }

            // notify any session listeners
            try {
                notifySessionListeners(
                        SessionListener.EventType.END,
                        renameSummary.toString());
            } catch (Exception e) {
                LOG.error("session listener processing failed", e);
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
            if (info.getFileNumber() == 0) {
                // once the first copy has started, change tab icon
                mainView.setViewIconToProcessing();
            }
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
        mainView.setViewIconToEnterValues(); // reset tab icon after completion
        resetCopyProgressComponents(false);
        int numberOfCopyFailures = failedCopyRowIndices.size();

        displaySummaryDialog(numberOfCopyFailures);

        if ((numberOfCopyFailures == 0) && (!isSessionCancelled)) {
            // everything succeeded, so reset the main view
            mainView.resetFileTable();
        } else {
            // we had errors, so remove the files copied successfully
            // and restore the rest of the model
            FileTableModel tableModel = mainView.getTableModel();
            tableModel.removeSuccessfullyCopiedFiles(failedCopyRowIndices);
            mainView.setFileTableEnabled(true, true);
        }

        mainView.setRenameTaskInProgress(false, true);
    }

    private void displaySummaryDialog(int numberOfCopyFailures) {
        String dialogTitle;
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
        areaScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        areaScrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JPanel appPanel = mainView.getPanel();
        Dimension appPanelSize = appPanel.getSize();
        int dialogWidth = (int) (appPanelSize.getWidth() * 0.8);
        int dialogHeight = (int) (appPanelSize.getHeight() * 0.8);
        Dimension dialogSize = new Dimension(dialogWidth, dialogHeight);

        JOptionPane jop = new JOptionPane(areaScrollPane,
                                          JOptionPane.INFORMATION_MESSAGE);
        jop.setPreferredSize(dialogSize);
        JDialog jd = jop.createDialog(appPanel, dialogTitle);
        jd.setModal(false);
        jd.setVisible(true);
    }

    /**
     * Renames all files in the main view table model.
     */
    private void renameFiles() {
        FileTableModel model = mainView.getTableModel();
        List<FileTableRow> modelRows = model.getRows();

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
        RenameField[] fields;
        String toDirectoryPath;
        RenameFieldRow fieldRow;

        for (FileTableRow modelRow : modelRows) {

            rowFile = modelRow.getFile();
            renamedFile = null;
            isRenameSuccessful = false;
            isStartNotificationSuccessful = false;
            fields = modelRow.getFields();

            if (! isDerivedForSession) {
                toDirectoryPath =
                        outputDirConfig.getDerivedPath(rowFile, fields);
                toDirectory = new File(toDirectoryPath);
            }

            fieldRow = new RenameFieldRow(rowFile, fields, toDirectory);
            try {
                fieldRow = notifyCopyListeners(CopyListener.EventType.START,
                                               fieldRow);
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

            if (isSessionCancelled) {
                handleCancelOfSession(rowIndex, numberOfRows, renamedFile);
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
                             RenameFieldRow fieldRow) {

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
                notifyCopyListeners(
                        CopyListener.EventType.END_SUCCESS,
                        fieldRow);
            } else {
                notifyCopyListeners(
                        CopyListener.EventType.END_FAIL,
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

            renameSummary.append("renamed ");

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

        renameSummary.append(rowFile.getName());
        if (renamedFile != null) {
            renameSummary.append(" to ");
            renameSummary.append(renamedFile.getAbsolutePath());
        }
        renameSummary.append("\n");
    }

    /**
     * Handles clean up needed if the session is cancelled while files
     * are being renamed.
     *
     * @param  rowIndex      the row index for the last renamed file.
     * @param  numberOfRows  the total number files being renamed.
     * @param  renamedFile   the last renamed file.
     */
    private void handleCancelOfSession(int rowIndex,
                                       int numberOfRows,
                                       File renamedFile) {
        if (rowIndex < numberOfRows) {
            LOG.warn("Rename session cancelled after copy of " +
                     renamedFile.getName() + ".");
            renameSummary.append("\nRename session cancelled.");

            // mark all remaining rows as failed
            for (int i = rowIndex; i < numberOfRows; i++) {
                failedCopyRowIndices.add(i);
            }
        } else {
            // reset cancel flag since cancel occurred after
            // last file was already renamed
            isSessionCancelled = false;
        }
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
    private RenameFieldRow notifyCopyListeners(CopyListener.EventType eventType,
                                               RenameFieldRow row)
            throws ExternalDataException, ExternalSystemException {
        for (CopyListener listener : copyListenerList) {
            row = listener.processEvent(eventType, row);
        }
        return row;
    }

    /**
     * Utility method to notify registered listeners about a session event.
     *
     * @param eventType the current event type.
     * @param message   the event message.
     * @throws ExternalDataException   if a listener detects a data error.
     * @throws ExternalSystemException if a system error occurs within a listener.
     */
    private void notifySessionListeners(SessionListener.EventType eventType,
                                        String message)
            throws ExternalDataException, ExternalSystemException {
        for (SessionListener listener : sessionListenerList) {
            listener.processEvent(eventType, message);
        }
    }

}