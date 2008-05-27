/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.simpsonlab;

import org.janelia.it.ims.tmog.plugin.RenamePluginDataRow;

/**
 * This class encapsulates information for Simpson Lab fly lines.
 */
public class Line {
    private String name;
    private String insertionNumber;

    public Line(RenamePluginDataRow row) {
        this(row.getCoreValue(ImageProperty.LINE_NAME),
             row.getCoreValue(ImageProperty.INSERTION_NUMBER_NAME));
    }

    public Line(String name,
                String insertionNumber) {
        this.name = name;
        this.insertionNumber = insertionNumber;
    }

    public String getFullName() {
        String fullName = name;
        if ((insertionNumber != null) && (insertionNumber.length() > 0)) {
            fullName = name + "_" + insertionNumber;
        }
        return fullName;
    }

    /**
     * @return a string representation of this object.
     */
    public String toString() {
        return getFullName();
    }

    public String getSpecimenNamespace() {
        return SPECIMEN_LSID + getFullName();
    }

    private static final String SPECIMEN_LSID =
            "urn:lsid:janelia.org:simpson_lab_line_specimen:";
}
