/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.janelia.it.ims.tmog.plugin.PluginDataRow;

/**
 * This class sets the image family using a configured field name
 * to retrieve the property value from a data field row.
 *
 * @author Eric Trautman
 */
public class FamilySetter extends SimpleSetter {

    /**
     * The type value for family data stored in the image_property table.
     */
    public static final String TYPE = "family";

    /**
     * Value constructor.
     *
     * @param  fieldName  the display name of the data field that contains
     *                    family information.
     */
    public FamilySetter(String fieldName) {
        super(TYPE, fieldName);
    }

    /**
     * Sets the family for the specified image.
     *
     * @param  row    current row being processed.
     * @param  image  image to be updated.
     */
    public void setProperty(PluginDataRow row,
                            Image image) {
        image.setFamily(row.getDataField(getFieldName()));
    }
}