/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin;

import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.field.PluginDataModel;
import org.janelia.it.ims.tmog.target.FileTarget;
import org.janelia.it.ims.tmog.target.Target;

import java.io.File;
import java.util.HashMap;

/**
 * This class wraps the data collected for a specific target (row) and
 * provides simplified access to that data for plug-in components.
 *
 * @author Eric Trautman
 */
public class PluginDataRow {

    /** The row of collected data fields. */
    private DataRow dataRow;

    /** Map of field display names to model objects. */
    private HashMap<String, DataField> displayNameToFieldMap;

    /**
     * Constructs a copy complete information object.
     *
     * @param  dataRow     the row of collected data fields.
     */
    public PluginDataRow(DataRow dataRow) {
        this.dataRow = dataRow;
        this.displayNameToFieldMap = new HashMap<String, DataField>();
        for (DataField field : dataRow.getFields()) {
            String displayName = field.getDisplayName();
            if (displayName != null) {
                this.displayNameToFieldMap.put(displayName, field);
            }
        }
    }

    /**
     * @return the row of collected data fields.
     */
    public DataRow getDataRow() {
        return dataRow;
    }

    /**
     * @return map of display names to fields for this row.
     */
    public HashMap<String, DataField> getDisplayNameToFieldMap() {
        return displayNameToFieldMap;
    }

    /**
     * Returns the field model object value associated with the specified
     * display name or null if no associated model exists.
     *
     * @param  fieldDisplayName  display name of desired field model.
     *
     * @return the associated field model value or null.
     */
    public String getCoreValue(String fieldDisplayName) {
        String value = null;
        DataField field = displayNameToFieldMap.get(fieldDisplayName);
        if (field != null) {
            value = field.getCoreValue();
        }
        return value;
    }

    /**
     * Returns the field model associated with the specified
     * display name or null if no associated model exists.
     *
     * @param  fieldDisplayName  display name of desired field model.
     *
     * @return the associated field model or null.
     */
    public DataField getDataField(String fieldDisplayName) {
        return displayNameToFieldMap.get(fieldDisplayName);
    }

    /**
     * Returns the plugin model object value associated with the specified
     * display name.
     *
     * @param  fieldDisplayName  name of desired plugin model.
     *
     * @return the associated plugin model value.
     *
     * @throws IllegalArgumentException
     *   if the specified display name does not reference a plugin data model.
     */
    public Object getPluginDataValue(String fieldDisplayName)
            throws IllegalArgumentException {

        Object value;
        DataField field = displayNameToFieldMap.get(fieldDisplayName);
        if (field instanceof PluginDataModel) {
            value = ((PluginDataModel) field).getValue();
        } else {
            throw new IllegalArgumentException(
                    "PluginDataModel instance with displayName '" +
                    fieldDisplayName + "' cannot be found in " + this);
        }
        return value;
    }

    /**
     * Applies the specified string value to the field with the
     * specified display name.
     *
     * @param  fieldDisplayName  identifies the field to be updated.
     * @param  value             the new value for the field.
     */
    public void applyPluginDataValue(String fieldDisplayName,
                                     String value) {
        DataField field = displayNameToFieldMap.get(fieldDisplayName);
        if (field != null) {
            field.applyValue(value);
        }
    }

    /**
     * Sets the value for the plugin data model with the specified display name.
     *
     * @param  fieldDisplayName  identifies the field to be updated.
     * @param  value             the new value for the field.
     *
     * @throws IllegalArgumentException
     *   if the specified display name does not reference a plugin data model.
     */
    public void setPluginDataValue(String fieldDisplayName,
                                   Object value)
            throws IllegalArgumentException {

        DataField field = displayNameToFieldMap.get(fieldDisplayName);
        if (field instanceof PluginDataModel) {
            ((PluginDataModel) field).setValue(value);
        } else {
            throw new IllegalArgumentException(
                    "PluginDataModel instance with displayName '" +
                    fieldDisplayName + "' cannot be found in " + this);
        }
    }

    /**
     * @return the target file or null if this row's target is not a file.
     */
    public File getTargetFile() {
        File targetFile = null;
        Target target = dataRow.getTarget();
        if (target instanceof FileTarget) {
            targetFile = ((FileTarget) target).getFile();
        }
        return targetFile;
    }
    
    /**
     * @return a string representation of this object.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("PluginDataRow");
        sb.append("{dataRow=").append(dataRow);
        sb.append('}');
        return sb.toString();
    }

}