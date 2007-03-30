/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.plugin;

/**
 * This interface identifies the methods required for all copy completion
 * event listeners.
 *
 * @author Eric Trautman
 */
public interface CopyCompleteListener extends Plugin {

    /**
     * Notifies this listener that a copy event has completed successfully.
     *
     * @param  info  details about the event.
     *
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     *
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    public void completedSuccessfulCopy(CopyCompleteInfo info)
            throws ExternalDataException, ExternalSystemException;
}
