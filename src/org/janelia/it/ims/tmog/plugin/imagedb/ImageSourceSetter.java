/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.janelia.it.ims.tmog.plugin.PluginDataRow;

/**
 * This class sets the image source lab using a configured field name
 * to retrieve the property value from a data field row.
 *
 * @author Eric Trautman
 */
public class ImageSourceSetter
        extends SimpleSetter {

    /**
     * The type value for image sources stored in the image_property table.
     */
    public static final String TYPE = "image_source";

    /**
     * Value constructor.
     *
     * @param  fieldName  the display name of the data field that contains
     *                    image source information.
     */
    public ImageSourceSetter(String fieldName) {
        super(TYPE, fieldName);
    }

    /**
     * Sets the capture date for the specified image.
     *
     * @param  row    current row being processed.
     * @param  image  image to be updated.
     */
    public void setProperty(PluginDataRow row,
                            Image image) {
        final String value = deriveValue(row);
        image.setSource(value);
    }
}