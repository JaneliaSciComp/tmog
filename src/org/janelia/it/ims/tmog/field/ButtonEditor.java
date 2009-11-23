/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.field;

import org.janelia.it.ims.tmog.TransmogrifierTableModel;
import org.janelia.it.ims.tmog.view.component.ButtonPanel;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import java.awt.*;

/**
 * This class supports the editing of button cells
 * within the file table.
 *
 * @author Eric Trautman
 */
public class ButtonEditor extends AbstractCellEditor
        implements TableCellEditor {

    public Object getCellEditorValue() {
        return null;
    }

    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int column) {
        if (value instanceof ButtonPanel) {
            TableModel tableModel = table.getModel();
            if (tableModel instanceof TransmogrifierTableModel) {
                TransmogrifierTableModel model =
                        (TransmogrifierTableModel) tableModel;
                ButtonPanel panel = (ButtonPanel) value;
                if (panel.isDeleteRow()) {
                    stopCellEditing();
                    model.removeRow(row);
                } else if (panel.isCopyPreviousRow()) {
                    stopCellEditing();
                    model.copyRow((row - 1), row);
                } else if (panel.isAddRow()) {
                    stopCellEditing();
                    model.addRow(row);
                }
            }
        }

        return null;
    }

}
