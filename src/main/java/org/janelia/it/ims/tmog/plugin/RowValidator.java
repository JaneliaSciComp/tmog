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
public interface RowValidator extends Plugin {

    /**
     * This method is called before any individual row is validated.
     * It allows the plugin to process/cache session data prior
     * to validating individual rows.
     *
     * NOTE that validator instances are shared across all sessions so
     * care must be taken to properly segregate and clean up data for
     * individual sessions (see {@link #stopSessionValidation}).
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
            throws ExternalSystemException;

    /**
     * Validates the set of information collected for
     * a specific row.
     *
     * @param  sessionName  unique name for session being validated.
     * @param  row          the user supplied information to be validated.
     *
     * @throws ExternalDataException
     *   if the data is not valid.
     *
     * @throws ExternalSystemException
     *   if any error occurs while validating the data.
     */
    public void validate(String sessionName,
                         PluginDataRow row)
            throws ExternalDataException, ExternalSystemException;

    /**
     * This method is called after individual row validation has
     * completed (whether or not it completes successfully).
     * It allows the plugin to clean-up any cached data for the
     * individual session being validated.
     *
     * @param  sessionName  unique name for session being validated.
     */
    public void stopSessionValidation(String sessionName);

}
