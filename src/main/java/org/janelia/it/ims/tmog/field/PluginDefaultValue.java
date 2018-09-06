/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.config.ConfigurationException;

import java.util.Map;

/**
 * This interface specifies the methods required for all plugin default values.
 *
 * @author Eric Trautman
 */
public interface PluginDefaultValue extends DefaultValue {

    /**
     * Initializes the plug-in instance.
     *
     * @param  properties  configured properties for the instance.
     *
     * @throws ConfigurationException
     *   if the properties are invalid.
     */
    public void init(Map<String, String> properties)
            throws ConfigurationException;
}