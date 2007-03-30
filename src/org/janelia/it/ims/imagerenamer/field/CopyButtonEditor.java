/*
 * Copyright © 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer.field;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This class supports the editing of a copy button cell
 * within the file table.
 *
 * @author Eric Trautman
 */
public class CopyButtonEditor extends AbstractCellEditor
        implements TableCellEditor, ActionListener {

    private JTable fileTable;
    private JButton copyButton;

    public CopyButtonEditor() {
    }

    public Object getCellEditorValue() {
        return copyButton;
    }

    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int column) {
        fileTable = table;
        if (row > 0) {
            copyButton = (JButton) value;
            ActionListener[] listeners = copyButton.getActionListeners();
            for (ActionListener listener : listeners) {
                copyButton.removeActionListener(listener);
            }
            copyButton.addActionListener(this);
        } else {
            copyButton = null;
        }
        return copyButton;
    }

    public void actionPerformed(ActionEvent e) {
        int row = fileTable.getSelectedRow();
        TableModel model = fileTable.getModel();
        for (int col = 2; col < model.getColumnCount(); col++) {
            RenameField preValue = (RenameField) model.getValueAt((row-1), col);
            model.setValueAt(preValue.getNewInstance(), row, col);
        }
        fileTable.repaint();
        fireEditingStopped(); //Make the renderer reappear.
        fileTable.changeSelection(row, 2, false, false);
        fileTable.editCellAt(row, 2);
    }
}
