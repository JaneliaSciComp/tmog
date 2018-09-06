/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import org.janelia.it.ims.tmog.config.PluginConfiguration;

import java.io.File;

/**
 * This class validates a text value composed of multiple fields.
 *
 * @author Eric Trautman
 */
public class WritableFromFileValidator
        extends SimpleRowValidator {

    /** Format for error messages. */
    private String errorMessageFormat;

    /**
     * Empty constructor required by
     * {@link org.janelia.it.ims.tmog.config.PluginFactory}.
     */
    public WritableFromFileValidator() {
        this.errorMessageFormat =
                "You do not have permission to write to %s.";
    }

    /**
     * Verifies that the plugin is ready for use.
     *
     * @param  config  the plugin configuration.
     *
     * @throws ExternalSystemException
     *   if the plugin can not be initialized.
     */
    public void init(PluginConfiguration config)
            throws ExternalSystemException {
        final String configuredMessage = config.getProperty("errorMessage");
        if (configuredMessage != null) {
            this.errorMessageFormat = configuredMessage;
        }
    }

    /**
     * Validates derived value(s) for the current row.
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
            throws ExternalDataException, ExternalSystemException {

        if (row instanceof RenamePluginDataRow) {
            final File fromFile = ((RenamePluginDataRow) row).getFromFile();
            if (! fromFile.canWrite()) {
                throw new ExternalSystemException(
                        String.format(errorMessageFormat,
                                      fromFile.getAbsolutePath()));
            }
        }

    }

}