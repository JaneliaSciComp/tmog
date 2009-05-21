/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.wormtracker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.filefilter.FileNamePatternFilter;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowListener;
import org.janelia.it.ims.tmog.target.FileTarget;
import org.janelia.it.ims.tmog.target.Target;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This class adds summary prefix information to data rows
 * based upon target file (directory/zip file) contents.
 *
 * @author Eric Trautman
 */
public class SummaryPrefixPlugin implements RowListener {

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(SummaryPrefixPlugin.class);

    /** The plugin data name for summary prefix information. */
    private static final String SUMMARY_PREFIX_NAME = "Prefix";

    private static final String SUMMARY_EXTENSION = ".summary";

    private static final FileFilter SUMMARY_FILTER =
            new FileNamePatternFilter(".*\\.summary");

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public SummaryPrefixPlugin() {
    }

    /**
     * Verifies that the plugin is ready for use by checking external
     * dependencies.
     *
     * @param config the plugin configuration.
     *
     * @throws ExternalSystemException
     *   if the plugin can not be initialized.
     */
    public void init(PluginConfiguration config) throws ExternalSystemException {
        // nothing to check
    }

    /**
     * Processes the specified event.
     *
     * @param  eventType  type of event.
     * @param  row        details about the event.
     *
     * @return the data row for processing (with any
     *         updates from this plugin).
     *
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     *
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    public PluginDataRow processEvent(EventType eventType,
                                      PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        PluginDataRow updatedRow = row;
        if (eventType.equals(EventType.START)) {
            updatedRow = startingCopy(row);
        }
        return updatedRow;
    }

    /**
     * Processes start event.
     *
     * @param  row  the row information for the event.
     *
     * @return row information with updated rank.
     *
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     *
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    private PluginDataRow startingCopy(PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        String summaryFileName = null;

        DataRow dataRow = row.getDataRow();
        Target target = dataRow.getTarget();
        if (target instanceof FileTarget) {
            FileTarget fileTarget = (FileTarget) target;
            File file = fileTarget.getFile();

            if (file.isDirectory()) {

                File[] summaryFiles = file.listFiles(SUMMARY_FILTER);
                if (summaryFiles.length > 0) {
                    summaryFileName = summaryFiles[0].getName();
                }

                if (summaryFiles.length > 1) {
                    StringBuilder msg = new StringBuilder();
                    msg.append("multiple summary files exist in ");
                    msg.append(file.getAbsolutePath());
                    msg.append(", files are: ");
                    for (File summaryFile : summaryFiles) {
                        msg.append(summaryFile.getName());
                        msg.append(", ");
                    }
                    msg.setLength(msg.length() - 2);
                    LOG.warn(msg.toString());
                }

            } else {

                String fileName = file.getName();
                if (fileName.endsWith(".zip")) {
                    try {
                        ZipFile zipFile = new ZipFile(file);
                        Enumeration<? extends ZipEntry> entries =
                                zipFile.entries();
                        ZipEntry entry;
                        while (entries.hasMoreElements()) {
                            entry = entries.nextElement();
                            fileName = entry.getName();
                            if (fileName.endsWith(SUMMARY_EXTENSION)) {
                                // strip off parent directory info
                                File summaryFile = new File(fileName);
                                summaryFileName = summaryFile.getName();
                                break;
                            }
                        }
                    } catch (IOException e) {
                        LOG.error("failed to open zip file " +
                                  file.getAbsolutePath(), e);
                    }
                }
            }

            if (summaryFileName == null) {
                LOG.error("summary file not found in " +
                          file.getAbsolutePath());
                // TODO: should a data exception be thrown here?
            }
        }

        if (summaryFileName != null) {
            final int prefixLen = summaryFileName.length() -
                                  SUMMARY_EXTENSION.length();
            row.setPluginDataValue(SUMMARY_PREFIX_NAME,
                                   summaryFileName.substring(0, prefixLen));
        }

        return row;
    }

}