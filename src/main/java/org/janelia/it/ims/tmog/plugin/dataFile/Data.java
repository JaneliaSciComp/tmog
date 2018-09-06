/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.dataFile;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Set of named data items, each item containing a set of named properties.
 *
 * @author Eric Trautman
 */
@XmlRootElement
public class Data {

    // <data>
    //    <item name="foo">
    //      <property name="p1">value1</property>
    //      <property name="p2">value2</property>
    //      <property name="p3">value3</property>
    //    </item>
    //    <item name="bar">
    //      <property name="p1">value11</property>
    //    </item>
    // </data>

    @XmlElement(name = "item")
    private List<Item> items;

    private Map<String, Item> keyToItemMap;

    public Data() {
        this.items = null;
    }

    public void mapItems() {
        if (items == null) {
            keyToItemMap = new HashMap<String, Item>();
        } else {
            Map<String, Item> map = new HashMap<String, Item>(items.size());
            for (Item item : items) {
                map.put(item.getName(), item);
            }
            keyToItemMap = map;
        }
    }

    public Item getItem(String itemName) {
        if (keyToItemMap == null) {
            mapItems();
        }
        return keyToItemMap.get(itemName);
    }

    public void addItem(Item item) {
        if (items == null) {
            items = new ArrayList<Item>();
        }
        items.add(item);
    }

    public String getValue(String itemName,
                           String propertyName) {
        String value = null;
        final Item item = getItem(itemName);
        if (item != null) {
            value = item.getPropertyValue(propertyName);
        }
        return value;
    }

    public int size() {
        int size = 0;
        if (items != null) {
            size = items.size();
        }
        return size; 
    }
}
