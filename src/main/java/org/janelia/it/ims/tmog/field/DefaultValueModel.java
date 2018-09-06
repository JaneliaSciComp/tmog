/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

/**
 * This interface specifies the methods required for models that support
 * default values.
 *
 * @author Eric Trautman
 */
public interface DefaultValueModel {

    public void addDefaultValue(DefaultValue defaultValue);

    public DefaultValueList getDefaultValueList();

}