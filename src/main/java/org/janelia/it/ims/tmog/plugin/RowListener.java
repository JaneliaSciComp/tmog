/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

/**
 * This interface identifies the methods required for all row event listeners.
 *
 * @author Eric Trautman
 */
public interface RowListener extends Plugin {

    public enum EventType {
        START_ROW, END_ROW_SUCCESS, END_ROW_FAIL 
    }

    /**
     * Notifies this listener that a copy event has occurred.
     *
     * @param  eventType  type of copy event.
     * @param  row        details about the event.
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
    public PluginDataRow processEvent(EventType eventType,
                                      PluginDataRow row)
            throws ExternalDataException, ExternalSystemException;
}
