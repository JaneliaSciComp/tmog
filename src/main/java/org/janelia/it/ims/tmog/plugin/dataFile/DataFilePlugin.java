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
import org.janelia.it.ims.tmog.plugin.RowUpdater;
import org.janelia.it.utils.PathUtil;
import org.janelia.it.utils.StringUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * This plug-in loads a formatted data file that can be used to populate
 * fields.
 *
 * @author Eric Trautman
 */
public class DataFilePlugin
        implements RowUpdater {

    public static final String TMOG_ROW_KEY_PROPERTY_NAME = "tmog-row.key";
    public static final String FILE_PROPERTY_NAME = "data-file.name";
    public static final String TSV_FILE_KEY_PROPERTY_NAME = "data-file.tsv-key";

    private PropertyTokenList keyField;

    private Map<String, String> rowFieldNameToItemPropertyNameMap;

    private Data data;

    private String tsvKeyFieldName;

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

            if (TMOG_ROW_KEY_PROPERTY_NAME.equals(key)) {
                dataFileKeyValue = value;
            } else if (FILE_PROPERTY_NAME.equals(key)) {
                dataFileNameValue = value;
            } else if (TSV_FILE_KEY_PROPERTY_NAME.equals(key)) {
                tsvKeyFieldName = value;
            } else if (StringUtil.isDefined(key) &&
                       StringUtil.isDefined(value)) {
                rowFieldNameToItemPropertyNameMap.put(key, value);
            }
        }

        checkRequiredProperty(TMOG_ROW_KEY_PROPERTY_NAME, dataFileKeyValue);
        setDataFileKey(dataFileKeyValue, props);

        checkRequiredProperty(FILE_PROPERTY_NAME, dataFileNameValue);

        final File dataFile = new File(PathUtil.convertPath(dataFileNameValue));

        if (! dataFile.canRead()) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG + "Unable to read data file " +
                    dataFile.getAbsolutePath() + ".");
        }

        //noinspection ConstantConditions
        if (dataFileNameValue.endsWith(".xml")) {
            parseXmlDataFile(dataFile);
        } else if (dataFileNameValue.endsWith(".tsv")) {
            checkRequiredProperty(TSV_FILE_KEY_PROPERTY_NAME, tsvKeyFieldName);
            parseTsvDataFile(dataFile);
        } else {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG + "Data file " +
                    dataFile.getAbsolutePath() +
                    " must have '.xml' or '.tsv' suffix.");
        }

        LOG.info("init: mapped " + rowFieldNameToItemPropertyNameMap.size() +
                 " fields to data file item properties");
    }

    /**
     * Allows plug-in to update the specified row.
     *
     * @param  row  row to be updated.
     *
     * @return the data field row for processing (with any
     *         updates from this plugin).
     *
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     *
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    public PluginDataRow updateRow(PluginDataRow row)
            throws ExternalDataException, ExternalSystemException {

        final List<String> itemNames =
                keyField.deriveValues(row.getDisplayNameToFieldMap(), false);
        if (itemNames.size() > 0) {
            final String itemName = itemNames.get(0);
            final Item item = data.getItem(itemName);
            if (item != null) {
                String propertyName;
                for (String field : rowFieldNameToItemPropertyNameMap.keySet()) {
                    propertyName = rowFieldNameToItemPropertyNameMap.get(field);
                    row.applyPluginDataValue(field,
                                             item.getPropertyValue(propertyName));
                }
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

    private void parseXmlDataFile(File dataFile)
            throws ExternalSystemException {


        LOG.info("parseXmlDataFile: parsing " + dataFile.getAbsolutePath());

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
                    dataFile.getAbsolutePath() + ".", e);
        }

        verifyDataWasLoaded(dataFile);

        LOG.info("parseXmlDataFile: loaded " + data.size() +
                 " data items from " + dataFile.getAbsolutePath());
    }

    private void parseTsvDataFile(File dataFile)
            throws ExternalSystemException {

        LOG.info("parseTsvDataFile: parsing " + dataFile.getAbsolutePath());

        data = new Data();

        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(dataFile));
            String line = in.readLine();
            String[] fieldNames = null;
            String[] fieldValues;
            Item item;
            int itemNameIndex = -1;
            while (line != null) {

                if (fieldNames == null) {

                    fieldNames = TSV.split(line);

                    for (int i = 0; i < fieldNames.length; i++) {
                        if (tsvKeyFieldName.equals(fieldNames[i])) {
                            itemNameIndex = i;
                            break;
                        }
                    }

                    if (itemNameIndex == -1) {
                        throw new ExternalSystemException(
                                INIT_FAILURE_MSG + "Missing '" +
                                tsvKeyFieldName +
                                "' header field in data file " +
                                dataFile.getAbsolutePath() + ".");
                    }

                } else {

                    fieldValues = TSV.split(line);

                    if (itemNameIndex < fieldValues.length) {

                        item = new Item(fieldValues[itemNameIndex]);

                        for (int i = 0;
                             ((i < fieldNames.length) &&
                              (i < fieldValues.length));
                             i++) {

                            if (i != itemNameIndex) {
                                item.addProperty(new Property(fieldNames[i],
                                                              fieldValues[i]));
                            }
                        }

                        data.addItem(item);
                    }
                }

                line = in.readLine();
            }

        } catch (ExternalSystemException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG + "Failed to parse data file " +
                    dataFile.getAbsolutePath() + ".", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOG.error("failed to close " + dataFile.getAbsolutePath());
                }
            }

        }

        verifyDataWasLoaded(dataFile);

        LOG.info("parseTsvDataFile: loaded " + data.size() +
                 " data items from " + dataFile.getAbsolutePath());
    }

    private void verifyDataWasLoaded(File dataFile)
            throws ExternalSystemException {
        if ((data == null) || (data.size() == 0)) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG + "No data was found in " +
                    dataFile.getAbsolutePath() + ".");
        }
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

    private static final Pattern TSV = Pattern.compile("\t");
}