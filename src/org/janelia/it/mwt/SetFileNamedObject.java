/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.mwt;

import java.util.ArrayList;
import java.util.List;

/**
 * The set of data values associated with a key (named) element in
 * a multi-worm tracker set XML file.
 *
 * @author Eric Trautman
 */
public class SetFileNamedObject {

    private String name;
    private List<String> values;
    private List<Integer> dimSizes;

    public SetFileNamedObject() {
        this.values = new ArrayList<String>();
        this.dimSizes = new ArrayList<Integer>();
    }

    public String getName() {
        return name;
    }

    public List<String> getValues() {
        return values;
    }

    public String getValue()
            throws IllegalStateException {
        String value = null;
        final int numberOfValues = values.size();
        if (numberOfValues == 1) {
            value = values.get(0);
        } else if (numberOfValues > 1) {
            throw new IllegalStateException(
                    "'" + name + "' attribute has multiple values: " + values);
        }
        return value;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void addValue(String value) {
        // first value is always the name
        if (name == null) {
            name = value;
        } else {
            values.add(value);
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void addDimSize(String dimSize) {
        dimSizes.add(Integer.parseInt(dimSize));
    }

    @Override
    public boolean equals(Object o) {
        boolean isEqual = false;
        if (this == o) {
            isEqual = true;
        } else if (o instanceof SetFileNamedObject) {
            SetFileNamedObject that = (SetFileNamedObject) o;
            if (name == null) {
                isEqual = (that.name == null);
            } else {
                isEqual = (this.name.equals(that.name));
            }
        }
        return isEqual;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return toJson();
    }

    /**
     * @return a JSON representation of this object.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("\"");
        sb.append(name);
        sb.append("\":");
        final int numberOfDimSizes = dimSizes.size();
        if (numberOfDimSizes > 0) {
            sb.append("{\"dim\":[");
            sb.append(dimSizes.get(0));
            for (int i = 1; i < numberOfDimSizes; i++) {
                sb.append(',');
                sb.append(dimSizes.get(i));
            }
            sb.append("], \"values\":");
        }
        final int numberOfValues = values.size();
        if (numberOfValues == 1) {
            sb.append('"');
            sb.append(values.get(0));
            sb.append('"');
        } else if (numberOfValues > 0) {
            sb.append("[\"");
            sb.append(values.get(0));
            for (int i = 1; i < numberOfValues; i++) {
                sb.append("\",\"");
                sb.append(values.get(i));
            }
            sb.append("\"]");
        } else {
            sb.append("\"\"");
        }
        if (numberOfDimSizes > 0) {
            sb.append('}');
        }

        return sb.toString();
    }
}
