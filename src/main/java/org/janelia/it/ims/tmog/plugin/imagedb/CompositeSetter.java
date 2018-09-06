/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.PropertyTokenList;

import java.util.List;
import java.util.Map;

/**
 * This class sets the image property using a composite of text and
 * field names.
 *
 * @author Eric Trautman
 */
public class CompositeSetter implements ImagePropertySetter {

    private String propertyType;
    private PropertyTokenList tokens;

    public CompositeSetter(String propertyType,
                           String compositeFieldString,
                           Map<String, String> properties)
            throws IllegalArgumentException {

        this.propertyType = propertyType;
        this.tokens = new PropertyTokenList(compositeFieldString,
                                            properties);
    }

    /**
     * @param  row  the current data row.
     *
     * @return the composite value for the specified row.
     *         If the composite pattern includes a
     *         non-concatenated field group, the value
     *         associated with the first field group
     *         is returned.
     */
    public String getValue(PluginDataRow row) {
        String value = null;
        final List<String> values =
                tokens.deriveValues(row.getDisplayNameToFieldMap(), false);
        if (values.size() > 0) {
            value = values.get(0);
        }
        return value;
    }

    public void setProperty(PluginDataRow row,
                            Image image) {
        
        final List<String> values =
                tokens.deriveValues(row.getDisplayNameToFieldMap(), false);

        String pType = propertyType;
        int i = 0;
        for (String value : values) {
            if (value.length() > 0) {
                image.addProperty(pType, value);
                i++;
                pType = propertyType + "-" + i;
            }
        }
    }
}