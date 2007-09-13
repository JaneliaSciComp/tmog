/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.config;

import java.util.HashMap;
import java.util.Map;

/**
 * This class encapsulates information about configured plugin components.
 *
 * @author Eric Trautman
 */
public class PluginConfiguration {

    /**
     * The name of this plugin's class.
     */
    private String className;

    /**
     * The set of properties for this plugin.
     */
    private Map<String, String> properties;

    /**
     * Empty constructor.
     */
    public PluginConfiguration() {
        this.properties = new HashMap<String, String>();
    }

    /**
     * @return this plugin's class name.
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets this plugin's class name.
     *
     * @param className the plugin class name.
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * @param name the name of the property to lookup.
     * @return the value for the specified property name.
     */
    public String getProperty(String name) {
        return properties.get(name);
    }

    /**
     * Adds the specified property name/value pair to this object's
     * set of properties.
     *
     * @param name  name of the property.
     * @param value value of the property.
     */
    public void setProperty(String name,
                            String value) {
        this.properties.put(name, value);
    }
}