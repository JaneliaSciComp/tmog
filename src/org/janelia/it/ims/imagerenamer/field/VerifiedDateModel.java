/*
 * Copyright © 2008 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.field;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * This model supports inserting verified (validated) date strings
 * into a rename datePattern.
 *
 * @author Eric Trautman
 */
public class VerifiedDateModel extends VerifiedFieldModel {

    private String datePattern;
    private SimpleDateFormat formatter;

    public VerifiedDateModel() {
        super();
        this.formatter = new SimpleDateFormat();
        this.formatter.setLenient(false);
    }

    public boolean verify() {

        boolean isValid = true;
        String value = getFullText();

        if ((value != null) && (value.length() > 0)) {
            try {
                formatter.parse(value);
            } catch (ParseException e) {
                setErrorMessage(
                        "This field should contain a valid date that " +
                        "is formatted with the pattern: " +
                        getDatePattern());
                isValid = false;
            }

        } else if (isRequired()) {
            isValid = false;
            setRequiredErrorMessage();
        }

        return isValid;
    }

    public VerifiedDateModel getNewInstance() {
        VerifiedDateModel instance = new VerifiedDateModel();
        cloneValuesForNewInstance(instance);
        instance.datePattern = datePattern;
        instance.formatter = formatter;
        return instance;
    }

    public String getDatePattern() {
        return formatter.toPattern();
    }

    public void setDatePattern(String datePattern) {
        this.datePattern = datePattern;
        this.formatter = new SimpleDateFormat(datePattern);
        this.formatter.setLenient(false);
    }

}