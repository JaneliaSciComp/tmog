/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

/**
 * <p>
 * This model supports inserting verified (validated) integer text
 * into a rename pattern.</p>
 * <p>
 * NOTE: The getter and setter methods may seem redundant but their
 * type specific declarations are required to support XML parsing of the
 * configuration file by Digester.</p>
 *
 * @author Eric Trautman
 */
public class VerifiedIntegerModel
        extends VerifiedRangeModel<Integer> {

    public VerifiedIntegerModel() {
        super();
    }

    public Integer getMinimumValue() {
        return getMinimum();
    }

    public Integer getMaximumValue() {
        return getMaximum();
    }

    public void setMinimumValue(Integer minimumValue) {
        setMinimum(minimumValue);
    }

    public void setMaximumValue(Integer maximumValue) {
        setMaximum(maximumValue);
    }

    public Integer getValueOf(String valueStr) throws NumberFormatException {
        Integer value = null;
        if ((valueStr != null) && (valueStr.length() > 0)) {
            value = new Integer(valueStr);
        }
        return value;
    }

    public String getValueName() {
        return "an integer value";
    }

    public VerifiedIntegerModel getNewInstance(boolean isCloneRequired) {
        VerifiedIntegerModel instance = this;
        if (isCloneRequired || (! isSharedForAllSessionFiles())) {
            instance = new VerifiedIntegerModel();
            cloneValuesForNewInstance(instance);
        }
        return instance;
    }

}