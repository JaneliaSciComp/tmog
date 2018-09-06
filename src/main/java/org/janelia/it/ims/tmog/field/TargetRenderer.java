/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.target.FileTarget;
import org.janelia.it.ims.tmog.target.Target;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.File;

/**
 * This class supports the rendering of a target cell
 * within the file table.
 *
 * @author Eric Trautman
 */
public class TargetRenderer extends DefaultTableCellRenderer {

    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
        Component cellRenderer;

        if (value instanceof Target) {
            Target target = (Target) value;
            cellRenderer =
                    super.getTableCellRendererComponent(table,
                                                        target.getName(),
                                                        isSelected,
                                                        hasFocus,
                                                        row,
                                                        column);
            // add absolute path tool tip for file targets
            if ((target instanceof FileTarget) &&
                (cellRenderer instanceof JComponent)) {
                File targetFile = ((FileTarget) target).getFile();
                if (targetFile != null) {
                    ((JComponent) cellRenderer).setToolTipText(
                            targetFile.getAbsolutePath());
                }
            }

        } else {
            cellRenderer =
                    super.getTableCellRendererComponent(table,
                                                        value,
                                                        isSelected,
                                                        hasFocus,
                                                        row,
                                                        column);
        }

        return cellRenderer;
    }

}
