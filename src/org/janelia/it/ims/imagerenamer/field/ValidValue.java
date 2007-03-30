/*
 * Copyright � 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.field;

/**
 * This class encapsulates a simple name and value pair.
 *
 * @author Eric Trautman
 */
public class ValidValue {
    private String displayName;
    private String value;

    public ValidValue() {
    }

    public ValidValue(String displayName) {
        this.displayName = displayName;
        this.value = displayName;
    }

    public ValidValue(String displayName,
                      String value) {
        this.displayName = displayName;
        this.value = value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        boolean isEqual = false;
        if (this == o) {
            isEqual = true;
        } else if (o instanceof ValidValue) {
            ValidValue that = (ValidValue) o;
            if (value == null) {
                isEqual = that.value == null;
            } else {
                isEqual = value.equals(that.value);
            }
        }
        return isEqual;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        if (value != null) {
            hashCode = value.hashCode();
        }
        return hashCode;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
