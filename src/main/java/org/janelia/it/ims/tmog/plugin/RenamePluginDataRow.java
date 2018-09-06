/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.plugin;

import org.janelia.it.ims.tmog.DataRow;
import org.janelia.it.ims.tmog.field.DataField;

import java.io.File;
import java.util.List;

/**
 * This class encapsulates the set of rename information collected for
 * a specific file (row).
 *
 * @author Eric Trautman
 */
public class RenamePluginDataRow extends PluginDataRow {

    /** The original file being copied and renamed. */
    private File fromFile;

    /** The directory where the renamed file should be placed. */
    private File outputDirectory;

    /** The renamed file. */
    private File renamedFile;

    private boolean overwriteRequiredForRename;
    
    /**
     * Constructs a copy complete information object.
     *
     * @param  fromFile         the original file being copied and renamed.
     * @param  dataRow          the row of collected data fields.
     * @param  outputDirectory  directory where the renamed file should
     *                          be placed.
     */
    public RenamePluginDataRow(File fromFile,
                               DataRow dataRow,
                               File outputDirectory) {
        super(dataRow);
        this.fromFile = fromFile;
        this.outputDirectory = outputDirectory;
        this.overwriteRequiredForRename = false;
    }

    /**
     * @return the original file being copied and renamed.
     */
    public File getFromFile() {
        return fromFile;
    }

    /**
     * @return the directory where the renamed file should be placed.
     */
    public File getOutputDirectory() {
        return outputDirectory;
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
     * @return true if renaming this file will overwrite an existing file;
     *         otherwise false.
     */
    public boolean isOverwriteRequiredForRename() {
        return overwriteRequiredForRename;
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
        super.setPluginDataValue(fieldDisplayName, value);
        // unset renamedFile to ensure regeneration with new plugin data
        renamedFile = null;
    }

    /**
     * @return the renamed file.
     */
    public File getTargetFile() {
        return getRenamedFile();
    }

    /**
     * @return a string representation of this object.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RenamePluginDataRow");
        sb.append("{fromFile=").append(fromFile);
        sb.append(", dataRow=").append(getDataRow());
        sb.append(", outputDirectory=").append(outputDirectory);
        sb.append('}');
        return sb.toString();
    }

    private void setRenamedFile() {
        StringBuilder fileName = new StringBuilder();
        DataRow dataRow = getDataRow();
        // TODO: add support for nested fields
        List<DataField> dataFields = dataRow.getFields();
        for (DataField field : dataFields) {
            if (field.isMarkedForTask()) {
                fileName.append(field.getFileNameValue());
            }
        }
        renamedFile = new File(outputDirectory, fileName.toString());
        overwriteRequiredForRename = renamedFile.exists();
    }
}