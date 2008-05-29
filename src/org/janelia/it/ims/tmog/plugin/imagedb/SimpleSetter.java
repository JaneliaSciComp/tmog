/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.janelia.it.ims.tmog.plugin.PluginDataRow;

/**
 * This class sets an image property using a configured field name
 * to retrieve the property value from a data field row.
 *
 * @author Eric Trautman
 */
public class SimpleSetter implements ImagePropertySetter {

    private String propertyType;
    private String fieldName;

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
    }

    /**
     * @return the display name of the row data field that contains
     *         this property's value.
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Adds this property's type and value to the specified image.
     *
     * @param  row    current row being processed.
     * @param  image  image to be updated.
     */
    public void setProperty(PluginDataRow row,
                            Image image) {
        image.addProperty(propertyType,
                          row.getCoreValue(fieldName));
    }
}