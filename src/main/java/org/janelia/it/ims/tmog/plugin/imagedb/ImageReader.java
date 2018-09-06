/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.janelia.it.ims.tmog.plugin.ExternalSystemException;

import java.util.Map;

/**
 * This interface specifies the methods supported by all image readers.
 *
 * @author Eric Trautman
 */
public interface ImageReader {

    /**
     * Verifies that the reader is available.
     *
     * @throws ExternalSystemException
     *   if this reader cannot be used.
     */
    public void checkAvailability() throws ExternalSystemException;

    /**
     * @param  family          image family.
     * @param  relativePath    image relative path.
     *
     * @return a map of the specified image's properties
     *         or null if the image does not exist.
     *
     * @throws ExternalSystemException
     *   if retrieval fails.
     */
    public Map<String, String> getImageData(String family,
                                            String relativePath) throws ExternalSystemException;
}