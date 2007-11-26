package org.janelia.it.ims.imagerenamer.field;

import java.io.File;

/**
 * This model supports inserting the source file extension
 * into a rename pattern.
 *
 * @author Eric Trautman
 */
public class FileExtensionModel implements RenameField, SourceFileField {

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

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public void deriveSourceFileValues(File sourceFile) {
        String fileName = sourceFile.getName();
        int extStart = fileName.lastIndexOf('.');
        if ((extStart > -1) && (extStart < (fileName.length() - 1))) {
            extension = fileName.substring(extStart);
        }
    }

    @Override
    public String toString() {
        return getFileNameValue();
    }    
}
