/*
 * Copyright (c) 2018 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.janelia.it.ims.tmog.plugin.ExternalSystemException;

/**
 * This class supports management of image data within the SAGE database.
 *
 * @author Eric Trautman
 */
public class SageImageDao
        extends ImageDao {

    /**
     * Constructs a dao using the default manager and configuration.
     *
     * @param  dbConfigurationKey  the key for loading database
     *                             configuration information.
     *
     * @throws ExternalSystemException
     *   if the database configuration information cannot be loaded.
     */
    public SageImageDao(String dbConfigurationKey) throws ExternalSystemException {
        super(dbConfigurationKey);
    }

}