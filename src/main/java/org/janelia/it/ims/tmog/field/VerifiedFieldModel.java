/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.config.preferences.FieldDefault;
import org.janelia.it.ims.tmog.config.preferences.FieldDefaultSet;
import org.janelia.it.ims.tmog.target.Target;
import org.janelia.it.utils.PadFormatter;

import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * This class provides an abstract implementation for text fields that
 * can be verified.
 *
 * @author Eric Trautman
 */
public abstract class VerifiedFieldModel extends PlainDocument
        implements DataField, DefaultValueModel {

    private String displayName;
    private Integer displayWidth;
    private boolean isRequired;
    private String errorMessage;
    private String prefix;
    private String suffix;
    private boolean isEditable;
    private boolean isVisible;
    private boolean isCopyable;
    private boolean markedForTask;
    private boolean sharedForAllSessionFiles;
    private PadFormatter padFormatter;
    private DefaultValueList defaultValueList;

    public VerifiedFieldModel() {
        super();
        this.defaultValueList = new DefaultValueList();
        this.isEditable = true;
        this.isVisible = true;
        this.isCopyable = true;
        this.markedForTask = true;
        this.displayWidth = 100;
    }

    public abstract boolean verify();

    public String getDisplayName() {
        return displayName;
    }

    public Integer getDisplayWidth() {
        return displayWidth;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setDisplayWidth(Integer displayWidth) {
        this.displayWidth = displayWidth;
    }

    public boolean isEditable() {
        return isEditable;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public boolean isCopyable() {
        return isCopyable;
    }

    public boolean isMarkedForTask() {
        return markedForTask;
    }

    public boolean isSharedForAllSessionFiles() {
        return sharedForAllSessionFiles;
    }

    public void setSharedForAllSessionFiles(boolean sharedForAllSessionFiles) {
        this.sharedForAllSessionFiles = sharedForAllSessionFiles;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setMarkedForTask(boolean markedForTask) {
        this.markedForTask = markedForTask;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getPadFormat() {
        String padFormat = null;
        if (padFormatter != null) {
            padFormat = padFormatter.getFormat();
        }
        return padFormat;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setPadFormat(String padFormat) {
        this.padFormatter = new PadFormatter(padFormat);
    }

    public abstract VerifiedFieldModel getNewInstance(boolean isCloneRequired);

    public void cloneValuesForNewInstance(VerifiedFieldModel instance) {
        instance.setText(getFullText());
        instance.displayName = displayName;
        instance.displayWidth = displayWidth;
        instance.isRequired = isRequired;
        instance.prefix = prefix;
        instance.suffix = suffix;
        instance.isEditable = isEditable;
        instance.isVisible = isVisible;
        instance.isCopyable = isCopyable;
        instance.markedForTask = markedForTask;
        instance.sharedForAllSessionFiles = sharedForAllSessionFiles;
        instance.padFormatter = padFormatter;
        instance.defaultValueList = defaultValueList;  // shallow copy is ok
    }

    public String getCoreValue() {
        String coreValue = getFullText();
        if (coreValue == null) {
            coreValue = "";
        }
        return coreValue;
    }

    public String getFileNameValue() {
        String fileNameValue = getFullText();
        if ((fileNameValue != null) && (fileNameValue.length() > 0)) {

            if ((prefix != null) ||
                (suffix != null) ||
                (padFormatter != null)) {

                StringBuilder sb = new StringBuilder(64);
                if (prefix != null) {
                    sb.append(prefix);
                }
                if (padFormatter != null) {
                    sb.append(padFormatter.formatValue(fileNameValue));
                } else {
                    sb.append(fileNameValue);
                }
                if (suffix != null) {
                    sb.append(suffix);
                }
                fileNameValue = sb.toString();
            }

        } else {
            fileNameValue = "";
        }
        return fileNameValue;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public void addDefaultValue(DefaultValue defaultValue) {
        defaultValueList.add(defaultValue);
    }

    public DefaultValueList getDefaultValueList() {
        return defaultValueList;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Initializes this field's value based upon the specified target.
     *
     * @param  target  the target being processed.
     */
    public void initializeValue(Target target) {
        String defaultValue = defaultValueList.getValue(target);
        if (defaultValue != null) {
            setText(defaultValue);
        } else {
            setText("");
        }
    }

    @Override
    public void applyValue(String value) {
        if (value != null) {
            setText(value);
        }
    }

    public void applyDefault(FieldDefaultSet defaultSet) {
        final FieldDefault fieldDefault =
                defaultSet.getFieldDefault(displayName);
        if (fieldDefault != null) {
            final String value = fieldDefault.getValue();
            applyValue(value);
        }
    }

    public void addAsDefault(FieldDefaultSet defaultSet) {
        final String coreValue = getCoreValue();
        if (coreValue.length() > 0) {
            FieldDefault fieldDefault = new FieldDefault();
            fieldDefault.setName(displayName);
            fieldDefault.setValue(coreValue);
            defaultSet.addFieldDefault(fieldDefault);
        }
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setRequired(boolean required) {
        isRequired = required;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setEditable(boolean editable) {
        isEditable = editable;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setCopyable(boolean copyable) {
        isCopyable = copyable;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setRequiredErrorMessage() {
        this.errorMessage = "This is a required field.";
    }

    public String getFullText() {
        String text;
        try {
            text = getText(0, getLength());
        } catch (BadLocationException e) {
            text = null;
        }
        return text;
    }

    public void setText(String t) {
        try {
            this.replace(0, this.getLength(), t, null);
        } catch (BadLocationException e) {
	        e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return getFullText();
    }
}
