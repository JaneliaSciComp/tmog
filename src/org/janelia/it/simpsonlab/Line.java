/*
 * Copyright © 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.simpsonlab;

/**
 * This class encapsulates information for Simpson Lab fly lines.
 */
public class Line {
    private String name;

    public Line(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getSpecimenNamespace() {
        return SPECIMEN_LSID + name;
    }

    private static final String SPECIMEN_LSID =
            "urn:lsid:janelia.org:simpson_lab_line_specimen:";
}
