/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.config.output;

import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.utils.PathUtil;

import java.io.File;
import java.util.List;

/**
 * This class encapsulates a (textual) path fragment to be included
 * in the output directory path.
 *
 * @author Eric Trautman
 */
public class Path implements OutputDirectoryComponent {

    private String path;

    /**
     * Empty constructor.
     */
    public Path() {
    }

    /**
     * Constructs a path fragment with the specified value.
     *
     * @param  path  path fragment value.
     */
    public Path(String path) {
        setPath(path);
    }

    /**
     * @return the path fragment value.
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the path fragment value, converting it if necessary for the
     * current operating system.
     *
     * @param  path  path fragment value.
     */
    public void setPath(String path) {
        this.path = PathUtil.convertPath(path);
    }

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
        return path;
    }

    /**
     * @return a description of this output directory path fragment for display.
     */
    public String getDescription() {
        return path;
    }

    /**
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return getDescription();
    }
}