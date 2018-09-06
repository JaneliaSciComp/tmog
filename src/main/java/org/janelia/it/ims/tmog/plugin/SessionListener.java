/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import org.janelia.it.ims.tmog.DataRow;

import java.util.List;

/**
 * This interface identifies the methods required for all session
 * event listeners.
 *
 * @author Eric Trautman
 */
public interface SessionListener extends Plugin {

    /**
     * Notifies this listener that session processing has started.
     *
     * @param  modelRows  list of data rows to be processed.
     *
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     *
     * @return the specified model rows with potentially modified content.
     */
    public List<DataRow> startSession(List<DataRow> modelRows)
            throws ExternalDataException, ExternalSystemException;

    /**
     * Notifies this listener that session processing has ended.
     *
     * @param  message  a message summarizing what was processed.
     *
     * @throws ExternalDataException
     *   if a recoverable data error occurs during processing.
     * @throws ExternalSystemException
     *   if a non-recoverable system error occurs during processing.
     */
    public void endSession(String message)
            throws ExternalDataException, ExternalSystemException;
}
