/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.dataFile;

import loci.formats.CoreMetadata;
import loci.formats.in.ZeissLSMReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.config.PluginConfiguration;
import org.janelia.it.ims.tmog.plugin.ExternalDataException;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.RowUpdater;
import org.janelia.it.utils.StringUtil;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * This plug-in loads data from the Zeiss meta data block in an LSM file
 * to populate tmog fields.
 *
 * @author Eric Trautman
 */
public class ZeissLsmDataPlugin
        implements RowUpdater {

    public static final String PLUGIN_SCALE_PROPERTY_NAME = "plugin-scale";

    /** Maps row fields to their Zeiss property name.  */
    private Map<String, String> rowFieldNameToLsmPropertyMap;

    /** Maps row fields to a map of Zeiss property values to field values. */
    private Map<String, Map<String, String>>
            rowFieldNameToLsmPropertyValueMapMap;

    /** Scale for rounding decimal values. */
    private Integer scale;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public ZeissLsmDataPlugin() {
        this.rowFieldNameToLsmPropertyMap = new HashMap<String, String>();
        this.rowFieldNameToLsmPropertyValueMapMap =
                new HashMap<String, Map<String, String>>();
        this.scale = null;
    }

    /**
     * Initializes the plug-in and verifies that it is ready for use by
     * checking external dependencies.
     *
     * @param  config  the plugin configuration.
     *
     * @throws ExternalSystemException
     *   if the plugin can not be initialized.
     */
    public void init(PluginConfiguration config) throws ExternalSystemException {

        final Map<String, String> props = config.getProperties();

        rowFieldNameToLsmPropertyMap.clear();

        String value;
        for (String key : props.keySet()) {
            value = props.get(key);

            if (PLUGIN_SCALE_PROPERTY_NAME.equals(key)) {

                try {
                    this.scale = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new ExternalSystemException(
                            INIT_FAILURE_MSG + "  The '" +
                            PLUGIN_SCALE_PROPERTY_NAME +
                            "' must be an integer value.", e);
                }

            } else if (StringUtil.isDefined(key) &&
                       StringUtil.isDefined(value)) {

                value = getPropertyNameAndAddMap(key, value);
                rowFieldNameToLsmPropertyMap.put(key, value);
            }
        }

        if (rowFieldNameToLsmPropertyMap.size() == 0) {
            throw new ExternalSystemException(
                    INIT_FAILURE_MSG +
                    "At least one field to property mapping must be specified.");
        }

        LOG.info("init: mapped " + rowFieldNameToLsmPropertyMap.size() +
                 " properties");
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

        final File file = row.getTargetFile();
        if (file != null) {
            try {
                ZeissLSMReader zlr = new ZeissLSMReader();
                zlr.initFile(file.getAbsolutePath());
                CoreMetadata data = zlr.getCore();
                if (data != null) {
                    updateRowWithZeissData(row, data);
                }
            } catch (Exception e) {
                LOG.error("failed to load Zeiss data from " +
                          file.getAbsolutePath(), e);
            }
        }

        return row;
    }

    private void updateRowWithZeissData(PluginDataRow row,
                                        CoreMetadata data) {
        String propertyName;
        Object value;
        for (String fieldName : rowFieldNameToLsmPropertyMap.keySet()) {

            propertyName = rowFieldNameToLsmPropertyMap.get(fieldName);
            value = data.seriesMetadata.get(propertyName);

            if ((scale != null) && (value instanceof Number)) {
                BigDecimal bd = new BigDecimal(String.valueOf(value));
                value = bd.setScale(scale, RoundingMode.HALF_UP);
            }

            if (value != null) {

                String valueStr = String.valueOf(value);

                final Map<String, String> propertyValueToFieldValueMap =
                        rowFieldNameToLsmPropertyValueMapMap.get(fieldName);

                if (propertyValueToFieldValueMap != null) {
                    String mappedValue =
                            propertyValueToFieldValueMap.get(valueStr);
                    if (mappedValue != null) {
                        valueStr = mappedValue;
                    }
                }

                row.applyPluginDataValue(fieldName,
                                         valueStr);
            }
        }
    }

    private String getPropertyNameAndAddMap(String fieldName,
                                            String nameWithEmbeddedMap) {

        String propertyName = nameWithEmbeddedMap;

        final int mapStart = nameWithEmbeddedMap.indexOf(FIELD_MAP_IDENTIFIER);

        if (mapStart > 0) {
            propertyName = nameWithEmbeddedMap.substring(0, mapStart);
            Map<String, String> map = new HashMap<String, String>();
            final int mapDataStart = mapStart + FIELD_MAP_IDENTIFIER.length();
            if (mapDataStart < nameWithEmbeddedMap.length()) {
                final String mapData =
                        nameWithEmbeddedMap.substring(mapDataStart);
                final String[] pairs = mapData.split("\\|");
                String key;
                String value;
                int keyEnd;
                int valueStart;
                for (String pair : pairs) {
                    keyEnd = pair.indexOf('=');
                    valueStart = keyEnd + 1;
                    if ((keyEnd > 0) && (valueStart < pair.length())) {
                        key = pair.substring(0, keyEnd);
                        value = pair.substring(valueStart);
                        map.put(key, value);
                    } else {
                        throw new IllegalArgumentException(
                                "The '" + fieldName +
                                "' property value has malformed map pair '" +
                                pair + "'.");
                    }
                }
            } else {
                throw new IllegalArgumentException(
                        "The '" + fieldName +
                        "' property value is missing map data.");
            }

            if (map.size() > 0) {
                rowFieldNameToLsmPropertyValueMapMap.put(fieldName, map);
            }
        }

        return propertyName;
    }

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(ZeissLsmDataPlugin.class);

    private static final String INIT_FAILURE_MSG =
            "Failed to initialize Zeiss LSM Data plug-in.  ";

    private static final String FIELD_MAP_IDENTIFIER = "$MAP:";

}