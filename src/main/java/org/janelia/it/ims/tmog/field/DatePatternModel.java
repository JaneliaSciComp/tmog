/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.config.preferences.FieldDefaultSet;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class provides an abstract implementation for date based field models.
 *
 * @author Eric Trautman
 */
public abstract class DatePatternModel implements DataField, DatePatternField {
    private String displayName;
    private Integer displayWidth;
    private String datePattern;
    private boolean markedForTask;
    private boolean sharedForAllSessionFiles;

    public DatePatternModel() {
        this.markedForTask = true;
        this.sharedForAllSessionFiles = false;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getDisplayWidth() {
        return displayWidth;
    }

    public void setDisplayWidth(Integer displayWidth) {
        this.displayWidth = displayWidth;
    }

    public boolean isEditable() {
        return false;
    }

    public boolean isVisible() {
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

    public boolean isSharedForAllSessionFiles() {
        return sharedForAllSessionFiles;
    }

    public void setSharedForAllSessionFiles(boolean sharedForAllSessionFiles) {
        this.sharedForAllSessionFiles = sharedForAllSessionFiles;
    }

    public abstract DatePatternModel getNewInstance(boolean isCloneRequired);

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
    public void applyValue(String value) {
        // single string values do not get applied to this model
    }

    public void applyDefault(FieldDefaultSet defaultSet) {
        // defaults do not get applied to this model
    }

    public void addAsDefault(FieldDefaultSet defaultSet) {
        // defaults do not get applied to this model
    }

    @Override
    public String toString() {
        return getFileNameValue();
    }

    protected void initNewInstance(DatePatternModel instance) {
        instance.setDisplayName(displayName);
        instance.setDisplayWidth(displayWidth);
        instance.setDatePattern(datePattern);
        instance.setMarkedForTask(markedForTask);
        instance.setSharedForAllSessionFiles(sharedForAllSessionFiles);
    }

}
