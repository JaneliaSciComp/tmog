/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.target.Target;

import java.util.Formatter;

/**
 * This model supports inserting plugin provided data into a rename pattern.
 *
 * @author Eric Trautman
 */
public class PluginDataModel implements DataField {

    private String displayName;
    private Object value;
    private String format;
    private boolean markedForTask;

    public PluginDataModel() {
        this.markedForTask = true;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Integer getDisplayWidth() {
        return null;
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

    public PluginDataModel getNewInstance(boolean isCloneRequired) {
        PluginDataModel instance = new PluginDataModel();
        instance.setDisplayName(displayName);
        instance.setValue(value);
        instance.setFormat(format);
        instance.setMarkedForTask(markedForTask);
        return instance;
    }

    public String getCoreValue() {
        return getFileNameValue();
    }

    public String getFileNameValue() {
        String fileNameValue;
        if (value == null) {
            fileNameValue = "";
        } else {
            if (format != null) {
                StringBuilder sb = new StringBuilder();
                Formatter formatter = new Formatter(sb);
                formatter.format(format, value);
                fileNameValue = sb.toString();
            } else {
                fileNameValue = String.valueOf(value);
            }
        }
        return fileNameValue;
    }

    public boolean verify() {
        return true;
    }

    public String getErrorMessage() {
        return null;
    }

    /**
     * Initializes this field's value based upon the specified target.
     *
     * @param  target  the target being processed.
     */
    public void initializeValue(Target target) {
        value = null;
    }

    public Object getValue() {
        return value;
    }

    public String getFormat() {
        return format;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("PluginDataModel");
        sb.append("{displayName='").append(displayName).append('\'');
        sb.append(", value='").append(value).append('\'');
        sb.append(", format='").append(format).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
