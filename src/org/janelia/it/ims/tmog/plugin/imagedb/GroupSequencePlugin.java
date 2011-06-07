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
import org.janelia.it.ims.tmog.plugin.PropertyTokenList;
import org.janelia.it.ims.tmog.plugin.RowListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This plug-in retrieves sequence numbers from the Image database.
 * It maps retrieved sequence numbers to a group value so that the same
 * sequence number can be reused for different rows with the same group
 * value.  The group value map is cleared at the beginning of each session. 
 *
 * @author Eric Trautman
 */
public class GroupSequencePlugin
        implements RowListener {

    /** The data access object for retrieving and updating image data. */
    private ImageDao dao;

    /** The key used to locate jdbc connection configuration for the plug-in. */
    private String dbConfigurationKey;

    /** Name of the field containing the group identifier. */
    private String groupFieldName;

    /** Name of the field to be populated with the actual sequence number. */
    private String targetFieldName;

    /** Token list for building the namespace identifier. */
    private PropertyTokenList namespaceIdentifier;

    /**
     * Map of field values to previously generated sequence numbers.
     * This map is defined as a thread local variable so that each
     * concurrent task can manage its own map.
     */
    private ThreadLocal<Map<String, Integer>> fieldValueToSequenceLocalMap =
            new ThreadLocal<Map<String, Integer>>() {
                @Override
                protected Map<String, Integer> initialValue() {
                    return new HashMap<String, Integer>();
                }
            };

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public GroupSequencePlugin() {
    }

    /**
     * Verifies that the plugin is ready for use by checking external
     * dependencies.
     *
     * @param  config  the plugin configuration.
     *
     * @throws org.janelia.it.ims.tmog.plugin.ExternalSystemException
     *   if the plugin can not be initialized.
     */
    public void init(PluginConfiguration config) throws ExternalSystemException {
        Map<String, String> props = config.getProperties();
        this.dbConfigurationKey = getRequiredProperty("db.config.key", config);
        this.groupFieldName = getRequiredProperty("groupFieldName", config);
        this.targetFieldName = getRequiredProperty("targetFieldName", config);
        final String namespacePattern =
                getRequiredProperty("namespace", config);
        try {
            this.namespaceIdentifier = new PropertyTokenList(namespacePattern,
                                                             props);
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
     * @throws org.janelia.it.ims.tmog.plugin.ExternalDataException
     *   if a recoverable data error occurs during processing.
     * @throws org.janelia.it.ims.tmog.plugin.ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    public PluginDataRow processEvent(EventType eventType,
                                      PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {
        if (EventType.START_ROW.equals(eventType)) {
            row = startingEvent(row);
        } else if (EventType.START_LOOP.equals(eventType)) {
            Map<String, Integer> map = fieldValueToSequenceLocalMap.get();
            map.clear();
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
     * @throws org.janelia.it.ims.tmog.plugin.ExternalDataException
     *   if a recoverable data error occurs during processing.
     * @throws org.janelia.it.ims.tmog.plugin.ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    private PluginDataRow startingEvent(PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {


        String namespace = null;
        try {
            final String value = row.getCoreValue(groupFieldName);
            Map<String, Integer> map = fieldValueToSequenceLocalMap.get();
            Integer seqNumber = map.get(value);
            if (seqNumber == null) {
                final List<String> namespaceList =
                        namespaceIdentifier.deriveValues(
                                row.getDisplayNameToFieldMap(),
                                false);
                namespace = namespaceList.get(0);
                seqNumber = dao.getNextSequenceNumber(namespace);
                map.put(value, seqNumber);
                if (LOG.isInfoEnabled()) {
                    LOG.info("Retrieved sequence number " + seqNumber +
                             " for " + namespace + " and value '" +
                             value +"'.");
                }
            }

            row.setPluginDataValue(targetFieldName, seqNumber);

        } catch (ExternalSystemException e) {
            throw new ExternalSystemException(
                    "Failed to retrieve sequence number for " +
                    namespace + ".  Detailed data is: " + row, e);
        }

        return row;
    }

    private String getRequiredProperty(String propertyName,
                                       PluginConfiguration config)
            throws ExternalSystemException {
        String value = config.getProperty(propertyName);
        if ((value == null) || (value.length() < 1)) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG +
                    "Please specify a value for the '" + propertyName +
                    "' plug-in property.");
        }
        return value;
    }

    /**
     * Create the dao for this manager if it does not already exist.
     *
     * @throws org.janelia.it.ims.tmog.plugin.ExternalSystemException
     *   if any error occurs during creation.
     */
    private synchronized void setDao() throws ExternalSystemException {
        if (dao == null) {
            dao = new ImageDao(dbConfigurationKey);
        }
    }

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(GroupSequencePlugin.class);

    private static final String INIT_FAILURE_MSG =
            "Failed to initialize Group Sequence Number plug-in.  ";

}