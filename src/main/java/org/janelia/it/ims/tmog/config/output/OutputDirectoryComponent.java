/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.config.output;

import org.janelia.it.ims.tmog.field.DataField;

import java.io.File;
import java.util.List;

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
     * @param  dataFields  the validated rename fields supplied by the user.
     *
     * @return the path fragment derived from the specified source data.
     */
    public String getValue(File sourceFile,
                           List<DataField> dataFields);

    /**
     * @return a description of this output directory path fragment for display.
     */
    public String getDescription();
}