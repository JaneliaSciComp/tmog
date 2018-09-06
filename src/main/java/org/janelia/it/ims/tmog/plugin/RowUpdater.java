/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

/**
 * Interface for plug-ins that need to update data rows before
 * validation and normal task processing occurs.
 *
 * @author Eric Trautman
 */
public interface RowUpdater
        extends Plugin {

    /**
     * Allows plug-in to update the specified row.
     *
     * @param  row  row to be updated.
     *
     * @return the data field row for processing (with any
     *         updates from this plugin).
     *
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     *
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    public PluginDataRow updateRow(PluginDataRow row)
            throws ExternalDataException, ExternalSystemException;
}
