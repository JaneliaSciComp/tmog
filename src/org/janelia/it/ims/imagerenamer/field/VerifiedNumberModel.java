/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.field;

/**
 * This model supports inserting verified (validated) numeric text
 * into a rename pattern.
 *
 * @author Eric Trautman
 */
public class VerifiedNumberModel extends VerifiedFieldModel {

    private Integer minimumValue;
    private Integer maximumValue;

    public VerifiedNumberModel() {
        super();
    }

    public boolean verify() {

        boolean isValid = true;
        String valueStr = getFullText();

        if ((valueStr != null) && (valueStr.length() > 0)) {
            try {
                int value = Integer.parseInt(valueStr);
                if ((minimumValue != null) && (value < minimumValue)) {
                    isValid = false;
                    setMinMaxErrorMessage();
                } else if ((maximumValue != null) && (value > maximumValue)) {
                    isValid = false;
                    setMinMaxErrorMessage();
                }
            } catch (NumberFormatException e) {
                isValid = false;
                setErrorMessage("This field should contain a valid number.");
            }

        } else if (isRequired()) {
            isValid = false;
            setRequiredErrorMessage();
        }

        return isValid;
    }

    public VerifiedNumberModel getNewInstance() {
        VerifiedNumberModel instance = new VerifiedNumberModel();
        instance.setText(getFullText());
        instance.setDisplayName(getDisplayName());
        instance.setRequired(isRequired());
        instance.minimumValue = minimumValue;
        instance.maximumValue = maximumValue;
        return instance;
    }

    public Integer getMinimumValue() {
        return minimumValue;
    }

    public Integer getMaximumValue() {
        return maximumValue;
    }

    public void setMinimumValue(Integer minimumValue) {
        this.minimumValue = minimumValue;
    }

    public void setMaximumValue(Integer maximumValue) {
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
                msg = "This field should contain a number that is between " +
                      minimumValue + " and " + maximumValue + ".";
            } else {
                msg = "This field should contain a number that is " +
                      "greater than or equal to " + minimumValue + ".";
            }
        } else if (maximumValue != null) {
            msg = "This field should contain a number that is " +
                  "less than or equal to " + maximumValue + ".";
        }

        setErrorMessage(msg);
    }
}
