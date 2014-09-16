/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.janelia.it.ims.tmog.plugin.PluginDataRow;

import java.util.HashMap;
import java.util.Map;

/**
 * This class sets an image property using a configured field name
 * to retrieve the property value from a data field row.
 *
 * If the field name is specified as '$STATIC:value', then
 * the specified value will be used for all rows.
 * 
 * If the field name ends with an encoded map,
 * the map will be used to translate core field values to their
 * mapped database values.
 *
 * Maps are encoded like this:
 * [field name]$MAP:[key]=[value]|[key]=[value]
 *
 * @author Eric Trautman
 */
public class SimpleSetter implements ImagePropertySetter {

    private String propertyType;
    private String fieldName;
    private String staticValue;
    private Map<String, String> coreValueToDbValueMap;
    private String mapDefaultValue;

    /**
     * Value constructor.
     *
     * @param  propertyType  the type name of the property in the
     *                       image_property table.
     *
     * @param  fieldName     the display name of the data field that contains
     *                       the property value.
     */
    public SimpleSetter(String propertyType,
                        String fieldName) {
        this.propertyType = propertyType;
        this.fieldName = fieldName;
        this.staticValue = null;
        this.coreValueToDbValueMap = null;
        this.mapDefaultValue = null;

        final int staticStart = fieldName.indexOf(FIELD_STATIC_IDENTIFIER);
        if (staticStart > -1) {
            if (staticStart > 0) {
                throw new IllegalArgumentException(
                        "The property type name '" + propertyType +
                        "' static value must begin with '" +
                        FIELD_STATIC_IDENTIFIER +
                        "' but the field value is '" + fieldName + "'.");
            }
            final int staticValueStart =
                    staticStart + FIELD_STATIC_IDENTIFIER.length();
            if (staticValueStart < fieldName.length()) {
                this.staticValue = fieldName.substring(staticValueStart);
            }
        }

        final int mapStart = fieldName.indexOf(FIELD_MAP_IDENTIFIER);
        if (mapStart > 0) {
            this.fieldName = fieldName.substring(0, mapStart);
            this.coreValueToDbValueMap = new HashMap<String, String>();
            final int mapDataStart = mapStart + FIELD_MAP_IDENTIFIER.length();
            if (mapDataStart < fieldName.length()) {
                final String mapData = fieldName.substring(mapDataStart);
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
                        if (MAP_DEFAULT_KEY.equals(key)) {
                            this.mapDefaultValue = value;
                        } else {
                            this.coreValueToDbValueMap.put(key, value);
                        }
                    } else {
                        throw new IllegalArgumentException(
                                "The property type name '" + propertyType +
                                "' has malformed map pair '" + pair + "'.");
                    }
                }
            } else {
                throw new IllegalArgumentException(
                        "The field name value '" + fieldName +
                        "' for property type name '" + propertyType +
                        "' is missing map data.");
            }
        }

        if ((this.staticValue != null) &&
            (this.coreValueToDbValueMap != null)) {
            throw new IllegalArgumentException(
                    "The field name value '" + fieldName +
                    "' for property type name '" + propertyType +
                    "' may not contain both a static value and a map.");
        }

        if (this.fieldName.length() == 0) {
            throw new IllegalArgumentException(
                    "The field name value for property type name '" +
                    propertyType + "' must be defined.");
        }
    }

    /**
     * @return the display name of the row data field that contains
     *         this property's value.
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * @param  row    current row being processed.
     *
     * @return the core value or mapped database value for this field.
     */
    public String deriveValue(PluginDataRow row) {

        String value;

        if (staticValue != null) {

            value = staticValue;

        } else {

            value = row.getCoreValue(getFieldName());

            if (coreValueToDbValueMap != null) {
                String mappedValue = coreValueToDbValueMap.get(value);

                if ((mappedValue != null) && mappedValue.startsWith(FIELD_VALUE_OF_IDENTIFIER)) {
                    final String valueOfFieldName = mappedValue.substring(FIELD_VALUE_OF_IDENTIFIER.length());
                    mappedValue = row.getCoreValue(valueOfFieldName);
                }

                if (mappedValue == null) {
                    if (mapDefaultValue != null) {
                        value = mapDefaultValue;
                    }
                } else {
                    value = mappedValue;
                }
            }
        }

        return value;
    }

    /**
     * Adds this property's type and value to the specified image.
     *
     * @param  row    current row being processed.
     * @param  image  image to be updated.
     */
    public void setProperty(PluginDataRow row,
                            Image image) {

        final String value = deriveValue(row);
        image.addProperty(propertyType,
                          value);
    }

    protected static final String FIELD_STATIC_IDENTIFIER = "$STATIC:";
    protected static final String FIELD_MAP_IDENTIFIER = "$MAP:";
    protected static final String FIELD_VALUE_OF_IDENTIFIER = "$VALUE_OF:";
    protected static final String MAP_DEFAULT_KEY = "MAP_DEFAULT";
}