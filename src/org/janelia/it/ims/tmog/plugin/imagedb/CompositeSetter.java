/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.PropertyToken;

import java.util.List;

/**
 * This class sets the image property using a composite of text and
 * field names.
 *
 * @author Eric Trautman
 */
public class CompositeSetter implements ImagePropertySetter {

    private String propertyType;
    private List<PropertyToken> tokens;

    public CompositeSetter(String propertyType,
                           String compositeFieldString)
            throws IllegalArgumentException {

        this.propertyType = propertyType;
        this.tokens = PropertyToken.parseTokens(compositeFieldString);
    }

    public String getValue(PluginDataRow row) {
        StringBuilder sb = new StringBuilder();
        for (PropertyToken token : tokens) {
            sb.append(token.getValue(row));
        }
        return sb.toString();
    }

    public void setProperty(PluginDataRow row,
                            Image image) {
        String value = getValue(row);
        if (value.length() > 0) {
            image.addProperty(propertyType, value);
        }
    }
}