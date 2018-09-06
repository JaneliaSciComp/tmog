/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.target.Target;

/**
 * This class encapsulates a static default field value.
 *
 * @author Eric Trautman
 */
public class StaticDefaultValue implements DefaultValue {

    private String value;

    public StaticDefaultValue() {
    }

    public String getValue(Target target) {
        return value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "StaticDefaultValue{" +
               "value='" + value + '\'' +
               '}';
    }
}