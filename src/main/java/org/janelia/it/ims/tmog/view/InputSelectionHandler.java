/*
 * Copyright (c) 2015 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.view;

import java.io.File;

/**
 * The common interface for all input handlers.
 *
 * @author Eric Trautman
 */
public interface InputSelectionHandler {

    /**
     * @return the default directory managed by this handler (or null for non-file based handlers).
     */
    File getDefaultDirectory();

    /**
     * Enables or disables the setDirectoryButton.
     * Always hides the cancelButton.
     *
     * @param  isEnabled  indicates whether the setDirectoryButton should
     *                    be enabled.
     */
    void setEnabled(boolean isEnabled);

    /**
     * Resets (blanks out) the input root label,
     * enables the set input button, hides the cancel button,
     * and notifies the parent view.
     */
    void resetInputRoot();

}