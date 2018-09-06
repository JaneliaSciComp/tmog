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
 * This model supports including static text as a data field.
 *
 * @author Eric Trautman
 */
public class StaticDataModel implements DataField {
    private String name;
    private String value;
    private boolean markedForTask;

    public StaticDataModel() {
        this.markedForTask = true;
    }

    public StaticDataModel(String name,
                           String value) {
        this();
        this.name = name;
        this.value = value;
    }

    public String getDisplayName() {
        return name;
    }

    public Integer getDisplayWidth() {
        return null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public StaticDataModel getNewInstance(boolean isCloneRequired) {
        StaticDataModel instance = new StaticDataModel();
        instance.setName(name);
        instance.setValue(value);
        instance.setMarkedForTask(markedForTask);
        return instance;
    }

    public String getCoreValue() {
        return getFileNameValue();
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
     * Initializes this field's value based upon the specified target.
     *
     * @param  target  the target being processed.
     */
    public void initializeValue(Target target) {
        // nothing to initialize
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
