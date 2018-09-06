/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.config.preferences.FieldDefaultSet;
import org.janelia.it.ims.tmog.target.Target;

/**
 * This interface specifies the methods required for all data fields.
 *
 * @author Eric Trautman
 */
public interface DataField {

    /**
     * @return the display name (column header) for this field.
     */
    public String getDisplayName();

    /**
     * @return the preferred width for this field's column
     *         (or null if none has been configured).
     */
    public Integer getDisplayWidth();

    /**
     * @return true if the field should be editable in
     *         within the data field table; otherwise false.
     */
    public boolean isEditable();

    /**
     * @return true if the field should be displayed in
     *         the data field table; otherwise false.
     */
    public boolean isVisible();

    /**
     * @return true if the field should be copied when its row is
     *         copied in the file table; otherwise false.
     */
    public boolean isCopyable();

    /**
     * @return true if the field should be utilized for task processing;
     *         false if it should be ignored by the task.
     */
    public boolean isMarkedForTask();

    /**
     * @param  isCloneRequired  indicates whether a clone (deep copy)
     *                          is required (true) or optional (false).
     *
     * @return a new instance of this field (similar to clone - deep copy).
     */
    public DataField getNewInstance(boolean isCloneRequired);

    /**
     * @return the core value for this field (as entered).
     */
    public String getCoreValue();

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


    /**
     * Initializes this field's value based upon the specified target.
     *
     * @param  target  the target being processed.
     */
    public void initializeValue(Target target);

    /**
     * If the specified value is not empty, sets this field's core value
     * the specified value.
     *
     * @param  value  value to apply.
     */
    public void applyValue(String value);

    /**
     * If the specified set of defaults contains a value for this field,
     * sets this field's core value to that default.
     *
     * @param  defaultSet  set of default values to examine.
     */
    public void applyDefault(FieldDefaultSet defaultSet);

    /**
     * Adds this field's core value as a default for the specified set.
     *
     * @param  defaultSet  set of default values to modify.
     */
    public void addAsDefault(FieldDefaultSet defaultSet);
}
