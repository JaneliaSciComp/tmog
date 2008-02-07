package org.janelia.it.ims.imagerenamer.field;

import java.io.File;

/**
 * This model supports inserting the source file extension
 * into a rename pattern.
 *
 * @author Eric Trautman
 */
public class FileExtensionModel implements RenameField {

    private String extension;

    public FileExtensionModel() {
    }

    public String getDisplayName() {
        return null;
    }

    public boolean isEditable() {
        return false;
    }

    public FileExtensionModel getNewInstance() {
        // do not copy extension (must be derived when rename occurs)
        return new FileExtensionModel();
    }

    public String getCoreValue() {
        return getFileNameValue();
    }

    public String getFileNameValue() {
        String value = getExtension();
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
     * Initializes this field's value if necessary.
     *
     * @param sourceFile the source file being renamed.
     */
    public void initializeValue(File sourceFile) {
        String fileName = sourceFile.getName();
        int extStart = fileName.lastIndexOf('.');
        if ((extStart > -1) && (extStart < (fileName.length() - 1))) {
            extension = fileName.substring(extStart);
        }
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    @Override
    public String toString() {
        return getFileNameValue();
    }    
}
