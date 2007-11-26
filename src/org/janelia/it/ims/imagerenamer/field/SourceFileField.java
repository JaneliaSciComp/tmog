package org.janelia.it.ims.imagerenamer.field;

import java.io.File;

/**
 * This interface specifies the methods required for all fields
 * that derive values from the source file being renamed.
 *
 * @author Eric Trautman
 */
public interface SourceFileField {

    public void deriveSourceFileValues(File sourceFile);

}
