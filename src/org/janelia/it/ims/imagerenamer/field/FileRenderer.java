/*
 * Copyright � 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.field;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.JTable;
import java.awt.Component;
import java.io.File;

/**
 * This class supports the rendering of a file cell
 * within the file table.
 *
 * @author Peter Davies
 */
public class FileRenderer extends DefaultTableCellRenderer {

    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
        Component cellRenderer;

        if (value instanceof File) {
            File file = (File) value;
            cellRenderer =
                    super.getTableCellRendererComponent(table,
                                                        file.getName(),
                                                        isSelected,
                                                        hasFocus,
                                                        row,
                                                        column);
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
