/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.target.Target;

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

    public RunTimeModel getNewInstance(boolean isCloneRequired) {
        RunTimeModel instance = new RunTimeModel();
        initNewInstance(instance);
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
     * Initializes this field's value based upon the specified target.
     *
     * @param  target  the target being processed.
     */
    public void initializeValue(Target target) {
        // nothing to initialize
    }

}
