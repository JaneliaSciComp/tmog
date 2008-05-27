/*
 * Copyright � 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.DataTableModel;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This class supports the editing of button cells
 * within the file table.
 *
 * @author Eric Trautman
 */
public class ButtonEditor extends AbstractCellEditor
        implements TableCellEditor, ActionListener {

    private JTable fileTable;
    private JButton button;
    private int row;
    private int column;

    public ButtonEditor() {
    }

    public Object getCellEditorValue() {
        return button;
    }

    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int column) {
        fileTable = table;
        this.row = row;
        this.column = column;
        button = (JButton) value;
        ActionListener[] listeners = this.button.getActionListeners();
        for (ActionListener listener : listeners) {
            button.removeActionListener(listener);
        }
        button.addActionListener(this);

        return button;
    }

    public void actionPerformed(ActionEvent e) {

        DataTableModel model = (DataTableModel) fileTable.getModel();
        final boolean isRemoveFileButton = (column == 0);
        final boolean isCopyButton = (column == 1);

        fireEditingStopped(); //Make the renderer reappear.

        if (isRemoveFileButton) {
            model.removeRow(row);
        } else if (isCopyButton) {
            model.copyRow((row - 1), row);
        }

        if (isCopyButton) {
            final int firstFieldCol = DataTableModel.getFirstFieldColumn();
            fileTable.changeSelection(row, firstFieldCol, false, false);
            fileTable.editCellAt(row, firstFieldCol);
        }

    }
}
