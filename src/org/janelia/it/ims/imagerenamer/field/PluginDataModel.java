/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.field;

import java.io.File;
import java.util.Formatter;

/**
 * This model supports inserting plugin provided data into a rename pattern.
 *
 * @author Eric Trautman
 */
public class PluginDataModel implements RenameField {

    private String displayName;
    private Object value;
    private String format;

    public PluginDataModel() {
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEditable() {
        return false;
    }

    public boolean isCopyable() {
        return false;
    }

    public PluginDataModel getNewInstance() {
        PluginDataModel instance = new PluginDataModel();
        instance.setDisplayName(displayName);
        instance.setValue(value);
        instance.setFormat(format);
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
     * Initializes this field's value if necessary.
     *
     * @param sourceFile the source file being renamed.
     */
    public void initializeValue(File sourceFile) {
        // nothing to initialize
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
