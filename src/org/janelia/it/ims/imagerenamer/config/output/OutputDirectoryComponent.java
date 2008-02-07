/*
 * Copyright © 2008 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.config.output;

import org.janelia.it.ims.imagerenamer.field.RenameField;

import java.io.File;

/**
 * This interface describes the methods supported by all configured output
 * directory components.
 *
 * @author Eric Trautman
 */
public interface OutputDirectoryComponent {

    /**
     * Uses the specified source data to derive an output directory
     * path fragment.
     *
     * @param  sourceFile    the source file being renamed.
     * @param  renameFields  the validated rename fields supplied by the user.
     *
     * @return the path fragment derived from the specified source data.
     */
    public String getValue(File sourceFile,
                           RenameField[] renameFields);

    /**
     * @return a description of this output directory path fragment for display.
     */
    public String getDescription();
}