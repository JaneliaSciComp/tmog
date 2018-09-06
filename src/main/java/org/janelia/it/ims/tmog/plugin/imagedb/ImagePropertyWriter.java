/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.janelia.it.ims.tmog.plugin.ExternalSystemException;

/**
 * This interface specifies the methods supported by all image property writers.
 *
 * @author Eric Trautman
 */
public interface ImagePropertyWriter {

    /**
     * Verifies that the writer is available.
     *
     * @throws ExternalSystemException
     *   if this writer cannot be used.
     */
    public void checkAvailability() throws ExternalSystemException;

    /**
     * Writes (saves) the specified image properties.
     *
     * @param  image  image to be persisted.
     *
     * @return the persisted image with any updates.
     *
     * @throws ExternalSystemException
     *   if the save fails.
     */
    public Image saveProperties(Image image) throws ExternalSystemException;
}