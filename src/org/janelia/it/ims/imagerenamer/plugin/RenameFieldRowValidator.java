/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.plugin;

/**
 * This interface identifies the methods required to support external
 * validation of rename field row data.
 *
 * @author Eric Trautman
 */
public interface RenameFieldRowValidator extends Plugin {

    /**
     * Validates the set of rename information collected for
     * a specific file (row).
     *
     * @param  row  the user supplied rename information to be validated.
     *
     * @throws ExternalDataException
     *   if the data is not valid.
     *
     * @throws ExternalSystemException
     *   if any error occurs while validating the data.
     */
    public void validate(RenameFieldRow row)
            throws ExternalDataException, ExternalSystemException;
}
