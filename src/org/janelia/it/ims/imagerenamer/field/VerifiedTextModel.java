/*
 * Copyright © 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.field;

/**
 * This model supports inserting verified (validated) free-form text
 * into a rename pattern.
 *
 * @author Eric Trautman
 */
public class VerifiedTextModel extends VerifiedFieldModel {

    private Integer minimumLength;
    private Integer maximumLength;
    private String pattern;
    private boolean upcase;

    public VerifiedTextModel() {
        super();
    }

    public boolean verify() {

        boolean isValid = true;
        String value = getFullText();

        if ((value != null) && (value.length() > 0)) {
            if ((minimumLength != null) && (value.length() < minimumLength)) {
                isValid = false;
                setMinMaxErrorMessage();
            } else if ((maximumLength != null) &&
                    (value.length() > maximumLength)) {
                isValid = false;
                setMinMaxErrorMessage();
            } else if (pattern != null) {
                if (! value.matches(pattern)) {
                    isValid = false;
                    setErrorMessage(
                            "Please enter a value that matches the pattern: " +
                            pattern);
                }
            }

        } else if (isRequired()) {
            isValid = false;
            setErrorMessage("This is a required field.  Would you like to fill it in now?");
        }
        if (isValid && upcase) setText(getFullText().toUpperCase()); //force to upeer case only if valid
        return isValid;
    }

    public VerifiedTextModel getNewInstance() {
        VerifiedTextModel instance = new VerifiedTextModel();
        instance.setText(getFullText());
        instance.setDisplayName(getDisplayName());
        instance.setRequired(isRequired());
        instance.minimumLength = minimumLength;
        instance.maximumLength = maximumLength;
        instance.pattern = pattern;
        instance.upcase = upcase;
        return instance;
    }

    public Integer getMinimumLength() {
        return minimumLength;
    }

    public Integer getMaximumLength() {
        return maximumLength;
    }

    public String getPattern() {
        return pattern;
    }

    public boolean getUpcase() {
        return upcase;
    }

    public void setMinimumLength(Integer minimumLength) {
        this.minimumLength = minimumLength;
    }

    public void setMaximumLength(Integer maximumLength) {
        this.maximumLength = maximumLength;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public void setUpcase(boolean upcase) {
        this.upcase = upcase;
    }

    private void setMinMaxErrorMessage() {
        String msg = null;

        if (minimumLength != null) {
            if (maximumLength != null) {
                msg = "Please enter a value that is between " + minimumLength +
                       " and " + maximumLength + " characters.";
            } else {
                msg = "Please enter a value that with at least " +
                      minimumLength + " characters.";
            }
        } else if (maximumLength != null) {
            msg = "Please enter a value with no more than " +
                   maximumLength + " characters.";
        }

        setErrorMessage(msg);
    }
}
