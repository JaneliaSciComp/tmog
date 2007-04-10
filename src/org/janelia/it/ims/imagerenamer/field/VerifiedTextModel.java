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
    private boolean convertToUpperCase;

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
                            "This field should contain a value that " +
                            "matches the pattern: " + pattern);
                }
            }

        } else if (isRequired()) {
            isValid = false;
            setRequiredErrorMessage();
        }

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
        instance.convertToUpperCase = convertToUpperCase;
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

    public boolean getConvertToUpperCase() {
        return convertToUpperCase;
    }

    @Override
    public void setText(String t) {
        String text = t;
        if ((t != null) && convertToUpperCase) {
            text = t.toUpperCase();
        }
        super.setText(text);
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

    public void setConvertToUpperCase(boolean convertToUpperCase) {
        this.convertToUpperCase = convertToUpperCase;
    }

    private void setMinMaxErrorMessage() {
        String msg = null;

        if (minimumLength != null) {
            if (maximumLength != null) {
                msg = "This field should contain a value that is between " +
                      minimumLength + " and " + maximumLength + " characters.";
            } else {
                msg = "This field should contain a value that with at least " +
                      minimumLength + " characters.";
            }
        } else if (maximumLength != null) {
            msg = "This field should contain a value with no more than " +
                   maximumLength + " characters.";
        }

        setErrorMessage(msg);
    }
}
