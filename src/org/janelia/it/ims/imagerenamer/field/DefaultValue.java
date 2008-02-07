/*
 * Copyright © 2008 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.field;

import java.io.File;

/**
 * This interface specifies the methods required for all default values.
 *
 * @author Eric Trautman
 */
public interface DefaultValue {
    public String getValue(File sourceFile);
}