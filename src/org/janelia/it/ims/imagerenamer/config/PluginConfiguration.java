/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.config;

/**
 * This class encapsulates information about configured plugin components.
 *
 * @author Eric Trautman
 */
public class PluginConfiguration {

    /** The name of this plugin's class. */
    private String className;

    /**
     * Empty constructor.
     */
    public PluginConfiguration() {
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
     * @param  className  the plugin class name.
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param   o   the reference object with which to compare.
     *
     * @return  <code>true</code> if this object is the same as the obj
     *          argument; <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object o) {
        boolean isEqual = false;
        if (this == o) {
            isEqual = true;
        } else if (o instanceof PluginConfiguration) {
            PluginConfiguration that = (PluginConfiguration) o;
            if (this.className == null) {
                isEqual = (that.className == null);
            } else {
                isEqual = this.className.equals(that.className);
            }
        }
        return isEqual;
    }

    /**
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        int hashCode = 0;
        if (className != null) {
            hashCode = className.hashCode();
        }
        return hashCode;
    }
}