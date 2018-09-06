/*
 * Copyright (c) 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.config.output;

import org.janelia.it.ims.tmog.field.DataField;

import java.io.File;
import java.util.List;

/**
 * This class simply returns the path of the source file being processed.
 *
 * @author Eric Trautman
 */
public class SourcePath implements OutputDirectoryComponent {

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
                           List<DataField> dataFields) {
        return sourceFile.getParent();
    }

    /**
     * @return a description of this output directory path fragment for display.
     */
    public String getDescription() {
        return "[source file directory]";
    }

    /**
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return getDescription();
    }
}