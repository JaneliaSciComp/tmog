/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import org.janelia.it.ims.tmog.DataRow;

import java.util.List;

/**
 * This interface identifies the methods required to support external
 * validation of row data.
 *
 * @author Eric Trautman
 */
public abstract class SimpleRowValidator
        implements RowValidator {

    /**
     * Simple row validators do not use session data,
     * so this method can be ignored.
     *
     * @param  sessionName  unique name for session being validated.
     * @param  allRows      unmodifiable list of all rows for the session
     *                      about to be validated.
     *
     * @throws ExternalSystemException
     *   if any error occurs while setting up for validation.
     */
    public void startSessionValidation(String sessionName,
                                       List<DataRow> allRows)
            throws ExternalSystemException {
    }

    /**
     * Simple row validators do not use session data,
     * so this method can be ignored.
     *
     * @param  sessionName  unique name for session being validated.
     */
    public void stopSessionValidation(String sessionName) {
    }

    public abstract void validate(String sessionName,
                                  PluginDataRow row)
            throws ExternalDataException, ExternalSystemException;
}
