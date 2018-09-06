/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.config;

/**
 * Simple callback handler interface for background configuration load processes.
 *
 * @author Eric Trautman
 */
public interface ConfigurationLoadCompletionHandler {

    /**
     * Called when the configuration load completes successfully.
     *
     * @param  config  the loaded configuration data.
     */
    public void handleConfigurationLoadSuccess(TransmogrifierConfiguration config);

    /**
     * Called when the configuration load fails.
     *
     * @param  failure  the specific failure.
     */
    public void handleConfigurationLoadFailure(Exception failure);
}
