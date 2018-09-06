/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.utils.PathUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * This class handles row and session events "published" by the
 * transmogrifier tool.  As renaming is performed, this plug-in
 * records the set of plugin data for each successfully processed row.
 * Upon completion of session processing, the plug-in creates
 * a "renamed-files-...tsv" file that contains the old and new paths/names
 * of each successfully processed file.
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
public class SessionCompleteRenameWriter
        implements SessionListener, RowListener {

    /**
     * The logger for this class.
     */
    private static final Logger LOG =
            Logger.getLogger(SessionCompleteRenameWriter.class);

    /**
     * Plug-in instances are shared across all session threads,
     * so we need to track successfully processed rows for each thread.
     */
    private Map<Thread, List<PluginDataRow>> threadToSuccessfulRows;

    /**
     * The configured parent directory for all session files.
     */
    private File directory;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public SessionCompleteRenameWriter() {
        this.threadToSuccessfulRows =
                new ConcurrentHashMap<Thread, List<PluginDataRow>>();
    }

    /**
     * Verifies that the plugin is ready for use.
     *
     * @param config the plugin configuration.
     *
     * @throws org.janelia.it.ims.tmog.plugin.ExternalSystemException
     *   if the plugin can not be initialized.
     */
    public void init(PluginConfiguration config)
            throws ExternalSystemException {

        final String directoryName = config.getProperty("directory");
        if (directoryName == null) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG + "Please specify a directory.");
        }

        directory = new File(PathUtil.convertPath(directoryName));

        if (! directory.canWrite()) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG +
                    "You do not have permission to write to " +
                    directory.getAbsolutePath() + ".");
        }
    }

    /**
     * For successful end row events, saves the row so that it can be
     * referenced later when the session ends.
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
    public PluginDataRow processEvent(EventType eventType,
                                      PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        final Thread currentThread = Thread.currentThread();
        if (EventType.END_ROW_SUCCESS.equals(eventType)) {
            List<PluginDataRow> successList =
                    threadToSuccessfulRows.get(currentThread);
            if (successList == null) {
                successList = new ArrayList<PluginDataRow>();
                threadToSuccessfulRows.put(currentThread, successList);
            }
            successList.add(row);
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
                writeFileForCurrentThread();
    }

    protected void writeFileForCurrentThread()
            throws ExternalSystemException {

        final Thread currentThread = Thread.currentThread();
        final List<PluginDataRow> successList =
                    threadToSuccessfulRows.get(currentThread);

        if ((successList != null) && (successList.size() > 0)) {

            final String fileNameWithSuffix =
                    "renamed-files" + SDF.format(new Date());
            File file = new File(directory, fileNameWithSuffix);

            FileWriter writer = null;
            try {
                writer = new FileWriter(file);
                StringBuilder sb = new StringBuilder(512);
                writer.write("FROM\tTO\n");
                for (PluginDataRow row : successList) {
                    if (row instanceof RenamePluginDataRow) {
                        sb.setLength(0);
                        RenamePluginDataRow r = (RenamePluginDataRow) row;
                        sb.append(r.getFromFile().getAbsolutePath());
                        sb.append('\t');
                        sb.append(r.getTargetFile().getAbsolutePath());
                        sb.append('\n');
                        writer.write(sb.toString());
                    }
                }
            } catch (Exception e) {
                throw new ExternalSystemException(
                        "Failed to write session results to " +
                        file.getAbsolutePath() + ".", e);
            } finally {
                threadToSuccessfulRows.remove(currentThread);
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        LOG.error("failed to close " + file.getAbsolutePath(), e);
                    }
                }
            }

        }
    }

    private static final String INIT_FAILURE_MSG =
            "Failed to initialize SessionCompleteRenameWriter plug-in.  ";

    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("'-'yyyyMMdd'-'HHmmss'-'SSS'.tsv'");
}