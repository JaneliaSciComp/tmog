/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.chacrm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.imagerenamer.config.PluginConfiguration;
import org.janelia.it.ims.imagerenamer.plugin.CopyListener;
import org.janelia.it.ims.imagerenamer.plugin.ExternalDataException;
import org.janelia.it.ims.imagerenamer.plugin.ExternalSystemException;
import org.janelia.it.ims.imagerenamer.plugin.RenameFieldRow;

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
public class CleanupChacrmManager implements CopyListener {

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
     * {@link org.janelia.it.ims.imagerenamer.config.PluginFactory}.
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
            dao.checkConnection();
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
        } catch (SystemException e) {
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
    public RenameFieldRow processEvent(EventType eventType,
                                       RenameFieldRow row)
            throws ExternalDataException, ExternalSystemException {
        if (EventType.END_SUCCESS.equals(eventType)) {
            File fromFile = row.getFromFile();
            String fromFileName = fromFile.getAbsolutePath();
            ImageLocation fromFileImageLocation =
                    RenamerEventManager.getImageLocation(fromFileName,
                                     labImageSharePattern);
            if (fromFileImageLocation != null) {
                try {
                    dao.deleteImageLocationAndRollbackStatus(
                            fromFileImageLocation);
                } catch (Exception e) {
                    // log this error, but allow transaction to complete
                    LOG.error("failed to remove " + fromFileImageLocation +
                              " for file " + fromFileName +
                              " from ChaCRM database", e);
                }
            }
        }
        return row;
    }

    /**
     * Create the dao for this manager if it does not already exist.
     *
     * @throws SystemException if any error occurs during creation.
     */
    private synchronized void setDao() throws SystemException {
        if (dao == null) {
            dao = new TransformantDao();
        }
    }
}