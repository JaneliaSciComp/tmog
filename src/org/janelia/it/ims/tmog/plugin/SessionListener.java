/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.plugin;

/**
 * This interface identifies the methods required for all session
 * event listeners.
 *
 * @author Eric Trautman
 */
public interface SessionListener extends Plugin {

    public enum EventType {
        END
    }

    /**
     * Notifies this listener that a session event has occurred.
     *
     * @param eventType type of session event.
     * @param message   details about the event.
     * @throws ExternalDataException   if a recoverable data error occurs during processing.
     * @throws ExternalSystemException if a non-recoverable system error occurs during processing.
     */
    public void processEvent(SessionListener.EventType eventType,
                             String message)
            throws ExternalDataException, ExternalSystemException;
}
