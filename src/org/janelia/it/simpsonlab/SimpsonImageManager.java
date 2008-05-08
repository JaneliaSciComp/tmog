/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.simpsonlab;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.imagerenamer.config.PluginConfiguration;
import org.janelia.it.ims.imagerenamer.plugin.CopyListener;
import org.janelia.it.ims.imagerenamer.plugin.ExternalDataException;
import org.janelia.it.ims.imagerenamer.plugin.ExternalSystemException;
import org.janelia.it.ims.imagerenamer.plugin.RenamePluginDataRow;

import java.io.File;

/**
 * This class handles events "published" by the image renamer tool.
 *
 * @author Eric Trautman
 */
public class SimpsonImageManager implements CopyListener {

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(SimpsonImageManager.class);

    /**
     * The data access object for retrieving and updating image data.
     */
    private ImageDao dao;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.imagerenamer.config.PluginFactory}.
     */
    public SimpsonImageManager() {
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
        } catch (SystemException e) {
            throw new ExternalSystemException(
                    "Failed to initialize Simpson Lab Image plugin.  " +
                    e.getMessage(),
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
    public RenamePluginDataRow processEvent(EventType eventType,
                                       RenamePluginDataRow row)
            throws ExternalDataException, ExternalSystemException {
        switch (eventType) {
            case START:
                row = startingCopy(row);
                break;
            case END_SUCCESS:
// Removed successful copy processing because this will now be handled by
// the secondary data generation process.
// Leaving the code intact, just in case we need to quickly restore it
// in the future.
//                completedSuccessfulCopy(row);
                break;
        }
        return row;
    }

    /**
     * Processes start copy event.
     *
     * @param  row  the row information for the event.
     *
     * @return row information with updated rank.
     *
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    private RenamePluginDataRow startingCopy(RenamePluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        Line line = null;
        try {
            line = new Line(row);
            int specimenNumber = dao.getNextSpecimenNumber(line);
            row.setPluginDataValue(ImageProperty.SPECIMEN_NUMBER_NAME,
                                   specimenNumber);
            if (LOG.isInfoEnabled()) {
                LOG.info("Retrieved specimen number " + specimenNumber +
                         " for line '" + line + "'.  Row data is now: " + row);
            }
        } catch (SystemException e) {
            throw new ExternalSystemException(
                    "Failed to retrieve specimen for number for line '" +
                    line + "'.  Detailed data is: " + row, e);
        }

        return row;
    }

    /**
     * Processes completed copy successfully event.
     *
     * @param  row  the row information for the event.
     *
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    private void completedSuccessfulCopy(RenamePluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        Image image;
        String fileName = null;
        File renamedFile = row.getRenamedFile();
        if (renamedFile != null) {
            fileName = renamedFile.getAbsolutePath();
        }
        try {
            Line line = new Line(row);
            image = new Image(line, row);
            image = dao.addImage(image);
            if (LOG.isInfoEnabled()) {
                LOG.info("successfully persisted image metadata (" + image +
                         ") for " + fileName);
            }
        } catch (Exception e) {
            throw new ExternalSystemException(
                    "Failed to save image data for " + fileName +
                    ".  Detailed data is: " + row, e);
        }
    }

    /**
     * Create the dao for this manager if it does not already exist.
     *
     * @throws SystemException if any error occurs during creation.
     */
    private synchronized void setDao() throws SystemException {
        if (dao == null) {
            dao = new ImageDao();
        }
    }
}