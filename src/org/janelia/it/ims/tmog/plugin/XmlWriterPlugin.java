/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
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
 * This plug-in writes transmogrifier row data to an XML file.
 *
 * @author Eric Trautman
 */
public class XmlWriterPlugin implements RowListener {

    /**
     * The configured base directory for all xml files.
     */
    private File directory;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public XmlWriterPlugin() {
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
    public void init(PluginConfiguration config) throws ExternalSystemException {
        try {
            String directoryName = config.getProperty("directory");
            if ((directoryName != null) && (directory.length() > 0)) {
                directoryName = PathUtil.convertPath(directoryName);
                directory = new File(directoryName);
                if (directory.exists()) {
                    if (directory.isDirectory()) {
                        if (!directory.canWrite()) {
                            throw new ExternalSystemException(
                                    INIT_FAILURE_MSG +
                                    "Unable to write to directory: " +
                                    directory.getAbsolutePath());
                        }
                    } else {
                        throw new ExternalSystemException(
                                INIT_FAILURE_MSG +
                                "Configured directory (" +
                                directory.getAbsolutePath() +
                                ") for is not a directory.");
                    }
                } else {
                    throw new ExternalSystemException(
                            INIT_FAILURE_MSG +
                            "Unable to find directory: " +
                            directory.getAbsolutePath());
                }
            } else {
                directory = null;
            }
        } catch (Throwable t) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG + t.getMessage(),
                    t);
        }
    }

    /**
     * Writes an XML representation of the specified row for
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
            writeXml(row);
        }
        return row;
    }

    private void writeXml(PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        XmlStringBuilder xml = new XmlStringBuilder("row");
        xml.setRow(row, null);

        final File targetFile = row.getTargetFile();
        final String xmlFileName = targetFile.getName() + ".xml";
        File xmlDirectory;
        if (directory != null) {
            xmlDirectory = directory;
        } else {
            xmlDirectory = targetFile.getParentFile();
        }
        File file = new File(xmlDirectory, xmlFileName);
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file);
            fileWriter.write(xml.toString());
        } catch (Throwable t) {
            throw new ExternalSystemException(
                    "Failed to write session log file: " +
                    file.getAbsolutePath(), t);
        } finally {
            closeWriter(fileWriter, file);
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
                LOG.warn("failed to close xml file: " +
                         file.getAbsolutePath(), t);
            }
        }
    }

    private static final Logger LOG = Logger.getLogger(XmlWriterPlugin.class);

    private static final String INIT_FAILURE_MSG =
            "Failed to initialize XML writer plug-in.  ";
}