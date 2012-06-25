/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.config.preferences.FieldDefaultSet;
import org.janelia.it.ims.tmog.target.FileTarget;
import org.janelia.it.ims.tmog.target.Target;

import java.io.File;

/**
 * This model supports capturing the target's relative path
 * for processing.
 *
 * @author Eric Trautman
 */
public class FileRelativePathModel implements DataField {

    private String displayName;
    private Integer displayWidth;
    private boolean markedForTask;
    private String path;
    private boolean visible;

    public FileRelativePathModel() {
        this.markedForTask = true;
        this.visible = true;
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

    public boolean isCopyable() {
        return false;
    }

    public boolean isMarkedForTask() {
        return markedForTask;
    }

    public void setMarkedForTask(boolean markedForTask) {
        this.markedForTask = markedForTask;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public FileRelativePathModel getNewInstance(boolean isCloneRequired) {
        FileRelativePathModel instance = new FileRelativePathModel();
        instance.setDisplayName(displayName);
        instance.setDisplayWidth(displayWidth);
        instance.setMarkedForTask(markedForTask);
        instance.setVisible(visible);
        // do not copy path (must be derived when rename occurs)
        return instance;
    }

    public String getCoreValue() {
        return getFileNameValue();
    }

    public String getFileNameValue() {
        String value = path;
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
        path = null;
        if (target instanceof FileTarget) {
            FileTarget fileTarget = (FileTarget) target;
            File file = fileTarget.getFile();
            File rootPathFile = fileTarget.getRootPath();

            if ((file != null) && (rootPathFile != null)) {     
                String parentName = file.getParent();
                if (parentName != null) {
                    int start = rootPathFile.getAbsolutePath().length() + 1;
                    if (start < parentName.length()) {
                        path = parentName.substring(start);
                        path = path.replace('\\', '/');
                    }
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

    @Override
    public String toString() {
        return getFileNameValue();
    }
}