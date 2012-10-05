/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.RenamePluginDataRow;

import java.io.File;

/**
 * This class sets the microscope_filename property for an image.
 *
 * @author Eric Trautman
 */
public class MicroscopeFilenameSetter
        implements ImagePropertySetter {

    /**
     * The type value for filename data stored in the image_property table.
     */
    public static final String TYPE = "microscope_filename";

    /**
     * Adds a microscope filename property type and value to the specified image.
     *
     * @param  row    current row being processed.
     * @param  image  image to be updated.
     */
    public void setProperty(PluginDataRow row,
                            Image image) {
        if (row instanceof RenamePluginDataRow) {
            final File fromFile = ((RenamePluginDataRow) row).getFromFile();
            if (fromFile != null) {
                image.addProperty(TYPE, fromFile.getName());
            }
        }
    }
}