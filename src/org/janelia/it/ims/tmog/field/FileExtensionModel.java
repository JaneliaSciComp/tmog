/*
 * Copyright (c) 2015 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.config.preferences.FieldDefaultSet;
import org.janelia.it.ims.tmog.target.Target;

/**
 * This model supports inserting the source file extension
 * into a rename pattern.
 *
 * @author Eric Trautman
 */
public class FileExtensionModel implements DataField {

    private String extension;
    private boolean markedForTask;

    public FileExtensionModel() {
        this.markedForTask = true;
    }

    public String getDisplayName() {
        return null;
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

    public FileExtensionModel getNewInstance(boolean isCloneRequired) {
        FileExtensionModel instance = new FileExtensionModel();
        instance.setMarkedForTask(markedForTask);
        // do not copy extension (must be derived when rename occurs)
        return instance;
    }

    public String getCoreValue() {
        return getFileNameValue();
    }

    public String getFileNameValue() {
        String value = getExtension();
        if (value == null) {
            value = "";
        }
        return value;
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
        extension = null;
        if (target != null) {
            String fileName = target.getName();
            if (fileName.endsWith(LSM_BZ2_EXTENSION)) {
                extension = LSM_BZ2_EXTENSION;
            } else {
                final int extStart = fileName.lastIndexOf('.');
                if ((extStart > -1) && (extStart < (fileName.length() - 1))) {
                    extension = fileName.substring(extStart);
                }
            }
        }
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

    public String getExtension() {
        return extension;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setExtension(String extension) {
        this.extension = extension;
    }

    @Override
    public String toString() {
        return getFileNameValue();
    }

    private static final String LSM_BZ2_EXTENSION = ".lsm.bz2";
}
