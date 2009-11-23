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

    /**
     * Constructs a renderer for field group cells in the specified parent
     * data table.
     *
     * @param  parentTable  data table that containf field group cells.
     */
    public DataFieldGroupRenderer(DataTable parentTable) {
        this.parentTable = parentTable;
        this.nestedTable = null;
    }

    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
        Component cellRenderer;


        if (value instanceof DataFieldGroupModel) {
            DataFieldGroupModel model = (DataFieldGroupModel) value;
            if (nestedTable == null) {
                // NOTE: need to create nested table here instead of
                //       in renderer constructor to prevent infinite
                //       recursion problem when the nested table sets
                //       up its renderers
                nestedTable = new NestedDataTable(parentTable);                
            }
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