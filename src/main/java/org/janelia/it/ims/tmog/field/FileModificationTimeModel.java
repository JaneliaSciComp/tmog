/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.target.Target;

import java.io.File;
import java.util.Date;

/**
 * This model supports inserting formatted source file modification times
 * into a rename pattern.
 *
 * @author Eric Trautman
 */
public class FileModificationTimeModel extends DatePatternModel {

    private Date sourceDate;

    public FileModificationTimeModel() {
    }

    public FileModificationTimeModel getNewInstance(boolean isCloneRequired) {
        FileModificationTimeModel instance = new FileModificationTimeModel();
        initNewInstance(instance);
        // do not copy sourceDate (must be derived when rename occurs)
        return instance;
    }

    public String getFileNameValue() {
        return getFileNameValue(sourceDate);
    }

    /**
     * Initializes this field's value based upon the specified target.
     *
     * @param  target  the target being processed.
     */
    public void initializeValue(Target target) {
        File sourceFile = null;
        if (target != null) {
            Object sourceValue = target.getInstance();
            if (sourceValue instanceof File) {
                sourceFile = (File) sourceValue;
            }
        }

        if (sourceFile != null) {
            long modTime = sourceFile.lastModified();
            sourceDate = new Date(modTime);
        } else {
            sourceDate = null;
        }
    }

    public Date getSourceDate() {
        return sourceDate;
    }
}
