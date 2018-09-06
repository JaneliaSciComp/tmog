/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowListener;

import java.util.HashMap;
import java.util.Map;

/**
 * This plug-in retrieves sequence numbers from the Image database.
 *
 * @author Eric Trautman
 */
public class ImageSequencePlugin implements RowListener {

    /** The data access object for retrieving and updating image data. */
    private ImageDao dao;

    /** The key used to locate jdbc connection configuration for the plug-in. */
    private String dbConfigurationKey;

    /** Map of data field names to associated sequence namespace builders. */
    private Map<String, CompositeSetter> fieldNameToNamespaceBuilderMap;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public ImageSequencePlugin() {
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
        Map<String, String> props = config.getProperties();
        this.dbConfigurationKey = props.remove("db.config.key");
        if ((this.dbConfigurationKey == null) ||
            (this.dbConfigurationKey.length() < 1)) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG +
                    "Please specify a value for the db.config.key " +
                    "plug-in property.");
        }
        
        if (props.size() < 1) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG +
                    "Please specify at least one sequence " +
                    "plug-in property.");
        }

        this.fieldNameToNamespaceBuilderMap =
                new HashMap<String, CompositeSetter>();
        for (String fieldName : props.keySet()) {
            // TODO: find a way to validate that the data field exists
            fieldNameToNamespaceBuilderMap.put(
                    fieldName,
                    new CompositeSetter(fieldName,
                                        props.get(fieldName),
                                        props));
        }

        try {
            setDao();
            dao.checkAvailability();
        } catch (ExternalSystemException e) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG +
                    e.getMessage(),
                    e);
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
        if (EventType.START_ROW.equals(eventType)) {
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

        String namespace = null;
        try {
            CompositeSetter namespaceSetter;
            int seqNumber;
            for (String seqProperty : fieldNameToNamespaceBuilderMap.keySet()) {
                namespaceSetter =
                        fieldNameToNamespaceBuilderMap.get(seqProperty);
                namespace = namespaceSetter.getValue(row);
                seqNumber = dao.getNextSequenceNumber(namespace);
                row.setPluginDataValue(seqProperty, seqNumber);
                if (LOG.isInfoEnabled()) {
                    LOG.info("Retrieved sequence number " + seqNumber +
                             " for " + namespace + ".");
                }
            }

        } catch (ExternalSystemException e) {
            throw new ExternalSystemException(
                    "Failed to retrieve sequence number for " +
                    namespace + ".  Detailed data is: " + row, e);
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
            dao = new ImageDao(dbConfigurationKey);
        }
    }

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(ImageSequencePlugin.class);

    private static final String INIT_FAILURE_MSG =
            "Failed to initialize Sequence Number plug-in.  ";

}