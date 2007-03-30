/*
 * Copyright © 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.field;

/**
 * This interface specifies the methods required for all rename fields.
 *
 * @author Eric Trautman
 */
public interface RenameField {

    /**
     * @return the display name (column header) for this field.
     */
    public String getDisplayName();

    /**
     * @return true if the field should be editable (and displayed) in
     *         within the file table; otherwise false.
     */
    public boolean isEditable();

    /**
     * @return a new instance of this field (similar to clone - deep copy).
     */
    public RenameField getNewInstance();

    /**
     * @return the value to be used when renaming a file
     *         (may differ from what is displayed in the user interface).
     */
    public String getFileNameValue();

    /**
     * Verfies that the field contents are valid.
     * If the field contents are not valid, the {@link #getErrorMessage}
     * method can be called to retrieve detailed error information.
     *
     * @return true if the contents are valid; otherwise false.
     */
    public boolean verify();

    /**
     * Returns a detailed error message if the {@link #verify} method has been
     * called and this field is not valid.
     *
     * @return a detailed error message if verification failed; otherwise null.
     */
    public String getErrorMessage();
}
