/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

import java.math.BigDecimal;

/**
 * <p>
 * This model supports inserting verified (validated) decimal text
 * into a rename pattern.</p>
 * <p>
 * NOTE: The getter and setter methods may seem redundant but their
 * type specific declarations are required to support XML parsing of the
 * configuration file by Digester.</p>
 *
 * @author Eric Trautman
 */
public class VerifiedDecimalModel extends VerifiedRangeModel<BigDecimal> {

    public VerifiedDecimalModel() {
        super();
    }

    public BigDecimal getMinimumValue() {
        return getMinimum();
    }

    public BigDecimal getMaximumValue() {
        return getMaximum();
    }

    public void setMinimumValue(BigDecimal minimumValue) {
        setMinimum(minimumValue);
    }

    public void setMaximumValue(BigDecimal maximumValue) {
        setMaximum(maximumValue);
    }

    public BigDecimal getValueOf(String valueStr) throws NumberFormatException {
        BigDecimal value = null;
        if ((valueStr != null) && (valueStr.length() > 0)) {
            value = new BigDecimal(valueStr);
        }
        return value;
    }

    public String getValueName() {
        return "a decimal value";
    }

    public VerifiedDecimalModel getNewInstance(boolean isCloneRequired) {
        VerifiedDecimalModel instance = this;
        if (isCloneRequired || (! isSharedForAllSessionFiles())) {
            instance = new VerifiedDecimalModel();
            cloneValuesForNewInstance(instance);
        }
        return instance;
    }

}