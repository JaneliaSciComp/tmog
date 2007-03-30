/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.plugin;

import org.janelia.it.ims.imagerenamer.field.RenameField;

import java.io.File;
import java.util.HashMap;

/**
 * This class encapsulates the set of rename information collected for
 * a specific file (row).
 *
 * @author Eric Trautman
 */
public class RenameFieldRow {

    /** The original file being copied and renamed. */
    private File fromFile;

    /** Map of rename field display names to rename field model objects. */
    private HashMap<String, RenameField> displayNameToFieldMap;

    /**
     * Constructs a copy complete information object.
     *
     * @param fromFile      the original file being copied and renamed.
     * @param renameFields  the list of rename field model objects referenced
     *                      during processing.
     */
    public RenameFieldRow(File fromFile,
                          RenameField[] renameFields) {
        this.fromFile = fromFile;
        this.displayNameToFieldMap = new HashMap<String, RenameField>();
        for (RenameField field : renameFields) {
            String displayName = field.getDisplayName();
            if (displayName != null) {
                this.displayNameToFieldMap.put(displayName, field);
            }
        }
    }

    /**
     * @return the original file being copied and renamed.
     */
    public File getFromFile() {
        return fromFile;
    }

    /**
     * Returns the field model object associated with the specified
     * display name or null if no associated model exists.
     *
     * @param  fieldDisplayName  display name of desired field model.
     *
     * @return the associated field model or null.
     */
    public String getFileNameValue(String fieldDisplayName) {
        String value = null;
        RenameField field = displayNameToFieldMap.get(fieldDisplayName);
        if (field != null) {
            value = field.getFileNameValue();
        }
        return value;
    }

    /**
     * @return a string representation of this object.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RenameFieldRow");
        sb.append("{fromFile=").append(fromFile);
        sb.append(", displayNameToFieldMap=").append(displayNameToFieldMap);
        sb.append('}');
        return sb.toString();
    }
}
