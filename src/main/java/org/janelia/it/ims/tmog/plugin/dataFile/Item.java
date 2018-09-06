/*
 * Copyright (c) 2015 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.dataFile;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Local data store item (that contains properties).
 *
 * @author Eric Trautman
 */
public class Item {

    @XmlAttribute
    private String name;

    @XmlElement(name = "property")
    private List<Property> properties;

    private Map<String, String> propertyNameToValueMap;

    @SuppressWarnings({"UnusedDeclaration"})
    public Item() {
        this(null);
    }

    public Item(String name) {
        this.name = name;
        this.properties = null;
    }

    public void mapItems() {
        if (properties == null) {
            propertyNameToValueMap = new HashMap<String, String>();
        } else {
            Map<String, String> map =
                    new HashMap<String, String>(properties.size());
            for (Property p : properties) {
                map.put(p.getName(), p.getValue());
            }
            propertyNameToValueMap = map;
        }
    }

    public String getName() {
        return name;
    }

    public boolean hasPropertyValue(String propertyName) {
        return (getPropertyValue(propertyName) != null);
    }

    public String getPropertyValue(String propertyName) {
        if (propertyNameToValueMap == null) {
            mapItems();
        }
        return propertyNameToValueMap.get(propertyName);
    }

    public void addProperty(Property property) {
        if (properties == null) {
            properties = new ArrayList<Property>();
        }
        properties.add(property);
        propertyNameToValueMap = null;
    }

    public int size() {
        int size = 0;
        if (properties != null) {
            size = properties.size();
        }
        return size;
    }

}
