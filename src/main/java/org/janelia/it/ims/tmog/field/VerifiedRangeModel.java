/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

/**
 * This model supports inserting verified (validated) text
 * into a rename pattern.  Verification includes ensuring that
 * the specified value is within a range of values.
 *
 * @author Eric Trautman
 */
public abstract class VerifiedRangeModel<T extends Comparable<T>>
        extends VerifiedFieldModel {

    private T minimum;
    private T maximum;

    public VerifiedRangeModel() {
        super();
    }

    public boolean verify() {

        boolean isValid = true;
        String valueStr = getFullText();
        try {
            T value = getValueOf(valueStr);

            if (value != null) {
                if ((minimum != null) &&
                    (value.compareTo(minimum) < 0)) {
                    isValid = false;
                    setMinMaxErrorMessage();
                } else if ((maximum != null) &&
                           (value.compareTo(maximum) > 0)) {
                    isValid = false;
                    setMinMaxErrorMessage();
                }

            } else if (isRequired()) {
                isValid = false;
                setRequiredErrorMessage();
            }

        } catch (IllegalArgumentException e) {
            isValid = false;
            setErrorMessage("This field should contain " +
                            getValueName() + ".");
        }

        return isValid;
    }

    /**
     * Converts the specified value string into a object than can be
     * compared to range constraints.
     *
     * @param  valueStr  string to convert.
     *
     * @return an object that can be compared to range constraints.
     *
     * @throws IllegalArgumentException
     *   if the value string cannot be converted.
     */
    public abstract T getValueOf(String valueStr)
            throws IllegalArgumentException;

    /**
     * @return a name (e.g. "an integer value") for the type of values
     *         verified by this model.  This name is used in verfication
     *         error messages.
     */
    public abstract String getValueName();

    public void cloneValuesForNewInstance(VerifiedRangeModel<T> instance) {
        super.cloneValuesForNewInstance(instance);
        instance.minimum = minimum;
        instance.maximum = maximum;
    }

    public T getMinimum() {
        return minimum;
    }

    public T getMaximum() {
        return maximum;
    }

    public void setMinimum(T minimum) {
        this.minimum = minimum;
    }

    public void setMaximum(T maximum) {
        this.maximum = maximum;
    }

// TODO: consider numeric padding support
//    public String getFileNameValue() {
//        String fileNameValue = super.getFileNameValue();
//        Integer value = Integer.parseInt(fileNameValue);
//        fileNameValue = String.format("%1$03d", value);
//        return fileNameValue;
//    }

    public String getFullText() {
        String text = super.getFullText();
        if (text != null) {
            text = text.trim();
        }
        return text;
    }

    private void setMinMaxErrorMessage() {
        String msg = null;
        
        if (minimum != null) {
            if (maximum != null) {
                msg = "This field should contain " + getValueName() +
                      " that is between " + minimum + " and " +
                                                              maximum + ".";
            } else {
                msg = "This field should contain " + getValueName() +
                      " that is greater than or equal to " + minimum + ".";
            }
        } else if (maximum != null) {
            msg = "This field should contain " + getValueName() +
                  " that is less than or equal to " + maximum + ".";
        }

        setErrorMessage(msg);
    }
}
