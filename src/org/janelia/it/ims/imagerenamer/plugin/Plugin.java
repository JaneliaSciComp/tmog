/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.plugin;

import org.janelia.it.ims.imagerenamer.config.PluginConfiguration;

/**
 * This interface identifies the methods required for all renamer
 * plugin components.
 *
 * @author Eric Trautman
 */
public interface Plugin {

    /**
     * Initializes the plugin and verifies that it is ready for use.
     *
     * @param config the plugin configuration.
     * @throws ExternalSystemException if the plugin can not be initialized.
     */
    public void init(PluginConfiguration config) throws ExternalSystemException;
}
