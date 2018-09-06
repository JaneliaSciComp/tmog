/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.config;

import org.janelia.it.ims.tmog.field.DefaultValue;
import org.janelia.it.ims.tmog.target.Target;

/**
 * This class ...
 *
 * @author Eric Trautman
 */
public class PluginDefaultValueConfiguration
        extends PluginConfiguration
        implements DefaultValue {

    public PluginDefaultValueConfiguration() {
    }

    public String getValue(Target target) {
        throw new UnsupportedOperationException(
                "An instance of the default value plug-in class (" +
                getClassName() +
                ") was not constructed from configuration information.  " +
                "You may want to verify the DefaultValueList implementation.");
    }

}