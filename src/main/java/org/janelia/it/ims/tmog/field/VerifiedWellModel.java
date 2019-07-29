/*
 * Copyright (c) 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

/**
 * This model supports inserting verified (validated) free-form text
 * into a rename pattern.
 *
 * @author Rob Svirskas
 */
public class VerifiedWellModel extends VerifiedFieldModel {

    private Integer formFactor;
    private Integer minimumX, maximumX;
    private char minimumY, maximumY;

    public VerifiedWellModel() {
        super();
        minimumX = 1;
        minimumY = 'A';
    }

    public boolean verify() {

        boolean isValid = true;
        String value = getFullText();

        if ((value != null) && (value.length() > 0)) {

            value = value.toUpperCase();
            String stringXCoord = value.substring(1);
            if (value.length() == 2) {
              value = value.substring(0,1) + '0' + stringXCoord;
            }
            setText(value);

            Integer xCoord;
            try {
                xCoord = Integer.parseInt(stringXCoord);
                char yCoord = value.charAt(0);
                if (xCoord < minimumX || xCoord > maximumX || stringXCoord.length() > String.valueOf(maximumX).length()) {
                    isValid = false;
                    setXErrorMessage();
                }
                else if (yCoord < minimumY || yCoord > maximumY) {
                    isValid = false;
                    setYErrorMessage();
                }
            } catch (NumberFormatException e) {
                isValid = false;
                setXErrorMessage();
            }
        } else if (isRequired()) {
            isValid = false;
            setRequiredErrorMessage();
        }

        return isValid;
    }

    public VerifiedWellModel getNewInstance(boolean isCloneRequired) {
        VerifiedWellModel instance = this;
        if (isCloneRequired || (! isSharedForAllSessionFiles())) {
            instance = new VerifiedWellModel();
            cloneValuesForNewInstance(instance);
            instance.setFormFactor(formFactor);
        }
        return instance;
    }

    public Integer getFormFactor() {
        return formFactor;
    }

    public Integer getMinimumX() {
        return minimumX;
    }

    public Integer getMaximumX() {
        return maximumX;
    }

    public char getMinimumY() {
        return minimumY;
    }

    public char getMaximumY() {
        return maximumY;
    }

    public void setFormFactor(Integer formFactor) throws IllegalArgumentException {
        this.formFactor = formFactor;
        if (formFactor == 96) {
            maximumX = 12;
            maximumY = 'H';
        }
        else if (formFactor == 192) {
            maximumX = 24;
            maximumY = 'H';
        }
        else if (formFactor == 384) {
            maximumX = 24;
            maximumY = 'P';
        }
        else {
            throw new IllegalArgumentException("Valid form factors are: 96, 192, 384 (received " + formFactor + ")");
        }
    }

    private void setXErrorMessage() {
        String msg = "The well X coordinate should be between " +
                minimumX + " and " + maximumX + ".";
        setErrorMessage(msg);
    }

    private void setYErrorMessage() {
        String msg = "The well Y coordinate should be between " +
                minimumY + " and " + maximumY + ".";
        setErrorMessage(msg);
    }
}
