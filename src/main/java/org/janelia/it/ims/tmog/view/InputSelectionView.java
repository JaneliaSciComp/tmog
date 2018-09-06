/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.view;

import org.janelia.it.ims.tmog.target.FileTarget;

import javax.swing.*;
import java.io.File;
import java.util.List;

/**
 * This interface specifies the callback methods required for views
 * that use the {@link InputSelectionHandler}.
 *
 * @author Eric Trautman
 */
public interface InputSelectionView {
    /**
     * @return the primary content panel (container) for the view.
     */
    public JPanel getPanel();

    /**
     * This method is called after the user has selected a root
     * input file or directory.
     *
     * @param  selectedFile  the selected root.
     */
    public void handleInputRootSelection(File selectedFile);

    /**
     * This method is called after the input root has been reset
     * (either because of an explicit call to
     * {@link InputSelectionHandler#resetInputRoot()} or because other
     * events managed by the handler result in a reset).
     */
    public void handleInputRootReset();

    /**
     * This method is called after a list of input targets has been
     * derived from an input root selection by the user.
     *
     * @param  targets  targets within the selected input root that
     *                  match any configured filters.
     */
    public void processInputTargets(List<FileTarget> targets);
}
