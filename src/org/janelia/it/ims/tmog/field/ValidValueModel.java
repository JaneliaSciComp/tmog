/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.Target;

import javax.swing.*;
import java.util.ArrayList;

/**
 * This model supports inserting a selected value from a predefined set
 * of valid values into a rename pattern.
 *
 * @author Eric Trautman
 */
public class ValidValueModel extends AbstractListModel implements ComboBoxModel, DataField {

    private String displayName;
    private boolean isRequired;
    private ArrayList<ValidValue> validValues;
    private ValidValue selectedValue;
    private String errorMessage;
    private String prefix;
    private String suffix;
    private boolean isCopyable;
  
    public ValidValueModel() {
        this.validValues = new ArrayList<ValidValue>();
        this.isCopyable = true;
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

    public boolean isCopyable() {
        return isCopyable;
    }

    public ValidValueModel getNewInstance() {
        ValidValueModel instance = new ValidValueModel();
        instance.displayName = displayName;
        instance.isRequired = isRequired;
        instance.validValues = validValues; // shallow copy should be safe
        instance.selectedValue = selectedValue;
        instance.prefix = prefix;
        instance.suffix = suffix;
        instance.isCopyable = isCopyable;
        return instance;
    }

    public String getCoreValue() {
        String coreValue;
        if (selectedValue != null) {
            coreValue = selectedValue.getValue();
        } else {
            coreValue = "";
        }
        return coreValue;
    }

    public String getFileNameValue() {
        String fileNameValue;
        if (selectedValue != null) {
            StringBuilder sb = new StringBuilder(64);
            if (prefix != null) {
                sb.append(prefix);
            }
            sb.append(selectedValue.getValue());
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
        // nothing to initialize
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
            if (displayName != null) {
                length = displayName.length();
                if (length > longestLength) {
                    longestName = displayName;
                    longestLength = length;
                }
            }
        }
        return longestName;
    }
}
