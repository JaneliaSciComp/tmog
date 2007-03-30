/*
 * Copyright © 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.field;

import org.janelia.it.ims.imagerenamer.field.RenameField;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import java.util.ArrayList;

/**
 * This model supports inserting a selected value from a predefined set
 * of valid values into a rename pattern.
 *
 * @author Eric Trautman
 */
public class ValidValueModel extends AbstractListModel implements ComboBoxModel, RenameField {

    private String displayName;
    private boolean isRequired;
    private ArrayList<ValidValue> validValues;
    private ValidValue selectedValue;
    private String errorMessage;

    public ValidValueModel() {
        this.validValues = new ArrayList<ValidValue>();
    }

    public void addValidValue(ValidValue validValue) {
        validValues.add(validValue);
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEditable() {
        return true;
    }

    public ValidValueModel getNewInstance() {
        ValidValueModel instance = new ValidValueModel();
        instance.displayName = displayName;
        instance.isRequired = isRequired;
        instance.validValues = validValues; // shallow copy should be safe
        instance.selectedValue = selectedValue;
        return instance;
    }

    public String getFileNameValue() {
        String fileNameValue;
        if (selectedValue != null) {
            fileNameValue = selectedValue.getValue();
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

    public boolean isRequired() {
        return isRequired;
    }

    public ValidValue getSelectedValue() {
        if (isRequired && (selectedValue == null) &&
                (validValues.size() == 1)) {
            selectedValue = validValues.get(0);        
        }
        return selectedValue;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setRequired(boolean required) {
        isRequired = required;
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

    public Object getElementAt(int index) {
        return validValues.get(index);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ValidValueModel");
        sb.append("{displayName='").append(displayName).append('\'');
        sb.append(", validValues=").append(validValues);
        sb.append(", selectedValue=").append(selectedValue);
        sb.append('}');
        return sb.toString();
    }

    public String getLongestDisplayName() {
        String longestName = "";
        int longestLength = 0;

        int length;
        String displayName;
        for (ValidValue value : validValues) {
            displayName = value.getDisplayName();
            length = displayName.length();
            if (length > longestLength) {
                longestName = displayName;
                longestLength = length;
            }
        }
        return longestName;
    }
}
