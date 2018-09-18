/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.view.component;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

/**
 * This component supports management of a session processing icon.
 *
 * @author Eric Trautman
 */
public class SessionIcon extends ImageIcon {

    private Component parent;

    /**
     * Creates a session image icon with the default enter values image.
     *
     * @param  parent  the parent container for this icon.
     */
    public SessionIcon(Component parent) {
        super();
        this.parent = parent;
        setToEnterValues();
    }

    public void setToWait() {
        setViewLabelIcon(WAIT_ICON);
    }

    public void setToProcessing() {
        setViewLabelIcon(PROCESSING_ICON);
    }

    public void setToEnterValues() {
        setViewLabelIcon(ENTER_VALUES_ICON);
    }

    private void setViewLabelIcon(ImageIcon imageIcon) {
        setImage(imageIcon.getImage());
        setDescription(imageIcon.getDescription());
        Graphics g = parent.getGraphics();
        if (g != null) { // only repaint if the icon has already been rendered
            parent.repaint();
        }
    }

    private static final URL ENTER_VALUES_IMAGE_URL =
            SessionIcon.class.getResource("/images/tmog_16x16.png");
    private static final ImageIcon ENTER_VALUES_ICON =
            new ImageIcon(ENTER_VALUES_IMAGE_URL, "Entering Values");

    private static final URL WAIT_IMAGE_URL =
            SessionIcon.class.getResource("/images/16-clock.png");
    private static final ImageIcon WAIT_ICON =
            new ImageIcon(WAIT_IMAGE_URL, "Waiting to Begin Processing");

    private static final URL PROCESSING_IMAGE_URL =
            SessionIcon.class.getResource("/images/16-spinner.gif");
    private static final ImageIcon PROCESSING_ICON =
            new ImageIcon(PROCESSING_IMAGE_URL, "Processing Files");

}
