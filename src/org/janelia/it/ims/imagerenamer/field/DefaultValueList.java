/*
 * Copyright © 2008 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.field;

import java.io.File;
import java.util.ArrayList;

/**
 * A simple list of default value objects.
 *
 * @author Eric Trautman
 */
public class DefaultValueList extends ArrayList<DefaultValue> {

    /**
     * Constructs an empty list with an initial capacity of ten.
     */
    public DefaultValueList() {
        super();
    }

    public String getValue(File sourceFile) {
        String value = null;
        for (DefaultValue defaultValue : this) {
            value = defaultValue.getValue(sourceFile);
            if (value != null) {
                break;
            }
        }
        return value;
    }

}