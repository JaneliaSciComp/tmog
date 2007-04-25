/*
 * Copyright © 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.field;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;

/**
 * This class supports the rendering of a button cells
 * within the file table.
 *
 * @author Eric Trautman
 */
public class ButtonRenderer implements TableCellRenderer {

    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
        return (JButton) value;
    }

}
