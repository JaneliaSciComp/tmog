/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
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

}
