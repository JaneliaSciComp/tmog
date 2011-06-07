/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import org.janelia.it.ims.tmog.config.PluginConfiguration;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This {@link SessionSummaryFileWriter} also handles row events "published"
 * by the transmogrifier tool,  saving the last successfully renamed file
 * so that the summary file can be written to the same directory.
 *
 * @author Eric Trautman
 */
public class RelativeSessionSummaryFileWriter extends SessionSummaryFileWriter
        implements RowListener {

    /**
     * Plug-in instances are shared across all session threads,
     * so we need to track successfully renamed files for each thread.
     */
    private Map<Thread, File> threadToLastSuccessfulFileMap;

    /**
     * Empty constructor.
     */
    public RelativeSessionSummaryFileWriter() {
        this.threadToLastSuccessfulFileMap =
                new ConcurrentHashMap<Thread, File>();
    }

    /**
     * @return the session summary file's parent directory.
     */
    public File getDirectory() {
        File summaryDirectory = null;
        File lastSuccessfulFile =
          threadToLastSuccessfulFileMap.remove(Thread.currentThread());
        if (lastSuccessfulFile != null) {
            summaryDirectory = lastSuccessfulFile.getParentFile();
        }
        return summaryDirectory;
    }

    public void init(PluginConfiguration config) throws ExternalSystemException {
    }

    /**
     * For successful rename events, saves the renamed file.
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
                threadToLastSuccessfulFileMap.put(
                        Thread.currentThread(),
                        ((RenamePluginDataRow) row).getRenamedFile());
            }
        }
        return row;
    }


}