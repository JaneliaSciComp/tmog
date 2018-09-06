/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.field.PluginDataModel;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.SessionListener;

import java.util.HashMap;
import java.util.List;

/**
 * This plug-in retrieves sequence numbers from the Image database.
 * It maps retrieved sequence numbers to a group value so that the same
 * sequence number can be reused for different rows with the same group
 * value.  The group value map is cleared at the beginning of each session. 
 *
 * @author Eric Trautman
 */
public class GroupSequencePlugin
        implements SessionListener {

    /** The data access object for retrieving and updating image data. */
    private ImageDao dao;

    /** The key used to locate jdbc connection configuration for the plug-in. */
    private String dbConfigurationKey;

    /** Name of the field containing the group identifier. */
    private String groupFieldName;

    /** Name of the field to be populated with the actual sequence number. */
    private String targetFieldName;

    /** The group sequence namespace identifier. */
    private String namespaceIdentifier;

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
     * @throws ExternalSystemException
     *   if the plugin can not be initialized.
     */
    @Override
    public void init(PluginConfiguration config) throws ExternalSystemException {
        this.dbConfigurationKey = getRequiredProperty("db.config.key", config);
        this.groupFieldName = getRequiredProperty("groupFieldName", config);
        this.targetFieldName = getRequiredProperty("targetFieldName", config);
        this.namespaceIdentifier = getRequiredProperty("namespace", config);
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

    @Override
    public List<DataRow> startSession(List<DataRow> modelRows)
            throws ExternalDataException, ExternalSystemException {

        Integer groupFieldIndex = null;
        Integer targetFieldIndex = null;

        if (modelRows.size() > 0) {
            final DataRow firstRow = modelRows.get(0);
            final int fieldCount = firstRow.getFieldCount();
            DataField field;
            String displayName;
            for (int i = 0; i < fieldCount; i++) {
                field = firstRow.getField(i);
                displayName = field.getDisplayName();
                if (groupFieldName.equals(displayName)) {
                    groupFieldIndex = i;
                } else if (targetFieldName.equals(displayName)) {
                    targetFieldIndex = i;
                }
            }

            if (targetFieldIndex != null) {
                field = firstRow.getField(targetFieldIndex);
                if (! (field instanceof PluginDataModel)) {
                    throw new ExternalDataException(
                            "Target field '" + targetFieldName +
                            "' is not configured as a pluginData element.  " +
                            "Please correct your data fields configuration.");
                }
            }
        }

        if (groupFieldIndex == null) {
            throw new ExternalDataException(
                    "Group field '" + groupFieldName +
                    "' is missing from rows.  Please correct the " +
                    "Group Sequence Plugin configuration.");
        }

        if (targetFieldIndex == null) {
            throw new ExternalDataException(
                    "Target field '" + targetFieldName +
                    "' is missing from rows.  Please correct the " +
                    "Group Sequence Plugin configuration.");
        }

        retrieveAndSetSequenceNumbers(modelRows,
                                      groupFieldIndex,
                                      targetFieldIndex);

        return modelRows;
    }

    @Override
    public void endSession(String message)
            throws ExternalDataException, ExternalSystemException {
        // ignore this event
    }

    private void retrieveAndSetSequenceNumbers(List<DataRow> modelRows,
                                               Integer groupFieldIndex,
                                               Integer targetFieldIndex)
            throws ExternalSystemException {
        HashMap<String, Integer> sequenceNameToNumberMap =
                new HashMap<String, Integer>();

        DataField groupNameField;
        PluginDataModel targetField;
        String priorTargetFieldValue;
        String name;
        Integer number;
        for (DataRow row : modelRows) {

            groupNameField = row.getField(groupFieldIndex);
            name = groupNameField.getCoreValue();
            targetField = (PluginDataModel) row.getField(targetFieldIndex);
            priorTargetFieldValue = targetField.getCoreValue();

            if (priorTargetFieldValue.length() > 0) {

                LOG.info("keeping previous sequence number " +
                         priorTargetFieldValue);

            } else if ((name != null) && (name.length() > 0)) {

                number = sequenceNameToNumberMap.get(name);

                if (number == null) {
                    number = dao.getNextSequenceNumber(namespaceIdentifier);
                    sequenceNameToNumberMap.put(name, number);
                    if (LOG.isInfoEnabled()) {
                        LOG.info("retrieved sequence number " + number +
                                 " for '" + namespaceIdentifier +
                                 "' namespace and '" + name + "' group");
                    }
                }

                targetField.setValue(number);
            }

        }
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
            "Failed to initialize the Group Sequence Plugin.  ";

}