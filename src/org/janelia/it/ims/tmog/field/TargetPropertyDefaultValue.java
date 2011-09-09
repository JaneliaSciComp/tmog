/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.target.Target;

import java.util.HashMap;
import java.util.Map;

/**
 * This class encapsulates a default field value that is based upon
 * a target property.  If desired, specific target property values
 * can also be mapped to actual values.
 *
 * @author Eric Trautman
 */
public class TargetPropertyDefaultValue
        implements DefaultValue {

    private String propertyName;
    private Map<String, String> map;

    @SuppressWarnings({"UnusedDeclaration"})
    public TargetPropertyDefaultValue() {
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void addMappedValue(MappedValue mappedValue) {
        if (map == null) {
            this.map = new HashMap<String, String>();            
        }
        map.put(mappedValue.getFrom(), mappedValue.getTo());
    }

    public String getValue(Target target) {
        String value = target.getProperty(propertyName);
        if (map != null) {
            String mappedValue = map.get(value);
            if (mappedValue != null) {
                value = mappedValue;
            }
        }
        return value;
    }

}