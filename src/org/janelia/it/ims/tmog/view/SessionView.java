/*
 * Copyright (c) 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.view;

import org.janelia.it.ims.tmog.view.component.SessionIcon;

import javax.swing.*;
import java.io.File;

/**
 * This interface specifies the methods required for all session views.
 *
 * @author Eric Trautman
 */
public interface SessionView {

    /**
     * Enumerates the supported data table resize options.
     */
    public enum ResizeType { WINDOW, DATA, PREFERENCES }

    /**
     * @return the primary content panel (container) for the view.
     */
    public JPanel getPanel();

    /**
     * @return the default directory for the view
     *         (used to default file chooser dialogs).
     */
    public File getDefaultDirectory();

    /**
     * @return true if the session's task is in progress; otherwise false.
     */
    public boolean isTaskInProgress();

    /**
     * @return the session's processing icon.
     */
    public SessionIcon getSessionIcon();

    /**
     * Sets the preferences for this view's current project
     * based upon the view's current state.
     */
    public void setPreferencesForCurrentProject();

    /**
     * Clears the preferences for this view's current project.
     */
    public void clearPreferencesForCurrentProject();

    /**
     * Resizes this view's data table.
     *
     * @param  resizeType  identifies type of resize to perform.
     */
    public void resizeDataTable(ResizeType resizeType);
}
