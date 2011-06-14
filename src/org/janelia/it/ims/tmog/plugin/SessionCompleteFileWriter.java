/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.config.PluginConfiguration;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * This class handles row and session events "published" by the
 * transmogrifier tool.  As renaming is performed, this plug-in
 * records the set of parent directories for the renamed files.
 * Upon completion of session processing, the plug-in creates
 * an empty completion file in each of the recorded parent directories.
 * The completion files can then be used to trigger other external
 * processes.
 * </p>
 *
 * <p>
 * Note that this plug-in should be configured solely as a "sessionListener".
 * The {@link org.janelia.it.ims.tmog.config.PluginFactory#constructInstances}
 * method takes care of adding it as a "rowListener".
 * </p>
 *
 * @author Eric Trautman
 */
public class SessionCompleteFileWriter
        implements SessionListener, RowListener {

    /**
     * The logger for this class.
     */
    private static final Logger LOG =
            Logger.getLogger(SessionCompleteFileWriter.class);

    /**
     * Plug-in instances are shared across all session threads,
     * so we need to track target directories for successfully
     * renamed files for each thread.
     */
    private Map<Thread, Set<File>> threadToDirectorySetMap;

    /**
     * The configured name for all completion files.
     */
    private String fileName;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public SessionCompleteFileWriter() {
        this.threadToDirectorySetMap =
                new ConcurrentHashMap<Thread, Set<File>>();
    }

    /**
     * Verifies that the plugin is ready for use.
     *
     * @param config the plugin configuration.
     *
     * @throws ExternalSystemException
     *   if the plugin can not be initialized.
     */
    public void init(PluginConfiguration config)
            throws ExternalSystemException {

        fileName = config.getProperty("fileName");
        if (fileName == null) {
            throw new ExternalSystemException(
                    "Please specify a fileName for the " +
                    "SessionCompleteFileWriter plug-in.");
        }
    }

    /**
     * For successful rename events, saves the parent directory of the
     * renamed file.
     *
     * @param  eventType  type of event.
     * @param  row        details about the event.
     *
     * @return the specified row unchanged.
     *
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    public PluginDataRow processEvent(RowListener.EventType eventType,
                                      PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        if (RowListener.EventType.END_ROW_SUCCESS.equals(eventType)) {
            if (row instanceof RenamePluginDataRow) {
                final Thread currentThread = Thread.currentThread();
                Set<File> directorySet =
                        threadToDirectorySetMap.get(currentThread);
                if (directorySet == null) {
                    directorySet = new HashSet<File>();
                    threadToDirectorySetMap.put(currentThread, directorySet);
                }
                final File renamedFile =
                        ((RenamePluginDataRow) row).getRenamedFile();
                directorySet.add(renamedFile.getParentFile());
            }
        }
        return row;
    }

    @Override
    public List<DataRow> startSession(List<DataRow> modelRows)
            throws ExternalDataException, ExternalSystemException {
        return null;  // ignored event
    }

    /**
     * Writes an empty file in each directory
     * where successful renamed files were created.
     *
     * @param  message  a message summarizing what was processed.
     *
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    @Override
    public void endSession(String message)
            throws ExternalDataException, ExternalSystemException {
                createCompletionFilesForCurrentThread();
    }

    private void createCompletionFilesForCurrentThread()
            throws ExternalSystemException {

        final Thread currentThread = Thread.currentThread();
        final Set<File> directorySet =
                threadToDirectorySetMap.get(currentThread);

        final String fileNameWithSuffix = fileName + SDF.format(new Date());

        try {
            for (File directory : directorySet) {

                final File completeFile = new File(directory, fileNameWithSuffix);

                boolean isFileCreated;
                try {
                    isFileCreated = completeFile.createNewFile();
                } catch (Throwable t) {
                    throw new ExternalSystemException(
                            "Failed to create session complete file: " +
                            completeFile.getAbsolutePath(), t);
                }

                if (isFileCreated) {
                    LOG.info("createCompletionFilesForCurrentThread created: " +
                             completeFile.getAbsolutePath());
                } else {
                    throw new ExternalSystemException(
                            "Session complete file already exists: " +
                            completeFile.getAbsolutePath());
                }
            }
        } finally {
            threadToDirectorySetMap.remove(currentThread);
        }
    }

    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("'-'yyyyMMdd'-'HHmmss'-'SSS'.txt'");
}