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
 * This model supports inserting formatted run times (the time when
 * the copy/rename is performed) into a rename pattern.
 *
 * @author Eric Trautman
 */
public class RunTimeModel extends DatePatternModel {

    private Date runTime;

    public RunTimeModel() {
    }

    public RunTimeModel getNewInstance() {
        RunTimeModel instance = new RunTimeModel();
        instance.setDisplayName(getDisplayName());
        instance.setDatePattern(getDatePattern());
        // do not copy runTime
        return instance;
    }

    public String getFileNameValue() {
        if (runTime == null) {
            runTime = new Date();
        }
        return getFileNameValue(runTime);
    }

    /**
     * Initializes this field's value if necessary.
     *
     * @param sourceFile the source file being renamed.
     */
    public void initializeValue(File sourceFile) {
        // nothing to initialize
    }

}
