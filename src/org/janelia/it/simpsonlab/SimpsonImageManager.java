/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.simpsonlab;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.PluginUtil;
import org.janelia.it.ims.tmog.plugin.RenamePluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowListener;

/**
 * This class handles events "published" by the transmogrifier tool.
 *
 * @author Eric Trautman
 */
public class SimpsonImageManager implements RowListener {

    /**
     * The data access object for retrieving and updating image data.
     */
    private SpecimenDao dao;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
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
        } catch (ExternalSystemException e) {
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
                                            PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {
        RenamePluginDataRow dataRow = PluginUtil.castRenameRow(row, this);
        if (EventType.START.equals(eventType)) {
            dataRow = startingCopy(dataRow);
        }
        return dataRow;
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
            row.setPluginDataValue(SPECIMEN_NUMBER_NAME,
                                   specimenNumber);
            if (LOG.isInfoEnabled()) {
                LOG.info("Retrieved specimen number " + specimenNumber +
                         " for line '" + line + "'.  Row data is now: " + row);
            }
        } catch (ExternalSystemException e) {
            throw new ExternalSystemException(
                    "Failed to retrieve specimen for number for line '" +
                    line + "'.  Detailed data is: " + row, e);
        }

        return row;
    }

    /**
     * Create the dao for this manager if it does not already exist.
     *
     * @throws ExternalSystemException
     *   if any error occurs during creation.
     */
    private synchronized void setDao() throws ExternalSystemException {
        if (dao == null) {
            dao = new SpecimenDao();
        }
    }

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(SimpsonImageManager.class);

    private static final String SPECIMEN_NUMBER_NAME = "Specimen Number";    
}