/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.config.preferences.FieldDefaultSet;
import org.janelia.it.ims.tmog.target.Target;

/**
 * This model supports capturing the target name for processing.
 *
 * @author Eric Trautman
 */
public class TargetNameModel implements DataField {

    private String displayName;
    private boolean markedForTask;
    private String targetName;

    public TargetNameModel() {
        this.markedForTask = true;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Integer getDisplayWidth() {
        return null;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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

    public TargetNameModel getNewInstance(boolean isCloneRequired) {
        TargetNameModel instance = new TargetNameModel();
        instance.setDisplayName(displayName);
        instance.setMarkedForTask(markedForTask);
        // do not copy targetName (must be derived when rename occurs)
        return instance;
    }

    public String getCoreValue() {
        return getFileNameValue();
    }

    public String getFileNameValue() {
        String value = targetName;
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
        targetName = null;
        if (target != null) {
            targetName = target.getName();
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