/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.plugin;

import org.janelia.it.ims.imagerenamer.field.RenameField;

import java.io.File;

/**
 * This class encapsulates information about a copy operation that
 * has been successfully completed.
 *
 * @author Eric Trautman
 */
public class CopyCompleteInfo extends RenameFieldRow {

    /** The new file after copying and renaming is complete. */
    private File toFile;

    /**
     * Constructs a copy complete information object.
     *
     * @param fromFile      the original file being copied and renamed.
     * @param toFile        the new file after copying and renaming is complete.
     * @param renameFields  the list of rename field model objects referenced
     *                      during processing.
     */
    public CopyCompleteInfo(File fromFile,
                            File toFile,
                            RenameField[] renameFields) {
        super(fromFile, renameFields);
        this.toFile = toFile;
    }

    /**
     * @return the new file after copying and renaming is complete.
     */
    public File getToFile() {
        return toFile;
    }

    /**
     * @return a string representation of this object.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("CopyCompleteInfo");
        sb.append("{toFile=").append(toFile);
        sb.append(", ").append(super.toString());
        sb.append('}');
        return sb.toString();
    }
}
