/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.utils.PathUtil;

import java.io.File;
import java.io.FileWriter;

/**
 * This base class provides common support for plug-ins that
 * write transmogrifier row data to a file.
 *
 * @author Eric Trautman
 */
public abstract class RowWriterPlugin
        implements RowListener {

    /**
     * The configured base directory for all files.
     */
    private File directory;

    /** Indicates whether the data file should be made writable by all users. */
    private boolean isWritableByEverybody;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public RowWriterPlugin() {
        this.isWritableByEverybody = false;
    }

    /**
     * Verifies that the plugin is ready for use by checking external
     * dependencies.
     *
     * @param  config  the plugin configuration.
     *
     * @throws ExternalSystemException
     *   if the plugin can not be initialized.
     */
    @Override
    public void init(PluginConfiguration config) throws ExternalSystemException {
        try {
            String directoryName = config.getProperty("directory");
            if ((directoryName != null) && (directoryName.length() > 0)) {
                directoryName = PathUtil.convertPath(directoryName);
                directory = new File(directoryName);
                if (directory.exists()) {
                    if (directory.isDirectory()) {
                        if (!directory.canWrite()) {
                            throw new ExternalSystemException(
                                    getInitFailureMessage() +
                                    "Unable to write to directory: " +
                                    directory.getAbsolutePath());
                        }
                    } else {
                        throw new ExternalSystemException(
                                getInitFailureMessage() +
                                "Configured directory (" +
                                directory.getAbsolutePath() +
                                ") for is not a directory.");
                    }
                } else {
                    throw new ExternalSystemException(
                            getInitFailureMessage() +
                            "Unable to find directory: " +
                            directory.getAbsolutePath());
                }
            } else {
                directory = null;
            }
        } catch (ExternalSystemException e) {
            throw e;
        } catch (Throwable t) {
            throw new ExternalSystemException(
                    getInitFailureMessage() + t.getMessage(),
                    t);
        }

        isWritableByEverybody =
                Boolean.parseBoolean(config.getProperty("writableByAll"));
    }

    /**
     * Writes a representation of the specified row for
     * {@link EventType#END_ROW_SUCCESS} events.
     *
     * @param  eventType  type of event.
     * @param  row        details about the event.
     *
     * @return the specified field row unchanged.
     *
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    public PluginDataRow processEvent(EventType eventType,
                                      PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {
        if (EventType.END_ROW_SUCCESS.equals(eventType)) {
            writeRow(row);
        }
        return row;
    }

    protected abstract String getRowRepresentation(PluginDataRow row);
    protected abstract File getFile(PluginDataRow row,
                                    File baseDirectory);
    protected abstract String getInitFailureMessage();

    private void writeRow(PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        final String representation = getRowRepresentation(row);
        if (representation != null) {
            final File file = getFile(row, directory);
            FileWriter fileWriter = null;
            try {
                fileWriter = new FileWriter(file, true);
                fileWriter.write(representation);
            } catch (Throwable t) {
                throw new ExternalSystemException(
                        "Failed to write row representation to " +
                        file.getAbsolutePath(), t);
            } finally {
                closeWriter(fileWriter, file);
            }

            if (isWritableByEverybody) {
                try {
                    if (! file.setWritable(true, false)) {
                        LOG.warn("failed to setWritable for " +
                                 file.getAbsolutePath());
                    }
                } catch (Throwable t) {
                    LOG.warn("failed to setWritable for " +
                             file.getAbsolutePath(), t);                                            
                }
            }
        }
    }

    /**
     * Utility to close the file writer.
     *
     * @param fileWriter  the writer used to write to the file.
     * @param file        the file being written.
     */
    private void closeWriter(FileWriter fileWriter,
                             File file) {
        if (fileWriter != null) {
            try {
                fileWriter.close();
            } catch (Throwable t) {
                LOG.warn("failed to close " + file.getAbsolutePath(), t);
            }
        }
    }

    private static final Logger LOG = Logger.getLogger(RowWriterPlugin.class);
}