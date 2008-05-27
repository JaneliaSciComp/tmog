/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.Target;

import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * This class provides an abstract implementation for text fields that
 * can be verified.
 *
 * @author Eric Trautman
 */
public abstract class VerifiedFieldModel extends PlainDocument implements DataField {

    private String displayName;
    private boolean isRequired;
    private String errorMessage;
    private String prefix;
    private String suffix;
    private boolean isCopyable;
    private DefaultValueList defaultValueList;

    public VerifiedFieldModel() {
        super();
        this.defaultValueList = new DefaultValueList();
        this.isCopyable = true;
    }

    public abstract boolean verify();

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEditable() {
        return true;
    }

    public boolean isCopyable() {
        return isCopyable;
    }

    public abstract VerifiedFieldModel getNewInstance();

    public void cloneValuesForNewInstance(VerifiedFieldModel instance) {
        instance.setText(getFullText());
        instance.displayName = displayName;
        instance.isRequired = isRequired;
        instance.prefix = prefix;
        instance.suffix = suffix;
        instance.isCopyable = isCopyable;
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
            if ((prefix != null) || (suffix != null)) {
                StringBuilder sb = new StringBuilder(64);
                if (prefix != null) {
                    sb.append(prefix);
                }
                sb.append(fileNameValue);
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
        }
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setRequired(boolean required) {
        isRequired = required;
    }

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
