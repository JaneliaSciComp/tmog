/*
 * Copyright © 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.field;

import javax.swing.text.PlainDocument;
import javax.swing.text.BadLocationException;

/**
 * This class provides an abstract implementation for text fields that
 * can be verified.
 *
 * @author Eric Trautman
 */
public abstract class VerifiedFieldModel extends PlainDocument implements RenameField {

    private String displayName;
    private boolean isRequired;
    private String errorMessage;

    public VerifiedFieldModel() {
        super();
    }

    public abstract boolean verify();

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEditable() {
        return true;
    }

    public abstract VerifiedFieldModel getNewInstance();

    public String getFileNameValue() {
        String fileNameValue = getFullText();
        if (fileNameValue == null) {
            fileNameValue = "";
        }
        return fileNameValue;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setRequired(boolean required) {
        isRequired = required;
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
