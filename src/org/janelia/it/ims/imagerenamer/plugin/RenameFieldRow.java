/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.plugin;

import org.janelia.it.ims.imagerenamer.field.PluginDataModel;
import org.janelia.it.ims.imagerenamer.field.RenameField;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;

/**
 * This class encapsulates the set of rename information collected for
 * a specific file (row).
 *
 * @author Eric Trautman
 */
public class RenameFieldRow {

    /** The original file being copied and renamed. */
    private File fromFile;

    /** The rename field model objects. */
    private RenameField[] renameFields;

    /** The directory where the renamed file should be placed. */
    private File outputDirectory;

    /** The renamed file. */
    private File renamedFile;

    /** Map of rename field display names to rename field model objects. */
    private HashMap<String, RenameField> displayNameToFieldMap;

    /**
     * Constructs a copy complete information object.
     *
     * @param  fromFile         the original file being copied and renamed.
     * @param  renameFields     the list of rename field model objects
     *                          referenced during processing.
     * @param  outputDirectory  directory where the renamed file should
     *                          be placed.
     */
    public RenameFieldRow(File fromFile,
                          RenameField[] renameFields,
                          File outputDirectory) {
        this.fromFile = fromFile;
        this.renameFields = renameFields;
        this.outputDirectory = outputDirectory;
        this.displayNameToFieldMap = new HashMap<String, RenameField>();
        for (RenameField field : renameFields) {
            String displayName = field.getDisplayName();
            if (displayName != null) {
                this.displayNameToFieldMap.put(displayName, field);
            }
        }
    }

    /**
     * @return the original file being copied and renamed.
     */
    public File getFromFile() {
        return fromFile;
    }

    /**
     * @return the rename field model objects.
     */
    public RenameField[] getRenameFields() {
        return renameFields;
    }

    /**
     * @return the directory where the renamed file should be placed.
     */
    public File getOutputDirectory() {
        return outputDirectory;
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
        RenameField field = displayNameToFieldMap.get(fieldDisplayName);
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
    public RenameField getRenameField(String fieldDisplayName) {
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
        RenameField field = displayNameToFieldMap.get(fieldDisplayName);
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
     * @return the renamed file based upon the field models for this row.
     */
    public File getRenamedFile() {
        if (renamedFile == null) {
            setRenamedFile();
        }
        return renamedFile;
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

        RenameField field = displayNameToFieldMap.get(fieldDisplayName);
        if (field instanceof PluginDataModel) {
            ((PluginDataModel) field).setValue(value);
        } else {
            throw new IllegalArgumentException(
                    "PluginDataModel instance with displayName '" +
                    fieldDisplayName + "' cannot be found in " + this);
        }
        // unset renamedFile to ensure regeneration with new plugin data
        renamedFile = null;
    }

    /**
     * @return the relative path (parent directory + file name) for the
     *         renamed file.
     */
    public String getRelativePath() {
        File renameDir = renamedFile.getParentFile();
        String relativePath;
        if (renameDir == null) {
            relativePath = renamedFile.getName();
        } else {
            relativePath = renameDir.getName() + "/" + renamedFile.getName();
        }
        return relativePath;
    }

    /**
     * @return a string representation of this object.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RenameFieldRow");
        sb.append("{fromFile=").append(fromFile);
        sb.append(", renameFields=").append(renameFields == null ? "null" : Arrays.asList(renameFields).toString());
        sb.append(", outputDirectory=").append(outputDirectory);
        sb.append('}');
        return sb.toString();
    }

    private void setRenamedFile() {
        StringBuilder fileName = new StringBuilder();
        for (RenameField field : renameFields) {
            fileName.append(field.getFileNameValue());
        }
        renamedFile = new File(outputDirectory, fileName.toString());
    }
}