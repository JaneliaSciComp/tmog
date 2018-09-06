/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.target;

import org.janelia.it.ims.tmog.config.ConfigurationException;

import java.io.InputStream;

/**
 * Interface for any data file that contains a list of targets for processing.
 *
 * @author Eric Trautman
 */
public interface TargetDataFile {

    /**
     * Validates the configured data file parameters.
     *
     * @throws ConfigurationException
     *   if any of the settings are invalid.
     */
    public void verify()
            throws ConfigurationException;

    /**
     * @param  stream  data stream from which to parse target information.
     *
     * @return list of targets parsed from the specified stream.
     *
     * @throws IllegalArgumentException
     *   if any errors occur while processing the stream.
     */
    public TargetList getTargets(InputStream stream) 
            throws IllegalArgumentException;

}


