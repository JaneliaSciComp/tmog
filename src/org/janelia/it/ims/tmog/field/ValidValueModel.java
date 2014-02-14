/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;

import ca.odell.glazedlists.BasicEventList;
import org.janelia.it.ims.tmog.config.preferences.FieldDefault;
import org.janelia.it.ims.tmog.config.preferences.FieldDefaultSet;
import org.janelia.it.ims.tmog.target.Target;

import javax.swing.*;
import java.util.Collections;
import java.util.Comparator;

/**
 * This model supports inserting a selected value from a predefined set
 * of valid values into a rename pattern.
 *
 * @author Eric Trautman
 */
public class ValidValueModel extends AbstractListModel<ValidValue>
        implements ComboBoxModel<ValidValue>, DataField, DefaultValueModel {

    private String displayName;
    private boolean isRequired;
    private boolean isAutoComplete;
    private BasicEventList<ValidValue> validValues;
    private ValidValue selectedValue;
    private String errorMessage;
    private String prefix;
    private String suffix;
    private boolean isCopyable;
    private boolean markedForTask;
    private boolean sharedForAllSessionFiles;
    private DefaultValueList defaultValueList;

    public ValidValueModel() {
        this.isRequired = false;
        this.isAutoComplete = false;
        this.validValues = new BasicEventList<ValidValue>();
        this.validValues.add(ValidValue.NONE);
        this.isCopyable = true;
        this.markedForTask = true;
        this.sharedForAllSessionFiles = false;
        this.defaultValueList = new DefaultValueList();
    }

    public void addValidValue(ValidValue validValue) {
        if (validValue.isDefined()) {
            validValues.add(validValue);
            if (validValue.isDefault() && (selectedValue == null)) {
                selectedValue = validValue;
            }
        }
    }

    public int size() {
        return validValues.size();
    }
    
    public void sortValues(Comparator<ValidValue> comparator) {
        Collections.sort(validValues, comparator);
    }

    public boolean isAutoComplete() {
        return isAutoComplete;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setAutoComplete(boolean autoComplete) {
        isAutoComplete = autoComplete;
    }

    public DefaultValueList getDefaultValueList() {
        return defaultValueList;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Integer getDisplayWidth() {
        return null;
    }

    public boolean isEditable() {
        return true;
    }

    public boolean isVisible() {
        return true;
    }

    public boolean isCopyable() {
        return isCopyable;
    }

    public boolean isMarkedForTask() {
        return markedForTask;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setMarkedForTask(boolean markedForTask) {
        this.markedForTask = markedForTask;
    }

    public boolean isSharedForAllSessionFiles() {
        return sharedForAllSessionFiles;
    }

    public void setSharedForAllSessionFiles(boolean sharedForAllSessionFiles) {
        this.sharedForAllSessionFiles = sharedForAllSessionFiles;
    }

    public void addDefaultValue(DefaultValue defaultValue) {
        defaultValueList.add(defaultValue);
    }

    public ValidValueModel getNewInstance(boolean isCloneRequired) {
        ValidValueModel instance = this;
        if (isCloneRequired || (! sharedForAllSessionFiles)) {
            instance = new ValidValueModel();
            instance.displayName = displayName;
            instance.isRequired = isRequired;
            instance.isAutoComplete = isAutoComplete;
            instance.validValues = validValues; // shallow copy should be safe
            instance.selectedValue = selectedValue;
            instance.prefix = prefix;
            instance.suffix = suffix;
            instance.isCopyable = isCopyable;
            instance.markedForTask = markedForTask;
            instance.sharedForAllSessionFiles = sharedForAllSessionFiles;
            instance.defaultValueList = defaultValueList;  // shallow copy is ok
        }
        return instance;
    }

    public String getCoreValue() {
        String coreValue;
        if (selectedValue != null) {
            coreValue = selectedValue.getValue();
            if (coreValue == null) {
                coreValue = "";
            }
        } else {
            coreValue = "";
        }
        return coreValue;
    }

    public String getFileNameValue() {
        String fileNameValue;
        String value = null;

        if (selectedValue != null) {
            value = selectedValue.getValue();
        }

        if ((value != null) && (value.length() > 0)) {
            StringBuilder sb = new StringBuilder(64);
            if (prefix != null) {
                sb.append(prefix);
            }
            sb.append(value);
            if (suffix != null) {
                sb.append(suffix);
            }
            fileNameValue = sb.toString();
        } else {
            fileNameValue = "";
        }

        return fileNameValue;
    }

    public boolean verify() {
        boolean isValid = true;
        errorMessage = null;
        if (isRequired && (selectedValue == null)) {
            isValid = false;
            errorMessage = "Please enter a value for this required field.";
        }
        return isValid;
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
        final String defaultValue = defaultValueList.getValue(target);
        applyValue(defaultValue);
    }

    @Override
    public void applyValue(String value) {
        if (value != null) {
            for (ValidValue validValue : validValues) {
                if (value.equals(validValue.getValue())) {
                    setSelectedValue(validValue);
                    break;
                }
            }
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

    public ValidValue getSelectedValue() {
        return selectedValue;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setRequired(boolean required) {
        if (isRequired != required) {
            isRequired = required;
            if (isRequired) {
                validValues.remove(ValidValue.NONE);
            } else {
                validValues.add(ValidValue.NONE);
            }
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setCopyable(boolean copyable) {
        isCopyable = copyable;
    }

    public void setSelectedValue(ValidValue newValue) {

        if (((selectedValue != null) && !selectedValue.equals(newValue)) ||
             ((selectedValue == null) && (newValue != null))) {

            selectedValue = newValue;
            fireContentsChanged(this, -1, -1);
        }
    }

    public Object getSelectedItem() {
        return getSelectedValue();
    }

    public void setSelectedItem(Object selectedItem) {
        if (selectedItem instanceof ValidValue) {
            setSelectedValue((ValidValue)selectedItem);
        } else {
            setSelectedValue(null);
        }
    }

    public int getSize() {
        return validValues.size();
    }

    public ValidValue getElementAt(int index) {
        return validValues.get(index);
    }

    @Override
    public String toString() {
        @SuppressWarnings("StringBufferReplaceableByString")
        final StringBuilder sb = new StringBuilder();
        sb.append("ValidValueModel");
        sb.append("{displayName='").append(displayName).append('\'');
        sb.append(", validValues=").append(validValues);
        sb.append(", selectedValue=").append(selectedValue);
        sb.append('}');
        return sb.toString();
    }

    protected BasicEventList<ValidValue> getValidValues() {
        return validValues;
    }

    /**
     * Sets the valid values for this model (shallow copy).
     *
     * @param  validValues  new list of values.
     */
    protected void setValidValues(BasicEventList<ValidValue> validValues) {
        this.validValues = validValues;
    }

    /**
     * Clears the current list of valid values.
     * This should only be called if a new set of values is to be loaded.
     */
    protected void clearValidValues() {
        validValues.clear();
        selectedValue = null;
    }
}
