/*
 * Copyright © 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.simpsonlab;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class encapsulates information about an image property.
 */
public class ImageProperty {

    public static final String LINE_NAME = "Line";
    public static final String INSERTION_NUMBER_NAME = "Insertion Number";
    public static final String ORGAN_NAME = "Organ";
    public static final String SPECIMEN_NUMBER_NAME = "Specimen Number";
    public static final String CREATED_BY_NAME = "Created By";

    public static final List<String> NAMES = 
            Arrays.asList(INSERTION_NUMBER_NAME,
                          ORGAN_NAME, SPECIMEN_NUMBER_NAME);

    private String name;
    private String value;

    public ImageProperty(String name,
                         String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getColumnName() {
        return DISPLAY_TO_COLUMN_NAME_MAP.get(name);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ImageProperty");
        sb.append("{name='").append(name).append('\'');
        sb.append(", value='").append(value).append('\'');
        sb.append('}');
        return sb.toString();
    }

    private static final Map<String, String> DISPLAY_TO_COLUMN_NAME_MAP;
    static {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(LINE_NAME, "line");
        map.put(INSERTION_NUMBER_NAME, "insertion");
        map.put(ORGAN_NAME, "organ");
        map.put(SPECIMEN_NUMBER_NAME, "specimen");
        map.put(CREATED_BY_NAME, "created_by");
        DISPLAY_TO_COLUMN_NAME_MAP = map;
    }
}