/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.view.component;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import java.awt.*;

/**
 * Extension of the data table header user interface that restores
 * the relationship between the header and any child field group headers.
 * This is needed to properly capture and handle mouse events in the
 * field group headers. 
 *
 * @author Eric Trautman
 */
public class DataTableHeaderUI
        extends BasicTableHeaderUI {

    protected DataTableHeader header;

    public DataTableHeaderUI(DataTableHeader header) {
        this.header = header;
    }

    public void paint(Graphics g,
                      JComponent c) {
        super.paint(g, c);
        header.restoreFieldGroupHeaders();
    }
}