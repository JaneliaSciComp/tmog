/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */
package org.janelia.it.ims.imagerenamer;

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
}
