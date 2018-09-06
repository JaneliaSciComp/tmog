/*
 * Copyright (c) 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.config.preferences;

/**
 * Base class for any object that has a name based identity.
 *
 * @author Eric Trautman
 */
public class NamedObject {

    private String name;

    public NamedObject() {
    }

    public NamedObject(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        boolean isEqual = false;
        if (this == o) {
            isEqual = true;
        } else if (o instanceof NamedObject) {
            NamedObject that = (NamedObject) o;
            if (name == null) {
                isEqual = (that.name == null);
            } else {
                isEqual = name.equals(that.name);
            }
        }
        return isEqual;
    }

    @Override
    public int hashCode() {
        int result = 0;
        if (name != null) {
            result = 31 * name.hashCode();
        }
        return result;
    }

}