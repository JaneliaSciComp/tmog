/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.janelia.it.ims.tmog.plugin.PluginDataRow;

/**
 * This class sets the image's previous and current relative path values.
 *
 * @author Eric Trautman
 */
public class RelativePathSetter
        extends SimpleSetter {

    /** Identifies this setter. */
    public static final String TYPE = "relative_path";

    /**
     * Value constructor.
     *
     * @param  fieldName  the display name of the data field that contains
     *                    relative path.
     */
    public RelativePathSetter(String fieldName) {
        super(TYPE, fieldName);
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
        if ((value != null) && (value.length() > 0)) {
            image.setRelativePaths(value, value);
        }
    }

}