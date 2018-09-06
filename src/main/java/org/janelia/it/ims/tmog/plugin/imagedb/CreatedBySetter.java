/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;

/**
 * This class sets the created_by property for an image.
 *
 * @author Eric Trautman
 */
public class CreatedBySetter extends SimpleSetter {

    /**
     * The type value for created by data stored in the image_property table.
     */
    public static final String TYPE = "created_by";

    /**
     * Value constructor.
     *
     * @param  fieldName  the display name of the data field that contains
     *                    created by information.
     */
    public CreatedBySetter(String fieldName) {
        super(TYPE, fieldName);
    }

    /**
     * Adds a created by property type and value to the specified image.
     *
     * @param  row    current row being processed.
     * @param  image  image to be updated.
     */
    public void setProperty(PluginDataRow row,
                            Image image) {
        String value;
        DataField field = row.getDataField(getFieldName());
        // if explicitly defined use that value, otherwise use user name
        if (field != null) {
            value = deriveValue(row);
        } else {
            value = System.getProperty("user.name");
        }
        image.addProperty(TYPE, value);
    }
}