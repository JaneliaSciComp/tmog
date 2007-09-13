/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.plugin;

/**
 * This interface identifies the methods required for all copy event listeners.
 *
 * @author Eric Trautman
 */
public interface CopyListener extends Plugin {

    public enum EventType { START, END_SUCCESS, END_FAIL }

    /**
     * Notifies this listener that a copy event has occurred.
     *
     * @param  eventType  type of copy event.
     * @param  row        details about the event.
     *
     * @return the rename field row for processing (with any
     *         updates from this plugin).
     *
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     *
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    public RenameFieldRow processEvent(EventType eventType,
                                       RenameFieldRow row)
            throws ExternalDataException, ExternalSystemException;
}
