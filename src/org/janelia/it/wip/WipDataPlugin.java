/*
 * Copyright 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.wip;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.RenamePluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowListener;
import org.janelia.it.ims.tmog.plugin.RowValidator;

/**
 * This plug-in retrieves batch names from the Work In Progress (WIP)
 * database.
 *
 * @author Eric Trautman
 */
public class WipDataPlugin
        implements RowValidator, RowListener {

    /** The data access object for retrieving WIP data. */
    private WipDao dao;

    private String batchNameFieldName = "Batch Name";
    private String batchNumberFieldName = "Batch Number";
    private String batchColorFieldName = "Batch Color";

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public WipDataPlugin() {
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

            String fieldName = config.getProperty("batchNameFieldName");
            if (fieldName != null) {
                batchNameFieldName = fieldName;
            }
            fieldName = config.getProperty("batchNumberFieldName");
            if (fieldName != null) {
                batchNumberFieldName = fieldName;
            }
            fieldName = config.getProperty("batchColorFieldName");
            if (fieldName != null) {
                batchColorFieldName = fieldName;
            }

        } catch (ExternalSystemException e) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG +
                    e.getMessage(),
                    e);
        }
    }

    /**
     * Validates the set of information collected for
     * a specific row.
     *
     * @param  row  the user supplied information to be validated.
     *
     * @throws ExternalDataException
     *   if the data is not valid.
     *
     * @throws ExternalSystemException
     *   if any error occurs while validating the data.
     */
    public void validate(PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        String batchName = getBatchName(row, false);

        if (batchName == null) {
            StringBuilder sb = new StringBuilder(128);
            sb.append("An existing Batch Name cannot be found for ");
            if (row instanceof RenamePluginDataRow) {
                sb.append(((RenamePluginDataRow)row).getFromFile().getName());
            } else {
                sb.append(row.getDataRow().getTarget().getName());
            }
            sb.append(".  \nPlease verify your Batch Number and Color settings.");
            throw new ExternalDataException(sb.toString());
        }
    }

    /**
     * Notifies this plug-in that an event has occurred.
     *
     * @param  eventType  type of event.
     * @param  row        details about the event.
     *
     * @return the field row for processing (with any updates from this plugin).
     *
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    public PluginDataRow processEvent(EventType eventType,
                                      PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {
        if (EventType.START.equals(eventType)) {
            row = startingEvent(row);
        }
        return row;
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
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    private PluginDataRow startingEvent(PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        String batchName = getBatchName(row, true);

        if (batchName == null) {
            throw new ExternalDataException(
                    "Failed to retrieve batch name for data row " + row);
        } else {
            row.setPluginDataValue(batchNameFieldName, batchName);
        }

        return row;
    }

    private String getBatchName(PluginDataRow row,
                                boolean logResult)
            throws ExternalSystemException {

        String batchName;
        try {
            String batchNumber = row.getCoreValue(batchNumberFieldName);
            String batchColor = row.getCoreValue(batchColorFieldName);
            batchName = dao.getBatchName(batchNumber, batchColor);

            if (logResult && LOG.isInfoEnabled()) {
                LOG.info("Retrieved batch name '" + batchName +
                         "' for number '" + batchNumber + "' and color '" +
                         batchColor + "'.");
            }

        } catch (ExternalSystemException e) {
            throw new ExternalSystemException(
                    "Failed to retrieve batch name for data row " + row,
                    e);
        }

        return batchName;
    }

    /**
     * Create the dao for this manager if it does not already exist.
     *
     * @throws ExternalSystemException
     *   if any error occurs during creation.
     */
    private synchronized void setDao() throws ExternalSystemException {
        if (dao == null) {
            dao = new WipDao("wip");
        }
    }

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(WipDataPlugin.class);

    private static final String INIT_FAILURE_MSG =
            "Failed to initialize WIP Data plug-in.  ";
}