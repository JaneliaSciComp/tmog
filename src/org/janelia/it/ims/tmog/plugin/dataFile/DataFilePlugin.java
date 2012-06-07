/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.dataFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.PropertyTokenList;
import org.janelia.it.ims.tmog.plugin.RowListener;
import org.janelia.it.utils.StringUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This plug-in loads a formatted data file that can be used to populate
 * fields.
 *
 * @author Eric Trautman
 */
public class DataFilePlugin
        implements RowListener {

    public static final String KEY_PROPERTY_NAME = "data-file.key";
    public static final String FILE_PROPERTY_NAME = "data-file.name";

    private PropertyTokenList keyField;

    private Map<String, String> rowFieldNameToItemPropertyNameMap;

    private Data data;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public DataFilePlugin() {
        this.rowFieldNameToItemPropertyNameMap = new HashMap<String, String>();
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


        final Map<String, String> props = config.getProperties();

        rowFieldNameToItemPropertyNameMap.clear();

        String dataFileKeyValue = null;
        String dataFileNameValue = null;

        String value;
        for (String key : props.keySet()) {

            value = props.get(key);

            if (KEY_PROPERTY_NAME.equals(key)) {

                dataFileKeyValue = value;
                checkRequiredProperty(key, dataFileKeyValue);
                setDataFileKey(value, props);

            } else if (FILE_PROPERTY_NAME.equals(key)) {

                dataFileNameValue = value;
                checkRequiredProperty(key, dataFileNameValue);
                parseDataFile(value);

            } else if (StringUtil.isDefined(key) &&
                       StringUtil.isDefined(value)) {
                rowFieldNameToItemPropertyNameMap.put(key, value);
            }
        }

        checkRequiredProperty(KEY_PROPERTY_NAME, dataFileKeyValue);
        checkRequiredProperty(FILE_PROPERTY_NAME, dataFileNameValue);

        LOG.info("init: mapped " + rowFieldNameToItemPropertyNameMap.size() +
                 " fields to data file item properties");
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

        final List<String> itemNames =
                keyField.deriveValues(row.getDisplayNameToFieldMap(), false);
        if (itemNames.size() > 0) {
            final String itemName = itemNames.get(0);
            final Item item = data.getItem(itemName);
            String propertyName;
            for (String field : rowFieldNameToItemPropertyNameMap.keySet()) {
                propertyName = rowFieldNameToItemPropertyNameMap.get(field);
                row.setPluginDataValue(field,
                                       item.getPropertyValue(propertyName));
            }
        }
        return row;
    }

    private void setDataFileKey(String keyPropertyValue,
                                Map<String, String> props)
            throws ExternalSystemException {

        try {
            keyField = new PropertyTokenList(keyPropertyValue, props);
        } catch (Exception e) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG + e.getMessage(), e);
        }

    }

    private void parseDataFile(String dataFileName)
            throws ExternalSystemException {

        final File dataFile = new File(dataFileName);

        if (! dataFile.canRead()) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG + "Unable to read data file " +
                    dataFile.getAbsolutePath() + ".");
        }

        try {
            JAXBContext ctx = JAXBContext.newInstance(Data.class);
            Unmarshaller unm = ctx.createUnmarshaller();
            Object o = unm.unmarshal(dataFile);
            if (o instanceof Data) {
                this.data = (Data) o;
            }
        } catch (Exception e) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG + "Failed to parse data file " +
                    dataFile.getAbsolutePath() + ".");
        }

        LOG.info("parseDataFile: loaded " + this.data.size() +
                 " data items from " + dataFile.getAbsolutePath());
    }

    private void checkRequiredProperty(String name,
                                       String value)
            throws ExternalSystemException {
        if ((value == null) || (value.length() == 0)) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG + "The '" + name +
                    "' property must be defined.");
        }
    }

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(DataFilePlugin.class);

    private static final String INIT_FAILURE_MSG =
            "Failed to initialize Data File plug-in.  ";
}