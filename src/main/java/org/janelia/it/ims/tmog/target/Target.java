/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.target;

/**
 * This interface specifies the methods required for all data targets.
 */
public interface Target {

    /**
     * @return the target instance.
     */
    public Object getInstance();

    /**
     * @return the target name.
     */
    public String getName();

    /**
     * @param  propertyName  name of desired property.
     *
     * @return the property value for this target
     *         or null if the property is not defined.
     */
    public String getProperty(String propertyName);
}
