/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.janelia.it.ims.tmog.plugin.PluginDataRow;

/**
 * This class sets the created_by property for an image.
 *
 * @author Eric Trautman
 */
public class CreatedBySetter implements ImagePropertySetter {

    /**
     * The type value for created by data stored in the image_property table.
     */
    public static final String TYPE = "created_by";

    /**
     * Adds a created by property type and value to the specified image.
     *
     * @param  row    current row being processed.
     * @param  image  image to be updated.
     */
    public void setProperty(PluginDataRow row,
                            Image image) {
        image.addProperty(TYPE,
                          System.getProperty("user.name"));
    }
}