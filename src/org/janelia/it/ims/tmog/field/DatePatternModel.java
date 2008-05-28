/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class provides an abstract implementation for date based field models.
 *
 * @author Eric Trautman
 */
public abstract class DatePatternModel implements DataField, DatePatternField {
    private String displayName;
    private String datePattern;
    private boolean markedForTask;

    public DatePatternModel() {
        this.markedForTask = true;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isEditable() {
        return false;
    }

    public boolean isCopyable() {
        return false;
    }

    public boolean isMarkedForTask() {
        return markedForTask;
    }

    public void setMarkedForTask(boolean markedForTask) {
        this.markedForTask = markedForTask;
    }

    public abstract DatePatternModel getNewInstance();

    public abstract String getFileNameValue();

    public String getCoreValue() {
        return getFileNameValue();
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

    protected void initNewInstance(DatePatternModel instance) {
        instance.setDisplayName(displayName);
        instance.setDatePattern(datePattern);
        instance.setMarkedForTask(markedForTask);
    }

}
