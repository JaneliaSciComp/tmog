/*
 * Copyright © 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.field;

import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * This class provides an abstract implementation for date based field models.
 *
 * @author Eric Trautman
 */
public abstract class DatePatternModel implements RenameField {
    private String datePattern;

    public DatePatternModel() {
    }

    public String getDisplayName() {
        return null;
    }

    public boolean isEditable() {
        return false;
    }

    public abstract DatePatternModel getNewInstance();

    public String getFileNameValue() {
        Date now = new Date();
        return getFileNameValue(now);
    }

    public String getFileNameValue(Date sourceDate) {
        String fileNameValue;
        if (datePattern != null)  {
            SimpleDateFormat sdf = new SimpleDateFormat(datePattern);
            fileNameValue = sdf.format(sourceDate);
        } else {
            fileNameValue = "";
        }
        return fileNameValue;
    }

    public boolean verify() {
        return true;
    }

    public String getErrorMessage() {
        return null;
    }

    public String getDatePattern() {
        return datePattern;
    }

    public void setDatePattern(String datePattern) {
        this.datePattern = datePattern;
    }

    @Override
    public String toString() {
        return getFileNameValue();
    }
}
