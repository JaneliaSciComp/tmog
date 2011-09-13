/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;

/**
 * This class encapsulates a simple name and value pair.
 *
 * @author Eric Trautman
 */
public class ValidValue {
    public static final ValidValue NONE = new ValidValue("");

    private String displayName;
    private String value;
    private boolean isDefault;

    @SuppressWarnings({"UnusedDeclaration"})
    public ValidValue() {
        this(null, null);
    }

    public ValidValue(String displayName) {
        this(displayName, displayName);
    }

    public ValidValue(String displayName,
                      String value) {
        this.displayName = displayName;
        this.value = value;
        this.isDefault = false;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void prefixDisplayName() {
        if (! value.equals(displayName)) {
            displayName = value + ": " + displayName;
        }
    }

    public String getValue() {
        return value;
    }

    public boolean isDefined() {
        return value != null;
    }
    
    public void setValue(String value) {
        this.value = value;
        if (this.displayName == null) {
            this.displayName = value;
        }
    }

    public boolean isDefault() {
        return isDefault;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
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
