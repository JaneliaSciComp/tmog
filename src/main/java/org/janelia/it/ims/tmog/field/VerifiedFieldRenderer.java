/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * This class supports the rendering of a verified field cell
 * within the file table.
 *
 * @author Eric Trautman
 */
public class VerifiedFieldRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {

            Component cellRenderer;

            if (value instanceof VerifiedFieldModel) {
                cellRenderer =
                        super.getTableCellRendererComponent(table,
                                                            value.toString(),
                                                            isSelected,
                                                            hasFocus,
                                                            row,
                                                            column);
                if (hasFocus) {
                  table.editCellAt(row, column);
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
