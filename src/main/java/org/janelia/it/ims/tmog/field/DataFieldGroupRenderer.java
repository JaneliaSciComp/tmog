/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.view.component.DataTable;
import org.janelia.it.ims.tmog.view.component.NestedDataTable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * This class supports the rendering of a field group cell
 * within the file table.
 *
 * @author Eric Trautman
 */
public class DataFieldGroupRenderer extends DefaultTableCellRenderer {

    private DataTable parentTable;
    private NestedDataTable nestedTable;

    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
        Component cellRenderer;


        if ((table instanceof DataTable) &&
            (value instanceof DataFieldGroupModel)) {

            if (parentTable != table) {
                parentTable = (DataTable) table;
                nestedTable = new NestedDataTable(parentTable);
            }

            DataFieldGroupModel model = (DataFieldGroupModel) value;
            nestedTable.setModel(model, column);

            // reflect row selection background in nested table 
            if (table.getSelectedRow() == row) {
                nestedTable.setBackground(table.getSelectionBackground());
            } else {
                nestedTable.setBackground(table.getBackground());
            }

            cellRenderer = nestedTable;

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