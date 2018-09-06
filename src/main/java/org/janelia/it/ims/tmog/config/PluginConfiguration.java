/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.config;

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
    @SuppressWarnings("UnusedDeclaration")
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * @return the full set of properties configured for this plug-in.
     */
    public Map<String, String> getProperties() {
        return properties;
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

    /**
     * Removes the specified property from this object.
     *
     * @param name  name of the property.
     */
    public void removeProperty(String name) {
        this.properties.remove(name);
    }

}