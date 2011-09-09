/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.target;

import java.util.HashMap;
import java.util.Map;

/**
 * The properties for a specific target.
 *
 * @author Eric Trautman
 */
public class TargetProperties {

    private String targetName;
    private Map<String, String> sharedProperties;
    private Map<String, String> specificProperties;

    public TargetProperties() {
        this.specificProperties = new HashMap<String, String>();
    }

    public String getTargetName() {
        return targetName;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getValue(String propertyName) {
        String value = specificProperties.get(propertyName);
        if ((value == null) && (sharedProperties != null)) {
            value = sharedProperties.get(propertyName);
        }
        return value;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void addSpecificProperty(String name,
                                    String value) {
        specificProperties.put(name, value);
    }

    public void setSharedProperties(Map<String, String> sharedProperties) {
        this.sharedProperties = sharedProperties;
    }

    @Override
    public String toString() {
        return "TargetProperties{" +
               "targetName='" + targetName + '\'' +
               ", specificProperties=" + specificProperties +
               ", sharedProperties=" + sharedProperties +
               '}';
    }
}
