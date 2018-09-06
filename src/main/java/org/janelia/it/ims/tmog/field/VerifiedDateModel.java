/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This model supports inserting verified (validated) date strings
 * into a rename datePattern.
 *
 * @author Eric Trautman
 */
public class VerifiedDateModel extends VerifiedFieldModel implements DatePatternField {

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
                Date date = formatter.parse(value);
                // reformat value in case it was entered in a parsable
                // but non-standard format (e.g. 2009_1_1 => 2009_01_01)
                setText(formatter.format(date));
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

    public VerifiedDateModel getNewInstance(boolean isCloneRequired) {
        VerifiedDateModel instance = this;
        if (isCloneRequired || (! isSharedForAllSessionFiles())) {
            instance = new VerifiedDateModel();
            cloneValuesForNewInstance(instance);
            instance.formatter = formatter;
        }
        return instance;
    }

    public String getDatePattern() {
        return formatter.toPattern();
    }

    public void setDatePattern(String datePattern) {
        this.formatter = new SimpleDateFormat(datePattern);
        this.formatter.setLenient(false);
    }

}