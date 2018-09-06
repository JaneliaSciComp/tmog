/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.target.FileTarget;
import org.janelia.it.ims.tmog.target.Target;
import org.janelia.it.utils.LsmCoreMetaDataCache;

import java.io.File;

/**
 * This class encapsulates a default field value that is taken from
 * the Zeiss core meta data of an LSM file.
 *
 * @author Eric Trautman
 */
public class LsmDefaultValue
        implements DefaultValue {

    private static LsmCoreMetaDataCache cache = new LsmCoreMetaDataCache();

    private String propertyName;
    private boolean truncateDecimal;

    public LsmDefaultValue() {
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setTruncateDecimal(boolean truncateDecimal) {
        this.truncateDecimal = truncateDecimal;
    }

    public String getValue(Target target) {
        String valueString = null;
        File sourceFile = null;
        if (target instanceof FileTarget) {
            sourceFile = ((FileTarget) target).getFile();
        }
        if ((sourceFile != null) && (sourceFile.getName().endsWith(".lsm"))) {
            Object value = cache.getValue(sourceFile, propertyName);
            if (value instanceof Double) {
                if (truncateDecimal) {
                    final long truncatedValue = ((Double) value).longValue();
                    valueString = String.valueOf(truncatedValue);
                } else {
                    valueString = String.valueOf(value);
                }
            } else if (value != null) {
                valueString = String.valueOf(value);
            }
        }
        return valueString;
    }

    @Override
    public String toString() {
        return "LsmDefaultValue{" +
               "propertyName='" + propertyName + '\'' +
               ", truncateDecimal=" + truncateDecimal +
               '}';
    }

}