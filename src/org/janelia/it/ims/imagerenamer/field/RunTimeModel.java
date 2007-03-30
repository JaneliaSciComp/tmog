/*
 * Copyright © 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.field;

/**
 * This model supports inserting formatted run times (the time when
 * the copy/rename is performed) into a rename pattern.
 *
 * @author Eric Trautman
 */
public class RunTimeModel extends DatePatternModel {

    public RunTimeModel() {
    }

    public RunTimeModel getNewInstance() {
        RunTimeModel instance = new RunTimeModel();
        instance.setDatePattern(getDatePattern());
        return instance;
    }

}
