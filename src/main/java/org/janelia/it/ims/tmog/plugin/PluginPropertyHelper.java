/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import org.janelia.it.ims.tmog.config.PluginConfiguration;

/**
 * Utilities for handling plug-in configuration properties.
 *
 * @author Eric Trautman
 */
public class PluginPropertyHelper {

    private PluginConfiguration config;
    private String initFailureMessage;

    public PluginPropertyHelper(PluginConfiguration config,
                                String initFailureMessage) {
        this.config = config;
        this.initFailureMessage = initFailureMessage;
    }

    public String getRequiredProperty(String propertyName)
            throws ExternalSystemException {
        String value = config.getProperty(propertyName);
        if ((value == null) || (value.length() < 1)) {
            throw new ExternalSystemException(
                    initFailureMessage +
                    "Please specify a value for the '" + propertyName +
                    "' plug-in property.");
        }
        return value;
    }

}