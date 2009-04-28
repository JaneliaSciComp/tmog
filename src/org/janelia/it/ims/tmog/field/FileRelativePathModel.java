/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

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

    public boolean isEditable() {
        // NOTE: This will make the value visible in the data table, 
        // but it is not really editable since it does not have a renderer.
        return visible;
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
    public String toString() {
        return getFileNameValue();
    }
}