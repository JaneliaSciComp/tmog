/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.chacrm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.PluginUtil;
import org.janelia.it.ims.tmog.plugin.RenamePluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowListener;

import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * This class removes ChaCRM data for renamed images.
 * It is intended to assist in the movement of ChaCRM images
 * to other projects.
 *
 * @author Eric Trautman
 */
public class CleanupChacrmManager implements RowListener {

    /** The logger for this class. */
    private static final Log LOG =
            LogFactory.getLog(CleanupChacrmManager.class);

    /**
     * The data access object for retrieving and updating transformant data.
     */
    private TransformantDao dao;

    /**
     * The configured pattern value used to identify when files are being
     * renamed from one area of the lab image file share to another.
     */
    private String labImageSharePatternValue;

    /** The compiled pattern for the labImageSharePatternValue. */
    private Pattern labImageSharePattern;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public CleanupChacrmManager() {
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
            setDao();
            dao.checkAvailability();
            labImageSharePatternValue =
                    config.getProperty("labImageSharePattern");
            if (labImageSharePatternValue != null) {
                labImageSharePattern =
                        Pattern.compile(labImageSharePatternValue);
            }
        } catch (PatternSyntaxException pse) {
            throw new ExternalSystemException(
                    "Failed to initialize ChaCRM plugin because of invalid " +
                    "labImageSharePattern '" + labImageSharePatternValue +
                    "'.  " + pse.getMessage(), pse);
        } catch (ExternalSystemException e) {
            throw new ExternalSystemException(
                    "Failed to initialize ChaCRM plugin.  " + e.getMessage(),
                    e);
        }
    }

    /**
     * Processes the specified copy event.
     *
     * @param  eventType  type of copy event.
     * @param  row        details about the event.
     *
     * @return the rename field row for processing (with any
     *         updates from this plugin).
     *
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    public PluginDataRow processEvent(EventType eventType,
                                      PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        RenamePluginDataRow dataRow = PluginUtil.castRenameRow(row, this);
        if (EventType.END_ROW_SUCCESS.equals(eventType)) {
            File fromFile = dataRow.getFromFile();
            String fromFileName = fromFile.getAbsolutePath();
            ImageLocation fromFileImageLocation =
                    ChacrmEventManager.getImageLocation(fromFileName,
                                     labImageSharePattern);
            if (fromFileImageLocation != null) {
                try {
                    User user = User.getUser(row);
                    dao.deleteImageLocationAndRollbackStatus(
                            fromFileImageLocation, user);
                } catch (Exception e) {
                    // log this error, but allow transaction to complete
                    LOG.error("failed to remove " + fromFileImageLocation +
                              " for file " + fromFileName +
                              " from ChaCRM database", e);
                }
            }
        }
        return dataRow;
    }

    /**
     * Create the dao for this manager if it does not already exist.
     *
     * @throws ExternalSystemException
     *   if any error occurs during creation.
     */
    private synchronized void setDao() throws ExternalSystemException {
        if (dao == null) {
            dao = new TransformantDao();
        }
    }
}