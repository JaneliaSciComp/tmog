/*
 * Copyright © 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.field;

import java.io.File;
import java.util.Date;

/**
 * This model supports inserting formatted source file modification times
 * into a rename pattern.
 *
 * @author Eric Trautman
 */
public class FileModificationTimeModel
        extends DatePatternModel implements SourceFileField {

    private Date sourceDate;

    public FileModificationTimeModel() {
    }

    public FileModificationTimeModel getNewInstance() {
        FileModificationTimeModel instance = new FileModificationTimeModel();
        instance.setDatePattern(getDatePattern());
        // do not copy sourceDate (must be derived when rename occurs)
        return instance;
    }

    public String getFileNameValue() {
        return getFileNameValue(sourceDate);
    }

    public Date getSourceDate() {
        return sourceDate;
    }

    public void deriveSourceFileValues(File sourceFile) {
        if (sourceFile != null) {
            long modTime = sourceFile.lastModified();
            sourceDate = new Date(modTime);
        } else {
            sourceDate = null;
        }
    }
}
