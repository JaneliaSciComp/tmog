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

    private T minimumValue;
    private T maximumValue;

    public VerifiedRangeModel() {
        super();
    }

    public boolean verify() {

        boolean isValid = true;
        String valueStr = getFullText();
        try {
            T value = getValueOf(valueStr);

            if (value != null) {
                if ((minimumValue != null) &&
                    (value.compareTo(minimumValue) < 0)) {
                    isValid = false;
                    setMinMaxErrorMessage();
                } else if ((maximumValue != null) &&
                           (value.compareTo(maximumValue) > 0)) {
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
        instance.minimumValue = minimumValue;
        instance.maximumValue = maximumValue;
    }

    public T getMinimumValue() {
        return minimumValue;
    }

    public T getMaximumValue() {
        return maximumValue;
    }

    public void setMinimumValue(T minimumValue) {
        this.minimumValue = minimumValue;
    }

    public void setMaximumValue(T maximumValue) {
        this.maximumValue = maximumValue;
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
        
        if (minimumValue != null) {
            if (maximumValue != null) {
                msg = "This field should contain " + getValueName() +
                      " that is between " + minimumValue + " and " +
                      maximumValue + ".";
            } else {
                msg = "This field should contain " + getValueName() +
                      " that is greater than or equal to " + minimumValue + ".";
            }
        } else if (maximumValue != null) {
            msg = "This field should contain " + getValueName() +
                  " that is less than or equal to " + maximumValue + ".";
        }

        setErrorMessage(msg);
    }
}
