/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.plugin;

/**
 * This class hosts common plug-in utility methods.
 *
 * @author Eric Trautman
 */
public class PluginUtil {

    /**
     * Utility (hack) to cast the specified plug-in row to a rename plug-in
     * row and throw a standard exception if the cast cannot be performed.
     *
     * @param  row        object to cast.
     * @param  forPlugin  the plug-in component that requires the rename
     *                    plug-in row data.
     *
     * @return the specified row cast as a rename plug-in row.
     *
     * @throws ExternalSystemException
     *   if the row cannot be cast.
     */
    public static RenamePluginDataRow castRenameRow(PluginDataRow row ,
                                                    Plugin forPlugin)
            throws ExternalSystemException {

        RenamePluginDataRow dataRow;
        if (row instanceof RenamePluginDataRow) {
            dataRow = (RenamePluginDataRow) row;
        } else {
            String rowClassName = null;
            if (row != null) {
                rowClassName = row.getClass().getName();
            }
            String pluginClassName = null;
            if (forPlugin != null) {
                pluginClassName = forPlugin.getClass().getName();
            }
            throw new ExternalSystemException(
                    "Invalid data row object with type '" +
                    rowClassName + "' passed to plug-in '" + pluginClassName +
                    "'. This plug-in expects data rows to have type '" +
                    RenamePluginDataRow.class.getName() +
                    "'.  Please review your task and plug-in configuration.");
        }

        return dataRow;
    }

}
