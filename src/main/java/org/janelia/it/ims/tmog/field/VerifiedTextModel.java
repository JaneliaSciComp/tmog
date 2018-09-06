/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;

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
    private String patternMatchFailureMessage;
    private boolean convertToUpperCase;

    public VerifiedTextModel() {
        super();
    }

    public boolean verify() {

        boolean isValid = true;
        String value = getFullText();

        if ((value != null) && (value.length() > 0)) {

            if (convertToUpperCase) {
                value = value.toUpperCase();
                setText(value);
            }

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
                    if (patternMatchFailureMessage == null) {
                        setErrorMessage("This field should contain a value that matches the pattern: " + pattern);
                    } else {
                        setErrorMessage(patternMatchFailureMessage);
                    }
                }
            }

        } else if (isRequired()) {
            isValid = false;
            setRequiredErrorMessage();
        }

        return isValid;
    }

    public VerifiedTextModel getNewInstance(boolean isCloneRequired) {
        VerifiedTextModel instance = this;
        if (isCloneRequired || (! isSharedForAllSessionFiles())) {
            instance = new VerifiedTextModel();
            cloneValuesForNewInstance(instance);
            instance.minimumLength = minimumLength;
            instance.maximumLength = maximumLength;
            instance.pattern = pattern;
            instance.patternMatchFailureMessage = patternMatchFailureMessage;
            instance.convertToUpperCase = convertToUpperCase;
        }
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

    public String getPatternMatchFailureMessage() {
        return patternMatchFailureMessage;
    }

    public boolean getConvertToUpperCase() {
        return convertToUpperCase;
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

    public void setPatternMatchFailureMessage(String patternMatchFailureMessage) {
        this.patternMatchFailureMessage = patternMatchFailureMessage;
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
