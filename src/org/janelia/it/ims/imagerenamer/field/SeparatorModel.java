/*
 * Copyright © 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.field;

import java.io.File;

/**
 * This model supports inserting static text into a rename pattern.
 *
 * @author Eric Trautman
 */
public class SeparatorModel implements RenameField {
    private String value;

    public SeparatorModel() {
    }

    public String getDisplayName() {
        return null;
    }

    public boolean isEditable() {
        return false;
    }

    public SeparatorModel getNewInstance() {
        SeparatorModel instance = new SeparatorModel();
        instance.setValue(value);
        return instance;
    }

    public String getFileNameValue() {
        String fileNameValue = value;
        if (value == null) {
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

    /**
     * Initializes this field's value if necessary.
     *
     * @param sourceFile the source file being renamed.
     */
    public void initializeValue(File sourceFile) {
        // nothing to initialize
    }
    
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
